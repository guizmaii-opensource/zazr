package com.guizmaii.zazr.internal;

import com.guizmaii.zazr.control.Try;
import org.jspecify.annotations.Nullable;

public interface TryModule {

    /** The Failure a capturing constructor returns when the computation yields null, which Success cannot hold. */
    static <T extends @Nullable Object> Try<T> nullResult(String constructor) {
        return new Try.Failure<>(new NullPointerException(constructor + ": the computation returned null"));
    }

    /** The Failure {@code zipWith} returns when {@code f} yields null, which Success cannot hold. */
    static <T extends @Nullable Object> Try<T> nullZipResult() {
        return new Try.Failure<>(new NullPointerException("Try.zipWith: f returned null"));
    }
}
