package webtemplate.backendcask

import webtemplate.shared.{ApiError, CreateEntryRequest}
import webtemplate.shared.db.SqliteEntryDao

final class EntryHandlers(dao: SqliteEntryDao) {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  def health(): cask.Response[String] =
    cask.Response(upickle.default.write(Map("status" -> "ok")), headers = jsonHeaders)

  def createEntry(request: cask.Request): cask.Response[String] = {
    try {
      val body = upickle.default.read[CreateEntryRequest](request.text())
      if (body.inputId.trim.isEmpty || body.value.trim.isEmpty) {
        cask.Response(
          upickle.default.write(ApiError("inputId and value must not be blank")),
          statusCode = 400,
          headers = jsonHeaders
        )
      } else {
        val entry = dao.insert(body.inputId, body.value)
        cask.Response(upickle.default.write(entry), statusCode = 201, headers = jsonHeaders)
      }
    } catch {
      case e: Exception =>
        cask.Response(
          upickle.default.write(ApiError(s"invalid request: ${e.getMessage}")),
          statusCode = 400,
          headers = jsonHeaders
        )
    }
  }

  def getHistory(inputId: String): cask.Response[String] = {
    try {
      cask.Response(upickle.default.write(dao.history(inputId)), headers = jsonHeaders)
    } catch {
      case _: Exception =>
        cask.Response(upickle.default.write(ApiError("internal error")), statusCode = 500, headers = jsonHeaders)
    }
  }
}
