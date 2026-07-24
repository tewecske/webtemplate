package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.{ApiError, CreateEntryRequest}
import webtemplate.shared.db.SqliteEntryDao

final class EntryHandlers(dao: SqliteEntryDao, auth: SessionAuthenticator) {

  val health: Response =
    Response.json(upickle.default.write(Map("status" -> "ok")))

  def createEntry(req: Request): ZIO[Any, Nothing, Response] =
    auth.authenticate(req).flatMap {
      case None => unauthorized
      case Some(user) =>
        (for {
          bodyStr <- req.body.asString
          parsed  <- ZIO.attempt(upickle.default.read[CreateEntryRequest](bodyStr))
          resp <-
            if (parsed.inputId.trim.isEmpty || parsed.value.trim.isEmpty)
              ZIO.succeed(
                Response
                  .json(upickle.default.write(ApiError("inputId and value must not be blank")))
                  .status(Status.BadRequest)
              )
            else
              ZIO.attemptBlocking(dao.insert(user.id, parsed.inputId, parsed.value))
                .map(entry => Response.json(upickle.default.write(entry)).status(Status.Created))
        } yield resp).catchAll { e =>
          ZIO.succeed(
            Response.json(upickle.default.write(ApiError(s"invalid request: ${e.getMessage}"))).status(Status.BadRequest)
          )
        }
    }

  def getHistory(inputId: String, req: Request): ZIO[Any, Nothing, Response] =
    auth.authenticate(req).flatMap {
      case None => unauthorized
      case Some(user) =>
        ZIO.attemptBlocking(dao.history(user.id, inputId))
          .map(entries => Response.json(upickle.default.write(entries)))
          .catchAll { _ =>
            ZIO.succeed(
              Response.json(upickle.default.write(ApiError("internal error"))).status(Status.InternalServerError)
            )
          }
    }

  private def unauthorized: ZIO[Any, Nothing, Response] =
    ZIO.succeed(Response.json(upickle.default.write(ApiError("not authenticated"))).status(Status.Unauthorized))
}
