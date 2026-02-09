package top.wsdx233.r2droid.core.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.wsdx233.r2droid.R

@Composable
fun JumpDialog(
    initialAddress: String,
    onDismiss: () -> Unit,
    onJump: (Long) -> Unit
) {
    var text by remember { mutableStateOf(initialAddress) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val title = stringResource(R.string.dialog_jump_title)
    val addressLabel = stringResource(R.string.dialog_jump_address_label)
    val addressHint = stringResource(R.string.dialog_jump_address_hint)
    val errorEmpty = stringResource(R.string.dialog_jump_error_empty)
    val errorInvalid = stringResource(R.string.dialog_jump_error_invalid)
    val goLabel = stringResource(R.string.dialog_jump_go)
    val cancelLabel = stringResource(R.string.dialog_cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { 
                        text = it 
                        error = null
                    },
                    label = { Text(addressLabel) },
                    placeholder = { Text(addressHint) },
                    isError = error != null,
                    singleLine = true
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val addrStr = text.removePrefix("0x").trim()
                    if (addrStr.isBlank()) {
                        error = errorEmpty
                        return@TextButton
                    }
                    try {
                        val addr = addrStr.toLong(16)
                        onJump(addr)
                        onDismiss()
                    } catch (e: NumberFormatException) {
                         error = errorInvalid
                    }
                }
            ) {
                Text(goLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        }
    )
}
