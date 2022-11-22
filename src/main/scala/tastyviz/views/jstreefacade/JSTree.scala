package tastyviz.views.jstreefacade

import scala.language.implicitConversions
import scalajs.js
import scalajs.js.annotation.*

import org.querki.jquery._

@js.native
trait JSTree extends JQuery:
  def jstree(): JSTree = js.native
  def jstree(config: JSTreeConfig): JSTree = js.native
  def jstree(b: Boolean): JSTree = js.native

  def search(s: String): Unit = js.native
  def clear_search(): Unit = js.native

  def open_all(): Unit = js.native
  def open_node(n: JQuery): Unit = js.native
  def close_all(): Unit = js.native
  def deselect_all(): Unit = js.native

implicit def jq2jstree(jq: JQuery): JSTree = jq.asInstanceOf[JSTree]
