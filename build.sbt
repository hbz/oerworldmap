import com.typesafe.sbt.less.Import.LessKeys

name := "oerworldmap"

version := "0.1"

scalaVersion := "2.11.7"

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  cache,
  javaWs,
  filters,
  "commons-validator" % "commons-validator" % "1.4.0",
  "com.github.fge" % "jackson-coreutils" % "1.8",
  "com.github.fge" % "json-schema-validator" % "2.2.6",
  "com.googlecode.json-simple" % "json-simple" % "1.1",
  "org.apache.commons" % "commons-email" % "1.3.3",
  "commons-io" % "commons-io" % "2.4",
  "org.elasticsearch" % "elasticsearch" % "2.4.0",
  "org.json" % "json" % "20141113",
  "org.pegdown" % "pegdown" % "1.5.0",
  "com.github.jknack" % "handlebars" % "4.0.6",
  "com.github.jknack" % "handlebars-markdown" % "4.0.6",
  "org.eclipse.jetty" % "jetty-util" % "8.1.12.v20130726",
  "org.apache.jena" % "apache-jena-libs" % "2.13.0",
  "io.apigee.trireme" % "trireme-kernel" % "0.8.8",
  "io.apigee.trireme" % "trireme-core" % "0.8.8",
  "io.apigee.trireme" % "trireme-node10src" % "0.8.8",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
  "junit" % "junit" % "4.11"
)

includeFilter in (Assets, LessKeys.less) := "main.less"

javaOptions in Test += "-Dconfig.file=conf/test.conf"
javaOptions in Test += "-Xmx3G"
