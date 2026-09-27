package dev.zazr.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code patch} on the four sequences, against a model computed in {@code long}, where {@code from + replaced} cannot overflow. */
public class PatchTest {

    private static int[] indices(int n) {
        return new int[] {
            Integer.MIN_VALUE,
            Integer.MIN_VALUE + 1,
            -1,
            0,
            1,
            n - 1,
            n,
            n + 1,
            Integer.MAX_VALUE - 1,
            Integer.MAX_VALUE
        };
    }

    private static java.util.List<Integer> model(
            java.util.List<Integer> elements, int from, java.util.List<Integer> that, int replaced) {
        long start = Math.min(Math.max(from, 0), elements.size());
        long end = Math.min(Math.max(from, 0) + (long) Math.max(replaced, 0), elements.size());
        java.util.List<Integer> result = new java.util.ArrayList<>(elements.subList(0, (int) start));
        result.addAll(that);
        result.addAll(elements.subList((int) Math.max(start, end), elements.size()));
        return result;
    }

    @Test
    public void shouldPatchAsTheModelOnEverySequenceAndBound() {
        java.util.List<java.util.List<Integer>> replacements =
                java.util.List.of(java.util.List.of(), java.util.List.of(-1), java.util.List.of(-1, -2, -3));
        for (int n : new int[] {0, 1, 5}) {
            java.util.List<Integer> elements = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                elements.add(i);
            }
            for (int from : indices(n)) {
                for (int replaced : indices(n)) {
                    for (java.util.List<Integer> that : replacements) {
                        java.util.List<Integer> expected = model(elements, from, that, replaced);
                        String call = "patch(" + from + ", " + that + ", " + replaced + ") of " + n;
                        assertThat(Vector.ofAll(elements)
                                        .patch(from, that, replaced)
                                        .asJava())
                                .as("Vector." + call)
                                .isEqualTo(expected);
                        assertThat(List.ofAll(elements)
                                        .patch(from, that, replaced)
                                        .asJava())
                                .as("List." + call)
                                .isEqualTo(expected);
                        assertThat(Queue.ofAll(elements)
                                        .patch(from, that, replaced)
                                        .asJava())
                                .as("Queue." + call)
                                .isEqualTo(expected);
                        assertThat(Queue.ofAll(elements.subList(0, n / 2))
                                        .enqueueAll(elements.subList(n / 2, n))
                                        .patch(from, that, replaced)
                                        .asJava())
                                .as("Queue with a rear." + call)
                                .isEqualTo(expected);
                        assertThat(LazyList.ofAll(elements)
                                        .patch(from, that, replaced)
                                        .asJava())
                                .as("LazyList." + call)
                                .isEqualTo(expected);
                        assertThat(Vector.range(0, n)
                                        .patch(from, that, replaced)
                                        .asJava())
                                .as("primitive Vector." + call)
                                .isEqualTo(expected);
                    }
                }
            }
        }
    }

    @Test
    public void shouldNotRepeatTheTailWhenFromPlusReplacedOverflows() {
        java.util.List<Integer> expected = java.util.List.of(1, 2, 9);
        assertThat(Vector.of(1, 2)
                        .patch(Integer.MAX_VALUE, List.of(9), Integer.MAX_VALUE)
                        .asJava())
                .isEqualTo(expected);
        assertThat(List.of(1, 2)
                        .patch(Integer.MAX_VALUE, List.of(9), Integer.MAX_VALUE)
                        .asJava())
                .isEqualTo(expected);
        assertThat(Queue.of(1, 2)
                        .patch(Integer.MAX_VALUE, List.of(9), Integer.MAX_VALUE)
                        .asJava())
                .isEqualTo(expected);
        assertThat(LazyList.of(1, 2)
                        .patch(Integer.MAX_VALUE, List.of(9), Integer.MAX_VALUE)
                        .asJava())
                .isEqualTo(expected);
        assertThat(Vector.of(1, 2).patch(1, List.of(9), Integer.MAX_VALUE).asJava())
                .isEqualTo(java.util.List.of(1, 9));
    }

    @Test
    public void shouldPatchANonEmptyVectorAsVectorAndTheModelOnEveryBound() {
        java.util.List<java.util.List<Integer>> replacements =
                java.util.List.of(java.util.List.of(), java.util.List.of(-1), java.util.List.of(-1, -2, -3));
        for (int n : new int[] {1, 5, 33}) {
            Vector<Integer> vector = Vector.range(0, n);
            NonEmptyVector<Integer> nev = NonEmptyVector.fromIterable(vector).get();
            java.util.List<Integer> elements = new java.util.ArrayList<>(vector.asJava());
            for (int from : indices(n)) {
                for (int replaced : indices(n)) {
                    for (java.util.List<Integer> that : replacements) {
                        String call = "NonEmptyVector.patch(" + from + ", " + that + ", " + replaced + ") of " + n;
                        Vector<Integer> patched = nev.patch(from, that, replaced);
                        assertThat(patched).as(call).isEqualTo(vector.patch(from, that, replaced));
                        assertThat(patched.asJava()).as(call).isEqualTo(model(elements, from, that, replaced));
                    }
                }
            }
        }
        assertThat(NonEmptyVector.of(1, 2).patch(Integer.MAX_VALUE, List.of(9), Integer.MAX_VALUE))
                .isEqualTo(Vector.of(1, 2, 9));
        assertThat(NonEmptyVector.of(1, 2).patch(1, List.of(9), Integer.MAX_VALUE))
                .isEqualTo(Vector.of(1, 9));
    }
}
