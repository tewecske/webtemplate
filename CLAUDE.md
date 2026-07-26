# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A full-stack Scala template JVM backend (zio-http), a
cross-compiled `shared` module (JVM + Scala.js), and a Vite frontend demo feature (Scala.js) —
all backed by SQLite. It exists to prove the whole toolchain works end-to-end and to be a
quick-start base for new projects. Auth is email/password (bcrypt) plus a dev-only Google stub —
see "Authentication" below.

## Commands

```
npm install && npm install --prefix web   # first-time setup
npm run dev                                # everything: backend + scalajs watch + vite
npm run dev:tmux                           # same, but one tmux pane per process
sbt compile                                # compile all Scala modules
sbt "~frontend/fastLinkJS"                 # scala.js watch only
web/: npm run typecheck                    # tsc --noEmit
```

`npm run dev` runs a `predev` step (`sbt --client 'root/compile'`) first to boot sbt's shared
background server once — this avoids a real race where two concurrent `sbt --client`
processes try to boot that server simultaneously and some fail. There are no tests yet.

## Architecture

```
modules/shared/{shared,jvm,js}   crossProject — DTOs+upickle codecs (shared/), SQLite DAO (jvm/)
modules/backend-zio              zio-http server, :8080, data/zio.sqlite
modules/frontend                 Scala.js, depends on shared.js
web/                              Vite + Tailwind v4 project (not an sbt module)
  scalajs/                       frontend
```

REST contract (`GET /health`, `POST /entries`,
`GET /entries/{inputId}`) and parse/serialize JSON only through the shared upickle codecs in
`modules/shared/shared/...` — never framework auto-JSON. SQLite file;
the DAO code itself lives in `modules/shared/jvm/.../db/SqliteEntryDao.scala`.

The Scala.js's JS output is wired into Vite via `@scala-js/vite-plugin-scalajs`
(`import 'scalajs:main.js'` in `web/scalajs/main.js`) — that plugin does not invoke sbt itself,
so `~frontend/fastLinkJS` must be running separately (it's part of `npm run dev`).

In dev, the frontend never talks to the backend directly — Vite proxies `/api/zio/*` → :8080
(prefix stripped), so everything is same-origin and CORS never comes up.
This proxy setup is dev/preview-only; a real prod deployment across origins would need CORS.

`backendZio`'s `reStart`/`run` tasks pin `baseDirectory` to the repo root
explicitly — without that, sbt-revolver's forked process working directory defaults to the
submodule's own directory, and the SQLite paths (`data/*.sqlite`, relative) end up in the
wrong place.

### Config (host/port/sqlite path)

Backend loads `webtemplate.shared.config.AppConfig(host, port, sqlitePath, auth)` case
class (`modules/shared/jvm/.../config/AppConfig.scala`, `auth` is a nested `AuthConfig`) via
PureConfig, reading HOCON under the `app` namespace. `AppConfig.load()` just calls
`ConfigSource.default.at("app").loadOrThrow[AppConfig]` — environment selection is Typesafe
Config's own built-in mechanism, not custom code.

Each backend module has its own `src/main/resources/application*.conf` (field names are
kebab-case in HOCON, e.g. `sqlite-path` → `sqlitePath`):
- `application.conf` — dev, loaded by default with zero flags (matches `npm run dev`'s ports).
- `application-test.conf` — separate port/sqlite file so a test run never touches dev data.
- `application-prod.conf` — illustrative values with `${?APP_HOST}` / `${?APP_PORT}` /
  `${?APP_SQLITE_PATH}` env-var overrides for deploy-time configuration.

Select a non-default file with the JVM system property Typesafe Config already understands:
`-Dconfig.resource=application-test.conf` (e.g. `java -Dconfig.resource=... -cp ... Main`, or
`set backendZio/javaOptions += "-Dconfig.resource=..."` before `sbt run` since `reStart`/`run`
aren't forked by default and won't otherwise see JVM system properties set on the sbt command
line unless sbt itself is launched with that `-D` flag).

### Authentication

Sessions are an opaque random token in an HttpOnly cookie, checked against a `sessions` table
in SQLite (`modules/shared/jvm/.../db/SqliteAuthDao.scala`) — not JWT, so a session can be
revoked server-side just by deleting the row. Passwords are hashed with bcrypt (`jbcrypt`) via
`webtemplate.shared.auth.PasswordHasher`. Entries are scoped by `user_id`, so each account only
sees its own notes.

Backend expose the same auth routes: `POST /auth/signup`, `POST /auth/login`,
`POST /auth/logout`, `GET /auth/me`, `POST /auth/google-dev`. `/entries*` routes return 401
without a valid session cookie.

The session cookie name is per-backend (`web_session`, set via
`AuthConfig.sessionCookieName` in each `application*.conf`) — deliberately.
When adding a Set-Cookie always set an explicit `Path=/`: zio-http's `Cookie.Response`
defaults to no Path attribute, which RFC 6265 then scopes to the *directory* of the request URI
(e.g. `/api/zio/auth`) rather than the whole app — the cookie silently never reaches `/entries`.

`/auth/google-dev` is a **dev-only stub**, not real Google OAuth: it trusts whatever email is
POSTed and creates/logs in that user. It's gated by `AuthConfig.googleDevLoginEnabled`, which is
`true` in `application.conf`/`application-test.conf` and `false` in `application-prod.conf`. Real
Google Sign-In would need a Google Cloud OAuth client ID and server-side ID-token verification
(e.g. `google-api-client`'s `GoogleIdTokenVerifier`) — deliberately not wired up.

## Known gotchas (already solved once, don't re-derive)

- If `npm run dev` was killed forcefully rather than via Ctrl+C, sbt's server socket under
  `~/.sbt/1.0/server/<hash>/sock` can go stale and cause "Connection refused" on next run —
  delete that socket file and retry.
- Metals (VS Code or nvim-metals) auto-regenerates `project/project/metals.sbt`, which on an
  outdated Metals install can pin an `sbt-metals`/`semanticdb-scalac` version combo that
  doesn't exist for this Scala version, breaking `sbt compile` entirely until that file is
  deleted. Real fix is updating the Metals client; this file is already gitignored
  (`project/project/`) so it never gets committed.
