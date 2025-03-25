package agent.coding

import agent.ContextManager
import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan
import agent.domain.action.ActionHandler
import agent.domain.action.ActionParser
import agent.domain.action.CompleteAction
import agent.domain.action.LLMAction
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import ai.LLMProvider
import org.slf4j.LoggerFactory

/**
 * Refactored code generator with extensible action system
 */
class ExtensibleFlowCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : CodeGenerator, Interactor {

    val actionHandler = ActionHandler(contextManager)

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val actionParser = ActionParser()

    override suspend fun execute(
        request: String,
        plan: GenerationPlan?,
    ): CodingResult {
        // Starting point - initial prompt to the LLM
        var result = promptForNextAction(request, plan, null)
        var action = result.first

        // Continue interaction until we get a Complete action
        while (action !is CompleteAction) {
            // Process the action
            val success = action.process()

            // Prepare feedback based on the action result
            val feedback = if (success) {
                "${action.actionType} executed successfully: ${action.summary()}"
            } else {
                "Failed to execute ${action.actionType}. Please try again with corrected parameters."
            }

            // Prompt for the next action with feedback
            result = promptForNextAction(request, plan, feedback)
            action = result.first
        }

        // Return the final result with the completion summary
        return CodingResult(
            explanation = action.summary(),
            success = true
        )
    }

    private suspend fun promptForNextAction(
        request: String,
        plan: GenerationPlan?,
        feedback: String?
    ): Pair<LLMAction, String> {
        val contextPrompt = contextManager.currentContextPrompt(true)
        val planSteps = plan?.steps?.joinToString("\n") { step ->
            "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
        } ?: ""

        val planStepsText = if (planSteps.isNotBlank()) {
            "Plan Steps:\n$planSteps"
        } else {
            ""
        }

        val expectedOutcome = plan?.expectedOutcome ?: ""
        val expectedOutcomeText = if (expectedOutcome.isNotBlank()) {
            "Expected Outcome:\n$expectedOutcome"
        } else {
            ""
        }

        val validationCriteria = plan?.validationCriteria?.joinToString("\n") { "- $it" } ?: ""
        val validationCriteriaText = if (validationCriteria.isNotBlank()) {
            "Validation Criteria:\n$validationCriteria"
        } else {
            ""
        }

        val feedbackSection = if (!feedback.isNullOrBlank()) "\n\n## Feedback from previous action:\n$feedback" else ""

        logger.info("feedback or request: " + (feedback ?: request))

        val prompt = """
            $contextPrompt

            You're an amazing developer, with many years of experience and a deep understanding of clean code and architecture.
            Based on the following generation plan you will make the necessary code changes.
            Use files in your current context to understand your changes.

            If the user doesn't ask for it specifically, don't add tests.

            ## Project Context:
            Based on the files in your current context, you understand the existing code structure and patterns.
            Look for similar implementations in the current codebase to maintain consistency.

            ## Coding Task:
            Based on the following generation plan, implement the necessary code changes.
            First explore the codebase to understand the current structure before making changes.

            $planStepsText

            $expectedOutcomeText

            $validationCriteriaText

            ## Implementation Approach:
            1. You will be implementing changes one action at a time
            2. For each action, you must return ONE of the following responses:
               - EDIT_FILE: To modify an existing file
               - CREATE_FILE: To create a new file
               - COMPLETE: When all changes are done
            3. You only change or create what planned and asked. No freewheeling.
            4. Definitely do not overengineer things. Keep it simple and clean in as few steps as possible.

            ## Response Format:
            You must respond with EXACTLY ONE of these formats:

            1. To edit a file:
            ```
            ACTION: EDIT_FILE
            FILE_PATH: /absolute/path/to/file
            EXPLANATION: Brief explanation of changes
            CONTENT:
            // Complete new content of the file
            ```
            
            2. To create a file:
            ```
            ACTION: CREATE_FILE
            FILE_PATH: /absolute/path/to/file
            EXPLANATION: Brief explanation of the file purpose
            CONTENT:
            // Complete content of the new file
            ```
            
            3. When complete:
            ```
            ACTION: COMPLETE
            SUMMARY: Detailed explanation of all changes made
            ```
            $feedbackSection
        """.trimIndent()

        var response = LLMProvider.prompt<String>(
            system = prompt,
            userMessage = feedback ?: request,
            actions = emptyList(),
            temperature = 0.7,
            parameterizedTypeReference = null
        )

        // Check if the response contains multiple actions and handle accordingly
        val actionCount = actionParser.countActionsInResponse(response)
        if (actionCount > 1) {
            logger.info(
                "Multiple actions detected in response ({}). Reinstructing LLM to provide only one action.",
                actionCount
            )
            response = reinstructWithSingleActionPrompt(prompt)
        }

        // Parse the response to extract the action type and parameters
        val (actionType, parameters) = actionParser.parseResponse(response)

        // Create the action object using the handler
        val action = try {
            actionHandler.createAction(actionType, parameters, response)
        } catch (e: Exception) {
            // If we can't parse the action, return a Complete action with an error message
            CompleteAction("Failed to parse action from LLM response: ${e.message}. Raw response: $response")
        }

        return Pair(action, response)
    }

    private suspend fun reinstructWithSingleActionPrompt(prompt: String): String {
        // Reinvoke the LLM with a clarifying message
        val clarificationPrompt = """
                    I noticed that you provided multiple actions in your previous response. Please provide EXACTLY ONE action.
                    Choose the most important or logical next step only. You can perform additional actions in subsequent steps.
                    
                    Remember to follow ONE of these formats:
                    
                    1. For editing a file:
                    ```
                    ACTION: EDIT_FILE
                    FILE_PATH: /absolute/path/to/file
                    EXPLANATION: Brief explanation of changes
                    CONTENT:
                    // Complete new content of the file
                    ```
                    
                    2. For creating a file:
                    ```
                    ACTION: CREATE_FILE
                    FILE_PATH: /absolute/path/to/file
                    EXPLANATION: Brief explanation of the file purpose
                    CONTENT:
                    // Complete content of the new file
                    ```
                    
                    3. When complete:
                    ```
                    ACTION: COMPLETE
                    SUMMARY: Detailed explanation of all changes made
                    ```
                """.trimIndent()

        return LLMProvider.prompt(
            system = prompt,
            userMessage = clarificationPrompt,
            actions = emptyList(),
            temperature = 0.5,
            parameterizedTypeReference = null
        )
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}