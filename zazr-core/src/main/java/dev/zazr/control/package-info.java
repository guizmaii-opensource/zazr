/**
 * Control structures like the disjoint union type {@linkplain dev.zazr.control.Either}, the optional value type
 * {@linkplain dev.zazr.control.Option} and {@linkplain dev.zazr.control.Try} for exception handling.
 * <p>
 * <strong>Either</strong>
 * <p>
 * The control package contains an implementation of the {@linkplain dev.zazr.control.Either} control which is either Left or Right.
 * A given Either is projected to a Left or a Right.
 * Both cases can be further processed with control operations map, flatMap, filter.
 * If a Right is projected to a Left, the Left control operations have no effect on the Right value.
 * If a Left is projected to a Right, the Right control operations have no effect on the Left value.
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
