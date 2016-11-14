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
  "commons-validator" % "commons-validator" % "1.5.1",
  "com.github.fge" % "jackson-coreutils" % "1.8",
  "com.github.fge" % "json-schema-validator" % "2.2.6",
  "org.apache.commons" % "commons-email" % "1.3.3",
  "commons-io" % "commons-io" % "2.5",
  "org.elasticsearch" % "elasticsearch" % "2.4.0",
  "org.pegdown" % "pegdown" % "1.6.0",
  "com.github.jknack" % "handlebars" % "4.0.6",
  "com.github.jknack" % "handlebars-markdown" % "4.0.6",
  "org.apache.jena" % "apache-jena-libs" % "3.1.0",
  "io.apigee.trireme" % "trireme-kernel" % "0.8.9",
  "io.apigee.trireme" % "trireme-core" % "0.8.9",
  "io.apigee.trireme" % "trireme-node10src" % "0.8.9"
)

includeFilter in (Assets, LessKeys.less) := "main.less"

javaOptions in Test += "-Dconfig.file=conf/test.conf"
javaOptions in Test += "-Xmx3G"
