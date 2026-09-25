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

public class Function4Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2, Object o3, Object o4) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(Function4.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        final Function4<Integer, Integer, Integer, Integer, Option<Integer>> lifted = Function4.lift((i1, i2, i3, i4) -> {
            if (i1 == 0) {
                return null;
            }
            if (i1 == 1) {
                throw new IllegalStateException("non-fatal");
            }
            if (i1 == 2) {
                throw new OutOfMemoryError("fatal");
            }
            return i1 + i2 + i3 + i4;
        });
        assertThat(lifted.apply(3, 1, 1, 1)).isEqualTo(Option.some(6));
        assertThat(lifted.apply(0, 1, 1, 1)).isEqualTo(Option.none());
        assertThat(lifted.apply(1, 1, 1, 1)).isEqualTo(Option.none());
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(2, 1, 1, 1));
    }

    @Test
    public void shouldRethrowFatalThrowableFromLiftTry() {
        final Function4<Integer, Integer, Integer, Integer, Try<Integer>> lifted =
            Function4.liftTry((i1, i2, i3, i4) -> { throw new OutOfMemoryError("fatal"); });
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(1, 1, 1, 1));
    }

    @Test
    public void shouldReturnFailureFromLiftTryOnNonFatalThrowable() {
        final Function4<Integer, Integer, Integer, Integer, Try<Integer>> lifted =
            Function4.liftTry((i1, i2, i3, i4) -> { throw new IllegalStateException("non-fatal"); });
        final Try<Integer> result = lifted.apply(1, 1, 1, 1);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("non-fatal");
    }

    @Test
    public void shouldPartiallyApply() {
        final Function4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        assertThat(f.apply(1).apply(2, 3, 4)).isEqualTo("1234");
        assertThat(f.apply(1, 2).apply(3, 4)).isEqualTo("1234");
        assertThat(f.apply(1, 2, 3).apply(4)).isEqualTo("1234");
    }

    @Test
    public void shouldConstant() {
        final Function4<Object, Object, Object, Object, Object> f = Function4.constant(6);
        assertThat(f.apply(1, 2, 3, 4)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() {
        final Function4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        final Function<Object, Function<Object, Function<Object, Function<Object, Object>>>> curried = f.curried();
        assertThat(curried.apply(1).apply(2).apply(3).apply(4)).isEqualTo("1234");
    }

    @Test
    public void shouldTuple() {
        final Function4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        final Function<Tuple4<Object, Object, Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled.apply(Tuple.of(1, 2, 3, 4))).isEqualTo("1234");
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        AtomicInteger integer = new AtomicInteger();
        Function4<Integer, Integer, Integer, Integer, Integer> divByZero = (i1, i2, i3, i4) -> 10 / integer.get();
        Function4<Integer, Integer, Integer, Integer, Try<Integer>> divByZeroTry = Function4.liftTry(divByZero);

        Try<Integer> res = divByZeroTry.apply(0, 0, 0, 0);
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getCause()).isNotNull();
        assertThat(res.getCause().getMessage()).isEqualToIgnoringCase("/ by zero");

        integer.incrementAndGet();
        res = divByZeroTry.apply(1, 2, 3, 4);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.get()).isEqualTo(10);
    }

    private static final Function4<Integer, Integer, Integer, Integer, Integer> recurrent1 = (i1, i2, i3, i4) -> i1 <= 0 ? i1 : Function4Test.recurrent1.apply(i1 - 1, i2, i3, i4) + 1;

    @Test
    public void shouldCalculatedRecursively() {
        assertThat(recurrent1.apply(11, 11, 11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22, 22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final Function4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        final Function<Object, Object> after = o -> o + "!";
        final Function4<Object, Object, Object, Object, Object> composed = f.andThen(after);
        assertThat(composed.apply(1, 2, 3, 4)).isEqualTo("1234!");
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1() {
          final Function4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2", "s3", "s4")).isEqualTo("XXs2s3s4");
      }

      @Test
      public void shouldCompose2() {
          final Function4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx", "s3", "s4")).isEqualTo("s1XXs3s4");
      }

      @Test
      public void shouldCompose3() {
          final Function4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose3(toUpperCase).apply("s1", "s2", "xx", "s4")).isEqualTo("s1s2XXs4");
      }

      @Test
      public void shouldCompose4() {
          final Function4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose4(toUpperCase).apply("s1", "s2", "s3", "xx")).isEqualTo("s1s2s3XX");
      }

    }

    @Test
    public void shouldNarrow(){
        final Function4<Number, Number, Number, Number, String> wideFunction = (o1, o2, o3, o4) -> String.format("Numbers are: %s, %s, %s, %s", o1, o2, o3, o4);
        final Function4<Integer, Integer, Integer, Integer, CharSequence> narrowFunction = Function4.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2, 3, 4)).isEqualTo("Numbers are: 1, 2, 3, 4");
    }
}