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
        val functions: List<FunctionInfo>? = null
    ) : ProjectUiState()
    data class Error(val message: String) : ProjectUiState()
}

class ProjectViewModel : ViewModel() {
    private val repository = ProjectRepository()

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Idle)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // Expose logs from LogManager
    val logs: StateFlow<List<top.wsdx233.r2droid.util.LogEntry>> = top.wsdx233.r2droid.util.LogManager.logs

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
                         R2PipeManager.execute(analysisCmd)
                     }
                     // Load Data (Overview only)
                     loadOverview()
                     
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
                binInfo = binInfoResult.getOrNull()
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
