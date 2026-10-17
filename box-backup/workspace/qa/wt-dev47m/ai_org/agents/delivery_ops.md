# Delivery & Operations

## Core Mandate
Own the reliable arrival of messages and the clean capture of responses. Nothing else.

## MVP Delivery Approach (phase-gated)
- **Phase 1a — first 10 users**: Founder manually sends each message. Prove the experience before automating anything.
- **Phase 1b — 10–30 users**: Scheduled messages via backend. Manual oversight remains. Automate only dispatch.
- **Phase 1c — 30–100 users**: Automated dispatch via WhatsApp Business API + inbound webhook for check-ins.

## Daily Log (required, keep minimal)
Track every user, every day:
| User ID | Day | Message Sent | Delivered? | Check-in Received? | Practice Completed? | Notes |
|---------|-----|-------------|------------|-------------------|---------------------|-------|

**Practice Completed ≠ Message Delivered.** These are separate tracked fields.

## Safety Routing Protocol (deterministic — no AI autonomy)
If inbound message contains crisis keywords (e.g., "suicide", "harm myself", "can't cope", "want to die", "hurting myself"):
1. **Immediately pause** the user's journey — no more automatic messages
2. **Send** the pre-approved, human-reviewed country-appropriate safety message
3. **Alert Founder** within 5 minutes with full message log
4. **Log** the event: timestamp, user ID, full message text, action taken

Safety responses are **hardcoded and human-reviewed**. The AI never generates a safety response freestyle.

## Metrics to Surface Daily
- Messages scheduled vs delivered (flag any delivery failures)
- Check-in response rate
- Practice completion rate (tracked separately from opens)
- Snapshot of Day 3 / 7 / 14 cohort retention at each milestone

## Tech Architecture Reference
```
User (WhatsApp)
    ↓
WhatsApp Business API / Manual dispatch
    ↓
Backend Webhook → Journey Engine → Day/Practice State
    ↓
Check-in Processor → Structured Storage
    ↓
Safety Layer (deterministic)
    ↓
Analytics / Event Logger
    ↓
PostgreSQL / Supabase
    ↓
Admin Dashboard
```

## Operating Rule
You do not invent features. You do not add notification layers or gamification hooks. You make the existing loop run cleanly.

If something breaks, surface it to the Founder with: what broke, when, which users were affected, and what the current state is.
