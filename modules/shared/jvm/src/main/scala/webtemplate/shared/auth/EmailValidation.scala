package webtemplate.shared.auth

/** Conservative email format check. Beyond basic shape validation, this exists to stop
  * markup/control characters from ever reaching storage, since email addresses are rendered
  * in admin UI. See CWE-79.
  */
object EmailValidation {
  private val forbiddenChars = Set('<', '>', '"', '\'', '\\')

  def isValid(email: String): Boolean = {
    if (email.isEmpty || email.exists(c => forbiddenChars.contains(c) || c.isWhitespace || c.isControl)) false
    else
      email.split("@", -1) match {
        case Array(local, domain) =>
          local.nonEmpty && domain.nonEmpty && domain.contains('.') &&
          !domain.startsWith(".") && !domain.endsWith(".")
        case _ => false
      }
  }
}
