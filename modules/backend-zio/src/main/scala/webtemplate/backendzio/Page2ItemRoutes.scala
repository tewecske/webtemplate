package webtemplate.backendzio

import zio.http._

object Page2ItemRoutes {
  def apply(handlers: Page2ItemHandlers): Routes[Any, Response] =
    Routes(
      Method.POST / "page2items" -> handler((req: Request) => handlers.createPage2Item(req)),
      Method.GET / "page2items" ->
        handler((req: Request) => handlers.getPage2Items(req))
    )
}
