/**
 * The implementation of the collections of {@code com.guizmaii.zazr.collection}: the tries, the red-black tree, the
 * single-pass iterator, the JDK views and the helpers shared by the public types.
 * <p>
 * <strong>This package is not API.</strong> {@code module-info.java} does not export it, so nothing outside the
 * module sees it on the module path; its types are {@code public} only so that the public collections can call them
 * across the package boundary. On the classpath they stay reachable, which is why the package name says
 * {@code internal}. Anything here may change or disappear without notice.
 */
@NullMarked
package com.guizmaii.zazr.collection.internal;

import org.jspecify.annotations.NullMarked;
