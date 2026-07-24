package webtemplate.shared.db

final case class UserRecord(id: Long, email: String, passwordHash: Option[String], createdAt: Long)
final case class SessionRecord(id: String, userId: Long, expiresAt: Long)

trait AuthDao {
  def createUserWithPassword(email: String, passwordHash: String): Either[String, UserRecord]
  def findByEmail(email: String): Option[UserRecord]
  def findById(userId: Long): Option[UserRecord]
  def findOrCreateGoogleUser(email: String): UserRecord
  def createSession(userId: Long, ttlMillis: Long): SessionRecord
  def findValidSession(sessionId: String): Option[SessionRecord]
  def deleteSession(sessionId: String): Unit
  def close(): Unit

  /** Resolves the raw session-cookie value straight to the user it belongs to, or None if
    * the cookie is missing, unknown, or expired.
    */
  def currentUser(sessionId: String): Option[UserRecord] =
    findValidSession(sessionId).flatMap(s => findById(s.userId))
}
