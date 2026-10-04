package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The ranges of TreeSet and TreeMap ({@code rangeFrom}, {@code rangeUntil}, {@code rangeTo}, {@code rangeFromUntil},
 * {@code minAfter}, {@code maxBefore}, {@code iteratorFrom}) and of their non-empty wrappers, against a
 * {@code java.util.TreeSet} and a {@code java.util.TreeMap} holding the same elements: on many sizes, on trees built
 * in one pass, by insertions and with removals, for bounds below, between, on and above the elements, under three
 * orders. Every result is a valid red-black tree, the receiver is left intact, and the result is the receiver itself
 * when nothing is cut and shares the rest of its tree otherwise.
 */
public class TreeRangeTest {

    private static final long SEED = 20261004L;
    private static final int[] SIZES = {0, 1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025
    };

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = Comparator.reverseOrder();
    // two numbers a multiple of a million apart are equal to the comparator but not to equals, and are distinct objects
    private static final int M = 1_000_000;
    private static final Comparator<Integer> MODULO = Comparator.comparingInt(i -> Math.floorMod(i, M));

    private static java.util.List<Comparator<Integer>> orders() {
        return java.util.List.of(NATURAL, REVERSED, MODULO);
    }

    // -- the trees behind the sets and maps

    @SuppressWarnings("unchecked")
    private static <T> RedBlackTree<T> tree(TreeSet<T> set) {
        return (RedBlackTree<T>) field(TreeSet.class, "tree", set);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> RedBlackTree<Tuple2<K, V>> tree(TreeMap<K, V> map) {
        return (RedBlackTree<Tuple2<K, V>>) field(TreeMap.class, "entries", map);
    }

    private static Object field(Class<?> type, String name, Object receiver) {
        try {
            java.lang.reflect.Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(receiver);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static IdentityHashMap<Object, Boolean> nodes(RedBlackTree<?> tree) {
        IdentityHashMap<Object, Boolean> nodes = new IdentityHashMap<>();
        java.util.ArrayDeque<RedBlackTree<?>> pending = new java.util.ArrayDeque<>();
        pending.push(tree);
        while (!pending.isEmpty()) {
            RedBlackTree<?> next = pending.pop();
            if (!next.isEmpty()) {
                nodes.put(next, true);
                pending.push(next.left());
                pending.push(next.right());
            }
        }
        return nodes;
    }

    // the number of nodes of `result` that are not nodes of `source`
    private static int newNodes(RedBlackTree<?> result, RedBlackTree<?> source) {
        IdentityHashMap<Object, Boolean> old = nodes(source);
        return (int) nodes(result).keySet().stream()
                .filter(node -> !old.containsKey(node))
                .count();
    }

    private static <T> java.util.List<T> list(java.util.Iterator<T> iterator) {
        java.util.List<T> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }

    private static <T> java.util.List<T> list(Iterable<T> iterable) {
        return list(iterable.iterator());
    }

    private static <K, V> java.util.List<Tuple2<K, V>> entries(java.util.Map<K, V> map) {
        return map.entrySet().stream()
                .map(e -> Tuple.of(e.getKey(), e.getValue()))
                .toList();
    }

    private static <K, V> Option<Tuple2<K, V>> entry(java.util.Map.@org.jspecify.annotations.Nullable Entry<K, V> e) {
        return e == null ? Option.none() : Option.some(Tuple.of(e.getKey(), e.getValue()));
    }

    // -- the inputs

    // 0, 2, 4, ..., 2 (n - 1): every odd number falls between two elements, -1 below them all, 2n - 1 above
    private static java.util.List<Integer> evens(int n) {
        java.util.List<Integer> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(2 * i);
        }
        return result;
    }

    /** The same elements in trees of three shapes: built in one pass, by insertions, and with removals. */
    private static java.util.List<TreeSet<Integer>> sets(Comparator<Integer> order, int n, Random random) {
        java.util.List<Integer> elements = evens(n);
        java.util.List<Integer> shuffled = new ArrayList<>(elements);
        java.util.Collections.shuffle(shuffled, random);
        TreeSet<Integer> built = TreeSet.ofAll(order, elements);
        TreeSet<Integer> inserted = Vector.ofAll(shuffled).foldLeft(TreeSet.empty(order), TreeSet::add);
        // odd numbers added among the elements, then removed: a shape that removals rebalanced
        java.util.List<Integer> odds = new ArrayList<>();
        for (int i = 0; i < n; i += 2) {
            odds.add(2 * i + 1);
        }
        java.util.List<Integer> mixed = new ArrayList<>(shuffled);
        mixed.addAll(odds);
        java.util.Collections.shuffle(mixed, random);
        TreeSet<Integer> grown = Vector.ofAll(mixed).foldLeft(TreeSet.empty(order), TreeSet::add);
        TreeSet<Integer> removed = Vector.ofAll(odds).foldLeft(grown, TreeSet::remove);
        return java.util.List.of(built, inserted, removed);
    }

    private static TreeMap<Integer, String> mapOf(TreeSet<Integer> keys) {
        return keys.foldLeft(TreeMap.<Integer, String>empty(keys.comparator()), (map, key) -> map.put(key, "v" + key));
    }

    /** Every bound from -1 to 2n on the small sizes, a sample with the extremes on the large ones. */
    private static java.util.List<Integer> bounds(int n, Random random) {
        java.util.List<Integer> result = new ArrayList<>();
        if (n <= 70) {
            for (int b = -1; b <= 2 * n; b++) {
                result.add(b);
            }
        } else {
            result.addAll(java.util.List.of(-1, 0, 1, 2 * n - 2, 2 * n - 1, 2 * n));
            for (int i = 0; i < 150; i++) {
                result.add(random.nextInt(2 * n + 2) - 1);
            }
        }
        return result;
    }

    /** Every pair of bounds on the small sizes, a sample on the large ones, in both orders. */
    private static java.util.List<int[]> boundPairs(int n, Random random) {
        java.util.List<Integer> bounds = bounds(n, random);
        java.util.List<int[]> result = new ArrayList<>();
        if (n <= 17) {
            for (int a : bounds) {
                for (int b : bounds) {
                    result.add(new int[] {a, b});
                }
            }
        } else {
            for (int i = 0; i < 300; i++) {
                result.add(
                        new int[] {bounds.get(random.nextInt(bounds.size())), bounds.get(random.nextInt(bounds.size()))
                        });
            }
        }
        return result;
    }

    // -- TreeSet against java.util.TreeSet

    @Nested
    class Sets {

        @Test
        void oneBoundRangesMatchTheModel() {
            Random random = new Random(SEED);
            for (Comparator<Integer> order : orders()) {
                for (int n : SIZES) {
                    for (TreeSet<Integer> set : sets(order, n, random)) {
                        java.util.List<Integer> before = list(set);
                        java.util.TreeSet<Integer> model = new java.util.TreeSet<>(order);
                        model.addAll(before);
                        for (int b : bounds(n, random)) {
                            String context = "n=" + n + ", bound=" + b + ", order=" + order;
                            TreeSet<Integer> from = set.rangeFrom(b);
                            TreeSet<Integer> until = set.rangeUntil(b);
                            TreeSet<Integer> to = set.rangeTo(b);
                            assertThat(list(from)).as(context).isEqualTo(list(model.tailSet(b, true)));
                            assertThat(list(until)).as(context).isEqualTo(list(model.headSet(b, false)));
                            assertThat(list(to)).as(context).isEqualTo(list(model.headSet(b, true)));
                            assertThat(set.minAfter(b)).as(context).isEqualTo(Option.ofNullable(model.ceiling(b)));
                            assertThat(set.maxBefore(b)).as(context).isEqualTo(Option.ofNullable(model.lower(b)));
                            assertThat(list(set.iteratorFrom(b))).as(context).isEqualTo(list(model.tailSet(b, true)));
                            for (TreeSet<Integer> result : java.util.List.of(from, until, to)) {
                                RedBlackTreeValidity.assertValid(tree(result));
                                assertThat(result.comparator()).isSameAs(set.comparator());
                                assertThat(result.size()).isEqualTo(list(result).size());
                            }
                        }
                        // the receiver is left intact
                        assertThat(list(set)).isEqualTo(before);
                        RedBlackTreeValidity.assertValid(tree(set));
                    }
                }
            }
        }

        @Test
        void twoBoundRangesMatchTheModel() {
            Random random = new Random(SEED + 1);
            for (Comparator<Integer> order : orders()) {
                for (int n : SIZES) {
                    for (TreeSet<Integer> set : sets(order, n, random)) {
                        java.util.List<Integer> before = list(set);
                        java.util.TreeSet<Integer> model = new java.util.TreeSet<>(order);
                        model.addAll(before);
                        for (int[] pair : boundPairs(n, random)) {
                            int from = pair[0];
                            int until = pair[1];
                            String context = "n=" + n + ", from=" + from + ", until=" + until + ", order=" + order;
                            TreeSet<Integer> result = set.rangeFromUntil(from, until);
                            // java.util.TreeSet.subSet throws when from is after until; the range is empty then
                            java.util.List<Integer> expected = order.compare(from, until) > 0
                                    ? java.util.List.of()
                                    : list(model.subSet(from, true, until, false));
                            assertThat(list(result)).as(context).isEqualTo(expected);
                            assertThat(result.size()).as(context).isEqualTo(expected.size());
                            assertThat(result.comparator()).isSameAs(set.comparator());
                            RedBlackTreeValidity.assertValid(tree(result));
                        }
                        assertThat(list(set)).isEqualTo(before);
                    }
                }
            }
        }

        @Test
        void aRangeThatCutsNothingIsTheReceiver() {
            for (int n : SIZES) {
                TreeSet<Integer> set = TreeSet.ofAll(evens(n));
                assertThat(set.rangeFrom(-1)).isSameAs(set);
                assertThat(set.rangeFrom(0)).isSameAs(set);
                assertThat(set.rangeUntil(2 * n)).isSameAs(set);
                assertThat(set.rangeUntil(2 * n - 1)).isSameAs(set);
                assertThat(set.rangeTo(2 * n - 2)).isSameAs(set);
                assertThat(set.rangeFromUntil(0, 2 * n - 1)).isSameAs(set);
                assertThat(set.rangeFromUntil(-5, 2 * n + 5)).isSameAs(set);
            }
        }

        @Test
        void aRangeSharesTheRestOfTheTree() {
            Random random = new Random(SEED + 2);
            for (int n : new int[] {64, 1024, 1025}) {
                for (TreeSet<Integer> set : sets(NATURAL, n, random)) {
                    RedBlackTree<Integer> source = tree(set);
                    // a red-black tree of n nodes is at most 2 log2(n + 1) high, and a cut joins once per level
                    int height = 2 * (32 - Integer.numberOfLeadingZeros(n + 1));
                    for (int b : bounds(n, random)) {
                        for (TreeSet<Integer> result : java.util.List.of(
                                set.rangeFrom(b),
                                set.rangeUntil(b),
                                set.rangeTo(b),
                                set.rangeFromUntil(b, b + n / 2),
                                set.rangeFromUntil(b - n / 3, b))) {
                            assertThat(newNodes(tree(result), source))
                                    .as("n=%s, bound=%s", n, b)
                                    .isLessThanOrEqualTo(4 * height);
                        }
                    }
                }
            }
        }

        @Test
        void aStartAfterTheEndGivesAnEmptyRange() {
            TreeSet<Integer> set = TreeSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
            assertThat(set.rangeFromUntil(5, 2)).isEmpty();
            assertThat(set.rangeFromUntil(5, 5)).isEmpty();
            assertThat(set.rangeFromUntil(100, -100)).isEmpty();
            TreeSet<Integer> reversed = TreeSet.ofAll(REVERSED, set);
            // in the reversed order 2 comes after 5
            assertThat(reversed.rangeFromUntil(2, 5)).isEmpty();
            assertThat(reversed.rangeFromUntil(5, 2)).containsExactly(5, 4, 3);
            assertThat(reversed.rangeFromUntil(5, 2).comparator()).isSameAs(REVERSED);
        }

        @Test
        void theBoundsAreComparedWithTheComparatorAndTheStoredElementsKept() {
            // values above the Integer cache, so that equal numbers are distinct objects
            java.util.List<Integer> stored = java.util.List.of(1000, 2000, 3000);
            TreeSet<Integer> set = TreeSet.ofAll(MODULO, stored);
            Integer probe = 2000 + M;
            assertThat(set.minAfter(probe).get()).isSameAs(stored.get(1));
            assertThat(set.maxBefore(probe + 1).get()).isSameAs(stored.get(1));
            assertThat(set.maxBefore(probe)).isEqualTo(Option.some(1000));
            assertThat(set.rangeFrom(probe).head()).isSameAs(stored.get(1));
            assertThat(set.rangeTo(probe).last()).isSameAs(stored.get(1));
            assertThat(set.rangeUntil(probe)).containsExactly(1000);
            assertThat(set.iteratorFrom(probe).next()).isSameAs(stored.get(1));
        }

        @Test
        void aCustomComparatorOrdersTheRanges() {
            Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
            TreeSet<String> words = TreeSet.of(caseInsensitive, "apple", "Banana", "cherry", "Date", "elder");
            assertThat(words.rangeFrom("b")).containsExactly("Banana", "cherry", "Date", "elder");
            assertThat(words.rangeUntil("D")).containsExactly("apple", "Banana", "cherry");
            assertThat(words.rangeTo("date")).containsExactly("apple", "Banana", "cherry", "Date");
            assertThat(words.rangeFromUntil("BANANA", "e")).containsExactly("Banana", "cherry", "Date");
            assertThat(words.minAfter("c")).isEqualTo(Option.some("cherry"));
            assertThat(words.maxBefore("CHERRY")).isEqualTo(Option.some("Banana"));
            assertThat(list(words.iteratorFrom("d"))).containsExactly("Date", "elder");
        }

        @Test
        void theTwoBoundFormIsCalledOnTheInstanceNotTheStaticRangeFactories() {
            TreeSet<Integer> numbers = TreeSet.rangeClosed(1, 9);
            TreeSet<Integer> fromUntil = numbers.rangeFromUntil(3, 6);
            assertThat(fromUntil).containsExactly(3, 4, 5);
            TreeSet<Character> letters = TreeSet.rangeClosed('a', 'z');
            TreeSet<Character> someLetters = letters.rangeFromUntil('x', 'z');
            assertThat(someLetters).containsExactly('x', 'y');
            TreeMap<Integer, String> map = TreeMap.of(1, "a", 2, "b", 3, "c");
            assertThat(map.rangeFromUntil(2, 3)).containsExactly(Tuple.of(2, "b"));
        }

        @Test
        void theEmptySetHasEmptyRanges() {
            TreeSet<Integer> empty = TreeSet.empty();
            assertThat(empty.rangeFrom(1)).isSameAs(empty);
            assertThat(empty.rangeUntil(1)).isSameAs(empty);
            assertThat(empty.rangeTo(1)).isSameAs(empty);
            assertThat(empty.rangeFromUntil(1, 2)).isSameAs(empty);
            assertThat(empty.minAfter(1)).isEqualTo(Option.none());
            assertThat(empty.maxBefore(1)).isEqualTo(Option.none());
            assertThat(empty.iteratorFrom(1).hasNext()).isFalse();
        }

        @Test
        void theIteratorStopsAtTheEnd() {
            TreeSet<Integer> set = TreeSet.of(1, 3, 5);
            java.util.Iterator<Integer> iterator = set.iteratorFrom(2);
            assertThat(iterator.next()).isEqualTo(3);
            assertThat(iterator.next()).isEqualTo(5);
            assertThat(iterator.hasNext()).isFalse();
            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
            assertThat(set.iteratorFrom(6).hasNext()).isFalse();
            assertThatThrownBy(() -> set.iteratorFrom(6).next()).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void aNullBoundIsRejected() {
            for (TreeSet<Integer> set : java.util.List.of(TreeSet.<Integer>empty(), TreeSet.of(1, 2))) {
                assertThatThrownBy(() -> set.rangeFrom(null)).hasMessage("from is null");
                assertThatThrownBy(() -> set.rangeUntil(null)).hasMessage("until is null");
                assertThatThrownBy(() -> set.rangeTo(null)).hasMessage("to is null");
                assertThatThrownBy(() -> set.rangeFromUntil(null, 1)).hasMessage("from is null");
                assertThatThrownBy(() -> set.rangeFromUntil(1, null)).hasMessage("until is null");
                assertThatThrownBy(() -> set.minAfter(null)).hasMessage("element is null");
                assertThatThrownBy(() -> set.maxBefore(null)).hasMessage("element is null");
                assertThatThrownBy(() -> set.iteratorFrom(null)).hasMessage("start is null");
            }
        }
    }

    // -- TreeMap against java.util.TreeMap

    @Nested
    class Maps {

        @Test
        void oneBoundRangesMatchTheModel() {
            Random random = new Random(SEED + 3);
            for (Comparator<Integer> order : orders()) {
                for (int n : SIZES) {
                    for (TreeSet<Integer> keys : sets(order, n, random)) {
                        TreeMap<Integer, String> map = mapOf(keys);
                        java.util.List<Tuple2<Integer, String>> before = list(map);
                        java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(order);
                        before.forEach(e -> model.put(e._1(), e._2()));
                        for (int b : bounds(n, random)) {
                            String context = "n=" + n + ", bound=" + b + ", order=" + order;
                            TreeMap<Integer, String> from = map.rangeFrom(b);
                            TreeMap<Integer, String> until = map.rangeUntil(b);
                            TreeMap<Integer, String> to = map.rangeTo(b);
                            assertThat(list(from)).as(context).isEqualTo(entries(model.tailMap(b, true)));
                            assertThat(list(until)).as(context).isEqualTo(entries(model.headMap(b, false)));
                            assertThat(list(to)).as(context).isEqualTo(entries(model.headMap(b, true)));
                            assertThat(map.minAfter(b)).as(context).isEqualTo(entry(model.ceilingEntry(b)));
                            assertThat(map.maxBefore(b)).as(context).isEqualTo(entry(model.lowerEntry(b)));
                            assertThat(list(map.iteratorFrom(b)))
                                    .as(context)
                                    .isEqualTo(entries(model.tailMap(b, true)));
                            for (TreeMap<Integer, String> result : java.util.List.of(from, until, to)) {
                                RedBlackTreeValidity.assertValid(tree(result));
                                assertThat(result.comparator()).isSameAs(map.comparator());
                                assertThat(result.size()).isEqualTo(list(result).size());
                            }
                        }
                        assertThat(list(map)).isEqualTo(before);
                        RedBlackTreeValidity.assertValid(tree(map));
                    }
                }
            }
        }

        @Test
        void twoBoundRangesMatchTheModel() {
            Random random = new Random(SEED + 4);
            for (Comparator<Integer> order : orders()) {
                for (int n : SIZES) {
                    for (TreeSet<Integer> keys : sets(order, n, random)) {
                        TreeMap<Integer, String> map = mapOf(keys);
                        java.util.List<Tuple2<Integer, String>> before = list(map);
                        java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(order);
                        before.forEach(e -> model.put(e._1(), e._2()));
                        for (int[] pair : boundPairs(n, random)) {
                            int from = pair[0];
                            int until = pair[1];
                            String context = "n=" + n + ", from=" + from + ", until=" + until + ", order=" + order;
                            TreeMap<Integer, String> result = map.rangeFromUntil(from, until);
                            java.util.List<Tuple2<Integer, String>> expected = order.compare(from, until) > 0
                                    ? java.util.List.of()
                                    : entries(model.subMap(from, true, until, false));
                            assertThat(list(result)).as(context).isEqualTo(expected);
                            assertThat(result.size()).as(context).isEqualTo(expected.size());
                            RedBlackTreeValidity.assertValid(tree(result));
                        }
                        assertThat(list(map)).isEqualTo(before);
                    }
                }
            }
        }

        @Test
        void aRangeThatCutsNothingIsTheReceiverAndTheRestIsShared() {
            Random random = new Random(SEED + 5);
            for (int n : SIZES) {
                TreeMap<Integer, String> map = mapOf(TreeSet.ofAll(evens(n)));
                assertThat(map.rangeFrom(-1)).isSameAs(map);
                assertThat(map.rangeUntil(2 * n)).isSameAs(map);
                assertThat(map.rangeTo(2 * n - 2)).isSameAs(map);
                assertThat(map.rangeFromUntil(0, 2 * n - 1)).isSameAs(map);
                int height = 2 * (32 - Integer.numberOfLeadingZeros(n + 1));
                for (int b : bounds(n, random)) {
                    assertThat(newNodes(tree(map.rangeFromUntil(b, b + n / 2)), tree(map)))
                            .isLessThanOrEqualTo(4 * height);
                }
            }
        }

        @Test
        void minAfterAndMaxBeforeGiveTheStoredEntry() {
            TreeMap<Integer, String> map = TreeMap.of(MODULO, 1000, "a", 2000, "b", 3000, "c");
            Tuple2<Integer, String> stored = list(map).get(1);
            assertThat(map.minAfter(2000 + M).get()).isSameAs(stored);
            assertThat(map.maxBefore(2001 + M).get()).isSameAs(stored);
            assertThat(map.maxBefore(2000 + M)).isEqualTo(Option.some(Tuple.of(1000, "a")));
            assertThat(map.iteratorFrom(2000 + M).next()).isSameAs(stored);
        }

        @Test
        void aCustomComparatorOrdersTheRanges() {
            TreeMap<Integer, String> map = TreeMap.of(REVERSED, 1, "a", 2, "b", 3, "c", 4, "d");
            assertThat(map.rangeFrom(3)).containsExactly(Tuple.of(3, "c"), Tuple.of(2, "b"), Tuple.of(1, "a"));
            assertThat(map.rangeUntil(2)).containsExactly(Tuple.of(4, "d"), Tuple.of(3, "c"));
            assertThat(map.rangeTo(3)).containsExactly(Tuple.of(4, "d"), Tuple.of(3, "c"));
            assertThat(map.rangeFromUntil(3, 1)).containsExactly(Tuple.of(3, "c"), Tuple.of(2, "b"));
            assertThat(map.rangeFromUntil(1, 3)).isEmpty();
            assertThat(map.minAfter(0)).isEqualTo(Option.none());
            assertThat(map.maxBefore(0)).isEqualTo(Option.some(Tuple.of(1, "a")));
        }

        @Test
        void aNullBoundIsRejected() {
            for (TreeMap<Integer, String> map :
                    java.util.List.of(TreeMap.<Integer, String>empty(), TreeMap.of(1, "a"))) {
                assertThatThrownBy(() -> map.rangeFrom(null)).hasMessage("from is null");
                assertThatThrownBy(() -> map.rangeUntil(null)).hasMessage("until is null");
                assertThatThrownBy(() -> map.rangeTo(null)).hasMessage("to is null");
                assertThatThrownBy(() -> map.rangeFromUntil(null, 1)).hasMessage("from is null");
                assertThatThrownBy(() -> map.rangeFromUntil(1, null)).hasMessage("until is null");
                assertThatThrownBy(() -> map.minAfter(null)).hasMessage("key is null");
                assertThatThrownBy(() -> map.maxBefore(null)).hasMessage("key is null");
                assertThatThrownBy(() -> map.iteratorFrom(null)).hasMessage("start is null");
            }
        }
    }

    // -- the non-empty wrappers delegate, returning the plain types

    @Nested
    class NonEmpty {

        @Test
        void theSortedSetRangesAreThoseOfTheWrappedSet() {
            NonEmptySortedSet<Integer> set = NonEmptySortedSet.of(1, 3, 5, 7);
            TreeSet<Integer> from = set.rangeFrom(3);
            TreeSet<Integer> until = set.rangeUntil(3);
            TreeSet<Integer> to = set.rangeTo(3);
            TreeSet<Integer> fromUntil = set.rangeFromUntil(2, 6);
            assertThat(from).containsExactly(3, 5, 7);
            assertThat(until).containsExactly(1);
            assertThat(to).containsExactly(1, 3);
            assertThat(fromUntil).containsExactly(3, 5);
            assertThat(set.rangeFrom(8)).isEmpty();
            assertThat(set.rangeFrom(0)).isSameAs(set.toSortedSet());
            assertThat(set.minAfter(4)).isEqualTo(Option.some(5));
            assertThat(set.maxBefore(3)).isEqualTo(Option.some(1));
            assertThat(set.maxBefore(1)).isEqualTo(Option.none());
            assertThat(list(set.iteratorFrom(4))).containsExactly(5, 7);
            assertThatThrownBy(() -> set.rangeFrom(null)).hasMessage("from is null");
        }

        @Test
        void theSortedMapRangesAreThoseOfTheWrappedMap() {
            NonEmptySortedMap<Integer, String> map =
                    NonEmptySortedMap.of(Tuple.of(1, "a"), Tuple.of(3, "c"), Tuple.of(5, "e"));
            TreeMap<Integer, String> from = map.rangeFrom(3);
            TreeMap<Integer, String> until = map.rangeUntil(3);
            TreeMap<Integer, String> to = map.rangeTo(3);
            TreeMap<Integer, String> fromUntil = map.rangeFromUntil(2, 5);
            assertThat(from).containsExactly(Tuple.of(3, "c"), Tuple.of(5, "e"));
            assertThat(until).containsExactly(Tuple.of(1, "a"));
            assertThat(to).containsExactly(Tuple.of(1, "a"), Tuple.of(3, "c"));
            assertThat(fromUntil).containsExactly(Tuple.of(3, "c"));
            assertThat(map.rangeFromUntil(5, 2)).isEmpty();
            assertThat(map.minAfter(4)).isEqualTo(Option.some(Tuple.of(5, "e")));
            assertThat(map.maxBefore(4)).isEqualTo(Option.some(Tuple.of(3, "c")));
            assertThat(list(map.iteratorFrom(2))).containsExactly(Tuple.of(3, "c"), Tuple.of(5, "e"));
            assertThatThrownBy(() -> map.minAfter(null)).hasMessage("key is null");
        }
    }
}
