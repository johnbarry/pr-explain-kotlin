package render

import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import model.ChangeKind
import model.StructuralChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructuralChangesTest {

    private fun render(changes: List<StructuralChange>): String =
        createHTML().html { body { structuralChangesBlock(changes) } }

    // ----- empty -----

    @Test
    fun `empty list renders nothing`() {
        val html = render(emptyList())
        assertFalse("changes-list" in html)
        assertFalse("Structural changes" in html)
    }

    // ----- per-kind layout -----

    @Test
    fun `added emits only after panel with green accent class`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.added,
                    subject = "NewType",
                    after = "class NewType { }",
                    caption = "Added.",
                ),
            ),
        )
        assertTrue("change--added" in html)
        assertTrue("class NewType { }" in html.replace("&lt;", "<").replace("&gt;", ">"))
        assertTrue("code--after" in html)
        assertFalse("code--before" in html)
        assertTrue("Added" in html) // label
    }

    @Test
    fun `removed emits only before panel with red accent class`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.removed,
                    subject = "OldType",
                    before = "class OldType { }",
                    caption = "Gone.",
                ),
            ),
        )
        assertTrue("change--removed" in html)
        assertTrue("code--before" in html)
        assertFalse("code--after" in html)
        assertTrue("Removed" in html)
    }

    @Test
    fun `signature_changed emits both before and after panels`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.signature_changed,
                    subject = "f",
                    before = "fun f(): Int",
                    after = "fun f(scale: Double): Int",
                    caption = "Now scaled.",
                ),
            ),
        )
        assertTrue("change--signature_changed" in html)
        assertTrue("code--before" in html)
        assertTrue("code--after" in html)
        assertTrue("change-panel--before" in html)
        assertTrue("change-panel--after" in html)
        assertTrue("Signature changed" in html)
    }

    @Test
    fun `responsibility_moved emits caption only - no code panels`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.responsibility_moved,
                    subject = "error inspection",
                    caption = "Moved to sendMessage().",
                ),
            ),
        )
        assertTrue("change--responsibility_moved" in html)
        assertFalse("<pre class=\"code" in html, "responsibility_moved should have no code blocks")
        assertTrue("Moved to sendMessage()." in html)
        assertTrue("Responsibility moved" in html)
    }

    @Test
    fun `renamed without code blocks emits caption only`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.renamed,
                    subject = "Old → New",
                    caption = "Just a rename.",
                ),
            ),
        )
        assertTrue("change--renamed" in html)
        assertFalse("<pre" in html)
    }

    @Test
    fun `renamed with both before and after renders as inline pair`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.renamed,
                    subject = "Foo",
                    before = "Foo",
                    after = "Bar",
                    caption = "Renamed.",
                ),
            ),
        )
        assertTrue("rename-pair" in html)
        assertTrue("rename-from" in html && "rename-to" in html)
        assertTrue("→" in html, "expected arrow separator")
        assertFalse("<pre" in html, "inline pair should not produce a pre block")
    }

    // ----- caption is markdown -----

    @Test
    fun `change caption is rendered as markdown`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.added,
                    subject = "X",
                    after = "x",
                    caption = "Adds **focus** here.",
                ),
            ),
        )
        assertTrue("<strong>focus</strong>" in html)
    }

    // ----- multiple items -----

    @Test
    fun `multiple changes render in order`() {
        val html = render(
            listOf(
                StructuralChange(kind = ChangeKind.added, subject = "A", after = "a", caption = "added"),
                StructuralChange(kind = ChangeKind.removed, subject = "B", before = "b", caption = "removed"),
                StructuralChange(
                    kind = ChangeKind.signature_changed, subject = "C",
                    before = "c", after = "c2", caption = "sig",
                ),
            ),
        )
        val aPos = html.indexOf("change--added")
        val bPos = html.indexOf("change--removed")
        val cPos = html.indexOf("change--signature_changed")
        assertTrue(aPos in 0..<bPos && bPos < cPos, "order should match input")
    }

    // ----- icon labels -----

    @Test
    fun `each kind renders its icon`() {
        val html = render(
            listOf(
                StructuralChange(kind = ChangeKind.added, subject = "a", after = "x", caption = "c"),
                StructuralChange(kind = ChangeKind.removed, subject = "b", before = "x", caption = "c"),
                StructuralChange(kind = ChangeKind.signature_changed, subject = "c", before = "x", after = "y", caption = "c"),
                StructuralChange(kind = ChangeKind.responsibility_moved, subject = "d", caption = "c"),
                StructuralChange(kind = ChangeKind.renamed, subject = "e", caption = "c"),
            ),
        )
        // Each icon must appear at least once.
        for (icon in listOf("+", "−", "∼", "↪", "✎")) {
            assertTrue(icon in html, "missing icon: $icon")
        }
    }

    // ----- focus-line marker -----

    @Test
    fun `code lines without focus marker get plain code-line class`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.added, subject = "x",
                    after = "fun a() = 1\nfun b() = 2",
                    caption = "c",
                ),
            ),
        )
        // Two lines, none marked.
        assertEquals(2, Regex("""class="code-line"""").findAll(html).count())
        assertEquals(0, Regex("""code-line--focus""").findAll(html).count())
    }

    @Test
    fun `focus-line marker double-slash highlights only the marked line`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.added, subject = "x",
                    after = "val a = 1\nval b = 2 // ←\nval c = 3",
                    caption = "c",
                ),
            ),
        )
        assertEquals(1, Regex("""code-line--focus""").findAll(html).count())
        assertEquals(2, Regex("""class="code-line"""").findAll(html).count())
        // The marker text stays visible in the rendered line.
        assertTrue("// ←" in html, "focus marker should remain visible")
    }

    @Test
    fun `focus-line marker hash highlights for python-style comments`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.added, subject = "x",
                    after = "a = 1\nb = 2  # ←\nc = 3",
                    caption = "c",
                ),
            ),
        )
        assertEquals(1, Regex("""code-line--focus""").findAll(html).count())
    }

    @Test
    fun `code body HTML-escapes special characters`() {
        val html = render(
            listOf(
                StructuralChange(
                    kind = ChangeKind.added, subject = "x",
                    after = "if (a < b && c > d) { }",
                    caption = "c",
                ),
            ),
        )
        assertTrue("a &lt; b &amp;&amp; c &gt; d" in html, "expected HTML escaping")
        assertFalse(Regex("""a < b && c > d""").containsMatchIn(html))
    }
}
