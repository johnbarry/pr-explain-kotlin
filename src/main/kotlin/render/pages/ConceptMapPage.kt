package render.pages

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import model.Walkthrough
import render.conceptMapMermaid

/**
 * Auto-generated overview of the walkthrough's concepts as a Mermaid
 * flowchart. The source is emitted into a `<div class="mermaid">` block;
 * step 5 wires up the Mermaid library that turns these into SVG at view
 * time.
 */
fun FlowContent.conceptMapPage(w: Walkthrough) {
    section(classes = "page page--map") {
        attributes["data-page"] = "concept-map"
        h1 { +"Concept map" }
        p(classes = "page-lede") {
            +"The concepts you'll meet, in reading order."
        }
        div(classes = "mermaid") {
            // Text-only — the rendering pass in step 5 reads this string
            // as the Mermaid source.
            +conceptMapMermaid(w)
        }
    }
}
