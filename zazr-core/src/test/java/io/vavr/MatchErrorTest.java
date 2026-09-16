package io.vavr;

import org.junit.jupiter.api.Test;

import static io.vavr.API.*;
import static io.vavr.API.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class MatchErrorTest {

    @Test
    public void shouldReturnCorrectObject() {
        final Object obj = new Object();
        try {
            Match(obj).of(
                    Case($(0), 0)
            );
            failBecauseExceptionWasNotThrown(MatchError.class);
        } catch (MatchError matchError) {
            assertThat(matchError.getObject()).isEqualTo(obj);
        }
    }
}
