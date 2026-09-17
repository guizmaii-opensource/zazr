package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.*;
import org.jspecify.annotations.Nullable;

/**
 * INTERNAL: Common {@code Map} functions (not intended to be public).
 *
 * @author Ruslan Sennov, Daniel Dietrich
 */
final class Maps {

    /**
     * Marker for "no value stored under this key", compared by identity only. It exists purely to
     * avoid allocating a {@link Option#some} on lookup hot paths: internal presence checks call
     * {@link #getOrAbsent(Map, Object)} (which is {@link Map#getOrElse(Object, Object)} with this as
     * the default) instead of {@link Map#get(Object)}, so no {@code Option} is boxed just to test
     * {@code isDefined()}. It is never stored in a map and never returned to a caller: a value equal
     * to it is impossible, because every value a caller can put is non-null and this instance is not
     * reachable outside this package (design 3.9 forbids a stored {@code null}, which is the only
     * thing this sentinel used to have to be told apart from).
     */
    static final Object ABSENT = new Object();

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object> V getOrAbsent(Map<K, V> map, K key) {
        return map.getOrElse(key, (V) ABSENT);
    }

    private Maps() {
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Tuple2<V, M> computeIfAbsent(M map, K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction is null");
        final V value = getOrAbsent(map, key);
        if (value != ABSENT) {
            return Tuple.of(value, map);
        } else {
            final V newValue = mappingFunction.apply(key);
            final M newMap = (M) map.put(key, newValue);
            return Tuple.of(newValue, newMap);
        }
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Tuple2<Option<V>, M> computeIfPresent(M map, K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        final V value = getOrAbsent(map, key);
        if (value != ABSENT) {
            final V newValue = remappingFunction.apply(key, value);
            final M newMap = (M) map.put(key, newValue);
            return Tuple.of(Option.some(newValue), newMap);
        } else {
            return Tuple.of(Option.none(), map);
        }
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M distinct(M map) {
        return map;
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M distinctBy(M map, OfEntries<K, V, M> ofEntries,
            Comparator<? super Tuple2<K, V>> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofEntries.apply(map.iterator().distinctBy(comparator));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, U extends @Nullable Object, M extends Map<K, V>> M distinctBy(
            M map, OfEntries<K, V, M> ofEntries, Function<? super Tuple2<K, V>, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofEntries.apply(map.iterator().distinctBy(keyExtractor));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M drop(M map, OfEntries<K, V, M> ofEntries, Supplier<M> emptySupplier, int n) {
        if (n <= 0) {
            return map;
        } else if (n >= map.size()) {
            return emptySupplier.get();
        } else {
            return ofEntries.apply(map.iterator().drop(n));
        }
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M dropRight(M map, OfEntries<K, V, M> ofEntries, Supplier<M> emptySupplier,
            int n) {
        if (n <= 0) {
            return map;
        } else if (n >= map.size()) {
            return emptySupplier.get();
        } else {
            return ofEntries.apply(map.iterator().dropRight(n));
        }
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M dropUntil(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(map, ofEntries, predicate.negate());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M dropWhile(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return ofEntries.apply(map.iterator().dropWhile(predicate));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M filter(M map, OfEntries<K, V, M> ofEntries,
            BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(map, ofEntries, t -> predicate.test(t._1(), t._2()));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M filter(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return ofEntries.apply(map.iterator().filter(predicate));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M filterKeys(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(map, ofEntries, t -> predicate.test(t._1()));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M filterValues(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(map, ofEntries, t -> predicate.test(t._2()));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, C extends @Nullable Object, M extends Map<K, V>> Map<C, M> groupBy(M map, OfEntries<K, V, M> ofEntries,
            Function<? super Tuple2<K, V>, ? extends C> classifier) {
        return Collections.groupBy(map, classifier, ofEntries);
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Iterator<M> grouped(M map, OfEntries<K, V, M> ofEntries, int size) {
        return sliding(map, ofEntries, size, size);
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Option<M> initOption(M map) {
        return map.isEmpty() ? Option.none() : Option.some((M) map.init());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M merge(M map, OfEntries<K, V, M> ofEntries,
            Map<? extends K, ? extends V> that) {
        Objects.requireNonNull(that, "that is null");
        if (map.isEmpty()) {
            return ofEntries.apply(Map.narrow(that));
        } else if (that.isEmpty()) {
            return map;
        } else {
            return that.foldLeft(map, (result, entry) -> !result.containsKey(entry._1()) ? put(result, entry) : result);
        }
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, U extends V, M extends Map<K, V>> M merge(
            M map, OfEntries<K, V, M> ofEntries,
            Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(collisionResolution, "collisionResolution is null");
        if (map.isEmpty()) {
            return ofEntries.apply(Map.narrow(that));
        } else if (that.isEmpty()) {
            return map;
        } else {
            return that.foldLeft(map, (result, entry) -> {
                final K key = entry._1();
                final U value = entry._2();
                final V current = getOrAbsent(result, key);
                final V newValue = current != ABSENT ? collisionResolution.apply(current, value) : value;
                return (M) result.put(key, newValue);
            });
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M ofStream(M map, java.util.stream.Stream<? extends T> stream,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(stream, "stream is null");
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return Stream.ofAll(stream).foldLeft(map, (m, el) -> (M) m.put(keyMapper.apply(el), valueMapper.apply(el)));
    }

    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M ofStream(M map, java.util.stream.Stream<? extends T> stream,
            Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        Objects.requireNonNull(stream, "stream is null");
        Objects.requireNonNull(entryMapper, "entryMapper is null");
        return Stream.ofAll(stream).foldLeft(map, (m, el) -> (M) m.put(entryMapper.apply(el)));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Tuple2<M, M> partition(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final java.util.List<Tuple2<K, V>> left = new java.util.ArrayList<>();
        final java.util.List<Tuple2<K, V>> right = new java.util.ArrayList<>();
        for (Tuple2<K, V> entry : map) {
            (predicate.test(entry) ? left : right).add(entry);
        }
        return Tuple.of(ofEntries.apply(left), ofEntries.apply(right));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M tap(M map, Consumer<? super Tuple2<K, V>> action) {
        Objects.requireNonNull(action, "action is null");
        map.forEach(action);
        return map;
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, U extends V, M extends Map<K, V>> M put(M map, K key, U value,
            BiFunction<? super V, ? super U, ? extends V> merge) {
        Objects.requireNonNull(merge, "the merge function is null");
        final V currentValue = getOrAbsent(map, key);
        if (currentValue == ABSENT) {
            return (M) map.put(key, value);
        } else {
            return (M) map.put(key, merge.apply(currentValue, value));
        }
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M put(M map, Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "entry is null");
        return (M) map.put(entry._1(), entry._2());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, U extends V, M extends Map<K, V>> M put(M map, Tuple2<? extends K, U> entry,
            BiFunction<? super V, ? super U, ? extends V> merge) {
        Objects.requireNonNull(merge, "the merge function is null");
        final V currentValue = getOrAbsent(map, entry._1());
        if (currentValue == ABSENT) {
            return put(map, entry);
        } else {
            return put(map, entry.map2(value -> merge.apply(currentValue, value)));
        }
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M reject(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(map, ofEntries, predicate.negate());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M reject(M map, OfEntries<K, V, M> ofEntries,
            BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(map, ofEntries, predicate.negate());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M rejectKeys(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filterKeys(map, ofEntries, predicate.negate());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M rejectValues(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filterValues(map, ofEntries, predicate.negate());
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M replace(M map, K key, V oldValue, V newValue) {
        return map.contains(Tuple.of(key, oldValue)) ? (M) map.put(key, newValue) : map;
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M replace(M map, Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        Objects.requireNonNull(currentElement, "currentElement is null");
        Objects.requireNonNull(newElement, "newElement is null");
        return (M) (map.contains(currentElement) ? map.remove(currentElement._1()).put(newElement) : map);
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M replaceAll(M map, BiFunction<? super K, ? super V, ? extends V> function) {
        return (M) map.map((k, v) -> Tuple.of(k, function.apply(k, v)));
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M replaceAll(M map, Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return replace(map, currentElement, newElement);
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M replaceValue(M map, K key, V value) {
        return map.containsKey(key) ? (M) map.put(key, value) : map;
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M scan(M map, Tuple2<K, V> zero,
            BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> operation,
            Function<Iterator<Tuple2<K, V>>, Traversable<Tuple2<K, V>>> finisher) {
        return (M) Collections.scanLeft(map, zero, operation, finisher);
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Iterator<M> slideBy(M map, OfEntries<K, V, M> ofEntries,
            Function<? super Tuple2<K, V>, ?> classifier) {
        return map.iterator().slideBy(classifier).map(ofEntries);
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Iterator<M> sliding(M map, OfEntries<K, V, M> ofEntries, int size) {
        return sliding(map, ofEntries, size, 1);
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Iterator<M> sliding(M map, OfEntries<K, V, M> ofEntries, int size, int step) {
        return map.iterator().sliding(size, step).map(ofEntries);
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Tuple2<M, M> span(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Tuple2<Iterator<Tuple2<K, V>>, Iterator<Tuple2<K, V>>> t = map.iterator().span(predicate);
        return Tuple.of(ofEntries.apply(t._1()), ofEntries.apply(t._2()));
    }

    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> Option<M> tailOption(M map) {
        return map.isEmpty() ? Option.none() : Option.some((M) map.tail());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M take(M map, OfEntries<K, V, M> ofEntries, int n) {
        if (n >= map.size()) {
            return map;
        } else {
            return ofEntries.apply(map.iterator().take(n));
        }
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M takeRight(M map, OfEntries<K, V, M> ofEntries, int n) {
        if (n >= map.size()) {
            return map;
        } else {
            return ofEntries.apply(map.iterator().takeRight(n));
        }
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M takeUntil(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(map, ofEntries, predicate.negate());
    }

    static <K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> M takeWhile(M map, OfEntries<K, V, M> ofEntries,
            Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final M taken = ofEntries.apply(map.iterator().takeWhile(predicate));
        return taken.size() == map.size() ? map : taken;
    }

    @FunctionalInterface
    interface OfEntries<K extends @Nullable Object, V extends @Nullable Object, M extends Map<K, V>> extends Function<Iterable<Tuple2<K, V>>, M> {
    }
}
