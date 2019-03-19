package de.neofonie.styla.core.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class MetaTagJsonUtils {

    public static MetaTag getMetaTag(final JsonElement tag) {
        final JsonObject attributes = getTagAttributes(tag);

        final MetaTag metaTag = new MetaTag();

        if (attributes != null) {
            metaTag.setProperty(getAttribute(attributes, "property"));
            metaTag.setName(getAttribute(attributes, "name"));
            metaTag.setContent(getAttribute(attributes, "content"));
        }

        if (metaTag.isValid()) {
            return metaTag;
        }

        return null;
    }

    private static String getAttribute(JsonObject attributes, String attributeName) {
        if (attributes.has(attributeName)) {
            final JsonElement jsonElement = attributes.get(attributeName);

            if (jsonElement != null) {
                return jsonElement.getAsString();
            }
        }

        return "";
    }

    private static JsonObject getTagAttributes(JsonElement tag) {
        final JsonObject object = tag.getAsJsonObject();

        if (object.has("attributes")) {
            JsonElement attributesElement = object.get("attributes");
            if (attributesElement != null) {
                return attributesElement.getAsJsonObject();
            }
        }

        return null;
    }

    public static class MetaTag {
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
