package top.wsdx233.r2droid.feature.bininfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.core.data.model.BinInfo
import top.wsdx233.r2droid.ui.theme.LocalAppFont

@Composable
fun OverviewCard(info: BinInfo) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.binary_overview_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            InfoRow(stringResource(R.string.binary_arch), info.arch)
            InfoRow(stringResource(R.string.binary_bits), "${info.bits}")
            InfoRow(stringResource(R.string.binary_os), info.os)
            InfoRow(stringResource(R.string.binary_type), info.type)
            InfoRow(stringResource(R.string.binary_machine), info.machine)
            InfoRow(stringResource(R.string.binary_language), info.language)
            InfoRow(stringResource(R.string.binary_compiled), info.compiled)
            InfoRow(stringResource(R.string.binary_subsystem), info.subSystem)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = LocalAppFont.current
        )
    }
}
