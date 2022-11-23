package tastyviz.views

import scalajs.js
import scalajs.js.JSConverters.*

import scalatags.JsDom.all.*
import org.querki.jquery.*
import tastyquery.Symbols.*

import tastyviz.models.TastySymbolModel
import ViewConstants.*

class SymbolInfoView(onSelectionChange: Seq[Symbol] => Unit):

  def clear() = $(ViewDivs.symbolInfoView).empty()

  def initialize() =
    $(ViewDivs.defTreeView).on("changed.jstree", { (event, data) =>
      onSelectionChange(
        data.asInstanceOf[js.Dynamic]
          .selected.asInstanceOf[js.Array[String]]
          .toSeq
          .map(ViewUtils.thisJSTree.get_node(_).data.getSymbol
            .asInstanceOf[js.Function0[Symbol]]()))
    })

  def displaySymbolInfo(model: TastySymbolModel) =
    $(ViewDivs.symbolInfoView).append(
      buildSymbolInfoHtml(model).render.outerHTML)

  private def buildSymbolInfoHtml(model: TastySymbolModel) =
    div(
      `class` := ViewStyles.symbolInfoBox,
      div(
        span("fully qual'd name:", `class` := ViewStyles.symbolInfoBoxDesc),
        span(model.fullName.toString, `class` := ViewStyles.treeSymbol),
      ),
      div(
        span("flags:", `class` := ViewStyles.symbolInfoBoxDesc),
        span(FlagsPrinter.print(model.flags)),
      ),
    )
