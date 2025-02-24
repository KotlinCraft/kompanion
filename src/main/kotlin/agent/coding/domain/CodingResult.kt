package agent.coding.domain

data class CodingResult(
        val editedFiles: List<String>,
        val createdFiles: List<String>
    )