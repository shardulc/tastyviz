package tastyviz.views

import scalajs.js
import scalajs.js.JSConverters.*

import scalatags.JsDom.all.*
import org.querki.jquery.*

import tastyquery.Contexts.Context
import tastyquery.Symbols.*
import ViewConstants.*
import tastyviz.models.TastySymbolModel

class SymbolInfoView(onClickSymbol: TastySymbolModel => Unit):

  def reset()(using Context) =
    $(ViewDivs.defTreeView).on("changed.jstree", { (event, data) =>
      $(ViewDivs.symbolInfoView).empty()
      data.asInstanceOf[js.Dynamic]
        .selected.asInstanceOf[js.Array[String]]
        .toSeq
        .foreach { id =>
          showSymbolInfo(TastySymbolModel(
            ViewUtils.thisJSTree.get_node(id).data.getSymbol
              .asInstanceOf[js.Function0[Symbol]]()))
        }
    })

  def showSymbolInfo(model: TastySymbolModel) =
    $(ViewDivs.symbolInfoView).append(
      buildSymbolInfoHtml(model).render.outerHTML)

  def buildSymbolInfoHtml(model: TastySymbolModel) =
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
