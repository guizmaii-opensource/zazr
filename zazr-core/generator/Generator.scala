import Generator._
import JavaGenerator._

import collection.immutable.ListMap
import scala.language.implicitConversions

val N = 8
val VARARGS = 10
val TARGET_MAIN = s"${project.getBasedir()}/src-gen/main/java"
val TARGET_TEST = s"${project.getBasedir()}/src-gen/test/java"
val CHARSET = java.nio.charset.StandardCharsets.UTF_8
val comment = "//"
val javadoc = "**"

/**
 * The implicit bound every generated type-parameter *declaration* must carry.
 *
 * The com.guizmaii.zazr packages are `@NullMarked`, which makes a bare `<T>` mean `<T extends Object>`
 * (non-null). Vavr containers deliberately accept null elements, so declarations widen to
 * `<T extends @Nullable Object>`. Type-parameter *usages* are unaffected.
 */
val nullableBound = "extends @Nullable Object"

/**
 * Pre-computed type variable strings for a given arity.
 *
 * Centralizes the repeated patterns like "T1, T2, T3", "<T1, T2, T3, R>", "t1, t2, t3", etc.
 * that are rebuilt identically throughout the generator.
 *
 * @param i the arity (number of type parameters), 0 to N
 */
case class Arity(i: Int) {
  val generics: String = (1 to i).gen(j => s"T$j")(using ", ") // "T1, T2, T3"
  val genericsTuple: String = if (i > 0) s"<$generics>" else "" // "<T1, T2, T3>" or "" for arity 0
  val fullGenerics: String = s"<${(i > 0).gen(s"$generics, ")}R>" // "<T1, T2, T3, R>"
  val genericsDecl: String = (1 to i).gen(j => s"T$j $nullableBound")(using ", ") // "T1 extends @Nullable Object, ..."
  val genericsTupleDecl: String = if (i > 0) s"<$genericsDecl>" else "" // "<T1 extends @Nullable Object, ...>"
  val fullGenericsDecl: String = s"<${(i > 0).gen(s"$genericsDecl, ")}R $nullableBound>" // "<T1 ..., R extends @Nullable Object>"
  val wideGenerics: String = (1 to i).gen(j => s"? super T$j")(using ", ") // "? super T1, ? super T2"
  val covariantGenerics: String = (1 to i).gen(j => s"? extends T$j")(using ", ") // "? extends T1, ? extends T2"
  val fullWideGenerics: String = s"<${(i > 0).gen(s"$wideGenerics, ")}? extends R>" // "<? super T1, ? super T2, ? extends R>"
  val genericsReversed: String = (1 to i).reverse.gen(j => s"T$j")(using ", ") // "T3, T2, T1"
  val genericsFunction: String = if (i > 0) s"$generics, " else "" // "T1, T2, T3, " or "" for arity 0 (trailing comma for prepending to R)
  val genericsReversedFunction: String = if (i > 0) s"$genericsReversed, " else "" // "T3, T2, T1, " or "" for arity 0 (trailing comma)
  val paramsDecl: String = (1 to i).gen(j => s"T$j t$j")(using ", ") // "T1 t1, T2 t2, T3 t3"
  val params: String = (1 to i).gen(j => s"t$j")(using ", ") // "t1, t2, t3"
  val paramsReversed: String = (1 to i).reverse.gen(j => s"t$j")(using ", ") // "t3, t2, t1"
  val tupled: String = (1 to i).gen(j => s"t._$j()")(using ", ") // "t._1(), t._2(), t._3()"
  val underscoreParams: String = (1 to i).gen(j => s"_$j")(using ", ") // "_1, _2, _3"

  /** Generates @param javadoc tags for type parameters T1..Ti */
  def typeParamDocs(description: Int => String = j => s"type of the ${j.ordinal} element"): String =
    (0 to i).gen(j => if (j == 0) "*" else s"* @param <T$j> ${description(j)}")(using "\n")
}

/**
 * Returns the standard java.util.function type name for the given arity.
 * Arities 0, 1, and 2 map to Supplier, Function, and BiFunction respectively;
 * higher arities map to Vavr's FunctionN.
 */
def javaFunctionType(i: Int, im: ImportManager): String = i match {
  case 0 => im.getType("java.util.function.Supplier")
  case 1 => im.getType("java.util.function.Function")
  case 2 => im.getType("java.util.function.BiFunction")
  case _ => s"Function$i"
}

def run(): Unit = {
  generateMainClasses()
  generateTestClasses()
}

/**
 * Generate Vavr src-gen/main/java classes
 */
def generateMainClasses(): Unit = {

  genFunctions()
  genTuples()
  genArrayTypes()

  /**
   * Generator of Functions
   */
  def genFunctions(): Unit = {

    // JDK functional interfaces first (docs/design.md 3.1): Function0..2 and CheckedFunction0 are adapters
    // over java.util.function.Supplier/Function/BiFunction and java.util.concurrent.Callable respectively, so
    // they are not generated. Function3..8 and CheckedFunction1..8 stay: the JDK has nothing at those arities,
    // or with checked exceptions.
    (1 to N).foreach(i => {

      genVavrFile("com.guizmaii.zazr", s"CheckedFunction$i")(genFunction("CheckedFunction", checked = true))
      if (i >= 3) genVavrFile("com.guizmaii.zazr", s"Function$i")(genFunction("Function", checked = false))

      def genFunction(name: String, checked: Boolean)(im: ImportManager, packageName: String, className: String): String = {

        val a = Arity(i)
        import a.{generics, genericsDecl, fullGenerics, fullGenericsDecl, wideGenerics, fullWideGenerics, genericsReversed, genericsTuple, genericsFunction, genericsReversedFunction, paramsDecl, params, paramsReversed, tupled}
        val genericsOptionReturnType = s"<${genericsFunction}${im.getType("com.guizmaii.zazr.control.Option")}<R>>"
        val genericsTryReturnType = s"<${genericsFunction}${im.getType("com.guizmaii.zazr.control.Try")}<R>>"
        val curried = if (i == 0) "v" else (1 to i).gen(j => s"t$j")(using " -> ")
        val compositionType = if(checked) "CheckedFunction1" else im.getType("java.util.function.Function")

        // imports

        val Objects = im.getType("java.util.Objects")
        val Try = if (checked) im.getType("com.guizmaii.zazr.control.Try") else ""
        val additionalExtends = (checked, i) match {
          case (false, 0) => im.getType("java.util.function.Supplier") + "<R>"
          case (false, 1) => im.getType("java.util.function.Function") + "<T1, R>"
          case (false, 2) => im.getType("java.util.function.BiFunction") + "<T1, T2, R>"
          case _ => ""
        }
        val extendsClause = if (additionalExtends.isEmpty) "" else s"extends $additionalExtends"
        def fullGenericsTypeF(checked: Boolean, i: Int): String = (checked, i) match {
          case (true, _) => im.getType(s"com.guizmaii.zazr.CheckedFunction$i") + fullWideGenerics
          case (false, 0) => im.getType("java.util.function.Supplier") + "<? extends R>"
          case (false, 1) => im.getType("java.util.function.Function") + "<? super T1, ? extends R>"
          case (false, 2) => im.getType("java.util.function.BiFunction") + "<? super T1, ? super T2, ? extends R>"
          case (false, _) => im.getType(s"com.guizmaii.zazr.Function$i") + fullWideGenerics
        }
        val fullGenericsType = fullGenericsTypeF(checked, i)
        val refApply = i match {
          case 0 => "get"
          case _ => "apply"
        }
        val callApply = s"$refApply($params)"

        // Currying peels off one argument at a time: every intermediate step is a plain (non-throwing) function,
        // since only the final application can invoke the underlying function and throw. Only the last step is a
        // CheckedFunction1 when this is a checked function; every step is the JDK Function otherwise.
        def curriedType(max: Int, function: String, idx: Int = 1): String = max match {
          case 0 => s"$className<R>"
          case 1 => if (checked) s"${function}1<T$idx, R>" else s"${javaFunctionType(1, im)}<T$idx, R>"
          case _ => s"${javaFunctionType(1, im)}<T$idx, ${curriedType(max - 1, function, idx + 1)}>"
        }

        def arguments(count: Int): String = count match {
          case 0 => "no arguments"
          case 1 => "one argument"
          case 2 => "two arguments"
          case 3 => "three arguments"
          case _ => s"$i arguments"
        }

        // lift() needs both regardless of checked-ness (a fatal throwable from an unchecked partialFunction
        // must propagate too); unchecked()/recover() (checked only) reuse the same imports.
        im.getStatic("com.guizmaii.zazr.internal.Throwables.sneakyThrow")
        im.getStatic("com.guizmaii.zazr.internal.Throwables.isFatal")

        xs"""
          /**
           * Represents a function with ${arguments(i)}.
           ${(0 to i).gen(j => if (j == 0) "*" else s"* @param <T$j> argument $j of the function")(using "\n")}
           * @param <R> return type of the function
           * @author Daniel Dietrich
           */
          @FunctionalInterface
          public interface $className$fullGenericsDecl $extendsClause {

              /$javadoc
               * Returns a function that always returns the constant
               * value that you give in parameter.
               *
               ${(1 to i).gen(j => s"* @param <T$j> generic parameter type $j of the resulting function")(using "\n")}
               * @param <R> the result type
               * @param value the value to be returned
               * @return a function always returning the given value
               */
              static $fullGenericsDecl $className$fullGenerics constant(R value) {
                  return ($params) -> value;
              }

              /$javadoc
               * Creates a {@code $className} based on
               * <ul>
               * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">method reference</a></li>
               * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html#syntax">lambda expression</a></li>
               * </ul>
               *
               * Examples (w.l.o.g. referring to $className):
               * <pre>{@code // using a lambda expression
               * $className$fullGenerics add1 = $className.of((${(1 to i).gen(j => s"t$j")(using ", ")}) -> ${(1 to i).gen(j => s"t$j")(using " + ")});
               *
               * // using a method reference
               * $className$fullGenerics add2 = $className.of(this::method);
               *
               * // using a lambda reference
               * $className$fullGenerics add3 = $className.of(add1::apply);
               * }</pre>
               *
               * @param methodReference (typically) a method reference, e.g. {@code Type::method}
               ${(0 to i).gen(j => if (j == 0) "* @param <R> return type" else s"* @param <T$j> ${j.ordinal} argument")(using "\n")}
               * @return a {@code $className}
               */
              static $fullGenericsDecl $className$fullGenerics of($className$fullGenerics methodReference) {
                  return methodReference;
              }

              /$javadoc
               * Lifts the given {@code partialFunction} into a function that returns an {@code Option} result.
               *
               * @param partialFunction a function that is not defined for all values of the domain (e.g. by throwing)
               ${(0 to i).gen(j => if (j == 0) "* @param <R> return type" else s"* @param <T$j> ${j.ordinal} argument")(using "\n")}
               * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Some(result)}
               *         if the function is defined for the given arguments, and {@code None} if it throws a non-fatal
               *         throwable. Fatal throwables (see {@link ${im.getType("com.guizmaii.zazr.control.Try")}}) are rethrown
               *         instead of being turned into {@code None}.
               */
              static $fullGenericsDecl ${javaFunctionType(i, im)}$genericsOptionReturnType lift($fullGenericsType partialFunction) {
                  ${
                    val lambdaArgs = if (i == 1) params else s"($params)"
                    xs"""
                      return $lambdaArgs -> {
                          try {
                              final R result = partialFunction.apply($params);
                              return result == null ? ${im.getType("com.guizmaii.zazr.control.Option")}.<R>none() : ${im.getType("com.guizmaii.zazr.control.Option")}.some(result);
                          } catch (Throwable t) {
                              if (isFatal(t)) {
                                  return sneakyThrow(t);
                              }
                              return ${im.getType("com.guizmaii.zazr.control.Option")}.<R>none();
                          }
                      };
                    """
                  }
              }

              /$javadoc
               * Lifts the given {@code partialFunction} into a function that returns a {@code Try} result.
               *
               * @param partialFunction a function that is not defined for all values of the domain (e.g. by throwing)
               ${(0 to i).gen(j => if (j == 0) "* @param <R> return type" else s"* @param <T$j> ${j.ordinal} argument")(using "\n")}
               * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Success(result)}
               *         if the function is defined for the given arguments, and {@code Failure(throwable)} if it throws a
               *         non-fatal throwable. Fatal throwables (see {@link ${im.getType("com.guizmaii.zazr.control.Try")}}) are rethrown
               *         instead of being wrapped.
               */
              static $fullGenericsDecl ${javaFunctionType(i, im)}$genericsTryReturnType liftTry($fullGenericsType partialFunction) {
                  ${
                    val supplier = s"() -> partialFunction.apply($params)"
                    val lambdaArgs = if (i == 1) params else s"($params)"
                    xs"""
                      return $lambdaArgs -> ${im.getType("com.guizmaii.zazr.control.Try")}.of($supplier);
                    """
                  }
              }

              /$javadoc
               * Narrows the given {@code $className$fullWideGenerics} to {@code $className$fullGenerics}
               *
               * @param f A {@code $className}
               ${(0 to i).gen(j => if (j == 0) "* @param <R> return type" else s"* @param <T$j> ${j.ordinal} argument")(using "\n")}
               * @return the given {@code f} instance as narrowed type {@code $className$fullGenerics}
               */
              @SuppressWarnings("unchecked")
              static $fullGenericsDecl $className$fullGenerics narrow($className$fullWideGenerics f) {
                  return ($className$fullGenerics) f;
              }

              ${(i == 1).gen(xs"""
                /$javadoc
                 * Returns the identity $className, i.e. the function that returns its input.
                 *
                 * @param <T> argument type (and return type) of the identity function
                 * @return the identity $className
                 */
                static <T $nullableBound> ${name}1<T, T> identity() {
                    return t -> t;
                }
              """)}

              /$javadoc
               * Applies this function to ${arguments(i)} and returns the result.
               ${(0 to i).gen(j => if (j == 0) "*" else s"* @param t$j argument $j")(using "\n")}
               * @return the result of function application
               * ${checked.gen("@throws Exception if something goes wrong applying this function to the given arguments")}
               */
              R apply($paramsDecl)${checked.gen(" throws Exception")};

              ${(1 until i).gen(j => {
                val remaining = i - j
                // The partial application's result type: the kept ${name}$remaining when the JDK has nothing
                // at that arity (remaining >= 3, or this is a checked function, since CheckedFunction1..8 all
                // stay), otherwise the JDK Supplier/Function/BiFunction at that arity.
                val resultType = if (checked || remaining >= 3) s"$name$remaining" else javaFunctionType(remaining, im)
                val partialApplicationArgs = (1 to j).gen(k => s"T$k t$k")(using ", ")
                val resultFunctionGenerics = (j+1 to i).gen(k => s"T$k")(using ", ")
                val resultFunctionArgs = (j+1 to i).gen(k => s"T$k t$k")(using ", ")
                val fixedApplyArgs = (1 to j).gen(k => s"t$k")(using ", ")
                val variableApplyArgs = (j+1 to i).gen(k => s"t$k")(using ", ")
                xs"""
                  /$javadoc
                   * Applies this function partially to ${j.numerus("argument")}.
                   *
                   ${(1 to j).gen(k => s"* @param t$k argument $k")(using "\n")}
                   * @return a partial application of this function
                   */
                  default $resultType<$resultFunctionGenerics, R> apply($partialApplicationArgs) {
                      return ($resultFunctionArgs) -> apply($fixedApplyArgs, $variableApplyArgs);
                  }
                """
              })(using "\n\n")}

              ${(i == 0 && !checked).gen(
                xs"""
                  /$javadoc
                   * Implementation of {@linkplain java.util.function.Supplier#get()}, just calls {@linkplain #apply()}.
                   *
                   * @return the result of {@code apply()}
                   */
                  @Override
                  default R get() {
                      return apply();
                  }
                """
              )}

              /**
               * Returns a curried version of this function.
               *
               * @return a curried function equivalent to this.
               */
              default ${curriedType(i, name)} curried() {
                  return ${if (i < 2) "this" else s"$curried -> apply($params)"};
              }

              /**
               * Returns a tupled version of this function.
               *
               * @return a tupled function equivalent to this.
               */
              default ${if (checked) s"${name}1" else javaFunctionType(1, im)}<Tuple$i$genericsTuple, R> tupled() {
                  return t -> apply($tupled);
              }


              ${checked.gen(xs"""
                /$javadoc
                 * Return a composed function that first applies this $className to the given arguments and in case of a
                 * non-fatal throwable tries to get a value from the {@code recover} function with the throwable information.
                 * A fatal throwable (see {@link ${im.getType("com.guizmaii.zazr.control.Try")}}) is never handed to
                 * {@code recover}: it propagates unchanged instead.
                 *
                 * @param recover the function applied in case of a non-fatal throwable
                 * @return a function composed of this and recover
                 * @throws NullPointerException if recover is null
                 */
                default ${javaFunctionType(i, im)}$fullGenerics recover(${im.getType("java.util.function.Function")}<? super Throwable, ? extends ${fullGenericsTypeF(checked = false, i)}> recover) {
                    Objects.requireNonNull(recover, "recover is null");
                    return ($params) -> {
                        try {
                            return this.apply($params);
                        } catch (Throwable throwable) {
                            if (isFatal(throwable)) {
                                return sneakyThrow(throwable);
                            }
                            final ${fullGenericsTypeF(checked = false, i)} func = recover.apply(throwable);
                            Objects.requireNonNull(func, () -> "recover return null for " + throwable.getClass() + ": " + throwable.getMessage());
                            return func.$callApply;
                        }
                    };
                }

                /$javadoc
                 * Returns an unchecked function that will <em>sneaky throw</em> if an exceptions occurs when applying the function.
                 *
                 * @return a new unchecked function that throws a {@code Throwable}.
                 */
                default ${javaFunctionType(i, im)}$fullGenerics unchecked() {
                    return ($params) -> {
                        try {
                            return apply($params);
                        } catch(Throwable t) {
                            return sneakyThrow(t);
                        }
                    };
                }
              """)}

              /$javadoc
               * Returns a composed function that first applies this $className${if (i == 0) "" else if (i == 1) " to the given argument" else " to the given arguments"} and then applies
               * {@linkplain $compositionType} {@code after} to the result.
               *
               * @param <V> return type of after
               * @param after the function applied after this
               * @return a function composed of this and after
               * @throws NullPointerException if after is null
               */
              default <V $nullableBound> $className<${genericsFunction}V> andThen($compositionType<? super R, ? extends V> after) {
                  $Objects.requireNonNull(after, "after is null");
                  return ($params) -> after.apply(apply($params));
              }

              ${(i == 1).gen(xs"""
                /$javadoc
                 * Returns a composed function that first applies the {@linkplain $compositionType} {@code before} the
                 * given argument and then applies this $className to the result.
                 *
                 * @param <V> argument type of before
                 * @param before the function applied before this
                 * @return a function composed of before and this
                 * @throws NullPointerException if before is null
                 */
                default <V $nullableBound> ${name}1<V, R> compose($compositionType<? super V, ? extends T1> before) {
                    $Objects.requireNonNull(before, "before is null");
                    return v -> apply(before.apply(v));
                }
              """)}

              ${(1 to i).gen(j => {
                val fName = s"before"
                val fGeneric = "S"
                val applicationArgs = (1 to i).gen(k => if (k == j) s"$fGeneric ${fGeneric.toLowerCase}" else s"T$k t$k")(using ", ")
                val generics = (1 to i).gen(k => if (k == j) "S" else s"T$k")(using ", ")
                val resultFunctionArgs = (j+1 to i).gen(k => s"T$k t$k")(using ", ")
                val applyArgs = (1 to i).gen(k => if (k ==j) s"$fName.apply(${fGeneric.toLowerCase})" else s"t$k")(using ", ")
                val variableApplyArgs = (j+1 to i).gen(k => s"t$k")(using ", ")
                val docAdd = i match
                  case 1 => ""
                  case 2 => " and the other argument"
                  case _ => " and the other arguments"
                xs"""
                  /$javadoc
                   * Returns a composed function that first applies the {@linkplain Function} {@code $fName} to the
                   * ${j.ordinal} argument and then applies this $className to the result$docAdd.
                   *
                   * @param <$fGeneric> argument type of $fName
                   * @param $fName the function applied before this
                   * @return a function composed of $fName and this
                   * @throws NullPointerException if $fName is null
                   */
                  default <S $nullableBound> $className<$generics, R> compose$j(${javaFunctionType(1, im)}<? super $fGeneric, ? extends T$j> $fName) {
                      Objects.requireNonNull($fName, "$fName is null");
                      return ($applicationArgs) -> apply($applyArgs);
                  }
                """
              })(using "\n\n")}
          }
        """
      }
    })
  }

  /**
   * Generator of com.guizmaii.zazr.Tuple*
   */
  def genTuples(): Unit = {

    genVavrFile("com.guizmaii.zazr", "Tuple")(genBaseTuple)

    (0 to N).foreach { i =>
      genVavrFile("com.guizmaii.zazr", s"Tuple$i")(genTuple(i))
    }

    /*
     * Generates Tuple1..N
     */
    def genTuple(i: Int)(im: ImportManager, packageName: String, className: String): String = {
      val a = Arity(i)
      val generics = a.genericsTuple
      val genericsDecl = a.genericsTupleDecl
      val paramsDecl = a.paramsDecl
      val params = a.underscoreParams
      val paramTypes = a.wideGenerics
      val resultGenerics = if (i == 0) "" else s"<${(1 to i).gen(j => s"U$j")(using ", ")}>"
      val resultGenericsDecl = if (i == 0) "" else s"<${(1 to i).gen(j => s"U$j $nullableBound")(using ", ")}>"
      val mapResult = i match {
        case 0 => ""
        case 1 => "? extends U1"
        case _ => s"Tuple$i<${(1 to i).gen(j => s"U$j")(using ", ")}>"
      }
      val comparableGenerics = if (i == 0) "" else s"<${(1 to i).gen(j => s"U$j extends Comparable<? super U$j>")(using ", ")}>"
      val untyped = if (i == 0) "" else s"<${(1 to i).gen(j => "?")(using ", ")}>"
      val functionType = javaFunctionType(i, im)
      val Comparator = im.getType("java.util.Comparator")
      val Objects = im.getType("java.util.Objects")
      val Seq = im.getType("com.guizmaii.zazr.collection.Seq")
      val List = im.getType("com.guizmaii.zazr.collection.List")
      if(i==2){
        im.getType("java.util.Map")
        im.getType("java.util.AbstractMap")
      }

      val components = (1 to i).gen(j => s"T$j _$j")(using ", ")

      xs"""
        /**
         * A tuple of ${i.numerus("element")} which can be seen as cartesian product of ${i.numerus("component")}.
         * <p>
         * A record: {@code equals} and {@code hashCode} are structural, the components are read with
         * {@code _1()}, {@code _2()}, ... and a tuple is deconstructed with a record pattern, e.g.
         * {@code case Tuple2(var a, var b)}. Components may be null.
         ${(0 to i).gen(j => if (j == 0) "*" else s"* @param <T$j> type of the ${j.ordinal} element")(using "\n")}
         ${(0 to i).gen(j => if (j == 0) "*" else s"* @param _$j the ${j.ordinal} element")(using "\n")}
         * @author Daniel Dietrich
         */
        public record $className$genericsDecl($components) implements Tuple, Comparable<$className$generics> {

            ${(i == 0).gen(xs"""
              /$javadoc
               * The singleton instance of Tuple0.
               */
              private static final Tuple0 INSTANCE = new Tuple0();

              /$javadoc
               * The singleton Tuple0 comparator.
               */
              private static final Comparator<Tuple0> COMPARATOR = (t1, t2) -> 0;

              /$javadoc
               * Returns the singleton instance of Tuple0.
               * <p>
               * {@code new Tuple0()} is legal (a record constructor is public) but every {@code Tuple0}
               * equals every other, so the singleton is only an allocation saving.
               *
               * @return The singleton instance of Tuple0.
               */
              public static Tuple0 instance() {
                  return INSTANCE;
              }
            """)}

            public static $genericsDecl $Comparator<$className$generics> comparator(${(1 to i).gen(j => s"$Comparator<? super T$j> t${j}Comp")(using ", ")}) {
                ${if (i == 0) xs"""
                  return COMPARATOR;
                """ else xs"""
                  return (t1, t2) -> {
                      ${(1 to i).gen(j => xs"""
                        final int check$j = t${j}Comp.compare(t1._$j(), t2._$j());
                        if (check$j != 0) {
                            return check$j;
                        }
                      """)(using "\n\n")}

                      // all components are equal
                      return 0;
                  };
                """}
            }

            ${(i > 0).gen(xs"""
              @SuppressWarnings("unchecked")
              private static $comparableGenerics int compareTo($className$untyped o1, $className$untyped o2) {
                  final $className$resultGenerics t1 = ($className$resultGenerics) o1;
                  final $className$resultGenerics t2 = ($className$resultGenerics) o2;

                  ${(1 to i).gen(j => xs"""
                    final int check$j = t1._$j().compareTo(t2._$j());
                    if (check$j != 0) {
                        return check$j;
                    }
                  """)(using "\n\n")}

                  // all components are equal
                  return 0;
              }
            """)}

            @Override
            public int arity() {
                return $i;
            }

            @Override
            public int compareTo($className$generics that) {
                ${if (i == 0) xs"""
                  Objects.requireNonNull(that, "that is null");
                  return 0;
                """ else xs"""
                  return $className.compareTo(this, that);
                """}
            }

            ${(1 to i).gen(j => xs"""
              /$javadoc
               * Returns a copy of this tuple with the ${j.ordinal} element replaced by the given {@code value}.
               *
               * @param value the new value
               * @return a copy of this tuple with a new value for the ${j.ordinal} element of this Tuple.
               */
              public $className$generics update$j(T$j value) {
                  return new $className<>(${(1 until j).gen(k => s"_$k")(using ", ")}${(j > 1).gen(", ")}value${(j < i).gen(", ")}${((j + 1) to i).gen(k => s"_$k")(using ", ")});
              }
            """)(using "\n\n")}

            ${(i == 2).gen(xs"""
              /$javadoc
               * Swaps the elements of this {@code Tuple}.
               *
               * @return A new Tuple where the first element is the second element of this Tuple
               *   and the second element is the first element of this Tuple.
               */
              public Tuple2<T2, T1> swap() {
                  return Tuple.of(_2, _1);
              }

              /$javadoc
               * Converts the tuple to java.util.Map.Entry {@code Tuple}.
               *
               * @return A  java.util.Map.Entry where the first element is the key and the second
               * element is the value.
               */
              public Map.Entry$generics toEntry() {
                  return new AbstractMap.SimpleEntry<>(_1, _2);
              }

            """)}

            ${(i > 0).gen(xs"""
              /$javadoc
               * Maps the components of this tuple using a mapper function.
               *
               * @param mapper the mapper function
               ${(1 to i).gen(j => s"* @param <U$j> new type of the ${j.ordinal} component")(using "\n")}
               * @return ${if (i == 1) "A new Tuple of same arity." else "the result of applying {@code mapper} to the components of this tuple"}
               * @throws NullPointerException if {@code mapper} is null
               */
              public $resultGenericsDecl $className$resultGenerics map($functionType<$paramTypes, $mapResult> mapper) {
                  Objects.requireNonNull(mapper, "mapper is null");
                  ${if (i == 1)
                    "return Tuple.of(mapper.apply(_1));"
                  else
                    s"return mapper.apply($params);"
                  }
              }
            """)}

            ${(i > 1).gen(xs"""
              /$javadoc
               * Maps the components of this tuple using a mapper function for each component.
               ${(0 to i).gen(j => if (j == 0) "*" else s"* @param f$j the mapper function of the ${j.ordinal} component")(using "\n")}
               ${(1 to i).gen(j => s"* @param <U$j> new type of the ${j.ordinal} component")(using "\n")}
               * @return A new Tuple of same arity.
               * @throws NullPointerException if one of the arguments is null
               */
              public $resultGenericsDecl $className$resultGenerics map(${(1 to i).gen(j => s"${im.getType("java.util.function.Function")}<? super T$j, ? extends U$j> f$j")(using ", ")}) {
                  ${(1 to i).gen(j => s"""Objects.requireNonNull(f$j, "f$j is null");""")(using "\n")}
                  return ${im.getType("com.guizmaii.zazr.Tuple")}.of(${(1 to i).gen(j => s"f$j.apply(_$j)")(using ", ")});
              }
            """)}

            ${(i > 1) `gen` (1 to i).gen(j => xs"""
              /$javadoc
               * Maps the ${j.ordinal} component of this tuple to a new value.
               *
               * @param <U> new type of the ${j.ordinal} component
               * @param mapper A mapping function
               * @return a new tuple based on this tuple and substituted ${j.ordinal} component
               */
              public <U $nullableBound> $className<${(1 to i).gen(k => if (j == k) "U" else s"T$k")(using ", ")}> map$j(${im.getType("java.util.function.Function")}<? super T$j, ? extends U> mapper) {
                  Objects.requireNonNull(mapper, "mapper is null");
                  final U u = mapper.apply(_$j);
                  return Tuple.of(${(1 to i).gen(k => if (j == k) "u" else s"_$k")(using ", ")});
              }
            """)(using "\n\n")}

            /**
             * Transforms this tuple to an object of type U.
             *
             * @param f Transformation which creates a new object of type U based on this tuple's contents.
             * @param <U> type of the transformation result
             * @return An object of type U
             * @throws NullPointerException if {@code f} is null
             */
            ${if (i == 0) xs"""
              public <U $nullableBound> U apply($functionType<? extends U> f) {
                  $Objects.requireNonNull(f, "f is null");
                  return f.get();
              }
            """ else xs"""
              public <U $nullableBound> U apply($functionType<$paramTypes, ? extends U> f) {
                  $Objects.requireNonNull(f, "f is null");
                  return f.apply($params);
              }
            """}

            @Override
            public $Seq<?> toSeq() {
                ${if (i == 0) xs"""
                  return $List.empty();
                """ else xs"""
                  return $List.of($params);
                """}
            }

            ${(i < N).gen(xs"""
              /$javadoc
               * Append a value to this tuple.
               *
               * @param <T${i+1}> type of the value to append
               * @param t${i+1} the value to append
               * @return a new Tuple with the value appended
               */
              public <T${i+1} $nullableBound> Tuple${i+1}<${(1 to i+1).gen(j => s"T$j")(using ", ")}> append(T${i+1} t${i+1}) {
                  return ${im.getType("com.guizmaii.zazr.Tuple")}.of(${(1 to i).gen(k => s"_$k")(using ", ")}${(i > 0).gen(", ")}t${i+1});
              }
            """)}

            ${(i < N) `gen` (1 to N-i).gen(j => xs"""
              /$javadoc
               * Concat a tuple's values to this tuple.
               *
               ${(i+1 to i+j).gen(k => s"* @param <T$k> the type of the ${k.ordinal} value in the tuple")(using "\n")}
               * @param tuple the tuple to concat
               * @return a new Tuple with the tuple values appended
               * @throws NullPointerException if {@code tuple} is null
               */
              public <${(i+1 to i+j).gen(k => s"T$k " + nullableBound)(using ", ")}> Tuple${i+j}<${(1 to i+j).gen(k => s"T$k")(using ", ")}> concat(Tuple$j<${(i+1 to i+j).gen(k => s"T$k")(using ", ")}> tuple) {
                  Objects.requireNonNull(tuple, "tuple is null");
                  return ${im.getType("com.guizmaii.zazr.Tuple")}.of(${(1 to i).gen(k => s"_$k")(using ", ")}${(i > 0).gen(", ")}${(1 to j).gen(k => s"tuple._$k()")(using ", ")});
              }
            """)(using "\n\n")}

            // -- Object: equals and hashCode are the record's; toString keeps the Scala-like "(a, b)" form

            @Override
            public String toString() {
                return ${if (i == 0) "\"()\"" else s""""(" + ${(1 to i).gen(j => s"_$j")(using " + \", \" + ")} + ")""""};
            }

        }
      """
    }

    /*
     * Generates Tuple
     */
    def genBaseTuple(im: ImportManager, packageName: String, className: String): String = {

      val Map = im.getType("java.util.Map")
      val Objects = im.getType("java.util.Objects")
      val Seq = im.getType("com.guizmaii.zazr.collection.Seq")

      def genFactoryMethod(i: Int) = {
        val a = Arity(i)
        import a.{generics, genericsDecl, paramsDecl, params, covariantGenerics => wideGenerics}
        xs"""
          /**
           * Creates a tuple of ${i.numerus("element")}.
           ${(0 to i).gen(j => if (j == 0) "*" else s"* @param <T$j> type of the ${j.ordinal} element")(using "\n")}
           ${(1 to i).gen(j => s"* @param t$j the ${j.ordinal} element")(using "\n")}
           * @return a tuple of ${i.numerus("element")}.
           */
          static <$genericsDecl> Tuple$i<$generics> of($paramsDecl) {
              return new Tuple$i<>($params);
          }
        """
      }

      def genHashMethod(i: Int) = {
        val paramsDecl = (1 to i).gen(j => s"@Nullable Object o$j")(using ", ")
        xs"""
          /**
           * Return the order-dependent hash of the ${i.numerus("given value")}.
           ${(0 to i).gen(j => if (j == 0) "*" else s"* @param o$j the ${j.ordinal} value to hash")(using "\n")}
           * @return the same result as {@link $Objects#${if (i == 1) "hashCode(Object)" else "hash(Object...)"}}
           */
          static int hash($paramsDecl) {
              ${if (i == 1) {
                s"return $Objects.hashCode(o1);"
              } else {
                xs"""
                  int result = 1;
                  ${(1 to i).gen(j => s"result = 31 * result + hash(o$j);")(using "\n")}
                  return result;
                """
              }}
          }
        """
      }

      def genNarrowMethod(i: Int) = {
        val a = Arity(i)
        import a.{generics, genericsDecl, covariantGenerics => wideGenerics}
        xs"""
          /**
           * Narrows a widened {@code Tuple$i<$wideGenerics>} to {@code Tuple$i<$generics>}.
           * This is eligible because immutable/read-only tuples are covariant.
           * @param t A {@code Tuple$i}.
           ${(1 to i).gen(j => s"* @param <T$j> the ${j.ordinal} component type")(using "\n")}
           * @return the given {@code t} instance as narrowed type {@code Tuple$i<$generics>}.
           */
          @SuppressWarnings("unchecked")
          static <$genericsDecl> Tuple$i<$generics> narrow(Tuple$i<$wideGenerics> t) {
              return (Tuple$i<$generics>) t;
          }
        """
      }

      def genSeqMethod(i: Int) = {
        val a = Arity(i)
        import a.{generics, genericsDecl}
        val seqs = (1 to i).gen(j => s"Seq<T$j>")(using ", ")
        val Stream = im.getType("com.guizmaii.zazr.collection.Stream")
        val widenedGenerics = a.covariantGenerics
        // Arities 2 and 3 have a hand-written counterpart on Seq: Seq#unzip / Seq#unzip3. Seq's own
        // implementations (Vector.java, List.java, ...) build the *receiver's own kind* with a per-kind,
        // single-pass loop and can't be reused here: this method's return type is always Seq-erased
        // (Stream-backed), never the caller's concrete class, so routing Seq#unzip through this method
        // would silently turn e.g. Vector.unzip into a pair of Streams. The other direction is safe,
        // though: Stream is itself a Seq, Stream.ofAll on an already-lazy Stream returns the same
        // instance (Stream.java:374), and Stream#unzip/#unzip3 already do exactly this split. Delegate
        // to it so there is one implementation of the split; the cost is one extra lazy identity-map
        // layer per element versus the previous direct s.map(Tuple$i::_j) calls, accepted because
        // Stream's map is lazy (no eager pass added, only a thin extra cons wrapper). Arities 1 and
        // 4..8 have no Seq counterpart to unify with, so they keep the direct implementation.
        val streamUnzipName = i match {
          case 2 => "unzip"
          case 3 => "unzip3"
          case _ => ""
        }
        val body =
          if (streamUnzipName.nonEmpty) {
            val Function = im.getType("java.util.function.Function")
            val streamSeqs = (1 to i).gen(j => s"$Stream<T$j>")(using ", ")
            xs"""
                $Objects.requireNonNull(tuples, "tuples is null");
                final Tuple$i<$streamSeqs> unzipped = $Stream.ofAll(tuples).$streamUnzipName($Function.identity());
                return Tuple.of(${(1 to i).gen(j => s"unzipped._$j()")(using ", ")});
            """
          } else {
            xs"""
                $Objects.requireNonNull(tuples, "tuples is null");
                final Stream<Tuple$i<$widenedGenerics>> s = $Stream.ofAll(tuples);
                return new Tuple$i<>(${(1 to i).gen(j => s"s.map(Tuple$i::_$j)")(using s", ")});
            """
          }
        xs"""
            /**
             * Splits a sequence of {@code Tuple$i} into a Tuple$i of {@code Seq}${(i > 1).gen("s")},
             * one per component.
             *
             ${(1 to i).gen(j => s"* @param <T$j> ${j.ordinal} component type")(using "\n")}
             * @param tuples an {@code Iterable} of tuples
             * @return a tuple of ${i.numerus(s"{@link $Seq}")}.
             */
            static <$genericsDecl> Tuple$i<$seqs> unzip$i(Iterable<? extends Tuple$i<$widenedGenerics>> tuples) {
                $body
            }
        """
      }

      xs"""
        /**
         * The base interface of all tuples.
         *
         * @author Daniel Dietrich
         */
        public interface Tuple {

            /**
             * The maximum arity of an Tuple.
             * <p>
             * Note: This value might be changed in a future version of Vavr.
             * So it is recommended to use this constant instead of hardcoding the current maximum arity.
             */
            int MAX_ARITY = $N;

            /**
             * Returns the number of elements of this tuple.
             *
             * @return the number of elements.
             */
            int arity();

            /**
             * Converts this tuple to a sequence.
             *
             * @return a {@code Seq} containing the elements of this tuple, in order (empty for {@code Tuple0}).
             */
            $Seq<?> toSeq();

            // -- factory methods

            /$javadoc
             * Creates the empty tuple.
             *
             * @return the empty tuple.
             */
            static Tuple0 empty() {
                return Tuple0.instance();
            }

            /**
             * Creates a {@code Tuple2} from a {@link $Map.Entry}.
             *
             * @param <T1> Type of first component (entry key)
             * @param <T2> Type of second component (entry value)
             * @param      entry A {@link java.util.Map.Entry}
             * @return a new {@code Tuple2} containing key and value of the given {@code entry}
             */
            static <T1 $nullableBound, T2 $nullableBound> Tuple2<T1, T2> fromEntry($Map.Entry<? extends T1, ? extends T2> entry) {
                $Objects.requireNonNull(entry, "entry is null");
                return new Tuple2<>(entry.getKey(), entry.getValue());
            }

            ${(1 to N).gen(genFactoryMethod)(using "\n\n")}

            ${(1 to N).gen(genHashMethod)(using "\n\n")}

            ${(1 to N).gen(genNarrowMethod)(using "\n\n")}

            ${(1 to N).gen(genSeqMethod)(using "\n\n")}

        }
      """
    }
  }

  /**
   * Generator of com.guizmaii.zazr.collection.*ArrayType
   */
  def genArrayTypes(): Unit = {

    val types = ListMap(
      "boolean" -> "Boolean",
      "byte" -> "Byte",
      "char" -> "Character",
      "double" -> "Double",
      "float" -> "Float",
      "int" -> "Integer",
      "long" -> "Long",
      "short" -> "Short",
      "Object" -> "Object" // fallback
    ) // note: there is no void[] in Java

    genVavrFile("com.guizmaii.zazr.collection", "ArrayType")((im: ImportManager, packageName: String, className: String) => xs"""
      import java.util.Collection;

      /**
       * Helper to replace reflective array access.
       *
       * @author Pap Lőrinc
       */
      interface ArrayType<T $nullableBound> {

          @SuppressWarnings("unchecked")
          static <T $nullableBound> ArrayType<T> obj() { return (ArrayType<T>) ObjectArrayType.INSTANCE; }

          Class<T> type();
          int lengthOf(Object array);
          T getAt(Object array, int index);

          Object empty();
          void setAt(Object array, int index, T value) throws ClassCastException;
          Object copy(Object array, int arraySize, int sourceFrom, int destinationFrom, int size);

          @SuppressWarnings("unchecked")
          static <T $nullableBound> ArrayType<T> of(Object array)  { return of((Class<T>) array.getClass().getComponentType()); }
          static <T $nullableBound> ArrayType<T> of(Class<T> type) { return !type.isPrimitive() ? obj() : ofPrimitive(type); }
          @SuppressWarnings("unchecked")
          static <T $nullableBound> ArrayType<T> ofPrimitive(Class<T> type) {
              if (boolean.class == type) {
                  return (ArrayType<T>) BooleanArrayType.INSTANCE;
              } else if (byte.class == type) {
                  return (ArrayType<T>) ByteArrayType.INSTANCE;
              } else if (char.class == type) {
                  return (ArrayType<T>) CharArrayType.INSTANCE;
              } else if (double.class == type) {
                  return (ArrayType<T>) DoubleArrayType.INSTANCE;
              } else if (float.class == type) {
                  return (ArrayType<T>) FloatArrayType.INSTANCE;
              } else if (int.class == type) {
                  return (ArrayType<T>) IntArrayType.INSTANCE;
              } else if (long.class == type) {
                  return (ArrayType<T>) LongArrayType.INSTANCE;
              } else if (short.class == type) {
                  return (ArrayType<T>) ShortArrayType.INSTANCE;
              } else {
                  throw new IllegalArgumentException(String.valueOf(type));
              }
          }

          default Object newInstance(int length) { return copy(empty(), length); }

          /** copy the range [from, to) of the source into a new array of length (to - from), starting at index 0 */
          default Object copyRange(Object array, int from, int to) {
              final int length = to - from;
              return copy(array, length, from, 0, length);
          }

          /** group an array into sub-arrays of groupSize elements each (the last one may be shorter) */
          default Object grouped(Object array, int groupSize) {
              final int arrayLength = lengthOf(array);
              final Object results = obj().newInstance(1 + ((arrayLength - 1) / groupSize));
              obj().setAt(results, 0, copyRange(array, 0, groupSize));

              for (int start = groupSize, i = 1; start < arrayLength; i++) {
                  final int nextLength = Math.min(groupSize, arrayLength - (i * groupSize));
                  obj().setAt(results, i, copyRange(array, start, start + nextLength));
                  start += nextLength;
              }

              return results;
          }

          /** clone the source and set the value at the given position */
          default Object copyUpdate(Object array, int index, T element) {
              final Object copy = copy(array, index + 1);
              setAt(copy, index, element);
              return copy;
          }

          default Object copy(Object array, int minLength) {
              final int arrayLength = lengthOf(array);
              final int length = Math.max(arrayLength, minLength);
              return copy(array, length, 0, 0, arrayLength);
          }

          /** clone the source and keep everything at and after the index; the leading slots hold null (or the default value for primitive array types) */
          default Object copyDrop(Object array, int index) {
              final int length = lengthOf(array);
              return copy(array, length, index, index, length - index);
          }

          /** clone the source and keep everything before and including the index */
          default Object copyTake(Object array, int lastIndex) {
              return copyRange(array, 0, lastIndex + 1);
          }

          /** Create a single element array */
          default Object asArray(T element) {
              final Object result = newInstance(1);
              setAt(result, 0, element);
              return result;
          }

          /** Store the content of an iterable in an array */
          static Object[] asArray(java.util.Iterator<?> it, int length) {
              final Object[] array = new Object[length];
              for (int i = 0; i < length; i++) {
                  array[i] = it.next();
              }
              return array;
          }

          @SuppressWarnings("unchecked")
          static <T $nullableBound> T asPrimitives(Class<?> primitiveClass, Iterable<?> values) {
              final java.util.List<Object> list = new java.util.ArrayList<>();
              values.forEach(list::add);
              final Object[] array = list.toArray();
              final ArrayType<T> type = of((Class<T>) primitiveClass);
              final Object results = type.newInstance(array.length);
              for (int i = 0; i < array.length; i++) {
                  type.setAt(results, i, (T) array[i]);
              }
              return (T) results;
          }

          ${types.keys.toSeq.gen(arrayType =>
            genArrayType(arrayType)(im, packageName, arrayType.capitalize + className)
          )(using "\n\n")}
      }
    """)

    def genArrayType(arrayType: String)(im: ImportManager, packageName: String, className: String): String = {
      val wrapperType = types(arrayType)
      val isPrimitive = arrayType != "Object"

      xs"""
        final class $className implements ArrayType<$wrapperType> {
            static final $className INSTANCE = new $className();
            static final $arrayType[] EMPTY = new $arrayType[0];

            private static $arrayType[] cast(Object array) { return ($arrayType[]) array; }

            @Override
            public Class<$wrapperType> type() { return $arrayType.class; }

            @Override
            public $arrayType[] empty() { return EMPTY; }

            @Override
            public int lengthOf(Object array) { return (array != null) ? cast(array).length : 0; }

            @Override
            public $wrapperType getAt(Object array, int index) { return cast(array)[index]; }

            @Override
            public void setAt(Object array, int index, $wrapperType value) ${if (isPrimitive) "throws ClassCastException " else ""}{
                ${if (isPrimitive)
                """if (value != null) {
                  |    cast(array)[index] = value;
                  |} else {
                  |    throw new ClassCastException();
                  |}""".stripMargin
              else "cast(array)[index] = value;" }
            }

            @Override
            public Object copy(Object array, int arraySize, int sourceFrom, int destinationFrom, int size) {
                return (size > 0)
                        ? copyNonEmpty(array, arraySize, sourceFrom, destinationFrom, size)
                        : new $arrayType[arraySize];
            }
            private static Object copyNonEmpty(Object array, int arraySize, int sourceFrom, int destinationFrom, int size) {
                final $arrayType[] result = new $arrayType[arraySize];
                System.arraycopy(array, sourceFrom, result, destinationFrom, size); /* has to be near the object allocation to avoid zeroing out the array */
                return result;
            }
        }
      """
    }
  }
}

/**
 * Generate Vavr src-gen/test/java classes
 */
def generateTestClasses(): Unit = {

  genFunctionTests()
  genMapOfEntriesTests()
  genTupleTests()
  genControlZipTests()

  /**
   * Generator of Function tests
   */
  def genFunctionTests(): Unit = {

    (1 to N).foreach(i => {

      genVavrFile("com.guizmaii.zazr", s"CheckedFunction${i}Test", baseDir = TARGET_TEST)(genFunctionTest("CheckedFunction", checked = true))
      if (i >= 3) genVavrFile("com.guizmaii.zazr", s"Function${i}Test", baseDir = TARGET_TEST)(genFunctionTest("Function", checked = false))

      def genFunctionTest(name: String, checked: Boolean)(im: ImportManager, packageName: String, className: String): String = {

        val AtomicInteger = im.getType("java.util.concurrent.atomic.AtomicInteger")
        val nested = im.getType("org.junit.jupiter.api.Nested")

        val functionArgsDecl = (1 to i).gen(j => s"Object o$j")(using ", ")
        val functionArgs = (1 to i).gen(j => s"o$j")(using ", ")
        val generics = (1 to i + 1).gen(j => "Object")(using ", ")

        val test = im.getType("org.junit.jupiter.api.Test")
        val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
        val assertThrows = im.getStatic("org.junit.jupiter.api.Assertions.assertThrows")
        val recFuncF1 = s"i1 <= 0 ? i1 : $className.recurrent1.apply(${(1 to i).gen(j => s"i$j" + (j == 1).gen(s" - 1"))(using ", ")}) + 1;"

        // The unchecked JDK type at arity 1: java.util.function.Function. Used both for the plain
        // unchecked companion (i == 1, 2) and for every andThen/composeJ argument (always arity 1).
        val jdkFunction1 = javaFunctionType(1, im)
        val uncheckedSelfType = javaFunctionType(i, im)

        def curriedType(max: Int, function: String): String = max match {
          case 1 => if (checked) s"${function}1<Object, Object>" else s"$jdkFunction1<Object, Object>"
          case _ => s"$jdkFunction1<Object, ${curriedType(max - 1, function)}>"
        }

        val wideGenericArgs = (1 to i).gen(j => "Number")(using ", ")
        val wideGenericResult = "String"
        val wideFunctionPattern = (1 to i).gen(j => "%s")(using ", ")
        val narrowGenericArgs = (1 to i).gen(j => "Integer")(using ", ")
        val narrowGenericResult = im.getType("java.lang.CharSequence")
        val narrowArgs = (1 to i).gen(j => j.toString)(using ", ")

        xs"""
          public class $className {

              @$test
              public void shouldCreateFromMethodReference() {
                  class Type {
                      Object methodReference($functionArgsDecl) {
                          return null;
                      }
                  }
                  final Type type = new Type();
                  assertThat($name$i.of(type::methodReference)).isNotNull();
              }

              @$test
              public void shouldLiftPartialFunction() {
                  assertThat($name$i.lift(($functionArgs) -> { while(true); })).isNotNull();
              }

              ${(i == 1).gen(xs"""
                @$test
                public void shouldCreateIdentityFunction()${checked.gen(" throws Exception")} {
                    final $name$i<String, String> identity = $name$i.identity();
                    final String s = "test";
                    assertThat(identity.apply(s)).isEqualTo(s);
                }
              """)}

              ${(i > 1).gen(xs"""
                @$test
                public void shouldPartiallyApply()${checked.gen(" throws Exception")} {
                    final $name$i<$generics> f = ($functionArgs) -> null;
                    ${(1 until i).gen(j => {
                      val partialArgs = (1 to j).gen(k => "null")(using ", ")
                      s"$assertThat(f.apply($partialArgs)).isNotNull();"
                    })(using "\n")}
                }
              """)}

              @$test
              public void shouldConstant()${checked.gen(" throws Exception")} {
                  final $name$i<$generics> f = $name$i.constant(6);
                  $assertThat(f.apply(${(1 to i).gen(j => s"$j")(using ", ")})).isEqualTo(6);
              }

              @$test
              public void shouldCurry() {
                  final $name$i<$generics> f = ($functionArgs) -> null;
                  final ${curriedType(i, name)} curried = f.curried();
                  $assertThat(curried).isNotNull();
              }

              @$test
              public void shouldTuple() {
                  final $name$i<$generics> f = ($functionArgs) -> null;
                  final ${if (checked) s"${name}1" else jdkFunction1}<Tuple$i<${(1 to i).gen(j => "Object")(using ", ")}>, Object> tupled = f.tupled();
                  $assertThat(tupled).isNotNull();
              }

              ${(!checked).gen(xs"""
                @$test
                public void shouldLiftTryPartialFunction() {
                    $AtomicInteger integer = new $AtomicInteger();
                    $name$i<${(1 to i + 1).gen(j => "Integer")(using ", ")}> divByZero = (${(1 to i).gen(j => s"i$j")(using ", ")}) -> 10 / integer.get();
                    $name$i<${(1 to i).gen(j => "Integer, ")(using "")}Try<Integer>> divByZeroTry = $name$i.liftTry(divByZero);

                    ${im.getType("com.guizmaii.zazr.control.Try")}<Integer> res = divByZeroTry.apply(${(1 to i).gen(j => s"0")(using ", ")});
                    assertThat(res.isFailure()).isTrue();
                    assertThat(res.getCause()).isNotNull();
                    assertThat(res.getCause().getMessage()).isEqualToIgnoringCase("/ by zero");

                    integer.incrementAndGet();
                    res = divByZeroTry.apply(${(1 to i).mkString(", ")});
                    assertThat(res.isSuccess()).isTrue();
                    assertThat(res.get()).isEqualTo(10);
                }
              """)}

              ${checked.gen(xs"""
                  ${
                    val types = s"<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest>"
                    def toArgList (s: String) = s.split("", i).mkString("\"", "\", \"", "\"") + (s.length + 2 to i).gen(j => ", \"\"")
                    xs"""

                      private static final $name$i$types digest = (${(1 to i).gen(j => s"s$j")(using ", ")}) -> ${im.getType("java.security.MessageDigest")}.getInstance(${(1 to i).gen(j => s"s$j")(using " + ")});

                      @$test
                      public void shouldRecover() {
                          final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest> recover = digest.recover(throwable -> (${(1 to i).gen(j => s"s$j")(using ", ")}) -> null);
                          final MessageDigest md5 = recover.apply(${toArgList("MD5")});
                          assertThat(md5).isNotNull();
                          assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
                          assertThat(md5.getDigestLength()).isEqualTo(16);
                          assertThat(recover.apply(${toArgList("Unknown")})).isNull();
                      }

                      @$test
                      public void shouldRecoverNonNull() {
                          final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest> recover = digest.recover(throwable -> null);
                          final MessageDigest md5 = recover.apply(${toArgList("MD5")});
                          assertThat(md5).isNotNull();
                          assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
                          assertThat(md5.getDigestLength()).isEqualTo(16);
                          final ${im.getType("com.guizmaii.zazr.control.Try")}<MessageDigest> unknown = ${im.getType("com.guizmaii.zazr.control.Try")}.of(() -> recover.apply(${toArgList("Unknown")}));
                          assertThat(unknown).isNotNull();
                          assertThat(unknown.isFailure()).isTrue();
                          assertThat(unknown.getCause()).isNotNull().isInstanceOf(NullPointerException.class);
                          assertThat(unknown.getCause().getMessage()).isNotEmpty().isEqualToIgnoringCase("recover return null for class java.security.NoSuchAlgorithmException: Unknown MessageDigest not available");
                      }

                      ${(i == 1 || i == N).gen(xs"""
                        @$test
                        public void shouldNotHandFatalThrowableToRecover() {
                            final $name$i$types fatal = (${(1 to i).gen(j => s"s$j")(using ", ")}) -> { throw new OutOfMemoryError("fatal"); };
                            final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest> recover =
                                fatal.recover(throwable -> { throw new AssertionError("recover must not see a fatal throwable"); });
                            $assertThrows(OutOfMemoryError.class, () -> recover.apply(${toArgList("MD5")}));
                        }

                        @$test
                        public void shouldHandNonFatalThrowableToRecover() {
                            final $name$i$types nonFatal = (${(1 to i).gen(j => s"s$j")(using ", ")}) -> { throw new IllegalStateException("non-fatal"); };
                            final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest> recover =
                                nonFatal.recover(throwable -> (${(1 to i).gen(j => s"s$j")(using ", ")}) -> null);
                            assertThat(recover.apply(${toArgList("MD5")})).isNull();
                        }
                      """)}

                      @$test
                      public void shouldUncheckedWork() {
                          final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest> unchecked = digest.unchecked();
                          final MessageDigest md5 = unchecked.apply(${toArgList("MD5")});
                          assertThat(md5).isNotNull();
                          assertThat(md5.getAlgorithm()).isEqualToIgnoringCase("MD5");
                          assertThat(md5.getDigestLength()).isEqualTo(16);
                      }

                      @$test
                      public void shouldUncheckedThrowIllegalState() {
                          $assertThrows(${im.getType("java.security.NoSuchAlgorithmException")}.class, () -> {
                              final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, MessageDigest> unchecked = digest.unchecked();
                              unchecked.apply(${toArgList("Unknown")}); $comment Look ma, we throw an undeclared checked exception!
                          });
                      }

                      @$test
                      public void shouldLiftTryPartialFunction() {
                          final $uncheckedSelfType<${(1 to i).gen(j => "String")(using ", ")}, Try<MessageDigest>> liftTry = $name$i.liftTry(digest);
                          final ${im.getType("com.guizmaii.zazr.control.Try")}<MessageDigest> md5 = liftTry.apply(${toArgList("MD5")});
                          assertThat(md5.isSuccess()).isTrue();
                          assertThat(md5.get()).isNotNull();
                          assertThat(md5.get().getAlgorithm()).isEqualToIgnoringCase("MD5");
                          assertThat(md5.get().getDigestLength()).isEqualTo(16);
                          final ${im.getType("com.guizmaii.zazr.control.Try")}<MessageDigest> unknown = liftTry.apply(${toArgList("Unknown")});
                          assertThat(unknown.isFailure()).isTrue();
                          assertThat(unknown.getCause()).isNotNull();
                          assertThat(unknown.getCause().getMessage()).isEqualToIgnoringCase("Unknown MessageDigest not available");
                      }
                    """
                  }
              """)}

              private static final $name$i<${(1 to i + 1).gen(j => "Integer")(using ", ")}> recurrent1 = (${(1 to i).gen(j => s"i$j")(using ", ")}) -> $recFuncF1

              @$test
              public void shouldCalculatedRecursively()${checked.gen(" throws Exception")} {
                  assertThat(recurrent1.apply(${(1 to i).gen(j => "11")(using ", ")})).isEqualTo(11);
                  ${(i > 0).gen(s"assertThat(recurrent1.apply(${(1 to i).gen(j => "22")(using ", ")})).isEqualTo(22);")}
              }

              @$test
              public void shouldComposeWithAndThen() {
                  final $name$i<$generics> f = ($functionArgs) -> null;
                  final ${if (checked) "CheckedFunction1" else jdkFunction1}<Object, Object> after = o -> null;
                  final $name$i<$generics> composed = f.andThen(after);
                  $assertThat(composed).isNotNull();
              }

              @Nested
              class ComposeTests {
                ${(1 to i).gen(j =>
                  val genArgs = (1 to i).gen(k => "String")(using ", ")
                  val params = (1 to i).gen(k => s"String s$k")(using ", ")
                  val values = (1 to i).gen(k => if (k == j) "\"xx\"" else s"\"s$k\"")(using ", ")
                  val expected = (1 to i).gen(k => if (k == j) "XX" else s"s$k")(using "")
                  val concat = (1 to i).gen(k => s"s$k")(using " + ")
                  xs"""

                  @$test
                  public void shouldCompose$j() ${checked.gen(" throws Exception ")}{
                      final $name$i<$genArgs, String> concat = ($params) -> $concat;
                      final $jdkFunction1<String, String> toUpperCase = String::toUpperCase;
                      assertThat(concat.compose$j(toUpperCase).apply($values)).isEqualTo(\"$expected\");
                  }

                  """
                )}

              }

              ${(i > 0).gen(xs"""
              @$test
              public void shouldNarrow()${checked.gen(" throws Exception")}{
                  final $name$i<$wideGenericArgs, $wideGenericResult> wideFunction = ($functionArgs) -> String.format("Numbers are: $wideFunctionPattern", $functionArgs);
                  final $name$i<$narrowGenericArgs, $narrowGenericResult> narrowFunction = $name$i.narrow(wideFunction);

                  $assertThat(narrowFunction.apply($narrowArgs)).isEqualTo("Numbers are: $narrowArgs");
              }
              """)}
          }
        """
      }
    })
  }

  def genMapOfEntriesTests(): Unit = {

    def genAllArity(im: ImportManager,
                mapName: String, mapBuilder: String,
                builderComparator: Boolean, keyComparator: Boolean): String = {
      val test = im.getType("org.junit.jupiter.api.Test")
      val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
      val assertThrows = im.getStatic("org.junit.jupiter.api.Assertions.assertThrows")
      val naturalComparator = if (builderComparator || keyComparator) im.getStatic(s"com.guizmaii.zazr.collection.Comparators.naturalComparator") else null
      val map = im.getType(s"com.guizmaii.zazr.collection.$mapName")
      (1 to VARARGS).gen(arity => xs"""
        @$test
        public void shouldConstructFrom${arity}Entries${if(builderComparator) "WithBuilderComparator" else ""}${if(keyComparator) "WithKeyComparator" else ""}${mapBuilder.capitalize}() {
          final $map<Integer, String> map =
            $map${if (mapBuilder.isEmpty) "" else s".$mapBuilder"}${if (builderComparator) s"($naturalComparator())" else if (mapBuilder.isEmpty) "" else "()"}
            .of(${if(keyComparator) s"$naturalComparator(), " else ""}${(1 to arity).gen(j => s"""$j, "$j"""")(using ", ")});
          $assertThat(map.size()).isEqualTo($arity);
          ${(1 to arity).gen(j => {
            s"""${if (mapBuilder.isEmpty) "" else s"$assertThat(map.get($j).get() instanceof ${im.getType(s"com.guizmaii.zazr.collection.${mapBuilder.substring(4)}")}).isTrue();\n"}$assertThat(map.get($j).get()${if (mapName.contains("Multimap")) ".head()" else ""}).isEqualTo("$j");"""
          })(using "\n")}
        }
      """)(using "\n\n")
    }

    def genMapOfEntriesTest(mapName: String): Unit = {
      val mapBuilders:List[String] = if (mapName.contains("Multimap")) List("withSeq", "withSet", "withSortedSet") else List("")
      val keyComparators:List[Boolean] = if (mapName.startsWith("Tree")) List(true, false) else List(false)
      genVavrFile("com.guizmaii.zazr.collection", s"${mapName}OfEntriesTest", baseDir = TARGET_TEST) ((im: ImportManager, packageName, className) => {
        xs"""
        public class ${mapName}OfEntriesTest {
          ${mapBuilders.flatMap(mapBuilder => {
          val builderComparators:List[Boolean] = if (mapBuilder.contains("Sorted")) List(true, false) else List(false)
          builderComparators.flatMap(builderComparator => keyComparators.map(keyComparator =>
            xs"""
              ${genAllArity(im, mapName, mapBuilder, builderComparator, keyComparator)}
              """
          ))
        }).mkString("\n\n")}
        }
        """
      })
    }

    genMapOfEntriesTest("HashMap")
    genMapOfEntriesTest("LinkedHashMap")
    genMapOfEntriesTest("TreeMap")

  }

  /**
   * Generator of Tuple tests
   */
  def genTupleTests(): Unit = {

    def genArgsForComparing(digits: Int, p: Int): String = {
      (1 to digits).gen(i => if(i == p) "1" else "0")(using ", ")
    }

    (0 to N).foreach(i => {

      genVavrFile("com.guizmaii.zazr", s"Tuple${i}Test", baseDir = TARGET_TEST)((im: ImportManager, packageName, className) => {

        val test = im.getType("org.junit.jupiter.api.Test")
        val assertThrows = im.getStatic("org.junit.jupiter.api.Assertions.assertThrows")
        val seq = im.getType("com.guizmaii.zazr.collection.Seq")
        val list = im.getType("com.guizmaii.zazr.collection.List")
        val stream = if (i == 0) "" else im.getType("com.guizmaii.zazr.collection.Stream")
        val comparator = im.getType("java.util.Comparator")
        val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
        val generics = if (i == 0) "" else s"<${(1 to i).gen(j => s"Object")(using ", ")}>"
        val intGenerics = if (i == 0) "" else s"<${(1 to i).gen(j => s"Integer")(using ", ")}>"
        val functionArgs = if (i == 0) "()" else s"${(i > 1).gen("(") + (1 to i).gen(j => s"o$j")(using ", ") + (i > 1).gen(")")}"
        val nullArgs = (1 to i).gen(j => "null")(using ", ")
        if(i==2){
          im.getType("java.util.AbstractMap")
          im.getType("java.util.Map")
        }


        xs"""
          public class $className {

              @$test
              public void shouldCreateTuple() {
                  final Tuple$i$generics tuple = createTuple();
                  $assertThat(tuple).isNotNull();
              }

              @$test
              public void shouldGetArity() {
                  final Tuple$i$generics tuple = createTuple();
                  $assertThat(tuple.arity()).isEqualTo($i);
              }

              ${(i > 0).gen(xs"""
                @$test
                public void shouldReturnElements() {
                    final Tuple$i$intGenerics tuple = createIntTuple(${(1 to i).gen(j => s"$j")(using ", ")});
                    ${(1 to i).gen(j => s"$assertThat(tuple._$j()).isEqualTo($j);\n")}
                }
              """)}

              ${(1 to i).gen(j =>
                xs"""
                  @$test
                  public void shouldUpdate$j() {
                    final Tuple$i$intGenerics tuple = createIntTuple(${(1 to i).gen(j => s"$j")(using ", ")}).update$j(42);
                    ${(1 to i).gen(k => s"$assertThat(tuple._$k()).isEqualTo(${if (j == k) 42 else k});\n")}
                  }
                """)(using "\n\n")}

              @$test
              public void shouldConvertToSeq() {
                  final $seq<?> actual = createIntTuple(${genArgsForComparing(i, 1)}).toSeq();
                  $assertThat(actual).isEqualTo($list.of(${genArgsForComparing(i, 1)}));
              }

              @$test
              public void shouldCompareEqual() {
                  final Tuple$i$intGenerics t0 = createIntTuple(${genArgsForComparing(i, 0)});
                  $assertThat(t0.compareTo(t0)).isZero();
                  $assertThat(intTupleComparator.compare(t0, t0)).isZero();
              }

              @$test
              public void shouldThrowWhenComparingToNull() {
                  final Tuple$i$intGenerics t0 = createIntTuple(${genArgsForComparing(i, 0)});
                  $assertThrows(NullPointerException.class, () -> t0.compareTo(null));
              }

              ${(1 to i).gen(j => xs"""
                @$test
                public void shouldCompare${j.ordinal}Arg() {
                    final Tuple$i$intGenerics t0 = createIntTuple(${genArgsForComparing(i, 0)});
                    final Tuple$i$intGenerics t$j = createIntTuple(${genArgsForComparing(i, j)});
                    $assertThat(t0.compareTo(t$j)).isNegative();
                    $assertThat(t$j.compareTo(t0)).isPositive();
                    $assertThat(intTupleComparator.compare(t0, t$j)).isNegative();
                    $assertThat(intTupleComparator.compare(t$j, t0)).isPositive();
                }
              """)(using "\n\n")}

              ${(i == 2).gen(xs"""
                @$test
                public void shouldSwap() {
                    $assertThat(createIntTuple(1, 2).swap()).isEqualTo(createIntTuple(2, 1));
                }

                @$test
                public void shouldConvertToEntry() {
                    Tuple$i$intGenerics tuple = createIntTuple(1,2);
                    Map.Entry$intGenerics entry = new AbstractMap.SimpleEntry<>(1, 2);
                    assertThat(tuple.toEntry().equals(entry));
                }

              """)}

              ${(i > 0).gen(xs"""
                @$test
                public void shouldMap() {
                    final Tuple$i$generics tuple = createTuple();
                    ${if (i == 1) xs"""
                      final Tuple$i$generics actual = tuple.map(o -> o);
                      $assertThat(actual).isEqualTo(tuple);
                    """ else xs"""
                      final Tuple$i$generics actual = tuple.map($functionArgs -> tuple);
                      $assertThat(actual).isEqualTo(tuple);
                    """}
                }

                @$test
                public void shouldMapComponents() {
                  final Tuple$i$generics tuple = createTuple();
                  ${(1 to i).gen(j => xs"""final ${im.getType("java.util.function.Function")}<Object, Object> f$j = ${im.getType("java.util.function.Function")}.identity();""")(using "\n")}
                  final Tuple$i$generics actual = tuple.map(${(1 to i).gen(j => s"f$j")(using ", ")});
                  $assertThat(actual).isEqualTo(tuple);
                }

                @$test
                public void shouldReturnTuple${i}OfUnzip$i() {
                  final $seq<Tuple$i<${(1 to i).gen(j => xs"Integer")(using ", ")}>> iterable = $list.of(${(1 to i).gen(j => xs"Tuple.of(${(1 to i).gen(k => xs"${k+2*j-1}")(using ", ")})")(using ", ")});
                  final Tuple$i<${(1 to i).gen(j => xs"$seq<Integer>")(using ", ")}> expected = Tuple.of(${(1 to i).gen(j => xs"$stream.of(${(1 to i).gen(k => xs"${2*k+j-1}")(using ", ")})")(using ", ")});
                  $assertThat(Tuple.unzip$i(iterable)).isEqualTo(expected);
                }
              """)}

              ${(i > 1).gen(xs"""
                @$test
                public void shouldReturnTuple${i}OfUnzip1() {
                  final $seq<Tuple$i<${(1 to i).gen(j => xs"Integer")(using ", ")}>> iterable = $list.of(Tuple.of(${(1 to i).gen(k => xs"$k")(using ", ")}));
                  final Tuple$i<${(1 to i).gen(j => xs"$seq<Integer>")(using ", ")}> expected = Tuple.of(${(1 to i).gen(j => xs"$stream.of($j)")(using ", ")});
                  $assertThat(Tuple.unzip$i(iterable)).isEqualTo(expected);
                }
              """)}

              ${(i > 1) `gen` (1 to i).gen(j => {
                val substitutedResultTypes = if (i == 0) "" else s"<${(1 to i).gen(k => if (k == j) "String" else "Integer")(using ", ")}>"
                val ones = (1 to i).gen(_ => "1")(using ", ")
                val result = (1 to i).gen(k => if (k == j) "\"X\"" else "1")(using ", ")
                xs"""
                  @$test
                  public void shouldMap${j.ordinal}Component() {
                    final Tuple$i$substitutedResultTypes actual = Tuple.of($ones).map$j(i -> "X");
                    final Tuple$i$substitutedResultTypes expected = Tuple.of($result);
                    assertThat(actual).isEqualTo(expected);
                  }
                """
              })(using "\n\n")}

              @$test
              public void shouldApplyTuple() {
                  final Tuple$i$generics tuple = createTuple();
                  final Tuple0 actual = tuple.apply($functionArgs -> Tuple0.instance());
                  assertThat(actual).isEqualTo(Tuple0.instance());
              }

              ${(i < N).gen(xs"""
                @$test
                public void shouldAppendValue() {
                    final Tuple${i+1}<${(1 to i+1).gen(j => s"Integer")(using ", ")}> actual = ${ if (i == 0) "Tuple0.instance()" else s"Tuple.of(${(1 to i).gen(j => xs"$j")(using ", ")})"}.append(${i+1});
                    final Tuple${i+1}<${(1 to i+1).gen(j => s"Integer")(using ", ")}> expected = Tuple.of(${(1 to i+1).gen(j => xs"$j")(using ", ")});
                    assertThat(actual).isEqualTo(expected);
                }
              """)}

              ${(i < N) `gen` (1 to N-i).gen(j => xs"""
                @$test
                public void shouldConcatTuple$j() {
                    final Tuple${i+j}<${(1 to i+j).gen(j => s"Integer")(using ", ")}> actual = ${ if (i == 0) "Tuple0.instance()" else s"Tuple.of(${(1 to i).gen(j => xs"$j")(using ", ")})"}.concat(Tuple.of(${(i+1 to i+j).gen(k => s"$k")(using ", ")}));
                    final Tuple${i+j}<${(1 to i+j).gen(j => s"Integer")(using ", ")}> expected = Tuple.of(${(1 to i+j).gen(j => xs"$j")(using ", ")});
                    assertThat(actual).isEqualTo(expected);
                }
              """)(using "\n\n")}

              @$test
              public void shouldRecognizeEquality() {
                  final Tuple$i$generics tuple1 = createTuple();
                  final Tuple$i$generics tuple2 = createTuple();
                  $assertThat((Object) tuple1).isEqualTo(tuple2);
              }

              @$test
              public void shouldRecognizeNonEquality() {
                  final Tuple$i$generics tuple = createTuple();
                  final Object other = new Object();
                  $assertThat(tuple).isNotEqualTo(other);
              }

              ${(i > 0).gen(xs"""
                @$test
                public void shouldRecognizeNonEqualityPerComponent() {
                    final Tuple$i<${(1 to i).gen(_ => "String")(using ", ")}> tuple = Tuple.of(${(1 to i).gen(j => "\"" + j + "\"")(using ", ")});
                    ${(1 to i).gen(j => {
                      val that = "Tuple.of(" + (1 to i).gen(k => if (j == k) "\"X\"" else "\"" + k + "\"")(using ", ") + ")"
                      s"$assertThat(tuple.equals($that)).isFalse();"
                    })(using "\n")}
                }
              """)}

              @$test
              public void shouldComputeCorrectHashCode() {
                  $assertThat(createTuple().hashCode()).isEqualTo(createTuple().hashCode());
                  ${(i > 0).gen(xs"""
                    $assertThat(createIntTuple(${genArgsForComparing(i, 0)}).hashCode()).isEqualTo(createIntTuple(${genArgsForComparing(i, 0)}).hashCode());
                    $assertThat(createIntTuple(${genArgsForComparing(i, 0)}).hashCode()).isNotEqualTo(createIntTuple(${genArgsForComparing(i, 1)}).hashCode());
                  """)}
              }

              @$test
              public void shouldDeconstructWithRecordPattern() {
                  final Object o = createIntTuple(${(1 to i).gen(j => s"$j")(using ", ")});
                  if (o instanceof Tuple$i(${(1 to i).gen(j => s"var v$j")(using ", ")})) {
                      ${(1 to i).gen(j => s"$assertThat(v$j).isEqualTo($j);\n")}
                  } else {
                      throw new AssertionError("record pattern did not match");
                  }
              }

              @$test
              public void shouldImplementToString() {
                  final String actual = createTuple().toString();
                  final String expected = "($nullArgs)";
                  $assertThat(actual).isEqualTo(expected);
              }

              private $comparator<Tuple$i$intGenerics> intTupleComparator = Tuple$i.comparator(${(1 to i).gen($j => s"Integer::compare")(using ", ")});

              private Tuple$i$generics createTuple() {
                  return ${if (i == 0) "Tuple0.instance()" else s"new Tuple$i<>($nullArgs)"};
              }

              private Tuple$i$intGenerics createIntTuple(${(1 to i).gen(j => s"Integer i$j")(using ", ")}) {
                  return ${if (i == 0) "Tuple0.instance()" else s"new Tuple$i<>(${(1 to i).gen(j => s"i$j")(using ", ")})"};
              }
          }
        """
      })
    })
  }

  /**
   * Generator of the tests of the static zip/zipWith family (arities 2..N) of the control types (design 3.4).
   * The main-code methods are hand-written in Option, Either, Try, Validation and Lazy; only their test matrix is
   * generated: one class per type, for every arity the all-success case, every single failing position, the null
   * checks, and what is specific to the type (Validation accumulates, Lazy defers).
   */
  def genControlZipTests(): Unit = {

    val arities = 2 to N

    def lambdaParams(n: Int): String = (1 to n).gen(j => s"a$j")(using ", ")
    def ignoredParams(n: Int): String = (1 to n).gen(_ => "_")(using ", ")
    def concatenation(n: Int): String = "\"\" + " + (1 to n).gen(j => s"a$j")(using " + ")
    def digits(n: Int): String = (1 to n).gen(j => s"$j")
    def ints(n: Int): String = (1 to n).gen(j => s"$j")(using ", ")
    def notCalled(n: Int): String = s"(${ignoredParams(n)}) -> { throw new AssertionError(\"must not be called\"); }"
    def operands(n: Int, ok: Int => String, failing: Int => String, fails: Int => Boolean): String =
      (1 to n).gen(j => if (fails(j)) failing(j) else ok(j))(using ", ")

    /**
     * Option, Either and Try: fail-fast, the first failure in argument order is the result.
     */
    def genFailFastZipTest(typeName: String, paramPrefix: String, valueType: String,
                           success: Int => String, wrapSuccess: String => String, failure: Int => String,
                           exactFailureInstance: Boolean): Unit = {

      genVavrFile("com.guizmaii.zazr.control", s"${typeName}ZipTest", baseDir = TARGET_TEST)((im: ImportManager, packageName, className) => {

        val test = im.getType("org.junit.jupiter.api.Test")
        val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
        val assertThatThrownBy = im.getStatic("org.assertj.core.api.Assertions.assertThatThrownBy")
        val AtomicInteger = im.getType("java.util.concurrent.atomic.AtomicInteger")
        val Tuple = im.getType("com.guizmaii.zazr.Tuple")

        def failureAssertion(actual: String, failing: String): String =
          if (exactFailureInstance) s"$assertThat($actual).isSameAs($failing);" else s"$assertThat($actual).isEqualTo(${failure(0)});"

        def nullResultAssertion(n: Int): String = {
          val call = s"$typeName.zipWith(${operands(n, success, failure, _ => false)}, (${lambdaParams(n)}) -> null)"
          if (typeName == "Try") xs"""
            final Try<Object> actual = $call;
            $assertThat(actual.isFailure()).isTrue();
            $assertThat(actual.getCause()).isInstanceOf(NullPointerException.class).hasMessage("Try.zipWith: f returned null");
          """ else xs"""
            $assertThatThrownBy(() -> $call).isInstanceOf(NullPointerException.class).hasMessage("$typeName.zipWith: f returned null");
          """
        }

        xs"""
          public class $className {

              ${arities.gen(n => xs"""
                @$test
                public void shouldZip${n}Successes() {
                    $assertThat($typeName.zip(${operands(n, success, failure, _ => false)})).isEqualTo(${wrapSuccess(s"$Tuple.of(${ints(n)})")});
                }

                @$test
                public void shouldZipWith${n}Successes() {
                    final $AtomicInteger calls = new $AtomicInteger();
                    final ${valueType.replace("Integer", "String")} actual = $typeName.zipWith(${operands(n, success, failure, _ => false)}, (${lambdaParams(n)}) -> {
                        calls.incrementAndGet();
                        return ${concatenation(n)};
                    });
                    $assertThat(actual).isEqualTo(${wrapSuccess(s""""${digits(n)}"""")});
                    $assertThat(calls.get()).isEqualTo(1);
                }

                @$test
                public void shouldFailWhenOneOf${n}Fails() {
                    ${(1 to n).gen(k => xs"""
                      final $valueType failing$k = ${failure(k)};
                      ${failureAssertion(s"$typeName.zip(${operands(n, success, j => s"failing$j", _ == k)})", s"failing$k")}
                      ${failureAssertion(s"$typeName.zipWith(${operands(n, success, j => s"failing$j", _ == k)}, ${notCalled(n)})", s"failing$k")}
                    """)(using "\n")}
                }

                ${(exactFailureInstance).gen(xs"""
                  @$test
                  public void shouldReturnTheFirstFailureOf${n}InArgumentOrder() {
                      ${(1 to n).gen(k => s"final $valueType failing$k = ${failure(k)};")(using "\n")}
                      ${(1 to n).gen(k => xs"""
                        ${failureAssertion(s"$typeName.zip(${operands(n, success, j => s"failing$j", _ >= k)})", s"failing$k")}
                        ${failureAssertion(s"$typeName.zipWith(${operands(n, success, j => s"failing$j", _ >= k)}, ${notCalled(n)})", s"failing$k")}
                      """)(using "\n")}
                  }
                """)}

                ${(typeName == "Try").gen(xs"""
                  @$test
                  public void shouldCaptureWhatTheCombinerOf${n}Throws() {
                      final RuntimeException boom = new IllegalStateException("boom");
                      $assertThat(Try.zipWith(${operands(n, success, failure, _ => false)}, (${ignoredParams(n)}) -> {
                          throw boom;
                      })).isEqualTo(Try.failure(boom));
                  }
                """)}

                @$test
                public void shouldRejectANullResultOfZipWith$n() {
                    ${nullResultAssertion(n)}
                }

                @$test
                public void shouldRejectANullArgumentOf$n() {
                    ${(1 to n).gen(k => xs"""
                      $assertThatThrownBy(() -> $typeName.zip(${operands(n, success, _ => "null", _ == k)})).isInstanceOf(NullPointerException.class).hasMessage("$paramPrefix$k is null");
                      $assertThatThrownBy(() -> $typeName.zipWith(${operands(n, success, _ => "null", _ == k)}, ${notCalled(n)})).isInstanceOf(NullPointerException.class).hasMessage("$paramPrefix$k is null");
                    """)(using "\n")}
                    $assertThatThrownBy(() -> $typeName.zipWith(${operands(n, success, failure, _ => false)}, null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
                }
              """)(using "\n\n")}
          }
        """
      })
    }

    genFailFastZipTest("Option", "o", "Option<Integer>",
      j => s"Option.some($j)", v => s"Option.some($v)", _ => "Option.<Integer>none()", exactFailureInstance = false)
    genFailFastZipTest("Either", "e", "Either<String, Integer>",
      j => s"Either.<String, Integer>right($j)", v => s"Either.right($v)", j => s"""Either.<String, Integer>left("e$j")""", exactFailureInstance = true)
    genFailFastZipTest("Try", "t", "Try<Integer>",
      j => s"Try.success($j)", v => s"Try.success($v)", j => s"""Try.<Integer>failure(new IllegalStateException("e$j"))""", exactFailureInstance = true)

    /**
     * Validation: accumulating, the errors of every Invalid operand in argument order.
     */
    genVavrFile("com.guizmaii.zazr.control", "ValidationZipTest", baseDir = TARGET_TEST)((im: ImportManager, packageName, className) => {

      val test = im.getType("org.junit.jupiter.api.Test")
      val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
      val assertThatThrownBy = im.getStatic("org.assertj.core.api.Assertions.assertThatThrownBy")
      val AtomicInteger = im.getType("java.util.concurrent.atomic.AtomicInteger")
      val Tuple = im.getType("com.guizmaii.zazr.Tuple")
      val NonEmptyVector = im.getType("com.guizmaii.zazr.collection.NonEmptyVector")

      def valid(j: Int): String = s"Validation.<String, Integer>valid($j)"
      def invalid(j: Int): String = s"""Validation.<String, Integer>invalid("e$j")"""
      def errors(ks: Seq[Int]): String = s"""Validation.invalidAll($NonEmptyVector.of(${ks.gen(k => s""""e$k"""")(using ", ")}))"""
      def all(n: Int): String = operands(n, valid, invalid, _ => false)

      xs"""
        public class $className {

            ${arities.gen(n => xs"""
              @$test
              public void shouldZip${n}Valids() {
                  $assertThat(Validation.zip(${all(n)})).isEqualTo(Validation.valid($Tuple.of(${ints(n)})));
              }

              @$test
              public void shouldZipWith${n}Valids() {
                  final $AtomicInteger calls = new $AtomicInteger();
                  final Validation<String, String> actual = Validation.zipWith(${all(n)}, (${lambdaParams(n)}) -> {
                      calls.incrementAndGet();
                      return ${concatenation(n)};
                  });
                  $assertThat(actual).isEqualTo(Validation.valid("${digits(n)}"));
                  $assertThat(calls.get()).isEqualTo(1);
              }

              @$test
              public void shouldKeepTheErrorsOfTheOneInvalidOf$n() {
                  ${(1 to n).gen(k => xs"""
                    $assertThat(Validation.zip(${operands(n, valid, invalid, _ == k)})).isEqualTo(${errors(Seq(k))});
                    $assertThat(Validation.zipWith(${operands(n, valid, invalid, _ == k)}, ${notCalled(n)})).isEqualTo(${errors(Seq(k))});
                  """)(using "\n")}
              }

              @$test
              public void shouldConcatenateTheErrorsOfTwoInvalidsOf${n}InArgumentOrder() {
                  ${(for (k <- 1 to n; m <- (k + 1) to n) yield xs"""
                    $assertThat(Validation.zip(${operands(n, valid, invalid, j => j == k || j == m)})).isEqualTo(${errors(Seq(k, m))});
                    $assertThat(Validation.zipWith(${operands(n, valid, invalid, j => j == k || j == m)}, ${notCalled(n)})).isEqualTo(${errors(Seq(k, m))});
                  """).mkString("\n")}
              }

              @$test
              public void shouldConcatenateTheErrorsOf${n}InvalidsInArgumentOrder() {
                  $assertThat(Validation.zip(${operands(n, valid, invalid, _ => true)})).isEqualTo(${errors(1 to n)});
                  $assertThat(Validation.zipWith(${operands(n, valid, invalid, _ => true)}, ${notCalled(n)})).isEqualTo(${errors(1 to n)});
              }

              @$test
              public void shouldRejectANullResultOfZipWith$n() {
                  $assertThatThrownBy(() -> Validation.zipWith(${all(n)}, (${lambdaParams(n)}) -> null)).isInstanceOf(NullPointerException.class).hasMessage("Validation.zipWith: f returned null");
              }

              @$test
              public void shouldRejectANullArgumentOf$n() {
                  ${(1 to n).gen(k => xs"""
                    $assertThatThrownBy(() -> Validation.zip(${operands(n, valid, _ => "null", _ == k)})).isInstanceOf(NullPointerException.class).hasMessage("v$k is null");
                    $assertThatThrownBy(() -> Validation.zipWith(${operands(n, valid, _ => "null", _ == k)}, ${notCalled(n)})).isInstanceOf(NullPointerException.class).hasMessage("v$k is null");
                  """)(using "\n")}
                  $assertThatThrownBy(() -> Validation.zipWith(${all(n)}, null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
              }
            """)(using "\n\n")}
        }
      """
    })

    /**
     * Lazy: nothing is evaluated before the result is, the operands are evaluated in argument order, once.
     */
    genVavrFile("com.guizmaii.zazr", "LazyZipTest", baseDir = TARGET_TEST)((im: ImportManager, packageName, className) => {

      val test = im.getType("org.junit.jupiter.api.Test")
      val assertThat = im.getStatic("org.assertj.core.api.Assertions.assertThat")
      val assertThatThrownBy = im.getStatic("org.assertj.core.api.Assertions.assertThatThrownBy")
      val AtomicInteger = im.getType("java.util.concurrent.atomic.AtomicInteger")
      val ArrayList = im.getType("java.util.ArrayList")
      val List = im.getType("java.util.List")

      def lazies(n: Int): String = (1 to n).gen(j => s"l$j")(using ", ")
      def tracked(n: Int): String = (1 to n).gen(j => xs"""
        final Lazy<Integer> l$j = Lazy.of(() -> {
            order.add($j);
            return $j;
        });
      """)(using "\n")
      def tupleType(n: Int): String = s"Tuple$n<${(1 to n).gen(_ => "Integer")(using ", ")}>"

      xs"""
        public class $className {

            ${arities.gen(n => xs"""
              @$test
              public void shouldNotEvaluateBeforeTheZipOf${n}Is() {
                  final $List<Integer> order = new $ArrayList<>();
                  ${tracked(n)}
                  final Lazy<${tupleType(n)}> zipped = Lazy.zip(${lazies(n)});
                  final Lazy<String> combined = Lazy.zipWith(${lazies(n)}, (${lambdaParams(n)}) -> ${concatenation(n)});
                  $assertThat(zipped.isEvaluated()).isFalse();
                  $assertThat(combined.isEvaluated()).isFalse();
                  ${(1 to n).gen(j => s"$assertThat(l$j.isEvaluated()).isFalse();")(using "\n")}
                  $assertThat(order).isEmpty();
              }

              @$test
              public void shouldEvaluateTheZipOf${n}InArgumentOrderAndCacheIt() {
                  final $List<Integer> order = new $ArrayList<>();
                  ${tracked(n)}
                  final Lazy<${tupleType(n)}> zipped = Lazy.zip(${lazies(n)});
                  $assertThat(zipped.get()).isEqualTo(Tuple.of(${ints(n)}));
                  $assertThat(order).containsExactly(${ints(n)});
                  $assertThat(zipped.isEvaluated()).isTrue();
                  ${(1 to n).gen(j => s"$assertThat(l$j.isEvaluated()).isTrue();")(using "\n")}
                  $assertThat(zipped.get()).isSameAs(zipped.get());
                  $assertThat(order).containsExactly(${ints(n)});
              }

              @$test
              public void shouldEvaluateTheZipWithOf${n}InArgumentOrderAndCacheIt() {
                  final $List<Integer> order = new $ArrayList<>();
                  final $AtomicInteger calls = new $AtomicInteger();
                  ${tracked(n)}
                  final Lazy<String> combined = Lazy.zipWith(${lazies(n)}, (${lambdaParams(n)}) -> {
                      calls.incrementAndGet();
                      return ${concatenation(n)};
                  });
                  $assertThat(calls.get()).isEqualTo(0);
                  $assertThat(combined.get()).isEqualTo("${digits(n)}");
                  $assertThat(order).containsExactly(${ints(n)});
                  $assertThat(combined.get()).isEqualTo("${digits(n)}");
                  $assertThat(calls.get()).isEqualTo(1);
                  $assertThat(order).containsExactly(${ints(n)});
              }

              @$test
              public void shouldHoldNullFromTheZipWithOf$n() {
                  final Lazy<Object> combined = Lazy.zipWith(${(1 to n).gen(j => s"Lazy.of(() -> $j)")(using ", ")}, (${ignoredParams(n)}) -> null);
                  $assertThat(combined.get()).isNull();
                  $assertThat(combined.isEvaluated()).isTrue();
              }

              @$test
              public void shouldZip${n}WithANullValue() {
                  final Lazy<${tupleType(n)}> zipped = Lazy.zip(${(1 to n).gen(j => if (j == 1) "Lazy.<Integer>of(() -> null)" else s"Lazy.of(() -> $j)")(using ", ")});
                  $assertThat(zipped.get()).isEqualTo(Tuple.of(${(1 to n).gen(j => if (j == 1) "null" else s"$j")(using ", ")}));
              }

              @$test
              public void shouldRejectANullArgumentOf$n() {
                  ${(1 to n).gen(k => xs"""
                    $assertThatThrownBy(() -> Lazy.zip(${(1 to n).gen(j => if (j == k) "null" else s"Lazy.of(() -> $j)")(using ", ")})).isInstanceOf(NullPointerException.class).hasMessage("l$k is null");
                    $assertThatThrownBy(() -> Lazy.zipWith(${(1 to n).gen(j => if (j == k) "null" else s"Lazy.of(() -> $j)")(using ", ")}, ${notCalled(n)})).isInstanceOf(NullPointerException.class).hasMessage("l$k is null");
                  """)(using "\n")}
                  $assertThatThrownBy(() -> Lazy.zipWith(${(1 to n).gen(j => s"Lazy.of(() -> $j)")(using ", ")}, null)).isInstanceOf(NullPointerException.class).hasMessage("f is null");
              }
            """)(using "\n\n")}
        }
      """
    })
  }
}

/**
 * Generates a class file (no license header: see NOTICE at the repository root).
 *
 * @param packageName Java package name
 * @param className Simple java class name
 * @param gen A generator which produces a String.
 */
def genVavrFile(packageName: String, className: String, baseDir: String = TARGET_MAIN)(gen: (ImportManager, String, String) => String, knownSimpleClassNames: List[String] = List()) =
  genJavaFile(baseDir, packageName, className)("")((im, pn, cn) => {
    // Every generated *main* class declares nullable-bounded type parameters (see `nullableBound`),
    // so the JSpecify @Nullable import is always required there. Generated tests declare none.
    if (baseDir == TARGET_MAIN) im.getType("org.jspecify.annotations.Nullable")
    gen(im, pn, cn)
  })(using CHARSET)

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
     J A V A   G E N E R A T O R   F R A M E W O R K
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

object JavaGenerator {

  import java.nio.charset.{Charset, StandardCharsets}

  import Generator._

  /**
   * Generates a Java file.
   *
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
      ${classHeader}package $packageName;

      /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
         G E N E R A T O R   C R A F T E D
      \*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

      ${importManager.getImports}

      $classBody
    """)
  }

  /**
   * A <em>stateful</em> ImportManager which generates an import section of a Java class file.
   *
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
  import java.nio.file.{Files, Paths, StandardOpenOption}

  /**
   * Generates a file by writing string contents to the file system.
   *
   * @param baseDir The base directory, e.g. src-gen
   * @param dirName The directory relative to baseDir, e.g. main/java
   * @param fileName The file name within baseDir/dirName
   * @param createOption One of java.nio.file.{StandardOpenOption.CREATE_NEW, StandardOpenOption.CREATE}, default: CREATE_NEW
   * @param contents The string contents of the file
   * @param charset The charset, by default UTF-8
   */
  def genFile(baseDir: String, dirName: String, fileName: String, createOption: StandardOpenOption = StandardOpenOption.CREATE_NEW)(contents: => String)(implicit charset: Charset = StandardCharsets.UTF_8): Unit = {

    // println(s"Generating $dirName${File.separator}$fileName")

    Files.write(
      Files.createDirectories(Paths.get(baseDir, dirName)).resolve(fileName),
      contents.getBytes(charset),
      createOption, StandardOpenOption.WRITE)
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

    // returns the grammatical number of a string, i.e. `i.numerus("name")` is
    // 0: "no name", 1: "one name", 2: "two names", 3: "three names", 4: "4 names", ...
    def numerus(noun: String): String = Math.abs(i) match {
      case 0 => s"no ${noun}s"
      case 1 => s"one $noun"
      case 2 => s"two ${noun}s"
      case 3 => s"three ${noun}s"
      case _ => s"$i ${noun}s"
    }

    // returns the a pluralized noun, e.g. 0: "names", 1: "name", -1: "name", 2: "names"
    def plural(noun: String): String = noun + (i != 1).gen("s")

  }

  implicit class StringExtensions(s: String) {

    // gets first char of s as string. throws if string is empty
    def first: String = s.substring(0, 1)

    // converts first char of s to upper case. throws if string is empty
    def firstUpper: String = s(0).toUpper + s.substring(1)

    // converts first char of s to lower case. throws if string is empty
    def firstLower: String = s(0).toLower + s.substring(1)
  }

  implicit class BooleanExtensions(condition: Boolean) {
    def gen(s: => String): String =  if (condition) s else ""
  }

  implicit class OptionExtensions(option: Option[Any]) {
    def gen(f: String => String): String =  option.map(any => f.apply(any.toString)).getOrElse("")
    def gen: String = option.map(any => any.toString).getOrElse("")
  }

  /**
   * Generates a String based on ints within a specific range.
   * {{{
   * (1 to 3).gen(i => s"x$i")(using ", ") // x1, x2, x3
   * (1 to 3).reverse.gen(i -> s"x$i")(using ", ") // x3, x2, x1
   * }}}
   *
   * @param range A Range
   */
  implicit class RangeExtensions(range: Range) {
    def gen(f: Int => String = String.valueOf)(implicit delimiter: String = ""): String =
      range map f mkString delimiter
  }

  /**
   * Generates a String based on an Iterable of objects. Objects are converted to Strings via toString.
   * {{{
   * // val a = "A"
   * // val b = "B"
   * // val c = "C"
   * Seq("a", "b", "c").gen(s => raw"""val $s = "${s.toUpperCase}"""")(using "\n")
   * }}}
   *
   * @param iterable An Iterable
   */
  implicit class IterableExtensions(iterable: Iterable[Any]) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      iterable.map(x => f.apply(x.toString)) mkString delimiter
  }

  /**
   * Generates a String based on a tuple of objects. Objects are converted to Strings via toString.
   * {{{
   * // val seq = Seq("a", "1", "true")
   * s"val seq = Seq(${("a", 1, true).gen(s => s""""$s"""")(using ", ")})"
   * }}}
   *
   * @param product A Product (all Scala tuples extend Product)
   */
  implicit class ProductExtensions(product: Product) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      product.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  /**
   * Provides StringContext extensions, e.g. indentation of cascaded rich strings.
   *
   * @param sc Current StringContext
   * @see <a href="https://gist.github.com/danieldietrich/5174348">this gist</a>
   */
  implicit class StringContextExtensions(sc: StringContext) {

    import scala.util.Properties.lineSeparator

    /**
     * Formats escaped strings.
     *
     * @param args StringContext parts
     * @return An aligned String
     */
    def xs(args: Any*): String = align(sc.s, args)

    /**
     * Formats raw/unescaped strings.
     *
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
