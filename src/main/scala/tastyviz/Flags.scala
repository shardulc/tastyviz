package tastyviz

import tastyquery.Flags

object FlagsPrinter:

  val flags = Seq(
    Flags.AbsOverride -> "abstract override",
    Flags.Abstract -> "abstract",
    Flags.Accessor -> "accessor",
    Flags.Artifact -> "artifact",
    Flags.Case -> "case",
    Flags.CaseAccessor -> "case accessor",
    Flags.Contravariant -> "contravariant",
    Flags.Covariant -> "covariant",
    Flags.Deferred -> "deferred",
    Flags.Enum -> "enum",
    Flags.Erased -> "erased",
    Flags.Exported -> "exported",
    Flags.Extension -> "extension",
    Flags.Final -> "final",
    Flags.Given -> "given",
    Flags.Implicit -> "implicit",
    Flags.Infix -> "infix",
    Flags.Inline -> "inline",
    Flags.InlineProxy -> "inline proxy",
    Flags.Lazy -> "lazy",
    Flags.Local -> "local",
    Flags.Macro -> "macro",
    Flags.Method -> "method",
    Flags.Module -> "module",
    Flags.ModuleVal -> "module val",
    Flags.ModuleClass -> "module class",
    Flags.Mutable -> "mutable",
    Flags.NoInitsInterface -> "no inits interface",
    Flags.Opaque -> "opaque",
    Flags.Open -> "open",
    Flags.Override -> "override",
    Flags.ParamAccessor -> "param accessor",
    Flags.Private -> "private",
    Flags.Protected -> "protected",
    Flags.Sealed -> "sealed",
    Flags.SuperParamAlias -> "super param alias",
    Flags.Static -> "static",
    Flags.Synthetic -> "synthetic",
    Flags.Trait -> "trait",
    Flags.Transparent -> "transparent",
    Flags.TypeParameter -> "type parameter",
  )

  def print(fset: Flags.FlagSet) =
    val flagList = flags
      .filter((flag, desc) => fset.is(flag))
      .map((flag, desc) => desc)
      .mkString(", ")
    if flagList.isEmpty() then "(none)" else flagList
