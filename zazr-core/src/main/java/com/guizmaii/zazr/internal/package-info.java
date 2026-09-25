/**
 * Helpers shared by {@code com.guizmaii.zazr}, {@code com.guizmaii.zazr.control} and
 * {@code com.guizmaii.zazr.collection} that are not collection internals.
 * <p>
 * <strong>This package is not API.</strong> {@code module-info.java} does not export it, so nothing outside the
 * module sees it on the module path; its types are {@code public} only so that the public packages can call them
 * across the package boundary. On the classpath they stay reachable, which is why the package name says
 * {@code internal}. Anything here may change or disappear without notice.
 */
@NullMarked
package com.guizmaii.zazr.internal;

import org.jspecify.annotations.NullMarked;
