package digital.tonima.mycarcompanion.core.designsystem.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val BOLD = Regex("""\*\*(.+?)\*\*""")
private val BULLET = Regex("""^\s*[*-]\s+""")
private val HEADING = Regex("""^\s*#{1,6}\s+""")

/**
 * Converts the small Markdown subset the AI tends to produce (`**bold**`, `*`/`-` bullets and
 * `#` headings) into an [AnnotatedString], so asterisks are not shown literally.
 */
fun markdownToAnnotatedString(markdown: String): AnnotatedString = buildAnnotatedString {
    markdown.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append('\n')
        val isHeading = HEADING.containsMatchIn(rawLine)
        val line = rawLine.replace(HEADING, "").replace(BULLET, "• ")
        val appendLine = {
            var last = 0
            BOLD.findAll(line).forEach { match ->
                append(line.substring(last, match.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
                last = match.range.last + 1
            }
            append(line.substring(last))
        }
        if (isHeading) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendLine() } else appendLine()
    }
}
