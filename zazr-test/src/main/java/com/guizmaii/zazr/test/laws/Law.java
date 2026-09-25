package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.CheckResult;
import com.guizmaii.zazr.test.Checkable;
import com.guizmaii.zazr.test.Property;

import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/**
 * A named rule that every value of a type must satisfy, stated once and checked against any subject that provides
 * the operations it needs.
 * <p>
 * The subject {@code S} is what the law is checked against: the arbitrary values of the type under test and the
 * operations the law calls on them (see {@link MapSubject}, {@link FlatMapSubject}, {@link ZipSubject},
 * {@link CollectionSubject}). A law builds its property with {@link Property#named(String)} under its own name, so a
 * falsified {@link CheckResult} carries the law's name and the counterexample.
 *
 * @param <S> the subject the law is checked against
 */
public final class Law<S> {

    private final String name;
    private final Function<? super S, ? extends Checkable> property;

    private Law(String name, Function<? super S, ? extends Checkable> property) {
        this.name = name;
        this.property = property;
    }

    /**
     * Creates a law.
     *
     * @param name     the law's name, reported when it fails
     * @param property builds the property of the law for a subject; it should name the property with {@code name}
     * @param <S>      the subject the law is checked against
     * @return a new law
     * @throws NullPointerException     if an argument is null
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static <S> Law<S> of(String name, Function<? super S, ? extends Checkable> property) {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(property, "property is null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name is blank");
        }
        return new Law<>(name, property);
    }

    /**
     * The law's name.
     *
     * @return the name given to {@link #of(String, Function)}
     */
    public String name() {
        return name;
    }

    /**
     * The property of this law for one subject.
     *
     * @param subject what the law is checked against
     * @return a checkable property
     * @throws NullPointerException if {@code subject} is null
     */
    public Checkable property(S subject) {
        Objects.requireNonNull(subject, "subject is null");
        return property.apply(subject);
    }

    /**
     * Checks this law against a subject.
     *
     * @param subject what the law is checked against
     * @param random  the source of randomness
     * @param size    the size hint given to the arbitraries
     * @param tries   the number of samples
     * @return the result of the check, named after this law
     * @throws NullPointerException if {@code subject} or {@code random} is null
     */
    public CheckResult check(S subject, Random random, int size, int tries) {
        Objects.requireNonNull(random, "random is null");
        return property(subject).check(random, size, tries);
    }

    /**
     * Checks this law against a subject with {@link Checkable#DEFAULT_SIZE} and {@link Checkable#DEFAULT_TRIES}.
     *
     * @param subject what the law is checked against
     * @param random  the source of randomness
     * @return the result of the check, named after this law
     * @throws NullPointerException if an argument is null
     */
    public CheckResult check(S subject, Random random) {
        return check(subject, random, Checkable.DEFAULT_SIZE, Checkable.DEFAULT_TRIES);
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
