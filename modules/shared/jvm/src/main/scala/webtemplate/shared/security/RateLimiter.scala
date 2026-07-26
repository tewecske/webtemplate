package webtemplate.shared.security

import java.util.concurrent.ConcurrentHashMap

/** Fixed-window attempt counter to blunt brute-force/credential-stuffing against
  * `/auth/login`, `/auth/signup`, `/auth/google-dev`. See CWE-307 / OWASP A07:2021.
  *
  * In-memory and keyed by caller-supplied string (normalized email in practice): each
  * backend is a single JVM process with its own SQLite file, so state doesn't need to
  * survive restarts or be shared across nodes.
  */
final class RateLimiter(maxAttempts: Int, windowMillis: Long) {
  private final case class Window(count: Int, windowStart: Long)
  private val attempts = new ConcurrentHashMap[String, Window]()

  private def now(): Long = System.currentTimeMillis()

  def recordFailure(key: String): Unit =
    attempts.compute(
      key,
      (_, existing) => {
        val t = now()
        existing match {
          case null                                          => Window(1, t)
          case Window(_, start) if t - start > windowMillis  => Window(1, t)
          case Window(count, start)                          => Window(count + 1, start)
        }
      }
    )

  def recordSuccess(key: String): Unit = attempts.remove(key)

  def isLocked(key: String): Boolean =
    Option(attempts.get(key)).exists { case Window(count, start) =>
      count >= maxAttempts && now() - start <= windowMillis
    }
}
