package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.Vector;
import dev.zazr.collection.internal.RedBlackTreeModule.Empty;
import dev.zazr.collection.internal.RedBlackTreeModule.Node;
import dev.zazr.control.Option;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import static dev.zazr.collection.internal.RedBlackTree.Color.BLACK;
import static dev.zazr.collection.internal.RedBlackTree.Color.RED;

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
            int result = empty.comparator.compare(value, this.value);
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
            int result = empty.comparator.compare(value, this.value);
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
                Node<?> node = (Node<?>) tree;
                String value = node.color + ":" + node.value;
                if (node.isLeaf()) {
                    return value;
                } else {
                    String left = node.left.isEmpty() ? "" : " " + toLispString(node.left);
                    String right = node.right.isEmpty() ? "" : " " + toLispString(node.right);
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

        private static <T extends @Nullable Object> Node<T> balanceLeft(
                Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
            if (color == BLACK) {
                if (!left.isEmpty()) {
                    Node<T> ln = (Node<T>) left;
                    if (ln.color == RED) {
                        if (!ln.left.isEmpty()) {
                            Node<T> lln = (Node<T>) ln.left;
                            if (lln.color == RED) {
                                Node<T> newLeft = new Node<>(BLACK, blackHeight, lln.left, lln.value, lln.right, empty);
                                Node<T> newRight = new Node<>(BLACK, blackHeight, ln.right, value, right, empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, ln.value, newRight, empty);
                            }
                        }
                        if (!ln.right.isEmpty()) {
                            Node<T> lrn = (Node<T>) ln.right;
                            if (lrn.color == RED) {
                                Node<T> newLeft = new Node<>(BLACK, blackHeight, ln.left, ln.value, lrn.left, empty);
                                Node<T> newRight = new Node<>(BLACK, blackHeight, lrn.right, value, right, empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, lrn.value, newRight, empty);
                            }
                        }
                    }
                }
            }
            return new Node<>(color, blackHeight, left, value, right, empty);
        }

        private static <T extends @Nullable Object> Node<T> balanceRight(
                Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
            if (color == BLACK) {
                if (!right.isEmpty()) {
                    Node<T> rn = (Node<T>) right;
                    if (rn.color == RED) {
                        if (!rn.right.isEmpty()) {
                            Node<T> rrn = (Node<T>) rn.right;
                            if (rrn.color == RED) {
                                Node<T> newLeft = new Node<>(BLACK, blackHeight, left, value, rn.left, empty);
                                Node<T> newRight =
                                        new Node<>(BLACK, blackHeight, rrn.left, rrn.value, rrn.right, empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, rn.value, newRight, empty);
                            }
                        }
                        if (!rn.left.isEmpty()) {
                            Node<T> rln = (Node<T>) rn.left;
                            if (rln.color == RED) {
                                Node<T> newLeft = new Node<>(BLACK, blackHeight, left, value, rln.left, empty);
                                Node<T> newRight = new Node<>(BLACK, blackHeight, rln.right, rn.value, rn.right, empty);
                                return new Node<>(RED, blackHeight + 1, newLeft, rln.value, newRight, empty);
                            }
                        }
                    }
                }
            }
            return new Node<>(color, blackHeight, left, value, right, empty);
        }

        // The deletion. Each step returns the subtree it rebuilt, whose root can be red and whose black height is the
        // one of the subtree it replaces or one less. The caller tells which by comparing `blackNodes` of the result
        // with its own `blackHeight` field, which is the black height of each of its children before the deletion, so
        // nothing is allocated to pass that flag up.

        // the number of black nodes on each path from the root of `tree` down, the empty leaf counted as one: the
        // stored `blackHeight` counts the black nodes below a node whatever its own colour (so `color(BLACK)` keeps
        // it), and a black node adds itself
        private static <T extends @Nullable Object> int blackNodes(RedBlackTree<T> tree) {
            if (tree instanceof Node<T> node) {
                return node.color == BLACK ? node.blackHeight + 1 : node.blackHeight;
            } else {
                return 1;
            }
        }

        /// `tree` without the element whose key equals `key`, or `tree` itself when there is none: a deletion
        /// by key, with no probe element. `key` is the first argument of each comparison, as in [#findByKey];
        /// an empty tree compares nothing. One walk down the tree, then a rebalancing on the way back up when
        /// the key is found, O(log n); nothing is allocated when it is not, and only the rebuilt nodes when
        /// it is.
        public static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> deleteByKey(
                RedBlackTree<E> tree, K key, Comparator<? super K> comparator, boolean entries) {
            RedBlackTree<E> deleted = doDeleteByKey(tree, key, comparator, entries);
            return deleted == null ? tree : color(deleted, BLACK);
        }

        // `tree` without the element whose key equals `key`, or `null` when there is none, passed up unchanged so that
        // no level is rebuilt; the depth of the recursion is the height of the tree, at most 2 log2(n + 1)
        private static <E extends @Nullable Object, K extends @Nullable Object> @Nullable RedBlackTree<E> doDeleteByKey(
                RedBlackTree<E> tree, K key, Comparator<? super K> comparator, boolean entries) {
            if (!(tree instanceof Node<E> node)) {
                return null;
            }
            int c = compareKey(comparator, entries, key, node.value);
            if (c < 0) {
                RedBlackTree<E> deleted = doDeleteByKey(node.left, key, comparator, entries);
                return deleted == null ? null : deletedLeft(node, deleted);
            } else if (c > 0) {
                RedBlackTree<E> deleted = doDeleteByKey(node.right, key, comparator, entries);
                return deleted == null ? null : deletedRight(node, node.value, deleted);
            } else {
                return deleteRoot(node);
            }
        }

        // `node` with its left subtree replaced by `left`, the result of a deletion from it, rebalanced when the black
        // height of `left` dropped
        private static <T extends @Nullable Object> RedBlackTree<T> deletedLeft(Node<T> node, RedBlackTree<T> left) {
            if (blackNodes(left) < node.blackHeight) {
                return Node.unbalancedRight(node.color, node.blackHeight - 1, left, node.value, node.right, node.empty);
            } else {
                return new Node<>(node.color, node.blackHeight, left, node.value, node.right, node.empty);
            }
        }

        // `node` with `value` as its own element and its right subtree replaced by `right`, the result of a deletion
        // from it, rebalanced when the black height of `right` dropped
        private static <T extends @Nullable Object> RedBlackTree<T> deletedRight(
                Node<T> node, T value, RedBlackTree<T> right) {
            if (blackNodes(right) < node.blackHeight) {
                return Node.unbalancedLeft(node.color, node.blackHeight - 1, node.left, value, right, node.empty);
            } else {
                return new Node<>(node.color, node.blackHeight, node.left, value, right, node.empty);
            }
        }

        // `node` without its own element: with no right subtree, its left subtree, which is empty or a red leaf (taken
        // in black); otherwise the least element of the right subtree takes its place
        private static <T extends @Nullable Object> RedBlackTree<T> deleteRoot(Node<T> node) {
            if (node.right instanceof Node<T> right) {
                return deletedRight(node, minimum(right), deleteMin(right));
            } else {
                return color(node.left, BLACK);
            }
        }

        // `node` without its least element: with no left subtree, its right subtree, which is empty or a red leaf
        // (taken in black)
        private static <T extends @Nullable Object> RedBlackTree<T> deleteMin(Node<T> node) {
            if (node.left instanceof Node<T> left) {
                return deletedLeft(node, deleteMin(left));
            } else {
                return color(node.right, BLACK);
            }
        }

        static <T extends @Nullable Object> Node<T> insert(RedBlackTree<T> tree, T value) {
            if (tree.isEmpty()) {
                Empty<T> empty = (Empty<T>) tree;
                return new Node<>(RED, 1, empty, value, empty, empty);
            } else {
                Node<T> node = (Node<T>) tree;
                int comparison = node.comparator().compare(value, node.value);
                if (comparison < 0) {
                    Node<T> newLeft = insert(node.left, value);
                    return (newLeft == node.left)
                            ? node
                            : Node.balanceLeft(
                                    node.color, node.blackHeight, newLeft, node.value, node.right, node.empty);
                } else if (comparison > 0) {
                    Node<T> newRight = insert(node.right, value);
                    return (newRight == node.right)
                            ? node
                            : Node.balanceRight(
                                    node.color, node.blackHeight, node.left, node.value, newRight, node.empty);
                } else {
                    // DEV-NOTE: Even if there is no _comparison_ difference, the object may not be _equal_.
                    //           To save an equals() call, which may be expensive, we return a new instance.
                    return new Node<>(node.color, node.blackHeight, node.left, value, node.right, node.empty);
                }
            }
        }

        /// Returns a valid tree holding the elements of `t1`, then `value`, then the elements of `t2`. Every element
        /// of `t1` is less than `value` and every element of `t2` greater. The roots of `t1` and `t2` can be
        /// red.
        static <T extends @Nullable Object> RedBlackTree<T> join(RedBlackTree<T> t1, T value, RedBlackTree<T> t2) {
            if (t1.isEmpty()) {
                return insertMin(t2, value, (Empty<T>) t2.emptyInstance()).color(BLACK);
            } else if (t2.isEmpty()) {
                return insertMax(t1, value, (Empty<T>) t1.emptyInstance()).color(BLACK);
            } else {
                // The stored blackHeight of a node counts the node as black whatever its colour, so a red root has one
                // black node fewer on its paths than a black root with the same blackHeight. Colouring both roots black
                // makes the stored values comparable and keeps them right.
                Node<T> n1 = ((Node<T>) t1).color(BLACK);
                Node<T> n2 = ((Node<T>) t2).color(BLACK);
                int comparison = n1.blackHeight - n2.blackHeight;
                if (comparison < 0) {
                    return Node.joinLT(n1, value, n2, n1.blackHeight).color(BLACK);
                } else if (comparison > 0) {
                    return Node.joinGT(n1, value, n2, n2.blackHeight).color(BLACK);
                } else {
                    return new Node<>(BLACK, n1.blackHeight + 1, n1, value, n2, n1.empty);
                }
            }
        }

        // `insert` of a value less than every element of `tree`: the same descent down the left spine and the same
        // rebalancing, without calling the comparator
        private static <T extends @Nullable Object> Node<T> insertMin(RedBlackTree<T> tree, T value, Empty<T> empty) {
            if (tree.isEmpty()) {
                return new Node<>(RED, 1, empty, value, empty, empty);
            } else {
                Node<T> node = (Node<T>) tree;
                return Node.balanceLeft(
                        node.color,
                        node.blackHeight,
                        insertMin(node.left, value, empty),
                        node.value,
                        node.right,
                        node.empty);
            }
        }

        // `insert` of a value greater than every element of `tree`, without calling the comparator
        private static <T extends @Nullable Object> Node<T> insertMax(RedBlackTree<T> tree, T value, Empty<T> empty) {
            if (tree.isEmpty()) {
                return new Node<>(RED, 1, empty, value, empty, empty);
            } else {
                Node<T> node = (Node<T>) tree;
                return Node.balanceRight(
                        node.color,
                        node.blackHeight,
                        node.left,
                        node.value,
                        insertMax(node.right, value, empty),
                        node.empty);
            }
        }

        /// Returns a valid tree holding the elements of `t1`, then the elements of `t2`, sharing both when
        /// either is empty. Every element of `t1` is less than every element of `t2`; the roots can be red. The
        /// maximum of `t1` is the middle value of [#join]. The `join2` of the Scala 2.13+ collections (see
        /// [#filter]).
        private static <T extends @Nullable Object> RedBlackTree<T> join2(RedBlackTree<T> t1, RedBlackTree<T> t2) {
            if (t1.isEmpty()) {
                return t2;
            } else if (t2.isEmpty()) {
                return t1;
            } else {
                Node<T> n1 = (Node<T>) t1;
                return join(withoutMaximum(n1), maximum(n1), t2);
            }
        }

        // `node` without its greatest element, rebuilt with a join per level of the right spine; the root can be red
        private static <T extends @Nullable Object> RedBlackTree<T> withoutMaximum(Node<T> node) {
            if (node.right.isEmpty()) {
                return node.left;
            } else {
                return join(node.left, node.value, withoutMaximum((Node<T>) node.right));
            }
        }

        /// The elements of `tree` for which `predicate` holds, as a tree with a black root. Port of
        /// `filterEntries` in `scala.collection.immutable.RedBlackTree` of the Scala 2.13 collections library,
        /// which Scala 3 uses unchanged: every subtree whose elements are all kept is returned as it is, and
        /// the kept parts are rejoined with [#join] (a kept node) or [#join2] (a removed node), so the result
        /// shares every subtree the predicate leaves whole, and is `tree` itself when every element is kept.
        /// `predicate` is called once per element, in ascending order. O(n) for n elements, with no comparator
        /// call.
        public static <T extends @Nullable Object> RedBlackTree<T> filter(
                RedBlackTree<T> tree, java.util.function.Predicate<? super T> predicate) {
            return tree.isEmpty() ? tree : color(filter((Node<T>) tree, predicate), BLACK);
        }

        // the depth of the recursion is the height of the tree, at most 2 log2(n + 1)
        private static <T extends @Nullable Object> RedBlackTree<T> filter(
                Node<T> node, java.util.function.Predicate<? super T> predicate) {
            RedBlackTree<T> left = node.left.isEmpty() ? node.left : filter((Node<T>) node.left, predicate);
            boolean keep = predicate.test(node.value);
            RedBlackTree<T> right = node.right.isEmpty() ? node.right : filter((Node<T>) node.right, predicate);
            if (!keep) {
                return join2(left, right);
            } else if (left == node.left && right == node.right) {
                return node;
            } else {
                return join(left, node.value, right);
            }
        }

        /// The elements of `tree` for which `predicate` holds, then the others, as two trees with a black root,
        /// in one walk. Port of `partitionEntries` in the Scala 2.13+ collections (see [#filter]): each side
        /// shares every subtree whose elements all go to it, and is `tree` itself when it gets every element.
        /// `predicate` is called once per element, in ascending order. O(n) for n elements, with no comparator
        /// call.
        public static <T extends @Nullable Object> Tuple2<RedBlackTree<T>, RedBlackTree<T>> partition(
                RedBlackTree<T> tree, java.util.function.Predicate<? super T> predicate) {
            if (tree.isEmpty()) {
                return Tuple.of(tree, tree);
            }
            Partition<T> partition = new Partition<>(tree);
            partition.walk((Node<T>) tree, predicate);
            return Tuple.of(color(partition.kept, BLACK), color(partition.rejected, BLACK));
        }

        // the two results of a partition of a subtree, written by `walk` in place of a pair allocated per node
        private static final class Partition<T extends @Nullable Object> {

            RedBlackTree<T> kept;
            RedBlackTree<T> rejected;

            Partition(RedBlackTree<T> tree) {
                this.kept = tree;
                this.rejected = tree;
            }

            // sets both fields to the results for the subtree `node`; the depth of the recursion is the height of the
            // tree, at most 2 log2(n + 1)
            @SuppressWarnings("Var")
            void walk(Node<T> node, java.util.function.Predicate<? super T> predicate) {
                RedBlackTree<T> leftKept = node.left;
                RedBlackTree<T> leftRejected = node.left;
                if (!node.left.isEmpty()) {
                    walk((Node<T>) node.left, predicate);
                    leftKept = kept;
                    leftRejected = rejected;
                }
                boolean keep = predicate.test(node.value);
                RedBlackTree<T> rightKept = node.right;
                RedBlackTree<T> rightRejected = node.right;
                if (!node.right.isEmpty()) {
                    walk((Node<T>) node.right, predicate);
                    rightKept = kept;
                    rightRejected = rejected;
                }
                if (keep) {
                    kept = (leftKept == node.left && rightKept == node.right)
                            ? node
                            : join(leftKept, node.value, rightKept);
                    rejected = join2(leftRejected, rightRejected);
                } else {
                    kept = join2(leftKept, rightKept);
                    rejected = (leftRejected == node.left && rightRejected == node.right)
                            ? node
                            : join(leftRejected, node.value, rightRejected);
                }
            }
        }

        private static <T extends @Nullable Object> Node<T> joinGT(Node<T> n1, T value, Node<T> n2, int h2) {
            if (n1.blackHeight == h2) {
                return new Node<>(RED, h2 + 1, n1, value, n2, n1.empty);
            } else {
                Node<T> node = joinGT((Node<T>) n1.right, value, n2, h2);
                return Node.balanceRight(n1.color, n1.blackHeight, n1.left, n1.value, node, n2.empty);
            }
        }

        private static <T extends @Nullable Object> Node<T> joinLT(Node<T> n1, T value, Node<T> n2, int h1) {
            if (n2.blackHeight == h1) {
                return new Node<>(RED, h1 + 1, n1, value, n2, n1.empty);
            } else {
                Node<T> node = joinLT(n1, value, (Node<T>) n2.left, h1);
                return Node.balanceLeft(n2.color, n2.blackHeight, node, n2.value, n2.right, n2.empty);
            }
        }

        /// Returns a valid tree holding the elements of `t1`, then the elements of `t2`. Every element of `t1` is less
        /// than every element of `t2`. The roots of `t1` and `t2` can be red. The minimum of `t2` is taken out
        /// and used as the middle value of [#join].
        static <T extends @Nullable Object> RedBlackTree<T> merge(RedBlackTree<T> t1, RedBlackTree<T> t2) {
            if (t1.isEmpty()) {
                return Node.color(t2, BLACK);
            } else if (t2.isEmpty()) {
                return Node.color(t1, BLACK);
            } else {
                Node<T> n2 = (Node<T>) t2;
                return Node.join(t1, minimum(n2), Node.deleteMin(n2));
            }
        }

        /// Returns a balanced tree of the first `size` elements of `sorted`, which are strictly increasing
        /// under the comparator of `empty`, in O(size) and with exactly `size` nodes. Port of `fromOrderedKeys`
        /// in `scala.collection.immutable.RedBlackTree` of the Scala 2.13 collections library, which Scala 3
        /// uses unchanged: the range is split around its middle element (the left part is never larger than the
        /// right), every node is black except the one-element subtrees on the deepest level, which are red; so
        /// every path has the same number of black nodes and no red node has a red child.
        static <T extends @Nullable Object> RedBlackTree<T> fromOrdered(
                Empty<T> empty, @Nullable Object[] sorted, int size) {
            // the deepest level holding a node, the root being on level 1
            int maxUsedDepth = Integer.SIZE - Integer.numberOfLeadingZeros(size);
            return fromOrdered(empty, sorted, 0, size, 1, maxUsedDepth);
        }

        // the slots read are within the first `size` of `sorted`, which hold elements, never null
        @SuppressWarnings({"unchecked", "NullAway"})
        private static <T extends @Nullable Object> RedBlackTree<T> fromOrdered(
                Empty<T> empty, @Nullable Object[] sorted, int from, int size, int level, int maxUsedDepth) {
            if (size == 0) {
                return empty;
            } else if (size == 1) {
                Color color = (level != maxUsedDepth || level == 1) ? BLACK : RED;
                return new Node<>(color, 1, empty, (T) sorted[from], empty, empty);
            } else {
                int leftSize = (size - 1) / 2;
                RedBlackTree<T> left = fromOrdered(empty, sorted, from, leftSize, level + 1, maxUsedDepth);
                RedBlackTree<T> right =
                        fromOrdered(empty, sorted, from + leftSize + 1, size - 1 - leftSize, level + 1, maxUsedDepth);
                // the stored blackHeight counts the black nodes below this one, the empty tree counting as one
                int blackHeight = left.isEmpty() ? 1 : ((Node<T>) left).blackHeight + (left.color() == BLACK ? 1 : 0);
                return new Node<>(BLACK, blackHeight, left, (T) sorted[from + leftSize], right, empty);
            }
        }

        /// Returns a tree of the same shape and colours as `tree`, holding `mapper.apply(e)` in place of each element
        /// `e` and ordered by `comparator`. `mapper` must be strictly increasing from the order of `tree` to
        /// `comparator` (not checked): the result is then a valid red-black tree, built in O(n) with no
        /// comparison and exactly one node per element. `mapper` is called once per element, in ascending
        /// order.
        public static <T extends @Nullable Object, R extends @Nullable Object> RedBlackTree<R> mapOrdered(
                RedBlackTree<T> tree,
                Comparator<? super R> comparator,
                java.util.function.Function<? super T, ? extends R> mapper) {
            return mapOrdered(tree, new Empty<>(comparator), mapper);
        }

        // the depth of the recursion is the height of the tree, at most 2 log2(n + 1)
        private static <T extends @Nullable Object, R extends @Nullable Object> RedBlackTree<R> mapOrdered(
                RedBlackTree<T> tree, Empty<R> empty, java.util.function.Function<? super T, ? extends R> mapper) {
            if (tree.isEmpty()) {
                return empty;
            }
            Node<T> node = (Node<T>) tree;
            RedBlackTree<R> left = mapOrdered(node.left, empty, mapper);
            R value = mapper.apply(node.value);
            RedBlackTree<R> right = mapOrdered(node.right, empty, mapper);
            return new Node<>(node.color, node.blackHeight, left, value, right, empty);
        }

        public static <T extends @Nullable Object> T maximum(Node<T> node) {
            @SuppressWarnings("Var")
            Node<T> curr = node;
            while (!curr.right.isEmpty()) {
                curr = (Node<T>) curr.right;
            }
            return curr.value;
        }

        public static <T extends @Nullable Object> T minimum(Node<T> node) {
            @SuppressWarnings("Var")
            Node<T> curr = node;
            while (!curr.left.isEmpty()) {
                curr = (Node<T>) curr.left;
            }
            return curr.value;
        }

        static <T extends @Nullable Object> Tuple2<RedBlackTree<T>, RedBlackTree<T>> split(
                RedBlackTree<T> tree, T value) {
            if (tree.isEmpty()) {
                return Tuple.of(tree, tree);
            } else {
                Node<T> node = (Node<T>) tree;
                int comparison = node.comparator().compare(value, node.value);
                if (comparison < 0) {
                    Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(node.left, value);
                    return Tuple.of(split._1(), Node.join(split._2(), node.value, Node.color(node.right, BLACK)));
                } else if (comparison > 0) {
                    Tuple2<RedBlackTree<T>, RedBlackTree<T>> split = Node.split(node.right, value);
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
        static <T extends @Nullable Object> Tuple2<RedBlackTree<T>, RedBlackTree<T>> splitAt(
                RedBlackTree<T> tree, int n) {
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
            Node<T> node = (Node<T>) tree;
            int leftSize = node.left.size();
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
            Node<T> node = (Node<T>) tree;
            int leftSize = node.left.size();
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
            int size = tree.size();
            int start = Math.max(from, 0);
            int end = Math.min(until, size);
            if (start >= end) {
                return tree.emptyInstance();
            } else if (start == 0 && end == size) {
                return tree;
            } else {
                return take(drop(tree, start), end - start);
            }
        }

        // -- ranges by key

        // The range operations compare a bound with the key of an element: the element itself for a set
        // (`entries` false), the first component of a `Tuple2` entry for a map (`entries` true), so that no probe
        // element is built. They are ports of `from`, `until`, `to`, `range`, `minAfter` and `maxBefore` (and their
        // `doFrom`, `doUntil`, `doTo`, `doRange` helpers) in `scala.collection.immutable.RedBlackTree` of the Scala
        // 2.13 collections library, which Scala 3 uses unchanged: the walk follows the path of the bound, every
        // subtree wholly inside the range is kept as it is, and the kept parts are rejoined with [#join], so the result
        // shares every untouched subtree and is `tree` itself when nothing is cut.

        @SuppressWarnings("unchecked")
        private static <E extends @Nullable Object, K extends @Nullable Object> int compareKey(
                Comparator<? super K> comparator, boolean entries, K key, E element) {
            K other = entries ? ((Tuple2<K, ?>) element)._1() : (K) element;
            return comparator.compare(key, other);
        }

        // `tree` itself when nothing is cut, otherwise the result with a black root
        private static <E extends @Nullable Object> RedBlackTree<E> blackRoot(
                RedBlackTree<E> tree, RedBlackTree<E> cut) {
            return cut == tree ? tree : color(cut, BLACK);
        }

        /// The elements whose key is greater than or equal to `from`. O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> rangeFrom(
                RedBlackTree<E> tree, K from, Comparator<? super K> comparator, boolean entries) {
            return blackRoot(tree, doFrom(tree, from, comparator, entries));
        }

        /// The elements whose key is less than `until`. O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> rangeUntil(
                RedBlackTree<E> tree, K until, Comparator<? super K> comparator, boolean entries) {
            return blackRoot(tree, doUntil(tree, until, comparator, entries));
        }

        /// The elements whose key is less than or equal to `to`. O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> rangeTo(
                RedBlackTree<E> tree, K to, Comparator<? super K> comparator, boolean entries) {
            return blackRoot(tree, doTo(tree, to, comparator, entries));
        }

        /// The elements whose key is greater than or equal to `from` and less than `until`; none when `from` is not
        /// less than `until`. O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> range(
                RedBlackTree<E> tree, K from, K until, Comparator<? super K> comparator, boolean entries) {
            return blackRoot(tree, doRange(tree, from, until, comparator, entries));
        }

        // the depth of the recursion of the four walks below is the height of the tree, at most 2 log2(n + 1)
        private static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> doFrom(
                RedBlackTree<E> tree, K from, Comparator<? super K> comparator, boolean entries) {
            if (!(tree instanceof Node<E> node)) {
                return tree;
            } else if (compareKey(comparator, entries, from, node.value) > 0) {
                return doFrom(node.right, from, comparator, entries);
            }
            RedBlackTree<E> left = doFrom(node.left, from, comparator, entries);
            return left == node.left ? node : join(left, node.value, node.right);
        }

        private static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> doUntil(
                RedBlackTree<E> tree, K until, Comparator<? super K> comparator, boolean entries) {
            if (!(tree instanceof Node<E> node)) {
                return tree;
            } else if (compareKey(comparator, entries, until, node.value) <= 0) {
                return doUntil(node.left, until, comparator, entries);
            }
            RedBlackTree<E> right = doUntil(node.right, until, comparator, entries);
            return right == node.right ? node : join(node.left, node.value, right);
        }

        private static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> doTo(
                RedBlackTree<E> tree, K to, Comparator<? super K> comparator, boolean entries) {
            if (!(tree instanceof Node<E> node)) {
                return tree;
            } else if (compareKey(comparator, entries, to, node.value) < 0) {
                return doTo(node.left, to, comparator, entries);
            }
            RedBlackTree<E> right = doTo(node.right, to, comparator, entries);
            return right == node.right ? node : join(node.left, node.value, right);
        }

        private static <E extends @Nullable Object, K extends @Nullable Object> RedBlackTree<E> doRange(
                RedBlackTree<E> tree, K from, K until, Comparator<? super K> comparator, boolean entries) {
            if (!(tree instanceof Node<E> node)) {
                return tree;
            } else if (compareKey(comparator, entries, from, node.value) > 0) {
                return doRange(node.right, from, until, comparator, entries);
            } else if (compareKey(comparator, entries, until, node.value) <= 0) {
                return doRange(node.left, from, until, comparator, entries);
            }
            RedBlackTree<E> left = doFrom(node.left, from, comparator, entries);
            RedBlackTree<E> right = doUntil(node.right, until, comparator, entries);
            return left == node.left && right == node.right ? node : join(left, node.value, right);
        }

        /// The element whose key equals `key`, or `null` when there is none: a lookup by key, with no probe
        /// element and no `Option`. `key` is the first argument of each comparison, as the probe element is in
        /// [Node#find]; an empty tree compares nothing. One walk down the tree, O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> @Nullable E findByKey(
                RedBlackTree<E> tree, K key, Comparator<? super K> comparator, boolean entries) {
            @SuppressWarnings("Var")
            RedBlackTree<E> t = tree;
            while (t instanceof Node<E> node) {
                int c = compareKey(comparator, entries, key, node.value);
                if (c == 0) {
                    return node.value;
                }
                t = c < 0 ? node.left : node.right;
            }
            return null;
        }

        /// The least element whose key is greater than or equal to `key`, or `null` when there is none. One walk
        /// down the tree, O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> @Nullable E minAfter(
                RedBlackTree<E> tree, K key, Comparator<? super K> comparator, boolean entries) {
            @SuppressWarnings("Var")
            RedBlackTree<E> t = tree;
            @SuppressWarnings("Var")
            E result = null;
            while (t instanceof Node<E> node) {
                int c = compareKey(comparator, entries, key, node.value);
                if (c == 0) {
                    return node.value;
                } else if (c < 0) {
                    result = node.value;
                    t = node.left;
                } else {
                    t = node.right;
                }
            }
            return result;
        }

        /// The greatest element whose key is less than `key`, or `null` when there is none. One walk down the
        /// tree, O(log n).
        public static <E extends @Nullable Object, K extends @Nullable Object> @Nullable E maxBefore(
                RedBlackTree<E> tree, K key, Comparator<? super K> comparator, boolean entries) {
            @SuppressWarnings("Var")
            RedBlackTree<E> t = tree;
            @SuppressWarnings("Var")
            E result = null;
            while (t instanceof Node<E> node) {
                if (compareKey(comparator, entries, key, node.value) <= 0) {
                    t = node.left;
                } else {
                    result = node.value;
                    t = node.right;
                }
            }
            return result;
        }

        /// The elements whose key is greater than or equal to `start`, in order: the iterator of the tree started
        /// at `start`. O(log n) to create, then O(1) per step on average.
        public static <E extends @Nullable Object, K extends @Nullable Object> Iterator<E> iteratorFrom(
                RedBlackTree<E> tree, K start, Comparator<? super K> comparator, boolean entries) {
            if (!(tree instanceof Node<E> root)) {
                return Iterator.empty();
            }
            InOrderIterator<E> iterator = new InOrderIterator<>(root);
            // Scala's `startFrom`: the nodes whose key is at least `start` are pushed on the way down, the path
            // turning left at each of them and right at the others
            @SuppressWarnings("Var")
            RedBlackTree<E> t = root;
            while (t instanceof Node<E> node) {
                if (compareKey(comparator, entries, start, node.value) <= 0) {
                    iterator.push(node);
                    t = node.left;
                } else {
                    t = node.right;
                }
            }
            return iterator;
        }

        /**
         * The number of leading elements, in order, for which {@code predicate} returns {@code expected}: the length
         * of the prefix {@code takeWhile} ({@code expected} true) or {@code takeUntil} ({@code expected} false) keeps.
         * O(k) for a prefix of k elements.
         */
        public static <T extends @Nullable Object> int prefixLength(
                RedBlackTree<T> tree, java.util.function.Predicate<? super T> predicate, boolean expected) {
            @SuppressWarnings("Var")
            int length = 0;
            java.util.Iterator<T> iterator = tree.iterator();
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
        public static <T extends @Nullable Object, R extends @Nullable Object> Vector<R> sliding(
                RedBlackTree<T> tree, int size, int step, java.util.function.Function<RedBlackTree<T>, R> wrap) {
            Collections.checkWindow(size, step);
            int length = tree.size();
            if (length == 0) {
                return Vector.empty();
            }
            Vector.Builder<R> builder = Vector.newBuilder();
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
        @SuppressWarnings("Var")
        public static <T extends @Nullable Object, R extends @Nullable Object> Vector<R> slideBy(
                RedBlackTree<T> tree,
                java.util.function.Function<? super T, ?> classifier,
                java.util.function.Function<RedBlackTree<T>, R> wrap) {
            Objects.requireNonNull(classifier, "classifier is null");
            if (tree.isEmpty()) {
                return Vector.empty();
            }
            Vector.Builder<R> builder = Vector.newBuilder();
            java.util.Iterator<T> iterator = tree.iterator();
            Object key = classifier.apply(iterator.next());
            int start = 0;
            int index = 1;
            while (iterator.hasNext()) {
                Object next = classifier.apply(iterator.next());
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
            Vector.Builder<Tuple2<T, Integer>> builder = Vector.newBuilder(tree.size());
            @SuppressWarnings("Var")
            int index = 0;
            for (T value : tree) {
                builder.add(Tuple.of(value, index++));
            }
            return builder.result();
        }

        private static <T extends @Nullable Object> Node<T> unbalancedLeft(
                Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
            if (!left.isEmpty()) {
                Node<T> ln = (Node<T>) left;
                if (ln.color == BLACK) {
                    Node<T> newNode = Node.balanceLeft(BLACK, blackHeight, ln.color(RED), value, right, empty);
                    return newNode;
                } else if (color == BLACK && !ln.right.isEmpty()) {
                    Node<T> lrn = (Node<T>) ln.right;
                    if (lrn.color == BLACK) {
                        Node<T> newRightNode =
                                Node.balanceLeft(BLACK, blackHeight, lrn.color(RED), value, right, empty);
                        Node<T> newNode = new Node<>(BLACK, ln.blackHeight, ln.left, ln.value, newRightNode, empty);
                        return newNode;
                    }
                }
            }
            throw new IllegalStateException(
                    "unbalancedLeft(" + color + ", " + blackHeight + ", " + left + ", " + value + ", " + right + ")");
        }

        private static <T extends @Nullable Object> Node<T> unbalancedRight(
                Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
            if (!right.isEmpty()) {
                Node<T> rn = (Node<T>) right;
                if (rn.color == BLACK) {
                    Node<T> newNode = Node.balanceRight(BLACK, blackHeight, left, value, rn.color(RED), empty);
                    return newNode;
                } else if (color == BLACK && !rn.left.isEmpty()) {
                    Node<T> rln = (Node<T>) rn.left;
                    if (rln.color == BLACK) {
                        Node<T> newLeftNode = Node.balanceRight(BLACK, blackHeight, left, value, rln.color(RED), empty);
                        Node<T> newNode = new Node<>(BLACK, rn.blackHeight, newLeftNode, rn.value, rn.right, empty);
                        return newNode;
                    }
                }
            }
            throw new IllegalStateException(
                    "unbalancedRight(" + color + ", " + blackHeight + ", " + left + ", " + value + ", " + right + ")");
        }
    }

    /**
     * An in-order walk of a tree, with the path of nodes whose value is still to be returned kept on an array stack,
     * the next one on top: O(log n) to create, then O(1) per step on average, nothing allocated per step.
     *
     * @param <T> Component type
     */
    final class InOrderIterator<T extends @Nullable Object> extends AbstractIterator<T> {

        // A red-black tree is at most twice as high as its black height, so the first array is almost always big
        // enough; it grows otherwise.
        private @Nullable Node<?>[] stack;
        private int depth = 0;

        InOrderIterator(Node<T> root) {
            this.stack = new Node<?>[Math.max(4, 2 * root.blackHeight + 2)];
        }

        /** Every element of the non-empty {@code root}, in order. */
        static <T extends @Nullable Object> InOrderIterator<T> all(Node<T> root) {
            InOrderIterator<T> iterator = new InOrderIterator<>(root);
            iterator.pushLeftChildren(root);
            return iterator;
        }

        @Override
        public boolean hasNext() {
            return depth > 0;
        }

        // AbstractIterator only calls getNext() after hasNext() returned true: the top of the stack is a node
        @SuppressWarnings({"unchecked", "NullAway"})
        @Override
        public T getNext() {
            Node<T> node = (Node<T>) stack[--depth];
            stack[depth] = null;
            if (node.right instanceof Node<T> right) {
                pushLeftChildren(right);
            }
            return node.value;
        }

        void push(Node<T> node) {
            if (depth == stack.length) {
                stack = Arrays.copyOf(stack, depth * 2);
            }
            stack[depth++] = node;
        }

        private void pushLeftChildren(Node<T> node) {
            @SuppressWarnings("Var")
            RedBlackTree<T> tree = node;
            while (tree instanceof Node<T> next) {
                push(next);
                tree = next.left;
            }
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
