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
