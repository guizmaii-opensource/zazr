package dev.zazr.test;

import dev.zazr.CheckedRunnable;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Vector;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import dev.zazr.control.Try;
import dev.zazr.control.Validation;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

import static dev.zazr.test.Assertion.anything;
import static dev.zazr.test.Assertion.contains;
import static dev.zazr.test.Assertion.containsString;
import static dev.zazr.test.Assertion.endsWithString;
import static dev.zazr.test.Assertion.equalTo;
import static dev.zazr.test.Assertion.exists;
import static dev.zazr.test.Assertion.forall;
import static dev.zazr.test.Assertion.hasAt;
import static dev.zazr.test.Assertion.hasFirst;
import static dev.zazr.test.Assertion.hasLast;
import static dev.zazr.test.Assertion.hasSameElements;
import static dev.zazr.test.Assertion.hasSize;
import static dev.zazr.test.Assertion.isEmpty;
import static dev.zazr.test.Assertion.isFailure;
import static dev.zazr.test.Assertion.isFalse;
import static dev.zazr.test.Assertion.isGreaterThan;
import static dev.zazr.test.Assertion.isGreaterThanOrEqualTo;
import static dev.zazr.test.Assertion.isInvalid;
import static dev.zazr.test.Assertion.isLeft;
import static dev.zazr.test.Assertion.isLessThan;
import static dev.zazr.test.Assertion.isLessThanOrEqualTo;
import static dev.zazr.test.Assertion.isNonEmpty;
import static dev.zazr.test.Assertion.isNone;
import static dev.zazr.test.Assertion.isRight;
import static dev.zazr.test.Assertion.isSome;
import static dev.zazr.test.Assertion.isSorted;
import static dev.zazr.test.Assertion.isSuccess;
import static dev.zazr.test.Assertion.isTrue;
import static dev.zazr.test.Assertion.isValid;
import static dev.zazr.test.Assertion.isWithin;
import static dev.zazr.test.Assertion.matchesRegex;
import static dev.zazr.test.Assertion.not;
import static dev.zazr.test.Assertion.startsWithString;
import static dev.zazr.test.Assertion.throwsA;
import static dev.zazr.test.Assertion.throwsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every assertion of the catalogue passes and fails on the right values, and explains a failure exactly. This file
 * also imports AssertJ's {@code assertThat} and calls {@code Assertion.assertThat} by its class, as a test using
 * both does.
 */
class AssertionTest {

    /// Tests a value, and returns the explanation of the failure, or "passed".
    private static <A> String explain(A value, Assertion<? super A> assertion) {
        return Assertion.assertThat(value, assertion).message().getOrElse("passed");
    }

    private static String explainCode(CheckedRunnable code, Assertion<? super CheckedRunnable> assertion) {
        return Assertion.assertThat(code, assertion).message().getOrElse("passed");
    }

    // -- values

    @Test
    void anythingHolds() {
        assertThat(explain(5, anything())).isEqualTo("passed");
        assertThat(explain(null, anything())).isEqualTo("passed");
        assertThat(anything()).hasToString("anything");
    }

    @Test
    void equalTo_() {
        assertThat(explain(5, equalTo(5))).isEqualTo("passed");
        assertThat(explain(5, equalTo(6))).isEqualTo("5 is not equal to 6");
        assertThat(explain("a", equalTo("b"))).isEqualTo("\"a\" is not equal to \"b\"");
        assertThat(explain('a', equalTo('b'))).isEqualTo("'a' is not equal to 'b'");
        assertThat(explain(null, equalTo(1))).isEqualTo("null is not equal to 1");
        assertThat(explain(null, equalTo(null))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2), equalTo(List.of(1, 2)))).isEqualTo("passed");
        assertThat(equalTo("x")).hasToString("equalTo(\"x\")");
    }

    @Test
    void isTrueAndIsFalse() {
        assertThat(explain(true, isTrue())).isEqualTo("passed");
        assertThat(explain(false, isTrue())).isEqualTo("false is not true");
        assertThat(explain(false, isFalse())).isEqualTo("passed");
        assertThat(explain(true, isFalse())).isEqualTo("true is not false");
    }

    @Test
    void comparisons() {
        assertThat(explain(6, isGreaterThan(5))).isEqualTo("passed");
        assertThat(explain(5, isGreaterThan(5))).isEqualTo("5 is not greater than 5");
        assertThat(explain(5, isGreaterThanOrEqualTo(5))).isEqualTo("passed");
        assertThat(explain(4, isGreaterThanOrEqualTo(5))).isEqualTo("4 is less than 5");
        assertThat(explain(4, isLessThan(5))).isEqualTo("passed");
        assertThat(explain(5, isLessThan(5))).isEqualTo("5 is not less than 5");
        assertThat(explain(5, isLessThanOrEqualTo(5))).isEqualTo("passed");
        assertThat(explain(6, isLessThanOrEqualTo(5))).isEqualTo("6 is greater than 5");
        assertThat(explain(1, isWithin(1, 3))).isEqualTo("passed");
        assertThat(explain(3, isWithin(1, 3))).isEqualTo("passed");
        assertThat(explain(0, isWithin(1, 3))).isEqualTo("0 is not within 1 and 3");
        assertThat(explain(4, isWithin(1, 3))).isEqualTo("4 is not within 1 and 3");
        assertThat(explain("b", isGreaterThan("a"))).isEqualTo("passed");
        assertThat(isWithin(1, 3)).hasToString("isWithin(1, 3)");
        assertThatThrownBy(() -> isGreaterThan(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> isWithin(null, 1)).isInstanceOf(NullPointerException.class);
    }

    // -- strings

    @Test
    void strings() {
        assertThat(explain("abc", startsWithString("ab"))).isEqualTo("passed");
        assertThat(explain("abc", startsWithString("b"))).isEqualTo("\"abc\" does not start with \"b\"");
        assertThat(explain("abc", endsWithString("bc"))).isEqualTo("passed");
        assertThat(explain("abc", endsWithString("b"))).isEqualTo("\"abc\" does not end with \"b\"");
        assertThat(explain("abc", containsString("b"))).isEqualTo("passed");
        assertThat(explain("abc", containsString("d"))).isEqualTo("\"abc\" does not contain \"d\"");
        assertThat(explain("a1", matchesRegex("[a-z][0-9]"))).isEqualTo("passed");
        assertThat(explain("a1b", matchesRegex("[a-z][0-9]"))).isEqualTo("\"a1b\" does not match \"[a-z][0-9]\"");
        assertThatThrownBy(() -> matchesRegex("[")).isInstanceOf(java.util.regex.PatternSyntaxException.class);
    }

    // -- collections

    @Test
    void emptiness() {
        assertThat(explain(List.empty(), isEmpty())).isEqualTo("passed");
        assertThat(explain(List.of(1), isEmpty())).isEqualTo("List(1) is not empty");
        assertThat(explain(List.of(1), isNonEmpty())).isEqualTo("passed");
        assertThat(explain(Vector.empty(), isNonEmpty())).isEqualTo("Vector() is empty");
        assertThat(explain(java.util.List.of(), isEmpty())).isEqualTo("passed");
    }

    @Test
    void hasSize_() {
        assertThat(explain(List.of(1, 2), hasSize(equalTo(2)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2), hasSize(equalTo(3))))
                .isEqualTo("List(1, 2) has size 2:\n  2 is not equal to 3");
        assertThat(explain(java.util.List.of(1, 2), hasSize(isGreaterThan(1)))).isEqualTo("passed");
        assertThat(hasSize(equalTo(3))).hasToString("hasSize(equalTo(3))");
    }

    @Test
    void containsAndExists() {
        assertThat(explain(List.of(1, 2), contains(2))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2), contains(3))).isEqualTo("List(1, 2) does not contain 3");
        assertThat(explain(List.of(1, 2), exists(isGreaterThan(1)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2), exists(isGreaterThan(2))))
                .isEqualTo("List(1, 2) has no element that satisfies isGreaterThan(2)");
        assertThat(explain(List.<Integer>empty(), exists(anything())))
                .isEqualTo("List() has no element that satisfies anything");
    }

    @Test
    void forall_() {
        assertThat(explain(List.of(1, 2), forall(isLessThan(3)))).isEqualTo("passed");
        assertThat(explain(List.<Integer>empty(), forall(isLessThan(3)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 5, 7), forall(isLessThan(3))))
                .isEqualTo("List(1, 5, 7) has 5 at index 1:\n  5 is not less than 3");
    }

    @Test
    void positions() {
        assertThat(explain(List.of(1, 2, 3), hasFirst(equalTo(1)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2, 3), hasFirst(equalTo(2))))
                .isEqualTo("List(1, 2, 3) has first element 1:\n  1 is not equal to 2");
        assertThat(explain(List.<Integer>empty(), hasFirst(equalTo(2)))).isEqualTo("List() has no first element");
        assertThat(explain(List.of(1, 2, 3), hasLast(equalTo(3)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2, 3), hasLast(equalTo(2))))
                .isEqualTo("List(1, 2, 3) has last element 3:\n  3 is not equal to 2");
        assertThat(explain(List.<Integer>empty(), hasLast(equalTo(2)))).isEqualTo("List() has no last element");
        assertThat(explain(List.of(1, 2, 3), hasAt(1, equalTo(2)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2, 3), hasAt(0, equalTo(2))))
                .isEqualTo("List(1, 2, 3) has 1 at index 0:\n  1 is not equal to 2");
        assertThat(explain(List.of(1, 2, 3), hasAt(3, equalTo(2))))
                .isEqualTo("List(1, 2, 3) has no element at index 3");
        assertThatThrownBy(() -> hasAt(-1, anything())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hasSameElements_() {
        assertThat(explain(List.of(1, 2, 2), hasSameElements(List.of(2, 1, 2)))).isEqualTo("passed");
        assertThat(explain(List.of(1, 2), hasSameElements(List.of(2, 1, 2))))
                .isEqualTo("List(1, 2) does not have the same elements as List(2, 1, 2)");
        assertThat(explain(List.of(1, 2), hasSameElements(java.util.List.of(1, 3))))
                .isEqualTo("List(1, 2) does not have the same elements as [1, 3]");
    }

    @Test
    void isSorted_() {
        assertThat(explain(List.of(1, 2, 2, 3), isSorted())).isEqualTo("passed");
        assertThat(explain(List.<Integer>empty(), isSorted())).isEqualTo("passed");
        assertThat(explain(List.of(1, 3, 2), isSorted())).isEqualTo("List(1, 3, 2) is not sorted: 3 comes before 2");
        assertThat(explain(List.of(3, 2, 1), isSorted(Comparator.<Integer>reverseOrder())))
                .isEqualTo("passed");
        assertThat(explain(List.of(1, 2), isSorted(Comparator.<Integer>reverseOrder())))
                .isEqualTo("List(1, 2) is not sorted: 1 comes before 2");
        // the name does not depend on the comparator's toString, which holds a hash code
        assertThat(isSorted(Comparator.<Integer>reverseOrder())).hasToString("isSorted(a comparator)");
        assertThat(isSorted()).hasToString("isSorted");
    }

    // -- Zazr types

    @Test
    void options() {
        assertThat(explain(Option.some(4), isSome(isGreaterThan(3)))).isEqualTo("passed");
        assertThat(explain(Option.some(4), isSome(isGreaterThan(5))))
                .isEqualTo("Some(4) holds 4:\n  4 is not greater than 5");
        assertThat(explain(Option.<Integer>none(), isSome(anything()))).isEqualTo("None is not a Some");
        assertThat(explain(Option.none(), isNone())).isEqualTo("passed");
        assertThat(explain(Option.some(4), isNone())).isEqualTo("Some(4) is not None");
    }

    @Test
    void eithers() {
        assertThat(explain(Either.<String, Integer>left("e"), isLeft(equalTo("e"))))
                .isEqualTo("passed");
        assertThat(explain(Either.<String, Integer>left("e"), isLeft(equalTo("f"))))
                .isEqualTo("Left(e) holds \"e\":\n  \"e\" is not equal to \"f\"");
        assertThat(explain(Either.<String, Integer>right(1), isLeft(anything())))
                .isEqualTo("Right(1) is not a Left");
        assertThat(explain(Either.<String, Integer>right(1), isRight(equalTo(1))))
                .isEqualTo("passed");
        assertThat(explain(Either.<String, Integer>right(1), isRight(equalTo(2))))
                .isEqualTo("Right(1) holds 1:\n  1 is not equal to 2");
        assertThat(explain(Either.<String, Integer>left("e"), isRight(anything())))
                .isEqualTo("Left(e) is not a Right");
    }

    @Test
    void tries() {
        IllegalStateException boom = new IllegalStateException("boom");
        assertThat(explain(Try.success(1), isSuccess(equalTo(1)))).isEqualTo("passed");
        assertThat(explain(Try.success(1), isSuccess(equalTo(2))))
                .isEqualTo("Success(1) holds 1:\n  1 is not equal to 2");
        assertThat(explain(Try.<Integer>failure(boom), isSuccess(anything())))
                .isEqualTo("Failure(java.lang.IllegalStateException: boom) is not a Success");
        assertThat(explain(Try.failure(boom), isFailure(anything()))).isEqualTo("passed");
        assertThat(explain(Try.failure(boom), isFailure(equalTo(new RuntimeException()))))
                .startsWith(
                        "Failure(java.lang.IllegalStateException: boom) holds java.lang.IllegalStateException: boom:\n  java.lang.IllegalStateException: boom is not equal to");
        assertThat(explain(Try.success(1), isFailure(anything()))).isEqualTo("Success(1) is not a Failure");
    }

    @Test
    void validations() {
        assertThat(explain(Validation.<String, Integer>valid(1), isValid(equalTo(1))))
                .isEqualTo("passed");
        assertThat(explain(Validation.<String, Integer>valid(1), isValid(equalTo(2))))
                .isEqualTo("Valid(1) holds 1:\n  1 is not equal to 2");
        assertThat(explain(Validation.<String, Integer>invalid("e"), isValid(anything())))
                .isEqualTo("Invalid(e) is not Valid");
        assertThat(explain(Validation.<String, Integer>invalid("e"), isInvalid(equalTo(NonEmptyVector.single("e")))))
                .isEqualTo("passed");
        assertThat(explain(Validation.<String, Integer>invalid("e"), isInvalid(hasSize(equalTo(2)))))
                .isEqualTo(
                        "Invalid(e) holds NonEmptyVector(e):\n  NonEmptyVector(e) has size 1:\n    1 is not equal to 2");
        assertThat(explain(Validation.<String, Integer>valid(1), isInvalid(anything())))
                .isEqualTo("Valid(1) is not Invalid");
    }

    // -- code that must throw

    @Test
    void throwing() {
        assertThat(explainCode(() -> Integer.parseInt("x"), throwsA(NumberFormatException.class)))
                .isEqualTo("passed");
        assertThat(explainCode(() -> Integer.parseInt("x"), throwsA(IllegalArgumentException.class)))
                .isEqualTo("passed");
        assertThat(explainCode(() -> Integer.parseInt("1"), throwsA(NumberFormatException.class)))
                .isEqualTo("the code did not throw");
        assertThat(explainCode(
                        () -> {
                            throw new IllegalStateException("boom");
                        },
                        throwsA(NumberFormatException.class)))
                .isEqualTo(
                        "the code threw java.lang.IllegalStateException: boom:\n  java.lang.IllegalStateException: boom is not a java.lang.NumberFormatException");
        assertThat(explainCode(
                        () -> {
                            throw new java.io.IOException("io");
                        },
                        throwsWith(hasMessage("io"))))
                .isEqualTo("passed");
        assertThat(explainCode(
                        () -> {
                            throw new AssertionError("inner");
                        },
                        throwsA(AssertionError.class)))
                .isEqualTo("passed");
        assertThat(throwsA(NumberFormatException.class)).hasToString("throwsA(java.lang.NumberFormatException)");
    }

    /// An assertion of the user's own, built with `Assertion.of`.
    private static Assertion<Throwable> hasMessage(String message) {
        return Assertion.of(
                "hasMessage(" + message + ")",
                thrown -> message.equals(thrown.getMessage())
                        ? TestResult.succeed()
                        : TestResult.fail(thrown + " has not the message " + message));
    }

    @Test
    void theCodeFormTakesLambdasWithoutACast() {
        TestResult result = Assertion.assertThat(() -> Integer.parseInt("x"), throwsA(NumberFormatException.class));
        assertThat(result.isSuccess()).isTrue();
        assertThat(Assertion.assertThat(5, equalTo(5)).isSuccess()).isTrue();
    }

    // -- composition

    @Test
    void and_() {
        assertThat(explain(5, isGreaterThan(1).and(isLessThan(9)))).isEqualTo("passed");
        assertThat(explain(5, isGreaterThan(6).and(isLessThan(9)))).isEqualTo("5 is not greater than 6");
        assertThat(explain(5, isGreaterThan(1).and(isLessThan(4)))).isEqualTo("5 is not less than 4");
        assertThat(explain(5, isGreaterThan(6).and(isLessThan(4))))
                .isEqualTo("5 is not greater than 6\n5 is not less than 4");
        assertThat(isGreaterThan(1).and(isLessThan(9))).hasToString("(isGreaterThan(1) and isLessThan(9))");
    }

    @Test
    void or_() {
        assertThat(explain(5, equalTo(5).or(equalTo(6)))).isEqualTo("passed");
        assertThat(explain(6, equalTo(5).or(equalTo(6)))).isEqualTo("passed");
        assertThat(explain(7, equalTo(5).or(equalTo(6))))
                .isEqualTo("neither of these holds:\n  7 is not equal to 5\n  7 is not equal to 6");
        assertThat(equalTo(5).or(equalTo(6))).hasToString("(equalTo(5) or equalTo(6))");
    }

    @Test
    void orTestsItsRightSideOnlyWhenItsLeftSideFails() {
        // the right side fails with an AssertJ AssertionError, or throws, on the value the left side accepts
        Assertion<Integer> positive = Assertion.of("positive", x -> {
            assertThat(x).isPositive();
            return TestResult.succeed();
        });
        Assertion<Integer> boom = Assertion.of("boom", x -> {
            throw new ArithmeticException("evaluated");
        });
        assertThat(explain(0, equalTo(0).or(positive))).isEqualTo("passed");
        assertThat(explain(0, equalTo(0).or(boom))).isEqualTo("passed");
        assertThat(Check.evaluate(
                        CheckConfig.defaults().withSeed(7),
                        Gen.constant(0),
                        equalTo(0).or(positive)))
                .isEqualTo(new CheckResult.Satisfied(200));
        assertThat(explain(5, equalTo(0).or(positive))).isEqualTo("passed");
        assertThatThrownBy(() -> explain(0, equalTo(1).or(boom))).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void andTestsBothSidesSoAThrowingSideAfterAFailedGuardThrows() {
        Assertion<List<Integer>> headIsPositive =
                Assertion.of("headIsPositive", l -> Assertion.assertThat(l.head(), isGreaterThan(0)));
        assertThatThrownBy(() -> Assertion.assertThat(List.<Integer>empty(), isNonEmpty(), headIsPositive))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(Check.evaluate(
                                CheckConfig.defaults().withSeed(7),
                                Gen.constant(List.<Integer>empty()),
                                isNonEmpty(),
                                headIsPositive)
                        .isErroneous())
                .isTrue();
        // behind the guard with or, the second assertion is not tested; the wider assertion may come first
        assertThat(explain(List.<Integer>empty(), isEmpty().or(headIsPositive))).isEqualTo("passed");
        assertThat(explain(List.of(3), isEmpty().or(headIsPositive))).isEqualTo("passed");
        assertThat(explain(List.of(-3), isEmpty().or(headIsPositive)))
                .isEqualTo("neither of these holds:\n  List(-3) is not empty\n  -3 is not greater than 0");
        assertThat(explain(List.of(3), isNonEmpty().and(headIsPositive))).isEqualTo("passed");
    }

    @Test
    void explanationsDoNotDependOnTheRun() {
        Assertion<CheckedRunnable> mustNotThrow = not(throwsA(Exception.class));
        assertThat(explainCode(() -> Integer.parseInt("x"), mustNotThrow))
                .isEqualTo("the code satisfies throwsA(java.lang.Exception), but must not");
        assertThat(explain(new int[] {1, 2}, equalTo(new int[] {1, 2}))).isEqualTo("passed");
        assertThat(explain(new int[] {1, 2}, equalTo(new int[] {1, 3}))).isEqualTo("[1, 2] is not equal to [1, 3]");
        assertThat(explain(new String[][] {{"a"}}, equalTo(new String[][] {{"b"}})))
                .isEqualTo("[[a]] is not equal to [[b]]");
        assertThat(equalTo(new long[] {1L})).hasToString("equalTo([1])");
    }

    @Test
    void validationAssertionsCombineInEitherOrder() {
        assertThat(explain(
                        Validation.<String, Integer>invalid("e"),
                        isValid(equalTo(1)).or(isInvalid(hasSize(equalTo(1))))))
                .isEqualTo("passed");
        assertThat(explain(
                        Validation.<String, Integer>valid(1),
                        isInvalid(hasSize(equalTo(1))).or(isValid(equalTo(1)))))
                .isEqualTo("passed");
        assertThat(isValid(equalTo(1)).or(isInvalid(hasSize(equalTo(1)))))
                .hasToString("(isValid(equalTo(1)) or isInvalid(hasSize(equalTo(1))))");
    }

    @Test
    void not_() {
        assertThat(explain(5, not(equalTo(6)))).isEqualTo("passed");
        assertThat(explain(5, not(equalTo(5)))).isEqualTo("5 satisfies equalTo(5), but must not");
        assertThat(not(equalTo(5))).hasToString("not(equalTo(5))");
    }

    @Test
    void label_() {
        assertThat(explain(5, equalTo(5).label("five"))).isEqualTo("passed");
        assertThat(explain(5, equalTo(6).label("six"))).isEqualTo("six: 5 is not equal to 6");
        assertThat(explain(Option.some(4), isSome(isGreaterThan(5)).label("big")))
                .isEqualTo("big:\n  Some(4) holds 4:\n    4 is not greater than 5");
        assertThat(equalTo(6).label("six")).hasToString("six");
        assertThat(explain(5, not(isGreaterThan(1).label("positive")))).isEqualTo("5 satisfies positive, but must not");
    }

    @Test
    void nestingRendersTheFailingPathOnly() {
        Assertion<Iterable<? extends Option<Integer>>> allSmall = forall(isSome(isLessThan(10)));
        assertThat(explain(List.of(Option.some(1), Option.some(12)), allSmall))
                .isEqualTo(
                        "List(Some(1), Some(12)) has Some(12) at index 1:\n  Some(12) holds 12:\n    12 is not less than 10");
    }

    // -- several assertions

    @Test
    void assertThatListsEveryFailingAssertion() {
        assertThat(Assertion.assertThat(5, isGreaterThan(1), isLessThan(9), not(equalTo(4)))
                        .isSuccess())
                .isTrue();
        assertThat(explain3(5, isGreaterThan(6), isLessThan(9), equalTo(4)))
                .isEqualTo("5 is not greater than 6\n5 is not equal to 4");
        assertThat(explain3("abc", startsWithString("x"), endsWithString("y"), containsString("z")))
                .isEqualTo(
                        "\"abc\" does not start with \"x\"\n\"abc\" does not end with \"y\"\n\"abc\" does not contain \"z\"");
    }

    @SafeVarargs
    private static <A> String explain3(A value, Assertion<? super A>... assertions) {
        return Assertion.assertThat(value, assertions).message().getOrElse("passed");
    }

    @Test
    void assertThatNeedsAnAssertion() {
        assertThatThrownBy(() -> Assertion.assertThat(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least one assertion is needed");
        assertThatThrownBy(() -> Assertion.assertThat(5, (Assertion<Integer>[]) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assertion.assertThat(5, equalTo(5), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assertion.assertThat((CheckedRunnable) null, throwsA(Exception.class)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void anAssertionOfTheUsersOwnRejectsNulls() {
        assertThatThrownBy(() -> Assertion.of(null, x -> TestResult.succeed()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assertion.<Integer>of("x", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assertion.<Integer>of("x", x -> null).test(1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("the test of x returned null");
        assertThatThrownBy(() -> equalTo(1).and(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> equalTo(1).or(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> equalTo(1).label(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> not(null)).isInstanceOf(NullPointerException.class);
    }
}
