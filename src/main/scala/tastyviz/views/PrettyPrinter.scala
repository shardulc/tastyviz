package tastyviz.views

import scala.collection.mutable

import scalatags.JsDom.all.*
import scalatags.JsDom.TypedTag

import tastyquery.Contexts.Context
import tastyquery.Exceptions.MemberNotFoundException
import tastyquery.Symbols.*
import tastyquery.Trees.*
import tastyquery.Flags
import tastyquery.Types.*

import ViewConstants.*
import scala.util.control.NonFatal.apply


class PrettyPrinter(using Context):
  type IDSymbolMap = mutable.Map[ID, Symbol]

  var currentID = 0
  def freshID() =
    currentID += 1
    s"#tv-node$currentID"

  def buildHtml(tree: Tree): (TypedTag[_], IDSymbolMap) =
    val symbols = mutable.Map.empty[ID, Symbol]
    val result = ul(buildHtml(tree, symbols))
    (result, symbols)

  def buildHtml(tree: Tree, symbols: IDSymbolMap): Modifier =
    tree match
      case t: ClassDef => buildHtmlClassDef(t, symbols)
      case t: New => buildHtmlNew(t, symbols)
      case t: Template => buildHtmlTemplate(t, symbols)
      case t: DefDef => buildHtmlDefDef(t, symbols)
      case t: ValDef => buildHtmlValDef(t, symbols)
      case t: Block => buildHtmlBlock(t, symbols)
      case t: Apply => buildHtmlApply(t, symbols)
      case t: Select => buildHtmlSelect(t, symbols)
      case t: Lambda => buildHtmlLambda(t, symbols)
      case t: Typed => buildHtml(t.expr, symbols)
      case t: Ident => buildHtmlIdent(t, symbols)
      case t: Literal => buildHtmlLiteral(t, symbols)
      case t: This => buildHtmlThis(t, symbols)
      case t: Assign => buildHtmlAssign(t, symbols)
      case t @ _ => li(span(`class` := ViewStyles.treeNodeType, t.getClass().getName()))

  def buildHtmlClassDef(tree: ClassDef, symbols: IDSymbolMap) =
    val thisID = freshID()
    symbols(thisID) = tree.symbol
    li(
      id := thisID,
      span(`class` := ViewStyles.treeNodeType, "ClassDef"),
      span(
        `class` := ViewStyles.treeSymbol,
        tree.symbol.asType.name.toString),
      ul(buildHtml(tree.rhs, symbols)),
    )

  def buildHtmlNew(tree: New, symbols: IDSymbolMap) =
    symbolToNode(typeToSymbol(tree.tpe), "New", symbols)

  def buildHtmlTemplate(tree: Template, symbols: IDSymbolMap) =
    val parents = tree.parents.map(_ match
      case t: (Apply | Block) =>
        buildHtml(t, symbols)
      case t: TypeTree =>
        buildHtmlTypeTree(t, symbols)
    )
    li(
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "Template"),
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "constructor"),
          ul(buildHtmlDefDef(tree.constr, symbols)),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "parents"),
          ul(parents: _*),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "self"),
          tree.self.fold(
            span(`class` := ViewStyles.treeNodeDesc, "(none)")
          )(t => ul(buildHtml(t, symbols))),
        ),
        li(
          `class` := "jstree-open",
          span(`class` := ViewStyles.treeNodeDesc, "body"),
          ul(tree.body.map(buildHtml(_, symbols)): _*),
        ),
      )
    )

  def buildHtmlDefDef(tree: DefDef, symbols: IDSymbolMap) =
    val thisID = freshID()
    symbols(thisID) = tree.symbol
    li(
      id := thisID,
      span(`class` := ViewStyles.treeNodeType, "DefDef"),
      span(`class` := ViewStyles.treeSymbol, tree.symbol.asTerm.name.toString),
      span(`class` := ViewStyles.treeType, ": " + ViewUtils.prettyPrintType(tree.symbol.declaredType)),
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "parameters"),
          ul(tree.paramLists.map(buildHtmlParamsClause(_, symbols)): _*),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "right-hand side"),
          tree.rhs.fold(
            span(`class` := ViewStyles.treeNodeDesc, "(none)")
          )(t => ul(buildHtml(t, symbols))),
        ),
      ),
    )

  def buildHtmlParamsClause(params: ParamsClause, symbols: IDSymbolMap) =
    params match
      case Left(p) => li(
        span(`class` := ViewStyles.treeNodeDesc, "term parameters"),
        if p.isEmpty then ul(li(span(`class` := ViewStyles.treeNodeDesc, "(none)")))
        else ul(p.map(buildHtmlValDef(_, symbols, isParameter = true)): _*),
      )
      case Right(p) => li(
        span(`class` := ViewStyles.treeNodeDesc, "type parameters"),
        span("can't handle these yet"),
      )

  def buildHtmlValDef(tree: ValDef, symbols: IDSymbolMap, isParameter: Boolean = false) =
    val thisID = freshID()
    symbols(thisID) = tree.symbol
    val elem = li(
      id := thisID,
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "ValDef"),
      span(`class` := ViewStyles.treeSymbol, tree.symbol.asTerm.name.toString),
    )
    val typeSpan = span(`class` := ViewStyles.treeType,
      ": " + ViewUtils.prettyPrintType(tree.symbol.declaredType))
    tree.rhs.fold(elem(
      span(`class` := ViewStyles.treeNodeDesc, "(no right-hand side)"),
      typeSpan,
    ))(t => elem(
      typeSpan,
      ul(buildHtml(t, symbols))
    ))

  def buildHtmlBlock(tree: Block, symbols: IDSymbolMap) =
    li(
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "Block"),
      ul((tree.stats :+ tree.expr).map(buildHtml(_, symbols)): _*),
    )

  def buildHtmlApply(tree: Apply, symbols: IDSymbolMap) =
    val args = if tree.args.isEmpty then List(li(span(`class` := ViewStyles.treeNodeDesc, "(none)")))
      else tree.args.map(buildHtml(_, symbols))
    li(
      span(`class` := ViewStyles.treeNodeType, "Apply"),
      `class` := "jstree-open",
      ul(
        li(
          `class` := "jstree-open",
          span(`class` := ViewStyles.treeNodeDesc, "function"),
          ul(buildHtml(tree.fun, symbols)),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "arguments"),
          ul(args: _*),
        ),
      )
    )

  def buildHtmlSelect(tree: Select, symbols: IDSymbolMap) =
    val symbolElem = symbolToNode(treeToSymbol(tree), "", symbols)
    li(
      span(`class` := ViewStyles.treeNodeType, "Select"),
      `class` := "jstree-open",
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "qualifier"),
          `class` := "jstree-open",
          ul(buildHtml(tree.qualifier, symbols)),
        ),
        symbolElem,
      )
    )

  def buildHtmlLambda(tree: Lambda, symbols: IDSymbolMap) =
    li(
      span(`class` := ViewStyles.treeNodeType, "Select"),
      `class` := "jstree-open",
      ul(buildHtml(tree.meth, symbols))
    )

  def buildHtmlIdent(tree: Ident, symbols: IDSymbolMap) =
    symbolToNode(treeToSymbol(tree), "Ident", symbols)

  def buildHtmlTypeTree(tree: TypeTree, symbols: IDSymbolMap) =
    symbolToNode(typeToSymbol(tree.toType), "TypeTree", symbols)

  def buildHtmlLiteral(tree: Literal, symbols: IDSymbolMap) =
    li(
      span(`class` := ViewStyles.treeNodeType, "Constant"),
      span(`class` := ViewStyles.treeSymbol, tree.constant.value.toString),
    )

  def buildHtmlThis(tree: This, symbols: IDSymbolMap) =
    li(
      span(`class` := ViewStyles.treeNodeType, "This"),
      ul(buildHtmlTypeTree(tree.qualifier, symbols)),
    )

  def buildHtmlAssign(tree: Assign, symbols: IDSymbolMap) =
    li(
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "Assign"),
      ul(
        li(
          `class` := "jstree-open",
          span(`class` := ViewStyles.treeNodeDesc, "lhs"),
          ul(buildHtml(tree.lhs, symbols)),
        ),
        li(
          `class` := "jstree-open",
          span(`class` := ViewStyles.treeNodeDesc, "rhs"),
          ul(buildHtml(tree.rhs, symbols)),
        ),
      ),
    )

  def treeToSymbol(t: TermTree) =
    try {
      typeToSymbol(t.tpe)
    } catch {
      case util.control.NonFatal(e) => None
    }

  def typeToSymbol(t: Type): Option[Symbol] =
    try {
      t match
        case t: TermRef => Some(t.symbol)
        case t: TypeRef => Some(t.symbol)
        case t: PackageRef => Some(t.symbol)
        case _ => None
    } catch {
      case util.control.NonFatal(e) => None
    }

  def symbolToNode(s: Option[Symbol], t: String, symbols: IDSymbolMap) =
    s.fold(
      li(
        span(`class` := ViewStyles.treeNodeType, t),
        span(`class` := ViewStyles.treeSymbol, "(not found)"),
      )
    ){ symbol =>
      val thisID = freshID()
      symbols(thisID) = symbol
      val elem = li(
        id := thisID,
        span(`class` := ViewStyles.treeNodeType, t),
        span(`class` := ViewStyles.treeSymbol, symbol.name.toString),
      )
      if symbol.isTerm
      then elem(span(`class` := ViewStyles.treeType,
        ": " + ViewUtils.prettyPrintType(symbol.asTerm.declaredType)))
      else elem
    }
