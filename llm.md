# Kompanion Project Overview

## Project Summary
Kompanion is an AI code companion agent built with Kotlin. It provides a desktop application interface to interact with LLMs (Large Language Models) for coding assistance and blockchain analysis. The application is built using Jetbrains Compose for desktop UI and integrates with Spring AI to communicate with different AI providers.

## Core Architecture

### Key Components

1. **AI Integration**
   - Supports multiple LLM providers (OpenAI, Anthropic) through a provider interface
   - Uses Spring AI for standardized AI communication
   - Flexible model selection through `LLMProvider` abstraction

2. **Agent System**
   - Core `Agent` class manages interactions with the LLMs
   - Supports different operational modes (coding, blockchain)
   - Uses a context management system to track files and relevant information
   - Implements tools to extend AI capabilities

3. **UI Layer**
   - Built with Jetbrains Compose for desktop
   - Features a chat-based interface similar to ChatGPT/Claude
   - Switches between different operational modes
   - Supports file context management

4. **Tool System**
   - Implements system tools that the AI can use
   - File operations (reading, writing)
   - Blockchain tools for analysis

## Main Packages

- `ai` - LLM integration (OpenAI, Anthropic)
- `agent` - Core agent functionality and tool implementation
- `agent.blockchain` - Blockchain-specific functionality
- `agent.coding` - Code analysis and modification features
- `agent.modes` - Different operational modes
- `agent.tools` - Tool implementations
- `common` - Shared utilities and components
- `config` - Configuration management
- `ui` - UI components and screens

## Key Classes

- `LLMProvider` - Abstract class for LLM integration
- `Agent` - Core agent implementation
- `ContextManager` - Manages files and context for the agent
- `Mode` - Interface for different operational modes
- `ToolManager` - Manages available tools for agents
- `ChatScreen` - Main UI for user interaction
- `Kompanion` - Main application class and entry point

## Main Features

### Coding Assistant
- Code analysis and suggestions
- File operations (read, write, modify)
- Context-aware responses based on loaded files

### Blockchain Analysis
- Blockchain data retrieval and analysis
- Smart contract interaction
- Transaction history analysis

### UI Features
- Chat-based interface
- Mode switching (coding, blockchain)
- File context management
- Provider selection (OpenAI, Anthropic)
- Settings configuration

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetbrains Compose for Desktop
- **AI Integration**: Spring AI
- **LLM Providers**: OpenAI, Anthropic
- **Build System**: Gradle (Kotlin DSL)
- **Libraries**:
  - Kotlinx Coroutines
  - Arrow (functional programming)
  - JGraphT (graph algorithms)
  - Jackson (YAML/JSON handling)
  - Spring AI
  - Model Context Protocol (MCP)

## Project Structure
The project follows a typical Gradle structure with Kotlin source files under src/main/kotlin. The main entry point is in main.kt, which initializes the desktop UI.

## Running the Application
The application can be built and run using Gradle. The main class is MainKt, which is configured in the Gradle build file.

## Configuration
Configuration is handled through the AppConfig system, which manages settings like:
- Current AI provider
- Working directory
- Model settings
- Tool configurations

## Extension Points
The system is designed to be extensible through:
1. New LLM providers by implementing the LLMProvider interface
2. New tools by implementing the appropriate tool interfaces
3. New modes by implementing the Mode interface

This project integrates AI capabilities into a desktop environment to provide coding and blockchain analysis assistance through a familiar chat interface.