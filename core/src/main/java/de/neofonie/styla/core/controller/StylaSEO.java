package de.neofonie.styla.core.controller;

import com.day.cq.wcm.api.PageManager;
import de.neofonie.styla.core.handler.api.MetaTagHandler;
import de.neofonie.styla.core.models.api.MetaTag;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;

import javax.annotation.PostConstruct;
import java.util.List;

@Model(adaptables = {SlingHttpServletRequest.class})
public class StylaSEO {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylaSEO.class);


    @SlingObject
    private Resource currentResource;

    @Self
    MetaTagHandler metaTagHandler;

    List<MetaTag> metaTags;

    private String body;

    @PostConstruct
    private void activate() {
        if (currentResource == null) {
            return;
        }

        LOGGER.debug(String.format("Found currentResource: %s", currentResource.getPath()));

        final Resource resource = currentResource.getChild("body");
        if (resource != null) {
            this.body = resource.getValueMap().get("body", "");
            LOGGER.info(String.format("Found body: %s", StringUtils.substring(this.body, 0, 20)));
        }

        final Resource metatags = currentResource.getChild("metatags");
        if (metatags != null) {
            this.metaTags = metaTagHandler.getMetaTags(metatags);
        }
    }

    public String getBody() {
        return body;
    }

    public List<MetaTag> getMetaTags() {
        return metaTags;
    }

}
