package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.test.Arbitrary;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A collection type checked by {@link CollectionLaws}.
 *
 * @param values  arbitrary collections
 * @param ofAll   builds a collection of the type under test from elements, in iteration order (a map takes its
 *                entries)
 * @param size    the collection's {@code size()}
 * @param toList  the collection's {@code toList()}
 * @param ordered whether equality depends on the iteration order (sequences) or not (sets and maps)
 * @param <T>     the element type (the entry type of a map)
 * @param <F>     the collection type
 */
public record CollectionSubject<T, F extends Iterable<T>>(Arbitrary<F> values, Function<Iterable<T>, F> ofAll,
                                                          ToIntFunction<F> size, Function<F, List<T>> toList,
                                                          boolean ordered) {

    /**
     * Creates a subject.
     *
     * @param values  arbitrary collections
     * @param ofAll   builds a collection of the type under test from elements
     * @param size    the collection's {@code size()}
     * @param toList  the collection's {@code toList()}
     * @param ordered whether equality depends on the iteration order
     * @throws NullPointerException if an argument is null
     */
    public CollectionSubject {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(ofAll, "ofAll is null");
        Objects.requireNonNull(size, "size is null");
        Objects.requireNonNull(toList, "toList is null");
    }
}
