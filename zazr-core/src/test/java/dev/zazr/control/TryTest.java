package dev.zazr.control;

import dev.zazr.CheckedConsumer;
import dev.zazr.CheckedFunction1;
import dev.zazr.CheckedFunction3;
import dev.zazr.CheckedPredicate;
import dev.zazr.CheckedRunnable;
import dev.zazr.Tuple;
import dev.zazr.Tuple0;
import dev.zazr.collection.Vector;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TryTest {

    private static final String OK = "ok";
    private static final String FAILURE = "failure";

    @Test
    public void shouldNotBeIterable() {
        // design 3.2: list.addAll(someTry) must not compile
        assertThat(Iterable.class.isAssignableFrom(Try.class)).isFalse();
    }

    @Test
    public void shouldThrowTheCauseOnGetOfFailure() {
        NoSuchElementException cause = new NoSuchElementException();
        assertThatThrownBy(() -> Try.failure(cause).get()).isSameAs(cause);
    }

    @Nested
    class EnsuringTests {
        @Test
        public void shouldRunTheFinalizerOnSuccess() {
            AtomicInteger count = new AtomicInteger();
            Try.run(() -> count.set(0)).ensuring(() -> count.set(1));
            assertThat(count.get()).isEqualTo(1);
        }

        @Test
        public void shouldRunTheFinalizerOnFailure() {
            AtomicInteger count = new AtomicInteger();
            Try.run(() -> {
                        throw new IllegalStateException(FAILURE);
                    })
                    .ensuring(() -> count.set(1));
            assertThat(count.get()).isEqualTo(1);
        }

        @Test
        public void shouldReturnThisWhenTheFinalizerCompletes() {
            Try<String> success = success();
            assertThat(success.ensuring(() -> {})).isSameAs(success);
            Try<String> failure = failure();
            assertThat(failure.ensuring(() -> {})).isSameAs(failure);
        }

        @Test
        public void shouldFailWithWhatTheFinalizerThrowsOnSuccess() {
            IllegalStateException thrown = new IllegalStateException(FAILURE);
            Try<String> result = success().ensuring(() -> {
                throw thrown;
            });
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(thrown);
        }

        @Test
        public void shouldAcceptACheckedFinalizer() {
            Try<String> result = success().ensuring(() -> {
                throw new IOException("io");
            });
            assertThat(result.getCause()).isInstanceOf(IOException.class);
        }

        @Test
        public void shouldPreserveOriginalFailureWhenTheFinalizerAlsoThrows() {
            IllegalStateException original = new IllegalStateException("original");
            IllegalArgumentException finallyEx = new IllegalArgumentException("finally");
            Try<Object> result = Try.<Object>failure(original).ensuring(() -> {
                throw finallyEx;
            });
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(original);
            assertThat(result.getCause().getSuppressed()).containsExactly(finallyEx);
        }

        @Test
        public void shouldKeepTheFailureUnchangedWhenTheFinalizerRethrowsTheCause() {
            IllegalStateException original = new IllegalStateException("original");
            Try<Object> failure = Try.failure(original);
            Try<Object> result = failure.ensuring(() -> {
                throw original;
            });
            assertThat(result).isSameAs(failure);
            assertThat(result.getCause()).isSameAs(original);
            assertThat(original.getSuppressed()).isEmpty();
        }

        @Test
        public void shouldKeepTheCheckedCauseUnchangedWhenTheFinalizerRethrowsIt() {
            IOException original = new IOException("original");
            Try<Object> failure = Try.failure(original);
            Try<Object> result = failure.ensuring(() -> {
                throw original;
            });
            assertThat(result).isSameAs(failure);
            assertThat(original.getSuppressed()).isEmpty();
        }

        @Test
        public void shouldSuppressAnExceptionEqualToButNotTheSameAsTheCause() {
            IllegalStateException original = new IllegalStateException("same message");
            IllegalStateException other = new IllegalStateException("same message");
            Try<Object> failure = Try.failure(original);
            Try<Object> result = failure.ensuring(() -> {
                throw other;
            });
            assertThat(result).isSameAs(failure);
            assertThat(original.getSuppressed()).containsExactly(other);
        }

        @Test
        public void shouldFailWithTheCauseItselfWhenTheFinalizerOfASuccessThrowsIt() {
            IllegalStateException thrown = new IllegalStateException(FAILURE);
            Try<String> result = success()
                    .ensuring(() -> {
                        throw thrown;
                    })
                    .ensuring(() -> {
                        throw thrown;
                    });
            assertThat(result.getCause()).isSameAs(thrown);
            assertThat(thrown.getSuppressed()).isEmpty();
        }

        @Test
        public void shouldRethrowAFatalErrorFromTheFinalizerOfAFailure() {
            IllegalStateException original = new IllegalStateException("original");
            StackOverflowError fatal = new StackOverflowError();
            assertThatThrownBy(() -> Try.<Object>failure(original).ensuring(() -> {
                        throw fatal;
                    }))
                    .isSameAs(fatal);
            assertThat(original.getSuppressed()).isEmpty();
        }

        @Test
        public void shouldRethrowFatalThrowableFromTheFinalizerOnSuccess() {
            assertThrows(
                    InterruptedException.class,
                    () -> Try.success(1).ensuring(() -> {
                        throw new InterruptedException();
                    }));
        }

        @Test
        public void shouldRethrowFatalThrowableFromTheFinalizerWhenAlreadyFailure() {
            IllegalStateException original = new IllegalStateException("original");
            assertThrows(
                    InterruptedException.class,
                    () -> Try.<Object>failure(original).ensuring(() -> {
                        throw new InterruptedException();
                    }));
            assertThat(original.getSuppressed()).isEmpty();
        }

        @Test
        public void shouldThrowOnNullFinalizer() {
            assertThrows(NullPointerException.class, () -> success().ensuring(null));
        }
    }

    @Nested
    class ExistsTests {
        @Test
        public void shouldBeAwareOfPropertyThatHoldsExistsOfSuccess() {
            assertThat(Try.success(1).exists(i -> i == 1)).isTrue();
        }

        @Test
        public void shouldBeAwareOfPropertyThatNotHoldsExistsOfSuccess() {
            assertThat(Try.success(1).exists(i -> i == 2)).isFalse();
        }

        @Test
        public void shouldNotHoldPropertyExistsOfFailure() {
            assertThat(failure().exists(e -> true)).isFalse();
        }

        @Test
        public void shouldNotHoldPropertyExistsWhenPredicateThrows() {
            assertThrows(Error.class, () -> {
                Try.success(1).exists(e -> {
                    throw new Error("error");
                });
            });
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> failure().exists(null));
        }
    }

    @Nested
    class ContainsTests {
        @Test
        public void shouldContainTheSuccessValue() {
            assertThat(Try.success(1).contains(1)).isTrue();
            assertThat(Try.success(1).contains(2)).isFalse();
            assertThat(Try.success(1).contains(null)).isFalse();
        }

        @Test
        public void shouldNotContainAnythingOnFailure() {
            assertThat(TryTest.<Integer>failure().contains(1)).isFalse();
        }
    }

    @Nested
    class ForallTests {
        @Test
        public void shouldBeAwareOfPropertyThatHoldsForAllOfSuccess() {
            assertThat(Try.success(1).forAll(i -> i == 1)).isTrue();
        }

        @Test
        public void shouldBeAwareOfPropertyThatNotHoldsForAllOfSuccess() {
            assertThat(Try.success(1).forAll(i -> i == 2)).isFalse();
        }

        @Test // a property holds for all elements of no elements
        public void shouldNotHoldPropertyForAllOfFailure() {
            assertThat(failure().forAll(e -> true)).isTrue();
        }

        @Test
        public void shouldNotHoldPropertyForAllWhenPredicateThrows() {
            assertThrows(Error.class, () -> {
                Try.success(1).forAll(e -> {
                    throw new Error("error");
                });
            });
        }

        @Test
        public void shouldThrowOnNullPredicate() {
            assertThrows(NullPointerException.class, () -> failure().forAll(null));
        }
    }

    @Nested
    class OrelseTests {
        @Test
        public void shouldReturnSelfOnOrElseIfSuccess() {
            Try<Integer> success = Try.success(42);
            assertThat(success.orElse(Try.success(0))).isSameAs(success);
        }

        @Test
        public void shouldReturnSelfOnOrElseSupplierIfSuccess() {
            Try<Integer> success = Try.success(42);
            assertThat(success.orElse(() -> Try.success(0))).isSameAs(success);
        }

        @Test
        public void shouldReturnAlternativeOnOrElseIfFailure() {
            Try<Integer> success = Try.success(42);
            assertThat(Try.failure(new RuntimeException()).orElse(success)).isSameAs(success);
        }

        @Test
        public void shouldReturnAlternativeOnOrElseSupplierIfFailure() {
            Try<Integer> success = Try.success(42);
            assertThat(Try.failure(new RuntimeException()).orElse(() -> success))
                    .isSameAs(success);
        }
    }

    @Nested
    class TryOfTests {
        @Test
        public void shouldCreateSuccessWhenCallingTryOfCheckedFunction0() {
            assertThat(Try.of(() -> 1) instanceof Try.Success).isTrue();
        }

        @Test
        public void shouldCreateFailureWhenCallingTryOfCheckedFunction0() {
            assertThat(
                            Try.of(() -> {
                                        throw new Error("error");
                                    })
                                    instanceof Try.Failure)
                    .isTrue();
        }

        @Test
        public void shouldThrowNullPointerExceptionWhenCallingTryOfCheckedFunction0() {
            assertThatThrownBy(() -> Try.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("supplier is null");
        }
    }

    @Nested
    class TryFoldTests {
        @Test
        public void shouldReturnValueIfSuccess() {
            Try<Integer> success = Try.success(42);
            assertThat(success.fold(
                            t -> {
                                throw new AssertionError("Not expected to be called");
                            },
                            Function.identity()))
                    .isEqualTo(42);
        }

        @Test
        public void shouldReturnAlternateValueIfFailure() {
            Try<Integer> success = Try.failure(new NullPointerException("something was null"));
            assertThat(success.<Integer>fold(t -> 42, a -> {
                        throw new AssertionError("Not expected to be called");
                    }))
                    .isEqualTo(42);
        }
    }

    @Nested
    class TryRunTests {
        @Test
        public void shouldCreateSuccessWhenCallingTryRunCheckedRunnable() {
            assertThat(Try.run(() -> {}) instanceof Try.Success).isTrue();
        }

        @Test
        public void shouldCreateFailureWhenCallingTryRunCheckedRunnable() {
            assertThat(
                            Try.run(() -> {
                                        throw new Error("error");
                                    })
                                    instanceof Try.Failure)
                    .isTrue();
        }

        @Test
        public void shouldThrowNullPointerExceptionWhenCallingTryRunCheckedRunnable() {
            assertThatThrownBy(() -> Try.run(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("runnable is null");
        }
    }

    // -- checked exceptions (docs/design.md 3.1 follow-up, #53): CheckedFunctionN/CheckedRunnable now declare
    // a checked Exception instead of the broader Throwable; a checked IOException reaches the caller (or the
    // Failure) unwrapped and un-rewrapped through every adapter that used to sneaky-throw to fit a Callable.

    @Nested
    class CheckedExceptionPropagationTests {

        @Test
        public void shouldCaptureTheOriginalIOExceptionInstanceFromTryOf() {
            IOException cause = new IOException("boom");
            Try<?> result = Try.of(() -> {
                throw cause;
            });
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(cause);
        }

        @Test
        public void shouldCaptureTheOriginalIOExceptionInstanceFromTryRun() {
            IOException cause = new IOException("boom");
            Try<?> result = Try.run(() -> {
                throw cause;
            });
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(cause);
        }

        @Test
        public void shouldCaptureTheOriginalIOExceptionInstanceFromLiftTry() {
            IOException cause = new IOException("boom");
            CheckedFunction1<Integer, String> throwing = i -> {
                throw cause;
            };
            Try<String> result = CheckedFunction1.liftTry(throwing).apply(1);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(cause);
        }

        @Test
        public void shouldRethrowTheOriginalIOExceptionInstanceFromCurriedLastStep() throws Exception {
            IOException cause = new IOException("boom");
            CheckedFunction3<Integer, Integer, Integer, String> f = (a, b, c) -> {
                throw cause;
            };
            CheckedFunction1<Integer, String> lastStep = f.curried().apply(1).apply(2);
            assertThatThrownBy(() -> lastStep.apply(3)).isSameAs(cause);
        }

        @Test
        public void shouldRethrowTheOriginalIOExceptionInstanceFromCheckedFunctionUnchecked() {
            IOException cause = new IOException("boom");
            CheckedFunction1<Integer, String> checked = i -> {
                throw cause;
            };
            assertThatThrownBy(() -> checked.unchecked().apply(1)).isSameAs(cause);
        }

        @Test
        public void shouldRethrowTheOriginalIOExceptionInstanceFromCheckedRunnableUnchecked() {
            IOException cause = new IOException("boom");
            CheckedRunnable checked = () -> {
                throw cause;
            };
            assertThatThrownBy(() -> checked.unchecked().run()).isSameAs(cause);
        }

        @Test
        public void shouldRethrowTheOriginalIOExceptionInstanceFromCheckedConsumerUnchecked() {
            IOException cause = new IOException("boom");
            CheckedConsumer<Integer> checked = i -> {
                throw cause;
            };
            assertThatThrownBy(() -> checked.unchecked().accept(1)).isSameAs(cause);
        }

        @Test
        public void shouldRethrowTheOriginalIOExceptionInstanceFromCheckedPredicateUnchecked() {
            IOException cause = new IOException("boom");
            CheckedPredicate<Integer> checked = i -> {
                throw cause;
            };
            assertThatThrownBy(() -> checked.unchecked().test(1)).isSameAs(cause);
        }

        @Test
        public void shouldStillRethrowFatalErrorsFromTryOfInsteadOfCapturingThem() {
            VirtualMachineError fatal = new OutOfMemoryError("fatal");
            assertThatThrownBy(() -> Try.of(() -> {
                        throw fatal;
                    }))
                    .isSameAs(fatal);
        }
    }

    // -- allocation-aware rewrites (#54): CheckedFunctionN.lift builds the Option directly (no throwaway Try)
    // and preserves the exact exception-propagation semantics the Try-based implementation had.

    @Nested
    class AllocationAwareRewriteTests {

        @Test
        public void shouldLiftCaptureACheckedExceptionAsNone() {
            CheckedFunction1<Integer, String> throwing = i -> {
                throw new IOException("boom");
            };
            assertThat(CheckedFunction1.lift(throwing).apply(1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldLiftCaptureARuntimeExceptionAsNone() {
            CheckedFunction1<Integer, String> throwing = i -> {
                throw new IllegalStateException("boom");
            };
            assertThat(CheckedFunction1.lift(throwing).apply(1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldLiftCaptureANonFatalAssertionErrorAsNone() {
            CheckedFunction1<Integer, String> throwing = i -> {
                throw new AssertionError("boom");
            };
            assertThat(CheckedFunction1.lift(throwing).apply(1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldLiftPropagateAFatalOutOfMemoryError() {
            OutOfMemoryError fatal = new OutOfMemoryError("fatal");
            CheckedFunction1<Integer, String> throwing = i -> {
                throw fatal;
            };
            assertThatThrownBy(() -> CheckedFunction1.lift(throwing).apply(1)).isSameAs(fatal);
        }
    }

    @Nested
    class FailureCauseTests {
        @Test
        public void shouldRethrowInterruptedException() {
            assertThrows(InterruptedException.class, () -> Try.failure(new InterruptedException()));
        }

        @Test
        public void shouldRethrowOutOfMemoryError() {
            assertThrows(OutOfMemoryError.class, () -> Try.failure(new OutOfMemoryError()));
        }

        @Test
        public void shouldDetectNonFatalException() {
            Exception exception = new Exception();
            assertThat(Try.failure(exception).getCause()).isSameAs(exception);
        }

        @Test
        public void shouldSubsequentlyHandOverCause() {
            Supplier<?> inner = () -> {
                throw new UnknownError("\uD83D\uDCA9");
            };
            Supplier<?> outer = () -> Try.of(inner::get).get();
            try {
                Try.of(outer::get).get();
                Assertions.fail("Exception expected");
            } catch (UnknownError x) {
                Assertions.assertThat(x.getMessage()).isEqualTo("\uD83D\uDCA9");
            } catch (Throwable x) {
                Assertions.fail("Unexpected exception type: " + x.getClass().getName());
            }
        }

        @Test
        public void shouldCreateFailureOnNonFatalException() {
            assertThat(failure().getCause()).isExactlyInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    class FailureNonfatalTests {
        @Test
        public void shouldReturnAndNotThrowOnNonFatal() {
            assertThat(Try.failure(new Exception())).isNotNull();
        }
    }

    @Nested
    class FailureFatalTests {
        @Test
        public void shouldReturnToStringOnFatal() {
            try {
                Try.of(() -> {
                    throw new UnknownError("test");
                });
                fail("Exception Expected");
            } catch (UnknownError x) {
                assertThat(x.getMessage()).isEqualTo("test");
            }
        }

        @Test
        public void shouldReturnEqualsOnFatal() {
            UnknownError error = new UnknownError();
            try {
                Try.of(() -> {
                    throw error;
                });
                fail("Exception Expected");
            } catch (UnknownError x) {
                try {
                    Try.of(() -> {
                        throw error;
                    });
                    fail("Exception Expected");
                } catch (UnknownError fatal) {
                    assertThat(x.equals(fatal)).isEqualTo(true);
                }
            }
        }
    }

    @Nested
    class FailureTests {
        @Test
        public void shouldDetectFailureOfRunnable() {
            assertThat(Try.of(() -> {
                                throw new RuntimeException();
                            })
                            .isFailure())
                    .isTrue();
        }

        @Test
        public void shouldPassThroughFatalException() {
            assertThrows(UnknownError.class, () -> {
                Try.of(() -> {
                    throw new UnknownError();
                });
            });
        }
    }

    @Nested
    class IsfailureTests {
        @Test
        public void shouldDetectFailureOnNonFatalException() {
            assertThat(failure().isFailure()).isTrue();
        }
    }

    @Nested
    class IssuccessTests {
        @Test
        public void shouldDetectNonSuccessOnFailure() {
            assertThat(failure().isSuccess()).isFalse();
        }
    }

    @Nested
    class GetTests {
        @Test
        public void shouldThrowWhenGetOnFailure() {
            assertThrows(RuntimeException.class, () -> failure().get());
        }

        @Test
        public void shouldThrowUndeclaredThrowableExceptionWhenUsingDynamicProxiesAndGetThrows() {
            Supplier<?> testee = (Supplier<?>) Proxy.newProxyInstance(
                    Supplier.class.getClassLoader(),
                    new Class<?>[] {Supplier.class},
                    (proxy, method, args) -> Try.failure(new Exception()).get());
            assertThatThrownBy(testee::get)
                    .isInstanceOf(UndeclaredThrowableException.class)
                    .hasCauseExactlyInstanceOf(Exception.class);
        }
    }

    @Nested
    class GetorelseTests {
        @Test
        public void shouldReturnElseWhenOrElseOnFailure() {
            assertThat(failure().getOrElse(OK)).isEqualTo(OK);
        }

        @Test
        public void shouldReturnSuppliedElseWhenOrElseOnFailure() {
            assertThat(failure().getOrElse(() -> OK)).isEqualTo(OK);
        }

        @Test
        public void shouldNotInvokeSupplierOnSuccess() {
            assertThat(success().getOrElse(() -> {
                        throw new AssertionError("must not be invoked");
                    }))
                    .isEqualTo(OK);
        }

        @Test
        public void shouldThrowOnNullSupplier() {
            Supplier<String> supplier = null;
            assertThrows(
                    NullPointerException.class, () -> TryTest.<String>failure().getOrElse(supplier));
        }
    }

    @Nested
    class GetornullTests {
        @Test
        public void shouldReturnNullWhenGetOrNullOnFailure() {
            assertThat(failure().getOrNull()).isNull();
        }

        @Test
        public void shouldReturnValueWhenGetOrNullOnSuccess() {
            assertThat(success().getOrNull()).isEqualTo(OK);
        }
    }

    @Nested
    class GetorelseFunctionTests {
        @Test
        public void shouldReturnValueComputedFromCauseOnFailure() {
            assertThat(failure().getOrElse(x -> OK)).isEqualTo(OK);
        }
    }

    @Nested
    class GetorelsethrowTests {
        @Test
        public void shouldThrowOtherWhenGetOrElseThrowOnFailure() {
            assertThrows(
                    IllegalStateException.class, () -> failure().getOrElseThrow(x -> new IllegalStateException(OK)));
        }

        @Test
        public void shouldThrowSuppliedWhenGetOrElseThrowOnFailure() {
            assertThrows(
                    IllegalStateException.class, () -> failure().getOrElseThrow(() -> new IllegalStateException(OK)));
        }

        @Test
        public void shouldReturnValueWhenGetOrElseThrowOnSuccess() {
            assertThat(success().getOrElseThrow(() -> new IllegalStateException(OK)))
                    .isEqualTo(OK);
        }

        @Test
        public void shouldThrowOnNullArguments() {
            assertThrows(NullPointerException.class, () -> failure().getOrElseThrow((Supplier<RuntimeException>) null));
            assertThrows(
                    NullPointerException.class,
                    () -> failure().getOrElseThrow((Function<Throwable, RuntimeException>) null));
        }
    }

    @Nested
    class CatchSomeTests {
        @Test
        public void shouldRecoverWhenFailureMatchesExactly() {
            Try<String> testee = failure(RuntimeException.class);
            assertThat(testee.catchSome(RuntimeException.class, x -> OK).isSuccess())
                    .isTrue();
        }

        @Test
        public void shouldRecoverWhenFailureIsAssignableFrom() {
            Try<String> testee = failure(UnsupportedOperationException.class);
            assertThat(testee.catchSome(RuntimeException.class, x -> OK).isSuccess())
                    .isTrue();
        }

        @Test
        public void shouldReturnThisWhenRecoverDifferentTypeOfFailure() {
            Try<String> testee = failure(RuntimeException.class);
            assertThat(testee.catchSome(NullPointerException.class, x -> OK)).isSameAs(testee);
        }

        @Test
        public void shouldReturnThisWhenRecoverSpecificFailureOnSuccess() {
            Try<String> testee = success();
            assertThat(testee.catchSome(RuntimeException.class, x -> OK)).isSameAs(testee);
        }
    }

    @Nested
    class CatchAllTests {
        @Test
        public void shouldRecoverOnFailure() {
            assertThat(failure().catchAll(x -> OK).get()).isEqualTo(OK);
        }

        @Test
        public void shouldReturnThisWhenRecoverOnSuccess() {
            Try<String> testee = success();
            assertThat(testee.catchAll(x -> OK)).isSameAs(testee);
        }
    }

    @Nested
    class CatchAllWithTests {
        @Test
        public void shouldRecoverWithOnFailure() {
            assertThat(TryTest.<String>failure().catchAllWith(x -> success()).get())
                    .isEqualTo(OK);
        }

        @Test
        public void shouldRecoverWithThrowingOnFailure() {
            RuntimeException error = error();
            assertThat(failure().catchAllWith(x -> {
                        throw error;
                    }))
                    .isEqualTo(Try.failure(error));
        }
    }

    @Nested
    class CatchSomeWithTests {
        @Test
        public void shouldNotTryToRecoverWhenItIsNotNeeded() {
            assertThat(Try.of(() -> OK)
                            .catchSomeWith(RuntimeException.class, x -> failure())
                            .get())
                    .isEqualTo(OK);
        }

        @Test
        public void shouldReturnExceptionWhenRecoveryWasNotSuccess() {
            Try<?> testee = Try.of(() -> {
                        throw error();
                    })
                    .catchSomeWith(IOException.class, x -> failure());
            assertThatThrownBy(testee::get).isInstanceOf(RuntimeException.class).hasMessage("error");
        }

        @Test
        public void shouldReturnErrorOfRecoveryWhenRecoveryFails() {
            Error error = new Error();
            Throwable actual = Try.failure(new IOException())
                    .catchSomeWith(IOException.class, x -> {
                        throw error;
                    })
                    .getCause();
            assertThat(actual).isSameAs(error);
        }

        @Test
        public void shouldReturnRecoveredValue() {
            assertThat(Try.of(() -> {
                                throw error();
                            })
                            .catchSomeWith(RuntimeException.class, x -> success())
                            .get())
                    .isEqualTo(OK);
        }

        @Test
        public void shouldHandleErrorDuringRecovering() {
            Try<?> t = Try.of(() -> {
                        throw new IllegalArgumentException(OK);
                    })
                    .catchSomeWith(IOException.class, x -> {
                        throw new IllegalStateException(FAILURE);
                    });
            assertThatThrownBy(t::get).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TapErrorTests {
        @Test
        public void shouldConsumeThrowableWhenCallingTapErrorGivenFailure() {
            String[] result = new String[] {FAILURE};
            failure().tapError(x -> result[0] = OK);
            assertThat(result[0]).isEqualTo(OK);
        }

        @Test
        public void shouldConsumeThrowableWhenCallingTapErrorWithMatchingExceptionTypeGivenFailure() {
            String[] result = new String[] {FAILURE};
            failure().tapError(RuntimeException.class, x -> result[0] = OK);
            assertThat(result[0]).isEqualTo(OK);
        }

        @Test
        public void shouldNotConsumeThrowableWhenCallingTapErrorWithNonMatchingExceptionTypeGivenFailure() {
            String[] result = new String[] {OK};
            failure().tapError(Error.class, x -> result[0] = FAILURE);
            assertThat(result[0]).isEqualTo(OK);
        }
    }

    @Nested
    class TooptionTests {
        @Test
        public void shouldConvertFailureToOption() {
            // the cause is dropped
            assertThat(failure().toOption()).isSameAs(Option.none());
        }

        @Test
        public void shouldConvertSuccessToOption() {
            assertThat(success().toOption()).isEqualTo(Option.some(OK));
        }
    }

    @Nested
    class TovectorTests {
        @Test
        public void shouldConvertFailureToEmptyVector() {
            assertThat(failure().toVector()).isSameAs(dev.zazr.collection.Vector.empty());
        }

        @Test
        public void shouldConvertSuccessToVectorOfOne() {
            assertThat(success().toVector()).isEqualTo(dev.zazr.collection.Vector.of(OK));
        }
    }

    @Nested
    class ToeitherTests {
        @Test
        public void shouldConvertFailureToEitherLeftOfTheCause() {
            RuntimeException cause = error();
            Either<Throwable, Object> either = Try.failure(cause).toEither();
            assertThat(either.isLeft()).isTrue();
            assertThat(either.getLeft()).isSameAs(cause);
        }

        @Test
        public void shouldConvertSuccessToEitherRight() {
            assertThat(success().toEither()).isEqualTo(Either.right(OK));
        }

        @Test
        public void shouldMapTheCauseThroughMapLeft() {
            // toEither(Function) is gone (design 3.2): the mapping composes on the Either
            Either<String, Object> converted = Try.failure(error()).toEither().mapLeft(Throwable::getMessage);
            assertThat(converted).isEqualTo(Either.left("error"));
        }
    }

    @Nested
    class TovalidationTests {
        @Test
        public void shouldConvertFailureToValidation() {
            Try<Object> failure = failure();
            Validation<Throwable, Object> invalid = failure.toValidation();
            assertThat(invalid).isEqualTo(Validation.invalid(failure.getCause()));
            assertThat(invalid.isInvalid()).isTrue();
        }

        @Test
        public void shouldConvertSuccessToValidation() {
            assertThat(success().toValidation()).isEqualTo(Validation.valid(OK));
        }

        @Test
        public void shouldMapTheCauseThroughMapError() {
            // toValidation(Function) is gone (design 3.2): the mapping composes on the Validation
            Validation<String, Object> validation =
                    Try.failure(error()).toValidation().mapError(Throwable::getMessage);
            assertThat(validation).isEqualTo(Validation.invalid("error"));
        }
    }

    @Nested
    class TocompletablefutureTests {
        @Test
        public void shouldConvertSuccessToCompletableFuture() {
            CompletableFuture<String> future = success().toCompletableFuture();
            assertThat(future.isDone());
            assertThat(Try.of(future::get).get()).isEqualTo(success().get());
        }

        @Test
        public void shouldConvertFailureToFailedCompletableFuture() {
            CompletableFuture<Object> future = failure().toCompletableFuture();
            assertThat(future.isDone());
            assertThat(future.isCompletedExceptionally());
            assertThatThrownBy(future::get)
                    .isExactlyInstanceOf(ExecutionException.class)
                    .hasCauseExactlyInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    class FromcompletablefutureTests {
        @Test
        public void shouldConvertCompletedFutureToSuccess() {
            CompletableFuture<String> future = CompletableFuture.completedFuture("ok");
            Try<String> result = Try.fromCompletableFuture(future);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEqualTo("ok");
        }

        @Test
        public void shouldConvertExceptionallyCompletedFutureToFailureWithUnwrappedCause() {
            RuntimeException cause = new RuntimeException("boom");
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(cause);
            Try<String> result = Try.fromCompletableFuture(future);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(cause);
        }

        @Test
        public void shouldConvertCancelledFutureToFailureWithCancellationException() {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.cancel(true);
            Try<String> result = Try.fromCompletableFuture(future);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(java.util.concurrent.CancellationException.class);
        }

        @Test
        public void shouldRethrowFatalCauseFromExceptionallyCompletedFuture() {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new InterruptedException());
            assertThatThrownBy(() -> Try.fromCompletableFuture(future)).isInstanceOf(InterruptedException.class);
        }

        @Test
        public void shouldBlockUntilPendingFutureIsCompletedFromAnotherThread() throws InterruptedException {
            CompletableFuture<String> future = new CompletableFuture<>();
            long delayMillis = 200;
            Thread completer = new Thread(() -> {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                future.complete("done");
            });
            completer.start();
            long startNanos = System.nanoTime();
            Try<String> result = Try.fromCompletableFuture(future);
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            completer.join();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEqualTo("done");
            assertThat(elapsedMillis).isGreaterThanOrEqualTo(delayMillis);
        }

        @Test
        public void shouldCaptureFutureCompletedWithNullAsFailure() {
            CompletableFuture<String> future = CompletableFuture.completedFuture(null);
            Try<String> result = Try.fromCompletableFuture(future);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(result.getCause().getMessage())
                    .isEqualTo("Try.fromCompletableFuture: the computation returned null");
        }

        @Test
        public void shouldRoundTripSuccessThroughToCompletableFuture() {
            Try<String> t = Try.success("ok");
            Try<String> result = Try.fromCompletableFuture(t.toCompletableFuture());
            assertThat(result).isEqualTo(t);
        }

        @Test
        public void shouldRoundTripFailureThroughToCompletableFutureWithSameCauseInstance() {
            RuntimeException cause = new RuntimeException("boom");
            Try<String> t = Try.<String>failure(cause);
            Try<String> result = Try.fromCompletableFuture(t.toCompletableFuture());
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isSameAs(cause);
        }
    }

    // -- filter

    @Test
    public void shouldFilterMatchingPredicateOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.filter(s -> true)).isEqualTo(actual);
    }

    @Test
    public void shouldFilterNonMatchingPredicateOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.filter(s -> false)).isEqualTo(actual);
    }

    @Test
    public void shouldFilterWithExceptionOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.filter(this::filter)).isEqualTo(actual);
    }

    @Test
    public void shouldReturnIdentityWhenFilterOnFailure() {
        Try<String> identity = failure();
        assertThat(identity.filter(s -> true)).isEqualTo(identity);
    }

    @Test
    public void shouldReturnIdentityWhenFilterWithErrorProviderOnFailure() {
        Try<String> identity = failure();
        assertThat(identity.filter(s -> false, ignored -> new IllegalArgumentException()))
                .isEqualTo(identity);
    }

    // -- flatMap

    @Test
    public void shouldFlatMapOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.flatMap(s -> Try.of(() -> s + "!"))).isEqualTo(actual);
    }

    @Test
    public void shouldFlatMapWithExceptionOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.flatMap(this::flatMap)).isEqualTo(actual);
    }

    @Nested
    class IsemptyTests {
        @Test
        public void shouldBeEmptyOnFailure() {
            assertThat(failure().isEmpty()).isTrue();
        }

        @Test
        public void shouldNotBeEmptyOnSuccess() {
            assertThat(success().isEmpty()).isFalse();
        }
    }

    @Nested
    class ForEachConsumerTests {
        @Test
        public void shouldForEachOnFailure() {
            List<String> actual = new ArrayList<>();
            TryTest.<String>failure().forEach(actual::add);
            assertThat(actual.isEmpty()).isTrue();
        }

        @Test
        public void shouldPropagateWhatTheActionThrows() {
            assertThrows(
                    IllegalStateException.class,
                    () -> success().forEach(s -> {
                        throw new IllegalStateException(s);
                    }));
        }

        @Test
        public void shouldThrowOnNullAction() {
            assertThrows(NullPointerException.class, () -> failure().forEach(null));
        }
    }

    // -- map

    @Test
    public void shouldMapOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.map(s -> s + "!")).isEqualTo(actual);
    }

    @Test
    public void shouldMapWithExceptionOnFailure() {
        Try<String> actual = failure();
        assertThat(actual.map(this::map)).isEqualTo(actual);
    }

    @Test
    public void shouldChainSuccessWithMap() {
        Try<Integer> actual = Try.of(() -> 100).map(x -> x + 100).map(x -> x + 50);

        Try<Integer> expected = Try.success(250);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldChainFailureWithMap() {
        Try<Integer> actual = Try.of(() -> 100)
                .map(x -> x + 100)
                .map(x -> Integer.parseInt("aaa") + x) // Throws exception.
                .map(x -> x / 2);
        assertThat(actual.toString()).isEqualTo("Failure(java.lang.NumberFormatException: For input string: \"aaa\")");
    }

    @Nested
    class AndthenTests {
        @Test
        public void shouldComposeFailureWithAndThenWhenFailing() {
            Try<Tuple0> actual = Try.run(() -> {
                        throw new Error("err1");
                    })
                    .andThen(() -> {
                        throw new Error("err2");
                    });
            assertThat(actual.toString()).isEqualTo("Failure(java.lang.Error: err1)");
        }

        @Test
        public void shouldChainConsumableSuccessWithAndThen() {
            Try<Integer> actual = Try.of(() -> new ArrayList<Integer>())
                    .andThenTry(arr -> arr.add(10))
                    .andThenTry(arr -> arr.add(30))
                    .andThenTry(arr -> arr.add(20))
                    .map(arr -> arr.get(1));

            Try<Integer> expected = Try.success(30);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldChainConsumableFailureWithAndThen() {
            Try<Integer> actual = Try.of(() -> new ArrayList<Integer>())
                    .andThenTry(arr -> arr.add(10))
                    .andThenTry(arr -> arr.add(Integer.parseInt("aaa"))) // Throws exception.
                    .andThenTry(arr -> arr.add(20))
                    .map(arr -> arr.get(1));
            assertThat(actual.toString())
                    .isEqualTo("Failure(java.lang.NumberFormatException: For input string: \"aaa\")");
        }

        // peek

        @Test
        public void shouldTapFailure() {
            List<Object> list = new ArrayList<>();
            Try<Object> failure = failure();
            assertThat(failure.tap(list::add)).isSameAs(failure);
            assertThat(list.isEmpty()).isTrue();
        }

        // equals

        @Test
        public void shouldEqualFailureIfObjectIsSame() {
            Try<?> failure = Try.failure(error());
            assertThat(failure).isEqualTo(failure);
        }

        @Test
        public void shouldNotEqualFailureIfObjectIsNull() {
            assertThat(Try.failure(error())).isNotNull();
        }

        @Test
        public void shouldNotEqualFailureIfObjectIsOfDifferentType() {
            assertThat(Try.failure(error()).equals(new Object())).isFalse();
        }

        @Test
        public void shouldEqualFailureWhenCauseIsTheSameObject() {
            Throwable error = error();
            assertThat(Try.failure(error)).isEqualTo(Try.failure(error));
        }

        @Test
        public void shouldNotEqualFailureWhenCausesAreDifferentObjects() {
            // same class, same message, still two exceptions: Failure equality is reference equality on the cause
            assertThat(Try.failure(error())).isNotEqualTo(Try.failure(error()));
        }

        @Test
        public void shouldNotEqualFailureWithDifferentExceptionType() {
            var npe = new NullPointerException("msg");
            var iae = new IllegalArgumentException("msg");
            npe.setStackTrace(iae.getStackTrace());
            assertThat(Try.failure(npe)).isNotEqualTo(Try.failure(iae));
        }

        @Test
        public void shouldNotEqualFailureWithDifferentMessage() {
            assertThat(Try.failure(new RuntimeException("a"))).isNotEqualTo(Try.failure(new RuntimeException("b")));
        }

        // hashCode

        @Test
        public void shouldHashFailure() {
            Throwable error = error();
            assertThat(Try.failure(error).hashCode())
                    .isEqualTo(Try.failure(error).hashCode());
        }

        // toString

        @Test
        public void shouldConvertFailureToString() {
            assertThat(Try.failure(error()).toString()).isEqualTo("Failure(java.lang.RuntimeException: error)");
        }
    }

    @Nested
    class CollectAllTests {
        @Test
        public void shouldConvertListOfSuccessToTryOfList() {
            List<Try<String>> tries = Arrays.asList(Try.success("a"), Try.success("b"), Try.success("c"));
            Try<Vector<String>> reducedTry = Try.collectAll(tries);
            assertThat(reducedTry instanceof Try.Success).isTrue();
            assertThat(reducedTry.get().size()).isEqualTo(3);
            assertThat(reducedTry.get().mkString()).isEqualTo("abc");
        }

        @Test
        public void shouldConvertListOfFailureToTryOfList() {
            Throwable t = new RuntimeException("failure");
            List<Try<String>> tries = Arrays.asList(Try.failure(t), Try.failure(t), Try.failure(t));
            Try<Vector<String>> reducedTry = Try.collectAll(tries);
            assertThat(reducedTry instanceof Try.Failure).isTrue();
        }

        @Test
        public void shouldConvertListOfMixedTryToTryOfList() {
            Throwable t = new RuntimeException("failure");
            List<Try<String>> tries = Arrays.asList(Try.success("a"), Try.failure(t), Try.success("c"));
            Try<Vector<String>> reducedTry = Try.collectAll(tries);
            assertThat(reducedTry instanceof Try.Failure).isTrue();
        }
    }

    @Nested
    class ForEachIterableTests {
        @Test
        public void shouldForEachListOfSuccessToTryOfList() {
            List<String> tries = Arrays.asList("a", "b", "c");
            Try<Vector<String>> reducedTry = Try.forEach(tries, Try::success);
            assertThat(reducedTry instanceof Try.Success).isTrue();
            assertThat(reducedTry.get().size()).isEqualTo(3);
            assertThat(reducedTry.get().mkString()).isEqualTo("abc");
        }

        @Test
        public void shouldForEachListOfFailureToTryOfList() {
            Throwable t = new RuntimeException("failure");
            List<Throwable> tries = Arrays.asList(t, t, t);
            Try<Vector<String>> reducedTry = Try.forEach(tries, Try::failure);
            assertThat(reducedTry instanceof Try.Failure).isTrue();
        }

        @Test
        public void shouldForEachListOfMixedTryToTryOfList() {
            Throwable t = new RuntimeException("failure");
            List<String> tries = Arrays.asList("a", "b", "c");
            Try<Vector<String>> reducedTry = Try.forEach(tries, x -> x.equals("b") ? Try.failure(t) : Try.success(x));
            assertThat(reducedTry instanceof Try.Failure).isTrue();
        }
    }

    @Nested
    class SuccessTests {
        @Test
        public void shouldDetectSuccessOfRunnable() {
            //noinspection ResultOfMethodCallIgnored
            assertThat(Try.run(() -> String.valueOf("side-effect")).isSuccess()).isTrue();
        }

        @Test
        public void shouldDetectSuccess() {
            assertThat(success().isSuccess()).isTrue();
        }

        @Test
        public void shouldDetectNonFailureOnSuccess() {
            assertThat(success().isFailure()).isFalse();
        }

        @Test
        public void shouldGetOnSuccess() {
            assertThat(success().get()).isEqualTo(OK);
        }

        @Test
        public void shouldGetOrElseOnSuccess() {
            assertThat(success().getOrElse((String) null)).isEqualTo(OK);
        }

        @Test
        public void shouldGetOrElseFunctionOnSuccess() {
            assertThat(success().getOrElse(x -> null)).isEqualTo(OK);
        }

        @Test
        public void shouldOrElseThrowOnSuccess() {
            assertThat(success().getOrElseThrow(x -> null)).isEqualTo(OK);
        }

        @Test
        public void shouldCatchAllOnSuccess() {
            assertThat(success().catchAll(x -> null).get()).isEqualTo(OK);
        }

        @Test
        public void shouldCatchAllWithOnSuccess() {
            assertThat(success().catchAllWith(x -> null).get()).isEqualTo(OK);
        }

        @Test
        public void shouldNotConsumeThrowableWhenCallingTapErrorGivenSuccess() {
            String[] result = new String[] {OK};
            success().tapError(x -> result[0] = FAILURE);
            assertThat(result[0]).isEqualTo(OK);
        }

        @Test
        public void shouldConvertSuccessToOption() {
            assertThat(success().toOption().get()).isEqualTo(OK);
        }

        @Test
        public void shouldConvertSuccessToEither() {
            assertThat(success().toEither().isRight()).isTrue();
        }

        @Test
        public void shouldConvertSuccessToValidValidation() {
            assertThat(success().toValidation().isValid()).isTrue();
        }

        @Test
        public void shouldFilterMatchingPredicateOnSuccess() {
            assertThat(success().filter(s -> true).get()).isEqualTo(OK);
        }

        @Test
        public void shouldFilterMatchingPredicateWithErrorProviderOnSuccess() {
            assertThat(success()
                            .filter(s -> true, s -> new IllegalArgumentException(s))
                            .get())
                    .isEqualTo(OK);
        }

        @Test
        public void shouldFilterNonMatchingPredicateOnSuccess() {
            Try<?> testee = success().filter(s -> false);
            assertThatThrownBy(testee::get).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        public void shouldFilterNonMatchingPredicateAndDefaultThrowableSupplierOnSuccess() {
            assertThat(success().filter(s -> false).getCause()).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        public void shouldUseErrorProviderWhenFilterNonMatchingPredicateOnSuccess() throws Exception {
            assertThat(success()
                            .filter(s -> false, str -> new IllegalArgumentException(str))
                            .getCause())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        public void shouldFilterWithExceptionOnSuccess() {
            assertThrows(RuntimeException.class, () -> {
                success()
                        .filter(s -> {
                            throw new RuntimeException("xxx");
                        })
                        .get();
            });
        }

        @Test
        public void shouldFlatMapOnSuccess() {
            assertThat(success().flatMap(s -> Try.of(() -> s + "!")).get()).isEqualTo(OK + "!");
        }

        @Test
        public void shouldFlatMapOnIterable() {
            Try<Integer> success = Try.success(1);
            assertThat(success().flatMap(ignored -> success)).isEqualTo(success);
        }

        @Test
        public void shouldFlatMapOnEmptyIterable() {
            Try<Integer> failure = Try.failure(new Error());
            assertThat(success().flatMap(ignored -> failure)).isEqualTo(failure);
        }

        @Test
        public void shouldFlatMapWithExceptionOnSuccess() {
            assertThrows(RuntimeException.class, () -> {
                success()
                        .flatMap(s -> {
                            throw new RuntimeException("xxx");
                        })
                        .get();
            });
        }

        @Test
        public void shouldForEachOnSuccess() {
            List<String> actual = new ArrayList<>();
            success().forEach(actual::add);
            assertThat(actual).isEqualTo(Collections.singletonList(OK));
        }

        @Test
        public void shouldMapOnSuccess() {
            assertThat(success().map(s -> s + "!").get()).isEqualTo(OK + "!");
        }

        @Test
        public void shouldMapWithExceptionOnSuccess() {
            Try<?> testee = success().map(s -> {
                throw new RuntimeException("xxx");
            });
            assertThatThrownBy(testee::get).isInstanceOf(RuntimeException.class).hasMessage("xxx");
        }

        @Test
        public void shouldThrowWhenCallingGetCauseOnSuccess() {
            assertThrows(UnsupportedOperationException.class, () -> success().getCause());
        }

        @Test
        public void shouldComposeSuccessWithAndThenWhenFailing() {
            Try<Tuple0> actual = Try.run(() -> {}).andThen(() -> {
                throw new Error("failure");
            });
            assertThat(actual.toString()).isEqualTo("Failure(java.lang.Error: failure)");
        }

        @Test
        public void shouldComposeSuccessWithAndThenWhenSucceeding() {
            Try<Tuple0> actual = Try.run(() -> {}).andThen(() -> {});
            Try<Tuple0> expected = Try.success(Tuple.empty());
            assertThat(actual).isEqualTo(expected);
        }

        // peek

        @Test
        public void shouldTapSuccess() {
            List<Object> list = new ArrayList<>();
            assertThat(success().tap(list::add)).isEqualTo(success());
            assertThat(list.isEmpty()).isFalse();
        }

        @Test
        public void shouldTapSuccessAndThrow() {
            assertThrows(
                    RuntimeException.class, () -> success().tap(t -> failure().get()));
        }

        @Test
        public void shouldThrowOnNullTapAction() {
            assertThrows(NullPointerException.class, () -> success().tap(null));
        }

        // equals

        @Test
        public void shouldEqualSuccessIfObjectIsSame() {
            Try<?> success = Try.success(1);
            assertThat(success).isEqualTo(success);
        }

        @Test
        public void shouldNotEqualSuccessIfObjectIsNull() {
            assertThat(Try.success(1)).isNotNull();
        }

        @Test
        public void shouldNotEqualSuccessIfObjectIsOfDifferentType() {
            assertThat(Try.success(1).equals(new Object())).isFalse();
        }

        @Test
        public void shouldEqualSuccess() {
            assertThat(Try.success(1)).isEqualTo(Try.success(1));
        }

        // hashCode

        @Test
        public void shouldHashSuccess() {
            assertThat(Try.success(1).hashCode()).isEqualTo(Objects.hashCode(1));
        }

        // toString

        @Test
        public void shouldConvertSuccessToString() {
            assertThat(Try.success(1).toString()).isEqualTo("Success(1)");
        }
    }

    @Nested
    class CheckedFunctionsTests {
        @Test
        public void shouldCreateIdentityCheckedFunction() {
            assertThat(Function.identity()).isNotNull();
        }

        @Test
        public void shouldEnsureThatIdentityCheckedFunctionReturnsIdentity() {
            assertThat(Function.identity().apply(1)).isEqualTo(1);
        }

        @Test
        public void shouldNegateCheckedPredicate() {
            CheckedPredicate<Integer> greaterThanZero = i -> i > 0;
            int num = 1;
            try {
                assertThat(greaterThanZero.test(num)).isTrue();
                assertThat(greaterThanZero.negate().test(-num)).isTrue();
            } catch (Throwable x) {
                Assertions.fail("should not throw");
            }
        }
    }

    // -- helpers

    private RuntimeException error() {
        return new RuntimeException("error");
    }

    @Nested
    class NullResultTests {

        // A Success cannot hold null, so a computation that returns null is a captured outcome, not a caller error.

        @Test
        public void shouldCaptureNullResultOfOfAsFailure() {
            Try<Object> result = Try.of(() -> null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(result.getCause().getMessage()).isEqualTo("Try.of: the computation returned null");
        }

        @Test
        public void shouldCaptureNullResultOfMapTryAsFailure() {
            Try<Object> result = Try.success(1).mapTry(i -> null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(result.getCause().getMessage()).isEqualTo("Try.mapTry: the computation returned null");
        }

        @Test
        public void shouldCaptureNullResultOfMapAsFailure() {
            assertThat(Try.success(1).map(i -> null).getCause()).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldCaptureNullResultOfCatchAllAsFailure() {
            assertThat(TryTest.<String>failure().catchAll(x -> null).getCause())
                    .isInstanceOf(NullPointerException.class);
            assertThat(TryTest.<String>failure()
                            .catchSome(RuntimeException.class, x -> null)
                            .getCause())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldReturnFallbackFromGetOrElseOnNullResult() {
            assertThat(Try.<String>of(() -> null).getOrElse("fallback")).isEqualTo("fallback");
            assertThat(Try.success(1).<String>mapTry(i -> null).getOrElse("fallback"))
                    .isEqualTo("fallback");
        }

        @Test
        public void shouldStillRejectNullInSuccessFactory() {
            assertThatThrownBy(() -> Try.success(null)).isInstanceOf(NullPointerException.class);
        }
    }

    private static <T> Try<T> failure() {
        return Try.failure(new RuntimeException());
    }

    private static <T, X extends Throwable> Try<T> failure(Class<X> exceptionType) {
        try {
            X exception = exceptionType.getConstructor().newInstance();
            return Try.failure(exception);
        } catch (Throwable e) {
            throw new IllegalStateException("Error instantiating " + exceptionType, e);
        }
    }

    private <T> boolean filter(T t) {
        throw new RuntimeException("xxx");
    }

    private <T> Try<T> flatMap(T t) {
        throw new RuntimeException("xxx");
    }

    private <T> T map(T t) {
        throw new RuntimeException("xxx");
    }

    private Try<String> success() {
        return Try.of(() -> "ok");
    }

    @Nested
    class CollectTests {
        @Test
        public void shouldCollectSuccessToSuccess() {
            assertThat(Try.success(2).collect(i -> Option.some(i * 10))).isEqualTo(Try.success(20));
        }

        @Test
        public void shouldFailWithNoSuchElementWhenTheMapperReturnsNone() {
            Try<Integer> actual = Try.success(2).collect(i -> Option.none());
            assertThat(actual.isFailure()).isTrue();
            assertThat(actual.getCause())
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Predicate does not hold for 2");
        }

        @Test
        public void shouldReturnThisFailureWithoutCallingTheMapper() {
            Try<Integer> failure = failure();
            assertThat(failure.collect(i -> {
                        throw new AssertionError("must not be called");
                    }))
                    .isSameAs(failure);
        }

        @Test
        public void shouldCaptureWhatTheMapperThrows() {
            RuntimeException thrown = error();
            assertThat(Try.success(1)
                            .collect(i -> {
                                throw thrown;
                            })
                            .getCause())
                    .isSameAs(thrown);
        }

        @Test
        public void shouldCaptureANullOptionFromTheMapperAsFailure() {
            Try<Object> actual = Try.success(1).collect(i -> null);
            assertThat(actual.getCause())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Try.collect: mapper returned null");
        }

        @Test
        public void shouldCollectWithASwitchInsideTheLambda() {
            Try<Object> shape = Try.success("circle");
            Try<Integer> actual = shape.collect(s -> switch (s) {
                case String str -> Option.some(str.length());
                default -> Option.none();
            });
            assertThat(actual).isEqualTo(Try.success(6));
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> success().collect(null));
        }
    }

    @Nested
    class MapErrorTests {
        @Test
        public void shouldReturnThisOnSuccess() {
            Try<String> success = success();
            assertThat(success.mapError(e -> {
                        throw new AssertionError("must not be called");
                    }))
                    .isSameAs(success);
        }

        @Test
        public void shouldReplaceTheCauseOnFailure() {
            IOException cause = new IOException("io");
            Try<Object> actual = Try.failure(cause).mapError(e -> new IllegalStateException("wrapped", e));
            assertThat(actual.isFailure()).isTrue();
            assertThat(actual.getCause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("wrapped");
            assertThat(actual.getCause().getCause()).isSameAs(cause);
        }

        @Test
        public void shouldCaptureWhatTheMapperThrows() {
            RuntimeException thrown = error();
            assertThat(failure()
                            .mapError(e -> {
                                throw thrown;
                            })
                            .getCause())
                    .isSameAs(thrown);
        }

        @Test
        public void shouldFailWithNullPointerExceptionWhenTheMapperReturnsNull() {
            assertThat(failure().mapError(e -> null).getCause()).isInstanceOf(NullPointerException.class);
        }

        @Test
        public void shouldRethrowAFatalMappedCause() {
            assertThrows(InterruptedException.class, () -> failure().mapError(e -> new InterruptedException()));
        }

        @Test
        public void shouldThrowOnNullMapper() {
            assertThrows(NullPointerException.class, () -> failure().mapError(null));
        }
    }

    @Nested
    class CatchFamilyEdgeTests {
        @Test
        public void shouldCaptureWhatCatchSomeThrows() {
            RuntimeException thrown = error();
            assertThat(failure()
                            .catchSome(RuntimeException.class, x -> {
                                throw thrown;
                            })
                            .getCause())
                    .isSameAs(thrown);
        }

        @Test
        public void shouldFailWithNullPointerExceptionWhenCatchAllWithReturnsNull() {
            assertThat(failure().catchAllWith(x -> null).getCause())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Try.catchAllWith: f returned null");
        }

        @Test
        public void shouldFailWithNullPointerExceptionWhenCatchSomeWithReturnsNull() {
            assertThat(failure()
                            .catchSomeWith(RuntimeException.class, x -> null)
                            .getCause())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Try.catchSomeWith: f returned null");
        }

        @Test
        public void shouldReturnThisWhenCatchSomeWithDoesNotMatch() {
            Try<String> failure = failure();
            assertThat(failure.catchSomeWith(IOException.class, x -> success())).isSameAs(failure);
        }

        @Test
        public void shouldReturnThisWhenCatchSomeWithOnSuccess() {
            Try<String> success = success();
            assertThat(success.catchSomeWith(RuntimeException.class, x -> failure()))
                    .isSameAs(success);
        }

        @Test
        public void shouldReturnTheTryTheRecoveryReturns() {
            Try<String> other = failure();
            assertThat(TryTest.<String>failure().catchAllWith(x -> other)).isSameAs(other);
            assertThat(TryTest.<String>failure().catchSomeWith(RuntimeException.class, x -> other))
                    .isSameAs(other);
        }

        @Test
        public void shouldThrowOnNullArguments() {
            assertThrows(NullPointerException.class, () -> failure().catchAll(null));
            assertThrows(NullPointerException.class, () -> failure().catchSome(null, x -> OK));
            assertThrows(NullPointerException.class, () -> failure().catchSome(RuntimeException.class, null));
            assertThrows(NullPointerException.class, () -> failure().catchAllWith(null));
            assertThrows(NullPointerException.class, () -> failure().catchSomeWith(null, x -> success()));
            assertThrows(NullPointerException.class, () -> failure().catchSomeWith(RuntimeException.class, null));
        }
    }

    @Nested
    class TapErrorOnSuccessTests {
        @Test
        public void shouldNotConsumeAnythingWhenCallingTapErrorWithExceptionTypeGivenSuccess() {
            String[] result = new String[] {OK};
            Try<String> success = success();
            assertThat(success.tapError(RuntimeException.class, x -> result[0] = FAILURE))
                    .isSameAs(success);
            assertThat(result[0]).isEqualTo(OK);
        }

        @Test
        public void shouldReturnThisOnTapErrorOfFailure() {
            Try<String> failure = failure();
            assertThat(failure.tapError(x -> {})).isSameAs(failure);
            assertThat(failure.tapError(RuntimeException.class, x -> {})).isSameAs(failure);
        }

        @Test
        public void shouldThrowOnNullArguments() {
            assertThrows(NullPointerException.class, () -> failure().tapError(null));
            assertThrows(NullPointerException.class, () -> failure().tapError(null, x -> {}));
            assertThrows(NullPointerException.class, () -> failure().tapError(RuntimeException.class, null));
        }
    }

    @Nested
    class ZipTests {

        @Test
        public void shouldPairTwoSuccesses() {
            assertThat(Try.success(1).zip(Try.success("a"))).isEqualTo(Try.success(Tuple.of(1, "a")));
        }

        @Test
        public void shouldReturnTheFirstFailureAsIs() {
            Try<Integer> failure = Try.failure(new IllegalStateException("a"));
            Try<String> otherFailure = Try.failure(new IllegalStateException("b"));
            assertThat(failure.zip(Try.success("x"))).isSameAs(failure);
            assertThat(Try.success(1).zip(otherFailure)).isSameAs(otherFailure);
            assertThat(failure.zip(otherFailure)).isSameAs(failure);
        }

        @Test
        public void shouldCombineWithZipWith() {
            AtomicInteger calls = new AtomicInteger();
            assertThat(Try.success(1).zipWith(Try.success(2), (a, b) -> {
                        calls.incrementAndGet();
                        return a + b;
                    }))
                    .isEqualTo(Try.success(3));
            assertThat(calls.get()).isEqualTo(1);
        }

        @Test
        public void shouldNotCallTheCombinerUnlessBothAreSuccess() {
            BiFunction<Integer, Integer, Integer> notCalled = (_, _) -> {
                throw new AssertionError("must not be called");
            };
            Try<Integer> failure = Try.failure(new IllegalStateException("a"));
            Try<Integer> otherFailure = Try.failure(new IllegalStateException("b"));
            assertThat(failure.zipWith(Try.success(2), notCalled)).isSameAs(failure);
            assertThat(Try.success(1).zipWith(failure, notCalled)).isSameAs(failure);
            assertThat(failure.zipWith(otherFailure, notCalled)).isSameAs(failure);
        }

        @Test
        public void shouldCaptureWhatTheCombinerThrows() {
            RuntimeException boom = new IllegalStateException("boom");
            assertThat(Try.success(1).zipWith(Try.success(2), (_, _) -> {
                        throw boom;
                    }))
                    .isEqualTo(Try.failure(boom));
        }

        @Test
        public void shouldRethrowAFatalCombinerError() {
            UnknownError fatal = new UnknownError("fatal");
            assertThatThrownBy(() -> Try.success(1).zipWith(Try.success(2), (_, _) -> {
                        throw fatal;
                    }))
                    .isSameAs(fatal);
        }

        @Test
        public void shouldCaptureANullCombinerResultAsAFailure() {
            Try<Object> actual = Try.success(1).zipWith(Try.success(2), (_, _) -> null);
            assertThat(actual.isFailure()).isTrue();
            assertThat(actual.getCause())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Try.zipWith: f returned null");
        }

        @Test
        public void shouldKeepTheLeftValueWithZipLeft() {
            Try<Integer> success = Try.success(1);
            Try<Integer> failure = Try.failure(new IllegalStateException("a"));
            Try<String> otherFailure = Try.failure(new IllegalStateException("b"));
            assertThat(success.zipLeft(Try.success("x"))).isSameAs(success);
            assertThat(success.zipLeft(otherFailure)).isSameAs(otherFailure);
            assertThat(failure.zipLeft(Try.success("x"))).isSameAs(failure);
            assertThat(failure.zipLeft(otherFailure)).isSameAs(failure);
        }

        @Test
        public void shouldKeepTheRightValueWithZipRight() {
            Try<String> success = Try.success("x");
            Try<Integer> failure = Try.failure(new IllegalStateException("a"));
            Try<String> otherFailure = Try.failure(new IllegalStateException("b"));
            assertThat(Try.success(1).zipRight(success)).isSameAs(success);
            assertThat(Try.success(1).zipRight(otherFailure)).isSameAs(otherFailure);
            assertThat(failure.zipRight(success)).isSameAs(failure);
            assertThat(failure.zipRight(otherFailure)).isSameAs(failure);
        }

        @Test
        public void shouldRejectNulls() {
            Try<Integer> success = Try.success(1);
            assertThatThrownBy(() -> success.zip(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("that is null");
            assertThatThrownBy(() -> success.zipWith(null, Integer::sum))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("that is null");
            assertThatThrownBy(() -> success.zipWith(Try.success(2), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("f is null");
            assertThatThrownBy(() -> success.zipLeft(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("that is null");
            assertThatThrownBy(() -> success.zipRight(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("that is null");
        }
    }

    // -- flatten

    @Nested
    class FlattenTests {

        @Test
        public void shouldFlattenEveryCombination() {
            Try<Integer> success = Try.success(1);
            Try<Integer> innerFailure = Try.failure(new IllegalStateException("inner"));
            Try<Try<Integer>> outerFailure = Try.failure(new IllegalArgumentException("outer"));
            assertThat(Try.flatten(Try.success(success))).isSameAs(success);
            assertThat(Try.flatten(Try.success(innerFailure))).isSameAs(innerFailure);
            assertThat(Try.flatten(outerFailure)).isSameAs(outerFailure);
            assertThat(Try.flatten(outerFailure).getCause())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("outer");
        }

        @Test
        public void shouldRemoveOneLevelOnly() {
            Try<Try<Integer>> twice = Try.success(Try.success(1));
            assertThat(Try.flatten(Try.success(twice))).isSameAs(twice);
        }

        @Test
        public void shouldWidenTheValueType() {
            Try<Number> number = Try.flatten(Try.success(Try.success(1)));
            assertThat(number).isEqualTo(Try.success(1));
        }

        @Test
        public void shouldRejectANullTry() {
            assertThatThrownBy(() -> Try.flatten(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("nested is null");
        }
    }
}
