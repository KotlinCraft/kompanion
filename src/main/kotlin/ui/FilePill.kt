package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilePill(fileName: String) {
    Text(
        text = fileName,
        color = textColor(fileName),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(2.dp)
            .background(color = pillColor(fileName), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
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
        else -> Color.Black
    }
}

private fun pillColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Color(0xFF2E6F40) // Kotlin - Green
        fileName.endsWith(".java") -> Color(0xFFB07219) // Java - Brown
        fileName.endsWith(".xml") -> Color(0xFFEB682D) // XML - Orange
        fileName.endsWith(".gradle") -> Color(0xFF02303A) // Gradle - Dark blue
        fileName.endsWith(".html") -> Color(0xFFE44D26) // HTML - Orange
        fileName.endsWith(".css") -> Color(0xFF264DE4) // CSS - Blue
        fileName.endsWith(".js") -> Color(0xFFF7DF1E) // JS - Yellow
        else -> Color(0xFFE0E0E0) // Default - Light grey
    }
}