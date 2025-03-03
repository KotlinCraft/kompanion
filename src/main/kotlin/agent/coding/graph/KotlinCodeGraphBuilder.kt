package agent.coding.graph

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * A builder class that automatically constructs a KotlinCodeGraph by analyzing source files
 */
class KotlinCodeGraphBuilder {
    
    private val files = mutableListOf<KotlinCodeGraph.FileInfo>()
    private val classes = mutableListOf<KotlinCodeGraph.ClassInfo>()
    private val functions = mutableListOf<KotlinCodeGraph.FunctionInfo>()
    private val properties = mutableListOf<KotlinCodeGraph.PropertyInfo>()
    private val relationships = mutableListOf<KotlinCodeGraph.RelationshipInfo>()
    
    // Maps to track IDs for relationships
    private val fileIdMap = mutableMapOf<String, String>()
    private val classIdMap = mutableMapOf<String, String>()
    private val functionIdMap = mutableMapOf<Pair<String?, String>, String>()
    private val propertyIdMap = mutableMapOf<Pair<String?, String>, String>()
    
    /**
     * Builds a KotlinCodeGraph by traversing a directory and analyzing Kotlin files
     * 
     * @param directoryPath The root directory to start traversal from
     * @return A KotlinCodeGraph constructed from the analyzed code
     */
    fun buildFromDirectory(directoryPath: String): KotlinCodeGraph {
        val codeGraph = KotlinCodeGraph()
        val rootDir = Paths.get(directoryPath).toAbsolutePath()
        
        // Clear previous data
        clearCollections()
        
        // Walk through directory and process Kotlin files
        walkDirectoryAndProcessFiles(rootDir, rootDir)
        
        // Process relationships
        processRelationships()
        
        // Build the graph
        codeGraph.buildManually(files, classes, functions, properties, relationships)
        
        println("Built code graph with ${files.size} files, ${classes.size} classes, " +
                "${functions.size} functions, ${properties.size} properties, " +
                "${relationships.size} relationships")
        
        return codeGraph
    }
    
    /**
     * Clears all collections to ensure a fresh start
     */
    private fun clearCollections() {
        files.clear()
        classes.clear()
        functions.clear()
        properties.clear()
        relationships.clear()
        fileIdMap.clear()
        classIdMap.clear()
        functionIdMap.clear()
        propertyIdMap.clear()
    }
    
    /**
     * Recursively walks through a directory and processes Kotlin files
     */
    private fun walkDirectoryAndProcessFiles(rootDir: Path, baseDir: Path) {
        println("Scanning directory: $rootDir")
        
        Files.walk(rootDir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
            .forEach { 
                println("Processing file: $it")
                processKotlinFile(it, baseDir) 
            }
    }
    
    /**
     * Processes a single Kotlin file and extracts code information
     */
    private fun processKotlinFile(filePath: Path, baseDir: Path) {
        try {
            val content = Files.readString(filePath)
            val relativeFilePath = baseDir.relativize(filePath).toString().replace('\\', '/')
            
            // Extract file information
            extractFileInfo(relativeFilePath, content)
            
            // Extract classes, interfaces, objects
            extractClasses(relativeFilePath, content)
            
            // Extract top-level functions
            extractTopLevelFunctions(relativeFilePath, content)
            
            // Extract top-level properties
            extractTopLevelProperties(relativeFilePath, content)
            
        } catch (e: Exception) {
            println("Error processing file $filePath: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Extracts file information including package and imports
     */
    private fun extractFileInfo(filePath: String, content: String) {
        val packageRegex = """package\s+([^\s;]+)""".toRegex()
        val importRegex = """import\s+([^\s;]+)""".toRegex()
        
        val packageName = packageRegex.find(content)?.groupValues?.get(1) ?: ""
        val imports = importRegex.findAll(content).map { it.groupValues[1] }.toList()
        
        val fileId = "file:$filePath"
        fileIdMap[filePath] = fileId
        
        files.add(KotlinCodeGraph.FileInfo(
            path = filePath,
            imports = imports,
            summary = when {
                packageName.isNotEmpty() -> "Kotlin file: $packageName (${filePath.substringAfterLast('/')})"
                else -> "Kotlin file: ${filePath.substringAfterLast('/')}"
            }
        ))
    }
    
    /**
     * Extracts class, interface, and object declarations from file content
     */
    private fun extractClasses(filePath: String, content: String) {
        // Pattern for class declarations (including data and sealed classes)
        val classRegex = """(data\s+)?(sealed\s+)?class\s+(\w+)(?:<[^>]*>)?(?:\s*:\s*([^{]*))?""".toRegex()
        // Pattern for interface declarations
        val interfaceRegex = """interface\s+(\w+)(?:<[^>]*>)?(?:\s*:\s*([^{]*))?""".toRegex()
        // Pattern for object declarations
        val objectRegex = """object\s+(\w+)(?:\s*:\s*([^{]*))?""".toRegex()
        
        // Extract classes
        extractClassLikeDeclarations(filePath, content, classRegex, isData = { it.groupValues[1].isNotEmpty() }, 
                                    isSealed = { it.groupValues[2].isNotEmpty() }, 
                                    nameGroup = 3, superTypesGroup = 4)
        
        // Extract interfaces (treat as classes in the graph)
        extractClassLikeDeclarations(filePath, content, interfaceRegex, isData = { false }, 
                                    isSealed = { false }, 
                                    nameGroup = 1, superTypesGroup = 2, 
                                    typeSummary = "Interface")
        
        // Extract objects (treat as classes in the graph)
        extractClassLikeDeclarations(filePath, content, objectRegex, isData = { false }, 
                                    isSealed = { false }, 
                                    nameGroup = 1, superTypesGroup = 2, 
                                    typeSummary = "Object")
    }
    
    /**
     * Helper function to extract class-like declarations (classes, interfaces, objects)
     */
    private fun extractClassLikeDeclarations(
        filePath: String, 
        content: String, 
        regex: Regex,
        isData: (MatchResult) -> Boolean,
        isSealed: (MatchResult) -> Boolean,
        nameGroup: Int,
        superTypesGroup: Int,
        typeSummary: String = "Class"
    ) {
        regex.findAll(content).forEach { matchResult ->
            val className = matchResult.groupValues[nameGroup]
            val superTypesStr = matchResult.groupValues[superTypesGroup].trim()
            
            val superTypes = if (superTypesStr.isNotEmpty()) {
                superTypesStr.split(",").map { it.trim().split("<")[0] }
            } else {
                emptyList()
            }
            
            val classId = "class:$className"
            classIdMap[className] = classId
            
            classes.add(KotlinCodeGraph.ClassInfo(
                name = className,
                filePath = filePath,
                superTypes = superTypes,
                isData = isData(matchResult),
                isSealed = isSealed(matchResult),
                summary = "$typeSummary $className" + when {
                    isData(matchResult) -> " (data class)"
                    isSealed(matchResult) -> " (sealed class)"
                    else -> ""
                }
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = fileIdMap[filePath] ?: "file:$filePath",
                targetId = classId
            ))
            
            // Add inheritance relationships
            superTypes.forEach { superType ->
                relationships.add(KotlinCodeGraph.RelationshipInfo(
                    type = "INHERITS",
                    sourceId = classId,
                    targetId = "class:$superType"
                ))
            }
            
            // Extract class members
            val bodyRange = findBodyRange(content, matchResult.range.last)
            if (bodyRange != null) {
                val (bodyStart, bodyEnd) = bodyRange
                val classBody = content.substring(bodyStart, bodyEnd)
                extractClassMembers(filePath, className, classBody)
            }
        }
    }
    
    /**
     * Finds the start and end indices of a code block (enclosed by braces)
     */
    private fun findBodyRange(content: String, startIndex: Int): Pair<Int, Int>? {
        var openBraceIndex = -1
        var braceCount = 0
        
        for (i in startIndex until content.length) {
            when (content[i]) {
                '{' -> {
                    if (openBraceIndex == -1) openBraceIndex = i + 1 // +1 to skip the opening brace
                    braceCount++
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0 && openBraceIndex != -1) {
                        return Pair(openBraceIndex, i)
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts members (functions and properties) from a class body
     */
    private fun extractClassMembers(filePath: String, className: String, classBody: String) {
        // Extract functions
        extractClassFunctions(filePath, className, classBody)
        
        // Extract properties
        extractClassProperties(filePath, className, classBody)
    }
    
    /**
     * Extracts functions from a class body
     */
    private fun extractClassFunctions(filePath: String, className: String, classBody: String) {
        // Pattern for function declarations
        val functionRegex = """fun\s+(\w+)(?:<[^>]*>)?\s*\(([^)]*)\)(?:\s*:\s*([^{]*))?""".toRegex()
        
        functionRegex.findAll(classBody).forEach { matchResult ->
            val functionName = matchResult.groupValues[1]
            val parameters = matchResult.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val returnType = matchResult.groupValues[3].trim().takeIf { it.isNotEmpty() }
            
            val functionId = "function:$className.$functionName"
            functionIdMap[className to functionName] = functionId
            
            functions.add(KotlinCodeGraph.FunctionInfo(
                name = functionName,
                filePath = filePath,
                className = className,
                parameters = parameters,
                returnType = returnType,
                summary = "Function $functionName in $className"
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = classIdMap[className] ?: "class:$className",
                targetId = functionId
            ))
        }
        
        // Also handle extension functions
        val extensionFunctionRegex = """fun\s+([^.]+)\s*\.\s*(\w+)(?:<[^>]*>)?\s*\(([^)]*)\)(?:\s*:\s*([^{]*))?""".toRegex()
        
        extensionFunctionRegex.findAll(classBody).forEach { matchResult ->
            val receiverType = matchResult.groupValues[1].trim()
            val functionName = matchResult.groupValues[2]
            val parameters = matchResult.groupValues[3].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val returnType = matchResult.groupValues[4].trim().takeIf { it.isNotEmpty() }
            
            val functionId = "function:$className.$functionName"
            functionIdMap[className to functionName] = functionId
            
            functions.add(KotlinCodeGraph.FunctionInfo(
                name = functionName,
                filePath = filePath,
                className = className,
                parameters = parameters,
                returnType = returnType,
                summary = "Extension function $functionName for $receiverType in $className",
                isExtension = true,
                receiverType = receiverType
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = classIdMap[className] ?: "class:$className",
                targetId = functionId
            ))
            
            // Add extension relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "EXTENDS",
                sourceId = functionId,
                targetId = "class:$receiverType"
            ))
        }
    }
    
    /**
     * Extracts properties from a class body
     */
    private fun extractClassProperties(filePath: String, className: String, classBody: String) {
        // Pattern for val properties
        val valPropertyRegex = """val\s+(\w+)(?:\s*:\s*([^=\n]*))?(?:\s*=\s*[^{]*)?((?:\s*get\(\)[^}]*})?(?:\s*set\([^)]*\)[^}]*})?)""".toRegex()
        // Pattern for var properties
        val varPropertyRegex = """var\s+(\w+)(?:\s*:\s*([^=\n]*))?(?:\s*=\s*[^{]*)?((?:\s*get\(\)[^}]*})?(?:\s*set\([^)]*\)[^}]*})?)""".toRegex()
        
        // Extract val properties
        extractProperties(filePath, className, classBody, valPropertyRegex)
        
        // Extract var properties
        extractProperties(filePath, className, classBody, varPropertyRegex)
    }
    
    /**
     * Helper function to extract properties using a regex pattern
     */
    private fun extractProperties(filePath: String, className: String, content: String, propertyRegex: Regex) {
        propertyRegex.findAll(content).forEach { matchResult ->
            val propertyName = matchResult.groupValues[1]
            val propertyType = matchResult.groupValues[2].trim().takeIf { it.isNotEmpty() }
            val accessors = matchResult.groupValues[3]
            
            val hasCustomGetter = accessors.contains("get()")
            val hasCustomSetter = accessors.contains("set(")
            
            val propertyId = "property:$className.$propertyName"
            propertyIdMap[className to propertyName] = propertyId
            
            properties.add(KotlinCodeGraph.PropertyInfo(
                name = propertyName,
                filePath = filePath,
                className = className,
                type = propertyType,
                hasCustomGetter = hasCustomGetter,
                hasCustomSetter = hasCustomSetter,
                summary = "Property $propertyName in $className" + if (propertyType != null) " of type $propertyType" else ""
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = classIdMap[className] ?: "class:$className",
                targetId = propertyId
            ))
        }
    }
    
    /**
     * Extracts top-level function declarations from file content
     */
    private fun extractTopLevelFunctions(filePath: String, content: String) {
        // Get all class body ranges to exclude class members
        val classBodies = findAllClassBodies(content)
        
        // Pattern for regular functions
        val functionRegex = """fun\s+(\w+)(?:<[^>]*>)?\s*\(([^)]*)\)(?:\s*:\s*([^{]*))?""".toRegex()
        // Pattern for extension functions
        val extensionFunctionRegex = """fun\s+([^.]+)\s*\.\s*(\w+)(?:<[^>]*>)?\s*\(([^)]*)\)(?:\s*:\s*([^{]*))?""".toRegex()
        
        // Extract regular functions
        functionRegex.findAll(content).forEach { matchResult ->
            // Skip if function is inside a class body
            if (isInsideAnyRange(matchResult.range.first, classBodies)) {
                return@forEach
            }
            
            val functionName = matchResult.groupValues[1]
            val parameters = matchResult.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val returnType = matchResult.groupValues[3].trim().takeIf { it.isNotEmpty() }
            
            val functionId = "function:$functionName"
            functionIdMap[null to functionName] = functionId
            
            functions.add(KotlinCodeGraph.FunctionInfo(
                name = functionName,
                filePath = filePath,
                className = null,
                parameters = parameters,
                returnType = returnType,
                summary = "Top-level function $functionName"
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = fileIdMap[filePath] ?: "file:$filePath",
                targetId = functionId
            ))
        }
        
        // Extract extension functions
        extensionFunctionRegex.findAll(content).forEach { matchResult ->
            // Skip if function is inside a class body
            if (isInsideAnyRange(matchResult.range.first, classBodies)) {
                return@forEach
            }
            
            val receiverType = matchResult.groupValues[1].trim()
            val functionName = matchResult.groupValues[2]
            val parameters = matchResult.groupValues[3].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val returnType = matchResult.groupValues[4].trim().takeIf { it.isNotEmpty() }
            
            val functionId = "function:$functionName"
            functionIdMap[null to functionName] = functionId
            
            functions.add(KotlinCodeGraph.FunctionInfo(
                name = functionName,
                filePath = filePath,
                className = null,
                parameters = parameters,
                returnType = returnType,
                summary = "Top-level extension function $functionName for $receiverType",
                isExtension = true,
                receiverType = receiverType
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = fileIdMap[filePath] ?: "file:$filePath",
                targetId = functionId
            ))
            
            // Add extension relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "EXTENDS",
                sourceId = functionId,
                targetId = "class:$receiverType"
            ))
        }
    }
    
    /**
     * Extracts top-level property declarations from file content
     */
    private fun extractTopLevelProperties(filePath: String, content: String) {
        // Get all class body ranges to exclude class members
        val classBodies = findAllClassBodies(content)
        
        // Pattern for val properties
        val valPropertyRegex = """val\s+(\w+)(?:\s*:\s*([^=\n]*))?(?:\s*=\s*[^{]*)?((?:\s*get\(\)[^}]*})?(?:\s*set\([^)]*\)[^}]*})?)""".toRegex()
        // Pattern for var properties
        val varPropertyRegex = """var\s+(\w+)(?:\s*:\s*([^=\n]*))?(?:\s*=\s*[^{]*)?((?:\s*get\(\)[^}]*})?(?:\s*set\([^)]*\)[^}]*})?)""".toRegex()
        
        // Extract val properties
        extractTopLevelProperties(filePath, content, valPropertyRegex, classBodies)
        
        // Extract var properties
        extractTopLevelProperties(filePath, content, varPropertyRegex, classBodies)
    }
    
    /**
     * Helper function to extract top-level properties using a regex pattern
     */
    private fun extractTopLevelProperties(
        filePath: String, 
        content: String, 
        propertyRegex: Regex, 
        classBodies: List<Pair<Int, Int>>
    ) {
        propertyRegex.findAll(content).forEach { matchResult ->
            // Skip if property is inside a class body
            if (isInsideAnyRange(matchResult.range.first, classBodies)) {
                return@forEach
            }
            
            val propertyName = matchResult.groupValues[1]
            val propertyType = matchResult.groupValues[2].trim().takeIf { it.isNotEmpty() }
            val accessors = matchResult.groupValues[3]
            
            val hasCustomGetter = accessors.contains("get()")
            val hasCustomSetter = accessors.contains("set(")
            
            val propertyId = "property:$propertyName"
            propertyIdMap[null to propertyName] = propertyId
            
            properties.add(KotlinCodeGraph.PropertyInfo(
                name = propertyName,
                filePath = filePath,
                className = null,
                type = propertyType,
                hasCustomGetter = hasCustomGetter,
                hasCustomSetter = hasCustomSetter,
                summary = "Top-level property $propertyName" + if (propertyType != null) " of type $propertyType" else ""
            ))
            
            // Add containment relationship
            relationships.add(KotlinCodeGraph.RelationshipInfo(
                type = "CONTAINS",
                sourceId = fileIdMap[filePath] ?: "file:$filePath",
                targetId = propertyId
            ))
        }
    }
    
    /**
     * Finds all class body ranges in the content
     */
    private fun findAllClassBodies(content: String): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        
        // Patterns for class-like declarations
        val classRegex = """class\s+\w+(?:<[^>]*>)?(?:\s*:\s*[^{]*)?""".toRegex()
        val interfaceRegex = """interface\s+\w+(?:<[^>]*>)?(?:\s*:\s*[^{]*)?""".toRegex()
        val objectRegex = """object\s+\w+(?:\s*:\s*[^{]*)?""".toRegex()
        
        // Find class bodies
        findBodies(content, classRegex, result)
        
        // Find interface bodies
        findBodies(content, interfaceRegex, result)
        
        // Find object bodies
        findBodies(content, objectRegex, result)
        
        return result
    }
    
    /**
     * Helper function to find body ranges for a given regex pattern
     */
    private fun findBodies(content: String, regex: Regex, result: MutableList<Pair<Int, Int>>) {
        regex.findAll(content).forEach { matchResult ->
            val bodyRange = findBodyRange(content, matchResult.range.last)
            if (bodyRange != null) {
                result.add(bodyRange)
            }
        }
    }
    
    /**
     * Checks if a position is inside any of the given ranges
     */
    private fun isInsideAnyRange(position: Int, ranges: List<Pair<Int, Int>>): Boolean {
        return ranges.any { (start, end) -> position in start..end }
    }
    
    /**
     * Process relationships between code elements
     * This is a simplified version that focuses on import and inheritance relationships
     */
    private fun processRelationships() {
        // Process import relationships
        files.forEach { file ->
            file.imports.forEach { importPath ->
                // Try to resolve the import to a class
                val className = importPath.substringAfterLast('.')
                val classId = classIdMap[className]
                
                if (classId != null) {
                    relationships.add(KotlinCodeGraph.RelationshipInfo(
                        type = "IMPORTS",
                        sourceId = fileIdMap[file.path] ?: "file:${file.path}",
                        targetId = classId
                    ))
                }
            }
        }
        
        // A more complex implementation would analyze function bodies to detect:
        // - Function calls (CALLS relationships)
        // - Property usages (USES relationships)
    }
}

/**
 * Example usage of KotlinCodeGraphBuilder
 */
fun main(args: Array<String>) {
    val sourceDirectory = args.getOrElse(0) { "src" }
    val outputFile = args.getOrElse(1) { "kotlin_code_graph.json" }
    
    println("Analyzing Kotlin source code in: $sourceDirectory")
    println("Output will be saved to: $outputFile")
    
    try {
        val builder = KotlinCodeGraphBuilder()
        val codeGraph = builder.buildFromDirectory(sourceDirectory)
        
        // Save the graph to a file
        codeGraph.saveToFile(outputFile)
        
        // Print some statistics
        val stats = codeGraph.exportStatistics()
        println("\nGraph Statistics:")
        stats.forEach { (key, value) -> println("$key: $value") }
        
    } catch (e: Exception) {
        println("Error building code graph: ${e.message}")
        e.printStackTrace()
    }
}