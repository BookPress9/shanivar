const {
  computeNatalChart,
  computeCurrentTransits,
  summarizeTransitsForPrompt,
  degreeWithinSign,
  computeMoonPhase
} = require('../src/utils/astrology');

describe('astrology utility — real ephemeris calculation', () => {

  describe('computeNatalChart', () => {
    // Reference case verified directly against the underlying library and cross-checked
    // against publicly documented astrological data: Albert Einstein, born 14 Mar 1879,
    // 11:30 local time, Ulm, Germany (48.4011 N, 9.9876 E). Widely documented as Sun in
    // Pisces / Moon in Sagittarius.
    it('matches a well-documented historical reference chart (Einstein)', () => {
      const chart = computeNatalChart({
        birthDate: '1879-03-14',
        birthTime: '11:30',
        utcOffset: '+00:59', // Ulm local mean time-ish; historical zone predates modern CET
        lat: 48.4011,
        lon: 9.9876
      });

      expect(chart.sun_sign).toBe('Pisces');
      expect(chart.moon_sign).toBe('Sagittarius');
    });

    it('a Jul 4 birth always has the Sun in Cancer, regardless of year/location', () => {
      const chart = computeNatalChart({
        birthDate: '2000-07-04', birthTime: '12:00', utcOffset: '+00:00', lat: 40.7128, lon: -74.0060
      });
      expect(chart.sun_sign).toBe('Cancer');
    });

    it('honors an explicit UTC offset over a geo-guessed timezone', () => {
      // Same local wall-clock time and location, two different explicit offsets ->
      // two different real UTC instants -> the Moon (which moves ~13deg/day, fast
      // enough to shift signs within hours) should differ or at minimum the raw
      // degree must differ, proving the offset is actually being applied.
      const a = computeNatalChart({
        birthDate: '1990-08-15', birthTime: '14:30', utcOffset: '+02:00', lat: 48.8566, lon: 2.3522
      });
      const b = computeNatalChart({
        birthDate: '1990-08-15', birthTime: '14:30', utcOffset: '-02:00', lat: 48.8566, lon: 2.3522
      });
      const moonDegA = a.planets.find(p => p.name === 'Moon').degree;
      const moonDegB = b.planets.find(p => p.name === 'Moon').degree;
      expect(a.moon_sign !== b.moon_sign || moonDegA !== moonDegB).toBe(true);
    });

    it('returns a full, well-shaped chart', () => {
      const chart = computeNatalChart({
        birthDate: '1990/08/15', birthTime: '14:30', utcOffset: '+02:00', lat: 48.8566, lon: 2.3522
      });

      expect(chart).toEqual(expect.objectContaining({
        sun_sign: expect.any(String),
        moon_sign: expect.any(String),
        rising_sign: expect.any(String),
        moon_phase: expect.any(String)
      }));
      expect(chart.planets).toHaveLength(10);
      expect(chart.planets.map(p => p.name)).toEqual([
        'Sun', 'Moon', 'Mercury', 'Venus', 'Mars', 'Jupiter', 'Saturn', 'Uranus', 'Neptune', 'Pluto'
      ]);
      expect(chart.houses).toHaveLength(12);
      chart.houses.forEach((h, i) => expect(h.house).toBe(i + 1));
    });

    it('is deterministic — same input always produces the same output', () => {
      const input = { birthDate: '1985-12-25', birthTime: '08:15', utcOffset: '-05:00', lat: 34.0522, lon: -118.2437 };
      expect(computeNatalChart(input)).toEqual(computeNatalChart(input));
    });

    it('accepts both YYYY-MM-DD and YYYY/MM/DD date formats identically', () => {
      const dashed = computeNatalChart({ birthDate: '2000-01-01', birthTime: '12:00', utcOffset: '+00:00', lat: 0, lon: 0 });
      const slashed = computeNatalChart({ birthDate: '2000/01/01', birthTime: '12:00', utcOffset: '+00:00', lat: 0, lon: 0 });
      expect(dashed).toEqual(slashed);
    });
  });

  describe('computeCurrentTransits', () => {
    it('returns 10 real planet positions for "now"', () => {
      const transits = computeCurrentTransits();
      expect(transits.planets).toHaveLength(10);
      transits.planets.forEach(p => {
        expect(typeof p.sign).toBe('string');
        expect(typeof p.degree).toBe('number');
      });
      expect(transits.computedAt).toEqual(expect.any(String));
    });

    it('is stable for a fixed point in time', () => {
      const fixedDate = new Date('2000-01-01T12:00:00Z');
      const t1 = computeCurrentTransits(fixedDate);
      const t2 = computeCurrentTransits(fixedDate);
      expect(t1.planets).toEqual(t2.planets);
      // Known real ephemeris data for 1 Jan 2000: Sun ~280.4 deg ecliptic longitude,
      // i.e. Capricorn.
      expect(t1.planets.find(p => p.name === 'Sun').sign).toBe('Capricorn');
    });
  });

  describe('summarizeTransitsForPrompt', () => {
    it('renders a compact, factual line suitable for an LLM prompt', () => {
      const transits = computeCurrentTransits(new Date('2000-01-01T12:00:00Z'));
      const summary = summarizeTransitsForPrompt(transits);
      expect(summary).toContain('Sun in Capricorn');
      expect(summary.split(',')).toHaveLength(10);
    });
  });

  describe('degreeWithinSign', () => {
    it('normalizes a full ecliptic longitude down to 0-30 within its sign', () => {
      expect(degreeWithinSign(280.3682)).toBeCloseTo(10.37, 1);
      expect(degreeWithinSign(0)).toBe(0);
      expect(degreeWithinSign(29.999)).toBeCloseTo(30, 1);
      expect(degreeWithinSign(360)).toBe(0);
    });
  });

  describe('computeMoonPhase', () => {
    it('identifies New Moon when Sun and Moon share the same longitude', () => {
      expect(computeMoonPhase(100, 100)).toBe('New Moon');
    });
    it('identifies Full Moon when Moon is opposite the Sun', () => {
      expect(computeMoonPhase(100, 280)).toBe('Full Moon');
    });
    it('identifies First Quarter at a 90 degree separation', () => {
      expect(computeMoonPhase(0, 90)).toBe('First Quarter');
    });
  });
});
