package io.vavr;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Small utility that allows to test code which write to standard error or standard out.
 *
 * @author Sebastian Zarnekow
 */
public class OutputTester {

    private static OutputStream failingOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException();
            }
        };
    }

    /**
     * Obtain a stream that fails on every attempt to write a byte.
     *
     * @return a new stream that will fail immediately.
     */
    public static PrintStream failingPrintStream() {
        return new PrintStream(failingOutputStream());
    }

    /**
     * Obtain a writer that fails on every attempt to write a byte.
     *
     * @return a new stream that will fail immediately.
     */
    public static PrintWriter failingPrintWriter() {
        return new PrintWriter(failingOutputStream());
    }

}
