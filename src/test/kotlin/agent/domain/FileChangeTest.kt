package agent.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileChangeTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    @Test
    fun `test serialization and deserialization of CreateFile`() {
        val createFile = FileChange.CreateFile(
            path = "src/main/kotlin/NewFile.kt",
            content = "fun main() {}",
            language = "kotlin"
        )

        val json = objectMapper.writeValueAsString(createFile)
        val deserialized = objectMapper.readValue<FileChange.CreateFile>(json)

        assertEquals(createFile, deserialized)
    }

    @Test
    fun `test serialization and deserialization of ModifyFile`() {
        val modifyFile = FileChange.ModifyFile(
            path = "src/main/kotlin/ExistingFile.kt",
            changes = listOf(
                FileChange.ModifyFile.Change(
                    searchContent = "oldContent",
                    replaceContent = "newContent",
                    description = "Update oldContent to newContent"
                )
            )
        )

        val json = objectMapper.writeValueAsString(modifyFile)
        val deserialized = objectMapper.readValue<FileChange.ModifyFile>(json)

        assertEquals(modifyFile, deserialized)
    }
}
