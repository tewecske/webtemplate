package webtemplate.backendcask

import webtemplate.shared.{AdminUserView, ApiError, CreateUserRequest, UpdateUserRequest}
import webtemplate.shared.auth.PasswordHasher
import webtemplate.shared.db.{AuthDao, UserRecord}

final class AdminHandlers(dao: AuthDao, auth: SessionAuthenticator) {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  def listUsers(request: cask.Request): cask.Response[String] =
    withAdmin(request) { _ =>
      cask.Response(upickle.default.write(dao.listUsers().map(toView)), headers = jsonHeaders)
    }

  def getUser(id: Long, request: cask.Request): cask.Response[String] =
    withAdmin(request) { _ =>
      dao.findById(id) match {
        case Some(user) => cask.Response(upickle.default.write(toView(user)), headers = jsonHeaders)
        case None       => notFound
      }
    }

  def createUser(request: cask.Request): cask.Response[String] =
    withAdmin(request) { _ =>
      try {
        val body = upickle.default.read[CreateUserRequest](request.text())
        val email = body.email.trim
        if (email.isEmpty || !email.contains("@")) badRequest("invalid email")
        else if (body.password.length < 8) badRequest("password must be at least 8 characters")
        else
          dao.createUserWithPassword(email, PasswordHasher.hash(body.password), body.isAdmin) match {
            case Right(user) => cask.Response(upickle.default.write(toView(user)), statusCode = 201, headers = jsonHeaders)
            case Left(err)   => badRequest(err)
          }
      } catch {
        case e: Exception => badRequest(s"invalid request: ${e.getMessage}")
      }
    }

  def updateUser(id: Long, request: cask.Request): cask.Response[String] =
    withAdmin(request) { admin =>
      try {
        val body = upickle.default.read[UpdateUserRequest](request.text())
        if (id == admin.id && body.isAdmin.contains(false)) conflict("cannot remove your own admin access")
        else
          dao.updateUser(id, body.email.map(_.trim), body.password.map(PasswordHasher.hash), body.isAdmin) match {
            case Right(user)            => cask.Response(upickle.default.write(toView(user)), headers = jsonHeaders)
            case Left("user not found") => notFound
            case Left(err)              => badRequest(err)
          }
      } catch {
        case e: Exception => badRequest(s"invalid request: ${e.getMessage}")
      }
    }

  def deleteUser(id: Long, request: cask.Request): cask.Response[String] =
    withAdmin(request) { admin =>
      if (id == admin.id) conflict("cannot delete your own account")
      else if (dao.deleteUser(id)) cask.Response(upickle.default.write(Map("status" -> "ok")), headers = jsonHeaders)
      else notFound
    }

  private def withAdmin(request: cask.Request)(f: UserRecord => cask.Response[String]): cask.Response[String] =
    auth.authenticateAdmin(request) match {
      case AdminAuthResult.Unauthenticated  => unauthorized
      case AdminAuthResult.Forbidden        => forbidden
      case AdminAuthResult.Authorized(user) => f(user)
    }

  private def toView(u: UserRecord): AdminUserView = AdminUserView(u.id, u.email, u.isAdmin, u.createdAt)

  private def unauthorized: cask.Response[String] =
    cask.Response(upickle.default.write(ApiError("not authenticated")), statusCode = 401, headers = jsonHeaders)
  private def forbidden: cask.Response[String] =
    cask.Response(upickle.default.write(ApiError("admin access required")), statusCode = 403, headers = jsonHeaders)
  private def notFound: cask.Response[String] =
    cask.Response(upickle.default.write(ApiError("user not found")), statusCode = 404, headers = jsonHeaders)
  private def conflict(msg: String): cask.Response[String] =
    cask.Response(upickle.default.write(ApiError(msg)), statusCode = 409, headers = jsonHeaders)
  private def badRequest(msg: String): cask.Response[String] =
    cask.Response(upickle.default.write(ApiError(msg)), statusCode = 400, headers = jsonHeaders)
}
