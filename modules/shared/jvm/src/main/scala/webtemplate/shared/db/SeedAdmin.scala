package webtemplate.shared.db

import webtemplate.shared.auth.PasswordHasher

/** Dev-only bootstrap: ensures a known admin account exists, gated by
  * AuthConfig.seedAdminEnabled (mirrors the /auth/google-dev stub's per-environment
  * config gating). Idempotent, so it's safe to call on every process startup.
  */
object SeedAdmin {
  def ensure(dao: AuthDao, email: String, password: String): Unit =
    dao.findByEmail(email) match {
      case None                  => dao.createUserWithPassword(email, PasswordHasher.hash(password), isAdmin = true); ()
      case Some(u) if !u.isAdmin => dao.updateUser(u.id, None, None, Some(true)); ()
      case Some(_)                => ()
    }
}
