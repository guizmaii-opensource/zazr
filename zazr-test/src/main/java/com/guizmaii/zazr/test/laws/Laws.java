package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.test.CheckConfig;

import java.util.Objects;

/**
 * An ordered set of laws checked against one subject. Law sets compose with {@link #and(Laws)}: a set of laws over a
 * narrow subject (say {@link MapSubject}) and a set over another ({@link ZipSubject}) combine into a set over any
 * subject that provides both.
 * <p>
 * {@link #check(Object, CheckConfig)} runs every law, not only up to the first failure, and
 * {@link #assertSatisfied(Object, CheckConfig)} reports each failing law by name with its counterexample and the seed
 * that replays it.
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
     * Checks every law of this set against a subject, each with the same configuration, so the seed of any failure
     * replays it.
     *
     * @param subject what the laws are checked against
     * @param config  the number of samples, the size and the seed of each law's check
     * @return one result per law, in order
     * @throws NullPointerException if an argument is null
     */
    public Vector<LawResult> check(S subject, CheckConfig config) {
        Objects.requireNonNull(subject, "subject is null");
        Objects.requireNonNull(config, "config is null");
        return laws.map(law -> checkOne(law, subject, config));
    }

    /**
     * Checks every law of this set against a subject with {@link CheckConfig#defaults()}.
     *
     * @param subject what the laws are checked against
     * @return one result per law, in order
     * @throws NullPointerException if {@code subject} is null
     */
    public Vector<LawResult> check(S subject) {
        return check(subject, CheckConfig.defaults());
    }

    /**
     * Checks every law of this set and throws when one of them is not satisfied.
     *
     * @param subject what the laws are checked against
     * @param config  the number of samples, the size and the seed of each law's check
     * @throws AssertionError       naming every law that was falsified or erroneous, with its sample number, its
     *                              counterexample, the explanation and the seed that replays it
     * @throws NullPointerException if an argument is null
     */
    public void assertSatisfied(S subject, CheckConfig config) {
        final Vector<LawResult> failures = check(subject, config).filter(result -> !result.isSatisfied());
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.map(LawResult::describe).mkString(failures.size() + " law(s) failed:\n", "\n", ""));
        }
    }

    /**
     * Checks every law of this set with {@link CheckConfig#defaults()} and throws when one of them is not satisfied.
     *
     * @param subject what the laws are checked against
     * @throws AssertionError       naming every law that was falsified or erroneous, as
     *                              {@link #assertSatisfied(Object, CheckConfig)}
     * @throws NullPointerException if {@code subject} is null
     */
    public void assertSatisfied(S subject) {
        assertSatisfied(subject, CheckConfig.defaults());
    }

    private static <S> LawResult checkOne(Law<? super S> law, S subject, CheckConfig config) {
        return new LawResult(law.name(), law.check(subject, config));
    }

    @Override
    public String toString() {
        return laws.map(Law::name).mkString("Laws(", ", ", ")");
    }
}
