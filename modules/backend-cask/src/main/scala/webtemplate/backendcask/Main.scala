package webtemplate.backendcask

import webtemplate.shared.config.AppConfig
import webtemplate.shared.db.{SqliteAuthDao, SqliteEntryDao}

object Main extends cask.Main {
  private val config = AppConfig.load()

  override def port: Int = config.port
  override def host: String = config.host

  private val entryDao = SqliteEntryDao(config.sqlitePath)
  private val authDao = SqliteAuthDao(config.sqlitePath)
  private val authenticator = new SessionAuthenticator(authDao, config.auth)

  private val authRoutes = new AuthRoutes(new AuthHandlers(authDao, config.auth, authenticator))
  private val entryRoutes = new EntryRoutes(new EntryHandlers(entryDao, authenticator))

  override def allRoutes: Seq[cask.Routes] = Seq(authRoutes, entryRoutes)
}
