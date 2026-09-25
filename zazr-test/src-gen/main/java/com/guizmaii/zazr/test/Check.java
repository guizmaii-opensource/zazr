
package com.guizmaii.zazr.test;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import com.guizmaii.zazr.*;
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
 * finite generators once. Each check stops at its first failure and returns a {@link CheckResult} that
 * carries the sample, its number and the seed; {@link CheckResult#assertIsSatisfied()} turns it into a test
 * failure.
 */
public final class Check {

    private Check() {
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
     *
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult check(Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        return check(CheckConfig.defaults(), g1, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
     *
     * @param config the number of samples, the size and the seed
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult check(CheckConfig config, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, g1.<Tuple1<T1>>map(Tuple::of), sample -> body.apply(sample._1()), false);
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
     *
     * @param samples the number of samples
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException     if a generator or {@code body} is null
     * @throws IllegalArgumentException if {@code samples} is negative
     */
    public static <T1> CheckResult checkN(int samples, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult checkAll(Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
     *
     * @param config the size and the seed
     * @param g1   the generator of the 1st value
     * @param body the property of a value: true when it holds
     * @param <T1> the type of the 1st value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, CheckedFunction1<? super T1, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, g1.<Tuple1<T1>>map(Tuple::of), sample -> body.apply(sample._1()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2), sample -> body.apply(sample._1(), sample._2()), false);
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
     *
     * @param g1   the generator of the 1st value
     * @param g2   the generator of the 2nd value
     * @param body the property of 2 values: true when it holds
     * @param <T1> the type of the 1st value
     * @param <T2> the type of the 2nd value
     * @return the result of the check
     * @throws NullPointerException if an argument is null
     */
    public static <T1, T2> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, CheckedFunction2<? super T1, ? super T2, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2), sample -> body.apply(sample._1(), sample._2()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
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
    public static <T1, T2, T3> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, g3, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2, T3> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3), sample -> body.apply(sample._1(), sample._2(), sample._3()), false);
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2, T3> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, g3, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
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
    public static <T1, T2, T3> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, g3, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2, T3> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, CheckedFunction3<? super T1, ? super T2, ? super T3, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3), sample -> body.apply(sample._1(), sample._2(), sample._3()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
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
    public static <T1, T2, T3, T4> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, g3, g4, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2, T3, T4> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4()), false);
    }

    /**
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2, T3, T4> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
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
    public static <T1, T2, T3, T4> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, g3, g4, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2, T3, T4> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> body) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(g1, "g1 is null");
        Objects.requireNonNull(g2, "g2 is null");
        Objects.requireNonNull(g3, "g3 is null");
        Objects.requireNonNull(g4, "g4 is null");
        Objects.requireNonNull(body, "body is null");
        return Runner.check(config, Gen.zip(g1, g2, g3, g4), sample -> body.apply(sample._1(), sample._2(), sample._3(), sample._4()), true);
    }

    /**
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
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
    public static <T1, T2, T3, T4, T5> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, g3, g4, g5, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2, T3, T4, T5> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
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
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2, T3, T4, T5> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
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
    public static <T1, T2, T3, T4, T5> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2, T3, T4, T5> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, CheckedFunction5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> body) {
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
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
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
    public static <T1, T2, T3, T4, T5, T6> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2, T3, T4, T5, T6> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
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
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2, T3, T4, T5, T6> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, g6, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
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
    public static <T1, T2, T3, T4, T5, T6> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2, T3, T4, T5, T6> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, CheckedFunction6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, Boolean> body) {
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
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
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
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
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
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, g6, g7, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
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
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2, T3, T4, T5, T6, T7> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, CheckedFunction7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, Boolean> body) {
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
     * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
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
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult check(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        return check(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, g8, body);
    }

    /**
     * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
     * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
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
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult check(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
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
     * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
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
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult checkN(int samples, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        return check(CheckConfig.defaults().withSamples(samples), g1, g2, g3, g4, g5, g6, g7, g8, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
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
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult checkAll(Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
        return checkAll(CheckConfig.defaults(), g1, g2, g3, g4, g5, g6, g7, g8, body);
    }

    /**
     * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
     * every combination of the values of finite generators, each checked once. A random generator gives one
     * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
     * fails.
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
    public static <T1, T2, T3, T4, T5, T6, T7, T8> CheckResult checkAll(CheckConfig config, Gen<? extends T1> g1, Gen<? extends T2> g2, Gen<? extends T3> g3, Gen<? extends T4> g4, Gen<? extends T5> g5, Gen<? extends T6> g6, Gen<? extends T7> g7, Gen<? extends T8> g8, CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, Boolean> body) {
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