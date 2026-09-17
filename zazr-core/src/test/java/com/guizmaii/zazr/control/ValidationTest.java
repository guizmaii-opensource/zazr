package com.guizmaii.zazr.control;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Validation.Invalid;
import com.guizmaii.zazr.control.Validation.Valid;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
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
    class ZipTests {

        @Test
        public void shouldPairTwoValids() {
            assertThat(Validation.<String, Integer>valid(1).zip(Validation.valid("a"))).isEqualTo(Validation.valid(Tuple.of(1, "a")));
        }

        @Test
        public void shouldKeepTheErrorsOfTheLeftInvalid() {
            assertThat(ValidationTest.<Integer>invalid("a").zip(Validation.valid(1))).isEqualTo(invalid("a"));
        }

        @Test
        public void shouldKeepTheErrorsOfTheRightInvalid() {
            assertThat(Validation.<String, Integer>valid(1).zip(invalid("b"))).isEqualTo(invalid("b"));
        }

        @Test
        public void shouldConcatenateTheErrorsOfTwoInvalidsInOrder() {
            assertThat(ValidationTest.<Integer>invalid("a").zip(invalid("b"))).isEqualTo(invalid("a", "b"));
            assertThat(ValidationTest.<Integer>invalid("b").zip(invalid("a"))).isEqualTo(invalid("b", "a"));
            assertThat(ValidationTest.<Integer>invalid("a", "b").zip(invalid("c", "d"))).isEqualTo(invalid("a", "b", "c", "d"));
        }

        @Test
        public void shouldReturnTheInvalidOperandItself() {
            final Validation<String, Integer> invalid = invalid("a");
            assertThat(invalid.zip(Validation.valid(1))).isSameAs(invalid);
            assertThat(Validation.<String, Integer>valid(1).zip(invalid)).isSameAs(invalid);
        }

        @Test
        public void shouldCombineWithZipWith() {
            assertThat(Validation.<String, Integer>valid(1).zipWith(Validation.valid(2), Integer::sum)).isEqualTo(Validation.valid(3));
            assertThat(ValidationTest.<Integer>invalid("a").zipWith(Validation.valid(2), Integer::sum)).isEqualTo(invalid("a"));
            assertThat(Validation.<String, Integer>valid(1).zipWith(invalid("b"), Integer::sum)).isEqualTo(invalid("b"));
            assertThat(ValidationTest.<Integer>invalid("a").zipWith(invalid("b"), Integer::sum)).isEqualTo(invalid("a", "b"));
        }

        @Test
        public void shouldNotCallTheCombinerUnlessBothAreValid() {
            assertThat(ValidationTest.<Integer>invalid("a").zipWith(Validation.valid(2), (_, _) -> {
                throw new AssertionError("must not be called");
            })).isEqualTo(invalid("a"));
        }

        @Test
        public void shouldRejectANullCombinerResult() {
            assertThatThrownBy(() -> Validation.valid(1).zipWith(Validation.valid(2), (_, _) -> null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
        }

        @Test
        public void shouldKeepTheLeftValueWithZipLeft() {
            assertThat(Validation.<String, Integer>valid(1).zipLeft(Validation.valid("a"))).isEqualTo(Validation.valid(1));
            assertThat(ValidationTest.<Integer>invalid("a").zipLeft(invalid("b"))).isEqualTo(invalid("a", "b"));
            assertThat(Validation.<String, Integer>valid(1).zipLeft(invalid("b"))).isEqualTo(invalid("b"));
        }

        @Test
        public void shouldKeepTheRightValueWithZipRight() {
            assertThat(Validation.<String, Integer>valid(1).zipRight(Validation.valid("a"))).isEqualTo(Validation.valid("a"));
            assertThat(ValidationTest.<Integer>invalid("a").zipRight(invalid("b"))).isEqualTo(invalid("a", "b"));
            assertThat(ValidationTest.<Integer>invalid("a").zipRight(Validation.valid("x"))).isEqualTo(invalid("a"));
        }

        @Test
        public void shouldZipWithAnEither() {
            assertThat(Validation.<String, Integer>valid(1).zip(Either.right("a"))).isEqualTo(Validation.valid(Tuple.of(1, "a")));
            assertThat(Validation.<String, Integer>valid(1).zip(Either.left("b"))).isEqualTo(invalid("b"));
            assertThat(ValidationTest.<Integer>invalid("a").zip(Either.right("x"))).isEqualTo(invalid("a"));
            assertThat(ValidationTest.<Integer>invalid("a").zip(Either.left("b"))).isEqualTo(invalid("a", "b"));
        }

        @Test
        public void shouldRejectNulls() {
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThatThrownBy(() -> valid.zip((Validation<String, Integer>) null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> valid.zip((Either<String, Integer>) null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> valid.zipWith(null, Integer::sum)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> valid.zipWith(Validation.valid(2), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> valid.zipLeft(null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> valid.zipRight(null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
        }
    }

    @Nested
    class CollectAllTests {

        @Test
        public void shouldCollectNothing() {
            assertThat(Validation.<String, Integer>collectAll(List.empty())).isEqualTo(Validation.valid(Vector.empty()));
        }

        @Test
        public void shouldCollectAllValids() {
            assertThat(Validation.collectAll(List.of(Validation.valid(1), Validation.valid(2), Validation.valid(3)))).isEqualTo(Validation.valid(Vector.of(1, 2, 3)));
        }

        @Test
        public void shouldReturnTheErrorsOfTheOneInvalid() {
            assertThat(Validation.collectAll(List.of(Validation.valid(1), invalid("a"), Validation.valid(3)))).isEqualTo(invalid("a"));
        }

        @Test
        public void shouldAccumulateTheErrorsOfEveryInvalidInOrder() {
            assertThat(Validation.collectAll(List.of(invalid("a", "b"), Validation.valid(2), invalid("c"), invalid("d")))).isEqualTo(invalid("a", "b", "c", "d"));
        }

        @Test
        public void shouldAcceptAJavaCollection() {
            assertThat(Validation.collectAll(java.util.List.of(Validation.valid(1), Validation.valid(2)))).isEqualTo(Validation.valid(Vector.of(1, 2)));
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.collectAll(null)).isInstanceOf(NullPointerException.class).hasMessage("validations is null");
            assertThatThrownBy(() -> Validation.collectAll(java.util.Arrays.asList(Validation.valid(1), null))).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void shouldValidateNothing() {
            assertThat(Validation.forEach(List.<String>empty(), ValidationTest::parse)).isEqualTo(Validation.valid(Vector.empty()));
        }

        @Test
        public void shouldValidateEveryElement() {
            assertThat(Validation.forEach(List.of("1", "2", "3"), ValidationTest::parse)).isEqualTo(Validation.valid(Vector.of(1, 2, 3)));
        }

        @Test
        public void shouldReturnTheErrorOfTheOneInvalidElement() {
            assertThat(Validation.forEach(List.of("1", "x", "3"), ValidationTest::parse)).isEqualTo(invalid("not a number: x"));
        }

        @Test
        public void shouldAccumulateTheErrorsOfEveryInvalidElementInOrder() {
            assertThat(Validation.forEach(List.of("x", "2", "y", "z"), ValidationTest::parse)).isEqualTo(invalid("not a number: x", "not a number: y", "not a number: z"));
        }

        @Test
        public void shouldCallTheFunctionForEveryElementEvenAfterAnInvalid() {
            final java.util.List<String> seen = new ArrayList<>();
            Validation.forEach(List.of("x", "2", "y"), s -> {
                seen.add(s);
                return parse(s);
            });
            assertThat(seen).containsExactly("x", "2", "y");
        }

        @Test
        public void shouldReturnANonEmptyVectorForANonEmptyVector() {
            final Validation<String, NonEmptyVector<Integer>> valid = Validation.forEach(NonEmptyVector.of("1", "2"), ValidationTest::parse);
            assertThat(valid).isEqualTo(Validation.valid(NonEmptyVector.of(1, 2)));
            assertThat(Validation.forEach(NonEmptyVector.of("x", "2", "y"), ValidationTest::parse)).isEqualTo(invalid("not a number: x", "not a number: y"));
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.forEach((Iterable<String>) null, ValidationTest::parse)).isInstanceOf(NullPointerException.class).hasMessage("values is null");
            assertThatThrownBy(() -> Validation.forEach((NonEmptyVector<String>) null, ValidationTest::parse)).isInstanceOf(NullPointerException.class).hasMessage("values is null");
            assertThatThrownBy(() -> Validation.forEach(List.of("1"), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.forEach(NonEmptyVector.of("1"), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.forEach(List.of("1"), _ -> null)).isInstanceOf(NullPointerException.class).hasMessage("f returned null");
        }
    }

    @Nested
    class PartitionTests {

        @Test
        public void shouldPartitionNothing() {
            assertThat(Validation.partition(List.<String>empty(), ValidationTest::parse)).isEqualTo(Tuple.of(Vector.empty(), Vector.empty()));
        }

        @Test
        public void shouldPutEveryValueOnTheRight() {
            assertThat(Validation.partition(List.of("1", "2"), ValidationTest::parse)).isEqualTo(Tuple.of(Vector.empty(), Vector.of(1, 2)));
        }

        @Test
        public void shouldPutTheOneErrorOnTheLeft() {
            assertThat(Validation.partition(List.of("1", "x", "2"), ValidationTest::parse)).isEqualTo(Tuple.of(Vector.of("not a number: x"), Vector.of(1, 2)));
        }

        @Test
        public void shouldFlattenTheErrorsOfEveryInvalidInOrder() {
            assertThat(Validation.partition(List.of(1, 2, 3, 4), i -> i % 2 == 0 ? Validation.valid(i) : invalid(i + " odd", i + " really odd")))
                    .isEqualTo(Tuple.of(Vector.of("1 odd", "1 really odd", "3 odd", "3 really odd"), Vector.of(2, 4)));
        }

        @Test
        public void shouldPutEveryErrorOnTheLeft() {
            assertThat(Validation.partition(List.of("x", "y"), ValidationTest::parse)).isEqualTo(Tuple.of(Vector.of("not a number: x", "not a number: y"), Vector.empty()));
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.partition(null, ValidationTest::parse)).isInstanceOf(NullPointerException.class).hasMessage("values is null");
            assertThatThrownBy(() -> Validation.partition(List.of("1"), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.partition(List.of("1"), _ -> null)).isInstanceOf(NullPointerException.class).hasMessage("f returned null");
        }
    }

    @Nested
    class OrElseTests {

        @Test
        public void shouldKeepAValidAndNotCallTheSupplier() {
            final Validation<String, Integer> valid = Validation.valid(1);
            assertThat(valid.orElse(() -> {
                throw new AssertionError("must not be called");
            })).isSameAs(valid);
        }

        @Test
        public void shouldReplaceAnInvalidWithTheAlternative() {
            assertThat(ValidationTest.<Integer>invalid("a").orElse(() -> Validation.valid(2))).isEqualTo(Validation.valid(2));
        }

        @Test
        public void shouldDropTheErrorsOfTheDiscardedSide() {
            assertThat(ValidationTest.<Integer>invalid("a").orElse(() -> invalid("b"))).isEqualTo(invalid("b"));
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.valid(1).orElse(null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> invalid("a").orElse(() -> null)).isInstanceOf(NullPointerException.class).hasMessage("that supplied null");
        }
    }

    @Nested
    class FlatMapTests {

        @Test
        public void shouldChainOnValid() {
            assertThat(Validation.<String, Integer>valid(1).flatMap(i -> Validation.valid(i + 1))).isEqualTo(Validation.valid(2));
            assertThat(Validation.<String, Integer>valid(1).flatMap(_ -> invalid("b"))).isEqualTo(invalid("b"));
        }

        @Test
        public void shouldShortCircuitWhereZipAccumulates() {
            final Validation<String, Integer> first = invalid("a");
            final AtomicInteger calls = new AtomicInteger();
            final Validation<String, Integer> chained = first.flatMap(_ -> {
                calls.incrementAndGet();
                return invalid("b");
            });
            // the second validation is not evaluated and its errors are not accumulated...
            assertThat(calls.get()).isZero();
            assertThat(chained).isEqualTo(invalid("a"));
            assertThat(chained).isSameAs(first);
            // ...whereas zip evaluates both and keeps both errors
            assertThat(first.zip(invalid("b"))).isEqualTo(invalid("a", "b"));
        }

        @Test
        public void shouldRunACrossFieldRuleAfterTheFieldsAreValidated() {
            final Validation<String, Integer> start = Validation.valid(1);
            final Validation<String, Integer> end = Validation.valid(3);
            final Validation<String, Integer> length = start.zip(end).flatMap(range -> range._1() < range._2() ? Validation.valid(range._2() - range._1()) : invalid("start after end"));
            assertThat(length).isEqualTo(Validation.valid(2));
            assertThat(end.zip(start).flatMap(range -> range._1() < range._2() ? Validation.valid(range._2() - range._1()) : invalid("start after end"))).isEqualTo(invalid("start after end"));
        }

        @Test
        public void shouldChainAnEitherStep() {
            assertThat(Validation.<String, Integer>valid(1).flatMapEither(i -> Either.right(i + 1))).isEqualTo(Validation.valid(2));
            assertThat(Validation.<String, Integer>valid(1).flatMapEither(_ -> Either.left("b"))).isEqualTo(invalid("b"));
        }

        @Test
        public void shouldShortCircuitTheEitherStep() {
            final Validation<String, Integer> first = invalid("a");
            assertThat(first.flatMapEither(_ -> {
                throw new AssertionError("must not be called");
            })).isSameAs(first);
        }

        @Test
        public void shouldRejectNulls() {
            assertThatThrownBy(() -> Validation.valid(1).flatMap(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.valid(1).flatMap(_ -> null)).isInstanceOf(NullPointerException.class).hasMessage("f returned null");
            assertThatThrownBy(() -> Validation.valid(1).flatMapEither(null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> Validation.valid(1).flatMapEither(_ -> null)).isInstanceOf(NullPointerException.class).hasMessage("f returned null");
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
