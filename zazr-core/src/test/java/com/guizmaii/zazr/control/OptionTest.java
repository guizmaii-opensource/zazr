package com.guizmaii.zazr.control;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.AbstractValueTest;
import com.guizmaii.zazr.collection.Seq;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OptionTest extends AbstractValueTest {

    // -- AbstractValueTest

    @Override
    protected <T> Option<T> empty() {
        return Option.none();
    }

    @Override
    protected <T> Option<T> of(T element) {
        return Option.some(element);
    }

    @SafeVarargs
    @Override
    protected final <T> Option<T> of(T... elements) {
        return of(elements[0]);
    }

    @Override
    protected boolean allowsNull() {
        return false;
    }

    @Override
    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return true;
    }

    @Override
    protected int getPeekNonNilPerformingAnAction() {
        return 1;
    }

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
            assertThat(Option.when(true, 1)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldRejectNullIfTrue() {
            assertThatThrownBy(() -> Option.when(true, () -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Option.when(true, (Object) null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldNotWrapIfFalse() {
            assertThat(Option.when(false, () -> null)).isEqualTo(Option.none());
            assertThat(Option.when(false, (Object) null)).isEqualTo(Option.none());
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
    }

    @Nested
    class SequenceTests {
        @Test
        public void shouldConvertListOfNonEmptyOptionsToOptionOfList() {
            final List<Option<String>> options = Arrays.asList(Option.some("a"), Option.some("b"), Option.some("c"));
            final Option<Seq<String>> reducedOption = Option.sequence(options);
            assertThat(reducedOption instanceof Option.Some).isTrue();
            assertThat(reducedOption.get().size()).isEqualTo(3);
            assertThat(reducedOption.get().mkString()).isEqualTo("abc");
        }

        @Test
        public void shouldConvertListOfEmptyOptionsToOptionOfList() {
            final List<Option<String>> options = Arrays.asList(Option.none(), Option.none(), Option.none());
            final Option<Seq<String>> option = Option.sequence(options);
            assertThat(option instanceof Option.None).isTrue();
        }

        @Test
        public void shouldConvertListOfMixedOptionsToOptionOfList() {
            final List<Option<String>> options = Arrays.asList(Option.some("a"), Option.none(), Option.some("c"));
            final Option<Seq<String>> option = Option.sequence(options);
            assertThat(option instanceof Option.None).isTrue();
        }
    }

    @Nested
    class TraverseTests {
        @Test
        public void shouldTraverseListOfNonEmptyOptionsToOptionOfList() {
            final List<String> options = Arrays.asList("a", "b", "c");
            final Option<Seq<String>> reducedOption = Option.traverse(options, Option::some);
            assertThat(reducedOption instanceof Option.Some).isTrue();
            assertThat(reducedOption.get().size()).isEqualTo(3);
            assertThat(reducedOption.get().mkString()).isEqualTo("abc");
        }

        @Test
        public void shouldTraverseListOfEmptyOptionsToOptionOfList() {
            final List<Option<String>> options = Arrays.asList(Option.none(), Option.none(), Option.none());
            final Option<Seq<String>> option = Option.traverse(options, Function.identity());
            assertThat(option instanceof Option.None).isTrue();
        }

        @Test
        public void shouldTraverseListOfMixedOptionsToOptionOfList() {
            final List<String> options = Arrays.asList("a", "b", "c");
            final Option<Seq<String>> option =
                Option.traverse(options, x -> x.equals("b") ? Option.none() : Option.some(x));
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
    }

    @Nested
    class Getorelse2Tests {
        @Test
        public void shouldGetValueOnGetOrElseGetWhenValueIsPresent() {
            assertThat(Option.some(1).getOrElse(() -> 2)).isEqualTo(1);
        }

        @Test
        public void shouldGetAlternativeOnGetOrElseGetWhenValueIsNotDefined() {
            assertThat(Option.none().getOrElse(() -> 2)).isEqualTo(2);
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
    }

    @Nested
    class TojavaoptionalTests {
        @Test
        public void shouldConvertNoneToJavaOptional() {
            final Option<Object> none = Option.none();
            assertThat(none.toJavaOptional()).isEqualTo(Optional.empty());
        }

        @Test
        public void shouldConvertSomeToJavaOptional() {
            final Option<Integer> some = Option.some(1);
            assertThat(some.toJavaOptional()).isEqualTo(Optional.of(1));
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
    class OnemptyTests {
        @Test
        public void shouldThrowNullPointerExceptionWhenNullOnEmptyActionPassed() {
            try {
                final Option<String> none = Option.none();
                none.onEmpty(null);
                Assertions.fail("No exception was thrown");
            } catch (NullPointerException exc) {
                assertThat(exc.getMessage()).isEqualTo("action is null");
            }
        }

        @Test
        public void shouldExecuteRunnableWhenOptionIsEmpty() {
            final AtomicBoolean state = new AtomicBoolean();
            final Option<?> option = Option.none().onEmpty(() -> state.set(false));
            assertThat(state.get()).isFalse();
            assertThat(option).isSameAs(Option.none());
        }

        @Test
        public void shouldNotThrowExceptionIfOnEmptySetAndOptionIsSome() {
            try {
                final Option<String> none = Option.some("value");
                none.onEmpty(() -> {
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
        public void shouldFlatMapNonEmptyIterable() {
            final Option<Integer> option = Option.some(2);
            assertThat(Option.some(1).flatMap(i -> option)).isEqualTo(Option.some(2));
        }

        @Test
        public void shouldFlatMapEmptyIterable() {
            final Option<Integer> option = Option.none();
            assertThat(Option.some(1).flatMap(i -> option)).isEqualTo(Option.none());
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
        public void shouldNotHoldPropertyForAllOfNone() {
            assertThat(Option.none().forAll(e -> true)).isTrue();
        }
    }

    @Nested
    class ForeachTests {
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
    }

    @Nested
    class ToeitherTests {
        @Test
        public void shouldMakeRightOnSomeToEither() {
            assertThat(Option.some(5).toEither("bad")).isEqualTo(Either.right(5));
        }

        @Test
        public void shouldMakeLeftOnNoneToEither() {
            assertThat(Option.none().toEither("bad")).isEqualTo(Either.left("bad"));
        }

        @Test
        public void shouldMakeLeftOnNoneToEitherSupplier() {
            assertThat(Option.none().toEither(() -> "bad")).isEqualTo(Either.left("bad"));
        }
    }

    @Nested
    class TovalidationTests {
        @Test
        public void shouldMakeValidOnSomeToValidation() {
            assertThat(Option.some(5).toValidation("bad")).isEqualTo(Validation.valid(5));
        }

        @Test
        public void shouldMakeLeftOnNoneToValidation() {
            assertThat(Option.none().toValidation("bad")).isEqualTo(Validation.invalid("bad"));
        }

        @Test
        public void shouldMakeLeftOnNoneToValidationSupplier() {
            assertThat(Option.none().toValidation(() -> "bad")).isEqualTo(Validation.invalid("bad"));
        }
    }

    @Nested
    class PeekTests {
        @Test
        public void shouldConsumePresentValueOnPeekWhenValueIsDefined() {
            final int[] actual = new int[] { -1 };
            final Option<Integer> testee = Option.some(1).peek(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(1);
            assertThat(testee).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldNotConsumeAnythingOnPeekWhenValueIsNotDefined() {
            final int[] actual = new int[] { -1 };
            final Option<Integer> testee = Option.<Integer> none().peek(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(-1);
            assertThat(testee).isEqualTo(Option.none());
        }
    }

    @Nested
    class TransformTests {
        @Test
        public void shouldThrowExceptionOnNullTransformFunction() {
            assertThrows(NullPointerException.class, () -> Option.some(1).transform(null));
        }

        @Test
        public void shouldApplyTransformFunctionToSome() {
            final Option<Integer> option = Option.some(1);
            final Function<Option<Integer>, String> f = o -> o.get().toString().concat("-transformed");
            assertThat(option.transform(f)).isEqualTo("1-transformed");
        }

        @Test
        public void shouldHandleTransformOnNone() {
            assertThat(Option.none().<String> transform(self -> self.isEmpty() ? "ok" : "failed")).isEqualTo("ok");
        }
    }

    @Nested
    class IteratorTests {
        @Test
        public void shouldReturnIteratorOfSome() {
            assertThat((Iterator<Integer>) Option.some(1).iterator()).isNotNull();
        }

        @Test
        public void shouldReturnIteratorOfNone() {
            assertThat((Iterator<Object>) Option.none().iterator()).isNotNull();
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
    class SpliteratorTests {
        @Test
        public void shouldHaveSizedSpliterator() {
            assertThat(of(1).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED)).isTrue();
        }

        @Test
        public void shouldHaveOrderedSpliterator() {
            assertThat(of(1).spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
        }

        @Test
        public void shouldReturnSizeWhenSpliterator() {
            assertThat(of(1).spliterator().getExactSizeIfKnown()).isEqualTo(1);
        }

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
    }
}
