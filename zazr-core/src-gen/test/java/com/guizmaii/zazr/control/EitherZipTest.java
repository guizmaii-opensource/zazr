package com.guizmaii.zazr.control;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guizmaii.zazr.Tuple;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class EitherZipTest {

    @Test
    public void shouldZip2Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2))).isEqualTo(Either.right(Tuple.of(1, 2)));
    }

    @Test
    public void shouldZipWith2Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), (a1, a2) -> {
            calls.incrementAndGet();
            return "" + a1 + a2;
        });
        assertThat(actual).isEqualTo(Either.right("12"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf2Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
    }

    @Test
    public void shouldReturnTheFirstFailureOf2InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(failing1, failing2)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, (_, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
    }

    @Test
    public void shouldRejectANullResultOfZipWith2() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), (a1, a2) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf2() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null)).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip3Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3))).isEqualTo(Either.right(Tuple.of(1, 2, 3)));
    }

    @Test
    public void shouldZipWith3Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), (a1, a2, a3) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3;
        });
        assertThat(actual).isEqualTo(Either.right("123"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf3Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3))).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
    }

    @Test
    public void shouldReturnTheFirstFailureOf3InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(failing1, failing2, failing3)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, failing3)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, (_, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
    }

    @Test
    public void shouldRejectANullResultOfZipWith3() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), (a1, a2, a3) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf3() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3))).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null)).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip4Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4))).isEqualTo(Either.right(Tuple.of(1, 2, 3, 4)));
    }

    @Test
    public void shouldZipWith4Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), (a1, a2, a3, a4) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4;
        });
        assertThat(actual).isEqualTo(Either.right("1234"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf4Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4))).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4))).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4)).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
    }

    @Test
    public void shouldReturnTheFirstFailureOf4InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        assertThat(Either.zip(failing1, failing2, failing3, failing4)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, failing3, failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, failing3, failing4)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, failing3, failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4)).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
    }

    @Test
    public void shouldRejectANullResultOfZipWith4() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), (a1, a2, a3, a4) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf4() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4))).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4))).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null)).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip5Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isEqualTo(Either.right(Tuple.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void shouldZipWith5Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), (a1, a2, a3, a4, a5) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5;
        });
        assertThat(actual).isEqualTo(Either.right("12345"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf5Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5))).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5)).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
    }

    @Test
    public void shouldReturnTheFirstFailureOf5InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        assertThat(Either.zip(failing1, failing2, failing3, failing4, failing5)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, failing3, failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5)).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5)).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
    }

    @Test
    public void shouldRejectANullResultOfZipWith5() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), (a1, a2, a3, a4, a5) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf5() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5))).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5))).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null)).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip6Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isEqualTo(Either.right(Tuple.of(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void shouldZipWith6Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (a1, a2, a3, a4, a5, a6) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6;
        });
        assertThat(actual).isEqualTo(Either.right("123456"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf6Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, Either.<String, Integer>right(6))).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        final Either<String, Integer> failing6 = Either.<String, Integer>left("e6");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6)).isSameAs(failing6);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
    }

    @Test
    public void shouldReturnTheFirstFailureOf6InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        final Either<String, Integer> failing6 = Either.<String, Integer>left("e6");
        assertThat(Either.zip(failing1, failing2, failing3, failing4, failing5, failing6)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, failing3, failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, failing6)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, failing6)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, failing6)).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, failing6)).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6)).isSameAs(failing6);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
    }

    @Test
    public void shouldRejectANullResultOfZipWith6() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (a1, a2, a3, a4, a5, a6) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf6() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), Either.<String, Integer>right(6))).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, Either.<String, Integer>right(6))).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, Either.<String, Integer>right(6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null)).isInstanceOf(NullPointerException.class).hasMessage("e6 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e6 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip7Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isEqualTo(Either.right(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void shouldZipWith7Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (a1, a2, a3, a4, a5, a6, a7) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7;
        });
        assertThat(actual).isEqualTo(Either.right("1234567"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf7Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        final Either<String, Integer> failing6 = Either.<String, Integer>left("e6");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, Either.<String, Integer>right(7))).isSameAs(failing6);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        final Either<String, Integer> failing7 = Either.<String, Integer>left("e7");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7)).isSameAs(failing7);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
    }

    @Test
    public void shouldReturnTheFirstFailureOf7InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        final Either<String, Integer> failing6 = Either.<String, Integer>left("e6");
        final Either<String, Integer> failing7 = Either.<String, Integer>left("e7");
        assertThat(Either.zip(failing1, failing2, failing3, failing4, failing5, failing6, failing7)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, failing3, failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, failing6, failing7)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, failing6, failing7)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, failing6, failing7)).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, failing6, failing7)).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, failing7)).isSameAs(failing6);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7)).isSameAs(failing7);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
    }

    @Test
    public void shouldRejectANullResultOfZipWith7() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (a1, a2, a3, a4, a5, a6, a7) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf7() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, Either.<String, Integer>right(6), Either.<String, Integer>right(7))).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, Either.<String, Integer>right(6), Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null, Either.<String, Integer>right(7))).isInstanceOf(NullPointerException.class).hasMessage("e6 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null, Either.<String, Integer>right(7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e6 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), null)).isInstanceOf(NullPointerException.class).hasMessage("e7 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), null, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e7 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldZip8Successes() {
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isEqualTo(Either.right(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    public void shouldZipWith8Successes() {
        final AtomicInteger calls = new AtomicInteger();
        final Either<String, String> actual = Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (a1, a2, a3, a4, a5, a6, a7, a8) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8;
        });
        assertThat(actual).isEqualTo(Either.right("12345678"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenOneOf8Fails() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        assertThat(Either.zip(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        final Either<String, Integer> failing6 = Either.<String, Integer>left("e6");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isSameAs(failing6);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        final Either<String, Integer> failing7 = Either.<String, Integer>left("e7");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7, Either.<String, Integer>right(8))).isSameAs(failing7);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7, Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
        final Either<String, Integer> failing8 = Either.<String, Integer>left("e8");
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), failing8)).isSameAs(failing8);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing8);
    }

    @Test
    public void shouldReturnTheFirstFailureOf8InArgumentOrder() {
        final Either<String, Integer> failing1 = Either.<String, Integer>left("e1");
        final Either<String, Integer> failing2 = Either.<String, Integer>left("e2");
        final Either<String, Integer> failing3 = Either.<String, Integer>left("e3");
        final Either<String, Integer> failing4 = Either.<String, Integer>left("e4");
        final Either<String, Integer> failing5 = Either.<String, Integer>left("e5");
        final Either<String, Integer> failing6 = Either.<String, Integer>left("e6");
        final Either<String, Integer> failing7 = Either.<String, Integer>left("e7");
        final Either<String, Integer> failing8 = Either.<String, Integer>left("e8");
        assertThat(Either.zip(failing1, failing2, failing3, failing4, failing5, failing6, failing7, failing8)).isSameAs(failing1);
        assertThat(Either.zipWith(failing1, failing2, failing3, failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing1);
        assertThat(Either.zip(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, failing6, failing7, failing8)).isSameAs(failing2);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), failing2, failing3, failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing2);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, failing6, failing7, failing8)).isSameAs(failing3);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), failing3, failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing3);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, failing6, failing7, failing8)).isSameAs(failing4);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), failing4, failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing4);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, failing6, failing7, failing8)).isSameAs(failing5);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), failing5, failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing5);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, failing7, failing8)).isSameAs(failing6);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), failing6, failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing6);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7, failing8)).isSameAs(failing7);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), failing7, failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing7);
        assertThat(Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), failing8)).isSameAs(failing8);
        assertThat(Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), failing8, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isSameAs(failing8);
    }

    @Test
    public void shouldRejectANullResultOfZipWith8() {
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (a1, a2, a3, a4, a5, a6, a7, a8) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Either.zipWith: f returned null");
    }

    @Test
    public void shouldRejectANullArgumentOf8() {
        assertThatThrownBy(() -> Either.zip(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zipWith(null, Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e1 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), null, Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e2 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), null, Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e3 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), null, Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e4 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), null, Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e5 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null, Either.<String, Integer>right(7), Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e6 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), null, Either.<String, Integer>right(7), Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e6 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), null, Either.<String, Integer>right(8))).isInstanceOf(NullPointerException.class).hasMessage("e7 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), null, Either.<String, Integer>right(8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e7 is null");
        assertThatThrownBy(() -> Either.zip(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), null)).isInstanceOf(NullPointerException.class).hasMessage("e8 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), null, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("e8 is null");
        assertThatThrownBy(() -> Either.zipWith(Either.<String, Integer>right(1), Either.<String, Integer>right(2), Either.<String, Integer>right(3), Either.<String, Integer>right(4), Either.<String, Integer>right(5), Either.<String, Integer>right(6), Either.<String, Integer>right(7), Either.<String, Integer>right(8), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }
}