package webtemplate.shared

final case class Entry(inputId: String, value: String, createdAt: Long)

object Entry {
  implicit val rw: upickle.default.ReadWriter[Entry] = upickle.default.macroRW
}
