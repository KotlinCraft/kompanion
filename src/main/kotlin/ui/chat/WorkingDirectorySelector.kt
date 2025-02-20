package ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javax.swing.JFileChooser

@Composable
fun WorkingDirectorySelector(
    workingDirectory: String,
    onWorkingDirectoryChange: (String) -> Unit,
    darkSecondary: Color
) {
    Surface(
        color = darkSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workingDirectory,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val selected = selectDirectory(workingDirectory)
                        if (selected != null) {
                            onWorkingDirectoryChange(selected)
                        }
                    }
            )
        }
    }
}

fun selectDirectory(initialDirectory: String): String? {
    val chooser = JFileChooser(initialDirectory)
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}
