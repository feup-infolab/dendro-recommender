name := "Recommender"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.apache.mahout" % "mahout-core" % "0.9",
  "org.apache.mahout" % "mahout-distribution" % "0.9",
  "org.mongodb" % "mongo-java-driver" % "2.12.3",
  "com.google.code.gson" % "gson" % "2.2.4",
  "org.atteo" % "evo-inflector" % "1.2",
  "org.apache.commons" % "commons-collections4" % "4.0",
  "com.google.guava" % "guava" % "14.0",
  "com.opencsv" % "opencsv" % "3.3",
  "org.apache.commons" % "commons-collections4" % "4.0",
  "com.jcraft" % "jsch" % "0.1.52",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.apache.commons" % "commons-math" % "2.2",
  "nz.ac.waikato.cms.weka" % "weka-dev" % "3.7.12",
  "org.codehaus.jettison" % "jettison" % "1.2",
  "com.googlecode.json-simple" % "json-simple" % "1.1"
)

resolvers += "MVN Repository at" at "http://mvnrepository.com/"


play.Project.playJavaSettings
