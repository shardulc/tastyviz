package tastyviz.views

import scala.collection.mutable

import org.querki.jquery._
import scalatags.JsDom.all.*

import jstreefacade.*

import scalajs.js.annotation.*
import scalajs.js
import tastyquery.Contexts.*
import tastyquery.Symbols.*
import java.util.TimerTask

import ViewConstants.*
import tastyviz.models.*


class View(
    classpath: List[String],
    onClickPackageDeclaration: TastySymbolModel => Unit,
    onClickPackageParent: () => Unit,
    onClickSymbol: TastySymbolModel => Unit)(using Context):

  val printer = PrettyPrinter()
  val defTreeView = DefTreeView(printer)
  val packageView = PackageView(
    classpath,
    onClickPackageDeclaration,
    onClickPackageParent)
  val symbolInfoView = SymbolInfoView(onClickSymbol)

  def reset() =
    $(ViewDivs.topLevelDivs).hide()

  def showDefTree(model: TastyDefTreeModel) =
    defTreeView.showDefTree(model)
    symbolInfoView.reset()

  def showPackage(model: TastyPackageModel) =
    packageView.showPackage(model)

  def showSymbolInfo(model: TastySymbolModel) =
    symbolInfoView.showSymbolInfo(model)
