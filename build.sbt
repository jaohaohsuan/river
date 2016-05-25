name := "river"

organization := "com.grandsys"

version := "0.1.0"

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.6", "2.11.6")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "spray repo" at "http://repo.spray.io",
  "spray nightlies repo" at "http://nightlies.spray.io"
)
val akkaV = "2.3.9"
val sprayV = "1.3.3"

libraryDependencies ++= Seq(
  "org.elasticsearch"   %  "elasticsearch"  % "2.3.1",
  "org.scalatest"       %% "scalatest"      % "2.2.6"   % "test",
  "org.scalacheck"      %% "scalacheck"     % "1.13.0"  % "test",
  "io.spray"            %%  "spray-can"     % sprayV,
  "io.spray"            %%  "spray-routing" % sprayV,
  "io.spray"            %%  "spray-testkit" % sprayV    % "test",
  "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
  "com.typesafe.akka"   %%  "akka-testkit"  % akkaV     % "test",
  "org.specs2"          %%  "specs2-core"   % "2.3.11"  % "test"
)

scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yinline-warnings",
    "-Ywarn-dead-code",
    "-Xfuture")

initialCommands := "import com.grandsys.river._"