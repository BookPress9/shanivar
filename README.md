# Shanivar (The Digital Engine) & The Hybrid Monk (The Brand)

**Continuing work:** read [the current handoff](docs/HANDOFF_NEXT.md),
[daily GitHub checkpoint](https://github.com/BookPress9/shanivar/tree/codex/daily-status),
and [daily work / recovery protocol](docs/continuity/README.md). The checkpoint records branch state;
the handoff records owners, tests, pending work and the next action. The broad ecosystem overview below is historical.

Current Android pitch/usability work follows the [initial MVP scope](android-app/INITIAL_MVP_SCOPE_2026-09-13.md) and [Claude implementation task](android-app/CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md): family introductions and younger self-use, one goal/app, a short protected session, optional pause and review. The older ecosystem overview below describes the broader vision, not the current initial-launch promise. See the [staged backlog](docs/strategy/shanivar-launch-backlog.md) for later capabilities.

Welcome to the central monorepo powering the **Shanivar** SaaS ecosystem and the custom-fit technical bridge for **The Hybrid Monk** e-commerce brand.

## 🌌 The Dual Architecture

Our ecosystem is split into two distinct, synergized entities:

### 1. SHANIVAR (SaaS & Web App)
* **Domain:** `shanivar.com`
* **Identity:** A gamified consciousness evolution platform—a digital monastery meets modern productivity app.
* **Tech Stack:** Next.js 15 (Frontend), Express.js + JWT (Backend), PostgreSQL (Database).
* **Core Features:**
  - **User Dashboard:** XP, levels, dimension progress (Focus, Awareness, Discipline, Resilience, Creation).
  - **Journey Map:** 6 stages of consciousness with visual progression.
  - **Daily Practices:** Breathwork, focus training, reflection.
  - **The AI Observer:** Gemini-powered AI chat integration serving as your personal consciousness mirror.

### 2. THE HYBRID MONK (E-Commerce Brand)
* **Identity:** The physical manifestation of the modern sanyasi. An identity-based clothing brand specializing in premium, custom-fit linen garments (Short Kurtas, Drop-Crotch Pants, Observer Capes).
* **Tech Stack:** Shopify (Storefront) + n8n (Automation) + MCP Servers.
* **Core Operations:** Udyam-registered manufacturing unit handling zero-dead-stock tailoring and physical "Welcome Kits".

### 🌉 The Bridge (Integration)
While distinct, Shanivar's technology powers The Hybrid Monk's premium customer experience:
- **The Custom-Fit Portal:** Shanivar hosts a sleek, Framer Motion-powered route (`/fit`) for garment measurements.
- **The Data Loop:** Shopify purchases trigger an n8n webhook -> Pings Shanivar Backend -> Updates the user's "Tailoring Pipeline" tracker -> Awards Gamified XP (e.g., "Creation Dimension" boost).

---

## 🏗 Repository Structure

```text
Shanivar/
├── frontend/          # Next.js 15 + TailwindCSS + TypeScript (Shanivar App + /fit Portal)
├── backend/           # Express.js API + JWT Authentication
├── database/          # PostgreSQL schema + seed data
├── n8n/               # Webhook receivers & Shopify integration workflows
├── docker-compose.yml # Full-stack orchestration
└── README.md          # This file
```

## 🚀 Quick Start (Development)

### 1. Database Setup
```bash
docker-compose up db -d
# Run schema and seeds
psql -d shanivar -f database/schema.sql
psql -d shanivar -f database/seed.sql
```

### 2. Backend API
```bash
cd backend
cp .env.example .env    # Edit with your values
npm install
npm run dev             # Runs on http://localhost:5000
```

### 3. Frontend Web App
```bash
cd frontend
npm install
npm run dev             # Runs on http://localhost:3000
```

## 🔧 Environment Variables

| Variable | Description | Default |
|---|---|---|
| `PORT` | Backend port | `5000` |
| `JWT_SECRET` | JWT signing key | (change in production!) |
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://postgres:postgres@localhost:5432/shanivar` |
| `FRONTEND_URL` | Frontend URL for CORS | `http://localhost:3000` |
| `NEXT_PUBLIC_API_URL` | API URL for frontend | `http://localhost:5000/api` |
| `SHOPIFY_WEBHOOK_SECRET` | Secret for verifying n8n/Shopify calls | |

## 📄 License
MIT
