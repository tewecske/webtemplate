package webtemplate.shared.auth

/** See CWE-521 / CWE-1284. The upper bound is pure input hygiene: bcrypt's effective input
  * is capped at 72 bytes (BCrypt.hashpw truncates silently rather than erroring past that),
  * so 128 doesn't change hashing correctness — it just stops pathological input from being
  * fed into it.
  */
object PasswordPolicy {
  private val MinLength = 8
  private val MaxLength = 128

  def validate(password: String): Either[String, Unit] =
    if (password.length < MinLength) Left(s"password must be at least $MinLength characters")
    else if (password.length > MaxLength) Left(s"password must be at most $MaxLength characters")
    else Right(())
}
