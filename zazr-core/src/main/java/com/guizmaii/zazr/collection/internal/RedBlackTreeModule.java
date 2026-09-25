package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Empty;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Node;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.RedBlackTree.Color.BLACK;
import static com.guizmaii.zazr.collection.internal.RedBlackTree.Color.RED;

public interface RedBlackTreeModule {

    /**
     * A non-empty tree node.
     *
     * @param <T> Component type
     */
    final class Node<T extends @Nullable Object> implements RedBlackTree<T> {

        final Color color;
        final int blackHeight;
        final RedBlackTree<T> left;
        final T value;
        final RedBlackTree<T> right;
        final Empty<T> empty;
        final int size;

        // This is no public API! The RedBlackTree takes care of passing the correct Comparator.
        Node(Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
            this.color = color;
            this.blackHeight = blackHeight;
            this.left = left;
            this.value = value;
            this.right = right;
            this.empty = empty;
            this.size = left.size() + right.size() + 1;
        }

        @Override
        public Color color() {
            return color;
        }

        @Override
        public Comparator<T> comparator() {
            return empty.comparator;
        }

        @Override
        public boolean contains(T value) {
            final int result = empty.comparator.compare(value, this.value);
            if (result < 0) {
                return left.contains(value);
            } else if (result > 0) {
                return right.contains(value);
            } else {
                return true;
            }
        }

        @Override
        public Empty<T> emptyInstance() {
            return empty;
        }

        @Override
        public Option<T> find(T value) {
            final int result = empty.comparator.compare(value, this.value);
            if (result < 0) {
                return left.find(value);
            } else if (result > 0) {
                return right.find(value);
            } else {
                return Option.some(this.value);
            }
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public RedBlackTree<T> left() {
            return left;
        }

        @Override
        public RedBlackTree<T> right() {
            return right;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public String toString() {
            return isLeaf() ? "(" + color + ":" + value + ")" : toLispString(this);
        }

        private static String toLispString(RedBlackTree<?> tree) {
            if (tree.isEmpty()) {
                return "";
            } else {
                final Node<?> node = (Node<?>) tree;
                final String value = node.color + ":" + node.value;
                if (node.isLeaf()) {
                    return value;
                } else {
                    final String left = node.left.isEmpty() ? "" : " " + toLispString(node.left);
                    final String right = node.right.isEmpty() ? "" : " " + toLispString(node.right);
                    return "(" + value + left + right + ")";
                }
            }
        }

        private boolean isLeaf() {
            return left.isEmpty() && right.isEmpty();
        }

        Node<T> color(Color color) {
            return (this.color == color) ? this : new Node<>(color, blackHeight, left, value, right, empty);
        }

        static <T extends @Nullable Object> RedBlackTree<T> color(RedBlackTree<T> tree, Color color) {
            return tree.isEmpty() ? tree : ((Node<T>) tree).color(color);
        }

        private static <T extends @Nullable Object> Node<T> balanceLeft(Color color, int blackHeight, RedBlackTree<T> left, T value,
                RedBlackTree<T> right, Empty<T> empty) {
            if (color == BLACK) {
                if (!left.isEmpty()) {
                    final Node<T> ln = (Node<T>) left;
                    if (ln.color == RED) {
                        if (!ln.left.isEmpty()) {
                            final Node<T> lln = (Node<T>) ln.left;
                            if (lln.color == RED) {
                                final Node<T> newLeft = new Node<>(BLACK, blackHeight, lln.left, lln.value, lln.right,
                                        empty);
                                final Node<T> newRight = new Node<>(BLACK, blackHeight, ln.right, value, right, empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, ln.value, newRight, empty);
                            }
                        }
                        if (!ln.right.isEmpty()) {
                            final Node<T> lrn = (Node<T>) ln.right;
                            if (lrn.color == RED) {
                                final Node<T> newLeft = new Node<>(BLACK, blackHeight, ln.left, ln.value, lrn.left,
                                        empty);
                                final Node<T> newRight = new Node<>(BLACK, blackHeight, lrn.right, value, right, empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, lrn.value, newRight, empty);
                            }
                        }
                    }
                }
            }
            return new Node<>(color, blackHeight, left, value, right, empty);
        }

        private static <T extends @Nullable Object> Node<T> balanceRight(Color color, int blackHeight, RedBlackTree<T> left, T value,
                RedBlackTree<T> right, Empty<T> empty) {
            if (color == BLACK) {
                if (!right.isEmpty()) {
                    final Node<T> rn = (Node<T>) right;
                    if (rn.color == RED) {
                        if (!rn.right.isEmpty()) {
                            final Node<T> rrn = (Node<T>) rn.right;
                            if (rrn.color == RED) {
                                final Node<T> newLeft = new Node<>(BLACK, blackHeight, left, value, rn.left, empty);
                                final Node<T> newRight = new Node<>(BLACK, blackHeight, rrn.left, rrn.value, rrn.right,
                                        empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, rn.value, newRight, empty);
                            }
                        }
                        if (!rn.left.isEmpty()) {
                            final Node<T> rln = (Node<T>) rn.left;
                            if (rln.color == RED) {
                                final Node<T> newLeft = new Node<>(BLACK, blackHeight, left, value, rln.left, empty);
                                final Node<T> newRight = new Node<>(BLACK, blackHeight, rln.right, rn.value, rn.right,
                                        empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, rln.value, newRight, empty);
                            }
                        }
                    }
                }
            }
            return new Node<>(color, blackHeight, left, value, right, empty);
        }

        private static <T extends @Nullable Object> Tuple2<? extends RedBlackTree<T>, Boolean> blackify(RedBlackTree<T> tree) {
            if (tree instanceof Node) {
                final Node<T> node = (Node<T>) tree;
                if (node.color == RED) {
                    return Tuple.of(node.color(BLACK), false);
                }
            }
            return Tuple.of(tree, true);
        }

        static <T extends @Nullable Object> Tuple2<? extends RedBlackTree<T>, Boolean> delete(RedBlackTree<T> tree, T value) {
            if (tree.isEmpty()) {
                return Tuple.of(tree, false);
            } else {
                final Node<T> node = (Node<T>) tree;
                final int comparison = node.comparator().compare(value, node.value);
                if (comparison < 0) {
                    final Tuple2<? extends RedBlackTree<T>, Boolean> deleted = delete(node.left, value);
                    final RedBlackTree<T> l = deleted._1();
                    final boolean d = deleted._2();
                    if (d) {
                        return Node.unbalancedRight(node.color, node.blackHeight - 1, l, node.value, node.right,
                                node.empty);
                    } else {
                        final Node<T> newNode = new Node<>(node.color, node.blackHeight, l, node.value, node.right,
                                node.empty);
                        return Tuple.of(newNode, false);
                    }
                } else if (comparison > 0) {
                    final Tuple2<? extends RedBlackTree<T>, Boolean> deleted = delete(node.right, value);
                    final RedBlackTree<T> r = deleted._1();
                    final boolean d = deleted._2();
                    if (d) {
                        return Node.unbalancedLeft(node.color, node.blackHeight - 1, node.left, node.value, r,
                                node.empty);
                    } else {
                        final Node<T> newNode = new Node<>(node.color, node.blackHeight, node.left, node.value, r,
                                node.empty);
                        return Tuple.of(newNode, false);
                    }
                } else {
                    if (node.right.isEmpty()) {
                        if (node.color == BLACK) {
                            return blackify(node.left);
                        } else {
                            return Tuple.of(node.left, false);
                        }
                    } else {
                        final Node<T> nodeRight = (Node<T>) node.right;
                        final Tuple3<? extends RedBlackTree<T>, Boolean, T> newRight = deleteMin(nodeRight);
                        final RedBlackTree<T> r = newRight._1();
                        final boolean d = newRight._2();
                        final T m = newRight._3();
                        if (d) {
                            return Node.unbalancedLeft(node.color, node.blackHeight - 1, node.left, m, r, node.empty);
                        } else {
                            final RedBlackTree<T> newNode = new Node<>(node.color, node.blackHeight, node.left, m, r,
                                    node.empty);
                            return Tuple.of(newNode, false);
                        }
                    }
                }
            }
        }

        private static <T extends @Nullable Object> Tuple3<? extends RedBlackTree<T>, Boolean, T> deleteMin(Node<T> node) {
            if (node.color() == BLACK && node.left().isEmpty() && node.right.isEmpty()){
                return Tuple.of(node.empty, true, node.value());
            } else if (node.color() == BLACK && node.left().isEmpty() && node.right().color() == RED){
                return Tuple.of(((Node<T>)node.right()).color(BLACK), false, node.value());
            } else if (node.color() == RED && node.left().isEmpty()){
                return Tuple.of(node.right(), false, node.value());
            } else{
                final Node<T> nodeLeft = (Node<T>) node.left;
                final Tuple3<? extends RedBlackTree<T>, Boolean, T> newNode = deleteMin(nodeLeft);
                final RedBlackTree<T> l = newNode._1();
                final boolean deleted = newNode._2();
                final T m = newNode._3();
                if (deleted) {
                    final Tuple2<Node<T>, Boolean> tD = Node.unbalancedRight(node.color, node.blackHeight - 1, l,
                            node.value, node.right, node.empty);
                    return Tuple.of(tD._1(), tD._2(), m);
                } else {
                    final Node<T> tD = new Node<>(node.color, node.blackHeight, l, node.value, node.right, node.empty);
                    return Tuple.of(tD, false, m);
                }
            }
        }

        static <T extends @Nullable Object> Node<T> insert(RedBlackTree<T> tree, T value) {
            if (tree.isEmpty()) {
                final Empty<T> empty = (Empty<T>) tree;
                return new Node<>(RED, 1, empty, value, empty, empty);
            } else {
                final Node<T> node = (Node<T>) tree;
                final int comparison = node.comparator().compare(value, node.value);
                if (comparison < 0) {
                    final Node<T> newLeft = insert(node.left, value);
                    return (newLeft == node.left)
                           ? node
                           : Node.balanceLeft(node.color, node.blackHeight, newLeft, node.value, node.right,
                            node.empty);
                } else if (comparison > 0) {
                    final Node<T> newRight = insert(node.right, value);
                    return (newRight == node.right)
                           ? node
                           : Node.balanceRight(node.color, node.blackHeight, node.left, node.value, newRight,
                            node.empty);
                } else {
                    // DEV-NOTE: Even if there is no _comparison_ difference, the object may not be _equal_.
                    //           To save an equals() call, which may be expensive, we return a new instance.
                    return new Node<>(node.color, node.blackHeight, node.left, value, node.right, node.empty);
                }
            }
        }

        /// Returns a valid tree holding the elements of `t1`, then `value`, then the elements of `t2`. Every element
        /// of `t1` is less than `value` and every element of `t2` greater. The roots of `t1` and `t2` can be red.
        static <T extends @Nullable Object> RedBlackTree<T> join(RedBlackTree<T> t1, T value, RedBlackTree<T> t2) {
            if (t1.isEmpty()) {
                return t2.insert(value);
            } else if (t2.isEmpty()) {
                return t1.insert(value);
            } else {
                // The stored blackHeight of a node counts the node as black whatever its colour, so a red root has one
                // black node fewer on its paths than a black root with the same blackHeight. Colouring both roots black
                // makes the stored values comparable and keeps them right.
                final Node<T> n1 = ((Node<T>) t1).color(BLACK);
                final Node<T> n2 = ((Node<T>) t2).color(BLACK);
                final int comparison = n1.blackHeight - n2.blackHeight;
                if (comparison < 0) {
                    return Node.joinLT(n1, value, n2, n1.blackHeight).color(BLACK);
                } else if (comparison > 0) {
                    return Node.joinGT(n1, value, n2, n2.blackHeight).color(BLACK);
                } else {
                    return new Node<>(BLACK, n1.blackHeight + 1, n1, value, n2, n1.empty);
                }
            }
        }

        private static <T extends @Nullable Object> Node<T> joinGT(Node<T> n1, T value, Node<T> n2, int h2) {
            if (n1.blackHeight == h2) {
                return new Node<>(RED, h2 + 1, n1, value, n2, n1.empty);
            } else {
                final Node<T> node = joinGT((Node<T>) n1.right, value, n2, h2);
                return Node.balanceRight(n1.color, n1.blackHeight, n1.left, n1.value, node, n2.empty);
            }
        }

        private static <T extends @Nullable Object> Node<T> joinLT(Node<T> n1, T value, Node<T> n2, int h1) {
            if (n2.blackHeight == h1) {
                return new Node<>(RED, h1 + 1, n1, value, n2, n1.empty);
            } else {
                final Node<T> node = joinLT(n1, value, (Node<T>) n2.left, h1);
                return Node.balanceLeft(n2.color, n2.blackHeight, node, n2.value, n2.right, n2.empty);
            }
        }

        /// Returns a valid tree holding the elements of `t1`, then the elements of `t2`. Every element of `t1` is less
        /// than every element of `t2`. The roots of `t1` and `t2` can be red. The minimum of `t2` is taken out and
        /// used as the middle value of [#join].
        static <T extends @Nullable Object> RedBlackTree<T> merge(RedBlackTree<T> t1, RedBlackTree<T> t2) {
            if (t1.isEmpty()) {
                return Node.color(t2, BLACK);
            } else if (t2.isEmpty()) {
                return Node.color(t1, BLACK);
            } else {
                final Tuple3<? extends RedBlackTree<T>, Boolean, T> withoutMinimum = Node.deleteMin((Node<T>) t2);
                return Node.join(t1, withoutMinimum._3(), withoutMinimum._1());
            }
        }

        /// Returns a balanced tree of the first `size` elements of `sorted`, which are strictly increasing under the
        /// comparator of `empty`, in O(size) and with exactly `size` nodes. Port of `fromOrderedKeys` in the Scala 3
        /// standard library (`scala.collection.immutable.RedBlackTree`, the Scala 2.13 collection library that Scala 3
        /// ships unchanged): the range is split around its middle element (the left part is never larger than the
        /// right), every node is black except the one-element subtrees on the deepest level, which are red; so every
        /// path has the same number of black nodes and no red node has a red child.
        static <T extends @Nullable Object> RedBlackTree<T> fromOrdered(Empty<T> empty, @Nullable Object[] sorted, int size) {
            // the deepest level holding a node, the root being on level 1
            final int maxUsedDepth = Integer.SIZE - Integer.numberOfLeadingZeros(size);
            return fromOrdered(empty, sorted, 0, size, 1, maxUsedDepth);
        }

        // the slots read are within the first `size` of `sorted`, which hold elements, never null
        @SuppressWarnings({"unchecked", "NullAway"})
        private static <T extends @Nullable Object> RedBlackTree<T> fromOrdered(Empty<T> empty, @Nullable Object[] sorted, int from, int size,
                int level, int maxUsedDepth) {
            if (size == 0) {
                return empty;
            } else if (size == 1) {
                final Color color = (level != maxUsedDepth || level == 1) ? BLACK : RED;
                return new Node<>(color, 1, empty, (T) sorted[from], empty, empty);
            } else {
                final int leftSize = (size - 1) / 2;
                final RedBlackTree<T> left = fromOrdered(empty, sorted, from, leftSize, level + 1, maxUsedDepth);
                final RedBlackTree<T> right = fromOrdered(empty, sorted, from + leftSize + 1, size - 1 - leftSize, level + 1, maxUsedDepth);
                // the stored blackHeight counts the black nodes below this one, the empty tree counting as one
                final int blackHeight = left.isEmpty()
                                        ? 1
                                        : ((Node<T>) left).blackHeight + (left.color() == BLACK ? 1 : 0);
                return new Node<>(BLACK, blackHeight, left, (T) sorted[from + leftSize], right, empty);
            }
        }

        /// Returns a tree of the same shape and colours as `tree`, holding `mapper.apply(e)` in place of each element
        /// `e` and ordered by `comparator`. `mapper` must be strictly increasing from the order of `tree` to
        /// `comparator` (not checked): the result is then a valid red-black tree, built in O(n) with no comparison
        /// and exactly one node per element. `mapper` is called once per element, in ascending order.
        public static <T extends @Nullable Object, R extends @Nullable Object> RedBlackTree<R> mapOrdered(RedBlackTree<T> tree,
                Comparator<? super R> comparator, java.util.function.Function<? super T, ? extends R> mapper) {
            return mapOrdered(tree, new Empty<>(comparator), mapper);
        }

        // the depth of the recursion is the height of the tree, at most 2 log2(n + 1)
        private static <T extends @Nullable Object, R extends @Nullable Object> RedBlackTree<R> mapOrdered(RedBlackTree<T> tree,
                Empty<R> empty, java.util.function.Function<? super T, ? extends R> mapper) {
            if (tree.isEmpty()) {
                return empty;
            }
            final Node<T> node = (Node<T>) tree;
            final RedBlackTree<R> left = mapOrdered(node.left, empty, mapper);
            final R value = mapper.apply(node.value);
            final RedBlackTree<R> right = mapOrdered(node.right, empty, mapper);
            return new Node<>(node.color, node.blackHeight, left, value, right, empty);
        }

        public static <T extends @Nullable Object> T maximum(Node<T> node) {
            Node<T> curr = node;
            while (!curr.right.isEmpty()) {
                curr = (Node<T>) curr.right;
            }
            return curr.value;
        }

        public static <T extends @Nullable Object> T minimum(Node<T> node) {
            Node<T> curr = node;
            while (!curr.left.isEmpty()) {
                curr = (Node<T>) curr.left;
            }
            return curr.value;
        }

        static <T extends @Nullable Object> Tuple2<RedBlackTree<T>, RedBlackTree<T>> split(RedBlackTree<T> tree, T value) {
            if (tree.isEmpty()) {
                return Tuple.of(tree, tree);
            } else {
                final Node<T> node = (Node<T>) tree;
                final int comparison = node.comparator().compare(value, node.value);
                if (comparison < 0) {
                    final Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(node.left, value);
                    return Tuple.of(split._1(), Node.join(split._2(), node.value, Node.color(node.right, BLACK)));
                } else if (comparison > 0) {
                    final Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(node.right, value);
                    return Tuple.of(Node.join(Node.color(node.left, BLACK), node.value, split._1()), split._2());
                } else {
                    return Tuple.of(Node.color(node.left, BLACK), Node.color(node.right, BLACK));
                }
            }
        }

        /**
         * Splits {@code tree} by rank: the first {@code n} elements in order and the others, both valid red-black
         * trees with a black root. {@link #split(RedBlackTree, Object)} with the left subtree's size in place of the
         * comparison; each half is built by one descent, a {@link #join(RedBlackTree, Object, RedBlackTree)} per
         * level, so O(log n).
         *
         * @param tree a tree
         * @param n    the rank of the split, from 0 to {@code tree.size()}
         * @return the elements of rank below {@code n} and those of rank {@code n} or more
         */
        static <T extends @Nullable Object> Tuple2<RedBlackTree<T>, RedBlackTree<T>> splitAt(RedBlackTree<T> tree, int n) {
            return Tuple.of(take(tree, n), drop(tree, n));
        }

        /**
         * The elements of rank below {@code n}, as a tree with a black root: the first half of
         * {@link #splitAt(RedBlackTree, int)}, without building the second. O(log n).
         *
         * @param tree a tree
         * @param n    the number of elements kept, from 0 to {@code tree.size()}
         * @return the first {@code n} elements
         */
        public static <T extends @Nullable Object> RedBlackTree<T> take(RedBlackTree<T> tree, int n) {
            if (n <= 0 || tree.isEmpty()) {
                return tree.emptyInstance();
            } else if (n >= tree.size()) {
                return color(tree, BLACK);
            }
            final Node<T> node = (Node<T>) tree;
            final int leftSize = node.left.size();
            if (n < leftSize) {
                return take(node.left, n);
            } else if (n == leftSize) {
                return color(node.left, BLACK);
            } else {
                return join(color(node.left, BLACK), node.value, take(node.right, n - leftSize - 1));
            }
        }

        /**
         * The elements of rank {@code n} or more, as a tree with a black root: the second half of
         * {@link #splitAt(RedBlackTree, int)}, without building the first. O(log n).
         *
         * @param tree a tree
         * @param n    the number of elements dropped, from 0 to {@code tree.size()}
         * @return the elements after the first {@code n}
         */
        public static <T extends @Nullable Object> RedBlackTree<T> drop(RedBlackTree<T> tree, int n) {
            if (n <= 0 || tree.isEmpty()) {
                return color(tree, BLACK);
            } else if (n >= tree.size()) {
                return tree.emptyInstance();
            }
            final Node<T> node = (Node<T>) tree;
            final int leftSize = node.left.size();
            if (n <= leftSize) {
                return join(drop(node.left, n), node.value, color(node.right, BLACK));
            } else {
                return drop(node.right, n - leftSize - 1);
            }
        }

        /**
         * The elements of rank {@code from} (inclusive) to {@code until} (exclusive), clamped to the tree, as a tree
         * with a black root: one {@link #drop(RedBlackTree, int)} then one {@link #take(RedBlackTree, int)}, so
         * O(log n); the result shares its untouched subtrees with {@code tree}.
         */
        public static <T extends @Nullable Object> RedBlackTree<T> slice(RedBlackTree<T> tree, int from, int until) {
            final int size = tree.size();
            final int start = Math.max(from, 0);
            final int end = Math.min(until, size);
            if (start >= end) {
                return tree.emptyInstance();
            } else if (start == 0 && end == size) {
                return tree;
            } else {
                return take(drop(tree, start), end - start);
            }
        }

        /**
         * The number of leading elements, in order, for which {@code predicate} returns {@code expected}: the length
         * of the prefix {@code takeWhile} ({@code expected} true) or {@code takeUntil} ({@code expected} false) keeps.
         * O(k) for a prefix of k elements.
         */
        public static <T extends @Nullable Object> int prefixLength(RedBlackTree<T> tree, java.util.function.Predicate<? super T> predicate, boolean expected) {
            int length = 0;
            final java.util.Iterator<T> iterator = tree.iterator();
            while (iterator.hasNext() && predicate.test(iterator.next()) == expected) {
                length++;
            }
            return length;
        }

        /**
         * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous,
         * each wrapped by {@code wrap}; the loop and the window rule are {@link Vector#sliding(int, int)}'s. Each
         * window is one {@link #slice(RedBlackTree, int, int)}, so O((n / step) log n).
         */
        public static <T extends @Nullable Object, R extends @Nullable Object> Vector<R> sliding(RedBlackTree<T> tree, int size, int step,
                java.util.function.Function<RedBlackTree<T>, R> wrap) {
            Collections.checkWindow(size, step);
            final int length = tree.size();
            if (length == 0) {
                return Vector.empty();
            }
            final Vector.Builder<R> builder = Vector.newBuilder();
            // past the first, a window is produced only while it holds at least one element the previous one did not
            for (long start = 0; start < length && (start == 0 || start - step + size < length); start += step) {
                builder.add(wrap.apply(slice(tree, (int) start, (int) Math.min(start + size, length))));
            }
            return builder.result();
        }

        /**
         * The maximal runs of consecutive elements with an equal key, {@code classifier} called once per element in
         * order, each run wrapped by {@code wrap}. One walk, then one {@link #slice(RedBlackTree, int, int)} per run:
         * O(n + r log n) for r runs.
         */
        public static <T extends @Nullable Object, R extends @Nullable Object> Vector<R> slideBy(RedBlackTree<T> tree,
                java.util.function.Function<? super T, ?> classifier, java.util.function.Function<RedBlackTree<T>, R> wrap) {
            Objects.requireNonNull(classifier, "classifier is null");
            if (tree.isEmpty()) {
                return Vector.empty();
            }
            final Vector.Builder<R> builder = Vector.newBuilder();
            final java.util.Iterator<T> iterator = tree.iterator();
            Object key = classifier.apply(iterator.next());
            int start = 0;
            int index = 1;
            while (iterator.hasNext()) {
                final Object next = classifier.apply(iterator.next());
                if (!Objects.equals(key, next)) {
                    builder.add(wrap.apply(slice(tree, start, index)));
                    start = index;
                    key = next;
                }
                index++;
            }
            builder.add(wrap.apply(slice(tree, start, index)));
            return builder.result();
        }

        /**
         * The elements paired with their rank, in order. O(n).
         */
        public static <T extends @Nullable Object> Vector<Tuple2<T, Integer>> zipWithIndex(RedBlackTree<T> tree) {
            final Vector.Builder<Tuple2<T, Integer>> builder = Vector.newBuilder(tree.size());
            int index = 0;
            for (T value : tree) {
                builder.add(Tuple.of(value, index++));
            }
            return builder.result();
        }

        private static <T extends @Nullable Object> Tuple2<Node<T>, Boolean> unbalancedLeft(Color color, int blackHeight, RedBlackTree<T> left,
                T value, RedBlackTree<T> right, Empty<T> empty) {
            if (!left.isEmpty()) {
                final Node<T> ln = (Node<T>) left;
                if (ln.color == BLACK) {
                    final Node<T> newNode = Node.balanceLeft(BLACK, blackHeight, ln.color(RED), value, right, empty);
                    return Tuple.of(newNode, color == BLACK);
                } else if (color == BLACK && !ln.right.isEmpty()) {
                    final Node<T> lrn = (Node<T>) ln.right;
                    if (lrn.color == BLACK) {
                        final Node<T> newRightNode = Node.balanceLeft(BLACK, blackHeight, lrn.color(RED), value, right,
                                empty);
                        final Node<T> newNode = new Node<>(BLACK, ln.blackHeight, ln.left, ln.value, newRightNode,
                                empty);
                        return Tuple.of(newNode, false);
                    }
                }
            }
            throw new IllegalStateException("unbalancedLeft(" + color + ", " + blackHeight + ", " + left + ", " + value + ", " + right + ")");
        }

        private static <T extends @Nullable Object> Tuple2<Node<T>, Boolean> unbalancedRight(Color color, int blackHeight, RedBlackTree<T> left,
                T value, RedBlackTree<T> right, Empty<T> empty) {
            if (!right.isEmpty()) {
                final Node<T> rn = (Node<T>) right;
                if (rn.color == BLACK) {
                    final Node<T> newNode = Node.balanceRight(BLACK, blackHeight, left, value, rn.color(RED), empty);
                    return Tuple.of(newNode, color == BLACK);
                } else if (color == BLACK && !rn.left.isEmpty()) {
                    final Node<T> rln = (Node<T>) rn.left;
                    if (rln.color == BLACK) {
                        final Node<T> newLeftNode = Node.balanceRight(BLACK, blackHeight, left, value, rln.color(RED),
                                empty);
                        final Node<T> newNode = new Node<>(BLACK, rn.blackHeight, newLeftNode, rn.value, rn.right,
                                empty);
                        return Tuple.of(newNode, false);
                    }
                }
            }
            throw new IllegalStateException("unbalancedRight(" + color + ", " + blackHeight + ", " + left + ", " + value + ", " + right + ")");
        }
    }

    /**
     * The empty tree node. It can't be a singleton because it depends on a {@link Comparator}.
     *
     * @param <T> Component type
     */
    final class Empty<T extends @Nullable Object> implements RedBlackTree<T> {

        final Comparator<T> comparator;

        // This is no public API! The RedBlackTree takes care of passing the correct Comparator.
        @SuppressWarnings("unchecked")
        Empty(Comparator<? super T> comparator) {
            this.comparator = (Comparator<T>) comparator;
        }

        @Override
        public Color color() {
            return BLACK;
        }

        @Override
        public Comparator<T> comparator() {
            return comparator;
        }

        @Override
        public boolean contains(T value) {
            return false;
        }

        @Override
        public Empty<T> emptyInstance() {
            return this;
        }

        @Override
        public Option<T> find(T value) {
            return Option.none();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public RedBlackTree<T> left() {
            throw new UnsupportedOperationException("left on empty");
        }

        @Override
        public RedBlackTree<T> right() {
            throw new UnsupportedOperationException("right on empty");
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public T value() {
            throw new NoSuchElementException("value on empty");
        }

        @Override
        public String toString() {
            return "()";
        }
    }
}
