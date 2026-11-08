package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// The messages of the `NullPointerException` that `unzip`, `unzip3`, `zipWithIndex` with a mapper, `zipWith`,
/// `zipAll` and `intersperse` throw on a null, on `Vector`, `NonEmptyVector`, `List`, `LazyList` and `Queue`: each
/// names the method called and what was null, with the index of the element for `unzip`, `unzip3` and
/// `zipWithIndex`. The strict types throw during the call; `LazyList` throws when the null is read.
public class SeqNullMessagesTest {

    /// 33 elements: the last one is past the first leaf of a `Vector`.
    private static final int SIZE = 33;

    /// The first, a middle and the last position.
    private static final int[] POSITIONS = {0, 16, SIZE - 1};

    private static final String[] ORDINALS = {"first", "second", "third"};

    /// The operations under test on one sequence of `0, 1, ..., SIZE - 1`, each result read in full.
    private interface Seq {

        /// The name of the type in the messages.
        String type();

        Object unzip(Function<Integer, Tuple2<? extends Integer, ? extends Integer>> unzipper);

        Object unzip3(Function<Integer, Tuple3<? extends Integer, ? extends Integer, ? extends Integer>> unzipper);

        Object zipWithIndex(BiFunction<Integer, Integer, Integer> mapper);

        Object zipWith(Iterable<Integer> that, BiFunction<Integer, Integer, Integer> mapper);

        Object zipAll(Iterable<Integer> that, Integer thisElem, Integer thatElem);

        Object intersperse(Integer separator);
    }

    private static Seq vector(Vector<Integer> v) {
        return new Seq() {
            @Override
            public String type() {
                return "Vector";
            }

            @Override
            public Object unzip(Function<Integer, Tuple2<? extends Integer, ? extends Integer>> unzipper) {
                return v.unzip(unzipper);
            }

            @Override
            public Object unzip3(
                    Function<Integer, Tuple3<? extends Integer, ? extends Integer, ? extends Integer>> unzipper) {
                return v.unzip3(unzipper);
            }

            @Override
            public Object zipWithIndex(BiFunction<Integer, Integer, Integer> mapper) {
                return v.zipWithIndex(mapper);
            }

            @Override
            public Object zipWith(Iterable<Integer> that, BiFunction<Integer, Integer, Integer> mapper) {
                return v.zipWith(that, mapper);
            }

            @Override
            public Object zipAll(Iterable<Integer> that, Integer thisElem, Integer thatElem) {
                return v.zipAll(that, thisElem, thatElem);
            }

            @Override
            public Object intersperse(Integer separator) {
                return v.intersperse(separator);
            }
        };
    }

    private static Seq nonEmptyVector(NonEmptyVector<Integer> v) {
        return new Seq() {
            @Override
            public String type() {
                return "NonEmptyVector";
            }

            @Override
            public Object unzip(Function<Integer, Tuple2<? extends Integer, ? extends Integer>> unzipper) {
                return v.unzip(unzipper);
            }

            @Override
            public Object unzip3(
                    Function<Integer, Tuple3<? extends Integer, ? extends Integer, ? extends Integer>> unzipper) {
                return v.unzip3(unzipper);
            }

            @Override
            public Object zipWithIndex(BiFunction<Integer, Integer, Integer> mapper) {
                return v.zipWithIndex(mapper);
            }

            @Override
            public Object zipWith(Iterable<Integer> that, BiFunction<Integer, Integer, Integer> mapper) {
                return v.zipWith(that, mapper);
            }

            @Override
            public Object zipAll(Iterable<Integer> that, Integer thisElem, Integer thatElem) {
                return v.zipAll(that, thisElem, thatElem);
            }

            @Override
            public Object intersperse(Integer separator) {
                return v.intersperse(separator);
            }
        };
    }

    private static Seq list(List<Integer> l) {
        return new Seq() {
            @Override
            public String type() {
                return "List";
            }

            @Override
            public Object unzip(Function<Integer, Tuple2<? extends Integer, ? extends Integer>> unzipper) {
                return l.unzip(unzipper);
            }

            @Override
            public Object unzip3(
                    Function<Integer, Tuple3<? extends Integer, ? extends Integer, ? extends Integer>> unzipper) {
                return l.unzip3(unzipper);
            }

            @Override
            public Object zipWithIndex(BiFunction<Integer, Integer, Integer> mapper) {
                return l.zipWithIndex(mapper);
            }

            @Override
            public Object zipWith(Iterable<Integer> that, BiFunction<Integer, Integer, Integer> mapper) {
                return l.zipWith(that, mapper);
            }

            @Override
            public Object zipAll(Iterable<Integer> that, Integer thisElem, Integer thatElem) {
                return l.zipAll(that, thisElem, thatElem);
            }

            @Override
            public Object intersperse(Integer separator) {
                return l.intersperse(separator);
            }
        };
    }

    /// Reads every side of every result in full, so that the lazy null is reached.
    private static Seq lazyList(LazyList<Integer> l) {
        return new Seq() {
            @Override
            public String type() {
                return "LazyList";
            }

            @Override
            public Object unzip(Function<Integer, Tuple2<? extends Integer, ? extends Integer>> unzipper) {
                Tuple2<LazyList<Integer>, LazyList<Integer>> sides = l.unzip(unzipper);
                return Tuple.of(sides._1().toVector(), sides._2().toVector());
            }

            @Override
            public Object unzip3(
                    Function<Integer, Tuple3<? extends Integer, ? extends Integer, ? extends Integer>> unzipper) {
                Tuple3<LazyList<Integer>, LazyList<Integer>, LazyList<Integer>> sides = l.unzip3(unzipper);
                return Tuple.of(
                        sides._1().toVector(), sides._2().toVector(), sides._3().toVector());
            }

            @Override
            public Object zipWithIndex(BiFunction<Integer, Integer, Integer> mapper) {
                return l.zipWithIndex(mapper).toVector();
            }

            @Override
            public Object zipWith(Iterable<Integer> that, BiFunction<Integer, Integer, Integer> mapper) {
                return l.zipWith(that, mapper).toVector();
            }

            @Override
            public Object zipAll(Iterable<Integer> that, Integer thisElem, Integer thatElem) {
                return l.zipAll(that, thisElem, thatElem).toVector();
            }

            @Override
            public Object intersperse(Integer separator) {
                return l.intersperse(separator).toVector();
            }
        };
    }

    private static Seq queue(Queue<Integer> q) {
        return new Seq() {
            @Override
            public String type() {
                return "Queue";
            }

            @Override
            public Object unzip(Function<Integer, Tuple2<? extends Integer, ? extends Integer>> unzipper) {
                return q.unzip(unzipper);
            }

            @Override
            public Object unzip3(
                    Function<Integer, Tuple3<? extends Integer, ? extends Integer, ? extends Integer>> unzipper) {
                return q.unzip3(unzipper);
            }

            @Override
            public Object zipWithIndex(BiFunction<Integer, Integer, Integer> mapper) {
                return q.zipWithIndex(mapper);
            }

            @Override
            public Object zipWith(Iterable<Integer> that, BiFunction<Integer, Integer, Integer> mapper) {
                return q.zipWith(that, mapper);
            }

            @Override
            public Object zipAll(Iterable<Integer> that, Integer thisElem, Integer thatElem) {
                return q.zipAll(that, thisElem, thatElem);
            }

            @Override
            public Object intersperse(Integer separator) {
                return q.intersperse(separator);
            }
        };
    }

    /// A Queue of `0, 1, ..., size - 1` built by `append`, so that all but its first element are in its rear list.
    private static Queue<Integer> appended(int size) {
        return Vector.range(0, size).foldLeft(Queue.<Integer>empty(), Queue::append);
    }

    /// Each type over `0, 1, ..., size - 1`, the Queue both with all its elements in front and built by `append`.
    private static java.util.List<Seq> seqs(int size) {
        java.util.List<Seq> seqs = new java.util.ArrayList<>();
        seqs.add(vector(Vector.range(0, size)));
        if (size > 0) {
            seqs.add(nonEmptyVector(
                    NonEmptyVector.fromVector(Vector.range(0, size)).get()));
        }
        seqs.add(list(List.range(0, size)));
        seqs.add(lazyList(LazyList.range(0, size)));
        seqs.add(queue(Queue.ofAll(List.range(0, size))));
        seqs.add(queue(appended(size)));
        return seqs;
    }

    private static Integer nullAt(int component, int at, int which, int i) {
        return component == which && i == at ? null : i;
    }

    @Test
    public void unzipNamesTheComponentAndTheElement() {
        for (Seq seq : seqs(SIZE)) {
            for (int at : POSITIONS) {
                for (int component = 0; component < 2; component++) {
                    int c = component;
                    assertThatNullPointerException()
                            .as(seq.type() + ", component " + c + ", at " + at)
                            .isThrownBy(() -> seq.unzip(i -> Tuple.of(nullAt(c, at, 0, i), nullAt(c, at, 1, i))))
                            .withMessage(seq.type() + ".unzip: the " + ORDINALS[c] + " component of element " + at
                                    + " is null");
                }
            }
        }
    }

    @Test
    public void unzip3NamesTheComponentAndTheElement() {
        for (Seq seq : seqs(SIZE)) {
            for (int at : POSITIONS) {
                for (int component = 0; component < 3; component++) {
                    int c = component;
                    assertThatNullPointerException()
                            .as(seq.type() + ", component " + c + ", at " + at)
                            .isThrownBy(() -> seq.unzip3(
                                    i -> Tuple.of(nullAt(c, at, 0, i), nullAt(c, at, 1, i), nullAt(c, at, 2, i))))
                            .withMessage(seq.type() + ".unzip3: the " + ORDINALS[c] + " component of element " + at
                                    + " is null");
                }
            }
        }
    }

    @Test
    public void unzipKeepsTheMessageOfANullTuple() {
        for (Seq seq : seqs(SIZE)) {
            assertThatNullPointerException()
                    .as(seq.type())
                    .isThrownBy(() -> seq.unzip(i -> i == 16 ? null : Tuple.of(i, i)))
                    .withMessage(seq.type() + ".unzip: unzipper returned null");
            assertThatNullPointerException()
                    .as(seq.type())
                    .isThrownBy(() -> seq.unzip3(i -> i == 16 ? null : Tuple.of(i, i, i)))
                    .withMessage(seq.type() + ".unzip3: unzipper returned null");
        }
    }

    @Test
    public void zipWithIndexNamesTheElement() {
        for (Seq seq : seqs(SIZE)) {
            for (int at : POSITIONS) {
                assertThatNullPointerException()
                        .as(seq.type() + ", at " + at)
                        .isThrownBy(() -> seq.zipWithIndex((e, i) -> i == at ? null : e))
                        .withMessage(seq.type() + ".zipWithIndex: mapper returned null for element " + at);
            }
        }
    }

    @Test
    public void zipWithNamesTheMethod() {
        for (Seq seq : seqs(SIZE)) {
            for (int at : POSITIONS) {
                assertThatNullPointerException()
                        .as(seq.type() + ", at " + at)
                        .isThrownBy(() -> seq.zipWith(List.range(0, SIZE), (a, b) -> a == at ? null : b))
                        .withMessage(seq.type() + ".zipWith: mapper returned null");
            }
        }
    }

    @Test
    public void nonEmptyVectorZipWithANonEmptyVectorNamesTheMethod() {
        NonEmptyVector<Integer> nev =
                NonEmptyVector.fromVector(Vector.range(0, SIZE)).get();
        for (int at : POSITIONS) {
            assertThatNullPointerException()
                    .as("at " + at)
                    .isThrownBy(() -> nev.zipWith(nev, (a, b) -> a == at ? null : b))
                    .withMessage("NonEmptyVector.zipWith: mapper returned null");
        }
    }

    @Test
    public void zipAllNamesTheNullFillerWhateverTheSizes() {
        for (int size : new int[] {0, 1, SIZE}) {
            for (Seq seq : seqs(size)) {
                for (List<Integer> that : java.util.List.of(List.<Integer>empty(), List.of(7), List.range(0, 40))) {
                    String where = seq.type() + ", size " + size + ", that of " + that.size();
                    assertThatNullPointerException()
                            .as(where)
                            .isThrownBy(() -> seq.zipAll(that, null, 0))
                            .withMessage(seq.type() + ".zipAll: thisElem is null");
                    assertThatNullPointerException()
                            .as(where)
                            .isThrownBy(() -> seq.zipAll(that, 0, null))
                            .withMessage(seq.type() + ".zipAll: thatElem is null");
                    assertThatNullPointerException()
                            .as(where)
                            .isThrownBy(() -> seq.zipAll(that, null, null))
                            .withMessage(seq.type() + ".zipAll: thisElem is null");
                }
            }
        }
    }

    @Test
    public void intersperseNamesTheMethod() {
        for (int size : new int[] {2, SIZE}) {
            for (Seq seq : seqs(size)) {
                assertThatNullPointerException()
                        .as(seq.type() + ", size " + size)
                        .isThrownBy(() -> seq.intersperse(null))
                        .withMessage(seq.type() + ".intersperse: element is null");
            }
        }
    }

    @Test
    public void intersperseOfFewerThanTwoElementsThrowsOnlyOnNonEmptyVectorAndLazyList() {
        for (int size : new int[] {0, 1}) {
            for (Seq seq : seqs(size)) {
                String type = seq.type();
                if (type.equals("NonEmptyVector") || type.equals("LazyList")) {
                    assertThatNullPointerException()
                            .as(type + ", size " + size)
                            .isThrownBy(() -> seq.intersperse(null))
                            .withMessage(type + ".intersperse: element is null");
                } else {
                    assertThatCode(() -> seq.intersperse(null))
                            .as(type + ", size " + size)
                            .doesNotThrowAnyException();
                }
            }
        }
    }

    @Test
    public void lazyListUnzipThrowsWhenTheNullComponentIsRead() {
        for (int at : POSITIONS) {
            for (int component = 0; component < 3; component++) {
                int c = component;
                AtomicInteger calls = new AtomicInteger();
                Tuple3<LazyList<Integer>, LazyList<Integer>, LazyList<Integer>> sides = LazyList.range(0, SIZE)
                        .unzip3(i -> {
                            calls.incrementAndGet();
                            return Tuple.of(nullAt(c, at, 0, i), nullAt(c, at, 1, i), nullAt(c, at, 2, i));
                        });
                assertThat(calls).as("component " + c + ", at " + at).hasValue(0);
                LazyList<Integer> nullSide = c == 0 ? sides._1() : c == 1 ? sides._2() : sides._3();
                if (at > 0) {
                    assertThat(nullSide.get(at - 1)).isEqualTo(at - 1);
                }
                assertThatNullPointerException()
                        .as("component " + c + ", at " + at)
                        .isThrownBy(() -> nullSide.get(at))
                        .withMessage(
                                "LazyList.unzip3: the " + ORDINALS[c] + " component of element " + at + " is null");
                for (int other = 0; other < 3; other++) {
                    if (other != c) {
                        LazyList<Integer> otherSide = other == 0 ? sides._1() : other == 1 ? sides._2() : sides._3();
                        assertThat(otherSide.toVector()).isEqualTo(Vector.range(0, SIZE));
                    }
                }
                assertThat(calls).as("component " + c + ", at " + at).hasValue(SIZE);
            }
            Tuple2<LazyList<Integer>, LazyList<Integer>> pair =
                    LazyList.range(0, SIZE).unzip(i -> Tuple.of(i, i == at ? null : i));
            assertThat(pair._1().toVector()).isEqualTo(Vector.range(0, SIZE));
            assertThatNullPointerException()
                    .isThrownBy(() -> pair._2().get(at))
                    .withMessage("LazyList.unzip: the second component of element " + at + " is null");
        }
    }

    @Test
    public void lazyListZipWithIndexAndZipWithThrowWhenTheNullIsRead() {
        for (int at : POSITIONS) {
            LazyList<Integer> indexed = LazyList.range(0, SIZE).zipWithIndex((e, i) -> i == at ? null : e);
            LazyList<Integer> zipped =
                    LazyList.range(0, SIZE).zipWith(List.range(0, SIZE), (a, b) -> a == at ? null : b);
            if (at > 0) {
                assertThat(indexed.get(at - 1)).isEqualTo(at - 1);
                assertThat(zipped.get(at - 1)).isEqualTo(at - 1);
            }
            assertThatNullPointerException()
                    .isThrownBy(() -> indexed.get(at))
                    .withMessage("LazyList.zipWithIndex: mapper returned null for element " + at);
            assertThatNullPointerException()
                    .isThrownBy(() -> zipped.get(at))
                    .withMessage("LazyList.zipWith: mapper returned null");
        }
    }

    @Test
    public void lazyListIntersperseAndZipAllCheckTheirArgumentsDuringTheCall() {
        LazyList<Integer> unread = LazyList.defer(() -> {
            throw new AssertionError("read");
        });
        assertThatNullPointerException()
                .isThrownBy(() -> unread.intersperse(null))
                .withMessage("LazyList.intersperse: element is null");
        assertThatNullPointerException()
                .isThrownBy(() -> unread.zipAll(List.of(1), null, 0))
                .withMessage("LazyList.zipAll: thisElem is null");
        assertThatNullPointerException()
                .isThrownBy(() -> unread.zipAll(List.of(1), 0, null))
                .withMessage("LazyList.zipAll: thatElem is null");
    }
}
