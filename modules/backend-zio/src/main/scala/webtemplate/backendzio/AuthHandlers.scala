package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.{ApiError, GoogleDevLoginRequest, LoginRequest, SignupRequest, UserView}
import webtemplate.shared.auth.{EmailValidation, PasswordHasher, PasswordPolicy}
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.{AuthDao, UserRecord}
import webtemplate.shared.security.SecurityLog

import java.time.Duration

final class AuthHandlers(dao: AuthDao, config: AuthConfig, auth: SessionAuthenticator) {
  private val ttlMillis = config.sessionTtlDays.toLong * 24 * 60 * 60 * 1000

  private def sessionCookie(sessionId: String, expiresAt: Long): Cookie.Response =
    Cookie.Response(
      name = config.sessionCookieName,
      content = sessionId,
      path = Some(Path.root),
      isHttpOnly = true,
      isSecure = config.secureCookie,
      maxAge = Some(Duration.ofMillis(expiresAt - java.lang.System.currentTimeMillis())),
      sameSite = Some(Cookie.SameSite.Lax)
    )

  private def clearCookie: Cookie.Response =
    Cookie.Response(
      name = config.sessionCookieName,
      content = "",
      path = Some(Path.root),
      isHttpOnly = true,
      isSecure = config.secureCookie,
      maxAge = Some(Duration.ZERO),
      sameSite = Some(Cookie.SameSite.Lax)
    )

  private def authSuccess(user: UserRecord, status: Status = Status.Ok): ZIO[Any, Throwable, Response] =
    ZIO.attemptBlocking(dao.createSession(user.id, ttlMillis)).map { session =>
      Response
        .json(upickle.default.write(UserView(user.id, user.email, user.isAdmin)))
        .status(status)
        .addCookie(sessionCookie(session.id, session.expiresAt))
    }

  private def badRequest(msg: String): ZIO[Any, Nothing, Response] =
    ZIO.succeed(Response.json(upickle.default.write(ApiError(msg))).status(Status.BadRequest))

  def signup(req: Request): ZIO[Any, Nothing, Response] =
    (for {
      bodyStr <- req.body.asString
      body    <- ZIO.attempt(upickle.default.read[SignupRequest](bodyStr))
      email = body.email.trim
      resp <-
        if (!EmailValidation.isValid(email))
          ZIO.succeed(Response.json(upickle.default.write(ApiError("invalid email"))).status(Status.BadRequest))
        else
          PasswordPolicy.validate(body.password) match {
            case Left(err) => ZIO.succeed(Response.json(upickle.default.write(ApiError(err))).status(Status.BadRequest))
            case Right(()) =>
              ZIO.attemptBlocking(dao.createUserWithPassword(email, PasswordHasher.hash(body.password))).flatMap {
                case Right(user) =>
                  SecurityLog.signupSucceeded(user.id, user.email)
                  authSuccess(user, Status.Created)
                case Left(err) =>
                  ZIO.succeed(Response.json(upickle.default.write(ApiError(err))).status(Status.BadRequest))
              }
          }
    } yield resp).catchAll { e =>
      ZIO.succeed(Response.json(upickle.default.write(ApiError(s"invalid request: ${e.getMessage}"))).status(Status.BadRequest))
    }

  def login(req: Request): ZIO[Any, Nothing, Response] =
    (for {
      bodyStr <- req.body.asString
      body    <- ZIO.attempt(upickle.default.read[LoginRequest](bodyStr))
      userOpt <- ZIO.attemptBlocking(dao.findByEmail(body.email))
      resp <- userOpt match {
        case Some(user) if user.passwordHash.exists(h => PasswordHasher.verify(body.password, h)) =>
          SecurityLog.loginSucceeded(user.id, user.email)
          authSuccess(user)
        case _ =>
          SecurityLog.loginFailed(body.email)
          ZIO.succeed(
            Response.json(upickle.default.write(ApiError("invalid email or password"))).status(Status.Unauthorized)
          )
      }
    } yield resp).catchAll { e =>
      ZIO.succeed(Response.json(upickle.default.write(ApiError(s"invalid request: ${e.getMessage}"))).status(Status.BadRequest))
    }

  def googleDevLogin(req: Request): ZIO[Any, Nothing, Response] =
    if (!config.googleDevLoginEnabled)
      ZIO.succeed(Response.json(upickle.default.write(ApiError("dev google login is disabled"))).status(Status.NotFound))
    else
      (for {
        bodyStr <- req.body.asString
        body    <- ZIO.attempt(upickle.default.read[GoogleDevLoginRequest](bodyStr))
        email = body.email.trim
        resp <-
          if (!EmailValidation.isValid(email))
            ZIO.succeed(Response.json(upickle.default.write(ApiError("invalid email"))).status(Status.BadRequest))
          else
            ZIO.attemptBlocking(dao.findOrCreateGoogleUser(email)).flatMap { user =>
              SecurityLog.loginSucceeded(user.id, user.email)
              authSuccess(user)
            }
      } yield resp).catchAll { e =>
        ZIO.succeed(Response.json(upickle.default.write(ApiError(s"invalid request: ${e.getMessage}"))).status(Status.BadRequest))
      }

  def me(req: Request): ZIO[Any, Nothing, Response] =
    auth.authenticate(req).map {
      case Some(user) => Response.json(upickle.default.write(UserView(user.id, user.email, user.isAdmin)))
      case None       => Response.json(upickle.default.write(ApiError("not authenticated"))).status(Status.Unauthorized)
    }

  def logout(req: Request): ZIO[Any, Nothing, Response] =
    auth.authenticate(req).flatMap { userOpt =>
      userOpt.foreach(u => SecurityLog.sessionDeleted(u.id))
      ZIO
        .foreachDiscard(req.cookie(config.sessionCookieName))(c => ZIO.attemptBlocking(dao.deleteSession(c.content)).ignore)
        .as(Response.json(upickle.default.write(Map("status" -> "ok"))).addCookie(clearCookie))
    }
}
