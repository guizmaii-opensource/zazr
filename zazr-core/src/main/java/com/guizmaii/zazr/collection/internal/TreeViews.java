package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.internal.MapViews.Part;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Node;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.Maps.ABSENT;

/**
 * The unmodifiable {@link NavigableSet} view of a {@link TreeSet} and {@link NavigableMap} view of a {@link TreeMap}.
 * The rules of {@link JavaConverters} hold: O(1) to create, nothing copied, every mutator throws
 * {@link UnsupportedOperationException} ({@code pollFirst}, {@code pollLast}, {@code pollFirstEntry} and
 * {@code pollLastEntry} included, since they remove).
 * <p>
 * A view reads the red-black tree of the persistent value directly, through a {@link TreeRange}: the tree, the key
 * comparator and optional bounds. The sub-views ({@code subSet}, {@code headMap}, ...), the descending views and the
 * key set of a map view share the tree and only carry other bounds or the other direction, so every navigation
 * ({@code ceiling}, {@code floor}, {@code higher}, {@code lower}, {@code first}, {@code last}), {@code contains},
 * {@code get} and {@code size} is O(log n), and an iterator is O(log n) to create and amortized O(1) per step. The
 * bounds follow {@link java.util.TreeMap}: a sub-view of a sub-view must lie within its bounds
 * ({@link IllegalArgumentException} otherwise), a lower bound greater than the upper bound is an
 * {@link IllegalArgumentException}, and a key the comparator cannot compare throws what the comparator throws, a
 * {@code null} key under the natural order a {@link NullPointerException}. {@code comparator()} is {@code null} for
 * the natural order, and a descending view's is {@link Collections#reverseOrder(Comparator)} of it.
 */
public final class TreeViews {

    private TreeViews() {
    }

    public static <T extends @Nullable Object> NavigableSet<T> asJava(TreeSet<T> set, RedBlackTree<T> tree) {
        return new NavigableKeySetView<>(set, new TreeRange<>(tree, tree.comparator(), false), false);
    }

    public static <K extends @Nullable Object, V extends @Nullable Object> NavigableMap<K, V> asJavaMap(TreeMap<K, V> map, RedBlackTree<Tuple2<K, V>> entries,
            Comparator<K> keyComparator) {
        return new NavigableMapView<>(map, new TreeRange<>(entries, keyComparator, true), false);
    }

    /**
     * A red-black tree with the comparator of its keys and optional bounds on them. The elements of the tree are the
     * keys ({@code entries == false}, a {@code TreeSet}) or {@link Tuple2} entries keyed by their first component
     * ({@code entries == true}, a {@code TreeMap}); a lookup compares the probe with the key of a node in place,
     * with no probe entry allocated. The tree never holds {@code null}, so the navigation primitives answer
     * {@code null} for "no such element".
     *
     * @param <E> the element type of the tree
     * @param <K> the key type
     */
    static final class TreeRange<E extends @Nullable Object, K extends @Nullable Object> {

        private final RedBlackTree<E> tree;
        private final Comparator<? super K> comparator;
        private final boolean natural;
        private final boolean entries;
        private final boolean fromStart;
        private final @Nullable K lo;
        private final boolean loInclusive;
        private final boolean toEnd;
        private final @Nullable K hi;
        private final boolean hiInclusive;

        /** The whole tree. */
        TreeRange(RedBlackTree<E> tree, Comparator<? super K> comparator, boolean entries) {
            this(tree, comparator, entries, true, null, true, true, null, true);
        }

        private TreeRange(RedBlackTree<E> tree, Comparator<? super K> comparator, boolean entries,
                          boolean fromStart, @Nullable K lo, boolean loInclusive,
                          boolean toEnd, @Nullable K hi, boolean hiInclusive) {
            this.tree = tree;
            this.comparator = comparator;
            this.natural = comparator instanceof NaturalComparator;
            this.entries = entries;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
            // the bounds are compared even when only one is given: a bound the comparator cannot compare fails now
            if (!fromStart && !toEnd) {
                if (compare(lo, hi) > 0) {
                    throw new IllegalArgumentException("fromKey > toKey");
                }
            } else {
                if (!fromStart) {
                    compare(lo, lo);
                }
                if (!toEnd) {
                    compare(hi, hi);
                }
            }
        }

        private TreeRange<E, K> with(boolean fromStart, @Nullable K lo, boolean loInclusive, boolean toEnd, @Nullable K hi, boolean hiInclusive) {
            return new TreeRange<>(tree, comparator, entries, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        @SuppressWarnings("unchecked")
        K key(E element) {
            return entries ? ((Tuple2<K, ?>) element)._1() : (K) element;
        }

        @SuppressWarnings("unchecked")
        private int compare(@Nullable Object key1, @Nullable Object key2) {
            return comparator.compare((K) key1, (K) key2);
        }

        /** The comparator a view reports: {@code null} for the natural order, reversed for a descending view. */
        @Nullable Comparator<? super K> comparator(boolean descending) {
            final Comparator<? super K> ascending = natural ? null : comparator;
            return descending ? Collections.reverseOrder(ascending) : ascending;
        }

        // -- bounds

        private boolean tooLow(@Nullable Object key) {
            if (!fromStart) {
                final int c = compare(key, lo);
                return c < 0 || (c == 0 && !loInclusive);
            }
            return false;
        }

        private boolean tooHigh(@Nullable Object key) {
            if (!toEnd) {
                final int c = compare(key, hi);
                return c > 0 || (c == 0 && !hiInclusive);
            }
            return false;
        }

        private boolean inRange(@Nullable Object key) {
            return !tooLow(key) && !tooHigh(key);
        }

        private boolean inClosedRange(@Nullable Object key) {
            return (fromStart || compare(key, lo) >= 0) && (toEnd || compare(hi, key) >= 0);
        }

        private boolean inRange(@Nullable Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        // -- the sub-ranges; on a descending view "from" is the high end

        @SuppressWarnings("unchecked")
        TreeRange<E, K> sub(boolean descending, @Nullable Object from, boolean fromInclusive, @Nullable Object to, boolean toInclusive) {
            if (!inRange(from, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(to, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return descending
                   ? with(false, (K) to, toInclusive, false, (K) from, fromInclusive)
                   : with(false, (K) from, fromInclusive, false, (K) to, toInclusive);
        }

        @SuppressWarnings("unchecked")
        TreeRange<E, K> head(boolean descending, @Nullable Object to, boolean inclusive) {
            if (!inRange(to, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return descending
                   ? with(false, (K) to, inclusive, toEnd, hi, hiInclusive)
                   : with(fromStart, lo, loInclusive, false, (K) to, inclusive);
        }

        @SuppressWarnings("unchecked")
        TreeRange<E, K> tail(boolean descending, @Nullable Object from, boolean inclusive) {
            if (!inRange(from, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return descending
                   ? with(fromStart, lo, loInclusive, false, (K) from, inclusive)
                   : with(false, (K) from, inclusive, toEnd, hi, hiInclusive);
        }

        // -- the whole tree, bounds ignored

        private @Nullable E treeFirst() {
            RedBlackTree<E> t = tree;
            E result = null;
            while (t instanceof Node<E> node) {
                result = node.value;
                t = node.left;
            }
            return result;
        }

        private @Nullable E treeLast() {
            RedBlackTree<E> t = tree;
            E result = null;
            while (t instanceof Node<E> node) {
                result = node.value;
                t = node.right;
            }
            return result;
        }

        /** The least element whose key is {@code >= key} ({@code > key} when {@code strict}). */
        private @Nullable E treeCeiling(@Nullable Object key, boolean strict) {
            RedBlackTree<E> t = tree;
            E result = null;
            while (t instanceof Node<E> node) {
                final int c = compare(key, key(node.value));
                if (c == 0 && !strict) {
                    return node.value;
                } else if (c < 0) {
                    result = node.value;
                    t = node.left;
                } else {
                    t = node.right;
                }
            }
            return result;
        }

        /** The greatest element whose key is {@code <= key} ({@code < key} when {@code strict}). */
        private @Nullable E treeFloor(@Nullable Object key, boolean strict) {
            RedBlackTree<E> t = tree;
            E result = null;
            while (t instanceof Node<E> node) {
                final int c = compare(key, key(node.value));
                if (c == 0 && !strict) {
                    return node.value;
                } else if (c > 0) {
                    result = node.value;
                    t = node.right;
                } else {
                    t = node.left;
                }
            }
            return result;
        }

        /** The number of elements whose key is {@code < key} ({@code <= key} when {@code inclusive}). */
        private int rank(@Nullable Object key, boolean inclusive) {
            RedBlackTree<E> t = tree;
            int rank = 0;
            while (t instanceof Node<E> node) {
                final int c = compare(key, key(node.value));
                if (c < 0 || (c == 0 && !inclusive)) {
                    t = node.left;
                } else {
                    rank += node.left.size() + 1;
                    t = node.right;
                }
            }
            return rank;
        }

        // -- within the bounds, in ascending order

        @Nullable E lowest() {
            final E e = fromStart ? treeFirst() : treeCeiling(lo, !loInclusive);
            return e == null || tooHigh(key(e)) ? null : e;
        }

        @Nullable E highest() {
            final E e = toEnd ? treeLast() : treeFloor(hi, !hiInclusive);
            return e == null || tooLow(key(e)) ? null : e;
        }

        @Nullable E ceiling(@Nullable Object key) {
            if (tooLow(key)) {
                return lowest();
            }
            final E e = treeCeiling(key, false);
            return e == null || tooHigh(key(e)) ? null : e;
        }

        @Nullable E higher(@Nullable Object key) {
            if (tooLow(key)) {
                return lowest();
            }
            final E e = treeCeiling(key, true);
            return e == null || tooHigh(key(e)) ? null : e;
        }

        @Nullable E floor(@Nullable Object key) {
            if (tooHigh(key)) {
                return highest();
            }
            final E e = treeFloor(key, false);
            return e == null || tooLow(key(e)) ? null : e;
        }

        @Nullable E lower(@Nullable Object key) {
            if (tooHigh(key)) {
                return highest();
            }
            final E e = treeFloor(key, true);
            return e == null || tooLow(key(e)) ? null : e;
        }

        /**
         * The element whose key equals {@code key}, or {@code null}. As {@link java.util.TreeMap#get} does, the
         * natural order rejects a {@code null} or non-{@link Comparable} key even when there is nothing to compare it
         * with.
         */
        @Nullable E find(@Nullable Object key) {
            if (natural) {
                Objects.requireNonNull(key, "key is null");
                if (!(key instanceof Comparable<?>)) {
                    throw new ClassCastException(key.getClass().getName() + " cannot be cast to java.lang.Comparable");
                }
            }
            if (!inRange(key)) {
                return null;
            }
            RedBlackTree<E> t = tree;
            while (t instanceof Node<E> node) {
                final int c = compare(key, key(node.value));
                if (c == 0) {
                    return node.value;
                }
                t = c < 0 ? node.left : node.right;
            }
            return null;
        }

        int size() {
            if (fromStart && toEnd) {
                return tree.size();
            }
            final int upper = toEnd ? tree.size() : rank(hi, hiInclusive);
            final int lower = fromStart ? 0 : rank(lo, !loInclusive);
            return Math.max(0, upper - lower);
        }

        boolean isEmpty() {
            return lowest() == null;
        }

        <X extends @Nullable Object> java.util.Iterator<X> iterator(boolean descending, Part part) {
            return new RangeIterator<>(this, descending, part);
        }

        @SuppressWarnings("unchecked")
        <X extends @Nullable Object> X export(E element, Part part) {
            if (!entries) {
                return (X) element;
            }
            final Tuple2<K, ?> entry = (Tuple2<K, ?>) element;
            return (X) switch (part) {
                case KEY -> entry._1();
                case VALUE -> entry._2();
                case ENTRY -> MapViews.entry(entry._1(), entry._2());
            };
        }
    }

    /**
     * An in-order walk of the elements of a {@link TreeRange}, ascending or descending, with the path from the root
     * kept on an array stack: O(log n) to create, amortized O(1) per step, nothing allocated per step but what
     * {@code part} asks for (a {@link java.util.Map.Entry} for the entries of a map).
     */
    private static final class RangeIterator<E extends @Nullable Object, K extends @Nullable Object, X extends @Nullable Object> implements java.util.Iterator<X> {

        private final TreeRange<E, K> range;
        private final boolean descending;
        private final Part part;
        private Node<E>[] stack;
        private int depth;
        private @Nullable E next;

        @SuppressWarnings("unchecked")
        RangeIterator(TreeRange<E, K> range, boolean descending, Part part) {
            this.range = range;
            this.descending = descending;
            this.part = part;
            // a red-black tree of n nodes is at most 2 * log2(n + 1) high
            final int size = range.tree.size();
            this.stack = (Node<E>[]) new Node<?>[2 * (Integer.SIZE - Integer.numberOfLeadingZeros(size)) + 2];
            RedBlackTree<E> t = range.tree;
            while (t instanceof Node<E> node) {
                if (descending) {
                    if (range.tooHigh(range.key(node.value))) {
                        t = node.left;
                    } else {
                        push(node);
                        t = node.right;
                    }
                } else {
                    if (range.tooLow(range.key(node.value))) {
                        t = node.right;
                    } else {
                        push(node);
                        t = node.left;
                    }
                }
            }
            advance();
        }

        private void push(Node<E> node) {
            if (depth == stack.length) {
                stack = Arrays.copyOf(stack, 2 * depth);
            }
            stack[depth++] = node;
        }

        @SuppressWarnings("NullAway") // clears the popped slot of the stack
        private void advance() {
            if (depth == 0) {
                next = null;
                return;
            }
            final Node<E> node = stack[--depth];
            stack[depth] = null;
            RedBlackTree<E> t = descending ? node.left : node.right;
            while (t instanceof Node<E> child) {
                push(child);
                t = descending ? child.right : child.left;
            }
            final E value = node.value;
            if (descending ? range.tooLow(range.key(value)) : range.tooHigh(range.key(value))) {
                next = null;
                depth = 0;
            } else {
                next = value;
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public X next() {
            final E element = next;
            if (element == null) {
                throw new NoSuchElementException();
            }
            advance();
            return range.export(element, part);
        }
    }

    /**
     * The {@link NavigableSet} view of the keys of a {@link TreeRange}: the view of a {@code TreeSet} and the key set
     * of a {@code TreeMap} view.
     */
    static final class NavigableKeySetView<E extends @Nullable Object, K extends @Nullable Object> extends SetViews.UnmodifiableSet<K> implements NavigableSet<K> {

        private final @Nullable Object owner;
        private final TreeRange<E, K> range;
        private final boolean descending;

        NavigableKeySetView(@Nullable Object owner, TreeRange<E, K> range, boolean descending) {
            this.owner = owner;
            this.range = range;
            this.descending = descending;
        }

        @Override
        public @Nullable Object underlying() {
            return owner;
        }

        private @Nullable K keyOrNull(@Nullable E element) {
            return element == null ? null : range.key(element);
        }

        private K key(@Nullable E element) {
            if (element == null) {
                throw new NoSuchElementException();
            }
            return range.key(element);
        }

        @Override
        public int size() {
            return range.size();
        }

        @Override
        public boolean isEmpty() {
            return range.isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object key) {
            return range.find(key) != null;
        }

        @Override
        public java.util.Iterator<K> iterator() {
            return range.iterator(descending, Part.KEY);
        }

        @Override
        public java.util.Iterator<K> descendingIterator() {
            return range.iterator(!descending, Part.KEY);
        }

        @Override
        public @Nullable Comparator<? super K> comparator() {
            return range.comparator(descending);
        }

        @Override
        public K first() {
            return key(descending ? range.highest() : range.lowest());
        }

        @Override
        public K last() {
            return key(descending ? range.lowest() : range.highest());
        }

        @Override
        public K getFirst() {
            return first();
        }

        @Override
        public K getLast() {
            return last();
        }

        @Override
        public @Nullable K lower(K key) {
            return keyOrNull(descending ? range.higher(key) : range.lower(key));
        }

        @Override
        public @Nullable K floor(K key) {
            return keyOrNull(descending ? range.ceiling(key) : range.floor(key));
        }

        @Override
        public @Nullable K ceiling(K key) {
            return keyOrNull(descending ? range.floor(key) : range.ceiling(key));
        }

        @Override
        public @Nullable K higher(K key) {
            return keyOrNull(descending ? range.lower(key) : range.higher(key));
        }

        /** Throws: the view is unmodifiable, and this would remove the element. */
        @Override
        public K pollFirst() {
            throw JavaConverters.unmodifiable();
        }

        /** Throws: the view is unmodifiable, and this would remove the element. */
        @Override
        public K pollLast() {
            throw JavaConverters.unmodifiable();
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return new NavigableKeySetView<>(null, range, !descending);
        }

        @Override
        public NavigableSet<K> reversed() {
            return descendingSet();
        }

        @Override
        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            return new NavigableKeySetView<>(null, range.sub(descending, fromElement, fromInclusive, toElement, toInclusive), descending);
        }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            return new NavigableKeySetView<>(null, range.head(descending, toElement, inclusive), descending);
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            return new NavigableKeySetView<>(null, range.tail(descending, fromElement, inclusive), descending);
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /** The {@link NavigableMap} view of a {@link TreeRange} of entries. */
    static final class NavigableMapView<K extends @Nullable Object, V extends @Nullable Object> extends MapViews.UnmodifiableSequencedMap<K, V> implements NavigableMap<K, V> {

        private final @Nullable Object owner;
        private final TreeRange<Tuple2<K, V>, K> range;
        private final boolean descending;

        NavigableMapView(@Nullable Object owner, TreeRange<Tuple2<K, V>, K> range, boolean descending) {
            this.owner = owner;
            this.range = range;
            this.descending = descending;
        }

        @Override
        public @Nullable Object underlying() {
            return owner;
        }

        @Override
        @Nullable Object lookup(@Nullable Object key) {
            final Tuple2<K, V> entry = range.find(key);
            return entry == null ? ABSENT : entry._2();
        }

        @Override
        public int size() {
            return range.size();
        }

        @Override
        public boolean isEmpty() {
            return range.isEmpty();
        }

        @Override
        java.util.Iterator<java.util.Map.Entry<K, V>> entryIterator() {
            return range.iterator(descending, Part.ENTRY);
        }

        @Override
        java.util.Iterator<K> keyIterator() {
            return range.iterator(descending, Part.KEY);
        }

        @Override
        java.util.Iterator<V> valueIterator() {
            return range.iterator(descending, Part.VALUE);
        }

        @Override
        public @Nullable Comparator<? super K> comparator() {
            return range.comparator(descending);
        }

        private java.util.Map.@Nullable Entry<K, V> entryOrNull(@Nullable Tuple2<K, V> entry) {
            return entry == null ? null : MapViews.entry(entry._1(), entry._2());
        }

        private @Nullable K keyOrNull(@Nullable Tuple2<K, V> entry) {
            return entry == null ? null : entry._1();
        }

        private K key(@Nullable Tuple2<K, V> entry) {
            if (entry == null) {
                throw new NoSuchElementException();
            }
            return entry._1();
        }

        private @Nullable Tuple2<K, V> first() {
            return descending ? range.highest() : range.lowest();
        }

        private @Nullable Tuple2<K, V> last() {
            return descending ? range.lowest() : range.highest();
        }

        @Override
        public K firstKey() {
            return key(first());
        }

        @Override
        public K lastKey() {
            return key(last());
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> firstEntry() {
            return entryOrNull(first());
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> lastEntry() {
            return entryOrNull(last());
        }

        private @Nullable Tuple2<K, V> lowerTuple(K key) {
            return descending ? range.higher(key) : range.lower(key);
        }

        private @Nullable Tuple2<K, V> floorTuple(K key) {
            return descending ? range.ceiling(key) : range.floor(key);
        }

        private @Nullable Tuple2<K, V> ceilingTuple(K key) {
            return descending ? range.floor(key) : range.ceiling(key);
        }

        private @Nullable Tuple2<K, V> higherTuple(K key) {
            return descending ? range.lower(key) : range.higher(key);
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> lowerEntry(K key) {
            return entryOrNull(lowerTuple(key));
        }

        @Override
        public @Nullable K lowerKey(K key) {
            return keyOrNull(lowerTuple(key));
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> floorEntry(K key) {
            return entryOrNull(floorTuple(key));
        }

        @Override
        public @Nullable K floorKey(K key) {
            return keyOrNull(floorTuple(key));
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> ceilingEntry(K key) {
            return entryOrNull(ceilingTuple(key));
        }

        @Override
        public @Nullable K ceilingKey(K key) {
            return keyOrNull(ceilingTuple(key));
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> higherEntry(K key) {
            return entryOrNull(higherTuple(key));
        }

        @Override
        public @Nullable K higherKey(K key) {
            return keyOrNull(higherTuple(key));
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return new NavigableMapView<>(null, range, !descending);
        }

        @Override
        public NavigableMapView<K, V> reversed() {
            return new NavigableMapView<>(null, range, !descending);
        }

        @Override
        public NavigableSet<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> sequencedKeySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return new NavigableKeySetView<>(null, range, descending);
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return new NavigableKeySetView<>(null, range, !descending);
        }

        @Override
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return new NavigableMapView<>(null, range.sub(descending, fromKey, fromInclusive, toKey, toInclusive), descending);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return new NavigableMapView<>(null, range.head(descending, toKey, inclusive), descending);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return new NavigableMapView<>(null, range.tail(descending, fromKey, inclusive), descending);
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
    }
}
