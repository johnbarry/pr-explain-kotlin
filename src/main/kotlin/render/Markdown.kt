package render

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

/**
 * Minimal markdown-to-HTML conversion for prose fields (problem, outcome,
 * premise, rationale, descriptions). Captions stay short and don't usually
 * need markdown; the body fields do.
 *
 * Configured once with the tables extension — that's the only flexmark ext
 * the spec calls for. The output is trusted (it comes from the walkthrough
 * author, not user input), so we splice it in via kotlinx.html's `unsafe`.
 */
object Markdown {
    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
    }
    private val parser: Parser = Parser.builder(options).build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

    /** Render [src] to an HTML fragment. Empty input returns empty string. */
    fun toHtml(src: String): String =
        if (src.isBlank()) "" else renderer.render(parser.parse(src))
}
