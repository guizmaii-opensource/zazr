//> using scala 3.9.0
//> using jvm system
//
// Prints the line and branch coverage of the last `make coverage` as Markdown: one table per module, one per package,
// and the ten classes with the most missed lines. CI appends the output to the job summary.
//
//   scala-cli run scripts/coverage-summary.scala -- zazr-test/target/site/jacoco-aggregate/jacoco.csv
//
// The input is the CSV report of JaCoCo's report-aggregate goal: one row per class, the GROUP column is
// "<aggregating module name>/<artifactId>", and the counters are missed and covered pairs. Nested classes are folded
// into their top-level class, whose source file is what a reader opens in the HTML report.

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class Counts(lineMissed: Long, lineCovered: Long, branchMissed: Long, branchCovered: Long) {
  def +(that: Counts): Counts =
    Counts(
      lineMissed + that.lineMissed,
      lineCovered + that.lineCovered,
      branchMissed + that.branchMissed,
      branchCovered + that.branchCovered
    )
}

object Counts {
  val zero: Counts = Counts(0, 0, 0, 0)
}

final case class Row(module: String, pkg: String, topLevelClass: String, counts: Counts)

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

@main def coverageSummary(csv: String): Unit = {
  val path = Path.of(csv)
  if (!Files.isRegularFile(path)) {
    System.err.println(s"$csv not found: run make coverage first")
    sys.exit(1)
  }
  val all = Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList
  val header = all.head.split(',').toList
  def column(name: String): Int = {
    val i = header.indexOf(name)
    if (i < 0) {
      System.err.println(s"$csv has no $name column")
      sys.exit(1)
    }
    i
  }
  val (group, pkg, cls) = (column("GROUP"), column("PACKAGE"), column("CLASS"))
  val (lm, lc, bm, bc) = (column("LINE_MISSED"), column("LINE_COVERED"), column("BRANCH_MISSED"), column("BRANCH_COVERED"))
  val rows = all.tail.filter(_.nonEmpty).map { line =>
    val f = line.split(',')
    Row(
      module = f(group).split('/').last,
      pkg = f(pkg),
      topLevelClass = f(cls).takeWhile(_ != '.'),
      counts = Counts(f(lm).toLong, f(lc).toLong, f(bm).toLong, f(bc).toLong)
    )
  }

  def sum(rs: Seq[Row]): Counts = rs.foldLeft(Counts.zero)(_ + _.counts)

  println("## Test coverage")
  println()
  table("Module", rows.groupBy(_.module).toSeq.sortBy(_._1).map { case (m, rs) => m -> sum(rs) } :+ ("total" -> sum(rows)))
  table(
    "Package (least covered lines first)",
    rows.groupBy(r => s"${r.module}: ${r.pkg}").toSeq.map { case (p, rs) => p -> sum(rs) }.sortBy(g => lineRatio(g._2))
  )
  table(
    "Class (ten with the most missed lines)",
    rows
      .groupBy(r => s"${r.pkg}.${r.topLevelClass}")
      .toSeq
      .map { case (c, rs) => c -> sum(rs) }
      .sortBy(g => -g._2.lineMissed)
      .take(10)
  )
}
