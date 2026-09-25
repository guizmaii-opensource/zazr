package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.internal.RedBlackTree.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RedBlackTreeIteratorTest {

    private static final int[] SIZES = { 0, 1, 2, 3, 7, 8, 31, 32, 33, 1023, 1024, 1025, 100_000 };

    private static final Comparator<Integer> NATURAL = Comparators.naturalComparator();

    private static java.util.List<Integer> iterated(RedBlackTree<Integer> tree) {
        final java.util.List<Integer> result = new ArrayList<>();
        final java.util.Iterator<Integer> iterator = tree.iterator();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    private static java.util.List<Integer> range(int size) {
        return IntStream.range(0, size).boxed().toList();
    }

    // the depth of the deepest node, the root being on level 1
    private static int height(RedBlackTree<?> tree) {
        return tree.isEmpty() ? 0 : 1 + Math.max(height(tree.left()), height(tree.right()));
    }

    @Test
    public void shouldIterateInOrderTreesBuiltByInsertion() {
        for (int size : SIZES) {
            final java.util.List<Integer> shuffled = new ArrayList<>(range(size));
            java.util.Collections.shuffle(shuffled, new Random(size));
            RedBlackTree<Integer> tree = RedBlackTree.empty(NATURAL);
            for (Integer value : shuffled) {
                tree = tree.insert(value);
            }
            assertThat(iterated(tree)).as("size %d", size).containsExactlyElementsOf(range(size));
        }
    }

    @Test
    public void shouldIterateInOrderTreesBuiltFromOrderedValues() {
        for (int size : SIZES) {
            final Object[] sorted = range(size).toArray();
            final RedBlackTree<Integer> tree = RedBlackTreeModule.Node.fromOrdered(new RedBlackTreeModule.Empty<>(NATURAL), sorted, size);
            assertThat(iterated(tree)).as("size %d", size).containsExactlyElementsOf(range(size));
        }
    }

    @Test
    public void shouldIterateInOrderAfterDeletions() {
        for (int size : SIZES) {
            RedBlackTree<Integer> tree = RedBlackTree.empty(NATURAL);
            for (int i = 0; i < size; i++) {
                tree = tree.insert(i);
            }
            final RedBlackTree<Integer> full = tree;
            for (int i = 0; i < size; i += 3) {
                tree = tree.delete(i);
            }
            final java.util.List<Integer> expected = range(size).stream().filter(i -> i % 3 != 0).toList();
            assertThat(iterated(tree)).as("size %d", size).containsExactlyElementsOf(expected);
            // the tree the deletions started from is unchanged
            assertThat(iterated(full)).as("original of size %d", size).containsExactlyElementsOf(range(size));
        }
    }

    @Test
    public void shouldNeverNeedMoreThanTheFirstStackForValidTrees() {
        // the iterator sizes its stack from the root's black height; a valid tree is never higher than that
        for (int size : SIZES) {
            RedBlackTree<Integer> tree = RedBlackTree.empty(NATURAL);
            for (int i = 0; i < size; i++) {
                tree = tree.insert(i);
            }
            if (!tree.isEmpty()) {
                final int blackHeight = ((RedBlackTreeModule.Node<Integer>) tree).blackHeight;
                assertThat(height(tree)).as("size %d", size).isLessThanOrEqualTo(Math.max(4, 2 * blackHeight + 2));
            }
        }
    }

    @Test
    public void shouldGrowTheStackWhenTheBlackHeightUnderstatesTheHeight() {
        // a left-leaning chain whose nodes all claim a black height of 1: higher than the first stack the iterator
        // allocates, so the stack has to grow while the leftmost path is pushed
        final RedBlackTreeModule.Empty<Integer> empty = new RedBlackTreeModule.Empty<>(NATURAL);
        final int length = 100;
        RedBlackTree<Integer> chain = empty;
        for (int i = length - 1; i >= 0; i--) {
            chain = new RedBlackTreeModule.Node<>(Color.BLACK, 1, empty, i, chain, empty);
        }
        RedBlackTree<Integer> leftChain = empty;
        for (int i = 0; i < length; i++) {
            leftChain = new RedBlackTreeModule.Node<>(Color.BLACK, 1, leftChain, i, empty, empty);
        }
        assertThat(iterated(chain)).containsExactlyElementsOf(range(length));
        assertThat(iterated(leftChain)).containsExactlyElementsOf(range(length));
    }

    @Test
    public void shouldThrowWhenIteratedPastTheEnd() {
        final java.util.Iterator<Integer> iterator = RedBlackTree.of(NATURAL, 1).iterator();
        assertThat(iterator.next()).isEqualTo(1);
        assertThat(iterator.hasNext()).isFalse();
        assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(RedBlackTree.empty(NATURAL).iterator()::next).isInstanceOf(NoSuchElementException.class);
    }
}
