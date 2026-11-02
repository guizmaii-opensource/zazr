package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.collection.internal.RedBlackTree.Color;
import dev.zazr.collection.internal.RedBlackTreeModule.Empty;
import dev.zazr.collection.internal.RedBlackTreeModule.Node;

import static dev.zazr.collection.internal.RedBlackTree.Color.BLACK;
import static dev.zazr.collection.internal.RedBlackTree.Color.RED;

/// A reference deletion for the differential tests of the red-black tree: the element `delete`, `deleteMin` and
/// `merge` written with a `Tuple2` (a `Tuple3` for `deleteMin`) that carries "the black height of this subtree
/// dropped" up each level as an explicit flag. It is kept apart from the main code on purpose, so that the tests
/// compare the main deletion, which reads that flag from the black heights, with an independent one.
///
/// Each level also checks the property the main deletion relies on: the flag is true exactly when the black height of
/// the result, read from its `blackHeight` field and its colour, is less than the one of the subtree it replaces.
final class RedBlackTreeDeletionReference {

    private RedBlackTreeDeletionReference() {}

    /// The black nodes on each path from the root of `tree` down, the empty leaf counted as one.
    static <T> int blackNodes(RedBlackTree<T> tree) {
        if (tree instanceof Node<T> node) {
            return node.blackHeight + (node.color == BLACK ? 1 : 0);
        } else {
            return 1;
        }
    }

    /// `tree` without `value`, with a black root; a new tree of the same shape when `value` is absent.
    static <T> RedBlackTree<T> delete(RedBlackTree<T> tree, T value) {
        return Node.color(deleteFrom(tree, value)._1(), BLACK);
    }

    /// The valid tree of the elements of `t1` then those of `t2`.
    static <T> RedBlackTree<T> merge(RedBlackTree<T> t1, RedBlackTree<T> t2) {
        if (t1.isEmpty()) {
            return Node.color(t2, BLACK);
        } else if (t2.isEmpty()) {
            return Node.color(t1, BLACK);
        } else {
            Tuple3<? extends RedBlackTree<T>, Boolean, T> withoutMinimum = deleteMin((Node<T>) t2);
            return Node.join(t1, withoutMinimum._3(), withoutMinimum._1());
        }
    }

    private static <T> void checkFlag(RedBlackTree<T> before, RedBlackTree<T> after, boolean dropped) {
        boolean derived = blackNodes(after) < blackNodes(before);
        if (dropped != derived) {
            throw new AssertionError(
                    "flag " + dropped + " but the black heights say " + derived + ": " + before + " became " + after);
        }
    }

    private static <T> Tuple2<? extends RedBlackTree<T>, Boolean> deleteFrom(RedBlackTree<T> tree, T value) {
        Tuple2<? extends RedBlackTree<T>, Boolean> result = doDelete(tree, value);
        checkFlag(tree, result._1(), result._2());
        return result;
    }

    private static <T> Tuple2<? extends RedBlackTree<T>, Boolean> doDelete(RedBlackTree<T> tree, T value) {
        if (tree.isEmpty()) {
            return Tuple.of(tree, false);
        } else {
            Node<T> node = (Node<T>) tree;
            int comparison = node.comparator().compare(value, node.value);
            if (comparison < 0) {
                return deletedLeft(node, deleteFrom(node.left, value));
            } else if (comparison > 0) {
                return deletedRight(node, deleteFrom(node.right, value));
            } else {
                return deleteRoot(node);
            }
        }
    }

    private static <T> Tuple2<? extends RedBlackTree<T>, Boolean> blackify(RedBlackTree<T> tree) {
        if (tree instanceof Node) {
            Node<T> node = (Node<T>) tree;
            if (node.color == RED) {
                return Tuple.of(node.color(BLACK), false);
            }
        }
        return Tuple.of(tree, true);
    }

    private static <T> Tuple2<? extends RedBlackTree<T>, Boolean> deletedLeft(
            Node<T> node, Tuple2<? extends RedBlackTree<T>, Boolean> deleted) {
        RedBlackTree<T> l = deleted._1();
        boolean d = deleted._2();
        if (d) {
            return unbalancedRight(node.color, node.blackHeight - 1, l, node.value, node.right, node.empty);
        } else {
            Node<T> newNode = new Node<>(node.color, node.blackHeight, l, node.value, node.right, node.empty);
            return Tuple.of(newNode, false);
        }
    }

    private static <T> Tuple2<? extends RedBlackTree<T>, Boolean> deletedRight(
            Node<T> node, Tuple2<? extends RedBlackTree<T>, Boolean> deleted) {
        RedBlackTree<T> r = deleted._1();
        boolean d = deleted._2();
        if (d) {
            return unbalancedLeft(node.color, node.blackHeight - 1, node.left, node.value, r, node.empty);
        } else {
            Node<T> newNode = new Node<>(node.color, node.blackHeight, node.left, node.value, r, node.empty);
            return Tuple.of(newNode, false);
        }
    }

    private static <T> Tuple2<? extends RedBlackTree<T>, Boolean> deleteRoot(Node<T> node) {
        if (node.right.isEmpty()) {
            if (node.color == BLACK) {
                return blackify(node.left);
            } else {
                return Tuple.of(node.left, false);
            }
        } else {
            Node<T> nodeRight = (Node<T>) node.right;
            Tuple3<? extends RedBlackTree<T>, Boolean, T> newRight = deleteMin(nodeRight);
            RedBlackTree<T> r = newRight._1();
            boolean d = newRight._2();
            T m = newRight._3();
            if (d) {
                return unbalancedLeft(node.color, node.blackHeight - 1, node.left, m, r, node.empty);
            } else {
                RedBlackTree<T> newNode = new Node<>(node.color, node.blackHeight, node.left, m, r, node.empty);
                return Tuple.of(newNode, false);
            }
        }
    }

    private static <T> Tuple3<? extends RedBlackTree<T>, Boolean, T> deleteMin(Node<T> node) {
        Tuple3<? extends RedBlackTree<T>, Boolean, T> result = doDeleteMin(node);
        checkFlag(node, result._1(), result._2());
        return result;
    }

    private static <T> Tuple3<? extends RedBlackTree<T>, Boolean, T> doDeleteMin(Node<T> node) {
        if (node.color() == BLACK && node.left().isEmpty() && node.right.isEmpty()) {
            return Tuple.of(node.empty, true, node.value());
        } else if (node.color() == BLACK
                && node.left().isEmpty()
                && node.right().color() == RED) {
            return Tuple.of(((Node<T>) node.right()).color(BLACK), false, node.value());
        } else if (node.color() == RED && node.left().isEmpty()) {
            return Tuple.of(node.right(), false, node.value());
        } else {
            Node<T> nodeLeft = (Node<T>) node.left;
            Tuple3<? extends RedBlackTree<T>, Boolean, T> newNode = deleteMin(nodeLeft);
            RedBlackTree<T> l = newNode._1();
            boolean deleted = newNode._2();
            T m = newNode._3();
            if (deleted) {
                Tuple2<Node<T>, Boolean> tD =
                        unbalancedRight(node.color, node.blackHeight - 1, l, node.value, node.right, node.empty);
                return Tuple.of(tD._1(), tD._2(), m);
            } else {
                Node<T> tD = new Node<>(node.color, node.blackHeight, l, node.value, node.right, node.empty);
                return Tuple.of(tD, false, m);
            }
        }
    }

    private static <T> Tuple2<Node<T>, Boolean> unbalancedLeft(
            Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
        if (!left.isEmpty()) {
            Node<T> ln = (Node<T>) left;
            if (ln.color == BLACK) {
                Node<T> newNode = balanceLeft(BLACK, blackHeight, ln.color(RED), value, right, empty);
                return Tuple.of(newNode, color == BLACK);
            } else if (color == BLACK && !ln.right.isEmpty()) {
                Node<T> lrn = (Node<T>) ln.right;
                if (lrn.color == BLACK) {
                    Node<T> newRightNode = balanceLeft(BLACK, blackHeight, lrn.color(RED), value, right, empty);
                    Node<T> newNode = new Node<>(BLACK, ln.blackHeight, ln.left, ln.value, newRightNode, empty);
                    return Tuple.of(newNode, false);
                }
            }
        }
        throw new IllegalStateException("unbalancedLeft");
    }

    private static <T> Tuple2<Node<T>, Boolean> unbalancedRight(
            Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
        if (!right.isEmpty()) {
            Node<T> rn = (Node<T>) right;
            if (rn.color == BLACK) {
                Node<T> newNode = balanceRight(BLACK, blackHeight, left, value, rn.color(RED), empty);
                return Tuple.of(newNode, color == BLACK);
            } else if (color == BLACK && !rn.left.isEmpty()) {
                Node<T> rln = (Node<T>) rn.left;
                if (rln.color == BLACK) {
                    Node<T> newLeftNode = balanceRight(BLACK, blackHeight, left, value, rln.color(RED), empty);
                    Node<T> newNode = new Node<>(BLACK, rn.blackHeight, newLeftNode, rn.value, rn.right, empty);
                    return Tuple.of(newNode, false);
                }
            }
        }
        throw new IllegalStateException("unbalancedRight");
    }

    private static <T> Node<T> balanceLeft(
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

    private static <T> Node<T> balanceRight(
            Color color, int blackHeight, RedBlackTree<T> left, T value, RedBlackTree<T> right, Empty<T> empty) {
        if (color == BLACK) {
            if (!right.isEmpty()) {
                Node<T> rn = (Node<T>) right;
                if (rn.color == RED) {
                    if (!rn.right.isEmpty()) {
                        Node<T> rrn = (Node<T>) rn.right;
                        if (rrn.color == RED) {
                            Node<T> newLeft = new Node<>(BLACK, blackHeight, left, value, rn.left, empty);
                            Node<T> newRight = new Node<>(BLACK, blackHeight, rrn.left, rrn.value, rrn.right, empty);
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
}
