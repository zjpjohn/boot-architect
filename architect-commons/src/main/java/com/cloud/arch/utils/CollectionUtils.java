package com.cloud.arch.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class CollectionUtils {

    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private CollectionUtils() {
        throw new UnsupportedOperationException("not support invoke.");
    }

    /**
     * 判断结合为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断结合不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * 判断map是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断map不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * 集合转换为List
     */
    public static <T, V> List<V> toList(Collection<T> values, Function<T, V> converter) {
        Preconditions.checkNotNull(converter);
        if (values == null || values.isEmpty()) {
            return Lists.newArrayList();
        }
        return values.stream().map(converter).collect(Collectors.toList());
    }

    /**
     * 集合转换为元素不重复的List
     */
    public static <T, V> List<V> distinctList(Collection<T> values, Function<T, V> converter) {
        Preconditions.checkNotNull(converter);
        if (values == null || values.isEmpty()) {
            return Lists.newArrayList();
        }
        return values.stream().map(converter).distinct().collect(Collectors.toList());
    }

    /**
     * 集合转换为Set
     */
    public static <T, V> Set<V> toSet(Collection<T> values, Function<T, V> converter) {
        Preconditions.checkNotNull(converter);
        if (values == null || values.isEmpty()) {
            return Sets.newHashSet();
        }
        return values.stream().map(converter).collect(Collectors.toSet());
    }

    /**
     * 集合转换为Map，重复key进行覆盖
     */
    public static <K, V> Map<K, V> toMap(Collection<V> values, Function<? super V, K> keyFunction) {
        Preconditions.checkNotNull(keyFunction);
        if (values == null || values.isEmpty()) {
            return Maps.newHashMap();
        }
        return values.stream().collect(Collectors.toMap(keyFunction, Function.identity(), (oldVal, newVal) -> newVal));
    }

    /**
     * 集合转换为Map，重复key进行覆盖
     */
    public static <K, V, T> Map<K, T> toMap(Collection<V> values,
                                            Function<? super V, K> keyFunction,
                                            Function<? super V, T> valueFunction) {
        Preconditions.checkNotNull(keyFunction);
        if (values == null || values.isEmpty()) {
            return Maps.newHashMap();
        }
        return values.stream().collect(Collectors.toMap(keyFunction, valueFunction, (oldVal, newVal) -> newVal));
    }

    /**
     * 集合扁平转换List
     */
    public static <V, R> List<R> flatList(Collection<V> values,
                                          Function<? super V, Collection<? extends R>> converter) {
        Preconditions.checkNotNull(converter);
        if (values == null || values.isEmpty()) {
            return Lists.newArrayList();
        }
        return values.stream().flatMap(v -> converter.apply(v).stream()).collect(Collectors.toList());
    }

    /**
     * 集合扁平转换Set
     */
    public static <V, R> Set<R> flatSet(Collection<V> values, Function<? super V, Collection<? extends R>> converter) {
        Preconditions.checkNotNull(converter);
        if (values == null || values.isEmpty()) {
            return Sets.newHashSet();
        }
        return values.stream().flatMap(v -> converter.apply(v).stream()).collect(Collectors.toSet());
    }

    /**
     * 集合分组转Map
     */
    public static <K, V> Map<K, List<V>> groupBy(Collection<V> values, Function<? super V, ? extends K> classifier) {
        Preconditions.checkNotNull(classifier);
        if (values == null || values.isEmpty()) {
            return Maps.newHashMap();
        }
        return values.stream().collect(Collectors.groupingBy(classifier));
    }

    /**
     * 集合分组统计计数
     */
    public static <K, V> Map<K, Long> counting(Collection<V> values, Function<? super V, ? extends K> keyFunction) {
        Preconditions.checkNotNull(keyFunction);
        if (values == null || values.isEmpty()) {
            return Maps.newHashMap();
        }
        return values.stream().collect(Collectors.groupingBy(keyFunction, Collectors.counting()));
    }

    public static <K, V> Map<K, V> newHashMap(int expectedSize) {
        return new HashMap<>((int) (expectedSize / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
    }

    public static <K, V> Map<K, V> newLinkedHashMap(int expectedSize) {
        return new LinkedHashMap<>((int) (expectedSize / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
    }

    public static List arrayToList(Object source) {
        return Arrays.asList(ObjectUtils.toObjectArray(source));
    }

    public static <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
        if (props != null) {
            for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements(); ) {
                String key   = (String) en.nextElement();
                Object value = props.get(key);
                if (value == null) {
                    // Allow for defaults fallback or potentially overridden accessor...
                    value = props.getProperty(key);
                }
                map.put((K) key, (V) value);
            }
        }
    }

    public static boolean contains(Iterator<?> iterator, Object element) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (ObjectUtils.nullSafeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean contains(Enumeration<?> enumeration, Object element) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Object candidate = enumeration.nextElement();
                if (ObjectUtils.nullSafeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsInstance(Collection<?> collection, Object element) {
        if (collection != null) {
            for (Object candidate : collection) {
                if (candidate == element) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
        return findFirstMatch(source, candidates) != null;
    }

    public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
        if (isEmpty(source) || isEmpty(candidates)) {
            return null;
        }
        for (E candidate : candidates) {
            if (source.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T findValueOfType(Collection<?> collection, Class<T> type) {
        if (isEmpty(collection)) {
            return null;
        }
        T value = null;
        for (Object element : collection) {
            if (type == null || type.isInstance(element)) {
                if (value != null) {
                    // More than one value found... no clear single value.
                    return null;
                }
                value = (T) element;
            }
        }
        return value;
    }

    public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
        if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
            return null;
        }
        for (Class<?> type : types) {
            Object value = findValueOfType(collection, type);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static boolean hasUniqueObject(Collection<?> collection) {
        if (isEmpty(collection)) {
            return false;
        }
        boolean hasCandidate = false;
        Object  candidate    = null;
        for (Object elem : collection) {
            if (!hasCandidate) {
                hasCandidate = true;
                candidate    = elem;
            } else if (candidate != elem) {
                return false;
            }
        }
        return true;
    }

    public static Class<?> findCommonElementType(Collection<?> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        Class<?> candidate = null;
        for (Object val : collection) {
            if (val != null) {
                if (candidate == null) {
                    candidate = val.getClass();
                } else if (candidate != val.getClass()) {
                    return null;
                }
            }
        }
        return candidate;
    }

    public static <T> T firstElement(Set<T> set) {
        if (isEmpty(set)) {
            return null;
        }
        if (set instanceof SortedSet) {
            return ((SortedSet<T>) set).first();
        }

        Iterator<T> it    = set.iterator();
        T           first = null;
        if (it.hasNext()) {
            first = it.next();
        }
        return first;
    }

    public static <T> T firstElement(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    public static <T> T lastElement(Set<T> set) {
        if (isEmpty(set)) {
            return null;
        }
        if (set instanceof SortedSet) {
            return ((SortedSet<T>) set).last();
        }

        // Full iteration necessary...
        Iterator<T> it   = set.iterator();
        T           last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        return last;
    }

    public static <T> T lastElement(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
        ArrayList<A> elements = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            elements.add(enumeration.nextElement());
        }
        return elements.toArray(array);
    }

    public static <E> Iterator<E> toIterator(Enumeration<E> enumeration) {
        return (enumeration != null ? new EnumerationIterator<>(enumeration) : Collections.emptyIterator());
    }

    public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> targetMap) {
        return new MultiValueMapAdapter<>(targetMap);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> MultiValueMap<K, V> unmodifiableMultiValueMap(MultiValueMap<? extends K, ? extends V> targetMap) {

        Assert.notNull(targetMap, "'targetMap' must not be null");
        Map<K, List<V>> result = newLinkedHashMap(targetMap.size());
        targetMap.forEach((key, value) -> {
            List<? extends V> values = Collections.unmodifiableList(value);
            result.put(key, (List<V>) values);
        });
        Map<K, List<V>> unmodifiableMap = Collections.unmodifiableMap(result);
        return toMultiValueMap(unmodifiableMap);
    }

    private static class EnumerationIterator<E> implements Iterator<E> {

        private final Enumeration<E> enumeration;

        public EnumerationIterator(Enumeration<E> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return this.enumeration.hasMoreElements();
        }

        @Override
        public E next() {
            if (!this.enumeration.hasMoreElements()) {
                throw new NoSuchElementException();
            }
            return this.enumeration.nextElement();
        }

        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported");
        }
    }

}
