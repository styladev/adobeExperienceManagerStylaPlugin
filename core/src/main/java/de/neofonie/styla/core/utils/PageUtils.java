package de.neofonie.styla.core.utils;

import com.day.cq.wcm.api.Page;
import de.neofonie.styla.core.models.CloudServiceModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Collection of Resource related methods for convenience.
 */
public final class PageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageUtils.class);

    public static void recursivelySearchForPage(final Iterator<Page> children, final List<Page> pages, final String templateType) {
        if (children == null) {
            LOGGER.warn("children parameter is empty");
            return;
        }

        while (children.hasNext()) {
            final Page nextChild = children.next();
            final ValueMap properties = nextChild.getProperties();
            if (properties != null) {
                final String currentTemplateType = properties.get("cq:template", "");

                if (StringUtils.equals(templateType, currentTemplateType)) {
                    pages.add(nextChild);
                }
            }
            final Iterator<Page> it = nextChild.listChildren();
            recursivelySearchForPage(it, pages, templateType);
        }
    }

}
