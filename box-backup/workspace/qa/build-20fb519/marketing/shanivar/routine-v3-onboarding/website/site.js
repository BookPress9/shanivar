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

  // 4a. "Your day" diagram: moves example times with the chosen wake and sleep times.
  var wake = document.getElementById('wake');
  var sleep = document.getElementById('sleep');
  function mins(t) { var p = t.split(':'); return (+p[0]) * 60 + (+p[1]); }
  function fmt(m) { m = ((m % 1440) + 1440) % 1440; var h = Math.floor(m / 60), n = m % 60; return (h < 10 ? '0' : '') + h + ':' + (n < 10 ? '0' : '') + n; }
  function put(id, txt) { var el = document.getElementById(id); if (el) el.textContent = txt; }
  function drawDay() {
    var w = mins(wake.value), s = mins(sleep.value);
    put('t-wake', fmt(w)); put('t-gym', fmt(w + 30)); put('t-win', fmt(w + 150) + ' – ' + fmt(w + 330));
    put('t-read', fmt(s - 90)); put('t-sleep', fmt(s));
  }
  if (wake && sleep) { wake.addEventListener('change', drawDay); sleep.addEventListener('change', drawDay); drawDay(); }

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

})();
