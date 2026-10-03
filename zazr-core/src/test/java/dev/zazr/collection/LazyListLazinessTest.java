package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// What each operation evaluates, counted: a call evaluates nothing (or what its note says, for the operations that
/// read the whole list or check bounds), and reading the first element of the result evaluates exactly the cells it
/// needs. Every public method returning a LazyList or a tuple of them has a row, which the coverage test checks.
class LazyListLazinessTest {

    private static final int SIZE = 10;

    /// The list 0, 1, ..., 9 counting its evaluated cells in `evaluated`.
    private static LazyList<Integer> counted(AtomicInteger evaluated) {
        return LazyList.tabulate(SIZE, i -> {
            evaluated.incrementAndGet();
            return i;
        });
    }

    /// What reading the first element of a result reads: the head of a LazyList, or of the first LazyList of a tuple.
    private static Object readFirst(Object result) {
        return switch (result) {
            case LazyList<?> list -> list.isEmpty() ? "empty" : list.head();
            case Tuple2<?, ?> pair -> readFirst(pair._1());
            case Tuple3<?, ?, ?> triple -> readFirst(triple._1());
            default -> throw new AssertionError("not a LazyList or a tuple: " + result);
        };
    }

    /// A row: the signature of the method, the call, the cells the call evaluates, and the cells evaluated once the
    /// first element of the result is read.
    private record Row(String signature, Function<LazyList<Integer>, Object> call, int afterCall, int afterFirst) {}

    private static final java.util.List<Row> ROWS = new ArrayList<>();

    private static void lazy(String signature, int afterFirst, Function<LazyList<Integer>, Object> call) {
        ROWS.add(new Row(signature, call, 0, afterFirst));
    }

    private static void eager(
            String signature, int afterCall, int afterFirst, Function<LazyList<Integer>, Object> call) {
        ROWS.add(new Row(signature, call, afterCall, afterFirst));
    }

    static {
        Comparator<Integer> natural = Comparator.naturalOrder();
        lazy("append(Object)", 1, l -> l.append(99));
        lazy("appendAll(Iterable)", 1, l -> l.appendAll(List.of(99)));
        lazy("appendSelf(Function)", 1, l -> l.appendSelf(Function.identity()));
        lazy("as(Object)", 1, l -> l.as(7));
        lazy("collect(Function)", 2, l -> l.collect(x -> x % 2 == 1 ? Option.some(x) : Option.none()));
        lazy("combinations()", SIZE, LazyList::combinations);
        lazy("combinations(int)", 2, l -> l.combinations(2));
        lazy("crossProduct()", 1, LazyList::crossProduct);
        lazy("crossProduct(Iterable)", 1, l -> l.crossProduct(List.of(1, 2)));
        lazy("crossProduct(int)", 1, l -> l.crossProduct(2));
        lazy("cycle()", 1, LazyList::cycle);
        lazy("cycle(int)", 1, l -> l.cycle(2));
        lazy("distinct()", 1, LazyList::distinct);
        lazy("distinctBy(Comparator)", 1, l -> l.distinctBy(natural));
        lazy("distinctBy(Function)", 1, l -> l.distinctBy(x -> x));
        lazy("distinctByKeepLast(Comparator)", SIZE, l -> l.distinctByKeepLast(natural));
        lazy("distinctByKeepLast(Function)", SIZE, l -> l.distinctByKeepLast(x -> x));
        lazy("drop(int)", 4, l -> l.drop(3));
        lazy("dropRight(int)", 4, l -> l.dropRight(3));
        lazy("dropRightUntil(Predicate)", SIZE, l -> l.dropRightUntil(x -> x < 5));
        lazy("dropRightWhile(Predicate)", SIZE, l -> l.dropRightWhile(x -> x > 5));
        lazy("dropUntil(Predicate)", 4, l -> l.dropUntil(x -> x == 3));
        lazy("dropWhile(Predicate)", 4, l -> l.dropWhile(x -> x < 3));
        lazy("duplicates()", SIZE, LazyList::duplicates);
        lazy("duplicatesBy(Function)", SIZE, l -> l.duplicatesBy(x -> x / 2));
        lazy("extend(Function)", 1, l -> l.extend(x -> x + 1));
        lazy("extend(Object)", 1, l -> l.extend(7));
        lazy("extend(Supplier)", 1, l -> l.extend(() -> 7));
        lazy("filter(Predicate)", 3, l -> l.filter(x -> x == 2));
        lazy("flatMap(Function)", 1, l -> l.flatMap(x -> List.of(x, x)));
        lazy("grouped(int)", 1, l -> l.grouped(3));
        eager("init()", 1, 2, LazyList::init);
        lazy("insert(int, Object)", 1, l -> l.insert(2, 99));
        lazy("insertAll(int, Iterable)", 1, l -> l.insertAll(2, List.of(99)));
        lazy("intersperse(Object)", 1, l -> l.intersperse(99));
        lazy("leftPadTo(int, Object)", SIZE, l -> l.leftPadTo(12, 99));
        lazy("map(Function)", 1, l -> l.map(x -> x + 1));
        lazy("orElse(Iterable)", 1, l -> l.orElse(List.of(99)));
        lazy("orElse(Supplier)", 1, l -> l.orElse(() -> List.of(99)));
        lazy("padTo(int, Object)", 1, l -> l.padTo(12, 99));
        lazy("partition(Predicate)", 2, l -> l.partition(x -> x == 1));
        lazy("partitionMap(Function)", 2, l -> l.partitionMap(x -> x == 1 ? Either.left(x) : Either.right(x)));
        lazy("patch(int, Iterable, int)", 1, l -> l.patch(2, List.of(99), 3));
        lazy("permutations()", SIZE, LazyList::permutations);
        lazy("prepend(Object)", 0, l -> l.prepend(99));
        lazy("prependAll(Iterable)", 0, l -> l.prependAll(List.of(99)));
        lazy("reject(Predicate)", 3, l -> l.reject(x -> x < 2));
        lazy("remove(Object)", 2, l -> l.remove(0));
        lazy("removeAll(Iterable)", 3, l -> l.removeAll(List.of(0, 1)));
        lazy("removeAll(Object)", 2, l -> l.removeAll(0));
        lazy("removeAll(Predicate)", 3, l -> l.reject(x -> x < 2));
        lazy("removeAt(int)", 2, l -> l.removeAt(0));
        lazy("removeFirst(Predicate)", 2, l -> l.removeFirst(x -> x == 0));
        lazy("removeLast(Predicate)", SIZE, l -> l.removeLast(x -> x == 0));
        lazy("replace(Object, Object)", 1, l -> l.replace(0, 99));
        lazy("replaceAll(Object, Object)", 1, l -> l.replaceAll(0, 99));
        lazy("retainAll(Iterable)", 3, l -> l.retainAll(List.of(2, 3)));
        eager("reverse()", SIZE, SIZE, LazyList::reverse);
        lazy("rotateLeft(int)", SIZE, l -> l.rotateLeft(3));
        lazy("rotateRight(int)", SIZE, l -> l.rotateRight(3));
        lazy("scan(Object, BiFunction)", 0, l -> l.scan(0, Integer::sum));
        lazy("scanLeft(Object, BiFunction)", 0, l -> l.scanLeft(0, Integer::sum));
        eager("scanRight(Object, BiFunction)", SIZE, SIZE, l -> l.scanRight(0, Integer::sum));
        eager("shuffle()", SIZE, SIZE, LazyList::shuffle);
        lazy("slice(int, int)", 3, l -> l.slice(2, 5));
        lazy("slideBy(Function)", 3, l -> l.slideBy(x -> x / 2));
        lazy("sliding(int)", 1, l -> l.sliding(2));
        lazy("sliding(int, int)", 1, l -> l.sliding(2, 3));
        eager("sortBy(Comparator, Function)", SIZE, SIZE, l -> l.sortBy(natural, x -> -x));
        eager("sortBy(Function)", SIZE, SIZE, l -> l.sortBy(x -> -x));
        eager("sorted()", SIZE, SIZE, LazyList::sorted);
        eager("sorted(Comparator)", SIZE, SIZE, l -> l.sorted(natural));
        lazy("span(Predicate)", 1, l -> l.span(x -> x < 3));
        lazy("splitAt(Predicate)", 1, l -> l.splitAt(x -> x == 3));
        lazy("splitAt(int)", 1, l -> l.splitAt(3));
        lazy("splitAtInclusive(Predicate)", 4, l -> l.splitAtInclusive(x -> x == 3));
        eager("subSequence(int)", 3, 4, l -> l.subSequence(3));
        eager("subSequence(int, int)", 4, 4, l -> l.subSequence(3, 5));
        eager("tail()", 1, 2, LazyList::tail);
        lazy("take(int)", 1, l -> l.take(3));
        lazy("takeRight(int)", SIZE, l -> l.takeRight(3));
        lazy("takeRightUntil(Predicate)", SIZE, l -> l.takeRightUntil(x -> x == 5));
        lazy("takeRightWhile(Predicate)", SIZE, l -> l.takeRightWhile(x -> x > 5));
        lazy("takeUntil(Predicate)", 1, l -> l.takeUntil(x -> x == 3));
        lazy("takeWhile(Predicate)", 1, l -> l.takeWhile(x -> x < 3));
        lazy("tap(Consumer)", 1, l -> l.tap(x -> {}));
        lazy("toLazyList()", 1, LazyList::toLazyList);
        lazy("unzip(Function)", 1, l -> l.unzip(x -> Tuple.of(x, -x)));
        lazy("unzip3(Function)", 1, l -> l.unzip3(x -> Tuple.of(x, -x, x)));
        lazy("update(int, Function)", 1, l -> l.update(2, (Integer x) -> -x));
        lazy("update(int, Object)", 1, l -> l.update(2, 99));
        lazy("zip(Iterable)", 1, l -> l.zip(List.of(1, 2)));
        lazy("zipAll(Iterable, Object, Object)", 1, l -> l.zipAll(List.of(1, 2), 0, 0));
        lazy("zipWith(Iterable, BiFunction)", 1, l -> l.zipWith(List.of(1, 2), Integer::sum));
        lazy("zipWithIndex()", 1, LazyList::zipWithIndex);
        lazy("zipWithIndex(BiFunction)", 1, l -> l.zipWithIndex(Integer::sum));
    }

    @Test
    void everyOperationEvaluatesOnlyWhatItsRowSays() {
        java.util.List<String> wrong = new ArrayList<>();
        for (Row row : ROWS) {
            AtomicInteger evaluated = new AtomicInteger();
            LazyList<Integer> source = counted(evaluated);
            Object result = row.call().apply(source);
            int afterCall = evaluated.get();
            readFirst(result);
            int afterFirst = evaluated.get();
            // memoised: reading it again evaluates nothing more
            readFirst(result);
            int again = evaluated.get();
            if (afterCall != row.afterCall() || afterFirst != row.afterFirst() || again != afterFirst) {
                wrong.add(row.signature() + ": " + afterCall + "/" + afterFirst + "/" + again + ", expected "
                        + row.afterCall() + "/" + row.afterFirst());
            }
        }
        assertThat(wrong).as("call/first read/second read").isEmpty();
    }

    @Test
    void everyOperationReturningALazyListHasARow() {
        TreeSet<String> signatures = new TreeSet<>();
        for (Method method : LazyList.class.getMethods()) {
            String returned = method.getReturnType().getName();
            boolean returnsLists = returned.equals(LazyList.class.getName()) || returned.startsWith("dev.zazr.Tuple");
            if (returnsLists && !Modifier.isStatic(method.getModifiers()) && !method.isBridge()) {
                signatures.add(signature(method));
            }
        }
        for (Row row : ROWS) {
            signatures.remove(row.signature());
        }
        assertThat(signatures).as("operations with no row").isEmpty();
    }

    private static String signature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append('(');
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            builder.append(i > 0 ? ", " : "").append(types[i].getSimpleName());
        }
        return builder.append(')').toString();
    }

    // -- construction

    /// The operations whose result depends on the whole list: those returning a LazyList compute nothing at the call
    /// and the whole list on the first read, as their notes say; groupBy computes the whole list at the call.
    @Test
    void theOperationsThatNeedTheWholeListComputeItWhenTheirNotesSay() {
        Comparator<Integer> byParity = Comparator.comparing(x -> x % 2);
        LinkedHashMap<String, Function<LazyList<Integer>, LazyList<Integer>>> lazyCalls = new LinkedHashMap<>();
        lazyCalls.put("duplicates()", LazyList::duplicates);
        lazyCalls.put("duplicatesBy(Function)", l -> l.duplicatesBy(x -> x % 2));
        lazyCalls.put("distinctByKeepLast(Comparator)", l -> l.distinctByKeepLast(byParity));
        lazyCalls.put("distinctByKeepLast(Function)", l -> l.distinctByKeepLast(x -> x % 2));
        lazyCalls.put("dropRightUntil(Predicate)", l -> l.dropRightUntil(x -> x == 3));
        lazyCalls.put("dropRightWhile(Predicate)", l -> l.dropRightWhile(x -> x > 3));
        lazyCalls.put("leftPadTo(int, Object)", l -> l.leftPadTo(SIZE + 2, 99));
        lazyCalls.put("removeLast(Predicate)", l -> l.removeLast(x -> x == 2));
        lazyCalls.put("rotateLeft(int)", l -> l.rotateLeft(2));
        lazyCalls.put("rotateRight(int)", l -> l.rotateRight(2));
        lazyCalls.put("takeRight(int)", l -> l.takeRight(2));
        lazyCalls.put("takeRightUntil(Predicate)", l -> l.takeRightUntil(x -> x == 3));
        lazyCalls.put("takeRightWhile(Predicate)", l -> l.takeRightWhile(x -> x > 3));
        java.util.Map<String, String> counts = new LinkedHashMap<>();
        lazyCalls.forEach((signature, call) -> {
            AtomicInteger evaluated = new AtomicInteger();
            LazyList<Integer> result = call.apply(counted(evaluated));
            int afterCall = evaluated.get();
            result.headOption();
            counts.put(signature, afterCall + "/" + evaluated.get());
        });
        AtomicInteger grouped = new AtomicInteger();
        counted(grouped).groupBy(x -> x % 2);
        counts.put("groupBy(Function)", grouped.get() + "/-");

        java.util.Map<String, String> expected = new LinkedHashMap<>();
        lazyCalls.keySet().forEach(signature -> expected.put(signature, "0/" + SIZE));
        expected.put("groupBy(Function)", SIZE + "/-");
        assertThat(counts)
                .as("cells evaluated at the call/after the first read")
                .containsExactlyEntriesOf(expected);
    }

    @Test
    void theFactoriesEvaluateNothing() {
        AtomicInteger calls = new AtomicInteger();
        LinkedHashMap<String, LazyList<?>> built = new LinkedHashMap<>();
        built.put("continually(Supplier)", LazyList.continually(calls::incrementAndGet));
        built.put("iterate(Object, Function)", LazyList.iterate(0, x -> calls.incrementAndGet()));
        built.put("iterate(Supplier)", LazyList.iterate(() -> Option.some(calls.incrementAndGet())));
        built.put("tabulate(int, Function)", LazyList.tabulate(3, x -> calls.incrementAndGet()));
        built.put("fill(int, Supplier)", LazyList.fill(3, calls::incrementAndGet));
        built.put("unfold(Object, Function)", LazyList.unfold(0, x -> {
            calls.incrementAndGet();
            return Option.some(Tuple.of(x + 1, x));
        }));
        built.put("cons(Object, Supplier)", LazyList.cons(0, () -> {
            calls.incrementAndGet();
            return LazyList.empty();
        }));
        built.put("defer(Supplier)", LazyList.defer(() -> {
            calls.incrementAndGet();
            return LazyList.of(1);
        }));
        built.put("ofAll(Iterable)", LazyList.ofAll(countingIterable(calls)));
        built.put(
                "ofAll(Stream)",
                LazyList.ofAll(
                        java.util.stream.Stream.generate(calls::incrementAndGet).limit(3)));
        built.put("concat(Iterable[])", LazyList.concat(countingIterable(calls), countingIterable(calls)));
        built.put("concat(Iterable)", LazyList.concat(List.of(countingIterable(calls))));
        built.put("flatten(Iterable)", LazyList.flatten(List.of(countingIterable(calls))));
        assertThat(calls.get()).isZero();
        for (var entry : built.entrySet()) {
            // cons is given its head: only its tail waits
            String expected =
                    entry.getKey().startsWith("cons") ? "LazyList(0, <not computed>)" : "LazyList(<not computed>)";
            assertThat(entry.getValue().toString()).as(entry.getKey()).isEqualTo(expected);
        }
    }

    /// An Iterable counting the calls to iterator(), hasNext() and next().
    private static Iterable<Integer> countingIterable(AtomicInteger calls) {
        return () -> {
            calls.incrementAndGet();
            return new java.util.Iterator<>() {
                int next;

                @Override
                public boolean hasNext() {
                    calls.incrementAndGet();
                    return next < 3;
                }

                @Override
                public Integer next() {
                    calls.incrementAndGet();
                    return next++;
                }
            };
        };
    }

    @Test
    void aLazyHeadIsComputedWhenRead() {
        AtomicInteger calls = new AtomicInteger();
        LazyList<Integer> list = LazyList.defer(() -> LazyList.cons(calls.incrementAndGet(), LazyList::empty));
        assertThat(calls.get()).isZero();
        assertThat(list.toString()).isEqualTo("LazyList(<not computed>)");
        assertThat(list.isEmpty()).isFalse();
        assertThat(calls.get()).isEqualTo(1);
        assertThat(list.head()).isEqualTo(1);
        assertThat(list.toString()).isEqualTo("LazyList(1, <not computed>)");
        assertThat(list.tail().isEmpty()).isTrue();
        assertThat(list.toString()).isEqualTo("LazyList(1)");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void iteratingEvaluatesOneCellPerElement() {
        AtomicInteger evaluated = new AtomicInteger();
        java.util.Iterator<Integer> iterator = counted(evaluated).iterator();
        assertThat(evaluated.get()).isZero();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(evaluated.get()).isEqualTo(1);
        assertThat(iterator.next()).isZero();
        assertThat(evaluated.get()).isEqualTo(1);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(evaluated.get()).isEqualTo(2);
    }

    // -- threads

    @Test
    void aCellIsEvaluatedOnceWhateverTheNumberOfThreadsReadingIt() throws Exception {
        for (int round = 0; round < 50; round++) {
            AtomicInteger calls = new AtomicInteger();
            LazyList<Integer> list = LazyList.defer(() -> {
                calls.incrementAndGet();
                Thread.yield();
                return LazyList.of(calls.get());
            });
            LazyList<Integer> mapped = list.map(x -> x * 10).appendAll(List.of(7));
            int threads = 16;
            CountDownLatch start = new CountDownLatch(1);
            java.util.List<Thread> started = new ArrayList<>();
            java.util.concurrent.ConcurrentLinkedQueue<Object> seen =
                    new java.util.concurrent.ConcurrentLinkedQueue<>();
            for (int i = 0; i < threads; i++) {
                started.add(Thread.ofVirtual().start(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    seen.add(mapped.toVector());
                }));
            }
            start.countDown();
            for (Thread thread : started) {
                thread.join();
            }
            assertThat(calls.get()).isEqualTo(1);
            assertThat(seen).hasSize(threads).containsOnly(Vector.of(10, 7));
        }
    }

    @Test
    void aFailureIsTheSameInstanceOnEveryThread() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        LazyList<Integer> list = LazyList.defer(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("failed once");
        });
        java.util.concurrent.ConcurrentLinkedQueue<Throwable> seen = new java.util.concurrent.ConcurrentLinkedQueue<>();
        java.util.List<Thread> started = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            started.add(Thread.ofVirtual().start(() -> {
                try {
                    list.isEmpty();
                } catch (IllegalStateException e) {
                    seen.add(e);
                }
            }));
        }
        for (Thread thread : started) {
            thread.join();
        }
        assertThat(calls.get()).isEqualTo(1);
        assertThat(seen).hasSize(8);
        assertThat(new java.util.HashSet<>(seen)).hasSize(1);
    }

    // The lists the concurrent toString test reads: a cycle, a cycle behind two cells, and a list with no cycle.
    private static final java.util.List<java.util.function.Supplier<LazyList<Integer>>> SHOWN_WHILE_EVALUATED =
            java.util.List.of(
                    () -> LazyList.range(0, 50).cycle(),
                    () -> LazyList.range(0, 50).cycle().prepend(-2).prepend(-1),
                    () -> LazyList.range(0, 50));

    @Test
    @org.junit.jupiter.api.Timeout(120)
    void toStringShowsAnEvaluatedPrefixWhileOtherThreadsEvaluateTheList() throws Exception {
        int depth = 160;
        java.util.List<java.util.Set<String>> possible = new ArrayList<>();
        for (var list : SHOWN_WHILE_EVALUATED) {
            // every text one thread sees while it evaluates the list cell by cell
            java.util.Set<String> texts = new java.util.HashSet<>();
            for (int n = 0; n <= depth; n++) {
                LazyList<Integer> read = list.get();
                read.take(n).size();
                texts.add(read.toString());
            }
            possible.add(texts);
        }
        java.util.List<String> last = java.util.List.of(
                Vector.range(0, 50).mkString("LazyList(", ", ", ", <cycle>)"),
                Vector.of(-1, -2).appendAll(Vector.range(0, 50)).mkString("LazyList(", ", ", ", <cycle>)"),
                Vector.range(0, 50).mkString("LazyList(", ", ", ")"));
        for (int kind = 0; kind < last.size(); kind++) {
            assertThat(possible.get(kind)).contains(last.get(kind));
        }
        // Four platform threads, kept across the rounds, and no yield, so that a toString runs while another thread
        // evaluates the cell it reads. A task that does not end within its timeout fails the test instead of hanging
        // the build; its thread is a daemon, so it does not keep the JVM alive.
        java.util.concurrent.ExecutorService threads = java.util.concurrent.Executors.newFixedThreadPool(
                4, Thread.ofPlatform().daemon().factory());
        try {
            for (int round = 0; round < 5000; round++) {
                int kind = round % SHOWN_WHILE_EVALUATED.size();
                LazyList<Integer> list = SHOWN_WHILE_EVALUATED.get(kind).get();
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch read = new CountDownLatch(2);
                java.util.Set<String> shown = java.util.concurrent.ConcurrentHashMap.newKeySet();
                java.util.List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    tasks.add(threads.submit(() -> {
                        awaitQuietly(start);
                        list.take(depth).size();
                        read.countDown();
                    }));
                    tasks.add(threads.submit(() -> {
                        awaitQuietly(start);
                        // while the readers evaluate the list, and a bounded number of times
                        for (int call = 0; call < 10_000 && read.getCount() > 0; call++) {
                            shown.add(list.toString());
                        }
                    }));
                }
                start.countDown();
                for (java.util.concurrent.Future<?> task : tasks) {
                    int current = round;
                    assertThatCode(() -> task.get(10, java.util.concurrent.TimeUnit.SECONDS))
                            .as("round %d", current)
                            .doesNotThrowAnyException();
                }
                assertThat(possible.get(kind)).as("round %d", round).containsAll(shown);
                assertThat(list.toString()).isEqualTo(last.get(kind));
            }
        } finally {
            threads.shutdownNow();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -- stack depth of chains built without reading

    @Test
    void aChainOf100000DropsReads() {
        LazyList<Integer> dropped =
                LazyList.range(0, 100_000).foldLeft(LazyList.range(0, 100_010), (l, i) -> l.drop(1));
        assertThat(dropped.head()).isEqualTo(100_000);
        assertThat(dropped.get(1)).isEqualTo(100_001);
        assertThat(dropped.toVector()).isEqualTo(Vector.range(100_000, 100_010));
    }

    @Test
    void aChainOfDropsEvaluatesNothingAtTheCallAndOnlyWhatItSkipsOnRead() {
        AtomicInteger evaluated = new AtomicInteger();
        LazyList<Integer> dropped = counted(evaluated).drop(1).drop(2).drop(3);
        assertThat(evaluated.get()).isZero();
        assertThat(dropped.head()).isEqualTo(6);
        assertThat(evaluated.get()).isEqualTo(7);
    }

    @Test
    void aDropOfADropKeepsBothLists() {
        LazyList<Integer> first = LazyList.range(0, 10).drop(2);
        LazyList<Integer> second = first.drop(3);
        assertThat(second.toVector()).isEqualTo(Vector.of(5, 6, 7, 8, 9));
        assertThat(first.toVector()).isEqualTo(Vector.of(2, 3, 4, 5, 6, 7, 8, 9));
        // first is evaluated now: dropping from it starts from its cells
        assertThat(first.drop(1).toVector()).isEqualTo(Vector.of(3, 4, 5, 6, 7, 8, 9));
        assertThat(second.drop(4).toVector()).isEqualTo(Vector.of(9));
    }

    @Test
    void aDropOfADropPastIntegerMaxValueDropsEverything() {
        assertThat(LazyList.range(0, 5).drop(Integer.MAX_VALUE).drop(1).isEmpty())
                .isTrue();
        assertThat(LazyList.range(0, 5).drop(Integer.MAX_VALUE - 1).drop(1).isEmpty())
                .isTrue();
    }

    @Test
    void aDropOfNothingOrOfAnEmptyDropIsTheSameList() {
        LazyList<Integer> dropped = LazyList.range(0, 5).drop(1);
        assertThat(dropped.drop(0)).isSameAs(dropped);
        assertThat(dropped.drop(-1)).isSameAs(dropped);
        LazyList<Integer> emptied = LazyList.range(0, 5).drop(7);
        assertThat(emptied.isEmpty()).isTrue();
        assertThat(emptied.drop(1)).isSameAs(emptied);
    }

    @Test
    void chainsOfAThousandLazyOperationsRead() {
        int depth = 1_000;
        LazyList<Integer> source = LazyList.range(0, 10);
        LazyList<Integer> mapped = LazyList.range(0, depth).foldLeft(source, (l, i) -> l.map(x -> x + 1));
        LazyList<Integer> filtered = LazyList.range(0, depth).foldLeft(source, (l, i) -> l.filter(x -> true));
        LazyList<Integer> taken = LazyList.range(0, depth).foldLeft(source, (l, i) -> l.take(100));
        LazyList<Integer> deferred = LazyList.range(0, depth).foldLeft(source, (l, i) -> LazyList.defer(() -> l));
        assertThat(mapped.head()).isEqualTo(depth);
        assertThat(filtered.head()).isZero();
        assertThat(taken.head()).isZero();
        assertThat(deferred.head()).isZero();
        assertThat(mapped.get(1)).isEqualTo(depth + 1);
        assertThat(filtered.get(1)).isEqualTo(1);
        assertThat(taken.get(1)).isEqualTo(1);
        assertThat(deferred.get(1)).isEqualTo(1);
    }

    // -- arguments that are the asJava() view of a LazyList

    @Test
    void flattenOfAnAsJavaViewEvaluatesNothingAtTheCall() {
        AtomicInteger evaluated = new AtomicInteger();
        LazyList<Integer> flat =
                LazyList.flatten(counted(evaluated).map(LazyList::of).asJava());
        assertThat(evaluated.get()).isZero();
        assertThat(flat.toVector()).isEqualTo(Vector.range(0, SIZE));
    }

    @Test
    void concatOfAnAsJavaViewEvaluatesNothingAtTheCall() {
        AtomicInteger evaluated = new AtomicInteger();
        LazyList<Integer> concatenated =
                LazyList.concat(counted(evaluated).map(List::of).asJava());
        assertThat(evaluated.get()).isZero();
        assertThat(concatenated.toVector()).isEqualTo(Vector.range(0, SIZE));
    }

    @Test
    void removeAllOfAnAsJavaViewEvaluatesNothingAtTheCall() {
        AtomicInteger evaluated = new AtomicInteger();
        LazyList<Integer> removed =
                LazyList.range(0, 12).removeAll(counted(evaluated).asJava());
        assertThat(evaluated.get()).isZero();
        assertThat(removed.toVector()).isEqualTo(Vector.of(10, 11));
    }

    @Test
    void theAsJavaViewOfAnEvaluatedEmptyListIsKnownToBeEmpty() {
        assertThat(LazyList.flatten(LazyList.<LazyList<Integer>>empty().asJava()))
                .isSameAs(LazyList.empty());
        assertThat(LazyList.concat(LazyList.<List<Integer>>empty().asJava())).isSameAs(LazyList.empty());
        LazyList<Integer> list = LazyList.range(0, 3);
        assertThat(list.removeAll(LazyList.<Integer>empty().asJava())).isSameAs(list);
    }

    // -- of(T...) and takeRight

    @Test
    void ofCopiesTheArrayAtTheCall() {
        Integer[] elements = {1, 2, 3};
        LazyList<Integer> list = LazyList.of(elements);
        elements[0] = 9;
        elements[1] = null;
        assertThat(list.toVector()).isEqualTo(Vector.of(1, 2, 3));
        assertThat(elements).containsExactly(9, null, 3);
    }

    @Test
    void takeRightOfNothingReadsNothing() {
        AtomicInteger evaluated = new AtomicInteger();
        LazyList<Integer> infinite = LazyList.from(0).map(i -> {
            evaluated.incrementAndGet();
            return i;
        });
        assertThat(infinite.takeRight(0)).isSameAs(LazyList.empty());
        assertThat(infinite.takeRight(-1)).isSameAs(LazyList.empty());
        assertThat(infinite.takeRight(0).isEmpty()).isTrue();
        assertThat(evaluated.get()).isZero();
    }

    // -- messages

    @Test
    void reduceOnAnEmptyListNamesNoInternalClass() {
        LazyList<Integer> deferred = LazyList.defer(LazyList::empty);
        for (LazyList<Integer> empty : List.of(LazyList.<Integer>empty(), deferred)) {
            assertThatThrownBy(() -> empty.reduce(Integer::sum))
                    .isInstanceOf(java.util.NoSuchElementException.class)
                    .hasMessage("reduceLeft on empty Empty");
            assertThatThrownBy(() -> empty.reduceLeft(Integer::sum))
                    .isInstanceOf(java.util.NoSuchElementException.class)
                    .hasMessage("reduceLeft on empty Empty");
        }
    }

    @Test
    void updateOfANegativeIndexOnAnEmptyListSaysSo() {
        assertThatThrownBy(() -> LazyList.<Integer>empty().update(-1, 0))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("update(-1, e) on Nil");
        assertThatThrownBy(() -> LazyList.<Integer>empty().update(-1, x -> x))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("update(-1, e) on Nil");
        assertThatThrownBy(() -> LazyList.of(1).update(-1, 0))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("update(-1, e)");
    }
}
