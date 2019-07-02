package de.neofonie.styla.core.utils;

import de.neofonie.styla.core.models.Seo;
import de.neofonie.styla.core.models.SeoHeadTag;
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
        writeMetaTagsToProperties(seo, properties);
        updateSeoBodyResource(resource, resourceResolver, seo.getHtml().getBody());
        modifiableValueMap.putAll(properties);

        try {
            resourceResolver.commit();
        } catch (PersistenceException e) {
            LOGGER.error("Could not commit change for resource " + resource.getPath());
        }
    }

    private static void writeMetaTagsToProperties(final Seo seo, final Map<String, Object> properties) {
        if (seo == null || seo.getTags() == null || seo.getTags().size() == 0) {
            LOGGER.warn("Couldn't find meta tags");
            return;
        }

        final Iterator<SeoHeadTag> iterator = seo.getTags().iterator();
        while (iterator.hasNext()) {
            final SeoHeadTag tag = iterator.next();
            LOGGER.info(String.format("Applying seo tag %s with %s", tag.getTag(), tag.toString()));
            writeMetaTag(tag, properties);
        }
    }

    private static void writeMetaTag(final SeoHeadTag tag, final Map<String, Object> properties) {
        if (tag == null) {
            return;
        }

        try {
            // canonical
            if ("link".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("rel") &&
                    "canonical".equalsIgnoreCase(tag.getAttributes().get("rel").toString())) {
                properties.put("stylaCanonical", tag.getAttributes().get("href").toString());
            }

            // robots
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("name") &&
                    "robots".equalsIgnoreCase(tag.getAttributes().get("name").toString())) {
                properties.put("stylaRobots", tag.getAttributes().get("content").toString());
            }

            // og:title
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "og:title".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaOgTitle", tag.getAttributes().get("content").toString());
            }

            // og:type
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "og:type".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaOgType", tag.getAttributes().get("content").toString());
            }

            // og:url
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "og:url".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaOgUrl", tag.getAttributes().get("content").toString());
            }

            // og:image
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "og:image".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaOgImage", tag.getAttributes().get("content").toString());
            }

            // og:image:width
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "og:image:width".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaOgImageWidth", tag.getAttributes().get("content").toString());
            }

            // og:image:height
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "og:image:height".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaOgImageHeight", tag.getAttributes().get("content").toString());
            }

            // twitter:title
            if ("meta".equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has("property") &&
                    "twitter:title".equalsIgnoreCase(tag.getAttributes().get("property").toString())) {
                properties.put("stylaTwitterTitle", tag.getAttributes().get("content").toString());
            }
        } catch(Exception e) {
            LOGGER.error("Failed to set meta tag", e);
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
}
