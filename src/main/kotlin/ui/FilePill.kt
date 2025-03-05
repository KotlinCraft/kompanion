package ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@Composable
fun FilePill(
    fileName: String,
    fileId: UUID? = null,
    onRemove: ((UUID) -> Unit)? = null
) {
    // Create an interaction source to track hover state
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isCloseHovered by closeInteractionSource.collectIsHoveredAsState()
    
    // Animate color changes
    val pillBgColor = pillColor(fileName)
    val textColor = textColor(fileName)
    val closeButtonBgColor = animateColorAsState(
        targetValue = if (isCloseHovered) 
            Color.White.copy(alpha = 0.3f) 
        else 
            Color.Black.copy(alpha = 0.25f),
        animationSpec = tween(150)
    )
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color = pillBgColor)
                .padding(start = 12.dp, end = if (fileId != null && onRemove != null) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // Filename text
            Text(
                text = fileName,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = if (fileId != null && onRemove != null) 6.dp else 0.dp)
            )
            
            // Only show the remove button if we have both fileId and onRemove handler
            if (fileId != null && onRemove != null) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(closeButtonBgColor.value)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = null // Remove ripple effect
                        ) { 
                            onRemove(fileId) 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove file",
                        tint = textColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun textColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Color.White
        fileName.endsWith(".java") -> Color.White
        fileName.endsWith(".xml") -> Color.White
        fileName.endsWith(".gradle") -> Color.White
        fileName.endsWith(".html") -> Color.White
        fileName.endsWith(".css") -> Color.White
        fileName.endsWith(".js") -> Color.White
        fileName.endsWith(".json") -> Color.White
        fileName.endsWith(".md") -> Color.White
        fileName.endsWith(".sol") -> Color.White
        fileName.endsWith(".ts") -> Color.White
        fileName.endsWith(".tsx") -> Color.White
        fileName.endsWith(".jsx") -> Color.White
        else -> Color.Black
    }
}

private fun pillColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Color(0xFF3E8E4F) // Kotlin - Green
        fileName.endsWith(".java") -> Color(0xFFB07219) // Java - Brown
        fileName.endsWith(".xml") -> Color(0xFFEB682D) // XML - Orange
        fileName.endsWith(".gradle") -> Color(0xFF02303A) // Gradle - Dark blue
        fileName.endsWith(".html") -> Color(0xFFE44D26) // HTML - Orange
        fileName.endsWith(".css") -> Color(0xFF264DE4) // CSS - Blue
        fileName.endsWith(".js") -> Color(0xFFF0DB4F) // JS - Yellow
        fileName.endsWith(".json") -> Color(0xFF8F8F8F) // JSON - Gray
        fileName.endsWith(".md") -> Color(0xFF546E7A) // Markdown - Blue gray
        fileName.endsWith(".sol") -> Color(0xFF4E5D94) // Solidity - Purple-blue
        fileName.endsWith(".ts") -> Color(0xFF3178C6) // TypeScript - Blue
        fileName.endsWith(".tsx") || fileName.endsWith(".jsx") -> Color(0xFF61DAFB) // React - Light blue
        else -> Color(0xFFE0E0E0) // Default - Light grey
    }
}