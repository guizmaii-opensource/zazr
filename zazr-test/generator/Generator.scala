// temporarily needed to circumvent https://issues.scala-lang.org/browse/SI-3772 (see case class Generics)
import Generator._
import JavaGenerator._

import scala.language.implicitConversions

val N = 8
val TARGET_MAIN = s"${project.getBasedir()}/src-gen/main/java"
val TARGET_TEST = s"${project.getBasedir()}/src-gen/test/java"
val CHARSET = java.nio.charset.StandardCharsets.UTF_8

/**
 * ENTRY POINT
 */
def run(): Unit = {
  generateMainClasses()
  generateTestClasses()
  deleteStaleFiles(s"${project.getBasedir()}/src-gen")
}

/**
 * Generate Vavr src-gen/main/java classes
 */
def generateMainClasses(): Unit = {

  // Workaround: Use /$** instead of /** in a StringContext when IntelliJ IDEA otherwise shows up errors in the editor
  val javadoc = "**"

  genPropertyChecks()

  /**
   * Generator of com.guizmaii.zazr.test.legacy.Property
   */
  def genPropertyChecks(): Unit = {

    genVavrFile("com.guizmaii.zazr.test.legacy", "Property")(genProperty)

    def genProperty(im: ImportManager, packageName: String, className: String): String = xs"""
      /**
       * A property builder which provides a fluent API to build checkable properties.
       */
      public class $className {

          private final String name;

          private $className(String name) {
              this.name = name;
          }

          /**
           * Defines a new Property.
           *
           * @param name property name
           * @return a new {@code Property}
           * @throws NullPointerException if name is null.
           * @throws IllegalArgumentException if name is empty or consists of whitespace only
           */
          public static Property named(String name) {
              ${im.getType("java.util.Objects")}.requireNonNull(name, "name is null");
              if (name.trim().isEmpty()) {
                  throw new IllegalArgumentException("name is empty");
              }
              return new Property(name);
          }

          private static void logSatisfied(String name, int tries, long millis, boolean exhausted) {
              if (exhausted) {
                  log(String.format("%s: Exhausted after %s tests in %s ms.", name, tries, millis));
              } else {
                  log(String.format("%s: OK, passed %s tests in %s ms.", name, tries, millis));
              }
          }

          private static void logFalsified(String name, int currentTry, long millis, String message) {
              log(String.format("%s: Falsified after %s passed tests in %s ms.", name, currentTry - 1, millis)
                      + (message == null ? "" : " Message: " + message));
          }

          private static void logErroneous(String name, int currentTry, long millis, String errorMessage) {
              log(String.format("%s: Errored after %s passed tests in %s ms with message: %s", name, Math.max(0, currentTry - 1), millis, errorMessage));
          }

          private static void log(String msg) {
              System.out.println(msg);
          }

          /**
           * Creates a CheckError caused by an exception when obtaining a generator.
           *
           * @param position The position of the argument within the argument list of the property, starting with 1.
           * @param size     The size hint passed to the {@linkplain Arbitrary} which caused the error.
           * @param cause    The error which occurred when the {@linkplain Arbitrary} tried to obtain the generator {@linkplain Gen}.
           * @return a new CheckError instance.
           */
          private static CheckError arbitraryError(int position, int size, Throwable cause) {
              return new CheckError(String.format("Arbitrary %s of size %s: %s", position, size, cause.getMessage()), cause);
          }

          /**
           * Creates a CheckError caused by an exception when generating a value.
           *
           * @param position The position of the argument within the argument list of the property, starting with 1.
           * @param size     The size hint of the arbitrary which called the generator {@linkplain Gen} which caused the error.
           * @param cause    The error which occurred when the {@linkplain Gen} tried to generate a random value.
           * @return a new CheckError instance.
           */
          private static CheckError genError(int position, int size, Throwable cause) {
              return new CheckError(String.format("Gen %s of size %s: %s", position, size, cause.getMessage()), cause);
          }

          /**
           * Creates a CheckError caused by an exception when testing a Predicate.
           *
           * @param cause The error which occurred when applying the {@linkplain java.util.function.Predicate}.
           * @return a new CheckError instance.
           */
          private static CheckError predicateError(Throwable cause) {
              return new CheckError("Applying predicate: " + cause.getMessage(), cause);
          }

          ${(1 to N).gen(i => {
              val generics = (1 to i).gen(j => s"T$j")(", ")
              val parameters = (1 to i).gen(j => s"a$j")(", ")
              val parametersDecl = (1 to i).gen(j => s"Arbitrary<T$j> a$j")(", ")
              xs"""
                  /$javadoc
                   * Returns a logical for all quantor of $i given variables.
                   *
                   ${(1 to i).gen(j => s"* @param <T$j> ${j.ordinal} variable type of this for all quantor")("\n")}
                   ${(1 to i).gen(j => s"* @param a$j ${j.ordinal} variable of this for all quantor")("\n")}
                   * @return a new {@code ForAll$i} instance of $i variables
                   */
                  public <$generics> ForAll$i<$generics> forAll($parametersDecl) {
                      return new ForAll$i<>(name, $parameters);
                  }
              """
          })("\n\n")}

          ${(1 to N).gen(i => {
              val generics = (1 to i).gen(j => s"T$j")(", ")
              val params = (name: String) => (1 to i).gen(j => s"$name$j")(", ")
              val parametersDecl = (1 to i).gen(j => s"Arbitrary<T$j> a$j")(", ")
              xs"""
                  /$javadoc
                   * Represents a logical for all quantor.
                   *
                   ${(1 to i).gen(j => s"* @param <T$j> ${j.ordinal} variable type of this for all quantor")("\n")}
                   */
                  public static class ForAll$i<$generics> {

                      private final String name;
                      ${(1 to i).gen(j => xs"""
                          private final Arbitrary<T$j> a$j;
                      """)("\n")}

                      ForAll$i(String name, $parametersDecl) {
                          this.name = name;
                          ${(1 to i).gen(j => xs"""
                              this.a$j = a$j;
                          """)("\n")}
                      }

                      /$javadoc
                       * Returns a checkable property that checks values of the $i variables of this {@code ForAll} quantor.
                       *
                       * @param predicate A $i-ary predicate
                       * @return a new {@code Property$i} of $i variables.
                       */
                      public Property$i<$generics> suchThat(${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> predicate) {
                          final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Condition> proposition = (${params("t")}) -> new Condition(true, predicate.apply(${params("t")}));
                          return new Property$i<>(name, ${params("a")}, proposition);
                      }

                      /$javadoc
                       * Returns a checkable property whose predicate can explain a failure.
                       * A failed result contributes its message to the {@link CheckResult} and assertion errors.
                       * A thrown exception or a null result makes the check erroneous.
                       *
                       * @param predicate A $i-ary predicate returning a non-null {@link PredicateResult}
                       * @return a new {@code Property$i} of $i variables
                       * @throws NullPointerException if predicate is null
                       */
                      public Property$i<$generics> suchThatResult(${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, PredicateResult> predicate) {
                          ${im.getType("java.util.Objects")}.requireNonNull(predicate, "predicate is null");
                          final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Condition> proposition = (${params("t")}) -> new Condition(true, predicate.apply(${params("t")}));
                          return new Property$i<>(name, ${params("a")}, proposition);
                      }
                  }
              """
          })("\n\n")}

          ${(1 to N).gen(i => {

              val checkedFunctionType = im.getType(s"com.guizmaii.zazr.CheckedFunction$i")
              val optionType = im.getType("com.guizmaii.zazr.control.Option")
              val randomType = im.getType("java.util.Random")
              val checkException = "CheckException"
              val tupleType = im.getType(s"com.guizmaii.zazr.Tuple")

              val generics = (1 to i).gen(j => s"T$j")(", ")
              val params = (paramName: String) => (1 to i).gen(j => s"$paramName$j")(", ")
              val parametersDecl = (1 to i).gen(j => s"Arbitrary<T$j> a$j")(", ")

              xs"""
                  /$javadoc
                   * Represents a $i-ary checkable property.
                   */
                  public static class Property$i<$generics> implements Checkable {

                      private final String name;
                      ${(1 to i).gen(j => xs"""
                          private final Arbitrary<T$j> a$j;
                      """)("\n")}
                      private final $checkedFunctionType<$generics, Condition> predicate;

                      Property$i(String name, $parametersDecl, $checkedFunctionType<$generics, Condition> predicate) {
                          this.name = name;
                          ${(1 to i).gen(j => xs"""
                              this.a$j = a$j;
                          """)("\n")}
                          this.predicate = predicate;
                      }

                      /$javadoc
                       * Returns an implication which composes this Property as pre-condition and a given post-condition.
                       *
                       * @param postcondition The postcondition of this implication
                       * @return A new Checkable implication
                       */
                      public Checkable implies($checkedFunctionType<$generics, Boolean> postcondition) {
                          final $checkedFunctionType<$generics, Condition> implication = (${params("t")}) -> {
                              final Condition precondition = predicate.apply(${params("t")});
                              if (precondition.isFalse()) {
                                  return Condition.EX_FALSO_QUODLIBET;
                              } else {
                                  return new Condition(true, postcondition.apply(${params("t")}));
                              }
                          };
                          return new Property$i<>(name, ${params("a")}, implication);
                      }

                      /$javadoc
                       * Returns an implication whose postcondition can explain a failure.
                       * The postcondition is evaluated only when this property holds; messages from
                       * rejected preconditions are discarded. A thrown exception or a null result
                       * makes the check erroneous.
                       *
                       * @param postcondition The postcondition returning a non-null {@link PredicateResult}
                       * @return A new Checkable implication
                       * @throws NullPointerException if postcondition is null
                       */
                      public Checkable impliesResult($checkedFunctionType<$generics, PredicateResult> postcondition) {
                          ${im.getType("java.util.Objects")}.requireNonNull(postcondition, "postcondition is null");
                          final $checkedFunctionType<$generics, Condition> implication = (${params("t")}) -> {
                              final Condition precondition = predicate.apply(${params("t")});
                              if (precondition.isFalse()) {
                                  return Condition.EX_FALSO_QUODLIBET;
                              } else {
                                  return new Condition(true, postcondition.apply(${params("t")}));
                              }
                          };
                          return new Property$i<>(name, ${params("a")}, implication);
                      }

                      @Override
                      public CheckResult check($randomType random, int size, int tries) {
                          ${im.getType("java.util.Objects")}.requireNonNull(random, "random is null");
                          if (tries < 0) {
                              throw new IllegalArgumentException("tries < 0");
                          }
                          final long startTime = System.currentTimeMillis();
                          try {
                              ${(1 to i).gen(j => {
                                  xs"""
                                    final Gen<T$j> gen$j;
                                    try {
                                        gen$j = a$j.apply(size);
                                    } catch (Throwable x) {
                                        throw arbitraryError($j, size, x);
                                    }
                                  """
                              })("\n")}
                              boolean exhausted = true;
                              for (int i = 1; i <= tries; i++) {
                                  try {
                                      ${(1 to i).gen(j => {
                                        xs"""
                                          final T$j val$j;
                                          try {
                                              val$j = gen$j.apply(random);
                                          } catch (Throwable x) {
                                              throw genError($j, size, x);
                                          }
                                        """
                                      })("\n")}
                                      try {
                                          final Condition condition;
                                          try {
                                              condition = predicate.apply(${(1 to i).gen(j => s"val$j")(", ")});
                                          } catch (Throwable x) {
                                              throw predicateError(x);
                                          }
                                          if (condition.precondition) {
                                              exhausted = false;
                                              if (!condition.postcondition) {
                                                  logFalsified(name, i, System.currentTimeMillis() - startTime, condition.message);
                                                  return new CheckResult.Falsified(name, i, $tupleType.of(${(1 to i).gen(j => s"val$j")(", ")}), $optionType.ofNullable(condition.message));
                                              }
                                          }
                                      } catch(CheckError err) {
                                          logErroneous(name, i, System.currentTimeMillis() - startTime, err.getMessage());
                                          return new CheckResult.Erroneous(name, i, err, $optionType.some($tupleType.of(${(1 to i).gen(j => s"val$j")(", ")})));
                                      }
                                  } catch(CheckError err) {
                                      logErroneous(name, i, System.currentTimeMillis() - startTime, err.getMessage());
                                      return new CheckResult.Erroneous(name, i, err, $optionType.none());
                                  }
                              }
                              logSatisfied(name, tries, System.currentTimeMillis() - startTime, exhausted);
                              return new CheckResult.Satisfied(name, tries, exhausted);
                          } catch(CheckError err) {
                              logErroneous(name, 0, System.currentTimeMillis() - startTime, err.getMessage());
                              return new CheckResult.Erroneous(name, 0, err, $optionType.none());
                          }
                      }
                  }
              """
          })("\n\n")}

          /**
           * Internally used to model conditions composed of pre- and post-condition.
           */
          static class Condition {

              static final Condition EX_FALSO_QUODLIBET = new Condition(false, true);

              final boolean precondition;
              final boolean postcondition;
              final String message;

              Condition(boolean precondition, boolean postcondition) {
                  this(precondition, postcondition, null);
              }

              Condition(boolean precondition, PredicateResult result) {
                  this(precondition, ${im.getType("java.util.Objects")}.requireNonNull(result, "predicate result is null").isSuccess(), result.message().getOrNull());
              }

              private Condition(boolean precondition, boolean postcondition, String message) {
                  this.precondition = precondition;
                  this.postcondition = postcondition;
                  this.message = message;
              }

              // ¬(p => q) ≡ ¬(¬p ∨ q) ≡ p ∧ ¬q
              boolean isFalse() {
                  return precondition && !postcondition;
              }
          }

          /**
           * Internally used to provide more specific error messages.
           */
          static class CheckError extends Error {

              CheckError(String message, Throwable cause) {
                  super(message, cause);
              }
          }
      }
    """
  }

  genCheck()

  /**
   * Generator of com.guizmaii.zazr.test.Check: check, checkN and checkAll for 1 to N generators.
   */
  def genCheck(): Unit = {

    genVavrFile("com.guizmaii.zazr.test", "Check")((im: ImportManager, packageName: String, className: String) => {

      val objects = im.getType("java.util.Objects")

      def genArity(i: Int): String = {
        val generics = (1 to i).gen(j => s"T$j")(using ", ")
        val gens = (1 to i).gen(j => s"Gen<? extends T$j> g$j")(using ", ")
        val gensArgs = (1 to i).gen(j => s"g$j")(using ", ")
        val checked = im.getType(s"com.guizmaii.zazr.CheckedFunction$i")
        val bodyType = s"$checked<${(1 to i).gen(j => s"? super T$j")(using ", ")}, Boolean>"
        val tupleType = im.getType(s"com.guizmaii.zazr.Tuple$i")
        val zipped = if (i == 1) s"g1.<$tupleType<T1>>map(${im.getType("com.guizmaii.zazr.Tuple")}::of)" else s"Gen.zip($gensArgs)"
        val apply = s"sample -> body.apply(${(1 to i).gen(j => s"sample._$j()")(using ", ")})"
        val genParams = (1 to i).gen(j => s"* @param g$j   the generator of the ${j.ordinal} value")(using "\n")
        val typeParams = (1 to i).gen(j => s"* @param <T$j> the type of the ${j.ordinal} value")(using "\n")
        val requireGens = (1 to i).gen(j => s"""$objects.requireNonNull(g$j, "g$j is null");""")(using "\n")
        val bodyDoc = if (i == 1) "the property of a value: true when it holds" else s"the property of $i values: true when it holds"
        xs"""
          /$javadoc
           * Checks {@code body} against {@link CheckConfig#defaults()}: 200 samples unless configured otherwise.
           *
           $genParams
           * @param body $bodyDoc
           $typeParams
           * @return the result of the check
           * @throws NullPointerException if an argument is null
           */
          public static <$generics> CheckResult check($gens, $bodyType body) {
              return check(CheckConfig.defaults(), $gensArgs, body);
          }

          /$javadoc
           * Checks {@code body} against {@code config.samples()} samples drawn pass after pass from the generators,
           * the size growing from 0 to {@code config.size()}. It stops at the first sample that fails.
           *
           * @param config the number of samples, the size and the seed
           $genParams
           * @param body $bodyDoc
           $typeParams
           * @return the result of the check
           * @throws NullPointerException if an argument is null
           */
          public static <$generics> CheckResult check(CheckConfig config, $gens, $bodyType body) {
              $objects.requireNonNull(config, "config is null");
              $requireGens
              $objects.requireNonNull(body, "body is null");
              return Runner.check(config, $zipped, $apply, false);
          }

          /$javadoc
           * Checks {@code body} against {@code samples} samples, the rest of {@link CheckConfig#defaults()} unchanged.
           *
           * @param samples the number of samples
           $genParams
           * @param body $bodyDoc
           $typeParams
           * @return the result of the check
           * @throws NullPointerException     if a generator or {@code body} is null
           * @throws IllegalArgumentException if {@code samples} is negative
           */
          public static <$generics> CheckResult checkN(int samples, $gens, $bodyType body) {
              return check(CheckConfig.defaults().withSamples(samples), $gensArgs, body);
          }

          /$javadoc
           * Checks {@code body} against every value of one pass of the generators, with {@link CheckConfig#defaults()}.
           *
           $genParams
           * @param body $bodyDoc
           $typeParams
           * @return the result of the check
           * @throws NullPointerException if an argument is null
           */
          public static <$generics> CheckResult checkAll($gens, $bodyType body) {
              return checkAll(CheckConfig.defaults(), $gensArgs, body);
          }

          /$javadoc
           * Checks {@code body} against every value of one pass of the generators, at the size {@code config.size()}:
           * every combination of the values of finite generators, each checked once. A random generator gives one
           * value per pass. The number of samples of {@code config} is not used. It stops at the first sample that
           * fails.
           *
           * @param config the size and the seed
           $genParams
           * @param body $bodyDoc
           $typeParams
           * @return the result of the check
           * @throws NullPointerException if an argument is null
           */
          public static <$generics> CheckResult checkAll(CheckConfig config, $gens, $bodyType body) {
              $objects.requireNonNull(config, "config is null");
              $requireGens
              $objects.requireNonNull(body, "body is null");
              return Runner.check(config, $zipped, $apply, true);
          }
        """
      }

      xs"""
        /$javadoc
         * Checks a property against generated values, from 1 to $N generators.
         * <p>
         * The property is a function of the generated values that returns {@code true} when it holds. It may also
         * throw an {@link AssertionError}, such as a failed JUnit or AssertJ assertion, which falsifies the sample
         * like {@code false} and keeps its message; any other exception makes the check {@link CheckResult.Erroneous}.
         * <p>
         * {@code check} and {@code checkN} run {@link CheckConfig#samples()} samples, pass after pass of the
         * generators, with a size that grows from 0 for the first sample to {@link CheckConfig#size()} for the last:
         * the first failure found is usually a small one. {@code checkAll} runs one pass, so it checks every value of
         * finite generators once. Each check stops at its first failure and returns a {@link CheckResult} that
         * carries the sample, its number and the seed; {@link CheckResult#assertIsSatisfied()} turns it into a test
         * failure.
         */
        public final class $className {

            private $className() {
            }

            ${(1 to N).gen(genArity)(using "\n\n")}
        }
      """
    })
  }
}

/**
 * Generate Vavr src-gen/test/java classes
 */
def generateTestClasses(): Unit = {

  genPropertyCheckTests()

  /**
   * Generator of Property-check tests
   */
  def genPropertyCheckTests(): Unit = {
    genVavrFile("com.guizmaii.zazr.test.legacy", "PropertyTest", baseDir = TARGET_TEST)((im: ImportManager, packageName, className) => {

      // main classes
      val list = im.getType("com.guizmaii.zazr.collection.List")
      val predicate = im.getType("com.guizmaii.zazr.CheckedFunction1")
      val random = im.getType("java.util.Random")
      val tuple = im.getType("com.guizmaii.zazr.Tuple")

      // test classes
      val test = im.getType("org.junit.jupiter.api.Test")
      val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
      val assertThrows = im.getStatic("org.junit.jupiter.api.Assertions.assertThrows")
      val woops  = "yay! (this is a negative test)"

      xs"""
        public class $className {

            static <T> $predicate<T, Boolean> tautology() {
                return any -> true;
            }

            static <T> $predicate<T, Boolean> falsum() {
                return any -> false;
            }

            static final Arbitrary<Object> OBJECTS = Gen.of(null).arbitrary();

            @$test
            public void shouldThrowWhenPropertyNameIsNull() {
                $assertThrows(NullPointerException.class, () -> Property.named(null));
            }

            @$test
            public void shouldThrowWhenPropertyNameIsEmpty() {
                $assertThrows(IllegalArgumentException.class, () -> Property.named(""));
            }

            // -- Property.check methods

            @$test
            public void shouldCheckUsingDefaultConfiguration() {
                final CheckResult result = Property.named("test").forAll(OBJECTS).suchThat(tautology()).check();
                $assertThat(result.isSatisfied()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
            }

            @$test
            public void shouldCheckGivenSizeAndTries() {
                final CheckResult result = Property.named("test").forAll(OBJECTS).suchThat(tautology()).check(0, 0);
                $assertThat(result.isSatisfied()).isTrue();
                $assertThat(result.isExhausted()).isTrue();
            }

            @$test
            public void shouldThrowOnCheckGivenNegativeTries() {
                $assertThrows(IllegalArgumentException.class, () -> Property.named("test").forAll(OBJECTS).suchThat(tautology()).check(0, -1));
            }

            @$test
            public void shouldCheckGivenRandomAndSizeAndTries() {
                final CheckResult result = Property.named("test").forAll(OBJECTS).suchThat(tautology()).check(new $random(), 0, 0);
                $assertThat(result.isSatisfied()).isTrue();
                $assertThat(result.isExhausted()).isTrue();
            }

            // -- satisfaction

            @$test
            public void shouldCheckPythagoras() {

                final Arbitrary<Double> real = n -> Gen.choose(0, (double) n).filter(d -> d > .0d);

                // (∀a,b ∈ ℝ+ ∃c ∈ ℝ+ : a²+b²=c²) ≡ (∀a,b ∈ ℝ+ : √(a²+b²) ∈ ℝ+)
                final Checkable property = Property.named("test").forAll(real, real).suchThat((a, b) -> Math.hypot(a, b) > .0d);
                final CheckResult result = property.check();

                $assertThat(result.isSatisfied()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
            }

            @$test
            public void shouldCheckZipAndThenUnzipIsIdempotentForListsOfSameLength() {
                // ∀is,ss: length(is) = length(ss) → unzip(zip(is, ss)) = (is, ss)
                final Arbitrary<$list<Integer>> ints = Arbitrary.list(size -> Gen.choose(0, size));
                final Arbitrary<$list<String>> strings = Arbitrary.list(
                        Arbitrary.string(
                            Gen.frequency(
                                Tuple.of(1, Gen.choose('A', 'Z')),
                                Tuple.of(1, Gen.choose('a', 'z')),
                                Tuple.of(1, Gen.choose('0', '9'))
                            )));
                final CheckResult result = Property.named("test")
                        .forAll(ints, strings)
                        .suchThat((is, ss) -> is.length() == ss.length())
                        .implies((is, ss) -> is.zip(ss).unzip(t -> t).equals($tuple.of(is, ss)))
                        .check();
                $assertThat(result.isSatisfied()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
            }

            // -- exhausting

            @$test
            public void shouldRecognizeExhaustedParameters() {
                final CheckResult result = Property.named("test").forAll(OBJECTS).suchThat(falsum()).implies(tautology()).check();
                $assertThat(result.isSatisfied()).isTrue();
                $assertThat(result.isExhausted()).isTrue();
            }

            // -- falsification

            @$test
            public void shouldFalsifyFalseProperty() {
                final Arbitrary<Integer> ones = n -> random -> 1;
                final CheckResult result = Property.named("test").forAll(ones).suchThat(one -> one == 2).check();
                $assertThat(result.isFalsified()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
                $assertThat(result.count()).isEqualTo(1);
            }

            // -- error detection

            @$test
            public void shouldRecognizeArbitraryError() {
                final Arbitrary<?> arbitrary = n -> { throw new RuntimeException("$woops"); };
                final CheckResult result = Property.named("test").forAll(arbitrary).suchThat(tautology()).check();
                $assertThat(result.isErroneous()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
                $assertThat(result.count()).isEqualTo(0);
                $assertThat(result.sample().isEmpty()).isTrue();
            }

            @$test
            public void shouldRecognizeGenError() {
                final Arbitrary<?> arbitrary = Gen.fail("$woops").arbitrary();
                final CheckResult result = Property.named("test").forAll(arbitrary).suchThat(tautology()).check();
                $assertThat(result.isErroneous()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
                $assertThat(result.count()).isEqualTo(1);
                $assertThat(result.sample().isEmpty()).isTrue();
            }

            @$test
            public void shouldRecognizePropertyError() {
                final Arbitrary<Integer> a1 = n -> random -> 1;
                final Arbitrary<Integer> a2 = n -> random -> 2;
                final CheckResult result = Property.named("test").forAll(a1, a2).suchThat((a, b) -> {
                    throw new RuntimeException("$woops");
                }).check();
                $assertThat(result.isErroneous()).isTrue();
                $assertThat(result.isExhausted()).isFalse();
                $assertThat(result.count()).isEqualTo(1);
                $assertThat(result.sample().isDefined()).isTrue();
                $assertThat(result.sample().get()).isEqualTo(Tuple.of(1, 2));
            }

            // -- Property.and tests

            @$test
            public void shouldCheckAndCombinationWhereFirstPropertyIsTrueAndSecondPropertyIsTrue() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final CheckResult result = p1.and(p2).check();
                $assertThat(result.isSatisfied()).isTrue();
            }

            @$test
            public void shouldCheckAndCombinationWhereFirstPropertyIsTrueAndSecondPropertyIsFalse() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final CheckResult result = p1.and(p2).check();
                $assertThat(result.isSatisfied()).isFalse();
            }

            @$test
            public void shouldCheckAndCombinationWhereFirstPropertyIsFalseAndSecondPropertyIsTrue() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final CheckResult result = p1.and(p2).check();
                $assertThat(result.isSatisfied()).isFalse();
            }

            @$test
            public void shouldCheckAndCombinationWhereFirstPropertyIsFalseAndSecondPropertyIsFalse() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final CheckResult result = p1.and(p2).check();
                $assertThat(result.isSatisfied()).isFalse();
            }

            // -- Property.or tests

            @$test
            public void shouldCheckOrCombinationWhereFirstPropertyIsTrueAndSecondPropertyIsTrue() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final CheckResult result = p1.or(p2).check();
                $assertThat(result.isSatisfied()).isTrue();
            }

            @$test
            public void shouldCheckOrCombinationWhereFirstPropertyIsTrueAndSecondPropertyIsFalse() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final CheckResult result = p1.or(p2).check();
                $assertThat(result.isSatisfied()).isTrue();
            }

            @$test
            public void shouldCheckOrCombinationWhereFirstPropertyIsFalseAndSecondPropertyIsTrue() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(tautology());
                final CheckResult result = p1.or(p2).check();
                $assertThat(result.isSatisfied()).isTrue();
            }

            @$test
            public void shouldCheckOrCombinationWhereFirstPropertyIsFalseAndSecondPropertyIsFalse() {
                final Checkable p1 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final Checkable p2 = Property.named("test").forAll(OBJECTS).suchThat(falsum());
                final CheckResult result = p1.or(p2).check();
                $assertThat(result.isSatisfied()).isFalse();
            }
        }
      """
    })

    for (i <- 1 to N) {
      genVavrFile("com.guizmaii.zazr.test.legacy", s"PropertyCheck${i}Test", baseDir = TARGET_TEST)((im: ImportManager, packageName, className) => {

        val generics = (1 to i).gen(j => "Object")(", ")
        val arbitraries = (1 to i).gen(j => "OBJECTS")(", ")
        val arbitrariesMinus1 = (1 until i).gen(j => "OBJECTS")(", ")
        val args = (1 to i).gen(j => s"o$j")(", ")

        // test classes
        val test = im.getType("org.junit.jupiter.api.Test")
        val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
        val assertThrows = im.getStatic("org.junit.jupiter.api.Assertions.assertThrows")
        val assertThatThrownBy = im.getStatic("org.assertj.core.api.Assertions.assertThatThrownBy")
        val tupleType = im.getType("com.guizmaii.zazr.Tuple")
        val woops = "yay! (this is a negative test)"

        xs"""
          public class $className {

              static final Arbitrary<Object> OBJECTS = Gen.of(null).arbitrary();

              @$test
              public void shouldApplyForAllOfArity$i() {
                  final Property.ForAll$i<${(1 to i).gen(j => "Object")(", ")}> forAll = Property.named("test").forAll(${(1 to i).gen(j => "null")(", ")});
                  $assertThat(forAll).isNotNull();
              }

              @$test
              public void shouldApplySuchThatOfArity$i() {
                  final Property.ForAll$i<$generics> forAll = Property.named("test").forAll($arbitraries);
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> predicate = ($args) -> true;
                  final Property.Property$i<$generics> suchThat = forAll.suchThat(predicate);
                  $assertThat(suchThat).isNotNull();
              }

              @$test
              public void shouldCheckTrueProperty$i() {
                  final Property.ForAll$i<$generics> forAll = Property.named("test").forAll($arbitraries);
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> predicate = ($args) -> true;
                  final CheckResult result = forAll.suchThat(predicate).check();
                  $assertThat(result.isSatisfied()).isTrue();
                  $assertThat(result.isExhausted()).isFalse();
              }

              @$test
              public void shouldCheckFalseProperty$i() {
                  final Property.ForAll$i<$generics> forAll = Property.named("test").forAll($arbitraries);
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> predicate = ($args) -> false;
                  final CheckResult result = forAll.suchThat(predicate).check();
                  $assertThat(result.isFalsified()).isTrue();
                  $assertThat(result.message().isEmpty()).isTrue();
              }

              @$test
              public void shouldCheckSuccessfulPredicateResult$i() {
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThatResult(($args) -> PredicateResult.success()).check(0, 3);
                  $assertThat(result.isSatisfied()).isTrue();
                  $assertThat(result.isExhausted()).isFalse();
                  $assertThat(result.count()).isEqualTo(3);
                  $assertThat(result.message().isEmpty()).isTrue();
              }

              @$test
              public void shouldReportPredicateFailureMessage$i() {
                  final CheckResult result = Property.named("test")
                          .forAll(${(1 to i).gen(j => s"Gen.of($j).arbitrary()")(", ")})
                          .suchThatResult(($args) -> PredicateResult.failure("failed: " + $tupleType.of($args)))
                          .check(0, 3);
                  $assertThat(result.isFalsified()).isTrue();
                  $assertThat(result.isErroneous()).isFalse();
                  $assertThat(result.count()).isEqualTo(1);
                  $assertThat(result.sample().get()).isEqualTo($tupleType.of(${(1 to i).gen(j => s"$j")(", ")}));
                  $assertThat(result.message().get()).isEqualTo("failed: (${(1 to i).gen(j => s"$j")(", ")})");
                  $assertThat(result.error().isEmpty()).isTrue();
                  $assertThatThrownBy(result::assertIsSatisfied)
                          .isInstanceOf(AssertionError.class)
                          .hasMessageContaining("failed: (${(1 to i).gen(j => s"$j")(", ")})");
              }

              @$test
              public void shouldCheckErroneousPredicateResult$i() {
                  final Exception cause = new Exception("$woops");
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThatResult(($args) -> { throw cause; }).check(0, 3);
                  $assertThat(result.isErroneous()).isTrue();
                  $assertThat(result.error().get()).hasCause(cause);
                  $assertThat(result.sample().isDefined()).isTrue();
                  $assertThat(result.message().isEmpty()).isTrue();
              }

              @$test
              public void shouldReportNullPredicateResultAsErroneous$i() {
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThatResult(($args) -> null).check(0, 3);
                  $assertThat(result.isErroneous()).isTrue();
                  $assertThat(result.error().get()).hasCauseInstanceOf(NullPointerException.class);
                  $assertThat(result.sample().isDefined()).isTrue();
              }

              @$test
              public void shouldRejectNullResultPredicate$i() {
                  $assertThrows(NullPointerException.class, () -> Property.named("test").forAll($arbitraries).suchThatResult(null));
              }

              @$test
              public void shouldReportPostconditionFailureMessage$i() {
                  final CheckResult result = Property.named("test")
                          .forAll(${(1 to i).gen(j => s"Gen.of($j).arbitrary()")(", ")})
                          .suchThat(($args) -> true)
                          .impliesResult(($args) -> PredicateResult.failure("postcondition: " + $tupleType.of($args)))
                          .check(0, 3);
                  $assertThat(result.isFalsified()).isTrue();
                  $assertThat(result.sample().get()).isEqualTo($tupleType.of(${(1 to i).gen(j => s"$j")(", ")}));
                  $assertThat(result.message().get()).isEqualTo("postcondition: (${(1 to i).gen(j => s"$j")(", ")})");
              }

              @$test
              public void shouldCheckSuccessfulResultImplication$i() {
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThatResult(($args) -> PredicateResult.success())
                          .impliesResult(($args) -> PredicateResult.success()).check(0, 3);
                  $assertThat(result.isSatisfied()).isTrue();
                  $assertThat(result.isExhausted()).isFalse();
                  $assertThat(result.message().isEmpty()).isTrue();
              }

              @$test
              public void shouldSkipResultPostconditionForFalseBooleanPrecondition$i() {
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThat(($args) -> false)
                          .impliesResult(($args) -> { throw new AssertionError("must not run"); }).check(0, 3);
                  $assertThat(result.isSatisfied()).isTrue();
                  $assertThat(result.isExhausted()).isTrue();
                  $assertThat(result.message().isEmpty()).isTrue();
              }

              @$test
              public void shouldDiscardRejectedPreconditionMessage$i() {
                  final Property.Property$i<$generics> property = Property.named("test").forAll($arbitraries)
                          .suchThatResult(($args) -> PredicateResult.failure("rejected input"));
                  final CheckResult booleanResult = property
                          .implies(($args) -> { throw new AssertionError("must not run"); }).check(0, 3);
                  final CheckResult detailedResult = property
                          .impliesResult(($args) -> { throw new AssertionError("must not run"); }).check(0, 3);
                  for (CheckResult result : new CheckResult[] { booleanResult, detailedResult }) {
                      $assertThat(result.isSatisfied()).isTrue();
                      $assertThat(result.isExhausted()).isTrue();
                      $assertThat(result.count()).isEqualTo(3);
                      $assertThat(result.message().isEmpty()).isTrue();
                  }
              }

              @$test
              public void shouldAllowBooleanPostconditionAfterPredicateResult$i() {
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThatResult(($args) -> PredicateResult.success())
                          .implies(($args) -> false).check(0, 3);
                  $assertThat(result.isFalsified()).isTrue();
                  $assertThat(result.message().isEmpty()).isTrue();
              }

              @$test
              public void shouldReportNullPostconditionResultAsErroneous$i() {
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThat(($args) -> true).impliesResult(($args) -> null).check(0, 3);
                  $assertThat(result.isErroneous()).isTrue();
                  $assertThat(result.error().get()).hasCauseInstanceOf(NullPointerException.class);
                  $assertThat(result.sample().isDefined()).isTrue();
              }

              @$test
              public void shouldCheckErroneousPostconditionResult$i() {
                  final Exception cause = new Exception("$woops");
                  final CheckResult result = Property.named("test").forAll($arbitraries)
                          .suchThat(($args) -> true).impliesResult(($args) -> { throw cause; }).check(0, 3);
                  $assertThat(result.isErroneous()).isTrue();
                  $assertThat(result.error().get()).hasCause(cause);
                  $assertThat(result.sample().isDefined()).isTrue();
              }

              @$test
              public void shouldRejectNullResultPostcondition$i() {
                  $assertThrows(NullPointerException.class, () -> Property.named("test").forAll($arbitraries).suchThat(($args) -> true).impliesResult(null));
              }

              @$test
              public void shouldCheckErroneousProperty$i() {
                  final Property.ForAll$i<$generics> forAll = Property.named("test").forAll($arbitraries);
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> predicate = ($args) -> { throw new RuntimeException("$woops"); };
                  final CheckResult result = forAll.suchThat(predicate).check();
                  $assertThat(result.isErroneous()).isTrue();
              }

              @$test
              public void shouldCheckProperty${i}ImplicationWithTruePrecondition() {
                  final Property.ForAll$i<$generics> forAll = Property.named("test").forAll($arbitraries);
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> p1 = ($args) -> true;
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> p2 = ($args) -> true;
                  final CheckResult result = forAll.suchThat(p1).implies(p2).check();
                  $assertThat(result.isSatisfied()).isTrue();
                  $assertThat(result.isExhausted()).isFalse();
              }

              @$test
              public void shouldCheckProperty${i}ImplicationWithFalsePrecondition() {
                  final Property.ForAll$i<$generics> forAll = Property.named("test").forAll($arbitraries);
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> p1 = ($args) -> false;
                  final ${im.getType(s"com.guizmaii.zazr.CheckedFunction$i")}<$generics, Boolean> p2 = ($args) -> true;
                  final CheckResult result = forAll.suchThat(p1).implies(p2).check();
                  $assertThat(result.isSatisfied()).isTrue();
                  $assertThat(result.isExhausted()).isTrue();
              }

              @$test
              public void shouldThrowOnProperty${i}CheckGivenNegativeTries() {
                  $assertThrows(IllegalArgumentException.class, () -> Property.named("test")
                      .forAll($arbitraries)
                      .suchThat(($args) -> true)
                      .check(Checkable.RNG.get(), 0, -1));
              }

              @$test
              public void shouldReturnErroneousProperty${i}CheckResultIfGenFails() {
                  final Arbitrary<Object> failingGen = Gen.fail("$woops").arbitrary();
                  final CheckResult result = Property.named("test")
                      .forAll(failingGen${(i > 1).gen(s", $arbitrariesMinus1")})
                      .suchThat(($args) -> true)
                      .check();
                  $assertThat(result.isErroneous()).isTrue();
              }

              @$test
              public void shouldReturnErroneousProperty${i}CheckResultIfArbitraryFails() {
                  final Arbitrary<Object> failingArbitrary = size -> { throw new RuntimeException("$woops"); };
                  final CheckResult result = Property.named("test")
                      .forAll(failingArbitrary${(i > 1).gen(s", $arbitrariesMinus1")})
                      .suchThat(($args) -> true)
                      .check();
                  $assertThat(result.isErroneous()).isTrue();
              }
          }
         """
      })
    }
  }

  genCheckTests()

  /**
   * Generator of the tests of com.guizmaii.zazr.test.Check, one class per arity.
   */
  def genCheckTests(): Unit = {
    for (i <- 1 to N) {
      genVavrFile("com.guizmaii.zazr.test", s"Check${i}Test", baseDir = TARGET_TEST)((im: ImportManager, packageName: String, className: String) => {

        val test = im.getType("org.junit.jupiter.api.Test")
        val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
        val assertThatThrownBy = im.getStatic("org.assertj.core.api.Assertions.assertThatThrownBy")
        val tuple = im.getType("com.guizmaii.zazr.Tuple")
        val option = im.getType("com.guizmaii.zazr.control.Option")
        val arrayList = im.getType("java.util.ArrayList")
        val jlist = im.getType("java.util.List")

        val params = (1 to i).gen(j => s"v$j")(using ", ")
        val constants = (1 to i).gen(j => s"Gen.constant($j)")(using ", ")
        val ones = (1 to i).gen(j => s"$j")(using ", ")
        val twos = (1 to i).gen(j => "TWO")(using ", ")
        val sum = (1 to i).gen(j => s"v$j")(using " + ")
        val tupleOfParams = s"$tuple.of($params)"
        val combinations = 1 << i

        def failingAt(k: Int): String = (1 to i).gen(j => if (j == k) "FAILING" else s"Gen.constant($j)")(using ", ")
        def nullAt(k: Int): String = (1 to i).gen(j => if (j == k) "null" else s"Gen.constant($j)")(using ", ")

        xs"""
          class $className {

              static final CheckConfig CONFIG = new CheckConfig(20, 10, 42L, 100);
              static final Gen<Integer> TWO = Gen.fromIterable($jlist.of(0, 1));
              static final IllegalStateException BOOM = new IllegalStateException("boom");
              static final Gen<Integer> FAILING = Gen.fromRandom(random -> {
                  throw BOOM;
              });

              @$test
              void passesTheValuesInOrder() {
                  final $arrayList<Object> seen = new $arrayList<>();
                  final CheckResult result = Check.check(CONFIG, $constants, ($params) -> seen.add($tupleOfParams));
                  $assertThat(result).isEqualTo(new CheckResult.Satisfied(20));
                  $assertThat(seen).hasSize(20).containsOnly($tuple.of($ones));
              }

              @$test
              void checkUsesTheDefaultConfiguration() {
                  $assertThat(Check.check($constants, ($params) -> true)).isEqualTo(new CheckResult.Satisfied(CheckConfig.defaults().samples()));
              }

              @$test
              void checkNRunsNSamples() {
                  $assertThat(Check.checkN(3, $constants, ($params) -> true)).isEqualTo(new CheckResult.Satisfied(3));
                  $assertThatThrownBy(() -> Check.checkN(-1, $constants, ($params) -> true)).isInstanceOf(IllegalArgumentException.class);
              }

              @$test
              void checkAllRunsEveryCombinationOnce() {
                  final $arrayList<Object> seen = new $arrayList<>();
                  $assertThat(Check.checkAll($twos, ($params) -> seen.add($tupleOfParams))).isEqualTo(new CheckResult.Satisfied($combinations));
                  $assertThat(seen).hasSize($combinations).doesNotHaveDuplicates();
                  seen.clear();
                  $assertThat(Check.checkAll(CONFIG, $twos, ($params) -> seen.add($tupleOfParams))).isEqualTo(new CheckResult.Satisfied($combinations));
                  $assertThat(seen).hasSize($combinations).doesNotHaveDuplicates();
              }

              @$test
              void falseFalsifiesTheCheck() {
                  $assertThat(Check.check(CONFIG, $constants, ($params) -> false))
                          .isEqualTo(new CheckResult.Falsified(1, 42L, $tuple.of($ones), $option.none()));
                  $assertThat(Check.checkAll(CONFIG, $twos, ($params) -> $sum < ${i}))
                          .isEqualTo(new CheckResult.Falsified($combinations, 42L, $tuple.of(${(1 to i).gen(j => "1")(using ", ")}), $option.none()));
              }

              @$test
              void anAssertionErrorFalsifiesTheCheck() {
                  $assertThat(Check.check(CONFIG, $constants, ($params) -> {
                      throw new AssertionError("sum " + ($sum));
                  })).isEqualTo(new CheckResult.Falsified(1, 42L, $tuple.of($ones), $option.some("sum ${(1 to i).sum}")));
              }

              @$test
              void anExceptionMakesTheCheckErroneous() {
                  $assertThat(Check.check(CONFIG, $constants, ($params) -> {
                      throw BOOM;
                  })).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, $option.some($tuple.of($ones))));
                  $assertThat(Check.check(CONFIG, $constants, ($params) -> null).isErroneous()).isTrue();
              }

              @$test
              void aFailingGeneratorMakesTheCheckErroneous() {
                  ${(1 to i).gen(k => xs"""
                    $assertThat(Check.check(CONFIG, ${failingAt(k)}, ($params) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, $option.none()));
                    $assertThat(Check.checkAll(CONFIG, ${failingAt(k)}, ($params) -> true)).isEqualTo(new CheckResult.Erroneous(1, 42L, BOOM, $option.none()));
                  """)(using "\n")}
              }

              @$test
              void rejectsNulls() {
                  $assertThatThrownBy(() -> Check.check((CheckConfig) null, $constants, ($params) -> true)).isInstanceOf(NullPointerException.class);
                  $assertThatThrownBy(() -> Check.checkAll((CheckConfig) null, $constants, ($params) -> true)).isInstanceOf(NullPointerException.class);
                  $assertThatThrownBy(() -> Check.check(CONFIG, $constants, null)).isInstanceOf(NullPointerException.class);
                  $assertThatThrownBy(() -> Check.checkAll(CONFIG, $constants, null)).isInstanceOf(NullPointerException.class);
                  ${(1 to i).gen(k => xs"""
                    $assertThatThrownBy(() -> Check.check(CONFIG, ${nullAt(k)}, ($params) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g$k is null");
                    $assertThatThrownBy(() -> Check.checkAll(CONFIG, ${nullAt(k)}, ($params) -> true)).isInstanceOf(NullPointerException.class).hasMessage("g$k is null");
                  """)(using "\n")}
              }
          }
        """
      })
    }
  }
}

/**
 * Adds the Vavr header to generated classes.
 * @param packageName Java package name
 * @param className Simple java class name
 * @param gen A generator which produces a String.
 */
def genVavrFile(packageName: String, className: String, baseDir: String = TARGET_MAIN)(gen: (ImportManager, String, String) => String, knownSimpleClassNames: List[String] = List()) =
  genJavaFile(baseDir, packageName, className)("")(gen)(CHARSET)

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
     J A V A   G E N E R A T O R   F R A M E W O R K
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

object JavaGenerator {

  import java.nio.charset.{Charset, StandardCharsets}

  import Generator._

  /**
   * Generates a Java file.
   * @param packageName Java package name
   * @param className Simple java class name
   * @param classHeader A class file header
   * @param gen A generator which produces a String.
   */
  def genJavaFile(baseDir: String, packageName: String, className: String)(classHeader: String)(gen: (ImportManager, String, String) => String, knownSimpleClassNames: List[String] = List())(implicit charset: Charset = StandardCharsets.UTF_8): Unit = {

    // DEV-NOTE: using File.separator instead of "/" does *not* work on windows!
    val dirName = packageName.replaceAll("[.]", "/")
    val fileName = className + ".java"
    val importManager = new ImportManager(packageName, knownSimpleClassNames)
    val classBody = gen.apply(importManager, packageName, className)

    genFile(baseDir, dirName, fileName)(xraw"""
      $classHeader
      package $packageName;

      /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
         G E N E R A T O R   C R A F T E D
      \*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

      ${importManager.getImports}

      $classBody
    """)
  }

  /**
   * A <em>stateful</em> ImportManager which generates an import section of a Java class file.
   * @param packageNameOfClass package name of the generated class
   * @param knownSimpleClassNames a list of class names which may not be imported from other packages
   */
  class ImportManager(packageNameOfClass: String, knownSimpleClassNames: List[String], wildcardThreshold: Int = 5) {

    import scala.collection.mutable

    val nonStaticImports = new mutable.HashMap[String, String]
    val staticImports = new mutable.HashMap[String, String]

    def getType(fullQualifiedName: String): String = simplify(fullQualifiedName, nonStaticImports)

    def getStatic(fullQualifiedName: String): String = simplify(fullQualifiedName, staticImports)

    def getImports: String = {

      def optimizeImports(imports: Seq[String], static: Boolean): String = {
        val counts = imports.map(getPackageName).groupBy(s => s).map { case (s, list) => s -> list.length }
        val directImports = imports.filter(s => counts(getPackageName(s)) <= wildcardThreshold)
        val wildcardImports = counts.filter { case (_, count) => count > wildcardThreshold }.keySet.toIndexedSeq.map(s => s"$s.*")
        (directImports ++ wildcardImports).sorted.map(fqn => s"import ${static.gen("static ")}$fqn;").mkString("\n")
      }

      val staticImportSection = optimizeImports(staticImports.keySet.toIndexedSeq, static = true)
      val nonStaticImportSection = optimizeImports(nonStaticImports.keySet.toIndexedSeq, static = false)
      Seq(staticImportSection, nonStaticImportSection).mkString("\n\n")
    }

    private def simplify(fullQualifiedName: String, imports: mutable.HashMap[String, String]): String = {
      val simpleName = getSimpleName(fullQualifiedName)
      val packageName = getPackageName(fullQualifiedName)
      if (packageName.isEmpty && !packageNameOfClass.isEmpty) {
        throw new IllegalStateException(s"Can't import class '$simpleName' located in default package")
      } else if (packageName == packageNameOfClass) {
        simpleName
      } else if (imports.contains(fullQualifiedName)) {
        imports(fullQualifiedName)
      } else if (simpleName != "*" && (knownSimpleClassNames.contains(simpleName) || imports.values.exists(simpleName.equals(_)))) {
        fullQualifiedName
      } else {
        imports += fullQualifiedName -> simpleName
        simpleName
      }
    }

    private def getPackageName(fqn: String): String = fqn.substring(0, Math.max(fqn.lastIndexOf("."), 0))
    private def getSimpleName(fqn: String): String = fqn.substring(fqn.lastIndexOf(".") + 1)
  }
}

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
     C O R E   G E N E R A T O R   F R A M E W O R K
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

/**
 * Core generator API
 */
object Generator {

  import java.nio.charset.{Charset, StandardCharsets}
  import java.nio.file.{Files, Path, Paths}
  import scala.jdk.CollectionConverters._

  // The real paths of the files written by this run, so that `deleteStaleFiles` knows which ones it no longer
  // produces, and their lower-cased paths, so that two names differing only in case, the same file on a
  // case-insensitive file system, fail the build.
  private val generated = scala.collection.mutable.Set.empty[Path]
  private val generatedIgnoringCase = scala.collection.mutable.Set.empty[String]

  /**
   * Generates a file by writing string contents to the file system. The file is written only when its content
   * differs from what is on disk: an unchanged file keeps its modification time, so the compiler does not
   * recompile the module.
   *
   * @param baseDir The base directory, e.g. src-gen
   * @param dirName The directory relative to baseDir, e.g. main/java
   * @param fileName The file name within baseDir/dirName
   * @param contents The string contents of the file
   * @param charset The charset, by default UTF-8
   */
  def genFile(baseDir: String, dirName: String, fileName: String)(contents: => String)(implicit charset: Charset = StandardCharsets.UTF_8): Unit = {
    val file = Paths.get(baseDir, dirName, fileName).toAbsolutePath.normalize
    if (!generatedIgnoringCase.add(file.toString.toLowerCase(java.util.Locale.ROOT))) {
      throw new IllegalStateException(s"$file is generated twice (file names are compared ignoring case)")
    }
    val bytes = contents.getBytes(charset)
    // On a case-insensitive file system, a class renamed by case only finds its old file under the old name:
    // delete it, so the file is written under the new name.
    if (Files.exists(file) && file.toRealPath().getFileName.toString != fileName) {
      Files.delete(file)
    }
    if (!Files.isRegularFile(file) || !java.util.Arrays.equals(Files.readAllBytes(file), bytes)) {
      Files.createDirectories(file.getParent)
      Files.write(file, bytes)
    }
    generated.add(file.toRealPath())
  }

  /**
   * Deletes the files under `root` that this run did not generate, then the directories left empty.
   * Called once every file has been generated.
   */
  def deleteStaleFiles(root: String): Unit = {
    val rootPath = Paths.get(root).toAbsolutePath.normalize
    if (Files.isDirectory(rootPath)) {
      val stream = Files.walk(rootPath)
      val paths = try stream.iterator.asScala.toList finally stream.close()
      paths.filter(p => Files.isRegularFile(p) && !generated.contains(p.toRealPath())).foreach(Files.delete)
      paths.filter(Files.isDirectory(_)).sortBy(p => -p.getNameCount).foreach { dir =>
        val entries = Files.list(dir)
        val empty = try !entries.iterator.hasNext finally entries.close()
        if (empty) {
          Files.delete(dir)
        }
      }
    }
  }

  implicit class IntExtensions(i: Int) {

    // returns i as ordinal, i.e. 1st, 2nd, 3rd, 4th, ...
    def ordinal: String =
      s"$i" + (if (i >= 4 && i <= 20) {
        "th"
      } else {
        i % 10 match {
          case 1 => "st"
          case 2 => "nd"
          case 3 => "rd"
          case _ => "th"
        }
      })
  }

  implicit class BooleanExtensions(condition: Boolean) {
    def gen(s: => String): String =  if (condition) s else ""
  }

  /**
   * Generates a String based on ints within a specific range.
   * {{{
   * (1 to 3).gen(i => s"x$i")(", ") // x1, x2, x3
   * (1 to 3).reverse.gen(i -> s"x$i")(", ") // x3, x2, x1
   * }}}
   * @param range A Range
   */
  implicit class RangeExtensions(range: Range) {
    def gen(f: Int => String = String.valueOf)(implicit delimiter: String = ""): String =
      range map f mkString delimiter
  }

  /**
   * Provides StringContext extensions, e.g. indentation of cascaded rich strings.
   * @param sc Current StringContext
   * @see <a href="https://gist.github.com/danieldietrich/5174348">this gist</a>
   */
  implicit class StringContextExtensions(sc: StringContext) {

    import scala.util.Properties.lineSeparator

    /**
     * Formats escaped strings.
     * @param args StringContext parts
     * @return An aligned String
     */
    def xs(args: Any*): String = align(sc.s, args)

    /**
     * Formats raw/unescaped strings.
     * @param args StringContext parts
     * @return An aligned String
     */
    def xraw(args: Any*): String = align(sc.raw, args)

    /**
     * Indenting a rich string, removing first and last newline.
     * A rich string consists of arguments surrounded by text parts.
     */
    private def align(interpolator: Seq[Any] => String, args: Seq[Any]): String = {

      // indent embedded strings, invariant: parts.length = args.length + 1
      val indentedArgs = for {
        (part, arg) <- sc.parts zip args.map(s => if (s == null) "" else s.toString)
      } yield {
        // get the leading space of last line of current part
        val space = """([ \t]*)[^\s]*$""".r.findFirstMatchIn(part).map(_.group(1)).getOrElse("")
        // add this leading space to each line (except the first) of current arg
        arg.split("\r?\n") match {
          case lines: Array[String] if lines.nonEmpty => lines reduce (_ + lineSeparator + space + _)
          case whitespace => whitespace mkString ""
        }
      }

      // remove first and last newline and split string into separate lines
      // adding termination symbol \u0000 in order to preserve empty strings between last newlines when splitting
      val split = (interpolator(indentedArgs).replaceAll( """(^[ \t]*\r?\n)|(\r?\n[ \t]*$)""", "") + '\u0000').split("\r?\n")

      // find smallest indentation
      val prefix = split filter (!_.trim().isEmpty) map { s =>
        """^\s+""".r.findFirstIn(s).getOrElse("")
      } match {
        case prefixes: Array[String] if prefixes.length > 0 => prefixes reduce { (s1, s2) =>
          if (s1.length <= s2.length) s1 else s2
        }
        case _ => ""
      }

      // align all lines
      val aligned = split map { s =>
        if (s.startsWith(prefix)) s.substring(prefix.length) else s
      } mkString lineSeparator dropRight 1 // dropping termination character \u0000

      // combine multiple newlines to two
      aligned.replaceAll("""[ \t]*\r?\n ([ \t]*\r?\n)+""", lineSeparator * 2)
    }
  }
}
