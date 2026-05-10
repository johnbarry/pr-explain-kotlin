package render

import model.Walkthrough

/**
 * One step in the player. The id becomes the URL hash fragment (e.g. `#concept-foo`)
 * and is also the value of `data-page` on the section element, so JS can look it up.
 */
data class Page(
    val id: String,
    val title: String,
    val type: PageType,
)

enum class PageType {
    COVER,
    CONCEPT_MAP,
    CONCEPT,
    SCRUTINIZE,
    OPEN_QUESTIONS,
    APPENDIX,
}

/**
 * Linear ordering of pages for the player. Optional sections (map, scrutinize,
 * questions, appendix) only appear when the walkthrough actually has content
 * for them — empty pages would just be dead clicks.
 *
 * Concept Map is included only when there are 3+ concepts; with 1–2 a map is
 * noise rather than overview.
 */
fun pageList(w: Walkthrough): List<Page> = buildList {
    add(Page("cover", "Overview", PageType.COVER))
    if (w.concepts.size >= 3) {
        add(Page("concept-map", "Concept map", PageType.CONCEPT_MAP))
    }
    w.concepts.forEach { c ->
        add(Page("concept-${c.id}", c.name, PageType.CONCEPT))
    }
    if (w.scrutinize.isNotEmpty()) {
        add(Page("scrutinize", "Scrutinize", PageType.SCRUTINIZE))
    }
    if (w.openQuestions.isNotEmpty()) {
        add(Page("open-questions", "Open questions", PageType.OPEN_QUESTIONS))
    }
    if (w.appendix.isNotEmpty()) {
        add(Page("appendix", "Appendix", PageType.APPENDIX))
    }
}
