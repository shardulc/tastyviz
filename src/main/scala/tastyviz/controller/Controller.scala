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
    onSelectionChange,
    onBackToPackage)
  val model = Model()

  def initialize() =
    packageStack.push(model.rootPackage)
    view.clearAndDisplayPackage(packageStack.top)
    view.showPackageView()

  def onClickPackageDeclaration(model: TastySymbolModel): Unit =
    val decl = packageStack.top.getDeclaration(model.symbol)
    decl match
      case Some(m: TastyPackageModel) =>
        packageStack.push(m)
        view.clearAndDisplayPackage(m)
        view.showPackageView()
      case Some(m: TastyDefTreeModel) =>
        view.clearAndDisplayDefTree(m)
        view.showDefTreeView()
      case _ => ()

  def onClickPackageParent(): Unit =
    if packageStack.length > 1 then packageStack.pop()
    view.clearAndDisplayPackage(packageStack.top)
    view.showPackageView()

  def onSelectionChange(symbols: Seq[Symbol]): Unit =
    view.clearSymbolInfo()
    symbols.foreach(s => view.displaySymbolInfo(TastySymbolModel(s)))

  def onBackToPackage(): Unit =
    view.clearAndDisplayPackage(packageStack.top)
    view.showPackageView()


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
