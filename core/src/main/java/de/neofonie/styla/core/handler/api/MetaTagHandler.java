package de.neofonie.styla.core.handler.api;

import de.neofonie.styla.core.models.api.MetaTag;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface MetaTagHandler {

    List<MetaTag> getMetaTags(Resource metatags);

}
