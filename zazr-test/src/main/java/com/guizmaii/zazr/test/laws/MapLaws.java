package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.legacy.Property;

import java.util.function.Function;

/**
 * The laws of {@code map}: mapping the identity changes nothing, and mapping twice is mapping the composition.
 */
public final class MapLaws {

    private MapLaws() {
    }

    /**
     * {@code fa.map(x -> x)} equals {@code fa}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<MapSubject<F>> mapIdentity() {
        return Law.of("mapIdentity", subject -> Property.named("mapIdentity")
                .forAll(subject.values())
                .suchThatResult(fa -> Results.equal(subject.map(fa, Function.identity()), fa)));
    }

    /**
     * {@code fa.map(f).map(g)} equals {@code fa.map(f.andThen(g))}, for integer functions {@code f} and {@code g}
     * drawn from a family of affine functions, some folding onto a small range.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<MapSubject<F>> mapComposition() {
        return Law.of("mapComposition", subject -> Property.named("mapComposition")
                .forAll(subject.values(), Functions.integers().arbitrary(), Functions.integers().arbitrary())
                .suchThatResult((fa, f, g) -> Results.equal(subject.map(subject.map(fa, f), g), subject.map(fa, f.andThen(g)))));
    }

    /**
     * {@link #mapIdentity()} and {@link #mapComposition()}.
     *
     * @param <F> the type under test
     * @return the law set
     */
    public static <F> Laws<MapSubject<F>> all() {
        return Laws.of(mapIdentity(), mapComposition());
    }
}
