package webtemplate.backendcask

import webtemplate.shared.config.AuthConfig
import webtemplate.shared.security.OriginCheck

/** Mix into a handler class with a `config: AuthConfig` member to gate state-changing
  * methods against cross-site requests. See CWE-352 / OWASP A01:2021.
  */
trait CsrfGuard {
  protected def csrfBlocked(request: cask.Request, config: AuthConfig): Boolean = {
    val origin = request.headers.get("origin").flatMap(_.headOption)
    val referer = request.headers.get("referer").flatMap(_.headOption)
    !OriginCheck.isAllowed(origin, referer, config.allowedOrigins.toSet)
  }
}
