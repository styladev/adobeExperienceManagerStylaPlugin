package de.neofonie.styla.core.handler.impl;

import de.neofonie.styla.core.handler.api.MetaTagHandler;
import de.neofonie.styla.core.models.api.MetaTag;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MetaTagHandler for handling all optional metatags of an empty page
 */
@Model(adaptables = {SlingHttpServletRequest.class, Resource.class}, adapters = {MetaTagHandler.class})
public class MetaTagHandlerImpl implements MetaTagHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetaTagHandlerImpl.class);

	/**
	 * Lists all nodes below the given metatag resource and adapts to an MetaTag object
	 * @param currentResource Resource metatags below jcr:content contains nodes for each metatag
	 * @return List with adapted MetaTag based on the nodes below jcr:content/metatags
	 */
	public List<MetaTag> getMetaTags(Resource currentResource) {
		final List<MetaTag> metaTagList = new ArrayList<>();

		if (currentResource != null) {
			final Iterator<Resource> resourceIterator = currentResource.listChildren();

			while (resourceIterator.hasNext()) {
				final Resource metaTagResource = resourceIterator.next();
				final MetaTag metaTag = metaTagResource.adaptTo(MetaTag.class);

				if (metaTag != null) {
					metaTagList.add(metaTag);
				}
				else {
					LOGGER.debug("No optional meta tags available for " + metaTagResource.getPath());
				}
			}
		}

		return metaTagList;
	}
}
