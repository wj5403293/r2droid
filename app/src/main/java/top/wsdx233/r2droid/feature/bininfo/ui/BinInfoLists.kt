package top.wsdx233.r2droid.feature.bininfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.core.data.model.FunctionInfo
import top.wsdx233.r2droid.core.data.model.ImportInfo
import top.wsdx233.r2droid.core.data.model.Relocation
import top.wsdx233.r2droid.core.data.model.Section
import top.wsdx233.r2droid.core.data.model.StringInfo
import top.wsdx233.r2droid.core.data.model.Symbol
import top.wsdx233.r2droid.core.ui.components.FilterableList
import top.wsdx233.r2droid.core.ui.components.ListItemActions
import top.wsdx233.r2droid.core.ui.components.UnifiedListItemWrapper
import top.wsdx233.r2droid.ui.theme.LocalAppFont

@Composable
fun SectionList(sections: List<Section>, actions: ListItemActions, onRefresh: (() -> Unit)? = null) {
    FilterableList(
        items = sections,
        filterPredicate = { item, query -> item.name.contains(query, ignoreCase = true) },
        placeholder = stringResource(R.string.search_sections_hint),
        onRefresh = onRefresh
    ) { section ->
        SectionItem(section, actions)
    }
}

@Composable
fun SectionItem(section: Section, actions: ListItemActions) {
    UnifiedListItemWrapper(
        title = section.name,
        address = section.vAddr,
        fullText = "Section: ${section.name}, Size: ${section.size}, Perm: ${section.perm}, VAddr: 0x${section.vAddr.toString(16)}",
        actions = actions
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = section.perm,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Size: ${section.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "VAddr: 0x${section.vAddr.toString(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = LocalAppFont.current
                    )
                }
            }
        }
    }
}

@Composable
fun SymbolList(symbols: List<Symbol>, actions: ListItemActions, onRefresh: (() -> Unit)? = null) {
    FilterableList(
        items = symbols,
        filterPredicate = { item, query -> item.name.contains(query, ignoreCase = true) },
        placeholder = stringResource(R.string.search_symbols_hint),
        onRefresh = onRefresh
    ) { symbol ->
        SymbolItem(symbol, actions)
    }
}

@Composable
fun SymbolItem(symbol: Symbol, actions: ListItemActions) {
    UnifiedListItemWrapper(
        title = symbol.name,
        address = symbol.vAddr,
        fullText = "Symbol: ${symbol.name}, Type: ${symbol.type}, VAddr: 0x${symbol.vAddr.toString(16)}",
        actions = actions
    ) {
        OutlinedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = symbol.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = symbol.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "0x${symbol.vAddr.toString(16)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = LocalAppFont.current,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ImportList(imports: List<ImportInfo>, actions: ListItemActions, onRefresh: (() -> Unit)? = null) {
    FilterableList(
        items = imports,
        filterPredicate = { item, query -> item.name.contains(query, ignoreCase = true) },
        placeholder = stringResource(R.string.search_imports_hint),
        onRefresh = onRefresh
    ) { item ->
        ImportItem(item, actions)
    }
}

@Composable
fun ImportItem(importInfo: ImportInfo, actions: ListItemActions) {
    UnifiedListItemWrapper(
        title = importInfo.name,
        address = if(importInfo.plt != 0L) importInfo.plt else null,
        fullText = "Import: ${importInfo.name}, Type: ${importInfo.type}, PLT: 0x${importInfo.plt.toString(16)}",
        actions = actions
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = importInfo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Type: ${importInfo.type}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (importInfo.plt != 0L) {
                     Text(
                        text = "PLT: 0x${importInfo.plt.toString(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = LocalAppFont.current
                    )
                }
            }
        }
    }
}

@Composable
fun RelocationList(relocations: List<Relocation>, actions: ListItemActions, onRefresh: (() -> Unit)? = null) {
    FilterableList(
        items = relocations,
        filterPredicate = { item, query -> item.name.contains(query, ignoreCase = true) },
        placeholder = stringResource(R.string.search_relocations_hint),
        onRefresh = onRefresh
    ) { relocation ->
        RelocationItem(relocation, actions)
    }
}

@Composable
fun RelocationItem(relocation: Relocation, actions: ListItemActions) {
    UnifiedListItemWrapper(
        title = relocation.name,
        address = relocation.vAddr,
        fullText = "Relocation: ${relocation.name}, Type: ${relocation.type}, VAddr: 0x${relocation.vAddr.toString(16)}",
        actions = actions
    ) {
        ListItem(
            headlineContent = { Text(relocation.name, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = { Text("Type: ${relocation.type}", style = MaterialTheme.typography.labelSmall) },
            trailingContent = {
                Text(
                    "0x${relocation.vAddr.toString(16)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = LocalAppFont.current
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
fun StringList(strings: List<StringInfo>, actions: ListItemActions, onRefresh: (() -> Unit)? = null) {
    FilterableList(
        items = strings,
        filterPredicate = { item, query -> item.string.contains(query, ignoreCase = true) },
        placeholder = stringResource(R.string.search_strings_hint),
        onRefresh = onRefresh
    ) { str ->
        StringItem(str, actions)
    }
}

@Composable
fun StringItem(stringInfo: StringInfo, actions: ListItemActions) {
    UnifiedListItemWrapper(
        title = stringInfo.string,
        address = stringInfo.vAddr,
        fullText = "String: ${stringInfo.string}, Section: ${stringInfo.section}, VAddr: 0x${stringInfo.vAddr.toString(16)}",
        actions = actions
    ) {
        Card(
            border = null,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                Text(
                    text = stringInfo.string,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     Text(
                        text = "0x${stringInfo.vAddr.toString(16)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = LocalAppFont.current
                    )
                    Text(
                        text = stringInfo.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = stringInfo.section,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun FunctionList(functions: List<FunctionInfo>, actions: ListItemActions, onRefresh: (() -> Unit)? = null) {
    FilterableList(
        items = functions,
        filterPredicate = { item, query -> item.name.contains(query, ignoreCase = true) },
        placeholder = stringResource(R.string.search_functions_hint),
        onRefresh = onRefresh
    ) { func ->
        FunctionItem(func, actions)
    }
}

@Composable
fun FunctionItem(func: FunctionInfo, actions: ListItemActions) {
    UnifiedListItemWrapper(
        title = func.name,
        address = func.addr,
        fullText = "Function: ${func.name}, Addr: 0x${func.addr.toString(16)}, Size: ${func.size}, BBs: ${func.nbbs}, Signature: ${func.signature}",
        actions = actions
    ) {
        ElevatedCard {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = func.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "sz: ${func.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "0x${func.addr.toString(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = LocalAppFont.current
                    )
                    Text(
                        text = "bbs: ${func.nbbs}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (func.signature.isNotEmpty()) {
                         Text(
                            text = func.signature,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
