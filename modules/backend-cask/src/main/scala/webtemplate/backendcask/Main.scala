package webtemplate.backendcask

import webtemplate.shared.config.AppConfig
import webtemplate.shared.db.SqliteEntryDao

object Main extends cask.Main {
  private val config = AppConfig.load()

  override def port: Int = config.port
  override def host: String = config.host

  private val dao = SqliteEntryDao(config.sqlitePath)
  private val entryRoutes = new EntryRoutes(new EntryHandlers(dao))

  override def allRoutes: Seq[cask.Routes] = Seq(entryRoutes)
}
