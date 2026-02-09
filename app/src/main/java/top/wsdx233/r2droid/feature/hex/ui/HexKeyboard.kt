package top.wsdx233.r2droid.feature.hex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.wsdx233.r2droid.ui.theme.LocalAppFont

@Composable
fun HexKeyboard(
    onNibbleClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit
) {
    // Layout like reference image: 2 rows of 8 keys
    val row1 = listOf('0', '1', '2', '3', '4', '5', '6', '7')
    val row2 = listOf('8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    
    // Colors matching the reference image (dark theme with cyan accents)
    val keyBackground = Color(0xFF2D2D2D) // Dark gray key background
    val keyTextColor = Color(0xFF4DD0E1) // Cyan text color
    val containerColor = Color(0xFF1A1A1A) // Very dark container
    
    Surface(
        color = containerColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // First row: 0-7
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row1.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(keyBackground, RoundedCornerShape(4.dp))
                            .clickable { onNibbleClick(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            color = keyTextColor,
                            fontFamily = LocalAppFont.current,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Second row: 8-F with keyboard icon at the end
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row2.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(keyBackground, RoundedCornerShape(4.dp))
                            .clickable { onNibbleClick(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            color = keyTextColor,
                            fontFamily = LocalAppFont.current,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Action row: Backspace and Close
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Backspace button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(Color(0xFF3D3D3D), RoundedCornerShape(4.dp))
                        .clickable { onBackspace() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Backspace",
                        tint = Color(0xFFE0E0E0)
                    )
                }
                
                // Close/Hide keyboard button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(Color(0xFF3D3D3D), RoundedCornerShape(4.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close Keyboard",
                        tint = Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}
