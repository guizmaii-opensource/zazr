package com.guizmaii.zazr.test;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Traversable;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import com.guizmaii.zazr.control.Validation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The generators of the Zazr types: the control types, the tuples, the collections and the layouts the collections
 * are built along. The internal representation of a collection is read by reflection, since no public method
 * exposes it.
 */
class GenTypesTest {

    /// The lengths a 32-wide trie treats differently: empty, one element, around one full leaf and around a full
    /// second level.
    private static final int[] BOUNDARIES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025 };

    static CheckConfig config(long seed) {
        return new CheckConfig(200, 100, seed, 1000);
    }

    /// Random longs, which practically never repeat: the number of draws of a set or a map is then its size.
    private static Gen<Long> distinct() {
        return Gen.fromRandom(random -> random.nextLong());
    }

    /// An element generator that must not run.
    private static Gen<Integer> failing() {
        return Gen.fromRandom(random -> {
            throw new AssertionError("the element generator ran");
        });
    }

    /// 0, 1, 2 and so on, one per draw, counted from 0 for each generator built.
    private static Gen<Integer> counter() {
        final int[] next = { 0 };
        return Gen.fromRandom(random -> next[0]++);
    }

    private static ArrayList<Integer> elements(int n) {
        final ArrayList<Integer> elements = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            elements.add(i);
        }
        return elements;
    }

    private static Object field(Object target, Class<?> owner, String name) {
        try {
            final Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static <T> void assertSome(List<T> samples, Predicate<? super T> shape, String description) {
        assertThat(samples.exists(shape::test)).as(description).isTrue();
    }

    /// The length generator of each of the collections whose length is drawn between 0 and the size.
    private static java.util.Map<String, Gen<Integer>> lengths() {
        final java.util.Map<String, Gen<Integer>> lengths = new java.util.LinkedHashMap<>();
        lengths.put("vector", Gen.vector(distinct()).map(Traversable::size));
        lengths.put("list", Gen.list(distinct()).map(Traversable::size));
        lengths.put("queue", Gen.queue(distinct()).map(Traversable::size));
        lengths.put("stream", Gen.stream(distinct()).map(Traversable::size));
        lengths.put("hashSet", Gen.hashSet(distinct()).map(Traversable::size));
        lengths.put("linkedHashSet", Gen.linkedHashSet(distinct()).map(Traversable::size));
        lengths.put("treeSet", Gen.treeSet(distinct()).map(Traversable::size));
        lengths.put("hashMap", Gen.hashMap(distinct(), distinct()).map(Traversable::size));
        lengths.put("linkedHashMap", Gen.linkedHashMap(distinct(), distinct()).map(Traversable::size));
        lengths.put("treeMap", Gen.treeMap(distinct(), distinct()).map(Traversable::size));
        return lengths;
    }

    /// Every generator of this class, over random elements.
    private static java.util.Map<String, Gen<?>> generators() {
        final Gen<Integer> ints = Gen.intValue();
        final java.util.Map<String, Gen<?>> gens = new java.util.LinkedHashMap<>();
        gens.put("option", Gen.option(ints));
        gens.put("some", Gen.some(ints));
        gens.put("either", Gen.either(ints, ints));
        gens.put("tryOf", Gen.tryOf(ints));
        gens.put("tryOf with failures", Gen.tryOf(ints, Gen.elements(new IllegalStateException("a"), new IllegalStateException("b"))));
        gens.put("validation", Gen.validation(ints, ints));
        gens.put("lazy", Gen.lazy(ints));
        gens.put("tuple2", Gen.tuple2(ints, ints));
        gens.put("tuple8", Gen.tuple8(ints, ints, ints, ints, ints, ints, ints, ints));
        gens.putAll(collections());
        return gens;
    }

    /// Every collection generator, over random elements.
    private static java.util.Map<String, Gen<? extends Iterable<?>>> collections() {
        final Gen<Integer> ints = Gen.intValue();
        final java.util.Map<String, Gen<? extends Iterable<?>>> gens = new java.util.LinkedHashMap<>();
        gens.put("vector", Gen.vector(ints));
        gens.put("vectorN", Gen.vectorN(40, ints));
        gens.put("nonEmptyVector", Gen.nonEmptyVector(ints));
        gens.put("list", Gen.list(ints));
        gens.put("queue", Gen.queue(ints));
        gens.put("stream", Gen.stream(ints));
        gens.put("hashSet", Gen.hashSet(ints));
        gens.put("linkedHashSet", Gen.linkedHashSet(ints));
        gens.put("treeSet", Gen.treeSet(ints));
        gens.put("hashMap", Gen.hashMap(ints, ints));
        gens.put("linkedHashMap", Gen.linkedHashMap(ints, ints));
        gens.put("treeMap", Gen.treeMap(ints, ints));
        return gens;
    }

    // -- control types

    @Test
    void optionGivesNoneForOnePassInFour() {
        final List<Option<Integer>> options = Gen.option(Gen.intValue()).runCollectN(2_000, config(1));
        assertThat(options.count(Option::isEmpty)).isBetween(400, 600);
        assertSome(options, Option::isDefined, "Some");
    }

    @Test
    void someAndNoneGiveOneShape() {
        assertThat(Gen.some(Gen.intValue(0, 9)).runCollectN(200, config(1))).allMatch(Option::isDefined)
                .allMatch(o -> o.get() >= 0 && o.get() <= 9);
        assertThat(Gen.<Integer>none().runCollect(config(1))).isEqualTo(List.of(Option.none()));
        assertThat(Gen.<Integer>none().runCollectN(5, config(1))).containsOnly(Option.none()).hasSize(5);
    }

    @Test
    void eitherGivesLeftAndRightAsLikely() {
        final List<Either<Integer, String>> eithers = Gen.either(Gen.intValue(), Gen.alphaNumericString()).runCollectN(2_000, config(1));
        assertThat(eithers.count(Either::isLeft)).isBetween(880, 1_120);
        assertSome(eithers, Either::isRight, "Right");
    }

    @Test
    void tryOfGivesAFailureForOnePassInFourWithSharedExceptions() {
        final List<Try<Integer>> tries = Gen.tryOf(Gen.intValue()).runCollectN(2_000, config(1));
        final List<Try<Integer>> failures = tries.filter(Try::isFailure);
        assertThat(failures.size()).isBetween(400, 600);
        assertSome(tries, Try::isSuccess, "Success");
        assertThat(failures.distinct().size()).isLessThan(failures.size()).isBetween(2, 3);
        // another generator fails with the same exception instances
        final List<Throwable> causes = failures.map(Try::getCause);
        assertThat(Gen.tryOf(Gen.constant(1)).runCollectN(200, config(2)).filter(Try::isFailure).map(Try::getCause)).isNotEmpty()
                .allMatch(cause -> causes.exists(c -> c == cause));
    }

    @Test
    void tryOfUsesTheGivenFailures() {
        final Exception failure = new IllegalStateException("given");
        final List<Try<Integer>> tries = Gen.tryOf(Gen.intValue(), Gen.constant(failure)).runCollectN(500, config(1));
        assertSome(tries, Try::isSuccess, "Success");
        assertThat(tries.filter(Try::isFailure)).isNotEmpty().allMatch(t -> t.getCause() == failure);
    }

    @Test
    void validationGivesValidAndOneToThreeErrors() {
        final List<Validation<String, Integer>> validations = Gen.validation(Gen.alphaNumericString(), Gen.intValue()).runCollectN(2_000, config(1));
        assertThat(validations.count(Validation::isValid)).isBetween(880, 1_120);
        final List<Integer> errorCounts = validations.filter(Validation::isInvalid)
                .map(v -> ((Validation.Invalid<String, Integer>) v).errors().size());
        assertThat(errorCounts).allMatch(n -> n >= 1 && n <= 3);
        for (int n = 1; n <= 3; n++) {
            final int errors = n;
            assertThat(errorCounts.count(c -> c == errors)).as("Invalid with %d errors", n).isGreaterThan(250);
        }
        // invalid and invalidAll build the same single-error value
        assertThat(Validation.<String, Integer>invalid("e")).isEqualTo(Validation.<String, Integer>invalidAll(NonEmptyVector.single("e")));
    }

    @Test
    void validationDrawsEachErrorAsTheFirstValueOfAPass() {
        final List<Validation<String, Integer>> validations = Gen.validation(Gen.fromIterable(java.util.List.of("x", "y")), Gen.intValue())
                .runCollectN(200, config(1));
        assertThat(validations.filter(Validation::isInvalid)).isNotEmpty()
                .allMatch(v -> ((Validation.Invalid<String, Integer>) v).errors().forAll("x"::equals));
    }

    @Test
    void lazyGivesEvaluatedAndUnevaluatedValues() {
        final List<Lazy<Integer>> lazies = Gen.lazy(Gen.intValue()).runCollectN(2_000, config(1));
        assertThat(lazies.count(Lazy::isEvaluated)).isBetween(880, 1_120);
        assertThat(Gen.lazy(Gen.size()).withSize(9).runCollectN(200, config(1))).allMatch(l -> l.get() == 9);
    }

    @Test
    void tuplesTakeTheirComponentsInOrder() {
        final Gen<Integer> g1 = Gen.constant(1);
        final Gen<Integer> g2 = Gen.constant(2);
        final Gen<Integer> g3 = Gen.constant(3);
        final Gen<Integer> g4 = Gen.constant(4);
        final Gen<Integer> g5 = Gen.constant(5);
        final Gen<Integer> g6 = Gen.constant(6);
        final Gen<Integer> g7 = Gen.constant(7);
        final Gen<Integer> g8 = Gen.constant(8);
        final CheckConfig config = config(1);
        assertThat(Gen.tuple2(g1, g2).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2)));
        assertThat(Gen.tuple3(g1, g2, g3).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2, 3)));
        assertThat(Gen.tuple4(g1, g2, g3, g4).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2, 3, 4)));
        assertThat(Gen.tuple5(g1, g2, g3, g4, g5).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5)));
        assertThat(Gen.tuple6(g1, g2, g3, g4, g5, g6).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5, 6)));
        assertThat(Gen.tuple7(g1, g2, g3, g4, g5, g6, g7).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
        assertThat(Gen.tuple8(g1, g2, g3, g4, g5, g6, g7, g8).runCollect(config)).isEqualTo(List.of(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    void tuplesOfFiniteGeneratorsGiveEveryCombinationAsZip() {
        final Gen<Integer> t = Gen.fromIterable(java.util.List.of(0, 1));
        final CheckConfig config = config(1);
        assertThat(Gen.tuple2(t, t).runCollect(config)).isEqualTo(Gen.zip(t, t).runCollect(config)).hasSize(4).doesNotHaveDuplicates();
        assertThat(Gen.tuple3(t, t, t).runCollect(config)).isEqualTo(Gen.zip(t, t, t).runCollect(config)).hasSize(8).doesNotHaveDuplicates();
        assertThat(Gen.tuple4(t, t, t, t).runCollect(config)).isEqualTo(Gen.zip(t, t, t, t).runCollect(config)).hasSize(16).doesNotHaveDuplicates();
        assertThat(Gen.tuple5(t, t, t, t, t).runCollect(config)).isEqualTo(Gen.zip(t, t, t, t, t).runCollect(config)).hasSize(32)
                .doesNotHaveDuplicates();
        assertThat(Gen.tuple6(t, t, t, t, t, t).runCollect(config)).isEqualTo(Gen.zip(t, t, t, t, t, t).runCollect(config)).hasSize(64)
                .doesNotHaveDuplicates();
        assertThat(Gen.tuple7(t, t, t, t, t, t, t).runCollect(config)).isEqualTo(Gen.zip(t, t, t, t, t, t, t).runCollect(config)).hasSize(128)
                .doesNotHaveDuplicates();
        assertThat(Gen.tuple8(t, t, t, t, t, t, t, t).runCollect(config)).isEqualTo(Gen.zip(t, t, t, t, t, t, t, t).runCollect(config))
                .hasSize(256).doesNotHaveDuplicates();
        assertThat(Gen.tuple2(t, Gen.fromIterable(java.util.List.of("a", "b"))).runCollect(config))
                .isEqualTo(List.of(Tuple.of(0, "a"), Tuple.of(0, "b"), Tuple.of(1, "a"), Tuple.of(1, "b")));
    }

    // -- wrappers over a finite generator

    private static final Gen<Integer> ONE_TWO_THREE = Gen.fromIterable(java.util.List.of(1, 2, 3));

    @Test
    void someGivesEveryValueOfAFiniteGenerator() {
        assertThat(Gen.some(ONE_TWO_THREE).runCollect(config(1))).isEqualTo(List.of(Option.some(1), Option.some(2), Option.some(3)));
    }

    /// Each pass is one of the shapes, and every shape occurs over the seeds.
    private static <A> void assertPassesAreOneOf(Gen<A> gen, java.util.List<List<A>> shapes, Function<List<A>, List<A>> normalize) {
        final java.util.Set<List<A>> seen = new java.util.HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            final List<A> pass = normalize.apply(gen.runCollect(config(seed)));
            assertThat(shapes).as("pass of seed %d", seed).contains(pass);
            seen.add(pass);
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(shapes);
    }

    @Test
    void optionPassesEveryValueOfAFiniteGeneratorThrough() {
        assertPassesAreOneOf(Gen.option(ONE_TWO_THREE),
                java.util.List.of(List.of(Option.none()), List.of(Option.some(1), Option.some(2), Option.some(3))), Function.identity());
    }

    @Test
    void eitherPassesEveryValueOfAFiniteGeneratorThrough() {
        assertPassesAreOneOf(Gen.either(ONE_TWO_THREE, Gen.fromIterable(java.util.List.of("a", "b"))),
                java.util.List.of(List.of(Either.left(1), Either.left(2), Either.left(3)), List.of(Either.right("a"), Either.right("b"))),
                Function.identity());
    }

    @Test
    void tryOfPassesEveryValueOfAFiniteGeneratorThrough() {
        final Exception first = new IllegalStateException("first");
        final Exception second = new IllegalStateException("second");
        assertPassesAreOneOf(Gen.tryOf(ONE_TWO_THREE, Gen.fromIterable(java.util.List.of(first, second))),
                java.util.List.of(List.of(Try.success(1), Try.success(2), Try.success(3)), List.of(Try.failure(first), Try.failure(second))),
                Function.identity());
        // with the shared failures, a failed pass gives one failure
        assertPassesAreOneOf(Gen.tryOf(ONE_TWO_THREE),
                java.util.List.of(List.of(Try.success(1), Try.success(2), Try.success(3)), List.of(Try.failure(first))),
                pass -> pass.map(t -> t.isFailure() ? Try.<Integer>failure(first) : t));
    }

    @Test
    void validationPassesEveryValueOfAFiniteGeneratorThrough() {
        assertPassesAreOneOf(Gen.validation(Gen.constant("e"), ONE_TWO_THREE).map(v -> v.isValid() ? v : Validation.<String, Integer>invalid("e")),
                java.util.List.of(List.of(Validation.valid(1), Validation.valid(2), Validation.valid(3)), List.of(Validation.invalid("e"))),
                Function.identity());
    }

    @Test
    void lazyPassesEveryValueOfAFiniteGeneratorThrough() {
        assertThat(Gen.lazy(ONE_TWO_THREE).runCollect(config(1)).map(Lazy::get)).isEqualTo(List.of(1, 2, 3));
    }

    // -- collection lengths

    @Test
    void collectionLengthsStayWithinTheSizeAndFavourTheEdges() {
        final List<Integer> edges = List.of(0, 1, 99, 100);
        lengths().forEach((name, gen) -> {
            final List<Integer> lengths = gen.withSize(100).runCollectN(1_000, config(1));
            assertThat(lengths).as(name).allMatch(n -> n >= 0 && n <= 100).containsAll(edges);
            // half of the lengths are an edge, and the other half are uniform over 101 lengths
            assertThat(lengths.count(edges::contains)).as(name).isBetween(420, 620);
        });
    }

    @Test
    void collectionLengthsAtSizesOneAndTwoReachZeroAndTheSize() {
        for (int size = 1; size <= 2; size++) {
            final int max = size;
            final int currentSize = size;
            lengths().forEach((name, gen) -> assertThat(gen.withSize(currentSize).runCollectN(200, config(1))).as(name + " at size " + max)
                    .allMatch(n -> n >= 0 && n <= max).contains(0, max));
        }
    }

    @Test
    void atSizeZeroEveryCollectionIsEmptyAndNoElementIsDrawn() {
        final java.util.List<Gen<? extends Traversable<?>>> gens = java.util.List.of(
                Gen.vector(failing()), Gen.vectorN(0, failing()), Gen.list(failing()), Gen.queue(failing()), Gen.stream(failing()),
                Gen.hashSet(failing()), Gen.linkedHashSet(failing()), Gen.treeSet(failing()),
                Gen.hashMap(failing(), failing()), Gen.linkedHashMap(failing(), failing()), Gen.treeMap(failing(), failing()));
        for (Gen<? extends Traversable<?>> gen : gens) {
            assertThat(gen.withSize(0).runCollectN(100, config(1))).hasSize(100).allMatch(Traversable::isEmpty);
        }
    }

    @Test
    void nonEmptyVectorHasOneToTheSizeElements() {
        final List<Integer> lengths = Gen.nonEmptyVector(distinct()).withSize(100).runCollectN(1_000, config(1)).map(NonEmptyVector::size);
        assertThat(lengths).allMatch(n -> n >= 1 && n <= 100).contains(1, 100);
        assertThat(Gen.nonEmptyVector(distinct()).withSize(2).runCollectN(200, config(1)).map(NonEmptyVector::size))
                .allMatch(n -> n >= 1 && n <= 2).contains(1, 2);
        assertThat(Gen.nonEmptyVector(Gen.constant(5)).withSize(1).runCollectN(50, config(1))).containsOnly(NonEmptyVector.single(5));
        assertThat(Gen.nonEmptyVector(Gen.constant(5)).withSize(0).runCollectN(50, config(1))).containsOnly(NonEmptyVector.single(5));
    }

    @Test
    void setsAndMapsHoldAtMostTheDrawnElements() {
        final Gen<Integer> small = Gen.intValue(0, 9);
        final java.util.List<Gen<? extends Traversable<Integer>>> sets = java.util.List.of(
                Gen.hashSet(small), Gen.linkedHashSet(small), Gen.treeSet(small));
        for (Gen<? extends Traversable<Integer>> gen : sets) {
            assertThat(gen.withSize(5).runCollectN(300, config(3)))
                    .allMatch(set -> set.size() <= 5 && set.forAll(x -> x >= 0 && x <= 9))
                    .anyMatch(set -> set.size() == 5);
        }
        final java.util.List<Gen<? extends Traversable<Tuple2<Integer, Integer>>>> maps = java.util.List.of(
                Gen.hashMap(small, small), Gen.linkedHashMap(small, small), Gen.treeMap(small, small));
        for (Gen<? extends Traversable<Tuple2<Integer, Integer>>> gen : maps) {
            assertThat(gen.withSize(5).runCollectN(300, config(3)))
                    .allMatch(map -> map.size() <= 5 && map.forAll(e -> e._1() >= 0 && e._1() <= 9 && e._2() >= 0 && e._2() <= 9))
                    .anyMatch(map -> map.size() == 5);
        }
    }

    @Test
    void everyCollectionHasItsRuntimeClass() {
        final java.util.Map<String, Class<?>> classes = java.util.Map.ofEntries(
                java.util.Map.entry("vector", Vector.class), java.util.Map.entry("vectorN", Vector.class),
                java.util.Map.entry("nonEmptyVector", NonEmptyVector.class), java.util.Map.entry("list", List.class),
                java.util.Map.entry("queue", Queue.class), java.util.Map.entry("stream", Stream.class),
                java.util.Map.entry("hashSet", HashSet.class), java.util.Map.entry("linkedHashSet", LinkedHashSet.class),
                java.util.Map.entry("treeSet", TreeSet.class), java.util.Map.entry("hashMap", HashMap.class),
                java.util.Map.entry("linkedHashMap", LinkedHashMap.class), java.util.Map.entry("treeMap", TreeMap.class));
        collections().forEach((name, gen) -> {
            final List<?> values = gen.runCollectN(100, config(1));
            assertThat(values).as(name).hasSize(100).allMatch(classes.get(name)::isInstance);
        });
    }

    // -- layouts

    @Test
    void theLayoutCountsAreTheOnesTheLayoutsCover() {
        assertThat(Shapes.VECTOR_LAYOUTS).isEqualTo(6);
        assertThat(Shapes.LIST_LAYOUTS).isEqualTo(3);
        assertThat(Shapes.QUEUE_LAYOUTS).isEqualTo(4);
        assertThat(Shapes.STREAM_LAYOUTS).isEqualTo(5);
        assertThat(Shapes.NON_EMPTY_VECTOR_LAYOUTS).isEqualTo(3);
        assertThat(Shapes.SET_LAYOUTS).isEqualTo(4);
        assertThat(Shapes.MAP_LAYOUTS).isEqualTo(4);
    }

    /// Elements for the prefixes and suffixes dropped again: none of them is one of the kept elements.
    private static final Gen<Integer> DROPPED = Gen.intValue(-1_000, -1);

    @Test
    void everySequenceLayoutHoldsTheElementsInOrder() {
        long seed = 0;
        for (int n : BOUNDARIES) {
            for (int layout = 0; layout < Shapes.VECTOR_LAYOUTS; layout++) {
                final Vector<Integer> vector = Shapes.vector(layout, elements(n), DROPPED, new Sampling(seed++, 1000), 100);
                assertThat(vector).as("vector layout %d of %d elements", layout, n).containsExactlyElementsOf(elements(n));
                assertThat(vector.size()).isEqualTo(n);
            }
            for (int layout = 0; layout < Shapes.LIST_LAYOUTS; layout++) {
                final List<Integer> list = Shapes.list(layout, elements(n), DROPPED, new Sampling(seed++, 1000), 100);
                assertThat(list).as("list layout %d of %d elements", layout, n).containsExactlyElementsOf(elements(n));
            }
            for (int layout = 0; layout < Shapes.QUEUE_LAYOUTS; layout++) {
                final Queue<Integer> queue = Shapes.queue(layout, elements(n), DROPPED, new Sampling(seed++, 1000), 100);
                assertThat(queue).as("queue layout %d of %d elements", layout, n).containsExactlyElementsOf(elements(n));
                assertThat(queue.size()).isEqualTo(n);
            }
            for (int layout = 0; layout < Shapes.STREAM_LAYOUTS; layout++) {
                final Stream<Integer> stream = Shapes.stream(layout, elements(n), DROPPED, new Sampling(seed++, 1000), 100);
                assertThat(stream).as("stream layout %d of %d elements", layout, n).containsExactlyElementsOf(elements(n));
            }
        }
    }

    @Test
    void everyNonEmptyVectorLayoutHoldsTheHeadThenTheTail() {
        long seed = 0;
        for (int n : BOUNDARIES) {
            final ArrayList<Integer> expected = new ArrayList<>();
            expected.add(-1);
            expected.addAll(elements(n));
            for (int tailLayout = 0; tailLayout < Shapes.VECTOR_LAYOUTS; tailLayout++) {
                final Vector<Integer> tail = Shapes.vector(tailLayout, elements(n), DROPPED, new Sampling(seed++, 1000), 100);
                for (int layout = 0; layout < Shapes.NON_EMPTY_VECTOR_LAYOUTS; layout++) {
                    assertThat(Shapes.nonEmptyVector(layout, -1, tail))
                            .as("non-empty vector layout %d over a tail of layout %d of %d elements", layout, tailLayout, n)
                            .containsExactlyElementsOf(expected);
                }
            }
        }
    }

    @Test
    void everySetLayoutHoldsTheElements() {
        // the extra elements overlap the kept ones, so the layout that removes them again must keep those
        final Gen<Integer> extra = Gen.intValue(0, 2_000);
        final java.util.List<Shapes.SetOps<Integer, ? extends Traversable<Integer>>> kinds = java.util.List.of(
                Shapes.hashSetOps(), Shapes.linkedHashSetOps(), Shapes.treeSetOps());
        long seed = 0;
        for (int n : BOUNDARIES) {
            // every element twice
            final ArrayList<Integer> drawn = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                drawn.add(i / 2);
            }
            final java.util.Set<Integer> expected = new java.util.HashSet<>(drawn);
            for (Shapes.SetOps<Integer, ? extends Traversable<Integer>> ops : kinds) {
                for (int layout = 0; layout < Shapes.SET_LAYOUTS; layout++) {
                    final Traversable<Integer> set = set(layout, new ArrayList<>(drawn), extra, ops, new Sampling(seed++, 1000));
                    assertThat(set).as("%s layout %d of %d draws", set.getClass().getSimpleName(), layout, n)
                            .containsExactlyInAnyOrderElementsOf(expected);
                }
            }
        }
    }

    private static <S extends Traversable<Integer>> S set(int layout, ArrayList<Integer> xs, Gen<Integer> extra,
                                                          Shapes.SetOps<Integer, S> ops, Sampling sampling) {
        return Shapes.set(layout, xs, extra, ops, sampling, 100);
    }

    @Test
    void everyMapLayoutHoldsTheLastValueOfEachKey() {
        // the extra keys are not among the kept ones
        final Gen<Integer> extraKeys = Gen.intValue(1_000_000, 2_000_000);
        final java.util.List<Shapes.MapOps<Integer, Integer, ? extends Traversable<Tuple2<Integer, Integer>>>> kinds = java.util.List.of(
                Shapes.hashMapOps(), Shapes.linkedHashMapOps(), Shapes.treeMapOps());
        long seed = 0;
        for (int n : BOUNDARIES) {
            // every key twice, the second time with another value
            final ArrayList<Tuple2<Integer, Integer>> entries = new ArrayList<>();
            final java.util.Map<Integer, Integer> expected = new java.util.HashMap<>();
            for (int i = 0; i < n; i++) {
                entries.add(Tuple.of(i / 2, i));
                expected.put(i / 2, i);
            }
            for (Shapes.MapOps<Integer, Integer, ? extends Traversable<Tuple2<Integer, Integer>>> ops : kinds) {
                for (int layout = 0; layout < Shapes.MAP_LAYOUTS; layout++) {
                    final Traversable<Tuple2<Integer, Integer>> map = map(layout, new ArrayList<>(entries), extraKeys, ops, new Sampling(seed++, 1000));
                    assertThat(toJava(map)).as("%s layout %d of %d entries", map.getClass().getSimpleName(), layout, n).isEqualTo(expected);
                    assertThat(map.size()).isEqualTo(expected.size());
                }
            }
        }
    }

    private static <M extends Traversable<Tuple2<Integer, Integer>>> M map(int layout, ArrayList<Tuple2<Integer, Integer>> xs, Gen<Integer> keys,
                                                                           Shapes.MapOps<Integer, Integer, M> ops, Sampling sampling) {
        return Shapes.map(layout, xs, keys, DROPPED, ops, sampling, 100);
    }

    private static <K, V> java.util.Map<K, V> toJava(Traversable<Tuple2<K, V>> map) {
        final java.util.Map<K, V> javaMap = new java.util.HashMap<>();
        for (Tuple2<K, V> entry : map) {
            javaMap.put(entry._1(), entry._2());
        }
        return javaMap;
    }

    // -- the representations the generators reach

    private static <T> List<T> samples(Gen<T> gen) {
        return gen.withSize(100).runCollectN(500, config(42));
    }

    @Test
    void vectorReachesATrieWithAnOffset() {
        final List<Vector<Integer>> vectors = samples(Gen.vector(Gen.intValue()));
        assertSome(vectors, Vector::isEmpty, "an empty vector");
        assertSome(vectors, v -> v.size() > 32, "a vector longer than a leaf");
        assertSome(vectors, v -> {
            final Object trie = field(v, Vector.class, "trie");
            return (int) field(trie, trie.getClass(), "offset") != 0;
        }, "a trie with an offset");
    }

    @Test
    void queueReachesBothInternalLists() {
        final List<Queue<Integer>> queues = samples(Gen.queue(Gen.intValue()));
        assertSome(queues, q -> !((List<?>) field(q, Queue.class, "front")).isEmpty() && ((List<?>) field(q, Queue.class, "rear")).size() > 1,
                "front and rear lists both non-empty");
        assertSome(queues, q -> q.size() > 1 && ((List<?>) field(q, Queue.class, "rear")).isEmpty(), "a front list only");
    }

    @Test
    void streamReachesEvaluatedAndUnevaluatedTails() {
        final List<Stream<Integer>> streams = samples(Gen.stream(Gen.intValue()));
        assertSome(streams, s -> !s.isEmpty() && !((Lazy<?>) field(s, Stream.Cons.class, "tail")).isEvaluated(), "an unevaluated tail");
        assertSome(streams, s -> !s.isEmpty() && ((Lazy<?>) field(s, Stream.Cons.class, "tail")).isEvaluated(), "an evaluated tail");
    }

    @Test
    void nonEmptyVectorReachesOneAndMoreThanALeaf() {
        final List<NonEmptyVector<Integer>> vectors = samples(Gen.nonEmptyVector(Gen.intValue()));
        assertSome(vectors, v -> v.size() == 1, "a single element");
        assertSome(vectors, v -> v.size() > 32, "more than one leaf");
    }

    // -- vectorN

    @Test
    void vectorNHasExactlyNElementsInTheDrawnOrder() {
        for (int n : new int[] { 0, 1, 31, 32, 33, 1023, 1024, 1025 }) {
            for (int size : new int[] { 100, 0 }) {
                assertThat(Gen.vectorN(n, Gen.intValue()).withSize(size).runCollectN(5, config(n))).as("vectorN(%d) at size %d", n, size)
                        .hasSize(5).allMatch(v -> v.size() == n);
                // the elements are the first n draws, whatever the layout
                for (long seed = 0; seed < 12; seed++) {
                    final Vector<Integer> vector = Gen.vectorN(n, counter()).withSize(size).runCollect(config(seed)).head();
                    assertThat(vector).as("vectorN(%d) at size %d, seed %d", n, size, seed).isEqualTo(Vector.range(0, n));
                }
            }
        }
        assertThatThrownBy(() -> Gen.vectorN(-1, Gen.intValue())).isInstanceOf(IllegalArgumentException.class);
    }

    // -- nulls

    /// A generator of null elements.
    private static final Gen<Integer> NULLS = Gen.constant(null);

    @Test
    void aNullElementMakesEveryCollectionGeneratorThrow() {
        // the Zazr collections reject null elements, keys and values
        final CheckConfig config = config(1).withSize(10);
        final java.util.Map<String, Gen<?>> gens = new java.util.LinkedHashMap<>();
        gens.put("vector", Gen.vector(NULLS));
        gens.put("vectorN", Gen.vectorN(3, NULLS));
        gens.put("nonEmptyVector", Gen.nonEmptyVector(NULLS));
        gens.put("list", Gen.list(NULLS));
        gens.put("queue", Gen.queue(NULLS));
        gens.put("stream", Gen.stream(NULLS));
        gens.put("hashSet", Gen.hashSet(NULLS));
        gens.put("linkedHashSet", Gen.linkedHashSet(NULLS));
        gens.put("treeSet", Gen.treeSet(NULLS));
        gens.put("hashMap with null keys", Gen.hashMap(NULLS, Gen.intValue()));
        gens.put("hashMap with null values", Gen.hashMap(Gen.intValue(), NULLS));
        gens.put("linkedHashMap with null keys", Gen.linkedHashMap(NULLS, Gen.intValue()));
        gens.put("linkedHashMap with null values", Gen.linkedHashMap(Gen.intValue(), NULLS));
        gens.put("treeMap with null keys", Gen.treeMap(NULLS, Gen.intValue()));
        gens.put("treeMap with null values", Gen.treeMap(Gen.intValue(), NULLS));
        gens.forEach((name, gen) -> {
            assertThatThrownBy(() -> gen.runCollectN(50, config)).as(name).isInstanceOf(NullPointerException.class);
            final CheckResult result = Check.check(config.withSamples(50), gen, value -> true);
            assertThat(result.isErroneous()).as(name).isTrue();
            assertThat(result.error().get()).as(name).isInstanceOf(NullPointerException.class);
        });
        // at size 0 no element is drawn, so no null reaches a collection
        assertThat(Gen.vector(NULLS).withSize(0).runCollectN(20, config)).allMatch(Vector::isEmpty);
        assertThat(Gen.hashMap(NULLS, NULLS).withSize(0).runCollectN(20, config)).allMatch(HashMap::isEmpty);
    }

    @Test
    void aNullValueMakesTheControlTypesThrowButReachesLazyAndTuples() {
        final CheckConfig config = config(1);
        final Gen<Integer> ints = Gen.intValue();
        // Some, Left, Right, Success, Valid and the errors of Invalid reject null
        final java.util.List<ThrowingCallable> calls = java.util.List.of(
                () -> Gen.option(NULLS).runCollectN(50, config), () -> Gen.some(NULLS).runCollect(config),
                () -> Gen.either(NULLS, ints).runCollectN(50, config), () -> Gen.either(ints, NULLS).runCollectN(50, config),
                () -> Gen.tryOf(NULLS).runCollectN(50, config), () -> Gen.tryOf(ints, Gen.<Exception>constant(null)).runCollectN(50, config),
                () -> Gen.validation(NULLS, ints).runCollectN(50, config), () -> Gen.validation(ints, NULLS).runCollectN(50, config));
        for (int i = 0; i < calls.size(); i++) {
            assertThatThrownBy(calls.get(i)).as("call %d", i).isInstanceOf(NullPointerException.class);
        }
        assertThat(Gen.lazy(NULLS).runCollectN(50, config)).allMatch(l -> l.get() == null);
        assertThat(Gen.tuple2(NULLS, NULLS).runCollect(config)).isEqualTo(List.of(Tuple.of(null, null)));
        assertThat(Gen.tuple8(NULLS, NULLS, NULLS, NULLS, NULLS, NULLS, NULLS, NULLS).runCollect(config))
                .isEqualTo(List.of(Tuple.of(null, null, null, null, null, null, null, null)));
        assertThat(Gen.option(Gen.lazy(NULLS)).runCollectN(50, config)).anyMatch(o -> o.isDefined() && o.get().get() == null);
    }

    @Test
    void everyGeneratorRejectsANullGenerator() {
        final Gen<Integer> g = Gen.intValue();
        final Gen<Integer> n = null;
        final java.util.List<ThrowingCallable> calls = java.util.List.of(
                () -> Gen.option(n), () -> Gen.some(n),
                () -> Gen.either(n, g), () -> Gen.either(g, n),
                () -> Gen.tryOf(n), () -> Gen.tryOf(n, Gen.constant(new IllegalStateException())), () -> Gen.tryOf(g, null),
                () -> Gen.validation(n, g), () -> Gen.validation(g, n),
                () -> Gen.lazy(n),
                () -> Gen.tuple2(g, n), () -> Gen.tuple3(g, g, n), () -> Gen.tuple4(g, g, g, n), () -> Gen.tuple5(g, g, g, g, n),
                () -> Gen.tuple6(g, g, g, g, g, n), () -> Gen.tuple7(g, g, g, g, g, g, n), () -> Gen.tuple8(g, g, g, g, g, g, g, n),
                () -> Gen.tuple8(n, g, g, g, g, g, g, g),
                () -> Gen.vector(n), () -> Gen.vectorN(1, n), () -> Gen.nonEmptyVector(n), () -> Gen.list(n), () -> Gen.queue(n),
                () -> Gen.stream(n), () -> Gen.hashSet(n), () -> Gen.linkedHashSet(n), () -> Gen.treeSet(n),
                () -> Gen.hashMap(n, g), () -> Gen.hashMap(g, n), () -> Gen.linkedHashMap(n, g), () -> Gen.linkedHashMap(g, n),
                () -> Gen.treeMap(n, g), () -> Gen.treeMap(g, n));
        for (int i = 0; i < calls.size(); i++) {
            assertThatThrownBy(calls.get(i)).as("call %d", i).isInstanceOf(NullPointerException.class);
        }
    }

    // -- filtered elements

    @Test
    void aFilteredElementGeneratorStillFillsTheCollections() {
        final Gen<Integer> evens = Gen.intValue(0, 100).filter(i -> i % 2 == 0);
        final List<Vector<Integer>> vectors = Gen.vector(evens).withSize(100).runCollectN(300, config(3));
        assertThat(vectors).allMatch(v -> v.forAll(i -> i % 2 == 0)).anyMatch(v -> v.size() == 100);
        final Gen<Long> evenLongs = distinct().filter(l -> l % 2 == 0);
        final List<HashSet<Long>> sets = Gen.hashSet(evenLongs).withSize(100).runCollectN(300, config(3));
        assertThat(sets).allMatch(s -> s.forAll(l -> l % 2 == 0)).anyMatch(s -> s.size() == 100);
        final List<TreeMap<Long, Long>> maps = Gen.treeMap(evenLongs, evenLongs).withSize(100).runCollectN(300, config(3));
        assertThat(maps).allMatch(m -> m.forAll(e -> e._1() % 2 == 0 && e._2() % 2 == 0)).anyMatch(m -> m.size() == 100);
    }

    // -- replay

    @Test
    void aSeedReplaysEveryGenerator() {
        generators().forEach((name, gen) -> assertThat(gen.runCollectN(200, config(7))).as(name).isEqualTo(gen.runCollectN(200, config(7))));
    }

    @Test
    void anotherSeedGivesOtherCollections() {
        collections().forEach((name, gen) -> assertThat(gen.runCollectN(50, config(7))).as(name).isNotEqualTo(gen.runCollectN(50, config(8))));
    }

    @Test
    void streamElementsAreDrawnWhenTheStreamIsGenerated() {
        final AtomicBoolean done = new AtomicBoolean();
        final Gen<Integer> elements = Gen.fromRandom(random -> {
            if (done.get()) {
                throw new AssertionError("an element was drawn after the run");
            }
            return random.nextInt();
        });
        final List<Stream<Integer>> first = Gen.stream(elements).withSize(100).runCollectN(300, config(7));
        done.set(true);
        final List<List<Integer>> evaluated = first.map(List::ofAll);
        done.set(false);
        final List<Stream<Integer>> second = Gen.stream(elements).withSize(100).runCollectN(300, config(7));
        done.set(true);
        assertThat(second.map(List::ofAll)).isEqualTo(evaluated);
        assertThat(evaluated).anyMatch(l -> l.size() > 32);
    }

    // -- the element size

    private static boolean onlySevens(Iterable<?> elements) {
        for (Object element : elements) {
            if (!Integer.valueOf(7).equals(element)) {
                return false;
            }
        }
        return true;
    }

    @Test
    void elementsAreDrawnAtTheCurrentSize() {
        final Gen<Integer> size = Gen.size();
        final java.util.List<Gen<? extends Iterable<?>>> gens = java.util.List.of(
                Gen.vector(size), Gen.vectorN(40, size), Gen.nonEmptyVector(size), Gen.list(size), Gen.queue(size), Gen.stream(size),
                Gen.hashSet(size), Gen.linkedHashSet(size), Gen.treeSet(size));
        for (Gen<? extends Iterable<?>> gen : gens) {
            assertThat(gen.withSize(7).runCollectN(100, config(1))).anyMatch(c -> c.iterator().hasNext()).allMatch(c -> onlySevens(c));
        }
        final java.util.List<Gen<? extends Traversable<Tuple2<Integer, Integer>>>> maps = java.util.List.of(
                Gen.hashMap(size, size), Gen.linkedHashMap(size, size), Gen.treeMap(size, size));
        for (Gen<? extends Traversable<Tuple2<Integer, Integer>>> gen : maps) {
            assertThat(gen.withSize(7).runCollectN(100, config(1))).anyMatch(m -> !m.isEmpty()).allMatch(m -> m.forAll(e -> e.equals(Tuple.of(7, 7))));
        }
        assertThat(Gen.option(size).withSize(7).runCollectN(100, config(1))).contains(Option.some(7)).allMatch(o -> o.forAll(x -> x == 7));
        assertThat(Gen.validation(size, size).withSize(7).runCollectN(100, config(1)))
                .allMatch(v -> v.isValid() ? v.get() == 7 : ((Validation.Invalid<Integer, Integer>) v).errors().forAll(e -> e == 7));
    }
}
