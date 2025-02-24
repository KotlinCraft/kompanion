package agent.fileops

import config.AppConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

private const val KOMPANION_FOLDER = ".kompanion"

class KompanionFileHandler {
    companion object {

        fun kompanionFolderExists(): Boolean {
            val latestDirectory = AppConfig.load().latestDirectory
            val folder = File("$latestDirectory/$KOMPANION_FOLDER")
            return folder.exists()
        }

        fun createFolder() {
            val latestDirectory = AppConfig.load().latestDirectory
            val folder = File("$latestDirectory/$KOMPANION_FOLDER")
            if (!folder.exists()) {
                folder.mkdirs()
            }
        }

        fun readFromKompanionDirectory(fileName: String): String {
            val latestDirectory = AppConfig.load().latestDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/$fileName"
            return try {
                val file = File(filePath)
                file.readText()
            } catch (e: IOException) {
                println("Error reading file: ${e.message}")
                ""
            }
        }

        fun append(fileName: String, text: String) {
            if (willExceedSizeLimit(fileName, text)) {
                handleExceedingLimit(fileName, text)
            } else {
                appendData(fileName, text)
            }
        }

        private fun willExceedSizeLimit(fileName: String, text: String): Boolean {
            val fileSize = getFileSize(fileName)
            return (fileSize + text.toByteArray().size) > 1_048_576
        }

        private fun getFileSize(fileName: String): Long {
            val latestDirectory = AppConfig.load().latestDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/$fileName"
            return if (File(filePath).exists()) Files.size(Paths.get(filePath)) else 0L
        }

        private fun appendData(fileName: String, text: String) {
            val latestDirectory = AppConfig.load().latestDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/$fileName"
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    file.createNewFile()
                }
                file.appendText(text + "\n")
            } catch (e: IOException) {
                println("Error appending to file: ${e.message}")
            }
        }

        private fun handleExceedingLimit(fileName: String, text: String) {
            val latestDirectory = AppConfig.load().latestDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/$fileName"
            val file = File(filePath)
            if (!file.exists()) file.createNewFile()
            val currentContent = file.readLines().takeLast(1000).joinToString(separator = "\n")
            val newContent = (currentContent + text).takeLast(1_048_576)
            file.writeText(newContent)
        }
    }
}


enum class KompanionFile(
    val fileName: String
) {
    MESSAGE_HISTORY("message_history.txt"),
}