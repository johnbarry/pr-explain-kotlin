package validate

import model.ChangeKind
import model.Concept
import model.Diagram
import model.DiagramKind
import model.Excerpt
import model.StructuralChange
import model.Walkthrough

enum class Severity { ERROR, WARNING }

data class ValidationIssue(
    val severity: Severity,
    /** Dotted/bracketed YAML-style path to the offending node, e.g. `concepts[2].keyExcerpts[3]`. */
    val path: String,
    val message: String,
) {
    override fun toString(): String = "$severity $path: $message"
}

data class ValidationResult(val issues: List<ValidationIssue>) {
    val errors: List<ValidationIssue> get() = issues.filter { it.severity == Severity.ERROR }
    val warnings: List<ValidationIssue> get() = issues.filter { it.severity == Severity.WARNING }
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

/**
 * Mermaid declarations the renderer is expected to support. Sources that don't
 * start with one of these get a warning — bad Mermaid would still render (or
 * fail to render) at view time, this is a best-effort heuristic.
 */
private val MERMAID_DECLARATIONS = listOf(
    "flowchart",
    "classDiagram",
    "sequenceDiagram",
    "stateDiagram-v2",
    "C4Context",
    "C4Container",
    "C4Component",
    "erDiagram",
)

fun validate(w: Walkthrough): ValidationResult {
    val issues = mutableListOf<ValidationIssue>()

    requireNonEmpty(issues, "title", w.title)
    requireNonEmpty(issues, "problem", w.problem)
    requireNonEmpty(issues, "outcome", w.outcome)
    if (w.audienceHint != null) {
        requireNonEmpty(issues, "audienceHint", w.audienceHint)
    }

    when (w.concepts.size) {
        in 2..5 -> Unit
        in 1..7 -> issues.warn(
            "concepts",
            "concepts.size = ${w.concepts.size}; spec recommends 2..5",
        )
        else -> issues.error(
            "concepts",
            "concepts.size = ${w.concepts.size}; must be in 1..7",
        )
    }

    w.concepts.forEachIndexed { i, c -> validateConcept(issues, "concepts[$i]", c) }

    w.scrutinize.forEachIndexed { i, s ->
        requireNonEmpty(issues, "scrutinize[$i].prompt", s.prompt)
        requireNonEmpty(issues, "scrutinize[$i].rationale", s.rationale)
    }
    w.openQuestions.forEachIndexed { i, q ->
        requireNonEmpty(issues, "openQuestions[$i]", q)
    }
    w.appendix.forEachIndexed { i, a ->
        requireNonEmpty(issues, "appendix[$i].path", a.path)
        requireNonEmpty(issues, "appendix[$i].description", a.description)
    }

    return ValidationResult(issues)
}

private fun validateConcept(issues: MutableList<ValidationIssue>, path: String, c: Concept) {
    requireNonEmpty(issues, "$path.id", c.id)
    requireNonEmpty(issues, "$path.name", c.name)
    requireNonEmpty(issues, "$path.premise", c.premise)

    validateDiagram(issues, "$path.diagram", c.diagram)

    if (c.keyExcerpts.size > MAX_KEY_EXCERPTS) {
        issues.error(
            "$path.keyExcerpts",
            "keyExcerpts.size = ${c.keyExcerpts.size}; hard cap is $MAX_KEY_EXCERPTS",
        )
    }

    c.structuralChanges.forEachIndexed { i, sc ->
        validateStructuralChange(issues, "$path.structuralChanges[$i]", sc)
    }
    c.keyExcerpts.forEachIndexed { i, e ->
        validateExcerpt(issues, "$path.keyExcerpts[$i]", e)
    }
}

private fun validateDiagram(issues: MutableList<ValidationIssue>, path: String, d: Diagram) {
    requireNonEmpty(issues, "$path.caption", d.caption)

    when (d.kind) {
        DiagramKind.comparison -> {
            if (d.before.isNullOrBlank()) {
                issues.error("$path.before", "kind=comparison requires before to be non-empty")
            } else {
                checkMermaid(issues, "$path.before", d.before)
            }
            if (d.after.isNullOrBlank()) {
                issues.error("$path.after", "kind=comparison requires after to be non-empty")
            } else {
                checkMermaid(issues, "$path.after", d.after)
            }
        }
        DiagramKind.none -> {
            if (!d.before.isNullOrBlank()) {
                issues.warn("$path.before", "kind=none should not provide before")
            }
            if (!d.after.isNullOrBlank()) {
                issues.warn("$path.after", "kind=none should not provide after")
            }
        }
        DiagramKind.structural,
        DiagramKind.behavioural,
        DiagramKind.state -> {
            if (d.after.isNullOrBlank()) {
                issues.error("$path.after", "kind=${d.kind} requires after to be non-empty")
            } else {
                checkMermaid(issues, "$path.after", d.after)
            }
            if (!d.before.isNullOrBlank()) {
                issues.warn(
                    "$path.before",
                    "kind=${d.kind} should not provide before; use kind=comparison if both panels are needed",
                )
            }
        }
    }
}

private fun validateStructuralChange(
    issues: MutableList<ValidationIssue>,
    path: String,
    c: StructuralChange,
) {
    requireNonEmpty(issues, "$path.subject", c.subject)
    requireNonEmpty(issues, "$path.caption", c.caption)

    when (c.kind) {
        ChangeKind.added -> {
            if (c.after.isNullOrBlank()) {
                issues.error("$path.after", "kind=added requires after")
            }
            if (!c.before.isNullOrBlank()) {
                issues.error("$path.before", "kind=added must not provide before")
            }
        }
        ChangeKind.removed -> {
            if (c.before.isNullOrBlank()) {
                issues.error("$path.before", "kind=removed requires before")
            }
            if (!c.after.isNullOrBlank()) {
                issues.error("$path.after", "kind=removed must not provide after")
            }
        }
        ChangeKind.signature_changed -> {
            if (c.before.isNullOrBlank()) {
                issues.error("$path.before", "kind=signature_changed requires before")
            }
            if (c.after.isNullOrBlank()) {
                issues.error("$path.after", "kind=signature_changed requires after")
            }
        }
        ChangeKind.responsibility_moved -> {
            if (!c.before.isNullOrBlank()) {
                issues.error(
                    "$path.before",
                    "kind=responsibility_moved must not provide before; the caption carries the meaning",
                )
            }
            if (!c.after.isNullOrBlank()) {
                issues.error(
                    "$path.after",
                    "kind=responsibility_moved must not provide after; the caption carries the meaning",
                )
            }
        }
        ChangeKind.renamed -> {
            if (!c.before.isNullOrBlank()) {
                issues.warn(
                    "$path.before",
                    "kind=renamed should not provide before unless the rename is non-obvious",
                )
            }
            if (!c.after.isNullOrBlank()) {
                issues.warn(
                    "$path.after",
                    "kind=renamed should not provide after unless the rename is non-obvious",
                )
            }
        }
    }
}

private fun validateExcerpt(issues: MutableList<ValidationIssue>, path: String, e: Excerpt) {
    requireNonEmpty(issues, "$path.file", e.file)
    requireNonEmpty(issues, "$path.excerpt", e.excerpt)
    requireNonEmpty(issues, "$path.caption", e.caption)
    if (e.lineHint != null && e.lineHint < 1) {
        issues.error("$path.lineHint", "lineHint must be ≥ 1, got ${e.lineHint}")
    }
}

private fun checkMermaid(issues: MutableList<ValidationIssue>, path: String, src: String) {
    val firstNonBlank = src.lineSequence().firstOrNull { it.isNotBlank() }?.trimStart() ?: return
    val matched = MERMAID_DECLARATIONS.any { firstNonBlank.startsWith(it) }
    if (!matched) {
        val preview = firstNonBlank.take(40)
        issues.warn(
            path,
            "Mermaid source does not start with a recognised declaration (got: \"$preview\")",
        )
    }
}

private fun requireNonEmpty(
    issues: MutableList<ValidationIssue>,
    path: String,
    value: String?,
) {
    if (value.isNullOrBlank()) {
        issues.error(path, "must be non-empty")
    }
}

private fun MutableList<ValidationIssue>.error(path: String, message: String) {
    add(ValidationIssue(Severity.ERROR, path, message))
}

private fun MutableList<ValidationIssue>.warn(path: String, message: String) {
    add(ValidationIssue(Severity.WARNING, path, message))
}

private const val MAX_KEY_EXCERPTS = 3
