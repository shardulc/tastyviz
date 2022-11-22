package tastyviz.jstreefacade

import scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.collection.mutable
import js.JSConverters.*

import org.querki.jquery._

trait JSTreeConfig extends js.Object:
  @JSName("core")
  val jsTreeCore: js.UndefOr[JSTreeConfigCore]
  @JSName("plugins")
  val jsTreePlugins: js.Array[String]
  @JSName("search")
  val jsTreeSearch: js.UndefOr[JSTreeConfigSearch]

object JSTreeConfig:
  def apply(
      core: Option[JSTreeConfigCore] = None,
      plugins: mutable.Seq[JSTreePlugins] = mutable.Seq.empty,
      search: Option[JSTreeConfigSearch] = None
      ) =
    new JSTreeConfig {
      @JSName("core")
      val jsTreeCore = core.orUndefined
      @JSName("plugins")
      val jsTreePlugins = plugins.map(_.name).toJSArray
      @JSName("search")
      val jsTreeSearch = search.orUndefined
    }


trait JSTreeConfigCore extends js.Object:
  @JSName("animation")
  val jsTreeAnimation: Boolean | Int
  @JSName("themes")
  val jsTreeThemes: js.UndefOr[JSTreeConfigCoreThemes]

object JSTreeConfigCore:
  def apply(
      animation: Option[Int] = Some(200),
      themes: Option[JSTreeConfigCoreThemes]) =
    new JSTreeConfigCore {
      @JSName("animation")
      val jsTreeAnimation = animation.getOrElse(false)
      @JSName("themes")
      val jsTreeThemes = themes.orUndefined
    }


trait JSTreeConfigCoreThemes extends js.Object:
  @JSName("variant")
  val jsTreeVariant: js.UndefOr[String]
  @JSName("icons")
  val jsTreeIcons: Boolean

object JSTreeConfigCoreThemes:
  def apply(variant: Option[String] = None, icons: Boolean = true) =
    new JSTreeConfigCoreThemes {
      @JSName("variant")
      val jsTreeVariant = variant.orUndefined
      @JSName("icons")
      val jsTreeIcons = icons
    }


enum JSTreePlugins(val name: String):
  case Search extends JSTreePlugins("search")


trait JSTreeConfigSearch extends js.Object:
  @JSName("search_callback")
  val jsTreeSearchCallback: js.UndefOr[js.Function2[String, js.Dynamic, Boolean]]

object JSTreeConfigSearch:
  def apply(searchCallback: Option[js.Function2[String, js.Dynamic, Boolean]] = None) =
    new JSTreeConfigSearch {
      @JSName("search_callback")
      val jsTreeSearchCallback = searchCallback.orUndefined
    }
