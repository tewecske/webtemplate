package webtemplate.shared.security

/** Sniffs the `email` field out of a raw JSON request body without a typed case class, so a
  * single generic extractor can back rate-limit keying for signup/login/google-dev-login —
  * bodies that are otherwise only decoded by each backend's own typed request DTOs.
  */
object RateLimitKey {
  def emailFromJsonBody(body: String): Option[String] =
    try
      ujson.read(body).obj.get("email").flatMap(_.strOpt).map(_.trim).filter(_.nonEmpty)
    catch { case _: Throwable => None }
}
