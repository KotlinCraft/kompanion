package agent.domain

import java.nio.file.Path

data class CodeFile(
    val path: Path,
    val content: String,
    val language: String
)