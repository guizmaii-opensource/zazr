package com.guizmaii.zazr;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class CheckedConsumerTest {

    // -- of

    @Test
    public void shouldCreateCheckedConsumerUsingLambda() {
        final CheckedConsumer<Object> consumer = CheckedConsumer.of(obj -> {});
        assertThat(consumer).isNotNull();
    }

    @Test
    public void shouldCreateCheckedConsumerUsingMethodReference() {
        final CheckedConsumer<Object> consumer = CheckedConsumer.of(CheckedConsumerTest::accept);
        assertThat(consumer).isNotNull();
    }

    private static void accept(Object obj) {
    }

    @Nested
    class AcceptTests {
        @Test
        public void shouldApplyNonThrowingCheckedConsumer() {
            final CheckedConsumer<?> f = t -> {};
            try {
                f.accept(null);
            } catch(Throwable x) {
                fail("should not have thrown", x);
            }
        }

        @Test
        public void shouldApplyThrowingCheckedConsumer() {
            final CheckedConsumer<?> f = t -> { throw new Error(); };
            try {
                f.accept(null);
                fail("should have thrown");
            } catch(Throwable x) {
                // ok
            }
        }
    }

    @Nested
    class AndthenTests {
        @Test
        public void shouldThrowWhenComposingCheckedConsumerUsingAndThenWithNullParameter() {
            final CheckedConsumer<?> f = t -> {};
            assertThatThrownBy(() -> f.andThen(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldComposeCheckedConsumerUsingAndThenWhenFirstOneSucceeds() {
            final AtomicBoolean result = new AtomicBoolean(false);
            final CheckedConsumer<?> f = t -> {};
            try {
                f.andThen(ignored -> result.set(true)).accept(null);
                assertThat(result.get()).isTrue();
            } catch(Throwable x) {
                fail("should not have thrown", x);
            }
        }

        @Test
        public void shouldComposeCheckedConsumerUsingAndThenWhenFirstOneFails() {
            final AtomicBoolean result = new AtomicBoolean(false);
            final CheckedConsumer<?> f = t -> { throw new Error(); };
            try {
                f.andThen(ignored -> result.set(true)).accept(null);
                fail("should have thrown");
            } catch(Throwable x) {
                assertThat(result.get()).isFalse();
            }
        }
    }

    @Nested
    class UncheckedTests {
        @Test
        public void shouldApplyAnUncheckedFunctionThatDoesNotThrow() {
            final Consumer<Object> consumer = CheckedConsumer.of(obj -> {}).unchecked();
            try {
                consumer.accept(null);
            } catch(Throwable x) {
                Assertions.fail("Did not excepect an exception but received: " + x.getMessage());
            }
        }

        @Test
        public void shouldApplyAnUncheckedFunctionThatThrows() {
            final Consumer<Object> consumer = CheckedConsumer.of(obj -> { throw new Error(); }).unchecked();
            try {
                consumer.accept(null);
                Assertions.fail("Did excepect an exception.");
            } catch(Error x) {
                // ok!
            }
        }
    }
}
