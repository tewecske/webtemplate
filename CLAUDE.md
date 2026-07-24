# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A full-stack Scala template: two interchangeable JVM backends (Cask and zio-http), a
cross-compiled `shared` module (JVM + Scala.js), and a Vite frontend with three parallel
implementations of the same demo feature (vanilla JS, TypeScript, Scala.js) — all backed by
SQLite. It exists to prove the whole toolchain works end-to-end and to be a quick-start base
for new projects. No authentication yet (deliberately deferred).

## Commands

```
npm install && npm install --prefix web   # first-time setup
npm run dev                                # everything: both backends + scalajs watch + vite
npm run dev:tmux                           # same, but one tmux pane per process
sbt compile                                # compile all Scala modules
sbt "~frontend/fastLinkJS"                 # scala.js watch only
web/: npm run typecheck                    # tsc --noEmit for the TS variant
```

`npm run dev` runs a `predev` step (`sbt --client 'root/compile'`) first to boot sbt's shared
background server once — this avoids a real race where three concurrent `sbt --client`
processes try to boot that server simultaneously and some fail. There are no tests yet.

## Architecture

```
modules/shared/{shared,jvm,js}   crossProject — DTOs+upickle codecs (shared/), SQLite DAO (jvm/)
modules/backend-cask             Cask server, :8080, data/cask.sqlite
modules/backend-zio              zio-http server, :8081, data/zio.sqlite
modules/frontend                 Scala.js, depends on shared.js
web/                              Vite + Tailwind v4 project (not an sbt module)
  vanilla/, ts/, scalajs/         three variants of the same page, one backend selector each
```

Both backends implement an **identical** REST contract (`GET /health`, `POST /entries`,
`GET /entries/{inputId}`) and parse/serialize JSON only through the shared upickle codecs in
`modules/shared/shared/...` — never framework auto-JSON — so the two backends are provably
interchangeable. Each backend writes to its own SQLite file to avoid cross-process write
contention; the DAO code itself lives once in `modules/shared/jvm/.../db/SqliteEntryDao.scala`
and is reused by both.

The Scala.js variant's JS output is wired into Vite via `@scala-js/vite-plugin-scalajs`
(`import 'scalajs:main.js'` in `web/scalajs/main.js`) — that plugin does not invoke sbt itself,
so `~frontend/fastLinkJS` must be running separately (it's part of `npm run dev`).

In dev, the frontend never talks to the backends directly — Vite proxies `/api/cask/*` → :8080
and `/api/zio/*` → :8081 (prefix stripped), so everything is same-origin and CORS never comes
up. This proxy setup is dev/preview-only; a real prod deployment across origins would need CORS.

`backendCask`/`backendZio`'s `reStart`/`run` tasks pin `baseDirectory` to the repo root
explicitly — without that, sbt-revolver's forked process working directory defaults to the
submodule's own directory, and the SQLite paths (`data/*.sqlite`, relative) end up in the
wrong place.

## Known gotchas (already solved once, don't re-derive)

- If `npm run dev` was killed forcefully rather than via Ctrl+C, sbt's server socket under
  `~/.sbt/1.0/server/<hash>/sock` can go stale and cause "Connection refused" on next run —
  delete that socket file and retry.
- Metals (VS Code or nvim-metals) auto-regenerates `project/project/metals.sbt`, which on an
  outdated Metals install can pin an `sbt-metals`/`semanticdb-scalac` version combo that
  doesn't exist for this Scala version, breaking `sbt compile` entirely until that file is
  deleted. Real fix is updating the Metals client; this file is already gitignored
  (`project/project/`) so it never gets committed.
