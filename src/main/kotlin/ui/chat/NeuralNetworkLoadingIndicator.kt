package ui.chat

import androidx.compose.runtime.Composable

/**
 * A futuristic neural network animation component to be used as a loading indicator
 * for AI thinking state. Uses either Canvas-based implementation or AVD based on platform.
 */
@Composable
fun NeuralNetworkLoadingIndicator() {
    // Use the loader to determine the best implementation for the current platform
    NeuralNetworkAnimationLoader.LoadingIndicator()
}
