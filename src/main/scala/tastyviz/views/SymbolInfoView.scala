package tastyviz.views

import scalajs.js
import scalajs.js.JSConverters.*

import scalatags.JsDom.all.*
import org.querki.jquery.*
import tastyquery.Symbols.*

import tastyviz.models.TastySymbolModel
import ViewConstants.*

class SymbolInfoView(
    onSelectionChange: Seq[Symbol] => Unit,
    encode: List[tastyquery.Names.Name] => String):

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
    val fullNameLinks = (1 to model.fullName.path.length)
      .map(model.fullName.path.take(_))
      .map(subpath => a(
        href := encode(subpath),
        subpath.last.toString,
      ))
      .flatMap(a => Seq(a, span(".")))
      .dropRight(1)
    div(
      `class` := ViewStyles.symbolInfoBox,
      div(
        span("fully qual'd name:", `class` := ViewStyles.symbolInfoBoxDesc),
        div(`class` := ViewStyles.treeSymbol)(fullNameLinks: _*)
      ),
      div(
        span("flags:", `class` := ViewStyles.symbolInfoBoxDesc),
        span(FlagsPrinter.print(model.flags)),
      ),
    )
