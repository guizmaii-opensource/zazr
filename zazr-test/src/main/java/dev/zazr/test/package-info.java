/**
 * Property checks: {@link dev.zazr.test.Gen} generates values, {@link dev.zazr.test.Check} checks a
 * property against them, configured by a {@link dev.zazr.test.CheckConfig}. {@code Check.check} throws an
 * {@link AssertionError} when a value breaks the property, and {@code Check.evaluate} returns a
 * {@link dev.zazr.test.CheckResult} instead. A check is a method call, so it runs in any test framework.
 */
package dev.zazr.test;
