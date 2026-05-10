package render

import model.AppendixEntry
import model.Concept
import model.Diagram
import model.DiagramKind
import model.ScrutinizeItem
import model.Walkthrough
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimplePagesTest {

    private fun c(id: String, name: String = "Concept $id") = Concept(
        id = id,
        name = name,
        premise = "Premise for **$id**.",
        diagram = Diagram(kind = DiagramKind.structural, after = "flowchart LR\n  A-->B", caption = "c"),
    )

    private fun w(
        title: String = "T",
        problem: String = "Initial **problem** statement.\n\nSecond paragraph.",
        outcome: String = "Reader knows X.",
        audienceHint: String? = null,
        concepts: List<Concept> = listOf(c("a"), c("b")),
        scrutinize: List<ScrutinizeItem> = emptyList(),
        openQuestions: List<String> = emptyList(),
        appendix: List<AppendixEntry> = emptyList(),
    ) = Walkthrough(
        title = title, problem = problem, outcome = outcome,
        audienceHint = audienceHint,
        concepts = concepts, scrutinize = scrutinize,
        openQuestions = openQuestions, appendix = appendix,
    )

    // ----- markdown helper -----

    @Test
    fun `Markdown toHtml wraps prose in p and renders bold`() {
        val html = Markdown.toHtml("Hello **world**.")
        assertTrue("<p>" in html && "</p>" in html, "expected <p> wrapping")
        assertTrue("<strong>world</strong>" in html, "expected bold rendering")
    }

    @Test
    fun `Markdown toHtml on blank input returns empty string`() {
        assertEquals("", Markdown.toHtml(""))
        assertEquals("", Markdown.toHtml("   \n  "))
    }

    // ----- cover -----

    @Test
    fun `cover page renders title, problem and outcome as markdown`() {
        val html = renderHtml(w(title = "My title"))
        assertTrue("""<h1 class="cover-title">My title</h1>""" in html)
        assertTrue("<strong>problem</strong>" in html, "problem should render markdown")
        assertTrue(">Problem<" in html && ">Outcome<" in html, "section headings present")
    }

    @Test
    fun `cover page renders audience hint only when present`() {
        val withHint = renderHtml(w(audienceHint = "Internal only"))
        assertTrue("""class="cover-audience"""" in withHint)
        assertTrue("Internal only" in withHint)

        val without = renderHtml(w(audienceHint = null))
        assertFalse("""class="cover-audience"""" in without)
    }

    // ----- concept map -----

    @Test
    fun `conceptMapMermaid emits a flowchart with one quoted node per concept`() {
        val src = conceptMapMermaid(w(concepts = listOf(c("a", "First"), c("b", "Second"), c("c", "Third"))))
        assertTrue(src.startsWith("flowchart LR\n"))
        assertTrue("""c0["First"]""" in src)
        assertTrue("""c1["Second"]""" in src)
        assertTrue("""c2["Third"]""" in src)
        assertTrue("c0 --> c1" in src)
        assertTrue("c1 --> c2" in src)
    }

    @Test
    fun `conceptMapMermaid escapes double quotes in names`() {
        val src = conceptMapMermaid(w(concepts = listOf(c("a", """A "tricky" name"""))))
        assertTrue("""A &quot;tricky&quot; name""" in src, "expected quote escaping; got: $src")
    }

    @Test
    fun `concept map page is rendered when 3 or more concepts and contains mermaid block`() {
        val html = renderHtml(w(concepts = listOf(c("a"), c("b"), c("c"))))
        assertTrue("""data-page="concept-map"""" in html)
        assertTrue("""<div class="mermaid">""" in html)
        assertTrue("flowchart LR" in html)
    }

    // ----- scrutinize -----

    @Test
    fun `scrutinize page lists each item with prompt and markdown rationale`() {
        val html = renderHtml(
            w(scrutinize = listOf(
                ScrutinizeItem(prompt = "Is the rename safe?", rationale = "Yes, because **callers** are internal."),
                ScrutinizeItem(prompt = "Cost?", rationale = "Negligible."),
            )),
        )
        assertTrue("""data-page="scrutinize"""" in html)
        assertTrue("Is the rename safe?" in html)
        assertTrue("Cost?" in html)
        assertTrue("<strong>callers</strong>" in html, "rationale should render markdown")
    }

    // ----- open questions -----

    @Test
    fun `open questions page renders each entry through markdown`() {
        val html = renderHtml(w(openQuestions = listOf("Should we cache the result?", "What about `null` handling?")))
        assertTrue("""data-page="open-questions"""" in html)
        assertTrue("Should we cache the result?" in html)
        assertTrue("<code>null</code>" in html, "markdown inline code should render")
    }

    // ----- appendix -----

    @Test
    fun `appendix page renders path as code and description as markdown`() {
        val html = renderHtml(
            w(appendix = listOf(
                AppendixEntry(path = "docs/spec.md", description = "The **canonical** spec."),
            )),
        )
        assertTrue("""data-page="appendix"""" in html)
        assertTrue("<code>docs/spec.md</code>" in html)
        assertTrue("<strong>canonical</strong>" in html)
    }
}
