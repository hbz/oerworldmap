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
  "commons-io" % "commons-io" % "2.4",
  "org.elasticsearch" % "elasticsearch" % "1.3.6",
  "org.json" % "json" % "20141113",
  "org.pegdown" % "pegdown" % "1.5.0",
  "com.github.jknack" % "handlebars" % "4.0.5",
  "org.eclipse.jetty" % "jetty-util" % "8.1.12.v20130726",
  "org.apache.jena" % "apache-jena-libs" % "2.13.0",
  "io.apigee.trireme" % "trireme-kernel" % "0.8.8",
  "io.apigee.trireme" % "trireme-core" % "0.8.8",
  "io.apigee.trireme" % "trireme-node10src" % "0.8.8"
)

includeFilter in (Assets, LessKeys.less) := "main.less"
