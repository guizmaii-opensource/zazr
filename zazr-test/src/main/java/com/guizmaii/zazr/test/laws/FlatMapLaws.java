package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.Property;

import java.util.function.Function;

/**
 * The laws of {@code flatMap} and {@code succeed}. The functions passed to {@code flatMap} return values drawn from
 * the subject's own arbitrary, at a size of at most {@value #FUNCTION_SIZE} so that nested collections stay small.
 */
public final class FlatMapLaws {

    /// The size hint of the values returned by the generated functions.
    static final int FUNCTION_SIZE = 8;

    private FlatMapLaws() {
    }

    /**
     * {@code fa.flatMap(f).flatMap(g)} equals {@code fa.flatMap(x -> f(x).flatMap(g))}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<FlatMapSubject<F>> flatMapAssociativity() {
        return Law.of("flatMapAssociativity", subject -> {
            final Arbitrary<Function<Object, F>> functions = functions(subject);
            return Property.named("flatMapAssociativity")
                    .forAll(subject.values(), functions, functions)
                    .suchThatResult((fa, f, g) -> Results.equal(
                            subject.flatMap(subject.flatMap(fa, f), g),
                            subject.flatMap(fa, x -> subject.flatMap(f.apply(x), g))));
        });
    }

    /**
     * {@code succeed(a).flatMap(f)} equals {@code f(a)}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<FlatMapSubject<F>> flatMapLeftIdentity() {
        return Law.of("flatMapLeftIdentity", subject -> Property.named("flatMapLeftIdentity")
                .forAll(Arbitrary.integer(), functions(subject))
                .suchThatResult((a, f) -> Results.equal(subject.flatMap(subject.succeed(a), f), f.apply(a))));
    }

    /**
     * {@code fa.flatMap(x -> succeed(x))} equals {@code fa}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<FlatMapSubject<F>> flatMapRightIdentity() {
        return Law.of("flatMapRightIdentity", subject -> Property.named("flatMapRightIdentity")
                .forAll(subject.values())
                .suchThatResult(fa -> Results.equal(subject.flatMap(fa, subject::succeed), fa)));
    }

    /**
     * {@code fa.map(f)} equals {@code fa.flatMap(x -> succeed(f(x)))}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<FlatMapSubject<F>> mapIsFlatMapSucceed() {
        return Law.of("mapIsFlatMapSucceed", subject -> Property.named("mapIsFlatMapSucceed")
                .forAll(subject.values(), Functions.integers().arbitrary())
                .suchThatResult((fa, f) -> Results.equal(subject.map(fa, f), subject.flatMap(fa, x -> subject.succeed(f.apply(x))))));
    }

    /**
     * {@link #flatMapAssociativity()}, {@link #flatMapLeftIdentity()}, {@link #flatMapRightIdentity()} and
     * {@link #mapIsFlatMapSucceed()}.
     *
     * @param <F> the type under test
     * @return the law set
     */
    public static <F> Laws<FlatMapSubject<F>> all() {
        return Laws.of(flatMapAssociativity(), flatMapLeftIdentity(), flatMapRightIdentity(), mapIsFlatMapSucceed());
    }

    private static <F> Arbitrary<Function<Object, F>> functions(FlatMapSubject<F> subject) {
        return size -> Functions.to(subject.values(), Math.min(size, FUNCTION_SIZE));
    }
}
