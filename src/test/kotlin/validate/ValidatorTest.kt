package validate

import model.ChangeKind
import model.Concept
import model.Diagram
import model.DiagramKind
import model.Excerpt
import model.ScrutinizeItem
import model.StructuralChange
import model.Walkthrough
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatorTest {

    /** A fixture with one concept and a structural diagram — handy as a base to mutate. */
    private fun minimalValid(
        concepts: List<Concept> = listOf(simpleConcept("c1", "First")),
    ) = Walkthrough(
        title = "T",
        problem = "P",
        outcome = "O",
        concepts = concepts,
    )

    private fun simpleConcept(
        id: String = "c1",
        name: String = "First",
        diagram: Diagram = Diagram(
            kind = DiagramKind.structural,
            after = "flowchart LR\n  A --> B",
            caption = "x",
        ),
        structuralChanges: List<StructuralChange> = emptyList(),
        keyExcerpts: List<Excerpt> = emptyList(),
    ) = Concept(
        id = id,
        name = name,
        premise = "p",
        diagram = diagram,
        structuralChanges = structuralChanges,
        keyExcerpts = keyExcerpts,
    )

    // ----- top-level required fields -----

    @Test
    fun `valid walkthrough has no errors and no warnings`() {
        val w = minimalValid(concepts = listOf(simpleConcept(), simpleConcept("c2", "Second")))
        val result = validate(w)
        assertFalse(result.hasErrors, "unexpected errors: ${result.errors}")
        assertEquals(emptyList(), result.warnings, "unexpected warnings")
    }

    @Test
    fun `empty title outcome problem each report errors with their path`() {
        val w = Walkthrough(title = "", problem = " ", outcome = "", concepts = listOf(simpleConcept()))
        val r = validate(w)
        assertContainsError(r, "title", "non-empty")
        assertContainsError(r, "problem", "non-empty")
        assertContainsError(r, "outcome", "non-empty")
    }

    // ----- concepts size -----

    @Test
    fun `concepts size 1 is allowed but warned (outside ideal 2-5)`() {
        val r = validate(minimalValid(listOf(simpleConcept())))
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts", "recommends 2..5")
    }

    @Test
    fun `concepts size above 5 is a warning, not an error`() {
        val r = validate(minimalValid((1..12).map { simpleConcept("c$it", "n$it") }))
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts", "recommends 2..5")
    }

    @Test
    fun `concepts size 6 is a warning, not an error`() {
        val r = validate(minimalValid((1..6).map { simpleConcept("c$it", "n$it") }))
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts", "recommends 2..5")
    }

    // ----- diagram contracts -----

    @Test
    fun `comparison diagram requires both before and after`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        diagram = Diagram(
                            kind = DiagramKind.comparison,
                            before = null,
                            after = null,
                            caption = "c",
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].diagram.before", "kind=comparison")
        assertContainsError(r, "concepts[0].diagram.after", "kind=comparison")
    }

    @Test
    fun `non-comparison diagram missing after is an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        diagram = Diagram(
                            kind = DiagramKind.behavioural,
                            after = null,
                            caption = "c",
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].diagram.after", "requires after")
    }

    @Test
    fun `non-comparison diagram with before is a warning`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        diagram = Diagram(
                            kind = DiagramKind.behavioural,
                            before = "sequenceDiagram\n  A->>B: x",
                            after = "sequenceDiagram\n  A->>B: y",
                            caption = "c",
                        ),
                    ),
                ),
            ),
        )
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts[0].diagram.before", "use kind=comparison")
    }

    @Test
    fun `kind=none with before or after is a warning`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        diagram = Diagram(
                            kind = DiagramKind.none,
                            before = "junk",
                            after = "junk",
                            caption = "Note",
                        ),
                    ),
                ),
            ),
        )
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts[0].diagram.before", "kind=none")
        assertContainsWarning(r, "concepts[0].diagram.after", "kind=none")
    }

    @Test
    fun `mermaid source not starting with recognised declaration warns`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        diagram = Diagram(
                            kind = DiagramKind.structural,
                            after = "graph TD\n  A --> B", // 'graph' is the legacy form, not in our list
                            caption = "c",
                        ),
                    ),
                ),
            ),
        )
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts[0].diagram.after", "recognised declaration")
    }

    // ----- structural change contracts -----

    @Test
    fun `added with before is an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        structuralChanges = listOf(
                            StructuralChange(
                                kind = ChangeKind.added,
                                subject = "Foo",
                                before = "old",
                                after = "new",
                                caption = "c",
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].structuralChanges[0].before", "must not provide before")
    }

    @Test
    fun `removed missing before is an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        structuralChanges = listOf(
                            StructuralChange(
                                kind = ChangeKind.removed,
                                subject = "Foo",
                                before = null,
                                after = null,
                                caption = "c",
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].structuralChanges[0].before", "requires before")
    }

    @Test
    fun `signature_changed requires both before and after`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        structuralChanges = listOf(
                            StructuralChange(
                                kind = ChangeKind.signature_changed,
                                subject = "Foo",
                                before = null,
                                after = null,
                                caption = "c",
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].structuralChanges[0].before", "requires before")
        assertContainsError(r, "concepts[0].structuralChanges[0].after", "requires after")
    }

    @Test
    fun `responsibility_moved with code blocks is an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        structuralChanges = listOf(
                            StructuralChange(
                                kind = ChangeKind.responsibility_moved,
                                subject = "Foo",
                                before = "old",
                                after = "new",
                                caption = "c",
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].structuralChanges[0].before", "must not provide before")
        assertContainsError(r, "concepts[0].structuralChanges[0].after", "must not provide after")
    }

    @Test
    fun `renamed with code blocks is a warning, not an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        structuralChanges = listOf(
                            StructuralChange(
                                kind = ChangeKind.renamed,
                                subject = "Old → New",
                                before = "x",
                                after = "y",
                                caption = "c",
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts[0].structuralChanges[0].before", "kind=renamed")
    }

    // ----- excerpt cap -----

    @Test
    fun `keyExcerpts size greater than 3 is a warning, not an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        keyExcerpts = (1..4).map {
                            Excerpt(file = "f$it.kt", excerpt = "x", caption = "c")
                        },
                    ),
                ),
            ),
        )
        assertFalse(r.hasErrors)
        assertContainsWarning(r, "concepts[0].keyExcerpts", "recommends at most 3")
    }

    @Test
    fun `keyExcerpt with negative lineHint is an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        keyExcerpts = listOf(
                            Excerpt(file = "f.kt", lineHint = 0, excerpt = "x", caption = "c"),
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].keyExcerpts[0].lineHint", "≥ 1")
    }

    // ----- captions and premises -----

    @Test
    fun `empty diagram caption is an error`() {
        val r = validate(
            minimalValid(
                listOf(
                    simpleConcept(
                        diagram = Diagram(
                            kind = DiagramKind.structural,
                            after = "flowchart LR\n  A-->B",
                            caption = "",
                        ),
                    ),
                ),
            ),
        )
        assertContainsError(r, "concepts[0].diagram.caption", "non-empty")
    }

    @Test
    fun `empty premise is an error`() {
        val w = minimalValid(listOf(simpleConcept().copy(premise = "")))
        assertContainsError(validate(w), "concepts[0].premise", "non-empty")
    }

    // ----- scrutinize, openQuestions, appendix -----

    @Test
    fun `empty scrutinize prompt or rationale errors`() {
        val w = minimalValid().copy(
            scrutinize = listOf(ScrutinizeItem(prompt = "", rationale = "")),
        )
        val r = validate(w)
        assertContainsError(r, "scrutinize[0].prompt", "non-empty")
        assertContainsError(r, "scrutinize[0].rationale", "non-empty")
    }

    @Test
    fun `empty openQuestion entry errors`() {
        val w = minimalValid().copy(openQuestions = listOf("first", "  "))
        val r = validate(w)
        assertContainsError(r, "openQuestions[1]", "non-empty")
    }

    // ----- helpers -----

    private fun assertContainsError(r: ValidationResult, path: String, fragment: String) {
        val match = r.errors.any { it.path == path && fragment in it.message }
        assertTrue(match, "expected ERROR at $path containing \"$fragment\". Got: ${r.errors}")
    }

    private fun assertContainsWarning(r: ValidationResult, path: String, fragment: String) {
        val match = r.warnings.any { it.path == path && fragment in it.message }
        assertTrue(match, "expected WARNING at $path containing \"$fragment\". Got: ${r.warnings}")
    }
}
