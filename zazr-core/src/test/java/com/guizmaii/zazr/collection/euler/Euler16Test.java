package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.collection.Vector;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Euler16Test {

    /**
     * <strong>Problem 16: Power digit sum</strong>
     * <p>
     * 2<sup>15</sup> = 32768 and the sum of its digits is 3 + 2 + 7 + 6 + 8 = 26.
     * <p>
     * What is the sum of the digits of the number 2<sup>1000</sup>?
     * <p>
     * See also <a href="https://projecteuler.net/problem=16">projecteuler.net problem 16</a>.
     */
    @Test
    public void shouldSolveProblem16() {
        assertThat(solve(15)).isEqualTo(26);
        assertThat(solve(1000)).isEqualTo(1_366);
    }

    private static long solve(int n) {
        return Vector.ofAll(BigInteger.valueOf(2).pow(n).toString().toCharArray())
                .map(c -> c - '0')
                .fold(0, (a, b) -> a + b);
    }
}
