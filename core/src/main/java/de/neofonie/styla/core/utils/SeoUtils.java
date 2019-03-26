package de.neofonie.styla.core.utils;

import com.google.common.collect.Iterators;
import de.neofonie.styla.core.models.Seo;
import de.neofonie.styla.core.models.SeoHeadTag;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SeoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeoUtils.class);

    public static void writeSeoToResource(final Seo seo, final Resource resource) {
        if (resource == null || seo == null) {
            return;
        }

        ResourceResolver resourceResolver = resource.getResourceResolver();
        ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);

        if (modifiableValueMap == null) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        removeMetaTags(resource, resourceResolver);
        writeMetaTags(seo, resource, properties);
        updateSeoBodyResource(resource, resourceResolver, seo.getHtml().getBody());

        try {
            resourceResolver.commit();
        } catch (PersistenceException e) {
            LOGGER.error("Could not commit change for resource " + resource.getPath());
        }
    }

    private static void writeMetaTags(final Seo seo, final Resource resource, final Map<String, Object> properties) {
        if (seo == null || seo.getTags() == null || seo.getTags().size() == 0) {
            LOGGER.warn("Couldn't find meta tags");
            return;
        }

        final Iterator<SeoHeadTag> iterator = seo.getTags().iterator();
        while (iterator.hasNext()) {
            final SeoHeadTag tag = iterator.next();
            LOGGER.info(String.format("Applying seo tag %s with %s", tag.getTag(), tag.toString()));

            final MetaTagJsonUtils.MetaTag metaTag = MetaTagJsonUtils.getMetaTag(tag);
            if (metaTag != null) {
                SeoUtils.writeMetaTag(resource, properties, metaTag);
            }
        }
    }

    public static void writeMetaTag(final Resource contentResource, final Map<String, Object> properties, final MetaTagJsonUtils.MetaTag metaTag) {
        if (contentResource == null) {
            LOGGER.error("Content resource is empty");
            return;
        }

        if (metaTag == null) {
            LOGGER.warn("Invalid meta tag found");
            return;
        }

        final ResourceResolver resourceResolver = contentResource.getResourceResolver();
        final ModifiableValueMap modifiableValueMap = contentResource.adaptTo(ModifiableValueMap.class);

        if (modifiableValueMap != null) {
            final Map<String, Object> currentProperties = new HashMap<>();

            checkForOpenGraphTags(metaTag, currentProperties);

            if (currentProperties.size() == 0) {
                checkForOtherTags(contentResource, resourceResolver, metaTag, currentProperties);
            } else {
                modifiableValueMap.putAll(currentProperties);
            }
        }

    }

    private static void updateSeoBodyResource(final Resource contentResource, final ResourceResolver resourceResolver, final String body) {
        try {
            final Resource bodyResource = ResourceUtil.getOrCreateResource(resourceResolver, contentResource.getPath() + "/body", Collections.emptyMap(), null, true);
            final ModifiableValueMap properties = bodyResource.adaptTo(ModifiableValueMap.class);
            if (properties != null) {
                properties.put("body", body);
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            LOGGER.error("Could not create seo body node for " + contentResource.getPath() + "/body", e);
        }
    }

    private static void removeMetaTags(Resource contentResource, ResourceResolver resourceResolver) {
        try {
            Resource metaTagsResource = ResourceUtil.getOrCreateResource(resourceResolver, contentResource.getPath() + "/metatags", Collections.emptyMap(), null, true);
            resourceResolver.delete(metaTagsResource);
            resourceResolver.commit();
        } catch (PersistenceException e) {
            LOGGER.error("Could not delete metatags node for " + contentResource.getPath() + "/metatags", e);
        }
    }

    private static void checkForOtherTags(Resource contentResource, ResourceResolver resourceResolver, MetaTagJsonUtils.MetaTag metaTag, Map<String, Object> properties) {
        try {
            Resource metaTagsResource = ResourceUtil.getOrCreateResource(resourceResolver, contentResource.getPath() + "/metatags", Collections.emptyMap(), null, true);

            Iterator<Resource> resourceIterator = metaTagsResource.listChildren();
            int numberOfMetaTags = Iterators.size(resourceIterator);

            boolean isProperty = StringUtils.isNotEmpty(metaTag.getProperty());
            properties.put("tagLabel", isProperty ? metaTag.getProperty() : metaTag.getName());
            properties.put("tagValue", metaTag.getContent());
            properties.put("isProperty", isProperty);

            ResourceUtil.getOrCreateResource(resourceResolver, (metaTagsResource.getPath() + "/item" + String.valueOf(numberOfMetaTags)), properties, null, true);
        } catch (PersistenceException e) {
            LOGGER.error("Could not create metatags node for " + contentResource.getPath() + "/metatags", e);
        }
    }

    private static void checkForOpenGraphTags(MetaTagJsonUtils.MetaTag metaTag, Map<String, Object> properties) {
        if (StringUtils.equals(metaTag.getProperty(), "og:type") && metaTag.getContent() != null) {
            properties.put("ogType", metaTag.getContent());
        }
        if (StringUtils.equals(metaTag.getProperty(), "og:title") && metaTag.getContent() != null) {
            properties.put("ogTitle", metaTag.getContent());
        }
        if (StringUtils.equals(metaTag.getProperty(), "og:url") && metaTag.getContent() != null) {
            properties.put("ogUrl", metaTag.getContent());
        }
        if (StringUtils.equals(metaTag.getProperty(), "og:image") && metaTag.getContent() != null) {
            properties.put("ogImage", metaTag.getContent());
        }
    }
}
