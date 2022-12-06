package tastyviz.models

import tastyquery.Contexts.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Trees.*
import tastyquery.Types.*

sealed trait TastyModel(val symbol: Symbol)(using Context):
  val name = symbol.name
  val fullName = symbol.fullName
  val flags = symbol.flags
  def owner =
    symbol.owner match
      case null => None
      case s: PackageSymbol => Some(TastyPackageModel(s))
      case s: Symbol if s.tree.nonEmpty =>
        Some(TastyDefTreeModel(s, s.tree.get.asInstanceOf[Tree]))
      case s @ _ => Some(TastySymbolModel(s))

class TastyPackageModel(symbol: PackageSymbol)(using Context)
    extends TastyModel(symbol):
  def declarations = symbol.declarations.map(TastySymbolModel(_))

  def getDeclaration(decl: Symbol): Option[TastyModel] =
    symbol.getDecl(decl.name).flatMap(_ match
      case s: PackageSymbol => Some(TastyPackageModel(s))
      case s: Symbol if s.tree.nonEmpty =>
        Some(TastyDefTreeModel(s, s.tree.get.asInstanceOf[Tree]))
      case _ => None)

class TastyDefTreeModel(symbol: Symbol, val tree: Tree)(using Context)
    extends TastyModel(symbol)

class TastySymbolModel(symbol: Symbol)(using Context)
    extends TastyModel(symbol):
  def tpe = if symbol.isTerm then Some(symbol.asTerm.declaredType) else None
  def typeBounds =
    if symbol.isType
    then symbol.asType match
      case _: ClassSymbol => None
      case s: TypeSymbolWithBounds => Some(s.bounds)
    else None

  private def typeSymbolsHelper(t: Type): Seq[Symbol] =
    t match
      case tt: NamedType => Seq(tt.symbol)
      case tt: PackageRef => Seq.empty
      case tt: MethodType =>
        tt.paramTypes.flatMap(typeSymbolsHelper) ++ typeSymbolsHelper(tt.resultType)
      case tt: AppliedType =>
        typeSymbolsHelper(tt.tycon) ++ tt.args.flatMap(typeSymbolsHelper)
      case tt: ThisType => typeSymbolsHelper(tt.tref)
      case tt: BoundedType =>
        val bounds = typeSymbolsHelper(tt.bounds.low)
          ++ typeSymbolsHelper(tt.bounds.high)
        tt.alias.fold(bounds)(a => bounds ++ typeSymbolsHelper(a))
      case tt: AndType => typeSymbolsHelper(tt.first) ++ typeSymbolsHelper(tt.second)
      case tt: OrType => typeSymbolsHelper(tt.first) ++ typeSymbolsHelper(tt.second)
      case _ => Seq.empty

  def typeSymbols = tpe.map(t => typeSymbolsHelper(t).distinct)
    .orElse(typeBounds.map(b =>
      (typeSymbolsHelper(b.low) ++ typeSymbolsHelper(b.high)).distinct))


class Model(using Context):

  def rootPackage = TastyPackageModel(ctx.defn.RootPackage)

  def find(path: List[Name]): Option[TastyModel] =
    if path.length == 0 then Some(rootPackage)
    else
      try {
        val symbol = ctx.findSymbolFromRoot(path)
        find(path.dropRight(1)).flatMap { owner =>
          symbol match
            case s: PackageSymbol => Some(TastyPackageModel(s))
            case s: Symbol if s.tree.nonEmpty =>
              Some(TastyDefTreeModel(s,
                s.tree.get.asInstanceOf[tastyquery.Trees.Tree]))
            case _ => None
        }
      } catch { case _: tastyquery.Exceptions.MemberNotFoundException => None }
