package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `sizeCompare(int)` and `sizeCompare(Iterable)` on every collection: the sign at the boundaries of each size, the
/// sizes the tries change shape at, and how many elements a size that takes a walk has counted when it answers.
public class SizeCompareTest {

    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};

    /// A collection of each type, of a given size, and its two `sizeCompare` methods.
    private record Kind(
            String name,
            IntFunction<Iterable<Integer>> of,
            Compare<Integer> withSize,
            Compare<Iterable<?>> withIterable) {}

    @FunctionalInterface
    private interface Compare<A> {
        int apply(Iterable<Integer> collection, A other);
    }

    @SuppressWarnings("unchecked")
    private static <C> Kind kind(
            String name,
            IntFunction<C> of,
            java.util.function.BiFunction<C, Integer, Integer> withSize,
            java.util.function.BiFunction<C, Iterable<?>, Integer> withIterable) {
        return new Kind(
                name,
                size -> (Iterable<Integer>) of.apply(size),
                (collection, other) -> withSize.apply((C) collection, other),
                (collection, other) -> withIterable.apply((C) collection, other));
    }

    private static java.util.List<Kind> kinds() {
        return java.util.List.of(
                kind("Vector", n -> Vector.range(0, n), Vector::sizeCompare, Vector::sizeCompare),
                kind("List", n -> List.range(0, n), List::sizeCompare, List::sizeCompare),
                kind("Queue", SizeCompareTest::queueOfFrontAndRear, Queue::sizeCompare, Queue::sizeCompare),
                kind("LazyList", n -> LazyList.range(0, n), LazyList::sizeCompare, LazyList::sizeCompare),
                kind("HashSet", n -> HashSet.range(0, n), HashSet::sizeCompare, HashSet::sizeCompare),
                kind(
                        "LinkedHashSet",
                        n -> LinkedHashSet.range(0, n),
                        LinkedHashSet::sizeCompare,
                        LinkedHashSet::sizeCompare),
                kind("TreeSet", n -> TreeSet.range(0, n), TreeSet::sizeCompare, TreeSet::sizeCompare),
                kind(
                        "HashMap",
                        n -> HashMap.ofEntries(Vector.range(0, n).map(i -> Tuple.of(i, i))),
                        HashMap::sizeCompare,
                        HashMap::sizeCompare),
                kind(
                        "LinkedHashMap",
                        n -> LinkedHashMap.ofEntries(Vector.range(0, n).map(i -> Tuple.of(i, i))),
                        LinkedHashMap::sizeCompare,
                        LinkedHashMap::sizeCompare),
                kind(
                        "TreeMap",
                        n -> TreeMap.<Integer, Integer>ofEntries(
                                Vector.range(0, n).map(i -> Tuple.of(i, i))),
                        TreeMap::sizeCompare,
                        TreeMap::sizeCompare));
    }

    /// The non-empty kinds, sized 1 and up.
    private static java.util.List<Kind> nonEmptyKinds() {
        return java.util.List.of(
                kind(
                        "NonEmptyVector",
                        n -> NonEmptyVector.unsafeFromVector(Vector.range(0, n)),
                        NonEmptyVector::sizeCompare,
                        NonEmptyVector::sizeCompare),
                kind(
                        "NonEmptySet",
                        n -> NonEmptySet.unsafeFromSet(HashSet.range(0, n)),
                        NonEmptySet::sizeCompare,
                        NonEmptySet::sizeCompare),
                kind(
                        "NonEmptySortedSet",
                        n -> NonEmptySortedSet.unsafeFromSortedSet(TreeSet.range(0, n)),
                        NonEmptySortedSet::sizeCompare,
                        NonEmptySortedSet::sizeCompare),
                kind(
                        "NonEmptyMap",
                        n -> NonEmptyMap.unsafeFromMap(
                                HashMap.ofEntries(Vector.range(0, n).map(i -> Tuple.of(i, i)))),
                        NonEmptyMap::sizeCompare,
                        NonEmptyMap::sizeCompare),
                kind(
                        "NonEmptySortedMap",
                        n -> NonEmptySortedMap.unsafeFromSortedMap(TreeMap.<Integer, Integer>ofEntries(
                                Vector.range(0, n).map(i -> Tuple.of(i, i)))),
                        NonEmptySortedMap::sizeCompare,
                        NonEmptySortedMap::sizeCompare));
    }

    /// A queue of `n` elements whose rear holds the second half, so that a count crosses from front to rear.
    private static Queue<Integer> queueOfFrontAndRear(int n) {
        Queue<Integer> front = Queue.ofAll(Vector.range(0, n / 2));
        return Vector.range(n / 2, n).foldLeft(front, Queue::enqueue);
    }

    /// The sizes each collection of `size` elements is compared with: around it, zero, negatives and the extremes.
    private static int[] others(int size) {
        return new int[] {size - 1, size, size + 1, 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
    }

    @TestFactory
    public java.util.List<DynamicTest> shouldCompareTheSizeWithAnIntAtEveryBoundary() {
        java.util.List<DynamicTest> tests = new ArrayList<>();
        for (Kind kind : kinds()) {
            tests.add(DynamicTest.dynamicTest(kind.name(), () -> {
                for (int size : SIZES) {
                    Iterable<Integer> collection = kind.of().apply(size);
                    for (int other : others(size)) {
                        assertThat(kind.withSize().apply(collection, other))
                                .as("%s of %d against %d", kind.name(), size, other)
                                .isEqualTo(Integer.compare(size, other));
                    }
                }
            }));
        }
        for (Kind kind : nonEmptyKinds()) {
            tests.add(DynamicTest.dynamicTest(kind.name(), () -> {
                for (int size : SIZES) {
                    if (size == 0) {
                        continue;
                    }
                    Iterable<Integer> collection = kind.of().apply(size);
                    for (int other : others(size)) {
                        assertThat(kind.withSize().apply(collection, other))
                                .as("%s of %d against %d", kind.name(), size, other)
                                .isEqualTo(Integer.compare(size, other));
                    }
                }
            }));
        }
        return tests;
    }

    /// The other collections a collection is compared with: each kind (stored sizes, sizes that take a walk), a JDK
    /// list, and a one-shot iterable, each of a given size.
    private static java.util.List<Tuple2<String, IntFunction<Iterable<?>>>> others() {
        java.util.List<Tuple2<String, IntFunction<Iterable<?>>>> others = new ArrayList<>();
        for (Kind kind : kinds()) {
            others.add(Tuple.of(kind.name(), n -> kind.of().apply(n)));
        }
        others.add(Tuple.of("NonEmptyVector", n -> NonEmptyVector.unsafeFromVector(Vector.range(0, n))));
        others.add(Tuple.of(
                "java.util.ArrayList", n -> new ArrayList<>(Vector.range(0, n).asJava())));
        others.add(Tuple.of("one-shot", n -> oneShot(Vector.range(0, n))));
        return others;
    }

    @TestFactory
    public java.util.List<DynamicTest> shouldCompareTheSizeWithAnotherCollectionOfEveryKind() {
        int[] sizes = {0, 1, 2, 32, 33};
        java.util.List<Kind> receivers = new ArrayList<>(kinds());
        receivers.addAll(nonEmptyKinds());
        java.util.List<DynamicTest> tests = new ArrayList<>();
        for (Kind kind : receivers) {
            tests.add(DynamicTest.dynamicTest(kind.name(), () -> {
                for (int size : sizes) {
                    if (size == 0 && kind.name().startsWith("NonEmpty")) {
                        continue;
                    }
                    Iterable<Integer> collection = kind.of().apply(size);
                    for (Tuple2<String, IntFunction<Iterable<?>>> other : others()) {
                        for (int otherSize : new int[] {size - 1, size, size + 1}) {
                            if (otherSize < 0 || (otherSize == 0 && other._1().startsWith("NonEmpty"))) {
                                continue;
                            }
                            assertThat(kind.withIterable()
                                            .apply(collection, other._2().apply(otherSize)))
                                    .as("%s of %d against %s of %d", kind.name(), size, other._1(), otherSize)
                                    .isEqualTo(Integer.compare(size, otherSize));
                        }
                    }
                }
            }));
        }
        return tests;
    }

    /// How many elements of an infinite LazyList `sizeCompare(otherSize)` computes, its result checked to be 1.
    private static int computedBySizeCompare(int otherSize) {
        AtomicInteger computed = new AtomicInteger();
        LazyList<Integer> infinite = LazyList.continually(computed::incrementAndGet);
        assertThat(infinite.sizeCompare(otherSize)).as("against %d", otherSize).isEqualTo(1);
        return computed.get();
    }

    @Test
    public void shouldComputeAtMostOneMoreElementThanTheSizeItIsComparedWithOnAnInfiniteLazyList() {
        assertThat(computedBySizeCompare(Integer.MIN_VALUE)).isZero();
        assertThat(computedBySizeCompare(-1)).isZero();
        assertThat(computedBySizeCompare(0)).isEqualTo(1);
        assertThat(computedBySizeCompare(1)).isEqualTo(2);
        assertThat(computedBySizeCompare(5)).isEqualTo(6);
        assertThat(computedBySizeCompare(1024)).isEqualTo(1025);
    }

    @Test
    public void shouldComputeOnlyWhatTheComparisonNeedsOnAFiniteLazyList() {
        AtomicInteger computed = new AtomicInteger();
        LazyList<Integer> three =
                LazyList.continually(computed::incrementAndGet).take(3);
        assertThat(three.sizeCompare(1)).isEqualTo(1);
        assertThat(computed.get()).isEqualTo(2);
        assertThat(three.sizeCompare(3)).isEqualTo(0);
        assertThat(three.sizeCompare(10)).isEqualTo(-1);
        assertThat(computed.get()).isEqualTo(3);
    }

    @Test
    public void shouldCompareAnInfiniteLazyListWithAnotherCollection() {
        AtomicInteger computed = new AtomicInteger();
        LazyList<Integer> infinite = LazyList.continually(computed::incrementAndGet);
        assertThat(infinite.sizeCompare(Vector.range(0, 10))).isEqualTo(1);
        assertThat(computed.get()).isEqualTo(11);
        assertThat(LazyList.from(0).sizeCompare(List.range(0, 10))).isEqualTo(1);
        assertThat(LazyList.from(0).sizeCompare(oneShot(Vector.range(0, 10)))).isEqualTo(1);
        assertThat(LazyList.from(0).sizeCompare(LazyList.range(0, 10))).isEqualTo(1);

        assertThat(Vector.range(0, 10).sizeCompare(LazyList.from(0))).isEqualTo(-1);
        assertThat(List.range(0, 10).sizeCompare(LazyList.from(0))).isEqualTo(-1);
        assertThat(Queue.of(1, 2).sizeCompare(LazyList.from(0))).isEqualTo(-1);
        assertThat(LazyList.range(0, 10).sizeCompare(LazyList.from(0))).isEqualTo(-1);
        assertThat(NonEmptyVector.of(1).sizeCompare(LazyList.from(0))).isEqualTo(-1);
        assertThat(HashSet.of(1).sizeCompare(LazyList.from(0))).isEqualTo(-1);
    }

    @Test
    public void shouldCompareAQueueHeldAtTheBackWithASizeThatIsNotStored() {
        for (int size : new int[] {0, 1, 2, 33}) {
            Queue<Integer> enqueued = Vector.range(0, size).foldLeft(Queue.empty(), Queue::enqueue);
            for (int other : new int[] {0, 1, 2, 32, 33, 34}) {
                int expected = Integer.compare(size, other);
                assertThat(enqueued.sizeCompare(List.range(0, other))).isEqualTo(expected);
                assertThat(enqueued.sizeCompare(oneShot(Vector.range(0, other))))
                        .isEqualTo(expected);
                assertThat(enqueued.sizeCompare(LazyList.range(0, other))).isEqualTo(expected);
                assertThat(List.range(0, other).sizeCompare(enqueued)).isEqualTo(-expected);
                assertThat(LazyList.range(0, other).sizeCompare(enqueued)).isEqualTo(-expected);
                assertThat(Vector.range(0, other).sizeCompare(enqueued)).isEqualTo(-expected);
            }
        }
        assertThat(Queue.of(1).enqueue(2).sizeCompare(LazyList.from(0))).isEqualTo(-1);
        assertThat(LazyList.from(0).sizeCompare(Queue.of(1).enqueue(2))).isEqualTo(1);
    }

    @Test
    public void shouldIterateAJdkCollectionInsteadOfAskingItsSize() {
        // the size of the view of an infinite LazyList never returns: only its iterator is read
        assertThat(Vector.range(0, 10).sizeCompare(LazyList.from(0).asJava())).isEqualTo(-1);
        assertThat(LazyList.from(0).sizeCompare(LazyList.from(0).take(3).asJava()))
                .isEqualTo(1);
    }

    @Test
    public void shouldIterateAOneShotIterableOnce() {
        assertThat(Vector.of(1, 2).sizeCompare(oneShot(Vector.of(1, 2)))).isEqualTo(0);
        assertThat(List.of(1, 2).sizeCompare(oneShot(Vector.of(1)))).isEqualTo(1);
        assertThat(LazyList.of(1).sizeCompare(oneShot(Vector.of(1, 2)))).isEqualTo(-1);
        assertThat(NonEmptySet.of(1, 2, 3).sizeCompare(oneShot(Vector.of(1, 2, 3, 4))))
                .isEqualTo(-1);
    }

    @Test
    public void shouldCountAOneShotIterableOnlyUpToTheStoredSize() {
        AtomicInteger read = new AtomicInteger();
        Iterable<Integer> counting = () -> new java.util.Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return read.incrementAndGet();
            }
        };
        assertThat(Vector.range(0, 5).sizeCompare(counting)).isEqualTo(-1);
        // five elements read, then hasNext tells there is a sixth
        assertThat(read.get()).isEqualTo(5);
    }

    @Test
    public void shouldNotChangeTheReceiver() {
        List<Integer> list = List.of(1, 2, 3);
        Queue<Integer> queue = queueOfFrontAndRear(4);
        assertThat(list.sizeCompare(1)).isEqualTo(1);
        assertThat(queue.sizeCompare(2)).isEqualTo(1);
        assertThat(list).containsExactly(1, 2, 3);
        assertThat(queue).containsExactly(0, 1, 2, 3);
    }

    @Test
    public void shouldRejectANullIterable() {
        java.util.List<Kind> receivers = new ArrayList<>(kinds());
        receivers.addAll(nonEmptyKinds());
        for (Kind kind : receivers) {
            Iterable<Integer> collection = kind.of().apply(1);
            assertThatNullPointerException()
                    .as(kind.name())
                    .isThrownBy(() -> kind.withIterable().apply(collection, null))
                    .withMessage("that is null");
        }
    }

    /// An iterable that is not a collection and can be iterated once.
    private static <T> Iterable<T> oneShot(Iterable<T> elements) {
        boolean[] used = {false};
        return () -> {
            assertThat(used[0]).as("iterated twice").isFalse();
            used[0] = true;
            return elements.iterator();
        };
    }
}
