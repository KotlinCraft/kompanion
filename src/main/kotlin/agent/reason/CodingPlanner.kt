package agent.reason

import agent.domain.GenerationPlan
import agent.domain.Understanding
import org.slf4j.LoggerFactory

/**
 * Enhances the reasoning process with an intermediate step
 * that works with "Reasoning LLMs"
 */
class CodingPlanner(
    private val baseReasoner: SimplePlanner,
    private val reasoningStrategy: ReasoningStrategy
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun createPlan(request: String, understanding: Understanding): GenerationPlan {
        // Create initial plan with the base reasoner
        val initialPlan = baseReasoner.createPlan(request, understanding)
        logger.debug("Initial plan created: {}", initialPlan)

        // Enhance it with the reasoning strategy (no tools used)
        val enhancedPlan = reasoningStrategy.reason(request, initialPlan)
        logger.debug("Enhanced plan created: {}", enhancedPlan)

        logger.info(initialPlan.toString())
        logger.info(enhancedPlan.toString())
        return enhancedPlan
    }
}
