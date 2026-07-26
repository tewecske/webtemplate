package webtemplate.backendcask

import webtemplate.shared.{ApiError, CreateEntryRequest}
import webtemplate.shared.config.AuthConfig
import webtemplate.shared.db.SqliteEntryDao

final class EntryHandlers(dao: SqliteEntryDao, auth: SessionAuthenticator, config: AuthConfig) extends CsrfGuard {
  private val jsonHeaders = Seq("Content-Type" -> "application/json") ++ SecurityHeaders(config)

  private def csrfBlockedResponse: cask.Response[String] =
    cask.Response(upickle.default.write(ApiError("cross-site request blocked")), statusCode = 403, headers = jsonHeaders)

  def health(): cask.Response[String] =
    cask.Response(upickle.default.write(Map("status" -> "ok")), headers = jsonHeaders)

  def createEntry(request: cask.Request): cask.Response[String] =
    if (csrfBlocked(request, config)) csrfBlockedResponse
    else
      auth.authenticate(request) match {
        case None => unauthorized
        case Some(user) =>
          try {
            val body = upickle.default.read[CreateEntryRequest](request.text())
            if (body.inputId.trim.isEmpty || body.value.trim.isEmpty) {
              cask.Response(
                upickle.default.write(ApiError("inputId and value must not be blank")),
                statusCode = 400,
                headers = jsonHeaders
              )
            } else {
              val entry = dao.insert(user.id, body.inputId, body.value)
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

  def getHistory(inputId: String, request: cask.Request): cask.Response[String] =
    auth.authenticate(request) match {
      case None => unauthorized
      case Some(user) =>
        try {
          cask.Response(upickle.default.write(dao.history(user.id, inputId)), headers = jsonHeaders)
        } catch {
          case _: Exception =>
            cask.Response(upickle.default.write(ApiError("internal error")), statusCode = 500, headers = jsonHeaders)
        }
    }

  private def unauthorized: cask.Response[String] =
    cask.Response(upickle.default.write(ApiError("not authenticated")), statusCode = 401, headers = jsonHeaders)
}
