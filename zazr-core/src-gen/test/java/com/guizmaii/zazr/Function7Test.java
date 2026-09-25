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

public class Function7Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(Function7.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        final Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Option<Integer>> lifted = Function7.lift((i1, i2, i3, i4, i5, i6, i7) -> {
            if (i1 == 0) {
                return null;
            }
            if (i1 == 1) {
                throw new IllegalStateException("non-fatal");
            }
            if (i1 == 2) {
                throw new OutOfMemoryError("fatal");
            }
            return i1 + i2 + i3 + i4 + i5 + i6 + i7;
        });
        assertThat(lifted.apply(3, 1, 1, 1, 1, 1, 1)).isEqualTo(Option.some(9));
        assertThat(lifted.apply(0, 1, 1, 1, 1, 1, 1)).isEqualTo(Option.none());
        assertThat(lifted.apply(1, 1, 1, 1, 1, 1, 1)).isEqualTo(Option.none());
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(2, 1, 1, 1, 1, 1, 1));
    }

    @Test
    public void shouldRethrowFatalThrowableFromLiftTry() {
        final Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Try<Integer>> lifted =
            Function7.liftTry((i1, i2, i3, i4, i5, i6, i7) -> { throw new OutOfMemoryError("fatal"); });
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(1, 1, 1, 1, 1, 1, 1));
    }

    @Test
    public void shouldReturnFailureFromLiftTryOnNonFatalThrowable() {
        final Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Try<Integer>> lifted =
            Function7.liftTry((i1, i2, i3, i4, i5, i6, i7) -> { throw new IllegalStateException("non-fatal"); });
        final Try<Integer> result = lifted.apply(1, 1, 1, 1, 1, 1, 1);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("non-fatal");
    }

    @Test
    public void shouldPartiallyApply() {
        final Function7<Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7) -> "" + o1 + o2 + o3 + o4 + o5 + o6 + o7;
        assertThat(f.apply(1).apply(2, 3, 4, 5, 6, 7)).isEqualTo("1234567");
        assertThat(f.apply(1, 2).apply(3, 4, 5, 6, 7)).isEqualTo("1234567");
        assertThat(f.apply(1, 2, 3).apply(4, 5, 6, 7)).isEqualTo("1234567");
        assertThat(f.apply(1, 2, 3, 4).apply(5, 6, 7)).isEqualTo("1234567");
        assertThat(f.apply(1, 2, 3, 4, 5).apply(6, 7)).isEqualTo("1234567");
        assertThat(f.apply(1, 2, 3, 4, 5, 6).apply(7)).isEqualTo("1234567");
    }

    @Test
    public void shouldConstant() {
        final Function7<Object, Object, Object, Object, Object, Object, Object, Object> f = Function7.constant(6);
        assertThat(f.apply(1, 2, 3, 4, 5, 6, 7)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() {
        final Function7<Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7) -> "" + o1 + o2 + o3 + o4 + o5 + o6 + o7;
        final Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Object>>>>>>> curried = f.curried();
        assertThat(curried.apply(1).apply(2).apply(3).apply(4).apply(5).apply(6).apply(7)).isEqualTo("1234567");
    }

    @Test
    public void shouldTuple() {
        final Function7<Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7) -> "" + o1 + o2 + o3 + o4 + o5 + o6 + o7;
        final Function<Tuple7<Object, Object, Object, Object, Object, Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled.apply(Tuple.of(1, 2, 3, 4, 5, 6, 7))).isEqualTo("1234567");
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        AtomicInteger integer = new AtomicInteger();
        Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> divByZero = (i1, i2, i3, i4, i5, i6, i7) -> 10 / integer.get();
        Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Try<Integer>> divByZeroTry = Function7.liftTry(divByZero);

        Try<Integer> res = divByZeroTry.apply(0, 0, 0, 0, 0, 0, 0);
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getCause()).isNotNull();
        assertThat(res.getCause().getMessage()).isEqualToIgnoringCase("/ by zero");

        integer.incrementAndGet();
        res = divByZeroTry.apply(1, 2, 3, 4, 5, 6, 7);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.get()).isEqualTo(10);
    }

    private static final Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> recurrent1 = (i1, i2, i3, i4, i5, i6, i7) -> i1 <= 0 ? i1 : Function7Test.recurrent1.apply(i1 - 1, i2, i3, i4, i5, i6, i7) + 1;

    @Test
    public void shouldCalculatedRecursively() {
        assertThat(recurrent1.apply(11, 11, 11, 11, 11, 11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22, 22, 22, 22, 22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final Function7<Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7) -> "" + o1 + o2 + o3 + o4 + o5 + o6 + o7;
        final Function<Object, Object> after = o -> o + "!";
        final Function7<Object, Object, Object, Object, Object, Object, Object, Object> composed = f.andThen(after);
        assertThat(composed.apply(1, 2, 3, 4, 5, 6, 7)).isEqualTo("1234567!");
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2", "s3", "s4", "s5", "s6", "s7")).isEqualTo("XXs2s3s4s5s6s7");
      }

      @Test
      public void shouldCompose2() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx", "s3", "s4", "s5", "s6", "s7")).isEqualTo("s1XXs3s4s5s6s7");
      }

      @Test
      public void shouldCompose3() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose3(toUpperCase).apply("s1", "s2", "xx", "s4", "s5", "s6", "s7")).isEqualTo("s1s2XXs4s5s6s7");
      }

      @Test
      public void shouldCompose4() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose4(toUpperCase).apply("s1", "s2", "s3", "xx", "s5", "s6", "s7")).isEqualTo("s1s2s3XXs5s6s7");
      }

      @Test
      public void shouldCompose5() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose5(toUpperCase).apply("s1", "s2", "s3", "s4", "xx", "s6", "s7")).isEqualTo("s1s2s3s4XXs6s7");
      }

      @Test
      public void shouldCompose6() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose6(toUpperCase).apply("s1", "s2", "s3", "s4", "s5", "xx", "s7")).isEqualTo("s1s2s3s4s5XXs7");
      }

      @Test
      public void shouldCompose7() {
          final Function7<String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7) -> s1 + s2 + s3 + s4 + s5 + s6 + s7;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose7(toUpperCase).apply("s1", "s2", "s3", "s4", "s5", "s6", "xx")).isEqualTo("s1s2s3s4s5s6XX");
      }

    }

    @Test
    public void shouldNarrow(){
        final Function7<Number, Number, Number, Number, Number, Number, Number, String> wideFunction = (o1, o2, o3, o4, o5, o6, o7) -> String.format("Numbers are: %s, %s, %s, %s, %s, %s, %s", o1, o2, o3, o4, o5, o6, o7);
        final Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, CharSequence> narrowFunction = Function7.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2, 3, 4, 5, 6, 7)).isEqualTo("Numbers are: 1, 2, 3, 4, 5, 6, 7");
    }
}