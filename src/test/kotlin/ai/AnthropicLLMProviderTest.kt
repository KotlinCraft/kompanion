package ai

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled

class AnthropicLLMProviderTest {
    
    @Test
    fun testGetSupportedModels() {
        val provider = AnthropicLLMProvider()
        val models = provider.getSupportedModels()
        
        assertFalse(models.isEmpty())
        assertTrue(models.contains("claude-3-opus-20240229"))
        assertTrue(models.contains("claude-3-sonnet-20240229"))
        assertTrue(models.contains("claude-3-haiku-20240307"))
    }
    
    @Test
    @Disabled("Requires API key to run")
    fun testModelCreation() {
        val provider = AnthropicLLMProvider()
        val model = provider.createModel()
        
        assertNotNull(model)
    }
}