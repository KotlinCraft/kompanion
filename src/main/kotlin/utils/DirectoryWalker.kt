package utils

import java.io.File

fun walkDirectory(dir: File, builder: StringBuilder, basePath: String = "") {
    val files = dir.listFiles() ?: return

    // Filter and sort files (non-empty directories and regular files)
    val sortedFiles = files
        .filter { file ->
            !file.name.startsWith(".") &&
                    file.name != "build" &&
                    file.name != "out" &&
                    file.name != "target" &&
                    (!file.isDirectory || (file.isDirectory && !isEmptyDirectory(file)))
        }
        .sortedWith(compareBy({ !it.isDirectory }, { it.name }))

    // If no valid files after filtering, return without adding anything
    if (sortedFiles.isEmpty()) return

    sortedFiles.forEach { file ->
        // Calculate the full path context
        val currentPath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

        if (file.isDirectory && file.listFiles().any { !it.isDirectory }) {
            builder.append("$currentPath/\n")

        } else {
            builder.append("$currentPath\n")
        }

        // Add a brief summary for code files
        if (!file.isDirectory && (file.extension == "kt" || file.extension == "java")) {
            val firstFewLines = file.readLines().take(2).joinToString("\n")
            val classLines = firstFewLines.lines().filter {
                it.contains("class ") || it.contains("interface ") || it.contains("object ")
            }

            classLines.forEach {
                builder.append("  ${it.trim()}\n")
            }
        }

        // Recursively process non-empty directories
        if (file.isDirectory) {
            walkDirectory(file, builder, currentPath)
        }
    }
}

/**
 * Checks if a directory is empty or only contains excluded items
 */
private fun isEmptyDirectory(dir: File): Boolean {
    val files = dir.listFiles() ?: return true

    // Check if there are any valid files (not hidden or build directories)
    return files.none { file ->
        !file.name.startsWith(".") &&
                file.name != "build" &&
                file.name != "out" &&
                file.name != "target" &&
                (!file.isDirectory || !isEmptyDirectory(file))
    }
}