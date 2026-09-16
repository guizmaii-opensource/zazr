/**
 * Control structures like the disjoint union type {@linkplain com.guizmaii.zazr.control.Either}, the optional value type
 * {@linkplain com.guizmaii.zazr.control.Option} and {@linkplain com.guizmaii.zazr.control.Try} for exception handling.
 * <p>
 * <strong>Either</strong>
 * <p>
 * The control package contains an implementation of the {@linkplain com.guizmaii.zazr.control.Either} control which is either Left or Right.
 * A given Either is projected to a Left or a Right.
 * Both cases can be further processed with control operations map, flatMap, filter.
 * If a Right is projected to a Left, the Left control operations have no effect on the Right value.
 * If a Left is projected to a Right, the Right control operations have no effect on the Left value.
 * <p>
 * <strong>Option</strong>
 * <p>
 * The Option control is a replacement for {@linkplain java.util.Optional}. An Option is either
 * {@linkplain com.guizmaii.zazr.control.Option.Some} value or {@linkplain com.guizmaii.zazr.control.Option.None}.
 * In contrast to Optional, Option supports null values, i.e. {@code Option.some(null)} yields {@code Some(null)}.
 * However, {@code Option.of(null)} results in None.
 * <p>
 * <strong>Try</strong>
 * <p>
 * Exceptions are handled with the {@linkplain com.guizmaii.zazr.control.Try} control which is either a
 * {@linkplain com.guizmaii.zazr.control.Try.Success}, containing a result, or a {@linkplain com.guizmaii.zazr.control.Try.Failure},
 * containing a (non-fatal) Throwable as its cause.
 */
@NullMarked
package com.guizmaii.zazr.control;

import org.jspecify.annotations.NullMarked;
