package de.neofonie.styla.core.models.impl;

import de.neofonie.styla.core.models.api.MetaTag;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;

/**
 * Model for representing nodes below ../jcr:content/metatags
 */
@Model(adaptables = Resource.class, adapters = {MetaTag.class})
public class MetaTagImpl implements MetaTag {

    @SlingObject
    private Resource currentResource;

    private String tagLabel;
    private String tagValue;
    private boolean isProperty;

    @PostConstruct
    public void init() {
        ValueMap properties = currentResource.getValueMap();

        if (properties != null) {
            this.tagLabel = properties.get("tagLabel", "");
            this.tagValue = properties.get("tagValue", "");
            this.isProperty = properties.get("isProperty", false);
        }
    }

    @Override
    public String getTagLabel() {
        return this.tagLabel;
    }

    @Override
    public String getTagValue() {
        return this.tagValue;
    }

    @Override
    public boolean isProperty() {
        return isProperty;
    }
}
