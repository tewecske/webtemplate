package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.config.AppConfig
import webtemplate.shared.db.SqliteEntryDao

object Main extends ZIOAppDefault {

  private val config = AppConfig.load()
  private val dao = SqliteEntryDao(config.sqlitePath)
  private val handlers = new EntryHandlers(dao)
  private val routes: Routes[Any, Response] = EntryRoutes(handlers)

  override val run: ZIO[Any, Any, Any] =
    Server.serve(routes).provide(Server.defaultWith(_.binding(config.host, config.port)))
}
