// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")

// The Mustache repository
resolvers += Resolver.url(
  "bintray-sbt-plugin-michaelallen",
  url("https://dl.bintray.com/michaelallen/sbt-plugins/")
)(Resolver.ivyStylePatterns)

resolvers += "bintray-maven-michaelallen" at "https://dl.bintray.com/michaelallen/maven/"

// Use the Mustache sbt plugin
addSbtPlugin("io.michaelallen.mustache" %% "sbt-mustache" % "0.2")