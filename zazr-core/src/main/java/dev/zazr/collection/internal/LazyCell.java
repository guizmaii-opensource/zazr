package dev.zazr.collection.internal;

import dev.zazr.collection.LazyList;
import dev.zazr.collection.Queue;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// The one implementation of [LazyList], ported from the evaluation model of Scala 3's `LazyList` (the Scala 2.13
/// collection library, which Scala 3 ships unchanged): a cell is a lazily evaluated state, either empty or a head and a
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
                set(cell.head, (LazyList<T>) cell.tail);
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
    @SuppressWarnings("NullAway") // a CONS state is written after head
    public final T head() {
        if (force() == EMPTY) {
            throw new NoSuchElementException("head of empty stream");
        }
        return head;
    }

    @Override
    @SuppressWarnings("NullAway") // a CONS state is written after tail
    public final LazyList<T> tail() {
        if (force() == EMPTY) {
            throw new UnsupportedOperationException("tail of empty stream");
        }
        return tail;
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

    /// The elements already evaluated, and `?` where the list is not evaluated yet (or its evaluation failed): nothing
    /// is evaluated.
    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder("LazyList(");
        @SuppressWarnings("Var")
        LazyList<T> list = this;
        @SuppressWarnings("Var")
        boolean first = true;
        while (true) {
            if (!(list instanceof LazyCell<T> cell) || cell.state != CONS && cell.state != EMPTY) {
                builder.append(first ? "?" : ", ?");
                break;
            } else if (cell.state == EMPTY) {
                break;
            }
            builder.append(first ? "" : ", ").append(cell.head);
            first = false;
            list = Objects.requireNonNull(cell.tail);
        }
        return builder.append(")").toString();
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
        @SuppressWarnings("NullAway") // compute() runs only while the state is not kept, so supplier is set
        Object compute() {
            return adopt(Objects.requireNonNull(supplier.get(), nullResult));
        }

        @Override
        void release() {
            supplier = null;
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
        @SuppressWarnings("NullAway") // compute() runs only while the state is not kept, so iterator is set
        Object compute() {
            java.util.Iterator<? extends T> source = iterator;
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
