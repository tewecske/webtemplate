package webtemplate.shared.security

/** CSRF defense-in-depth for cookie-session-authenticated state-changing requests. See
  * CWE-352 / OWASP A01:2021. `SameSite=Lax` session cookies already block the cookie from
  * riding along on cross-site requests in evergreen browsers; this is a second, independent
  * check on top of that.
  */
object OriginCheck {

  /** If `Origin` is present, it must match `allowedOrigins`. Otherwise fall back to
    * `Referer`'s origin. If neither header is present, allow the request: real browsers
    * always send `Origin` on state-changing fetches, so the absence of both means a
    * non-browser client (curl, tests, server-to-server), which cookie-jar-based CSRF can't
    * target anyway.
    */
  def isAllowed(originHeader: Option[String], refererHeader: Option[String], allowedOrigins: Set[String]): Boolean = {
    val normalizedAllowed = allowedOrigins.map(_.trim.toLowerCase)

    def originOf(headerValue: String): Option[String] =
      try {
        val uri = new java.net.URI(headerValue)
        (Option(uri.getScheme), Option(uri.getHost)) match {
          case (Some(scheme), Some(host)) =>
            val port = uri.getPort
            Some((if (port == -1) s"$scheme://$host" else s"$scheme://$host:$port").toLowerCase)
          case _ => None
        }
      } catch { case _: Throwable => None }

    val origin = originHeader.map(_.trim).filter(_.nonEmpty)
    val referer = refererHeader.map(_.trim).filter(_.nonEmpty)

    (origin, referer) match {
      case (Some(o), _)    => originOf(o).exists(normalizedAllowed.contains)
      case (None, Some(r)) => originOf(r).exists(normalizedAllowed.contains)
      case (None, None)    => true
    }
  }
}
