package render

import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import model.Excerpt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExcerptsTest {

    private fun render(excerpts: List<Excerpt>): String =
        createHTML().html { body { excerptsBlock(excerpts) } }

    @Test
    fun `empty list renders nothing`() {
        val html = render(emptyList())
        assertFalse("excerpts-list" in html)
        assertFalse("Key excerpts" in html)
    }

    @Test
    fun `excerpt renders path, code body and caption`() {
        val html = render(
            listOf(
                Excerpt(
                    file = "src/main/kotlin/A.kt",
                    excerpt = "fun a() = 1",
                    caption = "Trivial.",
                ),
            ),
        )
        assertTrue("excerpt-path" in html)
        assertTrue("src/main/kotlin/A.kt" in html)
        assertTrue("fun a() = 1" in html)
        assertTrue("Trivial." in html)
        // No line hint provided.
        assertFalse("excerpt-line" in html)
    }

    @Test
    fun `line hint renders next to path`() {
        val html = render(
            listOf(
                Excerpt(
                    file = "A.kt",
                    lineHint = 42,
                    excerpt = "x",
                    caption = "c",
                ),
            ),
        )
        assertTrue("excerpt-line" in html)
        assertTrue(":42" in html)
    }

    @Test
    fun `caption is rendered as markdown`() {
        val html = render(
            listOf(
                Excerpt(file = "A.kt", excerpt = "x", caption = "See **highlighted** line."),
            ),
        )
        assertTrue("<strong>highlighted</strong>" in html)
    }

    @Test
    fun `focus marker in excerpt highlights only the marked line`() {
        val html = render(
            listOf(
                Excerpt(
                    file = "A.kt",
                    excerpt = "val a = 1\nval b = 2 // ←\nval c = 3",
                    caption = "c",
                ),
            ),
        )
        assertEquals(1, Regex("""code-line--focus""").findAll(html).count())
        assertTrue("// ←" in html)
    }

    @Test
    fun `excerpt code body HTML-escapes specials`() {
        val html = render(
            listOf(
                Excerpt(file = "A.kt", excerpt = "Map<String, List<Int>>", caption = "c"),
            ),
        )
        assertTrue("Map&lt;String, List&lt;Int&gt;&gt;" in html)
    }

    @Test
    fun `multiple excerpts render in order`() {
        val html = render(
            listOf(
                Excerpt(file = "A.kt", excerpt = "first", caption = "1"),
                Excerpt(file = "B.kt", excerpt = "second", caption = "2"),
                Excerpt(file = "C.kt", excerpt = "third", caption = "3"),
            ),
        )
        val a = html.indexOf("A.kt")
        val b = html.indexOf("B.kt")
        val c = html.indexOf("C.kt")
        assertTrue(a in 0..<b && b < c)
    }

    @Test
    fun `concept page emits excerpts block when keyExcerpts present`() {
        val w = model.Walkthrough(
            title = "T", problem = "P", outcome = "O",
            concepts = listOf(
                model.Concept(
                    id = "c1", name = "One",
                    premise = "p",
                    diagram = model.Diagram(
                        kind = model.DiagramKind.structural,
                        after = "flowchart LR\n  A-->B",
                        caption = "c",
                    ),
                    keyExcerpts = listOf(
                        Excerpt(file = "f.kt", lineHint = 1, excerpt = "x", caption = "c"),
                    ),
                ),
            ),
        )
        val html = renderHtml(w)
        assertTrue("excerpts-heading" in html)
        assertTrue("f.kt" in html)
    }
}
