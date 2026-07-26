package webtemplate.backendzio

import zio.http.Request
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.security.OriginCheck

/** Mix into a handler class to gate state-changing methods against cross-site requests. See
  * CWE-352 / OWASP A01:2021.
  */
trait CsrfGuard {
  protected def csrfBlocked(req: Request, config: AuthConfig): Boolean = {
    val origin = req.headers.get("Origin")
    val referer = req.headers.get("Referer")
    !OriginCheck.isAllowed(origin, referer, config.allowedOrigins.toSet)
  }
}
