package de.neofonie.styla.core.jobs;


import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.neofonie.styla.core.models.CloudServiceModel;
import de.neofonie.styla.core.utils.MetaTagJcrUtils;
import de.neofonie.styla.core.utils.MetaTagJsonUtils;
import de.neofonie.styla.core.utils.PageUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
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
import java.util.*;

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

    @Reference
    private Scheduler scheduler;

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
        String scheduler_expression() default "0 15 12 ? * *";

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

        this.startCronjob(config);

        LOGGER.info("configure: contentRootPath='{}''", this.contentRootPath);
    }

    private void startCronjob(final Config config) {
        try {
            ScheduleOptions scheduleOptions = scheduler.EXPR(config.scheduler_expression());
            scheduleOptions.canRunConcurrently(false);
            scheduleOptions.config(Collections.emptyMap());
            scheduleOptions.name("Styla Seo Import Service");
            this.scheduler.schedule(this, scheduleOptions);
            LOGGER.info("Added seo import service scheduler job");
        } catch (Exception e) {
            LOGGER.error("Failed to add seo import service scheduler job", e);
        }
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
        ResourceResolver resourceResolver = getResourceResolver();
        Page contentRootPage = getContentRootPage(resourceResolver);

        if (contentRootPage != null) {
            List<Page> pages = new ArrayList();
            PageUtils.recursivelySearchForPage(contentRootPage.listChildren(), pages, templateType);
            LOGGER.info("Found pages (" + pages.size() + ")");

            for (Page childPage : pages) {
                LOGGER.info("Processing page: " + childPage.getName() + " - " + childPage.getPath());

                ValueMap properties = childPage.getProperties();

                if (properties != null && !properties.containsKey("allowSeoImport")) {
                    LOGGER.warn("Skip page due not allowedSeoImport");
                    continue;
                }
                String seoApiUrl = cloudServiceModel.getSeoApiUrl(resourceResolver, contentRootPage);
                seoApiUrl = seoApiUrl.replace("$URL", childPage.getName());
                seoApiUrl = seoApiUrl.replace("$LANG", childPage.getLanguage().toString());
                JsonArray metaData = getMetaData(seoApiUrl);

                if (metaData != null) {
                    Iterator<JsonElement> iterator = metaData.iterator();
                    while (iterator.hasNext()) {
                        JsonElement tag = iterator.next();

                        Resource contentResource = childPage.getContentResource();
                        MetaTagJsonUtils.MetaTag metaTag = MetaTagJsonUtils.getMetaTag(childPage, tag);
                        if (contentResource == null) {
                            LOGGER.warn("Content resource is empty for page " + childPage.getName());
                            continue;
                        }

                        MetaTagJcrUtils.writeMetaTags(contentResource, metaTag);

                        if (!autoActivate) {
                            LOGGER.warn("Autoactive is false");
                            continue;
                        }

                        try {
                            resourceResolver.refresh();
                            replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, contentResource.getPath());
                            resourceResolver.commit();
                        } catch (NullPointerException|ReplicationException|PersistenceException e) {
                            LOGGER.warn("Could not auto activate " + contentResource.getPath(), e);
                        }
                    }
                }

            }
        }
    }

    private JsonArray getMetaData(String seoApiUrl) {
        String jsonResponse = executeGetDataRequest(seoApiUrl);

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonTree = jsonParser.parse(jsonResponse);

        if (jsonTree == null) {
            LOGGER.warn("Seo api response should not be empty");
            return null;
        }

        JsonObject json = ((JsonObject) jsonTree);

        String status = json.get("status").getAsInt() + "";
        if (!status.startsWith("2")) {
            LOGGER.warn("Seo api is returning status: " + status);
        }

        return json.get("tags").getAsJsonArray();
    }

    private String executeGetDataRequest(String seoApiUrl) {
        LOGGER.info("Requesting seo api url: " + seoApiUrl);
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



    private Page getContentRootPage(ResourceResolver resourceResolver) {
        Resource contentRootResource = getContentRootResource(resourceResolver);
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
    private Resource getContentRootResource(ResourceResolver resourceResolver) {
        if (StringUtils.isNotEmpty(contentRootPath) && resourceResolver != null) {
            return resourceResolver.getResource(contentRootPath);
        }
        return null;
    }
}
