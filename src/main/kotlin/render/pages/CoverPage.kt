package render.pages

import kotlinx.html.FlowContent
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.unsafe
import model.Walkthrough
import render.Markdown

/**
 * Title page. Renders the walkthrough title, the problem statement, the
 * outcome the reader should reach, and an audience hint if present. The
 * problem and outcome are rendered as markdown — multi-line prose is the
 * common shape for these fields.
 */
fun FlowContent.coverPage(w: Walkthrough) {
    section(classes = "page page--cover") {
        attributes["data-page"] = "cover"
        h1(classes = "cover-title") { +w.title.ifBlank { "Untitled walkthrough" } }

        if (!w.audienceHint.isNullOrBlank()) {
            p(classes = "cover-audience") { +w.audienceHint }
        }

        h2 { +"Problem" }
        div(classes = "prose") { unsafe { +Markdown.toHtml(w.problem) } }

        h2 { +"Outcome" }
        div(classes = "prose") { unsafe { +Markdown.toHtml(w.outcome) } }
    }
}
