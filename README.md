# Architect Assistant

## Project Description
The Architect Assistant is a Spring Boot application designed to assist with architectural design tasks. It integrates with `llama.cpp` for AI-powered assistance and `PlantUML` for generating diagrams, specifically supporting C4 models.

## Technologies Used
*   **Spring Boot**: Framework for building the application.
*   **Thymeleaf**: Server-side Java template engine for web applications.
*   **LangChain4j**: Java library for building LLM-powered applications, integrated with `llama.cpp` for local AI model inference.
*   **PlantUML**: Tool for generating diagrams from a plain text language, used here for C4 models.
*   **Lombok**: Library to reduce boilerplate code.
*   **Jackson**: For JSON processing.
*   **Apache Commons Lang3**: Provides helper utilities for Java.

## Prerequisites
*   Java 21 or higher
*   Maven
*   A `llama.cpp` server running (e.g., `llama.cpp` with a compatible model like Llama 2)

## Configuration

The `application.properties` file located in `src/main/resources/` contains the following configurable properties:

```properties
server.port = 8080

llama.base-url=http://localhost:8081
llama.model-path="/Users/nshekhar/Workspace/model/llama.cpp/llama-2-7b-chat.Q4_K_M.gguf"
llama.context-size=4096
llama.temperature=0.7
llama.max-tokens=2048
llama.model-name="llama"

plantuml.output-directory="/Users/nshekhar/download/uml-diagram"
plantuml.image-format="PNG"

logging.level.com.nitin=DEBUG
logging.level.dev.lanchain4j=DEBUG
```

*   **`server.port`**: The port on which the Spring Boot application will run.
*   **`llama.base-url`**: The base URL for your running `llama.cpp` server.
*   **`llama.model-path`**: The absolute path to your Llama model file (e.g., `llama-2-7b-chat.Q4_K_M.gguf`).
*   **`llama.context-size`**: The context window size for the Llama model.
*   **`llama.temperature`**: Controls the randomness of the Llama model's output.
*   **`llama.max-tokens`**: The maximum number of tokens to generate in the Llama model's response.
*   **`llama.model-name`**: The name of the Llama model being used.
*   **`plantuml.output-directory`**: The directory where generated PlantUML diagrams will be saved.
*   **`plantuml.image-format`**: The image format for the generated PlantUML diagrams (e.g., PNG, SVG).

## Getting Started

### 1. Clone the repository
```bash
git clone <repository-url>
cd architect-assistant
```

### 2. Build the project
```bash
mvn clean install
```

### 3. Run the application
Ensure your `llama.cpp` server is running and accessible at the configured `llama.base-url`.
```bash
mvn spring-boot:run
```

The application will be accessible at `http://localhost:8080` (or the port configured in `application.properties`).

## Features

### C4 Model Generation
The application can assist in generating C4 model diagrams using PlantUML. This allows for clear and concise representation of software architecture.

### AI-Powered Assistance
Leverages `llama.cpp` through LangChain4j to provide intelligent assistance for architectural design queries and tasks.
