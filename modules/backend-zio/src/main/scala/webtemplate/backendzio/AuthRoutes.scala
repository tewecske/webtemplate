package webtemplate.backendzio

import zio.http._
import webtemplate.shared.security.RateLimiter

object AuthRoutes {
  def apply(handlers: AuthHandlers, rateLimiter: RateLimiter): Routes[Any, Response] =
    Routes(
      Method.POST / "auth" / "signup" -> handler((req: Request) => handlers.signup(req)),
      Method.POST / "auth" / "login" -> handler((req: Request) => handlers.login(req)),
      Method.POST / "auth" / "logout" -> handler((req: Request) => handlers.logout(req)),
      Method.POST / "auth" / "google-dev" -> handler((req: Request) => handlers.googleDevLogin(req)),
      Method.GET / "auth" / "me" -> handler((req: Request) => handlers.me(req))
    ) @@ new RateLimitMiddleware(rateLimiter)
}
