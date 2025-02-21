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
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(1.dp)
            .background(color = Color.Gray, shape = RoundedCornerShape(15.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
