const { filterXSS } = require('xss');

// Journal entries, community posts/replies etc. are plain-text content — strip all
// markup rather than allowlisting a rich-text subset, so storage never holds anything
// a client could later render unescaped and get bitten by.
function sanitizeText(value) {
  if (value === null || value === undefined) return value;
  return filterXSS(String(value), { whiteList: {}, stripIgnoreTag: true, stripIgnoreTagBody: ['script'] }).trim();
}

module.exports = { sanitizeText };
