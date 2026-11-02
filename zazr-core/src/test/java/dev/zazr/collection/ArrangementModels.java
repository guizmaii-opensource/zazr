package dev.zazr.collection;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// The recursive definitions of `combinations`, `permutations`, `rotateRight` and `HashSet.removeAll`, written with the
/// public API only: the models the faster implementations are compared with, element object by element object, and
/// on a LazyList cell read by cell read.
final class ArrangementModels {

    private ArrangementModels() {}

    // -- combinations: each element, followed by every combination of k - 1 elements after it

    static <T extends @Nullable Object> Vector<Vector<T>> combinations(Vector<T> elements, int k) {
        return (k == 0)
                ? Vector.of(Vector.empty())
                : elements.zipWithIndex()
                        .flatMap(t -> combinations(elements.drop(t._2() + 1), (k - 1))
                                .map((Vector<T> c) -> c.prepend(t._1())));
    }

    static <T extends @Nullable Object> List<List<T>> combinations(List<T> elements, int k) {
        return combine(elements, elements.size(), k);
    }

    // walks the tails of elements (of the given length): a tail shorter than k gives none, so the walk stops there
    private static <T extends @Nullable Object> List<List<T>> combine(List<T> elements, int length, int k) {
        if (k == 0) {
            return List.of(List.empty());
        }
        @SuppressWarnings("Var")
        List<List<T>> reversed = List.empty();
        @SuppressWarnings("Var")
        int remaining = length;
        for (List<T> rest = elements; remaining >= k; rest = rest.tail(), remaining--) {
            T head = rest.head();
            for (List<List<T>> tails = combine(rest.tail(), remaining - 1, k - 1);
                    !tails.isEmpty();
                    tails = tails.tail()) {
                reversed = reversed.prepend(tails.head().prepend(head));
            }
        }
        return reversed.reverse();
    }

    static <T extends @Nullable Object> LazyList<LazyList<T>> combinations(LazyList<T> elements, int k) {
        return (k == 0)
                ? LazyList.of(LazyList.empty())
                : elements.zipWithIndex()
                        .flatMap(t -> combinations(elements.drop(t._2() + 1), (k - 1))
                                .map((LazyList<T> c) -> c.prepend(t._1())));
    }

    // -- permutations: each distinct element, in order of first occurrence, followed by every permutation of the
    // others once its first occurrence is removed

    static <T extends @Nullable Object> Vector<Vector<T>> permutations(Vector<T> elements) {
        if (elements.isEmpty()) {
            return Vector.empty();
        } else if (elements.size() == 1) {
            return Vector.of(elements);
        } else {
            @SuppressWarnings("Var")
            Vector<Vector<T>> results = Vector.empty();
            for (T t : elements.distinct()) {
                for (Vector<T> ts : permutations(elements.remove(t))) {
                    results = results.append(Vector.of(t).appendAll(ts));
                }
            }
            return results;
        }
    }

    static <T extends @Nullable Object> List<List<T>> permutations(List<T> elements) {
        if (elements.isEmpty()) {
            return List.empty();
        } else if (elements.tail().isEmpty()) {
            return List.of(elements);
        } else {
            List<List<T>> zero = List.empty();
            return elements.distinct().foldLeft(zero, (xs, x) -> {
                Function<List<T>, List<T>> prepend = l -> l.prepend(x);
                return xs.appendAll(permutations(elements.remove(x)).map(prepend));
            });
        }
    }

    static <T extends @Nullable Object> LazyList<LazyList<T>> permutations(LazyList<T> elements) {
        return LazyList.defer(() -> {
            if (elements.isEmpty()) {
                return LazyList.empty();
            } else if (elements.tail().isEmpty()) {
                return LazyList.of(elements);
            } else {
                LazyList<LazyList<T>> zero = LazyList.empty();
                return elements.distinct().foldLeft(zero, (xs, x) -> {
                    Function<LazyList<T>, LazyList<T>> prepend = l -> l.prepend(x);
                    return xs.appendAll(permutations(elements.remove(x)).map(prepend));
                });
            }
        });
    }

    // -- rotateRight: the last k elements, then the others

    static <T extends @Nullable Object> List<T> rotateRight(List<T> elements, int n) {
        if (n == 0 || elements.isEmpty()) {
            return elements;
        }
        int k = Math.floorMod(n, elements.size());
        return (k == 0) ? elements : elements.takeRight(k).appendAll(elements.dropRight(k));
    }

    static <T extends @Nullable Object> Queue<T> rotateRight(Queue<T> elements, int n) {
        if (n == 0 || elements.isEmpty()) {
            return elements;
        }
        int k = Math.floorMod(n, elements.size());
        return (k == 0) ? elements : elements.takeRight(k).appendAll(elements.dropRight(k));
    }

    static <T extends @Nullable Object> LazyList<T> rotateRight(LazyList<T> elements, int n) {
        if (n == 0) {
            return elements;
        }
        return LazyList.defer(() -> {
            if (elements.isEmpty()) {
                return LazyList.empty();
            }
            int k = Math.floorMod(n, elements.size());
            return (k == 0) ? elements : elements.takeRight(k).appendAll(elements.dropRight(k));
        });
    }

    // -- HashSet.removeAll: the elements of this set that are not among the given ones, put in a set of their own

    static <T extends @Nullable Object> HashSet<T> removeAll(HashSet<T> set, Iterable<? extends T> elements) {
        java.util.Objects.requireNonNull(elements, "elements is null");
        if (set.isEmpty()) {
            return set;
        }
        HashSet<T> removed = HashSet.ofAll(elements);
        return removed.isEmpty() ? set : set.filter(e -> !removed.contains(e));
    }
}
