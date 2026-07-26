package webtemplate.shared.db

import java.security.SecureRandom
import java.sql.{Connection, SQLException}
import java.util.Base64

final class SqliteAuthDao private (conn: Connection) extends AuthDao {
  private val random = new SecureRandom()

  def createUserWithPassword(email: String, passwordHash: String, isAdmin: Boolean = false): Either[String, UserRecord] = {
    val normalized = email.trim.toLowerCase
    val createdAt = System.currentTimeMillis()
    val stmt = conn.prepareStatement(
      "INSERT INTO users (email, password_hash, created_at, is_admin) VALUES (?, ?, ?, ?)"
    )
    try {
      stmt.setString(1, normalized)
      stmt.setString(2, passwordHash)
      stmt.setLong(3, createdAt)
      stmt.setBoolean(4, isAdmin)
      stmt.executeUpdate()
      val rs = stmt.getGeneratedKeys
      try {
        rs.next()
        Right(UserRecord(rs.getLong(1), normalized, Some(passwordHash), createdAt, isAdmin))
      } finally rs.close()
    } catch {
      case e: SQLException if e.getMessage != null && e.getMessage.toUpperCase.contains("UNIQUE") =>
        Left("email already registered")
    } finally stmt.close()
  }

  def findByEmail(email: String): Option[UserRecord] = {
    val normalized = email.trim.toLowerCase
    val stmt = conn.prepareStatement(
      "SELECT id, email, password_hash, created_at, is_admin FROM users WHERE email = ?"
    )
    try {
      stmt.setString(1, normalized)
      val rs = stmt.executeQuery()
      try {
        if (rs.next()) {
          Some(
            UserRecord(
              rs.getLong("id"),
              rs.getString("email"),
              Option(rs.getString("password_hash")),
              rs.getLong("created_at"),
              rs.getBoolean("is_admin")
            )
          )
        } else None
      } finally rs.close()
    } finally stmt.close()
  }

  def findById(userId: Long): Option[UserRecord] = {
    val stmt = conn.prepareStatement(
      "SELECT id, email, password_hash, created_at, is_admin FROM users WHERE id = ?"
    )
    try {
      stmt.setLong(1, userId)
      val rs = stmt.executeQuery()
      try {
        if (rs.next()) {
          Some(
            UserRecord(
              rs.getLong("id"),
              rs.getString("email"),
              Option(rs.getString("password_hash")),
              rs.getLong("created_at"),
              rs.getBoolean("is_admin")
            )
          )
        } else None
      } finally rs.close()
    } finally stmt.close()
  }

  def listUsers(): List[UserRecord] = {
    val stmt = conn.prepareStatement(
      "SELECT id, email, password_hash, created_at, is_admin FROM users ORDER BY created_at DESC, id DESC"
    )
    try {
      val rs = stmt.executeQuery()
      try {
        val buf = List.newBuilder[UserRecord]
        while (rs.next())
          buf += UserRecord(
            rs.getLong("id"),
            rs.getString("email"),
            Option(rs.getString("password_hash")),
            rs.getLong("created_at"),
            rs.getBoolean("is_admin")
          )
        buf.result()
      } finally rs.close()
    } finally stmt.close()
  }

  def updateUser(
      id: Long,
      email: Option[String],
      passwordHash: Option[String],
      isAdmin: Option[Boolean]
  ): Either[String, UserRecord] =
    findById(id) match {
      case None => Left("user not found")
      case Some(existing) =>
        val newEmail = email.map(_.trim.toLowerCase).getOrElse(existing.email)
        val newHash = passwordHash.orElse(existing.passwordHash)
        val newIsAdmin = isAdmin.getOrElse(existing.isAdmin)
        val stmt = conn.prepareStatement("UPDATE users SET email = ?, password_hash = ?, is_admin = ? WHERE id = ?")
        try {
          stmt.setString(1, newEmail)
          newHash match {
            case Some(h) => stmt.setString(2, h)
            case None    => stmt.setNull(2, java.sql.Types.VARCHAR)
          }
          stmt.setBoolean(3, newIsAdmin)
          stmt.setLong(4, id)
          stmt.executeUpdate()
          Right(existing.copy(email = newEmail, passwordHash = newHash, isAdmin = newIsAdmin))
        } catch {
          case e: SQLException if e.getMessage != null && e.getMessage.toUpperCase.contains("UNIQUE") =>
            Left("email already registered")
        } finally stmt.close()
    }

  def deleteUser(id: Long): Boolean = {
    val delSessions = conn.prepareStatement("DELETE FROM sessions WHERE user_id = ?")
    try {
      delSessions.setLong(1, id)
      delSessions.executeUpdate()
    } finally delSessions.close()

    val delUser = conn.prepareStatement("DELETE FROM users WHERE id = ?")
    try {
      delUser.setLong(1, id)
      delUser.executeUpdate() > 0
    } finally delUser.close()
  }

  def findOrCreateGoogleUser(email: String): UserRecord = {
    val normalized = email.trim.toLowerCase
    findByEmail(normalized).getOrElse {
      val createdAt = System.currentTimeMillis()
      val stmt = conn.prepareStatement(
        "INSERT INTO users (email, password_hash, created_at) VALUES (?, NULL, ?)"
      )
      try {
        stmt.setString(1, normalized)
        stmt.setLong(2, createdAt)
        stmt.executeUpdate()
      } catch {
        case e: SQLException if e.getMessage != null && e.getMessage.toUpperCase.contains("UNIQUE") =>
          () // Lost a race against a concurrent signup/google-login for the same email.
      } finally stmt.close()
      // Re-fetch rather than trust getGeneratedKeys, since the insert may have lost the race above.
      findByEmail(normalized).get
    }
  }

  def createSession(userId: Long, ttlMillis: Long): SessionRecord = {
    val cleanupStmt = conn.prepareStatement("DELETE FROM sessions WHERE expires_at < ?")
    try {
      cleanupStmt.setLong(1, System.currentTimeMillis())
      cleanupStmt.executeUpdate()
    } finally cleanupStmt.close()

    val tokenBytes = new Array[Byte](32)
    random.nextBytes(tokenBytes)
    val sessionId = Base64.getUrlEncoder.withoutPadding.encodeToString(tokenBytes)
    val expiresAt = System.currentTimeMillis() + ttlMillis

    val stmt = conn.prepareStatement(
      "INSERT INTO sessions (id, user_id, expires_at, created_at) VALUES (?, ?, ?, ?)"
    )
    try {
      stmt.setString(1, sessionId)
      stmt.setLong(2, userId)
      stmt.setLong(3, expiresAt)
      stmt.setLong(4, System.currentTimeMillis())
      stmt.executeUpdate()
    } finally stmt.close()

    SessionRecord(sessionId, userId, expiresAt)
  }

  def findValidSession(sessionId: String): Option[SessionRecord] = {
    val stmt = conn.prepareStatement(
      "SELECT id, user_id, expires_at FROM sessions WHERE id = ? AND expires_at > ?"
    )
    try {
      stmt.setString(1, sessionId)
      stmt.setLong(2, System.currentTimeMillis())
      val rs = stmt.executeQuery()
      try {
        if (rs.next()) Some(SessionRecord(rs.getString("id"), rs.getLong("user_id"), rs.getLong("expires_at")))
        else None
      } finally rs.close()
    } finally stmt.close()
  }

  def deleteSession(sessionId: String): Unit = {
    val stmt = conn.prepareStatement("DELETE FROM sessions WHERE id = ?")
    try {
      stmt.setString(1, sessionId)
      stmt.executeUpdate()
    } finally stmt.close()
  }

  def close(): Unit = conn.close()
}

object SqliteAuthDao {
  def apply(dbPath: String): SqliteAuthDao = {
    val conn = SqliteConnection.open(dbPath)

    val pragmaStmt = conn.createStatement()
    try {
      pragmaStmt.execute(
        """CREATE TABLE IF NOT EXISTS users (
          |  id INTEGER PRIMARY KEY AUTOINCREMENT,
          |  email TEXT NOT NULL UNIQUE COLLATE NOCASE,
          |  password_hash TEXT NULL,
          |  created_at INTEGER NOT NULL,
          |  is_admin INTEGER NOT NULL DEFAULT 0
          |)""".stripMargin
      )
      pragmaStmt.execute(
        """CREATE TABLE IF NOT EXISTS sessions (
          |  id TEXT PRIMARY KEY,
          |  user_id INTEGER NOT NULL REFERENCES users(id),
          |  expires_at INTEGER NOT NULL,
          |  created_at INTEGER NOT NULL
          |)""".stripMargin
      )
      pragmaStmt.execute(
        "CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id)"
      )
    } finally pragmaStmt.close()

    new SqliteAuthDao(conn)
  }
}
