import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import org.scalajs.linker.interface.ModuleKind

val scala3Version   = "3.3.8"
val upickleVersion  = "4.4.3"
val sqliteJdbcVer   = "3.53.2.0"
val pureconfigVer   = "0.17.10"
val jbcryptVer      = "0.4"

ThisBuild / scalaVersion := scala3Version
ThisBuild / organization := "com.example.webtemplate"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/shared"))
  .settings(
    name := "shared",
    libraryDependencies += "com.lihaoyi" %%% "upickle" % upickleVersion
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.xerial" % "sqlite-jdbc" % sqliteJdbcVer,
      "com.github.pureconfig" %% "pureconfig-core" % pureconfigVer,
      "org.mindrot" % "jbcrypt" % jbcryptVer
    )
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS  = shared.js

lazy val backendCask = project.in(file("modules/backend-cask"))
  .dependsOn(sharedJVM)
  .settings(
    name := "backend-cask",
    libraryDependencies += "com.lihaoyi" %% "cask" % "0.11.3",
    Compile / mainClass := Some("webtemplate.backendcask.Main"),
    Compile / run / baseDirectory := (ThisBuild / baseDirectory).value,
    reStart / baseDirectory := (ThisBuild / baseDirectory).value
  )

lazy val backendZio = project.in(file("modules/backend-zio"))
  .dependsOn(sharedJVM)
  .settings(
    name := "backend-zio",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % "2.1.26",
      "dev.zio" %% "zio-http" % "3.11.2"
    ),
    Compile / mainClass := Some("webtemplate.backendzio.Main"),
    Compile / run / baseDirectory := (ThisBuild / baseDirectory).value,
    reStart / baseDirectory := (ThisBuild / baseDirectory).value
  )

lazy val frontend = project.in(file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .dependsOn(sharedJS)
  .settings(
    name := "frontend",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule)),
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0",
    // Points at the repo-root npm project (installed via `npm install`) rather than a second
    // one just for sbt. This also has to be where the actual npm packages consumed from
    // Scala.js live (not web/'s node_modules): the fastLinkJS output sits under
    // modules/frontend/target/..., and Vite/esbuild's module resolution walks up from an
    // importing file's real disk location looking for node_modules — the repo root is an
    // ancestor of both modules/frontend/target/... and web/, but web/ is only a sibling of
    // modules/frontend/, so a package installed solely in web/node_modules is unreachable from
    // the emitted Scala.js bundle.
    externalNpm := (ThisBuild / baseDirectory).value
  )

lazy val root = project.in(file("."))
  .aggregate(sharedJVM, sharedJS, backendCask, backendZio, frontend)
  .settings(
    name := "webtemplate",
    publish / skip := true
  )
