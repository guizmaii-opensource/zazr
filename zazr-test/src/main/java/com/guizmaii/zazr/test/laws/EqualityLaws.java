package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.test.Check;
import com.guizmaii.zazr.test.Gen;

import java.util.Objects;

/**
 * The contract of {@code equals} and {@code hashCode}, and an {@code equals} that agrees with a reference model.
 */
public final class EqualityLaws {

    private EqualityLaws() {
    }

    /**
     * For values {@code a} and {@code b}: {@code a} equals itself and its copy, the copy equals {@code a}, no value
     * equals {@code null}, {@code a.equals(b) == b.equals(a)}, and equal values have equal hash codes.
     *
     * @param <T> the type under test
     * @return the law
     */
    public static <T> Law<EqualitySubject<T>> equalsHashCodeConsistency() {
        return Law.of("equalsHashCodeConsistency", (subject, config) -> Check.check(config, subject.values(), subject.values(),
                (a, b) -> {
                    final T copy = subject.copy().apply(a);
                    return consistent(a, a) && consistent(a, copy)
                            && Results.check(copy.equals(a), () -> "the copy " + copy + " differs from " + a)
                            && Results.check(!a.equals(null), () -> a + " equals null")
                            && consistent(a, b);
                }));
    }

    /**
     * For values {@code a} and {@code b}: {@code a.equals(b)} holds exactly when their models are equal, both ways.
     * One pair in three is a value and its copy, one in three is two values drawn at a size of at most
     * {@value #SMALL_SIZE} so that values differing in one component only are common, and the rest are two values
     * drawn independently.
     *
     * @param <T> the type under test
     * @return the law
     */
    public static <T> Law<EqualitySubject<T>> equalsAgreesWithModel() {
        return Law.of("equalsAgreesWithModel", (subject, config) -> Check.check(config, pairs(subject),
                pair -> {
                    final T a = pair._1();
                    final T b = pair._2();
                    final Object modelA = subject.model().apply(a);
                    final Object modelB = subject.model().apply(b);
                    final boolean expected = Objects.equals(modelA, modelB);
                    return Results.check(a.equals(b) == expected && b.equals(a) == expected,
                            () -> a + (expected ? " differs from " : " equals ") + b + " but their models are "
                                    + modelA + " and " + modelB);
                }));
    }

    /**
     * {@link #equalsHashCodeConsistency()} and {@link #equalsAgreesWithModel()}.
     *
     * @param <T> the type under test
     * @return the law set
     */
    public static <T> Laws<EqualitySubject<T>> all() {
        return Laws.of(equalsHashCodeConsistency(), equalsAgreesWithModel());
    }

    /// The largest size of the small draws of {@link #equalsAgreesWithModel()}.
    static final int SMALL_SIZE = 2;

    private static <T> Gen<Tuple2<T, T>> pairs(EqualitySubject<T> subject) {
        final Gen<T> values = subject.values();
        final Gen<Tuple2<T, T>> copies = values.map(a -> Tuple.of(a, subject.copy().apply(a)));
        final Gen<Tuple2<T, T>> small = Gen.sized(size -> Gen.zip(values, values).withSize(Math.min(size, SMALL_SIZE)));
        final Gen<Tuple2<T, T>> independent = Gen.zip(values, values);
        return Gen.oneOf(copies, small, independent);
    }

    /// `a.equals(b) == b.equals(a)`, and equal values have equal hash codes; throws an `AssertionError` otherwise.
    static boolean consistent(Object a, Object b) {
        final boolean ab = a.equals(b);
        if (ab != b.equals(a)) {
            throw new AssertionError("equals is not symmetric between " + a + " and " + b);
        } else if (ab && a.hashCode() != b.hashCode()) {
            throw new AssertionError(a + " and " + b + " are equal but hash to " + a.hashCode() + " and " + b.hashCode());
        }
        return true;
    }
}
