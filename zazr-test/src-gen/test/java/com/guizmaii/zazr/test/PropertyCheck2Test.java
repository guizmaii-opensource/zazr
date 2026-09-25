
package com.guizmaii.zazr.test;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guizmaii.zazr.CheckedFunction2;
import com.guizmaii.zazr.Tuple;
import org.junit.jupiter.api.Test;

public class PropertyCheck2Test {

    static final Arbitrary<Object> OBJECTS = Gen.of(null).arbitrary();

    @Test
    public void shouldApplyForAllOfArity2() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(null, null);
        assertThat(forAll).isNotNull();
    }

    @Test
    public void shouldApplySuchThatOfArity2() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(OBJECTS, OBJECTS);
        final CheckedFunction2<Object, Object, Boolean> predicate = (o1, o2) -> true;
        final Property.Property2<Object, Object> suchThat = forAll.suchThat(predicate);
        assertThat(suchThat).isNotNull();
    }

    @Test
    public void shouldCheckTrueProperty2() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(OBJECTS, OBJECTS);
        final CheckedFunction2<Object, Object, Boolean> predicate = (o1, o2) -> true;
        final CheckResult result = forAll.suchThat(predicate).check();
        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.isExhausted()).isFalse();
    }

    @Test
    public void shouldCheckFalseProperty2() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(OBJECTS, OBJECTS);
        final CheckedFunction2<Object, Object, Boolean> predicate = (o1, o2) -> false;
        final CheckResult result = forAll.suchThat(predicate).check();
        assertThat(result.isFalsified()).isTrue();
        assertThat(result.message().isEmpty()).isTrue();
    }

    @Test
    public void shouldCheckSuccessfulPredicateResult2() {
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThatResult((o1, o2) -> PredicateResult.success()).check(0, 3);
        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.isExhausted()).isFalse();
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.message().isEmpty()).isTrue();
    }

    @Test
    public void shouldReportPredicateFailureMessage2() {
        final CheckResult result = Property.named("test")
                .forAll(Gen.of(1).arbitrary(), Gen.of(2).arbitrary())
                .suchThatResult((o1, o2) -> PredicateResult.failure("failed: " + Tuple.of(o1, o2)))
                .check(0, 3);
        assertThat(result.isFalsified()).isTrue();
        assertThat(result.isErroneous()).isFalse();
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.sample().get()).isEqualTo(Tuple.of(1, 2));
        assertThat(result.message().get()).isEqualTo("failed: (1, 2)");
        assertThat(result.error().isEmpty()).isTrue();
        assertThatThrownBy(result::assertIsSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failed: (1, 2)");
    }

    @Test
    public void shouldCheckErroneousPredicateResult2() {
        final Exception cause = new Exception("yay! (this is a negative test)");
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThatResult((o1, o2) -> { throw cause; }).check(0, 3);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).hasCause(cause);
        assertThat(result.sample().isDefined()).isTrue();
        assertThat(result.message().isEmpty()).isTrue();
    }

    @Test
    public void shouldReportNullPredicateResultAsErroneous2() {
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThatResult((o1, o2) -> null).check(0, 3);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).hasCauseInstanceOf(NullPointerException.class);
        assertThat(result.sample().isDefined()).isTrue();
    }

    @Test
    public void shouldRejectNullResultPredicate2() {
        assertThrows(NullPointerException.class, () -> Property.named("test").forAll(OBJECTS, OBJECTS).suchThatResult(null));
    }

    @Test
    public void shouldReportPostconditionFailureMessage2() {
        final CheckResult result = Property.named("test")
                .forAll(Gen.of(1).arbitrary(), Gen.of(2).arbitrary())
                .suchThat((o1, o2) -> true)
                .impliesResult((o1, o2) -> PredicateResult.failure("postcondition: " + Tuple.of(o1, o2)))
                .check(0, 3);
        assertThat(result.isFalsified()).isTrue();
        assertThat(result.sample().get()).isEqualTo(Tuple.of(1, 2));
        assertThat(result.message().get()).isEqualTo("postcondition: (1, 2)");
    }

    @Test
    public void shouldCheckSuccessfulResultImplication2() {
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThatResult((o1, o2) -> PredicateResult.success())
                .impliesResult((o1, o2) -> PredicateResult.success()).check(0, 3);
        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.isExhausted()).isFalse();
        assertThat(result.message().isEmpty()).isTrue();
    }

    @Test
    public void shouldSkipResultPostconditionForFalseBooleanPrecondition2() {
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThat((o1, o2) -> false)
                .impliesResult((o1, o2) -> { throw new AssertionError("must not run"); }).check(0, 3);
        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.isExhausted()).isTrue();
        assertThat(result.message().isEmpty()).isTrue();
    }

    @Test
    public void shouldDiscardRejectedPreconditionMessage2() {
        final Property.Property2<Object, Object> property = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThatResult((o1, o2) -> PredicateResult.failure("rejected input"));
        final CheckResult booleanResult = property
                .implies((o1, o2) -> { throw new AssertionError("must not run"); }).check(0, 3);
        final CheckResult detailedResult = property
                .impliesResult((o1, o2) -> { throw new AssertionError("must not run"); }).check(0, 3);
        for (CheckResult result : new CheckResult[] { booleanResult, detailedResult }) {
            assertThat(result.isSatisfied()).isTrue();
            assertThat(result.isExhausted()).isTrue();
            assertThat(result.count()).isEqualTo(3);
            assertThat(result.message().isEmpty()).isTrue();
        }
    }

    @Test
    public void shouldAllowBooleanPostconditionAfterPredicateResult2() {
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThatResult((o1, o2) -> PredicateResult.success())
                .implies((o1, o2) -> false).check(0, 3);
        assertThat(result.isFalsified()).isTrue();
        assertThat(result.message().isEmpty()).isTrue();
    }

    @Test
    public void shouldReportNullPostconditionResultAsErroneous2() {
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThat((o1, o2) -> true).impliesResult((o1, o2) -> null).check(0, 3);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).hasCauseInstanceOf(NullPointerException.class);
        assertThat(result.sample().isDefined()).isTrue();
    }

    @Test
    public void shouldCheckErroneousPostconditionResult2() {
        final Exception cause = new Exception("yay! (this is a negative test)");
        final CheckResult result = Property.named("test").forAll(OBJECTS, OBJECTS)
                .suchThat((o1, o2) -> true).impliesResult((o1, o2) -> { throw cause; }).check(0, 3);
        assertThat(result.isErroneous()).isTrue();
        assertThat(result.error().get()).hasCause(cause);
        assertThat(result.sample().isDefined()).isTrue();
    }

    @Test
    public void shouldRejectNullResultPostcondition2() {
        assertThrows(NullPointerException.class, () -> Property.named("test").forAll(OBJECTS, OBJECTS).suchThat((o1, o2) -> true).impliesResult(null));
    }

    @Test
    public void shouldCheckErroneousProperty2() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(OBJECTS, OBJECTS);
        final CheckedFunction2<Object, Object, Boolean> predicate = (o1, o2) -> { throw new RuntimeException("yay! (this is a negative test)"); };
        final CheckResult result = forAll.suchThat(predicate).check();
        assertThat(result.isErroneous()).isTrue();
    }

    @Test
    public void shouldCheckProperty2ImplicationWithTruePrecondition() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(OBJECTS, OBJECTS);
        final CheckedFunction2<Object, Object, Boolean> p1 = (o1, o2) -> true;
        final CheckedFunction2<Object, Object, Boolean> p2 = (o1, o2) -> true;
        final CheckResult result = forAll.suchThat(p1).implies(p2).check();
        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.isExhausted()).isFalse();
    }

    @Test
    public void shouldCheckProperty2ImplicationWithFalsePrecondition() {
        final Property.ForAll2<Object, Object> forAll = Property.named("test").forAll(OBJECTS, OBJECTS);
        final CheckedFunction2<Object, Object, Boolean> p1 = (o1, o2) -> false;
        final CheckedFunction2<Object, Object, Boolean> p2 = (o1, o2) -> true;
        final CheckResult result = forAll.suchThat(p1).implies(p2).check();
        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.isExhausted()).isTrue();
    }

    @Test
    public void shouldThrowOnProperty2CheckGivenNegativeTries() {
        assertThrows(IllegalArgumentException.class, () -> Property.named("test")
            .forAll(OBJECTS, OBJECTS)
            .suchThat((o1, o2) -> true)
            .check(Checkable.RNG.get(), 0, -1));
    }

    @Test
    public void shouldReturnErroneousProperty2CheckResultIfGenFails() {
        final Arbitrary<Object> failingGen = Gen.fail("yay! (this is a negative test)").arbitrary();
        final CheckResult result = Property.named("test")
            .forAll(failingGen, OBJECTS)
            .suchThat((o1, o2) -> true)
            .check();
        assertThat(result.isErroneous()).isTrue();
    }

    @Test
    public void shouldReturnErroneousProperty2CheckResultIfArbitraryFails() {
        final Arbitrary<Object> failingArbitrary = size -> { throw new RuntimeException("yay! (this is a negative test)"); };
        final CheckResult result = Property.named("test")
            .forAll(failingArbitrary, OBJECTS)
            .suchThat((o1, o2) -> true)
            .check();
        assertThat(result.isErroneous()).isTrue();
    }
}