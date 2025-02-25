import KompanionBuilder.AgentMode.*
import agent.InMemoryContextManager
import agent.domain.CodeFile
import agent.interaction.AgentMessage
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockchain.etherscan.EtherscanClientManager
import config.AppConfig
import kotlinx.coroutines.*
import ui.FilePill
import ui.SettingsDialog
import ui.chat.ChatMessage
import ui.chat.MessageBubble
import ui.chat.WorkingDirectorySelector
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.random.Random

private data class SlashCommand(
    val command: String,
    val description: String,
    val run: () -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen() {
    // Theme colors
    val darkBackground = Color(0xFF1E1E2E) // Darker background for better contrast
    val darkSecondary = Color(0xFF2D2D3F) // Slightly lighter for components
    val accentColor = Color(0xFF7289DA) // Discord-like accent color
    val successColor = Color(0xFF43B581) // Green for success indicators
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isWaitingForAnswer by remember { mutableStateOf(false) }

    // Mode state variable: "code", "ask", or "blockchain"
    var mode by remember { mutableStateOf("code") }

    var showSuggestions by remember { mutableStateOf(false) }
    var workingDirectory by remember { mutableStateOf(AppConfig.load().latestDirectory) }
    var pendingQuestion by remember { mutableStateOf<AgentQuestion?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }

    var userResponse by remember { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(false) }
    var configState by remember { mutableStateOf(AppConfig.load()) }

    val interactionHandler = object : InteractionHandler {
        override suspend fun interact(agentMessage: AgentMessage): String {
            return when (agentMessage) {
                is AgentQuestion -> {
                    messages = messages + ChatMessage(agentMessage.message, false)
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

                is AgentResponse -> {
                    messages = messages + ChatMessage(agentMessage.message, false)
                    ""
                }
            }
        }
    }

    val inMemoryContextManager = remember {
        InMemoryContextManager()
    }
    
    val etherscanClientManager = remember {
        EtherscanClientManager()
    }

    val codingKompanion = remember {
        Kompanion.builder()
            .withMode(CODE)
            .withInteractionHandler(interactionHandler)
            .withContextManager(inMemoryContextManager)
            .build()
    }

    val analystKompanion = remember {
        Kompanion.builder()
            .withMode(ASK)
            .withInteractionHandler(interactionHandler)
            .withContextManager(inMemoryContextManager)
            .build()
    }
    
    val blockchainKompanion = remember {
        Kompanion.builder()
            .withMode(BLOCKCHAIN)
            .withInteractionHandler(interactionHandler)
            .withContextManager(inMemoryContextManager)
            .withEtherscanClientManager(etherscanClientManager)
            .build()
    }

    val openFiles by analystKompanion.agent.fetchContextManager().getContext().collectAsState()

    // Local slash commands with callbacks to update the mode.
    val slashCommands = listOf(
        SlashCommand("/clear-context", "Clear the file context") {
            analystKompanion.agent.fetchContextManager().clearContext()
            messages = messages + ChatMessage("File context cleared.", false)
        },
        SlashCommand("/code", "Switch to code mode") { mode = "code" },
        SlashCommand("/ask", "Switch to ask mode") { mode = "ask" },
        SlashCommand("/blockchain", "Switch to blockchain mode") { mode = "blockchain" },
        SlashCommand("/add", "Switch to ask mode") {
            analystKompanion.agent.fetchContextManager().updateFiles(
                listOf(
                    CodeFile(Path.of("/opt/test${Random(1000).nextInt()}.html"), "", "html")
                )
            )
        },
        SlashCommand("/help", "Show available commands") {
            messages = messages + ChatMessage(
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
                        "code" -> codingKompanion.agent.perform(userMessage)
                        "ask" -> analystKompanion.agent.perform(userMessage)
                        "blockchain" -> blockchainKompanion.agent.perform(userMessage)
                        else -> "Invalid mode"
                    }
                    messages = messages + ChatMessage(response, false)
                    currentJob = null
                    isProcessing = false
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage("Error: ${e.message}", false)
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
        messages = messages + ChatMessage("Operation cancelled by user", false)
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

            messages = messages + ChatMessage(userMessage, true)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        TopBar(
            darkBackground = darkBackground,
            mode = mode,
            onSettingsClick = { showSettings = true }
        )

        if (showSettings) {
            SettingsDialog(initialConfig = configState, onClose = {
                configState = it
                AppConfig.save(it)
                showSettings = false
            })
        }

        // Mode Selector - moved from TopBar to here for better visibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(4.dp),
                backgroundColor = darkSecondary,
                shape = RoundedCornerShape(24.dp),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Code mode button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { mode = "code" }
                            .background(if (mode == "code") Color(0xFF2E6F40) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = "Code mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Code",
                                color = Color.White,
                                fontWeight = if (mode == "code") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Ask mode button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { mode = "ask" }
                            .background(if (mode == "ask") Color(0xFF4A6FD0) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.QuestionAnswer,
                                contentDescription = "Ask mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Ask",
                                color = Color.White,
                                fontWeight = if (mode == "ask") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Blockchain mode button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { mode = "blockchain" }
                            .background(if (mode == "blockchain") Color(0xFF936FBC) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = "Blockchain mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Blockchain",
                                color = Color.White,
                                fontWeight = if (mode == "blockchain") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
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
                MessageBubble(message)
            }
        }

        // Automatically scroll to the bottom on new messages
        LaunchedEffect(key1 = messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        LaunchedEffect(key1 = workingDirectory) {
            analystKompanion.agent.onload()
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
                        "Working Directory:",
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
                            FilePill(fileName = file.path.name)
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
                
                // Input textfield and send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            backgroundColor = darkSecondary
                        ),
                        placeholder = {
                            Text(
                                when {
                                    isProcessing && !isWaitingForAnswer -> "Thinking..."
                                    isWaitingForAnswer -> "Please answer the question..."
                                    else -> when (mode) {
                                        "code" -> "Ask me to code something..."
                                        "ask" -> "Ask me about your code..."
                                        "blockchain" -> "Ask me about blockchain data..."
                                        else -> "Type your message..."
                                    }
                                },
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && event.isMetaPressed && event.type == KeyEventType.KeyUp) {
                                    sendCurrentMessage()
                                    true
                                } else if (event.key == Key.R && event.isMetaPressed && event.type == KeyEventType.KeyUp) {
                                    performSlashCommand(slashCommands.first { it.command == "/clear" })
                                    true
                                } else if (event.key == Key.Tab && showSuggestions) {
                                    val firstCommand = slashCommands
                                        .sortedBy { it.command }.firstOrNull { it.command.startsWith(inputText.trim()) }
                                    if (firstCommand != null) {
                                        inputText = firstCommand.command
                                    }
                                    true
                                } else false
                            }
                    )

                    Spacer(Modifier.width(12.dp))

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isProcessing) Color.Red.copy(alpha = 0.7f) else accentColor)
                            .clickable { sendCurrentMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing && !isWaitingForAnswer) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (isProcessing) Icons.Default.Close else Icons.Default.Send,
                                contentDescription = if (isProcessing) "Cancel" else "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Keyboard shortcuts hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darkSecondary)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
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
                }
            }
        }
    }
}
