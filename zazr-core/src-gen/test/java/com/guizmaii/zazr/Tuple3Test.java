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

public class Tuple3Test {

    @Test
    public void shouldCreateTuple() {
        final Tuple3<Object, Object, Object> tuple = createTuple();
        assertThat(tuple).isNotNull();
    }

    @Test
    public void shouldGetArity() {
        final Tuple3<Object, Object, Object> tuple = createTuple();
        assertThat(tuple.arity()).isEqualTo(3);
    }

    @Test
    public void shouldReturnElements() {
        final Tuple3<Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
    }

    @Test
    public void shouldUpdate1() {
      final Tuple3<Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3).update1(42);
      assertThat(tuple._1()).isEqualTo(42);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(3);
    }

    @Test
    public void shouldUpdate2() {
      final Tuple3<Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3).update2(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(42);
      assertThat(tuple._3()).isEqualTo(3);
    }

    @Test
    public void shouldUpdate3() {
      final Tuple3<Integer, Integer, Integer> tuple = createIntTuple(1, 2, 3).update3(42);
      assertThat(tuple._1()).isEqualTo(1);
      assertThat(tuple._2()).isEqualTo(2);
      assertThat(tuple._3()).isEqualTo(42);
    }

    @Test
    public void shouldConvertToVector() {
        final Vector<?> actual = createIntTuple(1, 0, 0).toVector();
        assertThat(actual).isEqualTo(Vector.of(1, 0, 0));
    }

    @Test
    public void shouldCompareEqual() {
        final Tuple3<Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0);
        assertThat(t0.compareTo(t0)).isZero();
        assertThat(intTupleComparator.compare(t0, t0)).isZero();
    }

    @Test
    public void shouldThrowWhenComparingToNull() {
        final Tuple3<Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0);
        assertThrows(NullPointerException.class, () -> t0.compareTo(null));
    }

    @Test
    public void shouldCompare1stArg() {
        final Tuple3<Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0);
        final Tuple3<Integer, Integer, Integer> t1 = createIntTuple(1, 0, 0);
        assertThat(t0.compareTo(t1)).isNegative();
        assertThat(t1.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t1)).isNegative();
        assertThat(intTupleComparator.compare(t1, t0)).isPositive();
    }

    @Test
    public void shouldCompare2ndArg() {
        final Tuple3<Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0);
        final Tuple3<Integer, Integer, Integer> t2 = createIntTuple(0, 1, 0);
        assertThat(t0.compareTo(t2)).isNegative();
        assertThat(t2.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t2)).isNegative();
        assertThat(intTupleComparator.compare(t2, t0)).isPositive();
    }

    @Test
    public void shouldCompare3rdArg() {
        final Tuple3<Integer, Integer, Integer> t0 = createIntTuple(0, 0, 0);
        final Tuple3<Integer, Integer, Integer> t3 = createIntTuple(0, 0, 1);
        assertThat(t0.compareTo(t3)).isNegative();
        assertThat(t3.compareTo(t0)).isPositive();
        assertThat(intTupleComparator.compare(t0, t3)).isNegative();
        assertThat(intTupleComparator.compare(t3, t0)).isPositive();
    }

    @Test
    public void shouldMap() {
        final Tuple3<Object, Object, Object> tuple = createTuple();
        final Tuple3<Object, Object, Object> actual = tuple.map((o1, o2, o3) -> tuple);
        assertThat(actual).isEqualTo(tuple);
    }

    @Test
    public void shouldMapComponents() {
      final Tuple3<Object, Object, Object> tuple = createTuple();
      final Function<Object, Object> f1 = Function.identity();
      final Function<Object, Object> f2 = Function.identity();
      final Function<Object, Object> f3 = Function.identity();
      final Tuple3<Object, Object, Object> actual = tuple.map(f1, f2, f3);
      assertThat(actual).isEqualTo(tuple);
    }

    @Test
    public void shouldReturnTuple3OfUnzip3() {
      final List<Tuple3<Integer, Integer, Integer>> iterable = List.of(Tuple.of(2, 3, 4), Tuple.of(4, 5, 6), Tuple.of(6, 7, 8));
      final Tuple3<Vector<Integer>, Vector<Integer>, Vector<Integer>> expected = Tuple.of(Vector.of(2, 4, 6), Vector.of(3, 5, 7), Vector.of(4, 6, 8));
      assertThat(Tuple.unzip3(iterable)).isEqualTo(expected);
    }

    @Test
    public void shouldUnzip3Nothing() {
      final Tuple3<Vector<Integer>, Vector<Integer>, Vector<Integer>> expected = Tuple.of(Vector.empty(), Vector.empty(), Vector.empty());
      assertThat(Tuple.unzip3(List.<Tuple3<Integer, Integer, Integer>> empty())).isEqualTo(expected);
      assertThat(Tuple.unzip3(List.<Tuple3<Integer, Integer, Integer>> empty())._1()).isSameAs(Vector.empty());
    }

    @Test
    public void shouldRejectNullOnUnzip3() {
      assertThrows(NullPointerException.class, () -> Tuple.unzip3(null));
      assertThrows(NullPointerException.class, () -> Tuple.unzip3(List.of(Tuple.of((Integer) null, (Integer) null, (Integer) null))));
    }

    @Test
    public void shouldReturnTuple3OfUnzip1() {
      final List<Tuple3<Integer, Integer, Integer>> iterable = List.of(Tuple.of(1, 2, 3));
      final Tuple3<Vector<Integer>, Vector<Integer>, Vector<Integer>> expected = Tuple.of(Vector.of(1), Vector.of(2), Vector.of(3));
      assertThat(Tuple.unzip3(iterable)).isEqualTo(expected);
    }

    @Test
    public void shouldMap1stComponent() {
      final Tuple3<String, Integer, Integer> actual = Tuple.of(1, 1, 1).map1(i -> "X");
      final Tuple3<String, Integer, Integer> expected = Tuple.of("X", 1, 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap2ndComponent() {
      final Tuple3<Integer, String, Integer> actual = Tuple.of(1, 1, 1).map2(i -> "X");
      final Tuple3<Integer, String, Integer> expected = Tuple.of(1, "X", 1);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMap3rdComponent() {
      final Tuple3<Integer, Integer, String> actual = Tuple.of(1, 1, 1).map3(i -> "X");
      final Tuple3<Integer, Integer, String> expected = Tuple.of(1, 1, "X");
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldApplyTuple() {
        final Tuple3<Object, Object, Object> tuple = createTuple();
        final Tuple0 actual = tuple.apply((o1, o2, o3) -> Tuple0.instance());
        assertThat(actual).isEqualTo(Tuple0.instance());
    }

    @Test
    public void shouldAppendValue() {
        final Tuple4<Integer, Integer, Integer, Integer> actual = Tuple.of(1, 2, 3).append(4);
        final Tuple4<Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple1() {
        final Tuple4<Integer, Integer, Integer, Integer> actual = Tuple.of(1, 2, 3).concat(Tuple.of(4));
        final Tuple4<Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple2() {
        final Tuple5<Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 2, 3).concat(Tuple.of(4, 5));
        final Tuple5<Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple3() {
        final Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 2, 3).concat(Tuple.of(4, 5, 6));
        final Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple4() {
        final Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 2, 3).concat(Tuple.of(4, 5, 6, 7));
        final Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple5() {
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual = Tuple.of(1, 2, 3).concat(Tuple.of(4, 5, 6, 7, 8));
        final Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRecognizeEquality() {
        final Tuple3<Object, Object, Object> tuple1 = createTuple();
        final Tuple3<Object, Object, Object> tuple2 = createTuple();
        assertThat((Object) tuple1).isEqualTo(tuple2);
    }

    @Test
    public void shouldRecognizeNonEquality() {
        final Tuple3<Object, Object, Object> tuple = createTuple();
        final Object other = new Object();
        assertThat(tuple).isNotEqualTo(other);
    }

    @Test
    public void shouldRecognizeNonEqualityPerComponent() {
        final Tuple3<String, String, String> tuple = Tuple.of("1", "2", "3");
        assertThat(tuple.equals(Tuple.of("X", "2", "3"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "X", "3"))).isFalse();
        assertThat(tuple.equals(Tuple.of("1", "2", "X"))).isFalse();
    }

    @Test
    public void shouldComputeCorrectHashCode() {
        assertThat(createTuple().hashCode()).isEqualTo(createTuple().hashCode());
        assertThat(createIntTuple(0, 0, 0).hashCode()).isEqualTo(createIntTuple(0, 0, 0).hashCode());
        assertThat(createIntTuple(0, 0, 0).hashCode()).isNotEqualTo(createIntTuple(1, 0, 0).hashCode());
    }

    @Test
    public void shouldDeconstructWithRecordPattern() {
        final Object o = createIntTuple(1, 2, 3);
        if (o instanceof Tuple3(var v1, var v2, var v3)) {
            assertThat(v1).isEqualTo(1);
            assertThat(v2).isEqualTo(2);
            assertThat(v3).isEqualTo(3);
        } else {
            throw new AssertionError("record pattern did not match");
        }
    }

    @Test
    public void shouldImplementToString() {
        final String actual = createTuple().toString();
        final String expected = "(null, null, null)";
        assertThat(actual).isEqualTo(expected);
    }

    private Comparator<Tuple3<Integer, Integer, Integer>> intTupleComparator = Tuple3.comparator(Integer::compare, Integer::compare, Integer::compare);

    private Tuple3<Object, Object, Object> createTuple() {
        return new Tuple3<>(null, null, null);
    }

    private Tuple3<Integer, Integer, Integer> createIntTuple(Integer i1, Integer i2, Integer i3) {
        return new Tuple3<>(i1, i2, i3);
    }
}