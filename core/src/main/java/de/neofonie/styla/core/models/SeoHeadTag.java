package de.neofonie.styla.core.models;

import com.google.gson.JsonObject;

/**
 * The SeoHeadTag class is a representation of all head tags list within the styla seo api
 * response.
 *
 * @see Seo
 *
 * @author Sebastian Sachtleben
 */
public class SeoHeadTag {

    private String tag;
    private JsonObject attributes;
    private String content;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public JsonObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonObject attributes) {
        this.attributes = attributes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "SeoHeadTag{" +
                "tag='" + tag + '\'' +
                ", attributes=" + attributes +
                ", content='" + content + '\'' +
                '}';
    }
}
