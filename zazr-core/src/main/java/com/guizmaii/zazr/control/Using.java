package com.guizmaii.zazr.control;

import com.guizmaii.zazr.CheckedConsumer;
import com.guizmaii.zazr.CheckedFunction1;
import com.guizmaii.zazr.internal.TryModule;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.internal.Throwables.isFatal;
import static com.guizmaii.zazr.internal.Throwables.sneakyThrow;

/// Runs a computation over resources and releases them afterwards, whatever happened, with the outcome in a [Try].
///
/// [#of(Callable, CheckedFunction1)] manages one resource:
/// ```java
/// Try<String> line = Using.of(() -> Files.newBufferedReader(path), BufferedReader::readLine);
/// ```
/// [#manager(CheckedFunction1)] manages any number of resources, decided while the block runs. Each
/// [Manager#acquire(AutoCloseable)] registers one; they are all released in reverse order of acquisition when the
/// block ends, whether it returned or threw, including when a later acquisition threw:
/// ```java
/// Try<Long> copied = Using.manager(use -> {
///     var in  = use.acquire(new FileInputStream(source));
///     var out = use.acquire(new FileOutputStream(target));
///     return in.transferTo(out);
/// });
/// ```
///
/// ## Which throwable surfaces
///
/// When the block and one or more releases throw, or several releases throw, one throwable surfaces. The first one
/// thrown surfaces until a strictly more severe one is thrown, which takes its place, with the earlier one
/// [suppressed][Throwable#addSuppressed(Throwable)] in it; a throwable that is not more severe is suppressed in the
/// one surfacing when it is thrown. The severity scale, from the most severe:
///
/// 1. [VirtualMachineError], such as [OutOfMemoryError] and [StackOverflowError];
/// 2. [LinkageError];
/// 3. [InterruptedException] and `ThreadDeath`;
/// 4. any other throwable.
///
/// Unlike Java's `try`-with-resources, a more severe throwable
/// thrown by a release is therefore never hidden under a less severe one thrown by the block: an [OutOfMemoryError]
/// from `close()` surfaces, with the block's exception suppressed in it. A release that rethrows the throwable that
/// is surfacing (a `close()` rethrowing the block's exception, for instance) adds nothing: a throwable is never
/// suppressed in itself.
///
/// The outcome is then captured as [Try#of(Callable)] does: a `Success` of the result, a `Failure` of the throwable
/// that surfaced, or, when that throwable is fatal (see [Try]), the throwable itself rethrown. A `null` result is a
/// `Failure` of a [NullPointerException], and so is a `null` resource.
///
/// This is a port of `scala.util.Using` from the Scala 3 standard library, which ships the Scala 2.13 library's
/// `Using` unchanged
/// ([source](https://github.com/scala/scala/blob/2.13.x/src/library/scala/util/Using.scala)): `Using.of` is
/// `Using.apply`, `Using.manager` is `Using.Manager.apply`, and the severity scale is its `preferentiallySuppress`.
/// Two behaviours differ from that source: a throwable is never suppressed in itself (Scala's `addSuppressed` call
/// then throws an [IllegalArgumentException]), and [Manager#acquire(AutoCloseable)] after the block releases the
/// resource it was given before throwing (Scala's does not release it).
public final class Using {

    private Using() {
    }

    /// Obtains a resource from `resource`, passes it to `f` and closes it afterwards, whatever `f` did.
    ///
    /// The result is a `Success` of what `f` returned, or a `Failure` of what obtaining the resource, `f` or
    /// `close()` threw. When both `f` and `close()` throw, the more severe throwable surfaces with the other
    /// suppressed in it; on equal severity, the one from `f` (see [Using]). Fatal throwables are rethrown, as by
    /// [Try#of(Callable)]. The resource is not closed when obtaining it threw.
    ///
    /// @param resource obtains the resource; called once. A `null` resource is a `Failure` of a
    ///                 [NullPointerException]
    /// @param f        the computation over the resource
    /// @param <R>      the resource type
    /// @param <T>      the result type
    /// @return `Success` of the result of `f`, or a `Failure`; a `null` result is a `Failure` of a
    ///         [NullPointerException]
    /// @throws NullPointerException if `resource` or `f` is null
    public static <R extends AutoCloseable, T extends @Nullable Object> Try<T> of(Callable<? extends R> resource,
                                                                                 CheckedFunction1<? super R, ? extends T> f) {
        Objects.requireNonNull(resource, "resource is null");
        Objects.requireNonNull(f, "f is null");
        try {
            final T value = use(resource.call(), f);
            return value == null ? TryModule.nullResult("Using.of") : new Try.Success<>(value);
        } catch (Throwable t) {
            return new Try.Failure<>(t);
        }
    }

    /// Runs `f` with a new [Manager], then releases every resource acquired through it, in reverse order of
    /// acquisition, whatever `f` did.
    ///
    /// The result is a `Success` of what `f` returned, or a `Failure` of the most severe throwable thrown by `f` and the
    /// releases, the others suppressed in it (see [Using]). Every resource is released even when `f` or an earlier
    /// release threw. Fatal throwables are rethrown, as by [Try#of(Callable)].
    ///
    /// The manager is valid only while `f` runs; see [Manager#acquire(AutoCloseable)].
    ///
    /// @param f   the computation, given the manager that acquires its resources
    /// @param <T> the result type
    /// @return `Success` of the result of `f`, or a `Failure`; a `null` result is a `Failure` of a
    ///         [NullPointerException]
    /// @throws NullPointerException if `f` is null
    public static <T extends @Nullable Object> Try<T> manager(CheckedFunction1<? super Manager, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        try {
            final @Nullable T value = new Manager().manage(f);
            return value == null ? TryModule.nullResult("Using.manager") : new Try.Success<>(value);
        } catch (Throwable t) {
            return new Try.Failure<>(t);
        }
    }

    /// Acquires the resources of one [Using#manager(CheckedFunction1)] block, which releases them in reverse order of
    /// acquisition when it ends.
    ///
    /// A manager is valid only inside its block: after the block ended, `acquire` releases the resource it is given at
    /// once and throws an [IllegalStateException]. This includes a release that acquires through the manager being
    /// released. A manager is not safe for use by several threads at once.
    public static final class Manager {

        private static final @Nullable Object[] NO_SLOTS = new Object[0];

        /// Two slots per resource, in order of acquisition: the value, then its release action, `null` for an
        /// [AutoCloseable] closed by `close()`.
        private @Nullable Object[] slots = NO_SLOTS;
        private int size;
        private boolean closed;

        private Manager() {
        }

        /// Registers `resource`, closed by its `close()` when the block ends, and returns it.
        ///
        /// A lambda is an [AutoCloseable], so any release action can be registered this way; to keep the value and
        /// its release action apart, use [#acquire(Object, CheckedConsumer)].
        ///
        /// @param resource the resource
        /// @param <R>      the resource type
        /// @return `resource`
        /// @throws NullPointerException  if `resource` is null
        /// @throws IllegalStateException if the block of this manager has ended; `resource` is closed first, and a
        ///                               throwable from its `close()` is suppressed in the exception, or surfaces with
        ///                               the exception suppressed in it when it is more severe (see [Using])
        public <R extends AutoCloseable> R acquire(R resource) {
            Objects.requireNonNull(resource, "resource is null");
            if (closed) {
                return afterBlock(resource, null);
            }
            add(resource, null);
            return resource;
        }

        /// Registers `value`, released by `release` when the block ends, and returns it. For a value that is not an
        /// [AutoCloseable], or whose release is not its `close()`.
        ///
        /// @param value   the value to release
        /// @param release releases `value`; called once
        /// @param <A>     the value type
        /// @return `value`
        /// @throws NullPointerException  if `value` or `release` is null
        /// @throws IllegalStateException if the block of this manager has ended; `value` is released first, and a
        ///                               throwable from `release` is suppressed in the exception, or surfaces with the
        ///                               exception suppressed in it when it is more severe (see [Using])
        public <A> A acquire(A value, CheckedConsumer<? super A> release) {
            Objects.requireNonNull(value, "value is null");
            Objects.requireNonNull(release, "release is null");
            if (closed) {
                return afterBlock(value, release);
            }
            add(value, release);
            return value;
        }

        private void add(Object value, @Nullable CheckedConsumer<?> release) {
            if (size == slots.length) {
                slots = Arrays.copyOf(slots, size == 0 ? 8 : size * 2);
            }
            slots[size] = value;
            slots[size + 1] = release;
            size += 2;
        }

        private <T extends @Nullable Object> @Nullable T manage(CheckedFunction1<? super Manager, ? extends T> f) throws Throwable {
            @Nullable T result = null;
            @Nullable Throwable toThrow = null;
            try {
                result = f.apply(this);
            } catch (Throwable t) {
                toThrow = t;
            }
            closed = true;
            final @Nullable Object[] acquired = slots;
            slots = NO_SLOTS;
            for (int i = size - 2; i >= 0; i -= 2) {
                try {
                    release(Objects.requireNonNull(acquired[i]), (CheckedConsumer<?>) acquired[i + 1]);
                } catch (Throwable t) {
                    toThrow = toThrow == null ? t : preferentiallySuppress(toThrow, t);
                }
            }
            size = 0;
            if (toThrow != null) {
                throw toThrow;
            }
            return result;
        }

        private static <T> T afterBlock(Object value, @Nullable CheckedConsumer<?> release) {
            Throwable toThrow = new IllegalStateException(
                    "Using.Manager: acquire after the block of the manager ended; the resource was released");
            try {
                release(value, release);
            } catch (Throwable t) {
                toThrow = preferentiallySuppress(toThrow, t);
            }
            return sneakyThrow(toThrow);
        }

        @SuppressWarnings("unchecked")
        private static void release(Object value, @Nullable CheckedConsumer<?> release) throws Exception {
            if (release == null) {
                ((AutoCloseable) value).close();
            } else {
                ((CheckedConsumer<Object>) release).accept(value);
            }
        }
    }

    private static <R extends AutoCloseable, T extends @Nullable Object> T use(@Nullable R resource,
                                                                              CheckedFunction1<? super R, ? extends T> f) throws Throwable {
        if (resource == null) {
            throw new NullPointerException("Using.of: the resource is null");
        }
        final T value;
        try {
            value = f.apply(resource);
        } catch (Throwable t) {
            try {
                resource.close();
            } catch (Throwable c) {
                throw preferentiallySuppress(t, c);
            }
            throw t;
        }
        resource.close();
        return value;
    }

    /// The throwable that surfaces when `secondary` is thrown after `primary`, with the other one suppressed in it: the
    /// more severe of the two, `primary` on equal severity, and `primary` alone when they are the same throwable.
    private static Throwable preferentiallySuppress(Throwable primary, Throwable secondary) {
        if (secondary == primary) {
            return primary;
        }
        if (severity(secondary) > severity(primary)) {
            secondary.addSuppressed(primary);
            return secondary;
        }
        primary.addSuppressed(secondary);
        return primary;
    }

    private static int severity(Throwable t) {
        if (t instanceof VirtualMachineError) {
            return 3;
        }
        if (t instanceof LinkageError) {
            return 2;
        }
        // the remaining fatal throwables: InterruptedException and ThreadDeath
        return isFatal(t) ? 1 : 0;
    }
}
