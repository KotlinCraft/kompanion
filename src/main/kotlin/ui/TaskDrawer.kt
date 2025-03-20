package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Helper function to map task status to a color indicator.
private fun statusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.TODO -> Color.Red
        TaskStatus.DOING -> Color.Yellow
        TaskStatus.DONE -> Color.Green
    }
}

@Composable
fun TaskDrawer(tasks: List<Task> = listOf(
    Task("Dummy Task 1", TaskStatus.TODO),
    Task("Dummy Task 2", TaskStatus.DOING),
    Task("Dummy Task 3", TaskStatus.DONE)
)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2D2D3F), shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Tasks",
            color = Color.White,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Iterate over tasks displaying name and colored indicator.
        tasks.forEach { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor(task.status))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.name,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}