package com.guizmaii.zazr.control;

import com.guizmaii.zazr.CheckedConsumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsingTest {

    // -- the failure matrix

    /// What a step of a scenario does: the block, an acquisition or a release.
    enum Kind {
        OK(-1),
        NULL(-1),
        ORDINARY(0),
        INTERRUPTED(1),
        LINKAGE(2),
        VM(3),
        /// A release that rethrows the last throwable thrown before it, by the block or an earlier release; nothing
        /// when nothing was thrown yet.
        RETHROW_LAST(-1);

        /// The severity rule of `Using`, written out independently of its implementation.
        final int severity;

        Kind(int severity) {
            this.severity = severity;
        }

        Throwable create(String label) {
            return switch (this) {
                case ORDINARY -> new IOException(label);
                case INTERRUPTED -> new InterruptedException(label);
                case LINKAGE -> new LinkageError(label);
                case VM -> new OutOfMemoryError(label);
                default -> throw new IllegalStateException("no throwable for " + this);
            };
        }
    }

    static final Kind[] BODY = {Kind.OK, Kind.NULL, Kind.ORDINARY, Kind.INTERRUPTED, Kind.LINKAGE, Kind.VM};
    static final Kind[] ACQUISITION_FAILURE = {Kind.ORDINARY, Kind.INTERRUPTED, Kind.LINKAGE, Kind.VM};
    static final Kind[] RELEASE = {Kind.OK, Kind.ORDINARY, Kind.INTERRUPTED, Kind.LINKAGE, Kind.VM, Kind.RETHROW_LAST};

    /// `resources` resources are acquired in order, resource `i` by `acquire(AutoCloseable)` when `i` is even and by
    /// `acquire(value, release)` when it is odd; when `failAt` is not -1, creating resource `failAt` throws
    /// `failure` instead and the block stops there. Otherwise the block ends with `body`. Resource `i` is released
    /// with `releases[i]`.
    record Scenario(int resources, int failAt, Kind failure, Kind body, Kind[] releases) {

        int acquired() {
            return failAt == -1 ? resources : failAt;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("resources=" + resources);
            if (failAt == -1) {
                sb.append(", block ").append(body);
            } else {
                sb.append(", acquisition ").append(failAt).append(' ').append(failure);
            }
            sb.append(", releases in acquisition order ").append(java.util.Arrays.toString(releases));
            return sb.toString();
        }
    }

    /// The record of one run: the throwables in the order `Using` sees them (the same one twice when a release
    /// rethrows), their kinds, and the resources released, in order.
    static final class Run {
        final List<Throwable> events = new ArrayList<>();
        final Map<Throwable, Kind> kinds = new IdentityHashMap<>();
        final List<Integer> released = new ArrayList<>();

        Throwable raise(Kind kind, String label) {
            final Throwable t = kind.create(label);
            kinds.put(t, kind);
            events.add(t);
            return t;
        }

        void release(int index, Kind kind) throws Exception {
            released.add(index);
            switch (kind) {
                case OK -> {
                }
                case RETHROW_LAST -> {
                    if (!events.isEmpty()) {
                        final Throwable last = events.getLast();
                        events.add(last);
                        throw UsingTest.<Exception>sneaky(last);
                    }
                }
                default -> throw UsingTest.<Exception>sneaky(raise(kind, "release " + index));
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <X extends Throwable> X sneaky(Throwable t) throws X {
        throw (X) t;
    }

    /// A resource acquired by `acquire(AutoCloseable)` or `Using.of`.
    record Resource(Run run, int index, Kind release) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            run.release(index, release);
        }
    }

    static List<Scenario> scenarios(int maxResources, boolean withAcquisitionFailures) {
        final List<Scenario> all = new ArrayList<>();
        for (int n = 0; n <= maxResources; n++) {
            for (Kind[] releases : releaseCombinations(n)) {
                for (Kind body : BODY) {
                    all.add(new Scenario(n, -1, Kind.OK, body, releases));
                }
                if (withAcquisitionFailures) {
                    for (int failAt = 0; failAt < n; failAt++) {
                        if (onlyOkFrom(releases, failAt)) {
                            for (Kind failure : ACQUISITION_FAILURE) {
                                all.add(new Scenario(n, failAt, failure, Kind.OK, releases));
                            }
                        }
                    }
                }
            }
        }
        return all;
    }

    /// The resources from `failAt` on are never acquired, so their release kind plays no part: keep one combination.
    static boolean onlyOkFrom(Kind[] releases, int from) {
        for (int i = from; i < releases.length; i++) {
            if (releases[i] != Kind.OK) {
                return false;
            }
        }
        return true;
    }

    static List<Kind[]> releaseCombinations(int n) {
        List<Kind[]> combinations = new ArrayList<>();
        combinations.add(new Kind[0]);
        for (int i = 0; i < n; i++) {
            final List<Kind[]> next = new ArrayList<>();
            for (Kind[] prefix : combinations) {
                for (Kind kind : RELEASE) {
                    final Kind[] longer = java.util.Arrays.copyOf(prefix, prefix.length + 1);
                    longer[prefix.length] = kind;
                    next.add(longer);
                }
            }
            combinations = next;
        }
        return combinations;
    }

    static @Nullable String block(Run run, Scenario s) throws Exception {
        return switch (s.body()) {
            case OK -> "ok";
            case NULL -> null;
            default -> throw UsingTest.<Exception>sneaky(run.raise(s.body(), "block"));
        };
    }

    static Object runManager(Run run, Scenario s) {
        try {
            return Using.manager(use -> {
                for (int i = 0; i < s.resources(); i++) {
                    if (i == s.failAt()) {
                        throw UsingTest.<Exception>sneaky(run.raise(s.failure(), "acquisition " + i));
                    }
                    final int index = i;
                    final Kind release = s.releases()[i];
                    if (i % 2 == 0) {
                        final Resource resource = new Resource(run, index, release);
                        assertThat(use.acquire(resource)).isSameAs(resource);
                    } else {
                        final Integer value = 1000 + index;
                        assertThat(use.acquire(value, v -> {
                            assertThat(v).isSameAs(value);
                            run.release(index, release);
                        })).isSameAs(value);
                    }
                }
                return block(run, s);
            });
        } catch (Throwable t) {
            return t;
        }
    }

    static Object runOf(Run run, Scenario s) {
        try {
            return Using.of(() -> {
                if (s.failAt() == 0) {
                    throw UsingTest.<Exception>sneaky(run.raise(s.failure(), "acquisition 0"));
                }
                return new Resource(run, 0, s.releases()[0]);
            }, r -> {
                assertThat(r.index()).isZero();
                return block(run, s);
            });
        } catch (Throwable t) {
            return t;
        }
    }

    /// Checks the outcome of a run against the rules: every acquired resource released once, in reverse order; the
    /// most severe throwable surfaces, the first one on equal severity; each other throwable suppressed, once per time
    /// it was thrown, in the throwable that was surfacing when it was thrown; nothing suppressed in itself.
    static void check(Run run, Scenario s, Object outcome) {
        final List<Integer> expectedReleases = new ArrayList<>();
        for (int i = s.acquired() - 1; i >= 0; i--) {
            expectedReleases.add(i);
        }
        assertThat(run.released).as("release order").isEqualTo(expectedReleases);

        final Map<Throwable, List<Throwable>> expectedSuppressed = new IdentityHashMap<>();
        Throwable primary = null;
        for (Throwable t : run.events) {
            expectedSuppressed.putIfAbsent(t, new ArrayList<>());
            if (primary == null) {
                primary = t;
            } else if (t != primary) {
                if (run.kinds.get(t).severity > run.kinds.get(primary).severity) {
                    expectedSuppressed.get(t).add(primary);
                    primary = t;
                } else {
                    expectedSuppressed.get(primary).add(t);
                }
            }
        }
        for (Map.Entry<Throwable, List<Throwable>> e : expectedSuppressed.entrySet()) {
            assertThat(e.getKey().getSuppressed()).as("suppressed in %s", e.getKey())
                    .containsExactlyElementsOf(e.getValue());
        }

        if (primary == null) {
            if (s.body() == Kind.NULL) {
                assertThat(outcome).isInstanceOfSatisfying(Try.Failure.class,
                        f -> assertThat(f.cause()).isInstanceOf(NullPointerException.class));
            } else {
                assertThat(outcome).isEqualTo(Try.success("ok"));
            }
        } else if (run.kinds.get(primary) == Kind.ORDINARY) {
            final Throwable surfaced = primary;
            assertThat(outcome).isInstanceOfSatisfying(Try.Failure.class, f -> assertThat(f.cause()).isSameAs(surfaced));
        } else {
            // fatal throwables are rethrown, as by Try.of
            assertThat(outcome).isSameAs(primary);
        }
    }

    @TestFactory
    Stream<DynamicTest> managerFailureMatrix() {
        return scenarios(3, true).stream().map(s -> DynamicTest.dynamicTest(s.toString(), () -> {
            final Run run = new Run();
            check(run, s, runManager(run, s));
        }));
    }

    @TestFactory
    Stream<DynamicTest> ofFailureMatrix() {
        return scenarios(1, true).stream().filter(s -> s.resources() == 1)
                .map(s -> DynamicTest.dynamicTest(s.toString(), () -> {
                    final Run run = new Run();
                    check(run, s, runOf(run, s));
                }));
    }

    @Test
    void theMatrixCoversEveryCombination() {
        // every block outcome for every combination of releases, and every acquisition failure for every
        // combination of releases of the resources acquired before it
        int expected = 0;
        for (int n = 0; n <= 3; n++) {
            expected += (int) Math.pow(RELEASE.length, n) * BODY.length;
            for (int failAt = 0; failAt < n; failAt++) {
                expected += (int) Math.pow(RELEASE.length, failAt) * ACQUISITION_FAILURE.length;
            }
        }
        assertThat(scenarios(3, true)).hasSize(expected);
    }

    // -- the failure rules, spelled out

    @Nested
    class FailureRules {

        @Test
        void surfacesTheMostSevereAndSuppressesTheOthersWhereTheyWereThrown() {
            final IOException a = new IOException("block");
            final IOException b = new IOException("release 3");
            final OutOfMemoryError c = new OutOfMemoryError("release 2");
            final IOException d = new IOException("release 1");
            final List<String> released = new ArrayList<>();
            assertThatThrownBy(() -> Using.manager(use -> {
                use.acquire("1", r -> {
                    released.add(r);
                    throw d;
                });
                use.acquire("2", r -> {
                    released.add(r);
                    throw c;
                });
                use.acquire("3", r -> {
                    released.add(r);
                    throw b;
                });
                throw a;
            })).isSameAs(c);
            assertThat(released).containsExactly("3", "2", "1");
            assertThat(a.getSuppressed()).containsExactly(b);
            assertThat(c.getSuppressed()).containsExactly(a, d);
            assertThat(b.getSuppressed()).isEmpty();
            assertThat(d.getSuppressed()).isEmpty();
        }

        @Test
        void keepsTheFirstOnEqualSeverity() {
            final IOException first = new IOException("block");
            final IllegalStateException second = new IllegalStateException("close");
            final Try<String> result = Using.of(() -> () -> {
                throw second;
            }, r -> {
                throw first;
            });
            assertThat(result.getCause()).isSameAs(first);
            assertThat(first.getSuppressed()).containsExactly(second);
        }

        @Test
        void surfacesAnErrorFromCloseOverTheExceptionOfTheBlock() {
            // try-with-resources would throw the IOException with the OutOfMemoryError suppressed
            final IOException body = new IOException("block");
            final OutOfMemoryError oom = new OutOfMemoryError("close");
            assertThatThrownBy(() -> Using.of(() -> () -> {
                throw oom;
            }, r -> {
                throw body;
            })).isSameAs(oom);
            assertThat(oom.getSuppressed()).containsExactly(body);
        }

        @Test
        void ranksVirtualMachineErrorOverLinkageErrorOverInterruptedException() {
            final InterruptedException interrupted = new InterruptedException("block");
            final LinkageError linkage = new LinkageError("release 2");
            final StackOverflowError overflow = new StackOverflowError("release 1");
            assertThatThrownBy(() -> Using.manager(use -> {
                use.acquire(1, r -> {
                    throw overflow;
                });
                use.acquire(2, r -> {
                    throw linkage;
                });
                throw interrupted;
            })).isSameAs(overflow);
            assertThat(linkage.getSuppressed()).containsExactly(interrupted);
            assertThat(overflow.getSuppressed()).containsExactly(linkage);
        }

        @Test
        void ranksInterruptedExceptionOverAnOrdinaryException() {
            final IOException body = new IOException("block");
            final InterruptedException interrupted = new InterruptedException("close");
            assertThatThrownBy(() -> Using.of(() -> () -> {
                throw interrupted;
            }, r -> {
                throw body;
            })).isSameAs(interrupted);
            assertThat(interrupted.getSuppressed()).containsExactly(body);
        }

        @Test
        void neverSuppressesAThrowableInItselfWhenCloseRethrowsTheBlocksThrowable() {
            final IOException body = new IOException("block");
            final Try<String> result = Using.of(() -> () -> {
                throw body;
            }, r -> {
                throw body;
            });
            assertThat(result.getCause()).isSameAs(body);
            assertThat(body.getSuppressed()).isEmpty();
        }

        @Test
        void neverSuppressesAThrowableInItselfAndStillReleasesTheOthers() {
            final IOException body = new IOException("block");
            final List<Integer> released = new ArrayList<>();
            final Try<String> result = Using.manager(use -> {
                use.acquire(1, r -> released.add(r));
                use.acquire(2, r -> {
                    released.add(r);
                    throw body;
                });
                use.acquire(3, r -> {
                    released.add(r);
                    throw body;
                });
                throw body;
            });
            assertThat(result.getCause()).isSameAs(body);
            assertThat(body.getSuppressed()).isEmpty();
            assertThat(released).containsExactly(3, 2, 1);
        }

        @Test
        void neverSuppressesAThrowableInItselfWhenAReleaseRethrowsTheOneThatSurfaced() {
            final IOException first = new IOException("release 2");
            final Try<String> result = Using.manager(use -> {
                use.acquire(1, r -> {
                    throw first;
                });
                use.acquire(2, r -> {
                    throw first;
                });
                return "ok";
            });
            assertThat(result.getCause()).isSameAs(first);
            assertThat(first.getSuppressed()).isEmpty();
        }

        @Test
        void failsWithTheCloseFailureWhenOnlyCloseThrows() {
            final IllegalStateException closeFailure = new IllegalStateException("close");
            assertThat(Using.of(() -> () -> {
                throw closeFailure;
            }, r -> "ok").getCause()).isSameAs(closeFailure);
        }

        @Test
        void rethrowsAFatalBlockThrowableAfterReleasing() {
            final InterruptedException interrupted = new InterruptedException("block");
            final AtomicInteger closed = new AtomicInteger();
            assertThatThrownBy(() -> Using.of(() -> closed::incrementAndGet, r -> {
                throw interrupted;
            })).isSameAs(interrupted);
            assertThat(closed).hasValue(1);
        }

        @Test
        void discardsTheSuppressedThrowableWhenTheSurfacingOneDoesNotSupportSuppression() {
            final Exception noSuppression = new Exception("block", null, false, false) {};
            final IOException closeFailure = new IOException("close");
            final Try<String> result = Using.of(() -> () -> {
                throw closeFailure;
            }, r -> {
                throw noSuppression;
            });
            assertThat(result.getCause()).isSameAs(noSuppression);
            assertThat(noSuppression.getSuppressed()).isEmpty();
        }
    }

    // -- Using.of

    @Nested
    class Of {

        @Test
        void closesTheResourceOnceAfterASuccess() {
            final AtomicInteger closed = new AtomicInteger();
            final Try<String> result = Using.of(() -> closed::incrementAndGet, r -> "done");
            assertThat(result).isEqualTo(Try.success("done"));
            assertThat(closed).hasValue(1);
        }

        @Test
        void closesTheResourceOnceAfterAFailure() {
            final IOException cause = new IOException("boom");
            final AtomicInteger closed = new AtomicInteger();
            final Try<String> result = Using.of(() -> closed::incrementAndGet, r -> {
                throw cause;
            });
            assertThat(result.getCause()).isSameAs(cause);
            assertThat(closed).hasValue(1);
        }

        @Test
        void passesTheAcquiredResource() {
            final java.io.StringReader reader = new java.io.StringReader("x");
            assertThat(Using.of(() -> reader, r -> r == reader)).isEqualTo(Try.success(true));
        }

        @Test
        void failsWithoutClosingWhenTheAcquisitionThrows() {
            final IOException cause = new IOException("no resource");
            final Try<String> result = Using.<AutoCloseable, String>of(() -> {
                throw cause;
            }, r -> "unreachable");
            assertThat(result.getCause()).isSameAs(cause);
        }

        @Test
        void failsWithANullPointerExceptionOnANullResource() {
            final AtomicInteger calls = new AtomicInteger();
            final Try<String> result = Using.<AutoCloseable, String>of(() -> null, r -> {
                calls.incrementAndGet();
                return "unreachable";
            });
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(calls).hasValue(0);
        }

        @Test
        void failsWithANullPointerExceptionOnANullResultAndCloses() {
            final AtomicInteger closed = new AtomicInteger();
            final Try<String> result = Using.of(() -> closed::incrementAndGet, r -> null);
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class).hasMessageContaining("Using.of");
            assertThat(closed).hasValue(1);
        }

        @Test
        void failsWithTheCloseFailureOverANullResult() {
            final IOException closeFailure = new IOException("close");
            final Try<String> result = Using.of(() -> () -> {
                throw closeFailure;
            }, r -> null);
            assertThat(result.getCause()).isSameAs(closeFailure);
        }

        @Test
        void nests() {
            final List<String> closed = new ArrayList<>();
            final Try<String> result = Using.of(() -> () -> closed.add("outer"), o ->
                    Using.of(() -> () -> closed.add("inner"), i -> "both").get());
            assertThat(result).isEqualTo(Try.success("both"));
            assertThat(closed).containsExactly("inner", "outer");
        }

        @Test
        void throwsOnNullArguments() {
            assertThrows(NullPointerException.class, () -> Using.of(null, r -> "x"));
            assertThrows(NullPointerException.class, () -> Using.of(() -> () -> {}, null));
        }
    }

    // -- Using.manager

    @Nested
    class Manager {

        @Test
        void returnsTheResultWhenNothingIsAcquired() {
            assertThat(Using.manager(use -> 42)).isEqualTo(Try.success(42));
        }

        @Test
        void releasesInReverseOrderOfAcquisitionAcrossBothForms() {
            final List<String> released = new ArrayList<>();
            final Try<String> result = Using.manager(use -> {
                use.acquire(() -> released.add("first"));
                use.acquire("second", released::add);
                use.acquire(() -> released.add("third"));
                return "ok";
            });
            assertThat(result).isEqualTo(Try.success("ok"));
            assertThat(released).containsExactly("third", "second", "first");
        }

        @Test
        void releasesManyResourcesOnceEachInReverseOrder() {
            final List<Integer> released = new ArrayList<>();
            final Try<Integer> result = Using.manager(use -> {
                int sum = 0;
                for (int i = 0; i < 100; i++) {
                    sum += use.acquire(i, released::add);
                }
                return sum;
            });
            assertThat(result).isEqualTo(Try.success(4950));
            final List<Integer> expected = new ArrayList<>();
            for (int i = 99; i >= 0; i--) {
                expected.add(i);
            }
            assertThat(released).isEqualTo(expected);
        }

        @Test
        void acquiresTheSameValueTwiceAndReleasesItTwice() {
            final List<String> released = new ArrayList<>();
            Using.manager(use -> {
                use.acquire("x", released::add);
                return use.acquire("x", released::add);
            });
            assertThat(released).containsExactly("x", "x");
        }

        @Test
        void failsWithANullPointerExceptionOnANullResultAndReleases() {
            final AtomicInteger released = new AtomicInteger();
            final Try<String> result = Using.manager(use -> {
                use.acquire(released::incrementAndGet);
                return null;
            });
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Using.manager");
            assertThat(released).hasValue(1);
        }

        @Test
        void rejectsANullResourceAndReleasesTheEarlierOnes() {
            final AtomicInteger released = new AtomicInteger();
            final Try<String> result = Using.manager(use -> {
                use.acquire(released::incrementAndGet);
                use.acquire((AutoCloseable) null);
                return "unreachable";
            });
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(released).hasValue(1);
        }

        @Test
        void rejectsANullValueOrANullRelease() {
            final AtomicInteger released = new AtomicInteger();
            final Try<String> nullValue = Using.manager(use -> {
                use.acquire(released::incrementAndGet);
                use.acquire(null, v -> {});
                return "unreachable";
            });
            final Try<String> nullRelease = Using.manager(use -> {
                use.acquire(released::incrementAndGet);
                use.acquire("value", null);
                return "unreachable";
            });
            assertThat(nullValue.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(nullRelease.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(released).hasValue(2);
        }

        @Test
        void throwsOnANullBlock() {
            assertThrows(NullPointerException.class, () -> Using.manager(null));
        }

        @Test
        void nests() {
            final List<String> released = new ArrayList<>();
            final Try<String> result = Using.manager(outer -> {
                outer.acquire("outer", released::add);
                return Using.manager(inner -> inner.acquire("inner", released::add)).get();
            });
            assertThat(result).isEqualTo(Try.success("inner"));
            assertThat(released).containsExactly("inner", "outer");
        }
    }

    // -- the manager after its block

    @Nested
    class AfterTheBlock {

        Using.Manager leak() {
            final AtomicReference<Using.Manager> leaked = new AtomicReference<>();
            Using.manager(use -> {
                leaked.set(use);
                return "ok";
            });
            return leaked.get();
        }

        @Test
        void acquireOfAnAutoCloseableThrowsAndClosesIt() {
            final Using.Manager manager = leak();
            final AtomicInteger closed = new AtomicInteger();
            assertThatThrownBy(() -> manager.acquire(closed::incrementAndGet))
                    .isInstanceOf(IllegalStateException.class)
                    .hasNoSuppressedExceptions();
            assertThat(closed).hasValue(1);
        }

        @Test
        void acquireOfAValueThrowsAndReleasesIt() {
            final Using.Manager manager = leak();
            final List<String> released = new ArrayList<>();
            assertThatThrownBy(() -> manager.acquire("late", released::add))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(released).containsExactly("late");
        }

        @Test
        void aReleaseFailureIsSuppressedInTheIllegalStateException() {
            final Using.Manager manager = leak();
            final IOException releaseFailure = new IOException("release");
            assertThatThrownBy(() -> manager.acquire("late", v -> {
                throw releaseFailure;
            })).isInstanceOf(IllegalStateException.class).hasSuppressedException(releaseFailure);
        }

        @Test
        void aFatalReleaseFailureSurfacesWithTheIllegalStateExceptionSuppressed() {
            final Using.Manager manager = leak();
            final OutOfMemoryError oom = new OutOfMemoryError("close");
            assertThatThrownBy(() -> manager.acquire(() -> {
                throw oom;
            })).isSameAs(oom);
            assertThat(oom.getSuppressed()).singleElement().isInstanceOf(IllegalStateException.class);
        }

        @Test
        void aNullResourceIsANullPointerException() {
            final Using.Manager manager = leak();
            assertThrows(NullPointerException.class, () -> manager.acquire((AutoCloseable) null));
            assertThrows(NullPointerException.class, () -> manager.acquire(null, v -> {}));
            assertThrows(NullPointerException.class, () -> manager.acquire("x", (CheckedConsumer<String>) null));
        }

        @Test
        void aReleaseThatAcquiresThroughItsManagerReleasesThatResourceAndFails() {
            final AtomicInteger lateReleases = new AtomicInteger();
            final List<String> released = new ArrayList<>();
            final Try<String> result = Using.manager(use -> {
                use.acquire("first", released::add);
                use.acquire("second", v -> {
                    released.add(v);
                    use.acquire(lateReleases::incrementAndGet);
                });
                return "ok";
            });
            assertThat(result.getCause()).isInstanceOf(IllegalStateException.class);
            assertThat(lateReleases).hasValue(1);
            assertThat(released).containsExactly("second", "first");
        }
    }
}
