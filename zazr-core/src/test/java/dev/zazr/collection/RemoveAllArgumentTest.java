package dev.zazr.collection;

import dev.zazr.Tuple;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * A null iterable handed to {@code removeAll} is rejected at once, on an empty receiver as on a non-empty one, with
 * one message per kind of collection: "keys is null" on every map, "elements is null" on every set and sequence.
 */
public class RemoveAllArgumentTest {

    private record Call(String label, ThrowingCallable call) {}

    private static void assertEachThrows(String message, java.util.List<Call> calls) {
        for (Call call : calls) {
            assertThatNullPointerException()
                    .as(call.label())
                    .isThrownBy(call.call())
                    .withMessage(message);
        }
    }

    @Test
    public void shouldRejectNullKeysOnEveryMap() {
        Iterable<Integer> none = null;
        assertEachThrows(
                "keys is null",
                java.util.List.of(
                        new Call(
                                "HashMap()",
                                () -> HashMap.<Integer, String>empty().removeAll(none)),
                        new Call(
                                "HashMap(1, 2)",
                                () -> HashMap.of(1, "a", 2, "b").removeAll(none)),
                        new Call(
                                "LinkedHashMap()",
                                () -> LinkedHashMap.<Integer, String>empty().removeAll(none)),
                        new Call(
                                "LinkedHashMap(1, 2)",
                                () -> LinkedHashMap.of(1, "a", 2, "b").removeAll(none)),
                        new Call(
                                "TreeMap()",
                                () -> TreeMap.<Integer, String>empty().removeAll(none)),
                        new Call(
                                "TreeMap(1, 2)",
                                () -> TreeMap.of(1, "a", 2, "b").removeAll(none)),
                        new Call(
                                "NonEmptyMap(1, 2)",
                                () -> NonEmptyMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"))
                                        .removeAll(none)),
                        new Call(
                                "NonEmptySortedMap(1, 2)",
                                () -> NonEmptySortedMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"))
                                        .removeAll(none))));
    }

    @Test
    public void shouldRejectNullElementsOnEverySetAndSequence() {
        Iterable<Integer> none = null;
        assertEachThrows(
                "elements is null",
                java.util.List.of(
                        new Call("HashSet()", () -> HashSet.<Integer>empty().removeAll(none)),
                        new Call("HashSet(1, 2)", () -> HashSet.of(1, 2).removeAll(none)),
                        new Call(
                                "LinkedHashSet()",
                                () -> LinkedHashSet.<Integer>empty().removeAll(none)),
                        new Call(
                                "LinkedHashSet(1, 2)",
                                () -> LinkedHashSet.of(1, 2).removeAll(none)),
                        new Call("TreeSet()", () -> TreeSet.<Integer>empty().removeAll(none)),
                        new Call("TreeSet(1, 2)", () -> TreeSet.of(1, 2).removeAll(none)),
                        new Call("NonEmptySet(1, 2)", () -> NonEmptySet.of(1, 2).removeAll(none)),
                        new Call(
                                "NonEmptySortedSet(1, 2)",
                                () -> NonEmptySortedSet.of(1, 2).removeAll(none)),
                        new Call("Vector()", () -> Vector.<Integer>empty().removeAll(none)),
                        new Call("Vector(1, 2)", () -> Vector.of(1, 2).removeAll(none)),
                        new Call("List()", () -> List.<Integer>empty().removeAll(none)),
                        new Call("List(1, 2)", () -> List.of(1, 2).removeAll(none)),
                        new Call("Queue()", () -> Queue.<Integer>empty().removeAll(none)),
                        new Call("Queue(1, 2)", () -> Queue.of(1, 2).removeAll(none)),
                        new Call("LazyList()", () -> LazyList.<Integer>empty().removeAll(none)),
                        new Call("LazyList(1, 2)", () -> LazyList.of(1, 2).removeAll(none)),
                        new Call(
                                "NonEmptyVector(1, 2)",
                                () -> NonEmptyVector.of(1, 2).removeAll(none))));
    }
}
