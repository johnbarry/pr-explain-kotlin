// Mermaid initialization. Runs synchronously after the mermaid bundle has
// loaded and registered `window.mermaid`.
//
// Strategy: render *lazily* on `page:show`, not on DOMContentLoaded.
//
// Why: the player starts with every non-current page at `display: none`.
// Mermaid 11 with `useMaxWidth: true` measures the diagram's containing
// box via getBoundingClientRect at render time — inside a `display:none`
// ancestor that returns 0, and the SVG is laid out at minimum size. The
// first time the user navigates to that page the SVG is locked at the
// wrong dimensions because Mermaid has already set `data-processed` and
// won't re-run.
//
// By disabling `startOnLoad` and triggering `mermaid.run({nodes: ...})`
// from inside the `page:show` handler in player.js, every diagram is
// laid out at the moment its page becomes visible — when the parent's
// box is real.
(function () {
  'use strict';
  if (typeof window.mermaid === 'undefined') return;

  const dark = window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches;

  window.mermaid.initialize({
    startOnLoad: false,
    theme: dark ? 'dark' : 'default',
    themeVariables: {
      background: 'transparent',
      primaryTextColor: dark ? '#ececec' : '#1a1a1a',
    },
    securityLevel: 'strict',
    flowchart: { useMaxWidth: true, htmlLabels: true },
    sequence: { useMaxWidth: true },
    state: { useMaxWidth: true },
    er: { useMaxWidth: true },
  });

  // Process any `.mermaid` elements in the page that just became visible.
  // Re-firing is cheap: the run() call only touches elements that aren't
  // yet `data-processed`. We tolerate failures so a single broken diagram
  // doesn't break navigation for the rest of the document.
  document.addEventListener('page:show', function (e) {
    var id = e && e.detail && e.detail.id;
    if (!id) return;
    var section = document.querySelector('[data-page="' + id + '"]');
    if (!section) return;
    var unprocessed = section.querySelectorAll('.mermaid:not([data-processed])');
    if (unprocessed.length === 0) return;
    try {
      var maybePromise = window.mermaid.run({ nodes: Array.prototype.slice.call(unprocessed) });
      if (maybePromise && typeof maybePromise.catch === 'function') {
        maybePromise.catch(function () { /* swallow per-diagram errors */ });
      }
    } catch (_err) { /* swallow */ }
  });
})();
