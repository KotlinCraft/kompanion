import KompanionBuilder.AgentMode.*
import agent.InMemoryContextManager
import agent.interaction.*
import agent.modes.Mode
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import blockchain.etherscan.EtherscanClientManager
import config.AppConfig
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import ui.FilePill
import ui.SettingsDialog
import ui.ToolCounter
import ui.chat.*
import ui.info.InfoManager
import ui.info.InfoTooltip
import java.util.*

private data class SlashCommand(
    val command: String,
    val description: String,
    val run: () -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalTextApi::class)
@Composable
fun ChatScreen() {

    val logger = LoggerFactory.getLogger("ChatScreen")

    // Theme colors
    val darkBackground = Color(0xFF1E1E2E) // Darker background for better contrast
    val darkSecondary = Color(0xFF2D2D3F) // Slightly lighter for components
    val accentColor = Color(0xFF7289DA) // Discord-like accent color
    val successColor = Color(0xFF43B581) // Green for success indicators
    val glistenColor1 = Color(0xFF94A6E6) // Light blue-purple for glow start
    val glistenColor2 = Color(0xFF61DAFB) // Light cyan-blue for glow end

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isWaitingForAnswer by remember { mutableStateOf(false) }

    // Mode state variable: "code", or "blockchain"
    var mode by remember { mutableStateOf("blockchain") }

    var showSuggestions by remember { mutableStateOf(false) }
    var workingDirectory by remember { mutableStateOf(AppConfig.load().latestDirectory) }
    var pendingQuestion by remember { mutableStateOf<AgentQuestion?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }

    var userResponse by remember { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(false) }
    var configState by remember { mutableStateOf(AppConfig.load()) }

    // New state for confirmation dialog
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingConfirmation by remember { mutableStateOf<AgentAskConfirmation?>(null) }

    // Initial check for configuration issues
    LaunchedEffect(Unit) {
        InfoManager.checkConfigurationIssues()
    }

    // Check configuration issues whenever configState changes
    LaunchedEffect(configState) {
        InfoManager.checkConfigurationIssues()
    }

    val interactionHandler = object : InteractionHandler {
        override suspend fun interact(agentMessage: AgentMessage): String {
            return when (agentMessage) {
                is AgentQuestion -> {
                    messages = messages + ChatMessage(agentMessage.id, agentMessage.message, false)
                    isWaitingForAnswer = true
                    pendingQuestion = agentMessage
                    isProcessing = false
                    while (isWaitingForAnswer && userResponse.isBlank()) {
                        delay(100)
                    }
                    val response = userResponse
                    userResponse = ""
                    pendingQuestion = null
                    isProcessing = true
                    response
                }

                is ToolUsageMessage -> {
                    messages =
                        (messages).filter {
                            it.id != agentMessage.id
                        } + ToolMessage(agentMessage.id, agentMessage.message, agentMessage.toolIndicator)
                    ""
                }

                is AgentResponse -> {
                    messages = messages + ChatMessage(agentMessage.id, agentMessage.message, false)
                    ""
                }

                is AgentAskConfirmation -> {
                    messages = messages + ChatMessage(agentMessage.id, agentMessage.message, false)
                    pendingConfirmation = agentMessage
                    showConfirmationDialog = true
                    isProcessing = false

                    // Wait for user response
                    while (showConfirmationDialog) {
                        delay(100)
                    }

                    val result = if (userResponse == "yes") "yes" else "no"
                    userResponse = ""
                    pendingConfirmation = null
                    isProcessing = true
                    result
                }
            }
        }

        override fun removeChat(id: UUID) {
            messages = messages.filter {
                it.id == id
            }
        }
    }

    val inMemoryContextManager = remember {
        InMemoryContextManager()
    }

    val etherscanClientManager = remember {
        EtherscanClientManager()
    }

    fun createCodingKompanion(handler: InteractionHandler, contextManager: InMemoryContextManager): Kompanion {
        logger.info("creating coder")
        return Kompanion.builder()
            .withMode(CODE)
            .withInteractionHandler(handler)
            .withContextManager(contextManager)
            .withAppConfig(configState)
            .build()
    }

    // Function to create a blockchain Kompanion
    fun createBlockchainKompanion(
        handler: InteractionHandler,
        contextManager: InMemoryContextManager,
        etherscanManager: EtherscanClientManager
    ): Kompanion {
        logger.info("creating blockchain kompanion")
        return Kompanion.builder()
            .withMode(BLOCKCHAIN)
            .withInteractionHandler(handler)
            .withContextManager(contextManager)
            .withEtherscanClientManager(etherscanManager)
            .withAppConfig(configState)
            .build()
    }

    // Agent state to hold the three Kompanion agents
    class AgentState {
        var codingKompanion = createCodingKompanion(interactionHandler, inMemoryContextManager)
        var blockchainKompanion =
            createBlockchainKompanion(interactionHandler, inMemoryContextManager, etherscanClientManager)
    }


    // Create agents state and remember it
    val agentState = remember { AgentState() }

    // Function to recreate all agents with the latest config
    fun recreateAgents() {
        logger.info("Recreating agents with new configuration")
        agentState.codingKompanion = createCodingKompanion(interactionHandler, inMemoryContextManager)
        agentState.blockchainKompanion =
            createBlockchainKompanion(interactionHandler, inMemoryContextManager, etherscanClientManager)
    }

    val openFiles by agentState.blockchainKompanion.agent.fetchContextManager().getContext().collectAsState()

    // Get the current active mode based on the mode state
    val activeMode: Mode = remember(mode) {

        when (mode) {
            "code" -> agentState.codingKompanion.agent.mode
            "blockchain" -> agentState.blockchainKompanion.agent.mode
            else -> agentState.blockchainKompanion.agent.mode
        }
    }

    // Local slash commands with callbacks to update the mode.
    val slashCommands = listOf(
        SlashCommand("/clear-context", "Clear the file context") {
            agentState.blockchainKompanion.agent.fetchContextManager().clearContext()
            messages = messages + ChatMessage(UUID.randomUUID(), "File context cleared.", false)
        },
        SlashCommand("/code", "Switch to code mode") { mode = "code" },
        SlashCommand("/ask", "Switch to ask mode") { mode = "ask" },
        SlashCommand("/blockchain", "Switch to blockchain mode") { mode = "blockchain" },
        SlashCommand("/help", "Show available commands") {
            messages = messages + ChatMessage(
                UUID.randomUUID(),
                """
                Available commands:
                /code - Switch to code mode
                /ask - Switch to ask mode
                /blockchain - Switch to blockchain mode
                /help - Show available commands
                /clear - Clear chat history
                /clear-context - Clear the file context
                """.trimIndent(), false
            )
        },
        SlashCommand("/clear", "Clear chat history") {
            agentState.codingKompanion.agent.fetchContextManager().clearMessages()
            messages = emptyList()
        }
    )

    fun sendToBot(userMessage: String) {
        // Handle new request
        isProcessing = true
        currentJob = coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = when (mode) {
                        "code" -> agentState.codingKompanion.agent.perform(userMessage)
                        "blockchain" -> agentState.blockchainKompanion.agent.perform(userMessage)
                        else -> "Invalid mode"
                    }
                    messages = messages + ChatMessage(UUID.randomUUID(), response, false)
                    currentJob = null
                    isProcessing = false
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage(UUID.randomUUID(), "Error: ${e.message}", false)
                isProcessing = false
                currentJob = null
            }
        }
    }

    fun performSlashCommand(first: SlashCommand) {
        first.run()
    }

    fun cancelProcessing() {
        currentJob?.cancel()
        currentJob = null
        isProcessing = false
        messages = messages + ChatMessage(UUID.randomUUID(), "Operation cancelled by user", false)
    }

    // Local function to send the current message (and cancel if already processing)
    fun sendCurrentMessage() {
        showSuggestions = false
        if (isProcessing && !isWaitingForAnswer) {
            cancelProcessing()
        } else if (inputText.isNotBlank()) {
            val userMessage = inputText
            inputText = ""

            if (slashCommands.any { it.command == userMessage.trim() }) {
                performSlashCommand(slashCommands.first { it.command == userMessage.trim() })
                return
            }

            messages = messages + ChatMessage(UUID.randomUUID(), userMessage, true)
            if (isWaitingForAnswer && pendingQuestion != null) {
                // Handle answer to pending question
                isWaitingForAnswer = false
                pendingQuestion = null
                userResponse = userMessage
            } else {
                sendToBot(userMessage)
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmationDialog && pendingConfirmation != null) {
        Dialog(onDismissRequest = {
            showConfirmationDialog = false
            userResponse = "no" // Default to no if dismissed
        }) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                backgroundColor = darkSecondary,
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Confirmation",
                        tint = accentColor,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = pendingConfirmation?.message ?: "Confirm action?",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // No button
                        Button(
                            onClick = {
                                userResponse = "no"
                                showConfirmationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Gray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("No")
                        }

                        // Yes button
                        Button(
                            onClick = {
                                userResponse = "yes"
                                showConfirmationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = successColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text("Yes")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        TopBar(
            darkBackground = darkBackground,
            mode = mode,
            onSettingsClick = { showSettings = true },
            onModeChange = { newMode -> mode = newMode },
            isProcessing = isProcessing // Pass the isProcessing state to the TopBar
        )

        if (showSettings) {
            SettingsDialog(initialConfig = configState, onClose = { newConfig ->
                configState = newConfig
                AppConfig.save(newConfig)
                // Recreate agents with the new configuration
                recreateAgents()
                InfoManager.checkConfigurationIssues()
                showSettings = false
            })
        }

        // Messages area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                message.constructChatBubble()
            }
        }

        // Automatically scroll to the bottom on new messages
        LaunchedEffect(key1 = messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        // Listen for changes in the working directory and recreate agents
        LaunchedEffect(key1 = workingDirectory) {
            recreateAgents()
            isProcessing = false
            isWaitingForAnswer = false
        }

        // Bottom area: Working Directory + Open Files
        Surface(
            color = darkSecondary.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Working Directory Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Workdir:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    WorkingDirectorySelector(
                        workingDirectory = workingDirectory,
                        onWorkingDirectoryChange = { newDir ->
                            workingDirectory = newDir
                            AppConfig.save(
                                AppConfig.load().copy(latestDirectory = newDir)
                            )
                        },
                        darkSecondary = darkSecondary
                    )
                }

                // Open Files display
                if (openFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Open Files:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        openFiles.forEach { file ->
                            FilePill(fileName = file.name)
                        }
                    }
                }
            }
        }

        // Input area
        Surface(
            color = darkSecondary,
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Command suggestions
                if (showSuggestions) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = darkSecondary.copy(alpha = 0.9f),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            slashCommands
                                .filter { it.command.startsWith(inputText) }
                                .forEach { command ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { inputText = command.command + " " }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = command.command,
                                            color = accentColor,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Text(
                                            text = command.description,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                        }
                    }
                }

                // Create animation for glisten effect
                val infiniteTransition = rememberInfiniteTransition()

                // Animate the color gradient positions for border
                val gradientPosition = infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                // Animate shadow elevation for a pulsing effect
                val shadowElevation = infiniteTransition.animateFloat(
                    initialValue = 2f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                // Text shimmer animation with smooth bi-directional movement
                val shimmerOffsetX = infiniteTransition.animateFloat(
                    initialValue = -300f,  // Start from left outside of view
                    targetValue = 300f,   // End at right outside of view
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Reverse  // Reverse direction instead of restarting
                    )
                )

                // Input textfield and send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Determine if we should show the glisten effect
                    val showGlisten = !isProcessing && inputText.isEmpty()

                    // Get placeholder text based on current state and mode
                    val placeholderText = when {
                        isProcessing && !isWaitingForAnswer -> "Thinking..."
                        isWaitingForAnswer -> "Please answer the question..."
                        else -> when (mode) {
                            "code" -> "Ask me to code something..."
                            "blockchain" -> "Ask me about blockchain data..."
                            else -> "Type your message..."
                        }
                    }

                    // Box wrapper to apply the glisten effect
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        // Input field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                if (!isProcessing) {
                                    inputText = it
                                    showSuggestions = it.startsWith("/")
                                }
                            },
                            enabled = !isProcessing || isWaitingForAnswer,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color.White,
                                cursorColor = accentColor,
                                focusedBorderColor = if (showGlisten) Color.Transparent else accentColor,
                                unfocusedBorderColor = if (showGlisten) Color.Transparent else Color.Gray.copy(alpha = 0.5f),
                                backgroundColor = darkSecondary
                            ),
                            placeholder = {
                                if (showGlisten) {
                                    // Animated gradient text for placeholder with smooth bi-directional flow
                                    Text(
                                        text = placeholderText,
                                        style = TextStyle(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Gray.copy(alpha = 0.3f),
                                                    Color.Gray.copy(alpha = 0.5f),
                                                    glistenColor1.copy(alpha = 0.7f),
                                                    glistenColor2,
                                                    glistenColor1.copy(alpha = 0.7f),
                                                    Color.Gray.copy(alpha = 0.5f),
                                                    Color.Gray.copy(alpha = 0.3f)
                                                ),
                                                // Center the animation on the text area with the shimmerOffsetX 
                                                // determining the movement direction
                                                start = Offset(shimmerOffsetX.value - 150f, 0f),
                                                end = Offset(shimmerOffsetX.value + 150f, 0f)
                                            )
                                        ),
                                        fontSize = 16.sp
                                    )
                                } else {
                                    // Regular placeholder text
                                    Text(
                                        text = placeholderText,
                                        color = Color.Gray,
                                        fontSize = 16.sp
                                    )
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (showGlisten) {
                                        Modifier
                                            .shadow(
                                                elevation = shadowElevation.value.dp,
                                                shape = RoundedCornerShape(24.dp),
                                                ambientColor = glistenColor1,
                                                spotColor = glistenColor2
                                            )
                                            .border(
                                                width = 2.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(glistenColor1, glistenColor2, glistenColor1),
                                                    start = Offset(gradientPosition.value * 200, 0f),
                                                    end = Offset(gradientPosition.value * 200 + 100, 100f)
                                                ),
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && event.isMetaPressed && event.type == KeyEventType.KeyUp) {
                                        sendCurrentMessage()
                                        true
                                    } else if (event.key == Key.R && event.isMetaPressed && event.type == KeyEventType.KeyUp) {
                                        performSlashCommand(slashCommands.first { it.command == "/clear" })
                                        true
                                    } else if (event.key == Key.Tab && showSuggestions) {
                                        val firstCommand = slashCommands
                                            .sortedBy { it.command }
                                            .firstOrNull { it.command.startsWith(inputText.trim()) }
                                        if (firstCommand != null) {
                                            inputText = firstCommand.command
                                        }
                                        true
                                    } else false
                                }
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isProcessing) successColor else accentColor)
                            .clickable {
                                if (!isProcessing) {
                                    sendCurrentMessage()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = isProcessing) { processing ->
                            if (processing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Keyboard shortcuts hint with info icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darkSecondary)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Keyboard shortcuts (left side)
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⌘+Enter to send",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "⌘+R to clear",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            if (showSuggestions) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "Tab to complete command",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Tool counter and info icon (right side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolCounter(
                                accentColor = accentColor,
                                backgroundColor = darkBackground,
                                activeMode = activeMode
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            InfoTooltip(accentColor = accentColor, backgroundColor = darkBackground)
                        }
                    }
                }
            }
        }
    }
}