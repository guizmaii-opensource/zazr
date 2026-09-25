package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.PredicateResult;
import com.guizmaii.zazr.test.Property;

/**
 * The contract of {@code equals} and {@code hashCode}.
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
     * {@link #equalsHashCodeConsistency()}.
     *
     * @param <T> the type under test
     * @return the law set
     */
    public static <T> Laws<EqualitySubject<T>> all() {
        return Laws.of(equalsHashCodeConsistency());
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
