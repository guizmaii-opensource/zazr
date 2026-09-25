package com.guizmaii.zazr.test;

import com.guizmaii.zazr.CheckedFunction1;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Option;

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
     * Runs pass after pass of {@code gen}, each at the size of {@link #size(CheckConfig, int)}, until {@code sink}
     * received {@code config.samples()} values or asked to stop.
     *
     * @throws IllegalStateException if more passes in a row than the discard budget produce no value
     */
    static <A> void passes(CheckConfig config, Gen<A> gen, Gen.Sink<? super A> sink) {
        final int samples = config.samples();
        final Sampling sampling = new Sampling(config.seed(), config.maxDiscards());
        final int[] delivered = { 0 };
        int emptyInARow = 0;
        while (delivered[0] < samples) {
            final int before = delivered[0];
            final boolean more = gen.run(sampling, size(config, before), value -> {
                delivered[0]++;
                return sink.accept(value) && delivered[0] < samples;
            });
            if (!more) {
                return;
            } else if (delivered[0] > before) {
                emptyInARow = 0;
            } else if (++emptyInARow > config.maxDiscards()) {
                throw new IllegalStateException("the generator produced no value in " + emptyInARow
                        + " passes in a row, more than the discard budget of " + config.maxDiscards());
            }
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
                gen.run(new Sampling(seed, config.maxDiscards()), config.size(), sink);
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
