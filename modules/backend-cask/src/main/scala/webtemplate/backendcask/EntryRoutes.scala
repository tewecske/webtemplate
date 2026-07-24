package webtemplate.backendcask

class EntryRoutes(handlers: EntryHandlers) extends cask.Routes {

  @cask.get("/health")
  def health(): cask.Response[String] = handlers.health()

  @cask.post("/entries")
  def createEntry(request: cask.Request): cask.Response[String] = handlers.createEntry(request)

  @cask.get("/entries/:inputId")
  def getHistory(inputId: String, request: cask.Request): cask.Response[String] = handlers.getHistory(inputId, request)

  initialize()
}
