//> using scala 3.9.0
//> using jvm system
//
// Aligns the `=` signs and the trailing `//` comments of consecutive declarations in the fenced `java` blocks of the
// site, so that a block reads as columns. With --check it changes nothing and fails on a block that is not aligned.
//
//   scala-cli run scripts/align-docs-examples.scala -- [--check] [--max COLUMNS] [--exclude FILE]... PATH...
//
// A PATH is a Markdown file or a directory whose Markdown files are read (except the --exclude ones and those under a
// hidden directory). A block is a fence opened by ```java (indented or not) and closed by the next fence.
//
// The rule:
// - a group is a run of consecutive lines of a block that are each a whole statement ending with `;` on one line, at
//   the same indentation; a blank line, a comment line, or a statement spread over several lines ends the group;
// - when two or more lines of a group assign a value (`var x = ...`, `Type x = ...`, `x = ...`), their `=` signs are
//   aligned one space after the longest left-hand side;
// - when two or more lines of a group end with a `//` comment, the comments start at one column, two spaces after the
//   longest of those commented statements (a longer line without a comment does not push the column); a group with a
//   single comment keeps it one space after the `;`;
// - a lone line, and every line outside a group, is left as written.
// An alignment that would push a line past --max columns (the width of a code block on the site, 110 by default) is
// not applied and fails the run in both modes: move that group's type comments to their own line above the
// declaration, or drop the ones the reader does not need.

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

val opening = """^\s*```java(\s.*)?$""".r
val closing = """^\s*```\s*$""".r
val leftHandSide = """^\s*[A-Za-z_$][\w$.<>\[\]?, ]*[\w$]\s*$""".r
val keywordStart = """^\s*(case|return|yield|throw|assert)\b.*""".r

// The code of a line (without a trailing `//` comment and without trailing spaces), the comment (from `//`, or ""),
// the paren/bracket/brace balance of the code, and the index of its first top-level assignment `=` (or -1).
final case class Line(code: String, comment: String, balance: Int, assignment: Int)

def parse(line: String): Line = {
  var i = 0
  var depth = 0
  var balance = 0
  var assignment = -1
  var commentAt = -1
  while (i < line.length && commentAt < 0) {
    val c = line.charAt(i)
    if (c == '"' || c == '\'') {
      i += 1
      while (i < line.length && line.charAt(i) != c) {
        if (line.charAt(i) == '\\') i += 1
        i += 1
      }
    } else if (c == '/' && i + 1 < line.length && line.charAt(i + 1) == '/') {
      commentAt = i
    } else if (c == '(' || c == '[' || c == '{') {
      depth += 1
      balance += 1
    } else if (c == ')' || c == ']' || c == '}') {
      depth -= 1
      balance -= 1
    } else if (c == '=' && depth == 0 && assignment < 0) {
      val next = if (i + 1 < line.length) line.charAt(i + 1) else ' '
      val previous = if (i > 0) line.charAt(i - 1) else ' '
      if (next != '=' && next != '>' && !"=!<>+-*/%&|^".contains(previous)) assignment = i
    }
    i += 1
  }
  val code = (if (commentAt < 0) line else line.substring(0, commentAt)).replaceAll("\\s+$", "")
  val comment = if (commentAt < 0) "" else line.substring(commentAt)
  val isAssignment = assignment >= 0 && assignment < code.length &&
    leftHandSide.matches(code.substring(0, assignment)) && !keywordStart.matches(code)
  Line(code, comment, balance, if (isAssignment) assignment else -1)
}

def indentation(s: String): Int = s.length - s.stripLeading.length

def endsStatement(line: Line): Boolean = {
  val code = line.code.trim
  code.isEmpty || code.endsWith(";") || code.endsWith("{") || code.endsWith("}")
}

final case class Problem(line: Int, message: String)

// Aligns one block (its lines, `first` being the file line of the first one); returns the new lines and the problems.
def alignBlock(lines: Vector[String], first: Int, max: Int): (Vector[String], List[Problem]) = {
  val parsed = lines.map(parse)
  def isStatement(i: Int): Boolean = {
    val line = parsed(i)
    val code = line.code.trim
    code.nonEmpty && code.endsWith(";") && line.balance == 0 && (i == 0 || endsStatement(parsed(i - 1)))
  }
  val out = lines.toArray
  val problems = List.newBuilder[Problem]
  var i = 0
  while (i < lines.size) {
    if (!isStatement(i)) {
      i += 1
    } else {
      val start = i
      val indent = indentation(lines(i))
      i += 1
      while (i < lines.size && isStatement(i) && indentation(lines(i)) == indent) i += 1
      val group = start until i
      if (group.size >= 2) {
        val assigning = group.filter(j => parsed(j).assignment >= 0)
        def lhs(j: Int): String = parsed(j).code.substring(0, parsed(j).assignment).stripTrailing
        val lhsWidth = if (assigning.size >= 2) assigning.map(j => lhs(j).length).max else -1
        val codes = group.map { j =>
          val line = parsed(j)
          if (lhsWidth < 0 || line.assignment < 0) line.code
          else {
            lhs(j).padTo(lhsWidth, ' ') + " = " + line.code.substring(line.assignment + 1).stripLeading
          }
        }
        val commented = group.count(j => parsed(j).comment.nonEmpty)
        val column =
          if (commented == 0) 0
          else group.zip(codes).filter((j, _) => parsed(j).comment.nonEmpty).map(_._2.length).max + 2
        val aligned = group.zip(codes).map { (j, code) =>
          val comment = parsed(j).comment
          if (comment.isEmpty) code
          else if (commented >= 2) code.padTo(column, ' ') + comment
          else code + " " + comment
        }
        val tooWide = group.zip(aligned).filter { (j, line) =>
          val natural = parsed(j).code.length + (if (parsed(j).comment.isEmpty) 0 else 1 + parsed(j).comment.length)
          line.length > max && line.length > natural
        }
        if (tooWide.nonEmpty) {
          tooWide.foreach { (j, line) =>
            problems += Problem(
              first + j,
              s"aligning this group makes the line ${line.length} columns wide (the limit is $max): move the group's " +
                "type comments to their own line above each declaration, or drop the ones the reader does not need"
            )
          }
        } else {
          group.zip(aligned).foreach((j, line) => out(j) = line)
        }
      }
    }
  }
  (out.toVector, problems.result())
}

@main def run(args: String*): Unit = {
  var check = false
  var max = 110
  var excluded = Set.empty[Path]
  var paths = List.empty[Path]
  var rest = args.toList
  while (rest.nonEmpty) {
    rest match {
      case "--check" :: tail =>
        check = true
        rest = tail
      case "--max" :: n :: tail =>
        max = n.toInt
        rest = tail
      case "--exclude" :: f :: tail =>
        excluded += Path.of(f).normalize
        rest = tail
      case p :: tail =>
        paths = paths :+ Path.of(p)
        rest = tail
      case Nil => ()
    }
  }
  if (paths.isEmpty) {
    System.err.println(
      "usage: scala-cli run scripts/align-docs-examples.scala -- [--check] [--max COLUMNS] [--exclude FILE]... PATH..."
    )
    sys.exit(2)
  }
  val markdown = paths.flatMap { root =>
    if (Files.isDirectory(root)) {
      Files.walk(root).iterator.asScala
        .filter(p => p.toString.endsWith(".md"))
        .filterNot(p => root.relativize(p).iterator.asScala.exists(_.toString.startsWith(".")))
        .toList
    } else List(root)
  }.filterNot(p => excluded(p.normalize)).distinct.sortBy(_.toString)

  var blocks = 0
  val misaligned = List.newBuilder[String]
  val problems = List.newBuilder[String]
  val changedFiles = List.newBuilder[Path]
  markdown.foreach { file =>
    val content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    val lines = content.split("\n", -1).toVector
    val out = lines.toArray
    var i = 0
    while (i < lines.size) {
      if (opening.matches(lines(i))) {
        val start = i + 1
        i += 1
        while (i < lines.size && !closing.matches(lines(i))) i += 1
        if (i == lines.size) {
          System.err.println(s"$file:$start: unclosed ```java fence")
          sys.exit(2)
        }
        blocks += 1
        val body = lines.slice(start, i)
        val (aligned, found) = alignBlock(body, start + 1, max)
        found.foreach(p => problems += s"$file:${p.line}: ${p.message}")
        body.indices.find(j => body(j) != aligned(j)).foreach { j =>
          misaligned += s"$file:${start + j + 1}: not aligned: ${body(j).trim}"
        }
        aligned.indices.foreach(j => out(start + j) = aligned(j))
      }
      i += 1
    }
    val updated = out.mkString("\n")
    if (updated != content) {
      changedFiles += file
      if (!check) Files.write(file, updated.getBytes(StandardCharsets.UTF_8))
    }
  }
  val misalignedBlocks = misaligned.result()
  val tooWide = problems.result()
  tooWide.foreach(println)
  if (check) {
    misalignedBlocks.foreach(println)
    if (misalignedBlocks.nonEmpty) {
      println(s"${misalignedBlocks.size} java block(s) not aligned: run make docs-align and commit the result")
    }
  } else {
    changedFiles.result().foreach(f => println(s"aligned $f"))
  }
  if (tooWide.nonEmpty) {
    println(s"${tooWide.size} line(s) would be wider than $max columns once aligned; the groups were left as written")
  }
  if ((check && misalignedBlocks.nonEmpty) || tooWide.nonEmpty) sys.exit(1)
  if (check) println(s"docs-align: ${blocks} java blocks in ${markdown.size} files, all aligned")
}
