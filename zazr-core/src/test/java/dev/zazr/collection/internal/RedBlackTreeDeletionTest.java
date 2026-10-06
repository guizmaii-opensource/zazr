package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

/// The deletion of the red-black tree (`delete`, `Node.deleteByKey` and the `deleteMin` that `Node.merge` uses)
/// against `RedBlackTreeDeletionReference`, which passes the "black height dropped" flag up in a tuple: on random
/// trees, every result has the very shape of the reference's, node by node (colour, `blackHeight`, `size`, and the
/// element object itself), and a deletion of an absent element returns the tree itself.
public class RedBlackTreeDeletionTest {

    private static final long SEED = 20261006L;
    private static final int[] SIZES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 15, 16, 17, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025
    };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();
    private static final Comparator<Integer> MODULO_7 = Comparator.comparingInt(i -> Math.floorMod(i, 7));
    private static final List<Comparator<Integer>> ORDERS = List.of(NATURAL, REVERSED, MODULO_7);

    // -- the trees

    // random insertions into a range twice the size, so that the keys around the stored ones are absent
    private static RedBlackTree<Integer> inserted(Comparator<Integer> order, int size, Random random) {
        @SuppressWarnings("Var")
        RedBlackTree<Integer> tree = RedBlackTree.empty(order);
        for (int i = 0; i < 4 * size && tree.size() < size; i++) {
            tree = tree.insert(random.nextInt(4 * size + 1) - 2 * size);
        }
        return tree;
    }

    // the builder's shape: black everywhere except a red bottom level
    private static RedBlackTree<Integer> built(Comparator<Integer> order, int size, Random random) {
        TreeSet<Integer> distinct = new TreeSet<>(order);
        for (int i = 0; i < 4 * size && distinct.size() < size; i++) {
            distinct.add(random.nextInt(4 * size + 1) - 2 * size);
        }
        Object[] sorted = distinct.toArray();
        return RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(order), sorted, sorted.length);
    }

    // a larger tree with about a third of its elements deleted, through the reference, so that the shapes left behind
    // by deletions are covered too
    private static RedBlackTree<Integer> thinned(Comparator<Integer> order, int size, Random random) {
        RedBlackTree<Integer> larger = inserted(order, size + size / 2, random);
        List<Integer> elements = elements(larger);
        Collections.shuffle(elements, random);
        @SuppressWarnings("Var")
        RedBlackTree<Integer> tree = larger;
        for (int i = 0; i < elements.size() - size; i++) {
            tree = RedBlackTreeDeletionReference.delete(tree, elements.get(i));
        }
        return tree;
    }

    private static List<RedBlackTree<Integer>> trees(Comparator<Integer> order, Random random) {
        List<RedBlackTree<Integer>> trees = new ArrayList<>();
        for (int size : SIZES) {
            trees.add(inserted(order, size, random));
            trees.add(built(order, size, random));
            trees.add(thinned(order, size, random));
        }
        for (int i = 0; i < 200; i++) {
            trees.add(inserted(order, random.nextInt(100), random));
        }
        return trees;
    }

    private static <T> List<T> elements(RedBlackTree<T> tree) {
        List<T> result = new ArrayList<>();
        tree.forEach(result::add);
        return result;
    }

    // every key of the range around a small tree, every fifth around a large one
    private static int stride(int size) {
        return size > 100 ? 5 : 1;
    }

    // -- the comparison

    // `actual` and `expected` have the same nodes, colours, `blackHeight` and `size` fields and element objects; a
    // subtree that both share is not walked
    private static <T> void assertSameShape(RedBlackTree<T> actual, RedBlackTree<T> expected, String context) {
        if (actual == expected) {
            return; // a subtree both deletions kept
        }
        if (actual.isEmpty() || expected.isEmpty()) {
            if (actual.isEmpty() != expected.isEmpty()) {
                throw new AssertionError(context + ": " + actual + " but expected " + expected);
            }
            assertThat(actual.comparator()).as(context).isSameAs(expected.comparator());
            return;
        }
        RedBlackTreeModule.Node<T> a = (RedBlackTreeModule.Node<T>) actual;
        RedBlackTreeModule.Node<T> e = (RedBlackTreeModule.Node<T>) expected;
        if (a.color != e.color || a.blackHeight != e.blackHeight || a.size != e.size || a.value != e.value) {
            throw new AssertionError(context + ": " + a.color + " " + a.blackHeight + " " + a.size + " " + a.value
                    + " but expected " + e.color + " " + e.blackHeight + " " + e.size + " " + e.value + " in " + actual
                    + " and " + expected);
        }
        assertSameShape(a.left, e.left, context);
        assertSameShape(a.right, e.right, context);
    }

    // -- delete and deleteByKey of an element

    @Test
    void deletingAnElementGivesTheShapeOfTheReference() {
        Random random = new Random(SEED);
        for (Comparator<Integer> order : ORDERS) {
            for (RedBlackTree<Integer> tree : trees(order, random)) {
                String before = tree.toString();
                int size = tree.size();
                for (int key = -2 * size - 2; key <= 2 * size + 2; key += stride(size)) {
                    String context = key + " from a tree of " + size;
                    RedBlackTree<Integer> deleted = tree.delete(key);
                    RedBlackTree<Integer> expected = RedBlackTreeDeletionReference.delete(tree, key);
                    assertSameShape(deleted, expected, context);
                    RedBlackTree<Integer> byKey = RedBlackTreeModule.Node.deleteByKey(tree, key, order, false);
                    assertSameShape(byKey, expected, context);
                    if (tree.contains(key)) {
                        assertThat(deleted.size()).as(context).isEqualTo(size - 1);
                        assertThat(deleted.contains(key)).as(context).isFalse();
                    } else {
                        assertThat(deleted).as(context).isSameAs(tree);
                        assertThat(byKey).as(context).isSameAs(tree);
                    }
                    assertValid(deleted);
                }
                assertThat(tree.toString()).isEqualTo(before);
            }
        }
    }

    @Test
    void deletingEveryElementInARandomOrderGivesTheShapesOfTheReference() {
        Random random = new Random(SEED + 1);
        for (Comparator<Integer> order : ORDERS) {
            for (RedBlackTree<Integer> start : trees(order, random)) {
                List<Integer> keys = elements(start);
                Collections.shuffle(keys, random);
                List<RedBlackTree<Integer>> versions = new ArrayList<>();
                List<String> printed = new ArrayList<>();
                @SuppressWarnings("Var")
                RedBlackTree<Integer> tree = start;
                for (int key : keys) {
                    versions.add(tree);
                    printed.add(tree.toString());
                    RedBlackTree<Integer> deleted = tree.delete(key);
                    assertSameShape(
                            deleted,
                            RedBlackTreeDeletionReference.delete(tree, key),
                            key + " from a tree of " + tree.size());
                    assertValid(deleted);
                    assertThat(deleted.delete(key)).isSameAs(deleted);
                    tree = deleted;
                }
                assertThat(tree.isEmpty()).isTrue();
                assertThat(tree.comparator()).isSameAs(order);
                for (int i = 0; i < versions.size(); i++) {
                    assertThat(versions.get(i).toString()).isEqualTo(printed.get(i));
                    assertValid(versions.get(i));
                }
            }
        }
    }

    // -- deleteByKey of a map entry

    @Test
    void deletingAnEntryByKeyGivesTheShapeOfTheReferenceDeletingAProbe() {
        Random random = new Random(SEED + 2);
        for (Comparator<Integer> order : ORDERS) {
            Comparator<Tuple2<Integer, String>> byKey = (a, b) -> order.compare(a._1(), b._1());
            for (RedBlackTree<Integer> keys : trees(order, random)) {
                @SuppressWarnings("Var")
                RedBlackTree<Tuple2<Integer, String>> tree = RedBlackTree.empty(byKey);
                for (int key : elements(keys)) {
                    tree = tree.insert(Tuple.of(key, "v" + key));
                }
                int size = tree.size();
                for (int key = -2 * size - 2; key <= 2 * size + 2; key += stride(size)) {
                    RedBlackTree<Tuple2<Integer, String>> deleted =
                            RedBlackTreeModule.Node.deleteByKey(tree, key, order, true);
                    RedBlackTree<Tuple2<Integer, String>> expected =
                            RedBlackTreeDeletionReference.delete(tree, Tuple.of(key, "probe"));
                    assertSameShape(deleted, expected, key + " from a tree of " + tree.size());
                    if (deleted.size() == size) {
                        assertThat(deleted).isSameAs(tree);
                    }
                    assertValid(deleted);
                }
            }
        }
    }

    // -- merge, through deleteMin

    @Test
    void mergingTheTwoSubtreesOfEachNodeGivesTheShapeOfTheReference() {
        Random random = new Random(SEED + 3);
        for (Comparator<Integer> order : ORDERS) {
            for (RedBlackTree<Integer> tree : trees(order, random)) {
                mergeChildren(tree);
            }
        }
    }

    // the children of a node can have red roots, as the trees `merge` is given by `difference` and `intersection`
    private static void mergeChildren(RedBlackTree<Integer> tree) {
        if (tree.isEmpty()) {
            return;
        }
        RedBlackTree<Integer> merged = RedBlackTreeModule.Node.merge(tree.left(), tree.right());
        RedBlackTree<Integer> expected = RedBlackTreeDeletionReference.merge(tree.left(), tree.right());
        assertSameShape(merged, expected, "merge of the children of " + tree.value());
        assertValid(merged);
        assertThat(merged.size()).isEqualTo(tree.size() - 1);
        mergeChildren(tree.left());
        mergeChildren(tree.right());
    }

    // -- the black height of a subtree, as the deletion reads it

    @Test
    void theBlackHeightOfASubtreeIsItsFieldPlusOneWhenItsRootIsBlack() {
        Random random = new Random(SEED + 4);
        for (RedBlackTree<Integer> tree : trees(NATURAL, random)) {
            assertBlackNodes(tree);
        }
    }

    // `RedBlackTreeDeletionReference.blackNodes` of every subtree is the black nodes on each of its paths, counted
    private static int assertBlackNodes(RedBlackTree<Integer> tree) {
        if (tree.isEmpty()) {
            assertThat(RedBlackTreeDeletionReference.blackNodes(tree)).isEqualTo(1);
            return 1;
        }
        int below = assertBlackNodes(tree.left());
        assertThat(assertBlackNodes(tree.right())).isEqualTo(below);
        int counted = below + (tree.color() == RedBlackTree.Color.BLACK ? 1 : 0);
        assertThat(RedBlackTreeDeletionReference.blackNodes(tree)).isEqualTo(counted);
        return counted;
    }
}
