package dev.zazr.collection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `mapConserve`: the receiver itself when every element maps to itself, the same elements as `map` otherwise, the
/// mapper called once per element, and on `List` the cells after the last change shared.
class MapConserveTest {

    // a result that is never the element itself: a value outside the Integer cache, boxed anew by each call
    private static Integer fresh(int value) {
        return Integer.valueOf(1000 + value);
    }

    private static UnaryOperator<Integer> changing(int element) {
        return x -> (x == element) ? fresh(x) : x;
    }

    @Nested
    class ListTests {

        @Test
        public void returnsTheListItselfWhenNothingChanges() {
            for (int n : new int[] {0, 1, 2, 33}) {
                List<Integer> list = List.range(0, n);
                assertThat(list.mapConserve(x -> x)).isSameAs(list);
            }
        }

        @Test
        public void sharesTheCellsAfterTheLastChange() {
            List<Integer> list = List.range(0, 6);
            List<Integer> first = list.mapConserve(changing(0));
            assertThat(first).containsExactly(1000, 1, 2, 3, 4, 5);
            assertThat(first.head()).isNotSameAs(list.head());
            assertThat(first.tail()).isSameAs(list.tail());

            List<Integer> two = list.mapConserve(x -> (x == 1 || x == 3) ? fresh(x * 10) : x);
            assertThat(two).containsExactly(0, 1010, 2, 1030, 4, 5);
            assertThat(two.drop(4)).isSameAs(list.drop(4));

            List<Integer> last = list.mapConserve(changing(5));
            assertThat(last).containsExactly(0, 1, 2, 3, 4, 1005);
            assertThat(last.get(0)).isSameAs(list.get(0));
            assertThat(list).containsExactly(0, 1, 2, 3, 4, 5);
        }

        @Test
        public void givesTheElementsOfMap() {
            List<Integer> list = List.range(0, 40);
            Function<Integer, Integer> every = x -> fresh(x + 1);
            assertThat(list.mapConserve(every)).isEqualTo(list.map(every));
            assertThat(list.mapConserve(changing(20))).isEqualTo(list.map(changing(20)));
        }

        @Test
        public void callsTheMapperOncePerElementInOrder() {
            java.util.List<Integer> seen = new ArrayList<>();
            List.of(1, 2, 3, 4).mapConserve(x -> {
                seen.add(x);
                return (x == 2) ? fresh(20) : x;
            });
            assertThat(seen).containsExactly(1, 2, 3, 4);
        }

        @Test
        public void rejectsNulls() {
            assertThatNullPointerException()
                    .isThrownBy(() -> List.of(1).mapConserve(null))
                    .withMessage("mapper is null");
            assertThatNullPointerException().isThrownBy(() -> List.of(1, 2).mapConserve(x -> null));
        }
    }

    @Nested
    class VectorTests {

        @Test
        public void returnsTheVectorItselfWhenNothingChanges() {
            for (int n : new int[] {0, 1, 32, 33, 1025, 32769}) {
                Vector<Integer> vector = Vector.range(0, n);
                assertThat(vector.mapConserve(x -> x)).isSameAs(vector);
            }
        }

        @Test
        public void givesTheElementsOfMap() {
            for (int n : new int[] {1, 33, 1025}) {
                Vector<Integer> vector = Vector.range(0, n);
                Vector<Integer> changed = vector.mapConserve(changing(n - 1));
                assertThat(changed).isNotSameAs(vector).isEqualTo(vector.map(changing(n - 1)));
                assertThat(changed.last()).isEqualTo(999 + n);
                assertThat(vector.last()).isEqualTo(n - 1);
            }
        }

        @Test
        public void rejectsNulls() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Vector.of(1).mapConserve(null))
                    .withMessage("mapper is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> Vector.of(1).mapConserve(x -> null))
                    .withMessage("Vector: element is null");
        }

        @Test
        public void nonEmptyVectorReturnsItselfWhenNothingChanges() {
            NonEmptyVector<Integer> nev = NonEmptyVector.of(1, 2, 3);
            assertThat(nev.mapConserve(x -> x)).isSameAs(nev);
            assertThat(nev.mapConserve(changing(2))).isNotSameAs(nev).containsExactly(1, 1002, 3);
            assertThatNullPointerException().isThrownBy(() -> nev.mapConserve(null));
        }
    }

    @Nested
    class QueueTests {

        // every element at the front, and most of them at the back
        private java.util.List<Queue<Integer>> queues() {
            Queue<Integer> none = Queue.empty();
            return java.util.List.of(
                    Queue.ofAll(List.range(0, 5)), List.range(0, 5).foldLeft(none, Queue::enqueue));
        }

        @Test
        public void returnsTheQueueItselfWhenNothingChanges() {
            assertThat(Queue.<Integer>empty().mapConserve(x -> x)).isSameAs(Queue.empty());
            for (Queue<Integer> queue : queues()) {
                assertThat(queue.mapConserve(x -> x)).isSameAs(queue);
            }
        }

        @Test
        public void givesTheElementsOfMap() {
            for (Queue<Integer> queue : queues()) {
                for (int changed = 0; changed < 5; changed++) {
                    Queue<Integer> mapped = queue.mapConserve(changing(changed));
                    assertThat(mapped).isNotSameAs(queue).isEqualTo(queue.map(changing(changed)));
                    assertThat(queue).containsExactly(0, 1, 2, 3, 4);
                }
            }
            assertThatNullPointerException().isThrownBy(() -> Queue.of(1).mapConserve(null));
        }
    }

    @Nested
    class SetTests {

        @Test
        public void hashSetReturnsItselfWhenNothingChanges() {
            for (int n : new int[] {0, 1, 2, 33, 1025}) {
                HashSet<Integer> set = HashSet.range(0, n);
                assertThat(set.mapConserve(x -> x)).isSameAs(set);
            }
        }

        @Test
        public void hashSetGivesTheElementsOfMap() {
            HashSet<Integer> set = HashSet.range(0, 100);
            for (int changed : new int[] {0, 50, 99}) {
                HashSet<Integer> mapped = set.mapConserve(changing(changed));
                assertThat(mapped).isNotSameAs(set).isEqualTo(set.map(changing(changed)));
                assertThat(mapped).contains(1000 + changed).hasSize(100);
            }
            // a changed element mapped to another element of the set: both results are kept, as by map
            HashSet<Integer> pair = HashSet.of(1001, 1002);
            Function<Integer, Integer> shift = x -> Integer.valueOf(x + 1);
            assertThat(pair.mapConserve(shift))
                    .isEqualTo(HashSet.of(1002, 1003))
                    .isEqualTo(pair.map(shift));
            // results that are equal become one
            assertThat(HashSet.of(1, 2, 3).mapConserve(x -> fresh(0))).isEqualTo(HashSet.of(1000));
            assertThat(set).hasSize(100);
            assertThatNullPointerException().isThrownBy(() -> pair.mapConserve(null));
            assertThatNullPointerException()
                    .isThrownBy(() -> pair.mapConserve(x -> null))
                    .withMessage("HashSet: element is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> HashSet.range(0, 10).mapConserve(x -> (x == 9) ? null : fresh(x)))
                    .withMessage("HashSet: element is null");
        }

        @Test
        public void linkedHashSetKeepsTheOrderOfMap() {
            LinkedHashSet<Integer> set = LinkedHashSet.of(5, 3, 1, 4, 2);
            assertThat(set.mapConserve(x -> x)).isSameAs(set);
            assertThat(LinkedHashSet.<Integer>empty().mapConserve(x -> x)).isSameAs(LinkedHashSet.empty());
            for (int changed : new int[] {5, 1, 2}) {
                LinkedHashSet<Integer> mapped = set.mapConserve(changing(changed));
                assertThat(mapped).isNotSameAs(set).containsExactlyElementsOf(set.map(changing(changed)));
            }
            Function<Integer, Integer> half = x -> fresh(x / 2);
            assertThat(set.mapConserve(half)).containsExactlyElementsOf(set.map(half));
            assertThat(set).containsExactly(5, 3, 1, 4, 2);
            assertThatNullPointerException().isThrownBy(() -> set.mapConserve(null));
        }

        @Test
        public void treeSetKeepsItsComparator() {
            TreeSet<Integer> set = TreeSet.of(Comparator.reverseOrder(), 1, 2, 3, 4);
            assertThat(set.mapConserve(x -> x)).isSameAs(set);
            TreeSet<Integer> none = TreeSet.empty();
            assertThat(none.mapConserve(x -> x)).isSameAs(none);
            for (int changed : new int[] {4, 2, 1}) {
                TreeSet<Integer> mapped = set.mapConserve(changing(changed));
                assertThat(mapped).isNotSameAs(set);
                assertThat(mapped.comparator()).isSameAs(set.comparator());
                assertThat(mapped).containsExactlyElementsOf(set.map(Comparator.reverseOrder(), changing(changed)));
            }
            assertThat(set.mapConserve(x -> fresh(x % 2))).containsExactly(1001, 1000);
            assertThat(set).containsExactly(4, 3, 2, 1);
            assertThatNullPointerException().isThrownBy(() -> set.mapConserve(null));
        }

        @Test
        public void nonEmptySetsReturnThemselvesWhenNothingChanges() {
            NonEmptySet<Integer> nes = NonEmptySet.of(1, 2, 3);
            assertThat(nes.mapConserve(x -> x)).isSameAs(nes);
            assertThat(nes.mapConserve(changing(2))).isNotSameAs(nes).isEqualTo(NonEmptySet.of(1, 1002, 3));
            NonEmptySortedSet<Integer> ness = NonEmptySortedSet.of(1, 2, 3);
            assertThat(ness.mapConserve(x -> x)).isSameAs(ness);
            assertThat(ness.mapConserve(changing(2))).isNotSameAs(ness).containsExactly(1, 3, 1002);
        }
    }
}
