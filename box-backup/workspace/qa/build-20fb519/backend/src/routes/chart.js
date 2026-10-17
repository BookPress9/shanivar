const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { computeNatalChart } = require('../utils/astrology');

function isValidDate(str) {
  return typeof str === 'string' && /^\d{4}[-/]\d{2}[-/]\d{2}$/.test(str);
}
function isValidTime(str) {
  return typeof str === 'string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(str);
}
function isValidUtcOffset(str) {
  return typeof str === 'string' && /^[+-]([01]?\d|2[0-3]):[0-5]\d$/.test(str);
}
function isValidLat(n) {
  return typeof n === 'number' && Number.isFinite(n) && n >= -90 && n <= 90;
}
function isValidLon(n) {
  return typeof n === 'number' && Number.isFinite(n) && n >= -180 && n <= 180;
}

router.post('/generate', authenticateToken, async (req, res) => {
  const { birthDate, birthTime, utcOffset, lat, lon } = req.body;
  const userId = req.user.id;

  // Validate input before it gets interpolated into the AI prompt or hits the DB.
  // Rejects garbage/prompt-injection-shaped input early instead of burning a Gemini call on it.
  if (!isValidDate(birthDate)) {
    return res.status(400).json({ error: 'birthDate must be in YYYY-MM-DD (or YYYY/MM/DD) format.' });
  }
  if (!isValidTime(birthTime)) {
    return res.status(400).json({ error: 'birthTime must be in 24-hour HH:MM format.' });
  }
  if (utcOffset !== undefined && utcOffset !== null && utcOffset !== '' && !isValidUtcOffset(utcOffset)) {
    return res.status(400).json({ error: 'utcOffset must look like +02:00 or -05:30.' });
  }
  if (!isValidLat(lat)) {
    return res.status(400).json({ error: 'lat must be a number between -90 and 90.' });
  }
  if (!isValidLon(lon)) {
    return res.status(400).json({ error: 'lon must be a number between -180 and 180.' });
  }

  try {
    // Real astronomical calculation (Moshier ephemeris via circular-natal-horoscope-js)
    // — deterministic, offline, no API key, no network call. This used to ask Gemini
    // (a language model, not an ephemeris) to "calculate the exact astrological natal
    // chart," which could hallucinate astronomically wrong positions. See
    // backend/src/utils/astrology.js for the calculation and how it was verified
    // against known reference charts.
    const chartData = computeNatalChart({ birthDate, birthTime, utcOffset, lat, lon });

    // Store inside the database transaction
    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      // Check if user already has a chart, if so delete it so we can regenerate
      await client.query('DELETE FROM user_chart WHERE user_id = $1', [userId]);

      const chartInsert = await client.query(`
        INSERT INTO user_chart (user_id, birth_date, birth_time, utc_offset, latitude, longitude, sun_sign, moon_sign, rising_sign, moon_phase)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        RETURNING id
      `, [userId, birthDate, birthTime, utcOffset, lat, lon, chartData.sun_sign, chartData.moon_sign, chartData.rising_sign, chartData.moon_phase]);

      const chartId = chartInsert.rows[0].id;

      // Insert planets
      for (const planet of chartData.planets) {
        await client.query(`
          INSERT INTO planet_positions (chart_id, planet_name, zodiac_sign, degree, house)
          VALUES ($1, $2, $3, $4, $5)
        `, [chartId, planet.name, planet.sign, planet.degree, planet.house]);
      }

      // Insert houses
      for (const house of chartData.houses) {
        await client.query(`
          INSERT INTO houses (chart_id, house_number, zodiac_sign, degree)
          VALUES ($1, $2, $3, $4)
        `, [chartId, house.house, house.sign, house.degree]);
      }

      await client.query('COMMIT');

      res.status(201).json({
        message: "Chart generated and saved successfully",
        chart: chartData
      });

    } catch (dbError) {
      await client.query('ROLLBACK');
      throw dbError;
    } finally {
      client.release();
    }

  } catch (error) {
    console.error('Error generating chart:', error);
    res.status(500).json({ error: 'Failed to generate natal chart' });
  }
});

router.get('/my-chart', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const chartRes = await pool.query('SELECT * FROM user_chart WHERE user_id = $1', [userId]);
    
    if (chartRes.rows.length === 0) {
      return res.status(404).json({ error: 'Chart not found' });
    }

    const chart = chartRes.rows[0];
    const planetsRes = await pool.query('SELECT * FROM planet_positions WHERE chart_id = $1', [chart.id]);
    const housesRes = await pool.query('SELECT * FROM houses WHERE chart_id = $1', [chart.id]);

    res.json({
      metadata: chart,
      planets: planetsRes.rows,
      houses: housesRes.rows
    });
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch chart' });
  }
});

module.exports = router;
