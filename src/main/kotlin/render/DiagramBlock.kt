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
    val hasBefore = !d.before.isNullOrBlank()
    val hasAfter = !d.after.isNullOrBlank()

    when (d.kind) {
        DiagramKind.comparison -> {
            // Render whichever panel(s) the author actually provided. With
            // both, the usual side-by-side. With one, a single panel still
            // tagged as a comparison-figure. With neither, fall through to
            // caption-only (same shape as kind=none).
            if (!hasBefore && !hasAfter) {
                figure(classes = "diagram diagram--comparison diagram--empty") {
                    p(classes = "diagram-none-caption prose") {
                        unsafe { +Markdown.toHtml(d.caption) }
                    }
                }
            } else {
                figure(classes = "diagram diagram--comparison") {
                    div(classes = "comparison-panels") {
                        if (hasBefore) {
                            div(classes = "comparison-panel") {
                                span(classes = "panel-label") { +"Before" }
                                div(classes = "mermaid") { +(d.before ?: "") }
                            }
                        }
                        if (hasAfter) {
                            div(classes = "comparison-panel") {
                                span(classes = "panel-label") { +"After" }
                                div(classes = "mermaid") { +(d.after ?: "") }
                            }
                        }
                    }
                    figcaption(classes = "diagram-caption prose") {
                        unsafe { +Markdown.toHtml(d.caption) }
                    }
                }
            }
        }

        DiagramKind.structural, DiagramKind.behavioural, DiagramKind.state -> {
            figure(classes = "diagram diagram--${d.kind}") {
                if (hasAfter) {
                    div(classes = "mermaid") { +(d.after ?: "") }
                }
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
