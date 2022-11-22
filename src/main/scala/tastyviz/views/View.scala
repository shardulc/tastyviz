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

/*
to implement:

.on('changed.jstree', function (e, action) {
	$(treeSymbolInfoID).html("");
	action.selected.forEach(function (id) {
	    var selectedSymbol = $('#' + id);
	    if (selectedSymbol.attr('tv-fullName')) {
		$.get('/symbolInfo/' + selectedSymbol.attr('tv-fullName'), function (data) {
		    $(treeSymbolInfoID).append(data);
		});
	    }
	});
    })
*/

class View(
    classpath: List[String],
    onClickPackageDeclaration: Symbol => Unit,
    onClickPackageParent: () => Unit)(using Context):

  val printer = PrettyPrinter()
  val defTreeView = DefTreeView(printer)
  val packageView = PackageView(
    classpath,
    onClickPackageDeclaration,
    onClickPackageParent)

  def initialize() =
    $(ViewDivs.topLevelDivs).hide()

  def showDefTree(model: TastyDefTreeModel) =
    defTreeView.showDefTree(model)

  def showPackage(model: TastyPackageModel) =
    packageView.showPackage(model)
