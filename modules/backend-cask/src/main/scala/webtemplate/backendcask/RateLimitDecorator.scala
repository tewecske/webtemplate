package webtemplate.backendcask

import webtemplate.shared.ApiError
import webtemplate.shared.security.{RateLimiter, RateLimitKey, SecurityLog}

/** Wired only into `AuthRoutes`'s own `decorators` override (see `AuthRoutes.scala`), not
  * application-wide — rate limiting is an auth-specific concern, not a generic per-route one.
  * Bookkeeping (success/failure) is derived from the wrapped response's status code, so
  * `AuthHandlers` no longer calls into `RateLimiter` directly. See CWE-307, OWASP A07:2021.
  */
class RateLimitDecorator(rateLimiter: RateLimiter) extends cask.RawDecorator {
  def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
    val path = ctx.exchange.getRequestPath
    val email = RateLimitKey.emailFromJsonBody(ctx.text()).getOrElse("").toLowerCase
    val key = s"$path:$email"

    if (rateLimiter.isLocked(key)) {
      SecurityLog.rateLimited(key, path)
      cask.router.Result.Success(
        cask.Response(
          upickle.default.write(ApiError("too many attempts, try again later")),
          statusCode = 429,
          headers = Seq("Content-Type" -> "application/json")
        )
      )
    } else
      delegate(ctx, Map()).map { resp =>
        if (path == "/auth/login") {
          if (resp.statusCode == 200) rateLimiter.recordSuccess(key) else rateLimiter.recordFailure(key)
        } else {
          // signup / google-dev-login: every attempt counts, success never clears the counter.
          rateLimiter.recordFailure(key)
        }
        resp
      }
  }
}
