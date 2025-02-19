import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import agent.ChatBot
import agent.CodeGenerationAgent
import agent.DefaultCodeGenerator
import agent.DefaultReasoner
import agent.InMemoryContextManager
import ai.OpenAIModel
import config.AppConfig
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen() {
    val darkBackground = Color(0xFF343541)
    val darkSecondary = Color(0xFF40414F)
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val chatBot = remember {
        if (System.getenv("KOMPANION_ENV") == "production") {
            val config = AppConfig.load()
            val model = OpenAIModel(config)
            val contextManager = InMemoryContextManager()
            val reasoner = DefaultReasoner(model, contextManager)
            val codeGenerator = DefaultCodeGenerator(model, contextManager)
            val agent = CodeGenerationAgent(reasoner, codeGenerator)
            ChatBot(agent)
        } else {
            FakeChatBot()
        }
    }
    
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
                    placeholder = { Text("Ask me about your code...", color = Color.Gray) },
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userMessage = inputText
                            messages = messages + ChatMessage(userMessage, true)
                            inputText = ""
                            
                            coroutineScope.launch {
                                try {
                                    val response = chatBot.handleMessage(userMessage)
                                    messages = messages + ChatMessage(response, false)
                                } catch (e: Exception) {
                                    messages = messages + ChatMessage("Error: ${e.message}", false)
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Surface(
        color = if (message.isUser) Color(0xFF343541) else Color(0xFF444654),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (message.isUser) "You: " else "Kompanion: ",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message.content,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}
