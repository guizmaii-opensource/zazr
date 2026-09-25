//> using scala 3.9.0
//> using jvm system
//
// Prints the line and branch coverage of the last `make coverage` as Markdown: one table per module, one per package,
// and the ten source files with the most missed lines. CI appends the output to the job summary.
//
//   scala-cli run scripts/coverage-summary.scala -- zazr-test/target/site/jacoco-aggregate/jacoco.xml
//
// The input is the XML report of JaCoCo's report-aggregate goal: a group per module, holding its packages, each holding
// its classes and source files, and every element carries its own counters. The numbers are those counters, never
// sums of the class counters: JaCoCo counts lines per class, so a line holding code of a method and of an anonymous
// or lambda class counts once in each class but once in its source file, package, group and report. The source file
// is also the page a reader opens in the HTML report, and its counters are the ones that page shows.

import java.nio.file.{Files, Path}
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.{Element, Node}

final case class Counts(lineMissed: Long, lineCovered: Long, branchMissed: Long, branchCovered: Long)

def percent(missed: Long, covered: Long): String = {
  val total = missed + covered
  if (total == 0) "n/a" else f"${covered * 100.0 / total}%.1f %%"
}

def lines(c: Counts): String = s"${percent(c.lineMissed, c.lineCovered)} (${c.lineCovered}/${c.lineMissed + c.lineCovered})"

def branches(c: Counts): String =
  s"${percent(c.branchMissed, c.branchCovered)} (${c.branchCovered}/${c.branchMissed + c.branchCovered})"

def lineRatio(c: Counts): Double = {
  val total = c.lineMissed + c.lineCovered
  if (total == 0) 1.0 else c.lineCovered.toDouble / total
}

def table(header: String, groups: Seq[(String, Counts)]): Unit = {
  println(s"| $header | Lines | Branches |")
  println("|---|---:|---:|")
  groups.foreach { case (name, c) => println(s"| `$name` | ${lines(c)} | ${branches(c)} |") }
  println()
}

// The child elements of `parent` named `tag`, in document order.
def children(parent: Element, tag: String): Seq[Element] = {
  val nodes = parent.getChildNodes
  (0 until nodes.getLength).map(nodes.item).collect {
    case e: Element if e.getTagName == tag => e
  }
}

// The counters of `element` itself. JaCoCo leaves out a counter whose total is 0 (a class without branches).
def counts(element: Element): Counts = {
  val byType = children(element, "counter").map { c =>
    c.getAttribute("type") -> (c.getAttribute("missed").toLong, c.getAttribute("covered").toLong)
  }.toMap
  val (lm, lc) = byType.getOrElse("LINE", (0L, 0L))
  val (bm, bc) = byType.getOrElse("BRANCH", (0L, 0L))
  Counts(lm, lc, bm, bc)
}

@main def coverageSummary(xml: String): Unit = {
  val path = Path.of(xml)
  if (!Files.isRegularFile(path)) {
    System.err.println(s"$xml not found: run make coverage first")
    sys.exit(1)
  }
  val factory = DocumentBuilderFactory.newInstance()
  // jacoco.xml declares a DOCTYPE whose report.dtd is not next to it.
  factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
  val report = factory.newDocumentBuilder().parse(path.toFile).getDocumentElement

  val modules = children(report, "group")
  val packages = for {
    module <- modules
    pkg <- children(module, "package")
  } yield (module.getAttribute("name"), pkg.getAttribute("name").replace('/', '.'), pkg)
  val sourceFiles = for {
    (_, pkgName, pkg) <- packages
    file <- children(pkg, "sourcefile")
  } yield s"$pkgName.${file.getAttribute("name").stripSuffix(".java")}" -> counts(file)

  println("## Test coverage")
  println()
  table(
    "Module",
    modules.map(m => m.getAttribute("name") -> counts(m)).sortBy(_._1) :+ ("total" -> counts(report))
  )
  table(
    "Package (least covered lines first)",
    packages.map { case (module, pkgName, pkg) => s"$module: $pkgName" -> counts(pkg) }.sortBy(g => lineRatio(g._2))
  )
  table("Source file (ten with the most missed lines)", sourceFiles.sortBy(g => -g._2.lineMissed).take(10))
}
