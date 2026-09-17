package com.guizmaii.zazr;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Try;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.Iterator.range;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LazyTest {

    @Nested
    class StaticNarrowTests {
        @Test
        public void shouldNarrow() {
            final String expected = "Zero args";
            final Lazy<String> wideFunction = Lazy.of(() -> expected);
            final Lazy<CharSequence> actual = Lazy.narrow(wideFunction);
            assertThat(actual.get()).isEqualTo(expected);
        }
    }

    @Nested
    class OfSupplierTests {
        @Test
        public void shouldThrowOnNullSupplier() {
            assertThrows(NullPointerException.class, () -> Lazy.of((Supplier<?>) null));
        }

        @Test
        public void shouldMemoizeValues() {
            final Lazy<Double> testee = Lazy.of(Math::random);
            final double expected = testee.get();
            for (int i = 0; i < 10; i++) {
                final double actual = testee.get();
                assertThat(actual).isEqualTo(expected);
            }
        }

        @Test
        public void shouldNotEvaluateOnCreation() {
            final AtomicInteger evaluations = new AtomicInteger();
            final Lazy<Integer> lazy = Lazy.of(evaluations::incrementAndGet);
            assertThat(evaluations.get()).isEqualTo(0);
            assertThat(lazy.get()).isEqualTo(1);
            assertThat(lazy.get()).isEqualTo(1);
            assertThat(evaluations.get()).isEqualTo(1);
        }

        @Test
        public void shouldRetryAfterAFailedEvaluation() {
            final AtomicInteger evaluations = new AtomicInteger();
            final Lazy<Integer> lazy = Lazy.of(() -> {
                if (evaluations.incrementAndGet() == 1) {
                    throw new IllegalStateException("first attempt fails");
                }
                return evaluations.get();
            });
            assertThatThrownBy(lazy::get).isInstanceOf(IllegalStateException.class);
            assertThat(lazy.isEvaluated()).isFalse();
            assertThat(lazy.get()).isEqualTo(2);
            assertThat(lazy.isEvaluated()).isTrue();
        }
    }

    @Nested
    class GetTests {
        @Test
        public void shouldGetTheValue() {
            assertThat(Lazy.of(() -> 1).get()).isEqualTo(1);
        }

        @Test
        public void shouldHoldNull() {
            final Lazy<Object> lazy = Lazy.of(() -> null);
            assertThat(lazy.get()).isNull();
            assertThat(lazy.isEvaluated()).isTrue();
        }
    }

    @Nested
    class PeekTests {
        @Test
        public void shouldPeek() {
            final Lazy<Integer> lazy = Lazy.of(() -> 1);
            final Lazy<Integer> peek = lazy.peek(v -> assertThat(v).isEqualTo(1));
            assertThat(peek).isSameAs(lazy);
        }

        @Test
        public void shouldEvaluateOnPeek() {
            final Lazy<Integer> lazy = Lazy.of(() -> 1);
            final int[] effect = { 0 };
            lazy.peek(i -> effect[0] = i);
            assertThat(effect[0]).isEqualTo(1);
            assertThat(lazy.isEvaluated()).isTrue();
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> Lazy.of(() -> 1).peek(null));
        }
    }

    @Nested
    class SequenceIterableTests {
        @Test
        public void shouldSequenceEmpty() {
            final List<Lazy<Integer>> testee = List.empty();
            final Lazy<Seq<Integer>> sequence = Lazy.sequence(testee);
            assertThat(sequence.get()).isEqualTo(Vector.empty());
        }

        @Test
        public void shouldSequenceNonEmptyLazy() {
            final List<Lazy<Integer>> testee = List.of(1, 2, 3).map(i -> Lazy.of(() -> i));
            final Lazy<Seq<Integer>> sequence = Lazy.sequence(testee);
            assertThat(sequence.get()).isEqualTo(Vector.of(1, 2, 3));
        }

        @Test
        public void shouldNotEvaluateEmptySequence() {
            final List<Lazy<Integer>> testee = List.empty();
            final Lazy<Seq<Integer>> sequence = Lazy.sequence(testee);
            assertThat(sequence.isEvaluated()).isFalse();
        }

        @Test
        public void shouldNotEvaluateNonEmptySequence() {
            final List<Lazy<Integer>> testee = List.of(1, 2, 3).map(i -> Lazy.of(() -> i));
            final Lazy<Seq<Integer>> sequence = Lazy.sequence(testee);
            assertThat(sequence.isEvaluated()).isFalse();
        }

        @Test
        public void shouldThrowWhenSequencingNull() {
            assertThrows(NullPointerException.class, () -> Lazy.sequence(null));
        }
    }

    @Nested
    class MapTests {
        @Test
        public void shouldMapOverLazyValue() {
            final Lazy<Integer> testee = Lazy.of(() -> 42);
            final Lazy<Integer> expected = Lazy.of(() -> 21);
            assertThat(testee.map(i -> i / 2)).isEqualTo(expected);
        }

        @Test
        public void shouldNotEvaluateOnMap() {
            final Lazy<Integer> testee = Lazy.of(() -> 42);
            final Lazy<Integer> mapped = testee.map(i -> i / 2);
            assertThat(testee.isEvaluated()).isFalse();
            assertThat(mapped.isEvaluated()).isFalse();
            assertThat(mapped.get()).isEqualTo(21);
            assertThat(testee.isEvaluated()).isTrue();
        }

        @Test
        public void shouldMapToNull() {
            assertThat(Lazy.of(() -> 1).map(i -> null).get()).isNull();
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Lazy.of(() -> 1).map(null));
        }
    }

    @Nested
    class FlatMapTests {
        @Test
        public void shouldFlatMapOverLazyValue() {
            final Lazy<Integer> testee = Lazy.of(() -> 42);
            assertThat(testee.flatMap(i -> Lazy.of(() -> i / 2)).get()).isEqualTo(21);
        }

        @Test
        public void shouldNotEvaluateOnFlatMap() {
            final Lazy<Integer> testee = Lazy.of(() -> 42);
            final Lazy<Integer> inner = Lazy.of(() -> 21);
            final Lazy<Integer> result = testee.flatMap(i -> inner);
            assertThat(testee.isEvaluated()).isFalse();
            assertThat(inner.isEvaluated()).isFalse();
            assertThat(result.isEvaluated()).isFalse();
            assertThat(result.get()).isEqualTo(21);
            assertThat(testee.isEvaluated()).isTrue();
            assertThat(inner.isEvaluated()).isTrue();
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> Lazy.of(() -> 1).flatMap(null));
        }

        @Test
        public void shouldRejectANullLazyReturnedByTheMapperOnEvaluation() {
            final Lazy<Integer> result = Lazy.of(() -> 1).flatMap(i -> null);
            assertThatThrownBy(result::get).isInstanceOf(NullPointerException.class).hasMessage("Lazy.flatMap: mapper returned null");
            assertThat(result.isEvaluated()).isFalse();
        }
    }

    @Nested
    class ToSupplierTests {
        @Test
        public void shouldSupplyTheValue() {
            final Lazy<Integer> lazy = Lazy.of(() -> 1);
            final Supplier<Integer> supplier = lazy.toSupplier();
            assertThat(lazy.isEvaluated()).isFalse();
            assertThat(supplier.get()).isEqualTo(1);
            assertThat(lazy.isEvaluated()).isTrue();
        }

        @Test
        public void shouldShareMemoizationWithTheLazy() {
            final AtomicInteger evaluations = new AtomicInteger();
            final Lazy<Integer> lazy = Lazy.of(evaluations::incrementAndGet);
            final Supplier<Integer> supplier = lazy.toSupplier();
            assertThat(supplier.get()).isEqualTo(1);
            assertThat(supplier.get()).isEqualTo(1);
            assertThat(lazy.get()).isEqualTo(1);
            assertThat(evaluations.get()).isEqualTo(1);
        }
    }

    @Nested
    class IsevaluatedTests {
        @Test
        public void shouldBeAwareOfEvaluated() {
            final Lazy<Void> lazy = Lazy.of(() -> null);
            assertThat(lazy.isEvaluated()).isFalse();
            assertThat(lazy.isEvaluated()).isFalse(); // remains not evaluated
            lazy.get();
            assertThat(lazy.isEvaluated()).isTrue();
        }
    }

    @Nested
    class ConcurrencyTests {
        @Test
        public void shouldSupportMultithreading() {
            final AtomicBoolean isEvaluated = new AtomicBoolean();
            final AtomicBoolean lock = new AtomicBoolean();
            final Lazy<Integer> lazy = Lazy.of(() -> {
                while (lock.get()) {
                    Try.run(() -> Thread.sleep(300));
                }
                return 1;
            });
            new Thread(() -> {
                Try.run(() -> Thread.sleep(100));
                new Thread(() -> {
                    Try.run(() -> Thread.sleep(100));
                    lock.set(false);
                }).start();
                isEvaluated.compareAndSet(false, lazy.isEvaluated());
                lazy.get();
            }).start();
            assertThat(isEvaluated.get()).isFalse();
            assertThat(lazy.get()).isEqualTo(1);
        }

        @Test
        @SuppressWarnings({ "StatementWithEmptyBody", "rawtypes" })
        public void shouldBeConsistentFromMultipleThreads() throws Exception {
            for (int i = 0; i < 100; i++) {
                final AtomicBoolean canProceed = new AtomicBoolean(false);
                final Vector<CompletableFuture<Void>> futures = Vector.range(0, 10).map(j -> {
                    final AtomicBoolean isEvaluated = new AtomicBoolean(false);
                    final Integer expected = ((j % 2) == 1) ? null : j;
                    Lazy<Integer> lazy = Lazy.of(() -> {
                        assertThat(isEvaluated.getAndSet(true)).isFalse();
                        return expected;
                    });
                    return Tuple.of(lazy, expected);
                }).flatMap(t -> range(0, 5).map(j -> runAsync(() -> {
                            while (!canProceed.get()) { /* busy wait */ }
                            assertThat(t._1().get()).isEqualTo(t._2());
                        }))
                );

                final CompletableFuture all = CompletableFuture.allOf(futures.toJavaList().toArray(new CompletableFuture<?>[0]));
                canProceed.set(true);
                all.join();
            }
        }
    }

    @Nested
    class EqualsTests {
        @SuppressWarnings({ "EqualsBetweenInconvertibleTypes", "EqualsWithItself" })
        @Test
        public void shouldDetectEqualObject() {
            assertThat(Lazy.of(() -> 1).equals("")).isFalse();
            assertThat(Lazy.of(() -> 1).equals(Lazy.of(() -> 1))).isTrue();
            assertThat(Lazy.of(() -> 1).equals(Lazy.of(() -> 2))).isFalse();
            final Lazy<Integer> same = Lazy.of(() -> 1);
            assertThat(same.equals(same)).isTrue();
        }

        @SuppressWarnings({ "EqualsBetweenInconvertibleTypes", "EqualsWithItself" })
        @Test
        public void shouldUseDefaultEqualsSemanticsForArrays() {
            assertThat(Lazy.of(() -> new Integer[] {1}).equals("")).isFalse();
            assertThat(Lazy.of(() -> new Integer[] {1}).equals(Lazy.of(() -> new Integer[] {1}))).isFalse();
            final Lazy<Integer[]> same = Lazy.of(() -> new Integer[] {1});
            assertThat(same.equals(same)).isTrue();
        }

        @Test
        public void shouldDetectUnequalObject() {
            assertThat(Lazy.of(() -> 1).equals(Lazy.of(() -> 2))).isFalse();
        }
    }

    @Nested
    class HashcodeTests {
        @Test
        public void shouldComputeHashCode() {
            assertThat(Lazy.of(() -> 1).hashCode()).isEqualTo(Objects.hashCode(1));
        }

        @Test
        public void shouldComputeHashCodeForArrays() {
            Integer[] value = new Integer[] {1};
            //noinspection ArrayHashCode
            assertThat(Lazy.of(() -> value).hashCode()).isEqualTo(value.hashCode());
        }
    }

    @Nested
    class TostringTests {
        @Test
        public void shouldConvertNonEvaluatedValueToString() {
            final Lazy<Integer> lazy = Lazy.of(() -> 1);
            assertThat(lazy.toString()).isEqualTo("Lazy(?)");
        }

        @Test
        public void shouldConvertEvaluatedValueToString() {
            final Lazy<Integer> lazy = Lazy.of(() -> 1);
            lazy.get();
            assertThat(lazy.toString()).isEqualTo("Lazy(1)");
        }
    }
}
