package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Empty;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule.Node;
import java.util.Arrays;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;

/// The state behind `TreeSet.Builder` and `TreeMap.Builder`: a sort-then-build accumulator. Elements are appended to
/// an array; `result()` sorts it with the tree's comparator (a stable sort, O(n) compares when the input is already
/// sorted), keeps the last of each run of elements the comparator finds equal, and builds a balanced red-black tree
/// bottom-up in O(n) (`Node.fromOrdered`). The result holds the same elements as inserting them one by one, the later
/// of two equal elements replacing the earlier one, but costs one array plus exactly one node per distinct element.
/// A builder created with `keepFirst` keeps the first of equal elements instead.
///
/// Single-use and not thread-safe: after [#result()], or after the comparator has thrown, every method throws
/// [IllegalStateException]. Null elements are the caller's to reject.
///
/// @param <T> the element type
public final class RedBlackTreeBuilder<T extends @Nullable Object> {

    private static final @Nullable Object[] EMPTY_BUFFER = new Object[0];

    private final Comparator<? super T> comparator;
    // the name used in the messages, e.g. "TreeSet.Builder"
    private final String name;
    // of equal elements, keep the one added first instead of the one added last
    private final boolean keepFirst;
    private @Nullable Object[] buffer;
    private int length;
    // buffer[0, compacted) is sorted and holds no two equal elements
    private int compacted;
    private boolean done;
    // set while the buffer is sorted: if the comparator throws, it stays set and the builder refuses any further use
    private boolean sorting;

    public RedBlackTreeBuilder(Comparator<? super T> comparator, String name) {
        this(comparator, name, 0, false);
    }

    /// A builder whose buffer starts with room for `capacity` elements, and which keeps the first of equal elements
    /// when `keepFirst` is true (as successive insertions that skip an element already present would) instead of the
    /// last.
    public RedBlackTreeBuilder(Comparator<? super T> comparator, String name, int capacity, boolean keepFirst) {
        this.comparator = comparator;
        this.name = name;
        this.keepFirst = keepFirst;
        this.buffer = (capacity <= 0) ? EMPTY_BUFFER : new Object[capacity];
    }

    /// The comparator the tree is ordered by.
    public Comparator<? super T> comparator() {
        return comparator;
    }

    /// Appends one element. The caller has called [#checkOpen()] and checked that the element is not null.
    public void add(T element) {
        if (length == buffer.length) {
            buffer = Arrays.copyOf(buffer, (length == 0) ? 16 : length + (length >> 1));
        }
        buffer[length++] = element;
    }

    /// Returns the number of distinct elements added so far. It sorts and compacts the buffer, so a builder that
    /// alternates `add` and `size` pays a sort of the accumulated elements each time (cheap on the sorted prefix).
    public int size() {
        checkOpen();
        compact();
        return length;
    }

    /// Returns the tree of the elements added, and closes this builder.
    public RedBlackTree<T> result() {
        checkOpen();
        compact();
        done = true;
        final Empty<T> empty = new Empty<>(comparator);
        final RedBlackTree<T> tree = Node.fromOrdered(empty, buffer, length);
        buffer = EMPTY_BUFFER;
        length = 0;
        compacted = 0;
        return tree;
    }

    public void checkOpen() {
        if (done) {
            throw new IllegalStateException("result() has already been called on this " + name);
        } else if (sorting) {
            throw new IllegalStateException("the comparator threw while this " + name + " was sorting; it cannot be used any more");
        }
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    private void compact() {
        if (compacted == length) {
            return;
        }
        final Comparator<Object> order = (Comparator<Object>) comparator;
        final Object[] elements = (Object[]) buffer;
        // a throwing comparator can leave a partly merged buffer behind
        sorting = true;
        Arrays.sort(elements, 0, length, order);
        // the sort is stable, so of equal elements the one added first comes first and the one added last comes last
        int kept = 1;
        for (int i = 1; i < length; i++) {
            if (order.compare(elements[kept - 1], elements[i]) == 0) {
                if (!keepFirst) {
                    elements[kept - 1] = elements[i];
                }
            } else {
                elements[kept++] = elements[i];
            }
        }
        sorting = false;
        Arrays.fill(elements, kept, length, null);
        length = kept;
        compacted = kept;
    }
}
