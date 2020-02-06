package de.neofonie.styla.core.models;

public class CrawlerConfig {

    private String templateType;
    private String contentRootPath;
    private String siteRootPath;

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getContentRootPath() {
        return contentRootPath;
    }

    public void setContentRootPath(String contentRootPath) {
        this.contentRootPath = contentRootPath;
    }

    public String getSiteRootPath() {
        return siteRootPath;
    }

    public void setSiteRootPath(String siteRootPath) {
        this.siteRootPath = siteRootPath;
    }
}
