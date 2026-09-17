package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class LazyZipTest {

    @Test
    public void shouldNotEvaluateBeforeTheZipOf2Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Tuple2<Integer, Integer>> zipped = Lazy.zip(l1, l2);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, (a1, a2) -> "" + a1 + a2);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf2InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Tuple2<Integer, Integer>> zipped = Lazy.zip(l1, l2);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2));
        assertThat(order).containsExactly(1, 2);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2);
    }

    @Test
    public void shouldEvaluateTheZipWithOf2InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, (a1, a2) -> {
            calls.incrementAndGet();
            return "" + a1 + a2;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("12");
        assertThat(order).containsExactly(1, 2);
        assertThat(combined.get()).isEqualTo("12");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf2() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), (_, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip2WithANullValue() {
        final Lazy<Tuple2<Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2));
    }

    @Test
    public void shouldRejectANullArgumentOf2() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null)).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, (_, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldNotEvaluateBeforeTheZipOf3Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Tuple3<Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, (a1, a2, a3) -> "" + a1 + a2 + a3);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(l3.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf3InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Tuple3<Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2, 3));
        assertThat(order).containsExactly(1, 2, 3);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(l3.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2, 3);
    }

    @Test
    public void shouldEvaluateTheZipWithOf3InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, (a1, a2, a3) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("123");
        assertThat(order).containsExactly(1, 2, 3);
        assertThat(combined.get()).isEqualTo("123");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2, 3);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf3() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), (_, _, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip3WithANullValue() {
        final Lazy<Tuple3<Integer, Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2), Lazy.of(() -> 3));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2, 3));
    }

    @Test
    public void shouldRejectANullArgumentOf3() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2), Lazy.of(() -> 3))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), Lazy.of(() -> 3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null, Lazy.of(() -> 3))).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, Lazy.of(() -> 3), (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), null)).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null, (_, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldNotEvaluateBeforeTheZipOf4Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Tuple4<Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, (a1, a2, a3, a4) -> "" + a1 + a2 + a3 + a4);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(l3.isEvaluated()).isFalse();
        assertThat(l4.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf4InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Tuple4<Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2, 3, 4));
        assertThat(order).containsExactly(1, 2, 3, 4);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(l3.isEvaluated()).isTrue();
        assertThat(l4.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2, 3, 4);
    }

    @Test
    public void shouldEvaluateTheZipWithOf4InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, (a1, a2, a3, a4) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("1234");
        assertThat(order).containsExactly(1, 2, 3, 4);
        assertThat(combined.get()).isEqualTo("1234");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2, 3, 4);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf4() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), (_, _, _, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip4WithANullValue() {
        final Lazy<Tuple4<Integer, Integer, Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2, 3, 4));
    }

    @Test
    public void shouldRejectANullArgumentOf4() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4))).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4))).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null)).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, (_, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldNotEvaluateBeforeTheZipOf5Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Tuple5<Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, (a1, a2, a3, a4, a5) -> "" + a1 + a2 + a3 + a4 + a5);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(l3.isEvaluated()).isFalse();
        assertThat(l4.isEvaluated()).isFalse();
        assertThat(l5.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf5InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Tuple5<Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2, 3, 4, 5));
        assertThat(order).containsExactly(1, 2, 3, 4, 5);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(l3.isEvaluated()).isTrue();
        assertThat(l4.isEvaluated()).isTrue();
        assertThat(l5.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldEvaluateTheZipWithOf5InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, (a1, a2, a3, a4, a5) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("12345");
        assertThat(order).containsExactly(1, 2, 3, 4, 5);
        assertThat(combined.get()).isEqualTo("12345");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf5() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), (_, _, _, _, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip5WithANullValue() {
        final Lazy<Tuple5<Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2, 3, 4, 5));
    }

    @Test
    public void shouldRejectANullArgumentOf5() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5))).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5))).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5))).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null)).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, (_, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldNotEvaluateBeforeTheZipOf6Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5, l6);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, l6, (a1, a2, a3, a4, a5, a6) -> "" + a1 + a2 + a3 + a4 + a5 + a6);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(l3.isEvaluated()).isFalse();
        assertThat(l4.isEvaluated()).isFalse();
        assertThat(l5.isEvaluated()).isFalse();
        assertThat(l6.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf6InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5, l6);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2, 3, 4, 5, 6));
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(l3.isEvaluated()).isTrue();
        assertThat(l4.isEvaluated()).isTrue();
        assertThat(l5.isEvaluated()).isTrue();
        assertThat(l6.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void shouldEvaluateTheZipWithOf6InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, l6, (a1, a2, a3, a4, a5, a6) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("123456");
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(combined.get()).isEqualTo("123456");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf6() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), (_, _, _, _, _, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip6WithANullValue() {
        final Lazy<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2, 3, 4, 5, 6));
    }

    @Test
    public void shouldRejectANullArgumentOf6() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6))).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6))).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), Lazy.of(() -> 6))).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), Lazy.of(() -> 6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, Lazy.of(() -> 6))).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, Lazy.of(() -> 6), (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null)).isInstanceOf(NullPointerException.class).hasMessage("l6 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null, (_, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l6 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldNotEvaluateBeforeTheZipOf7Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Integer> l7 = Lazy.of(() -> {
            order.add(7);
            return 7;
        });
        final Lazy<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5, l6, l7);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, l6, l7, (a1, a2, a3, a4, a5, a6, a7) -> "" + a1 + a2 + a3 + a4 + a5 + a6 + a7);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(l3.isEvaluated()).isFalse();
        assertThat(l4.isEvaluated()).isFalse();
        assertThat(l5.isEvaluated()).isFalse();
        assertThat(l6.isEvaluated()).isFalse();
        assertThat(l7.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf7InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Integer> l7 = Lazy.of(() -> {
            order.add(7);
            return 7;
        });
        final Lazy<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5, l6, l7);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2, 3, 4, 5, 6, 7));
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(l3.isEvaluated()).isTrue();
        assertThat(l4.isEvaluated()).isTrue();
        assertThat(l5.isEvaluated()).isTrue();
        assertThat(l6.isEvaluated()).isTrue();
        assertThat(l7.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void shouldEvaluateTheZipWithOf7InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Integer> l7 = Lazy.of(() -> {
            order.add(7);
            return 7;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, l6, l7, (a1, a2, a3, a4, a5, a6, a7) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("1234567");
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(combined.get()).isEqualTo("1234567");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf7() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip7WithANullValue() {
        final Lazy<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2, 3, 4, 5, 6, 7));
    }

    @Test
    public void shouldRejectANullArgumentOf7() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7))).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7))).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7))).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, Lazy.of(() -> 6), Lazy.of(() -> 7))).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, Lazy.of(() -> 6), Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null, Lazy.of(() -> 7))).isInstanceOf(NullPointerException.class).hasMessage("l6 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null, Lazy.of(() -> 7), (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l6 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), null)).isInstanceOf(NullPointerException.class).hasMessage("l7 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), null, (_, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l7 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }

    @Test
    public void shouldNotEvaluateBeforeTheZipOf8Is() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Integer> l7 = Lazy.of(() -> {
            order.add(7);
            return 7;
        });
        final Lazy<Integer> l8 = Lazy.of(() -> {
            order.add(8);
            return 8;
        });
        final Lazy<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5, l6, l7, l8);
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, l6, l7, l8, (a1, a2, a3, a4, a5, a6, a7, a8) -> "" + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8);
        assertThat(zipped.isEvaluated()).isFalse();
        assertThat(combined.isEvaluated()).isFalse();
        assertThat(l1.isEvaluated()).isFalse();
        assertThat(l2.isEvaluated()).isFalse();
        assertThat(l3.isEvaluated()).isFalse();
        assertThat(l4.isEvaluated()).isFalse();
        assertThat(l5.isEvaluated()).isFalse();
        assertThat(l6.isEvaluated()).isFalse();
        assertThat(l7.isEvaluated()).isFalse();
        assertThat(l8.isEvaluated()).isFalse();
        assertThat(order).isEmpty();
    }

    @Test
    public void shouldEvaluateTheZipOf8InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Integer> l7 = Lazy.of(() -> {
            order.add(7);
            return 7;
        });
        final Lazy<Integer> l8 = Lazy.of(() -> {
            order.add(8);
            return 8;
        });
        final Lazy<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(l1, l2, l3, l4, l5, l6, l7, l8);
        assertThat(zipped.get()).isEqualTo(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8));
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(zipped.isEvaluated()).isTrue();
        assertThat(l1.isEvaluated()).isTrue();
        assertThat(l2.isEvaluated()).isTrue();
        assertThat(l3.isEvaluated()).isTrue();
        assertThat(l4.isEvaluated()).isTrue();
        assertThat(l5.isEvaluated()).isTrue();
        assertThat(l6.isEvaluated()).isTrue();
        assertThat(l7.isEvaluated()).isTrue();
        assertThat(l8.isEvaluated()).isTrue();
        assertThat(zipped.get()).isSameAs(zipped.get());
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void shouldEvaluateTheZipWithOf8InArgumentOrderAndCacheIt() {
        final List<Integer> order = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<Integer> l1 = Lazy.of(() -> {
            order.add(1);
            return 1;
        });
        final Lazy<Integer> l2 = Lazy.of(() -> {
            order.add(2);
            return 2;
        });
        final Lazy<Integer> l3 = Lazy.of(() -> {
            order.add(3);
            return 3;
        });
        final Lazy<Integer> l4 = Lazy.of(() -> {
            order.add(4);
            return 4;
        });
        final Lazy<Integer> l5 = Lazy.of(() -> {
            order.add(5);
            return 5;
        });
        final Lazy<Integer> l6 = Lazy.of(() -> {
            order.add(6);
            return 6;
        });
        final Lazy<Integer> l7 = Lazy.of(() -> {
            order.add(7);
            return 7;
        });
        final Lazy<Integer> l8 = Lazy.of(() -> {
            order.add(8);
            return 8;
        });
        final Lazy<String> combined = Lazy.zipWith(l1, l2, l3, l4, l5, l6, l7, l8, (a1, a2, a3, a4, a5, a6, a7, a8) -> {
            calls.incrementAndGet();
            return "" + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(combined.get()).isEqualTo("12345678");
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(combined.get()).isEqualTo("12345678");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(order).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void shouldHoldNullFromTheZipWithOf8() {
        final Lazy<Object> combined = Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> null);
        assertThat(combined.get()).isNull();
        assertThat(combined.isEvaluated()).isTrue();
    }

    @Test
    public void shouldZip8WithANullValue() {
        final Lazy<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> zipped = Lazy.zip(Lazy.<Integer>of(() -> null), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8));
        assertThat(zipped.get()).isEqualTo(Tuple.of(null, 2, 3, 4, 5, 6, 7, 8));
    }

    @Test
    public void shouldRejectANullArgumentOf8() {
        assertThatThrownBy(() -> Lazy.zip(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zipWith(null, Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l1 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), null, Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l2 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), null, Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l3 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), null, Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l4 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), null, Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l5 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null, Lazy.of(() -> 7), Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l6 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), null, Lazy.of(() -> 7), Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l6 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), null, Lazy.of(() -> 8))).isInstanceOf(NullPointerException.class).hasMessage("l7 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), null, Lazy.of(() -> 8), (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l7 is null");
        assertThatThrownBy(() -> Lazy.zip(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), null)).isInstanceOf(NullPointerException.class).hasMessage("l8 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), null, (_, _, _, _, _, _, _, _) -> { throw new AssertionError("must not be called"); })).isInstanceOf(NullPointerException.class).hasMessage("l8 is null");
        assertThatThrownBy(() -> Lazy.zipWith(Lazy.of(() -> 1), Lazy.of(() -> 2), Lazy.of(() -> 3), Lazy.of(() -> 4), Lazy.of(() -> 5), Lazy.of(() -> 6), Lazy.of(() -> 7), Lazy.of(() -> 8), null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
    }
}