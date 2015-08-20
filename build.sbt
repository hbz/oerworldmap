name := "oerworldmap"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "commons-validator" % "commons-validator" % "1.4.0",
  "com.github.fge" % "jackson-coreutils" % "1.8",
  "com.github.fge" % "json-schema-validator" % "2.2.6",
  "com.googlecode.json-simple" % "json-simple" % "1.1",
  "org.apache.commons" % "commons-email" % "1.3.3",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "org.elasticsearch" % "elasticsearch" % "1.3.6",
  "org.json" % "json" % "20141113",
  "org.pegdown" % "pegdown" % "1.5.0",
  "com.github.jknack" % "handlebars" % "2.2.2",
  "uk.com.robust-it" % "cloning" % "1.9.2"
)

includeFilter in (Assets, LessKeys.less) := "main.less"
