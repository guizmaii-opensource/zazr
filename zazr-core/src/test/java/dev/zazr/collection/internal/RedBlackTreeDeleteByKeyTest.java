package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.RedBlackTreeValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

/// `Node.deleteByKey` on the elements of a set (`entries` false) and on the entries of a map (`entries` true): the
/// tree it gives is the very shape, colours included, that the reference element deletion
/// (`RedBlackTreeDeletionReference`) gives for the same element, and the tree itself when no key is equal.
public class RedBlackTreeDeleteByKeyTest {

    private static final long SEED = 20261005L;
    private static final int[] SIZES = {0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();
    private static final Comparator<Integer> REVERSED = NATURAL.reversed();
    private static final Comparator<Integer> MODULO_7 = Comparator.comparingInt(i -> Math.floorMod(i, 7));

    private static RedBlackTree<Integer> randomTree(Comparator<Integer> order, int size, Random random) {
        @SuppressWarnings("Var")
        RedBlackTree<Integer> tree = RedBlackTree.empty(order);
        for (int i = 0; i < 2 * size && tree.size() < size; i++) {
            tree = tree.insert(random.nextInt(4 * size + 1) - 2 * size);
        }
        return tree;
    }

    @Test
    void onTheElementsOfASetAsTheElementDelete() {
        Random random = new Random(SEED);
        for (Comparator<Integer> order : List.of(NATURAL, REVERSED, MODULO_7)) {
            for (int size : SIZES) {
                RedBlackTree<Integer> tree = randomTree(order, size, random);
                String before = tree.toString();
                for (int key = -2 * size - 2; key <= 2 * size + 2; key++) {
                    RedBlackTree<Integer> deleted = RedBlackTreeModule.Node.deleteByKey(tree, key, order, false);
                    String context = size + " " + key;
                    if (tree.contains(key)) {
                        assertThat(deleted.toString())
                                .as(context)
                                .isEqualTo(RedBlackTreeDeletionReference.delete(tree, key)
                                        .toString());
                        assertThat(deleted.size()).as(context).isEqualTo(tree.size() - 1);
                        assertThat(deleted.contains(key)).as(context).isFalse();
                    } else {
                        assertThat(deleted).as(context).isSameAs(tree);
                    }
                    assertValid(deleted);
                }
                assertThat(tree.toString()).isEqualTo(before);
            }
        }
    }

    @Test
    void onTheEntriesOfAMapAsTheElementDeleteOfAProbe() {
        Random random = new Random(SEED + 1);
        for (Comparator<Integer> order : List.of(NATURAL, REVERSED, MODULO_7)) {
            Comparator<Tuple2<Integer, String>> byKey = (a, b) -> order.compare(a._1(), b._1());
            for (int size : SIZES) {
                @SuppressWarnings("Var")
                RedBlackTree<Tuple2<Integer, String>> tree = RedBlackTree.empty(byKey);
                for (int i = 0; i < 2 * size && tree.size() < size; i++) {
                    int key = random.nextInt(4 * size + 1) - 2 * size;
                    tree = tree.insert(Tuple.of(key, "v" + key));
                }
                // removing the keys one at a time, in a random order, down to the empty tree
                List<Integer> keys = new ArrayList<>();
                tree.forEach(entry -> keys.add(entry._1()));
                java.util.Collections.shuffle(keys, random);
                for (int key : keys) {
                    Tuple2<Integer, String> probe = Tuple.of(key, "probe");
                    RedBlackTree<Tuple2<Integer, String>> deleted =
                            RedBlackTreeModule.Node.deleteByKey(tree, key, order, true);
                    assertThat(deleted.toString())
                            .isEqualTo(RedBlackTreeDeletionReference.delete(tree, probe)
                                    .toString());
                    assertValid(deleted);
                    assertThat(RedBlackTreeModule.Node.deleteByKey(deleted, key, order, true))
                            .isSameAs(deleted);
                    tree = deleted;
                }
                assertThat(tree.isEmpty()).isTrue();
                assertThat(tree.comparator()).isSameAs(byKey);
            }
        }
    }
}
