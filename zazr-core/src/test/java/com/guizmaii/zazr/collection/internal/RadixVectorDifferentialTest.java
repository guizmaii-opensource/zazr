package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.Vector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.VectorStatics.WIDTH;
import static com.guizmaii.zazr.collection.internal.VectorStatics.vectorSliceDim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Differential test of {@link RadixVector} against {@link Vector}: the same operations are applied to both, from fixed
 * seeds, and after every step the contents are compared (iterator, {@code forEach}, {@code get}, {@code head},
 * {@code last}, reverse iteration, bulk copy) and the shape invariants of the finger tree are checked. Every value
 * reached, and every argument used, is kept and compared again at the end: no operation may write into an array that
 * an earlier value can still reach.
 */
public class RadixVectorDifferentialTest {

    /* the sizes around the width of one leaf and of one Vector2 */
    private static final int[] SMALL_SIZES = { 0, 1, 2, 31, 32, 33, 63, 64, 65, 992, 993, 1023, 1024, 1025, 1056 };
    /* the sizes around the capacity of a Vector3 (32 768 = 32^3) */
    private static final int[] MEDIUM_SIZES = { 32735, 32736, 32737, 32767, 32768, 32769 };
    /* the sizes around the capacity of a Vector4 (2^20 = 32^4) */
    private static final int[] LARGE_SIZES = { (1 << 20) - 1, 1 << 20, (1 << 20) + 1 };

    // ---------------------------------------------------------------------------------------------------------------
    // random operation sequences

    @Test
    public void randomSequencesSmallA() {
        runSmallSequences(0, 600);
    }

    @Test
    public void randomSequencesSmallB() {
        runSmallSequences(600, 1200);
    }

    @Test
    public void randomSequencesSmallC() {
        runSmallSequences(1200, 1800);
    }

    @Test
    public void randomSequencesSmallD() {
        runSmallSequences(1800, 2400);
    }

    private static void runSmallSequences(int fromSeed, int toSeed) {
        for (int seed = fromSeed; seed < toSeed; seed++) {
            final Run run = new Run(seed);
            final int size = (seed % 2 == 0) ? SMALL_SIZES[(seed / 2) % SMALL_SIZES.length] : run.rnd.nextInt(800);
            final History history = History.values()[run.rnd.nextInt(History.values().length)];
            runSequence(run, size, history, 200, 1500);
        }
    }

    @Test
    public void randomSequencesAroundVector3Capacity() {
        int seed = 100_000;
        for (int size : MEDIUM_SIZES) {
            for (History history : new History[] { History.BUILDER, History.APPENDS, History.PREPENDS, History.SLICED }) {
                final Run run = new Run(seed++);
                runSequence(run, size, history, 60, 70_000);
            }
        }
    }

    @Test
    public void randomSequencesAroundVector4Capacity() {
        int seed = 200_000;
        for (int size : LARGE_SIZES) {
            for (History history : new History[] { History.BUILDER, History.SLICED }) {
                final Run run = new Run(seed++);
                runSequence(run, size, history, 25, 1 << 21);
            }
        }
    }

    private static void runSequence(Run run, int size, History history, int steps, int maxLength) {
        run.log("start " + history + " " + size);
        Pair p = run.checked(() -> build(run, size, history));
        for (int step = 0; step < steps; step++) {
            final Pair current = p;
            final int s = step;
            p = run.checked(() -> {
                run.step = s;
                return step(run, current, maxLength);
            });
        }
        run.recheckHistory();
    }

    private static Pair step(Run run, Pair p, int maxLength) {
        final Random rnd = run.rnd;
        final int n = p.v.length();
        int op = rnd.nextInt(100);
        if (n > maxLength && op < 60) {
            op = 60 + rnd.nextInt(34);
        }
        if (op < 15) {
            final Integer x = run.fresh();
            run.log("appended " + x);
            return run.record(p.r.appended(x), p.v.append(x));
        } else if (op < 30) {
            final Integer x = run.fresh();
            run.log("prepended " + x);
            return run.record(p.r.prepended(x), p.v.prepend(x));
        } else if (op < 38) {
            final int i = (rnd.nextInt(10) == 0) ? edgeIndex(rnd, n) : (n == 0 ? 0 : rnd.nextInt(n));
            final Integer x = run.fresh();
            run.log("updated " + i);
            return run.same(p, () -> p.r.updated(i, x), () -> p.v.update(i, x));
        } else if (op < 46) {
            final Operand o = operand(run);
            run.log("appendedAll " + o);
            return run.record(p.r.appendedAll(o.forRadix()), p.v.appendAll(o.forVector()));
        } else if (op < 54) {
            final Operand o = operand(run);
            run.log("prependedAll " + o);
            return run.record(p.r.prependedAll(o.forRadix()), p.v.prependAll(o.forVector()));
        } else if (op < 57) {
            return map(run, p);
        } else if (op < 60) {
            return rebuild(run, p);
        } else if (op < 68) {
            final int from = arg(rnd, n);
            final int until = arg(rnd, n);
            run.log("slice " + from + " " + until);
            return run.record(p.r.slice(from, until), p.v.slice(from, until));
        } else if (op < 72) {
            final int k = arg(rnd, n);
            run.log("take " + k);
            return run.record(p.r.take(k), p.v.take(k));
        } else if (op < 76) {
            final int k = arg(rnd, n);
            run.log("drop " + k);
            return run.record(p.r.drop(k), p.v.drop(k));
        } else if (op < 79) {
            final int k = arg(rnd, n);
            run.log("takeRight " + k);
            return run.record(p.r.takeRight(k), p.v.takeRight(k));
        } else if (op < 82) {
            final int k = arg(rnd, n);
            run.log("dropRight " + k);
            return run.record(p.r.dropRight(k), p.v.dropRight(k));
        } else if (op < 88) {
            run.log("tail");
            return run.same(p, p.r::tail, p.v::tail);
        } else if (op < 94) {
            run.log("init");
            return run.same(p, p.r::init, p.v::init);
        } else if (op < 97) {
            final int i = edgeIndex(rnd, n);
            run.log("get/head/last " + i);
            sameOutcome(() -> p.r.get(i), () -> p.v.get(i));
            sameOutcome(p.r::head, p.v::head);
            sameOutcome(p.r::last, p.v::last);
            return p;
        } else if (n <= maxLength / 2) {
            if (rnd.nextBoolean()) {
                run.log("appendedAll self");
                return run.record(p.r.appendedAll(p.r), p.v.appendAll(p.v));
            } else {
                run.log("prependedAll self");
                return run.record(p.r.prependedAll(p.r), p.v.prependAll(p.v));
            }
        } else {
            run.log("drop half");
            return run.record(p.r.drop(n / 2), p.v.drop(n / 2));
        }
    }

    private static Pair map(Run run, Pair p) {
        if (run.rnd.nextBoolean()) {
            run.log("map identity");
            final RadixVector<Integer> mapped = p.r.map(Function.identity());
            // nothing changed, so every array is shared
            assertThat(mapped.getClass()).isSameAs(p.r.getClass());
            for (int i = 0; i < p.r.vectorSliceCount(); i++) {
                assertThat(mapped.vectorSlice(i)).isSameAs(p.r.vectorSlice(i));
            }
            return run.record(mapped, p.v.map(Function.identity()));
        } else {
            run.log("map");
            final Function<Integer, Integer> f = x -> x ^ 0x5A5A5A;
            final RadixVector<Integer> mapped = p.r.map(f);
            assertThat(mapped.getClass()).isSameAs(p.r.getClass());
            return run.record(mapped, p.v.map(f));
        }
    }

    /* rebuilds the value through the builder: a first vector chunk (initFrom), single elements or a list, a second
     * vector chunk (addVector), then a few new elements */
    private static Pair rebuild(Run run, Pair p) {
        final Random rnd = run.rnd;
        final int n = p.v.length();
        final int a = rnd.nextInt(n + 1);
        final int b = a + rnd.nextInt(n - a + 1);
        final int extra = rnd.nextInt(40);
        run.log("rebuild " + a + " " + b + " +" + extra);
        final VectorBuilder<Integer> builder = RadixVector.newBuilder();
        builder.addAll(p.r.slice(0, a));
        final Vector<Integer> middle = p.v.slice(a, b);
        if (rnd.nextBoolean()) {
            for (Integer x : middle) {
                builder.add(x);
            }
        } else {
            builder.addAll(javaList(middle));
        }
        builder.addAll(p.r.slice(b, n));
        final List<Integer> tail = new ArrayList<>();
        for (int i = 0; i < extra; i++) {
            final Integer x = run.fresh();
            tail.add(x);
            builder.add(x);
        }
        assertThat(builder.size()).isEqualTo(n + extra);
        return run.record(builder.result(), p.v.appendAll(tail));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // deterministic sweeps

    /* appendedAll and prependedAll between every pair of shapes and histories */
    @Test
    public void appendedAllAndPrependedAllBetweenEveryPairOfShapes() {
        final Run run = new Run(300_000);
        final List<Pair> fixtures = new ArrayList<>();
        final int[] sizes = { 0, 1, 2, 31, 32, 33, 64, 1023, 1024, 1025, 2000, 32768, 32769 };
        for (int size : sizes) {
            for (History history : new History[] { History.BUILDER, History.PREPENDS, History.SLICED }) {
                if (size > 2000 && history == History.PREPENDS) {
                    continue;
                }
                fixtures.add(run.checked(() -> build(run, size, history)));
            }
        }
        for (Pair left : fixtures) {
            for (Pair right : fixtures) {
                run.checked(() -> {
                    run.log("appendedAll " + left.label + " ++ " + right.label);
                    run.record(left.r.appendedAll(right.r), left.v.appendAll(right.v));
                    run.log("prependedAll " + right.label + " ++ " + left.label);
                    return run.record(left.r.prependedAll(right.r), left.v.prependAll(right.v));
                });
            }
        }
        run.recheckHistory();
    }

    /*
     * appendedAll and prependedAll of two big vectors of close sizes: the builder starts from one (initFrom) and adds the
     * other by slices (addVector), whose data of dimension 3, 4 and 5 is shared when the builder is aligned on it and
     * split one dimension lower when it is not; the left sizes put the builder at every such alignment
     */
    @Test
    public void appendedAllOfBigVectorsAtEveryAlignment() {
        final Run run = new Run(350_000);
        final List<Pair> rights = new ArrayList<>();
        for (int size : new int[] { 3000, 70_000 }) {
            rights.add(run.checked(() -> build(run, size, History.BUILDER)));
            rights.add(run.checked(() -> {
                final Pair p = build(run, size + 37, History.BUILDER);
                return run.record(p.r.drop(37).prepended(-1), p.v.drop(37).prepend(-1));
            }));
        }
        for (Pair right : rights) {
            final int k = right.v.length();
            for (int delta : new int[] { -64, -33, -32, -1, 0, 1, 31, 32, 64, 1024, 1025, 32768, 32800 }) {
                final int size = k + delta;
                for (History history : new History[] { History.BUILDER, History.SLICED }) {
                    final Pair left = run.checked(() -> build(run, size, history));
                    run.checked(() -> {
                        run.log("appendedAll " + left.label + " ++ " + right.label);
                        run.record(left.r.appendedAll(right.r), left.v.appendAll(right.v));
                        run.log("prependedAll " + right.label + " ++ " + left.label);
                        run.record(left.r.prependedAll(right.r), left.v.prependAll(right.v));
                        run.log("appendedAll " + right.label + " ++ " + left.label);
                        run.record(right.r.appendedAll(left.r), right.v.appendAll(left.v));
                        run.log("prependedAll " + left.label + " ++ " + right.label);
                        return run.record(right.r.prependedAll(left.r), right.v.prependAll(left.v));
                    });
                }
            }
        }
        run.recheckHistory();
    }

    /* a small vector added to a much bigger one aligns the builder on the big one, whose leaves are then shared, not copied */
    @Test
    public void appendedAllAlignsOnTheBiggerVectorAndSharesItsLeaves() {
        final Run run = new Run(360_000);
        for (int big : new int[] { 5_000, 70_000 }) {
            final Pair right = run.checked(() -> build(run, big, History.BUILDER));
            for (int small : new int[] { 200, 1000, 1057, 2100 }) {
                if (small >= big - 64 || small < (big >>> 5)) {
                    continue;
                }
                for (History history : new History[] { History.BUILDER, History.PREPENDS, History.SLICED }) {
                    final Pair left = run.checked(() -> build(run, small, history));
                    final Pair appended = run.checked(() -> run.record(left.r.appendedAll(right.r), left.v.appendAll(right.v)));
                    final Pair prepended = run.checked(() -> run.record(right.r.prependedAll(left.r), right.v.prependAll(left.v)));
                    final java.util.Set<Object[]> rightLeaves = leaves(right.r);
                    for (Pair p : List.of(appended, prepended)) {
                        final java.util.Set<Object[]> shared = leaves(p.r);
                        shared.retainAll(rightLeaves);
                        // all of the big vector's full leaves but the ones at the seams
                        assertThat(shared.size()).as("%s ++ %s", left.label, right.label).isGreaterThanOrEqualTo(big / WIDTH - 2);
                    }
                }
            }
        }
        run.recheckHistory();
    }

    private static java.util.Set<Object[]> leaves(RadixVector<?> r) {
        final java.util.Set<Object[]> set = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        final int c = r.vectorSliceCount();
        for (int i = 0; i < c; i++) {
            collectLeaves(vectorSliceDim(c, i), r.vectorSlice(i), set);
        }
        return set;
    }

    private static void collectLeaves(int dim, Object[] a, java.util.Set<Object[]> set) {
        if (dim == 1) {
            set.add(a);
        } else {
            for (Object child : a) {
                collectLeaves(dim - 1, (Object[]) child, set);
            }
        }
    }

    /* slice, take, drop, takeRight and dropRight with bounds landing in every slice and at every slice boundary */
    @Test
    public void slicesLandingInEveryPrefixDataAndSuffix() {
        final Run run = new Run(400_000);
        final List<Pair> fixtures = new ArrayList<>();
        for (int size : new int[] { 0, 1, 2, 31, 32, 33, 64, 1023, 1024, 1025, 2000, 32767, 32768, 32769, 40_000 }) {
            for (History history : History.values()) {
                if (size > 2000 && history != History.BUILDER && history != History.SLICED) {
                    continue;
                }
                fixtures.add(run.checked(() -> build(run, size, history)));
            }
        }
        for (Pair p : fixtures) {
            sliceSweep(run, p, p.v.length() > 2000 ? 16 : 30);
        }
        for (int size : LARGE_SIZES) {
            final Pair p = run.checked(() -> build(run, size, History.BUILDER));
            sliceSweep(run, p, 10);
            // a Vector5 with non-trivial prefixes
            final Pair prepended = run.checked(() -> run.record(p.r.prepended(-1), p.v.prepend(-1)));
            sliceSweep(run, prepended, 8);
        }
        run.recheckHistory();
    }

    private static void sliceSweep(Run run, Pair p, int maxPoints) {
        final int n = p.v.length();
        final TreeSet<Integer> points = new TreeSet<>();
        points.add(0);
        points.add(n);
        for (int i = 0; i < p.r.vectorSliceCount(); i++) {
            final int boundary = p.r.vectorSlicePrefixLength(i);
            for (int d = -1; d <= 1; d++) {
                if (boundary + d >= 0 && boundary + d <= n) {
                    points.add(boundary + d);
                }
            }
        }
        while (points.size() < maxPoints && points.size() < n + 1) {
            points.add(run.rnd.nextInt(n + 1));
        }
        final List<Integer> list = new ArrayList<>(points);
        if (list.size() > maxPoints) {
            // keep the slice boundaries of the outer slices and a spread of the rest
            final List<Integer> kept = new ArrayList<>();
            for (int i = 0; i < maxPoints; i++) {
                kept.add(list.get((int) ((long) i * (list.size() - 1) / (maxPoints - 1))));
            }
            list.clear();
            list.addAll(new TreeSet<>(kept));
        }
        for (int lo : list) {
            run.checked(() -> {
                run.log(p.label + " take/drop/takeRight/dropRight " + lo);
                run.record(p.r.take(lo), p.v.take(lo));
                run.record(p.r.drop(lo), p.v.drop(lo));
                run.record(p.r.takeRight(lo), p.v.takeRight(lo));
                return run.record(p.r.dropRight(lo), p.v.dropRight(lo));
            });
            for (int hi : list) {
                if (hi > lo) {
                    run.checked(() -> {
                        run.log(p.label + " slice " + lo + " " + hi);
                        return run.record(p.r.slice(lo, hi), p.v.slice(lo, hi));
                    });
                }
            }
        }
    }

    /* every level transition, appending from empty and prepending from empty: the shapes go up one at a time */
    @Test
    public void levelTransitionsByAppendAndPrepend() {
        final Run run = new Run(500_000);
        for (boolean append : new boolean[] { true, false }) {
            RadixVector<Integer> r = RadixVector.empty();
            Vector<Integer> v = Vector.empty();
            int depth = 0;
            final List<Integer> transitions = new ArrayList<>();
            for (int i = 0; i < 33_000; i++) {
                final Integer x = run.fresh();
                r = append ? r.appended(x) : r.prepended(x);
                v = append ? v.append(x) : v.prepend(x);
                final int d = depth(r);
                if (d != depth) {
                    assertThat(d).as("depth after %d elements", i + 1).isEqualTo(depth + 1);
                    depth = d;
                    transitions.add(i + 1);
                }
                checkShape(r);
                if ((i & (i + 1)) == 0 || (i % 1024) == 1023 || i < 70) {
                    checkContents(new Pair(r, v, "transition"));
                }
            }
            checkContents(new Pair(r, v, "transition"));
            assertThat(depth).isEqualTo(4);
            assertThat(transitions).hasSize(4);
        }
        // Vector4 to Vector5, both ways, from a builder-made vector whose data is full
        for (boolean append : new boolean[] { true, false }) {
            final Pair start = run.checked(() -> build(run, (1 << 20) - 5, History.BUILDER));
            assertThat(start.r).isInstanceOf(RadixVector.Vector4.class);
            Pair p = start;
            for (int i = 0; i < 10; i++) {
                final Pair current = p;
                p = run.checked(() -> {
                    final Integer x = run.fresh();
                    run.log(append ? "appended" : "prepended");
                    return append ? run.record(current.r.appended(x), current.v.append(x))
                                  : run.record(current.r.prepended(x), current.v.prepend(x));
                });
            }
            assertThat(p.r).isInstanceOf(RadixVector.Vector5.class);
        }
        run.recheckHistory();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Vector6, reached by sharing (2^25 + 1 elements, but only 2^20 distinct leaves' worth of memory)

    @Test
    public void vector6ReachedBySelfConcatenation() {
        final int base = 1 << 20;
        final VectorBuilder<Integer> builder = RadixVector.newBuilder();
        for (int i = 0; i < base; i++) {
            builder.add(i);
        }
        RadixVector<Integer> v = builder.result();
        final List<RadixVector<Integer>> history = new ArrayList<>();
        history.add(v);
        for (int k = 0; k < 5; k++) {
            v = v.appendedAll(v);
            checkShape(v);
            history.add(v);
        }
        assertThat(v.length()).isEqualTo(1 << 25);
        assertThat(v).isInstanceOf(RadixVector.Vector5.class);
        checkModulo(v, base, 0);

        final RadixVector<Integer> appended = v.appended(-1);
        assertThat(appended).isInstanceOf(RadixVector.Vector6.class);
        checkShape(appended);
        assertThat(appended.last()).isEqualTo(-1);
        checkModulo(appended.init(), base, 0);

        final RadixVector<Integer> prepended = v.prepended(-2);
        assertThat(prepended).isInstanceOf(RadixVector.Vector6.class);
        checkShape(prepended);
        assertThat(prepended.head()).isEqualTo(-2);
        checkModulo(prepended.tail(), base, 0);

        // operations on a Vector6: updates in every slice, slices, tail and init, bulk append and prepend
        RadixVector<Integer> w = prepended.appended(-3);
        checkShape(w);
        final int n = w.length();
        for (int i = 0; i < w.vectorSliceCount(); i++) {
            final int at = Math.min(w.vectorSlicePrefixLength(i), n - 1);
            final RadixVector<Integer> updated = w.updated(at, -4);
            checkShape(updated);
            assertThat(updated.get(at)).isEqualTo(-4);
            assertThat(w.get(at)).isNotEqualTo(-4);
            if (at > 0) {
                assertThat(updated.get(at - 1)).isEqualTo(w.get(at - 1));
            }
        }
        for (int lo : new int[] { 1, 31, 33, 1025, 32769, (1 << 20) + 1, (1 << 25) - 7 }) {
            final RadixVector<Integer> dropped = w.drop(lo);
            checkShape(dropped);
            assertThat(dropped.length()).isEqualTo(n - lo);
            assertThat(dropped.head()).isEqualTo(w.get(lo));
            assertThat(dropped.last()).isEqualTo(-3);
            final RadixVector<Integer> taken = w.take(lo);
            checkShape(taken);
            assertThat(taken.length()).isEqualTo(lo);
            assertThat(taken.last()).isEqualTo(w.get(lo - 1));
            final RadixVector<Integer> sliced = w.slice(lo, n - lo);
            checkShape(sliced);
        }
        RadixVector<Integer> t = w;
        for (int i = 0; i < 40; i++) {
            t = t.tail().init();
            checkShape(t);
        }
        assertThat(t.length()).isEqualTo(n - 80);
        final RadixVector<Integer> small = RadixVector.ofAll(new Object[] { 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 });
        final RadixVector<Integer> bulk = w.appendedAll(small).prependedAll(small);
        checkShape(bulk);
        assertThat(bulk.length()).isEqualTo(n + 2 * small.length());
        assertThat(bulk.get(0)).isEqualTo(7);
        assertThat(bulk.last()).isEqualTo(24);
        final RadixVector<Integer> mapped = w.map(x -> x + 1);
        checkShape(mapped);
        assertThat(mapped.get(12345)).isEqualTo(w.get(12345) + 1);

        // a Vector5 whose data does not align on the builder (the left side ends 32 elements short of a 2^20 boundary):
        // the data of dimension 5 is added one dimension lower
        final int cut = (1 << 25) - 32;
        final RadixVector<Integer> misaligned = v.dropRight(32).appendedAll(v);
        final RadixVector<Integer> misalignedPrefix = v.prependedAll(v.dropRight(32));
        for (RadixVector<Integer> m : List.of(misaligned, misalignedPrefix)) {
            assertThat(m).isInstanceOf(RadixVector.Vector6.class);
            assertThat(m.length()).isEqualTo(cut + (1 << 25));
            checkShape(m);
            checkModulo(m.take(cut), base, 0);
            checkModulo(m.drop(cut), base, 0);
        }

        // nothing above wrote into the shared arrays
        for (RadixVector<Integer> h : history) {
            checkShape(h);
            checkModulo(h, base, 0);
        }
        checkModulo(v, base, 0);
    }

    /* the element at index i is (i - shift) mod base, checked through the iterator and a sample of get */
    private static void checkModulo(RadixVector<Integer> v, int base, int shift) {
        int i = 0;
        final Iterator<Integer> it = v.iterator();
        while (it.hasNext()) {
            final int x = it.next();
            if (x != Math.floorMod(i - shift, base)) {
                throw new AssertionError("element " + i + " is " + x);
            }
            i++;
        }
        assertThat(i).isEqualTo(v.length());
        for (int j = 0; j < v.length(); j += 99_991) {
            assertThat(v.get(j)).isEqualTo(Math.floorMod(j - shift, base));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // edges

    @Test
    public void emptyVectorFailsLikeVector() {
        final RadixVector<Integer> r = RadixVector.empty();
        final Vector<Integer> v = Vector.empty();
        sameOutcome(() -> r.get(0), () -> v.get(0));
        sameOutcome(() -> r.updated(0, 1), () -> v.update(0, 1));
        sameOutcome(r::head, v::head);
        sameOutcome(r::last, v::last);
        sameOutcome(r::tail, v::tail);
        sameOutcome(r::init, v::init);
        assertThatThrownBy(r::head).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(r::tail).isInstanceOf(UnsupportedOperationException.class);
        assertThat(r.iterator().hasNext()).isFalse();
        assertThat(r.reverseIterator().hasNext()).isFalse();
        for (int k : new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE }) {
            assertThat(r.take(k).length()).isZero();
            assertThat(r.drop(k).length()).isZero();
            assertThat(r.takeRight(k).length()).isZero();
            assertThat(r.dropRight(k).length()).isZero();
            assertThat(r.slice(k, Integer.MAX_VALUE).length()).isZero();
        }
    }

    @Test
    public void extremeArgumentsBehaveLikeVector() {
        final Run run = new Run(600_000);
        for (int size : new int[] { 1, 33, 1025, 32769 }) {
            final Pair p = run.checked(() -> build(run, size, History.BUILDER));
            for (int k : new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1, size - 1, size, size + 1, Integer.MAX_VALUE }) {
                run.checked(() -> {
                    run.log("extreme " + k);
                    run.record(p.r.take(k), p.v.take(k));
                    run.record(p.r.drop(k), p.v.drop(k));
                    run.record(p.r.takeRight(k), p.v.takeRight(k));
                    run.record(p.r.dropRight(k), p.v.dropRight(k));
                    run.record(p.r.slice(k, Integer.MAX_VALUE), p.v.slice(k, Integer.MAX_VALUE));
                    run.record(p.r.slice(Integer.MIN_VALUE, k), p.v.slice(Integer.MIN_VALUE, k));
                    sameOutcome(() -> p.r.get(k), () -> p.v.get(k));
                    return run.same(p, () -> p.r.updated(k, -1), () -> p.v.update(k, -1));
                });
            }
        }
        run.recheckHistory();
    }

    @Test
    public void nullElementsAreRejected() {
        final RadixVector<Integer> one = RadixVector.of(1);
        final RadixVector<Integer> big = RadixVector.ofAll(sequence(0, 100));
        assertThatThrownBy(() -> RadixVector.of((Integer) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RadixVector.ofAll(new Object[] { 1, null })).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RadixVector.ofAll(nullAt(100, 70))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> one.appended(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> big.prepended(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> big.updated(50, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> big.map(x -> x == 77 ? null : x)).isInstanceOf(NullPointerException.class);
        final List<Integer> withNull = new ArrayList<>(java.util.Arrays.asList(1, 2, null));
        assertThatThrownBy(() -> big.appendedAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> big.prependedAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> one.appendedAll(withNull)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RadixVector.<Integer> newBuilder().add(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void callerArraysAreCopiedNotAdopted() {
        final String[] strings = { "a", "b", "c" };
        final RadixVector<Object> r = RadixVector.ofAll(strings);
        strings[0] = "z";
        assertThat(r.get(0)).isEqualTo("a");
        // a String[] adopted as a leaf would throw ArrayStoreException here
        final RadixVector<Object> updated = r.updated(1, 42).appended(43).prepended(44);
        assertThat(updated.get(2)).isEqualTo(42);
        final Object[] many = sequence(0, 100);
        final RadixVector<Object> big = RadixVector.ofAll(many);
        many[5] = -1;
        assertThat(big.get(5)).isEqualTo(5);
    }

    @Test
    public void builderIsSingleShot() {
        final VectorBuilder<Integer> builder = RadixVector.newBuilder();
        for (int i = 0; i < 100; i++) {
            builder.add(i);
        }
        final RadixVector<Integer> built = builder.result();
        assertThat(built.length()).isEqualTo(100);
        assertThatThrownBy(() -> builder.add(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(List.of(1))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addArray(new Object[] { 1 }, 0, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        for (int i = 0; i < 100; i++) {
            assertThat(built.get(i)).isEqualTo(i);
        }
        final VectorBuilder<Integer> empty = RadixVector.newBuilder();
        assertThat(empty.size()).isZero();
        assertThat(empty.result()).isSameAs(RadixVector.empty());
        assertThatThrownBy(() -> empty.add(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void builderStartingFromAVectorNeverWritesIntoIt() {
        final Run run = new Run(700_000);
        for (int size : new int[] { 1, 32, 33, 64, 1024, 1056, 32768, 32800 }) {
            for (History history : new History[] { History.BUILDER, History.PREPENDS, History.SLICED }) {
                final Pair p = run.checked(() -> build(run, size, history));
                run.checked(() -> {
                    run.log("builder from " + p.label);
                    final VectorBuilder<Integer> builder = RadixVector.newBuilder();
                    builder.addAll(p.r);
                    final List<Integer> more = new ArrayList<>();
                    for (int i = 0; i < 70; i++) {
                        final Integer x = run.fresh();
                        more.add(x);
                        builder.add(x);
                    }
                    return run.record(builder.result(), p.v.appendAll(more));
                });
            }
        }
        run.recheckHistory();
    }

    @Test
    public void ofAllOfAnIterable() {
        final Run run = new Run(800_000);
        for (int size : new int[] { 0, 1, 32, 33, 1025, 5000 }) {
            final List<Integer> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(run.fresh());
            }
            final Vector<Integer> expected = Vector.ofAll(list);
            run.checked(() -> run.record(RadixVector.ofAll(list), expected));
            run.checked(() -> run.record(RadixVector.ofAll(oneShot(list)), expected));
            final RadixVector<Integer> r = RadixVector.ofAll(list);
            assertThat(RadixVector.ofAll(r)).isSameAs(r);
        }
        run.recheckHistory();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // building values through different histories

    private enum History { BUILDER, BUILDER_CHUNKS, OF_ALL, APPENDS, PREPENDS, ALTERNATING, SLICED }

    private static Pair build(Run run, int size, History history) {
        final Random rnd = run.rnd;
        final String label = history + "(" + size + ")";
        switch (history) {
            case BUILDER -> {
                final List<Integer> list = run.freshList(size);
                final VectorBuilder<Integer> builder = RadixVector.newBuilder();
                for (Integer x : list) {
                    builder.add(x);
                }
                return run.record(builder.result(), Vector.ofAll(list), label);
            }
            case BUILDER_CHUNKS -> {
                final List<Integer> list = run.freshList(size);
                final VectorBuilder<Integer> builder = RadixVector.newBuilder();
                int i = 0;
                while (i < size) {
                    final int chunk = Math.min(size - i, 1 + rnd.nextInt(rnd.nextBoolean() ? 40 : 1500));
                    final List<Integer> part = list.subList(i, i + chunk);
                    switch (rnd.nextInt(4)) {
                        case 0 -> part.forEach(builder::add);
                        case 1 -> builder.addAll(new ArrayList<>(part));
                        case 2 -> builder.addArray(part.toArray(), 0, chunk);
                        default -> builder.addAll(RadixVector.ofAll(part.toArray()));
                    }
                    i += chunk;
                }
                return run.record(builder.result(), Vector.ofAll(list), label);
            }
            case OF_ALL -> {
                final List<Integer> list = run.freshList(size);
                return run.record(RadixVector.ofAll(list.toArray()), Vector.ofAll(list), label);
            }
            case APPENDS -> {
                RadixVector<Integer> r = RadixVector.empty();
                Vector<Integer> v = Vector.empty();
                for (int i = 0; i < size; i++) {
                    final Integer x = run.fresh();
                    r = r.appended(x);
                    v = v.append(x);
                }
                return run.record(r, v, label);
            }
            case PREPENDS -> {
                RadixVector<Integer> r = RadixVector.empty();
                Vector<Integer> v = Vector.empty();
                for (int i = 0; i < size; i++) {
                    final Integer x = run.fresh();
                    r = r.prepended(x);
                    v = v.prepend(x);
                }
                return run.record(r, v, label);
            }
            case ALTERNATING -> {
                RadixVector<Integer> r = RadixVector.empty();
                Vector<Integer> v = Vector.empty();
                for (int i = 0; i < size; i++) {
                    final Integer x = run.fresh();
                    if (rnd.nextBoolean()) {
                        r = r.appended(x);
                        v = v.append(x);
                    } else {
                        r = r.prepended(x);
                        v = v.prepend(x);
                    }
                }
                return run.record(r, v, label);
            }
            case SLICED -> {
                final int before = rnd.nextInt(rnd.nextBoolean() ? 40 : 2000);
                final int after = rnd.nextInt(rnd.nextBoolean() ? 40 : 2000);
                final Pair whole = build(run, before + size + after, rnd.nextBoolean() ? History.BUILDER : History.OF_ALL);
                return run.record(whole.r.slice(before, before + size), whole.v.slice(before, before + size), label);
            }
            default -> throw new IllegalStateException();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // operands of appendedAll and prependedAll

    private enum Kind { RADIX, LIST, ONE_SHOT }

    private record Operand(Kind kind, Pair pair) {

        Iterable<Integer> forRadix() {
            return switch (kind) {
                case RADIX -> pair.r;
                case LIST -> javaList(pair.v);
                case ONE_SHOT -> oneShot(javaList(pair.v));
            };
        }

        Iterable<Integer> forVector() {
            return switch (kind) {
                case RADIX -> pair.v;
                case LIST -> javaList(pair.v);
                case ONE_SHOT -> oneShot(javaList(pair.v));
            };
        }

        @Override
        public String toString() {
            return kind + " " + pair.label;
        }
    }

    private static final int[] OPERAND_SIZES = { 0, 1, 2, 5, 10, 14, 15, 16, 31, 32, 33, 64, 100, 1000, 1024, 1025, 3000 };

    private static Operand operand(Run run) {
        final Random rnd = run.rnd;
        final int size = (rnd.nextInt(60) == 0) ? 32769 : (rnd.nextBoolean() ? OPERAND_SIZES[rnd.nextInt(OPERAND_SIZES.length)] : rnd.nextInt(2000));
        final Kind kind = Kind.values()[rnd.nextInt(Kind.values().length)];
        final History history = (kind == Kind.RADIX && size <= 3000) ? History.values()[rnd.nextInt(History.values().length)] : History.BUILDER;
        return new Operand(kind, build(run, size, history));
    }

    private static <T> Iterable<T> oneShot(List<T> list) {
        final AtomicBoolean used = new AtomicBoolean();
        return () -> {
            if (used.getAndSet(true)) {
                throw new IllegalStateException("a one-shot iterable was iterated twice");
            }
            return list.iterator();
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // arguments

    private static int arg(Random rnd, int n) {
        return switch (rnd.nextInt(20)) {
            case 0 -> Integer.MIN_VALUE;
            case 1 -> Integer.MAX_VALUE;
            case 2 -> -1 - rnd.nextInt(5);
            case 3 -> n + rnd.nextInt(5);
            default -> rnd.nextInt(n + 1);
        };
    }

    private static int edgeIndex(Random rnd, int n) {
        return switch (rnd.nextInt(6)) {
            case 0 -> -1;
            case 1 -> n;
            case 2 -> Integer.MIN_VALUE;
            case 3 -> Integer.MAX_VALUE;
            case 4 -> n - 1;
            default -> (n == 0) ? 0 : rnd.nextInt(n);
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the run: seed, trail of operations, and every value reached

    private record Pair(RadixVector<Integer> r, Vector<Integer> v, String label) {
    }

    private static final class Run {

        final long seed;
        final Random rnd;
        int step = -1;
        private int next;
        private final ArrayDeque<String> trail = new ArrayDeque<>();
        private final List<Pair> history = new ArrayList<>();

        Run(long seed) {
            this.seed = seed;
            this.rnd = new Random(seed);
        }

        Integer fresh() {
            return next++;
        }

        List<Integer> freshList(int size) {
            final List<Integer> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(fresh());
            }
            return list;
        }

        void log(String operation) {
            trail.addLast(operation);
            if (trail.size() > 25) {
                trail.removeFirst();
            }
        }

        Pair record(RadixVector<Integer> r, Vector<Integer> v) {
            return record(r, v, shapeOf(r) + "(" + r.length() + ")");
        }

        Pair record(RadixVector<Integer> r, Vector<Integer> v, String label) {
            final Pair p = new Pair(r, v, label);
            check(p);
            history.add(p);
            return p;
        }

        /* both operations succeed and give the same contents, or both fail with the same exception class */
        Pair same(Pair old, Supplier<RadixVector<Integer>> radix, Supplier<Vector<Integer>> vector) {
            RadixVector<Integer> r = null;
            Vector<Integer> v = null;
            RuntimeException re = null;
            RuntimeException ve = null;
            try {
                r = radix.get();
            } catch (RuntimeException e) {
                re = e;
            }
            try {
                v = vector.get();
            } catch (RuntimeException e) {
                ve = e;
            }
            if (re != null || ve != null) {
                if (re == null || ve == null || re.getClass() != ve.getClass()) {
                    throw new AssertionError("different outcomes: radix " + (re == null ? "succeeded" : re) + ", vector " + (ve == null ? "succeeded" : ve));
                }
                return old;
            }
            return record(r, v);
        }

        Pair checked(Supplier<Pair> action) {
            try {
                return action.get();
            } catch (AssertionError | RuntimeException e) {
                throw new AssertionError("seed " + seed + ", step " + step + ", last operations " + trail + ": " + e, e);
            }
        }

        /* every value reached is compared again: an operation that wrote into a shared array shows up here */
        void recheckHistory() {
            for (Pair p : history) {
                try {
                    check(p);
                } catch (AssertionError | RuntimeException e) {
                    throw new AssertionError("seed " + seed + ": value " + p.label + " changed after it was built: " + e, e);
                }
            }
        }
    }

    private static void check(Pair p) {
        checkShape(p.r);
        checkContents(p);
    }

    private static void sameOutcome(Supplier<?> radix, Supplier<?> vector) {
        Object r = null;
        Object v = null;
        RuntimeException re = null;
        RuntimeException ve = null;
        try {
            r = radix.get();
        } catch (RuntimeException e) {
            re = e;
        }
        try {
            v = vector.get();
        } catch (RuntimeException e) {
            ve = e;
        }
        if (re != null || ve != null) {
            if (re == null || ve == null || re.getClass() != ve.getClass()) {
                throw new AssertionError("different outcomes: radix " + (re == null ? r : re) + ", vector " + (ve == null ? v : ve));
            }
        } else if (!java.util.Objects.equals(r, v)) {
            throw new AssertionError("different results: radix " + r + ", vector " + v);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // contents

    private static void checkContents(Pair p) {
        final RadixVector<Integer> r = p.r;
        final int n = p.v.length();
        if (r.length() != n) {
            throw new AssertionError("length " + r.length() + ", expected " + n);
        }
        if (r.isEmpty() != (n == 0)) {
            throw new AssertionError("isEmpty");
        }
        final Object[] expected = new Object[n];
        int k = 0;
        for (Integer x : p.v) {
            expected[k++] = x;
        }
        final Iterator<Integer> it = r.iterator();
        for (int i = 0; i < n; i++) {
            if (!it.hasNext()) {
                throw new AssertionError("iterator ends at " + i + " of " + n);
            }
            final Integer x = it.next();
            if (!expected[i].equals(x)) {
                throw new AssertionError("iterator at " + i + ": " + x + ", expected " + expected[i]);
            }
        }
        if (it.hasNext()) {
            throw new AssertionError("iterator goes beyond " + n);
        }
        final int[] index = { 0 };
        r.forEach(x -> {
            final int i = index[0]++;
            if (i >= n || !expected[i].equals(x)) {
                throw new AssertionError("forEach at " + i + ": " + x);
            }
        });
        if (index[0] != n) {
            throw new AssertionError("forEach visits " + index[0] + " of " + n);
        }
        final Object[] copy = new Object[n];
        r.copyToArray(copy, 0, n);
        if (!java.util.Arrays.equals(copy, expected)) {
            throw new AssertionError("copyToArray");
        }
        if (n <= 2048) {
            for (int i = 0; i < n; i++) {
                checkGet(r, i, expected);
            }
            final Iterator<Integer> reverse = r.reverseIterator();
            for (int i = n - 1; i >= 0; i--) {
                if (!expected[i].equals(reverse.next())) {
                    throw new AssertionError("reverseIterator at " + i);
                }
            }
            if (reverse.hasNext()) {
                throw new AssertionError("reverseIterator goes beyond " + n);
            }
        } else {
            for (int i = 0; i < n; i += 1 + n / 512) {
                checkGet(r, i, expected);
            }
            for (int s = 0; s < r.vectorSliceCount(); s++) {
                final int boundary = r.vectorSlicePrefixLength(s);
                for (int i = boundary - 1; i <= boundary; i++) {
                    if (i >= 0 && i < n) {
                        checkGet(r, i, expected);
                    }
                }
            }
            checkGet(r, n - 1, expected);
        }
        if (n > 0) {
            if (!expected[0].equals(r.head()) || !expected[n - 1].equals(r.last())) {
                throw new AssertionError("head or last");
            }
        }
    }

    private static void checkGet(RadixVector<Integer> r, int i, Object[] expected) {
        final Integer x = r.get(i);
        if (!expected[i].equals(x)) {
            throw new AssertionError("get(" + i + "): " + x + ", expected " + expected[i]);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // shape invariants

    private static String shapeOf(RadixVector<?> r) {
        return r.getClass().getSimpleName();
    }

    private static int depth(RadixVector<?> r) {
        return (r.vectorSliceCount() + 1) / 2;
    }

    /*
     * Vector0 is empty; Vector1 holds 1 to 32 elements. From Vector2 up: prefix1 and suffix1 hold 1 to 32 elements, the
     * other prefixes and suffixes 0 to 31 entries, the data 0 to 30 entries (62 for data6); every array below the top of
     * a slice is full (32 entries); the running lengths len1, len12, .. and the element count agree with the arrays.
     */
    private static void checkShape(RadixVector<?> r) {
        final int c = r.vectorSliceCount();
        final int depth = depth(r);
        switch (r) {
            case RadixVector.Vector0<?> v0 -> {
                assertInvariant(c == 0 && v0.length() == 0 && v0.prefix1.length == 0 && v0.suffix1.length == 0, r, "Vector0 is empty");
                assertInvariant(r == RadixVector.empty(), r, "Vector0 is a singleton");
                return;
            }
            case RadixVector.Vector1<?> v1 -> assertInvariant(c == 1 && v1.length() >= 1 && v1.length() <= WIDTH, r, "Vector1 holds 1 to 32 elements");
            case RadixVector.Vector2<?> v -> {
                assertInvariant(c == 3, r, "slice count");
                assertInvariant(v.len1 == v.prefix1.length, r, "len1");
            }
            case RadixVector.Vector3<?> v -> {
                assertInvariant(c == 5, r, "slice count");
                assertInvariant(v.len1 == v.prefix1.length && v.len12 == v.len1 + v.prefix2.length * WIDTH, r, "len1, len12");
            }
            case RadixVector.Vector4<?> v -> {
                assertInvariant(c == 7, r, "slice count");
                assertInvariant(v.len1 == v.prefix1.length && v.len12 == v.len1 + v.prefix2.length * WIDTH
                                && v.len123 == v.len12 + v.prefix3.length * WIDTH * WIDTH, r, "len1, len12, len123");
            }
            case RadixVector.Vector5<?> v -> {
                assertInvariant(c == 9, r, "slice count");
                assertInvariant(v.len1 == v.prefix1.length && v.len12 == v.len1 + v.prefix2.length * WIDTH
                                && v.len123 == v.len12 + v.prefix3.length * (1 << 10)
                                && v.len1234 == v.len123 + v.prefix4.length * (1 << 15), r, "len1 .. len1234");
            }
            case RadixVector.Vector6<?> v -> {
                assertInvariant(c == 11, r, "slice count");
                assertInvariant(v.len1 == v.prefix1.length && v.len12 == v.len1 + v.prefix2.length * WIDTH
                                && v.len123 == v.len12 + v.prefix3.length * (1 << 10)
                                && v.len1234 == v.len123 + v.prefix4.length * (1 << 15)
                                && v.len12345 == v.len1234 + v.prefix5.length * (1 << 20), r, "len1 .. len12345");
            }
        }
        if (r instanceof RadixVector.BigVector<?> big) {
            assertInvariant(big.suffix1 == r.vectorSlice(c - 1), r, "suffix1 is the last slice");
        }
        assertInvariant(r.prefix1 == r.vectorSlice(0), r, "prefix1 is the first slice");
        long count = 0;
        for (int i = 0; i < c; i++) {
            final int dim = vectorSliceDim(c, i);
            final Object[] slice = r.vectorSlice(i);
            assertInvariant(slice.getClass() == Object[].class, r, "slice " + i + " is an Object[]");
            if (dim == 1) {
                assertInvariant(slice.length >= 1 && slice.length <= WIDTH, r, "slice " + i + " (a leaf) holds 1 to 32 elements");
                checkLeaf(slice, r);
                count += slice.length;
            } else {
                final int max = (dim == depth) ? (depth == 6 ? 2 * WIDTH - 2 : WIDTH - 2) : WIDTH - 1;
                assertInvariant(slice.length <= max, r, "slice " + i + " of dimension " + dim + " holds at most " + max);
                for (Object child : slice) {
                    checkFull(dim - 1, child, r);
                }
                count += (long) slice.length << (5 * (dim - 1));
            }
            assertInvariant(r.vectorSlicePrefixLength(i) == count, r, "vectorSlicePrefixLength(" + i + ") = " + r.vectorSlicePrefixLength(i) + ", counted " + count);
        }
        assertInvariant(count == r.length(), r, "length " + r.length() + ", counted " + count);
        final long capacity = capacity(depth);
        assertInvariant(r.length() <= capacity, r, "a Vector" + depth + " holds at most " + capacity);
        assertInvariant(depth <= 1 || r.length() >= 2, r, "a Vector" + depth + " has a non-empty prefix1 and suffix1");
    }

    /* the most elements a VectorN can hold */
    private static long capacity(int depth) {
        if (depth <= 1) {
            return depth * WIDTH;
        }
        long c = 2L * WIDTH;
        for (int d = 2; d < depth; d++) {
            c += 2L * (WIDTH - 1) << (5 * (d - 1));
        }
        c += (long) (depth == 6 ? 2 * WIDTH - 2 : WIDTH - 2) << (5 * (depth - 1));
        return c;
    }

    private static void checkFull(int level, Object a, RadixVector<?> r) {
        assertInvariant(a instanceof Object[] array && array.getClass() == Object[].class && array.length == WIDTH, r,
            "an inner array of level " + level + " is a full Object[]");
        final Object[] array = (Object[]) a;
        if (level == 1) {
            checkLeaf(array, r);
        } else {
            for (Object child : array) {
                checkFull(level - 1, child, r);
            }
        }
    }

    private static void checkLeaf(Object[] leaf, RadixVector<?> r) {
        for (Object element : leaf) {
            if (!(element instanceof Integer) && !(element instanceof String)) {
                throw new AssertionError(shapeOf(r) + "(" + r.length() + "): a leaf holds " + element);
            }
        }
    }

    private static void assertInvariant(boolean holds, RadixVector<?> r, String invariant) {
        if (!holds) {
            throw new AssertionError(shapeOf(r) + "(" + r.length() + "): " + invariant);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private static List<Integer> javaList(Vector<Integer> v) {
        final List<Integer> list = new ArrayList<>(v.length());
        for (Integer x : v) {
            list.add(x);
        }
        return list;
    }

    private static Object[] sequence(int from, int until) {
        final Object[] a = new Object[until - from];
        for (int i = 0; i < a.length; i++) {
            a[i] = from + i;
        }
        return a;
    }

    private static Object[] nullAt(int size, int index) {
        final Object[] a = sequence(0, size);
        a[index] = null;
        return a;
    }
}
