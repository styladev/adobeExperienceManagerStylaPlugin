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

    private static final String TAG_LINK = "link";
    private static final String TAG_META = "meta";

    private static final String ATTRIBUTE_REL = "rel";
    private static final String ATTRIBUTE_HREF = "href";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_CONTENT = "content";
    private static final String ATTRIBUTE_PROPERTY = "property";

    private static final Logger LOGGER = LoggerFactory.getLogger(SeoUtils.class);

    public static void writeSeoToResource(final Seo seo, final Resource resource) {
        if (resource == null || seo == null) {
            return;
        }

        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);

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
            if (TAG_LINK.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_REL) &&
                    "canonical".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_REL).getAsString())) {
                properties.put("stylaCanonical", tag.getAttributes().get(ATTRIBUTE_HREF).getAsString());
            }

            // robots
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_NAME) &&
                    "robots".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_NAME).getAsString())) {
                properties.put("stylaRobots", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // og:title
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "og:title".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaOgTitle", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // og:type
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "og:type".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaOgType", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // og:url
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "og:url".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaOgUrl", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // og:image
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "og:image".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaOgImage", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // og:image:width
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "og:image:width".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaOgImageWidth", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // og:image:height
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "og:image:height".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaOgImageHeight", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // twitter:title
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "twitter:title".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaTwitterTitle", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // twitter:description
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "twitter:description".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaTwitterDescription", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
            }

            // twitter:image
            if (TAG_META.equalsIgnoreCase(tag.getTag()) && tag.getAttributes() != null &&
                    tag.getAttributes().has(ATTRIBUTE_PROPERTY) &&
                    "twitter:image".equalsIgnoreCase(tag.getAttributes().get(ATTRIBUTE_PROPERTY).getAsString())) {
                properties.put("stylaTwitterImage", tag.getAttributes().get(ATTRIBUTE_CONTENT).getAsString());
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
                properties.put("stylaBody", body);
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            LOGGER.error("Could not create seo body node for " + contentResource.getPath() + "/body", e);
        }
    }
}
