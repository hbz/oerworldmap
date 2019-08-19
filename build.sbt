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
  "org.elasticsearch" % "elasticsearch" % "6.2.1",
  "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "6.2.1",
  "org.apache.jena" % "apache-jena-libs" % "3.1.1",
  "com.github.jsonld-java" % "jsonld-java" % "0.12.3",
  "com.maxmind.geoip2" % "geoip2" % "2.8.0",
  "org.python" % "jython-standalone" % "2.7.1b2",
  "org.apache.httpcomponents" % "httpclient" % "4.5.5",
  "org.mnode.ical4j" % "ical4j" % "3.0.7"
)

val keycloak = Seq(
  "org.keycloak" % "keycloak-adapter-core" % "3.4.3.Final",
  "org.keycloak" % "keycloak-core" % "3.4.3.Final",
  "org.jboss.logging" % "jboss-logging" % "3.3.0.Final",
  "org.jboss.logging" % "jboss-logging-annotations" % "2.1.0.Final" % "provided",
  "org.jboss.logging" % "jboss-logging-processor" % "2.1.0.Final" % "provided",
  "org.keycloak" % "keycloak-admin-client" % "4.8.3.Final",
  "org.jboss.resteasy" % "resteasy-client" % "3.0.24.Final" excludeAll(
    ExclusionRule("junit", "junit"),
    ExclusionRule("org.jboss.logging"),
    ExclusionRule("net.jcip"),
    ExclusionRule("org.jboss.spec.javax.ws.rs"),
    ExclusionRule("org.jboss.spec.javax.servlet"),
    ExclusionRule("org.jboss.spec.javax.annotation"),
    ExclusionRule("javax.activation"),
    ExclusionRule("commons-io"),
    ExclusionRule("org.apache.httpcomponents")),
  "org.jboss.resteasy" % "resteasy-jaxrs" % "3.0.24.Final" excludeAll(
    ExclusionRule("junit", "junit"),
    ExclusionRule("org.jboss.logging"),
    ExclusionRule("net.jcip"),
    ExclusionRule("org.jboss.spec.javax.ws.rs"),
    ExclusionRule("org.jboss.spec.javax.servlet"),
    ExclusionRule("org.jboss.spec.javax.annotation"),
    ExclusionRule("javax.activation"),
    ExclusionRule("commons-io"),
    ExclusionRule("org.apache.httpcomponents")),
  "org.jboss.resteasy" % "resteasy-jackson2-provider" % "3.0.24.Final" excludeAll(
    ExclusionRule("junit", "junit"),
    ExclusionRule("org.jboss.logging"),
    ExclusionRule("net.jcip"),
    ExclusionRule("org.jboss.spec.javax.ws.rs"),
    ExclusionRule("org.jboss.spec.javax.servlet"),
    ExclusionRule("org.jboss.spec.javax.annotation"),
    ExclusionRule("javax.activation"),
    ExclusionRule("commons-io"),
    ExclusionRule("com.fasterxml.jackson.jaxrs"),
    ExclusionRule("org.apache.httpcomponents")),
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % "2.9.8",
  "org.apache.httpcomponents" % "httpclient" % "4.5.1",
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0",
  "org.jboss.spec.javax.annotation" % "jboss-annotations-api_1.2_spec" % "1.0.2.Final",
  "commons-io" % "commons-io" % "2.6"
)

libraryDependencies ++= keycloak

javaOptions in Test += "-Dconfig.file=conf/test.conf"
javaOptions in Test += "-Xmx3G"
javaOptions in Test += "-Dlogback.configurationFile=conf/logback-test.xml"
javaOptions in Test += "-Duser.timezone=UTC"