package tastyviz.controller

import concurrent.ExecutionContext.Implicits.global
import collection.mutable.Stack

import tastyquery.Contexts.*
import tastyquery.Symbols.*

import tastyviz.views.*
import tastyviz.models.*

class Controller(classpath: List[String])(using Context):
  val packageStack = Stack.empty[TastyPackageModel]
  val view = View(
    classpath,
    onClickPackageDeclaration,
    onClickPackageParent,
    onClickSymbol)
  val model = Model()

  def initialize() =
    view.reset()
    packageStack.push(model.rootPackage)
    view.showPackage(packageStack.top)

  def onClickPackageDeclaration(model: TastySymbolModel): Unit =
    val decl = packageStack.top.getDeclaration(model.symbol)
    decl match
      case Some(m: TastyPackageModel) =>
        packageStack.push(m)
        view.showPackage(m)
      case Some(m: TastyDefTreeModel) =>
        view.showDefTree(m)
      case _ => ()

  def onClickPackageParent(): Unit =
    if packageStack.length > 1 then packageStack.pop()
    view.showPackage(packageStack.top)

  def onClickSymbol(model: TastySymbolModel): Unit =
    view.showSymbolInfo(model)

object Controller:
  val host = "http://localhost:8080/"
  def main(args: Array[String]): Unit =
    val classpath = tastyviz.generated.JavaClasspaths.classpaths
      .map(path => host + path)
      .appendedAll(UserClasspath.classpath)
    ClasspathLoaders.read(classpath)
      .map(tastyquery.Contexts.init(_))
      .map{ ctx =>
        val controller = Controller(classpath)(using ctx)
        controller.initialize()
      }
