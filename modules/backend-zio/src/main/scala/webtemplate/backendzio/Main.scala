package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.db.SqliteEntryDao

object Main extends ZIOAppDefault {

  private val dao = SqliteEntryDao("data/zio.sqlite")
  private val handlers = new EntryHandlers(dao)
  private val routes: Routes[Any, Response] = EntryRoutes(handlers)

  override val run: ZIO[Any, Any, Any] =
    Server.serve(routes).provide(Server.defaultWithPort(8081))
}
