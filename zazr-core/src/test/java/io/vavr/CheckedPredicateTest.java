package io.vavr;

import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckedPredicateTest {

    // -- of

    @Test
    public void shouldCreateCheckedPredicateUsingLambda() {
        final CheckedPredicate<Object> predicate = CheckedPredicate.of(obj -> true);
        assertThat(predicate).isNotNull();
    }

    @Test
    public void shouldCreateCheckedPredicateUsingMethodReference() {
        final CheckedPredicate<Object> predicate = CheckedPredicate.of(CheckedPredicateTest::test);
        assertThat(predicate).isNotNull();
    }

    private static boolean test(Object obj) {
        return true;
    }

    @Nested
    class UncheckedTests {
        @Test
        public void shouldApplyAnUncheckedFunctionThatDoesNotThrow() {
            final Predicate<Object> preciate = CheckedPredicate.of(obj -> true).unchecked();
            try {
                preciate.test(null);
            } catch(Throwable x) {
                Assertions.fail("Did not excepect an exception but received: " + x.getMessage());
            }
        }

        @Test
        public void shouldApplyAnUncheckedFunctionThatThrows() {
            final Predicate<Object> preciate = CheckedPredicate.of(obj -> { throw new Error(); }).unchecked();
            try {
                preciate.test(null);
                Assertions.fail("Did excepect an exception.");
            } catch(Error x) {
                // ok!
            }
        }
    }
}
