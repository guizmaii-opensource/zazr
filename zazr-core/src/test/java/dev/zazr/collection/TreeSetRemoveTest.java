package dev.zazr.collection;

import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `TreeSet.remove`, and `NonEmptySortedSet.remove` through it, against a `java.util.TreeSet` holding the same
/// elements under the same comparator: natural (the default and an explicit one), reversed, and one that maps many
/// elements to one. Every result is a valid red-black tree, an absent element gives the set itself, and the receiver
/// never changes.
public class TreeSetRemoveTest {

    private static final long SEED = 20261006L;
    private static final int[] SIZES = {0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = Comparator.reverseOrder();
    private static final Comparator<Integer> MODULO_7 = Comparator.comparingInt(i -> Math.floorMod(i, 7));

    private record Fixture(TreeSet<Integer> set, java.util.TreeSet<Integer> model) {}

    private static Fixture fixture(TreeSet<Integer> empty, Comparator<Integer> order, int size, Random random) {
        java.util.TreeSet<Integer> model = new java.util.TreeSet<>(order);
        @SuppressWarnings("Var")
        TreeSet<Integer> set = empty;
        for (int i = 0; i < 4 * size && model.size() < size; i++) {
            int element = random.nextInt(4 * size + 1) - 2 * size;
            set = set.add(element);
            model.add(element);
        }
        return new Fixture(set, model);
    }

    private static List<Fixture> fixtures() {
        Random random = new Random(SEED);
        List<Fixture> fixtures = new ArrayList<>();
        for (int size : SIZES) {
            fixtures.add(fixture(TreeSet.empty(), NATURAL, size, random));
            fixtures.add(fixture(TreeSet.empty(NATURAL), NATURAL, size, random));
            fixtures.add(fixture(TreeSet.empty(REVERSED), REVERSED, size, random));
            fixtures.add(fixture(TreeSet.empty(MODULO_7), MODULO_7, size, random));
        }
        return fixtures;
    }

    private static <T> List<T> list(Iterable<T> set) {
        List<T> result = new ArrayList<>();
        set.forEach(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> RedBlackTree<T> tree(TreeSet<T> set) {
        try {
            java.lang.reflect.Field field = TreeSet.class.getDeclaredField("tree");
            field.setAccessible(true);
            return (RedBlackTree<T>) field.get(set);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    // `result` holds the model's elements, in its order, as a valid tree; an unchanged model means the receiver itself
    private static void assertRemoved(
            TreeSet<Integer> receiver, TreeSet<Integer> result, java.util.TreeSet<Integer> model, String context) {
        assertThat(list(result)).as(context).containsExactlyElementsOf(model);
        assertThat(result.comparator()).as(context).isSameAs(receiver.comparator());
        RedBlackTreeValidity.assertValid(tree(result));
        if (model.size() == receiver.size()) {
            assertThat(result).as(context).isSameAs(receiver);
        } else {
            assertThat(result).as(context).isNotSameAs(receiver);
        }
    }

    @Test
    void removingAnElementPresentOrAbsentMatchesTheModel() {
        for (Fixture fixture : fixtures()) {
            TreeSet<Integer> set = fixture.set();
            List<Integer> before = list(set);
            int size = set.size();
            int stride = size > 100 ? 17 : 1;
            for (int element = -2 * size - 2; element <= 2 * size + 2; element += stride) {
                java.util.TreeSet<Integer> model = new java.util.TreeSet<>(fixture.model());
                model.remove(element);
                assertRemoved(set, set.remove(element), model, size + " " + element);
            }
            assertThat(list(set)).isEqualTo(before);
        }
    }

    @Test
    void removingEveryElementInARandomOrderEndsEmptyAndKeepsTheOlderVersions() {
        Random random = new Random(SEED + 1);
        for (Fixture fixture : fixtures()) {
            List<Integer> elements = new ArrayList<>(fixture.model());
            Collections.shuffle(elements, random);
            java.util.TreeSet<Integer> model = new java.util.TreeSet<>(fixture.model());
            List<TreeSet<Integer>> versions = new ArrayList<>();
            @SuppressWarnings("Var")
            TreeSet<Integer> set = fixture.set();
            for (int element : elements) {
                versions.add(set);
                model.remove(element);
                TreeSet<Integer> removed = set.remove(element);
                assertRemoved(set, removed, model, "" + element);
                assertThat(removed.remove(element)).isSameAs(removed);
                set = removed;
            }
            assertThat(set.isEmpty()).isTrue();
            assertThat(set.comparator()).isSameAs(fixture.set().comparator());
            for (int i = 0; i < versions.size(); i++) {
                assertThat(versions.get(i).size()).isEqualTo(elements.size() - i);
                RedBlackTreeValidity.assertValid(tree(versions.get(i)));
            }
        }
    }

    @Test
    void anAbsentElementGivesTheSetItselfWhateverTheComparatorSays() {
        TreeSet<Integer> modulo = TreeSet.ofAll(MODULO_7, List.of(1, 2, 3));
        assertThat(modulo.remove(4)).isSameAs(modulo);
        assertThat(list(modulo.remove(8))).containsExactly(2, 3); // 8 is equal to 1 under the comparator
        TreeSet<Integer> empty = TreeSet.empty();
        assertThat(empty.remove(1)).isSameAs(empty);
        TreeSet<Integer> nullsFirst = TreeSet.ofAll(Comparator.nullsFirst(NATURAL), List.of(1, 2, 3));
        assertThat(nullsFirst.remove(null)).isSameAs(nullsFirst);
    }

    @Test
    void aNaturalOrderSetRejectsANullElementOnlyWhenNotEmpty() {
        TreeSet<Integer> empty = TreeSet.empty();
        assertThat(empty.remove(null)).isSameAs(empty);
        TreeSet<Integer> set = TreeSet.of(1, 2, 3);
        assertThatThrownBy(() -> set.remove(null)).isInstanceOf(NullPointerException.class);
        assertThat(list(set)).containsExactly(1, 2, 3);
    }

    @Test
    void theNonEmptySetRemovesThroughTheTreeSet() {
        NonEmptySortedSet<Integer> nonEmpty = NonEmptySortedSet.of(REVERSED, 1, 2, 3);
        TreeSet<Integer> absent = nonEmpty.remove(4);
        assertThat(list(absent)).containsExactly(3, 2, 1);
        assertThat(absent.remove(4)).isSameAs(absent);
        assertThat(list(nonEmpty.remove(2))).containsExactly(3, 1);
        assertThat(nonEmpty.remove(1).remove(2).remove(3).isEmpty()).isTrue();
    }
}
