package de.neofonie.styla.core.models;

import java.io.Serializable;
import java.util.List;

/**
 * The SEO class is a representation of the response from the styla seo api.
 * For example: http://seoapi.styla.com/clients/ci-oxid-nle?url=/
 *
 * @author Sebastian Sachtleben
 */
public class Seo implements Serializable {

    private String code;
    private boolean error;
    private int expire;
    private int responseCode;
    private int status;
    private List<SeoHeadTag> tags;
    private SeoHtml html;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<SeoHeadTag> getTags() {
        return tags;
    }

    public void setTags(List<SeoHeadTag> tags) {
        this.tags = tags;
    }

    public SeoHtml getHtml() {
        return html;
    }

    public void setHtml(SeoHtml html) {
        this.html = html;
    }

    @Override
    public String toString() {
        return "Seo{" +
                "code='" + code + '\'' +
                ", error=" + error +
                ", expire=" + expire +
                ", responseCode=" + responseCode +
                ", status=" + status +
                ", tags=" + tags +
                ", html=" + html +
                '}';
    }
}
