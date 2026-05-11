# pr-explain-kotlin

A Kotlin CLI that turns a "walkthrough YAML" file describing a change into a
self-contained interactive HTML walkthrough — concept by concept, with
Mermaid diagrams, structural-change summaries, and key code excerpts. The
output is one HTML file that opens from `file://` with no server and no
neighbours.

There is also a pure-JavaScript SPA version of the same renderer:
[`pr-explain`](https://github.com/johnbarry/pr-explain). Same schema, same
output shape — pick whichever fits your toolchain.

## Quick start

```
git clone https://github.com/johnbarry/pr-explain-kotlin.git
cd pr-explain-kotlin
gradle run --args="render -i walkthrough.yaml -o walkthrough.html"
```

The bundled `walkthrough.yaml` is a complete sample (the polling-to-Kafka
migration write-up). Open `walkthrough.html` in any modern browser.

### Validate without rendering

```
gradle run --args="validate walkthrough.yaml"
```

Errors block rendering; warnings are advisory. The validator reports a
YAML-style path (`concepts[2].keyExcerpts[1].lineHint`) so you can jump
straight to the offending node.

## Requirements

- JDK 21
- Gradle 8.5+ (or use the system `gradle`; no wrapper is shipped)

## Layout

```
src/main/kotlin/
  Main.kt                  CLI entry point (Clikt: `render`, `validate`)
  model/Walkthrough.kt     YAML schema (data classes, kotlinx.serialization)
  parse/YamlParser.kt      kaml-backed parse with snake_case mapping
  validate/Validator.kt    schema validator (errors + warnings)
  render/                  HTML emission (kotlinx.html)
    pages/                 per-page renderers (cover, concept, scrutinize, ...)
src/main/resources/
  styles/main.css          stylesheet (inlined into output)
  scripts/                 player JS, Mermaid init
  vendor/mermaid.min.js    bundled Mermaid 11 (inlined into output)
src/test/kotlin/           JUnit 5 tests (~73 tests; validator + renderer)
fixture.yaml               minimal sample exercising every schema field
fixture-broken.yaml        sample with deliberate validator violations
walkthrough.yaml           realistic 5-concept sample
```

## How the output works

The renderer emits a single HTML document with the CSS, the Mermaid bundle,
and the player JS inlined. Each page is a `<section data-page="…">` and the
player toggles `.is-current` to show one at a time. Mermaid runs lazily on
`page:show` because Mermaid 11's `useMaxWidth: true` measures hidden
ancestors as 0×0 — rendering only when the page is visible keeps diagrams at
their correct size.

## Generating walkthrough YAML — prompt

The CLI consumes a YAML file. The easy way to produce one is to paste a
commit or PR diff into an LLM with the prompt below. The schema is strict;
the prompt pins it so the model cannot invent fields.

> **Example topic to try this on:** the
> [`Refactor for elegance` commit (`2e218bb6`)](https://github.com/johnbarry/grpc-observability-demo/commit/2e218bb6)
> from `grpc-observability-demo` — 9 files, ~840 lines changed, multiple
> independent ideas (dedup service methods, shared test utils, correctness
> fixes). A commit like that is exactly where a walkthrough beats a raw diff.

Paste the diff and the prompt below into a capable model (Claude Opus /
Sonnet work well), save the response as `my-walkthrough.yaml`, then:

```
gradle run --args="render -i my-walkthrough.yaml -o my-walkthrough.html"
```

````
You are writing a *concept-first walkthrough* of a code change for a senior
reviewer. The output is YAML that another tool will render into HTML. Stick
to the schema exactly — do not invent fields, do not omit required fields.

# Output format — strict

```yaml
walkthrough:
  title: <short, descriptive>
  problem: |
    <2–4 sentences in markdown: why this change exists>
  outcome: |
    <2–4 sentences in markdown: what a reviewer should be able to answer
    after reading>
  audience_hint: <optional one-liner about prerequisites>

  concepts:           # 2..5 recommended; anything outside that range is a warning, not an error
    - id: <kebab-case>
      name: <short noun phrase>
      premise: |
        <markdown: the load-bearing idea behind this seam>
      diagram:
        kind: structural | behavioural | state | comparison | none
        # `comparison` requires both `before:` and `after:` (mermaid source).
        # `structural`/`behavioural`/`state` require only `after:`.
        # `none` requires neither.
        before: |
          <mermaid>     # only when kind=comparison
        after: |
          <mermaid>     # required unless kind=none
        caption: <one short sentence about what the diagram shows>
      structural_changes:           # optional, list
        - kind: added | removed | signature_changed | responsibility_moved | renamed
          subject: <thing that changed, e.g. ClassName.methodName>
          # `added`: requires `after:` only.
          # `removed`: requires `before:` only.
          # `signature_changed`: requires both `before:` and `after:`.
          # `responsibility_moved`: caption only (no before/after).
          # `renamed`: caption only; `before:`/`after:` allowed if the rename is non-obvious.
          before: |
            <code>
          after: |
            <code>
          caption: |
            <markdown: why this change matters in one or two lines>
      key_excerpts:                 # optional, up to 3
        - file: <repo-relative path>
          line_hint: <int, optional>
          excerpt: |
            <code; mark the one load-bearing line with `// ←` or `# ←`
             at end of line so it is highlighted>
          caption: |
            <markdown: what to notice on the marked line>

  scrutinize:         # optional, list of self-check prompts
    - prompt: |
        <one sentence question a reviewer should be able to answer>
      rationale: |
        <markdown answer / discussion>

  open_questions:     # optional, list of strings (markdown allowed)
    - <decision not yet made>

  appendix:           # optional, list of pointers
    - path: <repo-relative file or URL>
      description: <one line>
```

# Rules

1. **Concept count: aim for 2–5.** Each concept is *one idea*, not one file.
   A refactor that touches 9 files often still has only 3–4 ideas.
2. **`premise` is the load-bearing field.** It says *why* the seam exists —
   not what was changed. The diff already says what.
3. **Diagram `kind` discipline.** Use `comparison` only when the before/after
   contrast is itself the point (e.g. polling → push). Use `structural` for
   class/interface relationships, `behavioural` for sequence diagrams, `state`
   for state machines, `none` when the caption alone carries the meaning.
4. **Focus marker.** Inside `key_excerpts.excerpt`, put `// ←` (C-family) or
   `# ←` (Python/shell) at the end of *exactly one* line per excerpt — the
   load-bearing one. The renderer highlights it.
5. **Mermaid sources** must start with a recognised declaration:
   `flowchart`, `classDiagram`, `sequenceDiagram`, `stateDiagram-v2`,
   `erDiagram`, or `C4{Context,Container,Component}`.
6. **`structural_changes` is for design-level shifts**, not every diff hunk.
   Prefer 3–6 changes per concept; if a concept needs more, it's two concepts.
7. **No `before:` for `kind: added`. No `after:` for `kind: removed`.** These
   are validation errors.
8. **`scrutinize` items are questions, not statements.** The `rationale` is
   the author's answer — what the reader should be able to articulate.

# Your task

The user will paste a commit diff, a PR description, or both. Produce the YAML
walkthrough. Do not include any prose outside the YAML.
````

## Tests

```
gradle test
```

Runs the validator and renderer test suites (~73 tests).

## Schema reference

The full schema lives in `src/main/kotlin/model/Walkthrough.kt`; validation
rules are in `src/main/kotlin/validate/Validator.kt`. Both this project and
the JavaScript SPA implement identical rules.
