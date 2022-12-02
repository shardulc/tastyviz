package tastyviz.controller

import concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom.PopStateEvent
import org.scalajs.dom.window

import tastyquery.Names.*
import tastyquery.Contexts.*
import tastyquery.Symbols.*

import tastyviz.views.*
import tastyviz.models.*


class Controller(classpath: List[String])(using Context):

  object State:
    var current: TastyModel = model.rootPackage

    private def encodeName(name: Name) =
      if name.isTypeName then
        if name.toTypeName.wrapsObjectName
        then "tpo" + name.toString
        else "tpn" + name.toString
      else "tm" + name.toString

    def encode(fqn: List[Name]) =
      fqn.map(encodeName).mkString("#", "/", "")

    private def decodeName(name: String) =
      if name == defn.RootPackage.name.toString
      then defn.RootPackage.name
      else
        if name.startsWith("tm") then termName(name.drop(2)) else
        if name.startsWith("tpo") then termName(name.drop(3)).withObjectSuffix.toTypeName else
        if name.startsWith("tpn") then typeName(name.drop(3)) else
        termName(name)

    def decode(hash: String) =
      if hash.length > 0
      then hash.drop(1).split("/").toList.map(decodeName)
      else List.empty

    def show(model: TastyModel, replace: Boolean = false) =
      current = model
      val hash = encode(model.fullName.path)
      if replace then HistoryFacade.replaceState(null, "", hash)
      else HistoryFacade.pushState(null, "", hash)
      model match
        case m: TastyPackageModel =>
          view.clearAndDisplayPackage(m)
          view.showPackageView()
        case m: TastyDefTreeModel =>
          view.clearAndDisplayDefTree(m)
          view.showDefTreeView()
        case _: TastySymbolModel => ()

    def recover(urlNames: List[Name]): Unit =
      val m = (0 to urlNames.length)
        .map(urlNames.take(_))
        .map(model.find)
        .findLast(_.isDefined)
        .map(_.get)
        .getOrElse(model.rootPackage)
      show(m, replace = true)

    def onPopState(event: org.scalajs.dom.PopStateEvent) =
      event.preventDefault()
      recover(decode(window.location.hash))


  val view = View(
    classpath,
    onClickPackageDeclaration,
    onClickOwner,
    onSelectionChange,
    State.encode)
  val model = Model()

  def initialize() =
    window.addEventListener("popstate", (e) => State.onPopState(e))
    State.recover(State.decode(window.location.hash))

  def onClickPackageDeclaration(model: TastyModel): Unit =
    State.current match
      case m: TastyPackageModel =>
        m.getDeclaration(model.symbol).foreach(State.show(_))
      case _ => ()

  def onClickOwner(): Unit =
    State.current.owner.foreach(State.show(_))

  def onSelectionChange(symbols: Seq[Symbol]): Unit =
    view.clearSymbolInfo()
    symbols.foreach(s =>
      view.displaySymbolInfo(TastySymbolModel(s, State.current.owner.get)))

  def onBackToPackage(): Unit =
    println("back 2")
    HistoryFacade.back()


object Controller:
  def main(args: Array[String]): Unit =
    val classpath = tastyviz.generated.JavaClasspaths.classpaths
      .map(path => Config.host + path)
      .appendedAll(Config.classpath)
    ClasspathLoaders.read(classpath)
      .map(tastyquery.Contexts.init(_))
      .map{ ctx =>
        val controller = Controller(classpath)(using ctx)
        controller.initialize()
      }
