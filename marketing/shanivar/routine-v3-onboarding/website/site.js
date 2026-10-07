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

})();
