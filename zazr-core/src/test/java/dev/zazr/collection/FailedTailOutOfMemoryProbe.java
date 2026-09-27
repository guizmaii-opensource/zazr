package dev.zazr.collection;

/// Run in a JVM of its own with a small heap by `LazyListTest`: fills the heap, forces a tail whose supplier throws
/// while no memory is left, then frees the heap and forces it again. Prints what the next two forces threw.
public final class FailedTailOutOfMemoryProbe {

    private static final RuntimeException BOOM = new RuntimeException("boom");

    private FailedTailOutOfMemoryProbe() {}

    private static Throwable failure(LazyList<Integer> stream) {
        try {
            stream.tail();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    public static void main(String[] args) {
        LazyList<Integer> stream = LazyList.cons(1, () -> {
            throw BOOM;
        });
        java.util.ArrayList<Object> keep = new java.util.ArrayList<>(1 << 20);
        for (int size : new int[] {1 << 20, 1 << 14, 1 << 8, 16, 1, 0}) {
            try {
                while (true) {
                    keep.add(new long[size]);
                }
            } catch (OutOfMemoryError e) {
                // the heap is full at this size, go on with a smaller one
            }
        }
        try {
            stream.tail();
        } catch (Throwable t) {
            // the supplier's exception, or an OutOfMemoryError met on the way out of the cell
        }
        keep.clear();
        keep.trimToSize();
        System.gc();
        Throwable second = failure(stream);
        Throwable third = failure(stream);
        String result = second == null
                ? "no failure"
                : second.getClass().getSimpleName() + ": " + second.getMessage() + (third == second ? " (same)" : "");
        System.out.println(result);
    }
}
