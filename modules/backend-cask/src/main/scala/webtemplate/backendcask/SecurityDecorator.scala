package webtemplate.backendcask

import webtemplate.shared.ApiError
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.security.OriginCheck

/** Wired in application-wide via `cask.Main#mainDecorators` (see `Main.scala`) so every
  * route in every `cask.Routes` class gets baseline security headers and CSRF protection
  * without each handler wiring it in by hand. See CWE-352, CWE-693, OWASP A01:2021 /
  * A05:2021.
  */
class SecurityDecorator(config: AuthConfig) extends cask.RawDecorator {
  private val mutatingMethods = Set("POST", "PUT", "DELETE")

  def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
    val method = ctx.exchange.getRequestMethod.toString
    val csrfBlocked = mutatingMethods.contains(method) && {
      val origin = ctx.headers.get("origin").flatMap(_.headOption)
      val referer = ctx.headers.get("referer").flatMap(_.headOption)
      !OriginCheck.isAllowed(origin, referer, config.allowedOrigins.toSet)
    }
    if (csrfBlocked)
      cask.router.Result.Success(
        cask.Response(
          upickle.default.write(ApiError("cross-site request blocked")),
          statusCode = 403,
          headers = Seq("Content-Type" -> "application/json") ++ SecurityHeaders(config)
        )
      )
    else
      delegate(ctx, Map()).map(resp => resp.copy(headers = resp.headers ++ SecurityHeaders(config)))
  }
}
