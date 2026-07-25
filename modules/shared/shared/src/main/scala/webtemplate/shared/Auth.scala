package webtemplate.shared

final case class UserView(id: Long, email: String, isAdmin: Boolean)

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

final case class AdminUserView(id: Long, email: String, isAdmin: Boolean, createdAt: Long)

object AdminUserView {
  implicit val rw: upickle.default.ReadWriter[AdminUserView] = upickle.default.macroRW
}

final case class CreateUserRequest(email: String, password: String, isAdmin: Boolean)

object CreateUserRequest {
  implicit val rw: upickle.default.ReadWriter[CreateUserRequest] = upickle.default.macroRW
}

final case class UpdateUserRequest(
    email: Option[String] = None,
    password: Option[String] = None,
    isAdmin: Option[Boolean] = None
)

object UpdateUserRequest {
  implicit val rw: upickle.default.ReadWriter[UpdateUserRequest] = upickle.default.macroRW
}
