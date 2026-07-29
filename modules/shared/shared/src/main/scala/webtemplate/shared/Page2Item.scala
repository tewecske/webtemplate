package webtemplate.shared

final case class Page2Item(name: String, nickname: String, createdAt: Long)

object Page2Item {
  implicit val rw: upickle.default.ReadWriter[Page2Item] = upickle.default.macroRW
}
