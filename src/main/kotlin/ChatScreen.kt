package ui.chat

import agent.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.OpenAIModel
import config.AppConfig

@Composable
fun ChatScreen() {
    val darkBackground = Color(0xFF343541)
    val darkSecondary = Color(0xFF40414F)

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var workingDirectory by remember { mutableStateOf(System.getProperty("user.dir")) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val chatBot = remember { createChatBot(workingDirectory) { message -> 
        messages = messages + ChatMessage(message, false)
    }}

    Column(
        modifier = Modifier.fillMaxSize().background(darkBackground)
    ) {
        // Messages area
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        // Working directory selector
        Surface(
            color = darkSecondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Working Directory:",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (isEditingDirectory) {
                    TextField(
                        value = workingDirectory,
                        onValueChange = { workingDirectory = it },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = darkSecondary,
                            textColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { isEditingDirectory = false })
                    )
                } else {
                    Text(
                        workingDirectory,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isEditingDirectory = true }
                    )
                }
            }
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
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = darkSecondary,
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = {
                        Text(
                            if (isProcessing) "Thinking really hard..." else "Ask me about your code...",
                            color = Color.Gray
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

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
                    onClick = {
                        if (isProcessing) {
                            currentJob?.cancel()
                            currentJob = null
                            isProcessing = false
                            messages = messages + ChatMessage("Operation cancelled by user", false)
                        } else if (inputText.isNotBlank()) {
                            val userMessage = inputText
                            messages = messages + ChatMessage(userMessage, true)
                            inputText = ""
                            isProcessing = true

                            currentJob = coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        chatBot.handleMessage(
                                            message = userMessage,
                                            onMessage = { response ->
                                                messages = messages + ChatMessage(response, false)
                                                isProcessing = false
                                                currentJob = null
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    messages = messages + ChatMessage("Error: ${e.message}", false)
                                    isProcessing = false
                                    currentJob = null
                                }
                            }
                        }
                    }
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

private fun createChatBot(workingDirectory: String, onMessage: (String) -> Unit): ChatBot {
    return if (System.getenv("KOMPANION_ENV") == "production") {
        val config = AppConfig.load()
        val model = OpenAIModel(config)
        val contextManager = InMemoryContextManager(workingDirectory)
        val reasoner = DefaultReasoner(model, contextManager)
        val codeGenerator = DefaultCodeGenerator(model, contextManager)
        val agent = CodingAgent(reasoner, codeGenerator)
        ChatBot(agent, onMessage)
    } else {
        FakeChatBot(onMessage)
    }
}

private fun handleSendClick(
    isProcessing: Boolean,
    currentJob: kotlinx.coroutines.Job?,
    inputText: String,
    onProcessingChange: (Boolean) -> Unit,
    onCurrentJobChange: (kotlinx.coroutines.Job?) -> Unit,
    onMessagesChange: (List<ChatMessage>) -> Unit,
    messages: List<ChatMessage>,
    chatBot: ChatBot,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onInputTextChange: (String) -> Unit
) {
    if (isProcessing) {
        currentJob?.cancel()
        onCurrentJobChange(null)
        onProcessingChange(false)
        onMessagesChange(messages + ChatMessage("Operation cancelled by user", false))
    } else if (inputText.isNotBlank()) {
        val userMessage = inputText
        onMessagesChange(messages + ChatMessage(userMessage, true))
        onInputTextChange("")
        onProcessingChange(true)

        val job = coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatBot.handleMessage(
                        message = userMessage,
                        onMessage = { response ->
                            onMessagesChange(messages + ChatMessage(response, false))
                            onProcessingChange(false)
                            onCurrentJobChange(null)
                        }
                    )
                }
            } catch (e: Exception) {
                onMessagesChange(messages + ChatMessage("Error: ${e.message}", false))
                onProcessingChange(false)
                onCurrentJobChange(null)
            }
        }
        onCurrentJobChange(job)
    }
}
