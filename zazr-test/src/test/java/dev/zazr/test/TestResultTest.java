package dev.zazr.test;

import dev.zazr.control.Option;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestResultTest {

    private static final TestResult OK = TestResult.succeed();
    private static final TestResult A = TestResult.fail("a failed");
    private static final TestResult B = TestResult.fail("b failed");

    @Test
    void successAndFailure() {
        assertThat(OK.isSuccess()).isTrue();
        assertThat(OK.isFailure()).isFalse();
        assertThat(OK.message()).isEqualTo(Option.none());
        assertThat(A.isSuccess()).isFalse();
        assertThat(A.isFailure()).isTrue();
        assertThat(A.message()).isEqualTo(Option.some("a failed"));
        assertThat(OK).isSameAs(TestResult.succeed()).isEqualTo(new TestResult.Success());
        assertThat(A).isEqualTo(new TestResult.Failure("a failed"));
        assertThatThrownBy(() -> TestResult.fail(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void and_() {
        assertThat(OK.and(OK)).isEqualTo(OK);
        assertThat(OK.and(B)).isEqualTo(B);
        assertThat(A.and(OK)).isEqualTo(A);
        assertThat(A.and(B)).isEqualTo(TestResult.fail("a failed\nb failed"));
        assertThatThrownBy(() -> OK.and(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void or_() {
        assertThat(OK.or(B)).isEqualTo(OK);
        assertThat(A.or(OK)).isEqualTo(OK);
        assertThat(OK.or(OK)).isEqualTo(OK);
        assertThat(A.or(B)).isEqualTo(TestResult.fail("neither of these holds:\n  a failed\n  b failed"));
        assertThat(A.and(B).or(B)).isEqualTo(TestResult.fail("neither of these holds:\n  a failed\n  b failed\n  b failed"));
        assertThatThrownBy(() -> OK.or(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void label_() {
        assertThat(OK.label("x")).isEqualTo(OK);
        assertThat(A.label("x")).isEqualTo(TestResult.fail("x: a failed"));
        assertThat(A.and(B).label("x")).isEqualTo(TestResult.fail("x:\n  a failed\n  b failed"));
        assertThatThrownBy(() -> OK.label(null)).isInstanceOf(NullPointerException.class);
    }
}
