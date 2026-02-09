package top.wsdx233.r2droid.feature.disasm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.core.data.model.DisasmInstruction
import top.wsdx233.r2droid.ui.theme.LocalAppFont

/**
 * Placeholder row shown when instruction data is not yet loaded.
 */
@Composable
fun DisasmPlaceholderRow() {
    val disasmPlaceholderBg = colorResource(R.color.disasm_placeholder_background)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        // Address placeholder
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(18.dp)
                .padding(end = 4.dp)
                .background(disasmPlaceholderBg, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
        )
        // Bytes placeholder
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(18.dp)
                .padding(end = 4.dp)
                .background(disasmPlaceholderBg, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
        )
        // Disasm placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .background(disasmPlaceholderBg, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Helper function to format address in a compact way
 * Removes 0x prefix and leading zeros for shorter display
 */
private fun formatCompactAddress(addr: Long): String {
    val hex = "%X".format(addr)
    // Keep at least 4 characters for readability
    return if (hex.length <= 4) hex else hex.trimStart('0').ifEmpty { "0" }
}

/**
 * Format jump index - show only last 2 digits
 */
private fun formatJumpIndex(index: Int): String {
    return (index % 100).toString().padStart(2, '0')
}

@Composable
fun DisasmRow(
    instr: DisasmInstruction, 
    isSelected: Boolean, 
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showMenu: Boolean = false,
    menuContent: @Composable () -> Unit = {},
    jumpIndex: Int? = null,           // Index for this jump (if it's a jump instruction)
    jumpTargetIndex: Int? = null      // Index if this is a jump target
) {
    // Column background colors (themed)
    val colJumpBg = colorResource(R.color.disasm_col_jump)
    val colAddressBg = colorResource(R.color.disasm_col_address)
    val colBytesBg = colorResource(R.color.disasm_col_bytes)
    val colOpcodeBg = colorResource(R.color.disasm_col_opcode)
    val colCommentBg = colorResource(R.color.disasm_col_comment)
    val colFlagBg = colorResource(R.color.disasm_col_flag)
    val colFuncHeaderBg = colorResource(R.color.disasm_col_func_header)
    val colR2CommentBg = colorResource(R.color.disasm_col_r2_comment)
    val colXrefBg = colorResource(R.color.disasm_col_xref)
    val colInlineCommentBg = colorResource(R.color.disasm_col_inline_comment)
    
    // Color definitions
    val commentColor = Color(0xFF6A9955)  // Green for comments
    val flagColor = Color(0xFF4EC9B0)     // Teal for flags
    val funcNameColor = Color(0xFFDCDCAA) // Yellow for function names
    val funcIconColor = Color(0xFF569CD6) // Blue for function icon
    val jumpOutColor = Color(0xFF66BB6A)  // Green arrow for jump out (external)
    val jumpInColor = Color(0xFFFFCA28)   // Yellow arrow for jump in (external)
    val jumpInternalColor = Color(0xFF64B5F6) // Blue for internal jumps
    val addressColor = Color(0xFF888888)  // Gray for address
    val bytesColor = Color(0xFF999999)    // Lighter gray for bytes
    
    // Cutter style coloring logic
    val opcodeColor = when (instr.type) {
        "call", "ucall", "ircall" -> Color(0xFF42A5F5) // Blue
        "jmp", "cjmp", "ujmp" -> Color(0xFF66BB6A) // Green
        "ret" -> Color(0xFFEF5350) // Red
        "push", "pop", "rpush" -> Color(0xFFAB47BC) // Purple
        "cmp", "test", "acmp" -> Color(0xFFFFCA28) // Orange/Yellow
        "nop" -> Color.Gray
        "lea" -> Color(0xFF4FC3F7) // Light Blue
        "mov" -> Color(0xFFA25410) // White/Light Gray
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    // Check if this is the start of a function
    val isFunctionStart = instr.fcnAddr > 0 && instr.addr == instr.fcnAddr
    
    // Check for external jump out/in
    val isExternalJumpOut = instr.isJumpOut()   
    val hasExternalJumpIn = instr.hasJumpIn()
    
    // Check if this is a jump instruction (internal or external)
    val isJumpInstruction = instr.type in listOf("jmp", "cjmp", "ujmp")
    val isInternalJump = isJumpInstruction && instr.jump != null && !isExternalJumpOut
    
    // Determine jump direction for internal jumps
    val jumpDirection = if (isInternalJump && instr.jump != null) {
        if (instr.jump > instr.addr) "↓" else "↑"
    } else null
    
    // Prepare bytes - always truncate with ... if too long (max 10 chars displayed)
    val bytesStr = instr.bytes.lowercase()
    val displayBytes = if (bytesStr.length > 10) bytesStr.take(8) + "…" else bytesStr
    
    // Prepare inline comment
    val inlineComment = buildString {
        if (instr.ptr != null) {
            append("; ${formatCompactAddress(instr.ptr)}")
        }
        if (instr.refptr && instr.refs.isNotEmpty()) {
            val dataRef = instr.refs.firstOrNull { it.type == "DATA" }
            if (dataRef != null) {
                if (isNotEmpty()) append(" ")
                append("[${formatCompactAddress(dataRef.addr)}]")
            }
        }
    }.trim()
    
    // Only comments go to secondary row (not bytes)
    val hasInlineComment = inlineComment.isNotEmpty()
    
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .pointerInput(onClick, onLongClick) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                }
        ) {
            // === Pre-instruction annotations ===
            
            // 1. Display flags (like ;-- _start: or ;-- rip:)
            if (instr.flags.isNotEmpty()) {
                instr.flags.forEach { flag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) Color.Transparent else colFlagBg)
                            .padding(start = 80.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ";-- $flag:",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else flagColor,
                            fontFamily = LocalAppFont.current,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            // 2. Display function header if this is function start
            if (isFunctionStart) {
                val funcSize = if (instr.fcnLast > instr.fcnAddr) instr.fcnLast - instr.fcnAddr else 0
                val funcName = instr.flags.firstOrNull { 
                    !it.startsWith("section.") && !it.startsWith("reloc.") 
                } ?: "fcn.${"%%08x".format(instr.addr)}"
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color.Transparent else colFuncHeaderBg)
                        .padding(start = 80.dp, top = 2.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blue function icon
                    Text(
                        text = "▶",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else funcIconColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "$funcSize: ",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontFamily = LocalAppFont.current,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$funcName ();",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else funcNameColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // === Main instruction row (compact, single line) ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump indicator column (fixed width) - with background color
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) Color.Transparent else colJumpBg),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // External jump out - green left arrow
                        isExternalJumpOut -> {
                            Text(
                                text = "←",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else jumpOutColor,
                                fontFamily = LocalAppFont.current,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // External jump in target - yellow right arrow
                        hasExternalJumpIn -> {
                            Text(
                                text = "→",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else jumpInColor,
                                fontFamily = LocalAppFont.current,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Internal jump instruction - blue arrow with direction and last 2 digits
                        isInternalJump && jumpIndex != null -> {
                            Text(
                                text = "${jumpDirection ?: ""}${formatJumpIndex(jumpIndex)}",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else jumpInternalColor,
                                fontFamily = LocalAppFont.current,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Jump target - show target indicator with last 2 digits
                        jumpTargetIndex != null -> {
                            Text(
                                text = "▸${formatJumpIndex(jumpTargetIndex)}",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else jumpInternalColor,
                                fontFamily = LocalAppFont.current,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                
                // Address column - compact format with background
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) Color.Transparent else colAddressBg)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = formatCompactAddress(instr.addr),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else addressColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Bytes column - always visible, truncated with ...
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) Color.Transparent else colBytesBg)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = displayBytes,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else bytesColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 10.sp
                    )
                }
                
                // Opcode / Disasm column - with background, takes remaining space
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) Color.Transparent else colOpcodeBg)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = instr.disasm,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else opcodeColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        fontWeight = if(instr.type in listOf("call", "jmp", "cjmp", "ret")) FontWeight.Bold else FontWeight.Normal
                    )
                }
                
                // Inline comment column (if present and short enough for same line)
                if (hasInlineComment && inlineComment.length <= 20) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (isSelected) Color.Transparent else colInlineCommentBg)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = inlineComment,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else commentColor,
                            fontFamily = LocalAppFont.current,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // === Secondary row for long inline comments (only comments, not bytes) ===
            if (hasInlineComment && inlineComment.length > 20) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else colInlineCommentBg)
                        .padding(start = 80.dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inlineComment,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else commentColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 10.sp
                    )
                }
            }
            
            // === Post-instruction comment (from radare2) ===
            if (!instr.comment.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else colR2CommentBg)
                        .padding(start = 80.dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "; ${instr.comment}",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else commentColor,
                        fontFamily = LocalAppFont.current,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Show xref comments for jump targets
            if (instr.xrefs.isNotEmpty()) {
                val codeXrefs = instr.xrefs.filter { it.type == "CODE" }
                if (codeXrefs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else colXrefBg)
                            .padding(start = 80.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val xrefText = if (codeXrefs.size == 1) {
                            "; XREF from ${formatCompactAddress(codeXrefs[0].addr)}"
                        } else {
                            "; XREF from ${codeXrefs.size} locations"
                        }
                        Text(
                            text = xrefText,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else commentColor,
                            fontFamily = LocalAppFont.current,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        
        // Render menu inside Box so it anchors to this row
        if (showMenu) {
            menuContent()
        }
    }
}
