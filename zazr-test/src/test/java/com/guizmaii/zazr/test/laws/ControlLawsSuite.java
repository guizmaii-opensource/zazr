package com.guizmaii.zazr.test.laws;

import org.junit.jupiter.api.Test;

/**
 * The laws of a control type: {@code map}, {@code flatMap}, {@code zip}, {@code zipLeft}, {@code zipRight}, and
 * {@code equals}/{@code hashCode}. A subclass per type supplies the subject.
 *
 * @param <F> the type under test, with a wildcard element type
 * @param <S> its subject
 */
abstract class ControlLawsSuite<F, S extends FlatMapSubject<F> & ZipSidesSubject<F>> {

    abstract S subject();

    abstract EqualitySubject<F> equality();

    @Test
    void mapIdentity() {
        LawChecks.check(MapLaws.<F>mapIdentity(), subject());
    }

    @Test
    void mapComposition() {
        LawChecks.check(MapLaws.<F>mapComposition(), subject());
    }

    @Test
    void flatMapAssociativity() {
        LawChecks.check(FlatMapLaws.<F>flatMapAssociativity(), subject());
    }

    @Test
    void flatMapLeftIdentity() {
        LawChecks.check(FlatMapLaws.<F>flatMapLeftIdentity(), subject());
    }

    @Test
    void flatMapRightIdentity() {
        LawChecks.check(FlatMapLaws.<F>flatMapRightIdentity(), subject());
    }

    @Test
    void mapIsFlatMapSucceed() {
        LawChecks.check(FlatMapLaws.<F>mapIsFlatMapSucceed(), subject());
    }

    @Test
    void zipAssociativity() {
        LawChecks.check(ZipLaws.<F>zipAssociativity(), subject());
    }

    @Test
    void zipLeftIdentity() {
        LawChecks.check(ZipLaws.<F>zipLeftIdentity(), subject());
    }

    @Test
    void zipRightIdentity() {
        LawChecks.check(ZipLaws.<F>zipRightIdentity(), subject());
    }

    @Test
    void zipLeftIsZipThenFirst() {
        LawChecks.check(ZipLaws.<F>zipLeftIsZipThenFirst(), subject());
    }

    @Test
    void zipRightIsZipThenSecond() {
        LawChecks.check(ZipLaws.<F>zipRightIsZipThenSecond(), subject());
    }

    @Test
    void equalsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<F>equalsHashCodeConsistency(), equality());
    }
}
