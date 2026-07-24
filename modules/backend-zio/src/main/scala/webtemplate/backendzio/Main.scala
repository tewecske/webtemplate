package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.{ApiError, CreateEntryRequest}
import webtemplate.shared.db.SqliteEntryDao

object Main extends ZIOAppDefault {

  private val dao = SqliteEntryDao("data/zio.sqlite")

  private val routes: Routes[Any, Response] = Routes(
    Method.GET / "health" -> handler(Response.json(upickle.default.write(Map("status" -> "ok")))),

    Method.POST / "entries" -> handler { (req: Request) =>
      (for {
        bodyStr <- req.body.asString
        parsed  <- ZIO.attempt(upickle.default.read[CreateEntryRequest](bodyStr))
        resp <-
          if (parsed.inputId.trim.isEmpty || parsed.value.trim.isEmpty)
            ZIO.succeed(
              Response.json(upickle.default.write(ApiError("inputId and value must not be blank"))).status(Status.BadRequest)
            )
          else
            ZIO.attemptBlocking(dao.insert(parsed.inputId, parsed.value))
              .map(entry => Response.json(upickle.default.write(entry)).status(Status.Created))
      } yield resp).catchAll { e =>
        ZIO.succeed(
          Response.json(upickle.default.write(ApiError(s"invalid request: ${e.getMessage}"))).status(Status.BadRequest)
        )
      }
    },

    Method.GET / "entries" / string("inputId") -> handler { (inputId: String, _: Request) =>
      ZIO.attemptBlocking(dao.history(inputId))
        .map(entries => Response.json(upickle.default.write(entries)))
        .catchAll { _ =>
          ZIO.succeed(
            Response.json(upickle.default.write(ApiError("internal error"))).status(Status.InternalServerError)
          )
        }
    }
  )

  override val run: ZIO[Any, Any, Any] =
    Server.serve(routes).provide(Server.defaultWithPort(8081))
}
