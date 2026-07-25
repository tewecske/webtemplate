package webtemplate.backendcask

class AdminRoutes(handlers: AdminHandlers) extends cask.Routes {

  @cask.get("/admin/users")
  def listUsers(request: cask.Request): cask.Response[String] = handlers.listUsers(request)

  @cask.get("/admin/users/:id")
  def getUser(id: Long, request: cask.Request): cask.Response[String] = handlers.getUser(id, request)

  @cask.post("/admin/users")
  def createUser(request: cask.Request): cask.Response[String] = handlers.createUser(request)

  @cask.put("/admin/users/:id")
  def updateUser(id: Long, request: cask.Request): cask.Response[String] = handlers.updateUser(id, request)

  @cask.delete("/admin/users/:id")
  def deleteUser(id: Long, request: cask.Request): cask.Response[String] = handlers.deleteUser(id, request)

  initialize()
}
