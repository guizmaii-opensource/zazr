package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;

import static java.lang.Integer.bitCount;

/// The node of a `HashMap` trie that splits its 32 slots between inline entries and children (see [ChampNode]). Ported
/// from `BitmapIndexedMapNode` in `scala/collection/immutable/HashMap.scala` of the Scala 2.13 collections library,
/// which Scala 3 uses unchanged.
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

    static final BitmapIndexedMapNode<?, ?> EMPTY =
            new BitmapIndexedMapNode<>(null, 0, 0, EMPTY_OBJECTS, EMPTY_INTS, 0, 0);

    int dataMap;
    int nodeMap;
    Object[] content;
    int[] hashes;
    int size;
    int keyHashSum;
    // the builder token that may update this node in place; null for a node of a persistent operation
    final @Nullable Object owner;

    BitmapIndexedMapNode(
            @Nullable Object owner,
            int dataMap,
            int nodeMap,
            Object[] content,
            int[] hashes,
            int size,
            int keyHashSum) {
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

    /// The key kept for `key`, or null when absent.
    public @Nullable K findKey(K key) {
        return findKey(key, Objects.hashCode(key), 0);
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

    /// The trie of a `HashSet` of the keys of this trie, built node for node in O(n), with no hashing and no key
    /// comparison (see [MapNode#keyTrie()]).
    @Override
    public BitmapIndexedSetNode<K> keyTrie() {
        int payload = bitCount(dataMap);
        int children = bitCount(nodeMap);
        Object[] keys = new Object[payload + children];
        for (int i = 0; i < payload; i++) {
            keys[i] = content[2 * i];
        }
        // the children sit at the back of both arrays, in reverse slot order
        for (int i = 0; i < children; i++) {
            keys[keys.length - 1 - i] = getNode(i).keyTrie();
        }
        return new BitmapIndexedSetNode<>(null, dataMap, nodeMap, keys, hashes, size, keyHashSum);
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
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            return (hashes[index] == hash && Objects.equals(key, getKey(index))) ? getValue(index) : defaultValue;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).getOrElse(key, hash, shift + BIT_PARTITION_SIZE, defaultValue);
        } else {
            return defaultValue;
        }
    }

    @Override
    boolean containsKey(K key, int hash, int shift) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            return hashes[index] == hash && Objects.equals(key, getKey(index));
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).containsKey(key, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return false;
        }
    }

    @Override
    boolean holdsEntry(K key, V value, int hash, int shift) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            return getKey(index) == key && getValue(index) == value;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).holdsEntry(key, value, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return false;
        }
    }

    @Override
    @Nullable
    Tuple2<K, V> getEntry(K key, int hash, int shift) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            return (hashes[index] == hash && Objects.equals(key, getKey(index)))
                    ? Tuple.of(getKey(index), getValue(index))
                    : null;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).getEntry(key, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    K findKey(K key, int hash, int shift) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            K stored = getKey(index);
            return (hashes[index] == hash && Objects.equals(key, stored)) ? stored : null;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).findKey(key, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return null;
        }
    }

    // -- persistent updates

    @Override
    BitmapIndexedMapNode<K, V> updated(K key, V value, int hash, int shift, boolean replace) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            K key0 = getKey(index);
            int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(key0, key)) {
                if (replace && (key0 != key || getValue(index) != value)) {
                    return copyAndSetEntry(index, key, value);
                } else {
                    return this;
                }
            } else {
                MapNode<K, V> subNodeNew = mergeTwoKeyValPairs(
                        null, key0, getValue(index), hash0, key, value, hash, shift + BIT_PARTITION_SIZE);
                return copyAndMigrateFromInlineToNode(bitpos, hash0, subNodeNew);
            }
        } else if ((nodeMap & bitpos) != 0) {
            MapNode<K, V> subNode = getNode(indexFrom(nodeMap, bitpos));
            MapNode<K, V> subNodeNew = subNode.updated(key, value, hash, shift + BIT_PARTITION_SIZE, replace);
            return (subNodeNew == subNode) ? this : copyAndSetNode(bitpos, subNode, subNodeNew);
        } else {
            return copyAndInsertValue(bitpos, key, hash, value);
        }
    }

    @Override
    BitmapIndexedMapNode<K, V> removed(K key, int hash, int shift) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            if (hashes[index] == hash && Objects.equals(getKey(index), key)) {
                if (payloadArity() == 2 && nodeArity() == 0) {
                    // the node of the remaining entry either becomes the root, or is inlined by the parent: its slot is
                    // the one of the root level
                    int newDataMap = (shift == 0) ? (dataMap ^ bitpos) : bitposFrom(maskFrom(hash, 0));
                    int other = 1 - index;
                    return new BitmapIndexedMapNode<>(
                            null,
                            newDataMap,
                            0,
                            new Object[] {getKey(other), getValue(other)},
                            new int[] {hashes[other]},
                            1,
                            hashes[other]);
                } else {
                    return copyAndRemoveValue(bitpos, hash);
                }
            } else {
                return this;
            }
        } else if ((nodeMap & bitpos) != 0) {
            MapNode<K, V> subNode = getNode(indexFrom(nodeMap, bitpos));
            MapNode<K, V> subNodeNew = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE);
            if (subNodeNew == subNode) {
                return this;
            }
            int subNodeNewSize = subNodeNew.size();
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
    /// fragments at `shift` differ, a chain of single-child nodes down to where they do, or a collision node below
    /// the last level.
    static <K extends @Nullable Object, V extends @Nullable Object> MapNode<K, V> mergeTwoKeyValPairs(
            @Nullable Object owner, K key0, V value0, int hash0, K key1, V value1, int hash1, int shift) {
        if (shift >= HASH_CODE_LENGTH) {
            return new HashCollisionMapNode<>(hash0, new Object[] {key0, value0, key1, value1});
        }
        int mask0 = maskFrom(hash0, shift);
        int mask1 = maskFrom(hash1, shift);
        if (mask0 != mask1) {
            int dataMap = bitposFrom(mask0) | bitposFrom(mask1);
            int keyHashSum = hash0 + hash1;
            if (mask0 < mask1) {
                return new BitmapIndexedMapNode<>(
                        owner,
                        dataMap,
                        0,
                        new Object[] {key0, value0, key1, value1},
                        new int[] {hash0, hash1},
                        2,
                        keyHashSum);
            } else {
                return new BitmapIndexedMapNode<>(
                        owner,
                        dataMap,
                        0,
                        new Object[] {key1, value1, key0, value0},
                        new int[] {hash1, hash0},
                        2,
                        keyHashSum);
            }
        } else {
            MapNode<K, V> node =
                    mergeTwoKeyValPairs(owner, key0, value0, hash0, key1, value1, hash1, shift + BIT_PARTITION_SIZE);
            return new BitmapIndexedMapNode<>(
                    owner, 0, bitposFrom(mask0), new Object[] {node}, EMPTY_INTS, node.size(), node.keyHashSum());
        }
    }

    private BitmapIndexedMapNode<K, V> copyAndSetEntry(int index, K key, V value) {
        Object[] dst = content.clone();
        dst[2 * index] = key;
        dst[2 * index + 1] = value;
        return new BitmapIndexedMapNode<>(null, dataMap, nodeMap, dst, hashes, size, keyHashSum);
    }

    private BitmapIndexedMapNode<K, V> copyAndSetNode(int bitpos, MapNode<K, V> oldNode, MapNode<K, V> newNode) {
        Object[] dst = content.clone();
        dst[dst.length - 1 - nodeIndex(bitpos)] = newNode;
        return new BitmapIndexedMapNode<>(
                null,
                dataMap,
                nodeMap,
                dst,
                hashes,
                size - oldNode.size() + newNode.size(),
                keyHashSum - oldNode.keyHashSum() + newNode.keyHashSum());
    }

    private BitmapIndexedMapNode<K, V> copyAndInsertValue(int bitpos, K key, int hash, V value) {
        int dataIx = dataIndex(bitpos);
        int idx = 2 * dataIx;
        Object[] src = content;
        Object[] dst = new Object[src.length + 2];
        System.arraycopy(src, 0, dst, 0, idx);
        dst[idx] = key;
        dst[idx + 1] = value;
        System.arraycopy(src, idx, dst, idx + 2, src.length - idx);
        return new BitmapIndexedMapNode<>(
                null, dataMap | bitpos, nodeMap, dst, insertElement(hashes, dataIx, hash), size + 1, keyHashSum + hash);
    }

    private BitmapIndexedMapNode<K, V> copyAndRemoveValue(int bitpos, int hash) {
        int dataIx = dataIndex(bitpos);
        int idx = 2 * dataIx;
        Object[] src = content;
        Object[] dst = new Object[src.length - 2];
        System.arraycopy(src, 0, dst, 0, idx);
        System.arraycopy(src, idx + 2, dst, idx, src.length - idx - 2);
        return new BitmapIndexedMapNode<>(
                null, dataMap ^ bitpos, nodeMap, dst, removeElement(hashes, dataIx), size - 1, keyHashSum - hash);
    }

    // the content of this node with the entry of `bitpos` moved out, and `node` inserted among the children
    private Object[] migratedFromInlineToNode(int bitpos, MapNode<K, V> node) {
        int idxOld = 2 * dataIndex(bitpos);
        int idxNew = content.length - 2 - nodeIndex(bitpos);
        Object[] src = content;
        Object[] dst = new Object[src.length - 2 + 1];
        System.arraycopy(src, 0, dst, 0, idxOld);
        System.arraycopy(src, idxOld + 2, dst, idxOld, idxNew - idxOld);
        dst[idxNew] = node;
        System.arraycopy(src, idxNew + 2, dst, idxNew + 1, src.length - idxNew - 2);
        return dst;
    }

    private BitmapIndexedMapNode<K, V> copyAndMigrateFromInlineToNode(int bitpos, int hash, MapNode<K, V> node) {
        return new BitmapIndexedMapNode<>(
                null,
                dataMap ^ bitpos,
                nodeMap | bitpos,
                migratedFromInlineToNode(bitpos, node),
                removeElement(hashes, dataIndex(bitpos)),
                size - 1 + node.size(),
                keyHashSum - hash + node.keyHashSum());
    }

    private BitmapIndexedMapNode<K, V> copyAndMigrateFromNodeToInline(
            int bitpos, MapNode<K, V> oldNode, MapNode<K, V> node) {
        int idxOld = content.length - 1 - nodeIndex(bitpos);
        int dataIxNew = dataIndex(bitpos);
        int idxNew = 2 * dataIxNew;
        Object[] src = content;
        Object[] dst = new Object[src.length - 1 + 2];
        System.arraycopy(src, 0, dst, 0, idxNew);
        dst[idxNew] = node.getKey(0);
        dst[idxNew + 1] = node.getValue(0);
        System.arraycopy(src, idxNew, dst, idxNew + 2, idxOld - idxNew);
        System.arraycopy(src, idxOld + 1, dst, idxOld + 2, src.length - idxOld - 1);
        return new BitmapIndexedMapNode<>(
                null,
                dataMap | bitpos,
                nodeMap ^ bitpos,
                dst,
                insertElement(hashes, dataIxNew, node.getHash(0)),
                size - oldNode.size() + 1,
                keyHashSum - oldNode.keyHashSum() + node.keyHashSum());
    }

    // -- the operations on whole subtrees

    // Scala's concat: a first pass sorts each slot into one of nine cases, a second builds the node
    @Override
    @SuppressWarnings("Var")
    public BitmapIndexedMapNode<K, V> concat(MapNode<K, V> that, int shift) {
        BitmapIndexedMapNode<K, V> bm = (BitmapIndexedMapNode<K, V>) that;
        if (size == 0) {
            return bm;
        } else if (bm.size == 0 || bm == this) {
            return this;
        } else if (bm.size == 1) {
            BitmapIndexedMapNode<K, V> result = updated(bm.getKey(0), bm.getValue(0), bm.hashes[0], shift, true);
            // this node when it holds the very entry of `bm`; `bm` when this node held only its key, whose entry wins
            return (result == this) ? this : (result.size == 1) ? bm : result;
        }
        // set as soon as the result differs from `bm`, which is returned otherwise
        boolean anyChangesMadeSoFar = false;
        int allMap = dataMap | bm.dataMap | nodeMap | bm.nodeMap;
        // both inclusive
        int minimumBitPos = bitposFrom(Integer.numberOfTrailingZeros(allMap));
        int maximumBitPos = bitposFrom(BRANCHING_FACTOR - Integer.numberOfLeadingZeros(allMap) - 1);

        // set when an entry of `bm` replaces one of this node by another key or value object
        boolean overwritesChangeThis = false;
        int leftNodeRightNode = 0;
        int leftDataRightNode = 0;
        int leftNodeRightData = 0;
        int leftDataOnly = 0;
        int rightDataOnly = 0;
        int leftNodeOnly = 0;
        int rightNodeOnly = 0;
        int leftDataRightDataMigrateToNode = 0;
        int leftDataRightDataRightOverwrites = 0;
        int dataToNodeMigrationTargets = 0;

        int bitpos = minimumBitPos;
        int leftIdx = 0;
        int rightIdx = 0;
        while (true) {
            if ((bitpos & dataMap) != 0) {
                if ((bitpos & bm.dataMap) != 0) {
                    int leftHash = hashes[leftIdx];
                    if (leftHash == bm.hashes[rightIdx] && Objects.equals(getKey(leftIdx), bm.getKey(rightIdx))) {
                        leftDataRightDataRightOverwrites |= bitpos;
                        if (getKey(leftIdx) != bm.getKey(rightIdx) || getValue(leftIdx) != bm.getValue(rightIdx)) {
                            overwritesChangeThis = true;
                        }
                    } else {
                        leftDataRightDataMigrateToNode |= bitpos;
                        dataToNodeMigrationTargets |= bitposFrom(maskFrom(leftHash, shift));
                    }
                    rightIdx++;
                } else if ((bitpos & bm.nodeMap) != 0) {
                    leftDataRightNode |= bitpos;
                } else {
                    leftDataOnly |= bitpos;
                }
                leftIdx++;
            } else if ((bitpos & nodeMap) != 0) {
                if ((bitpos & bm.dataMap) != 0) {
                    leftNodeRightData |= bitpos;
                    rightIdx++;
                } else if ((bitpos & bm.nodeMap) != 0) {
                    leftNodeRightNode |= bitpos;
                } else {
                    leftNodeOnly |= bitpos;
                }
            } else if ((bitpos & bm.dataMap) != 0) {
                rightDataOnly |= bitpos;
                rightIdx++;
            } else if ((bitpos & bm.nodeMap) != 0) {
                rightNodeOnly |= bitpos;
            }
            if (bitpos == maximumBitPos) {
                break;
            }
            bitpos <<= 1;
        }

        int newDataMap = leftDataOnly | rightDataOnly | leftDataRightDataRightOverwrites;
        int newNodeMap = leftNodeRightNode
                | leftDataRightNode
                | leftNodeRightData
                | leftNodeOnly
                | rightNodeOnly
                | dataToNodeMigrationTargets;
        if (newDataMap == (rightDataOnly | leftDataRightDataRightOverwrites) && newNodeMap == rightNodeOnly) {
            // nothing of this node makes it into the result: this node when `bm` holds its very entries and no other
            return (!overwritesChangeThis && (rightDataOnly | rightNodeOnly) == 0) ? this : bm;
        }
        int nextShift = shift + BIT_PARTITION_SIZE;

        // When the result can still be one of the two nodes, the children both hold are united first, so that
        // nothing is built when it is: this node when `bm` brings no other entry, no other key or value object, and
        // every united child is the child of this node (an entry of `bm` facing a child of this node is then
        // looked up in it); else `bm` when this node brings no other entry and every united child counts as the
        // child of `bm`. A union is this node's child whenever `bm`'s child brings nothing to it, so it counts as
        // `bm`'s child when it is that child, or when it is this node's child of the same size: the two children
        // then hold the same key and value objects. Once neither holds, the children united so far are kept for the
        // build below, which reads them instead of uniting them again.
        boolean asThis = !overwritesChangeThis
                && (rightDataOnly | rightNodeOnly | leftDataRightNode | leftDataRightDataMigrateToNode) == 0;
        boolean asThat =
                (leftDataOnly | leftNodeOnly | leftDataRightNode | leftNodeRightData | leftDataRightDataMigrateToNode)
                        == 0;
        MapNode<K, V>[] united = null;
        if (asThis || asThat) {
            int bits = leftNodeRightNode;
            for (int k = 0; bits != 0; k++) {
                int bit = Integer.lowestOneBit(bits);
                bits ^= bit;
                MapNode<K, V> leftNode = getNode(indexFrom(nodeMap, bit));
                MapNode<K, V> rightNode = bm.getNode(indexFrom(bm.nodeMap, bit));
                MapNode<K, V> newNode = leftNode.concat(rightNode, nextShift);
                if (united == null) {
                    boolean keepsThis = asThis && newNode == leftNode;
                    boolean keepsThat = asThat && keepsChildOf(newNode, leftNode, rightNode);
                    if (!keepsThis && !keepsThat) {
                        united = unitedSoFar(bm, leftNodeRightNode, k, asThis);
                    }
                    asThis = keepsThis;
                    asThat = keepsThat;
                }
                if (united != null) {
                    united[k] = newNode;
                }
            }
            if (asThis && holdsEntriesOf(bm, leftNodeRightData, nextShift)) {
                return this;
            } else if (asThat) {
                return bm;
            } else if (united == null) {
                // every united child is the child of this node, but an entry of `bm` changes one of its children
                united = unitedSoFar(bm, leftNodeRightNode, bitCount(leftNodeRightNode), true);
            }
        }

        int newDataSize = bitCount(newDataMap);
        int newContentSize = 2 * newDataSize + bitCount(newNodeMap);
        Object[] newContent = new Object[newContentSize];
        int[] newHashes = new int[newDataSize];
        int newSize = 0;
        int newKeyHashSum = 0;

        int leftDataIdx = 0;
        int rightDataIdx = 0;
        int leftNodeIdx = 0;
        int rightNodeIdx = 0;
        int unitedIdx = 0;
        int compressedDataIdx = 0;
        int compressedNodeIdx = 0;
        bitpos = minimumBitPos;
        while (true) {
            if ((bitpos & leftNodeRightNode) != 0) {
                MapNode<K, V> leftNode = getNode(leftNodeIdx);
                MapNode<K, V> rightNode = bm.getNode(rightNodeIdx);
                MapNode<K, V> newNode = (united != null) ? united[unitedIdx++] : leftNode.concat(rightNode, nextShift);
                if (!keepsChildOf(newNode, leftNode, rightNode)) {
                    anyChangesMadeSoFar = true;
                }
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                rightNodeIdx++;
                leftNodeIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataRightNode) != 0) {
                MapNode<K, V> n = bm.getNode(rightNodeIdx);
                MapNode<K, V> newNode =
                        n.updated(getKey(leftDataIdx), getValue(leftDataIdx), hashes[leftDataIdx], nextShift, false);
                if (newNode != n) {
                    anyChangesMadeSoFar = true;
                }
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                rightNodeIdx++;
                leftDataIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftNodeRightData) != 0) {
                anyChangesMadeSoFar = true;
                MapNode<K, V> newNode = getNode(leftNodeIdx)
                        .updated(
                                bm.getKey(rightDataIdx),
                                bm.getValue(rightDataIdx),
                                bm.hashes[rightDataIdx],
                                nextShift,
                                true);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                leftNodeIdx++;
                rightDataIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataOnly) != 0) {
                anyChangesMadeSoFar = true;
                newContent[2 * compressedDataIdx] = content[2 * leftDataIdx];
                newContent[2 * compressedDataIdx + 1] = content[2 * leftDataIdx + 1];
                newHashes[compressedDataIdx] = hashes[leftDataIdx];
                newKeyHashSum += hashes[leftDataIdx];
                compressedDataIdx++;
                leftDataIdx++;
                newSize++;
            } else if ((bitpos & rightDataOnly) != 0) {
                newContent[2 * compressedDataIdx] = bm.content[2 * rightDataIdx];
                newContent[2 * compressedDataIdx + 1] = bm.content[2 * rightDataIdx + 1];
                newHashes[compressedDataIdx] = bm.hashes[rightDataIdx];
                newKeyHashSum += bm.hashes[rightDataIdx];
                compressedDataIdx++;
                rightDataIdx++;
                newSize++;
            } else if ((bitpos & leftNodeOnly) != 0) {
                anyChangesMadeSoFar = true;
                MapNode<K, V> newNode = getNode(leftNodeIdx);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                leftNodeIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & rightNodeOnly) != 0) {
                MapNode<K, V> newNode = bm.getNode(rightNodeIdx);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                rightNodeIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataRightDataMigrateToNode) != 0) {
                anyChangesMadeSoFar = true;
                MapNode<K, V> newNode = mergeTwoKeyValPairs(
                        null,
                        getKey(leftDataIdx),
                        getValue(leftDataIdx),
                        hashes[leftDataIdx],
                        bm.getKey(rightDataIdx),
                        bm.getValue(rightDataIdx),
                        bm.hashes[rightDataIdx],
                        nextShift);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                leftDataIdx++;
                rightDataIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataRightDataRightOverwrites) != 0) {
                newContent[2 * compressedDataIdx] = bm.content[2 * rightDataIdx];
                newContent[2 * compressedDataIdx + 1] = bm.content[2 * rightDataIdx + 1];
                newHashes[compressedDataIdx] = bm.hashes[rightDataIdx];
                newKeyHashSum += bm.hashes[rightDataIdx];
                compressedDataIdx++;
                rightDataIdx++;
                leftDataIdx++;
                newSize++;
            }
            if (bitpos == maximumBitPos) {
                break;
            }
            bitpos <<= 1;
        }
        return anyChangesMadeSoFar
                ? new BitmapIndexedMapNode<>(
                        null, newDataMap, newNodeMap, newContent, newHashes, newSize, newKeyHashSum)
                : bm;
    }

    // `true` when `newNode`, the union of `leftNode` and `rightNode`, holds the very entries of `rightNode` and no
    // other: it is `rightNode`, or `leftNode` (to which `rightNode` then brings nothing) of the same size
    private static <K extends @Nullable Object, V extends @Nullable Object> boolean keepsChildOf(
            MapNode<K, V> newNode, MapNode<K, V> leftNode, MapNode<K, V> rightNode) {
        return newNode == rightNode || (newNode == leftNode && leftNode.size() == rightNode.size());
    }

    // `true` when each entry of `bm` in a slot of `slots`, where this node has a child, is the very key and value
    // object that child holds
    @SuppressWarnings("Var")
    private boolean holdsEntriesOf(BitmapIndexedMapNode<K, V> bm, int slots, int nextShift) {
        int bits = slots;
        while (bits != 0) {
            int bit = Integer.lowestOneBit(bits);
            bits ^= bit;
            MapNode<K, V> child = getNode(indexFrom(nodeMap, bit));
            int index = indexFrom(bm.dataMap, bit);
            if (!child.holdsEntry(bm.getKey(index), bm.getValue(index), bm.hashes[index], nextShift)) {
                return false;
            }
        }
        return true;
    }

    // the array of the united children of `concat`, holding its first `count` ones: the children of this node when
    // `ofThis`, else those of `bm`, which is what each of them united to
    @SuppressWarnings("Var")
    private MapNode<K, V>[] unitedSoFar(
            BitmapIndexedMapNode<K, V> bm, int leftNodeRightNode, int count, boolean ofThis) {
        @SuppressWarnings("unchecked")
        MapNode<K, V>[] united = (MapNode<K, V>[]) new MapNode<?, ?>[bitCount(leftNodeRightNode)];
        int bits = leftNodeRightNode;
        for (int k = 0; k < count; k++) {
            int bit = Integer.lowestOneBit(bits);
            bits ^= bit;
            united[k] = ofThis ? getNode(indexFrom(nodeMap, bit)) : bm.getNode(indexFrom(bm.nodeMap, bit));
        }
        return united;
    }

    @Override
    @SuppressWarnings("Var")
    public BitmapIndexedMapNode<K, V> filter(BiPredicate<? super K, ? super V> predicate, boolean keep) {
        int payload = payloadArity();
        int children = nodeArity();
        // the entries first, then the children, as the iteration goes
        int keptDataMap = 0;
        int newSize = 0;
        int newKeyHashSum = 0;
        int bits = dataMap;
        for (int i = 0; i < payload; i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if (predicate.test(getKey(i), getValue(i)) == keep) {
                keptDataMap |= bitpos;
                newSize++;
                newKeyHashSum += hashes[i];
            }
        }
        MapNode<K, V>[] newChildren = null;
        int newNodeMap = 0;
        // the children down to one entry, whose entry comes back inline
        int migratedDataMap = 0;
        bits = nodeMap;
        for (int i = 0; i < children; i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            MapNode<K, V> child = getNode(i);
            MapNode<K, V> newChild = child.filter(predicate, keep);
            int childSize = newChild.size();
            // a dropped child is neither inline nor a child of the result, so it needs no array
            if (newChild != child && childSize != 0 && newChildren == null) {
                @SuppressWarnings("unchecked")
                MapNode<K, V>[] array = (MapNode<K, V>[]) new MapNode<?, ?>[children];
                newChildren = array;
            }
            if (newChildren != null) {
                newChildren[i] = newChild;
            }
            if (childSize >= 2) {
                newNodeMap |= bitpos;
            } else if (childSize == 1) {
                migratedDataMap |= bitpos;
            }
            newSize += childSize;
            newKeyHashSum += newChild.keyHashSum();
        }
        if (newSize == size) {
            return this;
        } else if (newSize == 0) {
            return MapNode.empty();
        }
        int newDataMap = keptDataMap | migratedDataMap;
        int newDataSize = bitCount(newDataMap);
        Object[] newContent = new Object[2 * newDataSize + bitCount(newNodeMap)];
        int[] newHashes = new int[newDataSize];
        int dataIdx = 0;
        int nodeIdx = 0;
        int oldDataIdx = 0;
        int oldNodeIdx = 0;
        bits = dataMap | nodeMap;
        while (bits != 0) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if ((bitpos & dataMap) != 0) {
                if ((bitpos & keptDataMap) != 0) {
                    newContent[2 * dataIdx] = content[2 * oldDataIdx];
                    newContent[2 * dataIdx + 1] = content[2 * oldDataIdx + 1];
                    newHashes[dataIdx++] = hashes[oldDataIdx];
                }
                oldDataIdx++;
            } else {
                // a dropped child is in neither map, so only children kept in full or in part are read here
                if ((bitpos & migratedDataMap) != 0) {
                    MapNode<K, V> newChild = newChild(newChildren, oldNodeIdx);
                    newContent[2 * dataIdx] = newChild.getKey(0);
                    newContent[2 * dataIdx + 1] = newChild.getValue(0);
                    newHashes[dataIdx++] = newChild.getHash(0);
                } else if ((bitpos & newNodeMap) != 0) {
                    newContent[newContent.length - 1 - nodeIdx++] = newChild(newChildren, oldNodeIdx);
                }
                oldNodeIdx++;
            }
        }
        return new BitmapIndexedMapNode<>(null, newDataMap, newNodeMap, newContent, newHashes, newSize, newKeyHashSum);
    }

    /// The partition of this trie, as two roots: see [MapNode#partition].
    public MapNode.Partition<K, V> partition(BiPredicate<? super K, ? super V> predicate) {
        MapNode.Partition<K, V> into = new MapNode.Partition<>(this);
        partition(predicate, into);
        return into;
    }

    // the walk of `filter`, with the children's two results read from `into` and both sides rebuilt from them
    @Override
    @SuppressWarnings("Var")
    void partition(BiPredicate<? super K, ? super V> predicate, MapNode.Partition<K, V> into) {
        int payload = payloadArity();
        int keptDataMap = 0;
        int bits = dataMap;
        for (int i = 0; i < payload; i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if (predicate.test(getKey(i), getValue(i))) {
                keptDataMap |= bitpos;
            }
        }
        int children = nodeArity();
        MapNode<K, V>[] keptChildren = null;
        MapNode<K, V>[] rejectedChildren = null;
        // the children of which a side gets nothing: no array of new children until one is only partly on that side
        int keptDroppedNodeMap = 0;
        int rejectedDroppedNodeMap = 0;
        bits = nodeMap;
        for (int i = 0; i < children; i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            MapNode<K, V> child = getNode(i);
            child.partition(predicate, into);
            if (into.kept.size() == 0) {
                keptDroppedNodeMap |= bitpos;
            } else {
                keptChildren = withChild(keptChildren, children, i, child, into.kept);
            }
            if (into.rejected.size() == 0) {
                rejectedDroppedNodeMap |= bitpos;
            } else {
                rejectedChildren = withChild(rejectedChildren, children, i, child, into.rejected);
            }
        }
        into.kept = rebuilt(keptDataMap, keptDroppedNodeMap, keptChildren);
        into.rejected = rebuilt(dataMap ^ keptDataMap, rejectedDroppedNodeMap, rejectedChildren);
    }

    // records the new child at `index`, not empty, once one child has changed into a non-empty node: null while
    // every child is the old one or dropped
    private static <K extends @Nullable Object, V extends @Nullable Object> MapNode<K, V> @Nullable [] withChild(
            MapNode<K, V> @Nullable [] newChildren,
            int children,
            int index,
            MapNode<K, V> child,
            MapNode<K, V> newChild) {
        if (newChildren != null) {
            newChildren[index] = newChild;
            return newChildren;
        }
        if (newChild == child) {
            return null;
        }
        @SuppressWarnings("unchecked")
        MapNode<K, V>[] array = (MapNode<K, V>[]) new MapNode<?, ?>[children];
        array[index] = newChild;
        return array;
    }

    // the node of the inline entries of `keptDataMap` and of the children: none of those of `droppedNodeMap`, the
    // new ones of `newChildren` and the old ones elsewhere (null entries, or a null array), where a child down to one
    // entry comes back inline; this node when nothing was dropped
    @SuppressWarnings("Var")
    private BitmapIndexedMapNode<K, V> rebuilt(
            int keptDataMap, int droppedNodeMap, MapNode<K, V> @Nullable [] newChildren) {
        if (keptDataMap == dataMap && droppedNodeMap == 0 && newChildren == null) {
            return this;
        } else if (keptDataMap == 0 && droppedNodeMap == nodeMap) {
            return MapNode.empty();
        }
        int newNodeMap = 0;
        int migratedDataMap = 0;
        int newSize = 0;
        int newKeyHashSum = 0;
        int payload = payloadArity();
        int bits = dataMap;
        for (int i = 0; i < payload; i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if ((bitpos & keptDataMap) != 0) {
                newSize++;
                newKeyHashSum += hashes[i];
            }
        }
        bits = nodeMap;
        int children = nodeArity();
        for (int i = 0; i < children; i++) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if ((bitpos & droppedNodeMap) != 0) {
                continue;
            }
            MapNode<K, V> child = newChild(newChildren, i);
            int childSize = child.size();
            if (childSize >= 2) {
                newNodeMap |= bitpos;
            } else {
                migratedDataMap |= bitpos;
            }
            newSize += childSize;
            newKeyHashSum += child.keyHashSum();
        }
        if (newSize == size) {
            return this;
        } else if (newSize == 0) {
            return MapNode.empty();
        }
        int newDataMap = keptDataMap | migratedDataMap;
        int newDataSize = bitCount(newDataMap);
        Object[] newContent = new Object[2 * newDataSize + bitCount(newNodeMap)];
        int[] newHashes = new int[newDataSize];
        int dataIdx = 0;
        int nodeIdx = 0;
        int oldDataIdx = 0;
        int oldNodeIdx = 0;
        bits = dataMap | nodeMap;
        while (bits != 0) {
            int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if ((bitpos & dataMap) != 0) {
                if ((bitpos & keptDataMap) != 0) {
                    newContent[2 * dataIdx] = content[2 * oldDataIdx];
                    newContent[2 * dataIdx + 1] = content[2 * oldDataIdx + 1];
                    newHashes[dataIdx++] = hashes[oldDataIdx];
                }
                oldDataIdx++;
            } else {
                if ((bitpos & migratedDataMap) != 0) {
                    MapNode<K, V> child = newChild(newChildren, oldNodeIdx);
                    newContent[2 * dataIdx] = child.getKey(0);
                    newContent[2 * dataIdx + 1] = child.getValue(0);
                    newHashes[dataIdx++] = child.getHash(0);
                } else if ((bitpos & newNodeMap) != 0) {
                    newContent[newContent.length - 1 - nodeIdx++] = newChild(newChildren, oldNodeIdx);
                }
                oldNodeIdx++;
            }
        }
        return new BitmapIndexedMapNode<>(null, newDataMap, newNodeMap, newContent, newHashes, newSize, newKeyHashSum);
    }

    private MapNode<K, V> newChild(MapNode<K, V> @Nullable [] newChildren, int index) {
        MapNode<K, V> child = (newChildren == null) ? null : newChildren[index];
        return (child == null) ? getNode(index) : child;
    }

    @Override
    public <W extends @Nullable Object> BitmapIndexedMapNode<K, W> transform(
            BiFunction<? super K, ? super V, ? extends W> f) {
        @SuppressWarnings("Var")
        Object[] newContent = null;
        int payload = payloadArity();
        for (int i = 0; i < payload; i++) {
            W value = Objects.requireNonNull(f.apply(getKey(i), getValue(i)), "HashMap: value is null");
            if (newContent == null && value != content[2 * i + 1]) {
                newContent = content.clone();
            }
            if (newContent != null) {
                newContent[2 * i + 1] = value;
            }
        }
        int children = nodeArity();
        for (int i = 0; i < children; i++) {
            MapNode<K, V> child = getNode(i);
            MapNode<K, W> newChild = child.transform(f);
            if (newContent == null && newChild != (Object) child) {
                newContent = content.clone();
            }
            if (newContent != null) {
                newContent[content.length - 1 - i] = newChild;
            }
        }
        if (newContent == null) {
            @SuppressWarnings("unchecked")
            BitmapIndexedMapNode<K, W> unchanged = (BitmapIndexedMapNode<K, W>) this;
            return unchanged;
        }
        return new BitmapIndexedMapNode<>(null, dataMap, nodeMap, newContent, hashes, size, keyHashSum);
    }

    // -- the updates in place of a builder

    @Override
    BitmapIndexedMapNode<K, V> putInPlace(Object owner, K key, V value, int hash, int shift) {
        BitmapIndexedMapNode<K, V> node = (this.owner == owner)
                ? this
                : new BitmapIndexedMapNode<>(owner, dataMap, nodeMap, content.clone(), hashes, size, keyHashSum);
        node.update(owner, key, value, hash, shift);
        return node;
    }

    // `this` is owned by `owner`; its hashes array is never written in place, only replaced, so it may be shared
    private void update(Object owner, K key, V value, int hash, int shift) {
        int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            int index = indexFrom(dataMap, bitpos);
            K key0 = getKey(index);
            int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(key0, key)) {
                content[2 * index] = key;
                content[2 * index + 1] = value;
            } else {
                MapNode<K, V> subNodeNew = mergeTwoKeyValPairs(
                        owner, key0, getValue(index), hash0, key, value, hash, shift + BIT_PARTITION_SIZE);
                Object[] newContent = migratedFromInlineToNode(bitpos, subNodeNew);
                hashes = removeElement(hashes, index);
                content = newContent;
                dataMap ^= bitpos;
                nodeMap |= bitpos;
                size = size - 1 + subNodeNew.size();
                keyHashSum = keyHashSum - hash0 + subNodeNew.keyHashSum();
            }
        } else if ((nodeMap & bitpos) != 0) {
            int slot = content.length - 1 - nodeIndex(bitpos);
            @SuppressWarnings("unchecked")
            MapNode<K, V> subNode = (MapNode<K, V>) content[slot];
            // read before the put, which may update the child in place
            int subNodeSize = subNode.size();
            int subNodeHashSum = subNode.keyHashSum();
            MapNode<K, V> subNodeNew = subNode.putInPlace(owner, key, value, hash, shift + BIT_PARTITION_SIZE);
            content[slot] = subNodeNew;
            size = size - subNodeSize + subNodeNew.size();
            keyHashSum = keyHashSum - subNodeHashSum + subNodeNew.keyHashSum();
        } else {
            int dataIx = dataIndex(bitpos);
            int idx = 2 * dataIx;
            Object[] src = content;
            Object[] dst = new Object[src.length + 2];
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
