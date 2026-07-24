import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import org.scalajs.linker.interface.ModuleKind

val scala3Version   = "3.3.8"
val upickleVersion  = "4.4.3"
val sqliteJdbcVer   = "3.53.2.0"
val pureconfigVer   = "0.17.10"

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
      "com.github.pureconfig" %% "pureconfig-core" % pureconfigVer
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
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    name := "frontend",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule)),
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0"
  )

lazy val root = project.in(file("."))
  .aggregate(sharedJVM, sharedJS, backendCask, backendZio, frontend)
  .settings(
    name := "webtemplate",
    publish / skip := true
  )
