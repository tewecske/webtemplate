package webtemplate.shared.db

import java.nio.file.{Files, Paths}
import java.sql.{Connection, DriverManager}

object SqliteConnection {
  def open(dbPath: String): Connection = {
    Option(Paths.get(dbPath).getParent).foreach(Files.createDirectories(_))

    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection(s"jdbc:sqlite:$dbPath")

    val pragmaStmt = conn.createStatement()
    try {
      pragmaStmt.execute("PRAGMA journal_mode=WAL;")
      pragmaStmt.execute("PRAGMA busy_timeout=5000;")
    } finally pragmaStmt.close()

    conn
  }
}
