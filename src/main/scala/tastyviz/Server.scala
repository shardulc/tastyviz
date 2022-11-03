package tastyviz

import scalatags.Text.all.*
import tastyquery.Names.*


object FullNameCodec:
  def encode(fullName: FullyQualifiedName) =
    fullName.path
      .map(_ match
        case n: TermName => "tm" + n.toString
        case n: TypeName if n.wrapsObjectName => "tpo" + n.toString
        case n: TypeName => "tpn" + n.toString)
      .mkString(".")

  def decode(fullName: String) = FullyQualifiedName(
    if fullName == "root" then List.empty[Name] else
    fullName
      .split("[.]")
      .toList
      .map(name =>
        if name.startsWith("tm") then termName(name.drop(2)) else
        if name.startsWith("tpo") then termName(name.drop(3)).withObjectSuffix.toTypeName else
        if name.startsWith("tpn") then typeName(name.drop(3)) else
        termName(name)))


object Server extends cask.MainRoutes:

  var classpaths = tastyviz.generated.JavaClasspaths.classpaths ++ Seq(
    "/home/shardulc/Documents/school/EPFL/LAMP/tastyviz/target/scala-3.1.3/classes",
  )

  var printer = PrettyPrinter(classpaths)

  override def main(args: Array[String]): Unit =
    if args.length > 0 then
      classpaths = classpaths ++ args.toList
      printer = PrettyPrinter(classpaths)
    super.main(Array.empty[String])

  def pageHead =
    head(
      link(
        rel := "stylesheet",
        href := "https://cdnjs.cloudflare.com/ajax/libs/jstree/3.3.12/themes/default/style.min.css"
      ),
      link(
        rel := "stylesheet",
        href := "/dist/style.css"
      ),
      script(src := "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.1/jquery.min.js"),
      script(src := "https://cdnjs.cloudflare.com/ajax/libs/jstree/3.3.12/jstree.min.js"),
      link(
        rel := "stylesheet",
        href := "https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;600"
      ),
    )

  def treeControlPane =
    div(
      `class` := "treeControl",
      input(`type` := "button", id := "expandAll", value := "expand all"),
      input(`type` := "button", id := "collapseAll", value := "collapse all"),
      input(`type` := "checkbox", id := "showTypes", name := "showTypes"),
      label("show types", `for` := "showTypes", id := "showTypesLabel"),
      input(placeholder := "search symbols...", `type` := "search", id := "searchSymbols"),
      input(placeholder := "search TASTy nodes...", `type` := "search", id := "searchNodes"),
    )


  @cask.staticFiles("/dist")
  def staticDist() = "dist"

  @cask.get("/")
  def showRootPackage() =
    cask.Response("", 302, Seq("Location" -> "/package/root/"))

  @cask.get("/package/:fullName")
  def showPackage(fullName: String) =
    val decoded = FullNameCodec.decode(fullName)
    val decodedLast = decoded.path.lastOption.map(_.toString).getOrElse("root")
    cask.Response(
      html(
        pageHead,
        body(
          p("initialized with classpaths:"),
          ul(for path <- classpaths yield li(path)),
          p(s"declarations in package ${decodedLast}:"),
          printer.getDeclarationsInPackage(decoded),
        ),
      ).render,
      200,
      Seq("content-type" -> "text/html"),
    )

  @cask.get("/symbol/:fullName")
  def showSymbol(fullName: String) =
    cask.Response(
      html(
        pageHead,
        body(
          `class` := "treeView",
          treeControlPane,
          div(
            `class` := "treeDisplay",
            printer.getDeclaration(FullNameCodec.decode(fullName)),
            div(
              id := printer.treeSymbolInfoID,
              span("select one or more symbols to inspect them")),
            script(src := "/dist/script.js")
          ),
        ),
      ).render,
      200,
      Seq("content-type" -> "text/html"),
    )

  @cask.get("/symbolInfo/:fullName")
  def partialShowSymbolInfo(fullName: String) =
    cask.Response(
      printer.getSymbolInfo(FullNameCodec.decode(fullName)).render,
      200,
      Seq("content-type" -> "text/html"),
    )

  initialize()
