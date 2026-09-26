package dev.zazr.collection;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// List.Builder: boundaries, an oracle fuzz, the List kept as the tail of the result, reuse and nulls.
public class ListBuilderTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 15, 16, 17, 31, 32, 33, 1023, 1024, 1025 };

    private static <T> java.util.List<T> javaList(Iterable<T> elements) {
        final java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    /// An iterable that is not a java.util.Collection and can be iterated once.
    private static <T> Iterable<T> oneShot(java.util.List<T> elements) {
        final boolean[] used = { false };
        return () -> {
            assertThat(used[0]).as("iterated twice").isFalse();
            used[0] = true;
            return elements.iterator();
        };
    }

    /// Whether `suffix` is, cell for cell, the end of `list`.
    private static <T> boolean endsWithCells(List<T> list, List<T> suffix) {
        final int skip = list.size() - suffix.size();
        List<T> rest = list;
        for (int i = 0; i < skip; i++) {
            rest = rest.tail();
        }
        return rest == suffix;
    }

    @Test
    public void shouldBuildTheListOfItsElementsAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Integer> input = IntStream.range(0, size).boxed().toList();
            final List.Builder<Integer> byElement = List.newBuilder();
            for (Integer element : input) {
                byElement.add(element);
            }
            assertThat(byElement.size()).isEqualTo(size);
            final List<Integer> built = byElement.result();
            assertThat(javaList(built)).isEqualTo(input);
            assertThat(built.size()).isEqualTo(size);
            assertThat(built).isEqualTo(List.ofAll(input));
            assertThat(javaList(List.<Integer> newBuilder().addAll(input).result())).isEqualTo(input);
            assertThat(javaList(List.<Integer> newBuilder().addAll(oneShot(input)).result())).isEqualTo(input);
            assertThat(javaList(List.<Integer> newBuilder().addAll(Vector.ofAll(input)).result())).isEqualTo(input);
        }
        assertThat(List.newBuilder().result()).isSameAs(List.empty());
        assertThat(List.newBuilder().addAll(java.util.List.of()).addAll(List.empty()).result()).isSameAs(List.empty());
    }

    @Test
    public void shouldMatchAnArrayListOnRandomSequencesOfCalls() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 300; round++) {
            final List.Builder<Integer> builder = List.newBuilder();
            final java.util.List<Integer> oracle = new ArrayList<>();
            final int calls = random.nextInt(60);
            for (int call = 0; call < calls; call++) {
                final int n = random.nextInt(40);
                final java.util.List<Integer> chunk = IntStream.range(0, n).map(i -> random.nextInt()).boxed().toList();
                switch (random.nextInt(5)) {
                    case 0 -> chunk.forEach(builder::add);
                    case 1 -> builder.addAll(chunk);
                    case 2 -> builder.addAll(oneShot(chunk));
                    case 3 -> builder.addAll(List.ofAll(chunk));
                    default -> builder.addAll(List.ofAll(chunk).asJava());
                }
                oracle.addAll(chunk);
                if (random.nextInt(4) == 0) {
                    assertThat(builder.size()).isEqualTo(oracle.size());
                }
            }
            assertThat(builder.size()).isEqualTo(oracle.size());
            final List<Integer> built = builder.result();
            assertThat(javaList(built)).isEqualTo(oracle);
            assertThat(built.size()).isEqualTo(oracle.size());
        }
    }

    @Test
    public void shouldReturnAListGivenToAnEmptyBuilderAsItIs() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            final List<Integer> source = List.ofAll(IntStream.range(0, size).boxed().toList());
            final List.Builder<Integer> builder = List.<Integer> newBuilder().addAll(source);
            assertThat(builder.size()).isEqualTo(size);
            assertThat(builder.size()).isEqualTo(size);
            assertThat(builder.result()).isSameAs(source);
            assertThat(List.<Integer> newBuilder().addAll(source.asJava()).result()).isSameAs(source);
            // the last List given becomes the tail, shared; the ones before it are copied
            final List<Integer> other = List.of(-1, -2);
            final List<Integer> built = List.<Integer> newBuilder().add(7).addAll(source).addAll(List.empty()).addAll(other).result();
            assertThat(javaList(built.take(size + 1))).isEqualTo(javaList(source.prepend(7)));
            assertThat(endsWithCells(built, other)).isTrue();
            assertThat(built.tail() == source).isFalse();
            final List<Integer> after = List.<Integer> newBuilder().add(7).add(8).addAll(source).result();
            assertThat(after.tail().tail()).isSameAs(source);
            assertThat(javaList(after)).isEqualTo(javaList(source.prepend(8).prepend(7)));
        }
    }

    @Test
    public void shouldNeverChangeAKeptList() {
        final List<Integer> source = List.of(1, 2, 3);
        final List.Builder<Integer> builder = List.<Integer> newBuilder().addAll(source);
        builder.add(4).addAll(source).add(5);
        final List<Integer> built = builder.result();
        assertThat(built).isEqualTo(List.of(1, 2, 3, 4, 1, 2, 3, 5));
        assertThat(source).isEqualTo(List.of(1, 2, 3));
        assertThat(source.size()).isEqualTo(3);
        // a List built by the builder is persistent: a new version leaves it as it was
        final List<Integer> prepended = built.prepend(0);
        final List<Integer> appended = built.append(6);
        assertThat(built).isEqualTo(List.of(1, 2, 3, 4, 1, 2, 3, 5));
        assertThat(prepended.tail()).isSameAs(built);
        assertThat(appended.size()).isEqualTo(9);
    }

    @Test
    public void shouldRefuseListBuilderUseAfterResult() {
        final List.Builder<Integer> builder = List.<Integer> newBuilder().add(1);
        final List<Integer> built = builder.result();
        assertThatThrownBy(() -> builder.add(2)).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this List.Builder");
        assertThatThrownBy(() -> builder.add(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(java.util.List.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(List.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(built).isEqualTo(List.of(1));
        final List.Builder<Integer> keeping = List.<Integer> newBuilder().addAll(List.of(1, 2));
        keeping.result();
        assertThatThrownBy(() -> keeping.add(3)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(keeping::size).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRejectNullsInTheListBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> List.newBuilder().add(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("List.Builder.add: element is null");
        assertThatThrownBy(() -> List.newBuilder().addAll(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("elements is null");
        final List.Builder<Integer> builder = List.newBuilder();
        assertThatThrownBy(() -> builder.addAll(java.util.Arrays.asList(3, 1, null, 2))).isInstanceOf(NullPointerException.class)
                .hasMessage("List.Builder.add: element is null");
        assertThat(builder.result()).isEqualTo(List.of(3, 1));
        // after a kept List: that List and the elements before the null
        final List.Builder<Integer> afterList = List.<Integer> newBuilder().addAll(List.of(1, 2));
        assertThatThrownBy(() -> afterList.addAll(java.util.Arrays.asList(3, null))).isInstanceOf(NullPointerException.class);
        assertThat(afterList.result()).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldCollectAListInParallel() {
        final java.util.List<Integer> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(random.nextInt());
        }
        assertThat(javaList(input.stream().collect(List.collector()))).isEqualTo(input);
        assertThat(javaList(input.parallelStream().collect(List.collector()))).isEqualTo(input);
        assertThat(java.util.stream.Stream.<Integer> empty().collect(List.collector())).isSameAs(List.empty());
    }

    @Test
    public void shouldKeepTheFactoryNullMessagesOnTheBuilderPaths() {
        final java.util.ArrayDeque<Integer> deque = new java.util.ArrayDeque<>(java.util.List.of(1, 2));
        final java.util.List<Integer> withNull = java.util.Arrays.asList(1, null);
        assertThatThrownBy(() -> List.ofAll(new java.util.LinkedHashSet<>(withNull))).isInstanceOf(NullPointerException.class)
                .hasMessage("List: element is null");
        assertThatThrownBy(() -> List.ofAll(withNull.stream())).isInstanceOf(NullPointerException.class)
                .hasMessage("List: element is null");
        assertThat(List.ofAll(deque)).isEqualTo(List.of(1, 2));
    }

    @Test
    public void shouldCopyAReversedViewInsteadOfKeepingIt() {
        final List<Integer> source = List.of(1, 2, 3);
        assertThat(List.<Integer> newBuilder().addAll(source.asJava().reversed()).result())
                .isEqualTo(List.of(3, 2, 1));
    }
}
