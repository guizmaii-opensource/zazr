
package dev.zazr.test;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import dev.zazr.*;
import java.util.Objects;

/**
 * Checks a property against generated values, from 1 to 8 generators.
 * <p>
 * The property is a function of the generated values that returns {@code true} when it holds. It may also
 * throw an {@link AssertionError}, such as a failed JUnit or AssertJ assertion, which falsifies the sample
 * like {@code false} and keeps its message; any other exception makes the check {@link CheckResult.Erroneous}.
 * <p>
 * {@code check} and {@code checkN} run {@link CheckConfig#samples()} samples, pass after pass of the
 * generators, with a size that grows from 0 for the first sample to {@link CheckConfig#size()} for the last:
 * the first failure found is usually a small one. {@code checkAll} runs one pass, so it checks every value of
 * finite generators once. Each stops at its first failure and fails the test: it throws an
 * {@link AssertionError} with the sample, its number and the seed, which any test framework reports.
 * <p>
 * {@code evaluate}, {@code evaluateN} and {@code evaluateAll} run the same checks and return the
 * {@link CheckResult} instead, for code that looks at the outcome.
 */
public final class Check {

    private Check() {
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, CheckedFunction1)}.
     *
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1> void check(Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        evaluate(g1, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, CheckedFunction1)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1> void check(CheckConfig config, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        evaluate(config, g1, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1> void checkN(int samples, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        evaluateN(samples, g1, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1> void checkAll(Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        evaluateAll(g1, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, CheckedFunction1)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1> void checkAll(CheckConfig config, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        evaluateAll(config, g1, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, CheckedFunction1)}.
     *
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult evaluate(Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, g1.<Tuple1<T1>>map(Tuple::of), sample -> body.apply(sample._1()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1> CheckResult evaluateN(int samples, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult evaluateAll(Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, g1.<Tuple1<T1>>map(Tuple::of), sample -> body.apply(sample._1()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, CheckedFunction2)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        evaluate(g1, g2, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, CheckedFunction2)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        evaluate(config, g1, g2, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        evaluateN(samples, g1, g2, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        evaluateAll(g1, g2, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, CheckedFunction2)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        evaluateAll(config, g1, g2, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, CheckedFunction2)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2), sample -> body.apply(sample._1(), sample._2()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2), sample -> body.apply(sample._1(), sample._2()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, Gen, CheckedFunction3)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        evaluate(g1, g2, g3, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, CheckedFunction3)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        evaluate(config, g1, g2, g3, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        evaluateN(samples, g1, g2, g3, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        evaluateAll(g1, g2, g3, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, Gen, CheckedFunction3)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        evaluateAll(config, g1, g2, g3, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, CheckedFunction3)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, g3, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3), sample -> body.apply(sample._1(), sample._2(), sample._3()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, g3, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, g3, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param body the property of 3 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3), sample -> body.apply(sample._1(), sample._2(), sample._3()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, Gen, Gen, CheckedFunction4)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        evaluate(g1, g2, g3, g4, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, CheckedFunction4)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        evaluate(config, g1, g2, g3, g4, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        evaluateN(samples, g1, g2, g3, g4, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        evaluateAll(g1, g2, g3, g4, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, Gen, Gen, CheckedFunction4)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        evaluateAll(config, g1, g2, g3, g4, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, CheckedFunction4)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, g3, g4, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, g3, g4, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param body the property of 4 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, Gen, Gen, Gen, CheckedFunction5)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        evaluate(g1, g2, g3, g4, g5, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, CheckedFunction5)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        evaluate(config, g1, g2, g3, g4, g5, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        evaluateN(samples, g1, g2, g3, g4, g5, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        evaluateAll(g1, g2, g3, g4, g5, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, Gen, Gen, Gen, CheckedFunction5)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        evaluateAll(config, g1, g2, g3, g4, g5, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, CheckedFunction5)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, g3, g4, g5, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param body the property of 5 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction6)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        evaluate(g1, g2, g3, g4, g5, g6, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction6)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        evaluate(config, g1, g2, g3, g4, g5, g6, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5, T6> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        evaluateN(samples, g1, g2, g3, g4, g5, g6, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        evaluateAll(g1, g2, g3, g4, g5, g6, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction6)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        evaluateAll(config, g1, g2, g3, g4, g5, g6, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction6)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5, g6), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5(), sample._6()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5, T6> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, g6, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param body the property of 6 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5, g6), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5(), sample._6()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction7)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        evaluate(g1, g2, g3, g4, g5, g6, g7, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction7)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        evaluate(config, g1, g2, g3, g4, g5, g6, g7, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5, T6, T7> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        evaluateN(samples, g1, g2, g3, g4, g5, g6, g7, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        evaluateAll(g1, g2, g3, g4, g5, g6, g7, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction7)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        evaluateAll(config, g1, g2, g3, g4, g5, g6, g7, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction7)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(g7, "g7 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5, g6, g7), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5(), sample._6(), sample._7()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, g6, g7, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param body the property of 7 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(g7, "g7 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5, g6, g7), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5(), sample._6(), sample._7()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, and fails
     * the test when a sample breaks it, as {@link #check(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction8)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> void check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        evaluate(g1, g2, g3, g4, g5, g6, g7, g8, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}, and fails the test at the first sample that breaks it.
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction8)} returns the result instead.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              sample breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> void check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        evaluate(config, g1, g2, g3, g4, g5, g6, g7, g8, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged,
     * and fails the test when a sample breaks it.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @throws AssertionError           with the counterexample or the error, the sample number and the seed,
     *                                  when a sample breaks the property or something throws
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> void checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        evaluateN(samples, g1, g2, g3, g4, g5, g6, g7, g8, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()},
     * and fails the test when a value breaks it.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> void checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        evaluateAll(g1, g2, g3, g4, g5, g6, g7, g8, body).assertIsSatisfied();
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()},
     * as {@link #evaluateAll(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction8)}, and fails the test when a
     * value breaks it.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @throws AssertionError       with the counterexample or the error, the sample number and the seed, when a
     *                              value breaks the property or something throws
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> void checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        evaluateAll(config, g1, g2, g3, g4, g5, g6, g7, g8, body).assertIsSatisfied();
    }

    /**
     * Evaluates {@code body} against {@link CheckConfig#defaults()}, 200 samples unless configured otherwise, as
     * {@link #evaluate(CheckConfig, Gen, Gen, Gen, Gen, Gen, Gen, Gen, Gen, CheckedFunction8)}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult evaluate(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        return evaluate(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, g8, body);
    }

    /**
     * Evaluates {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails, and returns the
     * result without throwing.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult evaluate(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(g7, "g7 is null");
        Objects.requireNonNull(g8, "g8 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5, g6, g7, g8), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5(), sample._6(), sample._7(), sample._8()), false);
    }

    /**
     * Evaluates {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()}
     * unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult evaluateN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        return evaluate(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, g6, g7, g8, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, with
     * {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult evaluateAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        return evaluateAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, g8, body);
    }

    /**
     * Evaluates {@code body} against every value of one pass of the generators, at the size
     * {@code config.size()}: every combination of the values of finite generators, each checked once. A random
     * generator gives one value per pass. The number of samples of {@code config} is not used. It stops at the
     * first sample that fails, and returns the result without throwing.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param g3   the generator of the 3rd value
     * @param g4   the generator of the 4th value
     * @param g5   the generator of the 5th value
     * @param g6   the generator of the 6th value
     * @param g7   the generator of the 7th value
     * @param g8   the generator of the 8th value
     * @param body the property of 8 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @param <T3> the type of the 3rd value
     * @param <T4> the type of the 4th value
     * @param <T5> the type of the 5th value
     * @param <T6> the type of the 6th value
     * @param <T7> the type of the 7th value
     * @param <T8> the type of the 8th value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult evaluateAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(g5, "g5 is null");
        Objects.requireNonNull(g6, "g6 is null");
        Objects.requireNonNull(g7, "g7 is null");
        Objects.requireNonNull(g8, "g8 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4, g5, g6, g7, g8), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4(), sample._5(), sample._6(), sample._7(), sample._8()), true);
    }
}