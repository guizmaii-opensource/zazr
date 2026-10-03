package dev.zazr.collection.internal;

import dev.zazr.collection.LazyList;
import dev.zazr.collection.Queue;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// The one implementation of [LazyList], ported from the evaluation model of the `LazyList` of the Scala 2.13
/// collections library, which Scala 3 uses unchanged: a cell is a lazily evaluated state, either empty or a head and a
/// tail, computed on the first read of [#isEmpty()], [#head()] or [#tail()] and kept. The tail is itself a cell, not
/// evaluated until it is read.
///
/// Unlike Scala, a failed evaluation is kept too: every later read throws the same exception, so a list read from a
/// one-shot source never skips or reorders an element. Only a [VirtualMachineError] is not kept, and the next read
/// evaluates the cell again. A cell whose evaluation needs its own state fails with [IllegalStateException] instead of
/// overflowing the stack. The evaluation runs under the lock of the cell, so it happens once whatever the number of
/// threads reading it.
public abstract class LazyCell<T extends @Nullable Object> implements LazyList<T> {

    // the state of a cell being evaluated: seeing it again on the thread evaluating it means the cell needs itself
    private static final Object EVALUATING = new Object();
    // the state of an evaluated empty cell
    private static final Object EMPTY = new Object();
    // the state of an evaluated non-empty cell: head and tail are set
    private static final Object CONS = new Object();

    private static final LazyCell<?> EMPTY_LIST = new Evaluated<>(EMPTY, null, null);

    // set with the state, before it, and read only once the state says CONS
    private @Nullable T head;
    private @Nullable LazyList<T> tail;

    // null until evaluated; then EMPTY, CONS, or the Throwable the evaluation threw. Written only under the lock of
    // this
    // cell, and never changed once it is one of those.
    private volatile @Nullable Object state;

    LazyCell() {}

    LazyCell(Object state, @Nullable T head, @Nullable LazyList<T> tail) {
        this.head = head;
        this.tail = tail;
        this.state = state;
    }

    // -- construction

    /// The evaluated empty list, the same instance every time.
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> LazyList<T> empty() {
        return (LazyList<T>) EMPTY_LIST;
    }

    /// An evaluated cell of `head`, which must not be null, then `tail`, not evaluated by this call.
    public static <T extends @Nullable Object> LazyList<T> cons(T head, LazyList<T> tail) {
        return new Evaluated<>(CONS, Objects.requireNonNull(head, "LazyList: element is null"), tail);
    }

    /// A list whose state is the state of the list `supplier` returns, evaluated on the first read.
    public static <T extends @Nullable Object> LazyList<T> defer(
            Supplier<? extends LazyList<? extends T>> supplier, String nullResult) {
        return new Deferred<>(supplier, nullResult);
    }

    /// The elements of `iterator`, each read when the list reaches it. A null element fails the read that meets it.
    public static <T extends @Nullable Object> LazyList<T> ofIterator(java.util.Iterator<? extends T> iterator) {
        return new FromIterator<>(iterator);
    }

    /// Whether `list` is already known to be empty, without evaluating anything.
    public static boolean knownIsEmpty(LazyList<?> list) {
        return list instanceof LazyCell<?> cell && cell.state == EMPTY;
    }

    /// Whether the state of `list` is already evaluated (empty, non-empty or failed), without evaluating anything.
    public static boolean isEvaluated(LazyList<?> list) {
        if (list instanceof LazyCell<?> cell) {
            Object current = cell.state;
            return current != null && current != EVALUATING;
        } else {
            return true;
        }
    }

    /// The elements of `list`, then `element`: O(1), nothing is evaluated.
    public static <T extends @Nullable Object> LazyList<T> append(LazyList<T> list, T element) {
        LazyList<T> last = cons(element, empty());
        return knownIsEmpty(list) ? last : new Appended<>(list, Queue.of(last));
    }

    /// The elements of `list`, then those of `that`: O(1), nothing is evaluated.
    public static <T extends @Nullable Object> LazyList<T> appendAll(LazyList<T> list, LazyList<T> that) {
        if (knownIsEmpty(that)) {
            return list;
        } else if (knownIsEmpty(list)) {
            return that;
        } else {
            return new Appended<>(list, Queue.of(that));
        }
    }

    /// The elements of `list` without its first `n > 0`, skipped when the result is first read. Dropping from
    /// a list that is itself such a drop, not read yet, makes one drop of the sum instead of a drop of a drop,
    /// so a chain of drops built without reading is read at the stack depth of one.
    public static <T extends @Nullable Object> LazyList<T> drop(LazyList<T> list, int n) {
        return new Dropped<>(list, n);
    }

    // -- evaluation

    /// Computes the state: sets head and tail and returns CONS, or returns EMPTY. Called under the lock of this cell,
    /// once, or again only after a [VirtualMachineError] left the cell as it was.
    abstract Object compute();

    /// Lets go of what [#compute()] needed, once its outcome is kept.
    void release() {}

    final void set(T head, LazyList<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    // the state of the evaluated list, or what evaluating it threw
    @SuppressWarnings("unchecked")
    final Object adopt(LazyList<? extends T> list) {
        if (list instanceof LazyCell<? extends T> cell) {
            Object outcome = cell.force();
            if (outcome == CONS) {
                set(cell.evaluatedHead(), (LazyList<T>) cell.evaluatedTail());
            }
            return outcome;
        } else if (list.isEmpty()) {
            return EMPTY;
        } else {
            set(list.head(), LazyList.narrow(list.tail()));
            return CONS;
        }
    }

    private Object force() {
        Object current = state;
        return current == CONS || current == EMPTY ? current : evaluate();
    }

    private Object evaluate() {
        @SuppressWarnings("Var")
        Object outcome;
        synchronized (this) {
            outcome = state;
            if (outcome == EVALUATING) {
                throw new IllegalStateException("LazyList: evaluating this list needs the list itself");
            } else if (outcome == null) {
                state = EVALUATING;
                // Nothing that can allocate or throw runs between the evaluation and the write of its outcome, not
                // even a type check, which may resolve a class: the cell is never left EVALUATING, even when the heap
                // or the stack is exhausted. Without an outcome, it is put back as it was.
                try {
                    outcome = compute();
                } catch (Throwable failure) {
                    outcome = failure;
                } finally {
                    state = outcome;
                }
                if (outcome instanceof VirtualMachineError) {
                    head = null;
                    tail = null;
                    state = null;
                } else {
                    release();
                }
            }
        }
        if (outcome instanceof Throwable failure) {
            throw LazyCell.<RuntimeException>rethrow(failure);
        }
        return outcome;
    }

    // throws the kept failure itself, whatever its type
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable failure) throws E {
        throw (E) failure;
    }

    // -- LazyList

    @Override
    public final boolean isEmpty() {
        return force() == EMPTY;
    }

    @Override
    public final T head() {
        if (force() == EMPTY) {
            throw new NoSuchElementException("head of empty LazyList");
        }
        return evaluatedHead();
    }

    @Override
    public final LazyList<T> tail() {
        if (force() == EMPTY) {
            throw new UnsupportedOperationException("tail of empty LazyList");
        }
        return evaluatedTail();
    }

    // the head of a cell whose state is CONS, which is written after head and tail: never null then
    private T evaluatedHead() {
        return Objects.requireNonNull(head, "LazyList: head of a cell that is not evaluated");
    }

    // the tail of a cell whose state is CONS
    private LazyList<T> evaluatedTail() {
        return Objects.requireNonNull(tail, "LazyList: tail of a cell that is not evaluated");
    }

    @Override
    public final java.util.Iterator<T> iterator() {
        return new LazyListModule.LazyListIterator<>(this);
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        return Collections.equals(this, o);
    }

    @Override
    public final int hashCode() {
        return Collections.hashOrdered(this);
    }

    /// The elements already evaluated, then `<not computed>` where the list is not evaluated yet (or its
    /// evaluation failed), or `<cycle>` where the evaluated cells link back to one already shown: nothing is
    /// evaluated, and it returns on a cyclic list too.
    ///
    /// Ported from `addStringNoForce` of the `LazyList` of the Scala 2.13 collections library, which Scala 3
    /// uses unchanged:
    /// [source](https://github.com/scala/scala/blob/2.13.x/src/library/scala/collection/immutable/LazyList.scala).
    /// A cursor and a scout going twice as fast walk the evaluated cells (Floyd's cycle detection), so a loop
    /// is found with no extra memory. Scala then leaves out the last cell of a loop that does not start at the
    /// first cell, since its lists close a loop with a copy of the cell before the loop. Here a cell can also
    /// link straight back (`prepend` on a cyclic list), so that last cell is left out only when its head is the
    /// same object as the head of the cell before the loop: the elements shown are the same then.
    @Override
    @SuppressWarnings("Var")
    public final String toString() {
        StringBuilder builder = new StringBuilder("LazyList(");
        Object first = state;
        if (first != CONS) {
            return builder.append(first == EMPTY ? ")" : "<not computed>)").toString();
        }
        builder.append(head);
        // the cursor is the next cell to show; the scout runs ahead, two cells per step, over the cells evaluated
        LazyList<T> cursor = this;
        LazyList<T> scout = evaluatedTail();
        boolean ended;
        if (cursor == scout) {
            ended = false;
        } else {
            cursor = scout;
            ended = !knownNonEmpty(scout);
            if (!ended) {
                scout = tailOf(scout);
                while (cursor != scout) {
                    if (!knownNonEmpty(scout)) {
                        ended = true;
                        break;
                    }
                    builder.append(", ").append(headOf(cursor));
                    cursor = tailOf(cursor);
                    scout = tailOf(scout);
                    if (knownNonEmpty(scout)) {
                        scout = tailOf(scout);
                    }
                }
            }
        }
        if (ended) {
            // no loop: the scout stopped at the first cell not known to be non-empty, and every cell before it is
            while (cursor != scout) {
                builder.append(", ").append(headOf(cursor));
                cursor = tailOf(cursor);
            }
            return (knownIsEmpty(scout) ? builder : builder.append(", <not computed>"))
                    .append(")")
                    .toString();
        }
        // a loop, where the scout met the cursor: it starts where a runner from this cell meets the scout. When it
        // starts at this cell, the cursor has gone round it once and every cell of it is shown.
        if (cursor != this) {
            LazyList<T> runner = this;
            LazyList<T> beforeLoop = this;
            while (runner != scout) {
                beforeLoop = runner;
                runner = tailOf(runner);
                scout = tailOf(scout);
            }
            do {
                LazyList<T> next = tailOf(cursor);
                if (next != scout || headOf(cursor) != headOf(beforeLoop)) {
                    builder.append(", ").append(headOf(cursor));
                }
                cursor = next;
            } while (cursor != scout);
        }
        return builder.append(", <cycle>)").toString();
    }

    // whether list is a cell already evaluated as non-empty: nothing is evaluated
    private static boolean knownNonEmpty(LazyList<?> list) {
        return list instanceof LazyCell<?> cell && cell.state == CONS;
    }

    // the head of a cell known to be non-empty
    @SuppressWarnings("unchecked")
    private static <T extends @Nullable Object> T headOf(LazyList<T> list) {
        return ((LazyCell<T>) list).evaluatedHead();
    }

    // the tail of a cell known to be non-empty
    @SuppressWarnings("unchecked")
    private static <T extends @Nullable Object> LazyList<T> tailOf(LazyList<T> list) {
        return ((LazyCell<T>) list).evaluatedTail();
    }

    // -- the kinds of cells

    // a cell created evaluated: the empty list, or a head and a tail
    private static final class Evaluated<T extends @Nullable Object> extends LazyCell<T> {

        Evaluated(Object state, @Nullable T head, @Nullable LazyList<T> tail) {
            super(state, head, tail);
        }

        @Override
        Object compute() {
            throw new AssertionError("an evaluated cell is never evaluated again");
        }
    }

    // the state of the list a supplier returns
    private static final class Deferred<T extends @Nullable Object> extends LazyCell<T> {

        // null once the state is kept
        private @Nullable Supplier<? extends LazyList<? extends T>> supplier;
        private final String nullResult;

        Deferred(Supplier<? extends LazyList<? extends T>> supplier, String nullResult) {
            this.supplier = Objects.requireNonNull(supplier, "supplier is null");
            this.nullResult = nullResult;
        }

        @Override
        Object compute() {
            // compute() runs only while the state is not kept, before release() clears supplier
            Supplier<? extends LazyList<? extends T>> source =
                    Objects.requireNonNull(supplier, "LazyList: a kept cell is evaluated again");
            return adopt(Objects.requireNonNull(source.get(), nullResult));
        }

        @Override
        void release() {
            supplier = null;
        }
    }

    // the elements of a list without its first ones, skipped on the first read
    private static final class Dropped<T extends @Nullable Object> extends LazyCell<T> {

        // null once the state is kept
        private @Nullable LazyList<T> source;
        private final int count;

        Dropped(LazyList<T> source, int count) {
            this.source = source;
            this.count = count;
        }

        @Override
        public LazyList<T> drop(int n) {
            // Correct whatever the state of this cell, so no lock is needed: a source read as null (the state is kept)
            // only makes a drop of this cell, which is evaluated then. A sum past Integer.MAX_VALUE is not collapsed.
            LazyList<T> from = source;
            if (n <= 0 || knownIsEmpty(this)) {
                return this;
            } else if (from != null && count <= Integer.MAX_VALUE - n) {
                return new Dropped<>(from, count + n);
            } else {
                return new Dropped<>(this, n);
            }
        }

        @Override
        Object compute() {
            // compute() runs only while the state is not kept, before release() clears source
            @SuppressWarnings("Var")
            LazyList<T> rest = Objects.requireNonNull(source, "LazyList: a kept cell is evaluated again");
            for (int i = count; i > 0 && !rest.isEmpty(); i--) {
                rest = rest.tail();
            }
            return adopt(rest);
        }

        @Override
        void release() {
            source = null;
        }
    }

    // the elements of an iterator, one cell per element
    private static final class FromIterator<T extends @Nullable Object> extends LazyCell<T> {

        // null once the state is kept
        private java.util.@Nullable Iterator<? extends T> iterator;

        FromIterator(java.util.Iterator<? extends T> iterator) {
            this.iterator = iterator;
        }

        @Override
        Object compute() {
            // compute() runs only while the state is not kept, before release() clears iterator
            java.util.Iterator<? extends T> source =
                    Objects.requireNonNull(iterator, "LazyList: a kept cell is evaluated again");
            if (!source.hasNext()) {
                return EMPTY;
            }
            set(Objects.requireNonNull(source.next(), "LazyList: element is null"), new FromIterator<>(source));
            return CONS;
        }

        @Override
        void release() {
            iterator = null;
        }
    }

    // The elements of prefix, then those of each list in pending, in order; any of them may be empty. append and
    // appendAll add one list to the queue, so a loop of them keeps every element one step away. Evaluating a cell
    // unwraps an Appended prefix into the queue in a loop and skips the empty parts in the same loop, so it never goes
    // through more than one Appended, whatever the number of calls that built it.
    private static final class Appended<T extends @Nullable Object> extends LazyCell<T> {

        private final LazyList<T> prefix;
        private final Queue<LazyList<T>> pending;

        Appended(LazyList<T> prefix, Queue<LazyList<T>> pending) {
            this.prefix = prefix;
            this.pending = pending;
        }

        @Override
        public LazyList<T> append(T element) {
            return new Appended<>(prefix, pending.append(cons(element, empty())));
        }

        @Override
        public LazyList<T> appendAll(Iterable<? extends T> elements) {
            LazyList<T> that = LazyList.ofAll(elements);
            return knownIsEmpty(that) ? this : new Appended<>(prefix, pending.append(that));
        }

        @Override
        Object compute() {
            @SuppressWarnings("Var")
            LazyList<T> first = prefix;
            @SuppressWarnings("Var")
            Queue<LazyList<T>> rest = pending;
            while (true) {
                while (first instanceof Appended<T> appended) {
                    rest = rest.isEmpty() ? appended.pending : appended.pending.append(joinLater(rest));
                    first = appended.prefix;
                }
                if (!first.isEmpty()) {
                    set(first.head(), rest.isEmpty() ? first.tail() : new Appended<>(first.tail(), rest));
                    return CONS;
                } else if (rest.isEmpty()) {
                    return EMPTY;
                }
                first = rest.head();
                rest = rest.tail();
            }
        }

        // The elements of the non-empty queue as one list, built in O(1) and evaluated only when it is reached.
        private static <T extends @Nullable Object> LazyList<T> joinLater(Queue<LazyList<T>> pending) {
            Queue<LazyList<T>> others = pending.tail();
            return others.isEmpty() ? pending.head() : new Appended<>(pending.head(), others);
        }
    }
}
