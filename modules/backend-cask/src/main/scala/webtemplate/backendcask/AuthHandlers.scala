package webtemplate.backendcask

import webtemplate.shared.{ApiError, GoogleDevLoginRequest, LoginRequest, SignupRequest, UserView}
import webtemplate.shared.auth.{EmailValidation, PasswordHasher, PasswordPolicy}
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.{AuthDao, UserRecord}
import webtemplate.shared.security.SecurityLog

import java.time.Instant

final class AuthHandlers(dao: AuthDao, config: AuthConfig, auth: SessionAuthenticator) {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")
  private val ttlMillis = config.sessionTtlDays.toLong * 24 * 60 * 60 * 1000

  private def sessionCookie(sessionId: String, expiresAt: Long): cask.Cookie =
    cask.Cookie(
      name = config.sessionCookieName,
      value = sessionId,
      path = "/",
      expires = Instant.ofEpochMilli(expiresAt),
      httpOnly = true,
      secure = config.secureCookie,
      sameSite = "Lax"
    )

  private def clearCookie: cask.Cookie =
    cask.Cookie(
      name = config.sessionCookieName,
      value = "",
      path = "/",
      expires = Instant.EPOCH,
      httpOnly = true,
      secure = config.secureCookie,
      sameSite = "Lax"
    )

  private def authSuccess(user: UserRecord, statusCode: Int = 200): cask.Response[String] = {
    val session = dao.createSession(user.id, ttlMillis)
    cask.Response(
      upickle.default.write(UserView(user.id, user.email, user.isAdmin)),
      statusCode = statusCode,
      headers = jsonHeaders,
      cookies = Seq(sessionCookie(session.id, session.expiresAt))
    )
  }

  private def badRequest(msg: String): cask.Response[String] =
    cask.Response(upickle.default.write(ApiError(msg)), statusCode = 400, headers = jsonHeaders)

  def signup(request: cask.Request): cask.Response[String] =
    try {
      val body = upickle.default.read[SignupRequest](request.text())
      val email = body.email.trim
      if (!EmailValidation.isValid(email)) badRequest("invalid email")
      else
        PasswordPolicy.validate(body.password) match {
          case Left(err) => badRequest(err)
          case Right(()) =>
            dao.createUserWithPassword(email, PasswordHasher.hash(body.password)) match {
              case Right(user) =>
                SecurityLog.signupSucceeded(user.id, user.email)
                authSuccess(user, statusCode = 201)
              case Left(err) => badRequest(err)
            }
        }
    } catch {
      case e: Exception => badRequest(s"invalid request: ${e.getMessage}")
    }

  def login(request: cask.Request): cask.Response[String] =
    try {
      val body = upickle.default.read[LoginRequest](request.text())
      dao.findByEmail(body.email) match {
        case Some(user) if user.passwordHash.exists(h => PasswordHasher.verify(body.password, h)) =>
          SecurityLog.loginSucceeded(user.id, user.email)
          authSuccess(user)
        case _ =>
          SecurityLog.loginFailed(body.email)
          cask.Response(upickle.default.write(ApiError("invalid email or password")), statusCode = 401, headers = jsonHeaders)
      }
    } catch {
      case e: Exception => badRequest(s"invalid request: ${e.getMessage}")
    }

  def googleDevLogin(request: cask.Request): cask.Response[String] =
    if (!config.googleDevLoginEnabled)
      cask.Response(upickle.default.write(ApiError("dev google login is disabled")), statusCode = 404, headers = jsonHeaders)
    else
      try {
        val body = upickle.default.read[GoogleDevLoginRequest](request.text())
        val email = body.email.trim
        if (!EmailValidation.isValid(email)) badRequest("invalid email")
        else {
          val user = dao.findOrCreateGoogleUser(email)
          SecurityLog.loginSucceeded(user.id, user.email)
          authSuccess(user)
        }
      } catch {
        case e: Exception => badRequest(s"invalid request: ${e.getMessage}")
      }

  def me(request: cask.Request): cask.Response[String] =
    auth.authenticate(request) match {
      case Some(user) => cask.Response(upickle.default.write(UserView(user.id, user.email, user.isAdmin)), headers = jsonHeaders)
      case None =>
        cask.Response(upickle.default.write(ApiError("not authenticated")), statusCode = 401, headers = jsonHeaders)
    }

  def logout(request: cask.Request): cask.Response[String] = {
    auth.authenticate(request).foreach(u => SecurityLog.sessionDeleted(u.id))
    request.cookies.get(config.sessionCookieName).foreach(c => dao.deleteSession(c.value))
    cask.Response(
      upickle.default.write(Map("status" -> "ok")),
      headers = jsonHeaders,
      cookies = Seq(clearCookie)
    )
  }
}
