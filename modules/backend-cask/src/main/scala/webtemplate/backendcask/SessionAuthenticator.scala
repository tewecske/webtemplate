package webtemplate.backendcask

import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.{AuthDao, UserRecord}

sealed trait AdminAuthResult
object AdminAuthResult {
  case object Unauthenticated extends AdminAuthResult
  case object Forbidden extends AdminAuthResult
  final case class Authorized(user: UserRecord) extends AdminAuthResult
}

final class SessionAuthenticator(dao: AuthDao, config: AuthConfig) {
  def authenticate(request: cask.Request): Option[UserRecord] =
    request.cookies.get(config.sessionCookieName).flatMap(c => dao.currentUser(c.value))

  def authenticateAdmin(request: cask.Request): AdminAuthResult =
    authenticate(request) match {
      case None                        => AdminAuthResult.Unauthenticated
      case Some(user) if !user.isAdmin => AdminAuthResult.Forbidden
      case Some(user)                  => AdminAuthResult.Authorized(user)
    }
}
