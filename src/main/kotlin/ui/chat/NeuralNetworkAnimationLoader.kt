package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.InputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream

/**
 * Utility to determine which version of the loading indicator to use.
 * For Android, we'd use the AVD implementation. For desktop, we'd use the Canvas one.
 */
object NeuralNetworkAnimationLoader {
    /**
     * Checks if we're in an environment where the AVD resources are available.
     * For desktop/non-Android environments, this will return false.
     */
    fun isAVDAvailable(): Boolean {
        return try {
            // Try to load the resource file
            val resourcePath = Path("src/main/resources/drawable/neural_network_loading.xml")
            val inputStream: InputStream = resourcePath.inputStream()
            inputStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Creates the appropriate loading indicator for the platform.
     * Uses the AVD version if available, otherwise falls back to the Canvas implementation.
     */
    @Composable
    fun LoadingIndicator() {
        val useAvd = remember { isAVDAvailable() }
        
        if (useAvd) {
            NeuralNetworkLoadingIndicatorAVD()
        } else {
            NeuralNetworkLoadingIndicatorCanvas()
        }
    }
}
