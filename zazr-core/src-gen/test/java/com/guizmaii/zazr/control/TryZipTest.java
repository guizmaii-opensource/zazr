package com.guizmaii.zazr.control;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guizmaii.zazr.Tuple;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class TryZipTest {

    @Test
    public void shouldZip2Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2))).isEqualTo(Try.success(Tuple.of(1, 2)));
    }

    @Test
    public void shouldZipWith2Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), (a1, a2) -> {
            calls.incrementAndGet();
            return "" + a1 + a2;
        });
        assertThat(actual).isEqualTo(Try.success("12"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf2Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
    }

    @Test
    public void shouldReturnTheFirstFailureOf2InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(failing1, failing2)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf2Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), (_, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf2() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), (_, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith2() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), (a1, a2) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf2() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null)).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip3Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3))).isEqualTo(Try.success(Tuple.of(1, 2, 3)));
    }

    @Test
    public void shouldZipWith3Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), (a1, a2, a3) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3;
        });
        assertThat(actual).isEqualTo(Try.success("123"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf3Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2), Try.success(3))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), Try.success(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2, Try.success(3))).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, Try.success(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
    }

    @Test
    public void shouldReturnTheFirstFailureOf3InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(failing1, failing2, failing3)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2, failing3)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf3Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), (_, _, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf3() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), (_, _, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith3() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), (a1, a2, a3) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf3() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2), Try.success(3))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), Try.success(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null, Try.success(3))).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, Try.success(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), null)).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null, (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip4Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4))).isEqualTo(Try.success(Tuple.of(1, 2, 3, 4)));
    }

    @Test
    public void shouldZipWith4Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), (a1, a2, a3, a4) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4;
        });
        assertThat(actual).isEqualTo(Try.success("1234"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf4Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2), Try.success(3), Try.success(4))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), Try.success(3), Try.success(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2, Try.success(3), Try.success(4))).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, Try.success(3), Try.success(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, Try.success(4))).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, Try.success(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4)).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
    }

    @Test
    public void shouldReturnTheFirstFailureOf4InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        assertThat(Try.zip(failing1, failing2, failing3, failing4)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, failing3, failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2, failing3, failing4)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, failing3, failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, failing4)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4)).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf4Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), (_, _, _, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf4() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), (_, _, _, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith4() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), (a1, a2, a3, a4) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf4() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2), Try.success(3), Try.success(4))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), Try.success(3), Try.success(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null, Try.success(3), Try.success(4))).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, Try.success(3), Try.success(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), null, Try.success(4))).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null, Try.success(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), null)).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), null, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip5Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5))).isEqualTo(Try.success(Tuple.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void shouldZipWith5Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), (a1, a2, a3, a4, a5) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5;
        });
        assertThat(actual).isEqualTo(Try.success("12345"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf5Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5))).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5))).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5))).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5)).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
    }

    @Test
    public void shouldReturnTheFirstFailureOf5InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        assertThat(Try.zip(failing1, failing2, failing3, failing4, failing5)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, failing3, failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2, failing3, failing4, failing5)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, failing3, failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, failing4, failing5)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, failing5)).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5)).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf5Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), (_, _, _, _, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf5() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), (_, _, _, _, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith5() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), (a1, a2, a3, a4, a5) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf5() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5))).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5))).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5))).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null)).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip6Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6))).isEqualTo(Try.success(Tuple.of(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void shouldZipWith6Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), (a1, a2, a3, a4, a5, a6) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6;
        });
        assertThat(actual).isEqualTo(Try.success("123456"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf6Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), Try.success(6))).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), Try.success(6))).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), Try.success(6))).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, Try.success(6))).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        final Try<Integer> failing6 = Try.<Integer>failure(new IllegalStateException("e6"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6)).isSameAs(failing6);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
    }

    @Test
    public void shouldReturnTheFirstFailureOf6InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        final Try<Integer> failing6 = Try.<Integer>failure(new IllegalStateException("e6"));
        assertThat(Try.zip(failing1, failing2, failing3, failing4, failing5, failing6)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, failing3, failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2, failing3, failing4, failing5, failing6)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, failing3, failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, failing4, failing5, failing6)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, failing6)).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, failing6)).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6)).isSameAs(failing6);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf6Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf6() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith6() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), (a1, a2, a3, a4, a5, a6) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf6() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), Try.success(6))).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), Try.success(6))).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), Try.success(6))).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, Try.success(6))).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, Try.success(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null)).isInstanceOf(NullPointerException.class).hasMessage("t6 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t6 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip7Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isEqualTo(Try.success(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void shouldZipWith7Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (a1, a2, a3, a4, a5, a6, a7) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7;
        });
        assertThat(actual).isEqualTo(Try.success("1234567"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf7Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), Try.success(6), Try.success(7))).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, Try.success(6), Try.success(7))).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        final Try<Integer> failing6 = Try.<Integer>failure(new IllegalStateException("e6"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, Try.success(7))).isSameAs(failing6);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        final Try<Integer> failing7 = Try.<Integer>failure(new IllegalStateException("e7"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7)).isSameAs(failing7);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
    }

    @Test
    public void shouldReturnTheFirstFailureOf7InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        final Try<Integer> failing6 = Try.<Integer>failure(new IllegalStateException("e6"));
        final Try<Integer> failing7 = Try.<Integer>failure(new IllegalStateException("e7"));
        assertThat(Try.zip(failing1, failing2, failing3, failing4, failing5, failing6, failing7)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, failing3, failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2, failing3, failing4, failing5, failing6, failing7)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, failing3, failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, failing4, failing5, failing6, failing7)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, failing6, failing7)).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, failing6, failing7)).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, failing7)).isSameAs(failing6);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7)).isSameAs(failing7);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf7Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf7() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith7() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (a1, a2, a3, a4, a5, a6, a7) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf7() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), Try.success(6), Try.success(7))).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), Try.success(6), Try.success(7))).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, Try.success(6), Try.success(7))).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, Try.success(6), Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null, Try.success(7))).isInstanceOf(NullPointerException.class).hasMessage("t6 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null, Try.success(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t6 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), null)).isInstanceOf(NullPointerException.class).hasMessage("t7 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), null, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t7 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip8Successes() {
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isEqualTo(Try.success(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    public void shouldZipWith8Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Try<String> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (a1, a2, a3, a4, a5, a6, a7, a8) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8;
        });
        assertThat(actual).isEqualTo(Try.success("12345678"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf8Fails() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        assertThat(Try.zip(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        assertThat(Try.zip(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, Try.success(6), Try.success(7), Try.success(8))).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        final Try<Integer> failing6 = Try.<Integer>failure(new IllegalStateException("e6"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, Try.success(7), Try.success(8))).isSameAs(failing6);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        final Try<Integer> failing7 = Try.<Integer>failure(new IllegalStateException("e7"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7, Try.success(8))).isSameAs(failing7);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7, Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
        final Try<Integer> failing8 = Try.<Integer>failure(new IllegalStateException("e8"));
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), failing8)).isSameAs(failing8);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing8);
    }

    @Test
    public void shouldReturnTheFirstFailureOf8InArgumentOrder() {
        final Try<Integer> failing1 = Try.<Integer>failure(new IllegalStateException("e1"));
        final Try<Integer> failing2 = Try.<Integer>failure(new IllegalStateException("e2"));
        final Try<Integer> failing3 = Try.<Integer>failure(new IllegalStateException("e3"));
        final Try<Integer> failing4 = Try.<Integer>failure(new IllegalStateException("e4"));
        final Try<Integer> failing5 = Try.<Integer>failure(new IllegalStateException("e5"));
        final Try<Integer> failing6 = Try.<Integer>failure(new IllegalStateException("e6"));
        final Try<Integer> failing7 = Try.<Integer>failure(new IllegalStateException("e7"));
        final Try<Integer> failing8 = Try.<Integer>failure(new IllegalStateException("e8"));
        assertThat(Try.zip(failing1, failing2, failing3, failing4, failing5, failing6, failing7, failing8)).isSameAs(failing1);
        assertThat(Try.zipWith(failing1, failing2, failing3, failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Try.zip(Try.success(1), failing2, failing3, failing4, failing5, failing6, failing7, failing8)).isSameAs(failing2);
        assertThat(Try.zipWith(Try.success(1), failing2, failing3, failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Try.zip(Try.success(1), Try.success(2), failing3, failing4, failing5, failing6, failing7, failing8)).isSameAs(failing3);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), failing3, failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, failing6, failing7, failing8)).isSameAs(failing4);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, failing6, failing7, failing8)).isSameAs(failing5);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, failing7, failing8)).isSameAs(failing6);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7, failing8)).isSameAs(failing7);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
        assertThat(Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), failing8)).isSameAs(failing8);
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing8);
    }

    @Test
    public void shouldCaptureWhatTheCombinerOf8Throws() {
        final RuntimeException boom = new IllegalStateException("boom");
        assertThat(Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> {
            throw boom;
        })).isEqualTo(Try.failure(boom));
    }

    @Test
    public void shouldRethrowAFatalCombinerErrorOf8() {
        final UnknownError fatal = new UnknownError("fatal");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> {
            throw fatal;
        })).isSameAs(fatal);
    }

    @Test
    public void shouldRejectANullResultOfZipWith8() {
        final Try<Object> actual = Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (a1, a2, a3, a4, a5, a6, a7, a8) -> null);
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf8() {
        assertThatThrownBy(() -> Try.zip(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zipWith(null, Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t1 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), null, Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t2 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), null, Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t3 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), Try.success(6), Try.success(7), Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), null, Try.success(5), Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t4 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, Try.success(6), Try.success(7), Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), null, Try.success(6), Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t5 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null, Try.success(7), Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t6 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), null, Try.success(7), Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t6 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), null, Try.success(8))).isInstanceOf(NullPointerException.class).hasMessage("t7 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), null, Try.success(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t7 is null");
        assertThatThrownBy(() -> Try.zip(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), null)).isInstanceOf(NullPointerException.class).hasMessage("t8 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), null, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("t8 is null");
        assertThatThrownBy(() -> Try.zipWith(Try.success(1), Try.success(2), Try.success(3), Try.success(4), Try.success(5), Try.success(6), Try.success(7), Try.success(8), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }
}