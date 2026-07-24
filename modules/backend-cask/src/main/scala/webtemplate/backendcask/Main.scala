package webtemplate.backendcask

import webtemplate.shared.db.SqliteEntryDao

object Main extends cask.Main {
  override def port: Int = 8080
  override def host: String = "0.0.0.0"

  private val dao = SqliteEntryDao("data/cask.sqlite")
  private val entryRoutes = new EntryRoutes(new EntryHandlers(dao))

  override def allRoutes: Seq[cask.Routes] = Seq(entryRoutes)
}
