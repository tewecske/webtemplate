package webtemplate.shared.db

final case class UserRecord(id: Long, email: String, passwordHash: Option[String], createdAt: Long, isAdmin: Boolean)
final case class SessionRecord(id: String, userId: Long, expiresAt: Long)

trait AuthDao {
  def createUserWithPassword(email: String, passwordHash: String, isAdmin: Boolean = false): Either[String, UserRecord]
  def findByEmail(email: String): Option[UserRecord]
  def findById(userId: Long): Option[UserRecord]
  def findOrCreateGoogleUser(email: String): UserRecord
  def createSession(userId: Long, ttlMillis: Long): SessionRecord
  def findValidSession(sessionId: String): Option[SessionRecord]
  def deleteSession(sessionId: String): Unit
  def listUsers(): List[UserRecord]
  def updateUser(id: Long, email: Option[String], passwordHash: Option[String], isAdmin: Option[Boolean]): Either[String, UserRecord]
  def deleteUser(id: Long): Boolean
  def close(): Unit

  /** Resolves the raw session-cookie value straight to the user it belongs to, or None if
    * the cookie is missing, unknown, or expired.
    */
  def currentUser(sessionId: String): Option[UserRecord] =
    findValidSession(sessionId).flatMap(s => findById(s.userId))
}
