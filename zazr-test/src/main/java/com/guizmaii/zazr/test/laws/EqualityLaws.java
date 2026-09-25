package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.Gen;
import com.guizmaii.zazr.test.PredicateResult;
import com.guizmaii.zazr.test.Property;

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
        return Law.of("equalsHashCodeConsistency", subject -> Property.named("equalsHashCodeConsistency")
                .forAll(subject.values(), subject.values())
                .suchThatResult((a, b) -> {
                    final T copy = subject.copy().apply(a);
                    return Results.both(consistent(a, a), Results.both(consistent(a, copy),
                            Results.both(Results.check(copy.equals(a), "the copy " + copy + " differs from " + a),
                                    Results.both(Results.check(!a.equals(null), a + " equals null"), consistent(a, b)))));
                }));
    }

    /**
     * For values {@code a} and {@code b}: {@code a.equals(b)} holds exactly when their models are equal, both ways.
     * One pair in three is a value and its copy, one in three is drawn at a size of at most {@value #SMALL_SIZE} so
     * that values differing in one component only are common, and the rest are drawn independently.
     *
     * @param <T> the type under test
     * @return the law
     */
    public static <T> Law<EqualitySubject<T>> equalsAgreesWithModel() {
        return Law.of("equalsAgreesWithModel", subject -> Property.named("equalsAgreesWithModel")
                .forAll(pairs(subject))
                .suchThatResult(pair -> {
                    final T a = pair._1();
                    final T b = pair._2();
                    final Object modelA = subject.model().apply(a);
                    final Object modelB = subject.model().apply(b);
                    final boolean expected = Objects.equals(modelA, modelB);
                    return Results.check(a.equals(b) == expected && b.equals(a) == expected,
                            a + (expected ? " differs from " : " equals ") + b + " but their models are "
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

    /// The size hint of the small draws of {@link #equalsAgreesWithModel()}.
    static final int SMALL_SIZE = 2;

    private static <T> Arbitrary<Tuple2<T, T>> pairs(EqualitySubject<T> subject) {
        return size -> {
            final Gen<T> full = subject.values().apply(size);
            final Gen<T> small = subject.values().apply(Math.min(size, SMALL_SIZE));
            return random -> switch (random.nextInt(3)) {
                case 0 -> {
                    final T a = full.apply(random);
                    yield Tuple.of(a, subject.copy().apply(a));
                }
                case 1 -> Tuple.of(small.apply(random), small.apply(random));
                default -> Tuple.of(full.apply(random), full.apply(random));
            };
        };
    }

    /// `a.equals(b) == b.equals(a)`, and equal values have equal hash codes.
    static PredicateResult consistent(Object a, Object b) {
        final boolean ab = a.equals(b);
        if (ab != b.equals(a)) {
            return PredicateResult.failure("equals is not symmetric between " + a + " and " + b);
        } else if (ab && a.hashCode() != b.hashCode()) {
            return PredicateResult.failure(a + " and " + b + " are equal but hash to " + a.hashCode() + " and " + b.hashCode());
        } else {
            return PredicateResult.success();
        }
    }
}
