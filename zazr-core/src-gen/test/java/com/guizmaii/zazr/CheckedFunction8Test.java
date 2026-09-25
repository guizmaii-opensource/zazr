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
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CheckedFunction8Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(CheckedFunction8.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        assertThat(CheckedFunction8.lift((o1, o2, o3, o4, o5, o6, o7, o8) -> { while(true); })).isNotNull();
    }

    @Test
    public void shouldPartiallyApply() throws Exception {
        final CheckedFunction8<Object, Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7, o8) -> null;
        assertThat(f.apply(null)).isNotNull();
        assertThat(f.apply(null, null)).isNotNull();
        assertThat(f.apply(null, null, null)).isNotNull();
        assertThat(f.apply(null, null, null, null)).isNotNull();
        assertThat(f.apply(null, null, null, null, null)).isNotNull();
        assertThat(f.apply(null, null, null, null, null, null)).isNotNull();
        assertThat(f.apply(null, null, null, null, null, null, null)).isNotNull();
    }

    @Test
    public void shouldConstant() throws Exception {
        final CheckedFunction8<Object, Object, Object, Object, Object, Object, Object, Object, Object> f = CheckedFunction8.constant(6);
        assertThat(f.apply(1, 2, 3, 4, 5, 6, 7, 8)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() {
        final CheckedFunction8<Object, Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7, o8) -> null;
        final Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, Function<Object, CheckedFunction1<Object, Object>>>>>>>> curried = f.curried();
        assertThat(curried).isNotNull();
    }

    @Test
    public void shouldTuple() {
        final CheckedFunction8<Object, Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7, o8) -> null;
        final CheckedFunction1<Tuple8<Object, Object, Object, Object, Object, Object, Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled).isNotNull();
    }

    private static final CheckedFunction8<String, String, String, String, String, String, String, String, MessageDigest> digest = (s1, s2, s3, s4, s5, s6, s7, s8) -> MessageDigest.getInstance(s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8);

    @Test
    public void shouldRecover() {
        final Function8<String, String, String, String, String, String, String, String, MessageDigest> recover = digest.recover(throwable -> (s1, s2, s3, s4, s5, s6, s7, s8) -> null);
        final MessageDigest md5 = recover.apply("M", "D", "5", "", "", "", "", "");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        assertThat(recover.apply("U", "n", "k", "n", "o", "w", "n", "")).isNull();
    }

    @Test
    public void shouldRecoverNonNull() {
        final Function8<String, String, String, String, String, String, String, String, MessageDigest> recover = digest.recover(throwable -> null);
        final MessageDigest md5 = recover.apply("M", "D", "5", "", "", "", "", "");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = Try.of(() -> recover.apply("U", "n", "k", "n", "o", "w", "n", ""));
        assertThat(unknown).isNotNull();
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull().isInstanceOf(NullPointerException.class);
        assertThat(unknown.getCause().getMessage()).isEqualTo("CheckedFunction8.recover: recover returned null");
        assertThat(unknown.getCause().getCause()).isInstanceOf(java.security.NoSuchAlgorithmException.class).hasMessage("Unknown MessageDigest not available");
    }

    @Test
    public void shouldNotHandFatalThrowableToRecover() {
        final CheckedFunction8<String, String, String, String, String, String, String, String, MessageDigest> fatal = (s1, s2, s3, s4, s5, s6, s7, s8) -> { throw new OutOfMemoryError("fatal"); };
        final Function8<String, String, String, String, String, String, String, String, MessageDigest> recover =
            fatal.recover(throwable -> { throw new AssertionError("recover must not see a fatal throwable"); });
        assertThrows(OutOfMemoryError.class, () -> recover.apply("M", "D", "5", "", "", "", "", ""));
    }

    @Test
    public void shouldHandNonFatalThrowableToRecover() {
        final CheckedFunction8<String, String, String, String, String, String, String, String, MessageDigest> nonFatal = (s1, s2, s3, s4, s5, s6, s7, s8) -> { throw new IllegalStateException("non-fatal"); };
        final Function8<String, String, String, String, String, String, String, String, MessageDigest> recover =
            nonFatal.recover(throwable -> (s1, s2, s3, s4, s5, s6, s7, s8) -> null);
        assertThat(recover.apply("M", "D", "5", "", "", "", "", "")).isNull();
    }

    @Test
    public void shouldUncheckedWork() {
        final Function8<String, String, String, String, String, String, String, String, MessageDigest> unchecked = digest.unchecked();
        final MessageDigest md5 = unchecked.apply("M", "D", "5", "", "", "", "", "");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
    }

    @Test
    public void shouldUncheckedThrowIllegalState() {
        assertThrows(NoSuchAlgorithmException.class, () -> {
            final Function8<String, String, String, String, String, String, String, String, MessageDigest> unchecked = digest.unchecked();
            unchecked.apply("U", "n", "k", "n", "o", "w", "n", ""); // Look ma, we throw an undeclared checked exception!
        });
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        final Function8<String, String, String, String, String, String, String, String, Try<MessageDigest>> liftTry = CheckedFunction8.liftTry(digest);
        final Try<MessageDigest> md5 = liftTry.apply("M", "D", "5", "", "", "", "", "");
        assertThat(md5.isSuccess()).isTrue();
        assertThat(md5.get()).isNotNull();
        assertThat(md5.get().getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.get().getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = liftTry.apply("U", "n", "k", "n", "o", "w", "n", "");
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull();
        assertThat(unknown.getCause().getMessage()).isEqualToIgnoringCase("Unknown MessageDigest not available");
    }

    private static final CheckedFunction8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> recurrent1 = (i1, i2, i3, i4, i5, i6, i7, i8) -> i1 <= 0 ? i1 : CheckedFunction8Test.recurrent1.apply(i1 - 1, i2, i3, i4, i5, i6, i7, i8) + 1;

    @Test
    public void shouldCalculatedRecursively() throws Exception {
        assertThat(recurrent1.apply(11, 11, 11, 11, 11, 11, 11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22, 22, 22, 22, 22, 22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final CheckedFunction8<Object, Object, Object, Object, Object, Object, Object, Object, Object> f = (o1, o2, o3, o4, o5, o6, o7, o8) -> null;
        final CheckedFunction1<Object, Object> after = o -> null;
        final CheckedFunction8<Object, Object, Object, Object, Object, Object, Object, Object, Object> composed = f.andThen(after);
        assertThat(composed).isNotNull();
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2", "s3", "s4", "s5", "s6", "s7", "s8")).isEqualTo("XXs2s3s4s5s6s7s8");
      }

      @Test
      public void shouldCompose2()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx", "s3", "s4", "s5", "s6", "s7", "s8")).isEqualTo("s1XXs3s4s5s6s7s8");
      }

      @Test
      public void shouldCompose3()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose3(toUpperCase).apply("s1", "s2", "xx", "s4", "s5", "s6", "s7", "s8")).isEqualTo("s1s2XXs4s5s6s7s8");
      }

      @Test
      public void shouldCompose4()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose4(toUpperCase).apply("s1", "s2", "s3", "xx", "s5", "s6", "s7", "s8")).isEqualTo("s1s2s3XXs5s6s7s8");
      }

      @Test
      public void shouldCompose5()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose5(toUpperCase).apply("s1", "s2", "s3", "s4", "xx", "s6", "s7", "s8")).isEqualTo("s1s2s3s4XXs6s7s8");
      }

      @Test
      public void shouldCompose6()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose6(toUpperCase).apply("s1", "s2", "s3", "s4", "s5", "xx", "s7", "s8")).isEqualTo("s1s2s3s4s5XXs7s8");
      }

      @Test
      public void shouldCompose7()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose7(toUpperCase).apply("s1", "s2", "s3", "s4", "s5", "s6", "xx", "s8")).isEqualTo("s1s2s3s4s5s6XXs8");
      }

      @Test
      public void shouldCompose8()  throws Exception {
          final CheckedFunction8<String, String, String, String, String, String, String, String, String> concat = (String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) -> s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose8(toUpperCase).apply("s1", "s2", "s3", "s4", "s5", "s6", "s7", "xx")).isEqualTo("s1s2s3s4s5s6s7XX");
      }

    }

    @Test
    public void shouldNarrow() throws Exception{
        final CheckedFunction8<Number, Number, Number, Number, Number, Number, Number, Number, String> wideFunction = (o1, o2, o3, o4, o5, o6, o7, o8) -> String.format("Numbers are: %s, %s, %s, %s, %s, %s, %s, %s", o1, o2, o3, o4, o5, o6, o7, o8);
        final CheckedFunction8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, CharSequence> narrowFunction = CheckedFunction8.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2, 3, 4, 5, 6, 7, 8)).isEqualTo("Numbers are: 1, 2, 3, 4, 5, 6, 7, 8");
    }
}