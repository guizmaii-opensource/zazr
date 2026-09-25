package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * A null function handed to a map is rejected at once, whatever the map holds: with the key present, with the key
 * absent, and on an empty map, where the function would never be called.
 */
public class MapFunctionArgumentTest {

    private static java.util.List<Map<Integer, String>> maps() {
        return java.util.List.of(
                HashMap.empty(), HashMap.of(1, "a"),
                LinkedHashMap.empty(), LinkedHashMap.of(1, "a"),
                TreeMap.empty(), TreeMap.of(1, "a"));
    }

    @Test
    public void shouldRejectANullRemappingFunctionInComputeIfPresent() {
        for (Map<Integer, String> map : maps()) {
            for (int key : new int[] { 1, 2 }) {
                assertThatNullPointerException().as(map + ", key " + key)
                        .isThrownBy(() -> map.computeIfPresent(key, (BiFunction<Integer, String, String>) null))
                        .withMessage("remappingFunction is null");
            }
        }
    }

    @Test
    public void shouldRejectANullMappingFunctionInComputeIfAbsent() {
        for (Map<Integer, String> map : maps()) {
            for (int key : new int[] { 1, 2 }) {
                assertThatNullPointerException().as(map + ", key " + key)
                        .isThrownBy(() -> map.computeIfAbsent(key, (Function<Integer, String>) null))
                        .withMessage("mappingFunction is null");
            }
        }
    }

    @Test
    public void shouldRejectANullFunctionInReplaceAll() {
        for (Map<Integer, String> map : maps()) {
            assertThatNullPointerException().as(map.toString())
                    .isThrownBy(() -> map.replaceAll((BiFunction<Integer, String, String>) null))
                    .withMessage("function is null");
        }
    }

    @Test
    public void shouldRejectANullSupplierInOrElse() {
        for (Map<Integer, String> map : maps()) {
            assertThatNullPointerException().as(map.toString())
                    .isThrownBy(() -> map.orElse((Supplier<Iterable<Tuple2<Integer, String>>>) null))
                    .withMessage("supplier is null");
        }
    }

    @Test
    public void shouldRejectANullSupplierInOrElseOnEveryOtherCollection() {
        final Supplier<Iterable<Integer>> none = null;
        final java.util.List<ThrowingCallable> calls = java.util.List.of(
                () -> Vector.<Integer> empty().orElse(none), () -> Vector.of(1).orElse(none),
                () -> List.<Integer> empty().orElse(none), () -> List.of(1).orElse(none),
                () -> Queue.<Integer> empty().orElse(none), () -> Queue.of(1).orElse(none),
                () -> Stream.<Integer> empty().orElse(none), () -> Stream.of(1).orElse(none),
                () -> HashSet.<Integer> empty().orElse(none), () -> HashSet.of(1).orElse(none),
                () -> LinkedHashSet.<Integer> empty().orElse(none), () -> LinkedHashSet.of(1).orElse(none),
                () -> TreeSet.<Integer> empty().orElse(none), () -> TreeSet.of(1).orElse(none));
        for (ThrowingCallable call : calls) {
            assertThatNullPointerException().isThrownBy(call).withMessage("supplier is null");
        }
    }
}
