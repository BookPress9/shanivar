// Real astronomical/astrological calculation — natal charts and transit positions.
//
// Uses `circular-natal-horoscope-js`, a pure-JS library (no native bindings, no API
// key, no network call) built on a bundled copy of the Moshier analytical ephemeris
// (the same algorithm Swiss Ephemeris itself falls back to when its high-precision
// data files aren't loaded — accurate to a fraction of an arcminute for any date in
// the range this app will ever see). Verified against known reference charts before
// being wired in here (see squad report): e.g. Einstein (14 Mar 1879, 11:30, Ulm)
// resolves to Sun in Pisces / Moon in Sagittarius as documented; the real outer-planet
// positions for 1 Jan 2000 (Jupiter ~25° Aries, Saturn ~40° Taurus, Uranus ~314°
// Aquarius, etc.) match published ephemeris data.
//
// Previously, chart.js asked Gemini (a language model, not an ephemeris) to "calculate
// the exact astrological natal chart" from birth data, and insights.js told Gemini to
// "consider the astrological transits for today" without giving it any real transit
// data. Both are replaced by real calculation here; Gemini is now only used for the
// interpretive/narrative layer, fed with real computed positions as ground truth.
const { Origin, Horoscope } = require('circular-natal-horoscope-js');
const tzlookup = require('tz-lookup');
const moment = require('moment-timezone');

const PLANET_KEYS = ['sun', 'moon', 'mercury', 'venus', 'mars', 'jupiter', 'saturn', 'uranus', 'neptune', 'pluto'];

const MOON_PHASE_NAMES = [
  'New Moon', 'Waxing Crescent', 'First Quarter', 'Waxing Gibbous',
  'Full Moon', 'Waning Gibbous', 'Last Quarter', 'Waning Crescent'
];

function parseUtcOffsetMinutes(utcOffset) {
  if (!utcOffset) return 0;
  const m = /^([+-])(\d{1,2}):(\d{2})$/.exec(utcOffset);
  if (!m) return 0;
  const sign = m[1] === '-' ? -1 : 1;
  return sign * (parseInt(m[2], 10) * 60 + parseInt(m[3], 10));
}

/**
 * Builds a circular-natal-horoscope-js Origin that respects an EXPLICIT UTC offset.
 *
 * Origin's own constructor only takes local wall-clock fields + lat/lon; internally
 * it auto-derives an IANA timezone from lat/lon (via tz-lookup) and converts using
 * that zone's historical rules. That's usually fine, but it silently overrides
 * whatever UTC offset the caller actually asserts (which this API validates and
 * accepts as an explicit field). To honor the caller's stated offset instead of a
 * geo-guessed one, we compute the true UTC instant from birthDate+birthTime+utcOffset
 * ourselves, then find what local wall-clock time in Origin's auto-detected zone
 * reproduces that exact same UTC instant, and feed it THAT — so Origin's internal
 * math (Julian Date, local sidereal time, houses) is built on the UTC instant the
 * caller actually meant, not a silently-substituted one.
 */
function buildOrigin({ birthDate, birthTime, utcOffset, lat, lon }) {
  const [y, mo, d] = birthDate.split(/[-/]/).map(n => parseInt(n, 10));
  const [h, mi] = birthTime.split(':').map(n => parseInt(n, 10));
  const offsetMin = parseUtcOffsetMinutes(utcOffset);

  const trueUtcMs = Date.UTC(y, mo - 1, d, h, mi) - offsetMin * 60000;

  const tzName = tzlookup(lat, lon);
  const localInTz = moment.tz(trueUtcMs, tzName);

  return new Origin({
    year: localInTz.year(),
    month: localInTz.month(),
    date: localInTz.date(),
    hour: localInTz.hour(),
    minute: localInTz.minute(),
    latitude: lat,
    longitude: lon
  });
}

function buildHoroscope(origin, houseSystem) {
  return new Horoscope({
    origin,
    houseSystem,
    zodiac: 'tropical',
    aspectPoints: ['bodies'],
    aspectWithPoints: ['bodies'],
    aspectTypes: ['major'],
    language: 'en'
  });
}

// Degree of a body/cusp WITHIN its zodiac sign (0-30), rather than the full 0-360
// ecliptic longitude — matches the shape the rest of the app (and its DB columns)
// already expects for a planet/house "degree".
function degreeWithinSign(decimalDegrees) {
  const normalized = ((decimalDegrees % 30) + 30) % 30;
  return Math.round(normalized * 100) / 100;
}

// Real moon phase from the actual Sun/Moon ecliptic angular separation, instead of
// asking an LLM to guess one.
function computeMoonPhase(sunLongitude, moonLongitude) {
  const angle = ((moonLongitude - sunLongitude) % 360 + 360) % 360;
  const idx = Math.floor(((angle + 22.5) % 360) / 45);
  return MOON_PHASE_NAMES[idx];
}

function extractPlanets(horoscope) {
  return PLANET_KEYS.map(key => {
    const body = horoscope.CelestialBodies[key];
    return {
      name: body.label,
      sign: body.Sign.label,
      degree: degreeWithinSign(body.ChartPosition.Ecliptic.DecimalDegrees),
      house: body.House ? body.House.id : null
    };
  });
}

function extractHouses(horoscope) {
  return horoscope.Houses.map(h => ({
    house: h.id,
    sign: h.Sign.label,
    degree: degreeWithinSign(h.ChartPosition.StartPosition.Ecliptic.DecimalDegrees)
  }));
}

/**
 * Computes a full natal chart from real birth data. Replaces the previous
 * Gemini-hallucinated chart in chart.js.
 */
function computeNatalChart({ birthDate, birthTime, utcOffset, lat, lon }) {
  const origin = buildOrigin({ birthDate, birthTime, utcOffset, lat, lon });
  const horoscope = buildHoroscope(origin, 'placidus');

  const sunLongitude = horoscope.CelestialBodies.sun.ChartPosition.Ecliptic.DecimalDegrees;
  const moonLongitude = horoscope.CelestialBodies.moon.ChartPosition.Ecliptic.DecimalDegrees;

  return {
    sun_sign: horoscope.CelestialBodies.sun.Sign.label,
    moon_sign: horoscope.CelestialBodies.moon.Sign.label,
    rising_sign: horoscope.Ascendant.Sign.label,
    moon_phase: computeMoonPhase(sunLongitude, moonLongitude),
    planets: extractPlanets(horoscope),
    houses: extractHouses(horoscope)
  };
}

/**
 * Computes current planetary transit positions ("today's sky"), for use as real
 * ground truth fed into the interpretive Gemini prompt in insights.js — replacing
 * the previous vague "consider the astrological transits for today" instruction
 * that gave Gemini no actual data to work from.
 *
 * Geocentric ecliptic longitude of a planet doesn't depend on the observer's
 * location on Earth, so transit positions are computed at lat/lon 0,0 (Etc/GMT,
 * always UTC+0, no DST) directly from the current UTC instant — no natal location
 * needed for this.
 */
function computeCurrentTransits(asOfDate) {
  const now = asOfDate instanceof Date ? asOfDate : new Date();
  const origin = new Origin({
    year: now.getUTCFullYear(),
    month: now.getUTCMonth(),
    date: now.getUTCDate(),
    hour: now.getUTCHours(),
    minute: now.getUTCMinutes(),
    latitude: 0,
    longitude: 0
  });
  const horoscope = buildHoroscope(origin, 'whole-sign');

  return {
    computedAt: now.toISOString(),
    planets: extractPlanets(horoscope).map(({ name, sign, degree }) => ({ name, sign, degree }))
  };
}

// Renders computed transits as a compact, factual line for embedding directly into
// an LLM prompt as ground truth (e.g. "Sun in Virgo (17.2°), Moon in Pisces (3.8°), ...").
function summarizeTransitsForPrompt(transits) {
  return transits.planets.map(p => `${p.name} in ${p.sign} (${p.degree}°)`).join(', ');
}

module.exports = {
  computeNatalChart,
  computeCurrentTransits,
  summarizeTransitsForPrompt,
  degreeWithinSign,
  computeMoonPhase
};
