package com.guizmaii.zazr.control;

import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Validation.Invalid;
import com.guizmaii.zazr.control.Validation.Valid;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidationTest {

    private static <A> Validation<String, A> invalid(String first, String... rest) {
        return Validation.invalidAll(NonEmptyVector.of(first, rest));
    }

    private static NonEmptyVector<String> errors(String first, String... rest) {
        return NonEmptyVector.of(first, rest);
    }

    private static Validation<String, Integer> parse(String s) {
        return Validation.of(() -> Integer.parseInt(s), _ -> "not a number: " + s);
    }

    @Test
    public void shouldNotBeIterable() {
        // design 3.2
        assertThat(Iterable.class.isAssignableFrom(Validation.class)).isFalse();
    }

    @Nested
    class ConstructorTests {

        @Test
        public void shouldCreateValid() {
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThat(valid).isEqualTo(new Valid<>(1));
            assertThat(valid.isValid()).isTrue();
            assertThat(valid.get()).isEqualTo(1);
        }

        @Test
        public void shouldCreateInvalidWithOneError() {
            final Validation<String, Integer> invalid = Validation.invalid("e");
            assertThat(invalid).isEqualTo(new Invalid<>(NonEmptyVector.single("e")));
            assertThat(invalid.isInvalid()).isTrue();
            assertThat(((Invalid<String, Integer>) invalid).errors()).isEqualTo(errors("e"));
        }

        @Test
        public void shouldCreateInvalidWithAllErrors() {
            final Validation<String, Integer> invalid = Validation.invalidAll(errors("a", "b"));
            assertThat(invalid).isEqualTo(new Invalid<>(errors("a", "b")));
            assertThat(((Invalid<String, Integer>) invalid).errors()).isEqualTo(errors("a", "b"));
        }

        @Test
        public void shouldCreateFromEither() {
            assertThat(Validation.fromEither(Either.right(1))).isEqualTo(Validation.valid(1));
            assertThat(Validation.fromEither(Either.left("e"))).isEqualTo(Validation.invalid("e"));
        }

        @Test
        public void shouldCreateFromOption() {
            assertThat(Validation.fromOption(Option.some(1), () -> "none")).isEqualTo(Validation.valid(1));
            assertThat(Validation.fromOption(Option.none(), () -> "none")).isEqualTo(Validation.invalid("none"));
        }

        @Test
        public void shouldNotCallTheSupplierOnSome() {
            assertThat(Validation.fromOption(Option.some(1), () -> {
                throw new AssertionError("must not be called");
            })).isEqualTo(Validation.valid(1));
        }

        @Test
        public void shouldCreateFromPredicate() {
            assertThat(Validation.fromPredicate(18, a -> a >= 18, a -> a + " is under 18")).isEqualTo(Validation.valid(18));
            assertThat(Validation.fromPredicate(17, a -> a >= 18, a -> a + " is under 18")).isEqualTo(Validation.invalid("17 is under 18"));
        }

        @Test
        public void shouldNotCallTheErrorFunctionWhenThePredicateHolds() {
            assertThat(Validation.fromPredicate(1, _ -> true, _ -> {
                throw new AssertionError("must not be called");
            })).isEqualTo(Validation.valid(1));
        }

        @Test
        public void shouldCreateFromTry() {
            final RuntimeException cause = new RuntimeException("boom");
            assertThat(Validation.fromTry(Try.success(1))).isEqualTo(Validation.valid(1));
            assertThat(Validation.fromTry(Try.failure(cause))).isEqualTo(Validation.invalid(cause));
        }

        @Test
        public void shouldCreateFromCallable() {
            assertThat(parse("42")).isEqualTo(Validation.valid(42));
            assertThat(parse("x")).isEqualTo(Validation.invalid("not a number: x"));
        }

        @Test
        public void shouldHandTheThrowableToOnError() {
            final RuntimeException cause = new RuntimeException("boom");
            final Validation<Throwable, Object> v = Validation.of(() -> {
                throw cause;
            }, t -> t);
            assertThat(v).isEqualTo(Validation.invalid(cause));
        }

        @Test
        public void shouldTreatANullResultAsANullPointerException() {
            final Validation<Class<?>, Object> v = Validation.of(() -> null, Throwable::getClass);
            assertThat(v).isEqualTo(Validation.invalid(NullPointerException.class));
        }

        @Test
        public void shouldRethrowFatalThrowables() {
            assertThatThrownBy(() -> Validation.of(() -> {
                throw new OutOfMemoryError();
            }, t -> t)).isInstanceOf(OutOfMemoryError.class);
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.valid(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> Validation.invalid(null)).isInstanceOf(NullPointerException.class).hasMessage("error is null");
            assertThatThrownBy(() -> Validation.invalidAll(null)).isInstanceOf(NullPointerException.class).hasMessage("errors is null");
            assertThatThrownBy(() -> Validation.fromEither(null)).isInstanceOf(NullPointerException.class).hasMessage("either is null");
            assertThatThrownBy(() -> Validation.fromOption(null, () -> "e")).isInstanceOf(NullPointerException.class).hasMessage("option is null");
            assertThatThrownBy(() -> Validation.fromOption(Option.some(1), null)).isInstanceOf(NullPointerException.class).hasMessage("ifNone is null");
            assertThatThrownBy(() -> Validation.fromOption(Option.none(), () -> null)).isInstanceOf(NullPointerException.class).hasMessage("error is null");
            assertThatThrownBy(() -> Validation.fromPredicate(null, _ -> true, _ -> "e")).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> Validation.fromPredicate(1, null, _ -> "e")).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
            assertThatThrownBy(() -> Validation.fromPredicate(1, _ -> true, null)).isInstanceOf(NullPointerException.class).hasMessage("ifFalse is null");
            assertThatThrownBy(() -> Validation.fromPredicate(1, _ -> false, _ -> null)).isInstanceOf(NullPointerException.class).hasMessage("error is null");
            assertThatThrownBy(() -> Validation.fromTry(null)).isInstanceOf(NullPointerException.class).hasMessage("t is null");
            assertThatThrownBy(() -> Validation.of(null, t -> t)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.of(() -> 1, null)).isInstanceOf(NullPointerException.class).hasMessage("onError is null");
            assertThatThrownBy(() -> Validation.of(() -> {
                throw new RuntimeException();
            }, _ -> null)).isInstanceOf(NullPointerException.class).hasMessage("error is null");
        }
    }

    @Nested
    class MapTests {

        @Test
        public void shouldMapTheValue() {
            assertThat(Validation.<String, Integer>valid(1).map(i -> i + 1)).isEqualTo(Validation.valid(2));
            final Validation<String, Integer> invalid = invalid("a");
            assertThat(invalid.map(i -> i + 1)).isSameAs(invalid);
        }

        @Test
        public void shouldMapEachError() {
            assertThat(ValidationTest.<Integer>invalid("a", "b").mapError(String::toUpperCase)).isEqualTo(invalid("A", "B"));
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThat(valid.mapError(String::toUpperCase)).isSameAs(valid);
        }

        @Test
        public void shouldMapTheErrorsAsAWhole() {
            assertThat(ValidationTest.<Integer>invalid("a", "b").mapErrorAll(es -> NonEmptyVector.single(es.mkString("+")))).isEqualTo(invalid("a+b"));
            assertThat(ValidationTest.<Integer>invalid("a", "b").mapErrorAll(NonEmptyVector::reverse)).isEqualTo(invalid("b", "a"));
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThat(valid.mapErrorAll(NonEmptyVector::reverse)).isSameAs(valid);
        }

        @Test
        public void shouldMapBothSides() {
            assertThat(Validation.<String, Integer>valid(1).mapBoth(String::toUpperCase, i -> i + 1)).isEqualTo(Validation.valid(2));
            assertThat(ValidationTest.<Integer>invalid("a", "b").mapBoth(String::toUpperCase, i -> i + 1)).isEqualTo(invalid("A", "B"));
        }

        @Test
        public void shouldRejectNullMapperResults() {
            assertThatThrownBy(() -> Validation.valid(1).map(_ -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> invalid("a").mapError(_ -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> invalid("a").mapErrorAll(_ -> null)).isInstanceOf(NullPointerException.class).hasMessage("errors is null");
            assertThatThrownBy(() -> Validation.valid(1).mapBoth(_ -> null, _ -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> invalid("a").mapBoth(_ -> null, _ -> null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.valid(1).map(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.valid(1).mapError(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.valid(1).mapErrorAll(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.valid(1).mapBoth(null, i -> i)).isInstanceOf(NullPointerException.class).hasMessage("errorMapper is null");
            assertThatThrownBy(() -> Validation.valid(1).mapBoth(e -> e, null)).isInstanceOf(NullPointerException.class).hasMessage("valueMapper is null");
        }
    }

    @Nested
    class EliminationTests {

        @Test
        public void shouldFold() {
            final int fromValid = Validation.<String, Integer>valid(1).fold(NonEmptyVector::size, i -> -i);
            final int fromInvalid = ValidationTest.<Integer>invalid("a", "b").fold(NonEmptyVector::size, i -> -i);
            assertThat(fromValid).isEqualTo(-1);
            assertThat(fromInvalid).isEqualTo(2);
        }

        @Test
        public void shouldGet() {
            assertThat(Validation.valid(1).get()).isEqualTo(1);
            assertThatThrownBy(() -> invalid("a").get()).isInstanceOf(NoSuchElementException.class).hasMessage("get() on Invalid");
        }

        @Test
        public void shouldGetOrElseAValue() {
            assertThat(Validation.<String, Integer>valid(1).getOrElse(2)).isEqualTo(1);
            assertThat(ValidationTest.<Integer>invalid("a").getOrElse(2)).isEqualTo(2);
            assertThat(ValidationTest.<Integer>invalid("a").getOrElse((Integer) null)).isNull();
        }

        @Test
        public void shouldGetOrElseAFunctionOfTheErrors() {
            assertThat(Validation.<String, Integer>valid(1).getOrElse(es -> {
                throw new AssertionError("must not be called");
            })).isEqualTo(1);
            assertThat(ValidationTest.<Integer>invalid("a", "b").getOrElse(NonEmptyVector::size)).isEqualTo(2);
        }

        @Test
        public void shouldGetOrElseThrow() {
            assertThat(Validation.<String, Integer>valid(1).getOrElseThrow(es -> new IllegalStateException(es.mkString()))).isEqualTo(1);
            assertThatThrownBy(() -> invalid("a", "b").getOrElseThrow(es -> new IllegalStateException(es.mkString(", ")))).isInstanceOf(IllegalStateException.class).hasMessage("a, b");
        }

        @Test
        public void shouldTapTheValue() {
            final java.util.List<Integer> seen = new ArrayList<>();
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThat(valid.tap(seen::add)).isSameAs(valid);
            assertThat(ValidationTest.<Integer>invalid("a").tap(seen::add)).isEqualTo(invalid("a"));
            assertThat(seen).containsExactly(1);
        }

        @Test
        public void shouldTapTheErrors() {
            final java.util.List<NonEmptyVector<String>> seen = new ArrayList<>();
            final Validation<String, Integer> invalid = invalid("a", "b");
            assertThat(invalid.tapError(seen::add)).isSameAs(invalid);
            assertThat(Validation.<String, Integer>valid(1).tapError(seen::add)).isEqualTo(Validation.valid(1));
            assertThat(seen).containsExactly(errors("a", "b"));
        }

        @Test
        public void shouldTellTheCase() {
            assertThat(Validation.valid(1).isValid()).isTrue();
            assertThat(Validation.valid(1).isInvalid()).isFalse();
            assertThat(invalid("a").isValid()).isFalse();
            assertThat(invalid("a").isInvalid()).isTrue();
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.valid(1).fold(null, i -> i)).isInstanceOf(NullPointerException.class).hasMessage("ifInvalid is null");
            assertThatThrownBy(() -> Validation.valid(1).fold(es -> es, null)).isInstanceOf(NullPointerException.class).hasMessage("ifValid is null");
            assertThatThrownBy(() -> Validation.valid(1).getOrElse((java.util.function.Function<NonEmptyVector<Object>, Integer>) null)).isInstanceOf(NullPointerException.class).hasMessage("other is null");
            assertThatThrownBy(() -> Validation.valid(1).getOrElseThrow(null)).isInstanceOf(NullPointerException.class).hasMessage("exceptionFunction is null");
            assertThatThrownBy(() -> Validation.valid(1).tap(null)).isInstanceOf(NullPointerException.class).hasMessage("action is null");
            assertThatThrownBy(() -> Validation.valid(1).tapError(null)).isInstanceOf(NullPointerException.class).hasMessage("action is null");
        }
    }

    @Nested
    class ConversionTests {

        @Test
        public void shouldConvertToEither() {
            assertThat(Validation.<String, Integer>valid(1).toEither()).isEqualTo(Either.right(1));
            assertThat(ValidationTest.<Integer>invalid("a", "b").toEither()).isEqualTo(Either.left(errors("a", "b")));
        }

        @Test
        public void shouldConvertToEitherWithMergedErrors() {
            assertThat(Validation.<String, Integer>valid(1).toEitherWith(es -> es.mkString(", "))).isEqualTo(Either.right(1));
            assertThat(ValidationTest.<Integer>invalid("a", "b").toEitherWith(es -> es.mkString(", "))).isEqualTo(Either.left("a, b"));
        }

        @Test
        public void shouldConvertToOption() {
            assertThat(Validation.valid(1).toOption()).isEqualTo(Option.some(1));
            assertThat(invalid("a").toOption()).isEqualTo(Option.none());
        }

        @Test
        public void shouldConvertToTry() {
            assertThat(Validation.<String, Integer>valid(1).toTry(es -> new IllegalStateException(es.mkString()))).isEqualTo(Try.success(1));
            final Try<Integer> failure = ValidationTest.<Integer>invalid("a", "b").toTry(es -> new IllegalStateException(es.mkString(", ")));
            assertThat(failure.isFailure()).isTrue();
            assertThat(failure.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("a, b");
        }

        @Test
        public void shouldConvertToVector() {
            assertThat(Validation.valid(1).toVector()).isEqualTo(Vector.of(1));
            assertThat(invalid("a").toVector()).isEqualTo(Vector.empty());
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.valid(1).toEitherWith(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> invalid("a").toEitherWith(_ -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Validation.valid(1).toTry(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> invalid("a").toTry(_ -> null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class ObjectTests {

        @Test
        public void shouldBeEqualByCase() {
            assertThat(Validation.valid(1)).isEqualTo(Validation.valid(1)).hasSameHashCodeAs(Validation.valid(1));
            assertThat(Validation.valid(1)).isNotEqualTo(Validation.valid(2));
            assertThat(invalid("a")).isEqualTo(invalid("a")).hasSameHashCodeAs(invalid("a"));
            assertThat(Validation.valid(1)).isNotEqualTo(invalid("1"));
        }

        @Test
        public void shouldBeOrderSensitiveOnTheErrors() {
            assertThat(invalid("a", "b")).isEqualTo(invalid("a", "b"));
            assertThat(invalid("a", "b")).isNotEqualTo(invalid("b", "a"));
            assertThat(invalid("a")).isNotEqualTo(invalid("a", "a"));
        }

        @Test
        public void shouldPrint() {
            assertThat(Validation.valid(1)).hasToString("Valid(1)");
            assertThat(invalid("a")).hasToString("Invalid(a)");
            assertThat(invalid("a", "b")).hasToString("Invalid(a, b)");
        }

        @Test
        public void shouldDeconstructInASwitch() {
            final Validation<String, Integer> valid = Validation.valid(1);
            final Validation<String, Integer> invalid = invalid("a", "b");
            for (Validation<String, Integer> v : java.util.List.of(valid, invalid)) {
                final String s = switch (v) {
                    case Valid(var value) -> "valid " + value;
                    case Invalid(var errors) -> "invalid " + errors.size() + ": " + errors.mkString(", ");
                };
                assertThat(s).isEqualTo(v == valid ? "valid 1" : "invalid 2: a, b");
            }
        }
    }
}
