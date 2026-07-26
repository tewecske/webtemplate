package webtemplate.frontend

import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Thenable.Implicits._

/** Fetches a static HTML fragment (served from web/public/pages/) and injects it into a mount
  * element the first time it's needed, caching the Future so repeat calls for the same url
  * neither re-fetch nor re-run wiring (also guards against a double-fetch race if the same
  * route resolves twice before the first fetch completes).
  */
object PageLoader {

  private val cache = scala.collection.mutable.Map.empty[String, Future[Unit]]

  def ensureLoaded(url: String, mount: dom.Element)(onFirstLoad: => Unit): Future[Unit] =
    cache.getOrElseUpdate(
      url,
      dom.fetch(url).flatMap(_.text()).map { html =>
        mount.innerHTML = html
        onFirstLoad
      }
    )
}
