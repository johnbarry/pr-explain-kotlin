package render

import kotlinx.html.FlowContent
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.ol
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.unsafe
import model.ChangeKind
import model.StructuralChange

/**
 * Renders a Concept's structural changes as a vertical list. Each item has
 * a kind label (icon + text), the subject (the thing that changed), an
 * optional code block per `ChangeKind`'s canonical shape, and a caption.
 *
 * Per-kind idiom — guided by what's most informative and matches the
 * validator's contract from step 2:
 *
 * - `added`              — `after` only; green accent.
 * - `removed`            — `before` only; red accent.
 * - `signature_changed`  — `before` then `after` stacked; amber accent.
 * - `responsibility_moved` — caption-only; blue accent.
 * - `renamed`            — caption-only; gray accent. If both `before` and
 *                          `after` are present (a warned but allowed
 *                          edge-case), they're shown as an inline
 *                          `old → new` pair.
 */
fun FlowContent.structuralChangesBlock(changes: List<StructuralChange>) {
    if (changes.isEmpty()) return
    section(classes = "changes") {
        h2(classes = "changes-heading") { +"Structural changes" }
        ol(classes = "changes-list") {
            changes.forEach { c ->
                li(classes = "change change--${c.kind}") {
                    changeHeader(c)
                    changeBody(c)
                    changeCaption(c)
                }
            }
        }
    }
}

private fun FlowContent.changeHeader(c: StructuralChange) {
    div(classes = "change-header") {
        span(classes = "change-icon") { +iconFor(c.kind) }
        span(classes = "change-label") { +labelFor(c.kind) }
        span(classes = "change-subject") { code { +c.subject } }
    }
}

private fun FlowContent.changeBody(c: StructuralChange) {
    when (c.kind) {
        ChangeKind.added -> {
            if (!c.after.isNullOrBlank()) codeBlock(c.after, "code--after")
        }

        ChangeKind.removed -> {
            if (!c.before.isNullOrBlank()) codeBlock(c.before, "code--before")
        }

        ChangeKind.signature_changed -> {
            if (!c.before.isNullOrBlank()) {
                div(classes = "change-panel change-panel--before") {
                    span(classes = "panel-label") { +"Before" }
                    codeBlockChild(c.before, "code--before")
                }
            }
            if (!c.after.isNullOrBlank()) {
                div(classes = "change-panel change-panel--after") {
                    span(classes = "panel-label") { +"After" }
                    codeBlockChild(c.after, "code--after")
                }
            }
        }

        ChangeKind.responsibility_moved -> {
            // Canonical form has no code blocks — the caption carries the
            // meaning. Validation errors on either panel being present, so
            // we don't render them here.
        }

        ChangeKind.renamed -> {
            // Canonical form has no code blocks either. If both are
            // provided (warned-but-allowed), render an inline `old → new`
            // pair so the rename is legible at a glance. If only one side
            // is given, drop down to a single block.
            val hasBefore = !c.before.isNullOrBlank()
            val hasAfter = !c.after.isNullOrBlank()
            when {
                hasBefore && hasAfter -> div(classes = "rename-pair") {
                    code(classes = "rename-from") { +c.before!! }
                    span(classes = "rename-arrow") { +" → " }
                    code(classes = "rename-to") { +c.after!! }
                }
                hasBefore -> codeBlock(c.before!!, "code--before")
                hasAfter -> codeBlock(c.after!!, "code--after")
            }
        }
    }
}

private fun FlowContent.changeCaption(c: StructuralChange) {
    div(classes = "change-caption prose") {
        unsafe { +Markdown.toHtml(c.caption) }
    }
}

private fun iconFor(k: ChangeKind): String = when (k) {
    ChangeKind.added -> "+"
    ChangeKind.removed -> "−"
    ChangeKind.signature_changed -> "∼"
    ChangeKind.responsibility_moved -> "↪"
    ChangeKind.renamed -> "✎"
}

private fun labelFor(k: ChangeKind): String = when (k) {
    ChangeKind.added -> "Added"
    ChangeKind.removed -> "Removed"
    ChangeKind.signature_changed -> "Signature changed"
    ChangeKind.responsibility_moved -> "Responsibility moved"
    ChangeKind.renamed -> "Renamed"
}
