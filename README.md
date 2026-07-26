# webtemplate

Full-stack Scala template: JVM backend (zio-http), a cross-compiled `shared`
module, and a Vite frontend (Scala.js) — backed by SQLite.

## Prerequisites

- Java 17+, sbt
- Node.js + npm

## First-time setup

```
npm install
npm install --prefix web
```

## Running everything

```
npm run dev
```

This first runs `predev` (`sbt --client 'root/compile'`), which boots sbt's shared background
server once so the three concurrent `sbt --client` watches below don't race each other trying
to start it. Then it brings up four watch processes together:

- `backend-zio` on `http://localhost:8080` (auto-restarts on change via sbt-revolver)
- the `frontend` Scala.js module (`~fastLinkJS`, recompiles on change)
- the Vite dev server (prints its own URL, typically `http://localhost:5173`) with HMR for
  JS/TS/CSS/HTML

Open the Vite URL and use the landing page to reach the demo. Vite's dev
proxy (`/api/zio/*` → :8080), so there's no CORS to configure in dev.

Data is stored in `data/web.sqlite` (gitignored, created on first run).

### Alternative: tmux, one pane per process

```
npm run dev:tmux
```

Runs the same four processes, but instead of interleaving prefixed output in one terminal
(what `concurrently` does above), it opens a new tmux window with three panes — zio-http,
the Scala.js watcher, and Vite each in their own titled pane. If you're already inside tmux it
adds the window to your current session; otherwise it creates/reuses a `webtemplate` session
and attaches to it. Close the window (or `Ctrl+b` `&`) to stop everything.

## Notes

- The `@scala-js/vite-plugin-scalajs` plugin does not invoke sbt itself — keep
  `sbt "~frontend/fastLinkJS"` running (it's part of `npm run dev` already).
- The dev proxy setup is dev/preview-only. A production deployment serving the built static
  site from a different origin than the JVM backends would need real CORS or a reverse proxy.
- Authentication is intentionally not included yet — routes are structured so it's easy to
  wrap with middleware later.
- If `npm run dev` was killed forcefully (e.g. `kill -9`, a crashed terminal) rather than via
  Ctrl+C, sbt's background server socket under `~/.sbt/1.0/server/<hash>/sock` can be left
  behind stale, causing the next `npm run dev` to fail with "Connection refused" errors from
  all three `sbt --client` processes. Fix: delete that project's `sock` file (find it by
  matching the hash sbt printed on last successful start, e.g.
  `rm ~/.sbt/1.0/server/<hash>/sock`) and run `npm run dev` again.
