// Theme toggle for generated Javadoc pages.
//
// Kept in sync with the main website docs theme:
// - localStorage key: 'tamboui-theme' (values: 'auto' | 'dark' | 'light')
// - applied via documentElement 'data-theme' attribute ('dark' | 'light' | unset for auto)
(function () {
  'use strict';

  var STORAGE_KEY = 'tamboui-theme';
  var DEFAULT_GITHUB_REPO = 'tamboui/tamboui';

  function getTheme() {
    return localStorage.getItem(STORAGE_KEY) || 'auto';
  }

  function applyTheme(theme) {
    if (theme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
    } else if (theme === 'light') {
      document.documentElement.setAttribute('data-theme', 'light');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  }

  function setTheme(theme) {
    localStorage.setItem(STORAGE_KEY, theme);
    applyTheme(theme);
  }

  // Apply early (prevents flash when user explicitly set a theme).
  try {
    applyTheme(getTheme());
  } catch (e) {
    // ignore (e.g. blocked localStorage)
  }

  function getGitHubRepo() {
    return (window.__TAMBOUI_GITHUB_REPO || DEFAULT_GITHUB_REPO).toString();
  }

  function getGitHubRef() {
    // Provided by the generated config script; fall back to "main".
    return (window.__TAMBOUI_GITHUB_REF || 'main').toString();
  }

  function computeModuleNameFromTitle() {
    // Examples:
    // - "Overview (tamboui-toolkit 1.0.0-SNAPSHOT API)"
    // - "Package dev.tamboui.toolkit (tamboui-toolkit 1.0.0-SNAPSHOT API)"
    var title = document.title || '';
    var m = title.match(/\(([^ )]+)\s+[0-9].*\s+API\)/);
    return m ? m[1] : null;
  }

  function computePackagePathFromUrl() {
    // Javadoc pages store packages as directories in the output.
    // Find "/dev/" and take directories up to the file name.
    var path = window.location.pathname || '';
    var idx = path.indexOf('/dev/');
    if (idx < 0) {
      return null;
    }
    var tail = path.substring(idx + 1); // drop leading "/"
    var parts = tail.split('/');
    if (parts.length < 2) {
      return null;
    }
    // Drop the last segment (file name).
    parts.pop();
    return parts.join('/');
  }

  function computePackageNameFromUrl() {
    var pkgPath = computePackagePathFromUrl();
    return pkgPath ? pkgPath.replace(/\//g, '.') : null;
  }

  function computeOuterClassNameFromUrl() {
    var path = window.location.pathname || '';
    var fileName = path.substring(path.lastIndexOf('/') + 1);
    if (!fileName || !fileName.endsWith('.html')) {
      return null;
    }
    if (fileName === 'package-summary.html' || fileName === 'package-tree.html') {
      return null;
    }
    var base = fileName.replace(/\.html$/, '');
    return base.split('.')[0] || null; // Outer.Inner -> Outer
  }

  function computeGitHubUrlFromInjectedMaps() {
    var repo = getGitHubRepo();
    var ref = getGitHubRef();
    var pkgToDir = window.__TAMBOUI_GITHUB_PACKAGE_TO_DIR_PATH;

    // Class page
    var pkg = computePackageNameFromUrl();
    var outer = computeOuterClassNameFromUrl();
    if (pkg && outer && pkgToDir && pkgToDir[pkg]) {
      return 'https://github.com/' + repo + '/blob/' + ref + '/' + pkgToDir[pkg] + '/' + outer + '.java';
    }

    // Package page
    var path = window.location.pathname || '';
    var fileName = path.substring(path.lastIndexOf('/') + 1);
    if ((fileName === 'package-summary.html' || fileName === 'package-tree.html') && pkgToDir) {
      var pkgPage = computePackageNameFromUrl();
      if (pkgPage && pkgToDir[pkgPage]) {
        return 'https://github.com/' + repo + '/tree/' + ref + '/' + pkgToDir[pkgPage] + '/';
      }
    }

    return null;
  }

  function computeGitHubUrl() {
    var repo = getGitHubRepo();
    var ref = getGitHubRef();

    // Aggregate Javadoc can't reliably infer module name from document.title.
    // Prefer the Gradle-injected mapping when available.
    var mapped = computeGitHubUrlFromInjectedMaps();
    if (mapped) {
      return mapped;
    }

    var moduleName = computeModuleNameFromTitle();
    if (!moduleName) {
      return 'https://github.com/' + repo;
    }

    var path = window.location.pathname || '';
    var fileName = path.substring(path.lastIndexOf('/') + 1);

    // Class-like pages: dev/.../Foo.html or Foo.Bar.html
    if (fileName && fileName.endsWith('.html') && fileName !== 'package-summary.html' && fileName !== 'package-tree.html') {
      var pkg = computePackagePathFromUrl();
      if (pkg) {
        var base = fileName.replace(/\.html$/, '');
        var outer = base.split('.')[0]; // Outer.Inner -> Outer.java
        return 'https://github.com/' + repo + '/blob/' + ref + '/' + moduleName + '/src/main/java/' + pkg + '/' + outer + '.java';
      }
    }

    // Package pages: link to the package directory
    if (fileName === 'package-summary.html' || fileName === 'package-tree.html') {
      var pkgDir = computePackagePathFromUrl();
      if (pkgDir) {
        return 'https://github.com/' + repo + '/tree/' + ref + '/' + moduleName + '/src/main/java/' + pkgDir + '/';
      }
    }

    // Fallback: module root
    return 'https://github.com/' + repo + '/tree/' + ref + '/' + moduleName;
  }

  function getOrCreateNavActionsContainer(anchor) {
    var existing = anchor.querySelector('.nav-actions');
    if (existing) {
      return existing;
    }
    var container = document.createElement('div');
    container.className = 'nav-actions';
    anchor.appendChild(container);
    return container;
  }

  function createThemeToggle(container) {
    // Javadoc templates vary across JDK versions; try a few anchors.
    var header = document.querySelector('header[role="banner"]') || document.querySelector('header');
    var topNav = document.querySelector('.top-nav') || (header ? header.querySelector('.top-nav') : null);
    var anchor = topNav || header || document.body;
    if (!anchor) {
      return;
    }

    // Avoid duplicates across pages / reruns.
    if (anchor.querySelector('.theme-toggle')) {
      return;
    }

    var toggle = document.createElement('button');
    toggle.className = 'theme-toggle';
    toggle.type = 'button';
    toggle.setAttribute('aria-label', 'Toggle theme');
    toggle.innerHTML =
      '<svg class="icon-sun" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">' +
      '<path fill="currentColor" d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 00-1.41 0 .996.996 0 000 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 00-1.41 0 .996.996 0 000 1.41l1.06 1.06c.39.39 1.03.39 1.41 0a.996.996 0 000-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 000-1.41.996.996 0 00-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 000-1.41.996.996 0 00-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/>' +
      '</svg>' +
      '<svg class="icon-moon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">' +
      '<path fill="currentColor" d="M12 3a9 9 0 109 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 01-4.4 2.26 5.403 5.403 0 01-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"/>' +
      '</svg>';

    toggle.addEventListener('click', function () {
      var current = getTheme();
      var next;
      if (current === 'auto') {
        next = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'light' : 'dark';
      } else if (current === 'dark') {
        next = 'light';
      } else {
        next = 'dark';
      }
      setTheme(next);
    });

    (container || getOrCreateNavActionsContainer(anchor)).appendChild(toggle);
  }

  function createGitHubLink(container) {
    var header = document.querySelector('header[role="banner"]') || document.querySelector('header');
    var topNav = document.querySelector('.top-nav') || (header ? header.querySelector('.top-nav') : null);
    var anchor = topNav || header || document.body;
    if (!anchor) {
      return;
    }

    if (anchor.querySelector('.github-link')) {
      return;
    }

    var link = document.createElement('a');
    link.className = 'github-link';
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.href = computeGitHubUrl();
    link.textContent = 'GitHub';

    (container || getOrCreateNavActionsContainer(anchor)).appendChild(link);
  }

  // Keep multiple tabs in sync (when hosted under the same origin as the website).
  window.addEventListener('storage', function (e) {
    if (!e || e.key !== STORAGE_KEY) {
      return;
    }
    applyTheme(getTheme());
  });

  function initUi() {
    var header = document.querySelector('header[role="banner"]') || document.querySelector('header');
    var topNav = document.querySelector('.top-nav') || (header ? header.querySelector('.top-nav') : null);
    var anchor = topNav || header || document.body;
    if (!anchor) {
      return;
    }
    var container = getOrCreateNavActionsContainer(anchor);
    createGitHubLink(container);
    createThemeToggle(container);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initUi);
  } else {
    initUi();
  }
})();


