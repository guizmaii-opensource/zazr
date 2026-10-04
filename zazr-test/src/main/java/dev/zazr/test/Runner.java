package dev.zazr.test;

import dev.zazr.CheckedFunction1;
import dev.zazr.Tuple;
import dev.zazr.control.Option;
import org.jspecify.annotations.Nullable;

/**
 * Runs the checks of {@link Check}: generates the samples of a run and checks each one.
 */
final class Runner {

    private Runner() {}

    /**
     * The size of the pass that starts after {@code done} samples: it grows linearly from 0 for the first sample to
     * the configured size for the last one.
     */
    static int size(CheckConfig config, int done) {
        int samples = config.samples();
        if (samples <= 1) {
            return config.size();
        }
        return (int) Math.min(config.size(), (long) config.size() * done / (samples - 1));
    }

    /**
     * Runs pass after pass of {@code gen} until {@code sink} received {@code config.samples()} values or asked to
     * stop. A pass runs at the size of {@link #size(CheckConfig, int)}; after passes without a value, the next one
     * runs at a larger size, doubling up to the configured size, so a generator with no value at a small size (a
     * filter that rejects the empty list) moves on to larger sizes. Each pass without a value spends one discard of
     * the run's budget, which starts again at each delivered sample.
     *
     * @throws IllegalStateException if the discard budget is exceeded before a sample
     */
    static <A> void passes(CheckConfig config, Gen<A> gen, Gen.Sink<? super A> sink) {
        int samples = config.samples();
        Sampling sampling = new Sampling(config.seed(), config.maxDiscards(), config.size());
        int[] delivered = {0};
        @SuppressWarnings("Var") // the passes without a value since the last one, across the passes of this loop
        long emptyInARow = 0;
        while (delivered[0] < samples) {
            int before = delivered[0];
            int gaveUp = sampling.filtersGaveUp;
            boolean more = gen.run(sampling, sampling.grow(size(config, before), emptyInARow), value -> {
                delivered[0]++;
                sampling.delivered();
                return sink.accept(value) && delivered[0] < samples;
            });
            if (!more) {
                return;
            } else if (delivered[0] > before) {
                emptyInARow = 0;
            } else {
                emptyInARow++;
                sampling.discard(1, sampling.filtersGaveUp > gaveUp);
            }
        }
    }

    /**
     * Runs one pass of {@code gen} at the configured size; the discard budget starts again at each value.
     *
     * @throws IllegalStateException if the discard budget is exceeded before a value, or a filter gave its pass up,
     *                               so a value is missing
     */
    static <A> void onePass(CheckConfig config, Gen<A> gen, Gen.Sink<? super A> sink) {
        Sampling sampling = new Sampling(config.seed(), config.maxDiscards(), config.size());
        boolean ended = gen.run(sampling, config.size(), value -> {
            sampling.discards = 0;
            return sink.accept(value);
        });
        if (ended && sampling.filtersGaveUp > 0) {
            throw sampling.filterGaveUp();
        }
    }

    /**
     * Checks {@code body} against the samples of {@code gen}: {@code config.samples()} of them, or every value of
     * one pass at the configured size when {@code all} is true.
     */
    static <T extends Tuple> CheckResult check(
            CheckConfig config, Gen<T> gen, CheckedFunction1<? super T, ?> body, boolean all) {
        long seed = config.seed();
        State state = new State();
        Gen.Sink<T> sink = sample -> {
            state.samples++;
            state.failure = evaluate(state.samples, seed, sample, body);
            return state.failure == null;
        };
        try {
            if (all) {
                onePass(config, gen, sink);
            } else {
                passes(config, gen, sink);
            }
        } catch (Throwable error) {
            rethrowFatal(error);
            return new CheckResult.Erroneous(state.samples + 1, seed, error, Option.none());
        }
        return state.failure == null ? new CheckResult.Satisfied(state.samples) : state.failure;
    }

    private static final class State {
        int samples;

        @Nullable
        CheckResult failure;
    }

    /// The failure of one sample, or null when it passed. A `Boolean` or a `TestResult` is judged; any other value
    /// passes, since the body completed without throwing (an AssertJ chain returns its `Assert`); `null` is
    /// erroneous.
    private static <T extends Tuple> @Nullable CheckResult evaluate(
            int sampleNumber, long seed, T sample, CheckedFunction1<? super T, ?> body) {
        try {
            return switch (body.apply(sample)) {
                case Boolean holds ->
                    holds ? null : new CheckResult.Falsified(sampleNumber, seed, sample, Option.none());
                case TestResult.Success ignored -> null;
                case TestResult.Failure(var explanation) ->
                    new CheckResult.Falsified(sampleNumber, seed, sample, Option.some(explanation));
                case null ->
                    new CheckResult.Erroneous(
                            sampleNumber,
                            seed,
                            new NullPointerException("the check returned null"),
                            Option.some(sample));
                case Object completed -> null;
            };
        } catch (AssertionError failure) {
            return new CheckResult.Falsified(sampleNumber, seed, sample, Option.ofNullable(failure.getMessage()));
        } catch (Throwable error) {
            rethrowFatal(error);
            return new CheckResult.Erroneous(sampleNumber, seed, error, Option.some(sample));
        }
    }

    /// A virtual machine error other than a stack overflow ends the check; an interrupt stays visible to the caller.
    private static void rethrowFatal(Throwable error) {
        if (error instanceof VirtualMachineError fatal && !(error instanceof StackOverflowError)) {
            throw fatal;
        } else if (error instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
