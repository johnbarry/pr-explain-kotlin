package render.pages

import kotlinx.html.FlowContent
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.ul
import kotlinx.html.unsafe
import model.Walkthrough
import render.Markdown

/**
 * Things the author hasn't decided yet. Rendered as a bulleted list so the
 * reader can see the open surface at a glance. Each entry is treated as
 * markdown — questions sometimes contain inline code or links.
 */
fun FlowContent.openQuestionsPage(w: Walkthrough) {
    section(classes = "page page--questions") {
        attributes["data-page"] = "open-questions"
        h1 { +"Open questions" }
        p(classes = "page-lede") {
            +"Decisions not yet made. Feedback welcome."
        }
        ul(classes = "questions-list") {
            w.openQuestions.forEach { q ->
                li(classes = "questions-item") {
                    unsafe { +Markdown.toHtml(q) }
                }
            }
        }
    }
}
