package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.ApiError
import webtemplate.shared.security.{RateLimiter, RateLimitKey, SecurityLog}

/** Applied only to the auth route slice in `Main.scala`, not globally — rate limiting is an
  * auth-specific concern, not a generic per-route one. Bookkeeping (success/failure) is
  * derived from the wrapped response's status code, so `AuthHandlers` no longer calls into
  * `RateLimiter` directly. See CWE-307, OWASP A07:2021.
  */
final class RateLimitMiddleware(rateLimiter: RateLimiter) extends Middleware[Any] {
  def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
    routes.transform { handler =>
      Handler.scoped[Env1] {
        Handler.fromFunctionZIO[Request] { req =>
          val path = req.path.toString
          for {
            bodyText <- req.body.asString.orElseSucceed("")
            email = RateLimitKey.emailFromJsonBody(bodyText).getOrElse("").toLowerCase
            key = s"$path:$email"
            resp <-
              if (rateLimiter.isLocked(key)) {
                SecurityLog.rateLimited(key, path)
                ZIO.succeed(
                  Response
                    .json(upickle.default.write(ApiError("too many attempts, try again later")))
                    .status(Status.TooManyRequests)
                )
              } else {
                val replayedReq = req.withBody(Body.fromString(bodyText))
                handler(replayedReq).map { r =>
                  if (path == "/auth/login") {
                    if (r.status == Status.Ok) rateLimiter.recordSuccess(key) else rateLimiter.recordFailure(key)
                  } else {
                    // signup / google-dev-login: every attempt counts, success never clears the counter.
                    rateLimiter.recordFailure(key)
                  }
                  r
                }
              }
          } yield resp
        }
      }
    }
}
