package dev.zazr.test;

import dev.zazr.CheckedRunnable;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import dev.zazr.control.Try;
import dev.zazr.control.Validation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A condition on a value of type {@code A} that explains why a value does not satisfy it, after zio-test's
 * {@code Assertion}.
 * <p>
 * {@link #assertThat(Object, Assertion[])} tests a value and returns a {@link TestResult}: a success, or a failure
 * whose explanation names the value and what went wrong, down the failing part of a nested assertion:
 *
 * <pre>{@code
 * assertThat(Option.some(4), isSome(isGreaterThan(5)))
 * // Some(4) holds 4:
 * //   4 is not greater than 5
 * }</pre>
 *
 * Assertions compose with {@link #and(Assertion)}, {@link #or(Assertion)}, {@link #not(Assertion)} and
 * {@link #label(String)}. An assertion is an immutable value, so one can be shared by many checks. Its
 * {@link #toString()} is its name, such as {@code equalTo(5)}.
 *
 * @param <A> the type of the values it tests
 */
public final class Assertion<A> {

    private final String name;
    private final Function<? super A, TestResult> test;

    private Assertion(String name, Function<? super A, TestResult> test) {
        this.name = name;
        this.test = test;
    }

    /**
     * An assertion made of a name and a test.
     *
     * @param name what the assertion checks, used when {@link #not(Assertion)} or {@link #exists(Assertion)} names
     *             it, such as {@code isEven}
     * @param test tests a value, returning a failure that explains why the value does not satisfy it
     * @param <A>  the type of the values it tests
     * @return a new assertion
     * @throws NullPointerException if an argument is null
     */
    public static <A> Assertion<A> of(String name, Function<? super A, TestResult> test) {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(test, "test is null");
        return new Assertion<>(name, test);
    }

    /// A leaf assertion: `explain(value)` is the explanation when `holds(value)` is false.
    private static <A> Assertion<A> leaf(String name, java.util.function.Predicate<? super A> holds, Function<? super A, String> explain) {
        return new Assertion<>(name, value -> holds.test(value) ? TestResult.succeed() : TestResult.fail(explain.apply(value)));
    }

    /**
     * Tests a value.
     *
     * @param value the value
     * @return a success, or a failure that explains why {@code value} does not satisfy this assertion
     * @throws NullPointerException if the test returned null
     */
    public TestResult test(A value) {
        return Objects.requireNonNull(test.apply(value), () -> "the test of " + name + " returned null");
    }

    // -- testing a value

    /**
     * Tests {@code value} against every assertion. The result fails when one of them fails, and its explanation
     * lists every failing assertion, in order. Every assertion is tested, even after one failed: an assertion that
     * throws on a value that failed an earlier one ends the check as erroneous, so a guard such as
     * {@code isNonEmpty()} does not protect the assertions after it (use {@link #or(Assertion)} or nest them).
     *
     * @param value      the value
     * @param assertions the assertions, at least one
     * @param <A>        the type of the value
     * @return the result of the assertions
     * @throws NullPointerException     if {@code assertions} or one of them is null
     * @throws IllegalArgumentException if no assertion is given
     */
    @SafeVarargs
    public static <A> TestResult assertThat(A value, Assertion<? super A>... assertions) {
        return all(assertions).test(value);
    }

    /**
     * Runs {@code code} and tests how it ended against every assertion, such as {@link #throwsA(Class)}. The result
     * fails when one of them fails, and its explanation lists every failing assertion, in order.
     *
     * @param code       the code to run
     * @param assertions the assertions, at least one
     * @return the result of the assertions
     * @throws NullPointerException     if {@code code}, {@code assertions} or one of them is null
     * @throws IllegalArgumentException if no assertion is given
     */
    @SafeVarargs
    public static TestResult assertThat(CheckedRunnable code, Assertion<? super CheckedRunnable>... assertions) {
        Objects.requireNonNull(code, "code is null");
        return all(assertions).test(code);
    }

    /// The assertions of a varargs call, combined with `and`.
    @SafeVarargs
    static <A> Assertion<A> all(Assertion<? super A>... assertions) {
        Objects.requireNonNull(assertions, "assertions is null");
        if (assertions.length == 0) {
            throw new IllegalArgumentException("at least one assertion is needed");
        }
        Assertion<A> all = narrow(Objects.requireNonNull(assertions[0], "assertions contains null"));
        for (int i = 1; i < assertions.length; i++) {
            all = all.and(Objects.requireNonNull(assertions[i], "assertions contains null"));
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private static <A> Assertion<A> narrow(Assertion<? super A> assertion) {
        return (Assertion<A>) assertion;
    }

    // -- composition

    /**
     * An assertion that holds when both hold. Both sides are tested, so that a failure explains every failing side:
     * {@code that} does not rely on this assertion holding, and an exception it throws on a value that fails this one
     * ends the check as erroneous.
     *
     * @param that the other assertion, possibly on a narrower type
     * @param <B>  the type of the values the combined assertion tests: {@code A}, or a narrower type that
     *             {@code that} needs, such as a {@code List} after an assertion on any {@code Iterable}
     * @return a new assertion
     * @throws NullPointerException if {@code that} is null
     */
    public <B extends A> Assertion<B> and(Assertion<? super B> that) {
        Objects.requireNonNull(that, "that is null");
        return new Assertion<B>("(" + name + " and " + that.name + ")", value -> test(value).and(that.test(value)));
    }

    /**
     * An assertion that holds when either holds. {@code that} is tested only when this assertion fails, so it may
     * rely on this one having failed; a failure explains both sides.
     *
     * @param that the other assertion, possibly on a narrower type
     * @param <B>  the type of the values the combined assertion tests: {@code A}, or a narrower type that
     *             {@code that} needs
     * @return a new assertion
     * @throws NullPointerException if {@code that} is null
     */
    public <B extends A> Assertion<B> or(Assertion<? super B> that) {
        Objects.requireNonNull(that, "that is null");
        return new Assertion<B>("(" + name + " or " + that.name + ")", value -> {
            final TestResult left = test(value);
            return left.isSuccess() ? left : left.or(that.test(value));
        });
    }

    /**
     * This assertion, named {@code label}: a failure starts with the label, and {@link #toString()} is the label.
     *
     * @param label a name for what this assertion checks
     * @return a new assertion
     * @throws NullPointerException if {@code label} is null
     */
    public Assertion<A> label(String label) {
        Objects.requireNonNull(label, "label is null");
        return new Assertion<>(label, value -> test(value).label(label));
    }

    /**
     * An assertion that holds when {@code assertion} does not.
     *
     * @param assertion the assertion to negate
     * @param <A>       the type of the values it tests
     * @return a new assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<A> not(Assertion<A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("not(" + assertion.name + ")", value -> assertion.test(value).isSuccess()
                ? TestResult.fail(show(value) + " satisfies " + assertion.name + ", but must not")
                : TestResult.succeed());
    }

    // -- values

    /**
     * An assertion that holds for every value.
     *
     * @param <A> the type of the values it tests
     * @return the assertion
     */
    public static <A> Assertion<A> anything() {
        return new Assertion<>("anything", value -> TestResult.succeed());
    }

    /**
     * An assertion that holds for a value equal to {@code expected}, as {@link Objects#deepEquals(Object, Object)}:
     * arrays are equal when their elements are.
     *
     * @param expected the expected value
     * @param <A>      the type of the values it tests
     * @return the assertion
     */
    public static <A> Assertion<A> equalTo(A expected) {
        return leaf("equalTo(" + show(expected) + ")", value -> Objects.deepEquals(value, expected),
                value -> show(value) + " is not equal to " + show(expected));
    }

    /**
     * An assertion that holds for {@code true}.
     *
     * @return the assertion
     */
    public static Assertion<Boolean> isTrue() {
        return leaf("isTrue", Boolean.TRUE::equals, value -> show(value) + " is not true");
    }

    /**
     * An assertion that holds for {@code false}.
     *
     * @return the assertion
     */
    public static Assertion<Boolean> isFalse() {
        return leaf("isFalse", Boolean.FALSE::equals, value -> show(value) + " is not false");
    }

    /**
     * An assertion that holds for a value greater than {@code reference}.
     *
     * @param reference the value to compare with
     * @param <A>       the type of the values it tests
     * @return the assertion
     * @throws NullPointerException if {@code reference} is null
     */
    public static <A extends Comparable<? super A>> Assertion<A> isGreaterThan(A reference) {
        Objects.requireNonNull(reference, "reference is null");
        return leaf("isGreaterThan(" + show(reference) + ")", value -> value.compareTo(reference) > 0,
                value -> show(value) + " is not greater than " + show(reference));
    }

    /**
     * An assertion that holds for a value greater than or equal to {@code reference}.
     *
     * @param reference the value to compare with
     * @param <A>       the type of the values it tests
     * @return the assertion
     * @throws NullPointerException if {@code reference} is null
     */
    public static <A extends Comparable<? super A>> Assertion<A> isGreaterThanOrEqualTo(A reference) {
        Objects.requireNonNull(reference, "reference is null");
        return leaf("isGreaterThanOrEqualTo(" + show(reference) + ")", value -> value.compareTo(reference) >= 0,
                value -> show(value) + " is less than " + show(reference));
    }

    /**
     * An assertion that holds for a value less than {@code reference}.
     *
     * @param reference the value to compare with
     * @param <A>       the type of the values it tests
     * @return the assertion
     * @throws NullPointerException if {@code reference} is null
     */
    public static <A extends Comparable<? super A>> Assertion<A> isLessThan(A reference) {
        Objects.requireNonNull(reference, "reference is null");
        return leaf("isLessThan(" + show(reference) + ")", value -> value.compareTo(reference) < 0,
                value -> show(value) + " is not less than " + show(reference));
    }

    /**
     * An assertion that holds for a value less than or equal to {@code reference}.
     *
     * @param reference the value to compare with
     * @param <A>       the type of the values it tests
     * @return the assertion
     * @throws NullPointerException if {@code reference} is null
     */
    public static <A extends Comparable<? super A>> Assertion<A> isLessThanOrEqualTo(A reference) {
        Objects.requireNonNull(reference, "reference is null");
        return leaf("isLessThanOrEqualTo(" + show(reference) + ")", value -> value.compareTo(reference) <= 0,
                value -> show(value) + " is greater than " + show(reference));
    }

    /**
     * An assertion that holds for a value between {@code min} and {@code max}, both included.
     *
     * @param min the smallest value
     * @param max the largest value
     * @param <A> the type of the values it tests
     * @return the assertion
     * @throws NullPointerException if an argument is null
     */
    public static <A extends Comparable<? super A>> Assertion<A> isWithin(A min, A max) {
        Objects.requireNonNull(min, "min is null");
        Objects.requireNonNull(max, "max is null");
        return leaf("isWithin(" + show(min) + ", " + show(max) + ")",
                value -> value.compareTo(min) >= 0 && value.compareTo(max) <= 0,
                value -> show(value) + " is not within " + show(min) + " and " + show(max));
    }

    // -- strings

    /**
     * An assertion that holds for a string that starts with {@code prefix}.
     *
     * @param prefix the prefix
     * @return the assertion
     * @throws NullPointerException if {@code prefix} is null
     */
    public static Assertion<String> startsWithString(String prefix) {
        Objects.requireNonNull(prefix, "prefix is null");
        return leaf("startsWithString(" + show(prefix) + ")", value -> value.startsWith(prefix),
                value -> show(value) + " does not start with " + show(prefix));
    }

    /**
     * An assertion that holds for a string that ends with {@code suffix}.
     *
     * @param suffix the suffix
     * @return the assertion
     * @throws NullPointerException if {@code suffix} is null
     */
    public static Assertion<String> endsWithString(String suffix) {
        Objects.requireNonNull(suffix, "suffix is null");
        return leaf("endsWithString(" + show(suffix) + ")", value -> value.endsWith(suffix),
                value -> show(value) + " does not end with " + show(suffix));
    }

    /**
     * An assertion that holds for a string that contains {@code part}.
     *
     * @param part the part
     * @return the assertion
     * @throws NullPointerException if {@code part} is null
     */
    public static Assertion<String> containsString(String part) {
        Objects.requireNonNull(part, "part is null");
        return leaf("containsString(" + show(part) + ")", value -> value.contains(part),
                value -> show(value) + " does not contain " + show(part));
    }

    /**
     * An assertion that holds for a string that the regular expression {@code regex} matches entirely.
     *
     * @param regex the regular expression, as {@link Pattern}
     * @return the assertion
     * @throws NullPointerException                   if {@code regex} is null
     * @throws java.util.regex.PatternSyntaxException if {@code regex} is not a regular expression
     */
    public static Assertion<String> matchesRegex(String regex) {
        final Pattern pattern = Pattern.compile(Objects.requireNonNull(regex, "regex is null"));
        return leaf("matchesRegex(" + show(regex) + ")", value -> pattern.matcher(value).matches(),
                value -> show(value) + " does not match " + show(regex));
    }

    // -- collections

    /**
     * An assertion that holds for an iterable without element.
     *
     * @return the assertion
     */
    public static Assertion<Iterable<?>> isEmpty() {
        return leaf("isEmpty", value -> !value.iterator().hasNext(), value -> show(value) + " is not empty");
    }

    /**
     * An assertion that holds for an iterable with at least one element.
     *
     * @return the assertion
     */
    public static Assertion<Iterable<?>> isNonEmpty() {
        return leaf("isNonEmpty", value -> value.iterator().hasNext(), value -> show(value) + " is empty");
    }

    /**
     * An assertion that holds for an iterable whose number of elements satisfies {@code size}.
     *
     * @param size the assertion on the number of elements
     * @return the assertion
     * @throws NullPointerException if {@code size} is null
     */
    public static Assertion<Iterable<?>> hasSize(Assertion<? super Integer> size) {
        Objects.requireNonNull(size, "size is null");
        return new Assertion<>("hasSize(" + size.name + ")", value -> {
            final int count = count(value);
            return nested(show(value) + " has size " + count, size.test(count));
        });
    }

    /**
     * An assertion that holds for an iterable with an element equal to {@code element}.
     *
     * @param element the element
     * @param <A>     the type of the elements
     * @return the assertion
     */
    public static <A> Assertion<Iterable<? extends A>> contains(A element) {
        return leaf("contains(" + show(element) + ")", value -> {
            for (A a : value) {
                if (Objects.equals(a, element)) {
                    return true;
                }
            }
            return false;
        }, value -> show(value) + " does not contain " + show(element));
    }

    /**
     * An assertion that holds for an iterable with at least one element that satisfies {@code assertion}.
     *
     * @param assertion the assertion on an element
     * @param <A>       the type of the elements
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Iterable<? extends A>> exists(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return leaf("exists(" + assertion.name + ")", value -> {
            for (A a : value) {
                if (assertion.test(a).isSuccess()) {
                    return true;
                }
            }
            return false;
        }, value -> show(value) + " has no element that satisfies " + assertion.name);
    }

    /**
     * An assertion that holds for an iterable whose every element satisfies {@code assertion}. A failure explains
     * the first element that does not.
     *
     * @param assertion the assertion on an element
     * @param <A>       the type of the elements
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Iterable<? extends A>> forall(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("forall(" + assertion.name + ")", value -> {
            int index = 0;
            for (A a : value) {
                final TestResult result = assertion.test(a);
                if (result.isFailure()) {
                    return nested(show(value) + " has " + show(a) + " at index " + index, result);
                }
                index++;
            }
            return TestResult.succeed();
        });
    }

    /**
     * An assertion that holds for an iterable whose first element satisfies {@code assertion}.
     *
     * @param assertion the assertion on the first element
     * @param <A>       the type of the elements
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Iterable<? extends A>> hasFirst(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("hasFirst(" + assertion.name + ")", value -> {
            final Iterator<? extends A> iterator = value.iterator();
            if (!iterator.hasNext()) {
                return TestResult.fail(show(value) + " has no first element");
            }
            final A first = iterator.next();
            return nested(show(value) + " has first element " + show(first), assertion.test(first));
        });
    }

    /**
     * An assertion that holds for an iterable whose last element satisfies {@code assertion}.
     *
     * @param assertion the assertion on the last element
     * @param <A>       the type of the elements
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Iterable<? extends A>> hasLast(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("hasLast(" + assertion.name + ")", value -> {
            final Iterator<? extends A> iterator = value.iterator();
            if (!iterator.hasNext()) {
                return TestResult.fail(show(value) + " has no last element");
            }
            A last = iterator.next();
            while (iterator.hasNext()) {
                last = iterator.next();
            }
            return nested(show(value) + " has last element " + show(last), assertion.test(last));
        });
    }

    /**
     * An assertion that holds for an iterable whose element at {@code index} (counted from 0 in iteration order)
     * satisfies {@code assertion}.
     *
     * @param index     the index
     * @param assertion the assertion on the element
     * @param <A>       the type of the elements
     * @return the assertion
     * @throws NullPointerException     if {@code assertion} is null
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public static <A> Assertion<Iterable<? extends A>> hasAt(int index, Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        Gen.requireNonNegative(index, "index");
        return new Assertion<>("hasAt(" + index + ", " + assertion.name + ")", value -> {
            int i = 0;
            for (A a : value) {
                if (i++ == index) {
                    return nested(show(value) + " has " + show(a) + " at index " + index, assertion.test(a));
                }
            }
            return TestResult.fail(show(value) + " has no element at index " + index);
        });
    }

    /**
     * An assertion that holds for an iterable with the same elements as {@code expected}, each as many times, in any
     * order.
     *
     * @param expected the expected elements
     * @param <A>      the type of the elements
     * @return the assertion
     * @throws NullPointerException if {@code expected} is null
     */
    public static <A> Assertion<Iterable<? extends A>> hasSameElements(Iterable<? extends A> expected) {
        Objects.requireNonNull(expected, "expected is null");
        final Map<Object, Integer> counts = counts(expected);
        return leaf("hasSameElements(" + show(expected) + ")", value -> counts(value).equals(counts),
                value -> show(value) + " does not have the same elements as " + show(expected));
    }

    /**
     * An assertion that holds for an iterable whose elements are in their natural order (equal neighbours allowed).
     *
     * @param <A> the type of the elements
     * @return the assertion
     */
    public static <A extends Comparable<? super A>> Assertion<Iterable<? extends A>> isSorted() {
        return isSorted("isSorted", Comparator.naturalOrder());
    }

    /**
     * An assertion that holds for an iterable whose elements are in the order of {@code comparator} (equal
     * neighbours allowed).
     *
     * @param comparator the order
     * @param <A>        the type of the elements
     * @return the assertion
     * @throws NullPointerException if {@code comparator} is null
     */
    public static <A> Assertion<Iterable<? extends A>> isSorted(Comparator<? super A> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return isSorted("isSorted(a comparator)", comparator);
    }

    private static <A> Assertion<Iterable<? extends A>> isSorted(String name, Comparator<? super A> comparator) {
        return new Assertion<>(name, value -> {
            final Iterator<? extends A> iterator = value.iterator();
            if (!iterator.hasNext()) {
                return TestResult.succeed();
            }
            A previous = iterator.next();
            while (iterator.hasNext()) {
                final A next = iterator.next();
                if (comparator.compare(previous, next) > 0) {
                    return TestResult.fail(show(value) + " is not sorted: " + show(previous) + " comes before " + show(next));
                }
                previous = next;
            }
            return TestResult.succeed();
        });
    }

    // -- Zazr types

    /**
     * An assertion that holds for a {@code Some} whose value satisfies {@code assertion}.
     *
     * @param assertion the assertion on the value
     * @param <A>       the type of the value
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Option<? extends A>> isSome(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isSome(" + assertion.name + ")", value -> switch (value) {
            case Option.Some<? extends A>(var a) -> nested(show(value) + " holds " + show(a), assertion.test(a));
            case Option.None<? extends A> ignored -> TestResult.fail(show(value) + " is not a Some");
        });
    }

    /**
     * An assertion that holds for {@code None}.
     *
     * @return the assertion
     */
    public static Assertion<Option<?>> isNone() {
        return leaf("isNone", Option::isEmpty, value -> show(value) + " is not None");
    }

    /**
     * An assertion that holds for a {@code Left} whose value satisfies {@code assertion}.
     *
     * @param assertion the assertion on the left value
     * @param <L>       the type of the left value
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <L> Assertion<Either<? extends L, ?>> isLeft(Assertion<? super L> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isLeft(" + assertion.name + ")", value -> value.isLeft()
                ? nested(show(value) + " holds " + show(value.getLeft()), assertion.test(value.getLeft()))
                : TestResult.fail(show(value) + " is not a Left"));
    }

    /**
     * An assertion that holds for a {@code Right} whose value satisfies {@code assertion}.
     *
     * @param assertion the assertion on the right value
     * @param <R>       the type of the right value
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <R> Assertion<Either<?, ? extends R>> isRight(Assertion<? super R> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isRight(" + assertion.name + ")", value -> value.isRight()
                ? nested(show(value) + " holds " + show(value.get()), assertion.test(value.get()))
                : TestResult.fail(show(value) + " is not a Right"));
    }

    /**
     * An assertion that holds for a {@code Success} whose value satisfies {@code assertion}.
     *
     * @param assertion the assertion on the value
     * @param <A>       the type of the value
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Try<? extends A>> isSuccess(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isSuccess(" + assertion.name + ")", value -> switch (value) {
            case Try.Success<? extends A>(var a) -> nested(show(value) + " holds " + show(a), assertion.test(a));
            case Try.Failure<? extends A> ignored -> TestResult.fail(show(value) + " is not a Success");
        });
    }

    /**
     * An assertion that holds for a {@code Failure} whose cause satisfies {@code assertion}.
     *
     * @param assertion the assertion on the cause
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static Assertion<Try<?>> isFailure(Assertion<? super Throwable> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isFailure(" + assertion.name + ")", value -> switch (value) {
            case Try.Failure<?>(var cause) -> nested(show(value) + " holds " + show(cause), assertion.test(cause));
            case Try.Success<?> ignored -> TestResult.fail(show(value) + " is not a Failure");
        });
    }

    /**
     * An assertion that holds for a {@code Valid} whose value satisfies {@code assertion}.
     *
     * @param assertion the assertion on the value
     * @param <A>       the type of the value
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <A> Assertion<Validation<?, ? extends A>> isValid(Assertion<? super A> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isValid(" + assertion.name + ")", value -> switch (value) {
            case Validation.Valid<?, ? extends A>(var a) -> nested(show(value) + " holds " + show(a), assertion.test(a));
            case Validation.Invalid<?, ? extends A> ignored -> TestResult.fail(show(value) + " is not Valid");
        });
    }

    /**
     * An assertion that holds for an {@code Invalid} whose errors satisfy {@code assertion}.
     *
     * @param assertion the assertion on the errors
     * @param <E>       the type of the errors
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static <E> Assertion<Validation<? extends E, ?>> isInvalid(Assertion<? super NonEmptyVector<E>> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("isInvalid(" + assertion.name + ")", value -> switch (value) {
            case Validation.Invalid<? extends E, ?> invalid -> {
                // an immutable vector of a subtype of E is a vector of E
                @SuppressWarnings("unchecked")
                final NonEmptyVector<E> errors = (NonEmptyVector<E>) invalid.errors();
                yield nested(show(value) + " holds " + show(errors), assertion.test(errors));
            }
            case Validation.Valid<? extends E, ?> ignored -> TestResult.fail(show(value) + " is not Invalid");
        });
    }

    // -- code that must throw

    /**
     * An assertion on code that holds when running it throws an instance of {@code type}. Use it with
     * {@link #assertThat(CheckedRunnable, Assertion[])}.
     *
     * @param type the class of the expected exception
     * @return the assertion
     * @throws NullPointerException if {@code type} is null
     */
    public static Assertion<CheckedRunnable> throwsA(Class<? extends Throwable> type) {
        Objects.requireNonNull(type, "type is null");
        return throwsWith(leaf("isA(" + type.getName() + ")", type::isInstance,
                thrown -> show(thrown) + " is not a " + type.getName())).label0("throwsA(" + type.getName() + ")");
    }

    /**
     * An assertion on code that holds when running it throws something that satisfies {@code assertion}. Use it
     * with {@link #assertThat(CheckedRunnable, Assertion[])}.
     *
     * @param assertion the assertion on what the code throws
     * @return the assertion
     * @throws NullPointerException if {@code assertion} is null
     */
    public static Assertion<CheckedRunnable> throwsWith(Assertion<? super Throwable> assertion) {
        Objects.requireNonNull(assertion, "assertion is null");
        return new Assertion<>("throwsWith(" + assertion.name + ")", code -> {
            try {
                code.run();
            } catch (Throwable thrown) {
                return nested("the code threw " + show(thrown), assertion.test(thrown));
            }
            return TestResult.fail("the code did not throw");
        });
    }

    /// This assertion under another name, its explanations unchanged.
    private Assertion<A> label0(String newName) {
        return new Assertion<>(newName, test);
    }

    // -- rendering

    /// A failure of `inner`, under a line that says what was tested; a success stays a success.
    private static TestResult nested(String context, TestResult inner) {
        return inner instanceof TestResult.Failure(var explanation)
                ? TestResult.fail(context + ":\n  " + explanation.replace("\n", "\n  "))
                : inner;
    }

    /// A value as the explanations show it: strings and characters quoted, arrays by their elements, code that is
    /// run as "the code" (its `toString` changes from one run to the next).
    static String show(Object value) {
        return switch (value) {
            case null -> "null";
            case String s -> "\"" + s + "\"";
            case Character c -> "'" + c + "'";
            case CheckedRunnable ignored -> "the code";
            case Object array when array.getClass().isArray() -> arrayString(array);
            default -> String.valueOf(value);
        };
    }

    /// The elements of an array of objects or of primitives, as `Arrays.deepToString` writes them.
    private static String arrayString(Object array) {
        final String wrapped = java.util.Arrays.deepToString(new Object[] { array });
        return wrapped.substring(1, wrapped.length() - 1);
    }

    private static int count(Iterable<?> iterable) {
        if (iterable instanceof java.util.Collection<?> collection) {
            return collection.size();
        }
        int count = 0;
        for (Object ignored : iterable) {
            count++;
        }
        return count;
    }

    private static Map<Object, Integer> counts(Iterable<?> iterable) {
        final Map<Object, Integer> counts = new HashMap<>();
        for (Object element : iterable) {
            counts.merge(element, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * The name of this assertion, such as {@code equalTo(5)} or {@code isSome(isGreaterThan(3))}.
     *
     * @return the name
     */
    @Override
    public String toString() {
        return name;
    }
}
