package webtemplate.shared

final case class CreateEntryRequest(inputId: String, value: String)

object CreateEntryRequest {
  implicit val rw: upickle.default.ReadWriter[CreateEntryRequest] = upickle.default.macroRW
}

final case class CreatePage2ItemRequest(name: String, nickname: String)

object CreatePage2ItemRequest {
  implicit val rw: upickle.default.ReadWriter[CreatePage2ItemRequest] = upickle.default.macroRW
}

final case class ApiError(error: String)

object ApiError {
  implicit val rw: upickle.default.ReadWriter[ApiError] = upickle.default.macroRW
}
