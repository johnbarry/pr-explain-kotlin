package render

import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import model.Diagram
import model.DiagramKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagramBlockTest {

    /** Render a single diagram block to HTML for inspection. */
    private fun renderBlock(d: Diagram): String =
        createHTML().html { body { diagramBlock(d) } }

    @Test
    fun `structural kind emits one mermaid panel and a caption`() {
        val html = renderBlock(
            Diagram(
                kind = DiagramKind.structural,
                after = "flowchart LR\n  A --> B",
                caption = "Two boxes.",
            ),
        )
        assertTrue("""class="diagram diagram--structural"""" in html)
        assertEquals(1, Regex("""<div class="mermaid">""").findAll(html).count())
        assertTrue("flowchart LR" in html)
        assertTrue("Two boxes." in html)
    }

    @Test
    fun `behavioural kind emits one mermaid panel`() {
        val html = renderBlock(
            Diagram(
                kind = DiagramKind.behavioural,
                after = "sequenceDiagram\n  A->>B: x",
                caption = "Round trip.",
            ),
        )
        assertTrue("""class="diagram diagram--behavioural"""" in html)
        assertEquals(1, Regex("""<div class="mermaid">""").findAll(html).count())
        assertTrue("sequenceDiagram" in html)
    }

    @Test
    fun `state kind emits one mermaid panel`() {
        val html = renderBlock(
            Diagram(
                kind = DiagramKind.state,
                after = "stateDiagram-v2\n  [*] --> A",
                caption = "Just state.",
            ),
        )
        assertTrue("""class="diagram diagram--state"""" in html)
        assertEquals(1, Regex("""<div class="mermaid">""").findAll(html).count())
    }

    @Test
    fun `comparison kind emits two labelled mermaid panels`() {
        val html = renderBlock(
            Diagram(
                kind = DiagramKind.comparison,
                before = "flowchart LR\n  A --> B",
                after = "flowchart LR\n  A --> C",
                caption = "Edge moved.",
            ),
        )
        assertTrue("""class="diagram diagram--comparison"""" in html)
        assertTrue("comparison-panels" in html)
        assertEquals(2, Regex("""<div class="mermaid">""").findAll(html).count())
        assertTrue("Before" in html && "After" in html)
        assertTrue("A --&gt; B" in html, "before source escaped into the HTML")
        assertTrue("A --&gt; C" in html, "after source escaped into the HTML")
    }

    @Test
    fun `none kind emits no mermaid panel, just a caption`() {
        val html = renderBlock(
            Diagram(kind = DiagramKind.none, caption = "Plain note."),
        )
        assertTrue("""class="diagram diagram--none"""" in html)
        assertFalse("""<div class="mermaid">""" in html)
        assertTrue("Plain note." in html)
    }

    @Test
    fun `caption is rendered as markdown`() {
        val html = renderBlock(
            Diagram(
                kind = DiagramKind.structural,
                after = "flowchart LR\n  A --> B",
                caption = "See **highlighted** edge.",
            ),
        )
        assertTrue("<strong>highlighted</strong>" in html)
    }

    @Test
    fun `renderHtml inlines mermaid library and init script`() {
        val html = renderHtml(walkthroughWithOneStructuralDiagram())
        // A unique marker from the bundle.
        assertTrue("__esbuild_esm_mermaid_nm" in html, "expected inlined mermaid bundle")
        assertTrue("""globalThis["mermaid"]""" in html || "globalThis['mermaid']" in html)
        // A marker that only the init script contains.
        assertTrue("mermaid.initialize" in html, "expected inlined mermaid-init")
        assertTrue("prefers-color-scheme" in html, "init reads theme preference")
        // No external script src — still self-contained.
        assertFalse(Regex("""<script[^>]+src=""").containsMatchIn(html))
    }

    @Test
    fun `concept page emits a diagram block alongside the premise`() {
        val html = renderHtml(walkthroughWithOneStructuralDiagram())
        assertTrue("""data-page="concept-c1"""" in html)
        assertTrue("""class="diagram diagram--structural"""" in html)
    }

    private fun walkthroughWithOneStructuralDiagram() = model.Walkthrough(
        title = "T", problem = "P", outcome = "O",
        concepts = listOf(
            model.Concept(
                id = "c1", name = "One",
                premise = "Premise.",
                diagram = Diagram(
                    kind = DiagramKind.structural,
                    after = "flowchart LR\n  A --> B",
                    caption = "Edge.",
                ),
            ),
        ),
    )
}
