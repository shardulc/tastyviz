package tastyviz.models

import tastyquery.Contexts.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Trees.*

sealed trait TastyModel(val symbol: Symbol)(using Context):
  val name = symbol.name
  val fullName = symbol.fullName

class TastyPackageModel(symbol: PackageSymbol)(using Context) extends TastyModel(symbol):
  def declarations = symbol.declarations

  def getDeclaration(decl: Symbol): Option[TastyModel] =
    symbol.getDecl(decl.name).flatMap(_ match
      case s: PackageSymbol => Some(TastyPackageModel(s))
      case s: Symbol if s.tree.nonEmpty =>
        Some(TastyDefTreeModel(s, s.tree.get.asInstanceOf[Tree]))
      case _ => None)

class TastyDefTreeModel(symbol: Symbol, val tree: Tree)(using Context)
    extends TastyModel(symbol)

class TastySymbolInfoModel(symbol: Symbol)(using Context) extends TastyModel(symbol)


class Model(using Context):

  def rootPackage = TastyPackageModel(ctx.defn.RootPackage)
