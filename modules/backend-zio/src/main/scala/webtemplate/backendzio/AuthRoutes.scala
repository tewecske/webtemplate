package webtemplate.backendzio

import zio.http._

object AuthRoutes {
  def apply(handlers: AuthHandlers): Routes[Any, Response] =
    Routes(
      Method.POST / "auth" / "signup" -> handler((req: Request) => handlers.signup(req)),
      Method.POST / "auth" / "login" -> handler((req: Request) => handlers.login(req)),
      Method.POST / "auth" / "logout" -> handler((req: Request) => handlers.logout(req)),
      Method.POST / "auth" / "google-dev" -> handler((req: Request) => handlers.googleDevLogin(req)),
      Method.GET / "auth" / "me" -> handler((req: Request) => handlers.me(req))
    )
}
