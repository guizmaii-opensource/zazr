package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Function3;
import com.guizmaii.zazr.Function4;
import com.guizmaii.zazr.Function5;
import com.guizmaii.zazr.Function6;
import com.guizmaii.zazr.Function7;
import com.guizmaii.zazr.Function8;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * A generator of values of type {@code A}, to check a property against.
 * <p>
 * Given a seeded source of randomness and a size, a generator produces a sequence of values, one <em>pass</em>. A
 * random generator such as {@link #intValue()} gives one value per pass; a finite generator such as
 * {@link #fromIterable(Iterable)} gives all its values, in order, in one pass. {@link Check#check} runs pass after
 * pass until it has its samples, and {@link Check#checkAll} runs one pass, so it checks every value of a finite
 * generator exactly once. A generator holds no state: the same seed and size give the same values.
 * <p>
 * The size bounds what a generator produces: the length of a collection or a string, how far {@link #small} and
 * {@link #large} reach. {@link #size()}, {@link #sized}, {@link #small} and {@link #large} read it, and
 * {@link #withSize(int)} sets it for one generator. The checks grow the size over a run, from 0 to the configured
 * size, so the first samples are the small ones.
 * <p>
 * {@link #map}, {@link #flatMap} and {@link #zip} combine generators; {@code flatMap} and {@code zip} run the second
 * generator for every value of the first, so two finite generators give every pair. {@link #filter} keeps the values
 * that satisfy a predicate, within a discard budget.
 *
 * @param <A> the type of the generated values
 */
public final class Gen<A> {

    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String ALPHA_NUMERIC = NUMERIC + ALPHA;

    private final Pass<A> pass;

    private Gen(Pass<A> pass) {
        this.pass = pass;
    }

    // -- the internal representation

    /**
     * One pass of a generator: pushes its values to {@code sink}, in order, until it has no more or the sink asks
     * to stop.
     */
    @FunctionalInterface
    interface Pass<A> {

        /**
         * Runs one pass.
         *
         * @param sampling the state of the run
         * @param size     the current size, never negative
         * @param sink     receives the values
         * @return false when the sink asked to stop, true when the pass ran to its end
         */
        boolean run(Sampling sampling, int size, Sink<? super A> sink);
    }

    /**
     * Receives the values of a pass.
     */
    @FunctionalInterface
    interface Sink<A> {

        /**
         * Receives one value.
         *
         * @param value a generated value
         * @return true to receive the next value, false to stop the pass
         */
        boolean accept(A value);
    }

    static <A> Gen<A> fromPass(Pass<A> pass) {
        return new Gen<>(pass);
    }

    boolean run(Sampling sampling, int size, Sink<? super A> sink) {
        return pass.run(sampling, size, sink);
    }

    /**
     * The first value of a pass. A pass that produced nothing after drawing random values is run again, up to the
     * discard budget; one that drew nothing would produce nothing again.
     *
     * @throws IllegalStateException when no pass produces a value
     */
    A draw(Sampling sampling, int size) {
        final Holder<A> holder = new Holder<>();
        for (int attempts = 0; ; attempts++) {
            final int draws = sampling.draws;
            pass.run(sampling, size, value -> {
                holder.value = value;
                holder.found = true;
                return false;
            });
            if (holder.found) {
                return holder.value;
            } else if (sampling.draws == draws) {
                throw new IllegalStateException("the generator produced no value at size " + size);
            } else if (attempts >= sampling.maxDiscards) {
                throw new IllegalStateException("the generator produced no value in " + (attempts + 1)
                        + " passes in a row at size " + size);
            }
        }
    }

    private static final class Holder<A> {
        A value;
        boolean found;
    }

    // -- constructors

    /**
     * A generator of one value.
     *
     * @param value the value, possibly null
     * @param <A>   the type of the value
     * @return a finite generator of {@code value}
     */
    public static <A> Gen<A> constant(A value) {
        return new Gen<>((sampling, size, sink) -> sink.accept(value));
    }

    /**
     * A generator of no value.
     *
     * @param <A> the type of the values it does not produce
     * @return a finite generator of nothing
     */
    public static <A> Gen<A> empty() {
        return new Gen<>((sampling, size, sink) -> true);
    }

    /**
     * A finite generator of the given values, in order. The values are copied when the generator is built, so a
     * one-shot iterable can be passed; an infinite one cannot.
     *
     * @param values the values, possibly null
     * @param <A>    the type of the values
     * @return a finite generator of {@code values}
     * @throws NullPointerException if {@code values} is null
     */
    public static <A> Gen<A> fromIterable(Iterable<? extends A> values) {
        Objects.requireNonNull(values, "values is null");
        final ArrayList<A> copy = new ArrayList<>();
        for (A value : values) {
            copy.add(value);
        }
        final Object[] array = copy.toArray();
        return new Gen<>((sampling, size, sink) -> {
            for (Object value : array) {
                @SuppressWarnings("unchecked")
                final A a = (A) value;
                if (!sink.accept(a)) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * A random choice among the given values, each as likely as the others. No value gives {@link #empty()}.
     *
     * @param values the values, possibly null
     * @param <A>    the type of the values
     * @return a random generator of one of {@code values}
     * @throws NullPointerException if {@code values} is null
     */
    @SafeVarargs
    public static <A> Gen<A> elements(A... values) {
        Objects.requireNonNull(values, "values is null");
        if (values.length == 0) {
            return empty();
        }
        final Object[] copy = Arrays.copyOf(values, values.length, Object[].class);
        return fromRandom(random -> {
            @SuppressWarnings("unchecked")
            final A a = (A) copy[random.nextInt(copy.length)];
            return a;
        });
    }

    /**
     * A random generator that computes each value from the source of randomness.
     *
     * @param f computes a value; it must draw every random number it uses from its argument, so that a seed
     *          replays the run
     * @param <A> the type of the values
     * @return a random generator
     * @throws NullPointerException if {@code f} is null
     */
    public static <A> Gen<A> fromRandom(Function<? super RandomGenerator, ? extends A> f) {
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> sink.accept(f.apply(sampling.draw())));
    }

    /**
     * A generator built when it runs, for generators that refer to themselves.
     *
     * @param gen builds the generator, each time it runs
     * @param <A> the type of the values
     * @return a generator of the values of {@code gen}'s generator
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<A> suspend(Supplier<? extends Gen<? extends A>> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return new Gen<>((sampling, size, sink) ->
                Objects.requireNonNull(gen.get(), "suspend: gen returned null").run(sampling, size, sink));
    }

    /**
     * A random choice among the given generators, each as likely as the others. No generator gives
     * {@link #empty()}.
     *
     * @param gens the generators
     * @param <A>  the type of the values
     * @return a generator of the values of one of {@code gens}, chosen at random for each pass
     * @throws NullPointerException if {@code gens} or one of them is null
     */
    @SafeVarargs
    public static <A> Gen<A> oneOf(Gen<? extends A>... gens) {
        Objects.requireNonNull(gens, "gens is null");
        final Gen<?>[] copy = Arrays.copyOf(gens, gens.length, Gen[].class);
        for (Gen<?> gen : copy) {
            Objects.requireNonNull(gen, "gens contains null");
        }
        if (copy.length == 0) {
            return empty();
        }
        return new Gen<>((sampling, size, sink) -> {
            @SuppressWarnings("unchecked")
            final Gen<? extends A> gen = (Gen<? extends A>) copy[sampling.draw().nextInt(copy.length)];
            return gen.run(sampling, size, sink);
        });
    }

    /**
     * A random choice among the given generators, each with a probability proportional to its weight: with
     * weights 9 and 1, the first generator is chosen nine times in ten. A generator of weight 0 is never chosen.
     * No generator gives {@link #empty()}.
     *
     * @param gens the generators and their weights
     * @param <A>  the type of the values
     * @return a generator of the values of one of {@code gens}, chosen at random for each pass
     * @throws NullPointerException     if {@code gens}, one of them, or one of their generators or weights is null
     * @throws IllegalArgumentException if a weight is negative, infinite or not a number, or if every weight is 0
     */
    @SafeVarargs
    public static <A> Gen<A> weighted(Tuple2<? extends Gen<? extends A>, Double>... gens) {
        Objects.requireNonNull(gens, "gens is null");
        if (gens.length == 0) {
            return empty();
        }
        final Gen<?>[] choices = new Gen<?>[gens.length];
        final double[] cumulative = new double[gens.length];
        double total = 0;
        for (int i = 0; i < gens.length; i++) {
            final Tuple2<? extends Gen<? extends A>, Double> entry = Objects.requireNonNull(gens[i], "gens contains null");
            choices[i] = Objects.requireNonNull(entry._1(), "gens contains a null generator");
            final double weight = Objects.requireNonNull(entry._2(), "gens contains a null weight");
            if (!(weight >= 0) || Double.isInfinite(weight)) {
                throw new IllegalArgumentException("weight " + weight + " is not a finite number >= 0");
            }
            total += weight;
            cumulative[i] = total;
        }
        if (!(total > 0) || Double.isInfinite(total)) {
            throw new IllegalArgumentException("the weights add up to " + total);
        }
        final double sum = total;
        int positive = cumulative.length - 1;
        while (cumulative[positive] == (positive == 0 ? 0 : cumulative[positive - 1])) {
            positive--;
        }
        final int lastPositive = positive;
        return new Gen<>((sampling, size, sink) -> {
            final double point = sampling.draw().nextDouble() * sum;
            // the first generator whose share ends after the point: a share of weight 0 ends where the previous one
            // does, so it is never the first; the bound covers a point rounded up to the sum
            int i = 0;
            while (i < lastPositive && cumulative[i] <= point) {
                i++;
            }
            @SuppressWarnings("unchecked")
            final Gen<? extends A> gen = (Gen<? extends A>) choices[i];
            return gen.run(sampling, size, sink);
        });
    }

    /**
     * A generator of lists built by applying {@code f} repeatedly to a state, starting from {@code initial}; the
     * length is drawn by {@link #small(IntFunction)}.
     *
     * @param initial the first state
     * @param f       generates the next state and an element from a state
     * @param <S>     the type of the state
     * @param <A>     the type of the elements
     * @return a generator of lists
     * @throws NullPointerException if {@code f} is null
     */
    public static <S, A> Gen<List<A>> unfoldGen(S initial, Function<? super S, ? extends Gen<? extends Tuple2<? extends S, ? extends A>>> f) {
        Objects.requireNonNull(f, "f is null");
        return small(n -> unfoldGenN(n, initial, f));
    }

    /**
     * A generator of lists of {@code n} elements built by applying {@code f} repeatedly to a state, starting from
     * {@code initial}. Each step draws the first value of one pass of {@code f}'s generator.
     *
     * @param n       the number of elements
     * @param initial the first state
     * @param f       generates the next state and an element from a state
     * @param <S>     the type of the state
     * @param <A>     the type of the elements
     * @return a generator of lists of {@code n} elements
     * @throws NullPointerException     if {@code f} is null
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public static <S, A> Gen<List<A>> unfoldGenN(int n, S initial, Function<? super S, ? extends Gen<? extends Tuple2<? extends S, ? extends A>>> f) {
        Objects.requireNonNull(f, "f is null");
        requireNonNegative(n, "n");
        return new Gen<>((sampling, size, sink) -> {
            final ArrayList<A> elements = new ArrayList<>(n);
            S state = initial;
            for (int i = 0; i < n; i++) {
                final Gen<? extends Tuple2<? extends S, ? extends A>> step = Objects.requireNonNull(f.apply(state), "unfoldGen: f returned null");
                final Tuple2<? extends S, ? extends A> next = Objects.requireNonNull(step.draw(sampling, size), "unfoldGen: f generated null");
                state = next._1();
                elements.add(next._2());
            }
            return sink.accept(List.ofAll(elements));
        });
    }

    /**
     * A generator of the lists of one value of each generator, in order: every combination when the generators are
     * finite.
     *
     * @param gens the generators
     * @param <A>  the type of the values
     * @return a generator of lists as long as {@code gens}
     * @throws NullPointerException if {@code gens} or one of them is null
     */
    public static <A> Gen<List<A>> collectAll(Iterable<? extends Gen<? extends A>> gens) {
        Objects.requireNonNull(gens, "gens is null");
        final ArrayList<Gen<? extends A>> copy = new ArrayList<>();
        for (Gen<? extends A> gen : gens) {
            copy.add(Objects.requireNonNull(gen, "gens contains null"));
        }
        return new Gen<>((sampling, size, sink) -> collectAll(copy, 0, List.empty(), sampling, size, sink));
    }

    private static <A> boolean collectAll(java.util.List<Gen<? extends A>> gens, int index, List<A> reversed,
                                          Sampling sampling, int size, Sink<? super List<A>> sink) {
        if (index == gens.size()) {
            return sink.accept(reversed.reverse());
        }
        return gens.get(index).run(sampling, size, a -> collectAll(gens, index + 1, reversed.prepend(a), sampling, size, sink));
    }

    // -- size

    /**
     * The current size.
     *
     * @return a generator of the size
     */
    public static Gen<Integer> size() {
        return new Gen<>((sampling, size, sink) -> sink.accept(size));
    }

    /**
     * A generator that depends on the current size.
     *
     * @param f the generator for a size
     * @param <A> the type of the values
     * @return a generator of the values of {@code f}'s generator at the current size
     * @throws NullPointerException if {@code f} is null
     */
    public static <A> Gen<A> sized(IntFunction<? extends Gen<? extends A>> f) {
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) ->
                Objects.requireNonNull(f.apply(size), "sized: f returned null").run(sampling, size, sink));
    }

    /**
     * A generator given a random size between 0 and the current size, most of them small: the size follows an
     * exponential distribution of mean a twenty-fifth of the current size.
     *
     * @param f the generator for a size
     * @param <A> the type of the values
     * @return a generator of the values of {@code f}'s generator at a small size
     * @throws NullPointerException if {@code f} is null
     */
    public static <A> Gen<A> small(IntFunction<? extends Gen<? extends A>> f) {
        return small(f, 0);
    }

    /**
     * A generator given a random size between {@code min} and the current size, most of them small: the size
     * follows an exponential distribution of mean a twenty-fifth of the current size, raised to {@code min}. When
     * {@code min} is above the current size, the size is {@code min}.
     *
     * @param f   the generator for a size
     * @param min the smallest size
     * @param <A> the type of the values
     * @return a generator of the values of {@code f}'s generator at a small size
     * @throws NullPointerException     if {@code f} is null
     * @throws IllegalArgumentException if {@code min} is negative
     */
    public static <A> Gen<A> small(IntFunction<? extends Gen<? extends A>> f, int min) {
        Objects.requireNonNull(f, "f is null");
        requireNonNegative(min, "min");
        return new Gen<>((sampling, size, sink) -> {
            final double exponential = -Math.log(1 - sampling.draw().nextDouble());
            final long drawn = Math.round(exponential * size / 25.0);
            final int chosen = (int) Math.max(min, Math.min(drawn, size));
            return Objects.requireNonNull(f.apply(chosen), "small: f returned null").run(sampling, size, sink);
        });
    }

    /**
     * A generator given a random size between 0 and the current size, all of them as likely.
     *
     * @param f the generator for a size
     * @param <A> the type of the values
     * @return a generator of the values of {@code f}'s generator at a random size
     * @throws NullPointerException if {@code f} is null
     */
    public static <A> Gen<A> large(IntFunction<? extends Gen<? extends A>> f) {
        return large(f, 0);
    }

    /**
     * A generator given a random size between {@code min} and the current size, all of them as likely. When
     * {@code min} is above the current size, the size is {@code min}.
     *
     * @param f   the generator for a size
     * @param min the smallest size
     * @param <A> the type of the values
     * @return a generator of the values of {@code f}'s generator at a random size
     * @throws NullPointerException     if {@code f} is null
     * @throws IllegalArgumentException if {@code min} is negative
     */
    public static <A> Gen<A> large(IntFunction<? extends Gen<? extends A>> f, int min) {
        Objects.requireNonNull(f, "f is null");
        requireNonNegative(min, "min");
        return new Gen<>((sampling, size, sink) -> {
            final int chosen = (int) sampling.draw().nextLong(min, Math.max(min, size) + 1L);
            return Objects.requireNonNull(f.apply(chosen), "large: f returned null").run(sampling, size, sink);
        });
    }

    /**
     * This generator at a fixed size, whatever the current size.
     *
     * @param size the size
     * @return a generator of the values of this generator at {@code size}
     * @throws IllegalArgumentException if {@code size} is negative
     */
    public Gen<A> withSize(int size) {
        requireNonNegative(size, "size");
        return new Gen<>((sampling, ignored, sink) -> pass.run(sampling, size, sink));
    }

    // -- combinators

    /**
     * A generator of the values of this generator transformed by {@code f}.
     *
     * @param f   transforms a value
     * @param <B> the type of the transformed values
     * @return a new generator
     * @throws NullPointerException if {@code f} is null
     */
    public <B> Gen<B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> pass.run(sampling, size, a -> sink.accept(f.apply(a))));
    }

    /**
     * A generator that runs the generator {@code f} gives for each value of this generator. With two finite
     * generators, it gives the values of {@code f}'s generator for every value of this one, in order.
     *
     * @param f   the generator for a value
     * @param <B> the type of the values of the new generator
     * @return a new generator
     * @throws NullPointerException if {@code f} is null
     */
    public <B> Gen<B> flatMap(Function<? super A, ? extends Gen<? extends B>> f) {
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> pass.run(sampling, size, a ->
                Objects.requireNonNull(f.apply(a), "flatMap: f returned null").run(sampling, size, sink)));
    }

    /**
     * A generator of the values of this generator that satisfy {@code predicate}.
     * <p>
     * When a pass of a random generator produced only rejected values, the pass runs again, so a filtered random
     * generator still gives one value per pass; a finite generator is not run again, it only loses the rejected
     * values. More rejected values in a row than the discard budget of the run ({@link CheckConfig#maxDiscards()})
     * throw an {@link IllegalStateException}. A predicate that rejects most values is better written as a
     * {@link #map} or a {@link #flatMap} that builds the wanted values directly.
     *
     * @param predicate the condition a value must satisfy
     * @return a new generator
     * @throws NullPointerException if {@code predicate} is null
     */
    public Gen<A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return new Gen<>((sampling, size, sink) -> {
            final FilterState state = new FilterState();
            while (true) {
                state.produced = false;
                state.accepted = false;
                final int draws = sampling.draws;
                final boolean more = pass.run(sampling, size, a -> {
                    state.produced = true;
                    if (predicate.test(a)) {
                        state.accepted = true;
                        state.rejected = 0;
                        return sink.accept(a);
                    } else if (++state.rejected > sampling.maxDiscards) {
                        throw new IllegalStateException("Gen.filter rejected " + state.rejected + " values in a row,"
                                + " more than the discard budget of " + sampling.maxDiscards
                                + ": generate the wanted values with map or flatMap instead of filtering them");
                    }
                    return true;
                });
                if (!more) {
                    return false;
                } else if (state.accepted || !state.produced || sampling.draws == draws) {
                    return true;
                }
            }
        });
    }

    private static final class FilterState {
        boolean produced;
        boolean accepted;
        int rejected;
    }

    /**
     * A generator of the values of this generator that do not satisfy {@code predicate}, as {@link #filter}.
     *
     * @param predicate the condition a value must not satisfy
     * @return a new generator
     * @throws NullPointerException if {@code predicate} is null
     */
    public Gen<A> filterNot(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    /**
     * A generator of the values of this generator followed by those of {@code that}, in one pass.
     *
     * @param that the next generator
     * @return a new generator
     * @throws NullPointerException if {@code that} is null
     */
    public Gen<A> concat(Gen<? extends A> that) {
        Objects.requireNonNull(that, "that is null");
        return new Gen<>((sampling, size, sink) -> pass.run(sampling, size, sink) && that.run(sampling, size, sink));
    }

    /**
     * A generator of the pairs of a value of this generator and a value of {@code that}, as
     * {@link #zip(Gen, Gen)}.
     *
     * @param that the second generator
     * @param <B>  the type of its values
     * @return a new generator
     * @throws NullPointerException if {@code that} is null
     */
    public <B> Gen<Tuple2<A, B>> zip(Gen<? extends B> that) {
        return zip(this, that);
    }

    /**
     * A generator combining a value of this generator and a value of {@code that} with {@code f}, as
     * {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param that the second generator
     * @param f    combines the two values
     * @param <B>  the type of the values of {@code that}
     * @param <C>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public <B, C> Gen<C> zipWith(Gen<? extends B> that, BiFunction<? super A, ? super B, ? extends C> f) {
        return zipWith(this, that, f);
    }

    // -- zip at every arity

    /**
     * A generator of the tuples of one value of each generator, drawn in order: for each value of the first, every
     * value of the second, and so on. Random generators give one tuple per pass; finite generators give every
     * combination.
     *
     * @param g1   the generator of the first component
     * @param g2   the generator of the second component
     * @param <T1> the type of the first component
     * @param <T2> the type of the second component
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> Gen<Tuple2<T1, T2>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2) {
        return zipWith(g1, g2, Tuple::of);
    }

    /**
     * A generator of 3-tuples, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> Gen<Tuple3<T1, T2, T3>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3) {
        return zipWith(g1, g2, g3, Tuple::of);
    }

    /**
     * A generator of 4-tuples, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> Gen<Tuple4<T1, T2, T3, T4>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4) {
        return zipWith(g1, g2, g3, g4, Tuple::of);
    }

    /**
     * A generator of 5-tuples, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> Gen<Tuple5<T1, T2, T3, T4, T5>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5) {
        return zipWith(g1, g2, g3, g4, g5, Tuple::of);
    }

    /**
     * A generator of 6-tuples, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param g6   the generator of component 6
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @param <T6> the type of component 6
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> Gen<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6) {
        return zipWith(g1, g2, g3, g4, g5, g6, Tuple::of);
    }

    /**
     * A generator of 7-tuples, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param g6   the generator of component 6
     * @param g7   the generator of component 7
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @param <T6> the type of component 6
     * @param <T7> the type of component 7
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Gen<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7) {
        return zipWith(g1, g2, g3, g4, g5, g6, g7, Tuple::of);
    }

    /**
     * A generator of 8-tuples, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param g6   the generator of component 6
     * @param g7   the generator of component 7
     * @param g8   the generator of component 8
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @param <T6> the type of component 6
     * @param <T7> the type of component 7
     * @param <T8> the type of component 8
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Gen<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8) {
        return zipWith(g1, g2, g3, g4, g5, g6, g7, g8, Tuple::of);
    }

    /**
     * A generator combining one value of each generator with {@code f}, the values drawn as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the first generator
     * @param g2   the second generator
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, BiFunction<? super T1, ? super T2, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> sink.accept(f.apply(t1, t2)))));
    }

    /**
     * A generator combining one value of each generator with {@code f}, as {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param g1   generator 1
     * @param g2   generator 2
     * @param g3   generator 3
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <T3> the type of the values of {@code g3}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> g3.run(sampling, size,
                t3 -> sink.accept(f.apply(t1, t2, t3))))));
    }

    /**
     * A generator combining one value of each generator with {@code f}, as {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param g1   generator 1
     * @param g2   generator 2
     * @param g3   generator 3
     * @param g4   generator 4
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <T3> the type of the values of {@code g3}
     * @param <T4> the type of the values of {@code g4}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> g3.run(sampling, size,
                t3 -> g4.run(sampling, size, t4 -> sink.accept(f.apply(t1, t2, t3, t4)))))));
    }

    /**
     * A generator combining one value of each generator with {@code f}, as {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param g1   generator 1
     * @param g2   generator 2
     * @param g3   generator 3
     * @param g4   generator 4
     * @param g5   generator 5
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <T3> the type of the values of {@code g3}
     * @param <T4> the type of the values of {@code g4}
     * @param <T5> the type of the values of {@code g5}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> g3.run(sampling, size,
                t3 -> g4.run(sampling, size, t4 -> g5.run(sampling, size, t5 -> sink.accept(f.apply(t1, t2, t3, t4, t5))))))));
    }

    /**
     * A generator combining one value of each generator with {@code f}, as {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param g1   generator 1
     * @param g2   generator 2
     * @param g3   generator 3
     * @param g4   generator 4
     * @param g5   generator 5
     * @param g6   generator 6
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <T3> the type of the values of {@code g3}
     * @param <T4> the type of the values of {@code g4}
     * @param <T5> the type of the values of {@code g5}
     * @param <T6> the type of the values of {@code g6}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> g3.run(sampling, size,
                t3 -> g4.run(sampling, size, t4 -> g5.run(sampling, size, t5 -> g6.run(sampling, size,
                        t6 -> sink.accept(f.apply(t1, t2, t3, t4, t5, t6)))))))));
    }

    /**
     * A generator combining one value of each generator with {@code f}, as {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param g1   generator 1
     * @param g2   generator 2
     * @param g3   generator 3
     * @param g4   generator 4
     * @param g5   generator 5
     * @param g6   generator 6
     * @param g7   generator 7
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <T3> the type of the values of {@code g3}
     * @param <T4> the type of the values of {@code g4}
     * @param <T5> the type of the values of {@code g5}
     * @param <T6> the type of the values of {@code g6}
     * @param <T7> the type of the values of {@code g7}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(g7, "g7 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> g3.run(sampling, size,
                t3 -> g4.run(sampling, size, t4 -> g5.run(sampling, size, t5 -> g6.run(sampling, size,
                        t6 -> g7.run(sampling, size, t7 -> sink.accept(f.apply(t1, t2, t3, t4, t5, t6, t7))))))))));
    }

    /**
     * A generator combining one value of each generator with {@code f}, as {@link #zipWith(Gen, Gen, BiFunction)}.
     *
     * @param g1   generator 1
     * @param g2   generator 2
     * @param g3   generator 3
     * @param g4   generator 4
     * @param g5   generator 5
     * @param g6   generator 6
     * @param g7   generator 7
     * @param g8   generator 8
     * @param f    combines the values
     * @param <T1> the type of the values of {@code g1}
     * @param <T2> the type of the values of {@code g2}
     * @param <T3> the type of the values of {@code g3}
     * @param <T4> the type of the values of {@code g4}
     * @param <T5> the type of the values of {@code g5}
     * @param <T6> the type of the values of {@code g6}
     * @param <T7> the type of the values of {@code g7}
     * @param <T8> the type of the values of {@code g8}
     * @param <R>  the type of the combined values
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Gen<R> zipWith(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(g7, "g7 is null");
        Objects.requireNonNull(g8, "g8 is null");
        Objects.requireNonNull(f, "f is null");
        return new Gen<>((sampling, size, sink) -> g1.run(sampling, size, t1 -> g2.run(sampling, size, t2 -> g3.run(sampling, size,
                t3 -> g4.run(sampling, size, t4 -> g5.run(sampling, size, t5 -> g6.run(sampling, size,
                        t6 -> g7.run(sampling, size, t7 -> g8.run(sampling, size, t8 -> sink.accept(f.apply(t1, t2, t3, t4, t5, t6, t7, t8)))))))))));
    }

    // -- scalars

    /**
     * A random boolean, {@code true} and {@code false} as likely.
     *
     * @return a random generator of booleans
     */
    public static Gen<Boolean> booleanValue() {
        return fromRandom(RandomGenerator::nextBoolean);
    }

    /**
     * A random {@code int} of the whole range, as {@link #intValue(int, int)}.
     *
     * @return a random generator of ints
     */
    public static Gen<Integer> intValue() {
        return intValue(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * A random {@code int} between {@code min} and {@code max}, both included. Half of the draws choose among the
     * edges in range: the bounds, their neighbours, -1, 0 and 1; the others are uniform over the range.
     *
     * @param min the smallest value
     * @param max the largest value
     * @return a random generator of ints
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static Gen<Integer> intValue(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " > max " + max);
        } else if (min == max) {
            return constant(min);
        }
        final int[] edges = java.util.stream.LongStream.of(min, min + 1L, -1, 0, 1, max - 1L, max)
                .filter(edge -> edge >= min && edge <= max).distinct().sorted().mapToInt(edge -> (int) edge).toArray();
        return fromRandom(random -> random.nextBoolean()
                ? edges[random.nextInt(edges.length)]
                : (int) random.nextLong(min, max + 1L));
    }

    /**
     * A random {@code long} of the whole range, as {@link #longValue(long, long)}.
     *
     * @return a random generator of longs
     */
    public static Gen<Long> longValue() {
        return longValue(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * A random {@code long} between {@code min} and {@code max}, both included. Half of the draws choose among the
     * edges in range: the bounds, their neighbours, -1, 0 and 1; the others are uniform over the range.
     *
     * @param min the smallest value
     * @param max the largest value
     * @return a random generator of longs
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static Gen<Long> longValue(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " > max " + max);
        } else if (min == max) {
            return constant(min);
        }
        final long[] edges = java.util.stream.LongStream.of(min, min + 1, -1, 0, 1, max - 1, max)
                .filter(edge -> edge >= min && edge <= max).distinct().sorted().toArray();
        return fromRandom(random -> {
            if (random.nextBoolean()) {
                return edges[random.nextInt(edges.length)];
            } else if (max < Long.MAX_VALUE) {
                return random.nextLong(min, max + 1);
            } else if (min > Long.MIN_VALUE) {
                return random.nextLong(min - 1, max) + 1;
            } else {
                return random.nextLong();
            }
        });
    }

    /**
     * A random {@code double} between 0 (included) and 1 (excluded), uniform.
     *
     * @return a random generator of doubles
     */
    public static Gen<Double> doubleValue() {
        return fromRandom(RandomGenerator::nextDouble);
    }

    /**
     * A random {@code double} between {@code min} and {@code max}, both included. Half of the draws choose among
     * the edges in range: the bounds, the values next to them, -1, 1, both zeros and the smallest nonzero values of
     * each sign; the others are uniform over the range.
     *
     * @param min the smallest value
     * @param max the largest value
     * @return a random generator of doubles
     * @throws IllegalArgumentException if a bound is infinite or not a number, or if {@code min > max}
     */
    public static Gen<Double> doubleValue(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            throw new IllegalArgumentException("the bounds " + min + " and " + max + " are not both finite");
        } else if (Double.compare(min, max) > 0) {
            throw new IllegalArgumentException("min " + min + " > max " + max);
        } else if (Double.compare(min, max) == 0) {
            return constant(min);
        }
        final double[] edges = java.util.stream.DoubleStream.of(min, Math.nextUp(min), -1.0, -Double.MIN_VALUE, -0.0, 0.0,
                        Double.MIN_VALUE, 1.0, Math.nextDown(max), max)
                .filter(edge -> Double.compare(edge, min) >= 0 && Double.compare(edge, max) <= 0)
                .boxed().distinct().sorted().mapToDouble(Double::doubleValue).toArray();
        return fromRandom(random -> {
            if (random.nextBoolean()) {
                return edges[random.nextInt(edges.length)];
            }
            final double fraction = random.nextDouble();
            return Math.max(min, Math.min(max, fraction * max + (1.0 - fraction) * min));
        });
    }

    /**
     * A random {@code char} of the whole range, as {@link #charValue(char, char)}.
     *
     * @return a random generator of chars
     */
    public static Gen<Character> charValue() {
        return charValue(Character.MIN_VALUE, Character.MAX_VALUE);
    }

    /**
     * A random {@code char} between {@code min} and {@code max}, both included, favouring the edges as
     * {@link #intValue(int, int)}.
     *
     * @param min the smallest char
     * @param max the largest char
     * @return a random generator of chars
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static Gen<Character> charValue(char min, char max) {
        return intValue(min, max).map(i -> (char) (int) i);
    }

    /**
     * A random ASCII letter, {@code A} to {@code Z} and {@code a} to {@code z}, each as likely.
     *
     * @return a random generator of letters
     */
    public static Gen<Character> alphaChar() {
        return charOf(ALPHA);
    }

    /**
     * A random ASCII digit or letter, {@code 0} to {@code 9}, {@code A} to {@code Z} and {@code a} to {@code z},
     * each as likely.
     *
     * @return a random generator of digits and letters
     */
    public static Gen<Character> alphaNumericChar() {
        return charOf(ALPHA_NUMERIC);
    }

    /**
     * A random ASCII digit, {@code 0} to {@code 9}, each as likely.
     *
     * @return a random generator of digits
     */
    public static Gen<Character> numericChar() {
        return charOf(NUMERIC);
    }

    /**
     * A random ASCII character, {@code \u0000} to {@code \u007F}, favouring the edges as {@link #charValue(char, char)}.
     *
     * @return a random generator of ASCII characters
     */
    public static Gen<Character> asciiChar() {
        return charValue('\u0000', '\u007F');
    }

    /**
     * A random printable ASCII character, {@code !} to {@code ~}, favouring the edges as
     * {@link #charValue(char, char)}.
     *
     * @return a random generator of printable characters
     */
    public static Gen<Character> printableChar() {
        return charValue('!', '~');
    }

    /**
     * A random Unicode character that is not a surrogate: {@code \u0000} to {@code ퟿} or {@code } to
     * {@code �}, each range as likely.
     *
     * @return a random generator of characters
     */
    public static Gen<Character> unicodeChar() {
        return oneOf(charValue('\u0000', '퟿'), charValue('', '�'));
    }

    private static Gen<Character> charOf(String chars) {
        return fromRandom(random -> chars.charAt(random.nextInt(chars.length())));
    }

    /**
     * A random string of {@link #unicodeChar()}s, as {@link #string(Gen)}.
     *
     * @return a random generator of strings
     */
    public static Gen<String> string() {
        return string(unicodeChar());
    }

    /**
     * A random string of the characters of {@code chars}, of a length between 0 and the current size. Half of the
     * lengths are the edges 0, 1, the size and the size minus one; the others are uniform.
     *
     * @param chars the generator of each character
     * @return a random generator of strings
     * @throws NullPointerException if {@code chars} is null
     */
    public static Gen<String> string(Gen<Character> chars) {
        Objects.requireNonNull(chars, "chars is null");
        return sized(size -> intValue(0, size)).flatMap(length -> stringN(length, chars));
    }

    /**
     * A random string of {@code n} characters of {@code chars}. Each character is the first value of one pass of
     * {@code chars}.
     *
     * @param n     the length
     * @param chars the generator of each character
     * @return a random generator of strings of length {@code n}
     * @throws NullPointerException     if {@code chars} is null
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public static Gen<String> stringN(int n, Gen<Character> chars) {
        Objects.requireNonNull(chars, "chars is null");
        requireNonNegative(n, "n");
        return new Gen<>((sampling, size, sink) -> {
            final char[] string = new char[n];
            for (int i = 0; i < n; i++) {
                string[i] = Objects.requireNonNull(chars.draw(sampling, size), "stringN: chars generated null");
            }
            return sink.accept(new String(string));
        });
    }

    /**
     * A random string of {@link #alphaNumericChar()}s, as {@link #string(Gen)}.
     *
     * @return a random generator of alphanumeric strings
     */
    public static Gen<String> alphaNumericString() {
        return string(alphaNumericChar());
    }

    /**
     * A random {@link LocalDateTime} of the whole range, as {@link #localDateTime(LocalDateTime, LocalDateTime)}.
     *
     * @return a random generator of date-times
     */
    public static Gen<LocalDateTime> localDateTime() {
        return localDateTime(LocalDateTime.MIN, LocalDateTime.MAX);
    }

    /**
     * A random {@link LocalDateTime} between {@code min} and {@code max}, both included. The epoch second and the
     * nanosecond are drawn as {@link #longValue(long, long)} and {@link #intValue(int, int)}, so the bounds and the
     * first and last nanoseconds of a second come up often; a date-time out of range is moved to the nearest bound.
     *
     * @param min the earliest date-time
     * @param max the latest date-time
     * @return a random generator of date-times
     * @throws NullPointerException     if an argument is null
     * @throws IllegalArgumentException if {@code min} is after {@code max}
     */
    public static Gen<LocalDateTime> localDateTime(LocalDateTime min, LocalDateTime max) {
        Objects.requireNonNull(min, "min is null");
        Objects.requireNonNull(max, "max is null");
        if (min.isAfter(max)) {
            throw new IllegalArgumentException("min " + min + " is after max " + max);
        }
        return zipWith(longValue(min.toEpochSecond(ZoneOffset.UTC), max.toEpochSecond(ZoneOffset.UTC)),
                intValue(0, 999_999_999),
                (second, nano) -> {
                    final LocalDateTime dateTime = LocalDateTime.ofEpochSecond(second, nano, ZoneOffset.UTC);
                    return dateTime.isBefore(min) ? min : dateTime.isAfter(max) ? max : dateTime;
                });
    }

    // -- the Zazr types

    /// The exceptions {@link #tryOf(Gen)} fails with, shared so that equal failures can be drawn twice.
    private static final Exception[] FAILURES = {
            new IllegalStateException("generated failure 1"),
            new IllegalArgumentException("generated failure 2"),
            new java.io.IOException("generated failure 3"),
    };

    /**
     * A random option: {@code None} for one pass in four, otherwise {@code Some} of each value of a pass of
     * {@code gen}.
     *
     * @param gen the generator of the values, possibly null ones
     * @param <A> the type of the values
     * @return a generator of options
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Option<A>> option(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return new Gen<>((sampling, size, sink) -> sampling.draw().nextInt(4) == 0
                ? sink.accept(Option.none())
                : gen.run(sampling, size, a -> sink.accept(Option.some(a))));
    }

    /**
     * {@code Some} of each value of {@code gen}.
     *
     * @param gen the generator of the values, possibly null ones
     * @param <A> the type of the values
     * @return a generator of {@code Some}s, finite when {@code gen} is
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Option<A>> some(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return gen.map(Option::some);
    }

    /**
     * {@code None}, once.
     *
     * @param <A> the type of the values the option does not hold
     * @return a finite generator of {@code None}
     */
    public static <A> Gen<Option<A>> none() {
        return constant(Option.none());
    }

    /**
     * A random either, {@code Left} and {@code Right} as likely: for each pass, a {@code Left} of each value of a
     * pass of {@code left}, or a {@code Right} of each value of a pass of {@code right}.
     *
     * @param left  the generator of the left values
     * @param right the generator of the right values
     * @param <L>   the type of the left values
     * @param <R>   the type of the right values
     * @return a generator of eithers
     * @throws NullPointerException if an argument is null
     */
    public static <L, R> Gen<Either<L, R>> either(Gen<L> left, Gen<R> right) {
        Objects.requireNonNull(left, "left is null");
        Objects.requireNonNull(right, "right is null");
        return new Gen<>((sampling, size, sink) -> sampling.draw().nextBoolean()
                ? left.run(sampling, size, l -> sink.accept(Either.left(l)))
                : right.run(sampling, size, r -> sink.accept(Either.right(r))));
    }

    /**
     * A random try: for one pass in four, a {@code Failure} of one of three exceptions shared by every call, so that
     * two failures drawn with the same exception are equal ({@code Failure} compares its cause by reference);
     * otherwise a {@code Success} of each value of a pass of {@code gen}.
     *
     * @param gen the generator of the values, possibly null ones
     * @param <A> the type of the values
     * @return a generator of tries
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Try<A>> tryOf(Gen<A> gen) {
        return tryOf(gen, elements(FAILURES));
    }

    /**
     * A random try: for one pass in four, a {@code Failure} of each exception of a pass of {@code failures};
     * otherwise a {@code Success} of each value of a pass of {@code gen}.
     *
     * @param gen      the generator of the values, possibly null ones
     * @param failures the generator of the exceptions; a fatal one is rethrown by {@code Try.failure}
     * @param <A>      the type of the values
     * @return a generator of tries
     * @throws NullPointerException if an argument is null
     */
    public static <A> Gen<Try<A>> tryOf(Gen<A> gen, Gen<? extends Exception> failures) {
        Objects.requireNonNull(gen, "gen is null");
        Objects.requireNonNull(failures, "failures is null");
        return new Gen<>((sampling, size, sink) -> sampling.draw().nextInt(4) == 0
                ? failures.run(sampling, size, e -> sink.accept(Try.failure(e)))
                : gen.run(sampling, size, a -> sink.accept(Try.success(a))));
    }

    /**
     * A random validation, {@code Valid} and {@code Invalid} as likely: for each pass, a {@code Valid} of each value
     * of a pass of {@code values}, or an {@code Invalid} of one to three errors, each the first value of one pass
     * of {@code errors}. A single error is built by {@code invalid} or {@code invalidAll}, as likely.
     *
     * @param errors the generator of the errors
     * @param values the generator of the values
     * @param <E>    the type of the errors
     * @param <A>    the type of the values
     * @return a generator of validations
     * @throws NullPointerException if an argument is null
     */
    public static <E, A> Gen<Validation<E, A>> validation(Gen<E> errors, Gen<A> values) {
        Objects.requireNonNull(errors, "errors is null");
        Objects.requireNonNull(values, "values is null");
        return new Gen<>((sampling, size, sink) -> {
            if (sampling.draw().nextBoolean()) {
                return values.run(sampling, size, a -> sink.accept(Validation.valid(a)));
            }
            NonEmptyVector<E> drawn = NonEmptyVector.single(errors.draw(sampling, size));
            for (int extra = sampling.draw().nextInt(3); extra > 0; extra--) {
                drawn = drawn.append(errors.draw(sampling, size));
            }
            return sink.accept(drawn.size() == 1 && sampling.draw().nextBoolean()
                    ? Validation.invalid(drawn.head())
                    : Validation.invalidAll(drawn));
        });
    }

    /**
     * A lazy value of each value of {@code gen}, already evaluated or not, as likely.
     *
     * @param gen the generator of the values, possibly null ones
     * @param <A> the type of the values
     * @return a generator of lazy values
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Lazy<A>> lazy(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return new Gen<>((sampling, size, sink) -> gen.run(sampling, size, a -> {
            final Lazy<A> lazy = Lazy.of(() -> a);
            if (sampling.draw().nextBoolean()) {
                lazy.get();
            }
            return sink.accept(lazy);
        }));
    }

    /**
     * A generator of pairs, as {@link #zip(Gen, Gen)}.
     *
     * @param g1   the generator of the first component
     * @param g2   the generator of the second component
     * @param <T1> the type of the first component
     * @param <T2> the type of the second component
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> Gen<Tuple2<T1, T2>> tuple2(Gen<T1> g1, Gen<T2> g2) {
        return zip(g1, g2);
    }

    /**
     * A generator of 3-tuples, as {@link #zip(Gen, Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> Gen<Tuple3<T1, T2, T3>> tuple3(Gen<T1> g1, Gen<T2> g2, Gen<T3> g3) {
        return zip(g1, g2, g3);
    }

    /**
     * A generator of 4-tuples, as {@link #zip(Gen, Gen, Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> Gen<Tuple4<T1, T2, T3, T4>> tuple4(Gen<T1> g1, Gen<T2> g2, Gen<T3> g3, Gen<T4> g4) {
        return zip(g1, g2, g3, g4);
    }

    /**
     * A generator of 5-tuples, as {@link #zip(Gen, Gen, Gen, Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> Gen<Tuple5<T1, T2, T3, T4, T5>> tuple5(Gen<T1> g1, Gen<T2> g2, Gen<T3> g3, Gen<T4> g4, Gen<T5> g5) {
        return zip(g1, g2, g3, g4, g5);
    }

    /**
     * A generator of 6-tuples, as {@link #zip(Gen, Gen, Gen, Gen, Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param g6   the generator of component 6
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @param <T6> the type of component 6
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> Gen<Tuple6<T1, T2, T3, T4, T5, T6>> tuple6(Gen<T1> g1, Gen<T2> g2, Gen<T3> g3, Gen<T4> g4, Gen<T5> g5, Gen<T6> g6) {
        return zip(g1, g2, g3, g4, g5, g6);
    }

    /**
     * A generator of 7-tuples, as {@link #zip(Gen, Gen, Gen, Gen, Gen, Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param g6   the generator of component 6
     * @param g7   the generator of component 7
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @param <T6> the type of component 6
     * @param <T7> the type of component 7
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Gen<Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple7(Gen<T1> g1, Gen<T2> g2, Gen<T3> g3, Gen<T4> g4, Gen<T5> g5, Gen<T6> g6, Gen<T7> g7) {
        return zip(g1, g2, g3, g4, g5, g6, g7);
    }

    /**
     * A generator of 8-tuples, as {@link #zip(Gen, Gen, Gen, Gen, Gen, Gen, Gen, Gen)}.
     *
     * @param g1   the generator of component 1
     * @param g2   the generator of component 2
     * @param g3   the generator of component 3
     * @param g4   the generator of component 4
     * @param g5   the generator of component 5
     * @param g6   the generator of component 6
     * @param g7   the generator of component 7
     * @param g8   the generator of component 8
     * @param <T1> the type of component 1
     * @param <T2> the type of component 2
     * @param <T3> the type of component 3
     * @param <T4> the type of component 4
     * @param <T5> the type of component 5
     * @param <T6> the type of component 6
     * @param <T7> the type of component 7
     * @param <T8> the type of component 8
     * @return a new generator
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Gen<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple8(Gen<T1> g1, Gen<T2> g2, Gen<T3> g3, Gen<T4> g4, Gen<T5> g5, Gen<T6> g6, Gen<T7> g7, Gen<T8> g8) {
        return zip(g1, g2, g3, g4, g5, g6, g7, g8);
    }

    // -- the Zazr collections
    //
    // Each collection generator gives one collection per pass. Its length is drawn between 0 and the current size,
    // favouring the edges as intValue(0, size): half of the lengths are 0, 1, the size or the size minus one, so the
    // laws see collections as long as the size allows, not only short ones. Each element is the first value of one
    // pass of the element generator, at the current size; a filtered element generator is run again until it gives
    // one. The collection is then built along one of several paths, each reaching another internal representation.

    /**
     * A random vector of up to the current size elements of {@code gen}, favouring the lengths 0, 1, the size and
     * the size minus one. Each element is the first value of one pass of {@code gen}. The vector is built by
     * {@code ofAll}, a builder, appends, prepends, as what is left after dropping a prefix (the trie keeps an
     * offset), or as a slice of a longer vector, each as likely.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of vectors
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Vector<A>> vector(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.vector(gen, Shapes.Length.UP_TO_SIZE);
    }

    /**
     * A random vector of exactly {@code n} elements of {@code gen}, built as {@link #vector(Gen)}; the elements drawn
     * to be dropped again are drawn only when the current size is not 0.
     *
     * @param n   the number of elements
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of vectors of {@code n} elements
     * @throws NullPointerException     if {@code gen} is null
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public static <A> Gen<Vector<A>> vectorN(int n, Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        requireNonNegative(n, "n");
        return Shapes.vector(gen, Shapes.Length.exactly(n));
    }

    /**
     * A random non-empty vector of one to {@code max(1, size)} elements of {@code gen}: a head, then a tail as
     * {@link #vector(Gen)} of up to the size minus one elements. Each element is the first value of one pass of
     * {@code gen}. The head and the tail are joined by {@code appendAll}, {@code fromVector} or {@code prepend},
     * each as likely.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of non-empty vectors
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<NonEmptyVector<A>> nonEmptyVector(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.nonEmptyVector(gen);
    }

    /**
     * A random list of up to the current size elements of {@code gen}, favouring the lengths 0, 1, the size and the
     * size minus one. Each element is the first value of one pass of {@code gen}. The list is built by
     * {@code ofAll}, by prepends, or as the rest of a longer list after {@code drop}, each as likely.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of lists
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<List<A>> list(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.list(gen);
    }

    /**
     * A random queue of up to the current size elements of {@code gen}, favouring the lengths 0, 1, the size and the
     * size minus one. Each element is the first value of one pass of {@code gen}. The elements sit in the front
     * list only ({@code ofAll}), one in front and the rest in the rear list (enqueues), in both lists
     * ({@code ofAll} then {@code enqueueAll}), or in a rear list reversed into the front by {@code drop}, each as
     * likely.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of queues
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Queue<A>> queue(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.queue(gen);
    }

    /**
     * A random finite stream of up to the current size elements of {@code gen}, favouring the lengths 0, 1, the size
     * and the size minus one. Each element is the first value of one pass of {@code gen}, drawn when the stream is
     * generated. The stream is built by {@code ofAll}, as a chain of lazy tails, as the same chain with a prefix
     * already evaluated, as an eager prefix with a lazy suffix appended, or as the rest of a longer chain after
     * {@code drop}, each as likely.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of streams
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<Stream<A>> stream(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.stream(gen);
    }

    /**
     * A random hash set of up to the current size drawn elements of {@code gen} (fewer when draws repeat), the number
     * of draws favouring 0, 1, the size and the size minus one. Each element is the first value of one pass of
     * {@code gen}. The set is built by {@code ofAll}, by one {@code add} at a time, after extra elements were added
     * and removed again, or after elements were removed and added back, each as likely.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of hash sets
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<HashSet<A>> hashSet(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.set(gen, Shapes.hashSetOps());
    }

    /**
     * A random linked hash set, built as {@link #hashSet(Gen)}.
     *
     * @param gen the generator of the elements, possibly null ones
     * @param <A> the type of the elements
     * @return a random generator of linked hash sets
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A> Gen<LinkedHashSet<A>> linkedHashSet(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.set(gen, Shapes.linkedHashSetOps());
    }

    /**
     * A random tree set in the natural order of the elements, built as {@link #hashSet(Gen)}.
     *
     * @param gen the generator of the elements; a null one makes the tree set throw
     * @param <A> the type of the elements
     * @return a random generator of tree sets
     * @throws NullPointerException if {@code gen} is null
     */
    public static <A extends Comparable<? super A>> Gen<TreeSet<A>> treeSet(Gen<A> gen) {
        Objects.requireNonNull(gen, "gen is null");
        return Shapes.set(gen, Shapes.treeSetOps());
    }

    /**
     * A random hash map of up to the current size drawn entries (fewer when keys repeat), the number of draws
     * favouring 0, 1, the size and the size minus one. Each entry is a key then a value, each the first value of one
     * pass of its generator. The map is built by {@code ofAll} of a JDK map, by one {@code put} at a time, after
     * extra keys were put and removed again, or with every key first put with another value and then overwritten,
     * each as likely.
     *
     * @param keys   the generator of the keys, possibly null ones
     * @param values the generator of the values, possibly null ones
     * @param <K>    the type of the keys
     * @param <V>    the type of the values
     * @return a random generator of hash maps
     * @throws NullPointerException if an argument is null
     */
    public static <K, V> Gen<HashMap<K, V>> hashMap(Gen<K> keys, Gen<V> values) {
        Objects.requireNonNull(keys, "keys is null");
        Objects.requireNonNull(values, "values is null");
        return Shapes.map(keys, values, Shapes.hashMapOps());
    }

    /**
     * A random linked hash map, built as {@link #hashMap(Gen, Gen)}.
     *
     * @param keys   the generator of the keys, possibly null ones
     * @param values the generator of the values, possibly null ones
     * @param <K>    the type of the keys
     * @param <V>    the type of the values
     * @return a random generator of linked hash maps
     * @throws NullPointerException if an argument is null
     */
    public static <K, V> Gen<LinkedHashMap<K, V>> linkedHashMap(Gen<K> keys, Gen<V> values) {
        Objects.requireNonNull(keys, "keys is null");
        Objects.requireNonNull(values, "values is null");
        return Shapes.map(keys, values, Shapes.linkedHashMapOps());
    }

    /**
     * A random tree map in the natural order of the keys, built as {@link #hashMap(Gen, Gen)}.
     *
     * @param keys   the generator of the keys; a null one makes the tree map throw
     * @param values the generator of the values, possibly null ones
     * @param <K>    the type of the keys
     * @param <V>    the type of the values
     * @return a random generator of tree maps
     * @throws NullPointerException if an argument is null
     */
    public static <K extends Comparable<? super K>, V> Gen<TreeMap<K, V>> treeMap(Gen<K> keys, Gen<V> values) {
        Objects.requireNonNull(keys, "keys is null");
        Objects.requireNonNull(values, "values is null");
        return Shapes.map(keys, values, Shapes.treeMapOps());
    }

    // -- running outside a check

    /**
     * The values of one pass of this generator, as {@link Check#checkAll} would check them with {@code config}:
     * every value of a finite generator, one value of a random one.
     *
     * @param config the size and the seed
     * @return the values, in order
     * @throws NullPointerException  if {@code config} is null, or a value is null: a {@link List} holds no null
     * @throws IllegalStateException if a {@link #filter} exceeds the discard budget
     */
    public List<A> runCollect(CheckConfig config) {
        Objects.requireNonNull(config, "config is null");
        final ArrayList<A> values = new ArrayList<>();
        pass.run(new Sampling(config.seed(), config.maxDiscards()), config.size(), value -> {
            values.add(value);
            return true;
        });
        return List.ofAll(values);
    }

    /**
     * The values of one pass of this generator with {@link CheckConfig#defaults()}.
     *
     * @return the values, in order
     * @throws NullPointerException  if a value is null: a {@link List} holds no null
     * @throws IllegalStateException if a {@link #filter} exceeds the discard budget
     */
    public List<A> runCollect() {
        return runCollect(CheckConfig.defaults());
    }

    /**
     * The first {@code n} values of this generator, as {@link Check#check} would check them with {@code config}
     * set to {@code n} samples: pass after pass, the size growing from 0 to the configured size.
     *
     * @param n      the number of values
     * @param config the size and the seed
     * @return the values, in order
     * @throws NullPointerException     if {@code config} is null, or a value is null: a {@link List} holds no null
     * @throws IllegalArgumentException if {@code n} is negative
     * @throws IllegalStateException    if the generator produces no value in more passes in a row than the discard
     *                                  budget, or a {@link #filter} exceeds it
     */
    public List<A> runCollectN(int n, CheckConfig config) {
        Objects.requireNonNull(config, "config is null");
        requireNonNegative(n, "n");
        final ArrayList<A> values = new ArrayList<>(n);
        Runner.passes(config.withSamples(n), this, value -> {
            values.add(value);
            return values.size() < n;
        });
        return List.ofAll(values);
    }

    /**
     * The first {@code n} values of this generator with {@link CheckConfig#defaults()}, as
     * {@link #runCollectN(int, CheckConfig)}.
     *
     * @param n the number of values
     * @return the values, in order
     * @throws NullPointerException     if a value is null: a {@link List} holds no null
     * @throws IllegalArgumentException if {@code n} is negative
     * @throws IllegalStateException    if the generator produces no value in more passes in a row than the discard
     *                                  budget, or a {@link #filter} exceeds it
     */
    public List<A> runCollectN(int n) {
        return runCollectN(n, CheckConfig.defaults());
    }

    static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " is negative: " + value);
        }
    }

    @Override
    public String toString() {
        return "Gen";
    }
}
