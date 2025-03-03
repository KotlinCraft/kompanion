package agent.coding.graph

import com.google.gson.*
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.File
import java.io.Serializable

/**
 * Node types in our code graph
 */
sealed class CodeNode : Serializable {
    abstract val id: String
    abstract val summary: String
    abstract val type: String

    data class FileNode(
        override val id: String,
        val path: String,
        override val summary: String,
        val imports: List<String> = emptyList()
    ) : CodeNode() {
        override val type: String = "FILE"
    }

    data class ClassNode(
        override val id: String,
        val filePath: String,
        val name: String,
        val superTypes: List<String> = emptyList(),
        override val summary: String,
        val isData: Boolean = false,
        val isSealed: Boolean = false
    ) : CodeNode() {
        override val type: String = "CLASS"
    }

    data class FunctionNode(
        override val id: String,
        val filePath: String,
        val className: String?,
        val name: String,
        val parameters: List<String> = emptyList(),
        val returnType: String?,
        override val summary: String,
        val isExtension: Boolean = false,
        val receiverType: String? = null
    ) : CodeNode() {
        override val type: String = "FUNCTION"
    }

    data class PropertyNode(
        override val id: String,
        val filePath: String,
        val className: String?,
        val name: String,
        val propertyType: String?,
        override val summary: String,
        val hasCustomGetter: Boolean = false,
        val hasCustomSetter: Boolean = false
    ) : CodeNode() {
        override val type: String = "PROPERTY"
    }
}

/**
 * Edge types to represent different relationships
 */
sealed class CodeEdge : DefaultEdge(), Serializable {
    abstract val type: String
    abstract val sourceId: String
    abstract val targetId: String

    data class Imports(
        override val sourceId: String,
        override val targetId: String
    ) : CodeEdge() {
        override val type: String = "IMPORTS"
    }

    data class Contains(
        override val sourceId: String,
        override val targetId: String
    ) : CodeEdge() {
        override val type: String = "CONTAINS"
    }

    data class Calls(
        override val sourceId: String,
        override val targetId: String
    ) : CodeEdge() {
        override val type: String = "CALLS"
    }

    data class Inherits(
        override val sourceId: String,
        override val targetId: String
    ) : CodeEdge() {
        override val type: String = "INHERITS"
    }

    data class Uses(
        override val sourceId: String,
        override val targetId: String
    ) : CodeEdge() {
        override val type: String = "USES"
    }

    data class Extends(
        override val sourceId: String,
        override val targetId: String
    ) : CodeEdge() {
        override val type: String = "EXTENDS"
    }
}

/**
 * Serializable representation of the graph for persistence
 */
data class SerializedGraph(
    val nodes: List<CodeNode>,
    val edges: List<SerializedEdge>
) : Serializable

data class SerializedEdge(
    val type: String,
    val sourceId: String,
    val targetId: String
) : Serializable

/**
 * The main class for building and managing the code knowledge graph
 */
class KotlinCodeGraph : Serializable {
    // Using JGraphT for the graph implementation
    private val graph: Graph<CodeNode, CodeEdge> = DefaultDirectedGraph(CodeEdge::class.java)
    private val nodeIndex = mutableMapOf<String, CodeNode>()

    /**
     * Manually build a graph from existing code information
     * This method bypasses the need for the Kotlin compiler API
     */
    fun buildManually(
        files: List<FileInfo>,
        classes: List<ClassInfo>,
        functions: List<FunctionInfo>,
        properties: List<PropertyInfo>,
        relationships: List<RelationshipInfo>
    ) {
        // Create file nodes
        files.forEach { file ->
            val fileNode = CodeNode.FileNode(
                id = "file:${file.path}",
                path = file.path,
                summary = file.summary ?: "Kotlin file",
                imports = file.imports
            )
            addNode(fileNode)
        }

        // Create class nodes
        classes.forEach { cls ->
            val classNode = CodeNode.ClassNode(
                id = "class:${cls.name}",
                filePath = cls.filePath,
                name = cls.name,
                superTypes = cls.superTypes,
                summary = cls.summary ?: "Class ${cls.name}",
                isData = cls.isData,
                isSealed = cls.isSealed
            )
            addNode(classNode)

            // Add containment relationship to file
            addEdge("file:${cls.filePath}", classNode.id,
                CodeEdge.Contains("file:${cls.filePath}", classNode.id))
        }

        // Create function nodes
        functions.forEach { func ->
            val functionId = if (func.className != null) {
                "function:${func.className}.${func.name}"
            } else {
                "function:${func.name}"
            }

            val functionNode = CodeNode.FunctionNode(
                id = functionId,
                filePath = func.filePath,
                className = func.className,
                name = func.name,
                parameters = func.parameters,
                returnType = func.returnType,
                summary = func.summary ?: "Function ${func.name}",
                isExtension = func.isExtension,
                receiverType = func.receiverType
            )
            addNode(functionNode)

            // Add containment relationship
            val parentId = if (func.className != null) {
                "class:${func.className}"
            } else {
                "file:${func.filePath}"
            }

            addEdge(parentId, functionNode.id,
                CodeEdge.Contains(parentId, functionNode.id))
        }

        // Create property nodes
        properties.forEach { prop ->
            val propertyId = if (prop.className != null) {
                "property:${prop.className}.${prop.name}"
            } else {
                "property:${prop.name}"
            }

            val propertyNode = CodeNode.PropertyNode(
                id = propertyId,
                filePath = prop.filePath,
                className = prop.className,
                name = prop.name,
                propertyType = prop.type,
                summary = prop.summary ?: "Property ${prop.name}",
                hasCustomGetter = prop.hasCustomGetter,
                hasCustomSetter = prop.hasCustomSetter
            )
            addNode(propertyNode)

            // Add containment relationship
            val parentId = if (prop.className != null) {
                "class:${prop.className}"
            } else {
                "file:${prop.filePath}"
            }

            addEdge(parentId, propertyNode.id,
                CodeEdge.Contains(parentId, propertyNode.id))
        }

        // Add other relationships
        relationships.forEach { rel ->
            when (rel.type) {
                "IMPORTS" -> addEdge(rel.sourceId, rel.targetId,
                    CodeEdge.Imports(rel.sourceId, rel.targetId))
                "CALLS" -> addEdge(rel.sourceId, rel.targetId,
                    CodeEdge.Calls(rel.sourceId, rel.targetId))
                "INHERITS" -> addEdge(rel.sourceId, rel.targetId,
                    CodeEdge.Inherits(rel.sourceId, rel.targetId))
                "USES" -> addEdge(rel.sourceId, rel.targetId,
                    CodeEdge.Uses(rel.sourceId, rel.targetId))
                "EXTENDS" -> addEdge(rel.sourceId, rel.targetId,
                    CodeEdge.Extends(rel.sourceId, rel.targetId))
            }
        }
    }

    /**
     * Add a node to the graph and index
     */
    private fun addNode(node: CodeNode) {
        graph.addVertex(node)
        nodeIndex[node.id] = node
    }

    /**
     * Add an edge between nodes
     */
    private fun addEdge(sourceId: String, targetId: String, edge: CodeEdge) {
        val source = nodeIndex[sourceId]
        val target = nodeIndex[targetId]

        if (source != null && target != null) {
            graph.addEdge(source, target, edge)
        }
    }

    /**
     * Retrieve context for a specific code element
     */
    fun getContextForNode(nodeId: String, depth: Int = 1): List<CodeNode> {
        val node = nodeIndex[nodeId] ?: return emptyList()
        val result = mutableListOf(node)

        if (depth <= 0) return result

        // Get related nodes based on the type
        when (node) {
            is CodeNode.FileNode -> {
                // Get contained classes, functions, and properties
                graph.outgoingEdgesOf(node)
                    .filter { it is CodeEdge.Contains }
                    .mapNotNull { edge -> graph.getEdgeTarget(edge) }
                    .forEach { result.add(it) }
            }
            is CodeNode.ClassNode -> {
                // Get container file, parent classes, and contained members
                graph.incomingEdgesOf(node)
                    .filter { it is CodeEdge.Contains || it is CodeEdge.Inherits }
                    .mapNotNull { edge -> graph.getEdgeSource(edge) }
                    .forEach { result.add(it) }

                graph.outgoingEdgesOf(node)
                    .filter { it is CodeEdge.Contains || it is CodeEdge.Inherits }
                    .mapNotNull { edge -> graph.getEdgeTarget(edge) }
                    .forEach { result.add(it) }
            }
            is CodeNode.FunctionNode -> {
                // Get containing class/file, called functions, and functions that call this
                graph.incomingEdgesOf(node)
                    .filter { it is CodeEdge.Contains || it is CodeEdge.Calls }
                    .mapNotNull { edge -> graph.getEdgeSource(edge) }
                    .forEach { result.add(it) }

                graph.outgoingEdgesOf(node)
                    .filter { it is CodeEdge.Calls || it is CodeEdge.Uses }
                    .mapNotNull { edge -> graph.getEdgeTarget(edge) }
                    .forEach { result.add(it) }
            }
            is CodeNode.PropertyNode -> {
                // Get containing class/file and functions that use this property
                graph.incomingEdgesOf(node)
                    .filter { it is CodeEdge.Contains || it is CodeEdge.Uses }
                    .mapNotNull { edge -> graph.getEdgeSource(edge) }
                    .forEach { result.add(it) }
            }
        }

        // Recursively get context for related nodes with reduced depth
        if (depth > 1) {
            val nextNodes = result.toList().drop(1) // Don't include the original node again
            nextNodes.forEach { relatedNode ->
                result.addAll(getContextForNode(relatedNode.id, depth - 1).drop(1)) // Drop the node itself to avoid duplicates
            }
        }

        return result.distinctBy { it.id }
    }

    /**
     * Query nodes by criteria
     */
    fun queryNodes(query: String): List<CodeNode> {
        // Simple text-based search
        return nodeIndex.values.filter { node ->
            when (node) {
                is CodeNode.FileNode -> node.path.contains(query, ignoreCase = true)
                is CodeNode.ClassNode -> node.name.contains(query, ignoreCase = true)
                is CodeNode.FunctionNode -> node.name.contains(query, ignoreCase = true) ||
                        node.summary.contains(query, ignoreCase = true)
                is CodeNode.PropertyNode -> node.name.contains(query, ignoreCase = true) ||
                        node.type?.contains(query, ignoreCase = true) == true
            }
        }
    }

    /**
     * Save the graph to a file
     */
    fun saveToFile(filePath: String) {
        val file = File(filePath)
        file.parentFile?.mkdirs() // Create directories if they don't exist

        // Convert the graph to a serializable format
        val serializedGraph = createSerializedGraph()

        // Use Gson for serialization
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(CodeNode::class.java, CodeNodeAdapter())
            .registerTypeAdapter(CodeEdge::class.java, CodeEdgeAdapter())
            .create()

        try {
            val jsonString = gson.toJson(serializedGraph)
            file.writeText(jsonString)
            println("Graph saved to $filePath with ${nodeIndex.size} nodes and ${serializedGraph.edges.size} edges")
        } catch (e: Exception) {
            println("Error saving graph: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Load the graph from a file
     */
    fun loadFromFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }

        try {
            // Read the JSON content
            val jsonString = file.readText()

            // Parse the JSON content using Gson
            val gson = GsonBuilder()
                .registerTypeAdapter(CodeNode::class.java, CodeNodeAdapter())
                .registerTypeAdapter(CodeEdge::class.java, CodeEdgeAdapter())
                .create()

            val serializedGraph = gson.fromJson(jsonString, SerializedGraph::class.java)

            // Clear existing graph
            nodeIndex.clear()
            val vertices = graph.vertexSet().toList()
            vertices.forEach { graph.removeVertex(it) }

            // Restore nodes
            serializedGraph.nodes.forEach { node ->
                addNode(node)
            }

            // Restore edges
            serializedGraph.edges.forEach { edge ->
                when (edge.type) {
                    "IMPORTS" -> addEdge(edge.sourceId, edge.targetId, CodeEdge.Imports(edge.sourceId, edge.targetId))
                    "CONTAINS" -> addEdge(edge.sourceId, edge.targetId, CodeEdge.Contains(edge.sourceId, edge.targetId))
                    "CALLS" -> addEdge(edge.sourceId, edge.targetId, CodeEdge.Calls(edge.sourceId, edge.targetId))
                    "INHERITS" -> addEdge(edge.sourceId, edge.targetId, CodeEdge.Inherits(edge.sourceId, edge.targetId))
                    "USES" -> addEdge(edge.sourceId, edge.targetId, CodeEdge.Uses(edge.sourceId, edge.targetId))
                    "EXTENDS" -> addEdge(edge.sourceId, edge.targetId, CodeEdge.Extends(edge.sourceId, edge.targetId))
                }
            }

            println("Graph loaded from $filePath with ${nodeIndex.size} nodes and ${serializedGraph.edges.size} edges")
        } catch (e: Exception) {
            println("Error loading graph: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Create a serializable representation of the graph
     */
    private fun createSerializedGraph(): SerializedGraph {
        val nodes = nodeIndex.values.toList()
        val edges = mutableListOf<SerializedEdge>()

        // Collect all edges
        for (source in graph.vertexSet()) {
            val outgoingEdges = graph.outgoingEdgesOf(source)
            for (edge in outgoingEdges) {
                val target = graph.getEdgeTarget(edge)
                edges.add(SerializedEdge(
                    type = (edge as CodeEdge).type,
                    sourceId = source.id,
                    targetId = target.id
                ))
            }
        }

        return SerializedGraph(nodes, edges)
    }

    /**
     * Export graph statistics
     */
    fun exportStatistics(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()

        // Node statistics
        val totalNodes = nodeIndex.size
        val fileNodes = nodeIndex.values.count { it is CodeNode.FileNode }
        val classNodes = nodeIndex.values.count { it is CodeNode.ClassNode }
        val functionNodes = nodeIndex.values.count { it is CodeNode.FunctionNode }
        val propertyNodes = nodeIndex.values.count { it is CodeNode.PropertyNode }

        stats["totalNodes"] = totalNodes
        stats["fileNodes"] = fileNodes
        stats["classNodes"] = classNodes
        stats["functionNodes"] = functionNodes
        stats["propertyNodes"] = propertyNodes

        // Edge statistics
        val edges = graph.edgeSet()
        val totalEdges = edges.size
        val importsEdges = edges.count { it is CodeEdge.Imports }
        val containsEdges = edges.count { it is CodeEdge.Contains }
        val callsEdges = edges.count { it is CodeEdge.Calls }
        val inheritsEdges = edges.count { it is CodeEdge.Inherits }
        val usesEdges = edges.count { it is CodeEdge.Uses }
        val extendsEdges = edges.count { it is CodeEdge.Extends }

        stats["totalEdges"] = totalEdges
        stats["importsEdges"] = importsEdges
        stats["containsEdges"] = containsEdges
        stats["callsEdges"] = callsEdges
        stats["inheritsEdges"] = inheritsEdges
        stats["usesEdges"] = usesEdges
        stats["extendsEdges"] = extendsEdges

        return stats
    }

    /**
     * Data classes to hold information for building the graph manually
     */
    data class FileInfo(
        val path: String,
        val imports: List<String> = emptyList(),
        val summary: String? = null
    )

    data class ClassInfo(
        val name: String,
        val filePath: String,
        val superTypes: List<String> = emptyList(),
        val isData: Boolean = false,
        val isSealed: Boolean = false,
        val summary: String? = null
    )

    data class FunctionInfo(
        val name: String,
        val filePath: String,
        val className: String? = null,
        val parameters: List<String> = emptyList(),
        val returnType: String? = null,
        val isExtension: Boolean = false,
        val receiverType: String? = null,
        val summary: String? = null
    )

    data class PropertyInfo(
        val name: String,
        val filePath: String,
        val className: String? = null,
        val type: String? = null,
        val hasCustomGetter: Boolean = false,
        val hasCustomSetter: Boolean = false,
        val summary: String? = null
    )

    data class RelationshipInfo(
        val type: String,
        val sourceId: String,
        val targetId: String
    )
}

/**
 * Custom type adapter for CodeNode serialization
 */
class CodeNodeAdapter : JsonSerializer<CodeNode>, JsonDeserializer<CodeNode> {
    override fun serialize(src: CodeNode, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src.type)

        when (src) {
            is CodeNode.FileNode -> {
                jsonObject.addProperty("id", src.id)
                jsonObject.addProperty("path", src.path)
                jsonObject.addProperty("summary", src.summary)
                val importsArray = JsonArray()
                src.imports.forEach { importsArray.add(it) }
                jsonObject.add("imports", importsArray)
            }
            is CodeNode.ClassNode -> {
                jsonObject.addProperty("id", src.id)
                jsonObject.addProperty("filePath", src.filePath)
                jsonObject.addProperty("name", src.name)
                jsonObject.addProperty("summary", src.summary)
                jsonObject.addProperty("isData", src.isData)
                jsonObject.addProperty("isSealed", src.isSealed)
                val superTypesArray = JsonArray()
                src.superTypes.forEach { superTypesArray.add(it) }
                jsonObject.add("superTypes", superTypesArray)
            }
            is CodeNode.FunctionNode -> {
                jsonObject.addProperty("id", src.id)
                jsonObject.addProperty("filePath", src.filePath)
                jsonObject.addProperty("className", src.className)
                jsonObject.addProperty("name", src.name)
                jsonObject.addProperty("summary", src.summary)
                jsonObject.addProperty("returnType", src.returnType)
                jsonObject.addProperty("isExtension", src.isExtension)
                jsonObject.addProperty("receiverType", src.receiverType)
                val parametersArray = JsonArray()
                src.parameters.forEach { parametersArray.add(it) }
                jsonObject.add("parameters", parametersArray)
            }
            is CodeNode.PropertyNode -> {
                jsonObject.addProperty("id", src.id)
                jsonObject.addProperty("filePath", src.filePath)
                jsonObject.addProperty("className", src.className)
                jsonObject.addProperty("name", src.name)
                jsonObject.addProperty("type", src.type)
                jsonObject.addProperty("summary", src.summary)
                jsonObject.addProperty("hasCustomGetter", src.hasCustomGetter)
                jsonObject.addProperty("hasCustomSetter", src.hasCustomSetter)
            }
        }

        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): CodeNode {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString ?: throw IllegalStateException("Node missing type field")

        return when (type) {
            "FILE" -> {
                val imports = mutableListOf<String>()
                jsonObject.get("imports")?.asJsonArray?.forEach { imports.add(it.asString) }

                CodeNode.FileNode(
                    id = jsonObject.get("id").asString,
                    path = jsonObject.get("path").asString,
                    summary = jsonObject.get("summary").asString,
                    imports = imports
                )
            }
            "CLASS" -> {
                val superTypes = mutableListOf<String>()
                jsonObject.get("superTypes")?.asJsonArray?.forEach { superTypes.add(it.asString) }

                CodeNode.ClassNode(
                    id = jsonObject.get("id").asString,
                    filePath = jsonObject.get("filePath").asString,
                    name = jsonObject.get("name").asString,
                    summary = jsonObject.get("summary").asString,
                    isData = jsonObject.get("isData")?.asBoolean ?: false,
                    isSealed = jsonObject.get("isSealed")?.asBoolean ?: false,
                    superTypes = superTypes
                )
            }
            "FUNCTION" -> {
                val parameters = mutableListOf<String>()
                jsonObject.get("parameters")?.asJsonArray?.forEach { parameters.add(it.asString) }

                CodeNode.FunctionNode(
                    id = jsonObject.get("id").asString,
                    filePath = jsonObject.get("filePath").asString,
                    className = if (jsonObject.has("className") && !jsonObject.get("className").isJsonNull)
                        jsonObject.get("className").asString else null,
                    name = jsonObject.get("name").asString,
                    summary = jsonObject.get("summary").asString,
                    returnType = if (jsonObject.has("returnType") && !jsonObject.get("returnType").isJsonNull)
                        jsonObject.get("returnType").asString else null,
                    isExtension = jsonObject.get("isExtension")?.asBoolean ?: false,
                    receiverType = if (jsonObject.has("receiverType") && !jsonObject.get("receiverType").isJsonNull)
                        jsonObject.get("receiverType").asString else null,
                    parameters = parameters
                )
            }
            "PROPERTY" -> {
                CodeNode.PropertyNode(
                    id = jsonObject.get("id").asString,
                    filePath = jsonObject.get("filePath").asString,
                    className = if (jsonObject.has("className") && !jsonObject.get("className").isJsonNull)
                        jsonObject.get("className").asString else null,
                    name = jsonObject.get("name").asString,
                    propertyType = if (jsonObject.has("type") && !jsonObject.get("type").isJsonNull)
                        jsonObject.get("type").asString else null,
                    summary = jsonObject.get("summary").asString,
                    hasCustomGetter = jsonObject.get("hasCustomGetter")?.asBoolean ?: false,
                    hasCustomSetter = jsonObject.get("hasCustomSetter")?.asBoolean ?: false
                )
            }
            else -> throw IllegalArgumentException("Unknown node type: $type")
        }
    }
}

/**
 * Custom type adapter for CodeEdge serialization
 */
class CodeEdgeAdapter : JsonSerializer<CodeEdge>, JsonDeserializer<CodeEdge> {
    override fun serialize(src: CodeEdge, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src.type)
        jsonObject.addProperty("sourceId", src.sourceId)
        jsonObject.addProperty("targetId", src.targetId)
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): CodeEdge {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString
        val sourceId = jsonObject.get("sourceId").asString
        val targetId = jsonObject.get("targetId").asString

        return when (type) {
            "IMPORTS" -> CodeEdge.Imports(sourceId, targetId)
            "CONTAINS" -> CodeEdge.Contains(sourceId, targetId)
            "CALLS" -> CodeEdge.Calls(sourceId, targetId)
            "INHERITS" -> CodeEdge.Inherits(sourceId, targetId)
            "USES" -> CodeEdge.Uses(sourceId, targetId)
            "EXTENDS" -> CodeEdge.Extends(sourceId, targetId)
            else -> throw IllegalArgumentException("Unknown edge type: $type")
        }
    }
}

/**
 * Example of how to use the graph
 */
fun main() {
    val codeGraph = KotlinCodeGraph()

    // Create example data - in a real scenario, this would come from your code parser
    val files = listOf(
        KotlinCodeGraph.FileInfo(
            path = "src/main/kotlin/com/example/UserService.kt",
            imports = listOf("com.example.model.User", "com.example.repository.UserRepository"),
            summary = "User service implementation"
        )
    )

    val classes = listOf(
        KotlinCodeGraph.ClassInfo(
            name = "UserService",
            filePath = "src/main/kotlin/com/example/UserService.kt",
            superTypes = listOf("Service"),
            isData = false,
            summary = "Service for managing users"
        )
    )

    val functions = listOf(
        KotlinCodeGraph.FunctionInfo(
            name = "authenticate",
            filePath = "src/main/kotlin/com/example/UserService.kt",
            className = "UserService",
            parameters = listOf("username: String", "password: String"),
            returnType = "User?",
            summary = "Authenticates a user with username and password"
        ),
        KotlinCodeGraph.FunctionInfo(
            name = "createUser",
            filePath = "src/main/kotlin/com/example/UserService.kt",
            className = "UserService",
            parameters = listOf("username: String", "email: String", "password: String"),
            returnType = "User",
            summary = "Creates a new user account"
        )
    )

    val properties = listOf(
        KotlinCodeGraph.PropertyInfo(
            name = "userRepository",
            filePath = "src/main/kotlin/com/example/UserService.kt",
            className = "UserService",
            type = "UserRepository",
            summary = "Repository for user data access"
        )
    )

    val relationships = listOf(
        KotlinCodeGraph.RelationshipInfo(
            type = "CALLS",
            sourceId = "function:UserService.authenticate",
            targetId = "function:UserService.createUser"
        )
    )

    // Build the graph manually
    codeGraph.buildManually(files, classes, functions, properties, relationships)

    // Save the graph
    codeGraph.saveToFile("kotlin_code_graph.json")

    // Load the graph in a new instance
    val loadedGraph = KotlinCodeGraph()
    loadedGraph.loadFromFile("kotlin_code_graph.json")

    // Print graph statistics
    val stats = loadedGraph.exportStatistics()
    println("\nGraph Statistics:")
    stats.forEach { (key, value) -> println("$key: $value") }

    // Get context for a function
    val context = loadedGraph.getContextForNode("function:UserService.authenticate", 2)
    println("\nContext for UserService.authenticate:")
    context.forEach { node ->
        println("${node.id}: ${node.summary}")
    }

    // Search for nodes
    val queryResults = loadedGraph.queryNodes("user")
    println("\nSearch results for 'user':")
    queryResults.forEach { node ->
        println("${node.id}: ${node.summary}")
    }
}