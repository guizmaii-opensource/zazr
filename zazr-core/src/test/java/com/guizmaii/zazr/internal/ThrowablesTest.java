package com.guizmaii.zazr.internal;

import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ThrowablesTest {

    @Nested
    class SneakyThrowTests {
        @Test
        public void shouldRethrowACheckedExceptionUndeclared() {
            final IOException error = new IOException("checked");
            assertThatThrownBy(() -> Throwables.sneakyThrow(error)).isSameAs(error);
        }

        @Test
        public void shouldRethrowAnError() {
            final StackOverflowError error = new StackOverflowError();
            assertThatThrownBy(() -> Throwables.sneakyThrow(error)).isSameAs(error);
        }
    }

    @Nested
    class IsFatalTests {
        @Test
        public void shouldClassifyInterruptedExceptionAsFatal() {
            assertThat(Throwables.isFatal(new InterruptedException())).isTrue();
        }

        @Test
        public void shouldClassifyLinkageErrorAsFatal() {
            assertThat(Throwables.isFatal(new LinkageError())).isTrue();
            assertThat(Throwables.isFatal(new NoClassDefFoundError())).isTrue();
        }

        @SuppressWarnings("removal")
        @Test
        public void shouldClassifyThreadDeathAsFatal() {
            assertThat(Throwables.isFatal(new ThreadDeath())).isTrue();
        }

        @Test
        public void shouldClassifyVirtualMachineErrorAsFatal() {
            assertThat(Throwables.isFatal(new OutOfMemoryError())).isTrue();
            assertThat(Throwables.isFatal(new StackOverflowError())).isTrue();
        }

        @Test
        public void shouldNotClassifyOrdinaryThrowablesAsFatal() {
            assertThat(Throwables.isFatal(new RuntimeException())).isFalse();
            assertThat(Throwables.isFatal(new IOException())).isFalse();
            assertThat(Throwables.isFatal(new AssertionError())).isFalse();
            assertThat(Throwables.isFatal(new Throwable())).isFalse();
        }
    }
}
