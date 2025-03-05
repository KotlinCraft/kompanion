package ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A futuristic neural network animation component to be used as a loading indicator
 * for AI thinking state. Implemented using Compose Canvas for better compatibility.
 */
@Composable
fun NeuralNetworkLoadingIndicatorCanvas() {
    // Background color
    val backgroundColor = Color(0xFF0F0F14)
    
    // Define colors for the neural network
    val primaryColor = Color(0xFF9333EA) // Purple
    val secondaryColor = Color(0xFFA855F7) // Lighter purple
    val accentColor = Color(0xFFC084FC) // Even lighter purple
    val particleColor = Color(0xFFF0ABFC) // Light pink/purple
    
    // Set up animations
    val infiniteTransition = rememberInfiniteTransition("neuralNetworkAnimation")
    
    // Pulse animation for neurons
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neuralPulse"
    )
    
    // Flow animation for data transfer effect
    val flowAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dataFlow"
    )
    
    // Particle animation for neuron bursts
    val particleAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleBurst"
    )
    
    // Secondary flow for cross connections
    val secondaryFlowAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondaryFlow"
    )
    
    // Loading dots animation
    val loadingDotsAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingDots"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color = backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                // Define neuron positions 
                val mainPathY = height * 0.5f
                val neurons = listOf(
                    Offset(width * 0.06f, mainPathY),
                    Offset(width * 0.19f, mainPathY),
                    Offset(width * 0.34f, mainPathY),
                    Offset(width * 0.5f, mainPathY),
                    Offset(width * 0.66f, mainPathY),
                    Offset(width * 0.81f, mainPathY),
                    Offset(width * 0.94f, mainPathY)
                )
                
                // Branch neurons
                val branchNeurons = listOf(
                    Offset(width * 0.28f, height * 0.25f),   // Top branch 1
                    Offset(width * 0.43f, height * 0.75f),   // Bottom branch 1
                    Offset(width * 0.56f, height * 0.15f),   // Top branch 2
                    Offset(width * 0.75f, height * 0.8f)     // Bottom branch 2
                )
                
                // Draw connections first (behind neurons)
                drawConnections(
                    neurons = neurons,
                    branchNeurons = branchNeurons,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    flowPosition = flowAnimation,
                    secondaryFlowPosition = secondaryFlowAnimation,
                    pulseScale = pulseAnimation
                )
                
                // Draw data particles
                drawDataParticles(
                    neurons = neurons,
                    branchNeurons = branchNeurons,
                    particleColor = particleColor,
                    flowPosition = flowAnimation,
                    secondaryFlowPosition = secondaryFlowAnimation
                )
                
                // Draw neurons over connections
                drawNeurons(
                    neurons = neurons,
                    branchNeurons = branchNeurons,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    pulseScale = pulseAnimation,
                    particleAnimation = particleAnimation
                )
                
                // Draw loading indicator dots at the bottom
                drawLoadingDots(
                    dotAnimation = loadingDotsAnimation,
                    dotColor = accentColor,
                    centerX = width / 2,
                    centerY = height * 0.9f
                )
            }
        }
    }
}

private fun DrawScope.drawConnections(
    neurons: List<Offset>,
    branchNeurons: List<Offset>,
    primaryColor: Color,
    secondaryColor: Color,
    flowPosition: Float,
    secondaryFlowPosition: Float,
    pulseScale: Float
) {
    // Main path connections
    for (i in 0 until neurons.size - 1) {
        // Gradient shader for main connections
        val gradient = Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.6f * pulseScale),
                secondaryColor.copy(alpha = 0.8f * pulseScale),
                primaryColor.copy(alpha = 0.6f * pulseScale)
            )
        )
        
        // Draw the main neural path
        drawLine(
            brush = gradient,
            start = neurons[i],
            end = neurons[i + 1],
            strokeWidth = 2f * pulseScale,
            pathEffect = PathEffect.cornerPathEffect(20f)
        )
    }
    
    // Branch connections (simplified as direct lines)
    // Top branches
    drawLine(
        color = secondaryColor.copy(alpha = 0.7f * pulseScale),
        start = neurons[1], // Connect from second main neuron
        end = branchNeurons[0],
        strokeWidth = 1.5f * pulseScale
    )
    
    drawLine(
        color = secondaryColor.copy(alpha = 0.7f * pulseScale),
        start = neurons[3], // Connect from middle main neuron
        end = branchNeurons[2],
        strokeWidth = 1.5f * pulseScale
    )
    
    // Bottom branches
    drawLine(
        color = secondaryColor.copy(alpha = 0.7f * pulseScale),
        start = neurons[2], // Connect from third main neuron
        end = branchNeurons[1],
        strokeWidth = 1.5f * pulseScale
    )
    
    drawLine(
        color = secondaryColor.copy(alpha = 0.7f * pulseScale),
        start = neurons[4], // Connect from fifth main neuron
        end = branchNeurons[3],
        strokeWidth = 1.5f * pulseScale
    )
    
    // Cross connections
    drawLine(
        color = primaryColor.copy(alpha = 0.5f * pulseScale),
        start = branchNeurons[0],
        end = branchNeurons[2],
        strokeWidth = 1f * pulseScale
    )
    
    drawLine(
        color = primaryColor.copy(alpha = 0.5f * pulseScale),
        start = branchNeurons[1],
        end = branchNeurons[3],
        strokeWidth = 1f * pulseScale
    )
}

private fun DrawScope.drawNeurons(
    neurons: List<Offset>,
    branchNeurons: List<Offset>,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    pulseScale: Float,
    particleAnimation: Float
) {
    // Draw main path neurons with pulsing effect
    neurons.forEachIndexed { index, neuron ->
        // Alternate colors
        val neuronColor = if (index % 2 == 0) primaryColor else secondaryColor
        val neuronSize = when (index) {
            0, neurons.lastIndex -> 6f  // First and last are smaller
            2, 4 -> 7f                  // Medium
            else -> 8f                  // Others are larger
        }
        
        // Outer glow effect
        drawCircle(
            color = neuronColor.copy(alpha = 0.3f),
            radius = (neuronSize + 4) * pulseScale,
            center = neuron
        )
        
        // Neuron center
        drawCircle(
            color = neuronColor,
            radius = neuronSize * pulseScale,
            center = neuron
        )
        
        // Add particle bursts to some of the main neurons
        if (index in listOf(1, 3, 4)) {
            val particleBurstProgress = (particleAnimation + index * 0.3f) % 1f
            if (particleBurstProgress < 0.5f) {
                val burstRadius = 20f * particleBurstProgress
                drawCircle(
                    color = accentColor.copy(alpha = 0.5f * (1 - particleBurstProgress * 2)),
                    radius = burstRadius,
                    center = neuron
                )
            }
        }
    }
    
    // Draw branch neurons
    branchNeurons.forEach { neuron ->
        // Outer glow effect
        drawCircle(
            color = accentColor.copy(alpha = 0.3f),
            radius = 7f * pulseScale,
            center = neuron
        )
        
        // Neuron center
        drawCircle(
            color = accentColor,
            radius = 5f * pulseScale,
            center = neuron
        )
    }
}

private fun DrawScope.drawDataParticles(
    neurons: List<Offset>,
    branchNeurons: List<Offset>,
    particleColor: Color,
    flowPosition: Float,
    secondaryFlowPosition: Float
) {
    // Main path data particles
    for (i in 0 until neurons.size - 1) {
        // Calculate position along the line based on flow animation
        val particlePos = (flowPosition + i * 0.15f) % 1f
        val x = neurons[i].x + (neurons[i + 1].x - neurons[i].x) * particlePos
        val y = neurons[i].y + (neurons[i + 1].y - neurons[i].y) * particlePos
        
        // Particle size varies slightly
        val particleSize = 3f + (i % 2) * 0.5f
        
        drawCircle(
            color = particleColor.copy(alpha = 0.8f),
            radius = particleSize,
            center = Offset(x, y)
        )
    }
    
    // Branch connection data particles
    val branchConnections = listOf(
        Pair(neurons[1], branchNeurons[0]),  // Top branch 1
        Pair(neurons[2], branchNeurons[1]),  // Bottom branch 1
        Pair(neurons[3], branchNeurons[2]),  // Top branch 2
        Pair(neurons[4], branchNeurons[3])   // Bottom branch 2
    )
    
    branchConnections.forEachIndexed { index, (start, end) ->
        val particlePos = (secondaryFlowPosition + index * 0.25f) % 1f
        val x = start.x + (end.x - start.x) * particlePos
        val y = start.y + (end.y - start.y) * particlePos
        
        drawCircle(
            color = particleColor.copy(alpha = 0.7f),
            radius = 2.5f,
            center = Offset(x, y)
        )
    }
    
    // Cross connection data particles
    val crossConnections = listOf(
        Pair(branchNeurons[0], branchNeurons[2]),  // Top cross
        Pair(branchNeurons[1], branchNeurons[3])   // Bottom cross
    )
    
    crossConnections.forEachIndexed { index, (start, end) ->
        val particlePos = (secondaryFlowPosition + 0.5f + index * 0.25f) % 1f
        val x = start.x + (end.x - start.x) * particlePos
        val y = start.y + (end.y - start.y) * particlePos
        
        drawCircle(
            color = particleColor.copy(alpha = 0.6f),
            radius = 2f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawLoadingDots(
    dotAnimation: Float,
    dotColor: Color,
    centerX: Float,
    centerY: Float
) {
    val dotCount = 5
    val dotRadius = 3f
    val dotSpacing = 10f
    val totalWidth = (dotCount - 1) * dotSpacing
    
    // Calculate the current active dot (0 to 4)
    val activeDot = dotAnimation.toInt() % dotCount
    
    for (i in 0 until dotCount) {
        val x = centerX - totalWidth / 2 + i * dotSpacing
        val dotAlpha = if (i == activeDot) 0.9f else 0.3f
        
        drawCircle(
            color = dotColor.copy(alpha = dotAlpha),
            radius = dotRadius,
            center = Offset(x, centerY)
        )
    }
}
