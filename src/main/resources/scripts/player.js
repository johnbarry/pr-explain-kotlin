// Walkthrough player. Reads <section data-page="..."> elements, shows one at
// a time based on the URL hash, handles prev/next clicks, keyboard shortcuts,
// and dispatches a `page:show` CustomEvent so future enhancements (Mermaid
// re-render, syntax highlight, focus marker) can hook in without touching
// this file.
(function () {
  'use strict';

  const sections = Array.from(document.querySelectorAll('section[data-page]'));
  const chips = Array.from(document.querySelectorAll('.chip[data-target]'));
  const prevBtn = document.querySelector('.btn-prev');
  const nextBtn = document.querySelector('.btn-next');
  const positionEl = document.querySelector('.controls-position');

  if (sections.length === 0) return;

  const ids = sections.map((s) => s.getAttribute('data-page'));

  function indexFromHash() {
    const hash = (location.hash || '').replace(/^#/, '');
    const i = ids.indexOf(hash);
    return i === -1 ? 0 : i;
  }

  function show(i) {
    const clamped = Math.max(0, Math.min(sections.length - 1, i));
    const id = ids[clamped];

    sections.forEach((s, idx) => {
      s.classList.toggle('is-current', idx === clamped);
    });
    chips.forEach((c) => {
      const active = c.getAttribute('data-target') === id;
      c.classList.toggle('is-current', active);
      // aria-current advertises the active nav item to assistive tech.
      if (active) c.setAttribute('aria-current', 'page');
      else c.removeAttribute('aria-current');
    });

    if (prevBtn) prevBtn.disabled = clamped === 0;
    if (nextBtn) nextBtn.disabled = clamped === sections.length - 1;
    if (positionEl) {
      positionEl.textContent = (clamped + 1) + ' / ' + sections.length;
    }

    // Keep the hash in sync without polluting browser history.
    const desiredHash = '#' + id;
    if (location.hash !== desiredHash) {
      history.replaceState(null, '', desiredHash);
    }

    // Scroll to top of the new page so long pages don't preserve scroll.
    window.scrollTo({ top: 0, behavior: 'instant' });

    document.dispatchEvent(new CustomEvent('page:show', {
      detail: { id: id, index: clamped, total: sections.length }
    }));
  }

  function go(delta) {
    const current = ids.indexOf(
      (sections.find((s) => s.classList.contains('is-current')) || sections[0])
        .getAttribute('data-page')
    );
    show(current + delta);
  }

  // Click handlers.
  if (prevBtn) prevBtn.addEventListener('click', () => go(-1));
  if (nextBtn) nextBtn.addEventListener('click', () => go(+1));

  chips.forEach((c) => {
    c.addEventListener('click', (e) => {
      e.preventDefault();
      const id = c.getAttribute('data-target');
      const i = ids.indexOf(id);
      if (i !== -1) show(i);
    });
  });

  // Keyboard shortcuts.
  // Skip when focus is on a form element so typing in inputs isn't hijacked.
  document.addEventListener('keydown', (e) => {
    const t = e.target;
    if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.isContentEditable)) {
      return;
    }
    if (e.metaKey || e.ctrlKey || e.altKey) return;

    switch (e.key) {
      case 'ArrowLeft':
      case 'k':
        e.preventDefault();
        go(-1);
        break;
      case 'ArrowRight':
      case 'j':
        e.preventDefault();
        go(+1);
        break;
      case 'Home':
        e.preventDefault();
        show(0);
        break;
      case 'End':
        e.preventDefault();
        show(sections.length - 1);
        break;
      case 'Escape':
        e.preventDefault();
        show(0);
        break;
    }
  });

  // Browser back/forward.
  window.addEventListener('hashchange', () => show(indexFromHash()));

  // Initial render.
  show(indexFromHash());
})();
