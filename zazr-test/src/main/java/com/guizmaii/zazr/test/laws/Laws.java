package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.legacy.CheckResult;
import com.guizmaii.zazr.test.legacy.Checkable;

import java.util.Objects;
import java.util.Random;

/**
 * An ordered set of laws checked against one subject. Law sets compose with {@link #and(Laws)}: a set of laws over a
 * narrow subject (say {@link MapSubject}) and a set over another ({@link ZipSubject}) combine into a set over any
 * subject that provides both.
 * <p>
 * {@link #check(Object, Random, int, int)} runs every law, not only up to the first failure, and
 * {@link #assertSatisfied(Object, Random, int, int)} reports each failing law by name with its counterexample.
 *
 * @param <S> the subject the laws are checked against
 */
public final class Laws<S> {

    private final Vector<Law<? super S>> laws;

    private Laws(Vector<Law<? super S>> laws) {
        this.laws = laws;
    }

    /**
     * Creates a law set.
     *
     * @param laws the laws, in the order they are checked
     * @param <S>  the subject the laws are checked against
     * @return a new law set
     * @throws NullPointerException if {@code laws} or one of them is null
     */
    @SafeVarargs
    public static <S> Laws<S> of(Law<? super S>... laws) {
        Objects.requireNonNull(laws, "laws is null");
        Vector<Law<? super S>> all = Vector.empty();
        for (Law<? super S> law : laws) {
            all = all.append(Objects.requireNonNull(law, "law is null"));
        }
        return new Laws<>(all);
    }

    /**
     * The laws of this set, in the order they are checked.
     *
     * @return the laws
     */
    public Vector<Law<? super S>> laws() {
        return laws;
    }

    /**
     * A law set holding the laws of this set followed by those of {@code other}.
     *
     * @param other the next laws
     * @param <T>   a subject that both sets can be checked against
     * @return a new law set
     * @throws NullPointerException if {@code other} is null
     */
    public <T extends S> Laws<T> and(Laws<? super T> other) {
        Objects.requireNonNull(other, "other is null");
        Vector<Law<? super T>> all = Vector.empty();
        for (Law<? super S> law : laws) {
            all = all.append(law);
        }
        for (Law<?> law : other.laws) {
            @SuppressWarnings("unchecked")
            final Law<? super T> checked = (Law<? super T>) law;
            all = all.append(checked);
        }
        return new Laws<>(all);
    }

    /**
     * A law set holding the laws of this set followed by {@code other}.
     *
     * @param other the next law
     * @param <T>   a subject that the laws of this set and {@code other} can be checked against
     * @return a new law set
     * @throws NullPointerException if {@code other} is null
     */
    public <T extends S> Laws<T> and(Law<? super T> other) {
        return and(Laws.<T>of(other));
    }

    /**
     * Checks every law of this set against a subject.
     *
     * @param subject what the laws are checked against
     * @param random  the source of randomness, shared by the laws in order
     * @param size    the size hint given to the arbitraries
     * @param tries   the number of samples per law
     * @return one result per law, in order, each named after its law
     * @throws NullPointerException if {@code subject} or {@code random} is null
     */
    public Vector<CheckResult> check(S subject, Random random, int size, int tries) {
        Objects.requireNonNull(subject, "subject is null");
        Objects.requireNonNull(random, "random is null");
        return laws.map(law -> checkOne(law, subject, random, size, tries));
    }

    /**
     * Checks every law of this set and throws when one of them is not satisfied.
     *
     * @param subject what the laws are checked against
     * @param random  the source of randomness, shared by the laws in order
     * @param size    the size hint given to the arbitraries
     * @param tries   the number of samples per law
     * @throws AssertionError       naming every law that was falsified or erroneous, with its counterexample
     * @throws NullPointerException if {@code subject} or {@code random} is null
     */
    public void assertSatisfied(S subject, Random random, int size, int tries) {
        final Vector<CheckResult> failures = check(subject, random, size, tries).filter(result -> !result.isSatisfied());
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.map(Laws::describe).mkString(failures.size() + " law(s) failed:\n", "\n", ""));
        }
    }

    /**
     * Checks every law of this set with {@link Checkable#DEFAULT_SIZE} and {@link Checkable#DEFAULT_TRIES} and throws
     * when one of them is not satisfied.
     *
     * @param subject what the laws are checked against
     * @param random  the source of randomness, shared by the laws in order
     * @throws AssertionError       naming every law that was falsified or erroneous, with its counterexample
     * @throws NullPointerException if an argument is null
     */
    public void assertSatisfied(S subject, Random random) {
        assertSatisfied(subject, random, Checkable.DEFAULT_SIZE, Checkable.DEFAULT_TRIES);
    }

    /**
     * Describes a failed law in one line: its name, the kind of failure and the counterexample.
     *
     * @param result the result of a law check
     * @return a description of the failure
     */
    static String describe(CheckResult result) {
        return switch (result) {
            case CheckResult.Satisfied satisfied -> satisfied.propertyName() + ": satisfied";
            case CheckResult.Falsified falsified -> falsified.propertyName() + ": falsified at check " + falsified.count()
                    + " by " + falsified.counterexample() + falsified.message().map(m -> " (" + m + ")").getOrElse("");
            case CheckResult.Erroneous erroneous -> erroneous.propertyName() + ": erroneous at check " + erroneous.count()
                    + erroneous.sample().map(sample -> " with " + sample).getOrElse("") + ": " + erroneous.cause();
        };
    }

    private static <S> CheckResult checkOne(Law<? super S> law, S subject, Random random, int size, int tries) {
        return law.check(subject, random, size, tries);
    }

    @Override
    public String toString() {
        return laws.map(Law::name).mkString("Laws(", ", ", ")");
    }
}
