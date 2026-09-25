package com.guizmaii.zazr.collection.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/// The invariants of the CHAMP tries of `HashMap` and `HashSet`, and the comparisons of two tries, for the tests.
final class ChampValidity {

    private ChampValidity() {
    }

    // -- maps

    /// Asserts every invariant of a map trie, the canonical form included, and returns its size:
    /// - the bitmaps are disjoint, and the arrays are as long as they say (two slots per entry, one per child);
    /// - every entry sits in the slot of its hash fragment, under the fragments of its path, with its own hash stored;
    /// - every child holds at least two entries (a single one would be inline), is a bitmap node above the last level
    ///   of hash bits and a collision node below it;
    /// - a collision node holds at least two distinct keys, all of its hash;
    /// - the cached sizes and hash sums are the recomputed ones.
    static int assertValid(BitmapIndexedMapNode<?, ?> root) {
        final int size = assertValidMap(root, 0, 0, 0);
        assertThat(root.size()).isEqualTo(size);
        return size;
    }

    private static int assertValidMap(MapNode<?, ?> node, int shift, int path, int pathMask) {
        if (node instanceof BitmapIndexedMapNode<?, ?> n) {
            assertThat(shift).as("bitmap node above the last level").isLessThan(ChampNode.HASH_CODE_LENGTH);
            assertThat(n.dataMap & n.nodeMap).as("disjoint bitmaps").isZero();
            final int payload = Integer.bitCount(n.dataMap);
            final int children = Integer.bitCount(n.nodeMap);
            assertThat(n.content.length).isEqualTo(2 * payload + children);
            assertThat(n.hashes.length).isEqualTo(payload);
            int size = 0;
            int hashSum = 0;
            int bits = n.dataMap;
            for (int i = 0; i < payload; i++) {
                final int fragment = Integer.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                final Object key = n.content[2 * i];
                final Object value = n.content[2 * i + 1];
                assertThat(key).isNotNull().isNotInstanceOf(MapNode.class);
                assertThat(value).isNotNull().isNotInstanceOf(MapNode.class);
                assertThat(n.hashes[i]).isEqualTo(Objects.hashCode(key));
                assertThat(ChampNode.maskFrom(n.hashes[i], shift)).as("entry in the slot of its fragment").isEqualTo(fragment);
                assertThat(n.hashes[i] & pathMask).as("entry under its path").isEqualTo(path);
                size++;
                hashSum += n.hashes[i];
            }
            bits = n.nodeMap;
            for (int i = 0; i < children; i++) {
                final int fragment = Integer.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                final Object child = n.content[n.content.length - 1 - i];
                assertThat(child).isInstanceOf(MapNode.class);
                final MapNode<?, ?> c = (MapNode<?, ?>) child;
                final int childShift = shift + ChampNode.BIT_PARTITION_SIZE;
                if (childShift < ChampNode.HASH_CODE_LENGTH) {
                    assertThat(c).isInstanceOf(BitmapIndexedMapNode.class);
                } else {
                    assertThat(c).isInstanceOf(HashCollisionMapNode.class);
                }
                final int childSize = assertValidMap(c, childShift, path | (fragment << shift), pathMask | (ChampNode.BIT_PARTITION_MASK << shift));
                assertThat(childSize).as("a child holds at least two entries").isGreaterThanOrEqualTo(2);
                size += childSize;
                hashSum += c.keyHashSum();
            }
            assertThat(n.size).isEqualTo(size);
            assertThat(n.keyHashSum).isEqualTo(hashSum);
            return size;
        } else {
            final HashCollisionMapNode<?, ?> n = (HashCollisionMapNode<?, ?>) node;
            assertThat(n.content.length % 2).isZero();
            final int size = n.content.length / 2;
            assertThat(size).isGreaterThanOrEqualTo(2);
            assertThat(n.hash).as("collision node under its path").isEqualTo(path);
            final java.util.Set<Object> keys = new java.util.HashSet<>();
            for (int i = 0; i < size; i++) {
                assertThat(n.content[2 * i]).isNotNull();
                assertThat(n.content[2 * i + 1]).isNotNull();
                assertThat(Objects.hashCode(n.content[2 * i])).isEqualTo(n.hash);
                assertThat(keys.add(n.content[2 * i])).as("distinct keys in a collision node").isTrue();
            }
            assertThat(n.keyHashSum()).isEqualTo(size * n.hash);
            return size;
        }
    }

    /// Asserts that two map tries have the same shape and hold the same key and value objects in the same slots; the
    /// order inside a collision node is ignored unless `collisionOrder` is set.
    static void assertSameShape(MapNode<?, ?> expected, MapNode<?, ?> actual, boolean collisionOrder) {
        assertThat(actual.getClass()).isEqualTo(expected.getClass());
        if (expected instanceof BitmapIndexedMapNode<?, ?> e) {
            final BitmapIndexedMapNode<?, ?> a = (BitmapIndexedMapNode<?, ?>) actual;
            assertThat(a.dataMap).isEqualTo(e.dataMap);
            assertThat(a.nodeMap).isEqualTo(e.nodeMap);
            assertThat(a.size).isEqualTo(e.size);
            assertThat(a.keyHashSum).isEqualTo(e.keyHashSum);
            assertThat(a.hashes).isEqualTo(e.hashes);
            assertThat(a.content.length).isEqualTo(e.content.length);
            final int payload = 2 * Integer.bitCount(e.dataMap);
            for (int i = 0; i < payload; i++) {
                assertThat(a.content[i]).isSameAs(e.content[i]);
            }
            for (int i = payload; i < e.content.length; i++) {
                assertSameShape((MapNode<?, ?>) e.content[i], (MapNode<?, ?>) a.content[i], collisionOrder);
            }
        } else {
            final HashCollisionMapNode<?, ?> e = (HashCollisionMapNode<?, ?>) expected;
            final HashCollisionMapNode<?, ?> a = (HashCollisionMapNode<?, ?>) actual;
            assertThat(a.hash).isEqualTo(e.hash);
            assertThat(a.content.length).isEqualTo(e.content.length);
            if (collisionOrder) {
                for (int i = 0; i < e.content.length; i++) {
                    assertThat(a.content[i]).isSameAs(e.content[i]);
                }
            } else {
                for (int i = 0; i < e.content.length; i += 2) {
                    final int j = 2 * a.indexOf(e.content[i]);
                    assertThat(j).isNotNegative();
                    assertThat(a.content[j]).isSameAs(e.content[i]);
                    assertThat(a.content[j + 1]).isSameAs(e.content[i + 1]);
                }
            }
        }
    }

    /// The bitmap nodes of a map trie, by identity.
    static Set<Object> internalNodes(MapNode<?, ?> node) {
        final Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
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
        final StringBuilder out = new StringBuilder();
        describeMap(node, out);
        return out.toString();
    }

    private static void describeMap(MapNode<?, ?> node, StringBuilder out) {
        out.append(node.getClass().getSimpleName()).append('@').append(System.identityHashCode(node));
        if (node instanceof BitmapIndexedMapNode<?, ?> n) {
            out.append("[d=").append(n.dataMap).append(",n=").append(n.nodeMap).append(",s=").append(n.size)
               .append(",h=").append(n.keyHashSum).append(",c@").append(System.identityHashCode(n.content))
               .append(",hs@").append(System.identityHashCode(n.hashes)).append(java.util.Arrays.toString(n.hashes)).append('(');
            final int payload = 2 * Integer.bitCount(n.dataMap);
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
            final HashCollisionMapNode<?, ?> n = (HashCollisionMapNode<?, ?>) node;
            out.append("[c@").append(System.identityHashCode(n.content)).append('(');
            for (Object o : n.content) {
                out.append(System.identityHashCode(o)).append(' ');
            }
            out.append(")]");
        }
    }

    /// The entries of a map trie, key and value objects, in iteration order.
    static java.util.List<Object[]> entries(MapNode<?, ?> root) {
        final java.util.List<Object[]> result = new ArrayList<>();
        root.iterator((k, v) -> new Object[] { k, v }).forEachRemaining(result::add);
        return result;
    }

    // -- sets

    /// Asserts every invariant of a set trie, as [#assertValid(BitmapIndexedMapNode)] does for a map, and returns its
    /// size.
    static int assertValid(BitmapIndexedSetNode<?> root) {
        final int size = assertValidSet(root, 0, 0, 0);
        assertThat(root.size()).isEqualTo(size);
        return size;
    }

    private static int assertValidSet(SetNode<?> node, int shift, int path, int pathMask) {
        if (node instanceof BitmapIndexedSetNode<?> n) {
            assertThat(shift).as("bitmap node above the last level").isLessThan(ChampNode.HASH_CODE_LENGTH);
            assertThat(n.dataMap & n.nodeMap).as("disjoint bitmaps").isZero();
            final int payload = Integer.bitCount(n.dataMap);
            final int children = Integer.bitCount(n.nodeMap);
            assertThat(n.content.length).isEqualTo(payload + children);
            assertThat(n.hashes.length).isEqualTo(payload);
            int size = 0;
            int hashSum = 0;
            int bits = n.dataMap;
            for (int i = 0; i < payload; i++) {
                final int fragment = Integer.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                final Object element = n.content[i];
                assertThat(element).isNotNull().isNotInstanceOf(SetNode.class);
                assertThat(n.hashes[i]).isEqualTo(Objects.hashCode(element));
                assertThat(ChampNode.maskFrom(n.hashes[i], shift)).as("element in the slot of its fragment").isEqualTo(fragment);
                assertThat(n.hashes[i] & pathMask).as("element under its path").isEqualTo(path);
                size++;
                hashSum += n.hashes[i];
            }
            bits = n.nodeMap;
            for (int i = 0; i < children; i++) {
                final int fragment = Integer.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                final Object child = n.content[n.content.length - 1 - i];
                assertThat(child).isInstanceOf(SetNode.class);
                final SetNode<?> c = (SetNode<?>) child;
                final int childShift = shift + ChampNode.BIT_PARTITION_SIZE;
                if (childShift < ChampNode.HASH_CODE_LENGTH) {
                    assertThat(c).isInstanceOf(BitmapIndexedSetNode.class);
                } else {
                    assertThat(c).isInstanceOf(HashCollisionSetNode.class);
                }
                final int childSize = assertValidSet(c, childShift, path | (fragment << shift), pathMask | (ChampNode.BIT_PARTITION_MASK << shift));
                assertThat(childSize).as("a child holds at least two elements").isGreaterThanOrEqualTo(2);
                size += childSize;
                hashSum += c.keyHashSum();
            }
            assertThat(n.size).isEqualTo(size);
            assertThat(n.keyHashSum).isEqualTo(hashSum);
            return size;
        } else {
            final HashCollisionSetNode<?> n = (HashCollisionSetNode<?>) node;
            assertThat(n.content.length).isGreaterThanOrEqualTo(2);
            assertThat(n.hash).as("collision node under its path").isEqualTo(path);
            final java.util.Set<Object> elements = new java.util.HashSet<>();
            for (Object element : n.content) {
                assertThat(element).isNotNull();
                assertThat(Objects.hashCode(element)).isEqualTo(n.hash);
                assertThat(elements.add(element)).as("distinct elements in a collision node").isTrue();
            }
            assertThat(n.keyHashSum()).isEqualTo(n.content.length * n.hash);
            return n.content.length;
        }
    }

    /// Asserts that two set tries have the same shape and hold the same element objects in the same slots; the order
    /// inside a collision node is ignored unless `collisionOrder` is set.
    static void assertSameShape(SetNode<?> expected, SetNode<?> actual, boolean collisionOrder) {
        assertThat(actual.getClass()).isEqualTo(expected.getClass());
        if (expected instanceof BitmapIndexedSetNode<?> e) {
            final BitmapIndexedSetNode<?> a = (BitmapIndexedSetNode<?>) actual;
            assertThat(a.dataMap).isEqualTo(e.dataMap);
            assertThat(a.nodeMap).isEqualTo(e.nodeMap);
            assertThat(a.size).isEqualTo(e.size);
            assertThat(a.keyHashSum).isEqualTo(e.keyHashSum);
            assertThat(a.hashes).isEqualTo(e.hashes);
            assertThat(a.content.length).isEqualTo(e.content.length);
            final int payload = Integer.bitCount(e.dataMap);
            for (int i = 0; i < payload; i++) {
                assertThat(a.content[i]).isSameAs(e.content[i]);
            }
            for (int i = payload; i < e.content.length; i++) {
                assertSameShape((SetNode<?>) e.content[i], (SetNode<?>) a.content[i], collisionOrder);
            }
        } else {
            final HashCollisionSetNode<?> e = (HashCollisionSetNode<?>) expected;
            final HashCollisionSetNode<?> a = (HashCollisionSetNode<?>) actual;
            assertThat(a.hash).isEqualTo(e.hash);
            assertThat(a.content.length).isEqualTo(e.content.length);
            for (int i = 0; i < e.content.length; i++) {
                if (collisionOrder) {
                    assertThat(a.content[i]).isSameAs(e.content[i]);
                } else {
                    assertThat(a.content[a.indexOf(e.content[i])]).isSameAs(e.content[i]);
                }
            }
        }
    }

    /// The bitmap nodes of a set trie, by identity.
    static Set<Object> internalNodes(SetNode<?> node) {
        final Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
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
        final StringBuilder out = new StringBuilder();
        describeSet(node, out);
        return out.toString();
    }

    private static void describeSet(SetNode<?> node, StringBuilder out) {
        out.append(node.getClass().getSimpleName()).append('@').append(System.identityHashCode(node));
        if (node instanceof BitmapIndexedSetNode<?> n) {
            out.append("[d=").append(n.dataMap).append(",n=").append(n.nodeMap).append(",s=").append(n.size)
               .append(",h=").append(n.keyHashSum).append(",c@").append(System.identityHashCode(n.content))
               .append(",hs@").append(System.identityHashCode(n.hashes)).append(java.util.Arrays.toString(n.hashes)).append('(');
            final int payload = Integer.bitCount(n.dataMap);
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
            final HashCollisionSetNode<?> n = (HashCollisionSetNode<?>) node;
            out.append("[c@").append(System.identityHashCode(n.content)).append('(');
            for (Object o : n.content) {
                out.append(System.identityHashCode(o)).append(' ');
            }
            out.append(")]");
        }
    }
}
