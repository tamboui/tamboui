(function () {
  'use strict';

  function byId(id) { return document.getElementById(id); }

  function ensureSearchUI() {
    var header = byId('header');
    if (!header) return null;

    var wrap = document.createElement('div');
    wrap.className = 'doc-search';
    wrap.innerHTML = '' +
      '<input id="doc-search-input" type="search" placeholder="Search docs..." aria-label="Search docs" />' +
      '<div id="doc-search-results" class="doc-search-results" hidden></div>';

    // Place search as first item in top navigation row (before Home/Getting Started/...)
    var nav = header.querySelector('.top-nav');
    if (nav) {
      nav.insertBefore(wrap, nav.firstChild);
    } else {
      // fallback: after title
      var title = header.querySelector('h1');
      if (title && title.parentNode) {
        title.insertAdjacentElement('afterend', wrap);
      } else {
        header.insertBefore(wrap, header.firstChild);
      }
    }

    return {
      input: byId('doc-search-input'),
      results: byId('doc-search-results')
    };
  }

  function snippet(text, q) {
    var idx = text.toLowerCase().indexOf(q.toLowerCase());
    if (idx < 0) return text.slice(0, 180);
    var start = Math.max(0, idx - 70);
    var end = Math.min(text.length, idx + 110);
    return (start > 0 ? '…' : '') + text.slice(start, end) + (end < text.length ? '…' : '');
  }

  function renderResults(container, docs, query, hits) {
    if (!query.trim()) {
      container.hidden = true;
      container.innerHTML = '';
      return;
    }
    if (!hits.length) {
      container.hidden = false;
      container.innerHTML = '<div class="doc-search-empty">No results for "' + query.replace(/</g, '&lt;') + '"</div>';
      return;
    }

    var html = hits.slice(0, 12).map(function (hit) {
      var d = docs[hit.ref];
      if (!d) return '';
      return '<a class="doc-search-item" href="' + d.url + '">' +
        '<div class="title">' + d.title + '</div>' +
        '<div class="snippet">' + snippet(d.body || '', query).replace(/</g, '&lt;') + '</div>' +
      '</a>';
    }).join('');

    container.hidden = false;
    container.innerHTML = html;
  }

  function init() {
    var ui = ensureSearchUI();
    if (!ui) return;

    function loadIndex() {
      return fetch('search-index.json').then(function (r) {
        if (!r.ok) throw new Error('index not found at relative path');
        return r.json();
      }).catch(function () {
        return fetch('/search-index.json').then(function (r) {
          if (!r.ok) throw new Error('index not found at root path');
          return r.json();
        });
      });
    }

    loadIndex()
      .then(function (entries) {
        var docs = {};
        entries.forEach(function (d, i) {
          docs[String(i)] = d;
        });

        var searchFn;
        if (typeof lunr !== 'undefined') {
          var idx = lunr(function () {
            this.ref('id');
            this.field('title', { boost: 10 });
            this.field('body');
            entries.forEach(function (d, i) {
              this.add({ id: String(i), title: d.title || '', body: d.body || '' });
            }, this);
          });
          searchFn = function (q) {
            return idx.search(q + '*');
          };
        } else {
          // Fallback when Lunr CDN is unavailable (offline/CSP): simple contains ranking
          searchFn = function (q) {
            var qq = q.toLowerCase();
            var hits = [];
            Object.keys(docs).forEach(function (id) {
              var d = docs[id];
              var t = (d.title || '').toLowerCase();
              var b = (d.body || '').toLowerCase();
              var score = 0;
              if (t.indexOf(qq) >= 0) score += 10;
              if (b.indexOf(qq) >= 0) score += 1;
              if (score > 0) hits.push({ ref: id, score: score });
            });
            hits.sort(function (a, b) { return b.score - a.score; });
            return hits;
          };
        }

        ui.input.addEventListener('input', function () {
          var q = ui.input.value.trim();
          if (!q) return renderResults(ui.results, docs, q, []);
          var hits = searchFn(q);
          renderResults(ui.results, docs, q, hits);
        });

        document.addEventListener('click', function (e) {
          if (!ui.results.contains(e.target) && e.target !== ui.input) {
            ui.results.hidden = true;
          }
        });
      })
      .catch(function () {
        // fail quietly; docs still usable without search
      });
  }

  document.addEventListener('DOMContentLoaded', init);
})();
