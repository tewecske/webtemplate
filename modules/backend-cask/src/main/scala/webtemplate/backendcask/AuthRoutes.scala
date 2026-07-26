package webtemplate.backendcask

import webtemplate.shared.security.RateLimiter

class AuthRoutes(handlers: AuthHandlers, rateLimiter: RateLimiter) extends cask.Routes {
  override def decorators: Seq[cask.router.Decorator[?, ?, ?, ?]] = Seq(new RateLimitDecorator(rateLimiter))

  @cask.post("/auth/signup")
  def signup(request: cask.Request): cask.Response[String] = handlers.signup(request)

  @cask.post("/auth/login")
  def login(request: cask.Request): cask.Response[String] = handlers.login(request)

  @cask.post("/auth/logout")
  def logout(request: cask.Request): cask.Response[String] = handlers.logout(request)

  @cask.post("/auth/google-dev")
  def googleDev(request: cask.Request): cask.Response[String] = handlers.googleDevLogin(request)

  @cask.get("/auth/me")
  def me(request: cask.Request): cask.Response[String] = handlers.me(request)

  initialize()
}
