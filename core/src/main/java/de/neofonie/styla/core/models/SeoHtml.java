package de.neofonie.styla.core.models;

/**
 * The SeoHtml class is a representation of the SEO html head and body content of the styla seo
 * api response.
 *
 * @see Seo
 *
 * @author Sebastian Sachtleben
 */
public class SeoHtml {

    private String head;
    private String body;

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "SeoHtml{" +
                "head='" + head + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
