package com.guizmaii.zazr.control;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class EitherTest {

    private static <T> Either<String, T> left() {
        return Either.left("empty");
    }

    @Test
    public void shouldNotBeIterable() {
        // design 3.2: Either.iterator() silently skipped a Left; the type is no longer Iterable
        assertThat(Iterable.class.isAssignableFrom(Either.class)).isFalse();
    }

    @Test
    public void shouldReturnSameWhenCallingMapOnLeft() {
        final Either<Integer, Object> actual = Either.left(1);
        assertThat(actual.map(v -> {throw new IllegalStateException();})).isSameAs(actual);
    }

    @Test
    public void shouldThrowIfRightGetLeft() {
        assertThrows(NoSuchElementException.class, () -> Either.right(1).getLeft());
    }

    @Test
    public void shouldThrowIfLeftGet() {
        assertThrows(NoSuchElementException.class, () -> Either.left(1).get());
    }

    @Test
    public void shouldGetRight() {
        assertThat(Either.right(1).get()).isEqualTo(1);
    }

    @Test
    public void shouldSwapLeft() {
        assertThat(Either.left(1).swap()).isEqualTo(Either.right(1));
    }

    @Test
    public void shouldSwapRight() {
        assertThat(Either.right(1).swap()).isEqualTo(Either.left(1));
    }

    @Nested
    public class EitherTests {

        @Test
        public void shouldBimapLeft() {
            final Either<Integer, String> actual = Either.<Integer, String>left(1).bimap(i -> i + 1, s -> s + "1");
            final Either<Integer, String> expected = Either.left(2);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldBimapRight() {
            final Either<Integer, String> actual = Either.<Integer, String>right("1").bimap(i -> i + 1, s -> s + "1");
            final Either<Integer, String> expected = Either.right("11");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTransformRight() {
            final String value = Either.<String, Integer>right(1).transform(e -> e.fold(l -> "L:" + l, r -> "R:" + r));
            assertThat(value).isEqualTo("R:1");
        }

        @Test
        public void shouldTransformLeft() {
            final String value = Either.<String, Integer>left("error").transform(e -> e.fold(l -> "L:" + l, r -> "R:" + r));
            assertThat(value).isEqualTo("L:error");
        }

        @Test
        public void shouldFoldLeft() {
            final String value = Either.left("L").fold(l -> l + "+", r -> r + "-");
            assertThat(value).isEqualTo("L+");
        }

        @Test
        public void shouldFoldRight() {
            final String value = Either.right("R").fold(l -> l + "-", r -> r + "+");
            assertThat(value).isEqualTo("R+");
        }
    }

    @Nested
    public class SequenceTests {

        @Test
        public void shouldThrowWhenSequencingNull() {
            assertThatThrownBy(() -> Either.sequence(null))
              .isInstanceOf(NullPointerException.class)
              .withFailMessage("eithers is null");
        }

        @Test
        public void shouldSequenceEmptyIterableOfEither() {
            final Iterable<Either<Integer, String>> eithers = List.empty();
            final Either<Seq<Integer>, Seq<String>> actual = Either.sequence(eithers);
            final Either<Seq<Integer>, Seq<String>> expected = Either.right(Vector.empty());
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldSequenceNonEmptyIterableOfRight() {
            final Iterable<Either<Integer, String>> eithers = List.of(Either.right("a"), Either.right("b"), Either.right("c"));
            final Either<Seq<Integer>, Seq<String>> actual = Either.sequence(eithers);
            final Either<Seq<Integer>, Seq<String>> expected = Either.right(Vector.of("a", "b", "c"));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldSequenceNonEmptyIterableOfLeft() {
            final Iterable<Either<Integer, String>> eithers = List.of(Either.left(1), Either.left(2), Either.left(3));
            final Either<Seq<Integer>, Seq<String>> actual = Either.sequence(eithers);
            final Either<Seq<Integer>, Seq<String>> expected = Either.left(Vector.of(1, 2, 3));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldSequenceNonEmptyIterableOfMixedEither() {
            final Iterable<Either<Integer, String>> eithers = List.of(Either.right("a"), Either.left(1), Either.right("c"), Either.left(3));
            final Either<Seq<Integer>, Seq<String>> actual = Either.sequence(eithers);
            final Either<Seq<Integer>, Seq<String>> expected = Either.left(Vector.of(1, 3));
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    public class SequenceRightTests {

        @Test
        public void shouldThrowWhenSequencingRightNull() {
            assertThatThrownBy(() -> Either.sequenceRight(null))
              .isInstanceOf(NullPointerException.class)
              .withFailMessage("eithers is null");
        }

        @Test
        public void shouldSequenceRightEmptyIterableOfEither() {
            final Iterable<Either<Integer, String>> eithers = List.empty();
            final Either<Integer, Seq<String>> actual = Either.sequenceRight(eithers);
            final Either<Integer, Seq<String>> expected = Either.right(Vector.empty());
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldSequenceRightNonEmptyIterableOfRight() {
            final Iterable<Either<Integer, String>> eithers = List.of(Either.right("a"), Either.right("b"), Either.right("c"));
            final Either<Integer, Seq<String>> actual = Either.sequenceRight(eithers);
            final Either<Integer, Seq<String>> expected = Either.right(Vector.of("a", "b", "c"));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldSequenceRightNonEmptyIterableOfLeft() {
            final Iterable<Either<Integer, String>> eithers = List.of(Either.left(1), Either.left(2), Either.left(3));
            final Either<Integer, Seq<String>> actual = Either.sequenceRight(eithers);
            final Either<Integer, Seq<String>> expected = Either.left(1);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldSequenceRightNonEmptyIterableOfMixedEither() {
            final Iterable<Either<Integer, String>> eithers = List.of(Either.right("a"), Either.left(1), Either.right("c"), Either.left(3));
            final Either<Integer, Seq<String>> actual = Either.sequenceRight(eithers);
            final Either<Integer, Seq<String>> expected = Either.left(1);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    public class TraverseTests {

        @Test
        public void shouldThrowWhenTraversingNull() {
            assertThatThrownBy(() -> Either.traverse(null, null))
              .isInstanceOf(NullPointerException.class)
              .withFailMessage("eithers is null");
        }

        @Test
        public void shouldTraverseEmptyIterableOfEither() {
            final Iterable<String> values = List.empty();
            final Either<Seq<Integer>, Seq<String>> actual = Either.traverse(values, Either::right);
            final Either<Seq<Integer>, Seq<String>> expected = Either.right(Vector.empty());
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTraverseNonEmptyIterableOfRight() {
            final Iterable<String> values = List.of("a", "b", "c");
            final Either<Seq<Integer>, Seq<String>> actual = Either.traverse(values, Either::right);
            final Either<Seq<Integer>, Seq<String>> expected = Either.right(Vector.of("a", "b", "c"));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTraverseNonEmptyIterableOfLeft() {
            final Iterable<Integer> values = List.of(1, 2, 3);
            final Either<Seq<Integer>, Seq<String>> actual = Either.traverse(values, Either::left);
            final Either<Seq<Integer>, Seq<String>> expected = Either.left(Vector.of(1, 2, 3));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTraverseNonEmptyIterableOfMixedEither() {
            final Iterable<String> values = List.of("a", "1", "c", "3");
            final Either<Seq<Integer>, Seq<String>> actual =
              Either.traverse(values, x -> x.matches("^\\d+$") ? Either.left(Integer.parseInt(x)) : Either.right(x));
            final Either<Seq<Integer>, Seq<String>> expected = Either.left(Vector.of(1, 3));
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    public class TraverseRightTests {
        @Test
        public void shouldThrowWhenTraversingRightNull() {
            assertThatThrownBy(() -> Either.traverseRight(null, null))
              .isInstanceOf(NullPointerException.class)
              .withFailMessage("eithers is null");
        }

        @Test
        public void shouldTraverseRightEmptyIterableOfEither() {
            final Iterable<String> values = List.empty();
            final Either<Integer, Seq<String>> actual = Either.traverseRight(values, Either::right);
            final Either<Integer, Seq<String>> expected = Either.right(Vector.empty());
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTraverseRightNonEmptyIterableOfRight() {
            final Iterable<String> values = List.of("a", "b", "c");
            final Either<Integer, Seq<String>> actual = Either.traverseRight(values, Either::right);
            final Either<Integer, Seq<String>> expected = Either.right(Vector.of("a", "b", "c"));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTraverseRightNonEmptyIterableOfLeft() {
            final Iterable<Integer> values = List.of(1, 2, 3);
            final Either<Integer, Seq<String>> actual = Either.traverseRight(values, Either::left);
            final Either<Integer, Seq<String>> expected = Either.left(1);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldTraverseRightNonEmptyIterableOfMixedEither() {
            final Iterable<String> values = List.of("a", "1", "c", "3");
            final Either<Integer, Seq<String>> actual =
              Either.traverseRight(values, x -> x.matches("^\\d+$") ? Either.left(Integer.parseInt(x)) : Either.right(x));
            final Either<Integer, Seq<String>> expected = Either.left(1);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    public class NarrowTests {

        @Test
        public void shouldNarrowRightEither() {
            Either<String, Integer> either = Either.right(42);
            Either<CharSequence, Number> narrow = Either.narrow(either);
            assertThat(narrow.get()).isEqualTo(42);
        }

        @Test
        public void shouldNarrowLeftEither() {
            Either<String, Integer> either = Either.left("vavr");
            Either<CharSequence, Number> narrow = Either.narrow(either);
            assertThat(narrow.getLeft()).isEqualTo("vavr");
        }
    }

    @Nested
    public class CondTests {

        @Test
        public void shouldReturnRightIfTestTrue() {
            Either<String, Integer> either = Either.cond(true, () -> 21, () -> "vavr");
            assertThat(either).isEqualTo(Either.right(21));
        }

        @Test
        public void shouldReturnLeftIfTestFalse() {
            Either<String, Integer> either = Either.cond(false, () -> 21, () -> "vavr");
            assertThat(either).isEqualTo(Either.left("vavr"));
        }

        @Test
        public void shouldNotEvaluateRightSupplierOnFalse() {
            Either<String, Integer> either = Either.cond(false, () -> {
                fail("Should not be called");
                return 21;
            }, () -> "vavr");
            assertThat(either).isEqualTo(Either.left("vavr"));
        }

        @Test
        public void shouldNotEvaluateLeftSupplierOnTrue() {
            Either<String, Integer> either = Either.cond(true, () -> 21, () -> {
                fail("Should not be called");
                return "vavr";
            });
            assertThat(either).isEqualTo(Either.right(21));
        }
        private class Animal {
            String name;
            Animal(String name) { this.name = name; }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Animal)) return false;
                Animal other = (Animal) o;
                return name.equals(other.name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }
        }

        private class Dog extends Animal {
            Dog(String name) { super(name); }
        }

        private class Cat extends Animal {
            Cat(String name) { super(name); }
        }

        @Test
        public void shouldBeFineWithCovariantLeft() {
            Either<Animal, Integer> either = Either.cond(false, () -> 21, () -> new Cat("vavr"));
            assertThat(either).isEqualTo(Either.left(new Cat("vavr")));
        }

        @Test
        public void shouldBeFineWithCovariantRight() {
            Either<String, Animal> either = Either.cond(true, () -> new Dog("vavr"), () -> "vavr");
            assertThat(either).isEqualTo(Either.right(new Dog("vavr")));
        }

        @Test
        public void shouldMakeTheSameDecisionNoMatterHowItsCalled() {
            Either<String, Integer> e1 = Either.cond(true, () -> 21, () -> "vavr");
            Either<String, Integer> e2 = Either.cond(true, 21, "vavr");

            Either<String, Integer> e3 = Either.cond(false, () -> 21, () -> "vavr");
            Either<String, Integer> e4 = Either.cond(false, 21, "vavr");

            assertThat(List.of(e1, e2)).allMatch(e -> e.equals(Either.right(21)));
            assertThat(List.of(e3, e4)).allMatch(e -> e.equals(Either.left("vavr")));
        }
    }

    @Nested
    public class IsEmptyTests {

        @Test
        public void shouldBeEmptyOnLeft() {
            assertThat(Either.left(1).isEmpty()).isTrue();
        }

        @Test
        public void shouldNotBeEmptyOnRight() {
            assertThat(Either.right(1).isEmpty()).isFalse();
        }
    }

    @Nested
    public class OrElseTests {

        @Test
        public void shouldEitherOrElseEither() {
            assertThat(Either.right(1).orElse(Either.right(2)).get()).isEqualTo(1);
            assertThat(Either.left(1).orElse(Either.right(2)).get()).isEqualTo(2);
        }

        @Test
        public void shouldEitherOrElseSupplier() {
            assertThat(Either.right(1).orElse(() -> Either.right(2)).get()).isEqualTo(1);
            assertThat(Either.left(1).orElse(() -> Either.right(2)).get()).isEqualTo(2);
        }
    }

    @Nested
    public class GetOrElseTests {

        @Test
        public void shouldGetRightValue() {
            assertThat(Either.right(1).getOrElse(2)).isEqualTo(1);
            assertThat(Either.right(1).getOrElse(() -> 2)).isEqualTo(1);
        }

        @Test
        public void shouldGetAlternativeOnLeft() {
            assertThat(EitherTest.<Integer>left().getOrElse(2)).isEqualTo(2);
            assertThat(EitherTest.<Integer>left().getOrElse(() -> 2)).isEqualTo(2);
        }

        @Test
        public void shouldAcceptNullAsAlternative() {
            assertThat(EitherTest.<Integer>left().getOrElse((Integer) null)).isNull();
            assertThat(Either.right(1).getOrElse((Integer) null)).isEqualTo(1);
        }

        @Test
        public void shouldNotInvokeSupplierOnRight() {
            assertThat(Either.right(1).getOrElse(() -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(1);
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            final Supplier<Integer> supplier = null;
            assertThrows(NullPointerException.class, () -> EitherTest.<Integer>left().getOrElse(supplier));
        }

        @Test
        public void shouldGetOrElseGetFromTheLeftValue() {
            assertThat(Either.<String, Integer>right(1).getOrElseGet(String::length)).isEqualTo(1);
            assertThat(Either.<String, Integer>left("abc").getOrElseGet(String::length)).isEqualTo(3);
        }
    }

    @Nested
    public class GetOrElseThrowTests {

        @Test
        public void shouldGetRightValue() {
            assertThat(Either.right(1).getOrElseThrow(() -> new IllegalStateException("x"))).isEqualTo(1);
            assertThat(Either.<String, Integer>right(1).getOrElseThrow(l -> new IllegalStateException(l))).isEqualTo(1);
        }

        @Test
        public void shouldThrowSuppliedExceptionOnLeft() {
            assertThrows(IllegalStateException.class, () -> left().getOrElseThrow(() -> new IllegalStateException("x")));
        }

        @Test
        public void shouldThrowExceptionBuiltFromTheLeftValue() {
            assertThatThrownBy(() -> EitherTest.<Integer>left().getOrElseThrow(l -> new IllegalStateException(l)))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("empty");
        }

        @Test
        public void shouldThrowOnNullArguments() {
            assertThrows(NullPointerException.class, () -> left().getOrElseThrow((Supplier<RuntimeException>) null));
            assertThrows(NullPointerException.class, () -> left().getOrElseThrow((Function<String, RuntimeException>) null));
        }
    }

    @Nested
    public class GetOrNullTests {

        @Test
        public void shouldGetRightValue() {
            assertThat(Either.right(1).getOrNull()).isEqualTo(1);
        }

        @Test
        public void shouldGetNullOnLeft() {
            assertThat(left().getOrNull()).isNull();
        }
    }

    @Nested
    public class LeftTests {

        @Test
        public void shouldReturnTrueWhenCallingIsLeftOnLeft() {
            assertThat(Either.left(1).isLeft()).isTrue();
        }

        @Test
        public void shouldReturnFalseWhenCallingIsRightOnLeft() {
            assertThat(Either.left(1).isRight()).isFalse();
        }
    }

    @Nested
    public class ContainsTests {

        @Test
        public void shouldContainTheRightValue() {
            assertThat(Either.right(1).contains(1)).isTrue();
            assertThat(Either.right(1).contains(2)).isFalse();
            assertThat(Either.right(1).contains(null)).isFalse();
        }

        @Test
        public void shouldNotContainTheLeftValue() {
            assertThat(Either.<Integer, Integer>left(1).contains(1)).isFalse();
        }
    }

    @Nested
    public class ExistsTests {

        @Test
        public void shouldTestTheRightValue() {
            assertThat(Either.right(1).exists(i -> i == 1)).isTrue();
            assertThat(Either.right(1).exists(i -> i == 2)).isFalse();
        }

        @Test
        public void shouldNotHoldOnLeft() {
            assertThat(left().exists(i -> true)).isFalse();
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> left().exists(null));
        }
    }

    @Nested
    public class ForAllTests {

        @Test
        public void shouldTestTheRightValue() {
            assertThat(Either.right(1).forAll(i -> i == 1)).isTrue();
            assertThat(Either.right(1).forAll(i -> i == 2)).isFalse();
        }

        @Test
        public void shouldHoldVacuouslyOnLeft() {
            assertThat(left().forAll(i -> false)).isTrue();
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> left().forAll(null));
        }
    }

    @Nested
    public class ForEachTests {

        @Test
        public void shouldConsumeTheRightValue() {
            final int[] actual = { -1 };
            Either.right(1).forEach(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(1);
        }

        @Test
        public void shouldNotConsumeAnythingOnLeft() {
            final int[] actual = { -1 };
            EitherTest.<Integer>left().forEach(i -> actual[0] = i);
            assertThat(actual[0]).isEqualTo(-1);
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> left().forEach(null));
        }
    }

    @Nested
    public class FilterTests {

        @Test
        public void shouldFilterRight() {
            Either<String, Integer> either = Either.right(42);
            assertThat(either.filter(i -> true).get()).isSameAs(either);
            assertThat(either.filter(i -> false)).isSameAs(Option.none());
        }

        @Test
        public void shouldFilterLeft() {
            Either<String, Integer> either = Either.left("vavr");
            assertThat(either.filter(i -> true).get()).isSameAs(either);
            assertThat(either.filter(i -> false).get()).isSameAs(either);
        }
    }

    @Nested
    public class FilterOrElseTests {

        @Test
        public void shouldFilterOrElseRight() {
            Either<String, Integer> either = Either.right(42);
            assertThat(either.filterOrElse(i -> true, Object::toString)).isSameAs(either);
            assertThat(either.filterOrElse(i -> false, Object::toString)).isEqualTo(Either.left("42"));
        }

        @Test
        public void shouldFilterOrElseLeft() {
            Either<String, Integer> either = Either.left("vavr");
            assertThat(either.filterOrElse(i -> true, Object::toString)).isSameAs(either);
            assertThat(either.filterOrElse(i -> false, Object::toString)).isSameAs(either);
        }
    }

    @Nested
    public class FlatMapTests {

        @Test
        public void shouldFlatMapRight() {
            Either<String, Integer> either = Either.right(42);
            assertThat(either.flatMap(v -> Either.right("ok")).get()).isEqualTo("ok");
        }

        @Test
        public void shouldFlatMapLeft() {
            Either<String, Integer> either = Either.left("vavr");
            assertThat(either.flatMap(v -> Either.right("ok"))).isSameAs(either);
        }
    }

    @Nested
    public class PeekTests {

        @Test
        public void shouldPeekRight() {
            final int[] effect = {0};
            final Either<String, Integer> right = Either.right(1);
            assertThat(right.peek(i -> effect[0] = i)).isSameAs(right);
            assertThat(effect[0]).isEqualTo(1);
        }

        @Test
        public void shouldNotPeekLeft() {
            final Either<String, Integer> left = left();
            assertThat(left.peek(i -> {throw new IllegalStateException();})).isSameAs(left);
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> Either.right(1).peek(null));
        }
    }

    @Nested
    public class PeekLeftTests {

        @Test
        public void shouldPeekLeftNil() {
            assertThat(left().peekLeft(t -> {})).isEqualTo(left());
        }

        @Test
        public void shouldPeekLeftForLeft() {
            final int[] effect = {0};
            final Either<Integer, ?> actual = Either.left(1).peekLeft(i -> effect[0] = i);
            assertThat(actual).isEqualTo(Either.left(1));
            assertThat(effect[0]).isEqualTo(1);
        }

        @Test
        public void shouldNotPeekLeftForRight() {
            Either.right(1).peekLeft(i -> {throw new IllegalStateException();});
        }
    }

    @Nested
    public class RightTests {

        @Test
        public void shouldReturnTrueWhenCallingIsRightOnRight() {
            assertThat(Either.right(1).isRight()).isTrue();
        }

        @Test
        public void shouldReturnFalseWhenCallingIsLeftOnRight() {
            assertThat(Either.right(1).isLeft()).isFalse();
        }
    }

    @Nested
    public class EqualsTests {

        @Test
        public void shouldEqualLeftIfObjectIsSame() {
            final Either<Integer, ?> left = Either.left(1);
            assertThat(left.equals(left)).isTrue();
        }

        @Test
        public void shouldNotEqualLeftIfObjectIsNull() {
            assertThat(Either.left(1).equals(null)).isFalse();
        }

        @Test
        public void shouldNotEqualLeftIfObjectIsOfDifferentType() {
            assertThat(Either.left(1).equals(new Object())).isFalse();
        }

        @Test
        public void shouldEqualLeft() {
            assertThat(Either.left(1)).isEqualTo(Either.left(1));
        }

        @Test
        public void shouldNotEqualLeftOfDifferentValue() {
            assertThat(Either.left(1)).isNotEqualTo(Either.left(2));
        }

        @Test
        public void shouldEqualRightIfObjectIsSame() {
            final Either<?, ?> right = Either.right(1);
            assertThat(right.equals(right)).isTrue();
        }

        @Test
        public void shouldNotEqualRightIfObjectIsNull() {
            assertThat(Either.right(1).equals(null)).isFalse();
        }

        @Test
        public void shouldNotEqualRightIfObjectIsOfDifferentType() {
            assertThat(Either.right(1).equals(new Object())).isFalse();
        }

        @Test
        public void shouldEqualRight() {
            assertThat(Either.right(1)).isEqualTo(Either.right(1));
        }

        @Test
        public void shouldNotEqualRightOfDifferentValue() {
            assertThat(Either.right(1)).isNotEqualTo(Either.right(2));
        }

        @Test
        public void shouldNotEqualLeftAndRightOfTheSameValue() {
            assertThat(Either.left(1)).isNotEqualTo(Either.right(1));
        }
    }

    @Nested
    public class ToOptionTests {

        @Test
        public void shouldConvertRightToSome() {
            assertThat(Either.right(42).toOption()).isEqualTo(Option.some(42));
        }

        @Test
        public void shouldConvertLeftToNone() {
            // the left value is dropped
            assertThat(Either.left("x").toOption()).isSameAs(Option.none());
        }
    }

    @Nested
    public class ToValidationTests {

        @Test
        public void shouldConvertToValidValidation() {
            final Validation<?, Integer> validation = Either.right(42).toValidation();
            assertThat(validation.isValid()).isTrue();
            assertThat(validation.get()).isEqualTo(42);
        }

        @Test
        public void shouldConvertToInvalidValidation() {
            final Validation<String, ?> validation = Either.left("vavr").toValidation();
            assertThat(validation.isInvalid()).isTrue();
            assertThat(validation.getError()).isEqualTo("vavr");
        }
    }

    @Nested
    public class ToVectorTests {

        @Test
        public void shouldConvertRightToVectorOfOne() {
            assertThat(Either.right(42).toVector()).isEqualTo(Vector.of(42));
        }

        @Test
        public void shouldConvertLeftToEmptyVector() {
            assertThat(Either.left("x").toVector()).isSameAs(Vector.empty());
        }
    }

    @Nested
    public class HashCodeTests {

        @Test
        public void shouldHashRight() {
            assertThat(Either.right(1).hashCode()).isEqualTo(Objects.hashCode(1));
        }

        @Test
        public void shouldHashLeft() {
            assertThat(Either.left(1).hashCode()).isEqualTo(Objects.hashCode(1));
        }
    }

    @Nested
    public class ToStringTests {

        @Test
        public void shouldConvertRightToString() {
            assertThat(Either.right(1).toString()).isEqualTo("Right(1)");
        }

        @Test
        public void shouldConvertLeftToString() {
            assertThat(Either.left(1).toString()).isEqualTo("Left(1)");
        }
    }

    @Nested
    public class ToTryTests {
        @Test
        void shouldConvertRightToTrySuccess() {
            Either<String, String> either = Either.right("ok");

            Try<String> result = either.toTry(IllegalStateException::new);

            assertThat(result).isEqualTo(Try.success("ok"));
        }

        @Test
        void shouldNotApplyTheMapperOnRight() {
            assertThat(Either.<String, String>right("ok").toTry(l -> {
                throw new AssertionError("must not be invoked");
            })).isEqualTo(Try.success("ok"));
        }

        @Test
        void shouldConvertLeftToTryFailureOfTheMappedLeftValue() {
            Either<String, String> either = Either.left("error");

            Try<String> result = either.toTry(IllegalStateException::new);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("error");
        }

        @Test
        void shouldConvertLeftOfThrowableWithTheIdentity() {
            final IOException cause = new IOException("boom");
            final Either<IOException, String> either = Either.left(cause);
            assertThat(either.toTry(t -> t).getCause()).isSameAs(cause);
        }

        @Test
        void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Either.right("ok").toTry(null));
        }

        @Test
        void shouldRejectNullCauseBuiltForLeft() {
            // a Failure cannot hold null
            assertThrows(NullPointerException.class, () -> Either.left("error").toTry(l -> null));
        }

        @Test
        void shouldRethrowFatalCauseBuiltForLeft() {
            assertThrows(InterruptedException.class, () -> Either.left("error").toTry(InterruptedException::new));
        }
    }
}
