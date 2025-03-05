package ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A futuristic neural network animation component to be used as a loading indicator
 * for AI thinking state. Uses AnimatedVectorDrawable for optimal performance.
 *
 * Note: You need to create the drawable XML files in your resources directory.
 * See the implementation guide in the comments.
 */
@Composable
fun NeuralNetworkLoadingIndicatorAVD() {
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
                .background(color = Color(0xFF0F0F14)),
            contentAlignment = Alignment.Center
        ) {
            // For Android, we can use AnimatedVectorDrawableCompat
            // However, for desktop or other platforms, this won't work
            // so we recommend using the Canvas implementation instead
            
            /* To use this with Android:
             * 1. Create `res/drawable/neural_network_loading.xml` as an AnimatedVectorDrawable
             * 2. Replace this comment with:
             *    Image(
             *        painter = rememberAnimatedVectorPainter(
             *            AnimatedImageVector.animatedVectorResource(id = R.drawable.neural_network_loading)
             *        ),
             *        contentDescription = "Loading",
             *        modifier = Modifier.fillMaxSize()
             *    )
             */
            
            // For desktop/preview, we'll fallback to the Canvas implementation
            NeuralNetworkLoadingIndicatorCanvas()
        }
    }
}
