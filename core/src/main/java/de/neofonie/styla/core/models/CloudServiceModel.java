package de.neofonie.styla.core.models;

import com.day.cq.analytics.testandtarget.util.Constants;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


@Model(adaptables = {Resource.class, SlingHttpServletRequest.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CloudServiceModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudServiceModel.class);

    @Inject
    private ResourceResolver resourceResolver;

    @Inject
    private Page currentPage;

    public String getSeoApiUrl(final ResourceResolver resourceResolver, final Page contentRootPage) {
        return getCloudServiceProperty(resourceResolver, contentRootPage);
    }

    public String getSeoApiUrl() {
        return getCloudServiceProperty("seoApiUrl");
    }

    public String getScript() {
        return getCloudServiceProperty("script");
    }

    private String getCloudServiceConfiguration(Page contentRootPage) {
        if (contentRootPage == null) {
            LOGGER.warn("ContentRootPage should not be null");
            return null;
        }

        final ValueMap properties = contentRootPage.getProperties();
        if (properties == null) {
            LOGGER.warn("Properties for contentRootPage should not be null");
            return null;
        }

        return properties.get(Constants.PN_CQ_CLOUD_SERVICE_CONFIGS, "");
    }

    private String getCloudServiceProperty(String property) {
        final Resource contentResource = currentPage.getContentResource();
        if (contentResource == null) {
            LOGGER.warn("Content resource for page " + currentPage.getName() + " is null");
            return null;
        }

        final HierarchyNodeInheritanceValueMap inheritedValueMap = new HierarchyNodeInheritanceValueMap(contentResource);
        final String cloudServicePath = inheritedValueMap.getInherited("cq:cloudserviceconfigs", "");

        if (!StringUtils.isNotEmpty(cloudServicePath)) {
            LOGGER.warn("Cloud service path is empty");
            return null;
        }

        final Resource cloudServiceResource = resourceResolver.getResource(cloudServicePath + "/" + JcrConstants.JCR_CONTENT);
        if (cloudServiceResource == null) {
            LOGGER.warn("Cloud service resource is empty");
            return null;
        }

        final ValueMap properties = cloudServiceResource.getValueMap();
        if (properties == null) {
            LOGGER.warn("Cloud service resource properties are empty");
            return null;
        }

        return properties.get(property, "");
    }

    private String getCloudServiceProperty(ResourceResolver resourceResolver, Page rootPage) {
        final String cloudServiceConfiguration = getCloudServiceConfiguration(rootPage);
        if (StringUtils.isEmpty(cloudServiceConfiguration)) {
            LOGGER.warn("Cloud service configuration for page " + rootPage.getName() + " is empty");
            return null;
        }

        final Resource cloudServiceResource = resourceResolver.getResource(cloudServiceConfiguration + "/" + JcrConstants.JCR_CONTENT);
        if (cloudServiceResource == null) {
            LOGGER.warn("Cloud service resource for page " + rootPage.getName() + " is empty");
            return null;
        }

        final ValueMap properties = cloudServiceResource.getValueMap();
        if (properties != null) {
            LOGGER.warn("Cloud service resource properties for page " + rootPage.getName() + " are empty");
            return null;
        }

        return properties.get("seoApiUrl", "");
    }
}
