package agent.domain

data class GenerationResult(
    val fileChanges: List<FileChange>,
    val explanation: String,
    val metadata: Map<String, Any> = emptyMap()
) {

    fun formatted(): String {
        return fileChanges.joinToString("\n") {
            when (it) {
                is FileChange.CreateFile -> """
                        // + ${it.path}
                        + ${it.content}
                    """.trimIndent()

                is FileChange.ModifyFile -> it.changes.joinToString("\n") { change ->
                    """ | // ~ // ${it.path}
                            |- ${change.searchContent}
                            | + ${change.replaceContent}""".trimMargin()
                }
            }
        }
    }
}
