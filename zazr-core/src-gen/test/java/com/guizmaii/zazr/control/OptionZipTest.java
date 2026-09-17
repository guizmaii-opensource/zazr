package com.guizmaii.zazr.control;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guizmaii.zazr.Tuple;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class OptionZipTest {

    @Test
    public void shouldZip2Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2))).isEqualTo(Option.some(Tuple.of(1, 2)));
    }

    @Test
    public void shouldZipWith2Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), (a1, a2) -> {
            calls.incrementAndGet();
            return "" + a1 + a2;
        });
        assertThat(actual).isEqualTo(Option.some("12"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf2Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), (_, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith2() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), (a1, a2) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf2() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null)).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip3Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3))).isEqualTo(Option.some(Tuple.of(1, 2, 3)));
    }

    @Test
    public void shouldZipWith3Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a1, a2, a3) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3;
        });
        assertThat(actual).isEqualTo(Option.some("123"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf3Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2), Option.some(3))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), Option.some(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2, Option.some(3))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, Option.some(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing3 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), failing3)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith3() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), (a1, a2, a3) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf3() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2), Option.some(3))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), Option.some(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null, Option.some(3))).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, Option.some(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), null)).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null, (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip4Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4))).isEqualTo(Option.some(Tuple.of(1, 2, 3, 4)));
    }

    @Test
    public void shouldZipWith4Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), (a1, a2, a3, a4) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4;
        });
        assertThat(actual).isEqualTo(Option.some("1234"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf4Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2), Option.some(3), Option.some(4))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), Option.some(3), Option.some(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2, Option.some(3), Option.some(4))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, Option.some(3), Option.some(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing3 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), failing3, Option.some(4))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), failing3, Option.some(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing4 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), failing4)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith4() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), (a1, a2, a3, a4) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf4() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2), Option.some(3), Option.some(4))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), Option.some(3), Option.some(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null, Option.some(3), Option.some(4))).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, Option.some(3), Option.some(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), null, Option.some(4))).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null, Option.some(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), null)).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), null, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip5Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5))).isEqualTo(Option.some(Tuple.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void shouldZipWith5Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), (a1, a2, a3, a4, a5) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5;
        });
        assertThat(actual).isEqualTo(Option.some("12345"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf5Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing3 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing4 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing5 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith5() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), (a1, a2, a3, a4, a5) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf5() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5))).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5))).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5))).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null)).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip6Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6))).isEqualTo(Option.some(Tuple.of(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void shouldZipWith6Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), (a1, a2, a3, a4, a5, a6) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6;
        });
        assertThat(actual).isEqualTo(Option.some("123456"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf6Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), Option.some(6))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing3 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), Option.some(6))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing4 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), Option.some(6))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing5 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, Option.some(6))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing6 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), failing6)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith6() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), (a1, a2, a3, a4, a5, a6) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf6() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), Option.some(6))).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), Option.some(6))).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), Option.some(6))).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, Option.some(6))).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, Option.some(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null)).isInstanceOf(NullPointerException.class).hasMessage("o6 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o6 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip7Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isEqualTo(Option.some(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void shouldZipWith7Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), (a1, a2, a3, a4, a5, a6, a7) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7;
        });
        assertThat(actual).isEqualTo(Option.some("1234567"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf7Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing3 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing4 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), Option.some(6), Option.some(7))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing5 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, Option.some(6), Option.some(7))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing6 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), failing6, Option.some(7))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), failing6, Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing7 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), failing7)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith7() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), (a1, a2, a3, a4, a5, a6, a7) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf7() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), Option.some(6), Option.some(7))).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), Option.some(6), Option.some(7))).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, Option.some(6), Option.some(7))).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, Option.some(6), Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null, Option.some(7))).isInstanceOf(NullPointerException.class).hasMessage("o6 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null, Option.some(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o6 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), null)).isInstanceOf(NullPointerException.class).hasMessage("o7 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), null, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o7 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip8Successes() {
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isEqualTo(Option.some(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    public void shouldZipWith8Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Option<String> actual = Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (a1, a2, a3, a4, a5, a6, a7, a8) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8;
        });
        assertThat(actual).isEqualTo(Option.some("12345678"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf8Fails() {
        final Option<Integer> failing1 = Option.<Integer>none();
        assertThat(Option.zip(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(failing1, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing2 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), failing2, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing3 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), failing3, Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing4 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), failing4, Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing5 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, Option.some(6), Option.some(7), Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), failing5, Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing6 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), failing6, Option.some(7), Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), failing6, Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing7 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), failing7, Option.some(8))).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), failing7, Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
        final Option<Integer> failing8 = Option.<Integer>none();
        assertThat(Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), failing8)).isEqualTo(Option.<Integer>none());
        assertThat(Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isEqualTo(Option.<Integer>none());
    }

    @Test
    public void shouldRejectANullResultOfZipWith8() {
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (a1, a2, a3, a4, a5, a6, a7, a8) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Option.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf8() {
        assertThatThrownBy(() -> Option.zip(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zipWith(null, Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o1 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), null, Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o2 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), null, Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o3 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), Option.some(6), Option.some(7), Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), null, Option.some(5), Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o4 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, Option.some(6), Option.some(7), Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), null, Option.some(6), Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o5 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null, Option.some(7), Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o6 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), null, Option.some(7), Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o6 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), null, Option.some(8))).isInstanceOf(NullPointerException.class).hasMessage("o7 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), null, Option.some(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o7 is null");
        assertThatThrownBy(() -> Option.zip(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), null)).isInstanceOf(NullPointerException.class).hasMessage("o8 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), null, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("o8 is null");
        assertThatThrownBy(() -> Option.zipWith(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), Option.some(6), Option.some(7), Option.some(8), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }
}