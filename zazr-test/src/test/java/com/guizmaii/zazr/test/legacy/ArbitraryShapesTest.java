package com.guizmaii.zazr.test.legacy;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.Tuple;
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
import java.util.Random;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The arbitraries of the Zazr types reach every structurally interesting shape. The internal representation of a
 * collection is read by reflection, since no public method exposes it.
 */
class ArbitraryShapesTest {

    private static final int SIZE = 100;
    private static final int SAMPLES = 500;

    private static <T> ArrayList<T> samples(Arbitrary<T> arbitrary) {
        final Gen<T> gen = arbitrary.apply(SIZE);
        final Random random = new Random(42);
        final ArrayList<T> samples = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            samples.add(gen.apply(random));
        }
        return samples;
    }

    private static <T> void assertSome(ArrayList<T> samples, Predicate<? super T> shape, String description) {
        assertThat(samples.stream().anyMatch(shape)).as(description).isTrue();
    }

    private static <T extends Traversable<?>> void assertEmptyAndLarge(Arbitrary<T> arbitrary) {
        final ArrayList<T> samples = samples(arbitrary);
        assertSome(samples, Traversable::isEmpty, "an empty value");
        assertSome(samples, t -> t.size() > 32, "a value longer than a 32-wide leaf");
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

    // -- control types

    @Test
    void optionCoversNoneAndSome() {
        final ArrayList<Option<Integer>> samples = samples(Arbitrary.option(Arbitrary.integer()));
        assertSome(samples, Option::isEmpty, "None");
        assertSome(samples, Option::isDefined, "Some");
    }

    @Test
    void eitherCoversLeftAndRight() {
        final ArrayList<Either<Integer, Integer>> samples = samples(Arbitrary.either(Arbitrary.integer(), Arbitrary.integer()));
        assertSome(samples, Either::isLeft, "Left");
        assertSome(samples, Either::isRight, "Right");
    }

    @Test
    void tryCoversSuccessAndEqualFailures() {
        final ArrayList<Try<Integer>> samples = samples(Arbitrary.tryOf(Arbitrary.integer()));
        assertSome(samples, Try::isSuccess, "Success");
        assertSome(samples, Try::isFailure, "Failure");
        final long distinctFailures = samples.stream().filter(Try::isFailure).distinct().count();
        assertThat(distinctFailures).isLessThan(samples.stream().filter(Try::isFailure).count());
    }

    @Test
    void validationCoversValidAndSeveralErrors() {
        final ArrayList<Validation<Integer, Integer>> samples = samples(Arbitrary.validation(Arbitrary.integer(), Arbitrary.integer()));
        assertSome(samples, Validation::isValid, "Valid");
        assertSome(samples, v -> v instanceof Validation.Invalid<Integer, Integer>(var errors) && errors.size() == 1, "one error");
        assertSome(samples, v -> v instanceof Validation.Invalid<Integer, Integer>(var errors) && errors.size() > 1, "several errors");
    }

    @Test
    void lazyCoversEvaluatedAndNot() {
        final ArrayList<Lazy<Integer>> samples = samples(Arbitrary.lazy(Arbitrary.integer()));
        assertSome(samples, Lazy::isEvaluated, "an evaluated Lazy");
        assertSome(samples, l -> !l.isEvaluated(), "an unevaluated Lazy");
    }

    @Test
    void tuplesTakeTheirComponentsInOrder() {
        final Gen<Integer> one = Gen.of(1);
        final Gen<Integer> two = Gen.of(2);
        final Random random = new Random(42);
        assertThat(Arbitrary.tuple2(one.arbitrary(), two.arbitrary()).apply(SIZE).apply(random)).isEqualTo(Tuple.of(1, 2));
        assertThat(Arbitrary.tuple8(one.arbitrary(), two.arbitrary(), one.arbitrary(), two.arbitrary(), one.arbitrary(),
                two.arbitrary(), one.arbitrary(), two.arbitrary()).apply(SIZE).apply(random))
                .isEqualTo(Tuple.of(1, 2, 1, 2, 1, 2, 1, 2));
    }

    // -- sequences

    @Test
    void vectorCoversTrieOffsetsAndLengths() {
        final ArrayList<Vector<Integer>> samples = samples(Arbitrary.vector(Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.vector(Arbitrary.integer()));
        assertSome(samples, v -> {
            final Object trie = field(v, Vector.class, "trie");
            return (int) field(trie, trie.getClass(), "offset") != 0;
        }, "a trie with an offset");
    }

    @Test
    void nonEmptyVectorCoversOneAndMany() {
        final ArrayList<NonEmptyVector<Integer>> samples = samples(Arbitrary.nonEmptyVector(Arbitrary.integer()));
        assertSome(samples, nev -> nev.size() == 1, "a single element");
        assertSome(samples, nev -> nev.size() > 32, "more than one leaf");
    }

    @Test
    void listCoversLengths() {
        assertEmptyAndLarge(Arbitrary.list(Arbitrary.integer()));
    }

    @Test
    void queueCoversBothInternalLists() {
        final ArrayList<Queue<Integer>> samples = samples(Arbitrary.queue(Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.queue(Arbitrary.integer()));
        assertSome(samples, q -> !((List<?>) field(q, Queue.class, "front")).isEmpty()
                && ((List<?>) field(q, Queue.class, "rear")).size() > 1, "front and rear lists both non-empty");
        assertSome(samples, q -> q.size() > 1 && ((List<?>) field(q, Queue.class, "rear")).isEmpty(), "a front list only");
    }

    @Test
    void streamCoversEvaluatedAndUnevaluatedTails() {
        final ArrayList<Stream<Integer>> samples = samples(Arbitrary.stream(Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.stream(Arbitrary.integer()));
        assertSome(samples, s -> !s.isEmpty() && !((Lazy<?>) field(s, Stream.Cons.class, "tail")).isEvaluated(),
                "an unevaluated tail");
        assertSome(samples, s -> !s.isEmpty() && ((Lazy<?>) field(s, Stream.Cons.class, "tail")).isEvaluated(),
                "an evaluated tail");
    }

    @Test
    void streamElementsAreDrawnWhenGenerated() {
        final Gen<Stream<Integer>> gen = Arbitrary.stream(Arbitrary.integer()).apply(SIZE);
        final Random first = new Random(7);
        final Random second = new Random(7);
        for (int i = 0; i < 50; i++) {
            final Stream<Integer> a = gen.apply(first);
            final Stream<Integer> b = gen.apply(second);
            assertThat(a).isEqualTo(b);
        }
    }

    @Test
    void nonpositiveSizesDrawNoElement() {
        final Arbitrary<Integer> failing = Gen.<Integer>fail().arbitrary();
        final Random random = new Random(5);
        for (int size : new int[] { 0, -1, Integer.MIN_VALUE }) {
            for (int i = 0; i < 20; i++) {
                assertThat(Arbitrary.vector(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.queue(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.list(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.stream(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.hashSet(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.linkedHashSet(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.treeSet(failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.hashMap(failing, failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.linkedHashMap(failing, failing).apply(size).apply(random)).isEmpty();
                assertThat(Arbitrary.treeMap(failing, failing).apply(size).apply(random)).isEmpty();
            }
        }
    }

    // -- sets and maps

    @Test
    void setsCoverLengths() {
        assertEmptyAndLarge(Arbitrary.hashSet(Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.linkedHashSet(Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.treeSet(Arbitrary.integer()));
    }

    @Test
    void mapsCoverLengths() {
        assertEmptyAndLarge(Arbitrary.hashMap(Arbitrary.integer(), Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.linkedHashMap(Arbitrary.integer(), Arbitrary.integer()));
        assertEmptyAndLarge(Arbitrary.treeMap(Arbitrary.integer(), Arbitrary.integer()));
    }

    @Test
    void setsAfterRemovalsHoldTheDrawnElementsOnly() {
        final Gen<Integer> small = Gen.choose(0, 9);
        final Gen<HashSet<Integer>> sets = Shapes.set(small, 5, Shapes.hashSetOps());
        final Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            assertThat(sets.apply(random).forAll(x -> x >= 0 && x <= 9)).isTrue();
        }
        final Gen<LinkedHashMap<Integer, Integer>> maps = Shapes.map(small, small, 5, Shapes.linkedHashMapOps());
        for (int i = 0; i < 200; i++) {
            assertThat(maps.apply(random).size()).isLessThanOrEqualTo(5);
        }
        assertThat(samples(Arbitrary.hashMap(Arbitrary.integer(), Arbitrary.integer()))).allMatch(m -> m instanceof HashMap<?, ?>);
        assertThat(samples(Arbitrary.linkedHashSet(Arbitrary.integer()))).allMatch(s -> s instanceof LinkedHashSet<?>);
        assertThat(samples(Arbitrary.treeSet(Arbitrary.integer()))).allMatch(s -> s instanceof TreeSet<?>);
        assertThat(samples(Arbitrary.treeMap(Arbitrary.integer(), Arbitrary.integer()))).allMatch(m -> m instanceof TreeMap<?, ?>);
    }
}
