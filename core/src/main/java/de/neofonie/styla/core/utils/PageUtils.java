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

//    /**
//     * Returns a {@link Stream} of the <em>direct</em> children of this resource. Does not include the resource itself.
//     * This method is a convenience that delegates {@link Resource#listChildren()} to {@link #asStream(Iterator)}.
//     */
//    public static Stream<Resource> streamChildren(Resource resource) {
//        return asStream(resource.listChildren());
//    }
//
//    /**
//     * <p>Return a {@link Stream} containing the given resource and <em>all</em> of it's children.</p>
//     * <p>Inspired by <a href="http://squirrel.pl/blog/2015/03/04/walking-recursive-data-structures-using-java-8-streams/">Konrad Garus</a>.</p>
//     */
//    public static Stream<Resource> streamAllChildren(Resource resource) {
//        return Stream.concat(
//                Stream.of(resource),
//                streamChildren(resource).flatMap(ResourceUtils::streamAllChildren));
//    }
//
//    /**
//     * <p>Convert an {@link Iterator} to a {@link Stream}</p>
//     * <p>Taken from <a href="http://stackoverflow.com/a/24511534/197574">stackoverflow</a>.</p>
//     */
//    public static <T> Stream<T> asStream(Iterator<T> iterator) {
//        // Iterable<T> is a FunctionalInterface which has only one abstract method iterator().
//        // So () -> iterator is a lambda expression instantiating an Iterable instance
//        // as an anonymous implementation.
//        Iterable<T> iterable = () -> iterator;
//        return StreamSupport.stream(iterable.spliterator(), false);
//    }
//
//    public static int countChildren(Resource resource) {
//        AtomicInteger rtn = new AtomicInteger(0);
//        if (resource.hasChildren()) {
//            resource.listChildren().forEachRemaining(res -> rtn.incrementAndGet());
//        }
//
//        return rtn.intValue();
//    }
//
//    /**
//     * get children of the given resource that have the given type.
//     *
//     * @param parent the resource for which to find the child. Must not be null.
//     * @param type the type as a string the child must have
//     * @return a list of resources with the given type. Can be empty but not null.
//     */
//    public static final List<Resource> getChildrenResourceWithType(Resource parent, String type) {
//        List<Resource> rtn = new ArrayList<>();
//        Iterator<Resource> resourceIterator = parent.listChildren();
//        while (resourceIterator.hasNext()) {
//            Resource next = resourceIterator.next();
//            if (next.isResourceType(type)) {
//                rtn.add(next);
//           }
//        }
//        return rtn;
//    }

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

//    /**
//     * @param resource might be null
//     * @return page denoted by or containing resource or null
//     */
//    public static Page getPage(Resource resource) {
//        if (resource == null) {
//            return null;
//        }
//        Page page = resource.adaptTo(Page.class);
//        if (page != null) {
//            return page;
//        }
//        Resource firstExistingPageResource = getFirstExistingPageResource(resource);
//        if (firstExistingPageResource == null) {
//            return null;
//        }
//        return firstExistingPageResource.adaptTo(Page.class);
//    }
//
//    /**
//     * Determine Resource from request.
//     * Used when {@code request#getResource} might contain non-existing resource because of template data.
//     *
//     * @param request may be null
//     * @return referenced resource or null
//     */
//    public static Resource getResource(SlingHttpServletRequest request) {
//        if (request != null) {
//            // fix for closed session: pass over a fresh session from resource
//            Resource requestResource = request.getResource();
//            if (requestResource != null) {
//                if (requestResource.adaptTo(Page.class) != null) {
//                    // request pointed at cq:Page resource
//                    return requestResource;
//
//                } else {
//                    // from here on I assume the requested path contains a layout reference
//                    return getFirstExistingPageResource(requestResource);
//                }
//            }
//        }
//        // can't work with nothing
//        return null;
//    }
//
//
//
//    /**
//     * Convenience delegate
//     *
//     * @param requestResource requested resource from request.getResource()
//     */
//    private static Resource getFirstExistingPageResource(Resource requestResource) {
//        Resource rtn = null;
//        ResourceResolver resourceResolver = requestResource.getResourceResolver();
//        Resource resolvedResource = resourceResolver.resolve(requestResource.getPath());
//        PageUrlMapper pageUrlMapper = resourceResolver.adaptTo(PageUrlMapper.class);
//        if (pageUrlMapper != null) {
//            rtn = pageUrlMapper.getFirstExistingPage(resolvedResource, resourceResolver, IfolorConstants.PATH_COMPONENT_PAGE_PRODUCT_DETAILS);
//        }
//        return rtn;
//    }


}
