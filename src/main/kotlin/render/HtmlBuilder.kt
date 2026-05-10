package render

import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.lang
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import kotlinx.html.unsafe
import model.Walkthrough
import render.pages.appendixPage
import render.pages.conceptMapPage
import render.pages.conceptPage
import render.pages.coverPage
import render.pages.openQuestionsPage
import render.pages.scrutinizePage

/**
 * Render the walkthrough to a single self-contained HTML string. CSS and JS are
 * inlined from classpath resources so the file works from `file://` with no
 * neighbours. Each page renders into its own `<section data-page="...">` and
 * the player JS shows exactly one section at a time.
 *
 * Step 3 only emits placeholder bodies for each page; per-page content (cover,
 * map, concept diagrams, etc.) lands in steps 4–7.
 */
fun renderHtml(w: Walkthrough): String {
    val pages = pageList(w)
    val css = Assets.css("main.css")
    val mermaidLib = Assets.vendor("mermaid.min.js")
    val mermaidInit = Assets.script("mermaid-init.js")
    val js = Assets.script("player.js")

    return "<!DOCTYPE html>\n" + createHTML().html {
        lang = "en"
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            title { +w.title.ifBlank { "Walkthrough" } }
            style { unsafe { +css } }
        }
        body {
            nav(classes = "topnav") {
                div(classes = "topnav-title") { +w.title.ifBlank { "Walkthrough" } }
                div(classes = "topnav-chips") {
                    pages.forEachIndexed { i, p ->
                        a(href = "#${p.id}", classes = "chip") {
                            attributes["data-target"] = p.id
                            attributes["data-index"] = i.toString()
                            +"${i + 1}. ${p.title}"
                        }
                    }
                }
            }

            main(classes = "stage") {
                // Each page emits a <section data-page="..."> — the player
                // toggles `is-current` to show one at a time.
                coverPage(w)
                if (w.concepts.size >= 3) conceptMapPage(w)
                w.concepts.forEachIndexed { i, c ->
                    conceptPage(c, i, w.concepts.size)
                }
                if (w.scrutinize.isNotEmpty()) scrutinizePage(w)
                if (w.openQuestions.isNotEmpty()) openQuestionsPage(w)
                if (w.appendix.isNotEmpty()) appendixPage(w)
            }

            div(classes = "controls") {
                button(classes = "btn btn-prev") {
                    attributes["aria-label"] = "Previous"
                    +"← Prev"
                }
                div(classes = "controls-position") {
                    attributes["aria-live"] = "polite"
                    +"1 / ${pages.size}"
                }
                button(classes = "btn btn-next") {
                    attributes["aria-label"] = "Next"
                    +"Next →"
                }
            }

            // Mermaid bundle first — assigns globalThis.mermaid.
            // Then our init overrides defaults before mermaid's own
            // DOMContentLoaded handler runs auto-startOnLoad.
            // Finally the player JS for navigation.
            script { unsafe { +mermaidLib } }
            script { unsafe { +mermaidInit } }
            script { unsafe { +js } }
        }
    }
}
