package dev.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import dev.zazr.collection.Vector;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Tuple0Test {

    @Test
    public void shouldCreateTuple() {
        Tuple0 tuple = createTuple();
        assertThat(tuple).isNotNull();
    }

    @Test
    public void shouldGetArity() {
        Tuple0 tuple = createTuple();
        assertThat(tuple.arity()).isEqualTo(0);
    }

    @Test
    public void shouldConvertToVector() {
        Vector<?> actual = createIntTuple().toVector();
        assertThat(actual).isEqualTo(Vector.of());
    }

    @Test
    public void shouldCompareEqual() {
        Tuple0 t0 = createIntTuple();
        assertThat(t0.compareTo(t0)).isZero();
        assertThat(intTupleComparator.compare(t0, t0)).isZero();
    }

    @Test
    public void shouldThrowWhenComparingToNull() {
        Tuple0 t0 = createIntTuple();
        assertThrows(NullPointerException.class, () -> t0.compareTo(null));
    }

    @Test
    public void shouldApplyTuple() {
        Tuple0 tuple = createTuple();
        Tuple0 actual = tuple.apply(() -> Tuple0.instance());
        assertThat(actual).isEqualTo(Tuple0.instance());
    }

    @Test
    public void shouldAppendValue() {
        Tuple1<Integer> actual = Tuple0.instance().append(1);
        Tuple1<Integer> expected = Tuple.of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple1() {
        Tuple1<Integer> actual = Tuple0.instance().concat(Tuple.of(1));
        Tuple1<Integer> expected = Tuple.of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple2() {
        Tuple2<Integer, Integer> actual = Tuple0.instance().concat(Tuple.of(1, 2));
        Tuple2<Integer, Integer> expected = Tuple.of(1, 2);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple3() {
        Tuple3<Integer, Integer, Integer> actual = Tuple0.instance().concat(Tuple.of(1, 2, 3));
        Tuple3<Integer, Integer, Integer> expected = Tuple.of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple4() {
        Tuple4<Integer, Integer, Integer, Integer> actual = Tuple0.instance().concat(Tuple.of(1, 2, 3, 4));
        Tuple4<Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple5() {
        Tuple5<Integer, Integer, Integer, Integer, Integer> actual =
                Tuple0.instance().concat(Tuple.of(1, 2, 3, 4, 5));
        Tuple5<Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple6() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple0.instance().concat(Tuple.of(1, 2, 3, 4, 5, 6));
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple7() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple0.instance().concat(Tuple.of(1, 2, 3, 4, 5, 6, 7));
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConcatTuple8() {
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> actual =
                Tuple0.instance().concat(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8));
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> expected =
                Tuple.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRecognizeEquality() {
        Tuple0 tuple1 = createTuple();
        Tuple0 tuple2 = createTuple();
        assertThat((Object) tuple1).isEqualTo(tuple2);
    }

    @Test
    public void shouldRecognizeNonEquality() {
        Tuple0 tuple = createTuple();
        Object other = new Object();
        assertThat(tuple).isNotEqualTo(other);
    }

    @Test
    public void shouldComputeCorrectHashCode() {
        assertThat(createTuple().hashCode()).isEqualTo(createTuple().hashCode());
    }

    @Test
    public void shouldDeconstructWithRecordPattern() {
        Object o = createIntTuple();
        if (o instanceof Tuple0()) {

        } else {
            throw new AssertionError("record pattern did not match");
        }
    }

    @Test
    public void shouldImplementToString() {
        String actual = createTuple().toString();
        String expected = "()";
        assertThat(actual).isEqualTo(expected);
    }

    private Comparator<Tuple0> intTupleComparator = Tuple0.comparator();

    private Tuple0 createTuple() {
        return Tuple0.instance();
    }

    private Tuple0 createIntTuple() {
        return Tuple0.instance();
    }
}
