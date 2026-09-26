package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.Check;
import com.guizmaii.zazr.test.CheckConfig;
import com.guizmaii.zazr.test.CheckResult;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A named rule that every value of a type must satisfy, stated once and checked against any subject that provides
 * the operations it needs.
 * <p>
 * The subject {@code S} is what the law is checked against: the generator of the values of the type under test and
 * the operations the law calls on them (see {@link MapSubject}, {@link FlatMapSubject}, {@link ZipSubject},
 * {@link CollectionSubject}). A law runs one {@link Check#check} with the configuration it is given; its body fails
 * a sample by throwing an {@link AssertionError} that explains what differed.
 *
 * @param <S> the subject the law is checked against
 */
public final class Law<S> {

    private final String name;
    private final BiFunction<? super S, ? super CheckConfig, ? extends CheckResult> check;

    private Law(String name, BiFunction<? super S, ? super CheckConfig, ? extends CheckResult> check) {
        this.name = name;
        this.check = check;
    }

    /**
     * Creates a law.
     *
     * @param name  the law's name, reported when it fails
     * @param check checks the law for a subject with a configuration, usually through {@link Check#check}
     * @param <S>   the subject the law is checked against
     * @return a new law
     * @throws NullPointerException     if an argument is null
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static <S> Law<S> of(String name, BiFunction<? super S, ? super CheckConfig, ? extends CheckResult> check) {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(check, "check is null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name is blank");
        }
        return new Law<>(name, check);
    }

    /**
     * The law's name.
     *
     * @return the name given to {@link #of(String, BiFunction)}
     */
    public String name() {
        return name;
    }

    /**
     * Checks this law against a subject.
     *
     * @param subject what the law is checked against
     * @param config  the number of samples, the size and the seed
     * @return the result of the check
     * @throws NullPointerException if an argument is null, or the law's check returned null
     */
    public CheckResult check(S subject, CheckConfig config) {
        Objects.requireNonNull(subject, "subject is null");
        Objects.requireNonNull(config, "config is null");
        return Objects.requireNonNull(check.apply(subject, config), () -> "the check of " + name + " returned null");
    }

    /**
     * Checks this law against a subject with {@link CheckConfig#defaults()}.
     *
     * @param subject what the law is checked against
     * @return the result of the check
     * @throws NullPointerException if {@code subject} is null
     */
    public CheckResult check(S subject) {
        return check(subject, CheckConfig.defaults());
    }

    /**
     * A law set holding this law followed by {@code other}.
     *
     * @param other the next law
     * @param <T>   the subject of both laws
     * @return a new law set
     * @throws NullPointerException if {@code other} is null
     */
    public <T extends S> Laws<T> and(Law<? super T> other) {
        return Laws.<T>of(this).and(other);
    }

    @Override
    public String toString() {
        return "Law(" + name + ")";
    }
}
