import agent.*
import agent.domain.FileSystemCodeApplier
import agent.interaction.AgentMessage
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import ai.OpenAIModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.SettingsDialog
import ui.chat.ChatMessage
import ui.chat.MessageBubble
import ui.chat.WorkingDirectorySelector

private data class SlashCommand(
    val command: String,
    val description: String,
    val run: () -> Unit = {}
)

@Composable
fun ChatScreen() {
    val darkBackground = Color(0xFF343541)
    val darkSecondary = Color(0xFF40414F)

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isWaitingForAnswer by remember { mutableStateOf(false) }

    // Mode state variable: "code" or "ask"
    var mode by remember { mutableStateOf("code") }

    var showSuggestions by remember { mutableStateOf(false) }
    var workingDirectory by remember { mutableStateOf(AppConfig.load().latestDirectory) }
    var pendingQuestion by remember { mutableStateOf<AgentQuestion?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var userResponse by remember { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(false) }
    var configState by remember { mutableStateOf(AppConfig.load()) }

    val onAgentMessage: suspend (AgentMessage) -> String = { message ->
        when (message) {
            is AgentQuestion -> {
                messages = messages + ChatMessage(message.message, false)
                isWaitingForAnswer = true
                pendingQuestion = message
                isProcessing = false
                while (isWaitingForAnswer && userResponse.isBlank()) {
                    delay(100)
                }
                val response = userResponse
                userResponse = ""
                response
            }

            is AgentResponse -> {
                messages = messages + ChatMessage(message.message, false)
                ""
            }
        }
    }

    val chatBot = remember {
        if (System.getenv("KOMPANION_ENV") == "production") {
            val config = AppConfig.load()
            val model = OpenAIModel(config)
            val contextManager = InMemoryContextManager(workingDirectory)
            val reasoner = DefaultReasoner(model, contextManager)
            val codeGenerator = DefaultCodeGenerator(model, contextManager)
            val codeApplier = FileSystemCodeApplier(contextManager)
            val agent = CodingAgent(reasoner, codeGenerator, codeApplier)
            ChatBot(agent, onAgentMessage)
        } else {
            FakeChatBot(onAgentMessage)
        }
    }

    // Local slash commands with callbacks to update the mode.
    val slashCommands = listOf(
        SlashCommand("/code", "Switch to code mode") { mode = "code" },
        SlashCommand("/ask", "Switch to ask mode") { mode = "ask" },
        SlashCommand("/help", "Show available commands") {
            messages = messages + ChatMessage(
                """
                Available commands:
                /code - Switch to code mode
                /ask - Switch to ask mode
                /help - Show available commands
                /clear - Clear chat history
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
                    val response = chatBot.handleMessage(message = userMessage)
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

        // Messages area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
        
        // Automatically scroll to the bottom on new messages
        LaunchedEffect(key1 = messages.size) {
            listState.animateScrollToItem(messages.size)
        }

        // Working Directory Selector (full width)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Input area
        Surface(
            color = darkSecondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.isMetaPressed && event.type == KeyEventType.KeyUp) {
                                sendCurrentMessage()
                                true
                            } else false
                        }
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = {
                            if (!isProcessing) {
                                inputText = it
                                // Only show suggestions if we're at the start of a command
                                showSuggestions = it.startsWith("/")
                            }
                        },
                        enabled = !isProcessing || isWaitingForAnswer,
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = darkSecondary,
                            textColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (isProcessing && !isWaitingForAnswer) "Thinking really hard..."
                                else if (isWaitingForAnswer) "Answer the question..."
                                else "Ask me about your code...",
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (showSuggestions) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = ((inputText.length) * 20).dp),
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            color = darkSecondary
                        ) {
                            Column(
                                modifier = Modifier.width(300.dp)
                            ) {
                                slashCommands
                                    .filter { it.command.startsWith(inputText) }
                                    .take(1)
                                    .forEach { command ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    inputText = command.command + " "
                                                },
                                            color = Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = command.command,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.width(80.dp)
                                                )
                                                Text(
                                                    text = command.description,
                                                    color = Color.Gray,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(
                    onClick = { sendCurrentMessage() }
                ) {
                    Icon(
                        if (isProcessing) Icons.Default.Close else Icons.Default.Send,
                        contentDescription = if (isProcessing) "Cancel" else "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
