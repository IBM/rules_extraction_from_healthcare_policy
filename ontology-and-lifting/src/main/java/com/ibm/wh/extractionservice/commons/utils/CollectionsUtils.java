package com.ibm.wh.extractionservice.commons.utils;

import static java.util.Arrays.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionsUtils {

    private CollectionsUtils() {
        // utility class
    }

    public static <T> T any(Collection<T> items) {
        return items.iterator().next();
    }

    public static <T> Set<T> unionAll(Set<T>... items) {
        return Stream.of(items).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public static <T> T first(List<T> items) {
        if (items.isEmpty()) throw new IllegalArgumentException("Can't get an item from an empty collection!");
        return items.get(0);
    }

    public static <T> Optional<T> optionalAny(Collection<T> items) {
        if (items.isEmpty()) return Optional.empty();
        return Optional.of(items.iterator().next());
    }

    public static <T> T sortAndGetFirst(Collection<T> elements, Comparator<? super T> comparator) {
        List<T> elementsList = new ArrayList<>(elements);
        elementsList.sort(comparator);
        return first(elementsList);
    }

}
