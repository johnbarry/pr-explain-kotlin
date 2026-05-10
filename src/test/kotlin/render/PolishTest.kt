package render

import model.Concept
import model.Diagram
import model.DiagramKind
import model.Walkthrough
import kotlin.test.Test
import kotlin.test.assertTrue

class PolishTest {

    private fun sample() = Walkthrough(
        title = "T", problem = "P", outcome = "O",
        concepts = listOf(
            Concept(
                id = "c1", name = "One",
                premise = "p",
                diagram = Diagram(
                    kind = DiagramKind.structural,
                    after = "flowchart LR\n  A-->B",
                    caption = "c",
                ),
            ),
        ),
    )

    @Test
    fun `output includes a print stylesheet`() {
        val html = renderHtml(sample())
        assertTrue("@media print" in html, "expected @media print rule in stylesheet")
        // A few of the print-only declarations.
        assertTrue("color-adjust" in html)
        assertTrue("break-after: page" in html)
    }

    @Test
    fun `player JS sets aria-current on the active chip`() {
        val html = renderHtml(sample())
        assertTrue("aria-current" in html, "player should toggle aria-current")
    }

    @Test
    fun `focus-visible rule is present for keyboard nav`() {
        val html = renderHtml(sample())
        assertTrue(":focus-visible" in html)
    }
}
