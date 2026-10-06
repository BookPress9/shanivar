/*
  The only script on this site. No network requests, no cookies, no storage, no third parties.

  1. Early-access form: no backend is connected yet (see README.md, open TODOs), so submit is
     stopped and a "preview only" message is shown.
     TODO(form-backend): replace this handler once the founder approves a backend.
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

  var form = document.getElementById('early-access-form');
  if (!form) return;
  var status = document.getElementById('form-status');
  form.addEventListener('submit', function (event) {
    event.preventDefault();
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    status.textContent = 'Preview only. This form is not connected yet, so nothing was sent or saved.';
  });
})();
