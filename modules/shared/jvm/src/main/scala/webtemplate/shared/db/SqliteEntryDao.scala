package webtemplate.shared.db

import webtemplate.shared.Entry

import java.nio.file.{Files, Paths}
import java.sql.{Connection, DriverManager}

final class SqliteEntryDao private (conn: Connection) extends EntryDao {

  def insert(inputId: String, value: String): Entry = {
    val createdAt = System.currentTimeMillis()
    val stmt = conn.prepareStatement(
      "INSERT INTO entries (input_id, value, created_at) VALUES (?, ?, ?)"
    )
    try {
      stmt.setString(1, inputId)
      stmt.setString(2, value)
      stmt.setLong(3, createdAt)
      stmt.executeUpdate()
    } finally stmt.close()
    Entry(inputId, value, createdAt)
  }

  def history(inputId: String): List[Entry] = {
    val stmt = conn.prepareStatement(
      "SELECT input_id, value, created_at FROM entries WHERE input_id = ? ORDER BY created_at DESC, id DESC"
    )
    try {
      stmt.setString(1, inputId)
      val rs = stmt.executeQuery()
      try {
        val buf = List.newBuilder[Entry]
        while (rs.next()) {
          buf += Entry(rs.getString("input_id"), rs.getString("value"), rs.getLong("created_at"))
        }
        buf.result()
      } finally rs.close()
    } finally stmt.close()
  }

  def close(): Unit = conn.close()
}

object SqliteEntryDao {
  def apply(dbPath: String): SqliteEntryDao = {
    val path = Paths.get(dbPath)
    Option(path.getParent).foreach(Files.createDirectories(_))

    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection(s"jdbc:sqlite:$dbPath")

    val pragmaStmt = conn.createStatement()
    try {
      pragmaStmt.execute("PRAGMA journal_mode=WAL;")
      pragmaStmt.execute("PRAGMA busy_timeout=5000;")
      pragmaStmt.execute(
        """CREATE TABLE IF NOT EXISTS entries (
          |  id INTEGER PRIMARY KEY AUTOINCREMENT,
          |  input_id TEXT NOT NULL,
          |  value TEXT NOT NULL,
          |  created_at INTEGER NOT NULL
          |)""".stripMargin
      )
      pragmaStmt.execute(
        "CREATE INDEX IF NOT EXISTS idx_entries_input_id_created_at ON entries(input_id, created_at DESC)"
      )
    } finally pragmaStmt.close()

    new SqliteEntryDao(conn)
  }
}
