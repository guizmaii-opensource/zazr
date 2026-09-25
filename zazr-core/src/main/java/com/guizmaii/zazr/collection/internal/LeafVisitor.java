package com.guizmaii.zazr.collection.internal;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface LeafVisitor<T extends @Nullable Object> {
    int visit(int index, T leaf, int start, int end);
}
