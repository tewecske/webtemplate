package webtemplate.shared.auth

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
  def hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

  def verify(password: String, hash: String): Boolean =
    try BCrypt.checkpw(password, hash)
    catch { case _: IllegalArgumentException => false }
}
