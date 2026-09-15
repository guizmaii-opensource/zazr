package io.vavr;

import java.util.Comparator;

import static java.util.Comparator.comparing;

public final class TestComparators {

    private TestComparators() {
    }

    public static Comparator<Object> toStringComparator() {
        return comparing(String::valueOf);
    }

}
