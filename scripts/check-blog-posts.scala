//> using scala 3.9.0
//> using jvm system
//
// Fails when a post of the blog has no author or no date, so that every post shows both.
//
//   scala-cli run scripts/check-blog-posts.scala -- docs/blog/posts
//
// Every Markdown file under the directory is a post. Its front matter (the lines between the opening `---` and the
// next `---`) must have:
//   - `authors:` with at least one author, inline (`authors: [guizmaii]`) or as a list (`- guizmaii` lines below it);
//     the blog plugin already fails the site build on an author missing from docs/blog/.authors.yml;
//   - `date:` with a value (`date: 2026-09-28`), or with a `created:` value below it (the plugin's long form).
// Only the top-level keys are read, as the blog plugin reads them; the plugin checks that the date is a valid date.

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

// the top-level keys of the front matter, each with its inline value and the indented lines below it
def frontMatter(lines: List[String]): Option[Map[String, (String, List[String])]] = {
  lines match {
    case first :: rest if first.trim == "---" =>
      val end = rest.indexWhere(_.trim == "---")
      if (end < 0) None
      else {
        val body = rest.take(end)
        val keyAt = body.zipWithIndex.collect {
          case (line, i) if line.nonEmpty && !line.head.isWhitespace && line.contains(':') => i
        }
        Some(keyAt.zipWithIndex.map { (start, k) =>
          val stop = if (k + 1 < keyAt.length) keyAt(k + 1) else body.length
          val line = body(start)
          val key = line.substring(0, line.indexOf(':')).trim
          val value = line.substring(line.indexOf(':') + 1).trim
          key -> (value, body.slice(start + 1, stop).map(_.trim).filter(_.nonEmpty))
        }.toMap)
      }
    case _ => None
  }
}

def hasAuthor(entry: Option[(String, List[String])]): Boolean = {
  entry match {
    case Some((inline, below)) if inline.isEmpty =>
      below.exists(l => l.startsWith("-") && l.drop(1).trim.nonEmpty)
    case Some((inline, _)) =>
      inline.startsWith("[") && inline.stripPrefix("[").stripSuffix("]").split(',').exists(_.trim.nonEmpty)
    case None => false
  }
}

def hasDate(entry: Option[(String, List[String])]): Boolean = {
  entry match {
    case Some((inline, below)) if inline.isEmpty =>
      below.exists(l => l.startsWith("created:") && l.stripPrefix("created:").trim.nonEmpty)
    case Some((_, _)) => true
    case None => false
  }
}

@main def checkBlogPosts(dirs: String*): Unit = {
  val posts = dirs.flatMap { dir =>
    Files.walk(Path.of(dir)).iterator().asScala.filter(p => Files.isRegularFile(p) && p.toString.endsWith(".md")).toList
  }.sortBy(_.toString)
  val problems = posts.flatMap { post =>
    val lines = Files.readAllLines(post, StandardCharsets.UTF_8).asScala.toList
    frontMatter(lines) match {
      case None => List(s"$post: no front matter")
      case Some(keys) =>
        (if (hasAuthor(keys.get("authors"))) Nil else List(s"$post: no author (authors: [<id>])")) ++
          (if (hasDate(keys.get("date"))) Nil else List(s"$post: no date (date: YYYY-MM-DD)"))
    }
  }
  if (problems.nonEmpty) {
    problems.foreach(println)
    println(s"${problems.length} problem(s): every blog post has an author and a date")
    sys.exit(1)
  }
  println(s"blog: ${posts.length} post(s), each with an author and a date")
}
