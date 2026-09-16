package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Vector;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.euler.Utils.MEMOIZED_FACTORIAL;
import static org.assertj.core.api.Assertions.assertThat;

public class Euler34Test {

    /**
     * <strong>Problem 34 Digit factorials</strong>
     * <p>
     * 145 is a curious number, as 1! + 4! + 5! = 1 + 24 + 120 = 145.
     * <p>
     * Find the sum of all numbers which are equal to the sum of the factorial
     * of their digits.
     * <p>
     * F
     * Note: as 1! = 1 and 2! = 2 are not sums they are not included.
     * <p>
     * See also <a href="https://projecteuler.net/problem=34">projecteuler.net
     * problem 34</a>.
     */
    @Test
    public void shouldSolveProblem34() {
        assertThat(sumOfDigitFactorial(145)).isEqualTo(145);
        assertThat(sumOfOfAllNumbersWhichAreEqualToSumOfDigitFactorial()).isEqualTo(40730);
    }

    private static int sumOfOfAllNumbersWhichAreEqualToSumOfDigitFactorial() {
        return Stream.rangeClosed(3, 2_540_160) // 9! * 7 = 2 540 160 is a seven digit number, as is 9! * 8, therefore 9! * 7 is the definitive upper limit we have to investigate.
                .filter(i -> i == sumOfDigitFactorial(i))
                .sum().intValue();
    }

    private static int sumOfDigitFactorial(int num) {
        return Vector.ofAll(Integer.toString(num).toCharArray())
                .map(c -> Character.digit(c, 10))
                .map(MEMOIZED_FACTORIAL)
                .sum().intValue();
    }
}
