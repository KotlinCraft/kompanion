package agent.domain

sealed class FileChange {
    data class CreateFile(
        val path: String,
        val content: String,
        val language: String
    ) : FileChange()

    data class ModifyFile(
        val path: String,
        val changes: List<Change>
    ) : FileChange() {
        data class Change(
            val searchContent: String,
            val replaceContent: String,
            val description: String
        )
    }
}
