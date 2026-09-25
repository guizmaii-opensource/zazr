package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The order in which a collection built from given elements must iterate, computed with the JDK collections.
 *
 * @param <T> the element type (the entry type of a map)
 */
@FunctionalInterface
public interface IterationOrder<T> {

    /**
     * The elements a collection built from {@code input} iterates over, in order.
     *
     * @param input the elements given to {@code ofAll}, a builder or a collector, in order
     * @return the expected iteration
     */
    List<T> of(List<T> input);

    /**
     * The input order, duplicates kept: the sequences.
     *
     * @param <T> the element type
     * @return the order
     */
    static <T> IterationOrder<T> input() {
        return ArrayList::new;
    }

    /**
     * The order of first occurrence, duplicates dropped: the insertion-ordered sets.
     *
     * @param <T> the element type
     * @return the order
     */
    static <T> IterationOrder<T> firstOccurrence() {
        return input -> new ArrayList<>(new LinkedHashSet<>(input));
    }

    /**
     * Sorted by {@code comparator}, duplicates dropped: the sorted sets.
     *
     * @param comparator the order of the elements
     * @param <T>        the element type
     * @return the order
     */
    static <T> IterationOrder<T> sorted(Comparator<? super T> comparator) {
        return input -> {
            final TreeSet<T> sorted = new TreeSet<>(comparator);
            sorted.addAll(input);
            return new ArrayList<>(sorted);
        };
    }

    /**
     * Keys in the order of their first occurrence, each with the value of its last occurrence: the
     * insertion-ordered maps, built by any factory, collector or successive puts, where putting an existing key
     * replaces its value in place.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the order
     */
    static <K, V> IterationOrder<Tuple2<K, V>> keysByFirstOccurrence() {
        return input -> {
            final LinkedHashMap<K, V> map = new LinkedHashMap<>();
            input.forEach(entry -> map.put(entry._1(), entry._2()));
            return map.entrySet().stream().map(entry -> com.guizmaii.zazr.Tuple.of(entry.getKey(), entry.getValue())).toList();
        };
    }

    /**
     * Keys sorted by {@code comparator}, each with the value of its last occurrence: the sorted maps.
     *
     * @param comparator the order of the keys
     * @param <K>        the key type
     * @param <V>        the value type
     * @return the order
     */
    static <K, V> IterationOrder<Tuple2<K, V>> keysSorted(Comparator<? super K> comparator) {
        return input -> {
            final TreeMap<K, V> map = new TreeMap<>(comparator);
            input.forEach(entry -> map.put(entry._1(), entry._2()));
            return map.entrySet().stream().map(entry -> com.guizmaii.zazr.Tuple.of(entry.getKey(), entry.getValue())).toList();
        };
    }
}
