package com.example.nervoscompanion.data

object PanelManifest {
  const val GITHUB_ASSETS_BASE_URL = "https://raw.githubusercontent.com/toastmanAu/ckb-directory-website/main/"

  data class PanelItem(val id: String, val name: String, val filePath: String)
  data class PanelGroup(val id: String, val name: String, val panels: List<PanelItem>)

  val groups = listOf(
    PanelGroup(
      "byterent", "ByteRent", listOf(
        PanelItem("byterent_01", "ByteRent 1", "assets/byterent_01.png"),
        PanelItem("byterent_02", "ByteRent 2", "assets/byterent_02.png"),
        PanelItem("byterent_03", "ByteRent 3", "assets/byterent_03.png")
      )
    ),
    PanelGroup(
      "cellswap", "CellSwap", listOf(
        PanelItem("cellswap_01", "CellSwap 1", "assets/cellswap_01.png")
      )
    ),
    PanelGroup(
      "ckb_explorer", "CKB Explorer", listOf(
        PanelItem("ckb_explorer_01", "CKB Explorer 1", "assets/ckb_explorer_01.png"),
        PanelItem("ckb_explorer_02", "CKB Explorer 2", "assets/ckb_explorer_02.png"),
        PanelItem("ckb_explorer_03", "CKB Explorer 3", "assets/ckb_explorer_03.png"),
        PanelItem("ckb_explorer_04", "CKB Explorer 4", "assets/ckb_explorer_04.png")
      )
    ),
    PanelGroup(
      "ckb_rpc_console", "CKB RPC Console", listOf(
        PanelItem("ckb_rpc_console_01", "CKB RPC Console 1", "assets/ckb_rpc_console_01.png"),
        PanelItem("ckb_rpc_console_02", "CKB RPC Console 2", "assets/ckb_rpc_console_02.png"),
        PanelItem("ckb_rpc_console_03", "CKB RPC Console 3", "assets/ckb_rpc_console_03.png"),
        PanelItem("ckb_rpc_console_04", "CKB RPC Console 4", "assets/ckb_rpc_console_04.png")
      )
    ),
    PanelGroup(
      "ckba", "CKBA", listOf(
        PanelItem("ckba_01", "CKBA 1", "assets/ckba_01.png"),
        PanelItem("ckba_02", "CKBA 2", "assets/ckba_02.png")
      )
    ),
    PanelGroup(
      "ckboost", "CKBoost", listOf(
        PanelItem("ckboost_01", "CKBoost 1", "assets/ckboost_01.png"),
        PanelItem("ckboost_02", "CKBoost 2", "assets/ckboost_02.png"),
        PanelItem("ckboost_03", "CKBoost 3", "assets/ckboost_03.png"),
        PanelItem("ckboost_04", "CKBoost 4", "assets/ckboost_04.png"),
        PanelItem("ckboost_05", "CKBoost 5", "assets/ckboost_05.png"),
        PanelItem("ckboost_06", "CKBoost 6", "assets/ckboost_06.png"),
        PanelItem("ckboost_07", "CKBoost 7", "assets/ckboost_07.png")
      )
    ),
    PanelGroup(
      "cklibrary", "CKLibrary", listOf(
        PanelItem("cklibrary_01", "CKLibrary 1", "assets/cklibrary_01.png"),
        PanelItem("cklibrary_02", "CKLibrary 2", "assets/cklibrary_02.png"),
        PanelItem("cklibrary_03", "CKLibrary 3", "assets/cklibrary_03.png"),
        PanelItem("cklibrary_04", "CKLibrary 4", "assets/cklibrary_04.png"),
        PanelItem("cklibrary_05", "CKLibrary 5", "assets/cklibrary_05.png"),
        PanelItem("cklibrary_06", "CKLibrary 6", "assets/cklibrary_06.png")
      )
    ),
    PanelGroup(
      "dao_portfolio_tracker", "DAO Portfolio Tracker", listOf(
        PanelItem("dao_portfolio_tracker_01", "DAO Portfolio Tracker 1", "assets/dao_portfolio_tracker_01.png")
      )
    ),
    PanelGroup(
      "fiber", "Fiber", listOf(
        PanelItem("fiber_01", "Fiber 1", "assets/fiber_01.png"),
        PanelItem("fiber_02", "Fiber 2", "assets/fiber_02.png"),
        PanelItem("fiber_03", "Fiber 3", "assets/fiber_03.png"),
        PanelItem("fiber_04", "Fiber 4", "assets/fiber_04.png"),
        PanelItem("fiber_05", "Fiber 5", "assets/fiber_05.png"),
        PanelItem("fiber_06", "Fiber 6", "assets/fiber_06.png"),
        PanelItem("fiber_07", "Fiber 7", "assets/fiber_07.png"),
        PanelItem("fiber_08", "Fiber 8", "assets/fiber_08.png")
      )
    ),
    PanelGroup(
      "holdem_bulls", "Holdem Bulls", listOf(
        PanelItem("holdem_bulls_01", "Holdem Bulls 1", "assets/holdem_bulls_01.png"),
        PanelItem("holdem_bulls_02", "Holdem Bulls 2", "assets/holdem_bulls_02.png"),
        PanelItem("holdem_bulls_03", "Holdem Bulls 3", "assets/holdem_bulls_03.png"),
        PanelItem("holdem_bulls_04", "Holdem Bulls 4", "assets/holdem_bulls_04.png"),
        PanelItem("holdem_bulls_05", "Holdem Bulls 5", "assets/holdem_bulls_05.png"),
        PanelItem("holdem_bulls_06", "Holdem Bulls 6", "assets/holdem_bulls_06.png")
      )
    ),
    PanelGroup(
      "ickb", "iCKB", listOf(
        PanelItem("ickb_01", "iCKB 1", "assets/ickb_01.png"),
        PanelItem("ickb_02", "iCKB 2", "assets/ickb_02.png"),
        PanelItem("ickb_03", "iCKB 3", "assets/ickb_03.png"),
        PanelItem("ickb_04", "iCKB 4", "assets/ickb_04.png"),
        PanelItem("ickb_05", "iCKB 5", "assets/ickb_05.png"),
        PanelItem("ickb_06", "iCKB 6", "assets/ickb_06.png")
      )
    ),
    PanelGroup(
      "joy_id", "JoyID", listOf(
        PanelItem("joy_id_01", "JoyID 1", "assets/joy_id_01.png"),
        PanelItem("joy_id_02", "JoyID 2", "assets/joy_id_02.png"),
        PanelItem("joy_id_03", "JoyID 3", "assets/joy_id_03.png"),
        PanelItem("joy_id_04", "JoyID 4", "assets/joy_id_04.png"),
        PanelItem("joy_id_05", "JoyID 5", "assets/joy_id_05.png"),
        PanelItem("joy_id_06", "JoyID 6", "assets/joy_id_06.png"),
        PanelItem("joy_id_07", "JoyID 7", "assets/joy_id_07.png"),
        PanelItem("joy_id_08", "JoyID 8", "assets/joy_id_08.png"),
        PanelItem("joy_id_09", "JoyID 9", "assets/joy_id_09.png")
      )
    ),
    PanelGroup(
      "mobit", "Mobit", listOf(
        PanelItem("mobit_01", "Mobit 1", "assets/mobit_01.png"),
        PanelItem("mobit_02", "Mobit 2", "assets/mobit_02.png"),
        PanelItem("mobit_03", "Mobit 3", "assets/mobit_03.png"),
        PanelItem("mobit_04", "Mobit 4", "assets/mobit_04.png"),
        PanelItem("mobit_05", "Mobit 5", "assets/mobit_05.png"),
        PanelItem("mobit_06", "Mobit 6", "assets/mobit_06.png"),
        PanelItem("mobit_07", "Mobit 7", "assets/mobit_07.png"),
        PanelItem("mobit_08", "Mobit 8", "assets/mobit_08.png")
      )
    ),
    PanelGroup(
      "nervos", "Nervos", listOf(
        PanelItem("nervos_01", "Nervos 1", "assets/nervos_01.png"),
        PanelItem("nervos_02", "Nervos 2", "assets/nervos_02.png"),
        PanelItem("nervos_03", "Nervos 3", "assets/nervos_03.png"),
        PanelItem("nervos_04", "Nervos 4", "assets/nervos_04.png")
      )
    ),
    PanelGroup(
      "nervos_dao_view", "Nervos DAO View", listOf(
        PanelItem("nervos_dao_view_05", "Nervos DAO View 5", "assets/nervos_dao_view_05.png")
      )
    ),
    PanelGroup(
      "nervos_dao_viewer", "Nervos DAO Viewer", listOf(
        PanelItem("nervos_dao_viewer_01", "Nervos DAO Viewer 1", "assets/nervos_dao_viewer_01.png"),
        PanelItem("nervos_dao_viewer_02", "Nervos DAO Viewer 2", "assets/nervos_dao_viewer_02.png"),
        PanelItem("nervos_dao_viewer_03", "Nervos DAO Viewer 3", "assets/nervos_dao_viewer_03.png"),
        PanelItem("nervos_dao_viewer_04", "Nervos DAO Viewer 4", "assets/nervos_dao_viewer_04.png"),
        PanelItem("nervos_dao_viewer_05", "Nervos DAO Viewer 5", "assets/nervos_dao_viewer_05.png")
      )
    ),
    PanelGroup(
      "perun", "Perun", listOf(
        PanelItem("perun_01", "Perun 1", "assets/perun_01.png"),
        PanelItem("perun_02", "Perun 2", "assets/perun_02.png"),
        PanelItem("perun_03", "Perun 3", "assets/perun_03.png"),
        PanelItem("perun_04", "Perun 4", "assets/perun_04.png"),
        PanelItem("perun_05", "Perun 5", "assets/perun_05.png"),
        PanelItem("perun_06", "Perun 6", "assets/perun_06.png"),
        PanelItem("perun_07", "Perun 7", "assets/perun_07.png")
      )
    ),
    PanelGroup(
      "pocket_node", "Pocket Node", listOf(
        PanelItem("pocket_node_01", "Pocket Node 1", "assets/pocket_node_01.png"),
        PanelItem("pocket_node_02", "Pocket Node 2", "assets/pocket_node_02.png"),
        PanelItem("pocket_node_03", "Pocket Node 3", "assets/pocket_node_03.png"),
        PanelItem("pocket_node_04", "Pocket Node 4", "assets/pocket_node_04.png"),
        PanelItem("pocket_node_05", "Pocket Node 5", "assets/pocket_node_05.png"),
        PanelItem("pocket_node_06", "Pocket Node 6", "assets/pocket_node_06.png"),
        PanelItem("pocket_node_07", "Pocket Node 7", "assets/pocket_node_07.png"),
        PanelItem("pocket_node_08", "Pocket Node 8", "assets/pocket_node_08.png"),
        PanelItem("pocket_node_09", "Pocket Node 9", "assets/pocket_node_09.png")
      )
    ),
    PanelGroup(
      "quantum_purse", "Quantum Purse", listOf(
        PanelItem("quantum_purse_01", "Quantum Purse 1", "assets/quantum_purse_01.png"),
        PanelItem("quantum_purse_02", "Quantum Purse 2", "assets/quantum_purse_02.png"),
        PanelItem("quantum_purse_03", "Quantum Purse 3", "assets/quantum_purse_03.png"),
        PanelItem("quantum_purse_04", "Quantum Purse 4", "assets/quantum_purse_04.png"),
        PanelItem("quantum_purse_05", "Quantum Purse 5", "assets/quantum_purse_05.png"),
        PanelItem("quantum_purse_06", "Quantum Purse 6", "assets/quantum_purse_06.png"),
        PanelItem("quantum_purse_07", "Quantum Purse 7", "assets/quantum_purse_07.png"),
        PanelItem("quantum_purse_08", "Quantum Purse 8", "assets/quantum_purse_08.png"),
        PanelItem("quantum_purse_09", "Quantum Purse 9", "assets/quantum_purse_09.png")
      )
    ),
    PanelGroup(
      "rfc_view", "RFC View", listOf(
        PanelItem("rfc_view_01", "RFC View 1", "assets/rfc_view_01.png"),
        PanelItem("rfc_view_02", "RFC View 2", "assets/rfc_view_02.png"),
        PanelItem("rfc_view_03", "RFC View 3", "assets/rfc_view_03.png")
      )
    ),
    PanelGroup(
      "rosen", "Rosen", listOf(
        PanelItem("rosen_01", "Rosen 1", "assets/rosen_01.png"),
        PanelItem("rosen_02", "Rosen 2", "assets/rosen_02.png"),
        PanelItem("rosen_03", "Rosen 3", "assets/rosen_03.png"),
        PanelItem("rosen_04", "Rosen 4", "assets/rosen_04.png"),
        PanelItem("rosen_05", "Rosen 5", "assets/rosen_05.png")
      )
    ),
    PanelGroup(
      "scryve", "Scryve", listOf(
        PanelItem("scryve_01", "Scryve 1", "assets/scryve_01.png"),
        PanelItem("scryve_02", "Scryve 2", "assets/scryve_02.png"),
        PanelItem("scryve_03", "Scryve 3", "assets/scryve_03.png"),
        PanelItem("scryve_04", "Scryve 4", "assets/scryve_04.png"),
        PanelItem("scryve_05", "Scryve 5", "assets/scryve_05.png"),
        PanelItem("scryve_06", "Scryve 6", "assets/scryve_06.png"),
        PanelItem("scryve_07", "Scryve 7", "assets/scryve_07.png")
      )
    ),
    PanelGroup(
      "talk_forum", "Talk Forum", listOf(
        PanelItem("talk_forum_01", "Talk Forum 1", "assets/talk_forum_01.png"),
        PanelItem("talk_forum_02", "Talk Forum 2", "assets/talk_forum_02.png"),
        PanelItem("talk_forum_03", "Talk Forum 3", "assets/talk_forum_03.png"),
        PanelItem("talk_forum_04", "Talk Forum 4", "assets/talk_forum_04.png"),
        PanelItem("talk_forum_05", "Talk Forum 5", "assets/talk_forum_05.png")
      )
    ),
    PanelGroup(
      "testnet_faucet", "Testnet Faucet", listOf(
        PanelItem("testnet_faucet_01", "Testnet Faucet 1", "assets/testnet_faucet_01.png"),
        PanelItem("testnet_faucet_02", "Testnet Faucet 2", "assets/testnet_faucet_02.png"),
        PanelItem("testnet_faucet_03", "Testnet Faucet 3", "assets/testnet_faucet_03.png"),
        PanelItem("testnet_faucet_04", "Testnet Faucet 4", "assets/testnet_faucet_04.png"),
        PanelItem("testnet_faucet_05", "Testnet Faucet 5", "assets/testnet_faucet_05.png")
      )
    ),
    PanelGroup(
      "tx_dao_yield", "TX DAO Yield", listOf(
        PanelItem("tx_dao_yield_01", "TX DAO Yield 1", "assets/tx_dao_yield_01.png"),
        PanelItem("tx_dao_yield_02", "TX DAO Yield 2", "assets/tx_dao_yield_02.png"),
        PanelItem("tx_dao_yield_03", "TX DAO Yield 3", "assets/tx_dao_yield_03.png"),
        PanelItem("tx_dao_yield_04", "TX DAO Yield 4", "assets/tx_dao_yield_04.png")
      )
    ),
    PanelGroup(
      "wyltek_industries", "Wyltek Industries", listOf(
        PanelItem("wyltek_industries_01", "Wyltek Industries 1", "assets/wyltek_industries_01.png"),
        PanelItem("wyltek_industries_02", "Wyltek Industries 2", "assets/wyltek_industries_02.png"),
        PanelItem("wyltek_industries_03", "Wyltek Industries 3", "assets/wyltek_industries_03.png"),
        PanelItem("wyltek_industries_04", "Wyltek Industries 4", "assets/wyltek_industries_04.png"),
        PanelItem("wyltek_industries_05", "Wyltek Industries 5", "assets/wyltek_industries_05.png"),
        PanelItem("wyltek_industries_06", "Wyltek Industries 6", "assets/wyltek_industries_06.png")
      )
    )
  )

  fun getGroupForCard(id: String): PanelGroup? {
    val groupId = when (id) {
      "dao_viewer" -> "nervos_dao_viewer"
      "dao_dashboard" -> "dao_portfolio_tracker"
      "tx_calculator" -> "tx_dao_yield"
      "rpc_console" -> "ckb_rpc_console"
      "local_rfc" -> "rfc_view"
      "fiber_node" -> "fiber"
      "cklibrary" -> "cklibrary"
      "cellswap" -> "cellswap"
      "wyltek" -> "wyltek_industries"
      "byterent" -> "byterent"
      "faucet" -> "testnet_faucet"
      "explorer" -> "ckb_explorer"
      "home_base" -> "nervos"
      "talk_forum" -> "talk_forum"
      else -> {
        // App names mapping (dynamic/fallback by names)
        val normalized = id.lowercase().replace("[^a-z0-9]".toRegex(), "")
        when {
          normalized.contains("joyid") -> "joy_id"
          normalized.contains("ickb") -> "ickb"
          normalized.contains("quantumpurse") -> "quantum_purse"
          normalized.contains("pocketnode") -> "pocket_node"
          normalized.contains("mobit") -> "mobit"
          normalized.contains("holdem") || normalized.contains("bulls") -> "holdem_bulls"
          normalized.contains("nervosdao") || normalized.contains("nervosdaoviewer") -> "nervos_dao_viewer"
          normalized.contains("nervdao") -> "nervos_dao_viewer"
          normalized.contains("daoportfolio") -> "dao_portfolio_tracker"
          normalized.contains("daoview") -> "nervos_dao_view"
          normalized.contains("ckba") -> "ckba"
          normalized.contains("ckboost") -> "ckboost"
          normalized.contains("perun") -> "perun"
          normalized.contains("rosen") -> "rosen"
          normalized.contains("scryve") -> "scryve"
          normalized.contains("byterent") -> "byterent"
          normalized.contains("cellswap") -> "cellswap"
          normalized.contains("explorer") -> "ckb_explorer"
          normalized.contains("rpc") || normalized.contains("console") -> "ckb_rpc_console"
          normalized.contains("library") -> "cklibrary"
          normalized.contains("fiber") -> "fiber"
          normalized.contains("faucet") -> "testnet_faucet"
          normalized.contains("wyltek") -> "wyltek_industries"
          normalized.contains("talk") || normalized.contains("forum") -> "talk_forum"
          else -> null
        }
      }
    }
    return groups.find { it.id == groupId }
  }

  fun getDefaultAssetUrl(id: String): Any? {
    val normalized = id.lowercase().replace("[^a-z0-9]".toRegex(), "")
    if (normalized == "cellscript") {
      return com.example.nervoscompanion.R.drawable.cell_script
    }
    if (normalized == "fiberstorybook") {
      return com.example.nervoscompanion.R.drawable.fiber_storybook
    }

    val group = getGroupForCard(id) ?: return null
    val targetSuffix = when (group.id) {
      "ckb_explorer" -> "_02"
      "ckb_rpc_console" -> "_03"
      "ckboost" -> "_04"
      "cklibrary" -> "_02"
      "fiber" -> "_02"
      "holdem_bulls" -> "_02"
      "ickb" -> "_04"
      "mobit" -> "_02"
      "nervos" -> "_03"
      "nervos_dao_view" -> "_05"
      "perun" -> "_03"
      "pocket_node" -> "_02"
      "quantum_purse" -> "_08"
      "rfc_view" -> "_02"
      "rosen" -> "_04"
      "testnet_faucet" -> "_03"
      "tx_dao_yield" -> "_02"
      else -> "_01"
    }
    val panel = group.panels.find { it.id.endsWith(targetSuffix) }
      ?: group.panels.find { it.id.endsWith("_01") }
      ?: group.panels.firstOrNull()
      ?: return null
    return "$GITHUB_ASSETS_BASE_URL${panel.filePath}"
  }
}
