package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.control.Option;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reflective checks shared by the tests of the non-empty collections: which overloads return or hold a non-empty
 * collection, that none of them is empty, and which methods of the plain type the wrapper lacks.
 */
final class NonEmptyChecks {

    private NonEmptyChecks() {}

    /* the wrapper types whose instances must never be empty */
    static final java.util.List<Class<?>> NON_EMPTY_TYPES = java.util.List.of(
            NonEmptyVector.class, NonEmptySet.class, NonEmptySortedSet.class);

    /* the signature of a method as the tables key it: name(SimpleParameterType, ...) */
    static String signature(Method method) {
        final StringJoiner joiner = new StringJoiner(", ", method.getName() + "(", ")");
        for (Class<?> parameter : method.getParameterTypes()) {
            joiner.add(parameter.getSimpleName());
        }
        return joiner.toString();
    }

    /* the signatures of the public methods of type whose result is or holds one of the non-empty types */
    static java.util.Set<String> holding(Class<?> type) {
        final java.util.Set<String> holding = new TreeSet<>();
        for (Method method : type.getMethods()) {
            final String returned = method.getGenericReturnType().getTypeName();
            for (Class<?> nonEmpty : NON_EMPTY_TYPES) {
                if (returned.contains(nonEmpty.getName())) {
                    holding.add(signature(method));
                }
            }
        }
        assertThat(holding).isNotEmpty();
        return holding;
    }

    /* every non-empty collection reachable from a result, through tuples, Options, maps and iterables, is non-empty */
    static void assertEveryNonEmptyCollectionIsNonEmpty(Object result, String call) {
        switch (result) {
            case NonEmptyVector<?> nev -> {
                assertThat(nev.toVector().isEmpty()).as(call).isFalse();
                nev.forEach(element -> assertEveryNonEmptyCollectionIsNonEmpty(element, call));
            }
            case NonEmptySet<?> nes -> {
                assertThat(nes.toSet().isEmpty()).as(call).isFalse();
                nes.forEach(element -> assertEveryNonEmptyCollectionIsNonEmpty(element, call));
            }
            case NonEmptySortedSet<?> ness -> {
                assertThat(ness.toSortedSet().isEmpty()).as(call).isFalse();
                ness.forEach(element -> assertEveryNonEmptyCollectionIsNonEmpty(element, call));
            }
            case Tuple2<?, ?> t -> {
                assertEveryNonEmptyCollectionIsNonEmpty(t._1(), call);
                assertEveryNonEmptyCollectionIsNonEmpty(t._2(), call);
            }
            case Tuple3<?, ?, ?> t -> {
                assertEveryNonEmptyCollectionIsNonEmpty(t._1(), call);
                assertEveryNonEmptyCollectionIsNonEmpty(t._2(), call);
                assertEveryNonEmptyCollectionIsNonEmpty(t._3(), call);
            }
            case Option<?> option -> option.forEach(value -> assertEveryNonEmptyCollectionIsNonEmpty(value, call));
            case Iterable<?> iterable -> iterable.forEach(element -> assertEveryNonEmptyCollectionIsNonEmpty(element, call));
            default -> { }
        }
    }

    /* a wrapper holding an empty collection, made through the private constructor, which the API never does */
    static Object hollow(Class<?> type, Object empty) {
        try {
            final java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return constructor.newInstance(empty);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static java.util.Set<String> publicInstanceMethodNames(Class<?> type) {
        final java.util.Set<String> names = new TreeSet<>();
        for (Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) && !method.isSynthetic() && !method.isBridge()
                    && method.getDeclaringClass() != Object.class) {
                names.add(method.getName());
            }
        }
        return names;
    }

    /* the public instance method names of plain that wrapper does not have */
    static java.util.Set<String> missing(Class<?> plain, Class<?> wrapper) {
        final java.util.Set<String> missing = new TreeSet<>(publicInstanceMethodNames(plain));
        missing.removeAll(publicInstanceMethodNames(wrapper));
        return missing;
    }
}
