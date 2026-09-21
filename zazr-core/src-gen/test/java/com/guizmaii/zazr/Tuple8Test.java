package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Vector;
import java.util.Comparator;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class Tuple8Test {

    @Test
    public void shouldCreateTuple() {
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple = createTuple();
        assertThat(tuple).isNotNull();
    }

    @Test
    public void shouldGetArity() {
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple = createTuple();
        assertThat(tuple.arity()).isEqualTo(8);
    }

    @Test
    public void shouldReturnElements() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
        assertThat(tuple._4()).isEqualTo(4);
        assertThat(tuple._5()).isEqualTo(5);
        assertThat(tuple._6()).isEqualTo(6);
        assertThat(tuple._7()).isEqualTo(7);
        assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate1() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update1(42);
      assertThat(tuple._1()).isEqualTo(42);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate2() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update2(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(42);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate3() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update3(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(42);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate4() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update4(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(42);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate5() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update5(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(42);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate6() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update6(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(42);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate7() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update7(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(42);
      assertThat(tuple._8()).isEqualTo(8);
    }

    @Test
    public void shouldUpdate8() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8).update8(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
      assertThat(tuple._4()).isEqualTo(4);
      assertThat(tuple._5()).isEqualTo(5);
      assertThat(tuple._6()).isEqualTo(6);
      assertThat(tuple._7()).isEqualTo(7);
      assertThat(tuple._8()).isEqualTo(42);
    }

    @Test
    public void shouldConvertToVector() {
        final Vector<?> actual = createIntTuple(1, 0, 0, 0, 0, 0, 0, 0).toVector();
        assertThat(actual).isEqualTo(Vector.of(1, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    public void shouldCompareEqual() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(t0.compareTo(t0)).isZero();
        assertThat(intTupleComparator.compare(t0, t0)).isZero();
    }

    @Test
    public void shouldThrowWhenComparingToNull() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        assertThrows(NullPointerException.class, () -> t0.compareTo(null));
    }

    @Test
    public void shouldCompare1stArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t1 = createIntTuple(1, 0, 0, 0, 0, 0, 0, 0);
        assertThat(t0.compareTo(t1)).isNegative();
        assertThat(t1.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t1)).isNegative();
        assertThat(intTupleComparator.compare(t1, t0)).isPositive();
    }

    @Test
    public void shouldCompare2ndArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t2 = createIntTuple(0, 1, 0, 0, 0, 0, 0, 0);
        assertThat(t0.compareTo(t2)).isNegative();
        assertThat(t2.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t2)).isNegative();
        assertThat(intTupleComparator.compare(t2, t0)).isPositive();
    }

    @Test
    public void shouldCompare3rdArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t3 = createIntTuple(0, 0, 1, 0, 0, 0, 0, 0);
        assertThat(t0.compareTo(t3)).isNegative();
        assertThat(t3.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t3)).isNegative();
        assertThat(intTupleComparator.compare(t3, t0)).isPositive();
    }

    @Test
    public void shouldCompare4thArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t4 = createIntTuple(0, 0, 0, 1, 0, 0, 0, 0);
        assertThat(t0.compareTo(t4)).isNegative();
        assertThat(t4.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t4)).isNegative();
        assertThat(intTupleComparator.compare(t4, t0)).isPositive();
    }

    @Test
    public void shouldCompare5thArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t5 = createIntTuple(0, 0, 0, 0, 1, 0, 0, 0);
        assertThat(t0.compareTo(t5)).isNegative();
        assertThat(t5.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t5)).isNegative();
        assertThat(intTupleComparator.compare(t5, t0)).isPositive();
    }

    @Test
    public void shouldCompare6thArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t6 = createIntTuple(0, 0, 0, 0, 0, 1, 0, 0);
        assertThat(t0.compareTo(t6)).isNegative();
        assertThat(t6.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t6)).isNegative();
        assertThat(intTupleComparator.compare(t6, t0)).isPositive();
    }

    @Test
    public void shouldCompare7thArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t7 = createIntTuple(0, 0, 0, 0, 0, 0, 1, 0);
        assertThat(t0.compareTo(t7)).isNegative();
        assertThat(t7.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t7)).isNegative();
        assertThat(intTupleComparator.compare(t7, t0)).isPositive();
    }

    @Test
    public void shouldCompare8thArg() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 0);
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t8 = createIntTuple(0, 0, 0, 0, 0, 0, 0, 1);
        assertThat(t0.compareTo(t8)).isNegative();
        assertThat(t8.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t8)).isNegative();
        assertThat(intTupleComparator.compare(t8, t0)).isPositive();
    }

    @Test
    public void shouldMap() {
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple = createTuple();
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> actual = tuple.map((o1, o2, o3, o4, o5, o6, o7, o8) -> tuple);
        assertThat(actual).isEqualTo(tuple);
    }

    @Test
    public void shouldMapComponents() {
      final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple = createTuple();
      final Function<Object, Object> f1 = Function.identity();
      final Function<Object, Object> f2 = Function.identity();
      final Function<Object, Object> f3 = Function.identity();
      final Function<Object, Object> f4 = Function.identity();
      final Function<Object, Object> f5 = Function.identity();
      final Function<Object, Object> f6 = Function.identity();
      final Function<Object, Object> f7 = Function.identity();
      final Function<Object, Object> f8 = Function.identity();
      final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> actual = tuple.map(f1, f2, f3, f4, f5, f6, f7, f8);
      assertThat(actual).isEqualTo(tuple);
    }

    @Test
    public void shouldReturnTuple8OfUnzip8() {
      final List<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> iterable = List.of(Tuple.of(2, 3, 4, 5, 6, 7, 8, 9), Tuple.of(4, 5, 6, 7, 8, 9, 10, 11), Tuple.of(6, 7, 8, 9, 10, 11, 12, 13), Tuple.of(8, 9, 10, 11, 12, 13, 14, 15), Tuple.of(10, 11, 12, 13, 14, 15, 16, 17), Tuple.of(12, 13, 14, 15, 16, 17, 18, 19), Tuple.of(14, 15, 16, 17, 18, 19, 20, 21), Tuple.of(16, 17, 18, 19, 20, 21, 22, 23));
      final Tuple8<Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>> expected = Tuple.of(Vector.of(2, 4, 6, 8, 10, 12, 14, 16), Vector.of(3, 5, 7, 9, 11, 13, 15, 17), Vector.of(4, 6, 8, 10, 12, 14, 16, 18), Vector.of(5, 7, 9, 11, 13, 15, 17, 19), Vector.of(6, 8, 10, 12, 14, 16, 18, 20), Vector.of(7, 9, 11, 13, 15, 17, 19, 21), Vector.of(8, 10, 12, 14, 16, 18, 20, 22), Vector.of(9, 11, 13, 15, 17, 19, 21, 23));
      assertThat(Tuple.unzip8(iterable)).isEqualTo(expected);
    }

    @Test
    public void shouldUnzip8Nothing() {
      final Tuple8<Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>> expected = Tuple.of(Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty(), Vector.empty());
      assertThat(Tuple.unzip8(List.<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> empty())).isEqualTo(expected);
      assertThat(Tuple.unzip8(List.<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> empty())._1()).isSameAs(Vector.empty());
    }

    @Test
    public void shouldRejectNullOnUnzip8() {
      assertThrows(NullPointerException.class, () -> Tuple.unzip8(null));
      assertThrows(NullPointerException.class, () -> Tuple.unzip8(List.of(Tuple.of((Integer) null, (Integer) null, (Integer) null, (Integer) null, (Integer) null, (Integer) null, (Integer) null, (Integer) null))));
    }

    @Test
    public void shouldReturnTuple8OfUnzip1() {
      final List<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> iterable = List.of(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8));
      final Tuple8<Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>, Vector<Integer>> expected = Tuple.of(Vector.of(1), Vector.of(2), Vector.of(3), Vector.of(4), Vector.of(5), Vector.of(6), Vector.of(7), Vector.of(8));
      assertThat(Tuple.unzip8(iterable)).isEqualTo(expected);
    }

    @Test
    public void shouldMap1stComponent() {
      final Tuple8<String, Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map1(i -> "X");
      final Tuple8<String, Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of("X", 1, 1, 1, 1, 1, 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap2ndComponent() {
      final Tuple8<Integer, String, Integer, Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map2(i -> "X");
      final Tuple8<Integer, String, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, "X", 1, 1, 1, 1, 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap3rdComponent() {
      final Tuple8<Integer, Integer, String, Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map3(i -> "X");
      final Tuple8<Integer, Integer, String, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 1, "X", 1, 1, 1, 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap4thComponent() {
      final Tuple8<Integer, Integer, Integer, String, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map4(i -> "X");
      final Tuple8<Integer, Integer, Integer, String, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 1, 1, "X", 1, 1, 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap5thComponent() {
      final Tuple8<Integer, Integer, Integer, Integer, String, Integer, Integer, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map5(i -> "X");
      final Tuple8<Integer, Integer, Integer, Integer, String, Integer, Integer, Integer> expected = Tuple.of(1, 1, 1, 1, "X", 1, 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap6thComponent() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, String, Integer, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map6(i -> "X");
      final Tuple8<Integer, Integer, Integer, Integer, Integer, String, Integer, Integer> expected = Tuple.of(1, 1, 1, 1, 1, "X", 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap7thComponent() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, String, Integer> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map7(i -> "X");
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, String, Integer> expected = Tuple.of(1, 1, 1, 1, 1, 1, "X", 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap8thComponent() {
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, String> actual = Tuple.of(1, 1, 1, 1, 1, 1, 1, 1).map8(i -> "X");
      final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, String> expected = Tuple.of(1, 1, 1, 1, 1, 1, 1, "X");
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldApplyTuple() {
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple = createTuple();
        final Tuple0 actual = tuple.apply((o1, o2, o3, o4, o5, o6, o7, o8) -> Tuple0.instance());
        assertThat(actual).isEqualTo(Tuple0.instance());
    }

    @Test
    public void shouldRecognizeEquality() {
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple1 = createTuple();
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple2 = createTuple();
        assertThat((Object) tuple1).isEqualTo(tuple2);
    }

    @Test
    public void shouldRecognizeNonEquality() {
        final Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> tuple = createTuple();
        final Object other = new Object();
        assertThat(tuple).isNotEqualTo(other);
    }

    @Test
    public void shouldRecognizeNonEqualityPerComponent() {
        final Tuple8<String, String, String, String, String, String, String, String> tuple = Tuple.of("1", "2", "3", "4", "5", "6", "7", "8");
        assertThat(tuple.equals(Tuple.of("X", "2", "3", "4", "5", "6", "7", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "X", "3", "4", "5", "6", "7", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "X", "4", "5", "6", "7", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "X", "5", "6", "7", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "4", "X", "6", "7", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "4", "5", "X", "7", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "4", "5", "6", "X", "8"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "3", "4", "5", "6", "7", "X"))).isFalse();
    }

    @Test
    public void shouldComputeCorrectHashCode() {
        assertThat(createTuple().hashCode()).isEqualTo(createTuple().hashCode());
        assertThat(createIntTuple(0, 0, 0, 0, 0, 0, 0, 0).hashCode()).isEqualTo(createIntTuple(0, 0, 0, 0, 0, 0, 0, 0).hashCode());
        assertThat(createIntTuple(0, 0, 0, 0, 0, 0, 0, 0).hashCode()).isNotEqualTo(createIntTuple(1, 0, 0, 0, 0, 0, 0, 0).hashCode());
    }

    @Test
    public void shouldDeconstructWithRecordPattern() {
        final Object o = createIntTuple(1, 2, 3, 4, 5, 6, 7, 8);
        if (o instanceof Tuple8(var v1, var v2, var v3, var v4, var v5, var v6, var v7, var v8)) {
            assertThat(v1).isEqualTo(1);
            assertThat(v2).isEqualTo(2);
            assertThat(v3).isEqualTo(3);
            assertThat(v4).isEqualTo(4);
            assertThat(v5).isEqualTo(5);
            assertThat(v6).isEqualTo(6);
            assertThat(v7).isEqualTo(7);
            assertThat(v8).isEqualTo(8);
        } else {
            throw new AssertionError("record pattern did not match");
        }
    }

    @Test
    public void shouldImplementToString() {
        final String actual = createTuple().toString();
        final String expected = "(null, null, null, null, null, null, null, null)";
        assertThat(actual).isEqualTo(expected);
    }

    private Comparator<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> intTupleComparator = Tuple8.comparator(Integer::compare, Integer::compare, Integer::compare, Integer::compare, Integer::compare, Integer::compare, Integer::compare, Integer::compare);

    private Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> createTuple() {
        return new Tuple8<>(null, null, null, null, null, null, null, null);
    }

    private Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> createIntTuple(Integer i1, Integer i2, Integer i3, Integer i4, Integer i5, Integer i6, Integer i7, Integer i8) {
        return new Tuple8<>(i1, i2, i3, i4, i5, i6, i7, i8);
    }
}