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
// content tab) and closed by the next fence. Its normalised text, each line trimmed, every run of spaces inside a line
// collapsed to one, and the blank lines dropped, must occur as whole lines in the normalised text of one of the
// TEST_FILEs. Collapsing the runs of spaces lets the pages align their `=` signs and type comments in columns (make
// docs-align) while the Java formatter lays out the test copies its own way.

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class Block(file: Path, line: Int, text: String)

def normalise(lines: Seq[String]): String =
  lines.map(_.trim.replaceAll("\\s+", " ")).filter(_.nonEmpty).mkString("\n")

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
