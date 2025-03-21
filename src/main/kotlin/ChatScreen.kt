import KompanionBuilder.AgentMode.FULL_AUTO
import KompanionBuilder.AgentMode.CODE
import agent.InMemoryContextManager
import agent.interaction.*
import agent.modes.Mode
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.yourdomain.kompanion.ui.components.ProviderSelector
import config.AppConfig
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import ui.FilePill
import ui.SettingsDialog
import ui.ToolCounter
import ui.chat.ChatMessage
import ui.chat.ToolMessage
import ui.chat.WorkingDirectorySelector
import ui.info.InfoTooltip
import java.util.*

private data class SlashCommand(
    val command: String,
    val description: String,
    val run: () -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalTextApi::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen() {
    val logger = LoggerFactory.getLogger("ChatScreen")

    // Theme colors
    val darkBackground = Color(0xFF1E1E2E)
    val darkSecondary = Color(0xFF2D2D3F)
    val accentColor = Color(0xFF7289DA)
    val successColor = Color(0xFF43B581)
    val warningColor = Color(0xFFFAA61A)
    val glistenColor1 = Color(0xFF94A6E6)
    val glistenColor2 = Color(0xFF61DAFB)

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isWaitingForAnswer by remember { mutableStateOf(false) }

    // Mode state variable: "code", or "full auto"
    var mode by remember { mutableStateOf("full-auto") }

    // Current AI provider state
    var currentProvider by remember { mutableStateOf(AppConfig.load().currentProvider) }

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
                        messages.filter { it.id != agentMessage.id } + ToolMessage(
                            agentMessage.id,
                            agentMessage.message,
                            agentMessage.toolIndicator
                        )
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
            messages = messages.filter { it.id == id }
        }
    }

    val inMemoryContextManager by remember { mutableStateOf(InMemoryContextManager()) }
    var chatMemory by remember { mutableStateOf(InMemoryChatMemory()) }

    fun clearChatMemory() {
        chatMemory.clear("default")
    }

    fun createCodingKompanion(handler: InteractionHandler, contextManager: InMemoryContextManager): Kompanion {
        logger.info("creating coder")
        return Kompanion.builder()
            .withMode(CODE)
            .withInteractionHandler(handler)
            .withChatMemory(chatMemory)
            .withContextManager(contextManager)
            .withAppConfig(configState)
            .withProvider(currentProvider)
            .build()
    }

    fun createFullAutoKompanion(handler: InteractionHandler, contextManager: InMemoryContextManager): Kompanion {
        logger.info("creating full-auto kompanion")
        return Kompanion.builder()
            .withMode(FULL_AUTO)
            .withChatMemory(chatMemory)
            .withInteractionHandler(handler)
            .withContextManager(contextManager)
            .withAppConfig(configState)
            .withProvider(currentProvider)
            .build()
    }

    class AgentState {
        var codingKompanion = createCodingKompanion(interactionHandler, inMemoryContextManager)
        var fullAutoCompanion = createFullAutoKompanion(interactionHandler, inMemoryContextManager)
    }

    val agentState = remember { AgentState() }

    fun recreateAgents() {
        logger.info("Recreating agents with new configuration")
        agentState.codingKompanion = createCodingKompanion(interactionHandler, inMemoryContextManager)
        agentState.fullAutoCompanion = createFullAutoKompanion(interactionHandler, inMemoryContextManager)
    }

    val openFiles by agentState.fullAutoCompanion.agent.fetchContextManager().getContext().collectAsState()

    val activeMode: Mode = remember(mode) {
        when (mode) {
            "code" -> agentState.codingKompanion.agent.mode
            "full-auto" -> agentState.fullAutoCompanion.agent.mode
            else -> agentState.fullAutoCompanion.agent.mode
        }
    }

    val activeContextManager = remember(mode) {
        when (mode) {
            "code" -> agentState.codingKompanion.agent.fetchContextManager()
            "full-auto" -> agentState.fullAutoCompanion.agent.fetchContextManager()
            else -> agentState.fullAutoCompanion.agent.fetchContextManager()
        }
    }

    val slashCommands = listOf(
        SlashCommand("/clear-context", "Clear the file context") {
            agentState.fullAutoCompanion.agent.fetchContextManager().clearContext()
            messages = messages + ChatMessage(UUID.randomUUID(), "File context cleared.", false)
        },
        SlashCommand("/code", "Switch to code mode") { mode = "code" },
        SlashCommand("/ask", "Switch to ask mode") { mode = "ask" },
        SlashCommand("/blockchain", "Switch to blockchain mode") { mode = "blockchain" },
        SlashCommand("/help", "Show available commands") {
            messages = messages + ChatMessage(
                UUID.randomUUID(), """
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
            messages = emptyList()
            clearChatMemory()
        }
    )

    fun sendToBot(userMessage: String) {
        isProcessing = true

        currentJob = coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = when (mode) {
                        "code" -> agentState.codingKompanion.agent.perform(userMessage)
                        "full-auto" -> agentState.fullAutoCompanion.agent.perform(userMessage)
                        else -> "Invalid mode"
                    }
                    messages = messages + ChatMessage(UUID.randomUUID(), response, false)

                    currentJob = null
                    isProcessing = false
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage(UUID.randomUUID(), e.message ?: "unknown error occurred :(", false)
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
                isWaitingForAnswer = false
                pendingQuestion = null
                userResponse = userMessage
            } else {
                sendToBot(userMessage)
            }
        }
    }

    if (showConfirmationDialog && pendingConfirmation != null) {
        Dialog(onDismissRequest = {
            showConfirmationDialog = false
            userResponse = "no"
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
                        Button(
                            onClick = {
                                userResponse = "no"
                                showConfirmationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text("No")
                        }
                        Button(
                            onClick = {
                                userResponse = "yes"
                                showConfirmationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = successColor),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text("Yes")
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main chat content 
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
        ) {
            val loadSettings = { AppConfig.load() }

            TopBar(
                darkBackground = darkBackground,
                mode = mode,
                onSettingsClick = { showSettings = true },
                onModeChange = { newMode -> mode = newMode },
                isProcessing = isProcessing
            )

            if (showSettings) {
                SettingsDialog(initialConfig = configState, onClose = { newConfig ->
                    configState = newConfig
                    recreateAgents()
                    showSettings = false
                })
            }

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

            LaunchedEffect(key1 = messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            LaunchedEffect(key1 = workingDirectory) {
                isProcessing = false
                isWaitingForAnswer = false
            }

            LaunchedEffect(key1 = currentProvider, key2 = workingDirectory) {
                recreateAgents()
            }

            Surface(
                color = darkSecondary.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                                configState = AppConfig.save(
                                    AppConfig.load().copy(latestDirectory = newDir)
                                )
                            },
                            darkSecondary = darkSecondary
                        )
                    }

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
                                FilePill(
                                    fileName = file.displayName,
                                    fileId = file.id,
                                    onRemove = { fileId ->
                                        activeContextManager.removeFile(fileId)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                color = darkSecondary,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (showSuggestions) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = darkSecondary.copy(alpha = 0.9f),
                            elevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                slashCommands.filter { it.command.startsWith(inputText) }
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

                    val infiniteTransition = rememberInfiniteTransition()
                    val gradientPosition = infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    val shadowElevation = infiniteTransition.animateFloat(
                        initialValue = 2f,
                        targetValue = 8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    val shimmerOffsetX = infiniteTransition.animateFloat(
                        initialValue = -300f,
                        targetValue = 300f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = EaseInOutQuad),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProviderSelector(
                            currentProvider = currentProvider,
                            onProviderSelected = { provider ->
                                configState = AppConfig.save(AppConfig.load().copy(currentProvider = provider))
                                currentProvider = provider
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        val showGlisten = !isProcessing && inputText.isEmpty()
                        val placeholderText = when {
                            isProcessing && !isWaitingForAnswer -> "Thinking..."
                            isWaitingForAnswer -> "Please answer the question..."
                            else -> when (mode) {
                                "code" -> "Ask me to code something..."
                                "blockchain" -> "Ask me about blockchain data..."
                                else -> "Type your message..."
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
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
                                                    start = Offset(shimmerOffsetX.value - 150f, 0f),
                                                    end = Offset(shimmerOffsetX.value + 150f, 0f)
                                                )
                                            ),
                                            fontSize = 16.sp
                                        )
                                    } else {
                                        Text(text = placeholderText, color = Color.Gray, fontSize = 16.sp)
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
                                            val firstCommand = slashCommands.sortedBy { it.command }
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isProcessing) successColor.copy(alpha = 0.6f) else Color(0xFF3A3A3A))
                                .clickable {
                                    if (!isProcessing) {
                                        sendCurrentMessage()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Outlined.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(darkSecondary)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⌘+Enter to send", color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("⌘+R to clear", color = Color.Gray, fontSize = 12.sp)
                                if (showSuggestions) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Tab to complete command", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
}