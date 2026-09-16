package com.guizmaii.zazr;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Programmatic JMH entry point. A benchmark name regex may be passed as the first argument
 * (defaults to all benchmarks).
 */
public final class JmhRunner {

    private JmhRunner() {
    }

    public static void main(String[] args) throws Exception {
        final String include = args.length > 0 ? args[0] : ".*Benchmark.*";
        final Options options = new OptionsBuilder()
                .include(include)
                .shouldDoGC(true)
                .build();
        new Runner(options).run();
    }
}
