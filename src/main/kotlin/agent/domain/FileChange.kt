package agent.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FileChange.CreateFile::class, name = "CreateFile"),
    JsonSubTypes.Type(value = FileChange.ModifyFile::class, name = "ModifyFile")
)
sealed class FileChange {
    data class CreateFile(
        val path: String,
        val content: String,
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
