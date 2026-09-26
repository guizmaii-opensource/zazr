package dev.zazr.collection;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * How far each sequence reads a slice holding a null: {@code lastIndexOfSlice} throws on every sequence, as
 * {@code Vector}'s does; {@code indexOfSlice} reads it as each type documents.
 */
public class SliceSearchNullTest {

    @Test
    public void shouldRejectANullInTheSliceOnEverySequence() {
        for (java.util.List<Integer> slice : java.util.List.of(Arrays.asList(1, null), Arrays.asList(null, 1), Arrays.asList(0, 1, null))) {
            for (int size : new int[] { 0, 1, 5 }) {
                final Vector<Integer> vector = Vector.range(0, size);
                assertThatNullPointerException().isThrownBy(() -> vector.lastIndexOfSlice(slice)).withMessage("Vector: element is null");
                assertThatNullPointerException().isThrownBy(() -> List.ofAll(vector).lastIndexOfSlice(slice)).withMessage("List: element is null");
                assertThatNullPointerException().isThrownBy(() -> Queue.ofAll(vector).lastIndexOfSlice(slice)).withMessage("List: element is null");
                assertThatNullPointerException().isThrownBy(() -> Stream.ofAll(vector).lastIndexOfSlice(slice)).withMessage("Stream: element is null");
                for (int end : new int[] { 0, 1, size, Integer.MAX_VALUE }) {
                    assertThatNullPointerException().isThrownBy(() -> vector.lastIndexOfSlice(slice, end));
                    assertThatNullPointerException().isThrownBy(() -> List.ofAll(vector).lastIndexOfSlice(slice, end));
                    assertThatNullPointerException().isThrownBy(() -> Queue.ofAll(vector).lastIndexOfSlice(slice, end));
                    assertThatNullPointerException().isThrownBy(() -> Stream.ofAll(vector).lastIndexOfSlice(slice, end));
                }
                if (size > 0) {
                    final NonEmptyVector<Integer> nev = NonEmptyVector.fromIterable(vector).get();
                    assertThatNullPointerException().isThrownBy(() -> nev.lastIndexOfSlice(slice)).withMessage("Vector: element is null");
                }
                // a negative end is answered -1 without reading the slice, on every sequence
                assertThat(vector.lastIndexOfSlice(slice, -1)).isEqualTo(-1);
                assertThat(List.ofAll(vector).lastIndexOfSlice(slice, -1)).isEqualTo(-1);
                assertThat(Queue.ofAll(vector).lastIndexOfSlice(slice, -1)).isEqualTo(-1);
                assertThat(Stream.ofAll(vector).lastIndexOfSlice(slice, -1)).isEqualTo(-1);
            }
        }
    }

    @Test
    public void shouldReadTheSliceOfIndexOfSliceOnlyAsFarAsEachSequenceDocuments() {
        final java.util.List<Integer> nullAfterMismatch = Arrays.asList(7, null);
        final java.util.List<Integer> nullFirst = Arrays.asList(null, 1);
        for (java.util.List<Integer> slice : java.util.List.of(nullAfterMismatch, nullFirst)) {
            // Vector and NonEmptyVector read the whole slice first
            assertThatNullPointerException().isThrownBy(() -> Vector.range(0, 5).indexOfSlice(slice)).withMessage("Vector: element is null");
            assertThatNullPointerException().isThrownBy(() -> Vector.empty().indexOfSlice(slice)).withMessage("Vector: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptyVector.of(0, 1).indexOfSlice(slice)).withMessage("Vector: element is null");
            // a non-empty List or Queue reads the whole slice first; an empty one answers without reading it
            for (int from : new int[] { 0, 1, 6 }) {
                assertThatNullPointerException().isThrownBy(() -> List.range(0, 5).indexOfSlice(slice, from)).withMessage("List: element is null");
                assertThatNullPointerException().isThrownBy(() -> Queue.ofAll(List.range(0, 5)).indexOfSlice(slice, from)).withMessage("List: element is null");
                assertThat(List.empty().indexOfSlice(slice, from)).isEqualTo(-1);
                assertThat(Queue.empty().indexOfSlice(slice, from)).isEqualTo(-1);
                assertThat(Stream.empty().indexOfSlice(slice, from)).isEqualTo(-1);
            }
        }
        // a Stream reads the slice only as far as the comparisons go: a null they reach throws, a null past them is
        // not seen
        assertThatNullPointerException().isThrownBy(() -> Stream.range(0, 5).indexOfSlice(Arrays.asList(null, 1))).withMessage("Stream: element is null");
        assertThatNullPointerException().isThrownBy(() -> Stream.range(0, 5).indexOfSlice(Arrays.asList(0, null))).withMessage("Stream: element is null");
        assertThatNullPointerException().isThrownBy(() -> Stream.range(0, 5).containsSlice(Arrays.asList(3, null))).withMessage("Stream: element is null");
        assertThatNullPointerException().isThrownBy(() -> Stream.range(0, 5).indexOfSliceOption(Arrays.asList(4, null))).withMessage("Stream: element is null");
        assertThat(Stream.range(0, 5).indexOfSlice(Arrays.asList(7, null))).isEqualTo(-1);
        assertThat(Stream.range(0, 5).containsSlice(Arrays.asList(7, null))).isFalse();
        assertThat(Stream.range(0, 5).indexOfSliceOption(Arrays.asList(7, null))).isEqualTo(dev.zazr.control.Option.none());
        // so an infinite slice is answered
        assertThat(Stream.of(0, 1).indexOfSlice(Stream.from(0))).isEqualTo(-1);
        assertThat(Stream.of(0, 1).containsSlice(Stream.from(0))).isFalse();
        assertThat(Stream.empty().indexOfSlice(Stream.from(0))).isEqualTo(-1);
        assertThat(List.empty().indexOfSlice(Stream.from(0))).isEqualTo(-1);
        assertThat(Queue.empty().indexOfSlice(Stream.from(0))).isEqualTo(-1);
    }
}
