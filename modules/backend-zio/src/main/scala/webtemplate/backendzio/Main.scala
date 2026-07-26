package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.config.AppConfig
import webtemplate.shared.db.{SeedAdmin, SqliteAuthDao, SqliteEntryDao}
import webtemplate.shared.security.RateLimiter

object Main extends ZIOAppDefault {

  private val config = AppConfig.load()
  private val entryDao = SqliteEntryDao(config.sqlitePath)
  private val authDao = SqliteAuthDao(config.sqlitePath)
  if (config.auth.seedAdminEnabled) SeedAdmin.ensure(authDao, config.auth.seedAdminEmail, config.auth.seedAdminPassword)
  private val authenticator = new SessionAuthenticator(authDao, config.auth)
  private val authRateLimiter = new RateLimiter(maxAttempts = 5, windowMillis = 15 * 60 * 1000)

  private val authHandlers = new AuthHandlers(authDao, config.auth, authenticator)
  private val entryHandlers = new EntryHandlers(entryDao, authenticator)
  private val adminHandlers = new AdminHandlers(authDao, authenticator)

  private val routes: Routes[Any, Response] =
    (AuthRoutes(authHandlers, authRateLimiter) ++ EntryRoutes(entryHandlers) ++ AdminRoutes(adminHandlers)) @@
      new SecurityMiddleware(config.auth)

  override val run: ZIO[Any, Any, Any] =
    Server.serve(routes).provide(Server.defaultWith(_.binding(config.host, config.port)))
}
