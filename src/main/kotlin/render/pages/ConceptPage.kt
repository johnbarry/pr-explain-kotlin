package render.pages

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.unsafe
import model.Concept
import render.Markdown
import render.diagramBlock
import render.excerptsBlock
import render.structuralChangesBlock

fun FlowContent.conceptPage(c: Concept, index: Int, total: Int) {
    section(classes = "page page--concept") {
        attributes["data-page"] = "concept-${c.id}"
        p(classes = "concept-position") { +"Concept ${index + 1} of $total" }
        h1 { +c.name }
        div(classes = "concept-premise prose") {
            unsafe { +Markdown.toHtml(c.premise) }
        }
        diagramBlock(c.diagram)
        structuralChangesBlock(c.structuralChanges)
        excerptsBlock(c.keyExcerpts)
    }
}
