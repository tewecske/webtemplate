package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.{AdminUserView, ApiError, CreateUserRequest, UpdateUserRequest}
import webtemplate.shared.auth.{EmailValidation, PasswordHasher, PasswordPolicy}
import webtemplate.shared.db.{AuthDao, UserRecord}
import webtemplate.shared.security.SecurityLog

final class AdminHandlers(dao: AuthDao, auth: SessionAuthenticator) {

  def listUsers(req: Request): ZIO[Any, Nothing, Response] =
    withAdmin(req) { _ =>
      ZIO
        .attemptBlocking(dao.listUsers().map(toView))
        .map(views => Response.json(upickle.default.write(views)))
        .catchAll(_ => ZIO.succeed(internalError))
    }

  def getUser(id: Long, req: Request): ZIO[Any, Nothing, Response] =
    withAdmin(req) { _ =>
      ZIO
        .attemptBlocking(dao.findById(id))
        .map {
          case Some(user) => Response.json(upickle.default.write(toView(user)))
          case None       => notFoundResp
        }
        .catchAll(_ => ZIO.succeed(internalError))
    }

  def createUser(req: Request): ZIO[Any, Nothing, Response] =
    withAdmin(req) { admin =>
      (for {
        bodyStr <- req.body.asString
        body    <- ZIO.attempt(upickle.default.read[CreateUserRequest](bodyStr))
        email = body.email.trim
        resp <-
          if (!EmailValidation.isValid(email)) ZIO.succeed(badRequestResp("invalid email"))
          else
            PasswordPolicy.validate(body.password) match {
              case Left(err) => ZIO.succeed(badRequestResp(err))
              case Right(()) =>
                ZIO.attemptBlocking(dao.createUserWithPassword(email, PasswordHasher.hash(body.password), body.isAdmin)).map {
                  case Right(user) =>
                    SecurityLog.adminAction(admin.id, "create_user", user.id, s"isAdmin=${user.isAdmin}")
                    Response.json(upickle.default.write(toView(user))).status(Status.Created)
                  case Left(err) => badRequestResp(err)
                }
            }
      } yield resp).catchAll(e => ZIO.succeed(badRequestResp(s"invalid request: ${e.getMessage}")))
    }

  def updateUser(id: Long, req: Request): ZIO[Any, Nothing, Response] =
    withAdmin(req) { admin =>
      (for {
        bodyStr <- req.body.asString
        body    <- ZIO.attempt(upickle.default.read[UpdateUserRequest](bodyStr))
        resp <-
          if (id == admin.id && body.isAdmin.contains(false))
            ZIO.succeed(conflictResp("cannot remove your own admin access"))
          else if (body.email.exists(e => !EmailValidation.isValid(e.trim)))
            ZIO.succeed(badRequestResp("invalid email"))
          else if (body.password.exists(p => PasswordPolicy.validate(p).isLeft))
            ZIO.succeed(badRequestResp(body.password.flatMap(p => PasswordPolicy.validate(p).left.toOption).getOrElse("invalid password")))
          else
            ZIO
              .attemptBlocking(dao.updateUser(id, body.email.map(_.trim), body.password.map(PasswordHasher.hash), body.isAdmin))
              .map {
                case Right(user) =>
                  val roleNote = body.isAdmin.map(v => s" isAdminChanged=$v").getOrElse("")
                  SecurityLog.adminAction(admin.id, "update_user", user.id, roleNote.trim)
                  Response.json(upickle.default.write(toView(user)))
                case Left("user not found") => notFoundResp
                case Left(err)              => badRequestResp(err)
              }
      } yield resp).catchAll(e => ZIO.succeed(badRequestResp(s"invalid request: ${e.getMessage}")))
    }

  def deleteUser(id: Long, req: Request): ZIO[Any, Nothing, Response] =
    withAdmin(req) { admin =>
      if (id == admin.id) ZIO.succeed(conflictResp("cannot delete your own account"))
      else
        ZIO
          .attemptBlocking(dao.deleteUser(id))
          .map {
            case true =>
              SecurityLog.adminAction(admin.id, "delete_user", id)
              Response.json(upickle.default.write(Map("status" -> "ok")))
            case false => notFoundResp
          }
          .catchAll(_ => ZIO.succeed(internalError))
    }

  private def withAdmin(req: Request)(f: UserRecord => ZIO[Any, Nothing, Response]): ZIO[Any, Nothing, Response] =
    auth.authenticateAdmin(req).flatMap {
      case AdminAuthResult.Unauthenticated =>
        ZIO.succeed(Response.json(upickle.default.write(ApiError("not authenticated"))).status(Status.Unauthorized))
      case AdminAuthResult.Forbidden =>
        ZIO.succeed(Response.json(upickle.default.write(ApiError("admin access required"))).status(Status.Forbidden))
      case AdminAuthResult.Authorized(user) => f(user)
    }

  private def toView(u: UserRecord): AdminUserView = AdminUserView(u.id, u.email, u.isAdmin, u.createdAt)

  private def notFoundResp: Response =
    Response.json(upickle.default.write(ApiError("user not found"))).status(Status.NotFound)
  private def internalError: Response =
    Response.json(upickle.default.write(ApiError("internal error"))).status(Status.InternalServerError)
  private def badRequestResp(msg: String): Response =
    Response.json(upickle.default.write(ApiError(msg))).status(Status.BadRequest)
  private def conflictResp(msg: String): Response =
    Response.json(upickle.default.write(ApiError(msg))).status(Status.Conflict)
}
