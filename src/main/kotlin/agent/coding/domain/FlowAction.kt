package agent.coding.domain

sealed class FlowAction {

    abstract fun summary(): String

    data class EditFile(
        val filePath: String,
        val content: String,
        val explanation: String
    ) : FlowAction() {
        override fun summary(): String {
            return explanation
        }
    }

    data class CreateFile(
        val filePath: String,
        val content: String,
        val explanation: String
    ) : FlowAction() {
        override fun summary(): String {
            return explanation
        }
    }

    data class Complete(
        val explanation: String
    ) : FlowAction() {
        override fun summary(): String {
            return explanation
        }
    }
}
