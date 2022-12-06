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

  private def fullNameLinks(symbol: Symbol) =
    Seq.unfold[Symbol, Option[Symbol]](Some(symbol))
        (s => s.map(ss => (ss, if ss.owner == null then None else Some(ss.owner))))
      .reverse
      .map(sub => a(
        href := encode(sub),
        sub.name.toString,
      ))
      .flatMap(a => Seq(a, span(".")))
      .dropRight(1)

  private def buildSymbolInfoHtml(model: TastySymbolModel) =
    val nameLinks = fullNameLinks(model.symbol)
    val fullNameDiv = div(
      span("fully qual'd name:", `class` := ViewStyles.symbolInfoBoxDesc),
      div(`class` := ViewStyles.treeSymbol)(nameLinks: _*)
    )

    val flagsDiv = div(
      span("flags:", `class` := ViewStyles.symbolInfoBoxDesc),
      span(FlagsPrinter.print(model.flags)),
    )

    val categories = Seq(
      ("term symbol", model.symbol.isTerm),
      ("type symbol", model.symbol.isType),
      ("class symbol", model.symbol.isClass),
      ("package symbol", model.symbol.isPackage))
    val categoriesDiv = div(
      span("this is a:", `class` := ViewStyles.symbolInfoBoxDesc),
      span(categories.filter(_._2).map(_._1).mkString(", "))
    )

    val tpeDiv = model.tpe.map(t => div(
      span("type:", `class` := ViewStyles.symbolInfoBoxDesc),
      span(ViewUtils.prettyPrintType(t)),
    ))

    val typeBoundsDiv = model.typeBounds.map(b => div(
      span("type bounds:", `class` := ViewStyles.symbolInfoBoxDesc),
      span(Seq(
          ViewUtils.prettyPrintType(b.low),
          model.name,
          ViewUtils.prettyPrintType(b.high))
        .mkString(" <: ")),
    ))

    val typeSymbolsDiv = model.typeSymbols.map(ts => div(
      span("links to types:", `class` := ViewStyles.symbolInfoBoxDesc),
      div(ts.map(fullNameLinks).flatMap(t => Seq(t, span(", "))).dropRight(1): _*)))

    val elem = div(
      `class` := ViewStyles.symbolInfoBox,
      fullNameDiv,
      flagsDiv,
      categoriesDiv
    )
    val maybeTypeElem = tpeDiv.fold(elem)(d => elem(d))
    val maybeBoundsElem = typeBoundsDiv.fold(maybeTypeElem)(d => maybeTypeElem(d))
    val maybeTypeSymbolsElem =
      typeSymbolsDiv.fold(maybeBoundsElem)(d => maybeBoundsElem(d))
    maybeTypeSymbolsElem
