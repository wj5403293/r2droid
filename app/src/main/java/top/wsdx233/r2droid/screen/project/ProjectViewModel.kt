package top.wsdx233.r2droid.screen.project

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.wsdx233.r2droid.data.model.*
import top.wsdx233.r2droid.data.repository.ProjectRepository
import top.wsdx233.r2droid.util.R2PipeManager

data class HexRow(val addr: Long, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HexRow
        if (addr != other.addr) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        var result = addr.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

sealed class ProjectUiState {
    object Idle : ProjectUiState()
    data class Configuring(val filePath: String) : ProjectUiState()
    object Analyzing : ProjectUiState()
    object Loading : ProjectUiState()
    data class Success(
        val binInfo: BinInfo? = null,
        val sections: List<Section>? = null,
        val symbols: List<Symbol>? = null,
        val imports: List<ImportInfo>? = null,
        val relocations: List<Relocation>? = null,
        val strings: List<StringInfo>? = null,
        val functions: List<FunctionInfo>? = null,
        val hexRows: List<HexRow>? = null,
        val disassembly: List<DisasmInstruction>? = null,
        val decompilation: DecompilationData? = null,
        val cursorAddress: Long = 0L
    ) : ProjectUiState()
    data class Error(val message: String) : ProjectUiState()
}

class ProjectViewModel : ViewModel() {
    private val repository = ProjectRepository()

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Idle)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // Expose logs from LogManager
    val logs: StateFlow<List<top.wsdx233.r2droid.util.LogEntry>> = top.wsdx233.r2droid.util.LogManager.logs
    
    // Global pointer
    private var currentOffset: Long = 0L
    private val HEX_CHUNK_SIZE = 256
    private val DISASM_CHUNK_SIZE = 50

    init {
        // Init logic moved to initialize() to support ViewModel reuse
        // in simple navigation setups (Activity-scoped ViewModel)
    }

    fun initialize() {
        val path = R2PipeManager.pendingFilePath
        if (path != null) {
            // New file waiting to be configured
            _uiState.value = ProjectUiState.Configuring(path)
        } else {
             // No new file pending.
             // If we are already displaying data (Success), do nothing.
             // If we are Idle/Error, try to recover session if connected.
             if (_uiState.value is ProjectUiState.Idle || _uiState.value is ProjectUiState.Error) {
                 if (R2PipeManager.isConnected) {
                    loadOverview()
                } else {
                     _uiState.value = ProjectUiState.Error("No file selected or session active")
                }
             }
        }
    }

    fun startAnalysisSession(context: Context, analysisCmd: String, writable: Boolean, startupFlags: String) {
         val currentState = _uiState.value
         if (currentState is ProjectUiState.Configuring) {
             viewModelScope.launch {
                 _uiState.value = ProjectUiState.Analyzing
                 
                 val flags = if (writable) "-w $startupFlags" else startupFlags
                 
                 // Open Session
                 val openResult = R2PipeManager.open(context, currentState.filePath, flags.trim())

                 if (openResult.isSuccess) {
                     // Run Analysis
                     if (analysisCmd.isNotBlank() && analysisCmd != "none") {
                         R2PipeManager.execute("$analysisCmd; iIj")
                     }
                     // Load Data (Overview only)
                     // Load Data (Overview only)
                     loadOverview()
                     
                     // Set initial offset to entry point if possible, else 0
                     val entryPointsResult = repository.getEntryPoints()
                     if (entryPointsResult.isSuccess) {
                         val entries = entryPointsResult.getOrNull()
                         // Use first entry point's vaddr
                         currentOffset = entries?.firstOrNull()?.vAddr ?: 0L
                     } else {
                         // Fallback to 0 if fails
                         currentOffset = 0L
                     }
                     
                     // Update cursor address in state
                     (_uiState.value as? ProjectUiState.Success)?.let {
                         _uiState.value = it.copy(cursorAddress = currentOffset)
                     }
                     
                     // Clear pending path so subsequent navigations (or rotations) rely on configured state
                     R2PipeManager.pendingFilePath = null
                 } else {
                     _uiState.value = ProjectUiState.Error(openResult.exceptionOrNull()?.message ?: "Unknown error")
                 }
             }
         }
    }

    private fun loadOverview() {
        viewModelScope.launch {
            // If completely new, show loading
            if (_uiState.value !is ProjectUiState.Success) {
                // We don't want to override Analyzing too early if we want to show it, 
                // but loadOverview is called AFTER analysis.
                // However, getting overview is fast.
            }
            
            val binInfoResult = repository.getOverview()
            if (binInfoResult.isFailure) {
                _uiState.value = ProjectUiState.Error("Failed to load binary info: ${binInfoResult.exceptionOrNull()?.message}")
                return@launch
            }
            
            _uiState.value = ProjectUiState.Success(
                binInfo = binInfoResult.getOrNull(),
                cursorAddress = currentOffset
            )
        }
    }

    fun loadSections() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.sections != null) return
        
        viewModelScope.launch {
            val result = repository.getSections()
            // Ensure we are still in success state
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(sections = result.getOrDefault(emptyList()))
            }
        }
    }

    fun loadSymbols() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.symbols != null) return

        viewModelScope.launch {
            val result = repository.getSymbols()
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(symbols = result.getOrDefault(emptyList()))
            }
        }
    }

    fun loadImports() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.imports != null) return

        viewModelScope.launch {
            val result = repository.getImports()
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(imports = result.getOrDefault(emptyList()))
            }
        }
    }

    fun loadRelocations() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.relocations != null) return

        viewModelScope.launch {
            val result = repository.getRelocations()
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(relocations = result.getOrDefault(emptyList()))
            }
        }
    }

    fun loadStrings() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.strings != null) return

        viewModelScope.launch {
            val result = repository.getStrings()
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(strings = result.getOrDefault(emptyList()))
            }
        }
    }

    fun loadFunctions() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.functions != null) return

        viewModelScope.launch {
            val result = repository.getFunctions()
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(functions = result.getOrDefault(emptyList()))
            }
        }
    }

    fun loadHex() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        if (current.hexRows != null) return
        
        viewModelScope.launch {
            // Center around currentOffset
            var hexStart = currentOffset - (HEX_CHUNK_SIZE / 2)
            if (hexStart < 0) hexStart = 0
            loadHexChunk(hexStart, append = true)
        }
    }
    
    fun loadHexMore(append: Boolean) {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        val rows = current.hexRows ?: return
        
        if (append) {
            val lastRow = rows.lastOrNull() ?: return
            val nextAddr = lastRow.addr + 16 
            viewModelScope.launch { loadHexChunk(nextAddr, true) }
        } else {
            val firstRow = rows.firstOrNull() ?: return
            var prevAddr = firstRow.addr - HEX_CHUNK_SIZE
            if (prevAddr < 0) {
                 if (firstRow.addr == 0L) return
                 prevAddr = 0
            }
            viewModelScope.launch { loadHexChunk(prevAddr, false) }
        }
    }

    fun updateCursor(addr: Long) {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        
        // Update state immediately for UI highlight
        currentOffset = addr
        
        _uiState.value = current.copy(cursorAddress = addr)
        
        viewModelScope.launch {
            R2PipeManager.execute("s $addr")
        }
    }

    fun loadDisassembly() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        
        // Check if we need reload (if current cursor is not visible/loaded)
        val existing = current.disassembly
        if (existing != null && existing.any { it.addr == currentOffset }) {
             // We have the address in memory, but is it centered?
             // Since we use auto-scroll in Viewer, it's fine.
             return
        }

        viewModelScope.launch {
             // Centering logic: seek to target, seek back 20 instrs, read.
             R2PipeManager.execute("s $currentOffset")
             R2PipeManager.execute("so -20") 
             val currentSeekAddrStr = R2PipeManager.execute("?v .")
             val startDisasmAddr = currentSeekAddrStr.toString().trim().toLongOrNull() ?: currentOffset
             
             loadDisasmChunk(startDisasmAddr, true)
             
             // Restore seek to cursor
             R2PipeManager.execute("s $currentOffset")
        }
    }

    fun jumpToAddress(addr: Long) {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        
        // Clamp addr
        val fileSize = current.binInfo?.size ?: Long.MAX_VALUE
        val target = addr.coerceIn(0, fileSize)
        
        currentOffset = target
        
        viewModelScope.launch {
            // Load Hex: Center around target
            // Start loading from target - 128 (approx) to show context
            var hexStart = target - (HEX_CHUNK_SIZE / 2)
            if (hexStart < 0) hexStart = 0
            
            val hexResult = repository.getHexDump(hexStart, HEX_CHUNK_SIZE)
            val bytes = hexResult.getOrNull() ?: ByteArray(0)
            
            val newRows = bytes.toList().chunked(16).mapIndexed { index, chunk ->
                 HexRow(hexStart + index * 16, chunk.toByteArray())
             }
             
             // Load Disassembly: Center around target
             // Strategy: Seek to target, seek back 20 instructions, read 50 instructions.
             // This ensures target is roughly in the middle.
             R2PipeManager.execute("s $target") // Move to target first
             R2PipeManager.execute("so -20") // Go back 20 opcodes
             val disasmStartOffsetResult = R2PipeManager.execute("s") // Get current offset (optional, for debug)
             
             // Now read disassembly from this new position
             // Note: getDisassembly uses 'pdj' which reads from current seek if no addr specified? 
             // Repository method `getDisassembly(addr, count)` does `pdj count @ addr`.
             // We need to know the address we just seeked to.
             // Helper: `?v .` returns current address.
             val currentSeekAddrStr = R2PipeManager.execute("?v .")
             val startDisasmAddr = currentSeekAddrStr.toString().trim().toLongOrNull() ?: target
             
             val disasmResult = repository.getDisassembly(startDisasmAddr, DISASM_CHUNK_SIZE)
             val newInstrs = disasmResult.getOrNull() ?: emptyList()
             
             // Reset seek to target for consistency (cursor position)
             R2PipeManager.execute("s $target")
             
             _uiState.value = current.copy(
                 hexRows = newRows, 
                 disassembly = newInstrs, 
                 decompilation = null,
                 cursorAddress = target
             )
        }
    }

    private suspend fun loadHexChunk(startAddr: Long, append: Boolean) {
         val result = repository.getHexDump(startAddr, HEX_CHUNK_SIZE)
         val bytes = result.getOrNull() ?: ByteArray(0)
         if (bytes.isEmpty()) return
         
         val newRows = bytes.toList().chunked(16).mapIndexed { index, chunk ->
             HexRow(startAddr + index * 16, chunk.toByteArray())
         }
         
         val currentState = _uiState.value
         if (currentState is ProjectUiState.Success) {
             val currentList = currentState.hexRows ?: emptyList()
             // Avoid duplicates?
             // Since this is infinite scroll, we just append or prepend.
             val merged = if (append) currentList + newRows else newRows + currentList
             // Limit size? Maybe keep 1000 rows max to avoid memory issues?
             // For now, let it grow.
             _uiState.value = currentState.copy(hexRows = merged)
         }
    }

//    fun loadDisassembly() {
//        val current = _uiState.value as? ProjectUiState.Success ?: return
//        if (current.disassembly != null) return
//
//        viewModelScope.launch {
//            loadDisasmChunk(currentOffset, true)
//        }
//    }
    
    fun loadDisasmMore(append: Boolean) {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        val instrs = current.disassembly ?: return
         if (append) {
            val last = instrs.lastOrNull() ?: return
            // We need to know where the last instruction ended.
            // But getting disasm at addr+size might not be aligned if we just guessed.
            val nextAddr = last.addr + last.size
            viewModelScope.launch { loadDisasmChunk(nextAddr, true) }
        } else {
             val first = instrs.firstOrNull() ?: return
             var prevAddr = first.addr - (DISASM_CHUNK_SIZE * 4) 
             if (prevAddr < 0) {
                 if (first.addr == 0L) return
                 prevAddr = 0
             }
             viewModelScope.launch { loadDisasmChunk(prevAddr, false) }
        }
    }
    
    private suspend fun loadDisasmChunk(startAddr: Long, append: Boolean) {
        val result = repository.getDisassembly(startAddr, DISASM_CHUNK_SIZE)
        val newInstrs = result.getOrNull() ?: emptyList()
        if (newInstrs.isEmpty()) return
        
        val currentState = _uiState.value
         if (currentState is ProjectUiState.Success) {
             val currentList = currentState.disassembly ?: emptyList()
             val merged = if (append) currentList + newInstrs else newInstrs + currentList
             
             // Deduplicate by addr
             val distinct = merged.distinctBy { it.addr }.sortedBy { it.addr }
             
             _uiState.value = currentState.copy(disassembly = distinct)
         }
    }

    fun loadDecompilation() {
        val current = _uiState.value as? ProjectUiState.Success ?: return
        // Reload always or only if null? 
        // If currentOffset changed, we might want to reload. 
        // But for now let's assume one-shot load upon tab click, unless explicit refresh?
        // User wants global pointer.
        
        viewModelScope.launch {
            // "Get function where pointer is located"
            val funcStart = repository.getFunctionStart(currentOffset).getOrDefault(currentOffset)
            val result = repository.getDecompilation(funcStart)
            
            val currentState = _uiState.value
            if (currentState is ProjectUiState.Success) {
                _uiState.value = currentState.copy(decompilation = result.getOrNull())
            }
        }
    }

    fun loadAllData() {
        // Deprecated or fallback to loading overview
        loadOverview()
    }

    fun retryLoadAll() {
        loadOverview()
    }

    override fun onCleared() {
        super.onCleared()
        R2PipeManager.close()
    }
}
