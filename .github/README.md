# Kompanion

AI-powered code assistant that iteratively generates and improves Kotlin code through reasoning and feedback loops.

## 🚧 Coming Soon 🚧

Kompanion is currently under active development. Stay tuned for the first release!

<img width="790" alt="image" src="https://github.com/user-attachments/assets/ccbb5e1a-f3a7-4ef7-bf08-6ff9fcdb52c6" />



## Overview

Kompanion is an intelligent coding assistant that helps you generate, improve, and understand Kotlin code. It uses a multi-step reasoning approach to:

1. Understand your requirements and existing codebase
2. Generate appropriate solutions
3. Iteratively refine code based on constraints and feedback
4. Provide clear explanations of its approach

## Features

- **Interactive Chat Interface** - A dynamic chat UI supporting multiple modes:
    - **Code Mode:** For code-related queries and generation.
    - **Ask Mode:** For general inquiries and explanations.
- **Slash Commands** - Quickly switch modes and invoke commands (e.g., `/code`, `/ask`, `/help`, `/clear`).
- **Auto-Scrolling** - The chat automatically scrolls to reveal new messages.
- **Working Directory Selector** - Update your working directory via a file selector with changes saved to your configuration.
- **Settings Dialog** - Easily update configuration settings (like your OpenAI API key) through an intuitive dialog.

## Getting Started

### Prerequisites

- JDK 11 or higher
- Kotlin 1.7+
- Gradle 7.0+

### Installation

```bash
git clone https://github.com/yourusername/kompanion.git
cd kompanion
./gradlew build
```

### Running Kompanion

```bash
./gradlew run
```

## Usage

Once launched, Kompanion offers an interactive chat interface that allows you to:

1. **Interact Seamlessly:** Switch between code and ask modes using slash commands or the mode indicator in the topbar.
2. **Send Messages Efficiently:** Use Command+Enter (Meta+Enter) to send messages.
3. **Stay Up-to-Date:** Enjoy automatic scrolling to see the latest messages without manual intervention.
4. **Customize Your Environment:** Easily update your working directory and configuration settings using the integrated file selector and settings dialog.

Example interactions coming soon!

## Architecture

Kompanion is built on a modular architecture with:

- Reasoning Engine - For understanding requirements and planning solutions
- Code Generation - For producing and refining Kotlin code
- Context Management - For maintaining awareness of your codebase
- Feedback System - For continuous improvement

## Contributing

Contribution guidelines will be available once the project reaches beta status.

## License

[MIT License](LICENSE)

## Acknowledgments

- This project builds upon research in AI-assisted programming
- Thanks to the Kotlin community for excellent language tooling
