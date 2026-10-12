/*
  The only script on this site. No network requests from this script, no cookies, no storage, no third-party scripts.

  1. Early-access form: submitted natively to Netlify Forms (no script needed). In a LOCAL preview only
     (localhost, 127.0.0.1 or a file:// page) there is no Netlify, so the submit is stubbed: it checks the
     email is filled, sends nothing anywhere, and goes to thanks.html. Live sites are untouched.
  2. Funnel attribution: copies utm_source / utm_medium / utm_campaign from the page URL into the
     hidden "source" field, and carries those UTM parameters onto internal links so a visitor who
     lands on about.html?utm_... keeps the tag when they reach the form. Nothing leaves the browser.
*/
(function () {
  var params = new URLSearchParams(window.location.search);
  var keys = ['utm_source', 'utm_medium', 'utm_campaign'];
  var utm = new URLSearchParams();
  keys.forEach(function (k) {
    var v = params.get(k);
    if (v) utm.set(k, v.slice(0, 64));
  });
  var utmString = utm.toString();

  if (utmString) {
    Array.prototype.forEach.call(document.querySelectorAll('a[href]'), function (a) {
      var href = a.getAttribute('href');
      if (!/^(index|about|privacy)\.html/.test(href)) return;
      var hashAt = href.indexOf('#');
      var path = hashAt === -1 ? href : href.slice(0, hashAt);
      var hash = hashAt === -1 ? '' : href.slice(hashAt);
      if (path.indexOf('?') !== -1) return;
      a.setAttribute('href', path + '?' + utmString + hash);
    });
  }

  var source = document.getElementById('ea-source');
  if (source && utmString) {
    source.value = keys.map(function (k) { return utm.get(k) || '-'; }).join(' / ');
  }

  // 3. Local-preview stub for the early-access form. Never runs on the live site.
  var host = window.location.hostname;
  var isLocal = window.location.protocol === 'file:' || host === 'localhost' || host === '127.0.0.1' || host === '::1' || host === '[::1]';
  var form = document.getElementById('early-access-form');
  if (isLocal && form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var email = document.getElementById('ea-email');
      var status = document.getElementById('form-status');
      if (!email || !email.value.trim()) {
        if (status) status.textContent = 'Please enter your email.';
        if (email) email.focus();
        return;
      }
      if (!form.reportValidity()) return;
      if (status) status.textContent = 'Local preview: nothing was sent.';
      window.location.href = 'thanks.html';
    });
  }

  // 4. Interactive elements (2026-10-12). Pure DOM changes in this page: no storage, no timers, no network.
  document.documentElement.classList.add('js');

  // 4a. "Your day" illustration: places sample activities relative to the chosen wake and sleep times.
  var wake = document.getElementById('wake');
  var sleep = document.getElementById('sleep');
  var list = document.getElementById('day-list');
  function mins(t) { var p = t.split(':'); return (+p[0]) * 60 + (+p[1]); }
  function fmt(m) { m = ((m % 1440) + 1440) % 1440; var h = Math.floor(m / 60), n = m % 60; return (h < 10 ? '0' : '') + h + ':' + (n < 10 ? '0' : '') + n; }
  function row(time, name, chip, chipClass) {
    var li = document.createElement('li'); li.className = 'event';
    var t = document.createElement('time'); t.textContent = time; li.appendChild(t);
    var s = document.createElement('strong'); s.textContent = name; li.appendChild(s);
    if (chip) { var c = document.createElement('span'); c.className = 'chip ' + chipClass; c.textContent = chip; li.appendChild(c); }
    return li;
  }
  function drawDay() {
    var w = mins(wake.value), s = mins(sleep.value);
    list.textContent = '';
    list.appendChild(row(fmt(w), 'Wake', 'Wake alarm · saved', 'saved'));
    list.appendChild(row(fmt(w + 30) + ' · 45 min', 'Gym'));
    list.appendChild(row(fmt(w + 150) + ' – ' + fmt(w + 330), 'Protection window', 'Needs device access', 'needs'));
    list.appendChild(row(fmt(s - 90) + ' · 30 min', 'Read a book'));
    list.appendChild(row(fmt(s), 'Make room for sleep'));
  }
  if (wake && sleep && list) { wake.addEventListener('change', drawDay); sleep.addEventListener('change', drawDay); drawDay(); }

  // 4b. Stillness moment: text only. Begin and End just switch which sentence shows. Nothing counts or is kept.
  var box = document.getElementById('still-box');
  var begin = document.getElementById('still-begin');
  var end = document.getElementById('still-end');
  var controls = document.getElementById('still-controls');
  if (box && begin && end && controls) {
    controls.hidden = false;
    box.setAttribute('data-state', 'intro');
    begin.addEventListener('click', function () { box.setAttribute('data-state', 'on'); begin.hidden = true; end.hidden = false; end.focus(); });
    end.addEventListener('click', function () { box.setAttribute('data-state', 'end'); end.hidden = true; begin.hidden = false; begin.textContent = 'Again'; begin.focus(); });
  }

  // 4c. Is / isn't toggles: answers start hidden with JS on, visible with JS off.
  Array.prototype.forEach.call(document.querySelectorAll('.tog'), function (b) {
    var p = document.getElementById(b.getAttribute('aria-controls'));
    if (!p) return;
    b.setAttribute('aria-expanded', 'false'); p.hidden = true;
    b.addEventListener('click', function () { var open = b.getAttribute('aria-expanded') === 'true'; b.setAttribute('aria-expanded', String(!open)); p.hidden = open; });
  });

  // 4d. Quiet scroll reveal of narrative beats. Off for reduced motion and when IntersectionObserver is missing.
  var beats = document.querySelectorAll('.reveal');
  var still = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (still || !('IntersectionObserver' in window)) {
    Array.prototype.forEach.call(beats, function (b) { b.classList.add('seen'); });
  } else {
    var io = new IntersectionObserver(function (es) { es.forEach(function (e) { if (e.isIntersecting) { e.target.classList.add('seen'); io.unobserve(e.target); } }); }, { threshold: 0.25 });
    Array.prototype.forEach.call(beats, function (b) { io.observe(b); });
  }

})();
