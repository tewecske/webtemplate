package webtemplate.backendzio

import zio.http._

object AdminRoutes {
  def apply(handlers: AdminHandlers): Routes[Any, Response] =
    Routes(
      Method.GET / "admin" / "users" -> handler((req: Request) => handlers.listUsers(req)),
      Method.GET / "admin" / "users" / long("id") -> handler((id: Long, req: Request) => handlers.getUser(id, req)),
      Method.POST / "admin" / "users" -> handler((req: Request) => handlers.createUser(req)),
      Method.PUT / "admin" / "users" / long("id") -> handler((id: Long, req: Request) => handlers.updateUser(id, req)),
      Method.DELETE / "admin" / "users" / long("id") -> handler((id: Long, req: Request) => handlers.deleteUser(id, req))
    )
}
