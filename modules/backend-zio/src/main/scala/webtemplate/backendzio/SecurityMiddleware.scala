package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.ApiError
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.security.OriginCheck

/** Applied once, globally, to the combined `Routes` value in `Main.scala` — adds baseline
  * security headers to every response and blocks cross-site mutating requests, so no
  * individual handler wires either in by hand. See CWE-352, CWE-693, OWASP A01:2021 /
  * A05:2021.
  */
final class SecurityMiddleware(config: AuthConfig) extends Middleware[Any] {
  private def isMutating(method: Method): Boolean =
    method == Method.POST || method == Method.PUT || method == Method.DELETE

  def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
    routes.transform { handler =>
      Handler.scoped[Env1] {
        Handler.fromFunctionZIO[Request] { req =>
          val csrfBlocked = isMutating(req.method) && {
            val origin = req.headers.get("Origin")
            val referer = req.headers.get("Referer")
            !OriginCheck.isAllowed(origin, referer, config.allowedOrigins.toSet)
          }
          if (csrfBlocked)
            ZIO.succeed(
              Response
                .json(upickle.default.write(ApiError("cross-site request blocked")))
                .status(Status.Forbidden)
                .addHeaders(SecurityHeaders(config))
            )
          else
            handler(req).map(_.addHeaders(SecurityHeaders(config)))
        }
      }
    }
}
