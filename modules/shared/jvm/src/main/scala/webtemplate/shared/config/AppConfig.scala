package webtemplate.shared.config

import pureconfig._

final case class AppConfig(host: String, port: Int, sqlitePath: String) derives ConfigReader

object AppConfig {

  /** Loads `application.conf` from the classpath, under the `app` namespace.
    * Select a different environment by overriding which file is loaded,
    * e.g. `-Dconfig.resource=application-prod.conf` (a JVM system property
    * understood directly by the underlying Typesafe Config library).
    */
  def load(): AppConfig =
    ConfigSource.default.at("app").loadOrThrow[AppConfig]
}
