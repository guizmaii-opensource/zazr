package dev.zazr.collection.internal;

import java.util.Arrays;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

/**
 * The insertion order of a {@code LinkedHashMap}: its keys, each stored under the ordinal it was given when it was
 * put, an int that grows by one with every new key and is never given twice within one map, so that the keys of a
 * map have distinct ordinals, in their order.
 * <p>
 * The keys are in a trie indexed by ordinal, as a {@code Vector} is indexed by position: 32 children per node, the
 * ordinal read 5 bits at a time from the root. A removed key leaves no trace: its leaf holds only the keys still
 * present, in a compact array beside a 32-bit map of the ordinals they occupy (the bitmap compression of
 * {@code HashMap}'s nodes), and a node left empty is dropped. Each branch counts the keys below it, so the key of a
 * given rank (its position among the keys present) is found by one walk from the root.
 * <p>
 * The last block of 32 ordinals, where the new keys go, is kept out of the trie, in {@code tail}, so that a new key
 * copies one leaf of at most 32 keys; a full tail goes into the trie when the next key is put, one path copy per 32
 * keys. Scala's {@code Vector} and Clojure's {@code PersistentVector} keep their last leaf apart in the same way.
 * <p>
 * Costs, for n keys present and o the next ordinal: {@link #append}, {@link #remove}, {@link #update},
 * {@link #first()}, {@link #last()} and {@link #slice} copy one path from the root, at most 7 levels (log32 of
 * {@code o}); the iterators read each leaf once. Scala's {@code TreeSeqMap} also orders its keys by ordinal, in a
 * Patricia trie without counts, which cuts by rank in O(n).
 *
 * @param <K> the key type
 */
public final class InsertionOrder<K extends @Nullable Object> {

    private static final int BITS = 5;
    private static final int MASK = 31;

    /// The ordinal never given: an order whose next ordinal reaches it is renumbered from 0 by its map.
    public static final int LIMIT = Integer.MAX_VALUE;

    private static final Leaf EMPTY_LEAF = new Leaf(0, new Object[0]);

    private static final InsertionOrder<?> EMPTY = new InsertionOrder<>(null, 0, EMPTY_LEAF, 0, 0, 0);

    /* the keys of ordinal below tailBase: a Leaf when shift is 0, a Branch otherwise, null when there is none */
    private final @Nullable Object root;
    /* the number of bits of the ordinal below the root's level: 0 for a leaf, then 5, 10, .. 30 */
    private final int shift;
    /* the keys of ordinal tailBase to next - 1 */
    private final Leaf tail;
    /* a multiple of 32; every key of the trie has a lower ordinal */
    private final int tailBase;
    /* the ordinal of the next key appended, at most tailBase + 32 */
    private final int next;
    private final int size;

    private InsertionOrder(@Nullable Object root, int shift, Leaf tail, int tailBase, int next, int size) {
        this.root = root;
        this.shift = shift;
        this.tail = tail;
        this.tailBase = tailBase;
        this.next = next;
        this.size = size;
    }

    /* the keys of a block of 32 ordinals: keys[i] has the ordinal of the i-th bit set in bitmap */
    private static final class Leaf {
        final int bitmap;
        final Object[] keys;

        Leaf(int bitmap, Object[] keys) {
            this.bitmap = bitmap;
            this.keys = keys;
        }
    }

    /* children[i] is the node of the i-th bit set in bitmap; size counts the keys below */
    private static final class Branch {
        final int bitmap;
        final Object[] children;
        final int size;

        Branch(int bitmap, Object[] children, int size) {
            this.bitmap = bitmap;
            this.children = children;
            this.size = size;
        }
    }

    /// The order with no key, whose first key gets the ordinal 0.
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object> InsertionOrder<K> empty() {
        return (InsertionOrder<K>) EMPTY;
    }

    /// The order with no key whose first key gets the ordinal `ordinal`, between 0 and [#LIMIT]: the tests start
    /// there to reach the renumbering without putting two billion keys.
    public static <K extends @Nullable Object> InsertionOrder<K> startingAt(int ordinal) {
        if (ordinal < 0 || ordinal > LIMIT) {
            throw new IllegalArgumentException("ordinal out of range: " + ordinal);
        }
        return new InsertionOrder<>(null, 0, EMPTY_LEAF, ordinal & ~MASK, ordinal, 0);
    }

    /// The order of `keys`, the i-th one under the ordinal i, in O(n).
    public static <K extends @Nullable Object> InsertionOrder<K> ofAll(java.util.List<? extends K> keys) {
        int count = keys.size();
        if (count == 0) {
            return empty();
        }
        int tailBase = (count - 1) & ~MASK;
        int leafCount = tailBase >>> BITS;
        Object[] level = new Object[leafCount];
        for (int i = 0; i < leafCount; i++) {
            level[i] = new Leaf(-1, keys.subList(i << BITS, (i + 1) << BITS).toArray());
        }
        Leaf tail = new Leaf(
                lowBits(count - tailBase), keys.subList(tailBase, count).toArray());
        @SuppressWarnings("Var")
        int shift = 0;
        @SuppressWarnings("Var")
        Object[] nodes = level;
        while (nodes.length > 1) {
            Object[] parents = new Object[(nodes.length + MASK) >>> BITS];
            for (int p = 0; p < parents.length; p++) {
                Object[] children = Arrays.copyOfRange(nodes, p << BITS, Math.min((p + 1) << BITS, nodes.length));
                @SuppressWarnings("Var")
                int below = 0;
                for (Object child : children) {
                    below += sizeOf(child);
                }
                parents[p] = new Branch(lowBits(children.length), children, below);
            }
            nodes = parents;
            shift += BITS;
        }
        return new InsertionOrder<>(nodes.length == 0 ? null : nodes[0], shift, tail, tailBase, count, count);
    }

    // the bitmap of the `count` lowest bits, count from 1 to 32
    private static int lowBits(int count) {
        return count == 32 ? -1 : (1 << count) - 1;
    }

    private static int sizeOf(Object node) {
        return node instanceof Leaf leaf ? leaf.keys.length : ((Branch) node).size;
    }

    // the index, in the compact array of a node, of the entry of the given bit
    private static int indexOf(int bitmap, int bit) {
        return Integer.bitCount(bitmap & (bit - 1));
    }

    // whether a root of this level holds every ordinal up to `ordinal`
    private static boolean covers(int shift, int ordinal) {
        return shift + BITS > 30 || (ordinal >>> (shift + BITS)) == 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /// The ordinal [#append] gives to its key.
    public int nextOrdinal() {
        return next;
    }

    /// Whether every ordinal has been given, so that [#append] cannot be called: the map renumbers its keys first.
    public boolean isFull() {
        return next == LIMIT;
    }

    /// This order with `key` last, under the ordinal [#nextOrdinal()]. The order must not be [#isFull()].
    public InsertionOrder<K> append(K key) {
        if (next - tailBase < 32) {
            Leaf newTail = new Leaf(tail.bitmap | (1 << (next - tailBase)), appended(tail.keys, key));
            return new InsertionOrder<>(root, shift, newTail, tailBase, next + 1, size + 1);
        }
        // the tail's block is used up: it goes into the trie, and the key starts the next block
        Leaf newTail = new Leaf(1, new Object[] {key});
        if (tail.keys.length == 0) {
            return new InsertionOrder<>(root, shift, newTail, next, next + 1, size + 1);
        }
        Object oldRoot = root;
        if (oldRoot == null) {
            int newShift = shiftFor(tailBase);
            return new InsertionOrder<>(chain(newShift, tailBase, tail), newShift, newTail, next, next + 1, size + 1);
        }
        @SuppressWarnings("Var")
        int newShift = shift;
        @SuppressWarnings("Var")
        Object grown = oldRoot;
        while (!covers(newShift, tailBase)) {
            grown = new Branch(1, new Object[] {grown}, sizeOf(grown));
            newShift += BITS;
        }
        Object newRoot = withLeaf((Branch) grown, newShift, tailBase, tail);
        return new InsertionOrder<>(newRoot, newShift, newTail, next, next + 1, size + 1);
    }

    // the level of the lowest root that holds `ordinal`
    @SuppressWarnings("Var")
    private static int shiftFor(int ordinal) {
        int shift = 0;
        while (!covers(shift, ordinal)) {
            shift += BITS;
        }
        return shift;
    }

    // the path from a node of level `shift` down to `leaf`, of the block at `base`
    private static Object chain(int shift, int base, Leaf leaf) {
        if (shift == 0) {
            return leaf;
        }
        return new Branch(
                1 << ((base >>> shift) & MASK), new Object[] {chain(shift - BITS, base, leaf)}, leaf.keys.length);
    }

    // `node`, of level `shift` (5 or more), with the leaf of the block at `base` added: a block above all of its keys
    private static Branch withLeaf(Branch node, int shift, int base, Leaf leaf) {
        int bit = 1 << ((base >>> shift) & MASK);
        int index = indexOf(node.bitmap, bit);
        int newSize = node.size + leaf.keys.length;
        if ((node.bitmap & bit) != 0) {
            Object[] children = node.children.clone();
            children[index] = withLeaf((Branch) children[index], shift - BITS, base, leaf);
            return new Branch(node.bitmap, children, newSize);
        }
        Object[] children = inserted(node.children, index, chain(shift - BITS, base, leaf));
        return new Branch(node.bitmap | bit, children, newSize);
    }

    /// This order without the key of `ordinal`, which must be present.
    public InsertionOrder<K> remove(int ordinal) {
        if (ordinal >= tailBase) {
            Leaf newTail = withoutKey(tail, ordinal);
            return new InsertionOrder<>(root, shift, newTail == null ? EMPTY_LEAF : newTail, tailBase, next, size - 1);
        }
        @SuppressWarnings("NullAway") // the key is present, so the trie is not empty
        Object newRoot = removed(root, shift, ordinal);
        return new InsertionOrder<>(newRoot, shift, tail, tailBase, next, size - 1);
    }

    // `node` without the key of `ordinal`, null when it was its last key
    private static @Nullable Object removed(Object node, int shift, int ordinal) {
        if (shift == 0) {
            return withoutKey((Leaf) node, ordinal);
        }
        Branch branch = (Branch) node;
        int bit = 1 << ((ordinal >>> shift) & MASK);
        int index = indexOf(branch.bitmap, bit);
        Object child = removed(branch.children[index], shift - BITS, ordinal);
        if (child != null) {
            Object[] children = branch.children.clone();
            children[index] = child;
            return new Branch(branch.bitmap, children, branch.size - 1);
        } else if (branch.children.length == 1) {
            return null;
        }
        return new Branch(branch.bitmap & ~bit, removedAt(branch.children, index), branch.size - 1);
    }

    private static @Nullable Leaf withoutKey(Leaf leaf, int ordinal) {
        if (leaf.keys.length == 1) {
            return null;
        }
        int bit = 1 << (ordinal & MASK);
        return new Leaf(leaf.bitmap & ~bit, removedAt(leaf.keys, indexOf(leaf.bitmap, bit)));
    }

    /// This order with `key` in place of the key of `ordinal`, which must be present.
    public InsertionOrder<K> update(int ordinal, K key) {
        if (ordinal >= tailBase) {
            return new InsertionOrder<>(root, shift, updated(tail, ordinal, key), tailBase, next, size);
        }
        @SuppressWarnings("NullAway") // the key is present, so the trie is not empty
        Object newRoot = updated(root, shift, ordinal, key);
        return new InsertionOrder<>(newRoot, shift, tail, tailBase, next, size);
    }

    private static Object updated(Object node, int shift, int ordinal, Object key) {
        if (shift == 0) {
            return updated((Leaf) node, ordinal, key);
        }
        Branch branch = (Branch) node;
        int index = indexOf(branch.bitmap, 1 << ((ordinal >>> shift) & MASK));
        Object[] children = branch.children.clone();
        children[index] = updated(children[index], shift - BITS, ordinal, key);
        return new Branch(branch.bitmap, children, branch.size);
    }

    private static Leaf updated(Leaf leaf, int ordinal, Object key) {
        Object[] keys = leaf.keys.clone();
        keys[indexOf(leaf.bitmap, 1 << (ordinal & MASK))] = key;
        return new Leaf(leaf.bitmap, keys);
    }

    /// The first key. The order must not be empty.
    @SuppressWarnings("unchecked")
    public K first() {
        Object node = root;
        if (node == null) {
            return (K) tail.keys[0];
        }
        @SuppressWarnings("Var")
        Object current = node;
        while (current instanceof Branch branch) {
            current = branch.children[0];
        }
        return (K) ((Leaf) current).keys[0];
    }

    /// The last key. The order must not be empty.
    @SuppressWarnings({"unchecked", "NullAway"})
    public K last() {
        Object[] tailKeys = tail.keys;
        if (tailKeys.length > 0) {
            return (K) tailKeys[tailKeys.length - 1];
        }
        @SuppressWarnings("Var")
        Object current = root;
        while (current instanceof Branch branch) {
            current = branch.children[branch.children.length - 1];
        }
        Object[] keys = ((Leaf) current).keys;
        return (K) keys[keys.length - 1];
    }

    /// The keys of rank `from` (inclusive) to `until` (exclusive), each under its ordinal: 0 <= from < until <= size.
    /// The next ordinal stays the same.
    public InsertionOrder<K> slice(int from, int until) {
        if (from == 0 && until == size) {
            return this;
        }
        Object oldRoot = root;
        int trieSize = oldRoot == null ? 0 : sizeOf(oldRoot);
        Object newRoot = oldRoot != null && from < trieSize ? sliced(oldRoot, from, Math.min(until, trieSize)) : null;
        Leaf newTail = until > trieSize ? sliced(tail, Math.max(from - trieSize, 0), until - trieSize) : EMPTY_LEAF;
        return new InsertionOrder<>(newRoot, shift, newTail, tailBase, next, until - from);
    }

    // the keys of rank lo (inclusive) to hi (exclusive) of `node`: 0 <= lo < hi <= its size
    private static Object sliced(Object node, int lo, int hi) {
        if (node instanceof Leaf leaf) {
            return sliced(leaf, lo, hi);
        }
        Branch branch = (Branch) node;
        if (lo == 0 && hi == branch.size) {
            return branch;
        }
        Object[] children = branch.children;
        // the children holding ranks lo and hi - 1, and the rank of the first key of the first of them
        @SuppressWarnings("Var")
        int first = 0;
        @SuppressWarnings("Var")
        int firstStart = 0;
        while (firstStart + sizeOf(children[first]) <= lo) {
            firstStart += sizeOf(children[first]);
            first++;
        }
        @SuppressWarnings("Var")
        int last = first;
        @SuppressWarnings("Var")
        int lastStart = firstStart;
        while (lastStart + sizeOf(children[last]) < hi) {
            lastStart += sizeOf(children[last]);
            last++;
        }
        Object[] kept = Arrays.copyOfRange(children, first, last + 1);
        if (first == last) {
            kept[0] = sliced(children[first], lo - firstStart, hi - firstStart);
        } else {
            kept[0] = sliced(children[first], lo - firstStart, sizeOf(children[first]));
            kept[kept.length - 1] = sliced(children[last], 0, hi - lastStart);
        }
        return new Branch(bitsBetween(branch.bitmap, first, last), kept, hi - lo);
    }

    private static Leaf sliced(Leaf leaf, int lo, int hi) {
        if (lo == 0 && hi == leaf.keys.length) {
            return leaf;
        }
        return new Leaf(bitsBetween(leaf.bitmap, lo, hi - 1), Arrays.copyOfRange(leaf.keys, lo, hi));
    }

    // the bits of `bitmap` from its first-th set bit to its last-th set bit, both included
    private static int bitsBetween(int bitmap, int first, int last) {
        int low = nthSetBit(bitmap, first);
        int high = nthSetBit(bitmap, last);
        return bitmap & (-1 >>> (31 - high)) & (-1 << low);
    }

    // the position of the n-th set bit of `bitmap`, from 0
    @SuppressWarnings("Var")
    private static int nthSetBit(int bitmap, int n) {
        int bits = bitmap;
        for (int i = 0; i < n; i++) {
            bits &= bits - 1;
        }
        return Integer.numberOfTrailingZeros(bits);
    }

    private static Object[] appended(Object[] array, Object element) {
        Object[] copy = Arrays.copyOf(array, array.length + 1);
        copy[array.length] = element;
        return copy;
    }

    private static Object[] inserted(Object[] array, int index, Object element) {
        Object[] copy = new Object[array.length + 1];
        System.arraycopy(array, 0, copy, 0, index);
        copy[index] = element;
        System.arraycopy(array, index, copy, index + 1, array.length - index);
        return copy;
    }

    private static Object[] removedAt(Object[] array, int index) {
        Object[] copy = new Object[array.length - 1];
        System.arraycopy(array, 0, copy, 0, index);
        System.arraycopy(array, index + 1, copy, index, array.length - index - 1);
        return copy;
    }

    /// The keys in order.
    public java.util.Iterator<K> iterator() {
        return new Forward<>(this, 0);
    }

    /// The keys in order from the one of rank `from`, 0 <= from <= size.
    public java.util.Iterator<K> iterator(int from) {
        return new Forward<>(this, from);
    }

    /// The keys from the last one to the first.
    public java.util.Iterator<K> reverseIterator() {
        return new Backward<>(this);
    }

    // the number of branch levels above the leaves of the trie
    private int depth() {
        return shift / BITS;
    }

    /* The keys from a rank on: the leaves of the trie from left to right, then the tail. branches[0 .. top] is the path
     * from the root to the current leaf, and positions[d] the index in branches[d] of the next child to visit. */
    private static final class Forward<K extends @Nullable Object> implements java.util.Iterator<K> {

        private final Leaf tail;
        private final Branch[] branches;
        private final int[] positions;
        private int top;
        private Object[] keys;
        private int index;
        private int remaining;

        @SuppressWarnings("Var")
        Forward(InsertionOrder<K> order, int from) {
            this.tail = order.tail;
            this.remaining = order.size - from;
            this.branches = new Branch[order.depth()];
            this.positions = new int[branches.length];
            this.top = -1;
            Object root = order.root;
            int trieSize = root == null ? 0 : sizeOf(root);
            if (root == null || from >= trieSize) {
                this.keys = tail.keys;
                this.index = from - trieSize;
                return;
            }
            // down from the root to the leaf of rank `from`, keeping the path
            Object node = root;
            int rank = from;
            while (node instanceof Branch branch) {
                int child = 0;
                while (rank >= sizeOf(branch.children[child])) {
                    rank -= sizeOf(branch.children[child]);
                    child++;
                }
                top++;
                branches[top] = branch;
                positions[top] = child + 1;
                node = branch.children[child];
            }
            this.keys = ((Leaf) node).keys;
            this.index = rank;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public K next() {
            if (remaining <= 0) {
                throw new NoSuchElementException("next() on empty iterator");
            }
            if (index == keys.length) {
                nextLeaf();
            }
            remaining--;
            return (K) keys[index++];
        }

        // the leftmost leaf after the current one, or the tail after the last leaf
        private void nextLeaf() {
            while (top >= 0 && positions[top] == branches[top].children.length) {
                top--;
            }
            if (top < 0) {
                keys = tail.keys;
                index = 0;
                return;
            }
            @SuppressWarnings("Var")
            Object node = branches[top].children[positions[top]++];
            while (node instanceof Branch branch) {
                top++;
                branches[top] = branch;
                positions[top] = 1;
                node = branch.children[0];
            }
            keys = ((Leaf) node).keys;
            index = 0;
        }
    }

    /* The keys from the last one: the tail from its end, then the leaves of the trie from right to left. branches[0 ..
     * top] is the path from the root to the current leaf, and positions[d] the index in branches[d] of the child
     * visited last; the root itself is entered once the tail is read. */
    private static final class Backward<K extends @Nullable Object> implements java.util.Iterator<K> {

        private final @Nullable Object root;
        private final Branch[] branches;
        private final int[] positions;
        private int top;
        private boolean rootEntered;
        private Object[] keys;
        private int index;
        private int remaining;

        Backward(InsertionOrder<K> order) {
            this.root = order.root;
            this.branches = new Branch[order.depth()];
            this.positions = new int[branches.length];
            this.top = -1;
            this.keys = order.tail.keys;
            this.index = keys.length;
            this.remaining = order.size;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public K next() {
            if (remaining <= 0) {
                throw new NoSuchElementException("next() on empty iterator");
            }
            if (index == 0) {
                previousLeaf();
            }
            remaining--;
            return (K) keys[--index];
        }

        // the rightmost leaf before the current one; the rightmost leaf of the trie after the tail
        @SuppressWarnings("NullAway") // keys are left, so the trie holds them once the tail is read
        private void previousLeaf() {
            @SuppressWarnings("Var")
            Object node;
            if (!rootEntered) {
                rootEntered = true;
                node = root;
            } else {
                while (positions[top] == 0) {
                    top--;
                }
                node = branches[top].children[--positions[top]];
            }
            while (node instanceof Branch branch) {
                top++;
                branches[top] = branch;
                positions[top] = branch.children.length - 1;
                node = branch.children[positions[top]];
            }
            keys = ((Leaf) node).keys;
            index = keys.length;
        }
    }
}
