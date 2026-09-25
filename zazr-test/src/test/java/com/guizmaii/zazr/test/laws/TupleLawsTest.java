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
 * The laws of the tuples {@code Tuple2} to {@code Tuple8}: {@code equals} and {@code hashCode}.
 */
class TupleLawsTest {

    private static final Arbitrary<Integer> INTS = Arbitrary.integer();

    @Test
    void tuple2EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple2<Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple2(INTS, INTS), t -> Tuple.of(t._1(), t._2())));
    }

    @Test
    void tuple3EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple3<Integer, Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple3(INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3())));
    }

    @Test
    void tuple4EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple4<Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple4(INTS, INTS, INTS, INTS), t -> Tuple.of(t._1(), t._2(), t._3(), t._4())));
    }

    @Test
    void tuple5EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple5<Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple5(INTS, INTS, INTS, INTS, INTS),
                        t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5())));
    }

    @Test
    void tuple6EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple6(INTS, INTS, INTS, INTS, INTS, INTS),
                        t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6())));
    }

    @Test
    void tuple7EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple7(INTS, INTS, INTS, INTS, INTS, INTS, INTS),
                        t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7())));
    }

    @Test
    void tuple8EqualsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>>equalsHashCodeConsistency(),
                new EqualitySubject<>(Arbitrary.tuple8(INTS, INTS, INTS, INTS, INTS, INTS, INTS, INTS),
                        t -> Tuple.of(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8())));
    }
}
