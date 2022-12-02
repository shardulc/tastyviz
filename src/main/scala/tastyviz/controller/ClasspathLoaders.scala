package tastyviz.controller

import scala.collection.mutable
import scala.concurrent.*
import scala.util.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.typedarray.*
import scala.scalajs.js.Thenable.Implicits.*

import org.scalajs.dom
import org.scalajs.dom.URL

import tastyquery.Classpaths.*

/** Classpath loaders using the JS DOM fetch() API.
  *
  * This API is specific to Scala.js when used in the browser with the
  * scala-js-dom facade. It is not available on the JVM nor in other
  * Scala.js environments.
  *
  * Ensure that jszip.js is available in the browser and that
  * scala-js-dom is added to the project's dependencies.
  */
object ClasspathLoaders:

  /** Reads the contents of a classpath.
    *
    * Entries can only be jar files. Directories throw an exception
    * and non-existing entries are ignored.
    *
    * This method will asynchronously read the contents of all `.class` and
    * `.tasty` files on the classpath. It returns a `Future` that will be
    * completed when all has been read into memory.
    *
    * The resulting [[Classpaths.Classpath]] can be given to [[Contexts.init]]
    * to create a [[Contexts.Context]]. The latter gives semantic access to all
    * the definitions on the classpath.
    *
    * @note the resulting [[Classpaths.Classpath.Entry Classpath.Entry]] entries of
    *       the returned [[Classpaths.Classpath]] correspond to the elements of `classpath`.
    */
  def read(classpath: List[String])(using ExecutionContext): Future[Classpath] =
    val allEntriesFuture = Future
      .traverse(classpath) { entry =>
        if entry.endsWith(".jar") then
          fromRemoteJarFile(entry)
        else
          throw new IllegalArgumentException("Illegal classpath entry: " + entry)
      }
      // .map(_.flatten)

    def compressPackages(allFiles: Seq[FileContent]): Iterable[PackageData] =
      allFiles
        .groupMap[String, ClassData | TastyData](_.packagePath) { fileContent =>
          val isClassFile = fileContent.name.endsWith(".class")
          val binaryName =
            if isClassFile then fileContent.name.stripSuffix(".class")
            else fileContent.name.stripSuffix(".tasty")
          if isClassFile then ClassData(binaryName, fileContent.debugPath, fileContent.content)
          else TastyData(binaryName, fileContent.debugPath, fileContent.content)
        }
        .map { (packagePath, classAndTastys) =>
          val packageName = packagePath.replace('/', '.').nn
          val (classes, tastys) = classAndTastys.partitionMap {
            case classData: ClassData => Left(classData)
            case tastyData: TastyData => Right(tastyData)
          }
          PackageData(packageName, IArray.from(classes.sortBy(_.binaryName)), IArray.from(tastys.sortBy(_.binaryName)))
        }

    for allEntries <- allEntriesFuture yield
      val compressedEntries = allEntries.map(compressPackages andThen IArray.from)
      Classpath.from(compressedEntries)
    /*for allFiles <- allFilesFuture yield
      val allPackageDatas =
        allFiles
          .groupMap[String, ClassData | TastyData](_.packagePath) { fileContent =>
            val isClassFile = fileContent.name.endsWith(".class")
            val binaryName =
              if isClassFile then fileContent.name.stripSuffix(".class")
              else fileContent.name.stripSuffix(".tasty")
            if isClassFile then ClassData(binaryName, fileContent.debugPath, fileContent.content)
            else TastyData(binaryName, fileContent.debugPath, fileContent.content)
          }
          .map { (packagePath, classAndTastys) =>
            val packageName = packagePath.replace('/', '.').nn
            val (classes, tastys) = classAndTastys.partitionMap {
              case classData: ClassData => Left(classData)
              case tastyData: TastyData => Right(tastyData)
            }
            PackageData(
              packageName,
              IArray.from(classes.sortBy(_.binaryName)),
              IArray.from(tastys.sortBy(_.binaryName))
            )
          }
      end allPackageDatas

      Classpath.from(IArray.from(allPackageDatas))*/
    end for
  end read

  private def fromRemoteJarFile(url: String)(using ExecutionContext): Future[List[FileContent]] =
    val parsedURL = URL(url)
    for
      response <- dom.fetch(url)
      data <- response.arrayBuffer()
      zip <- JSZip.loadAsync(data)
      files <- loadFromZip(parsedURL.pathname, zip)
    yield files.toList

  private def loadFromZip(jarPath: String, obj: JSZip.JSZip)(
    implicit ec: ExecutionContext
  ): Future[Iterator[FileContent]] =
    val entries = obj.files.valuesIterator
      .filter(e => isClassOrTasty(e.name) && !e.dir)

    def splitPackagePathAndName(relPath: String): (String, String) =
      val lastSlash = relPath.lastIndexOf('/')
      if lastSlash < 0 then ("", relPath)
      else (relPath.substring(0, lastSlash).nn, relPath.substring(lastSlash + 1).nn)

    Future.traverse(entries) { entry =>
      entry.async(JSZipInterop.arrayBuffer).toFuture.map { buf =>
        val (packagePath, name) = splitPackagePathAndName(entry.name)
        new FileContent(packagePath, name, s"$jarPath:${entry.name}", IArray.from(new Int8Array(buf).toArray))
      }
    }
  end loadFromZip

  private def isClassOrTasty(name: String): Boolean =
    name.endsWith(".class") || name.endsWith(".tasty")

  private class FileContent(val packagePath: String, val name: String, val debugPath: String, val content: IArray[Byte])

  private object MatchableJSException:
    def unapply(x: js.JavaScriptException): Some[Matchable] =
      Some(x.exception.asInstanceOf[Matchable])
  end MatchableJSException

  private object JSZipInterop:
    val arrayBuffer: String = "arraybuffer"
  end JSZipInterop

  @js.native
  @JSGlobal("JSZip")
  private object JSZip extends js.Object:
    trait JSZip extends js.Object:
      val files: js.Dictionary[ZipObject]
    end JSZip

    trait ZipObject extends js.Object:
      val name: String
      val dir: Boolean
      def async(tpe: JSZipInterop.arrayBuffer.type): js.Promise[ArrayBuffer]
    end ZipObject

    def loadAsync(data: Uint8Array): js.Promise[JSZip] = js.native
    def loadAsync(data: ArrayBuffer): js.Promise[JSZip] = js.native
  end JSZip
end ClasspathLoaders
