package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.Tuple4;
import com.guizmaii.zazr.Tuple5;
import com.guizmaii.zazr.Tuple6;
import com.guizmaii.zazr.Tuple7;
import com.guizmaii.zazr.Tuple8;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.control.Validation;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents an arbitrary object of type T.
 *
 * @param <T> The type of the arbitrary object.
 */
@FunctionalInterface
public interface Arbitrary<T> {

    /**
     * Returns a generator for objects of type T.
     * Use {@link Gen#map(Function)} and {@link Gen#flatMap(Function)} to
     * combine object generators.
     * <p>
     * Example:
     * <pre>
     * <code>
     * // represents arbitrary binary trees of a certain depth n
     * final class ArbitraryTree implements Arbitrary&lt;BinaryTree&lt;Integer&gt;&gt; {
     *     &#64;Override
     *     public Gen&lt;BinaryTree&lt;Integer&gt;&gt; apply(int n) {
     *         return Gen.choose(-1000, 1000).flatMap(value -&gt; {
     *                  if (n == 0) {
     *                      return Gen.of(BinaryTree.leaf(value));
     *                  } else {
     *                      return Gen.frequency(
     *                              Tuple.of(1, Gen.of(BinaryTree.leaf(value))),
     *                              Tuple.of(4, Gen.of(BinaryTree.branch(apply(n / 2).get(), value, apply(n / 2).get())))
     *                      );
     *                  }
     *         });
     *     }
     * }
     *
     * // tree generator with a size hint of 10
     * final Gen&lt;BinaryTree&lt;Integer&gt;&gt; treeGen = new ArbitraryTree().apply(10);
     *
     * // stream sum of tree node values to console for 100 arbitrary trees
     * Stream.of(() -&gt; treeGen.apply(RNG.get())).map(Tree::sum).take(100).forEach(System.out::println);
     * </code>
     * </pre>
     *
     * @param size A (not necessarily positive) size parameter which may be interpreted individually and is constant for all arbitrary objects regarding one property check.
     * @return A generator for objects of type T.
     */
    Gen<T> apply(int size);

    /**
     * Returns an Arbitrary based on this Arbitrary which produces unique values.
     *
     * @return A new generator
     */
    default Arbitrary<T> distinct() {
        return distinctBy(Function.identity());
    }

    /**
     * Returns an Arbitrary based on this Arbitrary which produces unique values based on the given comparator.
     *
     * @param comparator A comparator
     * @return A new generator
     */
    default Arbitrary<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * Returns an Arbitrary based on this Arbitrary which produces unique values based on the given function.
     *
     * @param <U>          key type
     * @param keyExtractor A function
     * @return A new generator
     */
    default <U> Arbitrary<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>();
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * Returns an Arbitrary based on this Arbitrary which produces values that fulfill the given predicate.
     *
     * @param predicate A predicate
     * @return A new generator
     */
    default Arbitrary<T> filter(Predicate<? super T> predicate) {
        return size -> apply(size).filter(predicate);
    }

    /**
     * Maps arbitrary objects T to arbitrary object U.
     *
     * @param mapper A function that maps arbitrary Ts to arbitrary Us given a mapper.
     * @param <U>    New type of arbitrary objects
     * @return A new Arbitrary
     */
    default <U> Arbitrary<U> flatMap(Function<? super T, ? extends Arbitrary<? extends U>> mapper) {
        return size -> {
            final Gen<T> gen = apply(size);
            return random -> mapper.apply(gen.apply(random)).apply(size).apply(random);
        };
    }

    /**
     * Intersperses values from this arbitrary instance with those of another.
     *
     * @param other another T arbitrary to accept values from.
     * @return A new T arbitrary
     */
    default Arbitrary<T> intersperse(Arbitrary<T> other) {
        Objects.requireNonNull(other, "other is null");
        return size -> this.apply(size).intersperse(other.apply(size));
    }

    /**
     * Maps arbitrary objects T to arbitrary object U.
     *
     * @param mapper A function that maps an arbitrary T to an object of type U.
     * @param <U>    Type of the mapped object
     * @return A new generator
     */
    default <U> Arbitrary<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return n -> {
            final Gen<T> generator = apply(n);
            return random -> mapper.apply(generator.apply(random));
        };
    }

    /**
     * Runs {@code action} on every generated value and returns an arbitrary of the same values.
     *
     * @param action what to do with each generated value
     * @return a new arbitrary
     * @throws NullPointerException if {@code action} is null
     */
    default Arbitrary<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        return size -> apply(size).tap(action);
    }

    /**
     * Generates an arbitrary value from a fixed set of values
     *
     * @param values A fixed set of values
     * @param <U>    Type of generator value
     * @return A new generator
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <U> Arbitrary<U> of(U... values) {
        return ofAll(Gen.choose(values));
    }

    /**
     * Generates an arbitrary value from a given generator
     *
     * @param generator A generator to produce arbitrary values
     * @param <U>       Type of generator value
     * @return A new generator
     */
    static <U> Arbitrary<U> ofAll(Gen<U> generator) {
        return size -> generator;
    }

    /**
     * Generates arbitrary integer values between {@code -abs(size)} and {@code abs(size)}, clipped to the
     * integer range, favoring boundaries and values around zero as described by {@link Gen#choose(int, int)}.
     *
     * @return A new Arbitrary of Integer
     */
    static Arbitrary<Integer> integer() {
        return size -> size == Integer.MIN_VALUE
                ? Gen.choose(Integer.MIN_VALUE, Integer.MAX_VALUE)
                : Gen.choose(-size, size);
    }

    /**
     * Generates arbitrary {@link LocalDateTime}s with {@link LocalDateTime#now()} as {@code median} and
     * {@link ChronoUnit#DAYS} as chronological unit.
     *
     * @return A new Arbitrary of LocalDateTime
     * @see #localDateTime(LocalDateTime, ChronoUnit)
     */
    static Arbitrary<LocalDateTime> localDateTime() {
        return localDateTime(ChronoUnit.DAYS);
    }

    /**
     * Generates arbitrary {@link LocalDateTime}s with {@link LocalDateTime#now()} as {@code median}.
     *
     * @param unit Chronological unit of {@code size}
     * @return A new Arbitrary of LocalDateTime
     * @see #localDateTime(LocalDateTime, ChronoUnit)
     */
    static Arbitrary<LocalDateTime> localDateTime(ChronoUnit unit) {
        return localDateTime(LocalDateTime.now(), unit);
    }

    /**
     * Generates arbitrary {@link LocalDateTime}s. All generated values are drawn from a range with {@code median}
     * as center and {@code median +/- size} as included boundaries. {@code unit} defines the chronological unit
     * of {@code size}. Half of the draws choose the range boundaries or the median; the other half sample
     * the range at millisecond resolution. Negative sizes are treated as their absolute value.
     *
     * <p>
     * Example:
     * <pre>
     * <code>
     * Arbitrary.localDateTime(LocalDateTime.now(), ChronoUnit.YEARS);
     * </code>
     * </pre>
     *
     * @param median Center of the LocalDateTime range
     * @param unit   Chronological unit of {@code size}
     * @return  A new Arbitrary of LocalDateTime
     */
    static Arbitrary<LocalDateTime> localDateTime(LocalDateTime median, ChronoUnit unit) {
        Objects.requireNonNull(median, "median is null");
        Objects.requireNonNull(unit, "unit is null");
        return size -> {
            if(size == 0) {
                return Gen.of(median);
            }
            final long radius = Math.abs((long) size);
            final LocalDateTime start = median.minus(radius, unit);
            final LocalDateTime end = median.plus(radius, unit);
            final long duration = Duration.between(start, end).toMillis();
            final Gen<LocalDateTime> dates = GenModule.chooseLong(0, duration)
                    .map(offset -> start.plus(offset, ChronoUnit.MILLIS));
            return GenModule.withEdges(dates, List.of(start, median, end).distinct());
        };
    }

    /**
     * Generates arbitrary strings based on a given alphabet represented by <em>gen</em>.
     * Lengths range from zero to {@code size}, favoring empty, singleton, and near-maximum strings.
     * Nonpositive sizes produce empty strings.
     * <p>
     * Example:
     * <pre>
     * <code>
     * Arbitrary.string(
     *     Gen.frequency(
     *         Tuple.of(1, Gen.choose('A', 'Z')),
     *         Tuple.of(1, Gen.choose('a', 'z')),
     *         Tuple.of(1, Gen.choose('0', '9'))));
     * </code>
     * </pre>
     *
     * @param gen A character generator
     * @return a new Arbitrary of String
     */
    static Arbitrary<String> string(Gen<Character> gen) {
        return size -> {
            final Gen<Integer> lengths = Gen.choose(0, Math.max(0, size));
            return random -> {
                final char[] chars = new char[lengths.apply(random)];
                for (int j = 0; j < chars.length; j++) {
                    chars[j] = gen.apply(random);
                }
                return new String(chars);
            };
        };
    }

    /**
     * Generates arbitrary options: {@code None} for one draw in four, {@code Some} of a value of {@code arbitraryT}
     * otherwise.
     *
     * @param arbitraryT arbitrary values, never null
     * @param <T>        value type
     * @return a new Arbitrary of Option
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<Option<T>> option(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> {
            final Gen<T> genT = arbitraryT.apply(size);
            return random -> random.nextInt(4) == 0 ? Option.none() : Option.some(genT.apply(random));
        };
    }

    /**
     * Generates arbitrary eithers, {@code Left} and {@code Right} with equal chances.
     *
     * @param arbitraryL arbitrary left values, never null
     * @param arbitraryR arbitrary right values, never null
     * @param <L>        left type
     * @param <R>        right type
     * @return a new Arbitrary of Either
     * @throws NullPointerException if an argument is null
     */
    static <L, R> Arbitrary<Either<L, R>> either(Arbitrary<L> arbitraryL, Arbitrary<R> arbitraryR) {
        Objects.requireNonNull(arbitraryL, "arbitraryL is null");
        Objects.requireNonNull(arbitraryR, "arbitraryR is null");
        return size -> {
            final Gen<L> genL = arbitraryL.apply(size);
            final Gen<R> genR = arbitraryR.apply(size);
            return random -> random.nextBoolean() ? Either.left(genL.apply(random)) : Either.right(genR.apply(random));
        };
    }

    /**
     * Generates arbitrary tries: a {@code Success} of a value of {@code arbitraryT} for three draws in four, a
     * {@code Failure} of one of a few shared exception instances otherwise. The instances are shared so that two
     * failures drawn with the same exception are equal ({@code Failure} compares its cause by reference).
     *
     * @param arbitraryT arbitrary values, never null
     * @param <T>        value type
     * @return a new Arbitrary of Try
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<Try<T>> tryOf(Arbitrary<T> arbitraryT) {
        return tryOf(arbitraryT, ofAll(Gen.choose(TryFailures.ALL)));
    }

    /**
     * Generates arbitrary tries: a {@code Success} of a value of {@code arbitraryT} for three draws in four, a
     * {@code Failure} of an exception of {@code failures} otherwise.
     *
     * @param arbitraryT arbitrary values, never null
     * @param failures   arbitrary non-fatal exceptions
     * @param <T>        value type
     * @return a new Arbitrary of Try
     * @throws NullPointerException if an argument is null
     */
    static <T> Arbitrary<Try<T>> tryOf(Arbitrary<T> arbitraryT, Arbitrary<? extends Exception> failures) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        Objects.requireNonNull(failures, "failures is null");
        return size -> {
            final Gen<T> genT = arbitraryT.apply(size);
            final Gen<? extends Exception> genFailure = failures.apply(size);
            return random -> random.nextInt(4) == 0 ? Try.failure(genFailure.apply(random)) : Try.success(genT.apply(random));
        };
    }

    /**
     * Generates arbitrary validations, {@code Valid} and {@code Invalid} with equal chances. An {@code Invalid}
     * holds between one and three errors.
     *
     * @param arbitraryE arbitrary errors, never null
     * @param arbitraryA arbitrary values, never null
     * @param <E>        error type
     * @param <A>        value type
     * @return a new Arbitrary of Validation
     * @throws NullPointerException if an argument is null
     */
    static <E, A> Arbitrary<Validation<E, A>> validation(Arbitrary<E> arbitraryE, Arbitrary<A> arbitraryA) {
        Objects.requireNonNull(arbitraryE, "arbitraryE is null");
        Objects.requireNonNull(arbitraryA, "arbitraryA is null");
        return size -> {
            final Gen<E> genE = arbitraryE.apply(size);
            final Gen<A> genA = arbitraryA.apply(size);
            return random -> {
                if (random.nextBoolean()) {
                    return Validation.valid(genA.apply(random));
                }
                NonEmptyVector<E> errors = NonEmptyVector.single(genE.apply(random));
                for (int extra = random.nextInt(3); extra > 0; extra--) {
                    errors = errors.append(genE.apply(random));
                }
                return errors.size() == 1 && random.nextBoolean()
                        ? Validation.invalid(errors.head())
                        : Validation.invalidAll(errors);
            };
        };
    }

    /**
     * Generates arbitrary lazy values of {@code arbitraryT}, already evaluated or not with equal chances.
     *
     * @param arbitraryT arbitrary values, never null
     * @param <T>        value type
     * @return a new Arbitrary of Lazy
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<Lazy<T>> lazy(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> {
            final Gen<T> genT = arbitraryT.apply(size);
            return random -> {
                final T value = genT.apply(random);
                final Lazy<T> lazy = Lazy.of(() -> value);
                if (random.nextBoolean()) {
                    lazy.get();
                }
                return lazy;
            };
        };
    }

    /**
     * Generates arbitrary 2-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @return a new Arbitrary of Tuple2
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2> Arbitrary<Tuple2<T1, T2>> tuple2(Arbitrary<T1> a1, Arbitrary<T2> a2) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random));
        };
    }

    /**
     * Generates arbitrary 3-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param a3   arbitrary component 3
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @param <T3> type of component 3
     * @return a new Arbitrary of Tuple3
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2, T3> Arbitrary<Tuple3<T1, T2, T3>> tuple3(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        Objects.requireNonNull(a3, "a3 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            final Gen<T3> g3 = a3.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random), g3.apply(random));
        };
    }

    /**
     * Generates arbitrary 4-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param a3   arbitrary component 3
     * @param a4   arbitrary component 4
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @param <T3> type of component 3
     * @param <T4> type of component 4
     * @return a new Arbitrary of Tuple4
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2, T3, T4> Arbitrary<Tuple4<T1, T2, T3, T4>> tuple4(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        Objects.requireNonNull(a3, "a3 is null");
        Objects.requireNonNull(a4, "a4 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            final Gen<T3> g3 = a3.apply(size);
            final Gen<T4> g4 = a4.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random), g3.apply(random), g4.apply(random));
        };
    }

    /**
     * Generates arbitrary 5-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param a3   arbitrary component 3
     * @param a4   arbitrary component 4
     * @param a5   arbitrary component 5
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @param <T3> type of component 3
     * @param <T4> type of component 4
     * @param <T5> type of component 5
     * @return a new Arbitrary of Tuple5
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2, T3, T4, T5> Arbitrary<Tuple5<T1, T2, T3, T4, T5>> tuple5(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4, Arbitrary<T5> a5) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        Objects.requireNonNull(a3, "a3 is null");
        Objects.requireNonNull(a4, "a4 is null");
        Objects.requireNonNull(a5, "a5 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            final Gen<T3> g3 = a3.apply(size);
            final Gen<T4> g4 = a4.apply(size);
            final Gen<T5> g5 = a5.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random), g3.apply(random), g4.apply(random), g5.apply(random));
        };
    }

    /**
     * Generates arbitrary 6-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param a3   arbitrary component 3
     * @param a4   arbitrary component 4
     * @param a5   arbitrary component 5
     * @param a6   arbitrary component 6
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @param <T3> type of component 3
     * @param <T4> type of component 4
     * @param <T5> type of component 5
     * @param <T6> type of component 6
     * @return a new Arbitrary of Tuple6
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2, T3, T4, T5, T6> Arbitrary<Tuple6<T1, T2, T3, T4, T5, T6>> tuple6(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4, Arbitrary<T5> a5, Arbitrary<T6> a6) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        Objects.requireNonNull(a3, "a3 is null");
        Objects.requireNonNull(a4, "a4 is null");
        Objects.requireNonNull(a5, "a5 is null");
        Objects.requireNonNull(a6, "a6 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            final Gen<T3> g3 = a3.apply(size);
            final Gen<T4> g4 = a4.apply(size);
            final Gen<T5> g5 = a5.apply(size);
            final Gen<T6> g6 = a6.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random), g3.apply(random), g4.apply(random), g5.apply(random), g6.apply(random));
        };
    }

    /**
     * Generates arbitrary 7-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param a3   arbitrary component 3
     * @param a4   arbitrary component 4
     * @param a5   arbitrary component 5
     * @param a6   arbitrary component 6
     * @param a7   arbitrary component 7
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @param <T3> type of component 3
     * @param <T4> type of component 4
     * @param <T5> type of component 5
     * @param <T6> type of component 6
     * @param <T7> type of component 7
     * @return a new Arbitrary of Tuple7
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2, T3, T4, T5, T6, T7> Arbitrary<Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple7(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4, Arbitrary<T5> a5, Arbitrary<T6> a6, Arbitrary<T7> a7) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        Objects.requireNonNull(a3, "a3 is null");
        Objects.requireNonNull(a4, "a4 is null");
        Objects.requireNonNull(a5, "a5 is null");
        Objects.requireNonNull(a6, "a6 is null");
        Objects.requireNonNull(a7, "a7 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            final Gen<T3> g3 = a3.apply(size);
            final Gen<T4> g4 = a4.apply(size);
            final Gen<T5> g5 = a5.apply(size);
            final Gen<T6> g6 = a6.apply(size);
            final Gen<T7> g7 = a7.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random), g3.apply(random), g4.apply(random), g5.apply(random), g6.apply(random), g7.apply(random));
        };
    }

    /**
     * Generates arbitrary 8-tuples whose components are drawn from the given arbitraries, in order.
     *
     * @param a1   arbitrary component 1
     * @param a2   arbitrary component 2
     * @param a3   arbitrary component 3
     * @param a4   arbitrary component 4
     * @param a5   arbitrary component 5
     * @param a6   arbitrary component 6
     * @param a7   arbitrary component 7
     * @param a8   arbitrary component 8
     * @param <T1> type of component 1
     * @param <T2> type of component 2
     * @param <T3> type of component 3
     * @param <T4> type of component 4
     * @param <T5> type of component 5
     * @param <T6> type of component 6
     * @param <T7> type of component 7
     * @param <T8> type of component 8
     * @return a new Arbitrary of Tuple8
     * @throws NullPointerException if an argument is null
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Arbitrary<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple8(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4, Arbitrary<T5> a5, Arbitrary<T6> a6, Arbitrary<T7> a7, Arbitrary<T8> a8) {
        Objects.requireNonNull(a1, "a1 is null");
        Objects.requireNonNull(a2, "a2 is null");
        Objects.requireNonNull(a3, "a3 is null");
        Objects.requireNonNull(a4, "a4 is null");
        Objects.requireNonNull(a5, "a5 is null");
        Objects.requireNonNull(a6, "a6 is null");
        Objects.requireNonNull(a7, "a7 is null");
        Objects.requireNonNull(a8, "a8 is null");
        return size -> {
            final Gen<T1> g1 = a1.apply(size);
            final Gen<T2> g2 = a2.apply(size);
            final Gen<T3> g3 = a3.apply(size);
            final Gen<T4> g4 = a4.apply(size);
            final Gen<T5> g5 = a5.apply(size);
            final Gen<T6> g6 = a6.apply(size);
            final Gen<T7> g7 = a7.apply(size);
            final Gen<T8> g8 = a8.apply(size);
            return random -> Tuple.of(g1.apply(random), g2.apply(random), g3.apply(random), g4.apply(random), g5.apply(random), g6.apply(random), g7.apply(random), g8.apply(random));
        };
    }

    /**
     * Generates arbitrary vectors of up to {@code size} elements of {@code arbitraryT}, favoring empty, singleton and
     * near-maximum lengths. Each vector is built along one of several paths: {@code ofAll}, a builder, appends,
     * prepends, a dropped prefix (the trie keeps an offset) and a slice of a longer vector.
     *
     * @param arbitraryT arbitrary elements, never null
     * @param <T>        element type
     * @return a new Arbitrary of Vector
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<Vector<T>> vector(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.vector(arbitraryT.apply(size), size);
    }

    /**
     * Generates arbitrary non-empty vectors of one to {@code max(1, size)} elements of {@code arbitraryT}, built
     * with {@code single}, {@code fromVector} and {@code prepend} on the shapes of {@link #vector(Arbitrary)}.
     *
     * @param arbitraryT arbitrary elements, never null
     * @param <T>        element type
     * @return a new Arbitrary of NonEmptyVector
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<NonEmptyVector<T>> nonEmptyVector(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.nonEmptyVector(arbitraryT.apply(size), size);
    }

    /**
     * Generates arbitrary lists of up to {@code size} elements of {@code arbitraryT}, favoring empty, singleton and
     * near-maximum lengths; nonpositive sizes produce empty lists. Each list is built with {@code ofAll}, with
     * prepends, or as the rest of a longer list after {@code drop}.
     * <p>
     * Example:
     * <pre>
     * <code>
     * Arbitrary.list(Arbitrary.integer());
     * </code>
     * </pre>
     *
     * @param arbitraryT Arbitrary elements of type T, never null
     * @param <T>        Component type of the List
     * @return a new Arbitrary of List&lt;T&gt;
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<List<T>> list(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.list(arbitraryT.apply(size), size);
    }

    /**
     * Generates arbitrary queues of up to {@code size} elements of {@code arbitraryT}, favoring empty, singleton and
     * near-maximum lengths. The elements sit in the front list only, in both internal lists, or in a rear list
     * reversed into the front by {@code drop}.
     *
     * @param arbitraryT arbitrary elements, never null
     * @param <T>        element type
     * @return a new Arbitrary of Queue
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<Queue<T>> queue(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.queue(arbitraryT.apply(size), size);
    }

    /**
     * Generates arbitrary finite streams of up to {@code size} elements of {@code arbitraryT}, favoring empty,
     * singleton and near-maximum lengths; nonpositive sizes produce empty streams. The elements are drawn when the
     * stream is generated; the stream is built with {@code ofAll}, as a chain of lazy tails (with or without an
     * evaluated prefix), as an eager prefix with a lazy suffix appended, or as the rest of a longer chain.
     * <p>
     * Example:
     * <pre>
     * <code>
     * Arbitrary.stream(Arbitrary.integer());
     * </code>
     * </pre>
     *
     * @param arbitraryT Arbitrary elements of type T, never null
     * @param <T>        Component type of the Stream
     * @return a new Arbitrary of Stream&lt;T&gt;
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<Stream<T>> stream(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.stream(arbitraryT.apply(size), size);
    }

    /**
     * Generates arbitrary hash sets of up to {@code size} drawn elements of {@code arbitraryT} (fewer when draws
     * repeat), built with {@code ofAll}, with one {@code add} at a time, after extra elements were added and removed
     * again, or after elements were removed and added back.
     *
     * @param arbitraryT arbitrary elements, never null
     * @param <T>        element type
     * @return a new Arbitrary of HashSet
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<HashSet<T>> hashSet(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.set(arbitraryT.apply(size), size, Shapes.hashSetOps());
    }

    /**
     * Generates arbitrary linked hash sets, along the same paths as {@link #hashSet(Arbitrary)}.
     *
     * @param arbitraryT arbitrary elements, never null
     * @param <T>        element type
     * @return a new Arbitrary of LinkedHashSet
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T> Arbitrary<LinkedHashSet<T>> linkedHashSet(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.set(arbitraryT.apply(size), size, Shapes.linkedHashSetOps());
    }

    /**
     * Generates arbitrary tree sets in natural order, along the same paths as {@link #hashSet(Arbitrary)}.
     *
     * @param arbitraryT arbitrary elements, never null
     * @param <T>        element type
     * @return a new Arbitrary of TreeSet
     * @throws NullPointerException if {@code arbitraryT} is null
     */
    static <T extends Comparable<? super T>> Arbitrary<TreeSet<T>> treeSet(Arbitrary<T> arbitraryT) {
        Objects.requireNonNull(arbitraryT, "arbitraryT is null");
        return size -> Shapes.set(arbitraryT.apply(size), size, Shapes.treeSetOps());
    }

    /**
     * Generates arbitrary hash maps of up to {@code size} drawn entries (fewer when keys repeat), built with
     * {@code ofAll} of a JDK map, with one {@code put} at a time, after extra keys were put and removed again, or
     * with every key first put with another value and then overwritten.
     *
     * @param arbitraryK arbitrary keys, never null
     * @param arbitraryV arbitrary values, never null
     * @param <K>        key type
     * @param <V>        value type
     * @return a new Arbitrary of HashMap
     * @throws NullPointerException if an argument is null
     */
    static <K, V> Arbitrary<HashMap<K, V>> hashMap(Arbitrary<K> arbitraryK, Arbitrary<V> arbitraryV) {
        Objects.requireNonNull(arbitraryK, "arbitraryK is null");
        Objects.requireNonNull(arbitraryV, "arbitraryV is null");
        return size -> Shapes.map(arbitraryK.apply(size), arbitraryV.apply(size), size, Shapes.hashMapOps());
    }

    /**
     * Generates arbitrary linked hash maps, along the same paths as {@link #hashMap(Arbitrary, Arbitrary)}.
     *
     * @param arbitraryK arbitrary keys, never null
     * @param arbitraryV arbitrary values, never null
     * @param <K>        key type
     * @param <V>        value type
     * @return a new Arbitrary of LinkedHashMap
     * @throws NullPointerException if an argument is null
     */
    static <K, V> Arbitrary<LinkedHashMap<K, V>> linkedHashMap(Arbitrary<K> arbitraryK, Arbitrary<V> arbitraryV) {
        Objects.requireNonNull(arbitraryK, "arbitraryK is null");
        Objects.requireNonNull(arbitraryV, "arbitraryV is null");
        return size -> Shapes.map(arbitraryK.apply(size), arbitraryV.apply(size), size, Shapes.linkedHashMapOps());
    }

    /**
     * Generates arbitrary tree maps in natural key order, along the same paths as
     * {@link #hashMap(Arbitrary, Arbitrary)}.
     *
     * @param arbitraryK arbitrary keys, never null
     * @param arbitraryV arbitrary values, never null
     * @param <K>        key type
     * @param <V>        value type
     * @return a new Arbitrary of TreeMap
     * @throws NullPointerException if an argument is null
     */
    static <K extends Comparable<? super K>, V> Arbitrary<TreeMap<K, V>> treeMap(Arbitrary<K> arbitraryK, Arbitrary<V> arbitraryV) {
        Objects.requireNonNull(arbitraryK, "arbitraryK is null");
        Objects.requireNonNull(arbitraryV, "arbitraryV is null");
        return size -> Shapes.map(arbitraryK.apply(size), arbitraryV.apply(size), size, Shapes.treeMapOps());
    }
}
