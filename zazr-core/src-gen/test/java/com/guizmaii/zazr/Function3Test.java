package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import java.lang.CharSequence;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class Function3Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2, Object o3) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(Function3.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        final Function3<Integer, Integer, Integer, Option<Integer>> lifted = Function3.lift((i1, i2, i3) -> {
            if (i1 == 0) {
                return null;
            }
            if (i1 == 1) {
                throw new IllegalStateException("non-fatal");
            }
            if (i1 == 2) {
                throw new OutOfMemoryError("fatal");
            }
            return i1 + i2 + i3;
        });
        assertThat(lifted.apply(3, 1, 1)).isEqualTo(Option.some(5));
        assertThat(lifted.apply(0, 1, 1)).isEqualTo(Option.none());
        assertThat(lifted.apply(1, 1, 1)).isEqualTo(Option.none());
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(2, 1, 1));
    }

    @Test
    public void shouldRethrowFatalThrowableFromLiftTry() {
        final Function3<Integer, Integer, Integer, Try<Integer>> lifted =
            Function3.liftTry((i1, i2, i3) -> { throw new OutOfMemoryError("fatal"); });
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(1, 1, 1));
    }

    @Test
    public void shouldReturnFailureFromLiftTryOnNonFatalThrowable() {
        final Function3<Integer, Integer, Integer, Try<Integer>> lifted =
            Function3.liftTry((i1, i2, i3) -> { throw new IllegalStateException("non-fatal"); });
        final Try<Integer> result = lifted.apply(1, 1, 1);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("non-fatal");
    }

    @Test
    public void shouldPartiallyApply() {
        final Function3<Object, Object, Object, Object> f = (o1, o2, o3) -> "" + o1 + o2 + o3;
        assertThat(f.apply(1).apply(2, 3)).isEqualTo("123");
        assertThat(f.apply(1, 2).apply(3)).isEqualTo("123");
    }

    @Test
    public void shouldConstant() {
        final Function3<Object, Object, Object, Object> f = Function3.constant(6);
        assertThat(f.apply(1, 2, 3)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() {
        final Function3<Object, Object, Object, Object> f = (o1, o2, o3) -> "" + o1 + o2 + o3;
        final Function<Object, Function<Object, Function<Object, Object>>> curried = f.curried();
        assertThat(curried.apply(1).apply(2).apply(3)).isEqualTo("123");
    }

    @Test
    public void shouldTuple() {
        final Function3<Object, Object, Object, Object> f = (o1, o2, o3) -> "" + o1 + o2 + o3;
        final Function<Tuple3<Object, Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled.apply(Tuple.of(1, 2, 3))).isEqualTo("123");
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        AtomicInteger integer = new AtomicInteger();
        Function3<Integer, Integer, Integer, Integer> divByZero = (i1, i2, i3) -> 10 / integer.get();
        Function3<Integer, Integer, Integer, Try<Integer>> divByZeroTry = Function3.liftTry(divByZero);

        Try<Integer> res = divByZeroTry.apply(0, 0, 0);
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getCause()).isNotNull();
        assertThat(res.getCause().getMessage()).isEqualToIgnoringCase("/ by zero");

        integer.incrementAndGet();
        res = divByZeroTry.apply(1, 2, 3);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.get()).isEqualTo(10);
    }

    private static final Function3<Integer, Integer, Integer, Integer> recurrent1 = (i1, i2, i3) -> i1 <= 0 ? i1 : Function3Test.recurrent1.apply(i1 - 1, i2, i3) + 1;

    @Test
    public void shouldCalculatedRecursively() {
        assertThat(recurrent1.apply(11, 11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final Function3<Object, Object, Object, Object> f = (o1, o2, o3) -> "" + o1 + o2 + o3;
        final Function<Object, Object> after = o -> o + "!";
        final Function3<Object, Object, Object, Object> composed = f.andThen(after);
        assertThat(composed.apply(1, 2, 3)).isEqualTo("123!");
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1() {
          final Function3<String, String, String, String> concat = (String s1, String s2, String s3) -> s1 + s2 + s3;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2", "s3")).isEqualTo("XXs2s3");
      }

      @Test
      public void shouldCompose2() {
          final Function3<String, String, String, String> concat = (String s1, String s2, String s3) -> s1 + s2 + s3;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx", "s3")).isEqualTo("s1XXs3");
      }

      @Test
      public void shouldCompose3() {
          final Function3<String, String, String, String> concat = (String s1, String s2, String s3) -> s1 + s2 + s3;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose3(toUpperCase).apply("s1", "s2", "xx")).isEqualTo("s1s2XX");
      }

    }

    @Test
    public void shouldNarrow(){
        final Function3<Number, Number, Number, String> wideFunction = (o1, o2, o3) -> String.format("Numbers are: %s, %s, %s", o1, o2, o3);
        final Function3<Integer, Integer, Integer, CharSequence> narrowFunction = Function3.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2, 3)).isEqualTo("Numbers are: 1, 2, 3");
    }
}