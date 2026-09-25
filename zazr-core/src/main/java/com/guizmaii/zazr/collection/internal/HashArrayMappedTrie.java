package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.EmptyNode;
import com.guizmaii.zazr.control.Option;
import org.jspecify.annotations.Nullable;


/**
 * An immutable <a href="https://en.wikipedia.org/wiki/Hash_array_mapped_trie">Hash array mapped trie (HAMT)</a>.
 *
 * @author Ruslan Sennov, Grzegorz Piwowarek
 */
public interface HashArrayMappedTrie<K extends @Nullable Object, V extends @Nullable Object> extends Iterable<Tuple2<K, V>> {

    static <K extends @Nullable Object, V extends @Nullable Object> HashArrayMappedTrie<K, V> empty() {
        return EmptyNode.instance();
    }

    boolean isEmpty();

    int size();

    Option<V> get(K key);

    Option<Tuple2<K, V>> getEntry(K key);

    V getOrElse(K key, V defaultValue);

    boolean containsKey(K key);

    HashArrayMappedTrie<K, V> put(K key, V value);

    HashArrayMappedTrie<K, V> remove(K key);

    @Override
    Iterator<Tuple2<K, V>> iterator();

    /**
     * Provide unboxed access to the keys in the trie.
     */
    Iterator<K> keysIterator();

    /**
     * Provide unboxed access to the values in the trie.
     */
    Iterator<V> valuesIterator();
}
