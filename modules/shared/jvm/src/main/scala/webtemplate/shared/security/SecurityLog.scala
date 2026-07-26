package webtemplate.shared.security

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/** Structured security event log. See CWE-778 / OWASP A09:2021. Backed by SLF4J
  * (logback-classic on the classpath) rather than a bespoke logger, so log level, format, and
  * destination are configured the normal way via logback.xml. Never log passwords, hashes, or
  * full session tokens.
  */
object SecurityLog {
  private val logger = Logger(LoggerFactory.getLogger("SECURITY"))

  private def emit(event: String, fields: (String, Any)*): Unit = {
    val kv = fields.map { case (k, v) => s"""$k="$v"""" }.mkString(" ")
    logger.info(s"event=$event $kv")
  }

  def loginFailed(email: String): Unit = emit("login_failed", "email" -> email)

  def loginSucceeded(userId: Long, email: String): Unit =
    emit("login_succeeded", "userId" -> userId, "email" -> email)

  def sessionDeleted(userId: Long): Unit = emit("session_deleted", "userId" -> userId)

  def signupSucceeded(userId: Long, email: String): Unit =
    emit("signup_succeeded", "userId" -> userId, "email" -> email)

  def adminAction(actorUserId: Long, action: String, targetUserId: Long, details: String = ""): Unit =
    emit("admin_action", "actorUserId" -> actorUserId, "action" -> action, "targetUserId" -> targetUserId, "details" -> details)

  def rateLimited(key: String, endpoint: String): Unit = emit("rate_limited", "key" -> key, "endpoint" -> endpoint)
}
