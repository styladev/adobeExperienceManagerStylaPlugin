package de.neofonie.styla.core.jobs;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.neofonie.styla.core.models.*;
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
import java.lang.reflect.Type;
import java.util.*;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

@Component(service = Runnable.class)
@Designate(ocd = SeoImportService.Config.class, factory = true)
public class SeoImportService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeoImportService.class);

    private static boolean cronjobRunning = false;

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
    private String siteRootPath;
    private boolean autoActivate;
    private String templateType;
    private String schedulerExpression;

    @ObjectClassDefinition(name = "Styla SEO job service", description = "CRON job for importing SEO data from Styla")
    public static @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "0 0 0 ? * *";

        @AttributeDefinition(name = "Template Type", description = "Import works only for pages with this template type")
        String templateType() default "/conf/styla/settings/wcm/templates/master";

        @AttributeDefinition(name = "Content Root Path", description = "Root path for the styla relating content")
        String contentRootPath() default "/content";

        @AttributeDefinition(name = "Site Root Path", description = "Site root path which is external invisible")
        String siteRootPath() default "";

        @AttributeDefinition(name = "Auto-Activate", description = "If checked, pages with imported SEO data will be activated automatically")
        boolean autoActivate() default true;

    }

    @Activate
    protected void activate(final Config config) {
        this.autoActivate = config.autoActivate();
        this.contentRootPath = config.contentRootPath();
        this.siteRootPath = config.siteRootPath();
        this.templateType = config.templateType();
        this.schedulerExpression = config.scheduler_expression();

        LOGGER.info("activate: contentRootPath='{}'", this.contentRootPath);
    }

    private ResourceResolver getResourceResolver() {
        final Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SEO_IMPORT_SERVICE);

        try {
            return resourceResolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException e) {
            LOGGER.error("Login Exception for Subservice - " + SEO_IMPORT_SERVICE, e.getMessage());
        }
        return null;
    }

    @Override
    public void run() {
        if (cronjobRunning) {
            LOGGER.warn("Aborting cronjob start since it is still running");
            return;
        }

        try {
            cronjobRunning = true;
            importData();
        } catch(Exception e) {
            LOGGER.error("Failed to execute cronjob", e);
        } finally {
            cronjobRunning = false;
        }
    }

    private void importData() {
        final String uniqueID = UUID.randomUUID().toString();
        final Date startDate = new Date();

        LOGGER.info(String.format("Start new scheduled import (%s) via expression %s", uniqueID, this.schedulerExpression));

        final String[] templateTypes = templateType.split("\\|");
        final String[] contentRootPaths = contentRootPath.split("\\|");
        final String[] siteRootPaths = siteRootPath.split("\\|");

        if (templateTypes.length != contentRootPaths.length) {
            LOGGER.error("The templateType and contentRootPath should not have different length for piped values");
            return;
        }

        if (contentRootPaths.length != siteRootPaths.length) {
            LOGGER.error("The contentRootPath and siteRootPath should not have different length for piped values");
            return;
        }

        final Map<String, CrawlerConfig> configs = new HashMap<>();
        for (int i = 0; i < templateTypes.length; i++) {
            final CrawlerConfig config = new CrawlerConfig();
            config.setContentRootPath(contentRootPaths[i]);
            config.setSiteRootPath(siteRootPaths[i]);
            config.setTemplateType(templateTypes[i]);
            configs.put(contentRootPaths[i], config);
        }

        for (final Map.Entry<String, CrawlerConfig> entry : configs.entrySet()) {
            final CrawlerConfig config = entry.getValue();
            importEntry(config.getContentRootPath(), config.getSiteRootPath(), config.getTemplateType());
        }

        final Date endDate = new Date();
        final float diffInMillies = endDate.getTime() - startDate.getTime();
        final float diffInMinutes = diffInMillies / (60 * 1000);
        LOGGER.info(String.format("Finished scheduled import in %.2f minutes (%s)", diffInMinutes, uniqueID));
    }

    private void importEntry(final String contentRootPath, final String siteRootPath, final String templateType) {
        LOGGER.info(String.format("Start importing seo data for contentPath=%s , sitePath=%s , template=%s",
                contentRootPath, siteRootPath, templateType));

        final ResourceResolver resourceResolver = getResourceResolver();
        final Page contentRootPage = getContentRootPage(resourceResolver, contentRootPath);

        if(contentRootPage == null) {
            LOGGER.error("Failed to find content root page");
            return;
        }

        final String client = cloudServiceModel.getClient(resourceResolver, contentRootPage);
        if (StringUtils.isNotBlank(client)) {
            // TODO: new implementation needs more testing
            //updatePagesViaPathsEndpoint(client, resourceResolver, contentRootPage, templateType);
            updatePagesInAem(client, resourceResolver, contentRootPage, siteRootPath, templateType);
        } else {
            updatePagesInAem(client, resourceResolver, contentRootPage, siteRootPath, templateType);
        }
    }

    private void updatePagesViaPathsEndpoint(final String client,
                                             final ResourceResolver resourceResolver,
                                             final Page contentRootPage,
                                             final String templateType) {
        LOGGER.debug("Using updatePagesViaPathsEndpoint() method");

        final String apiUrl = String.format("https://paths.styla.com/v1/delta/%s", client);
        final String jsonResponse = executeGetDataRequest(apiUrl);

        if (jsonResponse == null) {
            LOGGER.warn("Path api seems not be reachable");
            return;
        }

        final Gson gson = new GsonBuilder().create();
        final Type listType = new TypeToken<List<PathEntry>>() {}.getType();
        final List<PathEntry> pathEntries = gson.fromJson(jsonResponse, listType);

        if (pathEntries == null || pathEntries.size() == 0) {
            LOGGER.warn("Path api response should not be empty");
            return;
        }

        for (final PathEntry pathEntry : pathEntries) {
            if (pathEntry.getType() == null || PathEntryType.MODULE.geValue().equals(pathEntry.getType().geValue())) {
                LOGGER.debug(String.format("Ignore modular content on path %s", pathEntry.getPath()));
                continue;
            }

            LOGGER.info(String.format("Found path: %s", pathEntry.getPath()));

            final String name = pathEntry.getName();
            final String fullPath = contentRootPage.getPath() + pathEntry.getPath();

            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            final Page currentPage = pageManager.getPage(fullPath);
            if (currentPage != null) {
                updatePage(client, resourceResolver, currentPage, contentRootPage, templateType);
                return;
            }

            try {
                final Page newPage = pageManager.create(fullPath, name, templateType, name);
                updatePage(client, resourceResolver, newPage, contentRootPage, templateType);
            } catch (WCMException e) {
                LOGGER.error(String.format("Failed to create page on path %s", fullPath), e);
            }
        }
    }

    private void updatePagesInAem(final String client,
                                  final ResourceResolver resourceResolver,
                                  final Page rootPage,
                                  final String siteRootPath,
                                  final String templateType) {
        final List<Page> pages = Lists.newArrayList();
        PageUtils.recursivelySearchForPage(rootPage.listChildren(), pages, templateType);
        LOGGER.info(String.format("Found pages (%d)", pages.size()));

        for (final Page childPage : pages) {
            updatePage(client, resourceResolver, childPage, rootPage, siteRootPath);
        }

        // Todo fix
        LOGGER.info(String.format("Finish importing seo data for contentPath=%s , sitePath=%s , template=%s",
                rootPage.getPath(), siteRootPath, templateType));
    }

    private void updatePage(final String client,
                            final ResourceResolver resourceResolver,
                            final Page currentPage,
                            final Page rootPage,
                            final String siteRootPath) {
        LOGGER.info(String.format("Processing page: %s on %s", currentPage.getName(), currentPage.getPath()));

        if (!isSeoImportEnabled(currentPage)) {
            LOGGER.warn("Skip page due disableSeoImport flag");
            return;
        }

        final String path = getPagePathname(client, resourceResolver, rootPage, currentPage, siteRootPath);
        final String seoApiUrl = buildSeoApiUrl(resourceResolver, rootPage, currentPage, path);
        if (seoApiUrl != null) {
            final Seo seo = fetchSeoData(seoApiUrl);
            if (seo != null) {
                applySeoData(seo, currentPage, resourceResolver, path);
            } else {
                LOGGER.warn(String.format("Fetched seo is empty for page %s - no seo data is applied", currentPage.getName()));
            }
        }

        LOGGER.info(String.format("Finished page: %s on %s", currentPage.getName(), currentPage.getPath()));
    }

    private String getPagePathname(final String client,
                                   final ResourceResolver resourceResolver,
                                   final Page rootPage,
                                   final Page currentPage,
                                   final String siteRootPath) {
        final boolean isLegacyClient = StringUtils.isBlank(client);

        if (isLegacyClient) {
            return StringUtils.removeStart(currentPage.getPath(), rootPage.getPath());
        }

        final String pagePath = resourceResolver.map(String.format("%s.html", currentPage.getPath()));
        return StringUtils.removeStart(pagePath, siteRootPath);
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

    private String buildSeoApiUrl(final ResourceResolver resourceResolver,
                                  final Page rootPage,
                                  final Page currentPage,
                                  final String path) {
        if (path == null) {
            return null;
        }

        final String seoApiUrl = cloudServiceModel.getSeoApiUrl(resourceResolver, rootPage);

        if (StringUtils.isBlank(seoApiUrl)) {
            return null;
        }

        return seoApiUrl
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

    private void applySeoData(final Seo seo,
                              final Page page,
                              final ResourceResolver resourceResolver,
                              final String path) {
        if (seo == null) {
            LOGGER.warn(String.format("Couldn't find seo for %s on %s", page.getName(), page.getPath()));
            return;
        }

        LOGGER.info(String.format("Start applying seo to page: %s on %s", page.getName(), page.getPath()));

        final Resource resource = page.getContentResource();
        final String client = cloudServiceModel.getClient(resourceResolver, page);
        final String seoBody = buildStylaBody(client, path, seo);
        final boolean wasModified = SeoUtils.writeSeoToResource(resource, seo, path, seoBody);

        if (!autoActivate) {
            LOGGER.warn("Autoactive is false");
            return;
        }

        if (!wasModified) {
            LOGGER.debug("Page was not modified: {}", resource.getPath());
            return;
        }

        try {
            resourceResolver.refresh();
            LOGGER.debug("Page was modified, activating:  {} ", resource.getPath());
            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, resource.getPath());
            resourceResolver.commit();
        } catch (NullPointerException | ReplicationException | PersistenceException e) {
            LOGGER.warn(String.format("Could not auto activate %s", resource.getPath()), e);
        }

    }

    public String buildStylaBody(final String client, final String path, final Seo seo) {
        final boolean isLegacyClient = StringUtils.isBlank(client);
        final String containerAttributes = isLegacyClient ? "id=\"stylaMagazine\"" : new StringBuilder()
                    .append(String.format("data-styla-client=\"%s\"", client))
                    .append(StringUtils.isNotBlank(path) ? String.format(" data-styla-content=\"%s\"", path) : "")
                    .toString();

        LOGGER.debug("container attributes " + containerAttributes);
        final String stylaBody = new StringBuilder()
                .append("<div ")
                .append(containerAttributes)
                .append(">")
                .append(seo.getHtml().getBody())
                .append("</div>")
                .toString();

        LOGGER.debug(String.format("Styla Body = %s", stylaBody));
        return stylaBody;
    }

    private String executeGetDataRequest(String apiUrl) {
        LOGGER.info(String.format("Requesting api url: %s", apiUrl));
        String response = null;
        HttpClient httpClient = new HttpClient();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setSoTimeout(10000);
        params.setConnectionTimeout(10000);
        httpClient.getHttpConnectionManager().setParams(params);

        HttpMethod httpGet = new GetMethod(apiUrl);
        HttpMethodParams httpMethodParams = new HttpMethodParams();
        httpMethodParams.setSoTimeout(10000);
        httpMethodParams.setParameter("accept", "application/json; charset=UTF-8");
        httpMethodParams.setContentCharset("UTF-8");
        httpGet.setParams(httpMethodParams);

        // TODO: for non exiting pages the status code is 200 but the json contains a status field which might be 404 ...
        try {
            int statusCode = httpClient.executeMethod(httpGet);
            if (statusCode == HTTP_OK) {
                response = httpGet.getResponseBodyAsString();
            } else if (statusCode == HTTP_NOT_FOUND) {
                LOGGER.warn("HTTP request to " + apiUrl + " returned status code 404 and is not handled as a failure. No data?");
            } else {
                LOGGER.warn("HTTP request to " + apiUrl + " returned status code " + statusCode + ". Aborting.");
            }
        } catch (IOException e) {
            LOGGER.error("Error during HTTP request to " + apiUrl + ": " + e.getMessage());
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
