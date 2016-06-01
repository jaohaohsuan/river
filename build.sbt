import com.typesafe.sbt.packager.docker._
import ReleaseTransformations._

name := "river"

organization := "com.inu"

scalaVersion := "2.11.6"

maintainer := "Henry Jao"

crossScalaVersions := Seq("2.10.6", "2.11.6")

fork := true

packageName in Docker := packageName.value

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "river"

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
  //"org.elasticsearch"   %  "elasticsearch"  % "2.3.1",
  "org.scalatest"       %% "scalatest"      % "2.2.6"   % "test",
  "org.scalacheck"      %% "scalacheck"     % "1.13.0"  % "test",
  "io.spray"            %%  "spray-can"     % sprayV,
  "io.spray"            %%  "spray-httpx"   % sprayV,
  "io.spray"            %%  "spray-routing" % sprayV,
  "io.spray"            %%  "spray-testkit" % sprayV    % "test",
  "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
  "com.typesafe.akka"   %%  "akka-testkit"  % akkaV     % "test",
  "org.specs2"          %%  "specs2-core"   % "2.3.11"  % "test",
  "org.typelevel"       %%  "cats"          % "0.6.0",
  "org.json4s"          %% "json4s-native"  % "3.3.0",
  "org.json4s"          %% "json4s-ext"     % "3.3.0",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0"
)

scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked")

initialCommands := "import com.inu.river._"

enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning, GitBranchPrompt, BuildInfoPlugin)

packageName := "river"

dockerRepository := Some("127.0.0.1:5000/inu")

dockerCommands := Seq(
  Cmd("FROM", "java:8-jdk-alpine"),
  Cmd("MAINTAINER", maintainer.value),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
  Cmd("WORKDIR", "/opt/docker"),
  Cmd("ADD", "opt /opt"),
  ExecCmd("RUN", "chown", "-R", "daemon:daemon", "."),
  Cmd("USER", "daemon"),
  Cmd("ENTRYPOINT", s"bin/${packageName.value}")
)

git.useGitDescribe := true

git.baseVersion := "0.1.0"

sources in EditSource <++= baseDirectory.map(d => (d / "deployment" ** "*.yml").get)

targetDirectory in EditSource <<= baseDirectory(_ / "target")

flatten in EditSource := false

variables in EditSource <+= version { version => ("version", version )}

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  releaseStepTask(org.clapper.sbt.editsource.EditSourcePlugin.autoImport.clean in EditSource),
  releaseStepTask(org.clapper.sbt.editsource.EditSourcePlugin.autoImport.edit in EditSource)
)


