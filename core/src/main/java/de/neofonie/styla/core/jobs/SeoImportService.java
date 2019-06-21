package de.neofonie.styla.core.jobs;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.neofonie.styla.core.models.CloudServiceModel;
import de.neofonie.styla.core.models.Seo;
import de.neofonie.styla.core.utils.PageUtils;
import de.neofonie.styla.core.utils.SeoUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

@Component(service = Runnable.class)
@Designate(ocd = SeoImportService.Config.class, factory = true)
public class SeoImportService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeoImportService.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Replicator replicator;

    CloudServiceModel cloudServiceModel = new CloudServiceModel();

    /*
     * Name of the seo import service subservice
     */
    String SEO_IMPORT_SERVICE = "seoImportService";

    @Reference
    private SlingRepository repository;

    private String contentRootPath;
    private boolean autoActivate;
    private String templateType;

    @ObjectClassDefinition(name = "Styla SEO job service", description = "CRON job for importing SEO data from Styla")
    public static @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "0 * * ? * *";

        @AttributeDefinition(name = "Template Type", description = "Import works only for pages with this template type")
        String templateType() default "/conf/styla/settings/wcm/templates/master";

        @AttributeDefinition(name = "Content Root Path", description = "Root path for the styla relating content")
        String contentRootPath() default "/content";

        @AttributeDefinition(name = "Auto-Activate", description = "If checked, pages with imported SEO data will be activated automatically")
        boolean autoActivate() default true;

    }


    @Activate
    protected void activate(final Config config) {

        String configuredContentRootPath = String.valueOf(config.contentRootPath());
        String templateType = String.valueOf(config.templateType());
        this.autoActivate = Boolean.valueOf(config.autoActivate());
        this.contentRootPath = (configuredContentRootPath != null) ? configuredContentRootPath : null;
        this.templateType = StringUtils.isNotEmpty(templateType) ? templateType : "/conf/styla/settings/wcm/templates/master";

        LOGGER.info("configure: contentRootPath='{}'", this.contentRootPath);
    }

    private ResourceResolver getResourceResolver() {
        Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SEO_IMPORT_SERVICE);

        try {
            ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);
            if (resourceResolver != null) {
                return resourceResolver;
            } else {
                LOGGER.debug("ResourceResolver is null (Subservice {})", SEO_IMPORT_SERVICE);
            }
        } catch (LoginException e) {
            LOGGER.error("Login Exception for Subservice - " + SEO_IMPORT_SERVICE, e.getMessage());
        }
        return null;
    }

    @Override
    public void run() {
        final String[] templateTypes = templateType.split("\\|");
        final String[] contentRootPaths = contentRootPath.split("\\|");

        if (templateTypes.length != contentRootPaths.length) {
            LOGGER.error("The templateType and contentRootPath should not have different length for piped values");
            return;
        }

        for (int i = 0; i < templateTypes.length; i++) {
            importData(templateTypes[i], contentRootPaths[i]);
        }
    }

    private void importData(final String templateType, final String contentRootPath) {
        LOGGER.info(String.format("Start importing seo data for path=%s and template=%s",
                contentRootPath, templateType));

        ResourceResolver resourceResolver = getResourceResolver();
        Page contentRootPage = getContentRootPage(resourceResolver, contentRootPath);

        if(contentRootPage == null) {
            LOGGER.error("Failed to find content root page - aborting seo import service");
            return;
        }

        final List<Page> pages = Lists.newArrayList();
        pages.add(contentRootPage);
        PageUtils.recursivelySearchForPage(contentRootPage.listChildren(), pages, templateType);
        LOGGER.info(String.format("Found pages (%d)", pages.size()));

        for (final Page childPage : pages) {
            LOGGER.info(String.format("Processing page: %s on %s", childPage.getName(), childPage.getPath()));

            if (!isSeoImportEnabled(childPage)) {
                LOGGER.warn("Skip page due disableSeoImport flag");
                continue;
            }

            final String seoApiUrl = buildSeoApiUrl(resourceResolver, contentRootPage, childPage);
            if (seoApiUrl != null) {
                final Seo seo = fetchSeoData(seoApiUrl);
                if (seo != null) {
                    applySeoData(seo, childPage, resourceResolver);
                } else {
                    LOGGER.warn(String.format("Fetched seo is empty for page %s - no seo data is applied", childPage.getName()));
                }
            }

            LOGGER.info(String.format("Finished page: %s on %s", childPage.getName(), childPage.getPath()));
        }

        LOGGER.info("Finish importing seo data");
    }

    private boolean isSeoImportEnabled(Page page) {
        final ValueMap properties = page.getProperties();

        try {
            if (properties != null && properties.containsKey("disableSeoImport")) {
                final Object propertyValue = properties.get("disableSeoImport");
                return "false".equalsIgnoreCase(String.valueOf(propertyValue));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse 'disableSeoImport' property, fallback to true");
        }

        return true;
    }

    private String buildSeoApiUrl(final ResourceResolver resourceResolver, final Page rootPage, final Page currentPage) {
        final String path = StringUtils.removeStart(currentPage.getPath(), rootPage.getParent().getPath());

        if (path == null) {
            return null;
        }

        return cloudServiceModel.getSeoApiUrl(resourceResolver, rootPage)
           .replace("$URL", path)
           .replace("$LANG", currentPage.getLanguage().toString());
    }

    private Seo fetchSeoData(String seoApiUrl) {
        final String jsonResponse = executeGetDataRequest(seoApiUrl);

        if (jsonResponse == null) {
            LOGGER.warn("Seo api response should not be empty");
            return null;
        }

        final Gson gson = new GsonBuilder().create();
        final Seo seo = gson.fromJson(jsonResponse, Seo.class);

        final String status = seo.getStatus() + "";
        if (!status.startsWith("2")) {
            LOGGER.warn(String.format("Seo api is returning status: %s", status));
        }

        return seo;
    }

    private void applySeoData(final Seo seo, final Page page, final ResourceResolver resourceResolver) {
        if (seo == null) {
            LOGGER.warn(String.format("Couldn't find seo for %s on %s", page.getName(), page.getPath()));
            return;
        }

        LOGGER.info(String.format("Start applying seo to page: %s on %s", page.getName(), page.getPath()));

        final Resource resource = page.getContentResource();
        SeoUtils.writeSeoToResource(seo, resource);

        if (!autoActivate) {
            LOGGER.warn("Autoactive is false");
            return;
        }

        try {
            resourceResolver.refresh();
            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, resource.getPath());
            resourceResolver.commit();
        } catch (NullPointerException | ReplicationException | PersistenceException e) {
            LOGGER.warn(String.format("Could not auto activate %s", resource.getPath()), e);
        }

    }

    private String executeGetDataRequest(String seoApiUrl) {
        LOGGER.info(String.format("Requesting seo api url: %s", seoApiUrl));
        String response = null;
        HttpClient httpClient = new HttpClient();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setSoTimeout(10000);
        params.setConnectionTimeout(10000);
        httpClient.getHttpConnectionManager().setParams(params);

        HttpMethod httpGet = new GetMethod(seoApiUrl);
        HttpMethodParams httpMethodParams = new HttpMethodParams();
        httpMethodParams.setSoTimeout(10000);
        httpMethodParams.setParameter("accept", "application/json");
        httpGet.setParams(httpMethodParams);

        // TODO: for non exiting pages the status code is 200 but the json contains a status field which might be 404 ...
        try {
            int statusCode = httpClient.executeMethod(httpGet);
            if (statusCode == HTTP_OK) {
                response = httpGet.getResponseBodyAsString();
            } else if (statusCode == HTTP_NOT_FOUND) {
                LOGGER.warn("HTTP request to " + seoApiUrl + " returned status code 404 and is not handled as a failure. No data?");
            } else {
                LOGGER.warn("HTTP request to " + seoApiUrl + " returned status code " + statusCode + ". Aborting.");
            }
        } catch (IOException e) {
            LOGGER.error("Error during HTTP request to " + seoApiUrl + ": " + e.getMessage());
        }
        return response;
    }


    private Page getContentRootPage(final ResourceResolver resourceResolver, final String contentRootPath) {
        final Resource contentRootResource = getContentRootResource(resourceResolver, contentRootPath);
        if (contentRootResource != null) {
            return contentRootResource.adaptTo(Page.class);
        }
        return null;
    }

    /**
     * Resolve content root resource as configured, e.g. /content/styla/en
     *
     * @param resourceResolver
     * @return Resource content resource when configured path can be resolved, otherwise null
     */
    private Resource getContentRootResource(final ResourceResolver resourceResolver, final String contentRootPath) {
        if (StringUtils.isNotEmpty(contentRootPath) && resourceResolver != null) {
            return resourceResolver.getResource(contentRootPath);
        }
        return null;
    }
}
