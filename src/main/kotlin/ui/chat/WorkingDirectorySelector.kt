package ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javax.swing.JFileChooser

@Composable
fun WorkingDirectorySelector(
    workingDirectory: String,
    onWorkingDirectoryChange: (String) -> Unit,
    darkSecondary: Color
) {
    Card(
        backgroundColor = darkSecondary,
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth(0.3f)
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    val selected = selectDirectory(workingDirectory)
                    if (selected != null) {
                        onWorkingDirectoryChange(selected)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder, 
                contentDescription = "Select directory",
                tint = Color(0xFFFFCB6B),
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = workingDirectory,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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