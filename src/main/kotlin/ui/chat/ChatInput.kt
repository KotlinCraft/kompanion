package ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatInput(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isProcessing: Boolean,
    onSendClick: () -> Unit,
    darkSecondary: Color
) {
    Surface(
        color = darkSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = darkSecondary,
                    textColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        if (isProcessing) "Thinking really hard..." else "Ask me about your code...",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.width(8.dp))

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }

            IconButton(onClick = onSendClick) {
                Icon(
                    if (isProcessing) Icons.Default.Close else Icons.Default.Send,
                    contentDescription = if (isProcessing) "Cancel" else "Send",
                    tint = Color.White
                )
            }
        }
    }
}
