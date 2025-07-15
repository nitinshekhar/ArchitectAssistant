package com.nitin.service;

import com.nitin.dto.Conversation;
import com.nitin.dto.DesignResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nitin.service.C4ModelService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DesignService {

    private static final Logger log = LoggerFactory.getLogger(DesignService.class);
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("```plantuml\s*([\s]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern UML_PATTERN = Pattern.compile("(@startuml[\s]*?@enduml)", Pattern.CASE_INSENSITIVE);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private PlantUmlService plantUmlService;

    @Autowired
    private C4ModelService c4ModelService;

    private static final String DESIGN_PROMPT_TEMPLATE = """
            You are a software architect expert specializing in C4 model diagrams. You will be provided with a user request for a design.
            
            Please provide a detailed software architecture design using the C4 model approach that includes:
            1. A brief explanation of the system architecture
            2. Key components, their responsibilities, and relationships
            3. A C4 model diagram using PlantUML C4 syntax
            
            For the C4 diagram, please:
            - Start with a Context diagram (Level 1) showing the system and external actors
            - Include Container diagram (Level 2) if the system is complex enough
            - Use proper C4 PlantUML syntax with Person(), System(), Container(), Component() elements
            - Include clear relationships with Rel() statements
            - Add meaningful descriptions for each element
            - Use appropriate C4 styling and layout
            
            C4 PlantUML syntax reference:
            - Person(alias, label, description)
            - System(alias, label, description)
            - Container(alias, label, technology, description)
            - Component(alias, label, technology, description)
            - Rel(from, to, label, technology)
            - Rel_Back(), Rel_Neighbor(), Rel_Up(), Rel_Down() for positioning
            
            Format your response as follows:
            EXPLANATION:
            [Your architecture explanation here, describing the C4 levels and design decisions]
            
            PLANTUML:
            ```plantuml
            @startuml
            !include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml
            !include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml
            
            [Your C4 PlantUML code here]
            @enduml
            ```
            
            Please ensure the C4 diagram is syntactically correct and follows C4 model principles.
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
            log.info("LLaMA response: " + response);

            // Parse the response
            return parseDesignResponse(response, userRequest, conversationHistory);

        } catch (Exception e) {
            log.error("Error generating design: " + e.getMessage());
            return DesignResponse.builder()
                    .userRequest(userRequest)
                    .explanation("Error generating design: " + e.getMessage())
                    .plantUmlCode("")
                    .success(false)
                    .conversation(Conversation.builder()
                            .message("Error generating design: " + e.getMessage())
                            .sender(Conversation.Sender.ASSISTANT)
                            .build())
                    .build();
        }
    }

    private DesignResponse parseDesignResponse(String response, String userRequest, List<Conversation> conversationHistory) {
        DesignResponse.DesignResponseBuilder builder = DesignResponse.builder()
                .userRequest(userRequest);

        List<Conversation> updatedConversation = new ArrayList<>();
        if (conversationHistory != null) {
            updatedConversation.addAll(conversationHistory);
        }
        updatedConversation.add(Conversation.builder().message(userRequest).sender(Conversation.Sender.USER).build());

        try {
            // Extract explanation
            String explanation = extractSection(response, "EXPLANATION:", "PLANTUML:");
            if (explanation.isEmpty()) {
                // Fallback: use everything before PlantUML block
                explanation = extractBeforePlantUML(response);
            }

            // Extract PlantUML code
            String plantUmlCode = extractPlantUMLCode(response);

            // Validate and fix UML syntax
            if (!plantUmlCode.isEmpty()) {
                plantUmlCode = c4ModelService.validateAndEnhanceC4Syntax(plantUmlCode);

                // Generate diagram
                String diagramPath = plantUmlService.generateDiagram(plantUmlCode);

                // Add the file name
                String diagramFilename = java.nio.file.Paths.get(diagramPath).getFileName().toString();

                updatedConversation.add(Conversation.builder().message(explanation).sender(Conversation.Sender.ASSISTANT).build());

                return builder
                        .explanation(explanation)
                        .plantUmlCode(plantUmlCode)
                        .diagramPath(diagramPath)
                        .diagramFilename(diagramFilename)
                        .success(true)
                        .conversation(Conversation.builder()
                                .message(explanation)
                                .sender(Conversation.Sender.ASSISTANT)
                                .design(builder.build())
                                .build())
                        .build();
            } else {
                String errorMessage = "No valid PlantUML code found in response";
                updatedConversation.add(Conversation.builder().message(errorMessage).sender(Conversation.Sender.ASSISTANT).build());
                return builder
                        .explanation(explanation)
                        .plantUmlCode("")
                        .success(false)
                        .errorMessage(errorMessage)
                        .conversation(Conversation.builder()
                                .message(errorMessage)
                                .sender(Conversation.Sender.ASSISTANT)
                                .build())
                        .build();
            }

        } catch (IOException e) {
            log.error("Error generating diagram: " + e.getMessage());
            String errorMessage = "Error generating diagram: " + e.getMessage();
            updatedConversation.add(Conversation.builder().message(errorMessage).sender(Conversation.Sender.ASSISTANT).build());
            return builder
                    .explanation(extractBeforePlantUML(response))
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

    private String extractPlantUMLCode(String text) {
        // Try to extract from markdown code block first
        Matcher markdownMatcher = MARKDOWN_PATTERN.matcher(text);

        if (markdownMatcher.find()) {
            log.info("Mark Down Matcher Found");
            return markdownMatcher.group(1).trim();
        }

        // Try to extract from @startuml...@enduml block
        Matcher umlMatcher = UML_PATTERN.matcher(text);

        if (umlMatcher.find()) {
            log.info("UML Matcher Found");
            return umlMatcher.group(1).trim();
        }

        return "";
    }
}