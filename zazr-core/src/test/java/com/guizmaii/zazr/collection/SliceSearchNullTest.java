package com.guizmaii.zazr.collection;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/** {@code indexOfSlice} and {@code lastIndexOfSlice} with a null in the slice throw on every sequence, as {@code Vector}'s do. */
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
    public void shouldRejectANullInTheSliceOfIndexOfSliceOnEverySequence() {
        // a null after the first mismatch, a null at the start, and a sequence too short for the slice
        for (java.util.List<Integer> slice : java.util.List.of(Arrays.asList(7, null), Arrays.asList(null, 1), Arrays.asList(0, 1, null))) {
            for (int size : new int[] { 0, 1, 5 }) {
                final Vector<Integer> vector = Vector.range(0, size);
                for (int from : new int[] { 0, 1, size + 1 }) {
                    assertThatNullPointerException().isThrownBy(() -> vector.indexOfSlice(slice, from)).withMessage("Vector: element is null");
                    assertThatNullPointerException().isThrownBy(() -> List.ofAll(vector).indexOfSlice(slice, from)).withMessage("List: element is null");
                    assertThatNullPointerException().isThrownBy(() -> Queue.ofAll(vector).indexOfSlice(slice, from)).withMessage("List: element is null");
                    assertThatNullPointerException().isThrownBy(() -> Stream.ofAll(vector).indexOfSlice(slice, from)).withMessage("Stream: element is null");
                }
                assertThatNullPointerException().isThrownBy(() -> vector.indexOfSlice(slice)).withMessage("Vector: element is null");
                assertThatNullPointerException().isThrownBy(() -> List.ofAll(vector).indexOfSlice(slice)).withMessage("List: element is null");
                assertThatNullPointerException().isThrownBy(() -> Queue.ofAll(vector).indexOfSlice(slice)).withMessage("List: element is null");
                assertThatNullPointerException().isThrownBy(() -> Stream.ofAll(vector).indexOfSlice(slice)).withMessage("Stream: element is null");
                assertThatNullPointerException().isThrownBy(() -> Stream.ofAll(vector).containsSlice(slice)).withMessage("Stream: element is null");
                assertThatNullPointerException().isThrownBy(() -> Stream.ofAll(vector).indexOfSliceOption(slice)).withMessage("Stream: element is null");
                if (size > 0) {
                    final NonEmptyVector<Integer> nev = NonEmptyVector.fromIterable(vector).get();
                    assertThatNullPointerException().isThrownBy(() -> nev.indexOfSlice(slice)).withMessage("Vector: element is null");
                }
            }
        }
    }
}
