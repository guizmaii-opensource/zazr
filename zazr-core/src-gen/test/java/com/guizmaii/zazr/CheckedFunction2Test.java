package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guizmaii.zazr.control.Try;
import java.lang.CharSequence;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CheckedFunction2Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(CheckedFunction2.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        assertThat(CheckedFunction2.lift((o1, o2) -> { while(true); })).isNotNull();
    }

    @Test
    public void shouldPartiallyApply() throws Exception {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        assertThat(f.apply(null)).isNotNull();
    }

    @Test
    public void shouldConstant() throws Exception {
        final CheckedFunction2<Object, Object, Object> f = CheckedFunction2.constant(6);
        assertThat(f.apply(1, 2)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        final Function<Object, CheckedFunction1<Object, Object>> curried = f.curried();
        assertThat(curried).isNotNull();
    }

    @Test
    public void shouldTuple() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        final CheckedFunction1<Tuple2<Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled).isNotNull();
    }

    private static final CheckedFunction2<String, String, MessageDigest> digest = (s1, s2) -> MessageDigest.getInstance(s1 + s2);

    @Test
    public void shouldRecover() {
        final BiFunction<String, String, MessageDigest> recover = digest.recover(throwable -> (s1, s2) -> null);
        final MessageDigest md5 = recover.apply("M", "D5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        assertThat(recover.apply("U", "nknown")).isNull();
    }

    @Test
    public void shouldRecoverNonNull() {
        final BiFunction<String, String, MessageDigest> recover = digest.recover(throwable -> null);
        final MessageDigest md5 = recover.apply("M", "D5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = Try.of(() -> recover.apply("U", "nknown"));
        assertThat(unknown).isNotNull();
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull().isInstanceOf(NullPointerException.class);
        assertThat(unknown.getCause().getMessage()).isNotEmpty().isEqualToIgnoringCase("recover return null for class java.security.NoSuchAlgorithmException: Unknown MessageDigest not available");
    }

    @Test
    public void shouldUncheckedWork() {
        final BiFunction<String, String, MessageDigest> unchecked = digest.unchecked();
        final MessageDigest md5 = unchecked.apply("M", "D5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
    }

    @Test
    public void shouldUncheckedThrowIllegalState() {
        assertThrows(NoSuchAlgorithmException.class, () -> {
            final BiFunction<String, String, MessageDigest> unchecked = digest.unchecked();
            unchecked.apply("U", "nknown"); // Look ma, we throw an undeclared checked exception!
        });
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        final BiFunction<String, String, Try<MessageDigest>> liftTry = CheckedFunction2.liftTry(digest);
        final Try<MessageDigest> md5 = liftTry.apply("M", "D5");
        assertThat(md5.isSuccess()).isTrue();
        assertThat(md5.get()).isNotNull();
        assertThat(md5.get().getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.get().getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = liftTry.apply("U", "nknown");
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull();
        assertThat(unknown.getCause().getMessage()).isEqualToIgnoringCase("Unknown MessageDigest not available");
    }

    private static final CheckedFunction2<Integer, Integer, Integer> recurrent1 = (i1, i2) -> i1 <= 0 ? i1 : CheckedFunction2Test.recurrent1.apply(i1 - 1, i2) + 1;

    @Test
    public void shouldCalculatedRecursively() throws Exception {
        assertThat(recurrent1.apply(11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        final CheckedFunction1<Object, Object> after = o -> null;
        final CheckedFunction2<Object, Object, Object> composed = f.andThen(after);
        assertThat(composed).isNotNull();
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1()  throws Exception {
          final CheckedFunction2<String, String, String> concat = (String s1, String s2) -> s1 + s2;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2")).isEqualTo("XXs2");
      }

      @Test
      public void shouldCompose2()  throws Exception {
          final CheckedFunction2<String, String, String> concat = (String s1, String s2) -> s1 + s2;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx")).isEqualTo("s1XX");
      }

    }

    @Test
    public void shouldNarrow() throws Exception{
        final CheckedFunction2<Number, Number, String> wideFunction = (o1, o2) -> String.format("Numbers are: %s, %s", o1, o2);
        final CheckedFunction2<Integer, Integer, CharSequence> narrowFunction = CheckedFunction2.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2)).isEqualTo("Numbers are: 1, 2");
    }
}