package com.yourdomain.kompanion.ui.components

import ai.LLMProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import config.Provider

@Composable
fun ProviderSelector(
    currentProvider: Provider,
    onProviderSelected: (Provider) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2D2D3A))
                .border(
                    width = 1.dp,
                    color = Color(0xFF3D3D4D),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Provider logo or icon could be added here
            val providerText = when (currentProvider) {
                Provider.ANTHROPIC -> "Claude"
                Provider.OPENAI -> "GPT"
            }
            
            Text(
                text = providerText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Provider",
                tint = Color.White
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2D2D3A))
        ) {
            Provider.values().forEach { provider ->
                val providerName = when (provider) {
                    Provider.ANTHROPIC -> "Claude (Anthropic)"
                    Provider.OPENAI -> "GPT (OpenAI)"
                }
                
                DropdownMenuItem(
                    onClick = {
                        onProviderSelected(provider)
                        expanded = false
                    }
                ) {
                    Text(
                        text = providerName,
                        color = if (provider == currentProvider) Color(0xFF8B8BEC) else Color.White
                    )
                }
            }
        }
    }
}
