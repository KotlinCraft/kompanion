package agent

class FakeChatBot : ChatBot(FakeAgent()) {
    override suspend fun handleMessage(message: String): String {
        return """
            Generated Code:
            ```kotlin
            fun example() {
                println("This is fake generated code")
            }
            ```
            
            Explanation:
            This is a fake response for testing purposes.
            
            Next Steps:
            1. Review the generated code
            2. Test the implementation
            3. Consider edge cases
            
            Confidence: 95%
        """.trimIndent()
    }
}

private class FakeAgent : CodeAgent {
    override suspend fun process(request: UserRequest) = throw UnsupportedOperationException()
    override fun addFeedback(feedback: UserFeedback) = throw UnsupportedOperationException()
}
