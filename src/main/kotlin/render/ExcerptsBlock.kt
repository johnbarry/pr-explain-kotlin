package render

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.ol
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.unsafe
import model.Excerpt

/**
 * Renders a Concept's key excerpts. Validation has already capped the list
 * at 3 (step 2), so this just emits them as a vertical list. Each excerpt
 * shows a file path (with optional `:line` hint) above a code block whose
 * focus line — if marked with `// ←` or `# ←` — is highlighted by the same
 * `codeBlock` helper that powers structural changes.
 */
fun FlowContent.excerptsBlock(excerpts: List<Excerpt>) {
    if (excerpts.isEmpty()) return
    section(classes = "excerpts") {
        h2(classes = "excerpts-heading") { +"Key excerpts" }
        ol(classes = "excerpts-list") {
            excerpts.forEach { e ->
                li(classes = "excerpt") {
                    div(classes = "excerpt-header") {
                        span(classes = "excerpt-path") { +e.file }
                        if (e.lineHint != null) {
                            span(classes = "excerpt-line") { +":${e.lineHint}" }
                        }
                    }
                    codeBlock(e.excerpt, "code--excerpt")
                    div(classes = "excerpt-caption prose") {
                        unsafe { +Markdown.toHtml(e.caption) }
                    }
                }
            }
        }
    }
}
