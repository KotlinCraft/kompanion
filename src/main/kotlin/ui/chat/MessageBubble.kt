package ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageBubble(message: ChatMessage) {
    Surface(
        color = if (message.isUser) Color(0xFF343541) else Color(0xFF444654),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (message.isUser) "You: " else "Kompanion: ",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message.content,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}
