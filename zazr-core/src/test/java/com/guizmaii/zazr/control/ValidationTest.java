package com.guizmaii.zazr.control;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class ValidationTest {

    private static final String OK = "ok";
    private static final List<String> ERRORS = List.of("error1", "error2", "error3");

    @Test
    public void shouldNotBeIterable() {
        // design 3.2
        assertThat(Iterable.class.isAssignableFrom(Validation.class)).isFalse();
    }

    @Nested
    class GetTests {
        @Test
        public void shouldGetValid() {
            assertThat(Validation.valid(1).get()).isEqualTo(1);
        }

        @Test
        public void shouldThrowOnGetInvalid() {
            assertThrows(NoSuchElementException.class, () -> Validation.invalid("e").get());
        }
    }

    @Nested
    class IsEmptyTests {
        @Test
        public void shouldBeEmptyOnInvalid() {
            assertThat(Validation.invalid("e").isEmpty()).isTrue();
        }

        @Test
        public void shouldNotBeEmptyOnValid() {
            assertThat(Validation.valid(1).isEmpty()).isFalse();
        }
    }

    @Nested
    class GetOrElseTests {
        @Test
        public void shouldGetValueOnValid() {
            assertThat(Validation.valid(1).getOrElse(2)).isEqualTo(1);
            assertThat(Validation.valid(1).getOrElse(() -> 2)).isEqualTo(1);
        }

        @Test
        public void shouldGetAlternativeOnInvalid() {
            assertThat(Validation.<String, Integer>invalid("e").getOrElse(2)).isEqualTo(2);
            assertThat(Validation.<String, Integer>invalid("e").getOrElse(() -> 2)).isEqualTo(2);
        }

        @Test
        public void shouldAcceptNullAsAlternative() {
            assertThat(Validation.<String, Integer>invalid("e").getOrElse((Integer) null)).isNull();
        }

        @Test
        public void shouldNotInvokeSupplierOnValid() {
            assertThat(Validation.valid(1).getOrElse(() -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(1);
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            final Supplier<Integer> supplier = null;
            assertThrows(NullPointerException.class, () -> Validation.<String, Integer>invalid("e").getOrElse(supplier));
        }
    }

    @Nested
    class GetOrElseThrowTests {
        @Test
        public void shouldGetValueOnValid() {
            assertThat(Validation.valid(1).getOrElseThrow(() -> new IllegalStateException("x"))).isEqualTo(1);
        }

        @Test
        public void shouldThrowSuppliedExceptionOnInvalid() {
            assertThrows(IllegalStateException.class, () -> Validation.invalid("e").getOrElseThrow(() -> new IllegalStateException("x")));
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            assertThrows(NullPointerException.class, () -> Validation.invalid("e").getOrElseThrow(null));
        }
    }

    @Nested
    class GetOrNullTests {
        @Test
        public void shouldGetValueOnValid() {
            assertThat(Validation.valid(1).getOrNull()).isEqualTo(1);
        }

        @Test
        public void shouldGetNullOnInvalid() {
            assertThat(Validation.invalid("e").getOrNull()).isNull();
        }
    }

    @Nested
    class ContainsTests {
        @Test
        public void shouldContainTheValidValue() {
            assertThat(Validation.valid(1).contains(1)).isTrue();
            assertThat(Validation.valid(1).contains(2)).isFalse();
            assertThat(Validation.valid(1).contains(null)).isFalse();
        }

        @Test
        public void shouldNotContainTheError() {
            assertThat(Validation.<Integer, Integer>invalid(1).contains(1)).isFalse();
        }
    }

    @Nested
    class ExistsTests {
        @Test
        public void shouldTestTheValidValue() {
            assertThat(Validation.valid(1).exists(i -> i == 1)).isTrue();
            assertThat(Validation.valid(1).exists(i -> i == 2)).isFalse();
        }

        @Test
        public void shouldNotHoldOnInvalid() {
            assertThat(Validation.invalid("e").exists(i -> true)).isFalse();
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> Validation.invalid("e").exists(null));
        }
    }

    @Nested
    class ForAllTests {
        @Test
        public void shouldTestTheValidValue() {
            assertThat(Validation.valid(1).forAll(i -> i == 1)).isTrue();
            assertThat(Validation.valid(1).forAll(i -> i == 2)).isFalse();
        }

        @Test
        public void shouldHoldVacuouslyOnInvalid() {
            assertThat(Validation.invalid("e").forAll(i -> false)).isTrue();
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> Validation.invalid("e").forAll(null));
        }
    }

    @Nested
    class ValidationValidTests {
        @Test
        public void shouldCreateSuccessWhenCallingValidationSuccess() {
            assertThat(Validation.valid(1) instanceof Validation.Valid).isTrue();
        }
    }

    @Nested
    class ValidationInvalidTests {
        @Test
        public void shouldCreateFailureWhenCallingValidationFailure() {
            assertThat(Validation.invalid("error") instanceof Validation.Invalid).isTrue();
        }
    }

    @Nested
    class ValidationFromeitherTests {
        @Test
        public void shouldCreateFromRightEither() {
            Validation<String, Integer> validation = Validation.fromEither(Either.right(42));
            assertThat(validation.isValid()).isTrue();
            assertThat(validation.get()).isEqualTo(42);
        }

        @Test
        public void shouldCreateFromLeftEither() {
            Validation<String, Integer> validation = Validation.fromEither(Either.left("vavr"));
            assertThat(validation.isValid()).isFalse();
            assertThat(validation.getError()).isEqualTo("vavr");
        }
    }

    @Nested
    class ValidationFromtryTests {
        @Test
        public void shouldCreateFromSuccessTry() {
            Validation<Throwable, Integer> validation = Validation.fromTry(Try.success(42));
            assertThat(validation.isValid()).isTrue();
            assertThat(validation.get()).isEqualTo(42);
        }

        @Test
        public void shouldCreateFromFailureTry() {
            Throwable throwable = new Throwable("vavr");
            Validation<Throwable, Integer> validation = Validation.fromTry(Try.failure(throwable));
            assertThat(validation.isValid()).isFalse();
            assertThat(validation.getError()).isEqualTo(throwable);
        }
    }

    // -- Validation.narrow

    @Test
    public void shouldNarrowValid() {
        Validation<String, Integer> validation = Validation.valid(42);
        Validation<CharSequence, Number> narrow = Validation.narrow(validation);
        assertThat(narrow.get()).isEqualTo(42);
    }

    @Test
    public void shouldNarrowInvalid() {
        Validation<String, Integer> validation = Validation.invalid("vavr");
        Validation<CharSequence, Number> narrow = Validation.narrow(validation);
        assertThat(narrow.getError()).isEqualTo("vavr");
    }

    @Nested
    public class FromPredicateTests {

        @Test
        public void shouldReturnValidWhenPredicateHolds() {
            Validation<String, Integer> validation = Validation.fromPredicate(21, i -> i > 18, () -> "vavr");
            assertThat(validation).isEqualTo(Validation.valid(21));
        }

        @Test
        public void shouldReturnInvalidWhenPredicateFails() {
            Validation<String, Integer> validation = Validation.fromPredicate(12, i -> i > 18, () -> "vavr");
            assertThat(validation).isEqualTo(Validation.invalid("vavr"));
        }

        @Test
        public void shouldNotEvaluateErrorSupplierWhenPredicateHolds() {
            Validation<String, Integer> validation = Validation.fromPredicate(21, i -> true, () -> {
                fail("Should not be called");
                return "vavr";
            });
            assertThat(validation).isEqualTo(Validation.valid(21));
        }

        @Test
        public void shouldThrowWhenErrorSupplierReturnsNull() {
            assertThrows(NullPointerException.class, () -> Validation.fromPredicate(1, i -> false, () -> null));
        }

        private class Car {
            String name;

            Car(String name) {
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Car)) return false;
                Car other = (Car) o;
                return name.equals(other.name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }
        }

        private class Hatchback extends Car {
            Hatchback(String name) {
                super(name);
            }
        }

        private class Sedan extends Car {
            Sedan(String name) {
                super(name);
            }
        }

        @Test
        public void shouldBeFineWithCovariantError() {
            Validation<Car, Integer> validation = Validation.fromPredicate(21, i -> false, () -> new Hatchback("vavr"));
            assertThat(validation).isEqualTo(Validation.invalid(new Hatchback("vavr")));
        }

        @Test
        public void shouldBeFineWithCovariantValid() {
            Validation<String, Car> validation = Validation.fromPredicate(new Sedan("vavr"), c -> true, () -> "vavr");
            assertThat(validation).isEqualTo(Validation.valid(new Sedan("vavr")));
        }

        @Test
        public void shouldThrowWhenProvidedWithNull() {
            assertThrows(NullPointerException.class, () -> Validation.fromPredicate(null, i -> true, () -> "vavr"));
            assertThrows(NullPointerException.class, () -> Validation.fromPredicate(1, null, () -> "vavr"));
            assertThrows(NullPointerException.class, () -> Validation.fromPredicate(1, i -> true, null));
        }
    }

    @Nested
    class CollectAllTests {
        @Test
        public void shouldThrowWhenCollectingAllOfNull() {
            assertThrows(NullPointerException.class, () ->Validation.collectAll(null));
        }

        @Test
        public void shouldCreateValidWhenCollectingAllValids() {
            final Validation<Seq<String>, Seq<Integer>> actual = Validation.collectAll(List.of(
                    Validation.valid(1),
                    Validation.valid(2)
            ));
            assertThat(actual).isEqualTo(Validation.valid(List.of(1, 2)));
        }

        @Test
        public void shouldCreateInvalidWhenCollectingAllAnInvalid() {
            final Validation<Seq<String>, Seq<Integer>> actual = Validation.collectAll(List.of(
                    Validation.valid(1),
                    Validation.invalid(List.of("error1", "error2")),
                    Validation.valid(2),
                    Validation.invalid(List.of("error3", "error4"))
            ));
            assertThat(actual).isEqualTo(Validation.invalid(List.of("error1", "error2", "error3", "error4")));
        }
    }

    @Nested
    class ForEachTests {
        @Test
        public void shouldThrowWhenForEachOfNull() {
            assertThrows(NullPointerException.class, () ->Validation.forEach(null, null));
        }

        @Test
        public void shouldCreateValidWhenForEachValids() {
            final Validation<Seq<String>, Seq<Integer>> actual =
                Validation.forEach(List.of(1, 2), t -> Validation.valid(t)); // NOTE: Compilation error with Java 8 if we use a method reference
            assertThat(actual).isEqualTo(Validation.valid(List.of(1, 2)));
        }

        @Test
        public void shouldCreateInvalidWhenForEachAnInvalid() {
            final Validation<Seq<String>, Seq<Integer>> actual =
                Validation.forEach(
                    List.of(1, -1, 2, -2),
                    x -> x >= 0
                        ? Validation.valid(x)
                        : Validation.invalid(List.of("error" + x, "error" + (x+1))));
            assertThat(actual).isEqualTo(Validation.invalid(List.of("error-1", "error0", "error-2", "error-1")));
        }
    }

    @Nested
    class ToeitherTests {
        @Test
        public void shouldConvertToRightEither() {
            Either<?, Integer> either = Validation.valid(42).toEither();
            assertThat(either.isRight()).isTrue();
            assertThat(either.get()).isEqualTo(42);
        }

        @Test
        public void shouldConvertToLeftEither() {
            Either<String, ?> either = Validation.invalid("vavr").toEither();
            assertThat(either.isLeft()).isTrue();
            assertThat(either.getLeft()).isEqualTo("vavr");
        }
    }

    @Nested
    class ToeitherwithTests {
        @Test
        public void shouldConvertValidToRightWithoutApplyingTheMapper() {
            assertThat(Validation.<String, Integer>valid(42).toEitherWith(e -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(Either.right(42));
        }

        @Test
        public void shouldConvertInvalidToLeftOfTheMappedError() {
            assertThat(Validation.<String, Integer>invalid("vavr").toEitherWith(String::length)).isEqualTo(Either.left(4));
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Validation.valid(42).toEitherWith(null));
        }

        @Test
        public void shouldRejectNullLeftBuiltForInvalid() {
            // Left cannot hold null (design 3.9)
            assertThrows(NullPointerException.class, () -> Validation.invalid("vavr").toEitherWith(e -> null));
        }
    }

    @Nested
    class TooptionTests {
        @Test
        public void shouldConvertValidToSome() {
            assertThat(Validation.valid(42).toOption()).isEqualTo(Option.some(42));
        }

        @Test
        public void shouldConvertInvalidToNone() {
            // the error is dropped
            assertThat(Validation.invalid("vavr").toOption()).isSameAs(Option.none());
        }
    }

    @Nested
    class TotryTests {
        @Test
        public void shouldConvertValidToSuccessWithoutApplyingTheMapper() {
            assertThat(Validation.<String, Integer>valid(42).toTry(e -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(Try.success(42));
        }

        @Test
        public void shouldConvertInvalidToFailureOfTheMappedError() {
            final Try<Integer> result = Validation.<String, Integer>invalid("vavr").toTry(IllegalStateException::new);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("vavr");
        }

        @Test
        public void shouldConvertInvalidOfThrowableWithTheIdentity() {
            final RuntimeException cause = new RuntimeException("boom");
            assertThat(Validation.<RuntimeException, Integer>invalid(cause).toTry(t -> t).getCause()).isSameAs(cause);
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Validation.valid(42).toTry(null));
        }

        @Test
        public void shouldRejectNullCauseBuiltForInvalid() {
            assertThrows(NullPointerException.class, () -> Validation.invalid("vavr").toTry(e -> null));
        }

        @Test
        public void shouldRethrowFatalCauseBuiltForInvalid() {
            assertThrows(InterruptedException.class, () -> Validation.invalid("vavr").toTry(InterruptedException::new));
        }
    }

    @Nested
    class TovectorTests {
        @Test
        public void shouldConvertValidToVectorOfOne() {
            assertThat(Validation.valid(42).toVector()).isEqualTo(Vector.of(42));
        }

        @Test
        public void shouldConvertInvalidToEmptyVector() {
            assertThat(Validation.invalid("vavr").toVector()).isSameAs(Vector.empty());
        }
    }

    @Nested
    class FilterTests {
        @Test
        public void shouldFilterValid() {
            Validation<String, Integer> valid = Validation.valid(42);
            assertThat(valid.filter(i -> true).get()).isSameAs(valid);
            assertThat(valid.filter(i -> false)).isSameAs(Option.none());
        }

        @Test
        public void shouldFilterInvalid() {
            Validation<String, Integer> invalid = Validation.invalid("vavr");
            assertThat(invalid.filter(i -> true).get()).isSameAs(invalid);
            assertThat(invalid.filter(i -> false).get()).isSameAs(invalid);
        }
    }

    @Nested
    class FlatmapTests {
        @Test
        public void shouldFlatMapValid() {
            Validation<String, Integer> valid = Validation.valid(42);
            assertThat(valid.flatMap(v -> Validation.valid("ok")).get()).isEqualTo("ok");
        }

        @Test
        public void shouldFlatMapInvalid() {
            Validation<String, Integer> invalid = Validation.invalid("vavr");
            assertThat(invalid.flatMap(v -> Validation.valid("ok"))).isSameAs(invalid);
        }
    }

    // -- orElse

    @Test
    public void shouldReturnSelfOnOrElseIfValid() {
        Validation<Seq<String>, String> validValidation = valid();
        assertThat(validValidation.orElse(invalid())).isSameAs(validValidation);
    }

    @Test
    public void shouldReturnSelfOnOrElseSupplierIfValid() {
        Validation<Seq<String>, String> validValidation = valid();
        assertThat(validValidation.orElse(this::invalid)).isSameAs(validValidation);
    }

    @Test
    public void shouldReturnAlternativeOnOrElseIfValid() {
        Validation<Seq<String>, String> validValidation = valid();
        assertThat(invalid().orElse(validValidation)).isSameAs(validValidation);
    }

    @Test
    public void shouldReturnAlternativeOnOrElseSupplierIfValid() {
        Validation<Seq<String>, String> validValidation = valid();
        assertThat(invalid().orElse(() -> validValidation)).isSameAs(validValidation);
    }

    @Nested
    class GetorelseFunctionTests {
        @Test
        public void shouldReturnValueOnGetOrElseFunctionIfValid() {
            Validation<Integer, String> validValidation = valid();
            assertThat(validValidation.getOrElse(e -> "error" + e)).isEqualTo(OK);
        }

        @Test
        public void shouldReturnCalculationOnGetOrElseFunctionIfInvalid() {
            Validation<Integer, String> invalidValidation = Validation.invalid(42);
            assertThat(invalidValidation.getOrElse(e -> "error" + e)).isEqualTo("error42");
        }
    }

    @Nested
    class FoldTests {
        @Test
        public void shouldConvertSuccessToU() {
            Validation<Seq<String>, String> validValidation = valid();
            Integer result = validValidation.fold(Seq::length, String::length);
            assertThat(result).isEqualTo(2);
        }

        @Test
        public void shouldConvertFailureToU() {
            Validation<Seq<String>, String> invalidValidation = invalid();
            Integer result = invalidValidation.fold(Seq::length, String::length);
            assertThat(result).isEqualTo(3);
        }
    }

    @Nested
    class FlipTests {
        @Test
        public void shouldFlipSuccessToFailure() {
            assertThat(valid().flip() instanceof Validation.Invalid).isTrue();
            assertThat(valid().flip().getError()).isEqualTo(OK);
        }

        @Test
        public void shouldFlipFailureToSuccess() {
            assertThat(invalid().flip() instanceof Validation.Valid).isTrue();
            assertThat(invalid().flip().get()).isEqualTo(ERRORS);
        }
    }

    @Nested
    class MapTests {
        @Test
        public void shouldMapSuccessValue() {
            assertThat(valid().map(s -> s + "!").get()).isEqualTo(OK + "!");
        }

        @Test
        public void shouldMapFailureError() {
            assertThat(invalid().map(s -> 2).getError()).isEqualTo(ERRORS);
        }

        @Test
        public void shouldMapFailureErrorOnGet() {
            assertThrows(RuntimeException.class, () -> assertThat(invalid().map(s -> 2).get()).isEqualTo(ERRORS));
        }
    }

    @Nested
    class MapBothTests {
        @Test
        public void shouldMapOnlySuccessValue() {
            Validation<Seq<String>, String> validValidation = valid();
            Validation<Integer, Integer> validMapping = validValidation.mapBoth(Seq::length, String::length);
            assertThat(validMapping instanceof Validation.Valid).isTrue();
            assertThat(validMapping.get()).isEqualTo(2);
        }

        @Test
        public void shouldMapOnlyFailureValue() {
            Validation<Seq<String>, String> invalidValidation = invalid();
            Validation<Integer, Integer> invalidMapping = invalidValidation.mapBoth(Seq::length, String::length);
            assertThat(invalidMapping instanceof Validation.Invalid).isTrue();
            assertThat(invalidMapping.getError()).isEqualTo(3);
        }
    }

    @Nested
    class MaperrorTests {
        @Test
        public void shouldNotMapSuccess() {
            assertThat(valid().mapError(x -> 2).get()).isEqualTo(OK);
        }

        @Test
        public void shouldMapFailure() {
            assertThat(invalid().mapError(x -> 5).getError()).isEqualTo(5);
        }
    }

    @Nested
    class ForeachTests {
        @Test
        public void shouldProcessFunctionInForEach() {

            { // Valid.forEach
                java.util.List<String> accumulator = new ArrayList<>();
                Validation<String, String> v1 = Validation.valid("valid");
                v1.forEach(accumulator::add);
                assertThat(accumulator.size()).isEqualTo(1);
                assertThat(accumulator.get(0)).isEqualTo("valid");
            }

            { // Invalid.forEach
                java.util.List<String> accumulator = new ArrayList<>();
                Validation<String, String> v2 = Validation.invalid("error");
                v2.forEach(accumulator::add);
                assertThat(accumulator.size()).isEqualTo(0);
            }
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> Validation.valid("x").forEach(null));
        }
    }

    @Nested
    class CombineAndApplyTests {
        @Test
        public void shouldBuildUpForSuccessCombine() {
            Validation<String, String> v1 = Validation.valid("John Doe");
            Validation<String, Integer> v2 = Validation.valid(39);
            Validation<String, Option<String>> v3 = Validation.valid(Option.some("address"));
            Validation<String, Option<String>> v4 = Validation.valid(Option.none());
            Validation<String, String> v5 = Validation.valid("111-111-1111");
            Validation<String, String> v6 = Validation.valid("alt1");
            Validation<String, String> v7 = Validation.valid("alt2");
            Validation<String, String> v8 = Validation.valid("alt3");
            Validation<String, String> v9 = Validation.valid("alt4");

            Validation<Seq<String>, TestValidation> result = v1.combine(v2).ap(TestValidation::new);

            Validation<Seq<String>, TestValidation> result2 = v1.combine(v2).combine(v3).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result3 = v1.combine(v2).combine(v4).ap(TestValidation::new);

            Validation<Seq<String>, TestValidation> result4 = v1.combine(v2).combine(v3).combine(v5).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result5 = v1.combine(v2).combine(v3).combine(v5).combine(v6).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result6 = v1.combine(v2).combine(v3).combine(v5).combine(v6).combine(v7).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result7 = v1.combine(v2).combine(v3).combine(v5).combine(v6).combine(v7).combine(v8).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result8 = v1.combine(v2).combine(v3).combine(v5).combine(v6).combine(v7).combine(v8).combine(v9).ap(TestValidation::new);

            Validation<Seq<String>, String> result9 = v1.combine(v2).combine(v3).ap((p1, p2, p3) -> p1 + ":" + p2 + ":" + p3.getOrElse("none"));

            assertThat(result.isValid()).isTrue();
            assertThat(result2.isValid()).isTrue();
            assertThat(result3.isValid()).isTrue();
            assertThat(result4.isValid()).isTrue();
            assertThat(result5.isValid()).isTrue();
            assertThat(result6.isValid()).isTrue();
            assertThat(result7.isValid()).isTrue();
            assertThat(result8.isValid()).isTrue();
            assertThat(result9.isValid()).isTrue();

            assertThat(result.get() instanceof TestValidation).isTrue();
            assertThat(result9.get() instanceof String).isTrue();
        }

        @Test
        public void shouldBuildUpForSuccessMapN() {
            Validation<String, String> v1 = Validation.valid("John Doe");
            Validation<String, Integer> v2 = Validation.valid(39);
            Validation<String, Option<String>> v3 = Validation.valid(Option.some("address"));
            Validation<String, Option<String>> v4 = Validation.valid(Option.none());
            Validation<String, String> v5 = Validation.valid("111-111-1111");
            Validation<String, String> v6 = Validation.valid("alt1");
            Validation<String, String> v7 = Validation.valid("alt2");
            Validation<String, String> v8 = Validation.valid("alt3");
            Validation<String, String> v9 = Validation.valid("alt4");

            // Alternative map(n) functions to the 'combine' function
            Validation<Seq<String>, TestValidation> result = Validation.combine(v1, v2).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result2 = Validation.combine(v1, v2, v3).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result3 = Validation.combine(v1, v2, v4).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result4 = Validation.combine(v1, v2, v3, v5).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result5 = Validation.combine(v1, v2, v3, v5, v6).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result6 = Validation.combine(v1, v2, v3, v5, v6, v7).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result7 = Validation.combine(v1, v2, v3, v5, v6, v7, v8).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result8 = Validation.combine(v1, v2, v3, v5, v6, v7, v8, v9).ap(TestValidation::new);

            Validation<Seq<String>, String> result9 = Validation.combine(v1, v2, v3).ap((p1, p2, p3) -> p1 + ":" + p2 + ":" + p3.getOrElse("none"));

            assertThat(result.isValid()).isTrue();
            assertThat(result2.isValid()).isTrue();
            assertThat(result3.isValid()).isTrue();
            assertThat(result4.isValid()).isTrue();
            assertThat(result5.isValid()).isTrue();
            assertThat(result6.isValid()).isTrue();
            assertThat(result7.isValid()).isTrue();
            assertThat(result8.isValid()).isTrue();
            assertThat(result9.isValid()).isTrue();

            assertThat(result.get() instanceof TestValidation).isTrue();
            assertThat(result9.get() instanceof String).isTrue();
        }

        @Test
        public void shouldBuildUpForFailure() {
            Validation<String, String> v1 = Validation.valid("John Doe");
            Validation<String, Integer> v2 = Validation.valid(39);
            Validation<String, Option<String>> v3 = Validation.valid(Option.some("address"));

            Validation<String, String> e1 = Validation.invalid("error2");
            Validation<String, Integer> e2 = Validation.invalid("error1");
            Validation<String, Option<String>> e3 = Validation.invalid("error3");

            Validation<Seq<String>, TestValidation> result = v1.combine(e2).combine(v3).ap(TestValidation::new);
            Validation<Seq<String>, TestValidation> result2 = e1.combine(v2).combine(e3).ap(TestValidation::new);

            assertThat(result.isInvalid()).isTrue();
            assertThat(result2.isInvalid()).isTrue();
        }

        @Test
        public void shouldThrowNullPointerExceptionWhenApplyingNullBiFunction() {
            Validation<String, String> e1 = Validation.invalid("error1");
            Validation<String, Integer> e2 = Validation.invalid("error2");
            assertThrows(NullPointerException.class, () -> e1.combine(e2).ap(null));
        }
    }

    // -- miscellaneous

    @Test
    public void shouldThrowErrorOnGetErrorValid() {
        assertThrows(RuntimeException.class, () -> {
            Validation<String, String> v1 = valid();
            v1.getError();
        });
    }

    @Test
    public void shouldMatchLikeObjects() {
        Validation<String, String> v1 = Validation.valid("test");
        Validation<String, String> v2 = Validation.valid("test");
        Validation<String, String> v3 = Validation.valid("test diff");

        Validation<String, String> e1 = Validation.invalid("error1");
        Validation<String, String> e2 = Validation.invalid("error1");
        Validation<String, String> e3 = Validation.invalid("error diff");

        assertThat(v1.equals(v1)).isTrue();
        assertThat(v1.equals(v2)).isTrue();
        assertThat(v1.equals(v3)).isFalse();

        assertThat(e1.equals(e1)).isTrue();
        assertThat(e1.equals(e2)).isTrue();
        assertThat(e1.equals(e3)).isFalse();
    }

    @Test
    public void shouldReturnCorrectStringForToString() {
        Validation<String, String> v1 = Validation.valid("test");
        Validation<String, String> v2 = Validation.invalid("error");

        assertThat(v1.toString()).isEqualTo("Valid(test)");
        assertThat(v2.toString()).isEqualTo("Invalid(error)");
    }

    @Test
    public void shouldReturnHashCode() {
        Validation<String, String> v1 = Validation.valid("test");
        Validation<String, String> e1 = Validation.invalid("error");

        assertThat(v1.hashCode()).isEqualTo(Objects.hashCode(v1));
        assertThat(e1.hashCode()).isEqualTo(Objects.hashCode(e1));
    }

    // ------------------------------------------------------------------------------------------ //

    private <E> Validation<E, String> valid() {
        return Validation.valid(OK);
    }

    private <T> Validation<Seq<String>, T> invalid() {
        return Validation.invalid(ERRORS);
    }

    static class TestValidation {
        String name;
        Integer age;
        Option<String> address;
        String phone;
        String alt1;
        String alt2;
        String alt3;
        String alt4;

        TestValidation(String name, Integer age) {
            this.name = name;
            this.age = age;
            address = Option.none();
        }

        TestValidation(String name, Integer age, Option<String> address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }

        TestValidation(String name, Integer age, Option<String> address, String phone) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.phone = phone;
        }

        TestValidation(String name, Integer age, Option<String> address, String phone, String alt1) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.phone = phone;
            this.alt1 = alt1;
        }

        TestValidation(String name, Integer age, Option<String> address, String phone, String alt1, String alt2) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.phone = phone;
            this.alt1 = alt1;
            this.alt2 = alt2;
        }

        TestValidation(String name, Integer age, Option<String> address, String phone, String alt1, String alt2, String alt3) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.phone = phone;
            this.alt1 = alt1;
            this.alt2 = alt2;
            this.alt3 = alt3;
        }

        TestValidation(String name, Integer age, Option<String> address, String phone, String alt1, String alt2, String alt3, String alt4) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.phone = phone;
            this.alt1 = alt1;
            this.alt2 = alt2;
            this.alt3 = alt3;
            this.alt4 = alt4;
        }

        @Override
        public String toString() {
            return "TestValidation(" + name + "," + age + "," + address.getOrElse("none") + phone + "," + ")";
        }
    }

    // -- Complete Validation example, may be moved to Vavr documentation later

    @Test
    public void shouldValidateValidPerson() {
        final String name = "John Doe";
        final int age = 30;
        final Validation<Seq<String>, Person> actual = new PersonValidator().validatePerson(name, age);
        final Validation<Seq<String>, Person> expected = Validation.valid(new Person(name, age));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldValidateInvalidPerson() {
        final String name = "John? Doe!4";
        final int age = -1;
        final Validation<Seq<String>, Person> actual = new PersonValidator().validatePerson(name, age);
        final Validation<Seq<String>, Person> expected = Validation.invalid(List.of(
                "Name contains invalid characters: '!4?'",
                "Age must be greater than 0"
        ));
        assertThat(actual).isEqualTo(expected);
    }

    static class PersonValidator {

        private final String validNameChars = "[a-zA-Z ]";
        private final int minAge = 0;

        Validation<Seq<String>, Person> validatePerson(String name, int age) {
            return Validation.combine(validateName(name), validateAge(age)).ap(Person::new);
        }

        private Validation<String, String> validateName(String name) {
            final String invalid = name.replaceAll(validNameChars, "");
            return invalid.isEmpty()
                    ? Validation.<String, String> valid(name)
                    : Validation.<String, String> invalid("Name contains invalid characters: '"
                            + Vector.ofAll(invalid.toCharArray()).distinct().sorted().mkString() + "'");
        }

        private Validation<String, Integer> validateAge(int age) {
            return (age < minAge) ? Validation.invalid("Age must be greater than 0")
                                  : Validation.valid(age);
        }
    }

    static class Person {

        final String name;
        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Person) {
                final Person person = (Person) o;
                return Objects.equals(name, person.name) && age == person.age;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "Person(" + name + ", " + age + ")";
        }
    }

    @Nested
    class TapTests {
        @Test
        public void shouldTapValid() {
            java.util.List<String> accumulator = new ArrayList<>();
            Validation.valid("hello").tap(accumulator::add);
            assertThat(accumulator).containsExactly("hello");
        }

        @Test
        public void shouldNotTapInvalid() {
            java.util.List<String> accumulator = new ArrayList<>();
            Validation.<String, String>invalid("error").tap(accumulator::add);
            assertThat(accumulator).isEmpty();
        }

        @Test
        public void shouldThrowOnNullActionWhenValid() {
            assertThrows(NullPointerException.class, () -> Validation.valid("hello").tap(null));
        }

        @Test
        public void shouldThrowOnNullActionWhenInvalid() {
            assertThrows(NullPointerException.class, () -> Validation.invalid("error").tap(null));
        }
    }
}
