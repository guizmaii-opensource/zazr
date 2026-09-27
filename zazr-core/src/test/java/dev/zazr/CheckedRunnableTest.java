package dev.zazr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckedRunnableTest {

    // -- of

    @Test
    public void shouldCreateCheckedRunnableUsingLambda() {
        CheckedRunnable runnable = CheckedRunnable.of(() -> {});
        assertThat(runnable).isNotNull();
    }

    @Test
    public void shouldCreateCheckedRunnableUsingMethodReference() {
        CheckedRunnable runnable = CheckedRunnable.of(CheckedRunnableTest::run);
        assertThat(runnable).isNotNull();
    }

    private static void run() {}

    @Nested
    class UncheckedTests {
        @Test
        public void shouldApplyAnUncheckedFunctionThatDoesNotThrow() {
            Runnable runnable = CheckedRunnable.of(() -> {}).unchecked();
            try {
                runnable.run();
            } catch (Throwable x) {
                Assertions.fail("Did not expect an exception but received: " + x.getMessage());
            }
        }

        @Test
        public void shouldApplyAnUncheckedFunctionThatThrows() {
            Runnable runnable = CheckedRunnable.of(() -> {
                        throw new Error();
                    })
                    .unchecked();
            Assertions.assertThrows(Error.class, () -> runnable.run());
        }
    }
}
