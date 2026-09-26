package com.guizmaii.zazr.collection.internal;

import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

import static java.lang.Integer.bitCount;

/// The node of a `HashSet` trie that splits its 32 slots between inline elements and children (see [ChampNode]).
/// Ported from `BitmapIndexedSetNode` in `scala/collection/immutable/HashSet.scala` of the Scala 3 standard library (the
/// Scala 2.13 collection library, which Scala 3 ships unchanged).
///
/// `content` holds the elements, `payloadArity` slots in slot order, then the children in reverse slot order; `hashes`
/// the hash of each element. The node caches the size and the sum of the hashes of its subtree.
///
/// The fields are not final: a node owned by a [HashSetBuilder] (its `owner` is the builder's token) is updated in
/// place until the builder's `result()`, which fences the writes before the trie is published. A node of a persistent
/// operation has no owner and never changes; a trie is only reached through the final field of its `HashSet` or of a
/// view, whose freeze covers the nodes.
///
/// @param <T> the element type
public final class BitmapIndexedSetNode<T extends @Nullable Object> extends SetNode<T> {

    static final BitmapIndexedSetNode<?> EMPTY = new BitmapIndexedSetNode<>(null, 0, 0, EMPTY_OBJECTS, EMPTY_INTS, 0, 0);

    int dataMap;
    int nodeMap;
    Object[] content;
    int[] hashes;
    int size;
    int keyHashSum;
    // the builder token that may update this node in place; null for a node of a persistent operation
    final @Nullable Object owner;

    BitmapIndexedSetNode(@Nullable Object owner, int dataMap, int nodeMap, Object[] content, int[] hashes, int size, int keyHashSum) {
        this.owner = owner;
        this.dataMap = dataMap;
        this.nodeMap = nodeMap;
        this.content = content;
        this.hashes = hashes;
        this.size = size;
        this.keyHashSum = keyHashSum;
    }

    // -- the entry points of HashSet

    /// `true` when `element` is present.
    public boolean contains(T element) {
        return contains(element, Objects.hashCode(element), 0);
    }

    /// The element kept for `element`, or null when absent.
    public @Nullable T find(T element) {
        return find(element, Objects.hashCode(element), 0);
    }

    /// The trie with `element`: an equal element present is replaced when `replace` is set, and kept (this trie
    /// returned) otherwise; this trie too when it is the same object.
    public BitmapIndexedSetNode<T> updated(T element, boolean replace) {
        return updated(element, Objects.hashCode(element), 0, replace);
    }

    /// The trie without `element`; this trie when it is absent.
    public BitmapIndexedSetNode<T> removed(T element) {
        return removed(element, Objects.hashCode(element), 0);
    }

    // -- accessors

    @SuppressWarnings("unchecked")
    @Override
    T getPayload(int index) {
        return (T) content[index];
    }

    @Override
    int getHash(int index) {
        return hashes[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    SetNode<T> getNode(int index) {
        return (SetNode<T>) content[content.length - 1 - index];
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

    /// The sum of the hashes of the elements: the hash of a `java.util.Set` of them.
    @Override
    public int keyHashSum() {
        return keyHashSum;
    }

    int dataIndex(int bitpos) {
        return bitCount(dataMap & (bitpos - 1));
    }

    int nodeIndex(int bitpos) {
        return bitCount(nodeMap & (bitpos - 1));
    }

    // -- lookup

    @Override
    boolean contains(T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            return hashes[index] == hash && Objects.equals(element, getPayload(index));
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).contains(element, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return false;
        }
    }

    @Override
    @Nullable T find(T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            return (hashes[index] == hash && Objects.equals(element, getPayload(index))) ? getPayload(index) : null;
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).find(element, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return null;
        }
    }

    // -- persistent updates

    @Override
    BitmapIndexedSetNode<T> updated(T element, int hash, int shift, boolean replace) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            final T element0 = getPayload(index);
            final int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(element0, element)) {
                if (replace && element0 != element) {
                    final Object[] dst = content.clone();
                    dst[index] = element;
                    return new BitmapIndexedSetNode<>(null, dataMap, nodeMap, dst, hashes, size, keyHashSum);
                } else {
                    return this;
                }
            } else {
                final SetNode<T> subNodeNew = mergeTwoKeyValPairs(null, element0, hash0, element, hash, shift + BIT_PARTITION_SIZE);
                return copyAndMigrateFromInlineToNode(bitpos, hash0, subNodeNew);
            }
        } else if ((nodeMap & bitpos) != 0) {
            final SetNode<T> subNode = getNode(indexFrom(nodeMap, bitpos));
            final SetNode<T> subNodeNew = subNode.updated(element, hash, shift + BIT_PARTITION_SIZE, replace);
            return (subNodeNew == subNode) ? this : copyAndSetNode(bitpos, subNode, subNodeNew);
        } else {
            return copyAndInsertValue(bitpos, element, hash);
        }
    }

    @Override
    BitmapIndexedSetNode<T> removed(T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            if (hashes[index] == hash && Objects.equals(getPayload(index), element)) {
                if (payloadArity() == 2 && nodeArity() == 0) {
                    // the node of the remaining element either becomes the root, or is inlined by the parent: its slot
                    // is the one of the root level
                    final int newDataMap = (shift == 0) ? (dataMap ^ bitpos) : bitposFrom(maskFrom(hash, 0));
                    final int other = 1 - index;
                    return new BitmapIndexedSetNode<>(null, newDataMap, 0, new Object[] { getPayload(other) },
                            new int[] { hashes[other] }, 1, hashes[other]);
                } else {
                    return copyAndRemoveValue(bitpos, hash);
                }
            } else {
                return this;
            }
        } else if ((nodeMap & bitpos) != 0) {
            final SetNode<T> subNode = getNode(indexFrom(nodeMap, bitpos));
            final SetNode<T> subNodeNew = subNode.removed(element, hash, shift + BIT_PARTITION_SIZE);
            if (subNodeNew == subNode) {
                return this;
            }
            if (subNodeNew.size() == 1) {
                if (size == subNode.size()) {
                    // the child was all this node held: the single remaining element goes up, to be inlined by the parent
                    return (BitmapIndexedSetNode<T>) subNodeNew;
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

    /// The node of two elements whose hashes agree up to `shift`, owned by `owner`: one node holding both when their
    /// fragments at `shift` differ, a chain of single-child nodes down to where they do, or a collision node below the
    /// last level.
    static <T extends @Nullable Object> SetNode<T> mergeTwoKeyValPairs(@Nullable Object owner, T element0, int hash0, T element1,
            int hash1, int shift) {
        if (shift >= HASH_CODE_LENGTH) {
            return new HashCollisionSetNode<>(hash0, new Object[] { element0, element1 });
        }
        final int mask0 = maskFrom(hash0, shift);
        final int mask1 = maskFrom(hash1, shift);
        if (mask0 != mask1) {
            final int dataMap = bitposFrom(mask0) | bitposFrom(mask1);
            final int keyHashSum = hash0 + hash1;
            if (mask0 < mask1) {
                return new BitmapIndexedSetNode<>(owner, dataMap, 0, new Object[] { element0, element1 }, new int[] { hash0, hash1 }, 2, keyHashSum);
            } else {
                return new BitmapIndexedSetNode<>(owner, dataMap, 0, new Object[] { element1, element0 }, new int[] { hash1, hash0 }, 2, keyHashSum);
            }
        } else {
            final SetNode<T> node = mergeTwoKeyValPairs(owner, element0, hash0, element1, hash1, shift + BIT_PARTITION_SIZE);
            return new BitmapIndexedSetNode<>(owner, 0, bitposFrom(mask0), new Object[] { node }, EMPTY_INTS, node.size(), node.keyHashSum());
        }
    }

    private BitmapIndexedSetNode<T> copyAndSetNode(int bitpos, SetNode<T> oldNode, SetNode<T> newNode) {
        final Object[] dst = content.clone();
        dst[dst.length - 1 - nodeIndex(bitpos)] = newNode;
        return new BitmapIndexedSetNode<>(null, dataMap, nodeMap, dst, hashes, size - oldNode.size() + newNode.size(),
                keyHashSum - oldNode.keyHashSum() + newNode.keyHashSum());
    }

    private BitmapIndexedSetNode<T> copyAndInsertValue(int bitpos, T element, int hash) {
        final int dataIx = dataIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length + 1];
        System.arraycopy(src, 0, dst, 0, dataIx);
        dst[dataIx] = element;
        System.arraycopy(src, dataIx, dst, dataIx + 1, src.length - dataIx);
        return new BitmapIndexedSetNode<>(null, dataMap | bitpos, nodeMap, dst, insertElement(hashes, dataIx, hash), size + 1,
                keyHashSum + hash);
    }

    private BitmapIndexedSetNode<T> copyAndRemoveValue(int bitpos, int hash) {
        final int dataIx = dataIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length - 1];
        System.arraycopy(src, 0, dst, 0, dataIx);
        System.arraycopy(src, dataIx + 1, dst, dataIx, src.length - dataIx - 1);
        return new BitmapIndexedSetNode<>(null, dataMap ^ bitpos, nodeMap, dst, removeElement(hashes, dataIx), size - 1,
                keyHashSum - hash);
    }

    // the content of this node with the element of `bitpos` moved out, and `node` inserted among the children
    private Object[] migratedFromInlineToNode(int bitpos, SetNode<T> node) {
        final int idxOld = dataIndex(bitpos);
        final int idxNew = content.length - 1 - nodeIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length];
        System.arraycopy(src, 0, dst, 0, idxOld);
        System.arraycopy(src, idxOld + 1, dst, idxOld, idxNew - idxOld);
        dst[idxNew] = node;
        System.arraycopy(src, idxNew + 1, dst, idxNew + 1, src.length - idxNew - 1);
        return dst;
    }

    private BitmapIndexedSetNode<T> copyAndMigrateFromInlineToNode(int bitpos, int hash, SetNode<T> node) {
        return new BitmapIndexedSetNode<>(null, dataMap ^ bitpos, nodeMap | bitpos, migratedFromInlineToNode(bitpos, node),
                removeElement(hashes, dataIndex(bitpos)), size - 1 + node.size(), keyHashSum - hash + node.keyHashSum());
    }

    private BitmapIndexedSetNode<T> copyAndMigrateFromNodeToInline(int bitpos, SetNode<T> oldNode, SetNode<T> node) {
        final int idxOld = content.length - 1 - nodeIndex(bitpos);
        final int idxNew = dataIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length];
        System.arraycopy(src, 0, dst, 0, idxNew);
        dst[idxNew] = node.getPayload(0);
        System.arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
        System.arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length - idxOld - 1);
        return new BitmapIndexedSetNode<>(null, dataMap | bitpos, nodeMap ^ bitpos, dst, insertElement(hashes, idxNew, node.getHash(0)),
                size - oldNode.size() + 1, keyHashSum - oldNode.keyHashSum() + node.keyHashSum());
    }

    // -- the operations on whole subtrees

    // the concat of BitmapIndexedMapNode, on elements: a first pass sorts each slot into one of nine cases, a second
    // builds the node
    @Override
    public BitmapIndexedSetNode<T> concat(SetNode<T> that, int shift) {
        final BitmapIndexedSetNode<T> bm = (BitmapIndexedSetNode<T>) that;
        if (size == 0) {
            return bm;
        } else if (bm.size == 0 || bm == this) {
            return this;
        } else if (bm.size == 1) {
            final BitmapIndexedSetNode<T> result = updated(bm.getPayload(0), bm.hashes[0], shift, true);
            // this node held only the element of `bm`, which wins: the result is `bm`
            return result.size == 1 ? bm : result;
        }
        // set as soon as the result differs from `bm`, which is returned otherwise
        boolean anyChangesMadeSoFar = false;
        final int allMap = dataMap | bm.dataMap | nodeMap | bm.nodeMap;
        // both inclusive
        final int minimumBitPos = bitposFrom(Integer.numberOfTrailingZeros(allMap));
        final int maximumBitPos = bitposFrom(BRANCHING_FACTOR - Integer.numberOfLeadingZeros(allMap) - 1);

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
                    final int leftHash = hashes[leftIdx];
                    if (leftHash == bm.hashes[rightIdx] && Objects.equals(getPayload(leftIdx), bm.getPayload(rightIdx))) {
                        leftDataRightDataRightOverwrites |= bitpos;
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

        final int newDataMap = leftDataOnly | rightDataOnly | leftDataRightDataRightOverwrites;
        final int newNodeMap = leftNodeRightNode | leftDataRightNode | leftNodeRightData | leftNodeOnly | rightNodeOnly
                               | dataToNodeMigrationTargets;
        if (newDataMap == (rightDataOnly | leftDataRightDataRightOverwrites) && newNodeMap == rightNodeOnly) {
            // nothing of this node makes it into the result
            return bm;
        }

        final int newDataSize = bitCount(newDataMap);
        final int newContentSize = newDataSize + bitCount(newNodeMap);
        final Object[] newContent = new Object[newContentSize];
        final int[] newHashes = new int[newDataSize];
        int newSize = 0;
        int newKeyHashSum = 0;

        int leftDataIdx = 0;
        int rightDataIdx = 0;
        int leftNodeIdx = 0;
        int rightNodeIdx = 0;
        final int nextShift = shift + BIT_PARTITION_SIZE;
        int compressedDataIdx = 0;
        int compressedNodeIdx = 0;
        bitpos = minimumBitPos;
        while (true) {
            if ((bitpos & leftNodeRightNode) != 0) {
                final SetNode<T> rightNode = bm.getNode(rightNodeIdx);
                final SetNode<T> newNode = getNode(leftNodeIdx).concat(rightNode, nextShift);
                if (rightNode != newNode) {
                    anyChangesMadeSoFar = true;
                }
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                rightNodeIdx++;
                leftNodeIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataRightNode) != 0) {
                final SetNode<T> n = bm.getNode(rightNodeIdx);
                final SetNode<T> newNode = n.updated(getPayload(leftDataIdx), hashes[leftDataIdx], nextShift, false);
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
                final SetNode<T> newNode = getNode(leftNodeIdx).updated(bm.getPayload(rightDataIdx), bm.hashes[rightDataIdx], nextShift, true);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                leftNodeIdx++;
                rightDataIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataOnly) != 0) {
                anyChangesMadeSoFar = true;
                newContent[compressedDataIdx] = content[leftDataIdx];
                newHashes[compressedDataIdx] = hashes[leftDataIdx];
                newKeyHashSum += hashes[leftDataIdx];
                compressedDataIdx++;
                leftDataIdx++;
                newSize++;
            } else if ((bitpos & rightDataOnly) != 0) {
                newContent[compressedDataIdx] = bm.content[rightDataIdx];
                newHashes[compressedDataIdx] = bm.hashes[rightDataIdx];
                newKeyHashSum += bm.hashes[rightDataIdx];
                compressedDataIdx++;
                rightDataIdx++;
                newSize++;
            } else if ((bitpos & leftNodeOnly) != 0) {
                anyChangesMadeSoFar = true;
                final SetNode<T> newNode = getNode(leftNodeIdx);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                leftNodeIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & rightNodeOnly) != 0) {
                final SetNode<T> newNode = bm.getNode(rightNodeIdx);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                rightNodeIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataRightDataMigrateToNode) != 0) {
                anyChangesMadeSoFar = true;
                final SetNode<T> newNode = mergeTwoKeyValPairs(null, getPayload(leftDataIdx), hashes[leftDataIdx],
                        bm.getPayload(rightDataIdx), bm.hashes[rightDataIdx], nextShift);
                newContent[newContentSize - compressedNodeIdx - 1] = newNode;
                compressedNodeIdx++;
                leftDataIdx++;
                rightDataIdx++;
                newSize += newNode.size();
                newKeyHashSum += newNode.keyHashSum();
            } else if ((bitpos & leftDataRightDataRightOverwrites) != 0) {
                newContent[compressedDataIdx] = bm.content[rightDataIdx];
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
               ? new BitmapIndexedSetNode<>(null, newDataMap, newNodeMap, newContent, newHashes, newSize, newKeyHashSum)
               : bm;
    }

    @Override
    public BitmapIndexedSetNode<T> filter(Predicate<? super T> predicate, boolean keep) {
        // the elements first, then the children, as the iteration goes
        final int payload = payloadArity();
        int keptDataMap = 0;
        int bits = dataMap;
        for (int i = 0; i < payload; i++) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if (predicate.test(getPayload(i)) == keep) {
                keptDataMap |= bitpos;
            }
        }
        final int children = nodeArity();
        SetNode<T>[] newChildren = null;
        for (int i = 0; i < children; i++) {
            final SetNode<T> child = getNode(i);
            final SetNode<T> newChild = child.filter(predicate, keep);
            newChildren = withChild(newChildren, children, i, child, newChild);
        }
        return rebuilt(keptDataMap, newChildren);
    }

    @Override
    public BitmapIndexedSetNode<T> diff(SetNode<T> that, int shift) {
        final BitmapIndexedSetNode<T> bm = (BitmapIndexedSetNode<T>) that;
        final int payload = payloadArity();
        int keptDataMap = 0;
        int bits = dataMap;
        for (int i = 0; i < payload; i++) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if (!bm.contains(getPayload(i), hashes[i], shift)) {
                keptDataMap |= bitpos;
            }
        }
        final int children = nodeArity();
        SetNode<T>[] newChildren = null;
        bits = nodeMap;
        for (int i = 0; i < children; i++) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            final SetNode<T> child = getNode(i);
            final SetNode<T> newChild;
            if ((bitpos & bm.dataMap) != 0) {
                final int index = indexFrom(bm.dataMap, bitpos);
                newChild = child.removed(bm.getPayload(index), bm.hashes[index], shift + BIT_PARTITION_SIZE);
            } else if ((bitpos & bm.nodeMap) != 0) {
                newChild = child.diff(bm.getNode(indexFrom(bm.nodeMap, bitpos)), shift + BIT_PARTITION_SIZE);
            } else {
                newChild = child;
            }
            newChildren = withChild(newChildren, children, i, child, newChild);
        }
        return rebuilt(keptDataMap, newChildren);
    }

    // records the new child at `index` once one child has changed: null while every child is the old one
    private static <T extends @Nullable Object> SetNode<T> @Nullable [] withChild(SetNode<T> @Nullable [] newChildren, int children,
            int index, SetNode<T> child, SetNode<T> newChild) {
        SetNode<T>[] result = newChildren;
        if (newChild != child && result == null) {
            @SuppressWarnings("unchecked")
            final SetNode<T>[] array = (SetNode<T>[]) new SetNode<?>[children];
            result = array;
        }
        if (result != null) {
            result[index] = newChild;
        }
        return result;
    }

    // the node of the inline elements of `keptDataMap` and of the new children (null entries, or a null array: the
    // old child), where a child down to one element comes back inline and an empty one goes; this node when nothing
    // was dropped
    private BitmapIndexedSetNode<T> rebuilt(int keptDataMap, SetNode<T> @Nullable [] newChildren) {
        if (keptDataMap == dataMap && newChildren == null) {
            return this;
        }
        int newNodeMap = 0;
        int migratedDataMap = 0;
        int newSize = 0;
        int newKeyHashSum = 0;
        final int payload = payloadArity();
        int bits = dataMap;
        for (int i = 0; i < payload; i++) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if ((bitpos & keptDataMap) != 0) {
                newSize++;
                newKeyHashSum += hashes[i];
            }
        }
        bits = nodeMap;
        final int children = nodeArity();
        for (int i = 0; i < children; i++) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            final SetNode<T> child = newChild(newChildren, i);
            final int childSize = child.size();
            if (childSize >= 2) {
                newNodeMap |= bitpos;
            } else if (childSize == 1) {
                migratedDataMap |= bitpos;
            }
            newSize += childSize;
            newKeyHashSum += child.keyHashSum();
        }
        if (newSize == size) {
            return this;
        } else if (newSize == 0) {
            return SetNode.empty();
        }
        final int newDataMap = keptDataMap | migratedDataMap;
        final int newDataSize = bitCount(newDataMap);
        final Object[] newContent = new Object[newDataSize + bitCount(newNodeMap)];
        final int[] newHashes = new int[newDataSize];
        int dataIdx = 0;
        int nodeIdx = 0;
        int oldDataIdx = 0;
        int oldNodeIdx = 0;
        bits = dataMap | nodeMap;
        while (bits != 0) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            if ((bitpos & dataMap) != 0) {
                if ((bitpos & keptDataMap) != 0) {
                    newContent[dataIdx] = content[oldDataIdx];
                    newHashes[dataIdx++] = hashes[oldDataIdx];
                }
                oldDataIdx++;
            } else {
                final SetNode<T> child = newChild(newChildren, oldNodeIdx);
                if ((bitpos & migratedDataMap) != 0) {
                    newContent[dataIdx] = child.getPayload(0);
                    newHashes[dataIdx++] = child.getHash(0);
                } else if ((bitpos & newNodeMap) != 0) {
                    newContent[newContent.length - 1 - nodeIdx++] = child;
                }
                oldNodeIdx++;
            }
        }
        return new BitmapIndexedSetNode<>(null, newDataMap, newNodeMap, newContent, newHashes, newSize, newKeyHashSum);
    }

    private SetNode<T> newChild(SetNode<T> @Nullable [] newChildren, int index) {
        final SetNode<T> child = (newChildren == null) ? null : newChildren[index];
        return (child == null) ? getNode(index) : child;
    }

    @Override
    public boolean subsetOf(SetNode<T> that, int shift) {
        if (this == that) {
            return true;
        }
        final BitmapIndexedSetNode<T> node = (BitmapIndexedSetNode<T>) that;
        final int thisBitmap = dataMap | nodeMap;
        final int nodeBitmap = node.dataMap | node.nodeMap;
        if ((thisBitmap | nodeBitmap) != nodeBitmap) {
            return false;
        }
        int bits = thisBitmap;
        while (bits != 0) {
            final int bitpos = Integer.lowestOneBit(bits);
            bits ^= bitpos;
            final boolean isSubset;
            if ((dataMap & bitpos) != 0) {
                final int index = indexFrom(dataMap, bitpos);
                if ((node.dataMap & bitpos) != 0) {
                    // an element against an element
                    final int thatIndex = indexFrom(node.dataMap, bitpos);
                    isSubset = hashes[index] == node.hashes[thatIndex] && Objects.equals(getPayload(index), node.getPayload(thatIndex));
                } else {
                    // an element against a child
                    isSubset = node.getNode(indexFrom(node.nodeMap, bitpos)).contains(getPayload(index), hashes[index], shift + BIT_PARTITION_SIZE);
                }
            } else {
                // a child against a child; a child against an element cannot be a subset, the child holding two
                isSubset = (node.dataMap & bitpos) == 0
                           && getNode(indexFrom(nodeMap, bitpos)).subsetOf(node.getNode(indexFrom(node.nodeMap, bitpos)), shift + BIT_PARTITION_SIZE);
            }
            if (!isSubset) {
                return false;
            }
        }
        return true;
    }

    // -- the updates in place of a builder

    @Override
    BitmapIndexedSetNode<T> addInPlace(Object owner, T element, int hash, int shift) {
        final BitmapIndexedSetNode<T> node = (this.owner == owner)
                                             ? this
                                             : new BitmapIndexedSetNode<>(owner, dataMap, nodeMap, content.clone(), hashes, size, keyHashSum);
        node.update(owner, element, hash, shift);
        return node;
    }

    // `this` is owned by `owner`; its hashes array is never written in place, only replaced, so it may be shared
    private void update(Object owner, T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            final T element0 = getPayload(index);
            final int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(element0, element)) {
                // the element already there is kept
                return;
            } else {
                final SetNode<T> subNodeNew = mergeTwoKeyValPairs(owner, element0, hash0, element, hash, shift + BIT_PARTITION_SIZE);
                // an element and a child take one slot each, so the owned array is reused: the elements after the
                // migrated one and the children before the new one shift left by one
                final int idxNew = content.length - 1 - nodeIndex(bitpos);
                System.arraycopy(content, index + 1, content, index, idxNew - index);
                content[idxNew] = subNodeNew;
                hashes = removeElement(hashes, index);
                dataMap ^= bitpos;
                nodeMap |= bitpos;
                size = size - 1 + subNodeNew.size();
                keyHashSum = keyHashSum - hash0 + subNodeNew.keyHashSum();
            }
        } else if ((nodeMap & bitpos) != 0) {
            final int slot = content.length - 1 - nodeIndex(bitpos);
            @SuppressWarnings("unchecked")
            final SetNode<T> subNode = (SetNode<T>) content[slot];
            // read before the addition, which may update the child in place
            final int subNodeSize = subNode.size();
            final int subNodeHashSum = subNode.keyHashSum();
            final SetNode<T> subNodeNew = subNode.addInPlace(owner, element, hash, shift + BIT_PARTITION_SIZE);
            content[slot] = subNodeNew;
            size = size - subNodeSize + subNodeNew.size();
            keyHashSum = keyHashSum - subNodeHashSum + subNodeNew.keyHashSum();
        } else {
            final int dataIx = dataIndex(bitpos);
            final Object[] src = content;
            final Object[] dst = new Object[src.length + 1];
            System.arraycopy(src, 0, dst, 0, dataIx);
            dst[dataIx] = element;
            System.arraycopy(src, dataIx, dst, dataIx + 1, src.length - dataIx);
            content = dst;
            hashes = insertElement(hashes, dataIx, hash);
            dataMap |= bitpos;
            size++;
            keyHashSum += hash;
        }
    }
}
