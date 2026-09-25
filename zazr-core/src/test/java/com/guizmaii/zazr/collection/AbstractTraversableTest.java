package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.DoubleAssert;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.LongAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The cases every collection shares: the members {@link Traversable} declares (one pass, order-agnostic), the
 * factories every type has ({@code empty}, {@code of}, {@code ofAll}, {@code tabulate}, {@code fill}, the
 * collector), equality and {@code toString}. Everything else a type does is tested in its own test class.
 */
@ExtendWith(AbstractTraversableTest.TestTemplateProvider.class)
public abstract class AbstractTraversableTest {

    // A pass-through provider for @TestTemplate methods: one invocation, no parameterization.
    public static class TestTemplateProvider implements TestTemplateInvocationContextProvider {

        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public java.util.stream.Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
            return java.util.stream.Stream.of(new TestTemplateInvocationContext() {
            });
        }
    }

    /** The names of the members {@link Traversable} declares (design 3.7), instance methods only. */
    static final java.util.Set<String> TRAVERSABLE_MEMBERS = java.util.Set.of(
            "iterator", "size", "isEmpty", "nonEmpty", "contains", "containsAll", "exists", "forAll", "count", "find",
            "foldLeft", "mkString", "forEach", "toVector", "toList", "toSet", "stream", "toArray", "asJava", "spliterator");

    /**
     * The positional members the ordered sets and maps declare ({@code SortedSet}, {@code SortedMap},
     * {@code LinkedHashSet}, {@code LinkedHashMap}) and the hash-ordered ones must not (design 3.7).
     */
    static final java.util.Set<String> ORDERED_POSITIONAL_MEMBERS = java.util.Set.of(
            "head", "headOption", "last", "lastOption", "init", "initOption", "tail", "tailOption", "take", "takeRight",
            "takeWhile", "takeUntil", "drop", "dropRight", "dropWhile", "dropUntil", "zipWithIndex", "sliding", "grouped",
            "slideBy");

    protected <T> IterableAssert<T> assertThat(Iterable<T> actual) {
        return new IterableAssert<T>(actual) {
        };
    }

    protected <T> ObjectAssert<T> assertThat(T actual) {
        return new ObjectAssert<T>(actual) {
        };
    }

    protected <T> ObjectArrayAssert<T> assertThat(T[] actual) {
        return new ObjectArrayAssert<T>(actual) {
        };
    }

    protected BooleanAssert assertThat(Boolean actual) {
        return new BooleanAssert(actual) {
        };
    }

    protected DoubleAssert assertThat(Double actual) {
        return new DoubleAssert(actual) {
        };
    }

    protected IntegerAssert assertThat(Integer actual) {
        return new IntegerAssert(actual) {
        };
    }

    protected LongAssert assertThat(Long actual) {
        return new LongAssert(actual) {
        };
    }

    protected StringAssert assertThat(String actual) {
        return new StringAssert(actual) {
        };
    }

    /** The simple names of every interface above {@code type}, so that a test can state the whole supertype chain. */
    protected static java.util.Set<String> supertypeNames(Class<?> type) {
        final java.util.Set<String> names = new java.util.HashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> each : current.getInterfaces()) {
                names.add(each.getSimpleName());
                names.addAll(supertypeNames(each));
            }
        }
        return names;
    }

    /**
     * The type name {@code toString()} prints before the parenthesised elements, e.g. {@code List} for
     * {@code List(1, 2)}, taken from the empty instance; a test class whose empty and non-empty instances print
     * different names overrides it.
     */
    protected String stringPrefix() {
        final String empty = empty().toString();
        return empty.substring(0, empty.length() - "()".length());
    }

    // what the type guarantees: a Stream has no size that is known without a walk
    protected final boolean hasDefiniteSize() {
        return !(empty() instanceof Stream);
    }

    protected final boolean isDistinct() {
        return empty() instanceof Set || empty() instanceof IntMap;
    }

    protected abstract <T> Collector<T, ?, ? extends Traversable<T>> collector();

    protected abstract <T> Traversable<T> empty();

    protected boolean emptyShouldBeSingleton() {
        return true;
    }

    protected abstract <T> Traversable<T> of(T element);

    @SuppressWarnings("unchecked")
    protected abstract <T> Traversable<T> of(T... elements);

    protected abstract <T> Traversable<T> ofAll(Iterable<? extends T> elements);

    protected abstract <T extends Comparable<? super T>> Traversable<T> ofJavaStream(java.util.stream.Stream<? extends T> javaStream);

    protected abstract Traversable<Boolean> ofAll(boolean... elements);

    protected abstract Traversable<Byte> ofAll(byte... elements);

    protected abstract Traversable<Character> ofAll(char... elements);

    protected abstract Traversable<Double> ofAll(double... elements);

    protected abstract Traversable<Float> ofAll(float... elements);

    protected abstract Traversable<Integer> ofAll(int... elements);

    protected abstract Traversable<Long> ofAll(long... elements);

    protected abstract Traversable<Short> ofAll(short... elements);

    protected abstract <T> Traversable<T> tabulate(int n, Function<? super Integer, ? extends T> f);

    protected abstract <T> Traversable<T> fill(int n, Supplier<? extends T> s);

    // -- the Traversable interface itself

    @TestTemplate
    public void shouldDeclareExactlyTheTraversableMembers() {
        final java.util.Set<String> instanceMembers = new java.util.HashSet<>();
        final java.util.Set<String> staticMembers = new java.util.HashSet<>();
        for (java.lang.reflect.Method method : Traversable.class.getDeclaredMethods()) {
            if (!method.isSynthetic()) {
                (java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? staticMembers : instanceMembers).add(method.getName());
            }
        }
        assertThat(instanceMembers).isEqualTo(TRAVERSABLE_MEMBERS);
        assertThat(staticMembers).isEqualTo(java.util.Set.of("narrow"));
        assertThat(Traversable.class.getInterfaces()).containsExactly(Iterable.class);
    }

    // -- static empty()

    @TestTemplate
    public void shouldCreateNil() {
        final Traversable<?> actual = empty();
        assertThat(actual.size()).isEqualTo(0);
    }

    // -- static narrow()

    @TestTemplate
    public void shouldNarrowTraversable() {
        final Traversable<Double> doubles = of(1.0d);
        final Traversable<Number> numbers = Traversable.narrow(doubles);
        final boolean actual = numbers.contains(new BigDecimal("2.0"));
        assertThat(actual).isFalse();
    }

    // -- static of()

    @TestTemplate
    public void shouldCreateSeqOfSeqUsingCons() {
        final List<List<Object>> actual = of(List.empty()).toList();
        assertThat(actual).isEqualTo(List.of(List.empty()));
    }

    // -- static of(T...)

    @TestTemplate
    public void shouldCreateInstanceOfElements() {
        final List<Integer> actual = of(1, 2).toList();
        assertThat(actual).isEqualTo(List.of(1, 2));
    }

    // -- static of(Iterable)

    @TestTemplate
    public void shouldCreateListOfIterable() {
        final java.util.List<Integer> arrayList = asList(1, 2);
        final List<Integer> actual = ofAll(arrayList).toList();
        assertThat(actual).isEqualTo(List.of(1, 2));
    }

    // -- static ofAll(java.util.stream.Stream)

    @TestTemplate
    public void shouldCreateStreamFromEmptyJavaUtilStream() {
        final java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.empty();
        assertThat(ofJavaStream(javaStream)).isEqualTo(empty());
    }

    @TestTemplate
    public void shouldCreateStreamFromNonEmptyJavaUtilStream() {
        final java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3);
        assertThat(ofJavaStream(javaStream)).isEqualTo(of(1, 2, 3));
    }

    // -- static of(<primitive array>)

    @TestTemplate
    public void shouldCreateListOfPrimitiveBooleanArray() {
        final Traversable<Boolean> actual = ofAll(true, false);
        final Traversable<Boolean> expected = of(true, false);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveByteArray() {
        final Traversable<Byte> actual = ofAll((byte) 1, (byte) 2, (byte) 3);
        final Traversable<Byte> expected = of((byte) 1, (byte) 2, (byte) 3);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveCharArray() {
        final Traversable<Character> actual = ofAll('a', 'b', 'c');
        final Traversable<Character> expected = of('a', 'b', 'c');
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveDoubleArray() {
        final Traversable<Double> actual = ofAll(1d, 2d, 3d);
        final Traversable<Double> expected = of(1d, 2d, 3d);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveFloatArray() {
        final Traversable<Float> actual = ofAll(1f, 2f, 3f);
        final Traversable<Float> expected = of(1f, 2f, 3f);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveIntArray() {
        final Traversable<Integer> actual = ofAll(1, 2, 3);
        final Traversable<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveLongArray() {
        final Traversable<Long> actual = ofAll(1L, 2L, 3L);
        final Traversable<Long> expected = of(1L, 2L, 3L);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldCreateListOfPrimitiveShortArray() {
        final Traversable<Short> actual = ofAll((short) 1, (short) 2, (short) 3);
        final Traversable<Short> expected = of((short) 1, (short) 2, (short) 3);
        assertThat(actual).isEqualTo(expected);
    }

    // -- contains

    @TestTemplate
    public void shouldRecognizeNilContainsNoElement() {
        final boolean actual = empty().contains(null);
        assertThat(actual).isFalse();
    }

    @TestTemplate
    public void shouldRecognizeNonNilDoesNotContainElement() {
        final boolean actual = of(1, 2, 3).contains(0);
        assertThat(actual).isFalse();
    }

    @TestTemplate
    public void shouldRecognizeNonNilDoesContainElement() {
        final boolean actual = of(1, 2, 3).contains(2);
        assertThat(actual).isTrue();
    }

    // -- containsAll

    @TestTemplate
    public void shouldHandleDuplicates() {
        final boolean actual = of(1, 2, 3, 2, 3, 1).containsAll(of(1, 2, 2));
        assertThat(actual).isTrue();
    }

    @TestTemplate
    public void shouldRecognizeNilNotContainsAllElements() {
        final boolean actual = empty().containsAll(of(1, 2, 3));
        assertThat(actual).isFalse();
    }

    @TestTemplate
    public void shouldRecognizeNonNilNotContainsAllOverlappingElements() {
        final boolean actual = of(1, 2, 3).containsAll(of(2, 3, 4));
        assertThat(actual).isFalse();
    }

    @TestTemplate
    public void shouldRecognizeNonNilContainsAllOnSelf() {
        final boolean actual = of(1, 2, 3).containsAll(of(1, 2, 3));
        assertThat(actual).isTrue();
    }

    // -- count

    @TestTemplate
    public void shouldCountWhenIsEmpty() {
        assertThat(empty().count(ignored -> true)).isEqualTo(0);
    }

    @TestTemplate
    public void shouldCountWhenNoneSatisfiesThePredicate() {
        assertThat(of(1, 2, 3).count(ignored -> false)).isEqualTo(0);
    }

    @TestTemplate
    public void shouldCountWhenAllSatisfyThePredicate() {
        assertThat(of(1, 2, 3).count(ignored -> true)).isEqualTo(3);
    }

    @TestTemplate
    public void shouldCountWhenSomeSatisfyThePredicate() {
        assertThat(of(1, 2, 3).count(i -> i % 2 == 0)).isEqualTo(1);
    }

    // -- find

    @TestTemplate
    public void shouldFindFirstOfNil() {
        assertThat(empty().find(ignored -> true)).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldFindFirstOfNonNil() {
        assertThat(of(1, 2, 3, 4).find(i -> i % 2 == 0)).isEqualTo(Option.some(2));
    }

    // -- foldLeft

    @TestTemplate
    public void shouldFoldLeftNil() {
        assertThat(this.<String>empty().foldLeft("", (xs, x) -> xs + x)).isEqualTo("");
    }

    @TestTemplate
    public void shouldThrowWhenFoldLeftNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().foldLeft(null, null));
    }

    @TestTemplate
    public void shouldFoldLeftNonNil() {
        assertThat(of("a", "b", "c").foldLeft("!", (xs, x) -> xs + x)).isEqualTo("!abc");
    }

    // -- isEmpty

    @TestTemplate
    public void shouldRecognizeNil() {
        assertThat(empty().isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldRecognizeNonNil() {
        assertThat(of(1).isEmpty()).isFalse();
    }

    // -- isEmpty

    @TestTemplate
    public void shouldCalculateIsEmpty() {
        assertThat(empty().isEmpty()).isTrue();
        assertThat(of(1).isEmpty()).isFalse();
    }

    // -- iterator

    @TestTemplate
    public void shouldNotHasNextWhenNilIterator() {
        assertThat(empty().iterator().hasNext()).isFalse();
    }

    @TestTemplate
    public void shouldThrowOnNextWhenNilIterator() {
        assertThrows(NoSuchElementException.class, () -> empty().iterator().next());
    }

    @TestTemplate
    public void shouldIterateFirstElementOfNonNil() {
        assertThat(of(1, 2, 3).iterator().next()).isEqualTo(1);
    }

    @TestTemplate
    public void shouldFullyIterateNonNil() {
        final java.util.Iterator<Integer> iterator = of(1, 2, 3).iterator();
        int actual;
        for (int i = 1; i <= 3; i++) {
            actual = iterator.next();
            assertThat(actual).isEqualTo(i);
        }
        assertThat(iterator.hasNext()).isFalse();
    }

    @TestTemplate
    public void shouldThrowWhenCallingNextOnEmptyIterator() {
        assertThatThrownBy(() -> empty().iterator().next()).isInstanceOf(NoSuchElementException.class);
    }

    @TestTemplate
    public void shouldThrowWhenCallingNextTooOftenOnNonEmptyIterator() {
        final java.util.Iterator<Integer> iterator = of(1).iterator();
        assertThatThrownBy(() -> {
            iterator.next();
            iterator.next();
        }).isInstanceOf(NoSuchElementException.class);
    }

    // -- mkString()

    @TestTemplate
    public void shouldMkStringNil() {
        assertThat(empty().mkString()).isEqualTo("");
    }

    @TestTemplate
    public void shouldMkStringNonNil() {
        assertThat(of('a', 'b', 'c').mkString()).isEqualTo("abc");
    }

    // -- mkString(delimiter)

    @TestTemplate
    public void shouldMkStringWithDelimiterNil() {
        assertThat(empty().mkString(",")).isEqualTo("");
    }

    @TestTemplate
    public void shouldMkStringWithDelimiterNonNil() {
        assertThat(of('a', 'b', 'c').mkString(",")).isEqualTo("a,b,c");
    }

    // -- mkString(delimiter, prefix, suffix)

    @TestTemplate
    public void shouldMkStringWithDelimiterAndPrefixAndSuffixNil() {
        assertThat(empty().mkString("[", ",", "]")).isEqualTo("[]");
    }

    @TestTemplate
    public void shouldMkStringWithDelimiterAndPrefixAndSuffixNonNil() {
        assertThat(of('a', 'b', 'c').mkString("[", ",", "]")).isEqualTo("[a,b,c]");
    }

    // -- nonEmpty

    @TestTemplate
    public void shouldCalculateNonEmpty() {
        assertThat(empty().nonEmpty()).isFalse();
        assertThat(of(1).nonEmpty()).isTrue();
    }

    // -- spliterator

    @TestTemplate
    public void shouldSplitNil() {
        final java.util.List<Integer> actual = new java.util.ArrayList<>();
        this.<Integer>empty().spliterator().forEachRemaining(actual::add);
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldSplitNonNil() {
        final java.util.List<Integer> actual = new java.util.ArrayList<>();
        of(1, 2, 3).spliterator().forEachRemaining(actual::add);
        assertThat(actual).isEqualTo(asList(1, 2, 3));
    }

    @TestTemplate
    public void shouldHaveImmutableSpliterator() {
        assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.IMMUTABLE)).isTrue();
    }

    // -- equals

    @SuppressWarnings("EqualsWithItself")
    @TestTemplate
    public void shouldEqualSameTraversableInstance() {
        final Traversable<?> nonEmpty = of(1);
        assertThat(nonEmpty.equals(nonEmpty)).isTrue();
        assertThat(empty().equals(empty())).isTrue();
    }

    @TestTemplate
    public void shouldNilNotEqualsNull() {
        assertThat(empty()).isNotNull();
    }

    @TestTemplate
    public void shouldNonNilNotEqualsNull() {
        assertThat(of(1)).isNotNull();
    }

    @TestTemplate
    public void shouldEmptyNotEqualsDifferentType() {
        assertThat(empty()).isNotEqualTo("");
    }

    @TestTemplate
    public void shouldNonEmptyNotEqualsDifferentType() {
        assertThat(of(1)).isNotEqualTo("");
    }

    @TestTemplate
    public void shouldRecognizeEqualityOfNils() {
        assertThat(empty()).isEqualTo(empty());
    }

    @TestTemplate
    public void shouldRecognizeEqualityOfNonNils() {
        assertThat(of(1, 2, 3).equals(of(1, 2, 3))).isTrue();
    }

    @TestTemplate
    public void shouldRecognizeNonEqualityOfTraversablesOfSameSize() {
        assertThat(of(1, 2, 3).equals(of(1, 2, 4))).isFalse();
    }

    @TestTemplate
    public void shouldRecognizeNonEqualityOfTraversablesOfDifferentSize() {
        assertThat(of(1, 2, 3).equals(of(1, 2))).isFalse();
        assertThat(of(1, 2).equals(of(1, 2, 3))).isFalse();
    }

    // -- equals

    @TestTemplate
    public void shouldRecognizeSameObject() {
        final Traversable<Integer> v = of(1);
        //noinspection EqualsWithItself
        assertThat(v.equals(v)).isTrue();
    }

    @TestTemplate
    public void shouldRecognizeEqualObjects() {
        final Traversable<Integer> v1 = of(1);
        final Traversable<Integer> v2 = of(1);
        assertThat(v1.equals(v2)).isTrue();
    }

    @TestTemplate
    public void shouldRecognizeUnequalObjects() {
        final Traversable<Integer> v1 = of(1);
        final Traversable<Integer> v2 = of(2);
        assertThat(v1.equals(v2)).isFalse();
    }

    // -- hashCode

    @TestTemplate
    public void shouldCalculateHashCodeOfNil() {
        assertThat(empty().hashCode() == empty().hashCode()).isTrue();
    }

    @TestTemplate
    public void shouldCalculateHashCodeOfNonNil() {
        assertThat(of(1, 2).hashCode() == of(1, 2).hashCode()).isTrue();
    }

    @TestTemplate
    public void shouldCalculateDifferentHashCodesForDifferentTraversables() {
        assertThat(of(1, 2).hashCode() != of(2, 3).hashCode()).isTrue();
    }

    @TestTemplate
    public void shouldComputeHashCodeOfEmpty() {
        assertThat(empty().hashCode()).isEqualTo(1);
    }

    @TestTemplate
    public void shouldNotThrowStackOverflowErrorWhenCalculatingHashCodeOf1000000Integers() {
        assertThat(ofAll(Vector.range(0, 1000000)).hashCode()).isNotNull();
    }

    // -- toString

    @TestTemplate
    public void shouldConformEmptyStringRepresentation() {
        final Traversable<Object> testee = empty();
        if (!hasDefiniteSize()) {
            assertThat(testee.toString()).isEqualTo(stringPrefix() + "()");
            testee.size(); // evaluates all elements of lazy collections
        }
        assertThat(testee.toString()).isEqualTo(toString(testee));
    }

    @TestTemplate
    public void shouldConformNonEmptyStringRepresentation() {
        final Traversable<Object> testee = of("a", "b", "c");
        if (!hasDefiniteSize()) {
            assertThat(testee.toString()).isEqualTo(stringPrefix() + "(a, ?)");
            testee.size(); // evaluates all elements of lazy collections
        }
        assertThat(testee.toString()).isEqualTo(toString(testee));
    }

    private String toString(Traversable<?> traversable) {
        return traversable.mkString(stringPrefix() + "(", ", ", ")");
    }

    // -- static collector()

    @TestTemplate
    public void shouldStreamAndCollectNil() {
        testCollector(() -> {
            final Traversable<?> actual = java.util.stream.Stream.empty().collect(collector());
            assertThat(actual).isEmpty();
        });
    }

    @TestTemplate
    public void shouldStreamAndCollectNonNil() {
        testCollector(() -> {
            final Traversable<?> actual = java.util.stream.Stream.of(1, 2, 3).collect(this.<Object>collector());
            assertThat(actual).isEqualTo(of(1, 2, 3));
        });
    }

    @TestTemplate
    public void shouldParallelStreamAndCollectNil() {
        testCollector(() -> {
            final Traversable<?> actual = java.util.stream.Stream.empty().parallel().collect(collector());
            assertThat(actual).isEmpty();
        });
    }

    @TestTemplate
    public void shouldParallelStreamAndCollectNonNil() {
        testCollector(() -> {
            final Traversable<?> actual = java.util.stream.Stream.of(1, 2, 3).parallel()
              .collect(this.<Object>collector());
            assertThat(actual).isEqualTo(of(1, 2, 3));
        });
    }

    // -- tabulate(int, Function)

    @TestTemplate
    public void shouldTabulateTheSeq() {
        final Function<Number, Integer> f = i -> i.intValue() * i.intValue();
        final Traversable<Number> actual = tabulate(3, f);
        assertThat(actual).isEqualTo(of(0, 1, 4));
    }

    @TestTemplate
    public void shouldTabulateTheSeqCallingTheFunctionInTheRightOrder() {
        final java.util.LinkedList<Integer> ints = new java.util.LinkedList<>(asList(0, 1, 2));
        final Function<Integer, Integer> f = i -> ints.remove();
        final Traversable<Integer> actual = tabulate(3, f);
        assertThat(actual).isEqualTo(of(0, 1, 2));
    }

    @TestTemplate
    public void shouldTabulateTheSeqWith0Elements() {
        assertThat(tabulate(0, i -> i)).isEqualTo(empty());
    }

    @TestTemplate
    public void shouldTabulateTheSeqWith0ElementsWhenNIsNegative() {
        assertThat(tabulate(-1, i -> i)).isEqualTo(empty());
    }

    // -- fill(int, Supplier)

    @TestTemplate
    public void shouldFillTheSeqCallingTheSupplierInTheRightOrder() {
        final java.util.LinkedList<Integer> ints = new java.util.LinkedList<>(asList(0, 1));
        final Traversable<Number> actual = fill(2, ints::remove);
        assertThat(actual).isEqualTo(of(0, 1));
    }

    @TestTemplate
    public void shouldFillTheSeqWith0Elements() {
        assertThat(fill(0, () -> 1)).isEqualTo(empty());
    }

    @TestTemplate
    public void shouldFillTheSeqWith0ElementsWhenNIsNegative() {
        assertThat(fill(-1, () -> 1)).isEqualTo(empty());
    }

    @TestTemplate
    public void ofShouldReturnTheSingletonEmpty() {
        if (!emptyShouldBeSingleton()) {
            return;
        }
        assertThat(of()).isSameAs(empty());
    }

    @TestTemplate
    public void ofAllShouldReturnTheSingletonEmpty() {
        if (!emptyShouldBeSingleton()) {
            return;
        }
        assertThat(ofAll(java.util.List.of())).isSameAs(empty());
    }

    // -- forEach

    @TestTemplate
    public void shouldPerformsActionOnEachElement() {
        final int[] consumer = new int[1];
        final Traversable<Integer> value = of(1, 2, 3);
        value.forEach(i -> consumer[0] += i);
        assertThat(consumer[0]).isEqualTo(6);
    }

    @TestTemplate
    public void shouldThrowOnForEachWithNullAction() {
        assertThrows(NullPointerException.class, () -> of(1).forEach(null));
    }

    // -- exists

    @TestTemplate
    public void shouldBeAwareOfExistingElement() {
        assertThat(of(1, 2).exists(i -> i == 2)).isTrue();
    }

    @TestTemplate
    public void shouldBeAwareOfNonExistingElement() {
        assertThat(this.<Integer>empty().exists(i -> i == 1)).isFalse();
    }

    @TestTemplate
    public void shouldThrowOnExistsWithNullPredicate() {
        assertThrows(NullPointerException.class, () -> of(1).exists(null));
    }

    // -- forAll

    @TestTemplate
    public void shouldBeAwareOfPropertyThatHoldsForAll() {
        assertThat(of(2, 4).forAll(i -> i % 2 == 0)).isTrue();
    }

    @TestTemplate
    public void shouldBeAwareOfPropertyThatNotHoldsForAll() {
        assertThat(of(1, 2).forAll(i -> i % 2 == 0)).isFalse();
    }

    @TestTemplate
    public void shouldHoldPropertyForAllOfNil() {
        assertThat(this.<Integer>empty().forAll(i -> false)).isTrue();
    }

    @TestTemplate
    public void shouldThrowOnForAllWithNullPredicate() {
        assertThrows(NullPointerException.class, () -> of(1).forAll(null));
    }

    // -- toList, toSet, toVector

    @TestTemplate
    public void shouldConvertToList() {
        assertThat(of(1, 2, 3).toList()).isEqualTo(List.of(1, 2, 3));
        assertThat(empty().toList()).isSameAs(List.empty());
    }

    @TestTemplate
    public void shouldConvertToSet() {
        assertThat(of(1, 2, 3).toSet()).isEqualTo(HashSet.of(1, 2, 3));
        assertThat(empty().toSet()).isSameAs(HashSet.empty());
    }

    @TestTemplate
    public void shouldConvertToVector() {
        assertThat(of(1, 2, 3).toVector()).isEqualTo(Vector.of(1, 2, 3));
        assertThat(empty().toVector()).isSameAs(Vector.empty());
    }

    // -- toArray

    @TestTemplate
    public void shouldConvertToJavaArray() {
        assertThat(of(1, 2, 3).toArray()).isEqualTo(new Object[]{1, 2, 3});
        assertThat(empty().toArray()).isEmpty();
    }

    @TestTemplate
    public void shouldConvertToJavaArrayWithFactory() {
        final Integer[] ints = of(1, 2, 3).toArray(Integer[]::new);
        assertThat(ints).containsOnly(1, 2, 3);
    }

    // -- stream

    @TestTemplate
    public void shouldConvertToJavaStream() {
        final java.util.stream.Stream<Integer> s1 = of(1, 2, 3).stream();
        final java.util.stream.Stream<Integer> s2 = java.util.stream.Stream.of(1, 2, 3);
        assertThat(List.ofAll(s1::iterator)).isEqualTo(List.ofAll(s2::iterator));
    }

    @TestTemplate
    public void shouldHaveAReasonableToString() {
        final Traversable<Integer> value = of(1, 2);
        value.toList(); // evaluate all elements (e.g. for Stream)
        assertThat(value.toString()).contains("1", "2");
    }

    // -- null elements are rejected at construction, everywhere (design 3.9)

    @TestTemplate
    public void shouldRejectNullElementOnConstruction() {
        assertThrows(NullPointerException.class, () -> of((Integer) null));
    }

    private void testCollector(Runnable test) {
        test.run();
    }

    // -- toArray()

    @TestTemplate
    public void shouldCopyIntoAnObjectArray() {
        final Object[] array = of(1, 2, 3).toArray();
        assertThat(array.getClass()).isEqualTo(Object[].class);
        assertThat(array).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(of(1, 2, 3).toArray()).isNotSameAs(of(1, 2, 3).toArray());
    }

    @TestTemplate
    public void shouldCopyIntoATypedArrayOfTheExactSize() {
        final Integer[] array = of(1, 2, 3).toArray(Integer[]::new);
        assertThat(array.getClass()).isEqualTo(Integer[].class);
        assertThat(array.length).isEqualTo(3);
        assertThat(this.<Integer>empty().toArray(Integer[]::new).length).isEqualTo(0);
    }

    @TestTemplate
    public void shouldThrowOnToArrayWithNullFactory() {
        assertThrows(NullPointerException.class, () -> of(1).toArray(null));
    }

    // -- stream

    @TestTemplate
    public void shouldStreamTheElementsWithTheCollectionsCharacteristics() {
        final java.util.stream.Stream<Integer> stream = of(1, 2, 3).stream();
        assertThat(stream.isParallel()).isFalse();
        final Spliterator<Integer> spliterator = of(1, 2, 3).stream().spliterator();
        assertThat(spliterator.hasCharacteristics(Spliterator.IMMUTABLE)).isTrue();
        assertThat(spliterator.hasCharacteristics(Spliterator.SIZED)).isEqualTo(hasDefiniteSize());
        assertThat(spliterator.hasCharacteristics(Spliterator.DISTINCT)).isEqualTo(isDistinct());
        assertThat(stream.count()).isEqualTo(3L);
        assertThat(this.<Integer>empty().stream().count()).isEqualTo(0L);
    }

    // -- asJava

    @TestTemplate
    public void shouldViewTheElementsAsAJavaCollection() {
        final Traversable<Integer> elements = of(1, 2, 3);
        final java.util.Collection<Integer> view = elements.asJava();
        assertThat(view.size()).isEqualTo(3);
        assertThat(view.isEmpty()).isFalse();
        assertThat(view.contains(2)).isTrue();
        assertThat(view.contains(4)).isFalse();
        assertThat(view.containsAll(asList(1, 3))).isTrue();
        assertThat(List.ofAll(view)).isEqualTo(elements.toList());
        assertThat(view.toArray()).containsExactlyElementsOf(elements.toList());
        assertThat(List.ofAll(view.stream()::iterator)).isEqualTo(elements.toList());
        assertThat(this.<Integer>empty().asJava().isEmpty()).isTrue();
        assertThat(this.<Integer>empty().asJava().size()).isEqualTo(0);
    }

    @TestTemplate
    public void shouldRefuseEveryMutatorOnTheJavaView() {
        final java.util.Collection<Integer> view = of(1, 2, 3).asJava();
        assertThrows(UnsupportedOperationException.class, () -> view.add(4));
        assertThrows(UnsupportedOperationException.class, () -> view.addAll(asList(4, 5)));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(1));
        assertThrows(UnsupportedOperationException.class, () -> view.removeAll(asList(1)));
        assertThrows(UnsupportedOperationException.class, () -> view.retainAll(asList(1)));
        assertThrows(UnsupportedOperationException.class, () -> view.removeIf(i -> true));
        assertThrows(UnsupportedOperationException.class, view::clear);
        final java.util.Iterator<Integer> iterator = view.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @TestTemplate
    public void shouldRefuseEveryMutatorOnTheJavaViewEvenWhenNothingWouldChange() {
        final java.util.Collection<Integer> view = of(1, 2, 3).asJava();
        assertThrows(UnsupportedOperationException.class, () -> view.remove(99));
        assertThrows(UnsupportedOperationException.class, () -> view.removeAll(asList(99)));
        assertThrows(UnsupportedOperationException.class, () -> view.retainAll(asList(1, 2, 3)));
        assertThrows(UnsupportedOperationException.class, () -> view.removeIf(i -> false));
        assertThrows(UnsupportedOperationException.class, () -> view.addAll(asList()));
        final java.util.Collection<Integer> empty = this.<Integer>empty().asJava();
        assertThrows(UnsupportedOperationException.class, () -> empty.remove(1));
        assertThrows(UnsupportedOperationException.class, () -> empty.removeIf(i -> true));
        assertThat(view.size()).isEqualTo(3);
    }

    @TestTemplate
    public void shouldViewWithoutCopying() {
        final java.util.Collection<Integer> view = of(1, 2, 3).asJava();
        assertThat(view.getClass().getName()).startsWith("com.guizmaii.zazr.collection.JavaConverters$");
    }

    // helpers

    /**
     * Wraps a String in order to ensure that it is not Comparable.
     */
    static final class NonComparable {

        final String value;

        NonComparable(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof NonComparable) {
                final NonComparable that = (NonComparable) obj;
                return Objects.equals(this.value, that.value);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
