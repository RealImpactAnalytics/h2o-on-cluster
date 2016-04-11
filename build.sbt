name := "h2oOnCluster"

version := "1.0"

scalaVersion := "2.10.5"

resolvers ++= Seq(
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
  Classpaths.sbtPluginReleases
)


lazy val sparkVersion = "1.6.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql"     % sparkVersion,
  "org.apache.spark" %% "spark-hive"    % sparkVersion,
  "org.apache.hadoop" % "hadoop-client" % "2.6.0",
  "org.apache.hadoop" % "hadoop-aws" % "2.6.0",
  "ai.h2o" % "sparkling-water-core_2.10" % sparkVersion
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
  case x => MergeStrategy.last
}
