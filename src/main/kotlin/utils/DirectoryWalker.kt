package utils

import java.io.File

fun walkDirectory(dir: File, builder: StringBuilder, indentLevel: Int) {
        val indent = "  ".repeat(indentLevel)
        val files = dir.listFiles() ?: return

        files.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { file ->
            if (file.name.startsWith(".") || file.name == "build" || file.name == "out") {
                return@forEach  // Skip hidden and build directories
            }

            builder.append("$indent${file.name}${if (file.isDirectory) "/" else ""}\n")

            // Add a brief summary for code files
            if (!file.isDirectory && file.extension == "kt") {
                val firstFewLines = file.readLines().take(10).joinToString("\n")
                val packageLine = firstFewLines.lines().find { it.startsWith("package ") }
                val classLines = firstFewLines.lines().filter {
                    it.contains("class ") || it.contains("interface ") || it.contains("object ")
                }

                if (packageLine != null) {
                    builder.append("$indent  $packageLine\n")
                }
                classLines.forEach {
                    builder.append("$indent  ${it.trim()}\n")
                }
            }

            if (file.isDirectory) {
                walkDirectory(file, builder, indentLevel + 1)
            }
        }
    }