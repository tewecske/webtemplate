package webtemplate.shared

final case class UserView(id: Long, email: String)

object UserView {
  implicit val rw: upickle.default.ReadWriter[UserView] = upickle.default.macroRW
}

final case class SignupRequest(email: String, password: String)

object SignupRequest {
  implicit val rw: upickle.default.ReadWriter[SignupRequest] = upickle.default.macroRW
}

final case class LoginRequest(email: String, password: String)

object LoginRequest {
  implicit val rw: upickle.default.ReadWriter[LoginRequest] = upickle.default.macroRW
}

final case class GoogleDevLoginRequest(email: String)

object GoogleDevLoginRequest {
  implicit val rw: upickle.default.ReadWriter[GoogleDevLoginRequest] = upickle.default.macroRW
}
