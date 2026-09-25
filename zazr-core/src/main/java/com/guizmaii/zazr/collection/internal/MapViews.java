package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.LinkedHashMap;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.JavaConverters.unmodifiable;
import static com.guizmaii.zazr.collection.internal.Maps.ABSENT;

/**
 * The unmodifiable {@link java.util.Map} views: {@link java.util.Map} for {@link HashMap}, {@link SequencedMap} for
 * {@link LinkedHashMap}. The {@link java.util.NavigableMap} view of a {@code TreeMap} is in {@link TreeViews}. The
 * rules of {@link JavaConverters} hold: O(1) to create, nothing copied, every mutator throws
 * {@link UnsupportedOperationException}. {@code keySet()}, {@code values()} and {@code entrySet()} are views over the
 * same map, and their entries are immutable ({@link AbstractMap.SimpleImmutableEntry}). {@code equals} and
 * {@code hashCode} follow {@link java.util.Map}: a view equals any {@code java.util.Map} with the same mappings, and
 * its hash is the sum of {@code key.hashCode() ^ value.hashCode()} over the entries.
 */
public final class MapViews {

    private MapViews() {
    }

    public static <K extends @Nullable Object, V extends @Nullable Object> java.util.Map<K, V> asJavaMap(HashMap<K, V> map, HashArrayMappedTrie<K, V> trie) {
        return new HashMapView<>(map, trie);
    }

    /**
     * The {@link SequencedMap} view of a {@link LinkedHashMap}.
     *
     * @param map     the map
     * @param reverse the entries of {@code map} in reverse insertion order, each call a new iterator
     * @param <K>     the key type
     * @param <V>     the value type
     * @return the view
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> SequencedMap<K, V> asJavaMap(LinkedHashMap<K, V> map, Iterable<Tuple2<K, V>> reverse) {
        return new SequencedMapView<>(map, reverse, false);
    }

    static <K extends @Nullable Object, V extends @Nullable Object> java.util.Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /** What a {@link TupleIterator} yields for each entry of a Zazr map. */
    enum Part { KEY, VALUE, ENTRY }

    /**
     * The keys, the values or the {@link java.util.Map.Entry entries} of an iterator of Zazr map entries, one
     * iterator for the three so that no function is allocated per view.
     */
    static final class TupleIterator<K extends @Nullable Object, V extends @Nullable Object, X extends @Nullable Object> implements java.util.Iterator<X> {

        private final java.util.Iterator<Tuple2<K, V>> entries;
        private final Part part;

        TupleIterator(java.util.Iterator<Tuple2<K, V>> entries, Part part) {
            this.entries = entries;
            this.part = part;
        }

        @Override
        public boolean hasNext() {
            return entries.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public X next() {
            final Tuple2<K, V> entry = entries.next();
            return (X) switch (part) {
                case KEY -> entry._1();
                case VALUE -> entry._2();
                case ENTRY -> entry(entry._1(), entry._2());
            };
        }
    }

    /**
     * The base of the map views: the lookups go through {@link #lookup(Object)}, which answers the value or the
     * {@link Maps#ABSENT} sentinel with no {@code Option} allocated, and every mutator throws.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    abstract static class UnmodifiableMap<K extends @Nullable Object, V extends @Nullable Object> extends AbstractMap<K, V> implements JavaConverters.View {

        /** The value of {@code key}, or {@link Maps#ABSENT}. */
        abstract @Nullable Object lookup(@Nullable Object key);

        abstract java.util.Iterator<java.util.Map.Entry<K, V>> entryIterator();

        abstract java.util.Iterator<K> keyIterator();

        abstract java.util.Iterator<V> valueIterator();

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable V get(@Nullable Object key) {
            final Object value = lookup(key);
            return value == ABSENT ? null : (V) value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
            final Object value = lookup(key);
            return value == ABSENT ? defaultValue : (V) value;
        }

        @Override
        public boolean containsKey(@Nullable Object key) {
            return lookup(key) != ABSENT;
        }

        @Override
        public boolean containsValue(@Nullable Object value) {
            final java.util.Iterator<V> values = valueIterator();
            while (values.hasNext()) {
                if (Objects.equals(value, values.next())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public java.util.Set<K> keySet() {
            return new KeySetView<>(this);
        }

        @Override
        public Collection<V> values() {
            return new ValuesView<>(this);
        }

        @Override
        public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
            return new EntrySetView<>(this);
        }

        // -- mutators

        @Override
        public final @Nullable V put(K key, V value) {
            throw unmodifiable();
        }

        @Override
        public final void putAll(java.util.Map<? extends K, ? extends V> map) {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V remove(@Nullable Object key) {
            throw unmodifiable();
        }

        @Override
        public final boolean remove(@Nullable Object key, @Nullable Object value) {
            throw unmodifiable();
        }

        @Override
        public final void clear() {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V putIfAbsent(K key, V value) {
            throw unmodifiable();
        }

        @Override
        public final boolean replace(K key, V oldValue, V newValue) {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V replace(K key, V value) {
            throw unmodifiable();
        }

        @Override
        public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends @Nullable V> remappingFunction) {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V compute(K key, BiFunction<? super K, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
            throw unmodifiable();
        }

        @Override
        public final @Nullable V merge(K key, V value, BiFunction<? super V, ? super V, ? extends @Nullable V> remappingFunction) {
            throw unmodifiable();
        }

        /**
         * Throws: the view is unmodifiable.
         *
         * @param key   ignored
         * @param value ignored
         * @return never
         */
        public final V putFirst(K key, V value) {
            throw unmodifiable();
        }

        /**
         * Throws: the view is unmodifiable.
         *
         * @param key   ignored
         * @param value ignored
         * @return never
         */
        public final V putLast(K key, V value) {
            throw unmodifiable();
        }

        /**
         * Throws: the view is unmodifiable, and this would remove the entry.
         *
         * @return never
         */
        public final java.util.Map.Entry<K, V> pollFirstEntry() {
            throw unmodifiable();
        }

        /**
         * Throws: the view is unmodifiable, and this would remove the entry.
         *
         * @return never
         */
        public final java.util.Map.Entry<K, V> pollLastEntry() {
            throw unmodifiable();
        }
    }

    /**
     * The base of the {@link SequencedMap} views: {@link #reversed()} is a view of the same map in the opposite order,
     * and the key set, the values and the entry set are {@link SequencedSet}/{@link SequencedCollection} views whose
     * {@code reversed()} is the corresponding view of the reversed map.
     */
    abstract static class UnmodifiableSequencedMap<K extends @Nullable Object, V extends @Nullable Object> extends UnmodifiableMap<K, V> implements SequencedMap<K, V> {

        @Override
        public abstract UnmodifiableSequencedMap<K, V> reversed();

        @Override
        public SequencedSet<K> keySet() {
            return sequencedKeySet();
        }

        @Override
        public SequencedCollection<V> values() {
            return sequencedValues();
        }

        @Override
        public SequencedSet<java.util.Map.Entry<K, V>> entrySet() {
            return sequencedEntrySet();
        }

        @Override
        public SequencedSet<K> sequencedKeySet() {
            return new SequencedKeySetView<>(this);
        }

        @Override
        public SequencedCollection<V> sequencedValues() {
            return new SequencedValuesView<>(this);
        }

        @Override
        public SequencedSet<java.util.Map.Entry<K, V>> sequencedEntrySet() {
            return new SequencedEntrySetView<>(this);
        }
    }

    // -- the key set, the values and the entry set of a map view

    static class KeySetView<K extends @Nullable Object, V extends @Nullable Object> extends SetViews.UnmodifiableSet<K> {

        final UnmodifiableMap<K, V> map;

        KeySetView(UnmodifiableMap<K, V> map) {
            this.map = map;
        }

        @Override
        public @Nullable Object underlying() {
            return null;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object key) {
            return map.containsKey(key);
        }

        @Override
        public java.util.Iterator<K> iterator() {
            return map.keyIterator();
        }
    }

    static final class SequencedKeySetView<K extends @Nullable Object, V extends @Nullable Object> extends KeySetView<K, V> implements SequencedSet<K> {

        SequencedKeySetView(UnmodifiableSequencedMap<K, V> map) {
            super(map);
        }

        @Override
        public SequencedSet<K> reversed() {
            return ((UnmodifiableSequencedMap<K, V>) map).reversed().sequencedKeySet();
        }

        @Override
        public Spliterator<K> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.IMMUTABLE);
        }
    }

    static class ValuesView<K extends @Nullable Object, V extends @Nullable Object> extends JavaConverters.UnmodifiableCollection<V> {

        final UnmodifiableMap<K, V> map;

        ValuesView(UnmodifiableMap<K, V> map) {
            this.map = map;
        }

        @Override
        public @Nullable Object underlying() {
            return null;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object value) {
            return map.containsValue(value);
        }

        @Override
        public java.util.Iterator<V> iterator() {
            return map.valueIterator();
        }
    }

    static final class SequencedValuesView<K extends @Nullable Object, V extends @Nullable Object> extends ValuesView<K, V> implements SequencedCollection<V> {

        SequencedValuesView(UnmodifiableSequencedMap<K, V> map) {
            super(map);
        }

        @Override
        public SequencedCollection<V> reversed() {
            return ((UnmodifiableSequencedMap<K, V>) map).reversed().sequencedValues();
        }

        @Override
        public Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        }

        @Override
        public V getFirst() {
            final java.util.Iterator<V> values = iterator();
            if (!values.hasNext()) {
                throw new NoSuchElementException();
            }
            return values.next();
        }

        @Override
        public V getLast() {
            return reversed().getFirst();
        }

        @Override
        public void addFirst(V element) {
            throw unmodifiable();
        }

        @Override
        public void addLast(V element) {
            throw unmodifiable();
        }

        @Override
        public V removeFirst() {
            throw unmodifiable();
        }

        @Override
        public V removeLast() {
            throw unmodifiable();
        }
    }

    static class EntrySetView<K extends @Nullable Object, V extends @Nullable Object> extends SetViews.UnmodifiableSet<java.util.Map.Entry<K, V>> {

        final UnmodifiableMap<K, V> map;

        EntrySetView(UnmodifiableMap<K, V> map) {
            this.map = map;
        }

        @Override
        public @Nullable Object underlying() {
            return null;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object object) {
            if (!(object instanceof java.util.Map.Entry<?, ?> entry)) {
                return false;
            }
            final Object value = map.lookup(entry.getKey());
            return value != ABSENT && Objects.equals(value, entry.getValue());
        }

        @Override
        public java.util.Iterator<java.util.Map.Entry<K, V>> iterator() {
            return map.entryIterator();
        }
    }

    static final class SequencedEntrySetView<K extends @Nullable Object, V extends @Nullable Object> extends EntrySetView<K, V> implements SequencedSet<java.util.Map.Entry<K, V>> {

        SequencedEntrySetView(UnmodifiableSequencedMap<K, V> map) {
            super(map);
        }

        @Override
        public SequencedSet<java.util.Map.Entry<K, V>> reversed() {
            return ((UnmodifiableSequencedMap<K, V>) map).reversed().sequencedEntrySet();
        }

        @Override
        public Spliterator<java.util.Map.Entry<K, V>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.IMMUTABLE);
        }
    }

    // -- the views of the concrete maps

    /** The view of a {@link HashMap}: the lookups and the walks read its trie, with no {@code Tuple2} per entry. */
    static final class HashMapView<K extends @Nullable Object, V extends @Nullable Object> extends UnmodifiableMap<K, V> {

        private final HashMap<K, V> map;
        private final HashArrayMappedTrie<K, V> trie;

        HashMapView(HashMap<K, V> map, HashArrayMappedTrie<K, V> trie) {
            this.map = map;
            this.trie = trie;
        }

        @Override
        public Object underlying() {
            return map;
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        @Nullable Object lookup(@Nullable Object key) {
            return trie.getOrElse((K) key, (V) ABSENT);
        }

        @Override
        public int size() {
            return trie.size();
        }

        @Override
        public boolean isEmpty() {
            return trie.isEmpty();
        }

        @Override
        java.util.Iterator<java.util.Map.Entry<K, V>> entryIterator() {
            final java.util.Iterator<HashArrayMappedTrieModule.LeafNode<K, V>> nodes = ((HashArrayMappedTrieModule.AbstractNode<K, V>) trie).nodes();
            return new java.util.Iterator<>() {
                @Override
                public boolean hasNext() {
                    return nodes.hasNext();
                }

                @Override
                public java.util.Map.Entry<K, V> next() {
                    final HashArrayMappedTrieModule.LeafNode<K, V> node = nodes.next();
                    return entry(node.key(), node.value());
                }
            };
        }

        @Override
        java.util.Iterator<K> keyIterator() {
            return trie.keysIterator();
        }

        @Override
        java.util.Iterator<V> valueIterator() {
            return trie.valuesIterator();
        }
    }

    /**
     * The view of a {@link LinkedHashMap} in insertion order, or in reverse insertion order when {@code reversed} is
     * set (the view {@link #reversed()} returns).
     */
    static final class SequencedMapView<K extends @Nullable Object, V extends @Nullable Object> extends UnmodifiableSequencedMap<K, V> {

        private final LinkedHashMap<K, V> map;
        private final Iterable<Tuple2<K, V>> reverse;
        private final boolean reversed;

        SequencedMapView(LinkedHashMap<K, V> map, Iterable<Tuple2<K, V>> reverse, boolean reversed) {
            this.map = map;
            this.reverse = reverse;
            this.reversed = reversed;
        }

        @Override
        public @Nullable Object underlying() {
            return reversed ? null : map;
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        @Nullable Object lookup(@Nullable Object key) {
            return map.getOrElse((K) key, (V) ABSENT);
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        private java.util.Iterator<Tuple2<K, V>> tuples() {
            return reversed ? reverse.iterator() : map.iterator();
        }

        @Override
        java.util.Iterator<java.util.Map.Entry<K, V>> entryIterator() {
            return new TupleIterator<>(tuples(), Part.ENTRY);
        }

        @Override
        java.util.Iterator<K> keyIterator() {
            return new TupleIterator<>(tuples(), Part.KEY);
        }

        @Override
        java.util.Iterator<V> valueIterator() {
            return new TupleIterator<>(tuples(), Part.VALUE);
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> firstEntry() {
            if (map.isEmpty()) {
                return null;
            }
            final Tuple2<K, V> first = reversed ? map.last() : map.head();
            return entry(first._1(), first._2());
        }

        @Override
        public java.util.Map.@Nullable Entry<K, V> lastEntry() {
            if (map.isEmpty()) {
                return null;
            }
            final Tuple2<K, V> last = reversed ? map.head() : map.last();
            return entry(last._1(), last._2());
        }

        @Override
        public UnmodifiableSequencedMap<K, V> reversed() {
            return new SequencedMapView<>(map, reverse, !reversed);
        }
    }
}
