package dev.zazr.test;

import dev.zazr.CheckedFunction1;
import dev.zazr.Tuple;
import dev.zazr.control.Option;

/**
 * Runs the checks of {@link Check}: generates the samples of a run and checks each one.
 */
final class Runner {

    private Runner() {
    }

    /**
     * The size of the pass that starts after {@code done} samples: it grows linearly from 0 for the first sample to
     * the configured size for the last one.
     */
    static int size(CheckConfig config, int done) {
        final int samples = config.samples();
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
        final int samples = config.samples();
        final Sampling sampling = new Sampling(config.seed(), config.maxDiscards(), config.size());
        final int[] delivered = { 0 };
        long emptyInARow = 0;
        while (delivered[0] < samples) {
            final int before = delivered[0];
            final int gaveUp = sampling.filtersGaveUp;
            final boolean more = gen.run(sampling, sampling.grow(size(config, before), emptyInARow), value -> {
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
        final Sampling sampling = new Sampling(config.seed(), config.maxDiscards(), config.size());
        final boolean ended = gen.run(sampling, config.size(), value -> {
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
    static <T extends Tuple> CheckResult check(CheckConfig config, Gen<T> gen, CheckedFunction1<? super T, Boolean> body, boolean all) {
        final long seed = config.seed();
        final State state = new State();
        final Gen.Sink<T> sink = sample -> {
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
        CheckResult failure;
    }

    /// The failure of one sample, or null when it passed.
    private static <T extends Tuple> CheckResult evaluate(int sampleNumber, long seed, T sample, CheckedFunction1<? super T, Boolean> body) {
        try {
            final Boolean holds = body.apply(sample);
            if (holds == null) {
                return new CheckResult.Erroneous(sampleNumber, seed, new NullPointerException("the check returned null"), Option.some(sample));
            }
            return holds ? null : new CheckResult.Falsified(sampleNumber, seed, sample, Option.none());
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
