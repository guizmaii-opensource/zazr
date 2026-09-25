package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The checker the non-empty guarantee tests rely on catches an empty wrapper of every type, wherever it is nested. The
 * API cannot build one, so they are made through the private constructors.
 */
public class NonEmptyChecksTest {

    @Test
    public void shouldCatchAnEmptyWrapperOfEveryTypeNestedAnywhere() {
        final java.util.List<Object> hollows = java.util.List.of(
                NonEmptyChecks.hollow(NonEmptyVector.class, Vector.empty()),
                NonEmptyChecks.hollow(NonEmptySet.class, HashSet.empty()),
                NonEmptyChecks.hollow(NonEmptySortedSet.class, TreeSet.<Integer> empty()),
                NonEmptyChecks.hollow(NonEmptyMap.class, HashMap.empty()),
                NonEmptyChecks.hollow(NonEmptySortedMap.class, TreeMap.<Integer, Integer> empty()));
        assertThat(hollows).hasSize(NonEmptyChecks.NON_EMPTY_TYPES.size());
        for (Object hollow : hollows) {
            for (Object nested : java.util.List.of(hollow, Tuple.of(1, hollow), Tuple.of(1, 2, Vector.of(hollow)), Option.some(hollow), HashMap.of(1, hollow),
                    NonEmptySet.of(hollow), NonEmptyMap.single(1, hollow))) {
                assertThatThrownBy(() -> NonEmptyChecks.assertEveryNonEmptyCollectionIsNonEmpty(nested, "hollow"))
                  .as(hollow.getClass().getSimpleName())
                  .isInstanceOf(AssertionError.class);
            }
        }
    }

    @Test
    public void shouldAcceptNonEmptyWrappers() {
        NonEmptyChecks.assertEveryNonEmptyCollectionIsNonEmpty(Tuple.of(NonEmptyVector.of(1), NonEmptySet.of(1), NonEmptySortedSet.of(1),
                NonEmptyMap.single(1, NonEmptySortedMap.single(1, 1))), "full");
    }
}
