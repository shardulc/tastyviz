package tastyviz.views

import scalatags.JsDom.all.*
import org.querki.jquery.*
import tastyquery.Symbols.Symbol

import tastyviz.models.*
import ViewConstants.*

class PackageView(
    classpath: List[String],
    onClickPackageDeclaration: TastySymbolModel => Unit,
    onClickPackageParent: () => Unit):

  private def buildClasspathHtml =
    div(
      p("classpath:"),
      ul(classpath.map(li(_)): _*),
    )

  private def buildPackageDeclarationsHtml(model: TastyPackageModel) =
    val declarationLinks = model.declarations
      .sortWith(ViewUtils.symbolLt)
      .map(d => li(a(
        ViewUtils.prettyPrintSymbol(d.symbol),
        href := "javascript:void(0)",
        onclick := { () => onClickPackageDeclaration(d) })))
      .prepended(li(a(
        "<..>",
        href := "javascript:void(0)",
        onclick := { () => onClickPackageParent() })))
    div(
      p(s"declarations in ${ViewUtils.prettyPrintSymbol(model.symbol)}:"),
      ul(declarationLinks: _*),
    )

  def showPackage(model: TastyPackageModel) =
    $(ViewDivs.topLevelDivs).hide()
    $(ViewDivs.packageView)
      .html(buildClasspathHtml.render.outerHTML)
      .append(buildPackageDeclarationsHtml(model).render)
      .show()
