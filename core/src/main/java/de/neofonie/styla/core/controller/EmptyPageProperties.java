package de.neofonie.styla.core.controller;

import de.neofonie.styla.core.handler.api.MetaTagHandler;
import de.neofonie.styla.core.models.api.MetaTag;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Controller for delegating page property concerns of empty page to the respective handler
 */
@Model(adaptables = {SlingHttpServletRequest.class})
public class EmptyPageProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmptyPageProperties.class);

	@SlingObject
	private Resource currentResource;

	@Self
	MetaTagHandler metaTagHandler;

	List<MetaTag> metaTags;

	@PostConstruct
	private void activate() {
		Resource metatags = currentResource.getChild("metatags");

		if (metatags != null) {
			this.metaTags = metaTagHandler.getMetaTags(metatags);
		}
		else {
			LOGGER.debug("No optional meta tags available for " + currentResource.getPath());
		}
	}

	public List<MetaTag> getMetaTags() {
		return metaTags;
	}
}
