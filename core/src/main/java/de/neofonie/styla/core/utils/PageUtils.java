package de.neofonie.styla.core.utils;

import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Collection of Resource related methods for convenience.
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static void recursivelySearchForPage(Iterator<Page> children, List<Page> pages, String templateType) {
        if (children != null) {
            while (children.hasNext()) {
                Page nextChild = children.next();
                ValueMap properties = nextChild.getProperties();
                if (properties != null) {
                    String currentTemplateType = properties.get("cq:template", "");

                    if (StringUtils.equals(templateType, currentTemplateType)) {
                        pages.add(nextChild);
                    }
                }
                Iterator<Page> it = nextChild.listChildren();
                recursivelySearchForPage(it, pages, templateType);
            }
        }
    }

}
