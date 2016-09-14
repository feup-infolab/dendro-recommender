name := "Recommender"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.apache.mahout" % "mahout-core" % "0.9",
  "org.apache.mahout" % "mahout-distribution" % "0.9"
)

resolvers += "MVN Repository at" at "http://mvnrepository.com/"


play.Project.playJavaSettings
