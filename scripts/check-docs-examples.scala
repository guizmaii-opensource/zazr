//> using scala 3.9.0
//> using jvm system
//
// Fails when a fenced `java` block of the site or of the Agent Skill does not appear verbatim in one of the docs
// example tests, which compile and run every snippet (so the examples cannot rot).
//
//   scala-cli run scripts/check-docs-examples.scala -- --docs docs --docs skills --exclude docs/design.md TEST_FILE...
//
// Every Markdown file under each `--docs` directory is read, except the `--exclude` ones (docs/design.md is the
// design record, not the user guide) and those under a hidden directory (the generated tables of
// docs/collections/.costs). A block is a fence opened by ```java (indented or not, as inside an admonition or a
// content tab) and closed by the next fence. Its normalised text must occur in the normalised text of one of the
// TEST_FILEs, starting at the start of a statement. The normalised text is the sequence of tokens: the whitespace
// outside the string, char and text block literals and the comments is dropped (one space is kept where it separates
// two words or two operators), a line comment keeps its text with its runs of spaces collapsed, consecutive line
// comments are joined into one, and a block comment has its runs of whitespace collapsed. So the pages align their `=`
// signs and type comments in columns (make docs-align) while the Java formatter lays out the test copies its own way,
// breaking lines, indenting them and rewrapping long comments; the text inside a literal is what the snippet computes,
// so it must match exactly.

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class Block(file: Path, line: Int, text: String)

def isWord(c: Char): Boolean = Character.isLetterOrDigit(c) || c == '_' || c == '$'

def isOperator(c: Char): Boolean = "+-*/%&|^!<>=~?:".indexOf(c) >= 0

// The end of the literal that starts at `start` (a string, a char or a text block), escapes included.
def literalEnd(text: String, start: Int): Int = {
  val quote = if (text.startsWith("\"\"\"", start)) "\"\"\"" else text.charAt(start).toString
  var i = start + quote.length
  while (i < text.length && !text.startsWith(quote, i)) {
    if (text.charAt(i) == '\\') i += 1
    i += 1
  }
  math.min(i + quote.length, text.length)
}

def collapseSpaces(s: String): String = s.trim.split("\\s+").mkString(" ")

def normalise(text: String): String = {
  val out = new StringBuilder
  var space = false
  // where the text of the line comment `out` ends with starts, while no token has followed it: the next line comment
  // continues it
  var comment = -1
  var i = 0
  while (i < text.length) {
    val c = text.charAt(i)
    if (text.startsWith("//", i)) {
      val end = text.indexOf('\n', i) match {
        case -1 => text.length
        case n  => n
      }
      val body = text.substring(i + 2, end)
      if (comment >= 0) {
        val joined = collapseSpaces(out.substring(comment, out.length - 1) + " " + body)
        out.setLength(comment)
        out.append(joined).append('\n')
      } else {
        out.append("//")
        comment = out.length
        out.append(collapseSpaces(body)).append('\n')
      }
      space = false
      i = end
    } else if (Character.isWhitespace(c)) {
      // a blank line ends a run of line comments
      if (c == '\n') {
        val previous = if (i == 0) -1 else text.lastIndexOf('\n', i - 1)
        if (previous >= 0 && text.substring(previous, i).isBlank) comment = -1
      }
      space = true
      i += 1
    } else {
      comment = -1
      if (space && out.nonEmpty && ((isWord(out.last) && isWord(c)) || (isOperator(out.last) && isOperator(c)))) {
        out.append(' ')
      }
      space = false
      if (text.startsWith("/*", i)) {
        val end = text.indexOf("*/", i + 2) match {
          case -1 => text.length
          case n  => n + 2
        }
        out.append(text.substring(i, end).split("\\s+").mkString(" "))
        i = end
      } else if (c == '"' || c == '\'') {
        val end = literalEnd(text, i)
        out.append(text.substring(i, end))
        i = end
      } else {
        out.append(c)
        i += 1
      }
    }
  }
  out.toString
}

// Whether the token before `index` is an annotation without arguments, as `@Nested` before a nested test class.
def afterAnnotation(haystack: String, index: Int): Boolean = {
  var i = index - 1
  if (i < 0 || haystack.charAt(i) != ' ') false
  else {
    while (i > 0 && isWord(haystack.charAt(i - 1))) i -= 1
    i > 0 && i < index - 1 && haystack.charAt(i - 1) == '@'
  }
}

// Whether `needle` occurs in `haystack` at the start of a statement: after nothing, a line comment, or a character that
// ends a statement, a block or a label, and not followed by a character that would continue its last word.
def occurs(needle: String, haystack: String): Boolean = {
  var from = haystack.indexOf(needle)
  var found = false
  while (!found && from >= 0) {
    val end = from + needle.length
    val starts = from == 0 || ";{}:\n".indexOf(haystack.charAt(from - 1)) >= 0 || afterAnnotation(haystack, from)
    val ends = end == haystack.length || !isWord(haystack.charAt(end)) || !isWord(needle.last)
    found = starts && ends
    from = haystack.indexOf(needle, from + 1)
  }
  found
}

// The cases the comparison must get right, run before every check.
def selfTest(): Unit = {
  def check(ok: Boolean, what: String): Unit = {
    if (!ok) {
      System.err.println(s"check-docs-examples self-test failed: $what")
      sys.exit(3)
    }
  }
  def same(a: String, b: String, expected: Boolean): Unit = {
    val should = if (expected) "should" else "should not"
    check((normalise(a) == normalise(b)) == expected, s"[$a] and [$b] $should match")
  }
  def within(needle: String, haystack: String, expected: Boolean): Unit = {
    val should = if (expected) "should" else "should not"
    check(occurs(normalise(needle), normalise(haystack)) == expected, s"[$needle] $should occur in [$haystack]")
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
  same("return x;", "returnx;", false)
  same("var n = a - -b;", "var n = a--b;", false)
  same("var s = f(a,\n        b); // T\n\nvar t = 1;", "var s =\n    f(a, b); // T\nvar t = 1;", true)
  same("// a long comment\n// rewrapped\nvar x = 1;", "// a long\n// comment rewrapped\nvar x = 1;", true)
  same("// a\nvar x = 1;", "var x = 1; // a", false)
  same("/* a\n   b */ var x = 1;", "/* a b */\nvar x = 1;", true)
  same("// a\n\n// b\nvar x = 1;", "// a\n// b\nvar x = 1;", false)
  within("// b\nvar x = 1;", "// a\n\n// b\nvar x = 1;", true)
  within("class T {}", "@Nested\nclass T {}", true)
  within("class T {}", "final class T {}", false)
  within("var x = 1;", "void f() {\n    var x = 1;\n}", true)
  within("x = 1;", "int x = 1;", false)
  within("x = 1;", "max = 1;", false)
  within("var x = f(1);", "var x = f(1);\n", true)
  within("var x = f;", "var x = foo;", false)
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
      found += Block(file, start + 1, normalise(body.result().mkString("\n")))
    }
    i += 1
  }
  found.result()
}

@main def run(args: String*): Unit = {
  selfTest()
  var docs = List.empty[Path]
  var excluded = Set.empty[Path]
  var tests = List.empty[Path]
  var rest = args.toList
  while (rest.nonEmpty) {
    rest match {
      case "--docs" :: d :: tail =>
        docs = docs :+ Path.of(d)
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
    System.err.println("usage: scala-cli run scripts/check-docs-examples.scala -- --docs DIR... [--exclude FILE]... TEST_FILE...")
    sys.exit(2)
  }
  val markdown = docs.flatMap { root =>
    Files.walk(root).iterator.asScala
      .filter(p => p.toString.endsWith(".md") && !excluded(p.normalize))
      .filterNot(p => root.relativize(p).iterator.asScala.exists(_.toString.startsWith(".")))
      .toList
      .sortBy(_.toString)
  }
  val all = markdown.flatMap(blocks)
  val haystacks = tests.map(t => normalise(Files.readString(t, StandardCharsets.UTF_8)))
  val missing = all.filterNot(b => b.text.isEmpty || haystacks.exists(h => occurs(b.text, h)))
  missing.foreach { b =>
    println(s"${b.file}:${b.line}: this java block is not in ${tests.mkString(" or ")}: ${b.text.linesIterator.next()}")
  }
  if (all.isEmpty) {
    println(s"no java block found under ${docs.mkString(" or ")}")
    sys.exit(1)
  }
  if (missing.nonEmpty) {
    println(s"${missing.size} java block(s) not compiled and run by the docs example tests; copy each one verbatim into a test")
    sys.exit(1)
  }
  println(s"docs-examples: ${all.size} java blocks in ${markdown.size} pages, all in the docs example tests")
}
