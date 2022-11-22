package tastyviz

import scala.collection.mutable

import org.querki.jquery._
import scalatags.JsDom.all.*

import jstreefacade.*

import scalajs.js.annotation.*
import scalajs.js
import tastyquery.Contexts.*
import tastyquery.Symbols.*
import java.util.TimerTask

import ViewConstants.*

/*
to implement:

.on('changed.jstree', function (e, action) {
	$(treeSymbolInfoID).html("");
	action.selected.forEach(function (id) {
	    var selectedSymbol = $('#' + id);
	    if (selectedSymbol.attr('tv-fullName')) {
		$.get('/symbolInfo/' + selectedSymbol.attr('tv-fullName'), function (data) {
		    $(treeSymbolInfoID).append(data);
		});
	    }
	});
    })
*/


@js.native
@JSGlobalScope
object JSTimers extends js.Object:
  def setTimeout(f: js.Function0[js.Any], delay: Int): Int = js.native
  def clearTimeout(t: Int): Unit = js.native


class View(
    classpath: List[String],
    onClickPackageDeclaration: Symbol => Unit,
    onClickPackageParent: () => Unit)(using Context):

  val printer = PrettyPrinter()

  def thisJSTree: JSTree = $(ViewDivs.defTreeView).jstree(true)


  object SearchDispatcher:
    private val timer = java.util.Timer()
    private var latestTask: Option[SearchTask] = None

    enum SearchKey(
        val key: String,
        val searchBox: ViewConstants.ID,
        val searchStyle: ViewConstants.Class):
      case symbol extends SearchKey("s", ViewControls.searchSymbols, ViewStyles.treeSymbol)
      case node extends SearchKey("n", ViewControls.searchNodes, ViewStyles.treeNodeType)
    object SearchKey:
      val keys = Map("s" -> SearchKey.symbol, "n" -> SearchKey.node)

    class SearchTask(k: SearchKey) extends TimerTask:
      override def run(): Unit =
        thisJSTree.search(k.key + $(k.searchBox).value().toString)

    def searchSymbols(): Unit =
      latestTask.foreach(_.cancel())
      val newTask = SearchTask(SearchKey.symbol)
      latestTask = Some(newTask)
      timer.schedule(newTask, 250)

    def searchNodes(): Unit =
      latestTask.foreach(_.cancel())
      val newTask = SearchTask(SearchKey.node)
      latestTask = Some(newTask)
      timer.schedule(newTask, 250)

    def searchCallback(str: String, node: js.Dynamic): Boolean =
      if str.length <= 3 then
        thisJSTree.clear_search()
        false
      else
        $(node.text.asInstanceOf[String])
          .filter("." + SearchDispatcher.SearchKey.keys(str(0).toString).searchStyle)
          .get(0)
          .map(_.textContent.toLowerCase().contains(str.drop(1)))
          .getOrElse(false)


  def initializeJSTree() =
    val config = JSTreeConfig(
        core = Some(JSTreeConfigCore(
          animation = None,
          themes = Some(JSTreeConfigCoreThemes(Some("large"), false)))),
        plugins = mutable.Seq(JSTreePlugins.Search),
        search = Some(JSTreeConfigSearch(Some(SearchDispatcher.searchCallback)))
      )
    $(ViewDivs.defTreeView)
      .on("click", { (event: JQueryEventObject) => event.stopPropagation() })
      .jstree(config)
      .show()
    thisJSTree.open_node($(ViewDivs.defTreeView + " li").first())
    $(ViewControls.searchSymbols).keyup(() => SearchDispatcher.searchSymbols())
    $(ViewControls.searchNodes).keyup(() => SearchDispatcher.searchNodes())
    $(ViewControls.expandAll).click(() => thisJSTree.open_all())
    $(ViewControls.collapseAll).click(() => thisJSTree.close_all())
    $("body").click(() => thisJSTree.deselect_all())

  def initialize() =
    $("body > div").hide()

  def showDefTree(model: TastyDefTreeModel) =
    $(ViewDivs.packageView).hide()
    $(ViewDivs.defTreeView).html(printer.show(model.tree))
    initializeJSTree()
    $(ViewDivs.treeControl).add(ViewDivs.treeDisplay).show()

  private def buildClasspathHtml =
    div(
      p("classpath:"),
      ul(classpath.map(li(_)): _*),
    )

  private def buildPackageDeclarationsHtml(model: TastyPackageModel) =
    val declarationLinks = model.declarations
      .sortWith(symbolLt)
      .map(d => li(a(
        printer.prettyPrintSymbol(d),
        href := "javascript:void(0)",
        onclick := { () => onClickPackageDeclaration(d) })))
      .prepended(li(a(
        "<..>",
        href := "javascript:void(0)",
        onclick := { () => onClickPackageParent() })))
    div(
      p(s"declarations in ${printer.prettyPrintSymbol(model.symbol)}:"),
      ul(declarationLinks: _*),
    )

  def showPackage(model: TastyPackageModel) =
    $(ViewDivs.treeControl).add(ViewDivs.treeDisplay).hide()
    $(ViewDivs.packageView)
      .html(buildClasspathHtml.render.outerHTML)
      .append(buildPackageDeclarationsHtml(model).render)
      .show()


  def symbolLt(s1: Symbol, s2: Symbol): Boolean =
    s1 match
      case _: PackageSymbol =>
        s2 match
          case _: PackageSymbol => s1.name.toString < s2.name.toString
          case _ => true
      case _: TypeSymbol =>
        s2 match
          case _: PackageSymbol => false
          case _: TypeSymbol => s1.name.toString < s2.name.toString
          case _ => true
      case _ =>
        s2 match
          case _: PackageSymbol => false
          case _: TypeSymbol => false
          case _ => s1.name.toString < s2.name.toString
