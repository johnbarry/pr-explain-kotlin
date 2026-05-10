package render

import kotlinx.html.FlowContent
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.figcaption
import kotlinx.html.figure
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.unsafe
import model.Diagram
import model.DiagramKind

/**
 * Render a [Diagram] block. Layout depends on [DiagramKind]:
 *
 * - `comparison` — two side-by-side `.mermaid` panels labelled "Before" / "After".
 * - `structural`, `behavioural`, `state` — single `.mermaid` panel.
 * - `none` — caption-only; no diagram surface.
 *
 * The Mermaid source is dropped into `<div class="mermaid">` as text content.
 * Mermaid's runtime scans these on DOMContentLoaded (see mermaid-init.js) and
 * swaps the text for SVG. Captions are rendered as markdown so authors can
 * add emphasis or inline code.
 *
 * Validation (step 2) has already enforced the per-kind required panels, so
 * non-null assertions on `after` (and `before` for comparison) reflect that
 * contract — we won't render an invalid walkthrough.
 */
fun FlowContent.diagramBlock(d: Diagram) {
    when (d.kind) {
        DiagramKind.comparison -> {
            figure(classes = "diagram diagram--comparison") {
                div(classes = "comparison-panels") {
                    div(classes = "comparison-panel") {
                        span(classes = "panel-label") { +"Before" }
                        div(classes = "mermaid") { +(d.before ?: "") }
                    }
                    div(classes = "comparison-panel") {
                        span(classes = "panel-label") { +"After" }
                        div(classes = "mermaid") { +(d.after ?: "") }
                    }
                }
                figcaption(classes = "diagram-caption prose") {
                    unsafe { +Markdown.toHtml(d.caption) }
                }
            }
        }

        DiagramKind.structural, DiagramKind.behavioural, DiagramKind.state -> {
            figure(classes = "diagram diagram--${d.kind}") {
                div(classes = "mermaid") { +(d.after ?: "") }
                figcaption(classes = "diagram-caption prose") {
                    unsafe { +Markdown.toHtml(d.caption) }
                }
            }
        }

        DiagramKind.none -> {
            // No visual — the caption alone carries the meaning. Wrap in a
            // figure so downstream styling can treat it consistently.
            figure(classes = "diagram diagram--none") {
                p(classes = "diagram-none-caption prose") {
                    unsafe { +Markdown.toHtml(d.caption) }
                }
            }
        }
    }
}
