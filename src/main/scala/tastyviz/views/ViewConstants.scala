package tastyviz.views

object ViewConstants:
  type ID = String
  type Class = String

  object ViewDivs:
    val packageView: ID = "#tv-packageView"
    val treeControl: ID = "#tv-treeControl"
    val treeDisplay: ID = "#tv-treeDisplay"
    val defTreeView: ID = "#tv-defTreeView"
    val symbolTreeView: ID = "#tv-symbolTreeView"
    val symbolInfoView: ID = "#tv-symbolInfoView"

    val topLevelDivs = Seq(packageView, treeControl, treeDisplay).mkString(", ")

  object ViewControls:
    val expandAll: ID = "#tv-expandAll"
    val collapseAll: ID = "#tv-collapseAll"
    val showTypes: ID = "#tv-showTypes"
    val searchSymbols: ID = "#tv-searchSymbols"
    val searchNodes: ID = "#tv-searchNodes"
    val backToOwner: ID = "#tv-backToOwner"

  object ViewStyles:
    val treeNodeType: Class = "tv-treeNodeType"
    val treeNodeDesc: Class = "tv-treeNodeDesc"
    val treeSymbol: Class = "tv-treeSymbol"
    val treeType: Class = "tv-treeType"

    val symbolInfoBox: Class = "tv-symbolInfoBox"
    val symbolInfoBoxDesc: Class = "tv-symbolInfoBoxDesc"
