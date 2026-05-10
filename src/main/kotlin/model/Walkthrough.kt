package model

import kotlinx.serialization.Serializable

/**
 * Concept-first walkthrough document. The schema is the contract between the
 * generator that produces this YAML (currently a model prompt) and the renderer
 * that consumes it. Field names mirror the spec; YAML uses snake_case and kaml
 * is configured to translate (see parse.YamlParser).
 */
@Serializable
data class Walkthrough(
    val title: String,
    val problem: String,
    val outcome: String,
    val audienceHint: String? = null,
    val concepts: List<Concept>,
    val scrutinize: List<ScrutinizeItem> = emptyList(),
    val openQuestions: List<String> = emptyList(),
    val appendix: List<AppendixEntry> = emptyList(),
)

@Serializable
data class Concept(
    val id: String,
    val name: String,
    val premise: String,
    val diagram: Diagram,
    val structuralChanges: List<StructuralChange> = emptyList(),
    /** Hard cap of 3 enforced by validation (step 2). */
    val keyExcerpts: List<Excerpt> = emptyList(),
)

@Serializable
data class Diagram(
    val kind: DiagramKind,
    /** Mermaid source for the "before" panel. Only used when kind = comparison. */
    val before: String? = null,
    /** Mermaid source for the "after" / main panel. Required for all kinds except none. */
    val after: String? = null,
    val caption: String,
)

/**
 * Lowercase enum constants intentionally — they match the YAML literal values
 * produced by the generator prompt. kotlinx.serialization uses [Enum.name] by
 * default so this avoids per-value @SerialName annotations.
 */
@Suppress("EnumEntryName")
@Serializable
enum class DiagramKind { structural, behavioural, state, comparison, none }

@Serializable
data class StructuralChange(
    val kind: ChangeKind,
    val subject: String,
    val before: String? = null,
    val after: String? = null,
    val caption: String,
)

@Suppress("EnumEntryName")
@Serializable
enum class ChangeKind { added, removed, signature_changed, responsibility_moved, renamed }

@Serializable
data class Excerpt(
    val file: String,
    val lineHint: Int? = null,
    /** Elided code; focus lines are marked with the trailing comment "// ←" or "# ←". */
    val excerpt: String,
    val caption: String,
)

@Serializable
data class ScrutinizeItem(
    val prompt: String,
    val rationale: String,
)

@Serializable
data class AppendixEntry(
    val path: String,
    val description: String,
)
