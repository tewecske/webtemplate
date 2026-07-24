package webtemplate.backendcask

import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.{AuthDao, UserRecord}

final class SessionAuthenticator(dao: AuthDao, config: AuthConfig) {
  def authenticate(request: cask.Request): Option[UserRecord] =
    request.cookies.get(config.sessionCookieName).flatMap(c => dao.currentUser(c.value))
}
