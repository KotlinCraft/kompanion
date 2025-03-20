package ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.task.TaskItem
import ui.task.TaskStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskView(
    task: TaskItem,
    onDelete: (String) -> Unit
) {
    val successColor = Color(0xFF43B581)
    val warningColor = Color(0xFFFAA61A)
    val errorColor = Color(0xFFED4245)
    val defaultColor = Color(0xFF4F545C)
    val darkCard = Color(0xFF2F3136)

    val infiniteTransition = rememberInfiniteTransition()
    val pulsatingAlpha = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val taskColor = when (task.status) {
        TaskStatus.COMPLETED -> successColor
        TaskStatus.IN_PROGRESS -> warningColor
        TaskStatus.FAILED -> errorColor
        TaskStatus.CANCELLED -> errorColor.copy(alpha = 0.6f)
        TaskStatus.PENDING -> defaultColor
    }

    val taskIcon = when (task.status) {
        TaskStatus.COMPLETED -> Icons.Filled.CheckCircle
        TaskStatus.IN_PROGRESS -> Icons.Filled.Sync
        TaskStatus.FAILED -> Icons.Filled.Error
        TaskStatus.CANCELLED -> Icons.Filled.Cancel
        TaskStatus.PENDING -> Icons.Filled.HourglassEmpty
    }

    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeString = formatter.format(Date(task.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(darkCard)
            .border(1.dp, taskColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable { /* Potential future expansion */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task status icon with pulsating effect for in-progress tasks
            Icon(
                imageVector = taskIcon,
                contentDescription = "Task status",
                tint = taskColor,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (task.status == TaskStatus.IN_PROGRESS) pulsatingAlpha.value else 1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Task information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.description,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = timeString,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            
            // Delete button
            IconButton(
                onClick = { onDelete(task.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete task",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}