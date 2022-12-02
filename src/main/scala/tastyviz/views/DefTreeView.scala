package tastyviz.views

import java.util.{Timer, TimerTask}
import scala.collection.mutable
import scalajs.js

import org.querki.jquery.*

import tastyquery.Contexts.Context

import tastyviz.models.*
import tastyviz.views.jstreefacade.*
import ViewConstants.*
import ViewUtils.*

class DefTreeView(
    onSelectionChange: Seq[tastyquery.Symbols.Symbol] => Unit,
    onClickOwner: () => Unit,
    encode: tastyquery.Symbols.Symbol => String)(using Context):
  private val printer = PrettyPrinter()
  private val symbolInfoView = SymbolInfoView(onSelectionChange, encode)

  $(ViewControls.backToOwner).click { (e: JQEvt) =>
    e.preventDefault()
    onClickOwner()
  }

  def clear() =
    // thisJSTree only works after first init
    if $(ViewDivs.defTreeView).children("*").length > 0 then thisJSTree.destroy()
    $(ViewDivs.defTreeView).empty()
    clearSymbolInfo()

  def displayDefTree(model: TastyDefTreeModel) =
    val (result, symbols) = printer.buildHtml(model.tree)
    $(ViewDivs.defTreeView).append(result.render)
    initializeJSTree()
    symbols.foreachEntry((id, symbol) =>
      thisJSTree.get_node(id).data.getSymbol = {() => symbol})
    symbolInfoView.initialize()
    $(ViewDivs.treeControl)
      .add(ViewDivs.treeDisplay)

  private object SearchDispatcher:
    private val timer = Timer()
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


  private def initializeJSTree() =
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
    thisJSTree.open_node($(ViewDivs.defTreeView + " li").first())
    $(ViewControls.searchSymbols).keyup(() => SearchDispatcher.searchSymbols())
    $(ViewControls.searchNodes).keyup(() => SearchDispatcher.searchNodes())
    $(ViewControls.expandAll).click(() => thisJSTree.open_all())
    $(ViewControls.collapseAll).click(() => thisJSTree.close_all())


  def clearSymbolInfo() =
    symbolInfoView.clear()

  def displaySymbolInfo(model: TastySymbolModel) =
    symbolInfoView.displaySymbolInfo(model)
