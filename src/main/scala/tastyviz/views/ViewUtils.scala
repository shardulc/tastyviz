package tastyviz.views

import org.querki.jquery.*
import tastyquery.Symbols.*

import tastyviz.views.jstreefacade.*
import ViewConstants.*

object ViewUtils:
  def thisJSTree: JSTree = $(ViewDivs.defTreeView).jstree(true)

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

  def prettyPrintSymbol(s: Symbol): String = s match
    case NoSymbol => "<no symbol>"
    case _: PackageSymbol =>
      if s.isRoot then "package <root>"
      else s"package ${s.fullName.toString}"
    case _: TermSymbol => s.name.toString
    case _: ClassSymbol => s"class ${s.name.toString}"
    case _ => s.toString
