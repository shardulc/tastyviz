package tastyviz.views

import java.nio.file.Paths

import scalatags.Text.all.*

import tastyquery.Contexts
import tastyquery.Contexts.*
import tastyquery.Symbols.*
import tastyquery.Names.FullyQualifiedName
import tastyquery.Trees.*
import scalatags.generic.TypedTag
import tastyquery.Flags
import tastyquery.Types.TermRef

import ViewConstants.*

class PrettyPrinter(using Context):

  def getSymbolInfo(fullName: FullyQualifiedName) =
    val symbol = ctx.findSymbolFromRoot(fullName.path)
    div(
      `class` := "symbolInfoBox",
      div(
        span("fully qual'd name:", `class` := "symbolInfoBoxDesc"),
        span(symbol.fullName.toString, `class` := ViewStyles.treeSymbol),
      ),
      div(
        span("flags:", `class` := "symbolInfoBoxDesc"),
        span(FlagsPrinter.print(symbol.flags)),
      ),
    )

  def getDeclaration(fullName: FullyQualifiedName) =
    ctx.findSymbolFromRoot(fullName.path).tree match
      case Some(tree) =>
        div(
          id := ViewDivs.defTreeView,
          ul(
            buildHtml(tree.asInstanceOf[Tree]),
          ),
        )
      case None => p("no deftree")

  def show(tree: Tree) =
    ul(buildHtml(tree)).render

  def buildHtml(tree: Tree): Modifier =
    tree match
      case t: ClassDef => buildHtmlClassDef(t)
      case t: Template => buildHtmlTemplate(t)
      case t: DefDef => buildHtmlDefDef(t)
      case t: ValDef => buildHtmlValDef(t)
      case EmptyTree => buildHtmlEmptyTree
      case t: Apply => buildHtmlApply(t)
      case t: Select => buildHtmlSelect(t)
      case t @ _ => li(span(`class` := ViewStyles.treeNodeType, t.getClass().getName()))

  def buildHtmlClassDef(tree: ClassDef) =
    li(
      // attr("tv-fullName") := FullNameCodec.encode(tree.symbol.fullName),
      span(`class` := ViewStyles.treeNodeType, "ClassDef"),
      span(`class` := ViewStyles.treeSymbol, tree.symbol.asType.name.toString),
      ul(buildHtml(tree.rhs)),
    )

  def buildHtmlTemplate(tree: Template) =
    li(
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "Template"),
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "constructor"),
          ul(buildHtmlDefDef(tree.constr)),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "parents"),
          ul(tree.parents
            .filter(_.isInstanceOf[Tree])
            .map(_.asInstanceOf[Tree])
            .map(buildHtml): _*),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "self"),
          ul(buildHtml(tree.self)),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "body"),
          ul(tree.body.map(buildHtml): _*),
        ),
      )
    )

  def buildHtmlDefDef(tree: DefDef) =
    li(
      // attr("tv-fullName") := FullNameCodec.encode(FullyQualifiedName(
        // tree.symbol.enclosingDecl.fullName.path :+ tree.symbol.name)),
      span(`class` := ViewStyles.treeNodeType, "DefDef"),
      span(`class` := ViewStyles.treeSymbol, tree.symbol.asTerm.name.toString),
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "parameters"),
          ul(tree.paramLists.map(buildHtmlParamsClause): _*),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "right-hand side"),
          ul(buildHtml(tree.rhs)),
        ),
      ),
    )

  def buildHtmlParamsClause(params: ParamsClause) =
    params match
      case Left(p) => li(
        span(`class` := ViewStyles.treeNodeDesc, "term parameters"),
        if p.isEmpty then ul(li(span(`class` := ViewStyles.treeNodeDesc, "(none)")))
        else ul(p.map(buildHtmlValDef(_, isParameter = true)): _*),
      )
      case Right(p) => li(
        span(`class` := ViewStyles.treeNodeDesc, "type parameters"),
        span("can't handle these yet"),
      )

  def buildHtmlValDef(tree: ValDef, isParameter: Boolean = false) =
    // val fullName = if isParameter then List.empty
    //   else List(attr("tv-fullName") := FullNameCodec.encode(FullyQualifiedName(
    //     tree.symbol.enclosingDecl.fullName.path :+ tree.symbol.name)))
    li(
      `class` := "jstree-open",
      span(`class` := ViewStyles.treeNodeType, "ValDef"),
      span(`class` := ViewStyles.treeSymbol, tree.symbol.asTerm.name.toString),
      ul(buildHtml(tree.rhs)),
    )//(fullName: _*)

  def buildHtmlEmptyTree =
    li(
      span(`class` := ViewStyles.treeNodeType, "EmptyTree"),
    )

  def buildHtmlApply(tree: Apply) =
    val args = if tree.args.isEmpty then List(li(span(`class` := ViewStyles.treeNodeDesc, "(none)")))
      else tree.args.map(buildHtml)
    li(
      span(`class` := ViewStyles.treeNodeType, "Apply"),
      `class` := "jstree-open",
      ul(
        li(
          `class` := "jstree-open",
          span(`class` := ViewStyles.treeNodeDesc, "function"),
          ul(buildHtml(tree.fun)),
        ),
        li(
          span(`class` := ViewStyles.treeNodeDesc, "arguments"),
          ul(args: _*),
        ),
      )
    )

  def buildHtmlSelect(tree: Select) =
    li(
      span(`class` := ViewStyles.treeNodeType, "Select"),
      `class` := "jstree-open",
      ul(
        li(
          span(`class` := ViewStyles.treeNodeDesc, "qualifier"),
          ul(buildHtml(tree.qualifier)),
        ),
        li(
          span(`class` := ViewStyles.treeSymbol, tree.name.toString),
          // attr("tv-fullName") := FullNameCodec.encode(FullyQualifiedName(
          //   List(tree.name)))
        ),
      )
    )
