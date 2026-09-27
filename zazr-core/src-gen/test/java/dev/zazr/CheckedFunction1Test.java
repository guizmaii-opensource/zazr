package dev.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import dev.zazr.control.Option;
import dev.zazr.control.Try;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckedFunction1Test {

    @Test
    public void shouldCreateFromMethodReference() {
        class Type {
            Object methodReference(Object o1) {
                return null;
            }
        }
        Type type = new Type();
        assertThat(CheckedFunction1.of(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldLiftPartialFunction() {
        Function<Integer, Option<Integer>> lifted = CheckedFunction1.lift((i1) -> {
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
        Function<Integer, Try<Integer>> lifted = CheckedFunction1.liftTry((i1) -> {
            throw new OutOfMemoryError("fatal");
        });
        assertThrows(OutOfMemoryError.class, () -> lifted.apply(1));
    }

    @Test
    public void shouldReturnFailureFromLiftTryOnNonFatalThrowable() {
        Function<Integer, Try<Integer>> lifted = CheckedFunction1.liftTry((i1) -> {
            throw new Exception("non-fatal");
        });
        Try<Integer> result = lifted.apply(1);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(Exception.class).hasMessage("non-fatal");
    }

    @Test
    public void shouldCreateIdentityFunction() throws Exception {
        CheckedFunction1<String, String> identity = CheckedFunction1.identity();
        String s = "test";
        assertThat(identity.apply(s)).isEqualTo(s);
    }

    @Test
    public void shouldConstant() throws Exception {
        CheckedFunction1<Object, Object> f = CheckedFunction1.constant(6);
        assertThat(f.apply(1)).isEqualTo(6);
    }

    @Test
    public void shouldCurry() throws Exception {
        CheckedFunction1<Object, Object> f = (o1) -> "" + o1;
        CheckedFunction1<Object, Object> curried = f.curried();
        assertThat(curried.apply(1)).isEqualTo("1");
    }

    @Test
    public void shouldTuple() throws Exception {
        CheckedFunction1<Object, Object> f = (o1) -> "" + o1;
        CheckedFunction1<Tuple1<Object>, Object> tupled = f.tupled();
        assertThat(tupled.apply(Tuple.of(1))).isEqualTo("1");
    }

    private static final CheckedFunction1<String, MessageDigest> digest = (s1) -> MessageDigest.getInstance(s1);

    @Test
    public void shouldRecover() {
        Function<String, MessageDigest> recover = digest.recover(throwable -> (s1) -> null);
        MessageDigest md5 = recover.apply("MD5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        assertThat(recover.apply("Unknown")).isNull();
    }

    @Test
    public void shouldRecoverNonNull() {
        Function<String, MessageDigest> recover = digest.recover(throwable -> null);
        MessageDigest md5 = recover.apply("MD5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
        Try<MessageDigest> unknown = Try.of(() -> recover.apply("Unknown"));
        assertThat(unknown).isNotNull();
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull().isInstanceOf(NullPointerException.class);
        assertThat(unknown.getCause().getMessage()).isEqualTo("CheckedFunction1.recover: recover returned null");
        assertThat(unknown.getCause().getCause())
                .isInstanceOf(java.security.NoSuchAlgorithmException.class)
                .hasMessage("Unknown MessageDigest not available");
    }

    @Test
    public void shouldNotHandFatalThrowableToRecover() {
        CheckedFunction1<String, MessageDigest> fatal = (s1) -> {
            throw new OutOfMemoryError("fatal");
        };
        Function<String, MessageDigest> recover = fatal.recover(throwable -> {
            throw new AssertionError("recover must not see a fatal throwable");
        });
        assertThrows(OutOfMemoryError.class, () -> recover.apply("MD5"));
    }

    @Test
    public void shouldHandNonFatalThrowableToRecover() {
        CheckedFunction1<String, MessageDigest> nonFatal = (s1) -> {
            throw new IllegalStateException("non-fatal");
        };
        Function<String, MessageDigest> recover = nonFatal.recover(throwable -> (s1) -> null);
        assertThat(recover.apply("MD5")).isNull();
    }

    @Test
    public void shouldUncheckedWork() {
        Function<String, MessageDigest> unchecked = digest.unchecked();
        MessageDigest md5 = unchecked.apply("MD5");
        assertThat(md5).isNotNull();
        assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.getDigestLength()).isEqualTo(16);
    }

    @Test
    public void shouldUncheckedThrowIllegalState() {
        assertThrows(NoSuchAlgorithmException.class, () -> {
            Function<String, MessageDigest> unchecked = digest.unchecked();
            unchecked.apply("Unknown"); // Look ma, we throw an undeclared checked exception!
        });
    }

    @Test
    public void shouldLiftTryPartialFunction() {
        Function<String, Try<MessageDigest>> liftTry = CheckedFunction1.liftTry(digest);
        Try<MessageDigest> md5 = liftTry.apply("MD5");
        assertThat(md5.isSuccess()).isTrue();
        assertThat(md5.get()).isNotNull();
        assertThat(md5.get().getAlgorithm()).isEqualToIgnoringCase("MD5");
        assertThat(md5.get().getDigestLength()).isEqualTo(16);
        Try<MessageDigest> unknown = liftTry.apply("Unknown");
        assertThat(unknown.isFailure()).isTrue();
        assertThat(unknown.getCause()).isNotNull();
        assertThat(unknown.getCause().getMessage()).isEqualToIgnoringCase("Unknown MessageDigest not available");
    }

    private static final CheckedFunction1<Integer, Integer> recurrent1 =
            (i1) -> i1 <= 0 ? i1 : CheckedFunction1Test.recurrent1.apply(i1 - 1) + 1;

    @Test
    public void shouldCalculatedRecursively() throws Exception {
        assertThat(recurrent1.apply(11)).isEqualTo(11);
        assertThat(recurrent1.apply(22)).isEqualTo(22);
    }

    @Test
    public void shouldComposeWithAndThen() throws Exception {
        CheckedFunction1<Object, Object> f = (o1) -> "" + o1;
        CheckedFunction1<Object, Object> after = o -> o + "!";
        CheckedFunction1<Object, Object> composed = f.andThen(after);
        assertThat(composed.apply(1)).isEqualTo("1!");
    }

    @Test
    public void shouldComposeWithBefore() throws Exception {
        CheckedFunction1<String, Integer> length = String::length;
        CheckedFunction1<Integer, String> repeat = n -> "x".repeat(n);
        assertThat(length.compose(repeat).apply(3)).isEqualTo(3);
    }

    @Nested
    class ComposeTests {

        @Test
        public void shouldCompose1() throws Exception {
            CheckedFunction1<String, String> concat = (String s1) -> s1;
            Function<String, String> toUpperCase = String::toUpperCase;
            assertThat(concat.compose1(toUpperCase).apply("xx")).isEqualTo("XX");
        }
    }

    @Test
    public void shouldNarrow() throws Exception {
        CheckedFunction1<Number, String> wideFunction = (o1) -> String.format("Numbers are: %s", o1);
        CheckedFunction1<Integer, CharSequence> narrowFunction = CheckedFunction1.narrow(wideFunction);

        assertThat(narrowFunction.apply(1)).isEqualTo("Numbers are: 1");
    }
}
