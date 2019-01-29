package de.neofonie.styla.core.utils;

import com.google.common.collect.Iterators;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MetaTagJcrUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaTagJcrUtils.class);

    public static void writeMetaTags(Resource contentResource, MetaTagJsonUtils.MetaTag metaTag) {

        if (contentResource != null) {
            ResourceResolver resourceResolver = contentResource.getResourceResolver();
            ModifiableValueMap modifiableValueMap = contentResource.adaptTo(ModifiableValueMap.class);

            if (modifiableValueMap != null) {

                Map<String, Object> properties = new HashMap();
                checkForOpenGraphTags(metaTag, properties);

                if (properties.size() == 0) {
                    checkForOtherTags(contentResource, resourceResolver, metaTag, properties);
                }

                else if (properties.size() > 0) {
                    modifiableValueMap.putAll(properties);
                }
            }
            try {
                resourceResolver.commit();
            } catch (PersistenceException e) {
                LOGGER.error("Could not commit change for resource " + contentResource.getPath());
            }

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
