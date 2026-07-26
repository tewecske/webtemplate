package webtemplate.shared.security

import java.time.Instant

/** Minimal structured security event log to stdout — single-line, ISO-8601-timestamped,
  * key=value entries. See CWE-778 / OWASP A09:2021. A real deployment's process
  * manager/container runtime already captures stdout, so this is 12-factor-friendly without
  * needing a logging framework. Never log passwords, hashes, or full session tokens.
  */
object SecurityLog {
  private def emit(event: String, fields: (String, Any)*): Unit = {
    val kv = fields.map { case (k, v) => s"""$k="$v"""" }.mkString(" ")
    println(s"[SECURITY] ts=${Instant.now()} event=$event $kv")
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
