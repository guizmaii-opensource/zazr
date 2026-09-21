//> using scala 3.9.0
//
// Fails when a positional method of a collection lacks a "Complexity:" line in its javadoc (design.md 3.7).
//
//   scala-cli run scripts/check-complexity.scala -- zazr-core/src/main/java/com/guizmaii/zazr/collection/Vector.java ...
//
// How a javadoc block is matched to a method: each file is read top to bottom. A `/** ... */` block, or a run of
// `///` lines, is remembered as "the pending javadoc". Blank lines, annotations (`@Override`, `@SuppressWarnings`...)
// and `//` comments between it and the next code line are skipped. The next code line then either declares a method
// at class-member indentation (exactly four spaces, then modifiers or a return type, then `name(`), in which case the
// pending javadoc is that method's, or it is something else (a field, a nested class, a statement of a one-line
// method...), and the pending javadoc is dropped: a javadoc never attaches to a method further down. A method whose
// name is in the list below and whose javadoc (if any) has no line containing "Complexity:" is reported. Every
// overload is checked separately. Only names actually declared in the file at that indentation are checked, so a
// nested class's methods (indented deeper) and names the file does not declare are ignored.

import scala.io.Source

val positional: Set[String] = Set(
  "get", "update", "insert", "insertAll", "removeAt", "head", "tail", "init", "last", "slice", "subSequence", "take",
  "takeRight", "takeWhile", "takeUntil", "drop", "dropRight", "dropWhile", "dropUntil", "append", "appendAll",
  "prepend", "prependAll", "reverse", "sorted", "sortBy", "zip", "zipAll", "zipWith", "zipWithIndex", "sliding",
  "grouped", "scan", "scanLeft", "scanRight", "indexOf", "lastIndexOf", "indexWhere", "lastIndexWhere", "search",
  "padTo", "patch", "permutations", "combinations", "crossProduct", "intersperse", "rotateLeft", "rotateRight",
  "shuffle", "splitAt", "startsWith", "endsWith", "distinct", "distinctBy", "remove", "removeAll", "removeFirst",
  "removeLast", "replace", "replaceAll", "leftPadTo", "asJava",
  "span", "retainAll", "transpose", "tailOption", "initOption", "iterator"
)

// a member declaration: exactly 4 spaces (the fifth column is not a space, so statements of method bodies, indented
// deeper, never match), optional modifiers, optional type parameters, a return type, the name, `(`
val modifiers = """(?:(?:public|protected|private|static|final|abstract|default|synchronized|native)\s+)*"""
val declaration = ("""^ {4}(?=\S)""" + modifiers + """(?:<[^{;=]*?>\s+)?[\w.<>,?\[\]@ ]+?\s+(\w+)\s*\(""").r.unanchored

final case class Missing(file: String, line: Int, name: String)

def check(file: String): (Int, List[Missing]) =
  var doc = List.empty[String]     // the pending javadoc, lines in reverse order
  var hasDoc = false
  var inBlock = false              // inside a /** ... */ block
  var markdown = false             // the pending javadoc is a run of /// lines
  var checked = 0
  val missing = List.newBuilder[Missing]
  val source = Source.fromFile(file)
  try
    for (line, index) <- source.getLines().zipWithIndex do
      val lineNo = index + 1
      val trimmed = line.trim
      if inBlock then
        doc = line :: doc
        if line.contains("*/") then
          inBlock = false
          hasDoc = true
      else if trimmed.startsWith("/**") then
        doc = List(line)
        markdown = false
        inBlock = !line.contains("*/")
        hasDoc = !inBlock
      else if trimmed.startsWith("///") then
        if markdown && hasDoc then doc = line :: doc
        else
          doc = List(line)
          markdown = true
          hasDoc = true
      else if trimmed.isEmpty || trimmed.startsWith("@") || trimmed.startsWith("//") then
        () // skipped: the pending javadoc still belongs to the next code line
      else
        line match
          case declaration(name) if positional(name) =>
            checked += 1
            if !(hasDoc && doc.exists(_.contains("Complexity:"))) then missing += Missing(file, lineNo, name)
          case _ => ()
        doc = Nil
        hasDoc = false
        markdown = false
  finally source.close()
  (checked, missing.result())

@main def run(files: String*): Unit =
  if files.isEmpty then
    System.err.println("usage: scala-cli run scripts/check-complexity.scala -- FILE...")
    sys.exit(2)
  val results = files.map(check)
  val checked = results.map(_._1).sum
  val missing = results.flatMap(_._2)
  missing.foreach(m => println(s"${m.file}:${m.line}: ${m.name}() has no 'Complexity:' line in its javadoc"))
  if checked == 0 then
    println(s"no positional method found in ${files.mkString(" ")}")
    sys.exit(1)
  if missing.nonEmpty then
    println(s"${missing.size} positional method(s) without a 'Complexity:' line (see docs/design.md 3.7)")
    sys.exit(1)
  println(s"complexity: $checked positional method declarations checked, all documented")
