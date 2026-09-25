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

public class CheckedFunction1Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(CheckedFunction1.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        final Function<Integer, Option<Integer>> lifted = CheckedFunction1.lift((i1) -> {
            if (i1 == 0) {
                return null;
            }
            if (i1 == 1) {
                throw new Exception("non-fatal");
            }
            if (i1 == 2) {
                throw new OutOfMemoryError("fatal");
            }
            return i1;
        });
        assertThat(lifted.apply(3)).isEqualTo(Option.some(3));
        assertThat(lifted.apply(0)).isEqualTo(Option.none());
        assertThat(lifted.apply(1)).isEqualTo(Option.none());
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(2));
    }

    @Test
    public void shouldRethrowFatalThrowableFromLiftTry() {
        final Function<Integer, Try<Integer>> lifted =
            CheckedFunction1.liftTry((i1) -> { throw new OutOfMemoryError("fatal"); });
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(1));
    }

    @Test
    public void shouldReturnFailureFromLiftTryOnNonFatalThrowable() {
        final Function<Integer, Try<Integer>> lifted =
            CheckedFunction1.liftTry((i1) -> { throw new Exception("non-fatal"); });
        final Try<Integer> result = lifted.apply(1);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(Exception.class).hasMessage("non-fatal");
    }

    @Test
    public void shouldCreateIdentityFunction() throws Exception {
        final CheckedFunction1<String, String> identity = CheckedFunction1.identity();
        final String s = "test";
        assertThat(identity.apply(s)).isEqualTo(s);
    }

    @Test
    public void shouldConstant() throws Exception {
        final CheckedFunction1<Object, Object> f = CheckedFunction1.constant(6);
        assertThat(f.apply(1)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() throws Exception {
        final CheckedFunction1<Object, Object> f = (o1) -> "" + o1;
        final CheckedFunction1<Object, Object> curried = f.curried();
        assertThat(curried.apply(1)).isEqualTo("1");
    }

    @Test
    public void shouldTuple() throws Exception {
        final CheckedFunction1<Object, Object> f = (o1) -> "" + o1;
        final CheckedFunction1<Tuple1<Object>, Object> tupled = f.tupled();
        assertThat(tupled.apply(Tuple.of(1))).isEqualTo("1");
    }

    private static final CheckedFunction1<String, MessageDigest> digest = (s1) -> MessageDigest.getInstance(s1);

    @Test
    public void shouldRecover() {
        final Function<String, MessageDigest> recover = digest.recover(throwable -> (s1) -> null);
        final MessageDigest md5 = recover.apply("MD5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        assertThat(recover.apply("Unknown")).isNull();
    }

    @Test
    public void shouldRecoverNonNull() {
        final Function<String, MessageDigest> recover = digest.recover(throwable -> null);
        final MessageDigest md5 = recover.apply("MD5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = Try.of(() -> recover.apply("Unknown"));
        assertThat(unknown).isNotNull();
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull().isInstanceOf(NullPointerException.class);
        assertThat(unknown.getCause().getMessage()).isNotEmpty().isEqualToIgnoringCase("recover return null for class java.security.NoSuchAlgorithmException: Unknown MessageDigest not available");
    }

    @Test
    public void shouldNotHandFatalThrowableToRecover() {
        final CheckedFunction1<String, MessageDigest> fatal = (s1) -> { throw new OutOfMemoryError("fatal"); };
        final Function<String, MessageDigest> recover =
            fatal.recover(throwable -> { throw new AssertionError("recover must not see a fatal throwable"); });
        assertThrows(OutOfMemoryError.class, () -> recover.apply("MD5"));
    }

    @Test
    public void shouldHandNonFatalThrowableToRecover() {
        final CheckedFunction1<String, MessageDigest> nonFatal = (s1) -> { throw new IllegalStateException("non-fatal"); };
        final Function<String, MessageDigest> recover =
            nonFatal.recover(throwable -> (s1) -> null);
        assertThat(recover.apply("MD5")).isNull();
    }

    @Test
    public void shouldUncheckedWork() {
        final Function<String, MessageDigest> unchecked = digest.unchecked();
        final MessageDigest md5 = unchecked.apply("MD5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
    }

    @Test
    public void shouldUncheckedThrowIllegalState() {
        assertThrows(NoSuchAlgorithmException.class, () -> {
            final Function<String, MessageDigest> unchecked = digest.unchecked();
            unchecked.apply("Unknown"); // Look ma, we throw an undeclared checked exception!
        });
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        final Function<String, Try<MessageDigest>> liftTry = CheckedFunction1.liftTry(digest);
        final Try<MessageDigest> md5 = liftTry.apply("MD5");
        assertThat(md5.isSuccess()).isTrue();
        assertThat(md5.get()).isNotNull();
        assertThat(md5.get().getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.get().getDigestLength()).isEqualTo(16);
        final Try<MessageDigest> unknown = liftTry.apply("Unknown");
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull();
        assertThat(unknown.getCause().getMessage()).isEqualToIgnoringCase("Unknown MessageDigest not available");
    }

    private static final CheckedFunction1<Integer, Integer> recurrent1 = (i1) -> i1 <= 0 ? i1 : CheckedFunction1Test.recurrent1.apply(i1 - 1) + 1;

    @Test
    public void shouldCalculatedRecursively() throws Exception {
        assertThat(recurrent1.apply(11)).isEqualTo(11);
        assertThat(recurrent1.apply(22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() throws Exception {
        final CheckedFunction1<Object, Object> f = (o1) -> "" + o1;
        final CheckedFunction1<Object, Object> after = o -> o + "!";
        final CheckedFunction1<Object, Object> composed = f.andThen(after);
        assertThat(composed.apply(1)).isEqualTo("1!");
    }

    @Test
    public void shouldComposeWithBefore() throws Exception {
        final CheckedFunction1<String, Integer> length = String::length;
        final CheckedFunction1<Integer, String> repeat = n -> "x".repeat(n);
        assertThat(length.compose(repeat).apply(3)).isEqualTo(3);
    }

    @Nested
    class ComposeTests {

      @Test
      public void shouldCompose1()  throws Exception {
          final CheckedFunction1<String, String> concat = (String s1) -> s1;
          final Function<String, String> toUpperCase = String::toUpperCase;
          assertThat(concat.compose1(toUpperCase).apply("xx")).isEqualTo("XX");
      }

    }

    @Test
    public void shouldNarrow() throws Exception{
        final CheckedFunction1<Number, String> wideFunction = (o1) -> String.format("Numbers are: %s", o1);
        final CheckedFunction1<Integer, CharSequence> narrowFunction = CheckedFunction1.narrow(wideFunction);

        assertThat(narrowFunction.apply(1)).isEqualTo("Numbers are: 1");
    }
}