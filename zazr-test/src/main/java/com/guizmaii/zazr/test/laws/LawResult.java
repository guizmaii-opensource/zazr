package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.CheckConfig;
import com.guizmaii.zazr.test.CheckResult;

import java.util.Objects;

/**
 * The result of checking one law: the law's name and the result of its check.
 *
 * @param name   the name of the law
 * @param result the result of its check, which carries the counterexample and the seed that replays it
 */
public record LawResult(String name, CheckResult result) {

    /**
     * Creates a law result.
     *
     * @param name   the name of the law
     * @param result the result of its check
     * @throws NullPointerException if an argument is null
     */
    public LawResult {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(result, "result is null");
    }

    /**
     * Whether the law held for every sample.
     *
     * @return true when the result is {@link CheckResult.Satisfied}
     */
    public boolean isSatisfied() {
        return result.isSatisfied();
    }

    /**
     * Describes the result in one line: the law's name and, for a failure, the sample number, the counterexample,
     * the explanation or the exception, and the seed that replays it.
     *
     * @return a description of the result
     */
    public String describe() {
        return name + ": " + switch (result) {
            case CheckResult.Satisfied(var samples) -> "satisfied (" + samples + " samples)";
            case CheckResult.Falsified(var sampleNumber, var seed, var counterexample, var message) ->
                    "falsified at sample " + sampleNumber + " by " + counterexample + message.map(m -> ": " + m).getOrElse("")
                            + replay(seed);
            case CheckResult.Erroneous(var sampleNumber, var seed, var cause, var sample) ->
                    "erroneous at sample " + sampleNumber + sample.map(values -> " with " + values).getOrElse(", while generating it")
                            + ": " + cause + replay(seed);
        };
    }

    private static String replay(long seed) {
        return " (seed " + seed + ", replay with -D" + CheckConfig.SEED_PROPERTY + "=" + seed + ")";
    }
}
