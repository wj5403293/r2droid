package top.wsdx233.r2droid.feature.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.wsdx233.r2droid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisConfigScreen(
    filePath: String,
    onStartAnalysis: (cmd: String, writable: Boolean, flags: String) -> Unit
) {
    var selectedLevel by remember { mutableStateOf("aaa") }
    var customCmd by remember { mutableStateOf("") }
    var isWritable by remember { mutableStateOf(false) }
    var customFlags by remember { mutableStateOf("") }
    
    val levels = listOf(
        stringResource(R.string.analysis_level_none) to "none",
        stringResource(R.string.analysis_level_auto) to "aaa",
        stringResource(R.string.analysis_level_experimental) to "aaaa",
        stringResource(R.string.analysis_level_custom) to "custom"
    )
    
    val context = LocalContext.current
    var fileSize by remember { mutableStateOf(0L) }
    
    androidx.compose.runtime.LaunchedEffect(filePath) {
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                fileSize = file.length()
            } else if (filePath.startsWith("content://")) {
                val uri = android.net.Uri.parse(filePath)
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val isHeavyAnalysis = selectedLevel == "aaa" || selectedLevel == "aaaa"
    val isLargeFile = fileSize > 1024 * 1024 // 1MB
    val showWarning = isHeavyAnalysis && isLargeFile

    Scaffold(
        topBar = {
             CenterAlignedTopAppBar(title = { Text(stringResource(R.string.analysis_config_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // File Info
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.analysis_target_file), style = MaterialTheme.typography.labelMedium)
                    Text(filePath, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            // Analysis Level
            Text(stringResource(R.string.analysis_level_title), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLevel = value }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedLevel == value),
                            onClick = { selectedLevel = value }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
            
            if (selectedLevel == "custom") {
                OutlinedTextField(
                    value = customCmd,
                    onValueChange = { customCmd = it },
                    label = { Text(stringResource(R.string.analysis_custom_cmd_label)) },
                    placeholder = { Text(stringResource(R.string.analysis_custom_cmd_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Warning Card
            if (showWarning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.analysis_large_file_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.analysis_large_file_warning_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Startup Options
            Text(stringResource(R.string.analysis_startup_options), style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isWritable = !isWritable }) {
                Checkbox(checked = isWritable, onCheckedChange = { isWritable = it })
                Text(stringResource(R.string.analysis_writable_mode))
            }
            
            OutlinedTextField(
                value = customFlags,
                onValueChange = { customFlags = it },
                label = { Text(stringResource(R.string.analysis_startup_flags_label)) },
                placeholder = { Text(stringResource(R.string.analysis_startup_flags_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val finalCmd = if (selectedLevel == "custom") customCmd else selectedLevel
                    onStartAnalysis(finalCmd, isWritable, customFlags)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.analysis_start_btn))
            }
        }
    }
}
