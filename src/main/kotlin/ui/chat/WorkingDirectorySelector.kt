package ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorkingDirectorySelector(
    workingDirectory: String,
    onWorkingDirectoryChange: (String) -> Unit,
    darkSecondary: Color
) {
    var isEditingDirectory by remember { mutableStateOf(false) }

    Surface(
        color = darkSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditingDirectory) {
                TextField(
                    value = workingDirectory,
                    onValueChange = onWorkingDirectoryChange,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = darkSecondary,
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { isEditingDirectory = false })
                )
            } else {
                Text(
                    workingDirectory,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isEditingDirectory = true }
                )
            }
        }
    }
}
