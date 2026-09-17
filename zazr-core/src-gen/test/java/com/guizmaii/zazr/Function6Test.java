package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guizmaii.zazr.control.Try;
import java.lang.CharSequence;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class Function6Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(Function6.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        assertThat(Function6.lift((o1, o2, o3, o4, o5, o6) -> { while(true); })).isNotNull();
    }

    @Test
    public void shouldPartiallyApply() {
        final Function6<Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6) -> null;
        assertThat(f.apply(null)).isNotNull();
        assertThat(f.apply(null, null)).isNotNull();
        assertThat(f.apply(null, null, null)).isNotNull();
        assertThat(f.apply(null, null, null, null)).isNotNull();
        assertThat(f.apply(null, null, null, null, null)).isNotNull();
    }

    @Test
    public void shouldConstant() {
        final Function6<Object, Object, Object, Object, Object, Object, Object> f = Function6.constant(6);
        assertThat(f.apply(1, 2, 3, 4, 5, 6)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() {
        final Function6<Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6) -> null;
        final Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Object>>>>>> curried = f.curried();
        assertThat(curried).isNotNull();
    }

    @Test
    public void shouldTuple() {
        final Function6<Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6) -> null;
        final Function<Tuple6<Object, Object, Object, Object, Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled).isNotNull();
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        AtomicInteger integer = new AtomicInteger();
        Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> divByZero = (i1, i2, i3, i4, i5, i6) -> 10 / integer.get();
        Function6<Integer, Integer, Integer, Integer, Integer, Integer, Try<Integer>> divByZeroTry = Function6.liftTry(divByZero);

        Try<Integer> res = divByZeroTry.apply(0, 0, 0, 0, 0, 0);
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getCause()).isNotNull();
        assertThat(res.getCause().getMessage()).isEqualToIgnoringCase("/ by zero");

        integer.incrementAndGet();
        res = divByZeroTry.apply(1, 2, 3, 4, 5, 6);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.get()).isEqualTo(10);
    }

    private static final Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> recurrent1 = (i1, i2, i3, i4, i5, i6) -> i1 <= 0 ? i1 : Function6Test.recurrent1.apply(i1 - 1, i2, i3, i4, i5, i6) + 1;

    @Test
    public void shouldCalculatedRecursively() {
        assertThat(recurrent1.apply(11, 11, 11, 11, 11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22, 22, 22, 22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final Function6<Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6) -> null;
        final Function<Object, Object> after = o -> null;
        final Function6<Object, Object, Object, Object, Object, Object, Object> composed = f.andThen(after);
        assertThat(composed).isNotNull();
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1() {
          final Function6<String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6) -> s1 + s2 + s3 + s4 + s5 + s6;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2", "s3", "s4", "s5", "s6")).isEqualTo("XXs2s3s4s5s6");
      }

      @Test
      public void shouldCompose2() {
          final Function6<String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6) -> s1 + s2 + s3 + s4 + s5 + s6;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx", "s3", "s4", "s5", "s6")).isEqualTo("s1XXs3s4s5s6");
      }

      @Test
      public void shouldCompose3() {
          final Function6<String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6) -> s1 + s2 + s3 + s4 + s5 + s6;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose3(toUpperCase).apply("s1", "s2", "xx", "s4", "s5", "s6")).isEqualTo("s1s2XXs4s5s6");
      }

      @Test
      public void shouldCompose4() {
          final Function6<String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6) -> s1 + s2 + s3 + s4 + s5 + s6;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose4(toUpperCase).apply("s1", "s2", "s3", "xx", "s5", "s6")).isEqualTo("s1s2s3XXs5s6");
      }

      @Test
      public void shouldCompose5() {
          final Function6<String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6) -> s1 + s2 + s3 + s4 + s5 + s6;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose5(toUpperCase).apply("s1", "s2", "s3", "s4", "xx", "s6")).isEqualTo("s1s2s3s4XXs6");
      }

      @Test
      public void shouldCompose6() {
          final Function6<String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6) -> s1 + s2 + s3 + s4 + s5 + s6;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose6(toUpperCase).apply("s1", "s2", "s3", "s4", "s5", "xx")).isEqualTo("s1s2s3s4s5XX");
      }

    }

    @Test
    public void shouldNarrow(){
        final Function6<Number, Number, Number, Number, Number, Number, String> wideFunction = (o1, o2, o3, o4, o5, o6) -> String.format("Numbers are: %s, %s, %s, %s, %s, %s", o1, o2, o3, o4, o5, o6);
        final Function6<Integer, Integer, Integer, Integer, Integer, Integer, CharSequence> narrowFunction = Function6.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2, 3, 4, 5, 6)).isEqualTo("Numbers are: 1, 2, 3, 4, 5, 6");
    }
}