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
    encode: Symbol => String):

  def clear(): Unit = $(ViewDivs.symbolInfoView).empty()

  def initialize(): Unit =
    $(ViewDivs.defTreeView).on("changed.jstree", { (event, data) =>
      onSelectionChange(
        data.asInstanceOf[js.Dynamic]
          .selected.asInstanceOf[js.Array[String]]
          .toSeq
          .map(ViewUtils.thisJSTree.get_node(_).data.getSymbol
            .asInstanceOf[js.UndefOr[js.Function0[Symbol]]]
            .map(_.apply()).toOption)
          .collect { case Some(s) => s })
    })

  def displaySymbolInfo(model: TastySymbolModel): Unit =
    $(ViewDivs.symbolInfoView).append(
      buildSymbolInfoHtml(model).render.outerHTML)

  private def buildSymbolInfoHtml(model: TastySymbolModel) =
    val fullNameLinks = Seq.unfold[Symbol, Option[Symbol]](Some(model.symbol))
        (s => s.map(ss => (ss, if ss.owner == null then None else Some(ss.owner))))
      .reverse
      .map(sub => a(
        href := encode(sub),
        sub.name.toString,
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
      div(
        span("type:", `class` := ViewStyles.symbolInfoBoxDesc),
        span(model.tpe.fold("(this is not a TermSymbol)")(ViewUtils.prettyPrintType)),
      ),
    )
