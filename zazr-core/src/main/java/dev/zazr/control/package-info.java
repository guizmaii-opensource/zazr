/**
 * Control structures like the disjoint union type {@linkplain dev.zazr.control.Either}, the optional value type
 * {@linkplain dev.zazr.control.Option}, {@linkplain dev.zazr.control.Try} for exception handling,
 * {@linkplain dev.zazr.control.Validation} for checks that keep every error, and {@linkplain dev.zazr.control.Using}
 * for resources. {@code Option}, {@code Either}, {@code Try} and {@code Validation} are sealed interfaces of records,
 * taken apart with pattern matching.
 * <p>
 * <strong>Either</strong>
 * <p>
 * An {@linkplain dev.zazr.control.Either} is either a {@code Left} or a {@code Right}. It is right-biased:
 * {@code map}, {@code flatMap} and {@code filterOrElse} work on the {@code Right} value and pass a {@code Left}
 * through. The {@code Left} side is reached with {@code mapLeft}, {@code flip} and {@code fold}.
 * <p>
 * <strong>Option</strong>
 * <p>
 * The Option control is a replacement for {@linkplain java.util.Optional}. An Option is either
 * {@linkplain dev.zazr.control.Option.Some} value or {@linkplain dev.zazr.control.Option.None}.
 * Like Optional, Option never holds null: {@code Option.some(null)} throws and {@code Option.ofNullable(null)}
 * is None.
 * <p>
 * <strong>Try</strong>
 * <p>
 * Exceptions are handled with the {@linkplain dev.zazr.control.Try} control which is either a
 * {@linkplain dev.zazr.control.Try.Success}, containing a result, or a {@linkplain dev.zazr.control.Try.Failure},
 * containing a (non-fatal) Throwable as its cause.
 */
@NullMarked
package dev.zazr.control;

import org.jspecify.annotations.NullMarked;
