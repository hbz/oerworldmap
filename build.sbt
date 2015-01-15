name := "oerworldmap"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "1.3.6",
  "commons-validator" % "commons-validator" % "1.4.0",
  "org.json" % "json" % "20080701"
)