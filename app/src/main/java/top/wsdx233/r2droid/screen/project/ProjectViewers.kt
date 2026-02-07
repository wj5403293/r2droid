package top.wsdx233.r2droid.screen.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import top.wsdx233.r2droid.data.model.*

@Composable
fun HexViewer(
    hexRows: List<HexRow>, 
    totalSize: Long = 0L, 
    currentPos: Long = 0L,
    cursorAddress: Long = 0L, 
    onLoadMore: (Boolean) -> Unit,
    onScrollTo: (Long) -> Unit,
    onByteClick: (Long) -> Unit
) {
    if (hexRows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Data", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Infinite Scroll Logic
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            Pair(firstVisible < 5, totalItems > 0 && lastVisible >= totalItems - 5)
        }.collect { (nearTop, nearBottom) ->
            if (nearBottom) onLoadMore(true)
            if (nearTop) onLoadMore(false)
        }
    }

    // Auto-scroll to cursor
    androidx.compose.runtime.LaunchedEffect(cursorAddress, hexRows) {
        if (hexRows.isNotEmpty()) {
            val targetIndex = hexRows.indexOfFirst { it.addr <= cursorAddress && (it.addr + 16) > cursorAddress }
            if (targetIndex != -1) {
                // Calculate centering
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo.size
                val centerOffset = if (visibleItems > 0) visibleItems / 2 else 5
                val scrollIndex = (targetIndex - centerOffset).coerceAtLeast(0)
                listState.animateScrollToItem(scrollIndex)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Sticky Header: 0 1 2 3 ...
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(vertical = 4.dp)
        ) {
            Spacer(Modifier.width(70.dp)) // Addr space matching visual row
            // 8 columns
             (0..7).forEach { 
                 Text(
                     it.toString(), 
                     modifier = Modifier.weight(1f), 
                     textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                     fontSize = 12.sp,
                     color = Color.Gray
                 ) 
             }
            Spacer(Modifier.width(8.dp)) // Gap
            Text("01234567", fontSize = 12.sp, color = Color.Gray, letterSpacing = 2.sp)
            Spacer(Modifier.width(4.dp))
        }
        
        Box(Modifier.weight(1f)) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF0F0F0)), // Light bg
                    contentPadding = PaddingValues(0.dp) 
                ) {
                    items(hexRows) { row ->
                        // Determine if we split into 8-byte sub-rows
                        // 16 bytes total. 
                        // Row 1: bytes 0-7
                        // Row 2: bytes 8-15
                        
                        val b1 = row.bytes.take(8)
                        val b2 = row.bytes.drop(8)
                        
                        HexVisualRow(
                            addr = row.addr, 
                            bytes = b1, 
                            index = 0, 
                            cursorAddress = cursorAddress, 
                            onByteClick = onByteClick
                        )
                        if (b2.isNotEmpty()) {
                            HexVisualRow(
                                addr = row.addr + 8, 
                                bytes = b2, 
                                index = 1,
                                cursorAddress = cursorAddress,
                                onByteClick = onByteClick
                            )
                        }
                    }
                }
            }
            
            // Fast Scrollbar
            if (totalSize > 0) {
                 var sliderPos by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                 
                 androidx.compose.runtime.LaunchedEffect(currentPos) {
                     sliderPos = (currentPos.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                 }

                 // Custom Vertical Scrollbar
                 Box(
                     modifier = Modifier
                         .align(Alignment.CenterEnd)
                         .fillMaxHeight()
                         .width(24.dp)
                         .background(Color.Transparent)
                         .pointerInput(Unit) {
                             detectVerticalDragGestures { change, dragAmount ->
                                 val height = size.height
                                 val newY = (change.position.y / height).coerceIn(0f, 1f)
                                 onScrollTo((newY * totalSize).toLong())
                             }
                             detectTapGestures { offset ->
                                 val height = size.height
                                 val newY = (offset.y / height).coerceIn(0f, 1f)
                                 onScrollTo((newY * totalSize).toLong())
                             }
                         }
                 ) {
                     val thumbY = (currentPos.toFloat() / totalSize.toFloat())
                     val bias = (thumbY * 2 - 1).coerceIn(-1f, 1f)
                     Box(
                         Modifier
                             .align(androidx.compose.ui.BiasAlignment(0f, bias))
                             .size(8.dp, 40.dp)
                             .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                     )
                 }
            }
        }
        
        // Footer: Info
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
              Text("Pos: ${"0x%X".format(currentPos)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
              if (totalSize > 0L) {
                  Text("Size: ${"0x%X".format(totalSize)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
              }
        }
    }
}

@Composable
fun HexVisualRow(
    addr: Long, 
    bytes: List<Byte>, 
    index: Int, 
    cursorAddress: Long, 
    onByteClick: (Long) -> Unit
) {
    // 8 bytes row
    val oddRow = (addr / 8) % 2 == 1L
    val bgColor = if (oddRow) Color(0xFFE8EAF6) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .height(IntrinsicSize.Min)
    ) {
        // Address with gray background
        Box(
            modifier = Modifier
                .width(70.dp)
                .fillMaxHeight()
                .background(Color(0xFFDDDDDD)) // Gray background for address
                .padding(start = 4.dp, top = 2.dp)
        ) {
            Text(
                text = "%06X".format(addr), 
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        
        VerticalDivider()
        
        // Hex
        Row(Modifier.weight(1f)) {
             bytes.forEachIndexed { i, b ->
                val byteAddr = addr + i
                val isSelected = (byteAddr == cursorAddress)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onByteClick(byteAddr) }
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                         text = "%02X".format(b),
                         fontFamily = FontFamily.Monospace,
                         fontSize = 13.sp,
                         color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black,
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                         fontWeight = FontWeight.Medium
                    )
                }
             }
             // Padding if < 8 bytes
             repeat(8 - bytes.size) {
                 Spacer(Modifier.weight(1f))
             }
        }
        
        VerticalDivider()
        
        // ASCII
        Row(
            Modifier.width(100.dp).padding(start = 4.dp)
        ) {
            bytes.forEachIndexed { i, b ->
                val byteAddr = addr + i
                val isSelected = (byteAddr == cursorAddress)
                val c = b.toInt().toChar()
                val charStr = if (c.isISOControl() || !c.isDefined()) "." else c.toString()
                
                Box(
                    modifier = Modifier
                        .width(12.dp) // Fixed width per char approx
                        .clickable { onByteClick(byteAddr) }
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                     Text(
                        text = charStr,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black
                    )
                }
            }
        }
    }
}


@Composable
fun DisassemblyViewer(
    instructions: List<DisasmInstruction>, 
    cursorAddress: Long,
    onInstructionClick: (Long) -> Unit,
    onLoadMore: (Boolean) -> Unit
) {
    if (instructions.isEmpty()) {
         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Instructions", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
             val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            
            Pair(
               firstVisible < 5, 
               totalItems > 0 && lastVisible >= totalItems - 5
            )
        }.collect { (nearTop, nearBottom) ->
            if (nearBottom) onLoadMore(true)
            if (nearTop) onLoadMore(false)
        }
    }

    // Auto-scroll to cursor
    androidx.compose.runtime.LaunchedEffect(cursorAddress, instructions) {
        if (instructions.isNotEmpty()) {
            val targetIndex = instructions.indexOfFirst { it.addr == cursorAddress }
            if (targetIndex != -1) {
                // Determine centering
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo.size
                val centerOffset = if (visibleItems > 0) visibleItems / 2 else 5
                val scrollIndex = (targetIndex - centerOffset).coerceAtLeast(0)
                listState.animateScrollToItem(scrollIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
             contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(instructions) { instr ->
            DisasmRow(instr, isSelected = instr.addr == cursorAddress, onClick = { onInstructionClick(instr.addr) })
        }
    }
}

@Composable
fun DisasmRow(instr: DisasmInstruction, isSelected: Boolean, onClick: () -> Unit) {
    // Cutter style coloring logic
    val opcodeColor = when (instr.type) {
        "call", "ucall" -> Color(0xFF42A5F5) // Blue
        "jmp", "cjmp", "ujmp" -> Color(0xFF66BB6A) // Green
        "ret" -> Color(0xFFEF5350) // Red
        "push", "pop", "rpush" -> Color(0xFFAB47BC) // Purple
        "cmp", "test" -> Color(0xFFFFCA28) // Orange/Yellow
        "nop" -> Color.Gray
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    // Highlight fork/fail in comments if present? R2 pdj doesn't give comments easily in that struct unless in separate field.
    // We just show disasm.

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp) // tighter spacing
    ) {
        // Addr
        Text(
            text = "0x%08x".format(instr.addr),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(90.dp)
        )
        
        // Bytes (Optional - usually hidden in graph mode but shown in linear)
        // Let's show up to 8 bytes hex, if more then '..'
        val bytesStr = if (instr.bytes.length > 12) instr.bytes.take(12) + ".." else instr.bytes
        Text(
            text = bytesStr.padEnd(14),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.DarkGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(100.dp)
        )
        
        // Opcode / Disasm
        Text(
            text = instr.disasm,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else opcodeColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = if(instr.type in listOf("call", "jmp", "ret")) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DecompilationViewer(
    data: DecompilationData,
    cursorAddress: Long,
    onAddressClick: (Long) -> Unit
) {
    if (data.code.isBlank()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Decompilation Data", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var textLayoutResult by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val scrollState = androidx.compose.foundation.rememberScrollState()

    // Process annotations
    val annotatedString = buildAnnotatedString {
        append(data.code)
        
        data.annotations.forEach { note ->
            val start = note.start
            val end = note.end
            
            // Validate bounds
            if (start >= 0 && end <= data.code.length && start < end) {
                // Highlighting based on cursor
                // If annotation corresponds to current cursor address, highlight background
                if (note.offset == cursorAddress && cursorAddress != 0L) {
                    addStyle(
                        style = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer),
                        start = start,
                        end = end
                    )
                }
                
                val color = when(note.syntaxHighlight) {
                    "datatype" -> Color(0xFF569CD6) // Blue (VSCodeish)
                    "function_name" -> Color(0xFFFFD700) // Gold
                    "keyword" -> Color(0xFFC586C0) // Purple
                    "local_variable" -> Color(0xFF9CDCFE) // Light Blue
                    "global_variable" -> Color(0xFF4EC9B0) // Teal
                    "comment" -> Color(0xFF6A9955) // Green
                    "string" -> Color(0xFFCE9178) // Orange/Red
                    "offset" -> Color(0xFFB5CEA8) // Light Green
                    else -> null 
                }
                
                if (color != null) {
                    addStyle(
                        style = SpanStyle(color = color),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }
    
    // Auto-scroll logic
    val density = androidx.compose.ui.platform.LocalDensity.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    
    androidx.compose.runtime.LaunchedEffect(cursorAddress, textLayoutResult) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (cursorAddress == 0L) return@LaunchedEffect
        
        // Find annotation for cursor
        val note = data.annotations.firstOrNull { it.offset == cursorAddress }
        if (note != null && note.start < layout.layoutInput.text.length) {
            val line = layout.getLineForOffset(note.start)
            val top = layout.getLineTop(line)
            
            // Center the line (approx)
            val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
            val targetScroll = (top - screenHeightPx / 3).toInt().coerceAtLeast(0)
            
            scrollState.animateScrollTo(targetScroll)
        }
    }

    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = annotatedString,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFD4D4D4), // Standard light gray text
                lineHeight = 18.sp,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        if (pos.y <= layout.size.height) {
                            val offset = layout.getOffsetForPosition(pos)
                            val note = data.annotations.firstOrNull { it.start <= offset && it.end >= offset }
                            if (note != null && note.offset != 0L) {
                                onAddressClick(note.offset)
                            }
                        }
                    }
                }
            )
        }
    }
}
