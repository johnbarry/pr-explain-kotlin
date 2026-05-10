package render

import kotlinx.html.FlowContent
import kotlinx.html.HtmlBlockTag
import kotlinx.html.code
import kotlinx.html.pre
import kotlinx.html.unsafe

/**
 * Recognises a focus marker — `// ←` (C-family / Kotlin / Java / JS / Rust)
 * or `# ←` (Python / Ruby / Shell / YAML) — at end-of-line, allowing
 * trailing whitespace. The marker stays visible in the output so it acts as
 * a small inline anchor in addition to the CSS highlight.
 */
internal val FOCUS_MARKER = Regex("""(//|#)\s*←\s*$""")

/**
 * Render a code block with per-line wrappers so the focus line (if any) can
 * be highlighted. Output shape:
 *
 *     <pre class="code [extra]"><code>
 *       <span class="code-line">first line</span>
 *       <span class="code-line code-line--focus">marked // ←</span>
 *       ...
 *     </code></pre>
 *
 * Newlines between lines are preserved literally so "view source" and copy
 * paste from a rendered page still produce the original text.
 */
internal fun FlowContent.codeBlock(src: String, extraClass: String? = null) {
    val classes = listOfNotNull("code", extraClass).joinToString(" ")
    pre(classes = classes) {
        code {
            unsafe { +renderCodeLines(src) }
        }
    }
}

/** Same as [codeBlock] but as a child of a specific block parent (e.g. inside a panel). */
internal fun HtmlBlockTag.codeBlockChild(src: String, extraClass: String? = null) {
    val classes = listOfNotNull("code", extraClass).joinToString(" ")
    pre(classes = classes) {
        code {
            unsafe { +renderCodeLines(src) }
        }
    }
}

internal fun renderCodeLines(src: String): String {
    if (src.isEmpty()) return ""
    val sb = StringBuilder()
    // Use lines() to split — note this drops a final empty line if the
    // input ended with \n, which is what we want for clean rendering.
    val lines = src.lines().let {
        if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it
    }
    lines.forEachIndexed { i, line ->
        val isFocus = FOCUS_MARKER.containsMatchIn(line)
        val cls = if (isFocus) "code-line code-line--focus" else "code-line"
        sb.append("<span class=\"").append(cls).append("\">")
            .append(escapeHtml(line))
            .append("</span>")
        if (i < lines.lastIndex) sb.append('\n')
    }
    return sb.toString()
}

private fun escapeHtml(s: String): String = buildString(s.length) {
    for (c in s) when (c) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '"' -> append("&quot;")
        else -> append(c)
    }
}
