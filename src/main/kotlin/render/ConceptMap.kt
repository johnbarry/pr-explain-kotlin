package render

import model.Walkthrough

/**
 * Build a simple Mermaid flowchart that lists the walkthrough's concepts in
 * reading order. The schema doesn't model relationships between concepts —
 * the map just gives readers an overview of what's coming and what to expect.
 *
 * Layout: left-to-right linear chain (`c0 --> c1 --> ...`). Each node label
 * is the concept name, quoted so any reserved Mermaid characters (parens,
 * brackets, pipes) pass through. Internal double-quotes are HTML-escaped
 * with `&quot;` per Mermaid's quoted-label convention.
 */
fun conceptMapMermaid(w: Walkthrough): String {
    if (w.concepts.isEmpty()) return "flowchart LR\n  empty[\"(no concepts)\"]"

    val sb = StringBuilder("flowchart LR\n")
    w.concepts.forEachIndexed { i, c ->
        val label = c.name.replace("\"", "&quot;")
        sb.append("  c").append(i).append("[\"").append(label).append("\"]\n")
    }
    for (i in 0 until w.concepts.size - 1) {
        sb.append("  c").append(i).append(" --> c").append(i + 1).append('\n')
    }
    return sb.toString()
}
