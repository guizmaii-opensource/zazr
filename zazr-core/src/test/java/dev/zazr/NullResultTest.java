package dev.zazr;

import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.Stream;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import dev.zazr.control.Try;
import dev.zazr.control.Validation;
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
 * {@code Option}, an {@code Either}, a {@code Try}, a tuple, an iterable of elements, ...), a classifier's key or a
 * value a control type stores is rejected
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
            java.util.Map.entry("TreeMap.reduceOption", "a fold returns the caller's own result"),
            java.util.Map.entry("NonEmptyMap.fold", "a fold returns the caller's own result"),
            java.util.Map.entry("NonEmptyMap.reduce", "a fold returns the caller's own result"),
            java.util.Map.entry("NonEmptySortedMap.fold", "a fold returns the caller's own result"),
            java.util.Map.entry("NonEmptySortedMap.reduce", "a fold returns the caller's own result"));

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static java.util.List<Case> cases() {
        final java.util.List<Case> cases = new ArrayList<>();

        // -- Option
        cases.add(throwing("Option.collect(java.util.function.Function)", "Option.collect: mapper returned null", () -> Option.some(1).collect(x -> null)));
        cases.add(throwing("Option.flatMap(java.util.function.Function)", "Option.flatMap: mapper returned null", () -> Option.some(1).flatMap(x -> null)));
        cases.add(throwing("Option.forEach(java.lang.Iterable, java.util.function.Function)", "Option.forEach: mapper returned null", () -> Option.forEach(List.of(1), x -> null)));
        cases.add(throwing("Option.map", "Option.map: mapper returned null", () -> Option.some(1).map(x -> null)));
        cases.add(failure("Option.mapTry", "Option.mapTry: mapper returned null", () -> Option.some(1).mapTry(x -> null)));
        cases.add(throwing("Option.orElse(java.util.function.Supplier)", "Option.orElse: supplier returned null", () -> Option.none().orElse(() -> null)));
        cases.add(throwing("Option.toEither", "Option.toEither: leftSupplier returned null", () -> Option.none().toEither(() -> null)));
        cases.add(throwing("Option.toTry", "Option.toTry: ifEmpty returned null", () -> Option.none().toTry(() -> null)));
        cases.add(throwing("Option.toValidation", "Option.toValidation: invalidSupplier returned null", () -> Option.none().toValidation(() -> null)));
        cases.add(throwing("Option.when", "Option.when: supplier returned null", () -> Option.when(true, () -> null)));

        // -- Either
        cases.add(throwing("Either.filterOrElse", "Either.filterOrElse: zero returned null", () -> Either.right(1).filterOrElse(x -> false, x -> null)));
        cases.add(throwing("Either.flatMap(java.util.function.Function)", "Either.flatMap: mapper returned null", () -> Either.right(1).flatMap(x -> null)));
        cases.add(throwing("Either.forEach(java.lang.Iterable, java.util.function.Function)", "Either.forEach: mapper returned null", () -> Either.forEach(List.of(1), x -> null)));
        cases.add(throwing("Either.fromPredicate", "Either.fromPredicate: ifFalse returned null", () -> Either.fromPredicate(1, x -> false, x -> null)));
        cases.add(throwing("Either.map", "Either.map: mapper returned null", () -> Either.right(1).map(x -> null)));
        cases.add(throwing("Either.mapBoth", "Either.mapBoth: rightMapper returned null", () -> Either.right(1).mapBoth(l -> l, r -> null)));
        cases.add(throwing("Either.mapBoth", "Either.mapBoth: leftMapper returned null", () -> Either.left(1).mapBoth(l -> null, r -> r)));
        cases.add(throwing("Either.mapLeft", "Either.mapLeft: leftMapper returned null", () -> Either.left(1).mapLeft(x -> null)));
        cases.add(throwing("Either.orElse(java.util.function.Supplier)", "Either.orElse: supplier returned null", () -> Either.left(1).orElse(() -> null)));
        cases.add(throwing("Either.toTry", "Either.toTry: f returned null", () -> Either.left(1).toTry(x -> null)));

        // -- Try: the methods that run the function under Try return the exception as a Failure
        final Try<Integer> failed = Try.failure(new IllegalStateException("failed"));
        cases.add(failure("Try.catchAll", "Try.catchAll: f returned null", () -> failed.catchAll(e -> null)));
        cases.add(failure("Try.catchAllWith(java.util.function.Function)", "Try.catchAllWith: f returned null", () -> failed.catchAllWith(e -> null)));
        cases.add(failure("Try.catchSome", "Try.catchSome: f returned null", () -> failed.catchSome(IllegalStateException.class, e -> null)));
        cases.add(failure("Try.catchSomeWith(java.lang.Class, java.util.function.Function)", "Try.catchSomeWith: f returned null", () -> failed.catchSomeWith(IllegalStateException.class, e -> null)));
        cases.add(failure("Try.collect(java.util.function.Function)", "Try.collect: mapper returned null", () -> Try.success(1).collect(x -> null)));
        cases.add(failure("Try.filter", "Try.filter: ifFalse returned null", () -> Try.success(1).filter(x -> false, x -> null)));
        cases.add(failure("Try.flatMap(java.util.function.Function)", "Try.flatMap: mapper returned null", () -> Try.success(1).flatMap(x -> null)));
        cases.add(failure("Try.flatMapTry(dev.zazr.CheckedFunction1)", "Try.flatMapTry: mapper returned null", () -> Try.success(1).flatMapTry(x -> null)));
        cases.add(failure("Try.map", "Try.map: mapper returned null", () -> Try.success(1).map(x -> null)));
        cases.add(failure("Try.mapError", "Try.mapError: f returned null", () -> failed.mapError(e -> null)));
        cases.add(throwing("Try.forEach(java.lang.Iterable, java.util.function.Function)", "Try.forEach: mapper returned null", () -> Try.forEach(List.of(1), x -> null)));
        cases.add(throwing("Try.orElse(java.util.function.Supplier)", "Try.orElse: supplier returned null", () -> failed.orElse(() -> null)));

        // -- Validation
        cases.add(throwing("Validation.flatMap(java.util.function.Function)", "Validation.flatMap: f returned null", () -> Validation.valid(1).flatMap(x -> null)));
        cases.add(throwing("Validation.flatMapEither(java.util.function.Function)", "Validation.flatMapEither: f returned null", () -> Validation.valid(1).flatMapEither(x -> null)));
        cases.add(throwing("Validation.forEach(java.lang.Iterable, java.util.function.Function)", "Validation.forEach: f returned null", () -> Validation.forEach(List.of(1), x -> null)));
        cases.add(throwing("Validation.forEach(dev.zazr.collection.NonEmptyVector, java.util.function.Function)", "Validation.forEach: f returned null", () -> Validation.forEach(NonEmptyVector.of(1), x -> null)));
        cases.add(throwing("Validation.mapErrorAll(java.util.function.Function)", "Validation.mapErrorAll: f returned null", () -> Validation.invalid("e").mapErrorAll(e -> null)));
        cases.add(throwing("Validation.orElse(java.util.function.Supplier)", "Validation.orElse: that returned null", () -> Validation.invalid("e").orElse(() -> null)));
        cases.add(throwing("Validation.partition(java.lang.Iterable, java.util.function.Function)", "Validation.partition: f returned null", () -> Validation.partition(List.of(1), x -> null)));

        // -- Lazy
        cases.add(throwing("Lazy.flatMap(java.util.function.Function)", "Lazy.flatMap: mapper returned null", () -> Lazy.of(() -> 1).flatMap(x -> null).get()));

        // -- tuples
        cases.add(throwing("Tuple2.map(java.util.function.BiFunction)", "Tuple2.map: mapper returned null", () -> Tuple.of(1, 2).map((a, b) -> null)));
        cases.add(throwing("Tuple3.map(dev.zazr.Function3)", "Tuple3.map: mapper returned null", () -> Tuple.of(1, 2, 3).map((a, b, c) -> null)));
        cases.add(throwing("Tuple4.map(dev.zazr.Function4)", "Tuple4.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4).map((a, b, c, d) -> null)));
        cases.add(throwing("Tuple5.map(dev.zazr.Function5)", "Tuple5.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5).map((a, b, c, d, e) -> null)));
        cases.add(throwing("Tuple6.map(dev.zazr.Function6)", "Tuple6.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5, 6).map((a, b, c, d, e, f) -> null)));
        cases.add(throwing("Tuple7.map(dev.zazr.Function7)", "Tuple7.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5, 6, 7).map((a, b, c, d, e, f, g) -> null)));
        cases.add(throwing("Tuple8.map(dev.zazr.Function8)", "Tuple8.map: mapper returned null", () -> Tuple.of(1, 2, 3, 4, 5, 6, 7, 8).map((a, b, c, d, e, f, g, h) -> null)));

        // -- checked functions: recover returning null fails the composed function, with the throwable as the cause
        final IOException io = new IOException("io");
        cases.add(throwing("CheckedFunction1.recover", "CheckedFunction1.recover: recover returned null",
                () -> ((CheckedFunction1<Integer, Integer>) a -> { throw io; }).recover(t -> null).apply(1)));
        cases.add(throwing("CheckedFunction2.recover", "CheckedFunction2.recover: recover returned null",
                () -> ((CheckedFunction2<Integer, Integer, Integer>) (a, b) -> { throw io; }).recover(t -> null).apply(1, 2)));
        cases.add(throwing("CheckedFunction3.recover(java.util.function.Function)", "CheckedFunction3.recover: recover returned null",
                () -> ((CheckedFunction3<Integer, Integer, Integer, Integer>) (a, b, c) -> { throw io; }).recover(t -> null).apply(1, 2, 3)));
        cases.add(throwing("CheckedFunction4.recover(java.util.function.Function)", "CheckedFunction4.recover: recover returned null",
                () -> ((CheckedFunction4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4)));
        cases.add(throwing("CheckedFunction5.recover(java.util.function.Function)", "CheckedFunction5.recover: recover returned null",
                () -> ((CheckedFunction5<Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5)));
        cases.add(throwing("CheckedFunction6.recover(java.util.function.Function)", "CheckedFunction6.recover: recover returned null",
                () -> ((CheckedFunction6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5, 6)));
        cases.add(throwing("CheckedFunction7.recover(java.util.function.Function)", "CheckedFunction7.recover: recover returned null",
                () -> ((CheckedFunction7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f, g) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5, 6, 7)));
        cases.add(throwing("CheckedFunction8.recover(java.util.function.Function)", "CheckedFunction8.recover: recover returned null",
                () -> ((CheckedFunction8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f, g, h) -> { throw io; }).recover(t -> null).apply(1, 2, 3, 4, 5, 6, 7, 8)));

        // -- sequences
        cases.add(throwing("Vector.collect(java.util.function.Function)", "Vector.collect: mapper returned null", () -> Vector.of(1).collect(x -> null)));
        cases.add(throwing("Vector.flatMap(java.util.function.Function)", "Vector.flatMap: mapper returned null", () -> Vector.of(1).flatMap(x -> null)));
        cases.add(throwing("Vector.groupBy", "Vector.groupBy: classifier returned null", () -> Vector.of(1).groupBy(x -> null)));
        cases.add(throwing("Vector.orElse(java.util.function.Supplier)", "Vector.orElse: supplier returned null", () -> Vector.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("Vector.partitionMap(java.util.function.Function)", "Vector.partitionMap: f returned null", () -> Vector.of(1).partitionMap(x -> null)));
        cases.add(throwing("Vector.toLinkedMap(java.util.function.Function)", "Vector.toLinkedMap: f returned null", () -> Vector.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("Vector.toMap(java.util.function.Function)", "Vector.toMap: f returned null", () -> Vector.of(1).toMap(x -> null)));
        cases.add(throwing("Vector.toSortedMap(java.util.function.Function)", "Vector.toSortedMap: f returned null", () -> Vector.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("Vector.toSortedMap(java.util.Comparator, java.util.function.Function)", "Vector.toSortedMap: f returned null", () -> Vector.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("Vector.unfold(java.lang.Object, java.util.function.Function)", "Vector.unfold: f returned null", () -> Vector.unfold(1, x -> null)));
        cases.add(throwing("Vector.unfoldLeft(java.lang.Object, java.util.function.Function)", "Vector.unfoldLeft: f returned null", () -> Vector.unfoldLeft(1, x -> null)));
        cases.add(throwing("Vector.unfoldRight(java.lang.Object, java.util.function.Function)", "Vector.unfoldRight: f returned null", () -> Vector.unfoldRight(1, x -> null)));
        cases.add(throwing("Vector.unzip(java.util.function.Function)", "Vector.unzip: unzipper returned null", () -> Vector.of(1).unzip(x -> null)));
        cases.add(throwing("Vector.unzip3(java.util.function.Function)", "Vector.unzip3: unzipper returned null", () -> Vector.of(1).unzip3(x -> null)));

        cases.add(throwing("List.collect(java.util.function.Function)", "List.collect: mapper returned null", () -> List.of(1).collect(x -> null)));
        cases.add(throwing("List.flatMap(java.util.function.Function)", "List.flatMap: mapper returned null", () -> List.of(1).flatMap(x -> null)));
        cases.add(throwing("List.groupBy", "List.groupBy: classifier returned null", () -> List.of(1).groupBy(x -> null)));
        cases.add(throwing("List.orElse(java.util.function.Supplier)", "List.orElse: supplier returned null", () -> List.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("List.partitionMap(java.util.function.Function)", "List.partitionMap: f returned null", () -> List.of(1).partitionMap(x -> null)));
        cases.add(throwing("List.toLinkedMap(java.util.function.Function)", "List.toLinkedMap: f returned null", () -> List.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("List.toMap(java.util.function.Function)", "List.toMap: f returned null", () -> List.of(1).toMap(x -> null)));
        cases.add(throwing("List.toSortedMap(java.util.function.Function)", "List.toSortedMap: f returned null", () -> List.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("List.toSortedMap(java.util.Comparator, java.util.function.Function)", "List.toSortedMap: f returned null", () -> List.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("List.unfold(java.lang.Object, java.util.function.Function)", "List.unfold: f returned null", () -> List.unfold(1, x -> null)));
        cases.add(throwing("List.unfoldLeft(java.lang.Object, java.util.function.Function)", "List.unfoldLeft: f returned null", () -> List.unfoldLeft(1, x -> null)));
        cases.add(throwing("List.unfoldRight(java.lang.Object, java.util.function.Function)", "List.unfoldRight: f returned null", () -> List.unfoldRight(1, x -> null)));
        cases.add(throwing("List.unzip(java.util.function.Function)", "List.unzip: unzipper returned null", () -> List.of(1).unzip(x -> null)));
        cases.add(throwing("List.unzip3(java.util.function.Function)", "List.unzip3: unzipper returned null", () -> List.of(1).unzip3(x -> null)));

        cases.add(throwing("Queue.collect(java.util.function.Function)", "Queue.collect: mapper returned null", () -> Queue.of(1).collect(x -> null)));
        cases.add(throwing("Queue.flatMap(java.util.function.Function)", "Queue.flatMap: mapper returned null", () -> Queue.of(1).flatMap(x -> null)));
        cases.add(throwing("Queue.groupBy", "Queue.groupBy: classifier returned null", () -> Queue.of(1).groupBy(x -> null)));
        cases.add(throwing("Queue.orElse(java.util.function.Supplier)", "Queue.orElse: supplier returned null", () -> Queue.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("Queue.partitionMap(java.util.function.Function)", "Queue.partitionMap: f returned null", () -> Queue.of(1).partitionMap(x -> null)));
        cases.add(throwing("Queue.toLinkedMap(java.util.function.Function)", "Queue.toLinkedMap: f returned null", () -> Queue.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("Queue.toMap(java.util.function.Function)", "Queue.toMap: f returned null", () -> Queue.of(1).toMap(x -> null)));
        cases.add(throwing("Queue.toSortedMap(java.util.function.Function)", "Queue.toSortedMap: f returned null", () -> Queue.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("Queue.toSortedMap(java.util.Comparator, java.util.function.Function)", "Queue.toSortedMap: f returned null", () -> Queue.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("Queue.unfold(java.lang.Object, java.util.function.Function)", "Queue.unfold: f returned null", () -> Queue.unfold(1, x -> null)));
        cases.add(throwing("Queue.unfoldLeft(java.lang.Object, java.util.function.Function)", "Queue.unfoldLeft: f returned null", () -> Queue.unfoldLeft(1, x -> null)));
        cases.add(throwing("Queue.unfoldRight(java.lang.Object, java.util.function.Function)", "Queue.unfoldRight: f returned null", () -> Queue.unfoldRight(1, x -> null)));
        cases.add(throwing("Queue.unzip(java.util.function.Function)", "Queue.unzip: unzipper returned null", () -> Queue.of(1).unzip(x -> null)));
        cases.add(throwing("Queue.unzip3(java.util.function.Function)", "Queue.unzip3: unzipper returned null", () -> Queue.of(1).unzip3(x -> null)));

        // a Stream is lazy: the null is rejected when the element is reached
        cases.add(throwing("Stream.appendSelf(java.util.function.Function)", "Stream.appendSelf: mapper returned null", () -> Stream.of(1).appendSelf(s -> null).size()));
        cases.add(throwing("Stream.collect(java.util.function.Function)", "Stream.collect: mapper returned null", () -> Stream.of(1).collect(x -> null).size()));
        cases.add(throwing("Stream.cons(java.lang.Object, java.util.function.Supplier)", "Stream.cons: tailSupplier returned null", () -> Stream.cons(1, () -> null).tail()));
        cases.add(throwing("Stream.flatMap(java.util.function.Function)", "Stream.flatMap: mapper returned null", () -> Stream.of(1).flatMap(x -> null).size()));
        cases.add(throwing("Stream.groupBy", "Stream.groupBy: classifier returned null", () -> Stream.of(1).groupBy(x -> null)));
        cases.add(throwing("Stream.iterate(java.util.function.Supplier)", "Stream.iterate: supplier returned null", () -> Stream.iterate(() -> null).size()));
        cases.add(throwing("Stream.orElse(java.util.function.Supplier)", "Stream.orElse: supplier returned null", () -> Stream.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("Stream.partitionMap(java.util.function.Function)", "Stream.partitionMap: f returned null", () -> Stream.of(1).partitionMap(x -> null)._1().size()));
        cases.add(throwing("Stream.toLinkedMap(java.util.function.Function)", "Stream.toLinkedMap: f returned null", () -> Stream.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("Stream.toMap(java.util.function.Function)", "Stream.toMap: f returned null", () -> Stream.of(1).toMap(x -> null)));
        cases.add(throwing("Stream.toSortedMap(java.util.function.Function)", "Stream.toSortedMap: f returned null", () -> Stream.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("Stream.toSortedMap(java.util.Comparator, java.util.function.Function)", "Stream.toSortedMap: f returned null", () -> Stream.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("Stream.unfold(java.lang.Object, java.util.function.Function)", "Stream.unfold: f returned null", () -> Stream.unfold(1, x -> null).size()));
        cases.add(throwing("Stream.unfoldLeft(java.lang.Object, java.util.function.Function)", "Stream.unfoldLeft: f returned null", () -> Stream.unfoldLeft(1, x -> null).size()));
        cases.add(throwing("Stream.unfoldRight(java.lang.Object, java.util.function.Function)", "Stream.unfoldRight: f returned null", () -> Stream.unfoldRight(1, x -> null).size()));
        cases.add(throwing("Stream.unzip(java.util.function.Function)", "Stream.unzip: unzipper returned null", () -> Stream.of(1).unzip(x -> null)._1().size()));
        cases.add(throwing("Stream.unzip3(java.util.function.Function)", "Stream.unzip3: unzipper returned null", () -> Stream.of(1).unzip3(x -> null)._1().size()));

        cases.add(throwing("NonEmptyVector.collect(java.util.function.Function)", "NonEmptyVector.collect: mapper returned null", () -> NonEmptyVector.of(1).collect(x -> null)));
        cases.add(throwing("NonEmptyVector.flatMap(java.util.function.Function)", "NonEmptyVector.flatMap: mapper returned null", () -> NonEmptyVector.of(1).flatMap(x -> null)));
        cases.add(throwing("NonEmptyVector.flatMapAll(java.util.function.Function)", "NonEmptyVector.flatMapAll: mapper returned null", () -> NonEmptyVector.of(1).flatMapAll(x -> null)));
        cases.add(throwing("NonEmptyVector.groupBy", "NonEmptyVector.groupBy: classifier returned null", () -> NonEmptyVector.of(1).groupBy(x -> null)));
        cases.add(throwing("NonEmptyVector.partitionMap(java.util.function.Function)", "NonEmptyVector.partitionMap: f returned null", () -> NonEmptyVector.of(1).partitionMap(x -> null)));
        cases.add(throwing("NonEmptyVector.toLinkedMap(java.util.function.Function)", "NonEmptyVector.toLinkedMap: f returned null", () -> NonEmptyVector.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("NonEmptyVector.toMap(java.util.function.Function)", "NonEmptyVector.toMap: f returned null", () -> NonEmptyVector.of(1).toMap(x -> null)));
        cases.add(throwing("NonEmptyVector.toSortedMap(java.util.function.Function)", "NonEmptyVector.toSortedMap: f returned null", () -> NonEmptyVector.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptyVector.toSortedMap(java.util.Comparator, java.util.function.Function)", "NonEmptyVector.toSortedMap: f returned null", () -> NonEmptyVector.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("NonEmptyVector.unzip(java.util.function.Function)", "NonEmptyVector.unzip: unzipper returned null", () -> NonEmptyVector.of(1).unzip(x -> null)));
        cases.add(throwing("NonEmptyVector.unzip3(java.util.function.Function)", "NonEmptyVector.unzip3: unzipper returned null", () -> NonEmptyVector.of(1).unzip3(x -> null)));

        // -- sets: toMap and its siblings are the defaults of Set
        cases.add(throwing("HashSet.collect(java.util.function.Function)", "HashSet.collect: mapper returned null", () -> HashSet.of(1).collect(x -> null)));
        cases.add(throwing("HashSet.flatMap(java.util.function.Function)", "HashSet.flatMap: mapper returned null", () -> HashSet.of(1).flatMap(x -> null)));
        cases.add(throwing("HashSet.groupBy", "HashSet.groupBy: classifier returned null", () -> HashSet.of(1).groupBy(x -> null)));
        cases.add(throwing("HashSet.orElse(java.util.function.Supplier)", "HashSet.orElse: supplier returned null", () -> HashSet.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("HashSet.partitionMap(java.util.function.Function)", "HashSet.partitionMap: f returned null", () -> HashSet.of(1).partitionMap(x -> null)));
        cases.add(throwing("HashSet.toLinkedMap(java.util.function.Function)", "Set.toLinkedMap: f returned null", () -> HashSet.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("HashSet.toMap(java.util.function.Function)", "Set.toMap: f returned null", () -> HashSet.of(1).toMap(x -> null)));
        cases.add(throwing("HashSet.toSortedMap(java.util.function.Function)", "Set.toSortedMap: f returned null", () -> HashSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("HashSet.toSortedMap(java.util.Comparator, java.util.function.Function)", "Set.toSortedMap: f returned null", () -> HashSet.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));

        cases.add(throwing("LinkedHashSet.collect(java.util.function.Function)", "LinkedHashSet.collect: mapper returned null", () -> LinkedHashSet.of(1).collect(x -> null)));
        cases.add(throwing("LinkedHashSet.flatMap(java.util.function.Function)", "LinkedHashSet.flatMap: mapper returned null", () -> LinkedHashSet.of(1).flatMap(x -> null)));
        cases.add(throwing("LinkedHashSet.groupBy", "LinkedHashSet.groupBy: classifier returned null", () -> LinkedHashSet.of(1).groupBy(x -> null)));
        cases.add(throwing("LinkedHashSet.orElse(java.util.function.Supplier)", "LinkedHashSet.orElse: supplier returned null", () -> LinkedHashSet.empty().orElse((Supplier<Iterable<Object>>) () -> null)));
        cases.add(throwing("LinkedHashSet.partitionMap(java.util.function.Function)", "LinkedHashSet.partitionMap: f returned null", () -> LinkedHashSet.of(1).partitionMap(x -> null)));
        cases.add(throwing("LinkedHashSet.toLinkedMap(java.util.function.Function)", "Set.toLinkedMap: f returned null", () -> LinkedHashSet.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("LinkedHashSet.toMap(java.util.function.Function)", "Set.toMap: f returned null", () -> LinkedHashSet.of(1).toMap(x -> null)));
        cases.add(throwing("LinkedHashSet.toSortedMap(java.util.function.Function)", "Set.toSortedMap: f returned null", () -> LinkedHashSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("LinkedHashSet.toSortedMap(java.util.Comparator, java.util.function.Function)", "Set.toSortedMap: f returned null", () -> LinkedHashSet.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));

        cases.add(throwing("TreeSet.collect(java.util.function.Function)", "TreeSet.collect: mapper returned null", () -> TreeSet.of(1).collect(x -> null)));
        cases.add(throwing("TreeSet.collect(java.util.Comparator, java.util.function.Function)", "TreeSet.collect: mapper returned null", () -> TreeSet.of(1).collect(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("TreeSet.flatMap(java.util.function.Function)", "TreeSet.flatMap: mapper returned null", () -> TreeSet.of(1).flatMap(x -> (Iterable<Integer>) null)));
        cases.add(throwing("TreeSet.flatMap(java.util.Comparator, java.util.function.Function)", "TreeSet.flatMap: mapper returned null", () -> TreeSet.of(1).flatMap(Comparator.<Integer> naturalOrder(), x -> null)));
        cases.add(throwing("TreeSet.groupBy", "TreeSet.groupBy: classifier returned null", () -> TreeSet.of(1).groupBy(x -> null)));
        cases.add(throwing("TreeSet.orElse(java.util.function.Supplier)", "TreeSet.orElse: supplier returned null", () -> TreeSet.<Integer> empty().orElse((Supplier<Iterable<Integer>>) () -> null)));
        cases.add(throwing("TreeSet.toLinkedMap(java.util.function.Function)", "Set.toLinkedMap: f returned null", () -> TreeSet.of(1).toLinkedMap(x -> null)));
        cases.add(throwing("TreeSet.toMap(java.util.function.Function)", "Set.toMap: f returned null", () -> TreeSet.of(1).toMap(x -> null)));
        cases.add(throwing("TreeSet.toSortedMap(java.util.function.Function)", "Set.toSortedMap: f returned null", () -> TreeSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("TreeSet.toSortedMap(java.util.Comparator, java.util.function.Function)", "Set.toSortedMap: f returned null", () -> TreeSet.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));

        // -- maps: toMap and its siblings are the defaults of Map
        cases.add(throwing("HashMap.collect(java.util.function.BiFunction)", "HashMap.collect: mapper returned null", () -> HashMap.of(1, "a").collect((k, v) -> null)));
        cases.add(throwing("HashMap.fill(int, java.util.function.Supplier)", "HashMap.fill: s returned null", () -> HashMap.fill(1, () -> null)));
        cases.add(throwing("HashMap.flatMap(java.util.function.BiFunction)", "HashMap.flatMap: mapper returned null", () -> HashMap.of(1, "a").flatMap((k, v) -> null)));
        cases.add(throwing("HashMap.groupBy", "HashMap.groupBy: classifier returned null", () -> HashMap.of(1, "a").groupBy(e -> null)));
        cases.add(throwing("HashMap.map(java.util.function.BiFunction)", "HashMap.map: mapper returned null", () -> HashMap.of(1, "a").map((k, v) -> null)));
        cases.add(throwing("HashMap.ofAll(java.util.stream.Stream, java.util.function.Function)", "HashMap.ofAll: entryMapper returned null", () -> HashMap.ofAll(java.util.stream.Stream.of(1), x -> null)));
        cases.add(throwing("HashMap.orElse(java.util.function.Supplier)", "HashMap.orElse: supplier returned null", () -> HashMap.<Integer, String> empty().orElse((Supplier<Iterable<Tuple2<Integer, String>>>) () -> null)));
        cases.add(throwing("HashMap.tabulate(int, java.util.function.Function)", "HashMap.tabulate: f returned null", () -> HashMap.tabulate(1, i -> null)));
        cases.add(throwing("HashMap.toLinkedMap(java.util.function.Function)", "Map.toLinkedMap: f returned null", () -> HashMap.of(1, "a").toLinkedMap(e -> null)));
        cases.add(throwing("HashMap.toMap(java.util.function.Function)", "Map.toMap: f returned null", () -> HashMap.of(1, "a").toMap(e -> null)));
        cases.add(throwing("HashMap.toSortedMap(java.util.function.Function)", "Map.toSortedMap: f returned null", () -> HashMap.of(1, "a").toSortedMap(e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("HashMap.toSortedMap(java.util.Comparator, java.util.function.Function)", "Map.toSortedMap: f returned null", () -> HashMap.of(1, "a").toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));

        cases.add(throwing("LinkedHashMap.collect(java.util.function.BiFunction)", "LinkedHashMap.collect: mapper returned null", () -> LinkedHashMap.of(1, "a").collect((k, v) -> null)));
        cases.add(throwing("LinkedHashMap.fill(int, java.util.function.Supplier)", "LinkedHashMap.fill: s returned null", () -> LinkedHashMap.fill(1, () -> null)));
        cases.add(throwing("LinkedHashMap.flatMap(java.util.function.BiFunction)", "LinkedHashMap.flatMap: mapper returned null", () -> LinkedHashMap.of(1, "a").flatMap((k, v) -> null)));
        cases.add(throwing("LinkedHashMap.groupBy", "LinkedHashMap.groupBy: classifier returned null", () -> LinkedHashMap.of(1, "a").groupBy(e -> null)));
        cases.add(throwing("LinkedHashMap.map(java.util.function.BiFunction)", "LinkedHashMap.map: mapper returned null", () -> LinkedHashMap.of(1, "a").map((k, v) -> null)));
        cases.add(throwing("LinkedHashMap.ofAll(java.util.stream.Stream, java.util.function.Function)", "LinkedHashMap.ofAll: entryMapper returned null", () -> LinkedHashMap.ofAll(java.util.stream.Stream.of(1), x -> null)));
        cases.add(throwing("LinkedHashMap.orElse(java.util.function.Supplier)", "LinkedHashMap.orElse: supplier returned null", () -> LinkedHashMap.<Integer, String> empty().orElse((Supplier<Iterable<Tuple2<Integer, String>>>) () -> null)));
        cases.add(throwing("LinkedHashMap.tabulate(int, java.util.function.Function)", "LinkedHashMap.tabulate: f returned null", () -> LinkedHashMap.tabulate(1, i -> null)));
        cases.add(throwing("LinkedHashMap.toLinkedMap(java.util.function.Function)", "Map.toLinkedMap: f returned null", () -> LinkedHashMap.of(1, "a").toLinkedMap(e -> null)));
        cases.add(throwing("LinkedHashMap.toMap(java.util.function.Function)", "Map.toMap: f returned null", () -> LinkedHashMap.of(1, "a").toMap(e -> null)));
        cases.add(throwing("LinkedHashMap.toSortedMap(java.util.function.Function)", "Map.toSortedMap: f returned null", () -> LinkedHashMap.of(1, "a").toSortedMap(e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("LinkedHashMap.toSortedMap(java.util.Comparator, java.util.function.Function)", "Map.toSortedMap: f returned null", () -> LinkedHashMap.of(1, "a").toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));

        cases.add(throwing("TreeMap.collect(java.util.function.BiFunction)", "TreeMap.collect: mapper returned null", () -> TreeMap.of(1, "a").collect((k, v) -> null)));
        cases.add(throwing("TreeMap.collect(java.util.Comparator, java.util.function.BiFunction)", "TreeMap.collect: mapper returned null", () -> TreeMap.of(1, "a").collect(Comparator.<Integer> naturalOrder(), (k, v) -> null)));
        cases.add(throwing("TreeMap.fill(int, java.util.function.Supplier)", "TreeMap.fill: s returned null", () -> TreeMap.<Integer, String> fill(1, () -> null)));
        cases.add(throwing("TreeMap.fill(java.util.Comparator, int, java.util.function.Supplier)", "TreeMap.fill: s returned null", () -> TreeMap.<Integer, String> fill(Comparator.naturalOrder(), 1, () -> null)));
        cases.add(throwing("TreeMap.flatMap(java.util.function.BiFunction)", "TreeMap.flatMap: mapper returned null", () -> TreeMap.of(1, "a").flatMap((k, v) -> (Iterable<Tuple2<Integer, String>>) null)));
        cases.add(throwing("TreeMap.flatMap(java.util.Comparator, java.util.function.BiFunction)", "TreeMap.flatMap: mapper returned null", () -> TreeMap.of(1, "a").flatMap(Comparator.<Integer> naturalOrder(), (k, v) -> null)));
        cases.add(throwing("TreeMap.groupBy", "TreeMap.groupBy: classifier returned null", () -> TreeMap.of(1, "a").groupBy(e -> null)));
        cases.add(throwing("TreeMap.map(java.util.function.BiFunction)", "TreeMap.map: mapper returned null", () -> TreeMap.of(1, "a").map((k, v) -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("TreeMap.map(java.util.Comparator, java.util.function.BiFunction)", "TreeMap.map: mapper returned null", () -> TreeMap.of(1, "a").map(Comparator.<Integer> naturalOrder(), (k, v) -> null)));
        cases.add(throwing("TreeMap.ofAll(java.util.stream.Stream, java.util.function.Function)", "TreeMap.ofAll: entryMapper returned null", () -> TreeMap.ofAll(java.util.stream.Stream.of(1), x -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("TreeMap.ofAll(java.util.Comparator, java.util.stream.Stream, java.util.function.Function)", "TreeMap.ofAll: entryMapper returned null", () -> TreeMap.ofAll(Comparator.<Integer> naturalOrder(), java.util.stream.Stream.of(1), x -> null)));
        cases.add(throwing("TreeMap.orElse(java.util.function.Supplier)", "TreeMap.orElse: supplier returned null", () -> TreeMap.<Integer, String> empty().orElse((Supplier<Iterable<Tuple2<Integer, String>>>) () -> null)));
        cases.add(throwing("TreeMap.tabulate(int, java.util.function.Function)", "TreeMap.tabulate: f returned null", () -> TreeMap.<Integer, String> tabulate(1, i -> null)));
        cases.add(throwing("TreeMap.tabulate(java.util.Comparator, int, java.util.function.Function)", "TreeMap.tabulate: f returned null", () -> TreeMap.<Integer, String> tabulate(Comparator.naturalOrder(), 1, i -> null)));
        cases.add(throwing("TreeMap.toLinkedMap(java.util.function.Function)", "Map.toLinkedMap: f returned null", () -> TreeMap.of(1, "a").toLinkedMap(e -> null)));
        cases.add(throwing("TreeMap.toMap(java.util.function.Function)", "Map.toMap: f returned null", () -> TreeMap.of(1, "a").toMap(e -> null)));
        cases.add(throwing("TreeMap.toSortedMap(java.util.function.Function)", "Map.toSortedMap: f returned null", () -> TreeMap.of(1, "a").toSortedMap(e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("TreeMap.toSortedMap(java.util.Comparator, java.util.function.Function)", "Map.toSortedMap: f returned null", () -> TreeMap.of(1, "a").toSortedMap(Comparator.<Integer> naturalOrder(), x -> null)));
        // -- the non-empty sets and maps
        cases.add(throwing("NonEmptyMap.collect(java.util.function.BiFunction)", "NonEmptyMap.collect: mapper returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).collect((k, v) -> (Option<Tuple2<Integer, String>>) null)));
        cases.add(throwing("NonEmptyMap.flatMap(java.util.function.BiFunction)", "NonEmptyMap.flatMap: mapper returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).flatMap((k, v) -> (NonEmptyMap<Integer, String>) null)));
        cases.add(throwing("NonEmptyMap.flatMapAll(java.util.function.BiFunction)", "NonEmptyMap.flatMapAll: mapper returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).flatMapAll((k, v) -> (Iterable<Tuple2<Integer, String>>) null)));
        cases.add(throwing("NonEmptyMap.map(java.util.function.BiFunction)", "NonEmptyMap.map: mapper returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).map((k, v) -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptyMap.toLinkedMap(java.util.function.Function)", "NonEmptyMap.toLinkedMap: f returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).toLinkedMap(e -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptyMap.toMap(java.util.function.Function)", "NonEmptyMap.toMap: f returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).toMap(e -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptyMap.toSortedMap(java.util.function.Function)", "NonEmptyMap.toSortedMap: f returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).toSortedMap(e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptyMap.toSortedMap(java.util.Comparator, java.util.function.Function)", "NonEmptyMap.toSortedMap: f returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).toSortedMap(Comparator.<Integer> naturalOrder(), e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptyMap.groupBy", "NonEmptyMap.groupBy: classifier returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).groupBy(e -> null)));
        cases.add(throwing("NonEmptyMap.arrangeBy", "NonEmptyMap.arrangeBy: getKey returned null", () -> NonEmptyMap.of(Tuple.of(1, "a")).arrangeBy(e -> null)));
        cases.add(throwing("NonEmptySortedMap.collect(java.util.function.BiFunction)", "NonEmptySortedMap.collect: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).collect((k, v) -> (Option<Tuple2<Integer, String>>) null)));
        cases.add(throwing("NonEmptySortedMap.flatMap(java.util.function.BiFunction)", "NonEmptySortedMap.flatMap: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).flatMap((k, v) -> (NonEmptySortedMap<Integer, String>) null)));
        cases.add(throwing("NonEmptySortedMap.flatMapAll(java.util.function.BiFunction)", "NonEmptySortedMap.flatMapAll: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).flatMapAll((k, v) -> (Iterable<Tuple2<Integer, String>>) null)));
        cases.add(throwing("NonEmptySortedMap.map(java.util.function.BiFunction)", "NonEmptySortedMap.map: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).map((k, v) -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptySortedMap.toLinkedMap(java.util.function.Function)", "NonEmptySortedMap.toLinkedMap: f returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).toLinkedMap(e -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptySortedMap.toMap(java.util.function.Function)", "NonEmptySortedMap.toMap: f returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).toMap(e -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptySortedMap.toSortedMap(java.util.function.Function)", "NonEmptySortedMap.toSortedMap: f returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).toSortedMap(e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedMap.toSortedMap(java.util.Comparator, java.util.function.Function)", "NonEmptySortedMap.toSortedMap: f returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).toSortedMap(Comparator.<Integer> naturalOrder(), e -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedMap.groupBy", "NonEmptySortedMap.groupBy: classifier returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).groupBy(e -> null)));
        cases.add(throwing("NonEmptySortedMap.arrangeBy", "NonEmptySortedMap.arrangeBy: getKey returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).arrangeBy(e -> null)));
        cases.add(throwing("NonEmptySortedMap.collect(java.util.Comparator, java.util.function.BiFunction)", "NonEmptySortedMap.collect: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).collect(Comparator.<Integer> naturalOrder(), (k, v) -> (Option<Tuple2<Integer, String>>) null)));
        cases.add(throwing("NonEmptySortedMap.flatMap(java.util.Comparator, java.util.function.BiFunction)", "NonEmptySortedMap.flatMap: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).flatMap(Comparator.<Integer> naturalOrder(), (k, v) -> (NonEmptySortedMap<Integer, String>) null)));
        cases.add(throwing("NonEmptySortedMap.flatMapAll(java.util.Comparator, java.util.function.BiFunction)", "NonEmptySortedMap.flatMapAll: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).flatMapAll(Comparator.<Integer> naturalOrder(), (k, v) -> (Iterable<Tuple2<Integer, String>>) null)));
        cases.add(throwing("NonEmptySortedMap.map(java.util.Comparator, java.util.function.BiFunction)", "NonEmptySortedMap.map: mapper returned null", () -> NonEmptySortedMap.of(Tuple.of(1, "a")).map(Comparator.<Integer> naturalOrder(), (k, v) -> (Tuple2<Integer, String>) null)));
        cases.add(throwing("NonEmptySet.collect(java.util.function.Function)", "NonEmptySet.collect: mapper returned null", () -> NonEmptySet.of(1).collect(x -> (Option<Integer>) null)));
        cases.add(throwing("NonEmptySet.flatMap(java.util.function.Function)", "NonEmptySet.flatMap: mapper returned null", () -> NonEmptySet.of(1).flatMap(x -> (NonEmptySet<Integer>) null)));
        cases.add(throwing("NonEmptySet.flatMapAll(java.util.function.Function)", "NonEmptySet.flatMapAll: mapper returned null", () -> NonEmptySet.of(1).flatMapAll(x -> (Iterable<Integer>) null)));
        cases.add(throwing("NonEmptySet.toLinkedMap(java.util.function.Function)", "NonEmptySet.toLinkedMap: f returned null", () -> NonEmptySet.of(1).toLinkedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySet.toMap(java.util.function.Function)", "NonEmptySet.toMap: f returned null", () -> NonEmptySet.of(1).toMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySet.toSortedMap(java.util.function.Function)", "NonEmptySet.toSortedMap: f returned null", () -> NonEmptySet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySet.toSortedMap(java.util.Comparator, java.util.function.Function)", "NonEmptySet.toSortedMap: f returned null", () -> NonEmptySet.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySet.groupBy", "NonEmptySet.groupBy: classifier returned null", () -> NonEmptySet.of(1).groupBy(x -> null)));
        cases.add(throwing("NonEmptySet.arrangeBy", "NonEmptySet.arrangeBy: getKey returned null", () -> NonEmptySet.of(1).arrangeBy(x -> null)));
        cases.add(throwing("NonEmptySortedSet.collect(java.util.function.Function)", "NonEmptySortedSet.collect: mapper returned null", () -> NonEmptySortedSet.of(1).collect(x -> (Option<Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.flatMap(java.util.function.Function)", "NonEmptySortedSet.flatMap: mapper returned null", () -> NonEmptySortedSet.of(1).flatMap(x -> (NonEmptySortedSet<Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.flatMapAll(java.util.function.Function)", "NonEmptySortedSet.flatMapAll: mapper returned null", () -> NonEmptySortedSet.of(1).flatMapAll(x -> (Iterable<Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.toLinkedMap(java.util.function.Function)", "NonEmptySortedSet.toLinkedMap: f returned null", () -> NonEmptySortedSet.of(1).toLinkedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.toMap(java.util.function.Function)", "NonEmptySortedSet.toMap: f returned null", () -> NonEmptySortedSet.of(1).toMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.toSortedMap(java.util.function.Function)", "NonEmptySortedSet.toSortedMap: f returned null", () -> NonEmptySortedSet.of(1).toSortedMap(x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.toSortedMap(java.util.Comparator, java.util.function.Function)", "NonEmptySortedSet.toSortedMap: f returned null", () -> NonEmptySortedSet.of(1).toSortedMap(Comparator.<Integer> naturalOrder(), x -> (Tuple2<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.groupBy", "NonEmptySortedSet.groupBy: classifier returned null", () -> NonEmptySortedSet.of(1).groupBy(x -> null)));
        cases.add(throwing("NonEmptySortedSet.arrangeBy", "NonEmptySortedSet.arrangeBy: getKey returned null", () -> NonEmptySortedSet.of(1).arrangeBy(x -> null)));
        cases.add(throwing("NonEmptySet.partitionMap(java.util.function.Function)", "NonEmptySet.partitionMap: f returned null", () -> NonEmptySet.of(1).partitionMap(x -> (Either<Integer, Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.collect(java.util.Comparator, java.util.function.Function)", "NonEmptySortedSet.collect: mapper returned null", () -> NonEmptySortedSet.of(1).collect(Comparator.<Integer> naturalOrder(), x -> (Option<Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.flatMap(java.util.Comparator, java.util.function.Function)", "NonEmptySortedSet.flatMap: mapper returned null", () -> NonEmptySortedSet.of(1).flatMap(Comparator.<Integer> naturalOrder(), x -> (NonEmptySortedSet<Integer>) null)));
        cases.add(throwing("NonEmptySortedSet.flatMapAll(java.util.Comparator, java.util.function.Function)", "NonEmptySortedSet.flatMapAll: mapper returned null", () -> NonEmptySortedSet.of(1).flatMapAll(Comparator.<Integer> naturalOrder(), x -> (Iterable<Integer>) null)));

        // arrangeBy classifies through groupBy, and names itself
        cases.add(throwing("Vector.arrangeBy", "Vector.arrangeBy: getKey returned null", () -> Vector.of(1).arrangeBy(x -> null)));
        cases.add(throwing("List.arrangeBy", "List.arrangeBy: getKey returned null", () -> List.of(1).arrangeBy(x -> null)));
        cases.add(throwing("Queue.arrangeBy", "Queue.arrangeBy: getKey returned null", () -> Queue.of(1).arrangeBy(x -> null)));
        cases.add(throwing("Stream.arrangeBy", "Stream.arrangeBy: getKey returned null", () -> Stream.of(1).arrangeBy(x -> null)));
        cases.add(throwing("NonEmptyVector.arrangeBy", "NonEmptyVector.arrangeBy: getKey returned null", () -> NonEmptyVector.of(1).arrangeBy(x -> null)));
        cases.add(throwing("HashSet.arrangeBy", "Set.arrangeBy: getKey returned null", () -> HashSet.of(1).arrangeBy(x -> null)));
        cases.add(throwing("TreeSet.arrangeBy", "Set.arrangeBy: getKey returned null", () -> TreeSet.of(1).arrangeBy(x -> null)));
        cases.add(throwing("HashMap.arrangeBy", "Map.arrangeBy: getKey returned null", () -> HashMap.of(1, "a").arrangeBy(e -> null)));
        cases.add(throwing("TreeMap.arrangeBy", "Map.arrangeBy: getKey returned null", () -> TreeMap.of(1, "a").arrangeBy(e -> null)));
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
        missing.removeIf(signature -> EXCLUDED.containsKey(signature.substring(0, signature.indexOf('('))));
        assertThat(missing).as("public methods taking a function whose result is a Zazr type, with no row in cases()").isEmpty();
        assertThat(found.stream().map(signature -> signature.substring(0, signature.indexOf('('))).toList())
                .as("the exclusions name methods the scan finds").containsAll(EXCLUDED.keySet());
    }

    @Test
    public void shouldReportTheSameFailureWhenALazyResultIsForcedAgain() {
        final Stream<Object> flatMapped = Stream.of(1, 2).flatMap(x -> x == 2 ? null : List.of(x));
        assertThat(flatMapped.head()).isEqualTo(1);
        assertThatNullPointerException().isThrownBy(flatMapped::size).withMessage("Stream.flatMap: mapper returned null");
        assertThatNullPointerException().isThrownBy(flatMapped::size).withMessage("Stream.flatMap: mapper returned null");

        // a stateful supplier: forcing again must not ask it for the value after the null and drop the rejected one
        final java.util.Iterator<Option<Integer>> supplied = java.util.Arrays.asList(Option.some(1), null, Option.some(3), Option.<Integer> none()).iterator();
        final Stream<Integer> iterated = Stream.iterate(supplied::next);
        assertThatNullPointerException().isThrownBy(iterated::toVector).withMessage("Stream.iterate: supplier returned null");
        assertThatNullPointerException().isThrownBy(iterated::toVector).withMessage("Stream.iterate: supplier returned null");
        assertThat(iterated.head()).isEqualTo(1);

        // stateful functions that return null once: the failure is remembered, the function is not called again
        final java.util.concurrent.atomic.AtomicInteger unfoldCalls = new java.util.concurrent.atomic.AtomicInteger();
        final Stream<Integer> unfolded = Stream.unfoldRight(0, x -> x > 4 ? Option.none()
                : x == 2 && unfoldCalls.getAndIncrement() == 0 ? null : Option.some(Tuple.of(x, x + 1)));
        for (int i = 0; i < 3; i++) {
            assertThatNullPointerException().isThrownBy(unfolded::toVector).withMessage("Stream.unfoldRight: f returned null");
        }
        assertThat(unfoldCalls.get()).isEqualTo(1);
        final java.util.concurrent.atomic.AtomicInteger appendCalls = new java.util.concurrent.atomic.AtomicInteger();
        final Stream<Integer> appended = Stream.of(1, 2).appendSelf(self -> appendCalls.getAndIncrement() == 0 ? null : Stream.of(9));
        for (int i = 0; i < 3; i++) {
            assertThatNullPointerException().isThrownBy(appended::toVector).withMessage("Stream.appendSelf: mapper returned null");
        }
        assertThat(appendCalls.get()).isEqualTo(1);

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
        for (String pkg : java.util.List.of("dev/zazr", "dev/zazr/collection", "dev/zazr/control")) {
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
                        found.add(signature(type, method));
                        break;
                    }
                }
            }
        }
        return found;
    }

    /**
     * {@code Type.method(pkg.Param1, pkg.Param2)}, the erased parameter types fully qualified: one key per overload,
     * even where a Zazr type and a JDK type share a simple name ({@code Stream}, {@code List}, {@code Map}).
     */
    private static String signature(Class<?> type, Method method) {
        final java.util.StringJoiner parameters = new java.util.StringJoiner(", ", "(", ")");
        for (Class<?> parameter : method.getParameterTypes()) {
            parameters.add(parameter.getTypeName());
        }
        return type.getSimpleName() + "." + method.getName() + parameters;
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
        return resultClass != null && (resultClass.getName().startsWith("dev.zazr.") || resultClass == Iterable.class
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
