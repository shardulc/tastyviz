package tastyviz.views

import org.querki.jquery.*
import tastyquery.Symbols.*

import tastyviz.models.*
import tastyviz.views.jstreefacade.*
import ViewConstants.*

object ViewUtils:
  def thisJSTree: JSTree = $(ViewDivs.defTreeView).jstree(true)

  def symbolLt(s1: TastySymbolModel, s2: TastySymbolModel): Boolean =
    s1.symbol match
      case _: PackageSymbol =>
        s2.symbol match
          case _: PackageSymbol => s1.name.toString < s2.name.toString
          case _ => true
      case _: TypeSymbol =>
        s2.symbol match
          case _: PackageSymbol => false
          case _: TypeSymbol => s1.name.toString < s2.name.toString
          case _ => true
      case _ =>
        s2.symbol match
          case _: PackageSymbol => false
          case _: TypeSymbol => false
          case _ => s1.name.toString < s2.name.toString

  def prettyPrintSymbol(s: Symbol): String = s match
    case _: PackageSymbol =>
      if s.isRoot then "package <root>"
      else s"package ${s.fullName.toString}"
    case _: TermSymbol => s.name.toString
    case _: ClassSymbol => s"class ${s.name.toString}"
    case _ => s.toString
