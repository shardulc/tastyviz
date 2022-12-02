package tastyviz.models

import tastyquery.Contexts.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Trees.*

sealed trait TastyModel(val symbol: Symbol, val owner: Option[TastyModel])(using Context):
  val name = symbol.name
  val fullName = symbol.fullName
  val flags = symbol.flags

class TastyPackageModel(symbol: PackageSymbol, owner: Option[TastyModel])(using Context)
    extends TastyModel(symbol, owner):
  def declarations = symbol.declarations.map(TastySymbolModel(_, this))

  def getDeclaration(decl: Symbol): Option[TastyModel] =
    symbol.getDecl(decl.name).flatMap(_ match
      case s: PackageSymbol => Some(TastyPackageModel(s, Some(this)))
      case s: Symbol if s.tree.nonEmpty =>
        Some(TastyDefTreeModel(s, s.tree.get.asInstanceOf[Tree], this))
      case _ => None)

class TastyDefTreeModel(symbol: Symbol, val tree: Tree, owner: TastyModel)(using Context)
    extends TastyModel(symbol, Some(owner))

class TastySymbolModel(symbol: Symbol, owner: TastyModel)(using Context)
    extends TastyModel(symbol, Some(owner))


class Model(using Context):

  def rootPackage = TastyPackageModel(ctx.defn.RootPackage, None)

  def find(path: List[Name]): Option[TastyModel] =
    if path.length == 0 then Some(rootPackage)
    else
      try {
        val symbol = ctx.findSymbolFromRoot(path)
        find(path.dropRight(1)).flatMap { owner =>
          symbol match
            case s: PackageSymbol => Some(TastyPackageModel(s, Some(owner)))
            case s: Symbol if s.tree.nonEmpty =>
              Some(TastyDefTreeModel(s,
                s.tree.get.asInstanceOf[tastyquery.Trees.Tree], owner))
            case _ => None
        }
      } catch { case _: tastyquery.Exceptions.MemberNotFoundException => None }
