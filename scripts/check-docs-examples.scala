//> using scala 3.9.0
//> using jvm system
//
// Fails when a fenced `java` block of the site does not appear verbatim in one of the docs example tests, which
// compile and run every snippet (so the examples cannot rot).
//
//   scala-cli run scripts/check-docs-examples.scala -- --docs docs --exclude docs/design.md TEST_FILE...
//
// Every Markdown file under the `--docs` directory is read, except the `--exclude` ones (docs/design.md is the
// design record, not the user guide) and those under a hidden directory (the generated tables of
// docs/collections/.costs). A block is a fence opened by ```java (indented or not, as inside an admonition or a
// content tab) and closed by the next fence. Its normalised text, each line trimmed, every run of spaces or tabs
// outside a string, char or text block literal collapsed to one space, and the blank lines dropped, must occur as whole
// lines in the normalised text of one of the TEST_FILEs. Collapsing the runs of spaces lets the pages align their `=`
// signs and type comments in columns (make docs-align) while the Java formatter lays out the test copies its own way;
// the text inside a literal is what the snippet computes, so it must match exactly.

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class Block(file: Path, line: Int, text: String)

// Collapses the runs of spaces and tabs of a line outside the literals; `textBlock` says whether the line starts inside
// a text block, and the result says whether it ends inside one.
def collapse(line: String, textBlock: Boolean): (String, Boolean) = {
  val out = new StringBuilder
  var open = textBlock
  var i = 0
  while (i < line.length) {
    val c = line.charAt(i)
    if (open) {
      if (line.startsWith("\"\"\"", i)) {
        out.append("\"\"\"")
        open = false
        i += 3
      } else {
        out.append(c)
        if (c == '\\' && i + 1 < line.length) {
          out.append(line.charAt(i + 1))
          i += 1
        }
        i += 1
      }
    } else if (line.startsWith("\"\"\"", i)) {
      out.append("\"\"\"")
      open = true
      i += 3
    } else if (c == '"' || c == '\'') {
      val start = i
      i += 1
      while (i < line.length && line.charAt(i) != c) {
        if (line.charAt(i) == '\\') i += 1
        i += 1
      }
      i = math.min(i + 1, line.length)
      out.append(line.substring(start, i))
    } else if (c == ' ' || c == '\t') {
      while (i < line.length && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i += 1
      out.append(' ')
    } else {
      out.append(c)
      i += 1
    }
  }
  (out.toString, open)
}

def normalise(lines: Seq[String]): String = {
  var textBlock = false
  lines.map { line =>
    val (collapsed, open) = collapse(if (textBlock) line else line.trim, textBlock)
    textBlock = open
    collapsed.trim
  }.filter(_.nonEmpty).mkString("\n")
}

// The cases the comparison must get right, run before every check.
def selfTest(): Unit = {
  def same(a: String, b: String, expected: Boolean): Unit = {
    val actual = normalise(a.split("\n", -1).toSeq) == normalise(b.split("\n", -1).toSeq)
    if (actual != expected) {
      val should = if (expected) "should" else "should not"
      System.err.println(s"check-docs-examples self-test failed: [$a] and [$b] $should match")
      sys.exit(3)
    }
  }
  same("var env   = Map.of(1, 2);  // Map", "var env = Map.of(1, 2); // Map", true)
  same("var a\t= 1;", "var a = 1;", true)
  same("    var a = 1;", "var a = 1;", true)
  same("var shown = \"no value\";", "var shown = \"no   value\";", false)
  same("var shown = \"no value\";", "var shown = \"no\tvalue\";", false)
  same("var c = ' ';", "var c = '  ';", false)
  same("var e = \"a \\\" b\";  // x", "var e = \"a \\\" b\"; // x", true)
  same("var e = \"a \\\" b\";", "var e = \"a \\\"  b\";", false)
  same("var sql = \"\"\"\n    a  = 1;\n    \"\"\";", "var sql = \"\"\"\n    a = 1;\n    \"\"\";", false)
  same(
    "var sql = \"\"\"\n    a = 1;\n    \"\"\";  // String",
    "var sql = \"\"\"\n    a = 1;\n    \"\"\"; // String",
    true
  )
  same("var x = y;", "var x = z;", false)
}

val opening = """^\s*```java(\s.*)?$""".r
val closing = """^\s*```\s*$""".r

def blocks(file: Path): List[Block] = {
  val lines = Files.readAllLines(file, StandardCharsets.UTF_8).asScala.toVector
  val found = List.newBuilder[Block]
  var i = 0
  while (i < lines.size) {
    if (opening.matches(lines(i))) {
      val start = i
      i += 1
      val body = Vector.newBuilder[String]
      while (i < lines.size && !closing.matches(lines(i))) {
        body += lines(i)
        i += 1
      }
      if (i == lines.size) {
        System.err.println(s"$file:${start + 1}: unclosed ```java fence")
        sys.exit(2)
      }
      found += Block(file, start + 1, normalise(body.result()))
    }
    i += 1
  }
  found.result()
}

@main def run(args: String*): Unit = {
  selfTest()
  var docs = Option.empty[Path]
  var excluded = Set.empty[Path]
  var tests = List.empty[Path]
  var rest = args.toList
  while (rest.nonEmpty) {
    rest match {
      case "--docs" :: d :: tail =>
        docs = Some(Path.of(d))
        rest = tail
      case "--exclude" :: f :: tail =>
        excluded += Path.of(f).normalize
        rest = tail
      case f :: tail =>
        tests = tests :+ Path.of(f)
        rest = tail
      case Nil => ()
    }
  }
  if (docs.isEmpty || tests.isEmpty) {
    System.err.println("usage: scala-cli run scripts/check-docs-examples.scala -- --docs DIR [--exclude FILE]... TEST_FILE...")
    sys.exit(2)
  }
  val markdown = Files.walk(docs.get).iterator.asScala
    .filter(p => p.toString.endsWith(".md") && !excluded(p.normalize))
    .filterNot(p => docs.get.relativize(p).iterator.asScala.exists(_.toString.startsWith(".")))
    .toList
    .sortBy(_.toString)
  val all = markdown.flatMap(blocks)
  val haystacks = tests.map(t => "\n" + normalise(Files.readAllLines(t, StandardCharsets.UTF_8).asScala.toSeq) + "\n")
  val missing = all.filterNot(b => b.text.isEmpty || haystacks.exists(_.contains("\n" + b.text + "\n")))
  missing.foreach { b =>
    println(s"${b.file}:${b.line}: this java block is not in ${tests.mkString(" or ")}: ${b.text.linesIterator.next()}")
  }
  if (all.isEmpty) {
    println(s"no java block found under ${docs.get}")
    sys.exit(1)
  }
  if (missing.nonEmpty) {
    println(s"${missing.size} java block(s) not compiled and run by the docs example tests; copy each one verbatim into a test")
    sys.exit(1)
  }
  println(s"docs-examples: ${all.size} java blocks in ${markdown.size} pages, all in the docs example tests")
}
