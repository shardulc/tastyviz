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

import ViewConstants.*
import scalatags.JsDom.TypedTag

class PrettyPrinter(using Context):

  var currentID = 0
  def freshID() =
    currentID += 1
    s"#tv-node$currentID"

  def buildHtml(tree: Tree): (TypedTag[_], mutable.Map[ID, Symbol]) =
    val symbols = mutable.Map.empty[ID, Symbol]
    val result = ul(buildHtml(tree, symbols))
    (result, symbols)

  def buildHtml(tree: Tree, symbols: mutable.Map[ID, Symbol]): Modifier =
    tree match
      case t: ClassDef => buildHtmlClassDef(t, symbols)
      case t: Template => buildHtmlTemplate(t, symbols)
      case t: DefDef => buildHtmlDefDef(t, symbols)
      case t: ValDef => buildHtmlValDef(t, symbols)
      case EmptyTree => buildHtmlEmptyTree
      case t: Apply => buildHtmlApply(t, symbols)
      case t: Select => buildHtmlSelect(t, symbols)
      case t @ _ => li(span(`class` := ViewStyles.treeNodeType, t.getClass().getName()))

  def buildHtmlClassDef(tree: ClassDef, symbols: mutable.Map[ID, Symbol]) =
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

  def buildHtmlTemplate(tree: Template, symbols: mutable.Map[ID, Symbol]) =
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

  def buildHtmlDefDef(tree: DefDef, symbols: mutable.Map[ID, Symbol]) =
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

  def buildHtmlParamsClause(params: ParamsClause, symbols: mutable.Map[ID, Symbol]) =
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

  def buildHtmlValDef(tree: ValDef, symbols: mutable.Map[ID, Symbol], isParameter: Boolean = false) =
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

  def buildHtmlApply(tree: Apply, symbols: mutable.Map[ID, Symbol]) =
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

  def buildHtmlSelect(tree: Select, symbols: mutable.Map[ID, Symbol]) =
    val symbol = try {
      tree.tpe match
        case t: NamedType => t.symbol
        case t: PackageRef => t.symbol
        case _ => NoSymbol
    } catch {
      case e: tastyquery.Exceptions.MemberNotFoundException =>
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
