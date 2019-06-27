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

    public static void recursivelySearchForPage(Iterator<Page> children, List<Page> pages, String[] paths) {
        if (children != null) {
            while (children.hasNext()) {
                Page nextChild = children.next();
                ValueMap properties = nextChild.getProperties();
                if (properties != null) {
                    String path = nextChild.getPath();

                    if (pathMatchesAny(path, paths)) {
                        pages.add(nextChild);
                    }
                }
                Iterator<Page> it = nextChild.listChildren();
                recursivelySearchForPage(it, pages, paths);
            }
        }
    }

    public static Resource recursivelySearchForResource(String productSku, String productGroupSku, Iterator<Resource> resourceIterator) {
        if (resourceIterator != null) {
            while (resourceIterator.hasNext()) {
                Resource resourceNext = resourceIterator.next();
                ValueMap valueMap = resourceNext.getValueMap();
                // Check first if the full product sku is equal then check if it equals as product group sku
                String productSkuProp = valueMap != null && valueMap.containsKey("productSku") ? valueMap.get("productSku", String.class) : null;
                if (StringUtils.equals(productSkuProp, productSku) || StringUtils.equals(productSkuProp, productGroupSku)) {
                    return resourceNext;
                }
                Iterator<Resource> it = resourceNext.listChildren();
                Resource resource = recursivelySearchForResource(productSku, productGroupSku, it);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return null;
    }

    private static boolean pathMatchesAny(String in, String[] criteria){
        for(String criterion: criteria)
            if (in.matches(criterion))
                return true;
        return false;
    }


}
