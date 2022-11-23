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
    onSelectionChange: Seq[Symbol] => Unit,
    onBackToPackage: () => Unit)(using Context):

  private val defTreeView = DefTreeView(
    onSelectionChange,
    onBackToPackage)
  private val packageView = PackageView(
    classpath,
    onClickPackageDeclaration,
    onClickPackageParent)
  

  def showPackageView() =
    $(ViewDivs.topLevelDivs).hide()
    $(ViewDivs.packageView).show()

  def showDefTreeView() =
    $(ViewDivs.topLevelDivs).hide()
    $(ViewDivs.treeControl).add(ViewDivs.treeDisplay).show()

  def clearAndDisplayDefTree(model: TastyDefTreeModel) =
    defTreeView.clear()
    defTreeView.displayDefTree(model)

  def clearAndDisplayPackage(model: TastyPackageModel) =
    packageView.clear()
    packageView.displayPackage(model)

  def clearSymbolInfo() = defTreeView.clearSymbolInfo()
  def displaySymbolInfo(model: TastySymbolModel) = defTreeView.displaySymbolInfo(model)
