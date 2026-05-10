package render.pages

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ol
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.unsafe
import model.Walkthrough
import render.Markdown

/**
 * Prompts the reader should put to themselves to test their understanding
 * of the change. Each item pairs a question with the author's rationale —
 * the rationale doubles as an answer key the reader can compare against.
 */
fun FlowContent.scrutinizePage(w: Walkthrough) {
    section(classes = "page page--scrutinize") {
        attributes["data-page"] = "scrutinize"
        h1 { +"Scrutinize" }
        p(classes = "page-lede") {
            +"Questions to push on the design. The rationale is the author's answer — compare yours first."
        }
        ol(classes = "scrutinize-list") {
            w.scrutinize.forEach { item ->
                li(classes = "scrutinize-item") {
                    p(classes = "scrutinize-prompt") { +item.prompt }
                    div(classes = "scrutinize-rationale prose") {
                        unsafe { +Markdown.toHtml(item.rationale) }
                    }
                }
            }
        }
    }
}
