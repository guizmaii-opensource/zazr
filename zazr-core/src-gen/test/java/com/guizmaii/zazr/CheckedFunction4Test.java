package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import java.lang.CharSequence;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CheckedFunction4Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1, Object o2, Object o3, Object o4) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(CheckedFunction4.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        final Function4<Integer, Integer, Integer, Integer, Option<Integer>> lifted = CheckedFunction4.lift((i1, i2, i3, i4) -> {
            if (i1 == 0) {
                return null;
            }
            if (i1 == 1) {
                throw new Exception("non-fatal");
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
            CheckedFunction4.liftTry((i1, i2, i3, i4) -> { throw new OutOfMemoryError("fatal"); });
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(1, 1, 1, 1));
    }

    @Test
    public void shouldReturnFailureFromLiftTryOnNonFatalThrowable() {
        final Function4<Integer, Integer, Integer, Integer, Try<Integer>> lifted =
            CheckedFunction4.liftTry((i1, i2, i3, i4) -> { throw new Exception("non-fatal"); });
        final Try<Integer> result = lifted.apply(1, 1, 1, 1);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(Exception.class).hasMessage("non-fatal");
    }

    @Test
    public void shouldPartiallyApply() throws Exception {
        final CheckedFunction4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        assertThat(f.apply(1).apply(2, 3, 4)).isEqualTo("1234");
        assertThat(f.apply(1, 2).apply(3, 4)).isEqualTo("1234");
        assertThat(f.apply(1, 2, 3).apply(4)).isEqualTo("1234");
    }

    @Test
    public void shouldConstant() throws Exception {
        final CheckedFunction4<Object, Object, Object, Object, Object> f = CheckedFunction4.constant(6);
        assertThat(f.apply(1, 2, 3, 4)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() throws Exception {
        final CheckedFunction4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        final Function<Object, Function<Object, Function<Object, CheckedFunction1<Object, Object>>>> curried = f.curried();
        assertThat(curried.apply(1).apply(2).apply(3).apply(4)).isEqualTo("1234");
    }

    @Test
    public void shouldTuple() throws Exception {
        final CheckedFunction4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        final CheckedFunction1<Tuple4<Object, Object, Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled.apply(Tuple.of(1, 2, 3, 4))).isEqualTo("1234");
    }

    private static final CheckedFunction4<String, String, String, String, MessageDigest> digest = (s1, s2, s3, s4) -> MessageDigest.getInstance(s1 + s2 + s3 + s4);

    @Test
    public void shouldRecover() {
        final Function4<String, String, String, String, MessageDigest> recover = digest.recover(throwable -> (s1, s2, s3, s4) -> null);
        final MessageDigest md5 = recover.apply("M", "D", "5", "");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        assertThat(recover.apply("U", "n", "k", "nown")).isNull();
    }

    @Test
    public void shouldRecoverNonNull() {
        final Function4<String, String, String, String, MessageDigest> recover = digest.recover(throwable -> null);
        final MessageDigest md5 = recover.apply("M", "D", "5", "");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = Try.of(() -> recover.apply("U", "n", "k", "nown"));
        assertThat(unknown).isNotNull();
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull().isInstanceOf(NullPointerException.class);
        assertThat(unknown.getCause().getMessage()).isNotEmpty().isEqualToIgnoringCase("recover return null for class java.security.NoSuchAlgorithmException: Unknown MessageDigest not available");
    }

    @Test
    public void shouldNotHandFatalThrowableToRecover() {
        final CheckedFunction4<String, String, String, String, MessageDigest> fatal = (s1, s2, s3, s4) -> { throw new OutOfMemoryError("fatal"); };
        final Function4<String, String, String, String, MessageDigest> recover =
            fatal.recover(throwable -> { throw new AssertionError("recover must not see a fatal throwable"); });
        assertThrows(OutOfMemoryError.class, () -> recover.apply("M", "D", "5", ""));
    }

    @Test
    public void shouldHandNonFatalThrowableToRecover() {
        final CheckedFunction4<String, String, String, String, MessageDigest> nonFatal = (s1, s2, s3, s4) -> { throw new IllegalStateException("non-fatal"); };
        final Function4<String, String, String, String, MessageDigest> recover =
            nonFatal.recover(throwable -> (s1, s2, s3, s4) -> null);
        assertThat(recover.apply("M", "D", "5", "")).isNull();
    }

    @Test
    public void shouldUncheckedWork() {
        final Function4<String, String, String, String, MessageDigest> unchecked = digest.unchecked();
        final MessageDigest md5 = unchecked.apply("M", "D", "5", "");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
    }

    @Test
    public void shouldUncheckedThrowIllegalState() {
        assertThrows(NoSuchAlgorithmException.class, () -> {
            final Function4<String, String, String, String, MessageDigest> unchecked = digest.unchecked();
            unchecked.apply("U", "n", "k", "nown"); // Look ma, we throw an undeclared checked exception!
        });
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        final Function4<String, String, String, String, Try<MessageDigest>> liftTry = CheckedFunction4.liftTry(digest);
        final Try<MessageDigest> md5 = liftTry.apply("M", "D", "5", "");
        assertThat(md5.isSuccess()).isTrue();
        assertThat(md5.get()).isNotNull();
        assertThat(md5.get().getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.get().getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = liftTry.apply("U", "n", "k", "nown");
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull();
        assertThat(unknown.getCause().getMessage()).isEqualToIgnoringCase("Unknown MessageDigest not available");
    }

    private static final CheckedFunction4<Integer, Integer, Integer, Integer, Integer> recurrent1 = (i1, i2, i3, i4) -> i1 <= 0 ? i1 : CheckedFunction4Test.recurrent1.apply(i1 - 1, i2, i3, i4) + 1;

    @Test
    public void shouldCalculatedRecursively() throws Exception {
        assertThat(recurrent1.apply(11, 11, 11, 11)).isEqualTo(11);
        assertThat(recurrent1.apply(22, 22, 22, 22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() throws Exception {
        final CheckedFunction4<Object, Object, Object, Object, Object> f = (o1, o2, o3, o4) -> "" + o1 + o2 + o3 + o4;
        final CheckedFunction1<Object, Object> after = o -> o + "!";
        final CheckedFunction4<Object, Object, Object, Object, Object> composed = f.andThen(after);
        assertThat(composed.apply(1, 2, 3, 4)).isEqualTo("1234!");
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1()  throws Exception {
          final CheckedFunction4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx", "s2", "s3", "s4")).isEqualTo("XXs2s3s4");
      }

      @Test
      public void shouldCompose2()  throws Exception {
          final CheckedFunction4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose2(toUpperCase).apply("s1", "xx", "s3", "s4")).isEqualTo("s1XXs3s4");
      }

      @Test
      public void shouldCompose3()  throws Exception {
          final CheckedFunction4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose3(toUpperCase).apply("s1", "s2", "xx", "s4")).isEqualTo("s1s2XXs4");
      }

      @Test
      public void shouldCompose4()  throws Exception {
          final CheckedFunction4<String, String, String, String, String> concat = (String s1, String s2, String s3, String s4) -> s1 + s2 + s3 + s4;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose4(toUpperCase).apply("s1", "s2", "s3", "xx")).isEqualTo("s1s2s3XX");
      }

    }

    @Test
    public void shouldNarrow() throws Exception{
        final CheckedFunction4<Number, Number, Number, Number, String> wideFunction = (o1, o2, o3, o4) -> String.format("Numbers are: %s, %s, %s, %s", o1, o2, o3, o4);
        final CheckedFunction4<Integer, Integer, Integer, Integer, CharSequence> narrowFunction = CheckedFunction4.narrow(wideFunction);

        assertThat(narrowFunction.apply(1, 2, 3, 4)).isEqualTo("Numbers are: 1, 2, 3, 4");
    }
}