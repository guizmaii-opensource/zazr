package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Vector;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The package-private operations of the public collections that this package calls.
 * <p>
 * They stay package-private in {@code com.guizmaii.zazr.collection}, so that they are not API; instead, each
 * public class hands its accessor to this holder from its static initializer, and the internal code reaches
 * them through {@link #vector()} and {@link #queue()}. Each accessor is set once. An accessor read before its
 * class has been initialized initializes that class first, which registers it.
 */
public final class Access {

    /** The package-private operations of {@link Vector}. */
    public interface VectorAccess {
        <T extends @Nullable Object> Iterator<T> reverseIterator(Vector<T> vector);
    }

    /** The package-private operations of {@link Queue}. */
    public interface QueueAccess {
        <T extends @Nullable Object> Iterator<T> reverseIterator(Queue<T> queue);
    }

    private static volatile @Nullable VectorAccess vectorAccess;
    private static volatile @Nullable QueueAccess queueAccess;

    private Access() {
    }

    public static void setVectorAccess(VectorAccess access) {
        if (vectorAccess != null) {
            throw new IllegalStateException("VectorAccess is already set");
        }
        vectorAccess = Objects.requireNonNull(access, "access is null");
    }

    public static void setQueueAccess(QueueAccess access) {
        if (queueAccess != null) {
            throw new IllegalStateException("QueueAccess is already set");
        }
        queueAccess = Objects.requireNonNull(access, "access is null");
    }

    public static VectorAccess vector() {
        final VectorAccess access = vectorAccess;
        if (access != null) {
            return access;
        }
        initialize(Vector.class);
        return Objects.requireNonNull(vectorAccess, "VectorAccess is not set");
    }

    public static QueueAccess queue() {
        final QueueAccess access = queueAccess;
        if (access != null) {
            return access;
        }
        initialize(Queue.class);
        return Objects.requireNonNull(queueAccess, "QueueAccess is not set");
    }

    private static void initialize(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException x) {
            throw new IllegalStateException(x);
        }
    }
}
