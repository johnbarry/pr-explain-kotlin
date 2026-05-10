package render.pages

import kotlinx.html.FlowContent
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.dd
import kotlinx.html.dl
import kotlinx.html.dt
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.unsafe
import model.Walkthrough
import render.Markdown

/**
 * Pointers to supporting material — files, links, follow-ups. Rendered as
 * a description list so the path is visually keyed to its description.
 */
fun FlowContent.appendixPage(w: Walkthrough) {
    section(classes = "page page--appendix") {
        attributes["data-page"] = "appendix"
        h1 { +"Appendix" }
        p(classes = "page-lede") {
            +"Where to look next."
        }
        dl(classes = "appendix-list") {
            w.appendix.forEach { entry ->
                dt(classes = "appendix-path") { code { +entry.path } }
                dd(classes = "appendix-description") {
                    div(classes = "prose") { unsafe { +Markdown.toHtml(entry.description) } }
                }
            }
        }
    }
}
