const db = require('../db');

class AnalyticsEngine {
  static async logEvent(userId, eventType, eventData = {}) {
    try {
      await db.query(
        'INSERT INTO analytics_events (user_id, event_type, event_data) VALUES ($1, $2, $3)',
        [userId || null, eventType, eventData]
      );
    } catch (err) {
      console.error('Analytics Error:', err);
    }
  }
}

module.exports = AnalyticsEngine;
