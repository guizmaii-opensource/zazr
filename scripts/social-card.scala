//> using scala 3.9.0
//> using jvm system
//
// Draws the link preview card of the website (docs/assets/social-card.png), the 2400x1260 image that X, LinkedIn,
// Slack and the others show under a link to zazr.dev. The page tags that point to it are in overrides/main.html.
//
//   scala-cli run scripts/social-card.scala -- FONT_DIR [OUTPUT]
//
// FONT_DIR holds Inter-Bold.ttf, Inter-SemiBold.ttf and Inter-Regular.ttf, the site's text font (the static TTFs of
// https://github.com/rsms/inter/releases). OUTPUT defaults to docs/assets/social-card.png. The PNG is committed, so
// the site build needs neither this script nor the fonts; run it again when the logo, the colours or the tagline change.
//
// The card: Zaz (docs/assets/zaz-512.png) on the left, and on the right the wordmark and the tagline, on the espresso
// brown of the site header with the yuzu accent (docs/assets/zazr.css). The bottom left stays empty: X lays the page
// title over it, and every network already shows the domain under the card. The card is laid out on a 1200x630 grid
// and drawn at twice that size, so the text stays sharp when a network scales the card down or recompresses it.

import java.awt.{Color, Font, RenderingHints}
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@main def socialCard(fontDir: String, rest: String*): Unit = {
  val output = rest.headOption.getOrElse("docs/assets/social-card.png")
  val width  = 1200
  val height = 630
  val scale  = 2

  val espresso = Color(0x2e2017)
  val yuzu     = Color(0xf5a524)
  val cream    = Color(0xfff8ee)
  val muted    = Color(0xc9bcae)

  def font(name: String, size: Float): Font =
    Font.createFont(Font.TRUETYPE_FONT, File(fontDir, s"Inter-$name.ttf")).deriveFont(size)

  val card = BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB)
  val g    = card.createGraphics()
  g.scale(scale, scale)
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
  g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
  g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

  g.setColor(espresso)
  g.fillRect(0, 0, width, height)
  g.setColor(yuzu)
  g.fillRect(0, height - 12, width, 12)

  val logo     = ImageIO.read(File("docs/assets/zaz-512.png"))
  val logoSize = 340
  g.drawImage(logo, 80, (height - logoSize) / 2, logoSize, logoSize, null)

  val textX    = 480
  val maxWidth = width - textX - 70

  // Splits the text into lines no wider than maxWidth in the given font.
  def wrap(text: String, f: Font): List[String] = {
    val metrics = g.getFontMetrics(f)
    text.split(' ').toList.foldLeft(List.empty[String]) {
      case (Nil, word)                                                        => List(word)
      case (line :: rest, word) if metrics.stringWidth(s"$line $word") <= maxWidth => s"$line $word" :: rest
      case (lines, word)                                                      => word :: lines
    }.reverse
  }

  // Draws the lines from the baseline y, and returns the baseline after the last one.
  def draw(lines: List[String], f: Font, color: Color, y: Int, lineHeight: Int): Int = {
    g.setFont(f)
    g.setColor(color)
    lines.zipWithIndex.foreach((line, i) => g.drawString(line, textX, y + i * lineHeight))
    y + (lines.size - 1) * lineHeight
  }

  val wordmark = font("Bold", 132f)
  val title    = font("SemiBold", 44f)
  val subtitle = font("Regular", 32f)

  val afterWordmark = draw(List("Zazr"), wordmark, cream, 225, 0)
  val afterTitle    = draw(wrap("Modern Functional Programming for Java 25+", title), title, yuzu, afterWordmark + 80, 56)
  draw(wrap("Inspired by Scala 2.13+, ZIO, and zio-prelude", subtitle), subtitle, muted, afterTitle + 62, 44)

  g.dispose()
  ImageIO.write(card, "png", File(output))
  println(s"$output: ${card.getWidth}x${card.getHeight}")
}
