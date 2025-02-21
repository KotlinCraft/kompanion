package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilePill(fileName: String) {
    Text(
        text = fileName,
        color = textColor(fileName),
        fontSize = 12.sp,
        modifier = Modifier
            .padding(1.dp)
            .background(color = pillColor(fileName), shape = RoundedCornerShape(15.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun textColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Color.White
        else -> Color.Black
    }
}

private fun pillColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Color(0xFF2E6F40)
        else -> Color.LightGray
    }
}
