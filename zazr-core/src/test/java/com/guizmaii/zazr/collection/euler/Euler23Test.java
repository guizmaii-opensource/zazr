package com.guizmaii.zazr.collection.euler;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Stream;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <strong>Problem 23: Non-abundant sums</strong>
 * <p>
 * A perfect number is a number for which the sum of its proper divisors is
 * exactly equal to the number. For example, the sum of the proper divisors of
 * 28 would be 1 + 2 + 4 + 7 + 14 = 28, which means that 28 is a perfect number.
 * <p>
 * A number n is called deficient if the sum of its proper divisors is less than
 * n and it is called abundant if this sum exceeds n.
 * <p>
 * As 12 is the smallest abundant number, 1 + 2 + 3 + 4 + 6 = 16, the smallest
 * number that can be written as the sum of two abundant numbers is 24. By
 * mathematical analysis, it can be shown that all integers greater than 28123
 * can be written as the sum of two abundant numbers. However, this upper limit
 * cannot be reduced any further by analysis even though it is known that the
 * greatest number that cannot be expressed as the sum of two abundant numbers
 * is less than this limit.
 * <p>
 * Find the sum of all the positive integers which cannot be written as the sum
 * of two abundant numbers.
 * <p>
 * See also <a href="https://projecteuler.net/problem=23">projecteuler.net
 * problem 23</a>.
 */
public class Euler23Test {

    private static final long SMALLEST_ABUNDANT_NUMBER = 12;
    private static final long SMALLEST_NUMBER_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS = 2 * SMALLEST_ABUNDANT_NUMBER;
    private static final long LOWER_LIMIT_FOUND_BY_MATHEMATICAL_ANALYSIS_FOR_NUMBERS_THAT_CAN_BE_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS = 28123;

    @Test
    public void shouldSolveProblem23() {
        List.range(1, SMALLEST_ABUNDANT_NUMBER).forEach(n -> Assertions.assertThat(isAbundant.apply(n)).isFalse());
        Assertions.assertThat(isAbundant.apply(SMALLEST_ABUNDANT_NUMBER)).isTrue();
        Assertions.assertThat(isAbundant.apply(28L)).isFalse();
        assertThat(canBeWrittenAsTheSumOfTwoAbundantNumbers(SMALLEST_NUMBER_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS - 1)).isFalse();
        assertThat(canBeWrittenAsTheSumOfTwoAbundantNumbers(SMALLEST_NUMBER_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS)).isTrue();
        assertThat(canBeWrittenAsTheSumOfTwoAbundantNumbers(LOWER_LIMIT_FOUND_BY_MATHEMATICAL_ANALYSIS_FOR_NUMBERS_THAT_CAN_BE_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS + 1)).isTrue();
        assertThat(sumOfAllPositiveIntegersThatCannotBeWrittenAsTheSumOfTwoAbundantNumbers()).isEqualTo(4179871L);
    }

    private static long sumOfAllPositiveIntegersThatCannotBeWrittenAsTheSumOfTwoAbundantNumbers() {
        return Stream.rangeClosed(1, LOWER_LIMIT_FOUND_BY_MATHEMATICAL_ANALYSIS_FOR_NUMBERS_THAT_CAN_BE_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS)
                .filter(l -> !canBeWrittenAsTheSumOfTwoAbundantNumbers(l))
                .sum().longValue();
    }

    private static boolean canBeWrittenAsTheSumOfTwoAbundantNumbers(long l) {
        if (l < SMALLEST_NUMBER_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS) {
            return false;
        }
        if (l == SMALLEST_NUMBER_WRITTEN_AS_THE_SUM_OF_TO_ABUNDANT_NUMBERS) {
            return true;
        }
        return Stream.rangeClosed(SMALLEST_ABUNDANT_NUMBER, l / 2).exists(a -> isAbundant.apply(a) && isAbundant.apply(l - a));
    }

    private static final Function<Long, Boolean> isAbundant = Memoize.of((Long l) -> Utils.divisors(l).sum().longValue() > l);
}
