package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.document

// One compiled bundle (scalaJSUseMainModuleInitializer, no module splitting) is imported by
// every scalajs HTML page, so page dispatch happens at runtime via a data-page attribute
// on <body> rather than via separate entry points.
object Main {
  def main(args: Array[String]): Unit =
    document.body.getAttribute("data-page") match {
      case "entries"     => EntriesPage.setup()
      case "users-list"  => UsersListPage.setup()
      case "user-new"    => UserNewPage.setup()
      case "user-detail" => UserDetailPage.setup()
      case other         => dom.console.warn(s"webtemplate: unknown data-page '$other'")
    }
}
