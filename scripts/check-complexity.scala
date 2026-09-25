//> using scala 3.9.0
//> using jvm system
//
// Fails when a positional method of a collection lacks a "Complexity:" line in its javadoc (design.md 3.7).
//
//   scala-cli run scripts/check-complexity.scala -- zazr-core/src/main/java/com/guizmaii/zazr/collection/Vector.java ...
//
// The files are parsed by the JDK's own compiler (javax.tools + com.sun.source), so the doc comment of a method is
// whatever javac attaches to it, `/** */` or `///` alike, and multi-line signatures or annotations do not matter.
// `using jvm system` runs the script on the JDK in JAVA_HOME (the one the build uses, 25+), because javac must be at
// least as new as the sources it parses. Only the direct members of the top-level types of each file are checked
// (methods of nested classes are not), and only methods whose name is in the list below; every overload is checked
// separately.

import com.sun.source.tree.{ClassTree, MethodTree}
import com.sun.source.util.{DocTrees, JavacTask, TreePath}
import javax.tools.{Diagnostic, DiagnosticCollector, JavaFileObject, ToolProvider}
import scala.jdk.CollectionConverters.*

val positional: Set[String] = Set(
  "get", "update", "insert", "insertAll", "removeAt", "head", "tail", "init", "last", "slice", "subSequence", "take",
  "takeRight", "takeWhile", "takeUntil", "drop", "dropRight", "dropWhile", "dropUntil", "append", "appendAll",
  "prepend", "prependAll", "reverse", "sorted", "sortBy", "zip", "zipAll", "zipWith", "zipWithIndex", "sliding",
  "grouped", "slideBy", "scan", "scanLeft", "scanRight", "indexOf", "lastIndexOf", "indexWhere", "lastIndexWhere", "search",
  "padTo", "patch", "permutations", "combinations", "crossProduct", "intersperse", "rotateLeft", "rotateRight",
  "shuffle", "splitAt", "startsWith", "endsWith", "distinct", "distinctBy", "remove", "removeAll", "removeFirst",
  "removeLast", "replace", "replaceAll", "leftPadTo", "asJava",
  "span", "retainAll", "transpose", "tailOption", "initOption", "iterator",
  // the same, under the names the cons list, the queue, the lazy list and the maps give them
  "asJavaMap", "length", "reverseIterator", "containsSlice", "indexOfSlice", "lastIndexOfSlice", "prefixLength",
  "segmentLength", "splitAtInclusive", "distinctByKeepLast", "dropRightUntil", "dropRightWhile", "takeRightUntil",
  "takeRightWhile", "duplicates", "duplicatesBy",
  "peek", "peekOption", "pop", "popOption", "pop2", "pop2Option", "push", "pushAll",
  "enqueue", "enqueueAll", "dequeue", "dequeueOption",
  "cycle", "extend", "appendSelf"
)

final case class Missing(file: String, line: Int, name: String)

/** Parses the files once and returns (declarations checked, the ones without a Complexity line). */
def check(files: Seq[String]): (Int, List[Missing]) = {
  val compiler = ToolProvider.getSystemJavaCompiler
  val diagnostics = new DiagnosticCollector[JavaFileObject]()
  val fileManager = compiler.getStandardFileManager(diagnostics, null, null)
  try {
    val units = fileManager.getJavaFileObjectsFromStrings(files.asJava)
    val task = compiler.getTask(null, fileManager, diagnostics, java.util.List.of("-proc:none"), null, units).asInstanceOf[JavacTask]
    val docs = DocTrees.instance(task)
    val parsed = task.parse().asScala.toList
    val errors = diagnostics.getDiagnostics.asScala.filter(_.getKind == Diagnostic.Kind.ERROR)
    if (errors.nonEmpty) {
      errors.foreach(d => System.err.println(s"${d.getSource.getName}:${d.getLineNumber}: ${d.getMessage(null)}"))
      sys.exit(2)
    }
    var checked = 0
    val missing = List.newBuilder[Missing]
    for {
      unit <- parsed
      typeDecl <- unit.getTypeDecls.asScala
      cls <- Option(typeDecl).collect { case c: ClassTree => c }
      member <- cls.getMembers.asScala
      method <- Option(member).collect { case m: MethodTree if positional(m.getName.toString) => m }
    } {
      checked += 1
      val path = new TreePath(new TreePath(new TreePath(unit), cls), method)
      val doc = Option(docs.getDocComment(path))
      if (!doc.exists(_.contains("Complexity:"))) {
        val line = unit.getLineMap.getLineNumber(docs.getSourcePositions.getStartPosition(unit, method))
        missing += Missing(unit.getSourceFile.getName, line.toInt, method.getName.toString)
      }
    }
    (checked, missing.result())
  } finally {
    fileManager.close()
  }
}

@main def run(files: String*): Unit = {
  if (files.isEmpty) {
    System.err.println("usage: scala-cli run scripts/check-complexity.scala -- FILE...")
    sys.exit(2)
  }
  val (checked, missing) = check(files)
  missing.foreach(m => println(s"${m.file}:${m.line}: ${m.name}() has no 'Complexity:' line in its javadoc"))
  if (checked == 0) {
    println(s"no positional method found in ${files.mkString(" ")}")
    sys.exit(1)
  }
  if (missing.nonEmpty) {
    println(s"${missing.size} positional method(s) without a 'Complexity:' line (see docs/design.md 3.7)")
    sys.exit(1)
  }
  println(s"complexity: $checked positional method declarations checked, all documented")
}
