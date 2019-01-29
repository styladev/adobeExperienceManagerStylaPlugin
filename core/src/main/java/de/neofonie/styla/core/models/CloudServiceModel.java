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

import javax.inject.Inject;


@Model(adaptables = {Resource.class, SlingHttpServletRequest.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CloudServiceModel {

    @Inject
    private ResourceResolver resourceResolver;

    @Inject
    private Page currentPage;

    public String getSeoApiUrl(ResourceResolver resourceResolver, Page contentRootPage) {
        String seoApiUrl = null;
        seoApiUrl = getCloudServiceProperty(resourceResolver, contentRootPage);
        return seoApiUrl;
    }

    public String getSeoApiUrl() {
        return getCloudServiceProperty("seoApiUrl");
    }

    public String getScript() {
        return getCloudServiceProperty("script");
    }

    private String getCloudServiceConfiguration(Page contentRootPage) {
        String cloudServiceConfiguration = null;

        if (contentRootPage != null) {
            ValueMap properties = contentRootPage.getProperties();

            if (properties != null) {
                return properties.get(Constants.PN_CQ_CLOUD_SERVICE_CONFIGS, "");
            }
        }
        return cloudServiceConfiguration;
    }

    private String getCloudServiceProperty(String property) {
        String propertyValue = null;
        Resource contentResource = currentPage.getContentResource();

        if (contentResource != null) {
            HierarchyNodeInheritanceValueMap inheritedValueMap = new HierarchyNodeInheritanceValueMap(contentResource);
            String cloudServicePath = inheritedValueMap.getInherited("cq:cloudserviceconfigs", "");

            if (StringUtils.isNotEmpty(cloudServicePath)) {
                Resource cloudServiceResource = resourceResolver.getResource(cloudServicePath + "/" + JcrConstants.JCR_CONTENT);

                if (cloudServiceResource != null) {
                    ValueMap properties = cloudServiceResource.getValueMap();

                    if (properties != null) {
                        propertyValue = properties.get(property, "");
                    }
                }
            }
        }
        return propertyValue;
    }

    private String getCloudServiceProperty(ResourceResolver resourceResolver, Page contentRootPage) {
        String seoApiUrl = null;
        String property = "seoApiUrl";
        String cloudServiceConfiguration = getCloudServiceConfiguration(contentRootPage);

        if (StringUtils.isNotEmpty(cloudServiceConfiguration)) {
            Resource cloudServiceResource = resourceResolver.getResource(cloudServiceConfiguration + "/" + JcrConstants.JCR_CONTENT);

            if (cloudServiceResource != null) {
                ValueMap properties = cloudServiceResource.getValueMap();

                if (properties != null) {
                    seoApiUrl = properties.get(property, "");
                }
            }
        }
        return seoApiUrl;
    }
}
