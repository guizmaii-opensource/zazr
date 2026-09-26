package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The filter family of TreeSet and TreeMap (a walk that keeps the untouched subtrees) and the bulk operations that go
 * through the tree builder ({@code TreeSet.addAll}, {@code flatten}, {@code of}, {@code tabulate}, {@code fill},
 * {@code TreeMap.retainAll}): the same elements as the one-at-a-time path, the same object kept of equal ones, a valid
 * tree after every operation, the receiver itself when nothing changes, and the receiver left intact.
 */
public class TreeFilterAndBulkTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 3, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025 };

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
            final java.lang.reflect.Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(receiver);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void assertValid(TreeSet<?> set) {
        RedBlackTreeValidity.assertValid(tree(set));
    }

    private static void assertValid(TreeMap<?, ?> map) {
        RedBlackTreeValidity.assertValid(tree(map));
    }

    private static IdentityHashMap<Object, Boolean> nodes(RedBlackTree<?> tree) {
        final IdentityHashMap<Object, Boolean> nodes = new IdentityHashMap<>();
        final java.util.ArrayDeque<RedBlackTree<?>> pending = new java.util.ArrayDeque<>();
        pending.push(tree);
        while (!pending.isEmpty()) {
            final RedBlackTree<?> next = pending.pop();
            if (!next.isEmpty()) {
                nodes.put(next, true);
                pending.push(next.left());
                pending.push(next.right());
            }
        }
        return nodes;
    }

    // the number of nodes of the tree of `result` shared with the tree of `source`
    private static int sharedNodes(RedBlackTree<?> result, RedBlackTree<?> source) {
        final IdentityHashMap<Object, Boolean> old = nodes(source);
        int shared = 0;
        for (Object node : nodes(result).keySet()) {
            if (old.containsKey(node)) {
                shared++;
            }
        }
        return shared;
    }

    private static <T> java.util.List<T> list(Iterable<T> iterable) {
        final java.util.List<T> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    // the same elements, the same objects, in the same order
    private static <T> void assertSameElements(Iterable<T> actual, Iterable<T> expected) {
        final java.util.List<T> a = list(actual);
        final java.util.List<T> e = list(expected);
        assertThat(a).hasSameSizeAs(e);
        for (int i = 0; i < e.size(); i++) {
            assertThat(a.get(i)).as("element %s", i).isSameAs(e.get(i));
        }
    }

    // an Iterable whose iterator can be asked for once only
    private static <T> Iterable<T> oneShot(java.util.List<T> elements) {
        final boolean[] used = { false };
        return () -> {
            assertThat(used[0]).as("iterated twice").isFalse();
            used[0] = true;
            return elements.iterator();
        };
    }

    private static TreeSet<Integer> randomSet(Comparator<Integer> order, int size, Random random) {
        TreeSet<Integer> set = TreeSet.empty(order);
        while (set.size() < size) {
            set = set.add(random.nextInt(4 * size + 1) - 2 * size);
        }
        // some deletions, so that the shape is not only the one of insertions
        for (Integer element : list(set).subList(0, size / 3)) {
            set = set.remove(element);
        }
        while (set.size() < size) {
            set = set.add(random.nextInt(4 * size + 1) - 2 * size);
        }
        return set;
    }

    private static TreeMap<Integer, String> randomMap(Comparator<Integer> order, int size, Random random) {
        TreeMap<Integer, String> map = TreeMap.empty(order);
        for (Integer key : randomSet(order, size, random)) {
            map = map.put(key, "v" + Math.floorMod(key, 7));
        }
        return map;
    }

    private static java.util.List<Predicate<Integer>> predicates(Iterable<Integer> elements, Random random) {
        final java.util.List<Integer> all = list(elements);
        final int middle = all.isEmpty() ? 0 : all.get(all.size() / 2);
        final java.util.Set<Integer> chosen = new java.util.HashSet<>();
        for (Integer element : all) {
            if (random.nextInt(10) == 0) {
                chosen.add(element);
            }
        }
        final int first = all.isEmpty() ? 0 : all.getFirst();
        return java.util.List.of(x -> true, x -> false, x -> (x & 1) == 0, x -> x < middle, x -> x >= middle,
                chosen::contains, x -> !chosen.contains(x), x -> x != first);
    }

    // -- TreeSet: filter, reject, partition, removeAll, retainAll

    @Test
    public void shouldFilterATreeSetAsRebuildingTheKeptElementsWould() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeSet<Integer> set = randomSet(order, size, random);
                final java.util.List<Integer> before = list(set);
                for (Predicate<Integer> predicate : predicates(set, random)) {
                    final TreeSet<Integer> rebuilt = TreeSet.ofAll(order, before.stream().filter(predicate).toList());
                    final TreeSet<Integer> rejected = TreeSet.ofAll(order, before.stream().filter(predicate.negate()).toList());

                    final java.util.List<Integer> calls = new ArrayList<>();
                    final TreeSet<Integer> filtered = set.filter(x -> {
                        calls.add(x);
                        return predicate.test(x);
                    });
                    assertThat(calls).as("once per element, in order").isEqualTo(before);
                    assertValid(filtered);
                    assertSameElements(filtered, rebuilt);
                    assertThat(filtered.comparator()).isSameAs(set.comparator());

                    final TreeSet<Integer> rejecting = set.reject(predicate.negate());
                    assertValid(rejecting);
                    assertSameElements(rejecting, rebuilt);

                    final Tuple2<TreeSet<Integer>, TreeSet<Integer>> partition = set.partition(predicate);
                    assertValid(partition._1());
                    assertValid(partition._2());
                    assertSameElements(partition._1(), rebuilt);
                    assertSameElements(partition._2(), rejected);

                    final java.util.List<Integer> removed = before.stream().filter(predicate.negate()).toList();
                    final TreeSet<Integer> removeAll = set.removeAll(removed);
                    assertValid(removeAll);
                    assertSameElements(removeAll, rebuilt);
                    final TreeSet<Integer> retainAll = set.retainAll(oneShot(before.stream().filter(predicate).toList()));
                    assertValid(retainAll);
                    assertSameElements(retainAll, rebuilt);

                    // persistence
                    assertValid(set);
                    assertThat(list(set)).isEqualTo(before);
                }
            }
        }
    }

    @Test
    public void shouldReturnTheTreeSetItselfWhenNothingIsRemoved() {
        final Random random = new Random(SEED + 1);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeSet<Integer> set = randomSet(order, size, random);
                assertThat(set.filter(x -> true)).isSameAs(set);
                assertThat(set.reject(x -> false)).isSameAs(set);
                assertThat(set.partition(x -> true)._1()).isSameAs(set);
                assertThat(set.partition(x -> false)._2()).isSameAs(set);
                assertThat(set.partition(x -> true)._2()).isEmpty();
                assertThat(set.removeAll(java.util.List.of(Integer.MIN_VALUE))).isSameAs(set);
                assertThat(set.retainAll(list(set))).isSameAs(set);
            }
        }
    }

    @Test
    public void shouldShareTheUntouchedSubtreesOfATreeSet() {
        final Random random = new Random(SEED + 2);
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            final TreeSet<Integer> set = randomSet(order, 1025, random);
            final Integer first = set.head();
            final TreeSet<Integer> withoutFirst = set.filter(x -> !x.equals(first));
            // one element removed: all but a few nodes per level are the source's own
            assertThat(sharedNodes(tree(withoutFirst), tree(set))).isGreaterThanOrEqualTo(1024 - 3 * 25);
            final Integer middle = list(set).get(512);
            final Tuple2<TreeSet<Integer>, TreeSet<Integer>> halves = set.partition(x -> order.compare(x, middle) < 0);
            assertThat(sharedNodes(tree(halves._1()), tree(set)) + sharedNodes(tree(halves._2()), tree(set)))
                    .isGreaterThanOrEqualTo(1025 - 6 * 25);
        }
    }

    @Test
    public void shouldPassTheNullChecksOfTheTreeSetFilters() {
        final TreeSet<Integer> set = TreeSet.of(1, 2, 3);
        assertThatThrownBy(() -> set.filter(null)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
        assertThatThrownBy(() -> set.reject(null)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
        assertThatThrownBy(() -> set.partition(null)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
        assertThatThrownBy(() -> TreeSet.<Integer> empty().partition(null)).isInstanceOf(NullPointerException.class);
    }

    // -- TreeMap: the filter family and partition

    private static java.util.List<Tuple2<Integer, String>> entries(TreeMap<Integer, String> map) {
        return list(map);
    }

    private static void checkMapFilter(TreeMap<Integer, String> map, Comparator<Integer> order,
            Function<TreeMap<Integer, String>, TreeMap<Integer, String>> operation, Predicate<Tuple2<Integer, String>> kept) {
        final java.util.List<Tuple2<Integer, String>> before = entries(map);
        final TreeMap<Integer, String> result = operation.apply(map);
        assertValid(result);
        assertSameElements(result, TreeMap.ofEntries(order, before.stream().filter(kept).toList()));
        assertThat(result.comparator()).isSameAs(map.comparator());
        if (before.stream().allMatch(kept)) {
            assertThat(result).isSameAs(map);
        }
        assertValid(map);
        assertThat(entries(map)).isEqualTo(before);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldFilterATreeMapAsRebuildingTheKeptEntriesWould() {
        final Random random = new Random(SEED + 3);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeMap<Integer, String> map = randomMap(order, size, random);
                for (Predicate<Integer> keys : predicates(map.keySet(), random)) {
                    final Predicate<String> values = v -> v.equals("v1") || v.equals("v4");
                    checkMapFilter(map, order, m -> m.filter((k, v) -> keys.test(k)), e -> keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.reject((k, v) -> keys.test(k)), e -> !keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.filter(e -> keys.test(e._1())), e -> keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.reject(e -> keys.test(e._1())), e -> !keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.filterKeys(keys), e -> keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.rejectKeys(keys), e -> !keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.filterValues(values), e -> values.test(e._2()));
                    checkMapFilter(map, order, m -> m.rejectValues(values), e -> !values.test(e._2()));
                    checkMapFilter(map, order, m -> m.removeAll((k, v) -> keys.test(k)), e -> !keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.removeKeys(keys), e -> !keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.removeValues(values), e -> !values.test(e._2()));
                    checkMapFilter(map, order, m -> m.partition(e -> keys.test(e._1()))._1(), e -> keys.test(e._1()));
                    checkMapFilter(map, order, m -> m.partition(e -> keys.test(e._1()))._2(), e -> !keys.test(e._1()));
                }
            }
        }
    }

    @Test
    public void shouldCallTheTreeMapPredicateOncePerEntryInKeyOrder() {
        final Random random = new Random(SEED + 4);
        for (Comparator<Integer> order : orders()) {
            final TreeMap<Integer, String> map = randomMap(order, 100, random);
            final java.util.List<Integer> keys = list(map.keySet());
            final java.util.List<Integer> calls = new ArrayList<>();
            map.filter((k, v) -> calls.add(k) && (k & 1) == 0);
            assertThat(calls).isEqualTo(keys);
            calls.clear();
            map.partition(e -> calls.add(e._1()) && (e._1() & 1) == 0);
            assertThat(calls).isEqualTo(keys);
        }
    }

    @Test
    public void shouldShareTheUntouchedSubtreesOfATreeMap() {
        final TreeMap<Integer, String> map = randomMap(NATURAL, 1025, new Random(SEED + 5));
        final Integer last = map.last()._1();
        final TreeMap<Integer, String> withoutLast = map.filterKeys(k -> !k.equals(last));
        assertThat(sharedNodes(tree(withoutLast), tree(map))).isGreaterThanOrEqualTo(1024 - 3 * 25);
    }

    @Test
    public void shouldPassTheNullChecksOfTheTreeMapFilters() {
        final TreeMap<Integer, String> map = TreeMap.of(1, "a");
        for (Function<TreeMap<Integer, String>, Object> operation : java.util.List.<Function<TreeMap<Integer, String>, Object>> of(
                m -> m.filter((java.util.function.BiPredicate<Integer, String>) null),
                m -> m.reject((java.util.function.BiPredicate<Integer, String>) null),
                m -> m.filter((Predicate<Tuple2<Integer, String>>) null),
                m -> m.reject((Predicate<Tuple2<Integer, String>>) null),
                m -> m.filterKeys(null), m -> m.rejectKeys(null), m -> m.filterValues(null), m -> m.rejectValues(null),
                m -> m.partition(null))) {
            assertThatThrownBy(() -> operation.apply(map)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
        }
    }

    // -- TreeSet.addAll: of equal elements, the one already present, or else the first given, is kept

    private static <T> TreeSet<T> addedOneByOne(TreeSet<T> set, Iterable<? extends T> elements) {
        TreeSet<T> result = set;
        for (T element : elements) {
            result = result.add(element);
        }
        return result;
    }

    @Test
    public void shouldAddAllAsAddingOneByOneWould() {
        final Random random = new Random(SEED + 6);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                for (int count : new int[] { 0, 1, 2, size / 2, size, size + 1, 2 * size + 3 }) {
                    final TreeSet<Integer> set = randomSet(order, size, random);
                    final java.util.List<Integer> before = list(set);
                    final java.util.List<Integer> given = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        // new objects equal to present elements under every order, and repeated ones among the given
                        given.add(Integer.valueOf(random.nextInt(4 * size + 3) - 2 * size - 1 + (order == MODULO ? M : 0)));
                        if (random.nextInt(4) == 0) {
                            given.add(Integer.valueOf(given.getLast() + (order == MODULO ? 2 * M : 0)));
                        }
                    }
                    final TreeSet<Integer> expected = addedOneByOne(set, given);
                    for (Iterable<Integer> input : java.util.List.<Iterable<Integer>> of(given, Vector.ofAll(given),
                            oneShot(given), List.ofAll(given))) {
                        final TreeSet<Integer> added = set.addAll(input);
                        assertValid(added);
                        assertSameElements(added, expected);
                        if (expected.size() == set.size()) {
                            assertThat(added).isSameAs(set);
                        }
                    }
                    final TreeSet<Integer> fromEmpty = TreeSet.<Integer> empty(order).addAll(given);
                    assertValid(fromEmpty);
                    assertSameElements(fromEmpty, addedOneByOne(TreeSet.empty(order), given));
                    assertValid(set);
                    assertThat(list(set)).isEqualTo(before);
                }
            }
        }
    }

    @Test
    public void shouldKeepThePresentElementThenTheFirstGivenOnAddAll() {
        final Integer present = M + 5;
        final Integer firstGiven = 2 * M + 5;
        final Integer secondGiven = 3 * M + 5;
        final TreeSet<Integer> set = TreeSet.of(MODULO, present, 7);
        final java.util.List<Integer> given = Arrays.asList(firstGiven, 11, secondGiven, 12, 13);
        final TreeSet<Integer> added = set.addAll(given);
        assertThat(list(added).get(0)).isSameAs(present);
        final TreeSet<Integer> fromEmpty = TreeSet.<Integer> empty(MODULO).addAll(given);
        assertThat(list(fromEmpty).get(0)).isSameAs(firstGiven);
        assertThat(set.addAll(Arrays.asList(firstGiven, M + 7, 2 * M + 7))).isSameAs(set);
    }

    @Test
    public void shouldRejectANullElementOnAddAll() {
        for (TreeSet<Integer> set : java.util.List.of(TreeSet.<Integer> empty(), TreeSet.of(1, 2, 3))) {
            assertThatThrownBy(() -> set.addAll(Arrays.asList(1, null))).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> set.addAll(Arrays.asList(4, 5, 6, null))).isInstanceOf(NullPointerException.class);
        }
        assertThatThrownBy(() -> TreeSet.of(1).addAll(null)).isInstanceOf(NullPointerException.class).hasMessage("elements is null");
    }

    // -- flatten, of, tabulate, fill: of equal elements, the last one is kept

    private static <T> TreeSet<T> insertedKeepingLast(Comparator<? super T> order, Iterable<? extends T> elements) {
        TreeSet<T> result = TreeSet.empty(order);
        for (T element : elements) {
            result = result.remove(element).add(element);
        }
        return result;
    }

    @Test
    public void shouldFlattenAsInsertingOneByOneWould() {
        final Random random = new Random(SEED + 7);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final java.util.List<java.util.List<Integer>> nested = new ArrayList<>();
                final java.util.List<Integer> all = new ArrayList<>();
                for (int i = 0; i < 1 + random.nextInt(5); i++) {
                    final java.util.List<Integer> inner = new ArrayList<>();
                    for (int j = 0; j < size; j++) {
                        inner.add(Integer.valueOf(random.nextInt(2 * size + 1) + M * random.nextInt(3)));
                    }
                    nested.add(inner);
                    all.addAll(inner);
                }
                final TreeSet<Integer> expected = insertedKeepingLast(order, all);
                final TreeSet<Integer> flattened = TreeSet.flatten(order, nested);
                assertValid(flattened);
                assertSameElements(flattened, expected);
                final java.util.List<Iterable<Integer>> oneShots = new ArrayList<>();
                nested.forEach(inner -> oneShots.add(oneShot(inner)));
                final TreeSet<Integer> fromOneShots = TreeSet.flatten(order, oneShot(oneShots));
                assertValid(fromOneShots);
                assertSameElements(fromOneShots, expected);
            }
        }
        assertThat(TreeSet.flatten(java.util.List.<java.util.List<Integer>> of())).isEmpty();
        assertThatThrownBy(() -> TreeSet.flatten(java.util.List.of(Arrays.asList(1, null))))
                .isInstanceOf(NullPointerException.class).hasMessage("TreeSet.flatten: element is null");
    }

    @Test
    public void shouldBuildOfTabulateAndFillAsInsertingOneByOneWould() {
        final Random random = new Random(SEED + 8);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final Integer[] values = new Integer[size];
                for (int i = 0; i < size; i++) {
                    values[i] = Integer.valueOf(random.nextInt(2 * size + 1) + M * random.nextInt(3));
                }
                final Integer[] copy = values.clone();
                final TreeSet<Integer> expected = insertedKeepingLast(order, Arrays.asList(values));
                final TreeSet<Integer> of = TreeSet.of(order, values);
                assertValid(of);
                assertSameElements(of, expected);
                assertThat(values).as("the argument array is not reordered").containsExactly(copy);
                final TreeSet<Integer> tabulated = TreeSet.tabulate(order, size, i -> values[i]);
                assertValid(tabulated);
                assertSameElements(tabulated, expected);
                final int[] next = { 0 };
                final TreeSet<Integer> filled = TreeSet.fill(order, size, () -> values[next[0]++]);
                assertValid(filled);
                assertSameElements(filled, expected);
            }
        }
        assertThat(TreeSet.<Integer> of()).isEmpty();
        assertThatThrownBy(() -> TreeSet.of(1, null, 2)).isInstanceOf(NullPointerException.class).hasMessage("TreeSet: element is null");
        assertThatThrownBy(() -> TreeSet.tabulate(3, i -> i == 1 ? null : i)).isInstanceOf(NullPointerException.class);
    }

    // -- TreeMap.retainAll: of equal given entries, the last one is kept

    @Test
    public void shouldRetainAllAsInsertingThePresentEntriesOneByOneWould() {
        final Random random = new Random(SEED + 9);
        for (Comparator<Integer> order : orders()) {
            for (int size : SIZES) {
                final TreeMap<Integer, String> map = randomMap(order, size, random);
                final java.util.List<Tuple2<Integer, String>> before = entries(map);
                final java.util.List<Tuple2<Integer, String>> given = new ArrayList<>();
                for (int i = 0; i < size + 2; i++) {
                    final int key = random.nextInt(4 * size + 1) - 2 * size + (order == MODULO ? M * random.nextInt(3) : 0);
                    given.add(Tuple.of(key, "v" + Math.floorMod(key, 7)));
                    if (random.nextInt(3) == 0) {
                        given.add(Tuple.of(key, "other"));
                    }
                }
                TreeMap<Integer, String> expected = TreeMap.empty(order);
                for (Tuple2<Integer, String> entry : given) {
                    if (map.contains(entry)) {
                        expected = expected.put(entry._1(), entry._2());
                    }
                }
                for (Iterable<Tuple2<Integer, String>> input : java.util.List.<Iterable<Tuple2<Integer, String>>> of(given, oneShot(given))) {
                    final TreeMap<Integer, String> retained = map.retainAll(input);
                    assertValid(retained);
                    assertThat(list(retained)).isEqualTo(list(expected));
                    // the keys kept are the given objects, the last given of equal ones
                    final java.util.List<Tuple2<Integer, String>> actual = list(retained);
                    final java.util.List<Tuple2<Integer, String>> wanted = list(expected);
                    for (int i = 0; i < wanted.size(); i++) {
                        assertThat(actual.get(i)._1()).isSameAs(wanted.get(i)._1());
                    }
                }
                assertValid(map);
                assertThat(entries(map)).isEqualTo(before);
            }
        }
        assertThatThrownBy(() -> TreeMap.of(1, "a").retainAll(null)).isInstanceOf(NullPointerException.class);
    }
}
