//> using scala 3.9.0
//> using jvm system
//
// The "Complexity:" notes of the collections (design.md 3.7): a guard, and the generator of the complexity page.
//
//   scala-cli run scripts/check-complexity.scala -- [--context FILE]... [--page OUT] [--glance DIR] FILE...
//
// Every FILE is checked: a method of one of its top-level types whose name is in `documented` below must have a
// javadoc paragraph starting with "Complexity:", either its own or, when it overrides a method of a supertype
// declared in the parsed files, that supertype's (the notes of TreeSet and TreeMap live on SortedSet and SortedMap).
// The note must start with one of the expressions of `vocabulary`, so that the page can classify it; the rest of the
// note is free text. A `--context` file is parsed for the supertype chain (Traversable, Set, Map) but not checked.
// With `--page OUT` the script also writes the complexity page (docs/collections/complexity.md) from the notes:
// matrices of the common operations per family, then every documented method of every type. With `--glance DIR` it
// writes one table per type, the family's operations with their whole notes, that the collection pages include.
//
// The files are parsed by the JDK's own compiler (javax.tools + com.sun.source), so the doc comment of a method is
// whatever javac attaches to it, `/** */` or `///` alike, and multi-line signatures or annotations do not matter.
// `using jvm system` runs the script on the JDK in JAVA_HOME (the one the build uses, 25+), because javac must be at
// least as new as the sources it parses. Only the direct members of the top-level types of each file are checked
// (methods of nested classes are not), only the API ones (public or protected, or not private in an interface), and
// every overload is checked separately.

import com.sun.source.tree.{ClassTree, MethodTree, Tree}
import javax.lang.model.element.Modifier
import com.sun.source.util.{DocTrees, JavacTask, TreePath}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import javax.tools.{Diagnostic, DiagnosticCollector, JavaFileObject, ToolProvider}
import scala.jdk.CollectionConverters.*

val documented: Set[String] = Set(
  "get", "update", "insert", "insertAll", "removeAt", "head", "tail", "init", "last", "slice", "subSequence", "take",
  "takeRight", "takeWhile", "takeUntil", "drop", "dropRight", "dropWhile", "dropUntil", "append", "appendAll",
  "prepend", "prependAll", "reverse", "sorted", "sortBy", "zip", "zipAll", "zipWith", "zipWithIndex", "sliding",
  "grouped", "slideBy", "scan", "scanLeft", "scanRight", "indexOf", "lastIndexOf", "indexWhere", "lastIndexWhere", "search",
  "padTo", "patch", "permutations", "combinations", "crossProduct", "intersperse", "rotateLeft", "rotateRight",
  "shuffle", "splitAt", "startsWith", "endsWith", "distinct", "distinctBy", "remove", "removeAll", "removeFirst",
  "removeLast", "replace", "replaceAll", "leftPadTo", "asJava",
  "span", "retainAll", "transpose", "tailOption", "initOption", "iterator",
  // the same, under the names the cons list, the queue and the lazy list give them
  "asJavaMutable", "length", "reverseIterator", "containsSlice", "indexOfSlice", "lastIndexOfSlice", "prefixLength",
  "segmentLength", "splitAtInclusive", "distinctByKeepLast", "dropRightUntil", "dropRightWhile", "takeRightUntil",
  "takeRightWhile", "duplicates", "duplicatesBy",
  "peek", "peekOption", "pop", "popOption", "pop2", "pop2Option", "push", "pushAll",
  "enqueue", "enqueueAll", "dequeue", "dequeueOption",
  "cycle", "extend", "appendSelf",
  // the rows of the complexity page that are not positional: lookups, updates and the set algebra
  "contains", "concat", "add", "addAll", "put", "min", "max", "union", "intersect", "diff", "containsKey",
  "keySet", "values"
)

/** The complexity classes of the legend, cheapest first. */
enum Cost(val label: String, val scala: String, val meaning: String) {
  case Constant extends Cost("constant", "C", "a fixed number of steps, whatever the size")
  case EffectivelyConstant extends Cost("effectively constant", "eC",
    "O(log32 n): a walk or path copy of at most six trie levels (Vector) or a hash lookup in a 32-way trie (HashSet, HashMap)")
  case AmortisedConstant extends Cost("amortised constant", "aC",
    "constant on average over a sequence of operations; a single call may take O(n) (Queue reverses its rear list)")
  case Logarithmic extends Cost("logarithmic", "Log", "O(log n): one walk from the root of a balanced tree")
  case Lazy extends Cost("lazy", "",
    "nothing is computed now; each element is computed when the result reaches it (the note says what is forced)")
  case Linear extends Cost("linear", "L", "proportional to the number of elements named in the expression")
  case Linearithmic extends Cost("n log n", "", "a sort, or one tree operation per element")
  case Polynomial extends Cost("polynomial", "", "a product of sizes: a slice search, a matrix, a cartesian product")
  case Combinatorial extends Cost("combinatorial", "", "one result per permutation or combination")
}

/** The accepted leading expressions of a note. A new one is added here, with its class, before it is used. */
val vocabulary: Map[String, Cost] = Map(
  "O(1)" -> Cost.Constant,
  "effectively O(1)" -> Cost.EffectivelyConstant,
  "amortised O(1)" -> Cost.AmortisedConstant,
  "O(log n)" -> Cost.Logarithmic,
  "lazy" -> Cost.Lazy,
  "O(n)" -> Cost.Linear,
  "O(k)" -> Cost.Linear,
  "O(m)" -> Cost.Linear,
  "O(n + m)" -> Cost.Linear,
  "O(m + n)" -> Cost.Linear,
  "O(n + k)" -> Cost.Linear,
  "O(index)" -> Cost.Linear,
  "O(index + m)" -> Cost.Linear,
  "O(offset + m)" -> Cost.Linear,
  "O(from + k)" -> Cost.Linear,
  "O(beginIndex)" -> Cost.Linear,
  "O(endIndex)" -> Cost.Linear,
  "O(length - k)" -> Cost.Linear,
  "O(min(n, m))" -> Cost.Linear,
  "O(max(n, m))" -> Cost.Linear,
  "O(min(i, n - i))" -> Cost.Linear,
  "O(m + min(i, n - i))" -> Cost.Linear,
  "effectively O(min(n, size - n))" -> Cost.Linear,
  "O(n / step)" -> Cost.Linear,
  "O(n / size)" -> Cost.Linear,
  "O(k + log n)" -> Cost.Linear,
  "O(n log n)" -> Cost.Linearithmic,
  "O(m log(n + m))" -> Cost.Linearithmic,
  "O((n + m) log n)" -> Cost.Linearithmic,
  "O(m log n)" -> Cost.Linearithmic,
  "O(m + n log n)" -> Cost.Linearithmic,
  "O(n + r log n)" -> Cost.Linearithmic,
  "O((n / step) log n)" -> Cost.Linearithmic,
  "O((n / size) log n)" -> Cost.Linearithmic,
  "O(n * m)" -> Cost.Polynomial,
  "O(n * size)" -> Cost.Polynomial,
  "O(n * size / step)" -> Cost.Polynomial,
  "O(rows * columns)" -> Cost.Polynomial,
  "O(n^2)" -> Cost.Polynomial,
  "O(n^power)" -> Cost.Polynomial,
  "O(n + (n / step) * min(size, n - size))" -> Cost.Polynomial,
  "O(2^n)" -> Cost.Combinatorial,
  "O(n!)" -> Cost.Combinatorial,
  "O(n! * n)" -> Cost.Combinatorial,
  "O(C(n, k))" -> Cost.Combinatorial
)

final case class Decl(
    owner: String,
    name: String,
    params: List[String],
    signature: String,
    doc: Option[String],
    note: Option[String],
    file: String,
    line: Int,
    isStatic: Boolean
)

final case class TypeInfo(name: String, supers: List[String], decls: List[Decl], checked: Boolean)

/** The erased shape of a parameter type: no type arguments, no annotations, a type variable as Object. */
def erase(tpe: String): String = {
  var s = tpe.replaceAll("@[A-Za-z.]+\\s*", "")
  var prev = ""
  while (prev != s) {
    prev = s
    s = s.replaceAll("<[^<>]*>", "")
  }
  s = s.replace("...", "[]").trim
  if (s.matches("[A-Z][0-9]?(\\[\\])*")) s.replaceAll("^[A-Z][0-9]?", "Object") else s
}

/** The "Complexity:" paragraph of a doc comment, on one line. */
def noteOf(doc: String): Option[String] = {
  val start = doc.indexOf("Complexity:")
  if (start < 0) None
  else {
    val rest = doc.substring(start + "Complexity:".length)
    val lines = rest.linesIterator.toList
    val kept = lines.head :: lines.tail.takeWhile { l =>
      val t = l.trim
      t.nonEmpty && !t.startsWith("<p>") && !t.startsWith("@") && !t.startsWith("<")
    }
    Some(kept.map(_.trim).mkString(" ").replaceAll("\\s+", " ").trim)
  }
}

/** The leading expression of a note and its class, if the note starts with one of the vocabulary. */
def classify(note: String): Option[(String, Cost)] =
  vocabulary.toList
    .filter { case (expr, _) =>
      note.startsWith(expr) && {
        val after = note.drop(expr.length)
        after.isEmpty || after.head == '.' || after.head == ';' || after.head == ',' || after.head == ' ' ||
        after.head == ':'
      }
    }
    .sortBy { case (expr, _) => -expr.length }
    .headOption

def parse(files: Seq[String], checked: Set[String]): List[TypeInfo] = {
  val compiler = ToolProvider.getSystemJavaCompiler
  val diagnostics = new DiagnosticCollector[JavaFileObject]()
  val fileManager = compiler.getStandardFileManager(diagnostics, null, null)
  try {
    val units = fileManager.getJavaFileObjectsFromStrings(files.asJava)
    val task = compiler.getTask(null, fileManager, diagnostics, java.util.List.of("-proc:none"), null, units)
      .asInstanceOf[JavacTask]
    val docs = DocTrees.instance(task)
    val parsed = task.parse().asScala.toList
    val errors = diagnostics.getDiagnostics.asScala.filter(_.getKind == Diagnostic.Kind.ERROR)
    if (errors.nonEmpty) {
      errors.foreach(d => System.err.println(s"${d.getSource.getName}:${d.getLineNumber}: ${d.getMessage(null)}"))
      sys.exit(2)
    }
    for {
      unit <- parsed
      typeDecl <- unit.getTypeDecls.asScala.toList
      cls <- Option(typeDecl).collect { case c: ClassTree => c }.toList
    } yield {
      val file = unit.getSourceFile.getName
      val supers = (Option(cls.getExtendsClause).toList ++ cls.getImplementsClause.asScala)
        .map(t => erase(t.toString))
      val isInterface = cls.getKind == Tree.Kind.INTERFACE
      def isApi(m: MethodTree): Boolean = {
        val flags = m.getModifiers.getFlags
        isInterface && !flags.contains(Modifier.PRIVATE) || flags.contains(Modifier.PUBLIC) || flags.contains(Modifier.PROTECTED)
      }
      def docOf(m: MethodTree): Option[String] =
        Option(docs.getDocComment(new TreePath(new TreePath(new TreePath(unit), cls), m)))
      val decls = cls.getMembers.asScala.toList.collect {
        case m: MethodTree if isApi(m) && (documented(m.getName.toString) || docOf(m).exists(_.contains("Complexity:"))) =>
        val doc = docOf(m)
        val params = m.getParameters.asScala.toList.map(p => erase(p.getType.toString))
        val line = unit.getLineMap.getLineNumber(docs.getSourcePositions.getStartPosition(unit, m)).toInt
        val signature = s"${m.getName}(${m.getParameters.asScala.map(p => simpleType(p.getType.toString)).mkString(", ")})"
        val isStatic = m.getModifiers.getFlags.contains(Modifier.STATIC)
        Decl(cls.getSimpleName.toString, m.getName.toString, params, signature, doc, doc.flatMap(noteOf), file, line, isStatic)
      }
      TypeInfo(cls.getSimpleName.toString, supers, decls, checked(file))
    }
  } finally {
    fileManager.close()
  }
}

/** A parameter type as the page shows it: no annotations, no package, the type arguments kept. */
def simpleType(tpe: String): String =
  tpe.replaceAll("@[A-Za-z.]+\\s*", "").replaceAll("\\b(?:[a-z]+\\.)+([A-Z])", "$1").trim

/** The note of `decl`, its own or the one of the method it overrides in the nearest supertype that documents it. */
def resolve(types: Map[String, TypeInfo], decl: Decl): Option[Decl] =
  if (decl.note.isDefined) Some(decl)
  else {
    val owner = types(decl.owner)
    owner.supers.iterator
      .flatMap(types.get)
      .flatMap(t => t.decls.find(d => d.name == decl.name && d.params == decl.params).flatMap(resolve(types, _)))
      .nextOption()
  }

/** The declaration a type uses for `name(params)`: its own, or the nearest supertype's. */
def lookup(types: Map[String, TypeInfo], typeName: String, name: String, params: List[String]): Option[Decl] =
  types.get(typeName).flatMap { t =>
    t.decls.find(d => d.name == name && d.params == params)
      .orElse(t.supers.iterator.flatMap(s => lookup(types, s, name, params)).nextOption())
  }

/** Every overload of `name` a type has, its own first, then the inherited ones it does not override. */
def overloads(types: Map[String, TypeInfo], typeName: String, name: String): List[Decl] =
  types.get(typeName).toList.flatMap { t =>
    val own = t.decls.filter(d => d.name == name && !d.isStatic)
    val inherited = t.supers.flatMap(s => overloads(types, s, name)).filterNot(d => own.exists(_.params == d.params))
    (own ++ inherited).distinctBy(_.params)
  }

final case class Problem(file: String, line: Int, message: String)

def check(types: List[TypeInfo]): (Int, List[Problem]) = {
  val byName = types.map(t => t.name -> t).toMap
  val decls = types.filter(_.checked).flatMap(_.decls)
  val problems = decls.flatMap { d =>
    if (d.note.isEmpty && documented(d.name) && resolve(byName, d).isEmpty) {
      List(Problem(d.file, d.line, s"${d.name}() has no 'Complexity:' line in its javadoc"))
    } else if (d.note.exists(n => classify(n).isEmpty)) {
      List(Problem(d.file, d.line,
        s"${d.name}(): the 'Complexity:' note does not start with an expression of the vocabulary: ${d.note.get}"))
    } else {
      Nil
    }
  }
  (decls.size, problems)
}

// ---- the page

final case class Family(title: String, intro: String, columns: List[String], rows: List[String])

val families: List[Family] = List(
  Family(
    "Sequences",
    "`NonEmptyVector` wraps a `Vector`, so its costs are `Vector`'s; `Stream` is lazy, so most of its operations are " +
      "deferred until the result is read.",
    List("Vector", "List", "Queue", "Stream", "NonEmptyVector"),
    List("head", "tail", "last", "init", "get", "update", "prepend", "append", "prependAll", "appendAll", "insert",
      "removeAt", "take", "drop", "slice", "splitAt", "reverse", "sorted", "length", "contains", "indexOf",
      "zip", "sliding", "grouped", "distinct", "concat")
  ),
  Family(
    "Sets",
    "`TreeSet` inherits its notes from `SortedSet`. `head`, `take` and `drop` exist only where the order is defined: " +
      "insertion order on `LinkedHashSet`, the comparator's order on `TreeSet`.",
    List("HashSet", "LinkedHashSet", "TreeSet"),
    List("contains", "add", "remove", "union", "intersect", "diff", "min", "max", "head", "take", "drop")
  ),
  Family(
    "Maps",
    "`TreeMap` inherits its notes from `SortedMap`. As for the sets, only the ordered maps have positional members.",
    List("HashMap", "LinkedHashMap", "TreeMap"),
    List("get", "containsKey", "put", "remove", "keySet", "values", "head", "take", "drop")
  )
)

/** Javadoc inline tags to Markdown-free text: `{@code x}` and `{@link #m(int)}` become `x` and `m(int)`. */
def plain(note: String): String =
  note
    .replaceAll("\\{@(?:code|literal) ([^{}]*)\\}", "$1")
    .replaceAll("\\{@link(?:plain)? #?([^{} ]*)(?: ([^{}]*))?\\}", "$1")
    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

def html(s: String): String =
  s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

def cell(d: Decl): String = {
  val note = d.note.get
  val (expr, cost) = classify(note).get
  s"""<abbr class="cx cx-${cost.toString.toLowerCase}" title="${html(plain(note)).replace("|", "&#124;")}">${html(expr)}</abbr>"""
}

/** The rows of a family's matrix: one per operation, split by overload only where some type's overloads differ. */
def familyRows(byName: Map[String, TypeInfo], f: Family): List[(String, List[Option[Decl]])] = {
  def resolved(d: Decl): Decl =
    resolve(byName, d).getOrElse(sys.error(s"${d.owner}.${d.signature} (${d.file}:${d.line}) has no 'Complexity:' note"))
  f.rows.flatMap { op =>
    val perType = f.columns.map(t => t -> overloads(byName, t, op).map(resolved)).toMap
    val splits = f.columns.exists(t => perType(t).map(d => classify(d.note.get).get._1).distinct.size > 1)
    if (!splits) {
      List((s"`$op`", f.columns.map(t => perType(t).headOption)))
    } else {
      val shapes = f.columns.flatMap(perType).map(d => (d.params, d.signature)).distinctBy(_._1)
        .sortBy(s => (s._1.size, s._2))
      shapes.map { case (params, signature) =>
        (s"`$signature`", f.columns.map(t => lookup(byName, t, op, params).map(resolved)))
      }
    }
  }
}

/** Per type, the family's rows the type declares, with the whole note: included by the page of each collection. */
def glances(types: List[TypeInfo]): List[(String, String)] = {
  val byName = types.map(t => t.name -> t).toMap
  families.flatMap { f =>
    val rows = familyRows(byName, f)
    f.columns.zipWithIndex.map { case (t, i) =>
      val out = new StringBuilder
      out ++= "<!-- Generated by scripts/check-complexity.scala (make docs-complexity). Do not edit. -->\n\n"
      out ++= "| Operation | Cost | Note |\n|---|---|---|\n"
      rows.foreach { case (label, cells) =>
        cells(i).foreach { d =>
          out ++= s"| $label | ${cell(d)} | ${html(plain(d.note.get)).replace("|", "\\|")} |\n"
        }
      }
      (t, out.toString)
    }
  }
}

def page(types: List[TypeInfo]): String = {
  val byName = types.map(t => t.name -> t).toMap
  def resolved(d: Decl): Decl =
    resolve(byName, d).getOrElse(sys.error(s"${d.owner}.${d.signature} (${d.file}:${d.line}) has no 'Complexity:' note"))
  val out = new StringBuilder
  out ++= "<!-- Generated by scripts/check-complexity.scala (make docs-complexity) from the 'Complexity:' javadoc notes. Do not edit. -->\n\n"
  out ++= "# Complexity\n\n"
  out ++= "Every positional or size-sensitive method of a collection states its cost in its javadoc, in a paragraph that " +
    "starts with `Complexity:`. `make complexity` fails the build when one is missing or does not start with one of " +
    "the expressions below, and this page is generated from those notes, so it cannot drift from the code. Hover a " +
    "cell to read the whole note.\n\n"
  out ++= "## Legend\n\n"
  out ++= "n is the size of the receiver, m the size of the argument, k the number of elements taken, dropped or " +
    "skipped, i an index. The Scala column gives the abbreviation of the Scala collections' performance table for " +
    "the same class, where there is one.\n\n"
  out ++= "| Class | Scala | Meaning | Expressions |\n|---|---|---|---|\n"
  Cost.values.foreach { c =>
    val exprs = vocabulary.toList.filter(_._2 == c).map(_._1).sortBy(e => (e.length, e)).map(e => s"`$e`").mkString(", ")
    val scala = if (c.scala.isEmpty) "" else s"`${c.scala}`"
    out ++= s"""| <span class="cx cx-${c.toString.toLowerCase}">${c.label}</span> | $scala | ${c.meaning} | $exprs |\n"""
  }
  out ++= "\n\"Effectively\" and \"amortised\" are not the same promise. Effectively constant is a worst case that grows " +
    "with log32 n, which stays at six steps or fewer for any size a JVM can hold. Amortised constant is an average: " +
    "most calls are constant, the occasional call pays for the others. A lazy note describes what the call itself does; " +
    "reading the result costs what the note says is forced.\n\n"
  out ++= "`n/a` means the type does not declare the operation: the hash-ordered sets and maps have no positional " +
    "members because their order is not defined.\n"
  families.foreach { f =>
    out ++= s"\n## ${f.title}\n\n${f.intro}\n\n"
    val rows = familyRows(byName, f)
    out ++= s"| Operation | ${f.columns.map(c => s"`$c`").mkString(" | ")} |\n"
    out ++= s"|---|${f.columns.map(_ => "---").mkString("|")}|\n"
    rows.foreach { case (label, cells) =>
      out ++= s"| $label | ${cells.map(_.map(cell).getOrElse("n/a")).mkString(" | ")} |\n"
    }
  }
  out ++= "\n## Every documented method\n\n"
  out ++= "The notes as the javadoc states them, per type, in declaration order. A type that inherits a note says so.\n"
  val detailed = families.flatMap(_.columns).distinct
  detailed.foreach { t =>
    val info = byName(t)
    val own = info.decls
    def ancestors(name: String): List[TypeInfo] =
      byName.get(name).toList.flatMap(a => a :: a.supers.flatMap(ancestors))
    val inherited = info.supers.flatMap(ancestors).distinctBy(_.name).flatMap(_.decls)
      .filter(_.note.isDefined)
      .filterNot(d => own.exists(o => o.name == d.name && o.params == d.params))
    out ++= s"\n### `$t`\n\n| Method | Cost | Note |\n|---|---|---|\n"
    (own ++ inherited).distinctBy(d => (d.name, d.params)).foreach { d =>
      val src = resolved(d)
      val from = if (src.owner != t) s" (from `${src.owner}`)" else ""
      val note = html(plain(src.note.get)).replace("|", "\\|")
      out ++= s"| `${d.signature.replace("|", "\\|")}`$from | ${cell(src)} | $note |\n"
    }
  }
  out.toString
}

@main def run(args: String*): Unit = {
  var context = List.empty[String]
  var files = List.empty[String]
  var pageOut = Option.empty[String]
  var glanceDir = Option.empty[String]
  var rest = args.toList
  while (rest.nonEmpty) {
    rest match {
      case "--context" :: f :: tail =>
        context = context :+ f
        rest = tail
      case "--glance" :: d :: tail =>
        glanceDir = Some(d)
        rest = tail
      case "--page" :: f :: tail =>
        pageOut = Some(f)
        rest = tail
      case f :: tail =>
        files = files :+ f
        rest = tail
      case Nil => ()
    }
  }
  if (files.isEmpty) {
    System.err.println("usage: scala-cli run scripts/check-complexity.scala -- [--context FILE]... [--page OUT] [--glance DIR] FILE...")
    sys.exit(2)
  }
  val types = parse(files ++ context, files.toSet)
  val (checked, problems) = check(types)
  problems.foreach(p => println(s"${p.file}:${p.line}: ${p.message}"))
  if (checked == 0) {
    println(s"no documented method found in ${files.mkString(" ")}")
    sys.exit(1)
  }
  if (problems.nonEmpty) {
    println(s"${problems.size} method(s) without a valid 'Complexity:' line (see docs/design.md 3.7)")
    sys.exit(1)
  }
  println(s"complexity: $checked method declarations checked, all documented")
  pageOut.foreach { out =>
    Files.writeString(Path.of(out), page(types), StandardCharsets.UTF_8)
    println(s"complexity: wrote $out")
  }
  glanceDir.foreach { dir =>
    Files.createDirectories(Path.of(dir))
    glances(types).foreach { case (t, text) =>
      Files.writeString(Path.of(dir, s"$t.md"), text, StandardCharsets.UTF_8)
    }
    println(s"complexity: wrote the per-type tables in $dir")
  }
}

