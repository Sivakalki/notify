# Notify — Frontend

React dashboard for managing notification campaigns, cohorts, bulk uploads, and monitoring delivery analytics.

See the [root README](../README.md) for the full architecture and project overview.

---

## Prerequisites

| Tool | Version |
|---|---|
| Node.js | 20+ |
| npm | 9+ |

---

## Setup

```bash
npm install
```

---

## Running

```bash
npm run dev
```

Opens at **http://localhost:5173** with hot module replacement.

The backend API must be running at `http://localhost:8080` before using the dashboard. See the [backend README](../notify-backend/README.md) for setup.

---

## Environment Variables

Create a `.env.local` file in this directory to override defaults:

```env
VITE_API_BASE_URL=http://localhost:8080
```

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend API base URL |

---

## Build

```bash
npm run build
```

Output goes to `dist/`. Serve it with any static file server or the Vite preview server:

```bash
npm run preview
```

---

## Project Structure

```
src/
├── components/       Shared UI components (shadcn/ui based)
├── routes/           TanStack Router pages
│   ├── dashboard.tsx     Overview metrics
│   ├── campaigns.tsx     Campaign list and creation
│   ├── cohorts.tsx       Cohort management
│   ├── bulk-upload.tsx   CSV/Excel/JSON file upload
│   ├── send.tsx          Single notification send
│   ├── dlq.tsx           Dead-letter queue console
│   ├── api-keys.tsx      API key management
│   └── settings.tsx      App settings
├── hooks/            Custom React hooks (data fetching)
├── lib/              Utility functions
└── main.tsx          App entry point
```

---

## Tech Stack

- **React 19** with TypeScript
- **Vite** — build tool and dev server
- **TailwindCSS v4** — utility-first styling
- **shadcn/ui** — accessible component primitives
- **TanStack Router** — type-safe file-based routing
- **TanStack Query** — server state management and caching
- **Lucide React** — icons
