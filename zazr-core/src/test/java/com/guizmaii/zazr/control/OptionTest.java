package com.guizmaii.zazr.control;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OptionTest {

    @Nested
    class NarrowTests {
        @Test
        public void shouldNarrowOption() {
            final Option<Integer> option = Option.some(42);
            final Option<Number> narrow = Option.narrow(option);
            assertThat(narrow.get()).isEqualTo(42);
        }
    }

    @Nested
    class ConstructionTests {
        @Test
        public void shouldMapNullToNone() {
            assertThat(Option.ofNullable(null)).isEqualTo(Option.none());
        }

        @Test
        public void shouldMapNonNullToSome() {
            final Option<?> option = Option.some(new Object());
            assertThat(option.isDefined()).isTrue();
        }

        @Test
        public void shouldRejectNullInSome() {
            assertThatThrownBy(() -> Option.some(null)).isInstanceOf(NullPointerException.class).hasMessage("value is null");
            assertThatThrownBy(() -> new Option.Some<>(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldWrapIfTrue() {
            assertThat(Option.when(true, () -> 1)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldRejectNullIfTrue() {
            assertThatThrownBy(() -> Option.when(true, () -> null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldNotWrapIfFalse() {
            assertThat(Option.when(false, () -> null)).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotExecuteIfFalse() {
            assertThat(Option.when(false, () -> {
                throw new RuntimeException();
            })).isEqualTo(Option.none());
        }

        @Test
        public void shouldThrowExceptionOnWhenWithProvider() {
            assertThrows(NullPointerException.class, () -> assertThat(Option.when(false, (Supplier<?>) null)).isEqualTo(Option.none()));
        }

        @Test
        public void shouldWrapEmptyOptional() {
            assertThat(Option.ofOptional(Optional.empty())).isEqualTo(Option.none());
        }

        @Test
        public void shouldWrapSomeOptional() {
            assertThat(Option.ofOptional(Optional.of(1))).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldThrowExceptionOnNullOptional() {
            assertThrows(NullPointerException.class, () -> assertThat(Option.ofOptional(null)).isEqualTo(Option.none()));
        }

        @Test
        public void shouldNotBeIterable() {
            // design 3.2: for (x : option) and list.addAll(option) must not compile
            assertThat(Iterable.class.isAssignableFrom(Option.class)).isFalse();
        }
    }

    @Nested
    class CollectAllTests {
        @Test
        public void shouldConvertListOfNonEmptyOptionsToOptionOfList() {
            final java.util.List<Option<String>> options = Arrays.asList(Option.some("a"), Option.some("b"), Option.some("c"));
            final Option<Seq<String>> reducedOption = Option.collectAll(options);
            assertThat(reducedOption instanceof Option.Some).isTrue();
            assertThat(reducedOption.get().size()).isEqualTo(3);
            assertThat(reducedOption.get().mkString()).isEqualTo("abc");
        }

        @Test
        public void shouldConvertListOfEmptyOptionsToOptionOfList() {
            final java.util.List<Option<String>> options = Arrays.asList(Option.none(), Option.none(), Option.none());
            final Option<Seq<String>> option = Option.collectAll(options);
            assertThat(option instanceof Option.None).isTrue();
        }

        @Test
        public void shouldConvertListOfMixedOptionsToOptionOfList() {
            final java.util.List<Option<String>> options = Arrays.asList(Option.some("a"), Option.none(), Option.some("c"));
            final Option<Seq<String>> option = Option.collectAll(options);
            assertThat(option instanceof Option.None).isTrue();
        }
    }

    @Nested
    class ForEachIterableTests {
        @Test
        public void shouldForEachListOfNonEmptyOptionsToOptionOfList() {
            final java.util.List<String> options = Arrays.asList("a", "b", "c");
            final Option<Seq<String>> reducedOption = Option.forEach(options, Option::some);
            assertThat(reducedOption instanceof Option.Some).isTrue();
            assertThat(reducedOption.get().size()).isEqualTo(3);
            assertThat(reducedOption.get().mkString()).isEqualTo("abc");
        }

        @Test
        public void shouldForEachListOfEmptyOptionsToOptionOfList() {
            final java.util.List<Option<String>> options = Arrays.asList(Option.none(), Option.none(), Option.none());
            final Option<Seq<String>> option = Option.forEach(options, Function.identity());
            assertThat(option instanceof Option.None).isTrue();
        }

        @Test
        public void shouldForEachListOfMixedOptionsToOptionOfList() {
            final java.util.List<String> options = Arrays.asList("a", "b", "c");
            final Option<Seq<String>> option =
                Option.forEach(options, x -> x.equals("b") ? Option.none() : Option.some(x));
            assertThat(option instanceof Option.None).isTrue();
        }
    }

    @Nested
    class GetTests {
        @Test
        public void shouldSucceedOnGetWhenValueIsPresent() {
            assertThat(Option.some(1).get()).isEqualTo(1);
        }

        @Test
        public void shouldThrowOnGetWhenValueIsNotDefined() {
            assertThrows(NoSuchElementException.class, () -> Option.none().get());
        }
    }

    @Nested
    class OrelseTests {
        @Test
        public void shouldReturnSelfOnOrElseIfValueIsPresent() {
            final Option<Integer> opt = Option.some(42);
            assertThat(opt.orElse(Option.some(0))).isSameAs(opt);
        }

        @Test
        public void shouldReturnSelfOnOrElseSupplierIfValueIsPresent() {
            final Option<Integer> opt = Option.some(42);
            assertThat(opt.orElse(() -> Option.some(0))).isSameAs(opt);
        }

        @Test
        public void shouldReturnAlternativeOnOrElseIfValueIsNotDefined() {
            final Option<Integer> opt = Option.some(42);
            assertThat(Option.none().orElse(opt)).isSameAs(opt);
        }

        @Test
        public void shouldReturnAlternativeOnOrElseSupplierIfValueIsNotDefined() {
            final Option<Integer> opt = Option.some(42);
            assertThat(Option.none().orElse(() -> opt)).isSameAs(opt);
        }
    }

    @Nested
    class GetorelseTests {
        @Test
        public void shouldGetValueOnGetOrElseWhenValueIsPresent() {
            assertThat(Option.some(1).getOrElse(2)).isEqualTo(1);
        }

        @Test
        public void shouldGetAlternativeOnGetOrElseWhenValueIsNotDefined() {
            assertThat(Option.none().getOrElse(2)).isEqualTo(2);
        }

        @Test
        public void shouldAcceptNullAsAlternative() {
            assertThat(Option.<Integer>none().getOrElse((Integer) null)).isNull();
            assertThat(Option.some(1).getOrElse((Integer) null)).isEqualTo(1);
        }
    }

    @Nested
    class GetorelseSupplierTests {
        @Test
        public void shouldGetValueOnGetOrElseGetWhenValueIsPresent() {
            assertThat(Option.some(1).getOrElse(() -> 2)).isEqualTo(1);
        }

        @Test
        public void shouldGetAlternativeOnGetOrElseGetWhenValueIsNotDefined() {
            assertThat(Option.none().getOrElse(() -> 2)).isEqualTo(2);
        }

        @Test
        public void shouldNotInvokeSupplierWhenValueIsPresent() {
            assertThat(Option.some(1).getOrElse(() -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(1);
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            final Supplier<Integer> supplier = null;
            assertThrows(NullPointerException.class, () -> Option.<Integer>none().getOrElse(supplier));
        }
    }

    @Nested
    class GetorelsethrowTests {
        @Test
        public void shouldGetValueOnGetOrElseThrowWhenValueIsPresent() {
            assertThat(Option.some(1).getOrElseThrow(() -> new RuntimeException("none"))).isEqualTo(1);
        }

        @Test
        public void shouldThrowOnGetOrElseThrowWhenValueIsNotDefined() {
            assertThrows(RuntimeException.class, () -> Option.none().getOrElseThrow(() -> new RuntimeException("none")));
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            assertThrows(NullPointerException.class, () -> Option.none().getOrElseThrow(null));
        }
    }

    @Nested
    class GetornullTests {
        @Test
        public void shouldGetValueOnGetOrNullWhenValueIsPresent() {
            assertThat(Option.some(1).getOrNull()).isEqualTo(1);
        }

        @Test
        public void shouldGetNullOnGetOrNullWhenValueIsNotDefined() {
            assertThat(Option.none().getOrNull()).isNull();
        }
    }

    @Nested
    class IsdefinedTests {
        @Test
        public void shouldBePresentOnIsDefinedWhenValueIsDefined() {
            assertThat(Option.some(1).isDefined()).isTrue();
        }

        @Test
        public void shouldNotBePresentOnIsDefinedWhenValueIsNotDefined() {
            assertThat(Option.none().isDefined()).isFalse();
        }
    }

    @Nested
    class IsemptyTests {
        @Test
        public void shouldBeEmptyOnIsEmptyWhenValueIsEmpty() {
            assertThat(Option.none().isEmpty()).isTrue();
        }

        @Test
        public void shouldBePresentOnIsEmptyWhenValue() {
            assertThat(Option.some(1).isEmpty()).isFalse();
        }
    }

    @Nested
    class TapNoneTests {
        @Test
        public void shouldThrowNullPointerExceptionWhenNullTapNoneActionPassed() {
            try {
                final Option<String> none = Option.none();
                none.tapNone(null);
                Assertions.fail("No exception was thrown");
            } catch (NullPointerException exc) {
                assertThat(exc.getMessage()).isEqualTo("action is null");
            }
        }

        @Test
        public void shouldExecuteRunnableWhenOptionIsEmpty() {
            final AtomicBoolean state = new AtomicBoolean();
            final Option<?> option = Option.none().tapNone(() -> state.set(false));
            assertThat(state.get()).isFalse();
            assertThat(option).isSameAs(Option.none());
        }

        @Test
        public void shouldNotRunTapNoneActionOnSome() {
            try {
                final Option<String> none = Option.some("value");
                none.tapNone(() -> {
                    throw new RuntimeException("Exception from empty option!");
                });
            } catch (RuntimeException exc) {
                Assertions.fail("No exception should be thrown!");
            }
        }
    }

    @Nested
    class FilterTests {
        @Test
        public void shouldReturnSomeOnFilterWhenValueIsDefinedAndPredicateMatches() {
            assertThat(Option.some(1).filter(i -> i == 1)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldReturnNoneOnFilterWhenValueIsDefinedAndPredicateNotMatches() {
            assertThat(Option.some(1).filter(i -> i == 2)).isEqualTo(Option.none());
        }

        @Test
        public void shouldReturnNoneOnFilterWhenValueIsNotDefinedAndPredicateNotMatches() {
            assertThat(Option.<Integer> none().filter(i -> i == 1)).isEqualTo(Option.none());
        }
    }

    @Nested
    class MapTests {
        @Test
        public void shouldMapSome() {
            assertThat(Option.some(1).map(String::valueOf)).isEqualTo(Option.some("1"));
        }

        @Test
        public void shouldMapNone() {
            assertThat(Option.<Integer> none().map(String::valueOf)).isEqualTo(Option.none());
        }
    }

    @Nested
    class MapTry {
        @Test
        public void shouldMapTrySome() {
            assertThat(Option.some(1).mapTry(String::valueOf)).isEqualTo(Try.success("1"));
        }

        @Test
        public void shouldMapTryNone() {
            Try<String> result = Option.none().mapTry(String::valueOf);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause().getClass()).isEqualTo(NoSuchElementException.class);
            assertThat(result.getCause().getMessage()).isEqualTo("No value present");
        }

        @Test
        public void shouldMapTryCheckedException() {
            Try<Integer> result = Option.some("a")
                    .mapTry(this::checkedFunction);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause().getClass()).isEqualTo(Exception.class);
            assertThat(result.getCause().getMessage()).isEqualTo("message");
        }

        @Test
        public void shouldCaptureNullResultOfMapTryAsFailure() {
            assertThat(Option.some(1).mapTry(i -> null).getCause()).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Option.some(1).mapTry(null));
        }

        private Integer checkedFunction(String string) throws Exception {
            throw new Exception("message");
        }
    }

    @Nested
    class FlatmapTests {
        @Test
        public void shouldFlatMapSome() {
            assertThat(Option.some(1).flatMap(i -> Option.some(String.valueOf(i)))).isEqualTo(Option.some("1"));
        }

        @Test
        public void shouldFlatMapNone() {
            assertThat(Option.<Integer> none().flatMap(i -> Option.some(String.valueOf(i)))).isEqualTo(Option.none());
        }

        @Test
        public void shouldFlatMapToSome() {
            final Option<Integer> option = Option.some(2);
            assertThat(Option.some(1).flatMap(i -> option)).isEqualTo(Option.some(2));
        }

        @Test
        public void shouldFlatMapToNone() {
            final Option<Integer> option = Option.none();
            assertThat(Option.some(1).flatMap(i -> option)).isEqualTo(Option.none());
        }
    }

    @Nested
    class ContainsTests {
        @Test
        public void shouldContainTheValueOfSome() {
            assertThat(Option.some(1).contains(1)).isTrue();
        }

        @Test
        public void shouldNotContainAnotherValue() {
            assertThat(Option.some(1).contains(2)).isFalse();
        }

        @Test
        public void shouldNotContainNull() {
            assertThat(Option.some(1).contains(null)).isFalse();
        }

        @Test
        public void shouldNotContainAnythingInNone() {
            assertThat(Option.<Integer>none().contains(1)).isFalse();
            assertThat(Option.<Integer>none().contains(null)).isFalse();
        }
    }

    @Nested
    class ExistsTests {
        @Test
        public void shouldBeAwareOfPropertyThatHoldsExistsOfSome() {
            assertThat(Option.some(1).exists(i -> i == 1)).isTrue();
        }

        @Test
        public void shouldBeAwareOfPropertyThatNotHoldsExistsOfSome() {
            assertThat(Option.some(1).exists(i -> i == 2)).isFalse();
        }

        @Test
        public void shouldNotHoldPropertyExistsOfNone() {
            assertThat(Option.none().exists(e -> true)).isFalse();
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> Option.none().exists(null));
        }
    }

    @Nested
    class ForallTests {
        @Test
        public void shouldBeAwareOfPropertyThatHoldsForAllOfSome() {
            assertThat(Option.some(1).forAll(i -> i == 1)).isTrue();
        }

        @Test
        public void shouldBeAwareOfPropertyThatNotHoldsForAllOfSome() {
            assertThat(Option.some(1).forAll(i -> i == 2)).isFalse();
        }

        @Test // a property holds for all elements of no elements
        public void shouldHoldPropertyForAllOfNone() {
            assertThat(Option.none().forAll(e -> false)).isTrue();
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> Option.none().forAll(null));
        }
    }

    @Nested
    class ForEachConsumerTests {
        @Test
        public void shouldConsumePresentValueOnForEachWhenValueIsDefined() {
            final int[] actual = new int[] { -1 };
            Option.some(1).forEach(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(1);
        }

        @Test
        public void shouldNotConsumeAnythingOnForEachWhenValueIsNotDefined() {
            final int[] actual = new int[] { -1 };
            Option.<Integer> none().forEach(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(-1);
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> Option.none().forEach(null));
        }
    }

    @Nested
    class ToeitherTests {
        @Test
        public void shouldMakeRightOnSomeToEither() {
            assertThat(Option.some(5).toEither(() -> "bad")).isEqualTo(Either.right(5));
        }

        @Test
        public void shouldMakeLeftOnNoneToEither() {
            assertThat(Option.none().toEither(() -> "bad")).isEqualTo(Either.left("bad"));
        }

        @Test
        public void shouldNotInvokeSupplierOnSome() {
            assertThat(Option.some(5).toEither(() -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(Either.right(5));
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            assertThrows(NullPointerException.class, () -> Option.some(5).toEither(null));
        }

        @Test
        public void shouldRejectNullLeftSuppliedForNone() {
            // Left cannot hold null (design 3.9)
            assertThrows(NullPointerException.class, () -> Option.none().toEither(() -> null));
        }
    }

    @Nested
    class TotryTests {
        @Test
        public void shouldMakeSuccessOnSomeToTry() {
            assertThat(Option.some(5).toTry(IllegalStateException::new)).isEqualTo(Try.success(5));
        }

        @Test
        public void shouldMakeFailureOnNoneToTry() {
            final Exception x = new Exception("test");
            assertThat(Option.none().toTry(() -> x)).isEqualTo(Try.failure(x));
        }

        @Test
        public void shouldNotInvokeSupplierOnSome() {
            assertThat(Option.some(5).toTry(() -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(Try.success(5));
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            assertThrows(NullPointerException.class, () -> Option.some(5).toTry(null));
        }

        @Test
        public void shouldRejectNullCauseSuppliedForNone() {
            assertThrows(NullPointerException.class, () -> Option.none().toTry(() -> null));
        }

        @Test
        public void shouldRethrowFatalCauseSuppliedForNone() {
            // a Failure never holds a fatal throwable
            assertThrows(InterruptedException.class, () -> Option.none().toTry(InterruptedException::new));
        }
    }

    @Nested
    class TovalidationTests {
        @Test
        public void shouldMakeValidOnSomeToValidation() {
            assertThat(Option.some(5).toValidation(() -> "bad")).isEqualTo(Validation.valid(5));
        }

        @Test
        public void shouldMakeInvalidOnNoneToValidation() {
            assertThat(Option.none().toValidation(() -> "bad")).isEqualTo(Validation.invalid("bad"));
        }

        @Test
        public void shouldNotInvokeSupplierOnSome() {
            assertThat(Option.some(5).toValidation(() -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(Validation.valid(5));
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            assertThrows(NullPointerException.class, () -> Option.some(5).toValidation(null));
        }

        @Test
        public void shouldRejectNullErrorSuppliedForNone() {
            // Invalid cannot hold null (design 3.9)
            assertThrows(NullPointerException.class, () -> Option.none().toValidation(() -> null));
        }
    }

    @Nested
    class TovectorTests {
        @Test
        public void shouldConvertSomeToVector() {
            assertThat(Option.some(1).toVector()).isEqualTo(Vector.of(1));
        }

        @Test
        public void shouldConvertNoneToEmptyVector() {
            assertThat(Option.none().toVector()).isSameAs(Vector.empty());
        }
    }

    @Nested
    class TolistTests {
        @Test
        public void shouldConvertSomeToList() {
            assertThat(Option.some(1).toList()).isEqualTo(List.of(1));
        }

        @Test
        public void shouldConvertNoneToEmptyList() {
            assertThat(Option.none().toList()).isSameAs(List.empty());
        }
    }

    @Nested
    class TooptionalTests {
        @Test
        public void shouldConvertNoneToOptional() {
            final Option<Object> none = Option.none();
            assertThat(none.toOptional()).isEqualTo(Optional.empty());
        }

        @Test
        public void shouldConvertSomeToOptional() {
            final Option<Integer> some = Option.some(1);
            assertThat(some.toOptional()).isEqualTo(Optional.of(1));
        }
    }

    @Nested
    class StreamTests {
        @Test
        public void shouldStreamTheValueOfSome() {
            assertThat(Option.some(1).stream().collect(Collectors.toList())).containsExactly(1);
        }

        @Test
        public void shouldStreamNothingForNone() {
            assertThat(Option.none().stream().count()).isEqualTo(0L);
        }

        @Test
        public void shouldStreamSequentially() {
            assertThat(Option.some(1).stream().isParallel()).isFalse();
        }
    }

    @Nested
    class TapTests {
        @Test
        public void shouldConsumePresentValueOnTapWhenValueIsDefined() {
            final int[] actual = new int[] { -1 };
            final Option<Integer> testee = Option.some(1).tap(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(1);
            assertThat(testee).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldNotConsumeAnythingOnTapWhenValueIsNotDefined() {
            final int[] actual = new int[] { -1 };
            final Option<Integer> testee = Option.<Integer> none().tap(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(-1);
            assertThat(testee).isEqualTo(Option.none());
        }

        @Test
        public void shouldReturnTheSameInstance() {
            final Option<Integer> some = Option.some(1);
            assertThat(some.tap(i -> {})).isSameAs(some);
            assertThat(Option.none().tap(i -> {})).isSameAs(Option.none());
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> Option.some(1).tap(null));
        }
    }

    @Nested
    class EqualsTests {
        @Test
        public void shouldEqualNoneIfObjectIsSame() {
            final Option<?> none = Option.none();
            assertThat(none).isEqualTo(none);
        }

        @Test
        public void shouldEqualSomeIfObjectIsSame() {
            final Option<?> some = Option.some(1);
            assertThat(some).isEqualTo(some);
        }

        @Test
        public void shouldNotEqualNoneIfObjectIsNull() {
            assertThat(Option.none()).isNotNull();
        }

        @Test
        public void shouldNotEqualSomeIfObjectIsNull() {
            assertThat(Option.some(1)).isNotNull();
        }

        @Test
        public void shouldNotEqualNoneIfObjectIsOfDifferentType() {
            final Object none = Option.none();
            assertThat(none.equals(new Object())).isFalse();
        }

        @Test
        public void shouldNotEqualSomeIfObjectIsOfDifferentType() {
            final Object some = Option.some(1);
            assertThat(some.equals(new Object())).isFalse();
        }

        @Test
        public void shouldEqualSomeIfObjectsAreEquivalent() {
            assertThat(Option.some(1)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldNotEqualSomeIfObjectIsOfDifferentValue() {
            assertThat(Option.some(1)).isNotEqualTo(Option.some(2));
        }
    }

    @Nested
    class HashcodeTests {
        @Test
        public void shouldHashNone() {
            assertThat(Option.none().hashCode()).isEqualTo(new Option.None<>().hashCode());
        }

        @Test
        public void shouldHashSome() {
            assertThat(Option.some(1).hashCode()).isEqualTo(Objects.hashCode(1));
        }
    }

    @Nested
    class TostringTests {
        @Test
        public void shouldConvertSomeToString() {
            assertThat(Option.some(1).toString()).isEqualTo("Some(1)");
        }

        @Test
        public void shouldConvertNoneToString() {
            assertThat(Option.none().toString()).isEqualTo("None");
        }
    }

    @Nested
    class FoldTests {
        @Test
        public void foldStringToInt() {
            assertThat(Option.some("1").fold(() -> -1, Integer::valueOf)).isEqualTo(1);
            assertThat(Option.<String>none().fold(() -> -1, Integer::valueOf)).isEqualTo(-1);
        }

        @Test
        public void foldEither() {
            Either<String, Integer> right = Option.some(1).fold(() -> {
                throw new AssertionError("Must not happen");
            }, Either::right);
            Either<String, Integer> left = Option.<Integer>none().fold(() -> Either.left("Empty"), ignore -> {
                throw new AssertionError("Must not happen");
            });
            assertThat(right.get()).isEqualTo(1);
            assertThat(left.getLeft()).isEqualTo("Empty");
        }

        @Test
        public void shouldFoldToNull() {
            assertThat(Option.some(1).<Object>fold(() -> "none", i -> null)).isNull();
        }

        @Test
        public void shouldThrowOnNullArguments() {
            assertThrows(NullPointerException.class, () -> Option.some(1).fold(null, i -> i));
            assertThrows(NullPointerException.class, () -> Option.some(1).fold(() -> 1, null));
        }
    }

    @Nested
    class CollectTests {
        @Test
        public void shouldCollectSomeToSome() {
            assertThat(Option.some(2).collect(i -> Option.some(i * 10))).isEqualTo(Option.some(20));
        }

        @Test
        public void shouldCollectSomeToNone() {
            assertThat(Option.some(2).collect(i -> Option.none())).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotCallTheMapperOnNone() {
            assertThat(Option.<Integer>none().collect(i -> {
                throw new AssertionError("must not be called");
            })).isSameAs(Option.none());
        }

        @Test
        public void shouldCollectWithASwitchInsideTheLambda() {
            final Option<Object> shape = Option.some("circle");
            final Option<Integer> actual = shape.collect(s -> switch (s) {
                case String str -> Option.some(str.length());
                default -> Option.none();
            });
            assertThat(actual).isEqualTo(Option.some(6));
        }

        @Test
        public void shouldRejectANullOptionFromTheMapper() {
            assertThatThrownBy(() -> Option.some(1).collect(i -> null))
              .isInstanceOf(NullPointerException.class)
              .hasMessage("Option.collect: mapper returned null");
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Option.some(1).collect(null));
        }
    }

    @Nested
    class ZipTests {

        @Test
        public void shouldPairTwoSomes() {
            assertThat(Option.some(1).zip(Option.some("a"))).isEqualTo(Option.some(Tuple.of(1, "a")));
        }

        @Test
        public void shouldBeNoneWhenEitherSideIsNone() {
            assertThat(Option.some(1).zip(Option.none())).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zip(Option.some("a"))).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zip(Option.none())).isEqualTo(Option.none());
        }

        @Test
        public void shouldCombineWithZipWith() {
            final AtomicInteger calls = new AtomicInteger();
            assertThat(Option.some(1).zipWith(Option.some(2), (a, b) -> {
                calls.incrementAndGet();
                return a + b;
            })).isEqualTo(Option.some(3));
            assertThat(calls.get()).isEqualTo(1);
        }

        @Test
        public void shouldNotCallTheCombinerUnlessBothAreSome() {
            final BiFunction<Integer, Integer, Integer> notCalled = (_, _) -> {
                throw new AssertionError("must not be called");
            };
            assertThat(Option.some(1).zipWith(Option.none(), notCalled)).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zipWith(Option.some(2), notCalled)).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zipWith(Option.none(), notCalled)).isEqualTo(Option.none());
        }

        @Test
        public void shouldRejectANullCombinerResult() {
            assertThatThrownBy(() -> Option.some(1).zipWith(Option.some(2), (_, _) -> null))
              .isInstanceOf(NullPointerException.class)
              .hasMessage("Option.zipWith: f returned null");
        }

        @Test
        public void shouldKeepTheLeftValueWithZipLeft() {
            final Option<Integer> some = Option.some(1);
            assertThat(some.zipLeft(Option.some("a"))).isSameAs(some);
            assertThat(some.zipLeft(Option.none())).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zipLeft(Option.some("a"))).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zipLeft(Option.none())).isEqualTo(Option.none());
        }

        @Test
        public void shouldKeepTheRightValueWithZipRight() {
            final Option<String> some = Option.some("a");
            assertThat(Option.some(1).zipRight(some)).isSameAs(some);
            assertThat(Option.some(1).zipRight(Option.none())).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zipRight(some)).isEqualTo(Option.none());
            assertThat(Option.<Integer>none().zipRight(Option.none())).isEqualTo(Option.none());
        }

        @Test
        public void shouldRejectNulls() {
            final Option<Integer> some = Option.some(1);
            assertThatThrownBy(() -> some.zip(null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> some.zipWith(null, Integer::sum)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> some.zipWith(Option.some(2), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
            assertThatThrownBy(() -> some.zipLeft(null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
            assertThatThrownBy(() -> some.zipRight(null)).isInstanceOf(NullPointerException.class).hasMessage("that is null");
        }
    }
}
