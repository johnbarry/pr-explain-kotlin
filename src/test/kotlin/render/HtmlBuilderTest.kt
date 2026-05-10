package render

import model.Concept
import model.Diagram
import model.DiagramKind
import model.ScrutinizeItem
import model.Walkthrough
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlBuilderTest {

    private fun concept(id: String, name: String = "Concept $id") = Concept(
        id = id,
        name = name,
        premise = "premise",
        diagram = Diagram(kind = DiagramKind.structural, after = "flowchart LR\n  A-->B", caption = "c"),
    )

    private fun walkthrough(
        concepts: List<Concept> = listOf(concept("a"), concept("b")),
        scrutinize: List<ScrutinizeItem> = emptyList(),
        openQuestions: List<String> = emptyList(),
    ) = Walkthrough(
        title = "Test walkthrough",
        problem = "P",
        outcome = "O",
        concepts = concepts,
        scrutinize = scrutinize,
        openQuestions = openQuestions,
    )

    @Test
    fun `pageList includes cover and per-concept pages, skips map for under 3 concepts`() {
        val pages = pageList(walkthrough())
        assertEquals(listOf("cover", "concept-a", "concept-b"), pages.map { it.id })
    }

    @Test
    fun `pageList includes concept-map when concepts size is 3 or more`() {
        val w = walkthrough(concepts = listOf(concept("a"), concept("b"), concept("c")))
        val pages = pageList(w)
        assertEquals(
            listOf("cover", "concept-map", "concept-a", "concept-b", "concept-c"),
            pages.map { it.id },
        )
    }

    @Test
    fun `pageList includes optional sections only when populated`() {
        val w = walkthrough(
            scrutinize = listOf(ScrutinizeItem("p", "r")),
            openQuestions = listOf("q1"),
        )
        val pages = pageList(w)
        assertTrue("scrutinize" in pages.map { it.id })
        assertTrue("open-questions" in pages.map { it.id })
        assertTrue("appendix" !in pages.map { it.id })
    }

    @Test
    fun `renderHtml emits one section per page`() {
        val w = walkthrough()
        val html = renderHtml(w)
        val expected = pageList(w).size
        // Match the rendered HTML class — distinct enough to avoid colliding
        // with the literal "<section data-page" string in the inlined JS comment.
        val sectionCount = Regex("""<section class="page page--""").findAll(html).count()
        assertEquals(expected, sectionCount, "section count should match pageList size")
    }

    @Test
    fun `renderHtml emits one nav chip per page`() {
        val w = walkthrough()
        val html = renderHtml(w)
        val expected = pageList(w).size
        val chipCount = Regex("""class="chip"""").findAll(html).count()
        assertEquals(expected, chipCount, "chip count should match pageList size")
    }

    @Test
    fun `renderHtml inlines CSS and JS so the page is self-contained`() {
        val html = renderHtml(walkthrough())
        // CSS marker — a class only the bundled stylesheet defines.
        assertTrue("topnav-chips" in html, "expected inlined CSS")
        // JS marker — a string literal that only appears in the bundled script.
        assertTrue("page:show" in html, "expected inlined player JS")
        // No external asset references.
        assertTrue(
            !Regex("""<link[^>]+rel="stylesheet"""").containsMatchIn(html),
            "expected no external stylesheet links",
        )
        assertTrue(
            !Regex("""<script[^>]+src=""").containsMatchIn(html),
            "expected no external script tags",
        )
    }

    @Test
    fun `renderHtml emits prev next controls and position indicator`() {
        val html = renderHtml(walkthrough())
        assertTrue("btn-prev" in html)
        assertTrue("btn-next" in html)
        assertTrue("controls-position" in html)
    }

    @Test
    fun `renderHtml uses walkthrough title in title tag and topnav`() {
        val html = renderHtml(walkthrough())
        assertTrue("<title>Test walkthrough</title>" in html)
    }

    @Test
    fun `concept page anchor uses the concept id`() {
        val html = renderHtml(walkthrough(concepts = listOf(concept("foo-bar"))))
        assertTrue("""data-page="concept-foo-bar"""" in html)
    }
}
