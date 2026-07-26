package webtemplate.backendcask

import webtemplate.shared.{AdminUserView, ApiError, CreateUserRequest, UpdateUserRequest}
import webtemplate.shared.auth.{EmailValidation, PasswordHasher, PasswordPolicy}
import webtemplate.shared.db.{AuthDao, UserRecord}
import webtemplate.shared.security.SecurityLog

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
    withAdmin(request) { admin =>
      try {
        val body = upickle.default.read[CreateUserRequest](request.text())
        val email = body.email.trim
        if (!EmailValidation.isValid(email)) badRequest("invalid email")
        else
          PasswordPolicy.validate(body.password) match {
            case Left(err) => badRequest(err)
            case Right(()) =>
              dao.createUserWithPassword(email, PasswordHasher.hash(body.password), body.isAdmin) match {
                case Right(user) =>
                  SecurityLog.adminAction(admin.id, "create_user", user.id, s"isAdmin=${user.isAdmin}")
                  cask.Response(upickle.default.write(toView(user)), statusCode = 201, headers = jsonHeaders)
                case Left(err) => badRequest(err)
              }
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
        else if (body.email.exists(e => !EmailValidation.isValid(e.trim))) badRequest("invalid email")
        else if (body.password.exists(p => PasswordPolicy.validate(p).isLeft))
          badRequest(body.password.flatMap(p => PasswordPolicy.validate(p).left.toOption).getOrElse("invalid password"))
        else
          dao.updateUser(id, body.email.map(_.trim), body.password.map(PasswordHasher.hash), body.isAdmin) match {
            case Right(user) =>
              val roleNote = body.isAdmin.map(v => s" isAdminChanged=$v").getOrElse("")
              SecurityLog.adminAction(admin.id, "update_user", user.id, roleNote.trim)
              cask.Response(upickle.default.write(toView(user)), headers = jsonHeaders)
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
      else if (dao.deleteUser(id)) {
        SecurityLog.adminAction(admin.id, "delete_user", id)
        cask.Response(upickle.default.write(Map("status" -> "ok")), headers = jsonHeaders)
      } else notFound
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
