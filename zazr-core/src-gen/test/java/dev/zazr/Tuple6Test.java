package dev.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import dev.zazr.collection.List;
import dev.zazr.collection.Vector;
import java.util.Comparator;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Tuple6Test {

    @Test
    public void shouldCreateTuple() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple = createTuple();
        assertThat(tuple).isNotNull();
    }

    @Test
    public void shouldGetArity() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple = createTuple();
        assertThat(tuple.arity()).isEqualTo(6);
    }

    @Test
    public void shouldReturnElements() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(6);
    }

    @Test
    public void shouldUpdate1() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple =
                createIntTuple(1, 2, 3, 4, 5, 6).update1(42);
        assertThat(tuple._1()).isEqualTo(42);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(6);
    }

    @Test
    public void shouldUpdate2() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple =
                createIntTuple(1, 2, 3, 4, 5, 6).update2(42);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(42);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(6);
    }

    @Test
    public void shouldUpdate3() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple =
                createIntTuple(1, 2, 3, 4, 5, 6).update3(42);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(42);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(6);
    }

    @Test
    public void shouldUpdate4() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple =
                createIntTuple(1, 2, 3, 4, 5, 6).update4(42);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(42);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(6);
    }

    @Test
    public void shouldUpdate5() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple =
                createIntTuple(1, 2, 3, 4, 5, 6).update5(42);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(42);
        assertThat(tuple._6()).isEqualTo(6);
    }

    @Test
    public void shouldUpdate6() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple =
                createIntTuple(1, 2, 3, 4, 5, 6).update6(42);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(42);
    }

    @Test
    public void shouldConvertToVector() {
        Vector<?> actual = createIntTuple(1, 0, 0, 0, 0, 0).toVector();
        assertThat(actual).isEqualTo(Vector.of(1, 0, 0, 0, 0, 0));
    }

    @Test
    public void shouldCompareEqual() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        assertThat(t0.compareTo(t0)).isZero();
        assertThat(intTupleComparator.compare(t0, t0)).isZero();
    }

    @Test
    public void shouldThrowWhenComparingToNull() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        assertThrows(NullPointerException.class, () -> t0.compareTo(null));
    }

    @Test
    public void shouldCompare1stArg() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t1 = createIntTuple(1, 0, 0, 0, 0, 0);
        assertThat(t0.compareTo(t1)).isNegative();
        assertThat(t1.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t1)).isNegative();
        assertThat(intTupleComparator.compare(t1, t0)).isPositive();
    }

    @Test
    public void shouldCompare2ndArg() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t2 = createIntTuple(0, 1, 0, 0, 0, 0);
        assertThat(t0.compareTo(t2)).isNegative();
        assertThat(t2.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t2)).isNegative();
        assertThat(intTupleComparator.compare(t2, t0)).isPositive();
    }

    @Test
    public void shouldCompare3rdArg() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t3 = createIntTuple(0, 0, 1, 0, 0, 0);
        assertThat(t0.compareTo(t3)).isNegative();
        assertThat(t3.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t3)).isNegative();
        assertThat(intTupleComparator.compare(t3, t0)).isPositive();
    }

    @Test
    public void shouldCompare4thArg() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t4 = createIntTuple(0, 0, 0, 1, 0, 0);
        assertThat(t0.compareTo(t4)).isNegative();
        assertThat(t4.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t4)).isNegative();
        assertThat(intTupleComparator.compare(t4, t0)).isPositive();
    }

    @Test
    public void shouldCompare5thArg() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t5 = createIntTuple(0, 0, 0, 0, 1, 0);
        assertThat(t0.compareTo(t5)).isNegative();
        assertThat(t5.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t5)).isNegative();
        assertThat(intTupleComparator.compare(t5, t0)).isPositive();
    }

    @Test
    public void shouldCompare6thArg() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t6 = createIntTuple(0, 0, 0, 0, 0, 1);
        assertThat(t0.compareTo(t6)).isNegative();
        assertThat(t6.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t6)).isNegative();
        assertThat(intTupleComparator.compare(t6, t0)).isPositive();
    }

    @Test
    public void shouldMap() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple = createTuple();
        Tuple6<Object, Object, Object, Object, Object, Object> actual = tuple.map((o1, o2, o3, o4, o5, o6) -> tuple);
        assertThat(actual).isEqualTo(tuple);
    }

    @Test
    public void shouldMapComponents() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple = createTuple();
        Function<Object, Object> f1 = Function.identity();
        Function<Object, Object> f2 = Function.identity();
        Function<Object, Object> f3 = Function.identity();
        Function<Object, Object> f4 = Function.identity();
        Function<Object, Object> f5 = Function.identity();
        Function<Object, Object> f6 = Function.identity();
        Tuple6<Object, Object, Object, Object, Object, Object> actual = tuple.map(f1, f2, f3, f4, f5, f6);
        assertThat(actual).isEqualTo(tuple);
    }

    @Test
    public void shouldReturnTuple6OfUnzip6() {
        List<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> iterable = List.of(
                Tuple.of(2, 3, 4, 5, 6, 7),
                Tuple.of(4, 5, 6, 7, 8, 9),
                Tuple.of(6, 7, 8, 9, 10, 11),
                Tuple.of(8, 9, 10, 11, 12, 13),
                Tuple.of(10, 11, 12, 13, 14, 15),
                Tuple.of(12, 13, 14, 15, 16, 17));
        Tuple6<Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>>
                expected = Tuple.of(
                        Vector.of(2, 4, 6, 8, 10, 12),
                        Vector.of(3, 5, 7, 9, 11, 13),
                        Vector.of(4, 6, 8, 10, 12, 14),
                        Vector.of(5, 7, 9, 11, 13, 15),
                        Vector.of(6, 8, 10, 12, 14, 16),
                        Vector.of(7, 9, 11, 13, 15, 17));
        assertThat(Tuple.unzip6(iterable)).isEqualTo(expected);
    }

    @Test
    public void shouldUnzip6Nothing() {
        Tuple6<Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>>
                expected = Tuple.of(
                        Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty());
        assertThat(Tuple.unzip6(List.<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>>empty()))
                .isEqualTo(expected);
        assertThat(Tuple.unzip6(List.<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>>empty())
                        ._1())
                .isSameAs(Vector.empty());
    }

    @Test
    public void shouldRejectNullOnUnzip6() {
        assertThrows(NullPointerException.class, () -> Tuple.unzip6(null));
        assertThrows(
                NullPointerException.class,
                () -> Tuple.unzip6(List.of(Tuple.of(
                        (Integer) null, (Integer) null, (Integer) null, (Integer) null, (Integer) null, (Integer)
                                null))));
    }

    @Test
    public void shouldReturnTuple6OfUnzip1() {
        List<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> iterable =
                List.of(Tuple.of(1, 2, 3, 4, 5, 6));
        Tuple6<Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>>
                expected = Tuple.of(Vector.of(1), Vector.of(2), Vector.of(3), Vector.of(4), Vector.of(5), Vector.of(6));
        assertThat(Tuple.unzip6(iterable)).isEqualTo(expected);
    }

    @Test
    public void shouldMap1stComponent() {
        Tuple6<String, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple.of(1, 1, 1, 1, 1, 1).map1(i -> "X");
        Tuple6<String, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of("X", 1, 1, 1, 1, 1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap2ndComponent() {
        Tuple6<Integer, String, Integer, Integer, Integer, Integer> actual =
                Tuple.of(1, 1, 1, 1, 1, 1).map2(i -> "X");
        Tuple6<Integer, String, Integer, Integer, Integer, Integer> expected = Tuple.of(1, "X", 1, 1, 1, 1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap3rdComponent() {
        Tuple6<Integer, Integer, String, Integer, Integer, Integer> actual =
                Tuple.of(1, 1, 1, 1, 1, 1).map3(i -> "X");
        Tuple6<Integer, Integer, String, Integer, Integer, Integer> expected = Tuple.of(1, 1, "X", 1, 1, 1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap4thComponent() {
        Tuple6<Integer, Integer, Integer, String, Integer, Integer> actual =
                Tuple.of(1, 1, 1, 1, 1, 1).map4(i -> "X");
        Tuple6<Integer, Integer, Integer, String, Integer, Integer> expected = Tuple.of(1, 1, 1, "X", 1, 1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap5thComponent() {
        Tuple6<Integer, Integer, Integer, Integer, String, Integer> actual =
                Tuple.of(1, 1, 1, 1, 1, 1).map5(i -> "X");
        Tuple6<Integer, Integer, Integer, Integer, String, Integer> expected = Tuple.of(1, 1, 1, 1, "X", 1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap6thComponent() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, String> actual =
                Tuple.of(1, 1, 1, 1, 1, 1).map6(i -> "X");
        Tuple6<Integer, Integer, Integer, Integer, Integer, String> expected = Tuple.of(1, 1, 1, 1, 1, "X");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldApplyTuple() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple = createTuple();
        Tuple0 actual = tuple.apply((o1, o2, o3, o4, o5, o6) -> Tuple0.instance());
        assertThat(actual).isEqualTo(Tuple0.instance());
    }

    @Test
    public void shouldAppendValue() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple.of(1, 2, 3, 4, 5, 6).append(7);
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple1() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple.of(1, 2, 3, 4, 5, 6).concat(Tuple.of(7));
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple2() {
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple.of(1, 2, 3, 4, 5, 6).concat(Tuple.of(7, 8));
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected =
                Tuple.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRecognizeEquality() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple1 = createTuple();
        Tuple6<Object, Object, Object, Object, Object, Object> tuple2 = createTuple();
        assertThat((Object) tuple1).isEqualTo(tuple2);
    }

    @Test
    public void shouldRecognizeNonEquality() {
        Tuple6<Object, Object, Object, Object, Object, Object> tuple = createTuple();
        Object other = new Object();
        assertThat(tuple).isNotEqualTo(other);
    }

    @Test
    public void shouldRecognizeNonEqualityPerComponent() {
        Tuple6<String, String, String, String, String, String> tuple = Tuple.of("1", "2", "3", "4", "5", "6");
        assertThat(tuple.equals(Tuple.of("X", "2", "3", "4", "5", "6"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "X", "3", "4", "5", "6"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "X", "4", "5", "6"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "X", "5", "6"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "4", "X", "6"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "4", "5", "X"))).isFalse();
    }

    @Test
    public void shouldComputeCorrectHashCode() {
        assertThat(createTuple().hashCode()).isEqualTo(createTuple().hashCode());
        assertThat(createIntTuple(0, 0, 0, 0, 0, 0).hashCode())
                .isEqualTo(createIntTuple(0, 0, 0, 0, 0, 0).hashCode());
        assertThat(createIntTuple(0, 0, 0, 0, 0, 0).hashCode())
                .isNotEqualTo(createIntTuple(1, 0, 0, 0, 0, 0).hashCode());
    }

    @Test
    public void shouldDeconstructWithRecordPattern() {
        Object o = createIntTuple(1, 2, 3, 4, 5, 6);
        if (o instanceof Tuple6(var v1, var v2, var v3, var v4, var v5, var v6)) {
            assertThat(v1).isEqualTo(1);
            assertThat(v2).isEqualTo(2);
            assertThat(v3).isEqualTo(3);
            assertThat(v4).isEqualTo(4);
            assertThat(v5).isEqualTo(5);
            assertThat(v6).isEqualTo(6);
        } else {
            throw new AssertionError("record pattern did not match");
        }
    }

    @Test
    public void shouldImplementToString() {
        String actual = createTuple().toString();
        String expected = "(null, null, null, null, null, null)";
        assertThat(actual).isEqualTo(expected);
    }

    private Comparator<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> intTupleComparator =
            Tuple6.comparator(
                    Integer::compare,
                    Integer::compare,
                    Integer::compare,
                    Integer::compare,
                    Integer::compare,
                    Integer::compare);

    private Tuple6<Object, Object, Object, Object, Object, Object> createTuple() {
        return new Tuple6<>(null, null, null, null, null, null);
    }

    private Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> createIntTuple(
            Integer i1, Integer i2, Integer i3, Integer i4, Integer i5, Integer i6) {
        return new Tuple6<>(i1, i2, i3, i4, i5, i6);
    }
}
