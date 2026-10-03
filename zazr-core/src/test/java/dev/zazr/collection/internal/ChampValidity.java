package dev.zazr.collection.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/// The invariants of the CHAMP tries of `HashMap` and `HashSet`, and the comparisons of two tries, for the tests.
final class ChampValidity {

    private ChampValidity() {}

    /// The hash code whose mixed hash ([ChampNode#improve]) is `mixed`: a key of that hash code sits in the slots the
    /// fragments of `mixed` name. Each step of the mixing is undone in reverse order.
    static int hashCodeFor(int mixed) {
        // h ^ (h >>> 10)
        int h1 = mixed ^ (mixed >>> 10) ^ (mixed >>> 20) ^ (mixed >>> 30);
        // h + (h << 4), a product by 17
        int h2 = h1 * inverse(17);
        // h ^ (h >>> 14)
        int h3 = h2 ^ (h2 >>> 14) ^ (h2 >>> 28);
        // hcode + ~(hcode << 9) == -511 * hcode - 1
        return (h3 + 1) * inverse(-511);
    }

    // the inverse of an odd number modulo 2^32, by Newton's iteration
    private static int inverse(int odd) {
        return IntStream.range(0, 5).reduce(odd, (x, i) -> x * (2 - odd * x));
    }

    // -- maps

    /// Asserts every invariant of a map trie, the canonical form included, and returns its size:
    /// - the bitmaps are disjoint, and the arrays are as long as they say (two slots per entry, one per child);
    /// - every entry sits in the slot of its hash fragment, under the fragments of its path, with its own hash
    ///   stored;
    /// - every child holds at least two entries (a single one would be inline), is a bitmap node above the last
    ///   level of hash bits and a collision node below it;
    /// - a collision node holds at least two distinct keys, all of its hash;
    /// - the cached sizes and hash sums are the recomputed ones.
    static int assertValid(BitmapIndexedMapNode<?, ?> root) {
        int size = assertValidMap(root, 0, 0, 0);
        checkEqual(root.size(), size, "size of the root");
        return size;
    }

    private static int assertValidMap(MapNode<?, ?> node, int shift, int path, int pathMask) {
        if (node instanceof BitmapIndexedMapNode<?, ?> n) {
            check(shift < ChampNode.HASH_CODE_LENGTH, "bitmap node above the last level");
            checkEqual(n.dataMap & n.nodeMap, 0, "disjoint bitmaps");
            int payload = Integer.bitCount(n.dataMap);
            int children = Integer.bitCount(n.nodeMap);
            checkEqual(n.content.length, 2 * payload + children, "length of the content array");
            checkEqual(n.hashes.length, payload, "length of the hashes array");
            for (int i = 0, bits = n.dataMap; i < payload; i++, bits &= bits - 1) {
                int fragment = Integer.numberOfTrailingZeros(bits);
                Object key = n.content[2 * i];
                Object value = n.content[2 * i + 1];
                check(key != null && !(key instanceof MapNode), "a key is a non-null element");
                check(value != null && !(value instanceof MapNode), "a value is a non-null element");
                checkEqual(n.hashes[i], Objects.hashCode(key), "stored hash of an entry");
                checkEqual(ChampNode.maskFrom(n.hashes[i], shift), fragment, "entry in the slot of its fragment");
                checkEqual(ChampNode.improve(n.hashes[i]) & pathMask, path, "entry under its path");
            }
            int[] childSizes = new int[children];
            int[] childHashSums = new int[children];
            for (int i = 0, bits = n.nodeMap; i < children; i++, bits &= bits - 1) {
                int fragment = Integer.numberOfTrailingZeros(bits);
                Object child = n.content[n.content.length - 1 - i];
                check(child instanceof MapNode, "a child is a node");
                MapNode<?, ?> c = (MapNode<?, ?>) child;
                int childShift = shift + ChampNode.BIT_PARTITION_SIZE;
                if (childShift < ChampNode.HASH_CODE_LENGTH) {
                    check(c instanceof BitmapIndexedMapNode, "a child above the last level is a bitmap node");
                } else {
                    check(c instanceof HashCollisionMapNode, "a child at the last level is a collision node");
                }
                int childSize = assertValidMap(
                        c, childShift, path | (fragment << shift), pathMask | (ChampNode.BIT_PARTITION_MASK << shift));
                check(childSize >= 2, "a child holds at least two entries");
                childSizes[i] = childSize;
                childHashSums[i] = c.keyHashSum();
            }
            int size = payload + IntStream.of(childSizes).sum();
            int hashSum =
                    IntStream.of(n.hashes).sum() + IntStream.of(childHashSums).sum();
            checkEqual(n.size, size, "size field");
            checkEqual(n.keyHashSum, hashSum, "keyHashSum field");
            return size;
        } else {
            HashCollisionMapNode<?, ?> n = (HashCollisionMapNode<?, ?>) node;
            checkEqual(n.content.length % 2, 0, "a collision node holds pairs");
            int size = n.content.length / 2;
            check(size >= 2, "a collision node holds at least two entries");
            checkEqual(ChampNode.improve(n.hash), path, "collision node under its path");
            java.util.Set<Object> keys = new java.util.HashSet<>();
            for (int i = 0; i < size; i++) {
                check(n.content[2 * i] != null, "a key of a collision node is not null");
                check(n.content[2 * i + 1] != null, "a value of a collision node is not null");
                checkEqual(Objects.hashCode(n.content[2 * i]), n.hash, "hash of a key of a collision node");
                check(keys.add(n.content[2 * i]), "distinct keys in a collision node");
            }
            checkEqual(n.keyHashSum(), size * n.hash, "keyHashSum of a collision node");
            return size;
        }
    }

    /// Asserts that two map tries have the same shape and hold the same key and value objects in the same slots; the
    /// order inside a collision node is ignored unless `collisionOrder` is set.
    static void assertSameShape(MapNode<?, ?> expected, MapNode<?, ?> actual, boolean collisionOrder) {
        check(actual.getClass() == expected.getClass(), "same node class");
        if (expected instanceof BitmapIndexedMapNode<?, ?> e) {
            BitmapIndexedMapNode<?, ?> a = (BitmapIndexedMapNode<?, ?>) actual;
            checkEqual(a.dataMap, e.dataMap, "dataMap");
            checkEqual(a.nodeMap, e.nodeMap, "nodeMap");
            checkEqual(a.size, e.size, "size field");
            checkEqual(a.keyHashSum, e.keyHashSum, "keyHashSum field");
            check(java.util.Arrays.equals(a.hashes, e.hashes), "hashes");
            checkEqual(a.content.length, e.content.length, "length of the content array");
            int payload = 2 * Integer.bitCount(e.dataMap);
            for (int i = 0; i < payload; i++) {
                checkSame(a.content[i], e.content[i], "key or value object");
            }
            for (int i = payload; i < e.content.length; i++) {
                assertSameShape((MapNode<?, ?>) e.content[i], (MapNode<?, ?>) a.content[i], collisionOrder);
            }
        } else {
            HashCollisionMapNode<?, ?> e = (HashCollisionMapNode<?, ?>) expected;
            HashCollisionMapNode<?, ?> a = (HashCollisionMapNode<?, ?>) actual;
            checkEqual(a.hash, e.hash, "hash of a collision node");
            checkEqual(a.content.length, e.content.length, "length of a collision node");
            if (collisionOrder) {
                for (int i = 0; i < e.content.length; i++) {
                    checkSame(a.content[i], e.content[i], "key or value object of a collision node");
                }
            } else {
                for (int i = 0; i < e.content.length; i += 2) {
                    int j = 2 * a.indexOf(e.content[i]);
                    check(j >= 0, "key of a collision node present");
                    checkSame(a.content[j], e.content[i], "key object of a collision node");
                    checkSame(a.content[j + 1], e.content[i + 1], "value object of a collision node");
                }
            }
        }
    }

    /// The bitmap nodes of a map trie, by identity.
    static Set<Object> internalNodes(MapNode<?, ?> node) {
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        collectMapNodes(node, result);
        return result;
    }

    private static void collectMapNodes(MapNode<?, ?> node, Set<Object> result) {
        if (node instanceof BitmapIndexedMapNode<?, ?> n) {
            result.add(n);
            for (int i = 2 * Integer.bitCount(n.dataMap); i < n.content.length; i++) {
                collectMapNodes((MapNode<?, ?>) n.content[i], result);
            }
        }
    }

    /// A description of every node of a map trie, by identity, with its fields and the identity of its arrays and
    /// entries: equal descriptions taken before and after an operation mean that nothing reachable was changed.
    static String describe(MapNode<?, ?> node) {
        StringBuilder out = new StringBuilder();
        describeMap(node, out);
        return out.toString();
    }

    private static void describeMap(MapNode<?, ?> node, StringBuilder out) {
        out.append(node.getClass().getSimpleName()).append('@').append(System.identityHashCode(node));
        if (node instanceof BitmapIndexedMapNode<?, ?> n) {
            out.append("[d=")
                    .append(n.dataMap)
                    .append(",n=")
                    .append(n.nodeMap)
                    .append(",s=")
                    .append(n.size)
                    .append(",h=")
                    .append(n.keyHashSum)
                    .append(",c@")
                    .append(System.identityHashCode(n.content))
                    .append(",hs@")
                    .append(System.identityHashCode(n.hashes))
                    .append(java.util.Arrays.toString(n.hashes))
                    .append('(');
            int payload = 2 * Integer.bitCount(n.dataMap);
            for (int i = 0; i < n.content.length; i++) {
                if (i < payload) {
                    out.append(System.identityHashCode(n.content[i]));
                } else {
                    describeMap((MapNode<?, ?>) n.content[i], out);
                }
                out.append(' ');
            }
            out.append(")]");
        } else {
            HashCollisionMapNode<?, ?> n = (HashCollisionMapNode<?, ?>) node;
            out.append("[c@").append(System.identityHashCode(n.content)).append('(');
            for (Object o : n.content) {
                out.append(System.identityHashCode(o)).append(' ');
            }
            out.append(")]");
        }
    }

    /// The entries of a map trie, key and value objects, in iteration order.
    static java.util.List<Object[]> entries(MapNode<?, ?> root) {
        java.util.List<Object[]> result = new ArrayList<>();
        root.iterator((k, v) -> new Object[] {k, v}).forEachRemaining(result::add);
        return result;
    }

    // -- sets

    /// Asserts every invariant of a set trie, as [#assertValid(BitmapIndexedMapNode)] does for a map, and returns its
    /// size.
    static int assertValid(BitmapIndexedSetNode<?> root) {
        int size = assertValidSet(root, 0, 0, 0);
        checkEqual(root.size(), size, "size of the root");
        return size;
    }

    private static int assertValidSet(SetNode<?> node, int shift, int path, int pathMask) {
        if (node instanceof BitmapIndexedSetNode<?> n) {
            check(shift < ChampNode.HASH_CODE_LENGTH, "bitmap node above the last level");
            checkEqual(n.dataMap & n.nodeMap, 0, "disjoint bitmaps");
            int payload = Integer.bitCount(n.dataMap);
            int children = Integer.bitCount(n.nodeMap);
            checkEqual(n.content.length, payload + children, "length of the content array");
            checkEqual(n.hashes.length, payload, "length of the hashes array");
            for (int i = 0, bits = n.dataMap; i < payload; i++, bits &= bits - 1) {
                int fragment = Integer.numberOfTrailingZeros(bits);
                Object element = n.content[i];
                check(element != null && !(element instanceof SetNode), "an element is a non-null element");
                checkEqual(n.hashes[i], Objects.hashCode(element), "stored hash of an element");
                checkEqual(ChampNode.maskFrom(n.hashes[i], shift), fragment, "element in the slot of its fragment");
                checkEqual(ChampNode.improve(n.hashes[i]) & pathMask, path, "element under its path");
            }
            int[] childSizes = new int[children];
            int[] childHashSums = new int[children];
            for (int i = 0, bits = n.nodeMap; i < children; i++, bits &= bits - 1) {
                int fragment = Integer.numberOfTrailingZeros(bits);
                Object child = n.content[n.content.length - 1 - i];
                check(child instanceof SetNode, "a child is a node");
                SetNode<?> c = (SetNode<?>) child;
                int childShift = shift + ChampNode.BIT_PARTITION_SIZE;
                if (childShift < ChampNode.HASH_CODE_LENGTH) {
                    check(c instanceof BitmapIndexedSetNode, "a child above the last level is a bitmap node");
                } else {
                    check(c instanceof HashCollisionSetNode, "a child at the last level is a collision node");
                }
                int childSize = assertValidSet(
                        c, childShift, path | (fragment << shift), pathMask | (ChampNode.BIT_PARTITION_MASK << shift));
                check(childSize >= 2, "a child holds at least two elements");
                childSizes[i] = childSize;
                childHashSums[i] = c.keyHashSum();
            }
            int size = payload + IntStream.of(childSizes).sum();
            int hashSum =
                    IntStream.of(n.hashes).sum() + IntStream.of(childHashSums).sum();
            checkEqual(n.size, size, "size field");
            checkEqual(n.keyHashSum, hashSum, "keyHashSum field");
            return size;
        } else {
            HashCollisionSetNode<?> n = (HashCollisionSetNode<?>) node;
            check(n.content.length >= 2, "a collision node holds at least two elements");
            checkEqual(ChampNode.improve(n.hash), path, "collision node under its path");
            java.util.Set<Object> elements = new java.util.HashSet<>();
            for (Object element : n.content) {
                check(element != null, "an element of a collision node is not null");
                checkEqual(Objects.hashCode(element), n.hash, "hash of an element of a collision node");
                check(elements.add(element), "distinct elements in a collision node");
            }
            checkEqual(n.keyHashSum(), n.content.length * n.hash, "keyHashSum of a collision node");
            return n.content.length;
        }
    }

    /// Asserts that two set tries have the same shape and hold the same element objects in the same slots; the order
    /// inside a collision node is ignored unless `collisionOrder` is set.
    static void assertSameShape(SetNode<?> expected, SetNode<?> actual, boolean collisionOrder) {
        check(actual.getClass() == expected.getClass(), "same node class");
        if (expected instanceof BitmapIndexedSetNode<?> e) {
            BitmapIndexedSetNode<?> a = (BitmapIndexedSetNode<?>) actual;
            checkEqual(a.dataMap, e.dataMap, "dataMap");
            checkEqual(a.nodeMap, e.nodeMap, "nodeMap");
            checkEqual(a.size, e.size, "size field");
            checkEqual(a.keyHashSum, e.keyHashSum, "keyHashSum field");
            check(java.util.Arrays.equals(a.hashes, e.hashes), "hashes");
            checkEqual(a.content.length, e.content.length, "length of the content array");
            int payload = Integer.bitCount(e.dataMap);
            for (int i = 0; i < payload; i++) {
                checkSame(a.content[i], e.content[i], "element object");
            }
            for (int i = payload; i < e.content.length; i++) {
                assertSameShape((SetNode<?>) e.content[i], (SetNode<?>) a.content[i], collisionOrder);
            }
        } else {
            HashCollisionSetNode<?> e = (HashCollisionSetNode<?>) expected;
            HashCollisionSetNode<?> a = (HashCollisionSetNode<?>) actual;
            checkEqual(a.hash, e.hash, "hash of a collision node");
            checkEqual(a.content.length, e.content.length, "length of a collision node");
            for (int i = 0; i < e.content.length; i++) {
                if (collisionOrder) {
                    checkSame(a.content[i], e.content[i], "element object of a collision node");
                } else {
                    checkSame(a.content[a.indexOf(e.content[i])], e.content[i], "element object of a collision node");
                }
            }
        }
    }

    /// The bitmap nodes of a set trie, by identity.
    static Set<Object> internalNodes(SetNode<?> node) {
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        collectSetNodes(node, result);
        return result;
    }

    private static void collectSetNodes(SetNode<?> node, Set<Object> result) {
        if (node instanceof BitmapIndexedSetNode<?> n) {
            result.add(n);
            for (int i = Integer.bitCount(n.dataMap); i < n.content.length; i++) {
                collectSetNodes((SetNode<?>) n.content[i], result);
            }
        }
    }

    /// A description of every node of a set trie, as [#describe(MapNode)] gives for a map.
    static String describe(SetNode<?> node) {
        StringBuilder out = new StringBuilder();
        describeSet(node, out);
        return out.toString();
    }

    private static void describeSet(SetNode<?> node, StringBuilder out) {
        out.append(node.getClass().getSimpleName()).append('@').append(System.identityHashCode(node));
        if (node instanceof BitmapIndexedSetNode<?> n) {
            out.append("[d=")
                    .append(n.dataMap)
                    .append(",n=")
                    .append(n.nodeMap)
                    .append(",s=")
                    .append(n.size)
                    .append(",h=")
                    .append(n.keyHashSum)
                    .append(",c@")
                    .append(System.identityHashCode(n.content))
                    .append(",hs@")
                    .append(System.identityHashCode(n.hashes))
                    .append(java.util.Arrays.toString(n.hashes))
                    .append('(');
            int payload = Integer.bitCount(n.dataMap);
            for (int i = 0; i < n.content.length; i++) {
                if (i < payload) {
                    out.append(System.identityHashCode(n.content[i]));
                } else {
                    describeSet((SetNode<?>) n.content[i], out);
                }
                out.append(' ');
            }
            out.append(")]");
        } else {
            HashCollisionSetNode<?> n = (HashCollisionSetNode<?>) node;
            out.append("[c@").append(System.identityHashCode(n.content)).append('(');
            for (Object o : n.content) {
                out.append(System.identityHashCode(o)).append(' ');
            }
            out.append(")]");
        }
    }

    // -- plain checks: these run for every node and entry of every trie the tests build, so they build no AssertJ
    // assertion, and their message only on a failure

    private static void check(boolean holds, String invariant) {
        if (!holds) {
            throw new AssertionError(invariant);
        }
    }

    private static void checkEqual(int actual, int expected, String what) {
        if (actual != expected) {
            throw new AssertionError(what + ": " + actual + ", expected " + expected);
        }
    }

    private static void checkSame(Object actual, Object expected, String what) {
        if (actual != expected) {
            throw new AssertionError(what + ": " + actual + ", expected the instance " + expected);
        }
    }
}
