enablePlugins(ScalaJSPlugin)

ThisBuild / scalaVersion := "3.1.3"

val rtJarOpt = taskKey[Option[String]]("Path to rt.jar if it exists")
val javalibEntry = taskKey[String]("Path to rt.jar or \"jrt:/\"")
val copyToWWW = taskKey[Unit]("copy jars to www")

lazy val root = project.in(file("."))
  .settings(
    Global / onChangedBuildSource := ReloadOnSourceChanges,
    name := "tastyviz",
    scalaJSUseMainModuleInitializer := true,
    // scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },

    libraryDependencies ++= Seq(
      "ch.epfl.scala" %%% "tasty-query" % "0.2.0",
      // "ch.epfl.scala" %%% "tasty-query" % "0.0.0+555-aeac5431+20221110-1518-SNAPSHOT",
      "com.lihaoyi" %%% "scalatags" % "0.11.0",
      "org.querki" %%% "jquery-facade" % "2.1",
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
    Compile / javalibEntry := {
      val rtJar = (Compile / rtJarOpt).value

      val s = streams.value
      val targetRTJar = target.value / "extracted-rt.jar"

      rtJar.getOrElse {
        if (!targetRTJar.exists()) {
          s.log.info(s"Extracting jrt:/modules/java.base/ to $targetRTJar")
          extractRTJar(targetRTJar)
        }
        targetRTJar.getAbsolutePath()
      }
    },
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "generated" / "JavaClasspaths.scala"
      val q = "\""
      val cpList = "List(" + (Compile / managedClasspath).value.seq
        .map(_.data.absolutePath)
        .filter(_.contains("org/scala-lang/"))
        .+:((Compile / javalibEntry).value)
        .map(path => path.split("/").last)
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
    Compile / copyToWWW := {
      import java.nio.file.*

      val targetPath = target.value.toPath()
        .resolve("scala-" + scalaVersion.value)
        .resolve(name.value + "-fastopt")
      val output = (Compile / fastLinkJS).value
      output.data.publicModules.foreach{ module =>
        Files.copy(
          targetPath.resolve(module.jsFileName),
          Path.of("www", module.jsFileName),
          StandardCopyOption.REPLACE_EXISTING)
        module.sourceMapName.foreach(n => Files.copy(
          targetPath.resolve(n),
          Path.of("www", n),
          StandardCopyOption.REPLACE_EXISTING))
      }
      val javalibPath = Path.of((Compile / javalibEntry).value)
      Files.copy(
        javalibPath,
        Path.of("www", javalibPath.getFileName().toString()),
        StandardCopyOption.REPLACE_EXISTING)
    },
  )


def extractRTJar(targetRTJar: File): Unit = {
  import java.io.{IOException, FileOutputStream}
  import java.nio.file.{Files, FileSystems}
  import java.util.zip.{ZipEntry, ZipOutputStream}

  import scala.jdk.CollectionConverters._
  import scala.util.control.NonFatal

  val fs = FileSystems.getFileSystem(java.net.URI.create("jrt:/"))

  val zipStream = new ZipOutputStream(new FileOutputStream(targetRTJar))
  try {
    val javaBasePath = fs.getPath("modules", "java.base")
    Files.walk(javaBasePath).forEach({ p =>
      if (Files.isRegularFile(p)) {
        try {
          val data = Files.readAllBytes(p)
          val outPath = javaBasePath.relativize(p).iterator().asScala.mkString("/")
          val ze = new ZipEntry(outPath)
          zipStream.putNextEntry(ze)
          zipStream.write(data)
        } catch {
          case NonFatal(t) =>
            throw new IOException(s"Exception while extracting $p", t)
        }
      }
    })
  } finally {
    zipStream.close()
  }
}
