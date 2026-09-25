package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import static java.lang.Integer.bitCount;

/// The node of a `HashMap` trie that splits its 32 slots between inline entries and children (see [ChampNode]).
/// Ported from `BitmapIndexedMapNode` in `scala/collection/immutable/HashMap.scala` of the Scala 3 standard library (the
/// Scala 2.13 collection library, which Scala 3 ships unchanged).
///
/// `content` holds the key and value of each entry, `2 * payloadArity` slots in slot order, then the children in
/// reverse slot order; `hashes` the hash of each entry's key. The node caches the size and the sum of the key hashes of
/// its subtree.
///
/// The fields are not final: a node owned by a [HashMapBuilder] (its `owner` is the builder's token) is updated in
/// place until the builder's `result()`, which fences the writes before the trie is published. A node of a persistent
/// operation has no owner and never changes; a trie is only reached through the final field of its `HashMap` or of a
/// view, whose freeze covers the nodes.
///
/// @param <K> the key type
/// @param <V> the value type
public final class BitmapIndexedMapNode<K extends @Nullable Object, V extends @Nullable Object> extends MapNode<K, V> {

    static final BitmapIndexedMapNode<?, ?> EMPTY = new BitmapIndexedMapNode<>(null, 0, 0, EMPTY_OBJECTS, EMPTY_INTS, 0, 0);

    int dataMap;
    int nodeMap;
    Object[] content;
    int[] hashes;
    int size;
    int keyHashSum;
    // the builder token that may update this node in place; null for a node of a persistent operation
    final @Nullable Object owner;

    BitmapIndexedMapNode(@Nullable Object owner, int dataMap, int nodeMap, Object[] content, int[] hashes, int size, int keyHashSum) {
        this.owner = owner;
        this.dataMap = dataMap;
        this.nodeMap = nodeMap;
        this.content = content;
        this.hashes = hashes;
        this.size = size;
        this.keyHashSum = keyHashSum;
    }

    // -- the entry points of HashMap

    /// The value of `key`, or `defaultValue` when absent.
    public V getOrElse(K key, V defaultValue) {
        return getOrElse(key, Objects.hashCode(key), 0, defaultValue);
    }

    /// `true` when `key` is present.
    public boolean containsKey(K key) {
        return containsKey(key, Objects.hashCode(key), 0);
    }

    /// The key kept for `key` and its value, or null when absent.
    public @Nullable Tuple2<K, V> getEntry(K key) {
        return getEntry(key, Objects.hashCode(key), 0);
    }

    /// The trie with `key` mapped to `value`, the key and value of an equal key replaced; this trie when both are the
    /// objects already there.
    public BitmapIndexedMapNode<K, V> updated(K key, V value) {
        return updated(key, value, Objects.hashCode(key), 0, true);
    }

    /// The trie without `key`; this trie when the key is absent.
    public BitmapIndexedMapNode<K, V> removed(K key) {
        return removed(key, Objects.hashCode(key), 0);
    }

    // -- accessors

    @SuppressWarnings("unchecked")
    @Override
    K getKey(int index) {
        return (K) content[2 * index];
    }

    @SuppressWarnings("unchecked")
    @Override
    V getValue(int index) {
        return (V) content[2 * index + 1];
    }

    @Override
    int getHash(int index) {
        return hashes[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    MapNode<K, V> getNode(int index) {
        return (MapNode<K, V>) content[content.length - 1 - index];
    }

    @Override
    boolean hasNodes() {
        return nodeMap != 0;
    }

    @Override
    int nodeArity() {
        return bitCount(nodeMap);
    }

    @Override
    boolean hasPayload() {
        return dataMap != 0;
    }

    @Override
    int payloadArity() {
        return bitCount(dataMap);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    int keyHashSum() {
        return keyHashSum;
    }

    int dataIndex(int bitpos) {
        return bitCount(dataMap & (bitpos - 1));
    }

    int nodeIndex(int bitpos) {
        return bitCount(nodeMap & (bitpos - 1));
    }

    // -- lookups

    @Override
    V getOrElse(K key, int hash, int shift, V defaultValue) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            return (hashes[index] == hash && Objects.equals(key, getKey(index))) ? getValue(index) : defaultValue;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).getOrElse(key, hash, shift + BIT_PARTITION_SIZE, defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    boolean containsKey(K key, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            return hashes[index] == hash && Objects.equals(key, getKey(index));
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).containsKey(key, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return false;
        }
    }

    @Override
    @Nullable Tuple2<K, V> getEntry(K key, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            return (hashes[index] == hash && Objects.equals(key, getKey(index))) ? Tuple.of(getKey(index), getValue(index)) : null;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).getEntry(key, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return null;
        }
    }

    // -- persistent updates

    @Override
    BitmapIndexedMapNode<K, V> updated(K key, V value, int hash, int shift, boolean replace) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            final K key0 = getKey(index);
            final int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(key0, key)) {
                if (replace && (key0 != key || getValue(index) != value)) {
                    return copyAndSetEntry(index, key, value);
                } else {
                    return this;
                }
            } else {
                final MapNode<K, V> subNodeNew = mergeTwoKeyValPairs(null, key0, getValue(index), hash0, key, value, hash,
                        shift + BIT_PARTITION_SIZE);
                return copyAndMigrateFromInlineToNode(bitpos, hash0, subNodeNew);
            }
        } else if ((nodeMap & bitpos) != 0) {
            final MapNode<K, V> subNode = getNode(indexFrom(nodeMap, bitpos));
            final MapNode<K, V> subNodeNew = subNode.updated(key, value, hash, shift + BIT_PARTITION_SIZE, replace);
            return (subNodeNew == subNode) ? this : copyAndSetNode(bitpos, subNode, subNodeNew);
        } else {
            return copyAndInsertValue(bitpos, key, hash, value);
        }
    }

    @Override
    BitmapIndexedMapNode<K, V> removed(K key, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            if (hashes[index] == hash && Objects.equals(getKey(index), key)) {
                if (payloadArity() == 2 && nodeArity() == 0) {
                    // the node of the remaining entry either becomes the root, or is inlined by the parent: its slot is
                    // the one of the root level
                    final int newDataMap = (shift == 0) ? (dataMap ^ bitpos) : bitposFrom(maskFrom(hash, 0));
                    final int other = 1 - index;
                    return new BitmapIndexedMapNode<>(null, newDataMap, 0, new Object[] { getKey(other), getValue(other) },
                            new int[] { hashes[other] }, 1, hashes[other]);
                } else {
                    return copyAndRemoveValue(bitpos, hash);
                }
            } else {
                return this;
            }
        } else if ((nodeMap & bitpos) != 0) {
            final MapNode<K, V> subNode = getNode(indexFrom(nodeMap, bitpos));
            final MapNode<K, V> subNodeNew = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE);
            if (subNodeNew == subNode) {
                return this;
            }
            final int subNodeNewSize = subNodeNew.size();
            if (subNodeNewSize == 1) {
                if (size == subNode.size()) {
                    // the child was all this node held: the single remaining entry goes up, to be inlined by the parent
                    return (BitmapIndexedMapNode<K, V>) subNodeNew;
                } else {
                    return copyAndMigrateFromNodeToInline(bitpos, subNode, subNodeNew);
                }
            } else {
                return copyAndSetNode(bitpos, subNode, subNodeNew);
            }
        } else {
            return this;
        }
    }

    /// The node of two entries whose hashes agree up to `shift`, owned by `owner`: one node holding both when their
    /// fragments at `shift` differ, a chain of single-child nodes down to where they do, or a collision node below the
    /// last level.
    static <K extends @Nullable Object, V extends @Nullable Object> MapNode<K, V> mergeTwoKeyValPairs(@Nullable Object owner,
            K key0, V value0, int hash0, K key1, V value1, int hash1, int shift) {
        if (shift >= HASH_CODE_LENGTH) {
            return new HashCollisionMapNode<>(hash0, new Object[] { key0, value0, key1, value1 });
        }
        final int mask0 = maskFrom(hash0, shift);
        final int mask1 = maskFrom(hash1, shift);
        if (mask0 != mask1) {
            final int dataMap = bitposFrom(mask0) | bitposFrom(mask1);
            final int keyHashSum = hash0 + hash1;
            if (mask0 < mask1) {
                return new BitmapIndexedMapNode<>(owner, dataMap, 0, new Object[] { key0, value0, key1, value1 },
                        new int[] { hash0, hash1 }, 2, keyHashSum);
            } else {
                return new BitmapIndexedMapNode<>(owner, dataMap, 0, new Object[] { key1, value1, key0, value0 },
                        new int[] { hash1, hash0 }, 2, keyHashSum);
            }
        } else {
            final MapNode<K, V> node = mergeTwoKeyValPairs(owner, key0, value0, hash0, key1, value1, hash1, shift + BIT_PARTITION_SIZE);
            return new BitmapIndexedMapNode<>(owner, 0, bitposFrom(mask0), new Object[] { node }, EMPTY_INTS, node.size(), node.keyHashSum());
        }
    }

    private BitmapIndexedMapNode<K, V> copyAndSetEntry(int index, K key, V value) {
        final Object[] dst = content.clone();
        dst[2 * index] = key;
        dst[2 * index + 1] = value;
        return new BitmapIndexedMapNode<>(null, dataMap, nodeMap, dst, hashes, size, keyHashSum);
    }

    private BitmapIndexedMapNode<K, V> copyAndSetNode(int bitpos, MapNode<K, V> oldNode, MapNode<K, V> newNode) {
        final Object[] dst = content.clone();
        dst[dst.length - 1 - nodeIndex(bitpos)] = newNode;
        return new BitmapIndexedMapNode<>(null, dataMap, nodeMap, dst, hashes, size - oldNode.size() + newNode.size(),
                keyHashSum - oldNode.keyHashSum() + newNode.keyHashSum());
    }

    private BitmapIndexedMapNode<K, V> copyAndInsertValue(int bitpos, K key, int hash, V value) {
        final int dataIx = dataIndex(bitpos);
        final int idx = 2 * dataIx;
        final Object[] src = content;
        final Object[] dst = new Object[src.length + 2];
        System.arraycopy(src, 0, dst, 0, idx);
        dst[idx] = key;
        dst[idx + 1] = value;
        System.arraycopy(src, idx, dst, idx + 2, src.length - idx);
        return new BitmapIndexedMapNode<>(null, dataMap | bitpos, nodeMap, dst, insertElement(hashes, dataIx, hash), size + 1,
                keyHashSum + hash);
    }

    private BitmapIndexedMapNode<K, V> copyAndRemoveValue(int bitpos, int hash) {
        final int dataIx = dataIndex(bitpos);
        final int idx = 2 * dataIx;
        final Object[] src = content;
        final Object[] dst = new Object[src.length - 2];
        System.arraycopy(src, 0, dst, 0, idx);
        System.arraycopy(src, idx + 2, dst, idx, src.length - idx - 2);
        return new BitmapIndexedMapNode<>(null, dataMap ^ bitpos, nodeMap, dst, removeElement(hashes, dataIx), size - 1,
                keyHashSum - hash);
    }

    // the content of this node with the entry of `bitpos` moved out, and `node` inserted among the children
    private Object[] migratedFromInlineToNode(int bitpos, MapNode<K, V> node) {
        final int idxOld = 2 * dataIndex(bitpos);
        final int idxNew = content.length - 2 - nodeIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length - 2 + 1];
        System.arraycopy(src, 0, dst, 0, idxOld);
        System.arraycopy(src, idxOld + 2, dst, idxOld, idxNew - idxOld);
        dst[idxNew] = node;
        System.arraycopy(src, idxNew + 2, dst, idxNew + 1, src.length - idxNew - 2);
        return dst;
    }

    private BitmapIndexedMapNode<K, V> copyAndMigrateFromInlineToNode(int bitpos, int hash, MapNode<K, V> node) {
        return new BitmapIndexedMapNode<>(null, dataMap ^ bitpos, nodeMap | bitpos, migratedFromInlineToNode(bitpos, node),
                removeElement(hashes, dataIndex(bitpos)), size - 1 + node.size(), keyHashSum - hash + node.keyHashSum());
    }

    private BitmapIndexedMapNode<K, V> copyAndMigrateFromNodeToInline(int bitpos, MapNode<K, V> oldNode, MapNode<K, V> node) {
        final int idxOld = content.length - 1 - nodeIndex(bitpos);
        final int dataIxNew = dataIndex(bitpos);
        final int idxNew = 2 * dataIxNew;
        final Object[] src = content;
        final Object[] dst = new Object[src.length - 1 + 2];
        System.arraycopy(src, 0, dst, 0, idxNew);
        dst[idxNew] = node.getKey(0);
        dst[idxNew + 1] = node.getValue(0);
        System.arraycopy(src, idxNew, dst, idxNew + 2, idxOld - idxNew);
        System.arraycopy(src, idxOld + 1, dst, idxOld + 2, src.length - idxOld - 1);
        return new BitmapIndexedMapNode<>(null, dataMap | bitpos, nodeMap ^ bitpos, dst, insertElement(hashes, dataIxNew, node.getHash(0)),
                size - oldNode.size() + 1, keyHashSum - oldNode.keyHashSum() + node.keyHashSum());
    }

    // -- the updates in place of a builder

    @Override
    BitmapIndexedMapNode<K, V> putInPlace(Object owner, K key, V value, int hash, int shift) {
        final BitmapIndexedMapNode<K, V> node = (this.owner == owner)
                                                ? this
                                                : new BitmapIndexedMapNode<>(owner, dataMap, nodeMap, content.clone(), hashes, size, keyHashSum);
        node.update(owner, key, value, hash, shift);
        return node;
    }

    // `this` is owned by `owner`; its hashes array is never written in place, only replaced, so it may be shared
    private void update(Object owner, K key, V value, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            final K key0 = getKey(index);
            final int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(key0, key)) {
                content[2 * index] = key;
                content[2 * index + 1] = value;
            } else {
                final MapNode<K, V> subNodeNew = mergeTwoKeyValPairs(owner, key0, getValue(index), hash0, key, value, hash,
                        shift + BIT_PARTITION_SIZE);
                final Object[] newContent = migratedFromInlineToNode(bitpos, subNodeNew);
                hashes = removeElement(hashes, index);
                content = newContent;
                dataMap ^= bitpos;
                nodeMap |= bitpos;
                size = size - 1 + subNodeNew.size();
                keyHashSum = keyHashSum - hash0 + subNodeNew.keyHashSum();
            }
        } else if ((nodeMap & bitpos) != 0) {
            final int slot = content.length - 1 - nodeIndex(bitpos);
            @SuppressWarnings("unchecked")
            final MapNode<K, V> subNode = (MapNode<K, V>) content[slot];
            // read before the put, which may update the child in place
            final int subNodeSize = subNode.size();
            final int subNodeHashSum = subNode.keyHashSum();
            final MapNode<K, V> subNodeNew = subNode.putInPlace(owner, key, value, hash, shift + BIT_PARTITION_SIZE);
            content[slot] = subNodeNew;
            size = size - subNodeSize + subNodeNew.size();
            keyHashSum = keyHashSum - subNodeHashSum + subNodeNew.keyHashSum();
        } else {
            final int dataIx = dataIndex(bitpos);
            final int idx = 2 * dataIx;
            final Object[] src = content;
            final Object[] dst = new Object[src.length + 2];
            System.arraycopy(src, 0, dst, 0, idx);
            dst[idx] = key;
            dst[idx + 1] = value;
            System.arraycopy(src, idx, dst, idx + 2, src.length - idx);
            content = dst;
            hashes = insertElement(hashes, dataIx, hash);
            dataMap |= bitpos;
            size++;
            keyHashSum += hash;
        }
    }
}
