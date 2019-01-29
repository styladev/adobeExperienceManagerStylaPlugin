package de.neofonie.styla.core.utils;

import com.day.cq.wcm.api.Page;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaTagJsonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaTagJsonUtils.class);

    public static void processMetaTag(Page childPage, JsonElement tag) {
        JsonObject attributes = getTagAttributes(tag);

        MetaTag metaTag = new MetaTag();

        if (attributes != null) {
            metaTag.setProperty(getAttribute(attributes, "property"));
            metaTag.setName(getAttribute(attributes, "name"));
            metaTag.setContent(getAttribute(attributes, "content"));
        }

        if (metaTag.isValid()) {
            MetaTagJcrUtils.writeMetaTags(childPage.getContentResource(), metaTag);
        }

    }

    private static String getAttribute(JsonObject attributes, String attributeName) {
        String attribute = "";
        if (attributes.has(attributeName)) {
            JsonElement jsonElement = attributes.get(attributeName);

            if (jsonElement != null) {
                attribute = jsonElement.getAsString();
            }
        }

        return attribute;
    }

    private static JsonObject getTagAttributes(JsonElement tag) {
        JsonObject object = tag.getAsJsonObject();

        JsonObject attributes = null;
        if (object.has("attributes")) {
            JsonElement attributesElement = object.get("attributes");
            if (attributesElement != null) {
                attributes = attributesElement.getAsJsonObject();
            }
        }

        return attributes;
    }

    static class MetaTag {
        private String property;
        private String name;
        private String content;

        public void setProperty(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public boolean isValid() {
            return (StringUtils.isNotBlank(property) || StringUtils.isNotBlank(name)) && StringUtils.isNotBlank(content);
        }
    }
}
