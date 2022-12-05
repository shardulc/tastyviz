package tastyviz.views

import org.querki.jquery.*
import tastyquery.Symbols.*
import tastyquery.Types.*

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


  def prettyPrintType(t: Type): String =
    val printed = prettyPrintTypeHelper(t)
    if printed.startsWith("scala.Predef.") then printed.drop(13)
    else if printed.startsWith("scala.") then printed.drop(6)
    else printed

  private def prettyPrintTypeHelper(t: Type): String = t match
    case t: NamedType =>
      val prefix = t.prefix match
        case NoPrefix => ""
        case p: Type => prettyPrintType(p) + "."
      prefix + t.name
    case t: PackageRef => t.fullyQualifiedName.toString
    case t: MethodType =>
      t.paramTypes.map(prettyPrintType).mkString("(", ", ", ")")
        + " => " + prettyPrintType(t.resultType)
    case t: AppliedType =>
      prettyPrintType(t.tycon)
        + t.args.map(prettyPrintType).mkString("[", ", ", "]")
    case t: ThisType => s"this[${prettyPrintType(t.tref)}]"
    case _ => t.toString
