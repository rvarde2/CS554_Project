import sbt._

updateOptions := updateOptions.value.withGigahorse(false)

lazy val base = (project in file("."))
  .aggregate(
    common,
    application
  )
  .settings(CommonSettings.commonSettings: _*)

lazy val common = project.settings(CommonSettings.commonSettings: _*)

lazy val application = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")
  .enablePlugins(CinnamonAgentOnly)

(Compile / runMain) := ((Compile / runMain) in application).evaluated

onLoad in Global := (onLoad in Global).value andThen (Command.process("project application", _))

// https://mvnrepository.com/artifact/org.yaml/snakeyaml
libraryDependencies += "org.yaml" % "snakeyaml" % "1.8"