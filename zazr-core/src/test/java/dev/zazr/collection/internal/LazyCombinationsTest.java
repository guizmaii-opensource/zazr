package dev.zazr.collection.internal;

import dev.zazr.collection.LazyList;
import dev.zazr.collection.Vector;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// The state of the iterator behind `LazyList.combinations(k)`, which the results alone do not show: the positions
/// of its cells, that let the length found at the end skip in O(1) the cells that cannot move, and the moves made
/// again after a `VirtualMachineError`.
class LazyCombinationsTest {

    @Test
    void thePositionsAreThoseOfTheCellsAndTheLengthIsTheList() {
        for (int n = 1; n <= 20; n++) {
            for (int k : new int[] {1, n / 2 + 1, n - 1, n}) {
                if (k < 1) {
                    continue;
                }
                LazyListModule.CombinationIterator<Integer> iterator =
                        new LazyListModule.CombinationIterator<>(LazyList.range(0, n), k);
                @SuppressWarnings("Var")
                int count = 0;
                while (iterator.hasNext()) {
                    LazyList<Integer> combination = iterator.next();
                    for (int j = 0; j < k; j++) {
                        // the elements of the range are their positions
                        assertThat(iterator.position(j))
                                .as("n = %d, k = %d", n, k)
                                .isEqualTo(combination.get(j));
                    }
                    count++;
                }
                assertThat(count).isEqualTo(Vector.range(0, n).combinations(k).size());
                assertThat(iterator.knownLength()).as("n = %d, k = %d", n, k).isEqualTo(n);
            }
        }
    }

    // 0, 1, ..., n - 1, the evaluation of cell `failing` throwing a StackOverflowError the first time
    private static LazyList<Integer> failingOnce(int n, int failing, boolean[] failed) {
        return cellsFrom(0, n, failing, failed);
    }

    private static LazyList<Integer> cellsFrom(int i, int n, int failing, boolean[] failed) {
        if (i == n) {
            return LazyList.empty();
        }
        return LazyList.cons(i, () -> {
            if (i + 1 == failing && !failed[0]) {
                failed[0] = true;
                throw new StackOverflowError("simulated");
            }
            return cellsFrom(i + 1, n, failing, failed);
        });
    }

    // every combination, each element read, a VirtualMachineError met on the way followed by the same read again
    @SuppressWarnings("Var")
    private static java.util.List<java.util.List<Integer>> readRetrying(LazyList<LazyList<Integer>> result) {
        java.util.List<java.util.List<Integer>> read = new ArrayList<>();
        LazyList<LazyList<Integer>> cell = result;
        int errors = 0;
        while (true) {
            try {
                if (cell.isEmpty()) {
                    return read;
                }
                read.add(java.util.List.copyOf(cell.head().asJava()));
                cell = cell.tail();
            } catch (StackOverflowError e) {
                errors++;
                assertThat(errors).as("one error, then the same read succeeds").isEqualTo(1);
            }
        }
    }

    @Test
    void aVirtualMachineErrorIsMetThenTheSameMoveIsMadeAgain() {
        for (int n = 1; n <= 6; n++) {
            for (int failing = 1; failing <= n; failing++) {
                for (int k = 1; k <= n + 1; k++) {
                    boolean[] failed = {false};
                    java.util.List<java.util.List<Integer>> expected = new ArrayList<>();
                    LazyList.range(0, n).combinations(k).forEach(c -> expected.add(java.util.List.copyOf(c.asJava())));
                    assertThat(readRetrying(failingOnce(n, failing, failed).combinations(k)))
                            .as("n = %d, failing = %d, k = %d", n, failing, k)
                            .isEqualTo(expected);
                }
            }
        }
    }
}
