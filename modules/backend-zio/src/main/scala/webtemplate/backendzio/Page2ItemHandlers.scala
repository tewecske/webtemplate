package webtemplate.backendzio

import zio._
import zio.http._
import webtemplate.shared.{ApiError, CreatePage2ItemRequest}
import webtemplate.shared.db.SqlitePage2ItemDao

final class Page2ItemHandlers(dao: SqlitePage2ItemDao, auth: SessionAuthenticator) {

  val health: Response =
    Response.json(upickle.default.write(Map("status" -> "ok")))

  def createPage2Item(req: Request): ZIO[Any, Nothing, Response] =
    auth.authenticate(req).flatMap {
      case None => unauthorized
      case Some(user) =>
        (for {
          bodyStr <- req.body.asString
          parsed  <- ZIO.attempt(upickle.default.read[CreatePage2ItemRequest](bodyStr))
          resp <-
            if (parsed.name.trim.isEmpty || parsed.nickname.trim.isEmpty)
              ZIO.succeed(
                Response
                  .json(upickle.default.write(ApiError("name and nickname must not be blank")))
                  .status(Status.BadRequest)
              )
            else
              ZIO.attemptBlocking(dao.insert(user.id, parsed.name, parsed.nickname))
                .map(entry => Response.json(upickle.default.write(entry)).status(Status.Created))
        } yield resp).catchAll { e =>
          ZIO.succeed(
            Response.json(upickle.default.write(ApiError(s"invalid request: ${e.getMessage}"))).status(Status.BadRequest)
          )
        }
    }

  def getPage2Items(req: Request): ZIO[Any, Nothing, Response] =
    auth.authenticate(req).flatMap {
      case None => unauthorized
      case Some(user) =>
        ZIO.attemptBlocking(dao.items(user.id))
          .map(page2Items => Response.json(upickle.default.write(page2Items)))
          .catchAll { _ =>
            ZIO.succeed(
              Response.json(upickle.default.write(ApiError("internal error"))).status(Status.InternalServerError)
            )
          }
    }

  private def unauthorized: ZIO[Any, Nothing, Response] =
    ZIO.succeed(Response.json(upickle.default.write(ApiError("not authenticated"))).status(Status.Unauthorized))
}
