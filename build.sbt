name := "oerworldmap"

version := "0.1"

lazy val specs2core = "org.specs2" %% "specs2-core" % "2.4.14"

lazy val root = (project in file(".")).
  enablePlugins(PlayJava).
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*).
  settings(
    libraryDependencies += specs2core % "it,test"
  )

scalaVersion := "2.11.7"

routesGenerator := InjectedRoutesGenerator

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
  "org.apache.jena" % "apache-jena-libs" % "3.1.1",
  "io.apigee.trireme" % "trireme-kernel" % "0.8.9",
  "io.apigee.trireme" % "trireme-core" % "0.8.9",
  "io.apigee.trireme" % "trireme-node10src" % "0.8.9",
  "com.maxmind.geoip2" % "geoip2" % "2.8.0"
)

PlayKeys.playRunHooks += Grunt(baseDirectory.value)

javaOptions in Test += "-Dconfig.file=conf/test.conf"
javaOptions in Test += "-Xmx3G"
