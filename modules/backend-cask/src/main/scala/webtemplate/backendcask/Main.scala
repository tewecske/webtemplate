package webtemplate.backendcask

import webtemplate.shared.config.AppConfig
import webtemplate.shared.db.{SeedAdmin, SqliteAuthDao, SqliteEntryDao}
import webtemplate.shared.security.RateLimiter

object Main extends cask.Main {
  private val config = AppConfig.load()

  override def port: Int = config.port
  override def host: String = config.host

  private val entryDao = SqliteEntryDao(config.sqlitePath)
  private val authDao = SqliteAuthDao(config.sqlitePath)
  if (config.auth.seedAdminEnabled) SeedAdmin.ensure(authDao, config.auth.seedAdminEmail, config.auth.seedAdminPassword)
  private val authenticator = new SessionAuthenticator(authDao, config.auth)
  private val authRateLimiter = new RateLimiter(maxAttempts = 5, windowMillis = 15 * 60 * 1000)

  private val authRoutes = new AuthRoutes(new AuthHandlers(authDao, config.auth, authenticator), authRateLimiter)
  private val entryRoutes = new EntryRoutes(new EntryHandlers(entryDao, authenticator))
  private val adminRoutes = new AdminRoutes(new AdminHandlers(authDao, authenticator))

  override def mainDecorators: Seq[cask.router.Decorator[?, ?, ?, ?]] = Seq(new SecurityDecorator(config.auth))

  override def allRoutes: Seq[cask.Routes] = Seq(authRoutes, entryRoutes, adminRoutes)
}
