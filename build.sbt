val scala3Version = "3.3.1"

val catsCore = "2.10.0"
val catsEffect = "3.5.2"
val log4Cats = "2.6.0"
val logback = "1.4.11"
val fs2Core = "3.9.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "chat",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsCore,
      "org.typelevel" %% "cats-effect" % catsEffect,
      "co.fs2" %% "fs2-core" % fs2Core,
      "co.fs2" %% "fs2-io" % fs2Core,
      "org.typelevel" %% "log4cats-slf4j" % log4Cats,
      "ch.qos.logback" % "logback-classic" % logback,
      "org.scalatest" % "scalatest_3" % "3.2.18" % Test
    ),
  )

Compile / run / fork := true
