package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.{AuthDao, UserRecord}

final class SessionAuthenticator(dao: AuthDao, config: AuthConfig) {
  def authenticate(req: Request): ZIO[Any, Nothing, Option[UserRecord]] =
    req.cookie(config.sessionCookieName) match {
      case Some(cookie) => ZIO.attemptBlocking(dao.currentUser(cookie.content)).orElseSucceed(None)
      case None         => ZIO.succeed(None)
    }
}
