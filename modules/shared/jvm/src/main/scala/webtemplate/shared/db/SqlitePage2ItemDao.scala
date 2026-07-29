package webtemplate.shared.db

import webtemplate.shared.Page2Item

import java.sql.Connection

final class SqlitePage2ItemDao private (conn: Connection) extends Page2ItemDao {

  def insert(userId: Long, name: String, nickname: String): Page2Item = {
    val createdAt = System.currentTimeMillis()
    val stmt = conn.prepareStatement(
      "INSERT INTO page2items (user_id, name, nickname, created_at) VALUES (?, ?, ?, ?)"
    )
    try {
      stmt.setLong(1, userId)
      stmt.setString(2, name)
      stmt.setString(3, nickname)
      stmt.setLong(4, createdAt)
      stmt.executeUpdate()
    } finally stmt.close()
    Page2Item(name, nickname, createdAt)
  }

  def items(userId: Long): List[Page2Item] = {
    val stmt = conn.prepareStatement(
      "SELECT name, nickname, created_at FROM page2items WHERE user_id = ? ORDER BY created_at DESC, id DESC"
    )
    try {
      stmt.setLong(1, userId)
      val rs = stmt.executeQuery()
      try {
        val buf = List.newBuilder[Page2Item]
        while (rs.next()) {
          buf += Page2Item(rs.getString("name"), rs.getString("nickname"), rs.getLong("created_at"))
        }
        buf.result()
      } finally rs.close()
    } finally stmt.close()
  }

  def close(): Unit = conn.close()
}

object SqlitePage2ItemDao {
  def apply(dbPath: String): SqlitePage2ItemDao = {
    val conn = SqliteConnection.open(dbPath)

    val pragmaStmt = conn.createStatement()
    try {
      pragmaStmt.execute(
        """CREATE TABLE IF NOT EXISTS page2items (
          |  id INTEGER PRIMARY KEY AUTOINCREMENT,
          |  user_id INTEGER NOT NULL,
          |  name TEXT NOT NULL,
          |  nickname TEXT NOT NULL,
          |  created_at INTEGER NOT NULL
          |)""".stripMargin
      )
      pragmaStmt.execute(
        "CREATE INDEX IF NOT EXISTS idx_page2items_user_created_at ON page2items(user_id, created_at DESC)"
      )
    } finally pragmaStmt.close()

    new SqlitePage2ItemDao(conn)
  }
}
