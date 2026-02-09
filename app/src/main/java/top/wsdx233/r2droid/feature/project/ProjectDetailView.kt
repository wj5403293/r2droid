package top.wsdx233.r2droid.feature.project

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.wsdx233.r2droid.feature.decompiler.ui.DecompilationViewer
import top.wsdx233.r2droid.feature.disasm.DisasmEvent
import top.wsdx233.r2droid.feature.disasm.DisasmViewModel
import top.wsdx233.r2droid.feature.disasm.ui.DisassemblyViewer
import top.wsdx233.r2droid.feature.hex.HexEvent
import top.wsdx233.r2droid.feature.hex.HexViewModel
import top.wsdx233.r2droid.feature.hex.ui.HexViewer
import top.wsdx233.r2droid.util.R2PipeManager

@Composable
fun ProjectDetailView(
    viewModel: ProjectViewModel = hiltViewModel(),
    hexViewModel: HexViewModel = hiltViewModel(),
    disasmViewModel: DisasmViewModel = hiltViewModel(),
    tabIndex: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState as? ProjectUiState.Success ?: return

    androidx.compose.runtime.LaunchedEffect(tabIndex) {
        when (tabIndex) {
            0 -> {
                val sections = state.sections ?: emptyList()
                val path = R2PipeManager.currentFilePath
                val cursor = state.cursorAddress
                hexViewModel.onEvent(HexEvent.LoadHex(sections, path, cursor))
            }
            1 -> {
                val sections = state.sections ?: emptyList()
                val path = R2PipeManager.currentFilePath
                val cursor = state.cursorAddress
                disasmViewModel.onEvent(DisasmEvent.LoadDisassembly(sections, path, cursor))
            }
            2 -> viewModel.onEvent(ProjectEvent.LoadDecompilation)
        }
    }
    
    androidx.compose.runtime.LaunchedEffect(state.cursorAddress) {
        val cursor = state.cursorAddress
        if (tabIndex == 0) hexViewModel.onEvent(HexEvent.PreloadHex(cursor))
        if (tabIndex == 1) disasmViewModel.onEvent(DisasmEvent.Preload(cursor))
    }
    
    when(tabIndex) {
        0 -> {
            if (hexViewModel.hexDataManager == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val sections = state.sections ?: emptyList()
                    val path = R2PipeManager.currentFilePath
                    val cursor = state.cursorAddress
                    hexViewModel.onEvent(HexEvent.LoadHex(sections, path, cursor))
                }
                CircularProgressIndicator()
            } else {
                HexViewer(
                    viewModel = hexViewModel,
                    cursorAddress = state.cursorAddress,
                    scrollToSelectionTrigger = viewModel.scrollToSelectionTrigger,
                    onByteClick = { addr -> viewModel.onEvent(ProjectEvent.UpdateCursor(addr)) },
                    onShowXrefs = { addr -> disasmViewModel.onEvent(DisasmEvent.FetchXrefs(addr)) }
                )
            }
        }
        1 -> {
            if (disasmViewModel.disasmDataManager == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val sections = state.sections ?: emptyList()
                    val path = R2PipeManager.currentFilePath
                    val cursor = state.cursorAddress
                    disasmViewModel.onEvent(DisasmEvent.LoadDisassembly(sections, path, cursor))
                }
                CircularProgressIndicator()
            } else {
                DisassemblyViewer(
                    viewModel = disasmViewModel,
                    cursorAddress = state.cursorAddress,
                    scrollToSelectionTrigger = viewModel.scrollToSelectionTrigger,
                    onInstructionClick = { addr -> viewModel.onEvent(ProjectEvent.UpdateCursor(addr)) }
                )
            }
        }
        2 -> {
            if (state.decompilation == null) {
                CircularProgressIndicator()
            } else {
                DecompilationViewer(
                    viewModel = viewModel,
                    data = state.decompilation,
                    cursorAddress = state.cursorAddress,
                    onAddressClick = { addr -> viewModel.onEvent(ProjectEvent.UpdateCursor(addr)) }
                )
            }
        }
    }
}
