package agent

import agent.domain.AgentResponse
import agent.domain.CodeFile
import agent.domain.UserFeedback
import agent.domain.UserRequest

class CodeGenerationAgent(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
    private val contextManager: ContextManager
) : CodeAgent {
    override suspend fun process(request: UserRequest): AgentResponse {
        // 1. Analyze request and current context
        val understanding = reasoner.analyzeRequest(request)
        
        // 2. Generate initial plan
        val plan = reasoner.createPlan(understanding)
        
        // 3. Execute plan iteratively
        var currentCode = ""
        var iterations = 0
        val maxIterations = 3
        
        while (iterations < maxIterations) {
            // Generate or improve code
            val generationResult = codeGenerator.generate(plan, currentCode)
            
            // Evaluate result
            val evaluation = reasoner.evaluateCode(generationResult, understanding)
            
            if (evaluation.meetsRequirements) {
                return AgentResponse(
                    generatedCode = generationResult.code,
                    explanation = generationResult.explanation,
                    nextSteps = evaluation.suggestedImprovements,
                    confidence = evaluation.confidence
                )
            }
            
            currentCode = generationResult.code
            iterations++
        }
        
        return AgentResponse(
            generatedCode = currentCode,
            explanation = "Reached maximum iterations. Current best attempt provided.",
            nextSteps = listOf("Consider providing more specific requirements",
                             "Review current output and provide feedback"),
            confidence = 0.7f
        )
    }
    
    override fun updateContext(codeFiles: List<CodeFile>) {
        contextManager.updateFiles(codeFiles)
    }
    
    override fun addFeedback(feedback: UserFeedback) {
        reasoner.learn(feedback)
    }
}