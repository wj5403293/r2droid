package top.wsdx233.r2droid.feature.hex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.ui.theme.LocalAppFont
import androidx.compose.foundation.clickable

@Composable
fun HexPlaceholderRow(
    addr: Long,
    hexAddressBackground: Color = Color(0xFFDDDDDD),
    hexAddressText: Color = Color.Black,
    hexDivider: Color = Color(0xFFBDBDBD)
) {
    val hexPlaceholderRow = colorResource(R.color.hex_placeholder_row)
    val hexPlaceholderBlock = colorResource(R.color.hex_placeholder_block)
    val appFont = LocalAppFont.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(hexPlaceholderRow)
            .height(IntrinsicSize.Min)
    ) {
        // Address
        Box(
            modifier = Modifier
                .width(70.dp)
                .fillMaxHeight()
                .background(hexAddressBackground)
                .padding(start = 4.dp, top = 2.dp)
        ) {
            Text(
                text = "%06X".format(addr),
                color = hexAddressText,
                fontFamily = appFont,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        
        VerticalDivider()
        
        // Placeholder hex area
        Row(Modifier.weight(1f)) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .padding(2.dp)
                        .background(hexPlaceholderBlock, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                )
            }
        }
        
        VerticalDivider()
        
        // Placeholder ASCII area
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
                .padding(4.dp)
                .background(hexPlaceholderBlock, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun HexVisualRow(
    addr: Long, 
    bytes: List<Byte>, 
    index: Int, 
    cursorAddress: Long,
    selectedColumn: Int,
    highlightColor: Color,
    onByteClick: (Long) -> Unit,
    onByteLongClick: (Long) -> Unit = {},
    showMenu: Boolean = false,
    menuTargetAddress: Long? = null,
    menuContent: @Composable () -> Unit = {},
    editingBuffer: String = "",
    hexAddressBackground: Color = Color(0xFFDDDDDD),
    hexAddressText: Color = Color(0xFF424242),
    hexRowEven: Color = Color.White,
    hexRowOdd: Color = Color(0xFFE8EAF6),
    hexDivider: Color = Color(0xFFBDBDBD),
    hexByteText: Color = Color.Black
) {
    // 8 bytes row
    val oddRow = (addr / 8) % 2 == 1L
    val appFont = LocalAppFont.current
    
    // Check if this row contains the cursor
    val rowStartAddr = addr
    val rowEndAddr = addr + bytes.size - 1
    val isRowSelected = cursorAddress >= rowStartAddr && cursorAddress <= rowEndAddr
    
    // Base background: alternating colors (zebra stripes)
    val baseBgColor = if (oddRow) hexRowOdd else hexRowEven

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(baseBgColor)
            .height(IntrinsicSize.Min)
    ) {
        // Address with gray background - CENTERED, BOLD, DARK GRAY
        Box(
            modifier = Modifier
                .width(70.dp)
                .fillMaxHeight()
                .background(hexAddressBackground), // Gray background for address
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%06X".format(addr), 
                color = hexAddressText, // Dark gray
                fontFamily = appFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        
        VerticalDivider()
        
        // Hex - with divider between 4th and 5th columns, and column highlighting
        Row(Modifier.weight(1f).fillMaxHeight()) {
             bytes.forEachIndexed { i, b ->
                val byteAddr = addr + i
                val isSelected = (byteAddr == cursorAddress)
                val isColumnHighlighted = (i == selectedColumn)
                
                // Add divider before column 4 (between 3rd and 4th column, 0-indexed)
                if (i == 4) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(hexDivider)
                    )
                }
                
                // Background: base stays transparent, overlay highlight if needed
                // For selected cell: use primary container
                // For column/row highlight: use semi-transparent yellow overlay
                
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(byteAddr) {
                                detectTapGestures(
                                    onTap = { onByteClick(byteAddr) },
                                    onLongPress = { onByteLongClick(byteAddr) }
                                )
                            }
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        // Overlay: row highlight or column highlight (30% transparent yellow)
                        if (!isSelected && (isRowSelected || isColumnHighlighted)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(highlightColor)
                            )
                        }
                        
                        // Show editing buffer overlay if selected and typing
                        val displayText = if (isSelected && editingBuffer.isNotEmpty()) {
                             editingBuffer
                        } else {
                             "%02X".format(b)
                        }
                        
                        val textColor = if (isSelected) {
                            if (editingBuffer.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            hexByteText
                        }

                        Text(
                             text = displayText,
                             fontFamily = appFont,
                             fontSize = 13.sp,
                             color = textColor,
                             textAlign = TextAlign.Center,
                             fontWeight = FontWeight.Medium
                        )
                        
                        // Render menu if this specific byte is the target and menu is showing
                        if (showMenu && byteAddr == menuTargetAddress) {
                             menuContent()
                        }
                    }
             }
             // Padding if < 8 bytes
             repeat(8 - bytes.size) { padIndex ->
                 val actualIndex = bytes.size + padIndex
                 // Add divider before column 4 even in padding area
                 if (actualIndex == 4) {
                     Box(
                         modifier = Modifier
                             .width(1.dp)
                             .fillMaxHeight()
                             .background(hexDivider)
                     )
                 }
                 Spacer(Modifier.weight(1f))
             }
        }
        
        VerticalDivider()
        
        // ASCII with column highlighting
        Row(
            Modifier.width(100.dp).padding(start = 4.dp)
        ) {
            bytes.forEachIndexed { i, b ->
                val byteAddr = addr + i
                val isSelected = (byteAddr == cursorAddress)
                val isColumnHighlighted = (i == selectedColumn)
                val c = b.toInt().toChar()
                val charStr = if (c.isISOControl() || !c.isDefined()) "." else c.toString()
                
                Box(
                    modifier = Modifier
                        .width(12.dp) // Fixed width per char approx
                        .clickable { onByteClick(byteAddr) }
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    // Overlay: row highlight or column highlight (30% transparent yellow)
                    if (!isSelected && (isRowSelected || isColumnHighlighted)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(highlightColor)
                        )
                    }
                     Text(
                        text = charStr,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else hexByteText
                    )
                }
            }
        }
    }
}
