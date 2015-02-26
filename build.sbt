name := "oerworldmap"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "1.3.6",
  "commons-validator" % "commons-validator" % "1.4.0",
  "org.apache.commons" % "commons-email" % "1.3.3",
  "com.github.fge" % "jackson-coreutils" % "1.8",
  "com.github.fge" % "json-schema-validator" % "2.1.7",
  "org.json" % "json" % "20141113",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "com.googlecode.json-simple" % "json-simple" % "1.1"
)
