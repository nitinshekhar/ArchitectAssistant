
package com.nitin.service;

import com.nitin.dto.Conversation;
import com.nitin.dto.DesignResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DesignService {

    private static final Logger log = LoggerFactory.getLogger(DesignService.class);
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("```plantuml\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern UML_PATTERN = Pattern.compile("(@startuml[\\s\\S]*?@enduml)", Pattern.CASE_INSENSITIVE);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private PlantUmlService plantUmlService;

    @Autowired
    private C4ModelService c4ModelService;

    private record DiagramResult(String diagramPath, String diagramFilename) {}

    private static final String DESIGN_PROMPT_TEMPLATE = """
            You are a software architect expert specializing in C4 model diagrams.
            Your task is to create a software architecture design based on the user's request.
            Do NOT include any internal thought processes, planning, or `<think>` sections in your response.

            You MUST follow these instructions:
            1.  Provide a clear explanation of the architecture, its key components, and their relationships.
            2.  After the explanation, create a C4 model diagram using PlantUML syntax.
            3.  The diagram MUST be enclosed in a ```plantuml markdown block.
            4.  You MUST use the official C4-PlantUML syntax, including elements like Person(), System(), Container(), and Rel().
            5.  DO NOT use ASCII art or box-drawing characters like '+----+' to draw the diagram. You must use the C4-PlantUML keywords.

            Here is an example of a CORRECT C4 PlantUML diagram:
            ```plantuml
            @startuml
            !include C4_Context.puml

            Person(customer, "Customer", "A user of the system.")
            System(mySystem, "My Awesome System", "The system being designed.")

            Rel(customer, mySystem, "Uses")
            @enduml
            ```

            Now, generate the design for the user's request.
            """;

    public DesignResponse generateDesign(String userRequest, List<Conversation> conversationHistory) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(DESIGN_PROMPT_TEMPLATE));

            if (conversationHistory != null) {
                for (Conversation convo : conversationHistory) {
                    if (convo.getSender() == Conversation.Sender.USER) {
                        messages.add(UserMessage.from(convo.getMessage()));
                    } else if (convo.getSender() == Conversation.Sender.ASSISTANT) {
                        messages.add(SystemMessage.from(convo.getMessage()));
                    }
                }
            }
            messages.add(UserMessage.from(userRequest));

            // Generate response from LLaMA
            String response = chatLanguageModel.generate(messages).content().text();
            log.info("Full LLaMA response: " + response);

            // Parse the response
            return parseDesignResponse(response, userRequest);

        } catch (Exception e) {
            log.error("Error generating design: " + e.getMessage());
            return buildErrorResponse(userRequest, "Error generating design: " + e.getMessage());
        }
    }

    private DesignResponse parseDesignResponse(String response, String userRequest) {
        String explanation = extractExplanation(response);
        String plantUmlCode = extractPlantUMLCode(response);

        if (plantUmlCode.isEmpty()) {
            return buildNoUmlResponse(userRequest, explanation);
        }

        try {
            DiagramResult diagramResult = generateDiagramFromUml(plantUmlCode);
            return buildSuccessResponse(userRequest, explanation, plantUmlCode, diagramResult);
        } catch (IOException e) {
            log.error("Error generating diagram: " + e.getMessage());
            return buildErrorResponse(userRequest, "Error generating diagram: " + e.getMessage());
        }
    }

    private DiagramResult generateDiagramFromUml(String plantUmlCode) throws IOException {
        String processedUml = c4ModelService.validateAndEnhanceC4Syntax(plantUmlCode);
        String diagramPath = plantUmlService.generateDiagram(processedUml);
        String diagramFilename = java.nio.file.Paths.get(diagramPath).getFileName().toString();
        return new DiagramResult(diagramPath, diagramFilename);
    }

    private String extractExplanation(String response) {
        String explanation = extractSection(response, "EXPLANATION:", "PLANTUML:");
        return explanation.isEmpty() ? extractBeforePlantUML(response) : explanation;
    }

    private String extractPlantUMLCode(String text) {
        log.debug("Attempting to extract PlantUML code from text:\n{}", text);
        StringBuilder allUmlCode = new StringBuilder();

        Matcher markdownMatcher = MARKDOWN_PATTERN.matcher(text);
        boolean found = false;
        while (markdownMatcher.find()) {
            if (!found) {
                log.info("Found PlantUML code within markdown block(s).");
                found = true;
            }
            String umlBlock = markdownMatcher.group(1).trim();
            allUmlCode.append(umlBlock).append("\n");
        }

        if (found) {
            return allUmlCode.toString();
        }

        log.warn("No valid PlantUML code found in the response.");
        return "";
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return "";

        start += startMarker.length();
        int end = text.indexOf(endMarker, start);
        if (end == -1) end = text.length();

        return text.substring(start, end).trim();
    }

    private String extractBeforePlantUML(String text) {
        int plantUmlStart = text.indexOf("```plantuml");
        if (plantUmlStart == -1) {
            plantUmlStart = text.indexOf("@startuml");
        }

        if (plantUmlStart != -1) {
            return text.substring(0, plantUmlStart).trim();
        }

        return text.trim();
    }

    private DesignResponse buildSuccessResponse(String userRequest, String explanation, String plantUmlCode, DiagramResult diagramResult) {
        return DesignResponse.builder()
                .userRequest(userRequest)
                .explanation(explanation)
                .plantUmlCode(plantUmlCode)
                .diagramPath(diagramResult.diagramPath())
                .diagramFilename(diagramResult.diagramFilename())
                .success(true)
                .conversation(Conversation.builder()
                        .message(explanation)
                        .sender(Conversation.Sender.ASSISTANT)
                        .build())
                .build();
    }

    private DesignResponse buildNoUmlResponse(String userRequest, String explanation) {
        return DesignResponse.builder()
                .userRequest(userRequest)
                .explanation(explanation)
                .plantUmlCode("")
                .success(false)
                .errorMessage("No valid PlantUML code found in response")
                .conversation(Conversation.builder()
                        .message("No valid PlantUML code found in response")
                        .sender(Conversation.Sender.ASSISTANT)
                        .build())
                .build();
    }

    private DesignResponse buildErrorResponse(String userRequest, String errorMessage) {
        return DesignResponse.builder()
                .userRequest(userRequest)
                .explanation(errorMessage)
                .plantUmlCode("")
                .success(false)
                .errorMessage(errorMessage)
                .conversation(Conversation.builder()
                        .message(errorMessage)
                        .sender(Conversation.Sender.ASSISTANT)
                        .build())
                .build();
    }
}