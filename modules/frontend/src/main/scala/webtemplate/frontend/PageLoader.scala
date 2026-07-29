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

  private val htmlCache = scala.collection.mutable.Map.empty[String, Future[String]]
  private val mountedUrl = scala.collection.mutable.Map.empty[dom.Element, String]

  def ensureLoaded(url: String, mount: dom.Element)(onFirstLoad: => Unit): Future[Unit] =
    if (mountedUrl.get(mount).contains(url)) Future.successful(())
    else
      htmlCache.getOrElseUpdate(url, dom.fetch(url).flatMap(_.text())).map { html =>
        mount.innerHTML = html
        mountedUrl(mount) = url
        onFirstLoad
      }
}
