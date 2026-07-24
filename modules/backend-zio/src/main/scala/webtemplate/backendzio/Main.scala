package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.config.AppConfig
import webtemplate.shared.db.{SqliteAuthDao, SqliteEntryDao}

object Main extends ZIOAppDefault {

  private val config = AppConfig.load()
  private val entryDao = SqliteEntryDao(config.sqlitePath)
  private val authDao = SqliteAuthDao(config.sqlitePath)
  private val authenticator = new SessionAuthenticator(authDao, config.auth)

  private val authHandlers = new AuthHandlers(authDao, config.auth, authenticator)
  private val entryHandlers = new EntryHandlers(entryDao, authenticator)

  private val routes: Routes[Any, Response] = AuthRoutes(authHandlers) ++ EntryRoutes(entryHandlers)

  override val run: ZIO[Any, Any, Any] =
    Server.serve(routes).provide(Server.defaultWith(_.binding(config.host, config.port)))
}
