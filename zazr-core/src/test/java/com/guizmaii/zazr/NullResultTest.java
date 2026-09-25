package com.guizmaii.zazr;

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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * A function handed to a public method that returns {@code null} where the method needs a Zazr value (an
 * {@code Option}, an {@code Either}, a {@code Try}, a tuple, an iterable of elements, ...) or an element is rejected
 * at the call with a {@code NullPointerException} naming the type and the method; a method that runs the function
 * under {@code Try} returns a {@code Failure} of that exception instead. One row per method, and a guard that fails
 * when a public method of {@code zazr-core} taking a function whose result is a Zazr type has no row.
 */
public class NullResultTest {

    /** A method, the message its null result is rejected with, and a call that makes the function return null. */
    private record Case(String method, String message, boolean failure, Object call) {
    }

    private static Case throwing(String method, String message, ThrowingCallable call) {
        return new Case(method, message, false, call);
    }

    private static Case failure(String method, String message, Supplier<Try<?>> call) {
        return new Case(method, message, true, call);
    }

    /**
     * The methods the guard finds but that need no row: a fold returns whatever the caller's function returns
     * ({@code reduce} and {@code reduceOption} of a map are folds over its entries, which are tuples).
     */
    private static final java.util.Map<String, String> EXCLUDED = java.util.Map.ofEntries(
            java.util.Map.entry("HashMap.fold", "a fold returns the caller's own result"),
            java.util.Map.entry("HashMap.reduce", "a fold returns the caller's own result"),
            java.util.Map.entry("HashMap.reduceOption", "a fold returns the caller's own result"),
            java.util.Map.entry("LinkedHashMap.fold", "a fold returns the caller's own result"),
            java.util.Map.entry("LinkedHashMap.reduce", "a fold returns the caller's own result"),
            java.util.Map.entry("LinkedHashMap.reduceOption", "a fold returns the caller's own result"),
            java.util.Map.entry("TreeMap.fold", "a fold returns the caller's own result"),
            java.util.Map.entry("TreeMap.reduce", "a fold returns the caller's own result"),
            java.util.Map.entry("TreeMap.reduceOption", "a fold returns the caller's own result"));

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static java.util.List<Case> cases() {
        final java.util.List<Case> cases = new ArrayList<>();

        // -- Option
        cases.add(throwing("Option.collect", "Option.collect: mapper returned null", () -> Option.some(1).collect(x -> null)));
        cases.add(throwing("Option.flatMap", "Option.flatMap: mapper returned null", () -> Option.some(1).flatMap(x -> null)));
        cases.add(throwing("Option.forEach", "Option.forEach: mapper returned null", () -> Option.forEach(List.of(1), x -> null)));
        cases.add(throwing("Option.map", "Option.map: mapper returned null", () -> Option.some(1).map(x -> null)));
        cases.add(throwing("Option.orElse", "Option.orElse: supplier returned null", () -> Option.none().orElse(() -> null)));
        cases.add(throwing("Option.toEither", "Option.toEither: leftSupplier returned null", () -> Option.none().toEither(() -> null)));
        cases.add(throwing("Option.toTry", "Option.toTry: ifEmpty returned null", () -> Option.none().toTry(() -> null)));
        cases.add(throwing("Option.toValidation", "Option.toValidation: invalidSupplier returned null", () -> Option.none().toValidation(() -> null)));
        cases.add(throwing("Option.when", "Option.when: supplier returned null", () -> Option.when(true, () -> null)));

        // -- Either
        cases.add(throwing("Either.filterOrElse", "Either.filterOrElse: zero returned null", () -> Either.right(1).filterOrElse(x -> false, x -> null)));
        cases.add(throwing("Either.flatMap", "Either.flatMap: mapper returned null", () -> Either.right(1).flatMap(x -> null)));
        cases.add(throwing("Either.forEach", "Either.forEach: mapper returned null", () -> Either.forEach(List.of(1), x -> null)));
        cases.add(throwing("Either.fromPredicate", "Either.fromPredicate: ifFalse returned null", () -> Either.fromPredicate(1, x -> false, () -> null)));
        cases.add(throwing("Either.map", "Either.map: mapper returned null", () -> Either.right(1).map(x -> null)));
        cases.add(throwing("Either.mapBoth", "Either.mapBoth: rightMapper returned null", () -> Either.right(1).mapBoth(l -> l, r -> null)));
        cases.add(throwing("Either.mapBoth", "Either.mapBoth: leftMapper returned null", () -> Either.left(1).mapBoth(l -> null, r -> r)));
        cases.add(throwing("Either.mapLeft", "Either.mapLeft: leftMapper returned null", () -> Either.left(1).mapLeft(x -> null)));
        cases.add(throwing("Either.orElse", "Either.orElse: supplier returned null", () -> Either.left(1).orElse(() -> null)));
        cases.add(throwing("Either.toTry", "Either.toTry: f returned null", () -> Either.left(1).toTry(x -> null)));

        // -- Try: the methods that run the function under Try return the exception as a Failure
        final Try<Integer> failed = Try.failure(new IllegalStateException("failed"));
        cases.add(failure("Try.catchAll", "Try.catchAll: f returned null", () -> failed.catchAll(e -> null)));
        cases.add(failure("Try.catchAllWith", "Try.catchAllWith: f returned null", () -> failed.catchAllWith(e -> null)));
        cases.add(failure("Try.catchSome", "Try.catchSome: f returned null", () -> failed.catchSome(IllegalStateException.class, e -> null)));
        cases.add(failure("Try.catchSomeWith", "Try.catchSomeWith: f returned null", () -> failed.catchSomeWith(IllegalStateException.class, e -> null)));
        cases.add(failure("Try.collect", "Try.collect: mapper returned null", () -> Try.success(1).collect(x -> null)));
        cases.add(failure("Try.filter", "Try.filter: ifFalse returned null", () -> Try.success(1).filter(x -> false, x -> null)));
        cases.add(failure("Try.flatMap", "Try.flatMap: mapper returned null", () -> Try.success(1).flatMap(x -> null)));
        cases.add(failure("Try.flatMapTry", "Try.flatMapTry: mapper returned null", () -> Try.success(1).flatMapTry(x -> null)));
        cases.add(failure("Try.map", "Try.map: mapper returned null", () -> Try.success(1).map(x -> null)));
        cases.add(failure("Try.mapError", "Try.mapError: f returned null", () -> failed.mapError(e -> null)));
        cases.add(throwing("Try.forEach", "Try.forEach: mapper returned null", () -> Try.forEach(List.of(1), x -> null)));
        cases.add(throwing("Try.orElse", "Try.orElse: supplier returned null", () -> failed.orElse(() -> null)));

        // -- Validation
        cases.add(throwing("Validation.flatMap", "Validation.flatMap: f returned null", () -> Validation.valid(1).flatMap(x -> null)));
        cases.add(throwing("Validation.flatMapEither", "Validation.flatMapEither: f returned null", () -> Validation.valid(1).flatMapEither(x -> null)));
        cases.add(throwing("Validation.forEach", "Validation.forEach: f returned null", () -> Validation.forEach(List.of(1), x -> null)));
        cases.add(throwing("Validation.forEach", "Validation.forEach: f returned null", () -> Validation.forEach(NonEmptyVector.of(1), x -> null)));
        cases.add(throwing("Validation.mapErrorAll", "Validation.mapErrorAll: f returned null", () -> Validation.invalid("e").mapErrorAll(e -> null)));
        cases.add(throwing("Validation.orElse", "Validation.orElse: that returned null", () -> Validation.invalid("e").orElse(() -> null)));
        cases.add(throwing("Validation.partition", "Validation.partition: f returned null", () -> Validation.partition(List.of(1), x -> null)));

        // -- Lazy
        cases.add(throwing("Lazy.flatMap", "Lazy.flatMap: mapper returned null", () -> Lazy.of(() -> 1).flatMap(x -> null).get()));

        // -- tuples
        cases.add(throwing("Tuple2.map", "Tuple2.map: mapper returned null", () -> Tuple.of(1, 2).map((a, b) -> null)));
        cases.add(throwing("Tuple3.map", "Tuple3.map: mapper returned null", () -> Tuple.of(1, 2, 3).map((a, b, c) -> null)));
        cases.add(throwing("Tuple4.map", "Tuple4.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4).map((a, b, c, d) -> null)));
        cases.add(throwing("Tuple5.map", "Tuple5.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5).map((a, b, c, d, e) -> null)));
        cases.add(throwing("Tuple6.map", "Tuple6.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5, 6).map((a, b, c, d, e, f) -> null)));
        cases.add(throwing("Tuple7.map", "Tuple7.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5, 6, 7).map((a, b, c, d, e, f, g) -> null)));
        cases.add(throwing("Tuple8.map", "Tuple8.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5, 6, 7, 8).map((a, b, c, d, e, f, g, h) -> null)));

        // -- checked functions: recover returning null fails the composed function, with the throwable as the cause
        final IOException io = new IOException("io");
        cases.add(throwing("CheckedFunction1.recover", "CheckedFunction1.recover: recover returned null",
                () -> ((CheckedFunction1<Integer, Integer>) a -> { throw io; }).recover(t -> null).apply(1)));
        cases.add(throwing("CheckedFunction2.recover", "CheckedFunction2.recover: recover returned null",
                () -> ((CheckedFunction2<Integer, Integer, Integer>) (a, b) -> { throw io; }).recover(t -> null).apply(1, 2)));
        cases.add(throwing("CheckedFunction3.recover", "CheckedFunction3.recover: recover returned null",
                () -> ((CheckedFunction3<Integer, Integer, Integer, Integer>) (a, b, c) -> { throw io; }).recover(t -> null).apply(1, 2, 3)));
        cases.add(throwing("CheckedFunction4.recover", "CheckedFunction4.recover: recover returned null",
                () -> ((CheckedFunction4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4)));
        cases.add(throwing("CheckedFunction5.recover", "CheckedFunction5.recover: recover returned null",
                () -> ((CheckedFunction5<Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5)));
        cases.add(throwing("CheckedFunction6.recover", "CheckedFunction6.recover: recover returned null",
                () -> ((CheckedFunction6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5, 6)));
        cases.add(throwing("CheckedFunction7.recover", "CheckedFunction7.recover: recover returned null",
                () -> ((CheckedFunction7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f, g) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5, 6, 7)));
        cases.add(throwing("CheckedFunction8.recover", "CheckedFunction8.recover: recover returned null",
                () -> ((CheckedFunction8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f, g, h) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5, 6, 7, 8)));

        // -- sequences
        cases.add(throwing("Vector.collect", "Vector.collect: mapper returned null", () -> Vector.of(1).collect(x -> null)));
        cases.add(throwing("Vector.flatMap", "Vector.flatMap: mapper returned null", () -> Vector.of(1).flatMap(x -> null)));
        cases.add(throwing("Vector.groupBy", "Vector.groupBy: classifier returned null", () -> Vector.of(1).groupBy(x -> null)));
        cases.add(throwing("Vector.orElse", "Vector.orElse: supplier returned null", () -> Vector.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("Vector.partitionMap", "Vector.partitionMap: f returned null", () -> Vector.of(1).partitionMap(x -> null)));
        cases.add(throwing("Vector.toLinkedMap", "Vector.toLinkedMap: f returned null", () -> Vector.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("Vector.toMap", "Vector.toMap: f returned null", () -> Vector.of(1).toMap(x -> null)));
        cases.add(throwing("Vector.toSortedMap", "Vector.toSortedMap: f returned null", () -> Vector.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("Vector.toSortedMap", "Vector.toSortedMap: f returned null", () -> Vector.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("Vector.unfold", "Vector.unfold: f returned null", () -> Vector.unfold(1, x -> null)));
        cases.add(throwing("Vector.unfoldLeft", "Vector.unfoldLeft: f returned null", () -> Vector.unfoldLeft(1, x -> null)));
        cases.add(throwing("Vector.unfoldRight", "Vector.unfoldRight: f returned null", () -> Vector.unfoldRight(1, x -> null)));
        cases.add(throwing("Vector.unzip", "Vector.unzip: unzipper returned null", () -> Vector.of(1).unzip(x -> null)));
        cases.add(throwing("Vector.unzip3", "Vector.unzip3: unzipper returned null", () -> Vector.of(1).unzip3(x -> null)));

        cases.add(throwing("List.collect", "List.collect: mapper returned null", () -> List.of(1).collect(x -> null)));
        cases.add(throwing("List.flatMap", "List.flatMap: mapper returned null", () -> List.of(1).flatMap(x -> null)));
        cases.add(throwing("List.groupBy", "List.groupBy: classifier returned null", () -> List.of(1).groupBy(x -> null)));
        cases.add(throwing("List.orElse", "List.orElse: supplier returned null", () -> List.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("List.partitionMap", "List.partitionMap: f returned null", () -> List.of(1).partitionMap(x -> null)));
        cases.add(throwing("List.toLinkedMap", "List.toLinkedMap: f returned null", () -> List.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("List.toMap", "List.toMap: f returned null", () -> List.of(1).toMap(x -> null)));
        cases.add(throwing("List.toSortedMap", "List.toSortedMap: f returned null", () -> List.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("List.unfold", "List.unfold: f returned null", () -> List.unfold(1, x -> null)));
        cases.add(throwing("List.unfoldLeft", "List.unfoldLeft: f returned null", () -> List.unfoldLeft(1, x -> null)));
        cases.add(throwing("List.unfoldRight", "List.unfoldRight: f returned null", () -> List.unfoldRight(1, x -> null)));
        cases.add(throwing("List.unzip", "List.unzip: unzipper returned null", () -> List.of(1).unzip(x -> null)));
        cases.add(throwing("List.unzip3", "List.unzip3: unzipper returned null", () -> List.of(1).unzip3(x -> null)));

        cases.add(throwing("Queue.collect", "Queue.collect: mapper returned null", () -> Queue.of(1).collect(x -> null)));
        cases.add(throwing("Queue.flatMap", "Queue.flatMap: mapper returned null", () -> Queue.of(1).flatMap(x -> null)));
        cases.add(throwing("Queue.groupBy", "Queue.groupBy: classifier returned null", () -> Queue.of(1).groupBy(x -> null)));
        cases.add(throwing("Queue.orElse", "Queue.orElse: supplier returned null", () -> Queue.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("Queue.partitionMap", "Queue.partitionMap: f returned null", () -> Queue.of(1).partitionMap(x -> null)));
        cases.add(throwing("Queue.toLinkedMap", "Queue.toLinkedMap: f returned null", () -> Queue.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("Queue.toMap", "Queue.toMap: f returned null", () -> Queue.of(1).toMap(x -> null)));
        cases.add(throwing("Queue.toSortedMap", "Queue.toSortedMap: f returned null", () -> Queue.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("Queue.unfold", "Queue.unfold: f returned null", () -> Queue.unfold(1, x -> null)));
        cases.add(throwing("Queue.unfoldLeft", "Queue.unfoldLeft: f returned null", () -> Queue.unfoldLeft(1, x -> null)));
        cases.add(throwing("Queue.unfoldRight", "Queue.unfoldRight: f returned null", () -> Queue.unfoldRight(1, x -> null)));
        cases.add(throwing("Queue.unzip", "Queue.unzip: unzipper returned null", () -> Queue.of(1).unzip(x -> null)));
        cases.add(throwing("Queue.unzip3", "Queue.unzip3: unzipper returned null", () -> Queue.of(1).unzip3(x -> null)));

        // a Stream is lazy: the null is rejected when the element is reached
        cases.add(throwing("Stream.appendSelf", "Stream.appendSelf: mapper returned null", () -> Stream.of(1).appendSelf(s -> null).length()));
        cases.add(throwing("Stream.collect", "Stream.collect: mapper returned null", () -> Stream.of(1).collect(x -> null).length()));
        cases.add(throwing("Stream.cons", "Stream.cons: tailSupplier returned null", () -> Stream.cons(1, () -> null).tail()));
        cases.add(throwing("Stream.flatMap", "Stream.flatMap: mapper returned null", () -> Stream.of(1).flatMap(x -> null).length()));
        cases.add(throwing("Stream.groupBy", "Stream.groupBy: classifier returned null", () -> Stream.of(1).groupBy(x -> null)));
        cases.add(throwing("Stream.iterate", "Stream.iterate: supplier returned null", () -> Stream.iterate(() -> null).length()));
        cases.add(throwing("Stream.orElse", "Stream.orElse: supplier returned null", () -> Stream.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("Stream.partitionMap", "Stream.partitionMap: f returned null", () -> Stream.of(1).partitionMap(x -> null)._1().length()));
        cases.add(throwing("Stream.toLinkedMap", "Stream.toLinkedMap: f returned null", () -> Stream.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("Stream.toMap", "Stream.toMap: f returned null", () -> Stream.of(1).toMap(x -> null)));
        cases.add(throwing("Stream.toSortedMap", "Stream.toSortedMap: f returned null", () -> Stream.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("Stream.unfold", "Stream.unfold: f returned null", () -> Stream.unfold(1, x -> null).length()));
        cases.add(throwing("Stream.unfoldLeft", "Stream.unfoldLeft: f returned null", () -> Stream.unfoldLeft(1, x -> null).length()));
        cases.add(throwing("Stream.unfoldRight", "Stream.unfoldRight: f returned null", () -> Stream.unfoldRight(1, x -> null).length()));
        cases.add(throwing("Stream.unzip", "Stream.unzip: unzipper returned null", () -> Stream.of(1).unzip(x -> null)._1().length()));
        cases.add(throwing("Stream.unzip3", "Stream.unzip3: unzipper returned null", () -> Stream.of(1).unzip3(x -> null)._1().length()));

        cases.add(throwing("NonEmptyVector.collect", "NonEmptyVector.collect: mapper returned null", () -> NonEmptyVector.of(1).collect(x -> null)));
        cases.add(throwing("NonEmptyVector.flatMap", "NonEmptyVector.flatMap: mapper returned null", () -> NonEmptyVector.of(1).flatMap(x -> null)));
        cases.add(throwing("NonEmptyVector.flatMapAll", "NonEmptyVector.flatMapAll: mapper returned null", () -> NonEmptyVector.of(1).flatMapAll(x -> null)));
        cases.add(throwing("NonEmptyVector.groupBy", "NonEmptyVector.groupBy: classifier returned null", () -> NonEmptyVector.of(1).groupBy(x -> null)));
        cases.add(throwing("NonEmptyVector.partitionMap", "NonEmptyVector.partitionMap: f returned null", () -> NonEmptyVector.of(1).partitionMap(x -> null)));
        cases.add(throwing("NonEmptyVector.toLinkedMap", "NonEmptyVector.toLinkedMap: f returned null", () -> NonEmptyVector.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("NonEmptyVector.toMap", "NonEmptyVector.toMap: f returned null", () -> NonEmptyVector.of(1).toMap(x -> null)));
        cases.add(throwing("NonEmptyVector.toSortedMap", "NonEmptyVector.toSortedMap: f returned null", () -> NonEmptyVector.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptyVector.unzip", "NonEmptyVector.unzip: unzipper returned null", () -> NonEmptyVector.of(1).unzip(x -> null)));
        cases.add(throwing("NonEmptyVector.unzip3", "NonEmptyVector.unzip3: unzipper returned null", () -> NonEmptyVector.of(1).unzip3(x -> null)));

        // -- sets: toMap and its siblings are the defaults of Set
        cases.add(throwing("HashSet.collect", "HashSet.collect: mapper returned null", () -> HashSet.of(1).collect(x -> null)));
        cases.add(throwing("HashSet.flatMap", "HashSet.flatMap: mapper returned null", () -> HashSet.of(1).flatMap(x -> null)));
        cases.add(throwing("HashSet.groupBy", "HashSet.groupBy: classifier returned null", () -> HashSet.of(1).groupBy(x -> null)));
        cases.add(throwing("HashSet.orElse", "HashSet.orElse: supplier returned null", () -> HashSet.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("HashSet.partitionMap", "HashSet.partitionMap: f returned null", () -> HashSet.of(1).partitionMap(x -> null)));
        cases.add(throwing("HashSet.toLinkedMap", "Set.toLinkedMap: f returned null", () -> HashSet.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("HashSet.toMap", "Set.toMap: f returned null", () -> HashSet.of(1).toMap(x -> null)));
        cases.add(throwing("HashSet.toSortedMap", "Set.toSortedMap: f returned null", () -> HashSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));

        cases.add(throwing("LinkedHashSet.collect", "LinkedHashSet.collect: mapper returned null", () -> LinkedHashSet.of(1).collect(x -> null)));
        cases.add(throwing("LinkedHashSet.flatMap", "LinkedHashSet.flatMap: mapper returned null", () -> LinkedHashSet.of(1).flatMap(x -> null)));
        cases.add(throwing("LinkedHashSet.groupBy", "LinkedHashSet.groupBy: classifier returned null", () -> LinkedHashSet.of(1).groupBy(x -> null)));
        cases.add(throwing("LinkedHashSet.orElse", "LinkedHashSet.orElse: supplier returned null", () -> LinkedHashSet.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("LinkedHashSet.partitionMap", "LinkedHashSet.partitionMap: f returned null", () -> LinkedHashSet.of(1).partitionMap(x -> null)));
        cases.add(throwing("LinkedHashSet.toLinkedMap", "Set.toLinkedMap: f returned null", () -> LinkedHashSet.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("LinkedHashSet.toMap", "Set.toMap: f returned null", () -> LinkedHashSet.of(1).toMap(x -> null)));
        cases.add(throwing("LinkedHashSet.toSortedMap", "Set.toSortedMap: f returned null", () -> LinkedHashSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));

        cases.add(throwing("TreeSet.collect", "TreeSet.collect: mapper returned null", () -> TreeSet.of(1).collect(x -> null)));
        cases.add(throwing("TreeSet.flatMap", "TreeSet.flatMap: mapper returned null", () -> TreeSet.of(1).flatMap(x -> (Iterable<Integer>) null)));
        cases.add(throwing("TreeSet.flatMap", "TreeSet.flatMap: mapper returned null", () -> TreeSet.of(1).flatMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("TreeSet.groupBy", "TreeSet.groupBy: classifier returned null", () -> TreeSet.of(1).groupBy(x -> null)));
        cases.add(throwing("TreeSet.orElse", "TreeSet.orElse: supplier returned null", () -> TreeSet.<Integer> empty().orElse((Supplier<Iterable<Integer>>) () -> null)));
        cases.add(throwing("TreeSet.toLinkedMap", "Set.toLinkedMap: f returned null", () -> TreeSet.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("TreeSet.toMap", "Set.toMap: f returned null", () -> TreeSet.of(1).toMap(x -> null)));
        cases.add(throwing("TreeSet.toSortedMap", "Set.toSortedMap: f returned null", () -> TreeSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));

        // -- maps: toMap and its siblings are the defaults of Map
        cases.add(throwing("HashMap.collect", "HashMap.collect: mapper returned null", () -> HashMap.of(1, "a").collect((k, v) -> null)));
        cases.add(throwing("HashMap.fill", "HashMap.fill: s returned null", () -> HashMap.fill(1, () -> null)));
        cases.add(throwing("HashMap.flatMap", "HashMap.flatMap: mapper returned null", () -> HashMap.of(1, "a").flatMap((k, v) -> null)));
        cases.add(throwing("HashMap.groupBy", "HashMap.groupBy: classifier returned null", () -> HashMap.of(1, "a").groupBy(e -> null)));
        cases.add(throwing("HashMap.map", "HashMap.map: mapper returned null", () -> HashMap.of(1, "a").map((k, v) -> null)));
        cases.add(throwing("HashMap.ofAll", "HashMap.ofAll: entryMapper returned null", () -> HashMap.ofAll(java.util.stream.Stream.of(1), x -> null)));
        cases.add(throwing("HashMap.orElse", "HashMap.orElse: supplier returned null", () -> HashMap.<Integer, String> empty().orElse((Supplier<Iterable<Tuple2<Integer, String>>>) () -> null)));
        cases.add(throwing("HashMap.tabulate", "HashMap.tabulate: f returned null", () -> HashMap.tabulate(1, i -> null)));
        cases.add(throwing("HashMap.toLinkedMap", "Map.toLinkedMap: f returned null", () -> HashMap.of(1, "a").toLinkedMap(e -> null)));
        cases.add(throwing("HashMap.toMap", "Map.toMap: f returned null", () -> HashMap.of(1, "a").toMap(e -> null)));
        cases.add(throwing("HashMap.toSortedMap", "Map.toSortedMap: f returned null", () -> HashMap.of(1, "a").toSortedMap(e -> (Tuple2<Integer, Integer>) null)));

        cases.add(throwing("LinkedHashMap.collect", "LinkedHashMap.collect: mapper returned null", () -> LinkedHashMap.of(1, "a").collect((k, v) -> null)));
        cases.add(throwing("LinkedHashMap.fill", "LinkedHashMap.fill: s returned null", () -> LinkedHashMap.fill(1, () -> null)));
        cases.add(throwing("LinkedHashMap.flatMap", "LinkedHashMap.flatMap: mapper returned null", () -> LinkedHashMap.of(1, "a").flatMap((k, v) -> null)));
        cases.add(throwing("LinkedHashMap.groupBy", "LinkedHashMap.groupBy: classifier returned null", () -> LinkedHashMap.of(1, "a").groupBy(e -> null)));
        cases.add(throwing("LinkedHashMap.map", "LinkedHashMap.map: mapper returned null", () -> LinkedHashMap.of(1, "a").map((k, v) -> null)));
        cases.add(throwing("LinkedHashMap.ofAll", "LinkedHashMap.ofAll: entryMapper returned null", () -> LinkedHashMap.ofAll(java.util.stream.Stream.of(1), x -> null)));
        cases.add(throwing("LinkedHashMap.orElse", "LinkedHashMap.orElse: supplier returned null", () -> LinkedHashMap.<Integer, String> empty().orElse((Supplier<Iterable<Tuple2<Integer, String>>>) () -> null)));
        cases.add(throwing("LinkedHashMap.tabulate", "LinkedHashMap.tabulate: f returned null", () -> LinkedHashMap.tabulate(1, i -> null)));
        cases.add(throwing("LinkedHashMap.toLinkedMap", "Map.toLinkedMap: f returned null", () -> LinkedHashMap.of(1, "a").toLinkedMap(e -> null)));
        cases.add(throwing("LinkedHashMap.toMap", "Map.toMap: f returned null", () -> LinkedHashMap.of(1, "a").toMap(e -> null)));
        cases.add(throwing("LinkedHashMap.toSortedMap", "Map.toSortedMap: f returned null", () -> LinkedHashMap.of(1, "a").toSortedMap(e -> (Tuple2<Integer, Integer>) null)));

        cases.add(throwing("TreeMap.collect", "TreeMap.collect: mapper returned null", () -> TreeMap.of(1, "a").collect((k, v) -> null)));
        cases.add(throwing("TreeMap.fill", "TreeMap.fill: s returned null", () -> TreeMap.<Integer, String> fill(1, () -> null)));
        cases.add(throwing("TreeMap.fill", "TreeMap.fill: s returned null", () -> TreeMap.<Integer, String> fill(Comparator.naturalOrder(), 1, () -> null)));
        cases.add(throwing("TreeMap.flatMap", "TreeMap.flatMap: mapper returned null", () -> TreeMap.of(1, "a").flatMap((k, v) -> (Iterable<Tuple2<Integer, String>>) null)));
        cases.add(throwing("TreeMap.flatMap", "TreeMap.flatMap: mapper returned null", () -> TreeMap.of(1, "a").flatMap(Comparator.<Integer> naturalOrder(), (k, v) -> null)));
        cases.add(throwing("TreeMap.groupBy", "TreeMap.groupBy: classifier returned null", () -> TreeMap.of(1, "a").groupBy(e -> null)));
        cases.add(throwing("TreeMap.map", "TreeMap.map: mapper returned null", () -> TreeMap.of(1, "a").map((k, v) -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("TreeMap.map", "TreeMap.map: mapper returned null", () -> TreeMap.of(1, "a").map(Comparator.<Integer> naturalOrder(), (k, v) -> null)));
        cases.add(throwing("TreeMap.ofAll", "TreeMap.ofAll: entryMapper returned null", () -> TreeMap.ofAll(java.util.stream.Stream.of(1), x -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("TreeMap.ofAll", "TreeMap.ofAll: entryMapper returned null", () -> TreeMap.ofAll(Comparator.<Integer> naturalOrder(), java.util.stream.Stream.of(1), x -> null)));
        cases.add(throwing("TreeMap.orElse", "TreeMap.orElse: supplier returned null", () -> TreeMap.<Integer, String> empty().orElse((Supplier<Iterable<Tuple2<Integer, String>>>) () -> null)));
        cases.add(throwing("TreeMap.tabulate", "TreeMap.tabulate: f returned null", () -> TreeMap.<Integer, String> tabulate(1, i -> null)));
        cases.add(throwing("TreeMap.tabulate", "TreeMap.tabulate: f returned null", () -> TreeMap.<Integer, String> tabulate(Comparator.naturalOrder(), 1, i -> null)));
        cases.add(throwing("TreeMap.toLinkedMap", "Map.toLinkedMap: f returned null", () -> TreeMap.of(1, "a").toLinkedMap(e -> null)));
        cases.add(throwing("TreeMap.toMap", "Map.toMap: f returned null", () -> TreeMap.of(1, "a").toMap(e -> null)));
        cases.add(throwing("TreeMap.toSortedMap", "Map.toSortedMap: f returned null", () -> TreeMap.of(1, "a").toSortedMap(e -> (Tuple2<Integer, Integer>) null)));
        return cases;
    }

    @TestFactory
    @SuppressWarnings("unchecked")
    public java.util.List<DynamicTest> shouldRejectANullResultNamingTheMethod() {
        return cases().stream().map(c -> DynamicTest.dynamicTest(c.method() + " -> " + c.message(), () -> {
            if (c.failure()) {
                final Try<?> result = ((Supplier<Try<?>>) c.call()).get();
                assertThat(result.isFailure()).as(c.method()).isTrue();
                assertThat(result.getCause()).as(c.method()).isInstanceOf(NullPointerException.class).hasMessage(c.message());
            } else {
                assertThatNullPointerException().as(c.method()).isThrownBy((ThrowingCallable) c.call()).withMessage(c.message());
            }
        })).toList();
    }

    @Test
    public void shouldHaveARowForEveryMethodTakingAFunctionThatReturnsAZazrType() throws IOException, URISyntaxException {
        final java.util.Set<String> covered = new java.util.TreeSet<>();
        for (Case c : cases()) {
            covered.add(c.method());
        }
        final java.util.Set<String> found = methodsTakingAFunctionReturningAZazrType();
        final java.util.Set<String> missing = new java.util.TreeSet<>(found);
        missing.removeAll(covered);
        missing.removeAll(EXCLUDED.keySet());
        assertThat(missing).as("public methods taking a function whose result is a Zazr type, with no row in cases()").isEmpty();
        assertThat(found).as("the exclusions name methods the scan finds").containsAll(EXCLUDED.keySet());
    }

    @Test
    public void shouldReportTheSameFailureWhenALazyResultIsForcedAgain() {
        final Stream<Object> flatMapped = Stream.of(1, 2).flatMap(x -> x == 2 ? null : List.of(x));
        assertThat(flatMapped.head()).isEqualTo(1);
        assertThatNullPointerException().isThrownBy(flatMapped::length).withMessage("Stream.flatMap: mapper returned null");
        assertThatNullPointerException().isThrownBy(flatMapped::length).withMessage("Stream.flatMap: mapper returned null");

        final Stream<Integer> consed = Stream.cons(1, () -> null);
        assertThat(consed.head()).isEqualTo(1);
        assertThatNullPointerException().isThrownBy(consed::tail).withMessage("Stream.cons: tailSupplier returned null");
        assertThatNullPointerException().isThrownBy(consed::tail).withMessage("Stream.cons: tailSupplier returned null");

        final Lazy<Object> lazy = Lazy.of(() -> 1).flatMap(x -> null);
        assertThatNullPointerException().isThrownBy(lazy::get).withMessage("Lazy.flatMap: mapper returned null");
        assertThatNullPointerException().isThrownBy(lazy::get).withMessage("Lazy.flatMap: mapper returned null");
    }

    @Test
    public void shouldKeepTheReceiverUnchangedAfterANullResult() {
        final Vector<Integer> vector = Vector.of(1, 2, 3);
        assertThatNullPointerException().isThrownBy(() -> vector.flatMap(x -> x == 3 ? null : List.of(x)));
        assertThat(vector).containsExactly(1, 2, 3);
        final HashMap<Integer, String> map = HashMap.of(1, "a", 2, "b");
        assertThatNullPointerException().isThrownBy(() -> map.map((k, v) -> k == 2 ? null : Tuple.of(k, v)));
        assertThat(map).containsExactlyInAnyOrder(Tuple.of(1, "a"), Tuple.of(2, "b"));
    }

    @Test
    public void shouldKeepTheCauseWhenRecoverReturnsNull() {
        final IOException io = new IOException("io");
        final CheckedFunction1<Integer, Integer> failing = a -> { throw io; };
        assertThatNullPointerException().isThrownBy(() -> failing.recover(t -> null).apply(1)).withCause(io);
    }

    // -- the guard: every public method of an exported type taking a function whose result is a Zazr type

    private static java.util.Set<String> methodsTakingAFunctionReturningAZazrType() throws IOException, URISyntaxException {
        final Path root = Path.of(Option.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        final java.util.List<Class<?>> types = new ArrayList<>();
        for (String pkg : java.util.List.of("com/guizmaii/zazr", "com/guizmaii/zazr/collection", "com/guizmaii/zazr/control")) {
            try (java.util.stream.Stream<Path> files = Files.list(root.resolve(pkg))) {
                for (Path file : files.toList()) {
                    final String name = file.getFileName().toString();
                    if (name.endsWith(".class") && !name.contains("$") && !name.equals("package-info.class")) {
                        final Class<?> type = loadClass(pkg.replace('/', '.') + "." + name.substring(0, name.length() - ".class".length()));
                        if (Modifier.isPublic(type.getModifiers())) {
                            types.add(type);
                        }
                    }
                }
            }
        }
        assertThat(types).as("the exported types").contains(Option.class, Vector.class, Tuple2.class);
        // an interface another exported type implements (Traversable, Set, Map, ...) is covered through that type
        final java.util.Set<Class<?>> supertypes = new java.util.HashSet<>();
        for (Class<?> type : types) {
            for (Class<?> other : types) {
                if (other != type && type.isInterface() && type.isAssignableFrom(other)) {
                    supertypes.add(type);
                }
            }
        }
        final java.util.Set<String> found = new java.util.TreeSet<>();
        for (Class<?> type : types) {
            for (Method method : type.getMethods()) {
                final boolean isStatic = Modifier.isStatic(method.getModifiers());
                if (method.isBridge() || method.isSynthetic() || method.getDeclaringClass() == Object.class
                        || (isStatic && method.getDeclaringClass() != type) || (!isStatic && supertypes.contains(type))) {
                    continue;
                }
                for (Type parameter : method.getGenericParameterTypes()) {
                    if (returnsAZazrType(parameter)) {
                        found.add(type.getSimpleName() + "." + method.getName());
                        break;
                    }
                }
            }
        }
        return found;
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /** A parameter of a functional interface type whose single abstract method returns a Zazr type or an iterable. */
    private static boolean returnsAZazrType(Type parameter) {
        if (!(parameter instanceof ParameterizedType parameterized) || !(parameterized.getRawType() instanceof Class<?> raw)
                || !raw.isInterface() || !raw.isAnnotationPresent(FunctionalInterface.class)) {
            return false;
        }
        Method abstractMethod = null;
        for (Method method : raw.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                abstractMethod = method;
            }
        }
        if (abstractMethod == null) {
            return false;
        }
        Type result = abstractMethod.getGenericReturnType();
        if (result instanceof TypeVariable<?> variable && abstractMethod.getDeclaringClass() == raw) {
            final TypeVariable<?>[] variables = raw.getTypeParameters();
            for (int i = 0; i < variables.length; i++) {
                if (variables[i].getName().equals(variable.getName())) {
                    result = parameterized.getActualTypeArguments()[i];
                }
            }
        }
        final Class<?> resultClass = rawClassOf(result);
        return resultClass != null && (resultClass.getName().startsWith("com.guizmaii.zazr.") || resultClass == Iterable.class
                || resultClass == java.util.Iterator.class);
    }

    private static Class<?> rawClassOf(Type type) {
        return switch (type) {
            case Class<?> c -> c;
            case ParameterizedType p -> (Class<?>) p.getRawType();
            case WildcardType w -> rawClassOf(w.getUpperBounds()[0]);
            default -> null;
        };
    }
}
