package tastyviz.views

import java.nio.file.Paths
import scala.collection.mutable

import scalatags.JsDom.all.*
import org.querki.jquery.*

import tastyquery.Contexts.Context
import tastyquery.Symbols.*
import tastyquery.Trees.*
import tastyquery.Flags
import tastyquery.Types.*
import tastyquery.TypeTrees.TypeIdent

import ViewConstants.*
import scalatags.JsDom.TypedTag

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
      case EmptyTree => buildHtmlEmptyTree
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
    val symbol = tree.tpe.asInstanceOf[TypeRef].symbol
    val thisID = freshID()
    symbols(thisID) = symbol
    li(
      id := thisID,
      span(`class` := ViewStyles.treeNodeType, "New"),
      span(`class` := ViewStyles.treeSymbol, symbol.name.toString),
    )

  def buildHtmlTemplate(tree: Template, symbols: IDSymbolMap) =
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
          ul(tree.parents
            .filter(_.isInstanceOf[Tree])
            .map(_.asInstanceOf[Tree])
            .map(buildHtml(_, symbols)): _*),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "self"),
          ul(buildHtml(tree.self, symbols)),
        ),
        li(
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
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "parameters"),
          ul(tree.paramLists.map(buildHtmlParamsClause(_, symbols)): _*),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "right-hand side"),
          ul(buildHtml(tree.rhs, symbols)),
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
    li(
      id := thisID,
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "ValDef"),
      span(`class` := ViewStyles.treeSymbol, tree.symbol.asTerm.name.toString),
      ul(buildHtml(tree.rhs, symbols)),
    )

  def buildHtmlEmptyTree =
    li(
      span(`class` := ViewStyles.treeNodeType, "EmptyTree"),
    )

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
    val symbol = try {
      tree.tpe match
        case t: NamedType => t.symbol
        case t: PackageRef => t.symbol
        case _ => NoSymbol
    } catch {
      case scala.util.control.NonFatal(e) =>
        NoSymbol
    }
    val thisID = freshID()
    symbols(thisID) = symbol
    li(
      span(`class` := ViewStyles.treeNodeType, "Select"),
      `class` := "jstree-open",
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "qualifier"),
          ul(buildHtml(tree.qualifier, symbols)),
        ),
        li(
          id := thisID,
          span(`class` := ViewStyles.treeSymbol, symbol.name.toString),
        ),
      )
    )

  def buildHtmlLambda(tree: Lambda, symbols: IDSymbolMap) =
    li(
      span(`class` := ViewStyles.treeNodeType, "Select"),
      `class` := "jstree-open",
      ul(buildHtml(tree.meth, symbols))
    )

  def buildHtmlIdent(tree: Ident, symbols: IDSymbolMap) =
    val symbol = tree.tpe.asInstanceOf[TermRef].symbol
    val thisID = freshID()
    symbols(thisID) = symbol
    li(
      id := thisID,
      span(`class` := ViewStyles.treeNodeType, "Ident"),
      span(`class` := ViewStyles.treeSymbol, symbol.name.toString),
    )

  def buildHtmlTypeIdent(tree: TypeIdent, symbols: IDSymbolMap) =
    val symbol = tree.toType.asInstanceOf[TypeRef].symbol
    val thisID = freshID()
    symbols(thisID) = symbol
    li(
      id := thisID,
      span(`class` := ViewStyles.treeNodeType, "TypeIdent"),
      span(`class` := ViewStyles.treeSymbol, symbol.name.toString),
    )

  def buildHtmlLiteral(tree: Literal, symbols: IDSymbolMap) =
    li(
      span(`class` := ViewStyles.treeNodeType, "Constant"),
      span(`class` := ViewStyles.treeSymbol, tree.constant.value.toString),
    )

  def buildHtmlThis(tree: This, symbols: IDSymbolMap) =
    val thisLI = li(
      span(`class` := ViewStyles.treeNodeType, "This"),
    )
    tree.qualifier.fold(thisLI)(q => thisLI(ul(buildHtmlTypeIdent(q, symbols))))

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
