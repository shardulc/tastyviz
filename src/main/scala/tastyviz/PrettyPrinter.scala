package tastyviz

import java.nio.file.Paths

import scalatags.Text.all.*

import tastyquery.jdk.ClasspathLoaders
import tastyquery.Contexts
import tastyquery.Contexts.*
import tastyquery.Names.FullyQualifiedName
import tastyquery.Trees.*
import scalatags.generic.TypedTag


class PrettyPrinter(classpaths: Seq[String]):

  final val treeRootID = "deftreeRoot"
  final val treeNodeType = "treeNodeType"
  final val treeNodeDesc = "treeNodeDesc"
  final val treeSymbol = "treeSymbol"
  final val treeSymbolInfoID = "treeSymbolInfo"

  given Context = Contexts.init(ClasspathLoaders.read(classpaths.map(p => Paths.get(p)).toList))

  def getDeclarationsInPackage(fullName: FullyQualifiedName) =
    ul(
      for
        decl <- ctx.findPackageFromRoot(fullName)
          .asPackage
          .declarations
      yield
        val encodedName = FullNameCodec.encode(decl.fullName)
        val pkgOrSym = if decl.isPackage then "package" else "symbol"
        li(a(
          span(decl.toString(), cls := "symbol"),
          href := s"/${pkgOrSym}/${FullNameCodec.encode(decl.fullName)}"))
    )

  def getSymbolInfo(fullName: FullyQualifiedName) =
    val symbol = ctx.findSymbolFromRoot(fullName.path)
    div(
      `class` := "symbolInfoBox",
      div(
        span("fully qual'd name:", `class` := "symbolInfoBoxDesc"),
        span(symbol.fullName.toString, `class` := treeSymbol),
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
          id := treeRootID,
          ul(
            buildHtml(tree.asInstanceOf[Tree]),
          ),
        )
      case None => p("no deftree")

  def buildHtml(tree: Tree): Modifier =
    tree match
      case t: ClassDef => buildHtmlClassDef(t)
      case t: Template => buildHtmlTemplate(t)
      case t: DefDef => buildHtmlDefDef(t)
      case t: ValDef => buildHtmlValDef(t)
      case t @ _ => li(span(`class` := treeNodeType, t.getClass().getName()))

  def buildHtmlClassDef(tree: ClassDef) =
    li(
      attr("tv-fullName") := FullNameCodec.encode(tree.symbol.fullName),
      span(`class` := treeNodeType, "ClassDef"),
      span(`class` := treeSymbol, tree.symbol.asType.name.toString),
      ul(buildHtml(tree.rhs)),
    )

  def buildHtmlTemplate(tree: Template) =
    li(
      `class` := "jstree-open",
      span(`class` := treeNodeType, "Template"),
      ul(
        li(
          span(`class` := treeNodeDesc, "constructor"),
          ul(buildHtmlDefDef(tree.constr)),
        ),
        li(
          span(`class` := treeNodeDesc, "parents"),
          ul(tree.parents
            .filter(_.isInstanceOf[Tree])
            .map(_.asInstanceOf[Tree])
            .map(buildHtml): _*),
        ),
        li(
          span(`class` := treeNodeDesc, "self"),
          ul(buildHtml(tree.self)),
        ),
        li(
          span(`class` := treeNodeDesc, "body"),
          ul(tree.body.map(buildHtml): _*),
        ),
      )
    )

  def buildHtmlDefDef(tree: DefDef) =
    li(
      attr("tv-fullName") := FullNameCodec.encode(FullyQualifiedName(
        tree.symbol.enclosingDecl.fullName.path :+ tree.symbol.name)),
      span(`class` := treeNodeType, "DefDef"),
      span(`class` := treeSymbol, tree.symbol.asTerm.name.toString),
      ul(
        li(
          span(`class` := treeNodeDesc, "parameters"),
          ul(tree.paramLists.map(buildHtmlParamsClause): _*),
        ),
        li(
          span(`class` := treeNodeDesc, "right-hand side"),
          ul(buildHtml(tree.rhs)),
        ),
      ),
    )

  def buildHtmlParamsClause(params: ParamsClause) =
    params match
      case Left(p) => li(
        span(`class` := treeNodeDesc, "term parameters"),
        if p.isEmpty then ul(li(span(`class` := treeNodeDesc, "(none)")))
        else ul(p.map(buildHtml): _*),
      )
      case Right(p) => li(
        span(`class` := treeNodeDesc, "type parameters"),
        span("can't handle these yet"),
      )

  def buildHtmlValDef(tree: ValDef) =
    li(
      attr("tv-fullName") := FullNameCodec.encode(FullyQualifiedName(
        tree.symbol.enclosingDecl.fullName.path :+ tree.symbol.name)),
      `class` := "jstree-open",
      span(`class` := treeNodeType, "ValDef"),
      span(`class` := treeSymbol, tree.symbol.asTerm.name.toString),
      ul(buildHtml(tree.rhs)),
    )
