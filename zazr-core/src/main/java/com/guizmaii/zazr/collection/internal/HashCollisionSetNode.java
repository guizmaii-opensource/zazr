package com.guizmaii.zazr.collection.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/// The elements of a `HashSet` trie that have one same hash: a leaf below the last level of hash bits, holding at least
/// two elements. Ported from `HashCollisionSetNode` in `scala/collection/immutable/HashSet.scala` of the Scala 3
/// standard library (the Scala 2.13 collection library, which Scala 3 ships unchanged), with the elements kept in an
/// array instead of a vector. Immutable: every update copies the array, in a builder too.
///
/// @param <T> the element type
final class HashCollisionSetNode<T extends @Nullable Object> extends SetNode<T> {

    final int hash;
    // in insertion order
    final Object[] content;

    HashCollisionSetNode(int hash, Object[] content) {
        this.hash = hash;
        this.content = content;
    }

    // the index of `element`, or -1
    int indexOf(@Nullable Object element) {
        for (int i = 0; i < content.length; i++) {
            if (Objects.equals(content[i], element)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    @Override
    T getPayload(int index) {
        return (T) content[index];
    }

    @Override
    int getHash(int index) {
        return hash;
    }

    @Override
    SetNode<T> getNode(int index) {
        throw new IndexOutOfBoundsException("No sub-nodes present in hash-collision leaf node.");
    }

    @Override
    boolean hasNodes() {
        return false;
    }

    @Override
    int nodeArity() {
        return 0;
    }

    @Override
    boolean hasPayload() {
        return true;
    }

    @Override
    int payloadArity() {
        return content.length;
    }

    @Override
    public int size() {
        return content.length;
    }

    @Override
    int keyHashSum() {
        return content.length * hash;
    }

    @Override
    boolean contains(T element, int hash, int shift) {
        return this.hash == hash && indexOf(element) >= 0;
    }

    @Override
    SetNode<T> updated(T element, int hash, int shift, boolean replace) {
        final int index = indexOf(element);
        if (index >= 0) {
            if (replace && content[index] != element) {
                final Object[] dst = content.clone();
                dst[index] = element;
                return new HashCollisionSetNode<>(this.hash, dst);
            } else {
                return this;
            }
        } else {
            final Object[] dst = Arrays.copyOf(content, content.length + 1);
            dst[content.length] = element;
            return new HashCollisionSetNode<>(this.hash, dst);
        }
    }

    @Override
    SetNode<T> removed(T element, int hash, int shift) {
        final int index = (this.hash == hash) ? indexOf(element) : -1;
        if (index < 0) {
            return this;
        } else if (content.length == 2) {
            // one element left: a node of the root level, to be inlined by the parent
            return new BitmapIndexedSetNode<>(null, bitposFrom(maskFrom(this.hash, 0)), 0, new Object[] { content[1 - index] },
                    new int[] { this.hash }, 1, this.hash);
        } else {
            final Object[] dst = new Object[content.length - 1];
            System.arraycopy(content, 0, dst, 0, index);
            System.arraycopy(content, index + 1, dst, index, content.length - index - 1);
            return new HashCollisionSetNode<>(this.hash, dst);
        }
    }

    @Override
    SetNode<T> addInPlace(Object owner, T element, int hash, int shift) {
        return updated(element, hash, shift, false);
    }

    // `that` is a collision node too: two nodes at the same place below the last level hold elements of one hash
    @Override
    SetNode<T> concat(SetNode<T> that, int shift) {
        final HashCollisionSetNode<T> right = (HashCollisionSetNode<T>) that;
        if (right == this) {
            return this;
        }
        Object[] result = null;
        int length = right.content.length;
        for (Object element : content) {
            if (right.indexOf(element) < 0) {
                if (result == null) {
                    result = Arrays.copyOf(right.content, right.content.length + content.length);
                }
                result[length++] = element;
            }
        }
        return (result == null) ? right : new HashCollisionSetNode<>(hash, Arrays.copyOf(result, length));
    }

    @Override
    SetNode<T> filter(Predicate<? super T> predicate, boolean keep) {
        final Object[] kept = new Object[content.length];
        int length = 0;
        for (int i = 0; i < content.length; i++) {
            if (predicate.test(getPayload(i)) == keep) {
                kept[length++] = content[i];
            }
        }
        return withContent(kept, length);
    }

    @Override
    SetNode<T> diff(SetNode<T> that, int shift) {
        final Object[] kept = new Object[content.length];
        int length = 0;
        for (int i = 0; i < content.length; i++) {
            if (!that.contains(getPayload(i), hash, shift)) {
                kept[length++] = content[i];
            }
        }
        return withContent(kept, length);
    }

    // this node when all `length` elements of `kept` are kept, otherwise the node of them
    private SetNode<T> withContent(Object[] kept, int length) {
        if (length == content.length) {
            return this;
        } else if (length == 0) {
            return SetNode.empty();
        } else if (length == 1) {
            // one element left: a node of the root level, to be inlined by the parent
            return new BitmapIndexedSetNode<>(null, bitposFrom(maskFrom(hash, 0)), 0, new Object[] { kept[0] }, new int[] { hash }, 1, hash);
        } else {
            return new HashCollisionSetNode<>(hash, Arrays.copyOf(kept, length));
        }
    }

    @Override
    boolean subsetOf(SetNode<T> that, int shift) {
        if (this == that) {
            return true;
        } else if (that instanceof HashCollisionSetNode<T> node && content.length <= node.content.length) {
            for (Object element : content) {
                if (node.indexOf(element) < 0) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
