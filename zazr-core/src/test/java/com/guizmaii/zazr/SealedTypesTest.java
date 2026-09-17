package com.guizmaii.zazr;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.List.Cons;
import com.guizmaii.zazr.collection.List.Nil;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Either.Left;
import com.guizmaii.zazr.control.Either.Right;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Option.None;
import com.guizmaii.zazr.control.Option.Some;
import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.control.Try.Failure;
import com.guizmaii.zazr.control.Try.Success;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.control.Validation.Invalid;
import com.guizmaii.zazr.control.Validation.Valid;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The shapes design 3.1 and 3.9 promise: every sum type is a sealed interface over public nested records, so it is
 * eliminated with an exhaustive {@code switch} (no {@code default} anywhere below: the compiler proves every case
 * is covered), deconstructed with record patterns, and its cases follow the null policy and the record equality
 * contract. {@code None}, {@code Nil} and {@code Tuple0} are shared instances that a public record constructor can
 * duplicate, so they are compared with {@code ==} and with {@code equals}.
 */
public class SealedTypesTest {

    @Nested
    class SealedHierarchies {

        @Test
        public void shouldBeSealedOverTheirRecordCases() {
            assertThat(Option.class.isSealed()).isTrue();
            assertThat(Option.class.getPermittedSubclasses()).containsExactlyInAnyOrder(Some.class, None.class);
            assertThat(Either.class.isSealed()).isTrue();
            assertThat(Either.class.getPermittedSubclasses()).containsExactlyInAnyOrder(Left.class, Right.class);
            assertThat(Try.class.isSealed()).isTrue();
            assertThat(Try.class.getPermittedSubclasses()).containsExactlyInAnyOrder(Success.class, Failure.class);
            assertThat(Validation.class.isSealed()).isTrue();
            assertThat(Validation.class.getPermittedSubclasses()).containsExactlyInAnyOrder(Valid.class, Invalid.class);
            assertThat(List.class.isSealed()).isTrue();
            assertThat(List.class.getPermittedSubclasses()).containsExactlyInAnyOrder(Cons.class, Nil.class);
        }

        @Test
        public void shouldBeRecords() {
            for (Class<?> c : new Class<?>[] { Some.class, None.class, Left.class, Right.class, Success.class, Failure.class,
                    Valid.class, Invalid.class, Cons.class, Nil.class, Tuple0.class, Tuple1.class, Tuple2.class, Tuple8.class }) {
                assertThat(c.isRecord()).as(c.getSimpleName()).isTrue();
            }
        }
    }

    @Nested
    class OptionTests {

        private String describe(Option<Integer> option) {
            return switch (option) {
                case Some(var value) -> "Some(" + value + ")";
                case None() -> "None";
            };
        }

        private String label(Option<Tuple2<String, Integer>> option) {
            return switch (option) {
                case Some(Tuple2(var name, var count)) when count > 1 -> name + " x" + count;
                case Some(Tuple2(var name, _)) -> name;
                case None() -> "nothing";
            };
        }

        @Test
        public void shouldSwitchExhaustively() {
            assertThat(describe(Option.some(1))).isEqualTo("Some(1)");
            assertThat(describe(Option.none())).isEqualTo("None");
        }

        @Test
        public void shouldDeconstructNestedTupleWithGuard() {
            assertThat(label(Option.some(Tuple.of("apple", 3)))).isEqualTo("apple x3");
            assertThat(label(Option.some(Tuple.of("apple", 1)))).isEqualTo("apple");
            assertThat(label(Option.none())).isEqualTo("nothing");
        }

        @Test
        public void shouldMatchWithInstanceofPattern() {
            final Option<String> some = Option.some("x");
            assertThat(some instanceof Some(var value) && value.equals("x")).isTrue();
            assertThat(some instanceof None<?>).isFalse();
            assertThat(Option.none() instanceof None()).isTrue();
        }

        @Test
        public void shouldRejectNullInSome() {
            assertThatThrownBy(() -> new Some<>(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> Option.some(null)).isInstanceOf(NullPointerException.class);
            assertThat(Option.ofNullable(null)).isSameAs(Option.none());
            assertThat(Option.some(1)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldHonourRecordEqualityContract() {
            assertThat(new Some<>(1)).isEqualTo(Option.some(1)).hasSameHashCodeAs(Option.some(1));
            assertThat(new Some<>(1)).isNotEqualTo(new Some<>(2)).isNotEqualTo(Option.none());
            assertThat(new Some<>(1).value()).isEqualTo(1);
            assertThat(Option.some(1)).hasToString("Some(1)");
        }

        @Test
        public void shouldShareNone() {
            assertThat((Object) Option.<Integer>none()).isSameAs(Option.<String>none());
            assertThat(new None<>()).isEqualTo(Option.none()).hasSameHashCodeAs(Option.none());
            assertThat(Option.none()).isNotEqualTo(Option.some(1)).hasToString("None");
        }
    }

    @Nested
    class EitherTests {

        private String describe(Either<String, Integer> either) {
            return switch (either) {
                case Left(var error) -> "error: " + error;
                case Right(var value) when value < 0 -> "negative " + value;
                case Right(var value) -> "value " + value;
            };
        }

        @Test
        public void shouldSwitchExhaustively() {
            assertThat(describe(Either.left("boom"))).isEqualTo("error: boom");
            assertThat(describe(Either.right(-1))).isEqualTo("negative -1");
            assertThat(describe(Either.right(1))).isEqualTo("value 1");
        }

        @Test
        public void shouldMatchWithInstanceofPattern() {
            final Either<String, Integer> right = Either.right(1);
            assertThat(right instanceof Right(var value) && value == 1).isTrue();
            assertThat(right instanceof Left<?, ?>).isFalse();
        }

        @Test
        public void shouldRejectNullOnBothSides() {
            assertThatThrownBy(() -> new Left<>(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> new Right<>(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> Either.left(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Either.right(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldHonourRecordEqualityContract() {
            assertThat(new Right<>(1)).isEqualTo(Either.right(1)).hasSameHashCodeAs(Either.right(1));
            assertThat(new Left<>(1)).isEqualTo(Either.left(1)).hasSameHashCodeAs(Either.left(1));
            assertThat(new Left<>(1)).isNotEqualTo(new Right<>(1));
            assertThat(new Left<>("e").value()).isEqualTo("e");
            assertThat(new Right<>("v").value()).isEqualTo("v");
            assertThat(Either.left("e")).hasToString("Left(e)");
            assertThat(Either.right("v")).hasToString("Right(v)");
        }
    }

    @Nested
    class TryTests {

        private String describe(Try<Integer> t) {
            return switch (t) {
                case Success(var value) -> "ok " + value;
                case Failure(var cause) -> "failed: " + cause.getMessage();
            };
        }

        @Test
        public void shouldSwitchExhaustively() {
            assertThat(describe(Try.success(1))).isEqualTo("ok 1");
            assertThat(describe(Try.failure(new IllegalStateException("bad")))).isEqualTo("failed: bad");
        }

        @Test
        public void shouldMatchWithInstanceofPattern() {
            final Try<Integer> failure = Try.failure(new IllegalStateException("bad"));
            assertThat(failure instanceof Failure(var cause) && cause instanceof IllegalStateException).isTrue();
            assertThat(failure instanceof Success<?>).isFalse();
        }

        @Test
        public void shouldRejectNullInSuccessAndFailure() {
            assertThatThrownBy(() -> new Success<>(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> new Failure<>(null)).isInstanceOf(NullPointerException.class).hasMessage("cause is null");
            assertThatThrownBy(() -> Try.success(null)).isInstanceOf(NullPointerException.class);
            // a computation that yields null is captured, not thrown: every non-fatal outcome ends up in the Try
            assertThat(Try.of(() -> null).getCause()).isInstanceOf(NullPointerException.class);
            assertThat(Try.success(1).mapTry(i -> null).getCause()).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldRunToTheEmptyTuple() {
            assertThat(Try.run(() -> {})).isEqualTo(Try.success(Tuple.empty()));
            assertThat(Try.run(() -> {}).get()).isSameAs(Tuple0.instance());
            assertThat(Try.run(() -> {})).hasToString("Success(())");
        }

        @Test
        public void shouldEqualFailuresOnlyOnTheSameCause() {
            final RuntimeException cause = new RuntimeException("same");
            assertThat(new Failure<>(cause)).isEqualTo(Try.failure(cause)).hasSameHashCodeAs(Try.failure(cause));
            assertThat(new Failure<>(cause).cause()).isSameAs(cause);
            // same class, same message, same stack shape: still two exceptions
            assertThat(Try.failure(new RuntimeException("same"))).isNotEqualTo(Try.failure(new RuntimeException("same")));
            assertThat(Try.failure(cause)).isNotEqualTo(Try.success(cause));
        }

        @Test
        public void shouldHonourRecordEqualityContractForSuccess() {
            assertThat(new Success<>(1)).isEqualTo(Try.success(1)).hasSameHashCodeAs(Try.success(1));
            assertThat(new Success<>(1)).isNotEqualTo(new Success<>(2));
            assertThat(new Success<>(1).value()).isEqualTo(1);
            assertThat(Try.success(1)).hasToString("Success(1)");
        }
    }

    @Nested
    class ValidationTests {

        private String describe(Validation<String, Integer> v) {
            return switch (v) {
                case Valid(var value) -> "valid " + value;
                case Invalid(var error) -> "invalid " + error;
            };
        }

        @Test
        public void shouldSwitchExhaustively() {
            assertThat(describe(Validation.valid(1))).isEqualTo("valid 1");
            assertThat(describe(Validation.invalid("no"))).isEqualTo("invalid no");
        }

        @Test
        public void shouldMatchWithInstanceofPattern() {
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThat(valid instanceof Valid(var value) && value == 1).isTrue();
            assertThat(valid instanceof Invalid<?, ?>).isFalse();
        }

        @Test
        public void shouldRejectNullOnBothSides() {
            assertThatThrownBy(() -> new Valid<>(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> new Invalid<>(null)).isInstanceOf(NullPointerException.class).hasMessage("error is null");
            assertThatThrownBy(() -> Validation.valid(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Validation.invalid(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldHonourRecordEqualityContract() {
            assertThat(new Valid<>(1)).isEqualTo(Validation.valid(1)).hasSameHashCodeAs(Validation.valid(1));
            assertThat(new Invalid<>("e")).isEqualTo(Validation.invalid("e")).hasSameHashCodeAs(Validation.invalid("e"));
            assertThat(new Valid<>(1)).isNotEqualTo(new Invalid<>(1));
            assertThat(new Valid<>(1).value()).isEqualTo(1);
            assertThat(new Invalid<>("e").error()).isEqualTo("e");
            assertThat(Validation.valid(1)).hasToString("Valid(1)");
            assertThat(Validation.invalid("e")).hasToString("Invalid(e)");
        }
    }

    @Nested
    class ListTests {

        private int sum(List<Integer> list) {
            return switch (list) {
                case Cons(var head, var tail) -> head + sum(tail);
                case Nil() -> 0;
            };
        }

        private String describe(List<String> list) {
            return switch (list) {
                case Cons(var head, Nil()) -> "one: " + head;
                case Cons(var head, Cons(var second, _)) when head.equals(second) -> "starts twice with " + head;
                case Cons(var head, _) -> "starts with " + head;
                case Nil() -> "empty";
            };
        }

        @Test
        public void shouldSwitchExhaustivelyAndRecurse() {
            assertThat(sum(List.of(1, 2, 3))).isEqualTo(6);
            assertThat(sum(List.empty())).isEqualTo(0);
        }

        @Test
        public void shouldDeconstructNestedConsWithGuard() {
            assertThat(describe(List.of("a"))).isEqualTo("one: a");
            assertThat(describe(List.of("a", "a", "b"))).isEqualTo("starts twice with a");
            assertThat(describe(List.of("a", "b"))).isEqualTo("starts with a");
            assertThat(describe(List.empty())).isEqualTo("empty");
        }

        @Test
        public void shouldMatchWithInstanceofPattern() {
            final List<Integer> list = List.of(1, 2);
            assertThat(list instanceof Cons(var head, var tail) && head == 1 && tail.equals(List.of(2))).isTrue();
            assertThat(List.empty() instanceof Nil()).isTrue();
        }

        @Test
        public void shouldRejectNullTailAndNullHead() {
            assertThatThrownBy(() -> new Cons<>(1, null)).isInstanceOf(NullPointerException.class).hasMessage("tail is null");
            assertThatThrownBy(() -> new Cons<>(null, List.empty())).isInstanceOf(NullPointerException.class).hasMessage("List: element is null");
        }

        @Test
        public void shouldBuildFromRecordConstructorsAndWalkLength() {
            final List<Integer> list = new Cons<>(1, new Cons<>(2, new Cons<>(3, Nil.instance())));
            assertThat(list).isEqualTo(List.of(1, 2, 3)).hasSameHashCodeAs(List.of(1, 2, 3));
            assertThat(list.length()).isEqualTo(3);
            assertThat(list.tail().length()).isEqualTo(2);
            assertThat(list).hasToString("List(1, 2, 3)");
            assertThat(new Cons<>(1, List.empty()).tail()).isSameAs(List.empty());
        }

        @Test
        public void shouldKeepSeqEqualityAcrossImplementations() {
            // a List equals any Seq with the same elements, so equals and hashCode are not the record defaults
            assertThat(List.of(1, 2)).isEqualTo(Vector.of(1, 2)).hasSameHashCodeAs(Vector.of(1, 2));
            assertThat(List.empty()).isEqualTo(Vector.empty()).hasSameHashCodeAs(Vector.empty());
            assertThat(List.of(1, 2)).isNotEqualTo(List.of(2, 1));
        }

        @Test
        public void shouldShareNil() {
            assertThat((Object) List.<Integer>empty()).isSameAs(Nil.<String>instance());
            assertThat(new Nil<>()).isEqualTo(List.empty()).hasSameHashCodeAs(List.empty());
            assertThat(List.empty()).hasToString("List()");
        }
    }

    @Nested
    class NullMapperTests {

        // a function handed to map that returns null: the control types throw, Try captures, Lazy holds it

        @Test
        public void shouldThrowWhenMapperReturnsNull() {
            assertThatThrownBy(() -> Option.some(1).map(x -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Either.right(1).map(x -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Either.left(1).mapLeft(x -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Validation.valid(1).map(x -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Validation.invalid(1).mapError(x -> null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldCaptureNullMapperResultInTry() {
            assertThat(Try.success(1).map(x -> null).getCause()).isInstanceOf(NullPointerException.class);
            assertThat(Try.failure(new RuntimeException()).catchAll(t -> null).getCause()).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldHoldNullInLazy() {
            // Lazy is a value, not a container (design 3.11): it holds null and offers no conversion but get()
            final Lazy<Object> lazy = Lazy.of(() -> null);
            assertThat(lazy.get()).isNull();
            assertThat(lazy.map(x -> x).get()).isNull();
            assertThatThrownBy(() -> Option.some(lazy.get())).isInstanceOf(NullPointerException.class);
            assertThat(Option.ofNullable(lazy.get())).isSameAs(Option.none());
        }
    }

    @Nested
    class TupleTests {

        private String describe(Object o) {
            return switch (o) {
                case Tuple0() -> "()";
                case Tuple1(var a) -> "(" + a + ")";
                case Tuple2(var a, var b) -> "(" + a + ", " + b + ")";
                case Tuple3(var a, _, var c) -> "(" + a + ", _, " + c + ")";
                default -> "?";
            };
        }

        @Test
        public void shouldDeconstructWithRecordPatterns() {
            assertThat(describe(Tuple.empty())).isEqualTo("()");
            assertThat(describe(Tuple.of(1))).isEqualTo("(1)");
            assertThat(describe(Tuple.of(1, "a"))).isEqualTo("(1, a)");
            assertThat(describe(Tuple.of(1, 2, 3))).isEqualTo("(1, _, 3)");
            assertThat(describe("x")).isEqualTo("?");
        }

        @Test
        public void shouldDeconstructNestedTuples() {
            final Tuple2<Tuple2<Integer, Integer>, Option<String>> nested = Tuple.of(Tuple.of(1, 2), Option.some("s"));
            if (nested instanceof Tuple2(Tuple2(var a, var b), Some(var s))) {
                assertThat(a + b).isEqualTo(3);
                assertThat(s).isEqualTo("s");
            } else {
                throw new AssertionError("nested record pattern did not match");
            }
        }

        @Test
        public void shouldHonourRecordEqualityContract() {
            assertThat(new Tuple2<>(1, "a")).isEqualTo(Tuple.of(1, "a")).hasSameHashCodeAs(Tuple.of(1, "a"));
            assertThat(new Tuple2<>(1, "a")).isNotEqualTo(Tuple.of(1, "b")).isNotEqualTo(Tuple.of("a", 1));
            assertThat(Tuple.of(1, "a")._1()).isEqualTo(1);
            assertThat(Tuple.of(1, "a")._2()).isEqualTo("a");
            assertThat(Tuple.of(1, "a")).hasToString("(1, a)");
            assertThat(Tuple.of(1, "a").update2("b")).isEqualTo(Tuple.of(1, "b"));
        }

        @Test
        public void shouldAllowNullComponents() {
            final Tuple2<Object, Object> nulls = Tuple.of(null, null);
            assertThat(nulls._1()).isNull();
            assertThat(nulls).isEqualTo(new Tuple2<>(null, null)).hasToString("(null, null)");
        }

        @Test
        public void shouldShareTuple0() {
            assertThat(Tuple.empty() == Tuple0.instance()).isTrue();
            assertThat(new Tuple0()).isEqualTo(Tuple0.instance()).hasSameHashCodeAs(Tuple0.instance());
            assertThat(Tuple0.instance().arity()).isEqualTo(0);
            assertThat(Tuple0.instance()).hasToString("()");
        }
    }
}
