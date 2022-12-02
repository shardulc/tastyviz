package tastyviz.controller

import scalajs.js.annotation.*
import scalajs.js

@js.native
@JSGlobal("history")
object HistoryFacade extends js.Object:
  def pushState(state: js.Any, unused: String, url: String): Unit = js.native
  def pushState(state: js.Any, unused: String): Unit = js.native
  def replaceState(state: js.Any, unused: String, url: String): Unit = js.native
  def replaceState(state: js.Any, unused: String): Unit = js.native
  def back(): Unit = js.native
