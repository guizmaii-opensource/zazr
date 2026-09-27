package dev.zazr;

import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckedPredicateTest {

    // -- of

    @Test
    public void shouldCreateCheckedPredicateUsingLambda() {
        CheckedPredicate<Object> predicate = CheckedPredicate.of(obj -> true);
        assertThat(predicate).isNotNull();
    }

    @Test
    public void shouldCreateCheckedPredicateUsingMethodReference() {
        CheckedPredicate<Object> predicate = CheckedPredicate.of(CheckedPredicateTest::test);
        assertThat(predicate).isNotNull();
    }

    private static boolean test(Object obj) {
        return true;
    }

    // -- negate

    @Test
    public void shouldNegate() throws Exception {
        CheckedPredicate<Integer> isPositive = i -> i > 0;
        CheckedPredicate<Integer> negated = isPositive.negate();
        assertThat(negated.test(1)).isFalse();
        assertThat(negated.test(-1)).isTrue();
    }

    @Nested
    class UncheckedTests {
        @Test
        public void shouldApplyAnUncheckedFunctionThatDoesNotThrow() {
            Predicate<Object> preciate = CheckedPredicate.of(obj -> true).unchecked();
            try {
                preciate.test(null);
            } catch (Throwable x) {
                Assertions.fail("Did not excepect an exception but received: " + x.getMessage());
            }
        }

        @Test
        public void shouldApplyAnUncheckedFunctionThatThrows() {
            Predicate<Object> preciate = CheckedPredicate.of(obj -> {
                        throw new Error();
                    })
                    .unchecked();
            try {
                preciate.test(null);
                Assertions.fail("Did excepect an exception.");
            } catch (Error x) {
                // ok!
            }
        }
    }
}
