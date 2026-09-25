package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.Tuple4;
import com.guizmaii.zazr.Tuple5;
import com.guizmaii.zazr.Tuple6;
import com.guizmaii.zazr.Tuple7;
import com.guizmaii.zazr.Tuple8;
import com.guizmaii.zazr.test.Arbitrary;
import org.junit.jupiter.api.Test;

/**
 * The laws of the tuples {@code Tuple2} to {@code Tuple8}: {@code equals} and {@code hashCode}, with the components
 * as a JDK list for the model.
 */
class TupleLawsTest {

    private static final Arbitrary<Integer> INTS = Arbitrary.integer();

    private static EqualitySubject<Tuple2<Integer, Integer>> tuple2() {
        return new EqualitySubject<>(Arbitrary.tuple2(INTS, INTS), t -> Tuple.of(t._1(), t._2()), t -> java.util.List.of(t._1(), t._2()));
    }

    @Test
    void tuple2EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple2<Integer, Integer>>equalsHashCodeConsistency(), tuple2());
    }

    @Test
    void tuple2EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple2<Integer, Integer>>equalsAgreesWithModel(), tuple2());
    }

    private static EqualitySubject<Tuple3<Integer, Integer, Integer>> tuple3() {
        return new EqualitySubject<>(Arbitrary.tuple3(INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3()), t -> java.util.List.of(t._1(), t._2(), t._3()));
    }

    @Test
    void tuple3EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple3<Integer, Integer, Integer>>equalsHashCodeConsistency(), tuple3());
    }

    @Test
    void tuple3EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple3<Integer, Integer, Integer>>equalsAgreesWithModel(), tuple3());
    }

    private static EqualitySubject<Tuple4<Integer, Integer, Integer, Integer>> tuple4() {
        return new EqualitySubject<>(Arbitrary.tuple4(INTS, INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3(), t._4()), t -> java.util.List.of(t._1(), t._2(), t._3(), t._4()));
    }

    @Test
    void tuple4EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple4<Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(), tuple4());
    }

    @Test
    void tuple4EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple4<Integer, Integer, Integer, Integer>>equalsAgreesWithModel(), tuple4());
    }

    private static EqualitySubject<Tuple5<Integer, Integer, Integer, Integer, Integer>> tuple5() {
        return new EqualitySubject<>(Arbitrary.tuple5(INTS, INTS, INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5()), t -> java.util.List.of(t._1(), t._2(), t._3(), t._4(), t._5()));
    }

    @Test
    void tuple5EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple5<Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(), tuple5());
    }

    @Test
    void tuple5EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple5<Integer, Integer, Integer, Integer, Integer>>equalsAgreesWithModel(), tuple5());
    }

    private static EqualitySubject<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> tuple6() {
        return new EqualitySubject<>(Arbitrary.tuple6(INTS, INTS, INTS, INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6()), t -> java.util.List.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6()));
    }

    @Test
    void tuple6EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(), tuple6());
    }

    @Test
    void tuple6EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>>equalsAgreesWithModel(), tuple6());
    }

    private static EqualitySubject<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> tuple7() {
        return new EqualitySubject<>(Arbitrary.tuple7(INTS, INTS, INTS, INTS, INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7()), t -> java.util.List.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7()));
    }

    @Test
    void tuple7EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(), tuple7());
    }

    @Test
    void tuple7EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>>equalsAgreesWithModel(), tuple7());
    }

    private static EqualitySubject<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> tuple8() {
        return new EqualitySubject<>(Arbitrary.tuple8(INTS, INTS, INTS, INTS, INTS, INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8()), t -> java.util.List.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8()));
    }

    @Test
    void tuple8EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(), tuple8());
    }

    @Test
    void tuple8EqualsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>>equalsAgreesWithModel(), tuple8());
    }
}
