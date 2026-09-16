package io.vavr.collection.euler;

import io.vavr.Function1;
import io.vavr.collection.Iterator;
import io.vavr.collection.Stream;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Scanner;

final class Utils {

    private Utils() {
    }

    static final Function1<Integer, BigInteger> MEMOIZED_FACTORIAL = Function1.of(Utils::factorial).memoized();

    static final Function1<Long, Boolean> MEMOIZED_IS_PRIME = Function1.of(Utils::isPrime).memoized();

    static Stream<BigInteger> fibonacci() {
        return Stream.of(BigInteger.ZERO, BigInteger.ONE).appendSelf(self -> self.zip(self.tail()).map(t -> t._1.add(t._2)));
    }

    static BigInteger factorial(int n) {
        return Stream.rangeClosed(1, n).map(BigInteger::valueOf).fold(BigInteger.ONE, BigInteger::multiply);
    }

    static Stream<Long> factors(long number) {
        return Stream.rangeClosed(1, (long) Math.sqrt(number))
                .filter(d -> number % d == 0)
                .flatMap(d -> Stream.of(d, number / d))
                .distinct();
    }

    static Stream<Long> divisors(long l) {
        return factors(l).filter((d) -> d < l);
    }

    static boolean isPrime(long val) {
        if (val < 2L) {
            return false;
        }
        if (val == 2L) {
            return true;
        }
        final double upperLimitToCheck = Math.sqrt(val);
        return !PrimeNumbers.primes().takeWhile(d -> d <= upperLimitToCheck).exists(d -> val % d == 0);
    }

    static Stream<String> readLines(File file) {
        try {
            return Stream.ofAll(new Iterator<String>() {

                final Scanner scanner = new Scanner(file);

                @Override
                public boolean hasNext() {
                    final boolean hasNext = scanner.hasNextLine();
                    if (!hasNext) {
                        scanner.close();
                    }
                    return hasNext;
                }

                @Override
                public String next() {
                    return scanner.nextLine();
                }
            });
        } catch (FileNotFoundException e) {
            return Stream.empty();
        }
    }

    static File file(String fileName) {
        final URL resource = Utils.class.getResource(fileName);
        if (resource == null) {
            throw new RuntimeException("resource not found");
        }
        return new File(resource.getFile());
    }

    static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    static boolean isPalindrome(String val) {
        return val.equals(reverse(val));
    }

    static boolean isPalindrome(int val) {
        return isPalindrome(Long.toString(val));
    }
}
