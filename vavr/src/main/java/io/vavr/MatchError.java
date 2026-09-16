package io.vavr;

import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

/**
 * A {@link API.Match} throws a MatchError if no case matches the applied object.
 *
 * @author Daniel Dietrich
 */
public class MatchError extends NoSuchElementException {

    private final @Nullable Object obj;

    /**
     * Internally called by {@link API.Match}.
     *
     * @param obj The object which could not be matched, may be {@code null} if the matched value itself was {@code null}.
     */
    MatchError(@Nullable Object obj) {
        super((obj == null) ? "null" : "type: " + obj.getClass().getName() + ", value: " + obj);
        this.obj = obj;
    }

    /**
     * Returns the object which could not be matched.
     *
     * @return the object which could not be matched, or {@code null} if the matched value itself was {@code null}.
     */
    public @Nullable Object getObject() {
        return obj;
    }
}
