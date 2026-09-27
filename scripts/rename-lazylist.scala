//> using scala 3.9.0
//> using jvm system
//
// One-off migration: renames the lazy list `dev.zazr.collection.Stream` to `LazyList`, across the code, the tests, the
// site, the Agent Skill, the README and CLAUDE.md. It is idempotent, so it can be run again on a branch after merging
// code written against the old name, then followed by the usual regeneration steps:
//
//   scala-cli run scripts/rename-lazylist.scala
//   make fmt && make docs-complexity && make docs-align
//
// It runs from the repository root, on the files tracked by git, and leaves `docs/design.md` (the decision log, whose
// entries say what was true when they were written) and itself untouched.
//
// What it does:
//   1. `git mv` of the files named after the type (the class, its internal module, their tests, the site page and its
//      generated cost table). When a merge has brought the old file back beside the new one, the old one wins: it holds
//      the incoming changes, and renaming it again reproduces everything else.
//   2. Textual replacements. `java.util.stream.Stream` is masked first, and a file that imports
//      `java.util.stream.Stream` keeps its bare `Stream` (there it is the JDK type). The bare word `Stream`, its plural,
//      the Zazr identifiers made from it (`StreamModule`, `toStream`, `Stream...Tests`, ...), the zazr-test generator
//      `Gen.stream`, and the site links to the page are renamed.
//   3. It rewords the few sentences of the site and the skill about the name clash, which the rename makes untrue.
//   4. It prints the occurrences of `Stream` left in identifiers, for review: they name the JDK type or `stream()`.
//
// Deliberately kept: `java.util.stream.*` (`Stream`, `IntStream`, `LongStream`, `DoubleStream`, `StreamSupport`,
// `Collector`), `stream()` and `parallelStream()` (the JDK conversion) and the variables and parameters named `stream`,
// the identifiers that take a JDK stream (`javaStream`, `ofJavaStream`, `Maps.ofStream`, the `should...JavaStream`,
// `should...IntStream`, `shouldStream...` tests), the JDK I/O types (`PrintStream`, `InputStream`, ...), and the
// inherited test names outside `LazyListTest` that use the word loosely (`IteratorTest.shouldGenerateInfiniteStream...`,
// `TreeSetTest.shouldConstructStreamFrom...JavaStream`). Inside `LazyList.java` and `LazyListTest.java`, a `Stream` in
// an identifier is the lazy list unless a JDK word precedes it (`shouldCycleEmptyStream` becomes
// `shouldCycleEmptyLazyList`, `shouldGenerateIntStream` stays).

import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.nio.ByteBuffer
import java.nio.file.{Files, Path, Paths}
import scala.util.matching.Regex

val Self = "scripts/rename-lazylist.scala"
val Excluded = Set(Self, "docs/design.md")

val Moves: List[(String, String)] = List(
  "zazr-core/src/main/java/dev/zazr/collection/Stream.java" -> "zazr-core/src/main/java/dev/zazr/collection/LazyList.java",
  "zazr-core/src/main/java/dev/zazr/collection/internal/StreamModule.java" ->
    "zazr-core/src/main/java/dev/zazr/collection/internal/LazyListModule.java",
  "zazr-core/src/test/java/dev/zazr/collection/StreamTest.java" ->
    "zazr-core/src/test/java/dev/zazr/collection/LazyListTest.java",
  "zazr-test/src/test/java/dev/zazr/test/laws/StreamLawsTest.java" ->
    "zazr-test/src/test/java/dev/zazr/test/laws/LazyListLawsTest.java",
  "docs/collections/stream.md" -> "docs/collections/lazy-list.md",
  "docs/collections/.costs/Stream.md" -> "docs/collections/.costs/LazyList.md"
)

// The masked JDK type: a string that no rule matches.
val JdkStream = "java.util.stream.Stream"
val Mask = "\u0000JDK_STREAM\u0000"

final case class Rule(pattern: Regex, replacement: String, applies: String => Boolean = _ => true)

def only(paths: String*): String => Boolean = path => paths.contains(path)
def suffix(s: String): String => Boolean = path => path.endsWith(s)

val GenFiles = only("zazr-test/src/main/java/dev/zazr/test/Gen.java", "zazr-test/src/main/java/dev/zazr/test/Shapes.java")
// The files about the type, where a `Stream` inside an identifier is the lazy list unless a JDK word precedes it.
val LazyListFiles = only(
  "zazr-core/src/main/java/dev/zazr/collection/LazyList.java",
  "zazr-core/src/test/java/dev/zazr/collection/LazyListTest.java"
)
val ProseFiles = only(
  "zazr-core/src/main/java/dev/zazr/collection/LazyList.java",
  "zazr-core/src/main/java/dev/zazr/collection/internal/LazyListModule.java",
  "zazr-test/src/main/java/dev/zazr/test/Gen.java",
  "zazr-test/src/main/java/dev/zazr/test/Shapes.java"
)

// Applied in order, to every line of every file.
val Rules: List[Rule] = List(
  // identifiers made from the name
  Rule("""\btoStream\b""".r, "toLazyList"),
  Rule("""\bTostreamTests\b""".r, "ToLazyListTests"),
  Rule("""(?<=[a-z])ToStream(?=[A-Z]|\b)""".r, "ToLazyList"),
  Rule("""\blazyStream\b""".r, "lazyChain"),
  Rule("""\bSTREAM_LAYOUTS\b""".r, "LAZY_LIST_LAYOUTS"),
  Rule("""\bStream(?=[A-Z])(?!Support)""".r, "LazyList"),
  Rule("""(?<=[A-Za-z0-9])(?<!java)(?<!Java)(?<!Util)(?<!Int)(?<!Long)(?<!Double)(?<!arallel)(?<!should)(?<!\bof)Stream""".r, "LazyList", LazyListFiles),
  Rule("""\b(\w*(?:ElementsAreInfinite|OfAFinite|WhenThe))Stream""".r, "$1LazyList"),
  // the zazr-test generator
  Rule("""\b(Gen|Shapes)\.stream\(""".r, "$1.lazyList("),
  Rule("""(?<![.\w])stream\((?!\))""".r, "lazyList(", GenFiles),
  Rule("""\bstream(?=[A-Z])""".r, "lazyList", suffix("zazr-test/src/test/java/dev/zazr/test/GenTypesTest.java")),
  Rule(""""stream"""".r, "\"lazyList\"", suffix("zazr-test/src/test/java/dev/zazr/test/GenTypesTest.java")),
  Rule("""`stream`""".r, "`lazyList`", only("docs/testing.md")),
  // the site page
  Rule("""(?<=[(/ ])stream\.md\b""".r, "lazy-list.md"),
  Rule("""zazr\.dev/collections/stream/""".r, "zazr.dev/collections/lazy-list/"),
  Rule("""\bcomplexity\.md#stream\b""".r, "complexity.md#lazylist"),
  // the type
  Rule("""\bStreams\b""".r, "LazyLists"),
  Rule("""\bStream\b""".r, "LazyList")
)

// Lowercase "stream" as a noun in the javadoc of the files about the type (not in the `//` comments, where it names
// a variable).
val Comment = """^\s*(\*|///)""".r
val ProseStream = """(?<!@param )(?<!\{@code )(?<!lazily )(?<![`.\w])\bstream(s?)\b(?![(`}\w])(?!\.\w)""".r

// The sentences about the name clash with `java.util.stream.Stream`, which the rename makes untrue, as the rules above
// leave them and as they are reworded.
final case class Rewording(path: String, from: String, to: String)

val Rewordings: List[Rewording] = List(
  Rewording(
    "docs/collections/lazy-list.md",
    "Its name clashes with `java.util.stream.Stream`: import `dev.zazr.collection.LazyList`, and write the JDK one\n" +
      "in full when you need both.\n",
    "Unlike a `java.util.stream.Stream`, which is a one-shot pipeline, a `LazyList` is a collection: it can be read\n" +
      "many times, and each read after the first reuses what was computed.\n"
  ),
  Rewording(
    "docs/getting-started.md",
    "`List` and `LazyList` share their names with `java.util.List` and `java.util.stream.Stream`: import the Zazr ones " +
      "and\nspell the JDK ones out, as the examples on this site do.",
    "`List` shares its name with `java.util.List`: import the Zazr one and spell the JDK one out, as the examples on " +
      "this\nsite do."
  ),
  Rewording(
    "skills/zazr/SKILL.md",
    "`List` and `LazyList` clash with the JDK names: import Zazr's and write\n" +
      "   `java.util.List` and `java.util.stream.Stream` in full.",
    "`List` clashes with `java.util.List`: import Zazr's and write the JDK\n   one in full."
  ),
  Rewording(
    "skills/zazr/references/collections.md",
    "`List` and `LazyList` clash with `java.util.List` and `java.util.stream.Stream`: import Zazr's, spell the JDK ones " +
      "out.",
    "`List` clashes with `java.util.List`: import Zazr's, spell the JDK one out."
  ),
  Rewording(
    "skills/zazr/references/functional-java.md",
    "Imports: Zazr's `List`, `LazyList`, `Map`, `Set` and `Queue` share their names with JDK types. Import Zazr's and " +
      "write\nthe JDK ones in full (`java.util.List`, `java.util.stream.Stream`).",
    "Imports: Zazr's `List`, `Map`, `Set` and `Queue` share their names with JDK types. Import Zazr's and write the " +
      "JDK\nones in full (`java.util.List`, `java.util.Map`)."
  ),
  Rewording(
    "skills/zazr/references/functional-java.md",
    "Zazr's `LazyList` is a lazy list that keeps what it computed",
    "Zazr's `LazyList` keeps what it computed"
  )
)

// The redirect kept for the old address of the page.
val Redirect = "        collections/stream.md: collections/lazy-list.md"

def git(args: String*): String = {
  val process = new ProcessBuilder(("git" +: args)*).redirectErrorStream(true).start()
  val out = new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
  if (process.waitFor() != 0) {
    System.err.println(s"git ${args.mkString(" ")} failed:\n$out")
    sys.exit(2)
  }
  out
}

def readText(path: Path): Option[String] = {
  val bytes = Files.readAllBytes(path)
  if (bytes.contains(0.toByte)) None
  else {
    try {
      Some(
        StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString
      )
    } catch {
      case _: CharacterCodingException => None
    }
  }
}

def move(from: String, to: String): Unit = {
  val (source, target) = (Paths.get(from), Paths.get(to))
  if (Files.exists(source)) {
    if (Files.exists(target)) {
      println(s"$from came back beside $to: it replaces it")
      git("rm", "-q", "-f", to)
    }
    git("mv", from, to)
    println(s"moved $from -> $to")
  }
}

def rewriteLine(path: String, line: String, jdkStreamImported: Boolean): String = {
  var out = line.replace(JdkStream, Mask)
  for (rule <- Rules if rule.applies(path)) {
    val bare = rule.pattern.regex == """\bStream\b""" || rule.pattern.regex == """\bStreams\b"""
    if (!(bare && jdkStreamImported)) {
      out = rule.pattern.replaceAllIn(out, rule.replacement)
    }
  }
  if (ProseFiles(path) && Comment.findPrefixOf(out).isDefined) {
    out = ProseStream.replaceAllIn(out, m => s"lazy list${m.group(1)}")
  }
  out.replace(Mask, JdkStream)
}

def rewrite(path: String, text: String): String = {
  val jdkStreamImported = text.contains("import java.util.stream.Stream;")
  val lines = text.split("\n", -1).toList.map { line =>
    if (path == "mkdocs.yml" && line == Redirect) line else rewriteLine(path, line, jdkStreamImported)
  }
  val rewritten = Rewordings.filter(_.path == path).foldLeft(lines.mkString("\n")) { (text, r) =>
    text.replace(r.from, r.to)
  }
  if (path == "mkdocs.yml" && !rewritten.contains(Redirect)) {
    rewritten.replace("      redirect_maps:\n", s"      redirect_maps:\n$Redirect\n")
  } else {
    rewritten
  }
}

@main def run(): Unit = {
  if (!Files.exists(Paths.get("pom.xml")) || !Files.exists(Paths.get("scripts/rename-lazylist.scala"))) {
    System.err.println("run from the repository root: scala-cli run scripts/rename-lazylist.scala")
    sys.exit(2)
  }
  Moves.foreach(move)
  val files = git("ls-files", "-z").split('\u0000').filter(_.nonEmpty).filterNot(Excluded).toList
  var changed = 0
  val kept = scala.collection.mutable.TreeMap.empty[String, Int]
  for (file <- files) {
    val path = Paths.get(file)
    if (Files.isRegularFile(path)) {
      readText(path).foreach { text =>
        val rewritten = rewrite(file, text)
        if (rewritten != text) {
          Files.writeString(path, rewritten, StandardCharsets.UTF_8)
          changed += 1
        }
        """\w*Stream\w*""".r.findAllIn(rewritten).foreach(word => kept(word) = kept.getOrElse(word, 0) + 1)
      }
    }
  }
  println(s"$changed files rewritten")
  println("identifiers still containing Stream (JDK types, stream() and the tests about them):")
  kept.foreach { (word, count) => println(f"  $count%5d $word") }
}
