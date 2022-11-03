ThisBuild / scalaVersion := "3.1.3"

val rtJarOpt = taskKey[Option[String]]("Path to rt.jar if it exists")
val javalibEntry = taskKey[String]("Path to rt.jar or \"jrt:/\"")

lazy val root = project.in(file("."))
  .settings(
    Global / onChangedBuildSource := ReloadOnSourceChanges,
    name := "tastyviz",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "cask" % "0.8.3",
      "ch.epfl.scala" %% "tasty-query" % "0.2.0",
      "com.lihaoyi" %% "scalatags" % "0.11.0",
    ),

    Compile / rtJarOpt := {
      for (bootClasspath <- Option(System.getProperty("sun.boot.class.path"))) yield {
        val rtJarOpt = bootClasspath.split(java.io.File.pathSeparatorChar).find { path =>
          new java.io.File(path).getName() == "rt.jar"
        }
        rtJarOpt match {
          case Some(rtJar) =>
            rtJar
          case None =>
            throw new AssertionError(s"cannot find rt.jar in $bootClasspath")
        }
      }
    },
    Compile / javalibEntry := (Compile / rtJarOpt).value.getOrElse("jrt:/modules/java.base/"),
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "generated" / "JavaClasspaths.scala"
      val q = "\""
      val cpList = "List(" + (Compile / managedClasspath).value.seq
        .map(_.data.absolutePath)
        .filter(_.contains("org/scala-lang/"))
        .+:((Compile / javalibEntry).value)
        .map(s => s"${q}${s}${q}")
        .reduce((s1, s2) => s"${s1}, ${s2}") + ")"
      IO.write(file, s"""
package tastyviz.generated

object JavaClasspaths {
  val classpaths = ${cpList}
}
""")
      Seq(file)
    },
  )
