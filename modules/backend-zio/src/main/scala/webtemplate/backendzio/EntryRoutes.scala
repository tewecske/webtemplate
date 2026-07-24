package webtemplate.backendzio

import zio.http._

object EntryRoutes {
  def apply(handlers: EntryHandlers): Routes[Any, Response] =
    Routes(
      Method.GET / "health" -> handler(handlers.health),
      Method.POST / "entries" -> handler((req: Request) => handlers.createEntry(req)),
      Method.GET / "entries" / string("inputId") ->
        handler((inputId: String, _: Request) => handlers.getHistory(inputId))
    )
}
