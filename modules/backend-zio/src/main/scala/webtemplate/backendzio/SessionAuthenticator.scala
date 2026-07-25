package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.{AuthDao, UserRecord}

sealed trait AdminAuthResult
object AdminAuthResult {
  case object Unauthenticated extends AdminAuthResult
  case object Forbidden extends AdminAuthResult
  final case class Authorized(user: UserRecord) extends AdminAuthResult
}

final class SessionAuthenticator(dao: AuthDao, config: AuthConfig) {
  def authenticate(req: Request): ZIO[Any, Nothing, Option[UserRecord]] =
    req.cookie(config.sessionCookieName) match {
      case Some(cookie) => ZIO.attemptBlocking(dao.currentUser(cookie.content)).orElseSucceed(None)
      case None         => ZIO.succeed(None)
    }

  def authenticateAdmin(req: Request): ZIO[Any, Nothing, AdminAuthResult] =
    authenticate(req).map {
      case None                        => AdminAuthResult.Unauthenticated
      case Some(user) if !user.isAdmin => AdminAuthResult.Forbidden
      case Some(user)                  => AdminAuthResult.Authorized(user)
    }
}
