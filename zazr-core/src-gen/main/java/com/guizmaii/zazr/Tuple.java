package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import com.guizmaii.zazr.collection.Vector;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The base interface of all tuples.
 *
 * @author Daniel Dietrich
 */
public interface Tuple {

    /**
     * The maximum arity of an Tuple.
     * <p>
     * Note: This value might be changed in a future version of Vavr.
     * So it is recommended to use this constant instead of hardcoding the current maximum arity.
     */
    int MAX_ARITY = 8;

    /**
     * Returns the number of elements of this tuple.
     *
     * @return the number of elements.
     */
    int arity();

    /**
     * Converts this tuple to a {@link Vector} of its components, in order (empty for {@code Tuple0}).
     *
     * @return a {@code Vector} of the components
     * @throws NullPointerException if a component is null (a {@code Vector} holds no null)
     */
    Vector<?> toVector();

    // -- factory methods

    /**
     * Creates the empty tuple.
     *
     * @return the empty tuple.
     */
    static Tuple0 empty() {
        return Tuple0.instance();
    }

    /**
     * Creates a {@code Tuple2} from a {@link Map.Entry}.
     *
     * @param <T1> Type of first component (entry key)
     * @param <T2> Type of second component (entry value)
     * @param      entry A {@link java.util.Map.Entry}
     * @return a new {@code Tuple2} containing key and value of the given {@code entry}
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<T1, T2> fromEntry(Map.Entry<? extends T1, ? extends T2> entry) {
        Objects.requireNonNull(entry, "entry is null");
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }

    /**
     * Creates a tuple of one element.
     *
     * @param <T1> type of the 1st element
     * @param t1 the 1st element
     * @return a tuple of one element.
     */
    static <T1 extends @Nullable Object> Tuple1<T1> of(T1 t1) {
        return new Tuple1<>(t1);
    }

    /**
     * Creates a tuple of two elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @return a tuple of two elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple2<>(t1, t2);
    }

    /**
     * Creates a tuple of three elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param <T3> type of the 3rd element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @param t3 the 3rd element
     * @return a tuple of three elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
        return new Tuple3<>(t1, t2, t3);
    }

    /**
     * Creates a tuple of 4 elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param <T3> type of the 3rd element
     * @param <T4> type of the 4th element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @param t3 the 3rd element
     * @param t4 the 4th element
     * @return a tuple of 4 elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Tuple4<T1, T2, T3, T4> of(T1 t1, T2 t2, T3 t3, T4 t4) {
        return new Tuple4<>(t1, t2, t3, t4);
    }

    /**
     * Creates a tuple of 5 elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param <T3> type of the 3rd element
     * @param <T4> type of the 4th element
     * @param <T5> type of the 5th element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @param t3 the 3rd element
     * @param t4 the 4th element
     * @param t5 the 5th element
     * @return a tuple of 5 elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Tuple5<T1, T2, T3, T4, T5> of(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return new Tuple5<>(t1, t2, t3, t4, t5);
    }

    /**
     * Creates a tuple of 6 elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param <T3> type of the 3rd element
     * @param <T4> type of the 4th element
     * @param <T5> type of the 5th element
     * @param <T6> type of the 6th element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @param t3 the 3rd element
     * @param t4 the 4th element
     * @param t5 the 5th element
     * @param t6 the 6th element
     * @return a tuple of 6 elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Tuple6<T1, T2, T3, T4, T5, T6> of(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return new Tuple6<>(t1, t2, t3, t4, t5, t6);
    }

    /**
     * Creates a tuple of 7 elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param <T3> type of the 3rd element
     * @param <T4> type of the 4th element
     * @param <T5> type of the 5th element
     * @param <T6> type of the 6th element
     * @param <T7> type of the 7th element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @param t3 the 3rd element
     * @param t4 the 4th element
     * @param t5 the 5th element
     * @param t6 the 6th element
     * @param t7 the 7th element
     * @return a tuple of 7 elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return new Tuple7<>(t1, t2, t3, t4, t5, t6, t7);
    }

    /**
     * Creates a tuple of 8 elements.
     *
     * @param <T1> type of the 1st element
     * @param <T2> type of the 2nd element
     * @param <T3> type of the 3rd element
     * @param <T4> type of the 4th element
     * @param <T5> type of the 5th element
     * @param <T6> type of the 6th element
     * @param <T7> type of the 7th element
     * @param <T8> type of the 8th element
     * @param t1 the 1st element
     * @param t2 the 2nd element
     * @param t3 the 3rd element
     * @param t4 the 4th element
     * @param t5 the 5th element
     * @param t6 the 6th element
     * @param t7 the 7th element
     * @param t8 the 8th element
     * @return a tuple of 8 elements.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        return new Tuple8<>(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Return the order-dependent hash of the one given value.
     *
     * @param o1 the 1st value to hash
     * @return the same result as {@link Objects#hashCode(Object)}
     */
    static int hash(@Nullable Object o1) {
        return Objects.hashCode(o1);
    }

    /**
     * Return the order-dependent hash of the two given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        return result;
    }

    /**
     * Return the order-dependent hash of the three given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @param o3 the 3rd value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2, @Nullable Object o3) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        return result;
    }

    /**
     * Return the order-dependent hash of the 4 given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @param o3 the 3rd value to hash
     * @param o4 the 4th value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2, @Nullable Object o3, @Nullable Object o4) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        result = 31 * result + hash(o4);
        return result;
    }

    /**
     * Return the order-dependent hash of the 5 given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @param o3 the 3rd value to hash
     * @param o4 the 4th value to hash
     * @param o5 the 5th value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2, @Nullable Object o3, @Nullable Object o4, @Nullable Object o5) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        result = 31 * result + hash(o4);
        result = 31 * result + hash(o5);
        return result;
    }

    /**
     * Return the order-dependent hash of the 6 given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @param o3 the 3rd value to hash
     * @param o4 the 4th value to hash
     * @param o5 the 5th value to hash
     * @param o6 the 6th value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2, @Nullable Object o3, @Nullable Object o4, @Nullable Object o5, @Nullable Object o6) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        result = 31 * result + hash(o4);
        result = 31 * result + hash(o5);
        result = 31 * result + hash(o6);
        return result;
    }

    /**
     * Return the order-dependent hash of the 7 given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @param o3 the 3rd value to hash
     * @param o4 the 4th value to hash
     * @param o5 the 5th value to hash
     * @param o6 the 6th value to hash
     * @param o7 the 7th value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2, @Nullable Object o3, @Nullable Object o4, @Nullable Object o5, @Nullable Object o6, @Nullable Object o7) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        result = 31 * result + hash(o4);
        result = 31 * result + hash(o5);
        result = 31 * result + hash(o6);
        result = 31 * result + hash(o7);
        return result;
    }

    /**
     * Return the order-dependent hash of the 8 given values.
     *
     * @param o1 the 1st value to hash
     * @param o2 the 2nd value to hash
     * @param o3 the 3rd value to hash
     * @param o4 the 4th value to hash
     * @param o5 the 5th value to hash
     * @param o6 the 6th value to hash
     * @param o7 the 7th value to hash
     * @param o8 the 8th value to hash
     * @return the same result as {@link Objects#hash(Object...)}
     */
    static int hash(@Nullable Object o1, @Nullable Object o2, @Nullable Object o3, @Nullable Object o4, @Nullable Object o5, @Nullable Object o6, @Nullable Object o7, @Nullable Object o8) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        result = 31 * result + hash(o4);
        result = 31 * result + hash(o5);
        result = 31 * result + hash(o6);
        result = 31 * result + hash(o7);
        result = 31 * result + hash(o8);
        return result;
    }

    /**
     * Narrows a widened {@code Tuple1<? extends T1>} to {@code Tuple1<T1>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple1}.
     * @param <T1> the 1st component type
     * @return the given {@code t} instance as narrowed type {@code Tuple1<T1>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object> Tuple1<T1> narrow(Tuple1<? extends T1> t) {
        return (Tuple1<T1>) t;
    }

    /**
     * Narrows a widened {@code Tuple2<? extends T1, ? extends T2>} to {@code Tuple2<T1, T2>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple2}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @return the given {@code t} instance as narrowed type {@code Tuple2<T1, T2>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<T1, T2> narrow(Tuple2<? extends T1, ? extends T2> t) {
        return (Tuple2<T1, T2>) t;
    }

    /**
     * Narrows a widened {@code Tuple3<? extends T1, ? extends T2, ? extends T3>} to {@code Tuple3<T1, T2, T3>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple3}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @param <T3> the 3rd component type
     * @return the given {@code t} instance as narrowed type {@code Tuple3<T1, T2, T3>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<T1, T2, T3> narrow(Tuple3<? extends T1, ? extends T2, ? extends T3> t) {
        return (Tuple3<T1, T2, T3>) t;
    }

    /**
     * Narrows a widened {@code Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4>} to {@code Tuple4<T1, T2, T3, T4>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple4}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @param <T3> the 3rd component type
     * @param <T4> the 4th component type
     * @return the given {@code t} instance as narrowed type {@code Tuple4<T1, T2, T3, T4>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Tuple4<T1, T2, T3, T4> narrow(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> t) {
        return (Tuple4<T1, T2, T3, T4>) t;
    }

    /**
     * Narrows a widened {@code Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5>} to {@code Tuple5<T1, T2, T3, T4, T5>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple5}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @param <T3> the 3rd component type
     * @param <T4> the 4th component type
     * @param <T5> the 5th component type
     * @return the given {@code t} instance as narrowed type {@code Tuple5<T1, T2, T3, T4, T5>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Tuple5<T1, T2, T3, T4, T5> narrow(Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> t) {
        return (Tuple5<T1, T2, T3, T4, T5>) t;
    }

    /**
     * Narrows a widened {@code Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6>} to {@code Tuple6<T1, T2, T3, T4, T5, T6>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple6}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @param <T3> the 3rd component type
     * @param <T4> the 4th component type
     * @param <T5> the 5th component type
     * @param <T6> the 6th component type
     * @return the given {@code t} instance as narrowed type {@code Tuple6<T1, T2, T3, T4, T5, T6>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Tuple6<T1, T2, T3, T4, T5, T6> narrow(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> t) {
        return (Tuple6<T1, T2, T3, T4, T5, T6>) t;
    }

    /**
     * Narrows a widened {@code Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7>} to {@code Tuple7<T1, T2, T3, T4, T5, T6, T7>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple7}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @param <T3> the 3rd component type
     * @param <T4> the 4th component type
     * @param <T5> the 5th component type
     * @param <T6> the 6th component type
     * @param <T7> the 7th component type
     * @return the given {@code t} instance as narrowed type {@code Tuple7<T1, T2, T3, T4, T5, T6, T7>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Tuple7<T1, T2, T3, T4, T5, T6, T7> narrow(Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7> t) {
        return (Tuple7<T1, T2, T3, T4, T5, T6, T7>) t;
    }

    /**
     * Narrows a widened {@code Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8>} to {@code Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>}.
     * This is eligible because immutable/read-only tuples are covariant.
     * @param t A {@code Tuple8}.
     * @param <T1> the 1st component type
     * @param <T2> the 2nd component type
     * @param <T3> the 3rd component type
     * @param <T4> the 4th component type
     * @param <T5> the 5th component type
     * @param <T6> the 6th component type
     * @param <T7> the 7th component type
     * @param <T8> the 8th component type
     * @return the given {@code t} instance as narrowed type {@code Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>}.
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> narrow(Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8> t) {
        return (Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>) t;
    }

    /**
     * Splits an iterable of {@code Tuple1} into a Tuple1 of {@link Vector}, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of one {@code Vector}.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object> Tuple1<Vector<T1>> unzip1(Iterable<? extends Tuple1<? extends T1>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        for (Tuple1<? extends T1> t : tuples) {
            b1.add(t._1());
        }
        return Tuple.of(b1.result());
    }

    /**
     * Splits an iterable of {@code Tuple2} into a Tuple2 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of two {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<Vector<T1>, Vector<T2>> unzip2(Iterable<? extends Tuple2<? extends T1, ? extends T2>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        for (Tuple2<? extends T1, ? extends T2> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
        }
        return Tuple.of(b1.result(), b2.result());
    }

    /**
     * Splits an iterable of {@code Tuple3} into a Tuple3 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param <T3> 3rd component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of three {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<Vector<T1>, Vector<T2>, Vector<T3>> unzip3(Iterable<? extends Tuple3<? extends T1, ? extends T2, ? extends T3>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        final Vector.Builder<T3> b3 = Vector.newBuilder();
        for (Tuple3<? extends T1, ? extends T2, ? extends T3> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
            b3.add(t._3());
        }
        return Tuple.of(b1.result(), b2.result(), b3.result());
    }

    /**
     * Splits an iterable of {@code Tuple4} into a Tuple4 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param <T3> 3rd component type
     * @param <T4> 4th component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of 4 {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Tuple4<Vector<T1>, Vector<T2>, Vector<T3>, Vector<T4>> unzip4(Iterable<? extends Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        final Vector.Builder<T3> b3 = Vector.newBuilder();
        final Vector.Builder<T4> b4 = Vector.newBuilder();
        for (Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
            b3.add(t._3());
            b4.add(t._4());
        }
        return Tuple.of(b1.result(), b2.result(), b3.result(), b4.result());
    }

    /**
     * Splits an iterable of {@code Tuple5} into a Tuple5 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param <T3> 3rd component type
     * @param <T4> 4th component type
     * @param <T5> 5th component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of 5 {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Tuple5<Vector<T1>, Vector<T2>, Vector<T3>, Vector<T4>, Vector<T5>> unzip5(Iterable<? extends Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        final Vector.Builder<T3> b3 = Vector.newBuilder();
        final Vector.Builder<T4> b4 = Vector.newBuilder();
        final Vector.Builder<T5> b5 = Vector.newBuilder();
        for (Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
            b3.add(t._3());
            b4.add(t._4());
            b5.add(t._5());
        }
        return Tuple.of(b1.result(), b2.result(), b3.result(), b4.result(), b5.result());
    }

    /**
     * Splits an iterable of {@code Tuple6} into a Tuple6 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param <T3> 3rd component type
     * @param <T4> 4th component type
     * @param <T5> 5th component type
     * @param <T6> 6th component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of 6 {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Tuple6<Vector<T1>, Vector<T2>, Vector<T3>, Vector<T4>, Vector<T5>, Vector<T6>> unzip6(Iterable<? extends Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        final Vector.Builder<T3> b3 = Vector.newBuilder();
        final Vector.Builder<T4> b4 = Vector.newBuilder();
        final Vector.Builder<T5> b5 = Vector.newBuilder();
        final Vector.Builder<T6> b6 = Vector.newBuilder();
        for (Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
            b3.add(t._3());
            b4.add(t._4());
            b5.add(t._5());
            b6.add(t._6());
        }
        return Tuple.of(b1.result(), b2.result(), b3.result(), b4.result(), b5.result(), b6.result());
    }

    /**
     * Splits an iterable of {@code Tuple7} into a Tuple7 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param <T3> 3rd component type
     * @param <T4> 4th component type
     * @param <T5> 5th component type
     * @param <T6> 6th component type
     * @param <T7> 7th component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of 7 {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Tuple7<Vector<T1>, Vector<T2>, Vector<T3>, Vector<T4>, Vector<T5>, Vector<T6>, Vector<T7>> unzip7(Iterable<? extends Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        final Vector.Builder<T3> b3 = Vector.newBuilder();
        final Vector.Builder<T4> b4 = Vector.newBuilder();
        final Vector.Builder<T5> b5 = Vector.newBuilder();
        final Vector.Builder<T6> b6 = Vector.newBuilder();
        final Vector.Builder<T7> b7 = Vector.newBuilder();
        for (Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
            b3.add(t._3());
            b4.add(t._4());
            b5.add(t._5());
            b6.add(t._6());
            b7.add(t._7());
        }
        return Tuple.of(b1.result(), b2.result(), b3.result(), b4.result(), b5.result(), b6.result(), b7.result());
    }

    /**
     * Splits an iterable of {@code Tuple8} into a Tuple8 of {@link Vector}s, one per
     * component, in one pass with one builder per component.
     *
     * @param <T1> 1st component type
     * @param <T2> 2nd component type
     * @param <T3> 3rd component type
     * @param <T4> 4th component type
     * @param <T5> 5th component type
     * @param <T6> 6th component type
     * @param <T7> 7th component type
     * @param <T8> 8th component type
     * @param tuples an {@code Iterable} of tuples
     * @return a tuple of 8 {@code Vector}s.
     * @throws NullPointerException if {@code tuples}, a tuple or a component is null (a {@code Vector} holds no null)
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Tuple8<Vector<T1>, Vector<T2>, Vector<T3>, Vector<T4>, Vector<T5>, Vector<T6>, Vector<T7>, Vector<T8>> unzip8(Iterable<? extends Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8>> tuples) {
        Objects.requireNonNull(tuples, "tuples is null");
        final Vector.Builder<T1> b1 = Vector.newBuilder();
        final Vector.Builder<T2> b2 = Vector.newBuilder();
        final Vector.Builder<T3> b3 = Vector.newBuilder();
        final Vector.Builder<T4> b4 = Vector.newBuilder();
        final Vector.Builder<T5> b5 = Vector.newBuilder();
        final Vector.Builder<T6> b6 = Vector.newBuilder();
        final Vector.Builder<T7> b7 = Vector.newBuilder();
        final Vector.Builder<T8> b8 = Vector.newBuilder();
        for (Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8> t : tuples) {
            b1.add(t._1());
            b2.add(t._2());
            b3.add(t._3());
            b4.add(t._4());
            b5.add(t._5());
            b6.add(t._6());
            b7.add(t._7());
            b8.add(t._8());
        }
        return Tuple.of(b1.result(), b2.result(), b3.result(), b4.result(), b5.result(), b6.result(), b7.result(), b8.result());
    }

}