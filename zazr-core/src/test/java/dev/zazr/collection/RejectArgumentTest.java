package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * A null predicate handed to {@code reject}, {@code rejectKeys} or {@code rejectValues} is rejected at once with the
 * message "predicate is null", on every type that has them: on an empty receiver, where the predicate would never be
 * called, as on a non-empty one, and on a LazyList before any element is read.
 */
public class RejectArgumentTest {

    private record Call(String label, ThrowingCallable call) {}

    private static void assertEachRejectsTheNullPredicate(java.util.List<Call> calls) {
        for (Call call : calls) {
            assertThatNullPointerException()
                    .as(call.label())
                    .isThrownBy(call.call())
                    .withMessage("predicate is null");
        }
    }

    @Test
    public void shouldRejectANullPredicateInRejectOnEverySequence() {
        Predicate<Integer> none = null;
        assertEachRejectsTheNullPredicate(java.util.List.of(
                new Call("Vector()", () -> Vector.<Integer>empty().reject(none)),
                new Call("Vector(1, 2)", () -> Vector.of(1, 2).reject(none)),
                new Call("List()", () -> List.<Integer>empty().reject(none)),
                new Call("List(1, 2)", () -> List.of(1, 2).reject(none)),
                new Call("Queue()", () -> Queue.<Integer>empty().reject(none)),
                new Call("Queue(1, 2)", () -> Queue.of(1, 2).reject(none)),
                new Call("LazyList()", () -> LazyList.<Integer>empty().reject(none)),
                new Call("LazyList(1, 2)", () -> LazyList.of(1, 2).reject(none)),
                new Call("NonEmptyVector(1, 2)", () -> NonEmptyVector.of(1, 2).reject(none))));
    }

    @Test
    public void shouldRejectANullPredicateInRejectOnALazyListBeforeReadingAnElement() {
        Predicate<Integer> none = null;
        assertEachRejectsTheNullPredicate(java.util.List.of(
                new Call(
                        "infinite LazyList",
                        () -> LazyList.iterate(1, i -> i + 1).reject(none)),
                new Call(
                        "LazyList whose elements throw",
                        () -> LazyList.<Integer>continually(() -> {
                                    throw new AssertionError("an element was read");
                                })
                                .reject(none))));
    }

    @Test
    public void shouldRejectANullPredicateInRejectOnEverySet() {
        Predicate<Integer> none = null;
        assertEachRejectsTheNullPredicate(java.util.List.of(
                new Call("HashSet()", () -> HashSet.<Integer>empty().reject(none)),
                new Call("HashSet(1, 2)", () -> HashSet.of(1, 2).reject(none)),
                new Call("LinkedHashSet()", () -> LinkedHashSet.<Integer>empty().reject(none)),
                new Call("LinkedHashSet(1, 2)", () -> LinkedHashSet.of(1, 2).reject(none)),
                new Call("TreeSet()", () -> TreeSet.<Integer>empty().reject(none)),
                new Call("TreeSet(1, 2)", () -> TreeSet.of(1, 2).reject(none)),
                new Call("NonEmptySet(1, 2)", () -> NonEmptySet.of(1, 2).reject(none)),
                new Call(
                        "NonEmptySortedSet(1, 2)",
                        () -> NonEmptySortedSet.of(1, 2).reject(none))));
    }

    @Test
    public void shouldRejectANullPredicateInEveryRejectOfEveryMap() {
        BiPredicate<Integer, String> noBiPredicate = null;
        Predicate<Tuple2<Integer, String>> noEntryPredicate = null;
        Predicate<Integer> noKeyPredicate = null;
        Predicate<String> noValuePredicate = null;
        java.util.List<Map<Integer, String>> maps = java.util.List.of(
                HashMap.empty(), HashMap.of(1, "a", 2, "b"),
                LinkedHashMap.empty(), LinkedHashMap.of(1, "a", 2, "b"),
                TreeMap.empty(), TreeMap.of(1, "a", 2, "b"));
        java.util.List<Call> calls = new java.util.ArrayList<>();
        for (Map<Integer, String> map : maps) {
            String label = map.getClass().getSimpleName() + map;
            calls.add(new Call(label + ".reject(BiPredicate)", () -> map.reject(noBiPredicate)));
            calls.add(new Call(label + ".reject(Predicate)", () -> map.reject(noEntryPredicate)));
            calls.add(new Call(label + ".rejectKeys", () -> map.rejectKeys(noKeyPredicate)));
            calls.add(new Call(label + ".rejectValues", () -> map.rejectValues(noValuePredicate)));
        }
        NonEmptyMap<Integer, String> nonEmptyMap = NonEmptyMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        calls.add(new Call("NonEmptyMap.reject(BiPredicate)", () -> nonEmptyMap.reject(noBiPredicate)));
        calls.add(new Call("NonEmptyMap.reject(Predicate)", () -> nonEmptyMap.reject(noEntryPredicate)));
        calls.add(new Call("NonEmptyMap.rejectKeys", () -> nonEmptyMap.rejectKeys(noKeyPredicate)));
        calls.add(new Call("NonEmptyMap.rejectValues", () -> nonEmptyMap.rejectValues(noValuePredicate)));
        NonEmptySortedMap<Integer, String> nonEmptySortedMap = NonEmptySortedMap.of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        calls.add(new Call("NonEmptySortedMap.reject(BiPredicate)", () -> nonEmptySortedMap.reject(noBiPredicate)));
        calls.add(new Call("NonEmptySortedMap.reject(Predicate)", () -> nonEmptySortedMap.reject(noEntryPredicate)));
        calls.add(new Call("NonEmptySortedMap.rejectKeys", () -> nonEmptySortedMap.rejectKeys(noKeyPredicate)));
        calls.add(new Call("NonEmptySortedMap.rejectValues", () -> nonEmptySortedMap.rejectValues(noValuePredicate)));
        assertEachRejectsTheNullPredicate(calls);
    }
}
