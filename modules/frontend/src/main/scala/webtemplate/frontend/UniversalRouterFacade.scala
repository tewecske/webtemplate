package webtemplate.frontend

import org.scalablytyped.runtime.StObject
import typings.universalRouter.universalRouterMod.{RouterContext, RouterOptions, Routes, UniversalRouter}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** ScalablyTyped infers the JS import specifier "universal-router/universal-router" from the
  * package's .d.ts module declaration, but universal-router@10's package.json `exports` map
  * only publishes the bare "universal-router" specifier (no "/universal-router" subpath) - Vite
  * correctly rejects the inferred one. This overrides just the `@JSImport` annotation; every
  * other generated type (Route, RouteParams, RouterContext, ...) is used unchanged.
  */
@JSImport("universal-router", JSImport.Default)
@js.native
class UniversalRouterImpl[R, C /* <: RouterContext */] protected () extends StObject with UniversalRouter[R, C] {
  def this(routes: Routes[R, C]) = this()
  def this(routes: Routes[R, C], options: RouterOptions[R, C]) = this()
}
