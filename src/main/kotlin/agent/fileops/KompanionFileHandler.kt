package agent.fileops

import config.AppConfig
import java.io.File
import java.io.IOException

private const val KOMPANION_FOLDER = ".kompanion"

class KompanionFileHandler {
    companion object {
        enum class KompanionFile(
            val fileName: String
        ) {
            MESSAGE_HISTORY("message_history.txt"),
        }

        fun folderExists(): Boolean {
            val latestDirectory = AppConfig.load().currentDirectory
            val folder = File("$latestDirectory/$KOMPANION_FOLDER")
            return folder.exists()
        }

        fun createFolder() {
            val latestDirectory = AppConfig.load().currentDirectory
            val folder = File("$latestDirectory/$KOMPANION_FOLDER")
            if (!folder.exists()) {
                folder.mkdirs()
            }
        }

        fun readFromKompanionDirectory(fileName: String): String {
            val latestDirectory = AppConfig.load().currentDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/$fileName"
            return try {
                val file = File(filePath)
                file.readText()
            } catch (e: IOException) {
                println("Error reading file: ${e.message}")
                ""
            }
        }

        fun appendToKompanionFile(fileName: String, text: String) {
            val latestDirectory = AppConfig.load().currentDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/$fileName"
            try {
                val file = File(filePath)
                file.appendText(text)
            } catch (e: IOException) {
                println("Error appending to file: ${e.message}")

            }
        }

        fun createFile(kompanionFile: KompanionFile) {
            val latestDirectory = AppConfig.load().currentDirectory
            val filePath = "$latestDirectory/$KOMPANION_FOLDER/${kompanionFile.fileName}"
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    file.createNewFile()
                }
            } catch (e: IOException) {
                println("Error creating file: ${e.message}")
            }
        }
    }
}
