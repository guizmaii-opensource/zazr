package com.guizmaii.zazr.test;

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckableTest {

    // -- check()

    @Test
    public void shouldDelegateNoArgCheckThroughSizeAndTriesOverload() {
        final int[] capturedSize = { -1 };
        final int[] capturedTries = { -1 };
        final Checkable checkable = new Checkable() {
            @Override
            public CheckResult check(Random randomNumberGenerator, int size, int tries) {
                return new CheckResult.Satisfied("shouldDelegate", 0, false);
            }

            @Override
            public CheckResult check(int size, int tries) {
                capturedSize[0] = size;
                capturedTries[0] = tries;
                return check(RNG.get(), size, tries);
            }
        };

        final CheckResult result = checkable.check();

        assertThat(capturedSize[0]).isEqualTo(Checkable.DEFAULT_SIZE);
        assertThat(capturedTries[0]).isEqualTo(Checkable.DEFAULT_TRIES);
        assertThat(result.isSatisfied()).isTrue();
    }
}
