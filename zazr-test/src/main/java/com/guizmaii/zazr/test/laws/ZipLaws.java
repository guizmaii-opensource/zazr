package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.test.legacy.Arbitrary;
import com.guizmaii.zazr.test.legacy.Property;

/**
 * The laws of {@code zip}, {@code zipLeft} and {@code zipRight}.
 */
public final class ZipLaws {

    private ZipLaws() {
    }

    /**
     * {@code fa.zip(fb).zip(fc)} equals {@code fa.zip(fb.zip(fc))} once both are flattened to triples.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<ZipSubject<F>> zipAssociativity() {
        return Law.of("zipAssociativity", subject -> Property.named("zipAssociativity")
                .forAll(subject.values(), subject.values(), subject.values())
                .suchThatResult((fa, fb, fc) -> Results.equal(
                        subject.map(subject.zip(subject.zip(fa, fb), fc), ZipLaws::flattenLeft),
                        subject.map(subject.zip(fa, subject.zip(fb, fc)), ZipLaws::flattenRight))));
    }

    /**
     * {@code fa.zipLeft(succeed(a))} equals {@code fa}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<ZipSidesSubject<F>> zipLeftIdentity() {
        return Law.of("zipLeftIdentity", subject -> Property.named("zipLeftIdentity")
                .forAll(subject.values(), Arbitrary.integer())
                .suchThatResult((fa, a) -> Results.equal(subject.zipLeft(fa, subject.succeed(a)), fa)));
    }

    /**
     * {@code succeed(a).zipRight(fa)} equals {@code fa}.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<ZipSidesSubject<F>> zipRightIdentity() {
        return Law.of("zipRightIdentity", subject -> Property.named("zipRightIdentity")
                .forAll(subject.values(), Arbitrary.integer())
                .suchThatResult((fa, a) -> Results.equal(subject.zipRight(subject.succeed(a), fa), fa)));
    }

    /**
     * {@code fa.zipLeft(fb)} equals {@code fa.zip(fb).map(Tuple2::_1)}: it combines both sides as {@code zip} does.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<ZipSidesSubject<F>> zipLeftIsZipThenFirst() {
        return Law.of("zipLeftIsZipThenFirst", subject -> Property.named("zipLeftIsZipThenFirst")
                .forAll(subject.values(), subject.values())
                .suchThatResult((fa, fb) -> Results.equal(subject.zipLeft(fa, fb),
                        subject.map(subject.zip(fa, fb), t -> ((Tuple2<?, ?>) t)._1()))));
    }

    /**
     * {@code fa.zipRight(fb)} equals {@code fa.zip(fb).map(Tuple2::_2)}: it combines both sides as {@code zip} does.
     *
     * @param <F> the type under test
     * @return the law
     */
    public static <F> Law<ZipSidesSubject<F>> zipRightIsZipThenSecond() {
        return Law.of("zipRightIsZipThenSecond", subject -> Property.named("zipRightIsZipThenSecond")
                .forAll(subject.values(), subject.values())
                .suchThatResult((fa, fb) -> Results.equal(subject.zipRight(fa, fb),
                        subject.map(subject.zip(fa, fb), t -> ((Tuple2<?, ?>) t)._2()))));
    }

    /**
     * {@link #zipAssociativity()}.
     *
     * @param <F> the type under test
     * @return the law set
     */
    public static <F> Laws<ZipSubject<F>> zip() {
        return Laws.of(zipAssociativity());
    }

    /**
     * {@link #zipAssociativity()}, {@link #zipLeftIdentity()}, {@link #zipRightIdentity()},
     * {@link #zipLeftIsZipThenFirst()} and {@link #zipRightIsZipThenSecond()}.
     *
     * @param <F> the type under test
     * @return the law set
     */
    public static <F> Laws<ZipSidesSubject<F>> zipSides() {
        return ZipLaws.<F>zip().<ZipSidesSubject<F>>and(zipLeftIdentity())
                .and(zipRightIdentity())
                .and(zipLeftIsZipThenFirst())
                .and(zipRightIsZipThenSecond());
    }

    private static Object flattenLeft(Object nested) {
        final Tuple2<?, ?> outer = (Tuple2<?, ?>) nested;
        final Tuple2<?, ?> inner = (Tuple2<?, ?>) outer._1();
        return Tuple.of(inner._1(), inner._2(), outer._2());
    }

    private static Object flattenRight(Object nested) {
        final Tuple2<?, ?> outer = (Tuple2<?, ?>) nested;
        final Tuple2<?, ?> inner = (Tuple2<?, ?>) outer._2();
        return Tuple.of(outer._1(), inner._1(), inner._2());
    }
}
