package com.nitin.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Service
public class C4ModelService {

    public String validateAndEnhanceC4Syntax(String c4Code) {
        String enhanced = c4Code.trim();

        // Fix common syntax errors from LLM
        enhanced = enhanced.replaceAll("([a-zA-Z]+)/([a-zA-Z0-9_]+),", "$1($2,");

        // Remove existing C4 includes to avoid conflicts
        enhanced = enhanced.replaceAll("(?i)!include\\s+<?C4_\\w+.puml>?", "");

        // Add @startuml and @enduml if missing
        if (!enhanced.startsWith("@startuml")) {
            enhanced = "@startuml\n" + enhanced;
        }
        if (!enhanced.endsWith("@enduml")) {
            enhanced = enhanced + "\n@enduml";
        }

        // Build the header with includes and styling
        StringBuilder header = new StringBuilder();

        // Add includes based on elements found
        boolean hasComponent = enhanced.contains("Component(");
        boolean hasContainer = enhanced.contains("Container(");
        boolean hasContext = enhanced.contains("System(") || enhanced.contains("Person(");

        if (hasContext || hasContainer || hasComponent) {
            header.append("!include C4_Context.puml\n");
        }
        if (hasContainer || hasComponent) {
            header.append("!include C4_Container.puml\n");
        }
        if (hasComponent) {
            header.append("!include C4_Component.puml\n");
        }

        // Add basic C4 styling if not present
        if (!enhanced.contains("LAYOUT_") && !enhanced.contains("HIDE_STEREOTYPE")) {
            header.append(getC4Styling()).append("\n");
        }
        
        // Prepend header after @startuml
        enhanced = enhanced.replaceFirst("(?i)@startuml", "@startuml\n" + header.toString());

        return enhanced;
    }

    private String getC4Styling() {
        return """
                LAYOUT_WITH_LEGEND()
                HIDE_STEREOTYPE()""";
    }

    public C4DiagramType detectDiagramType(String c4Code) {
        String lowerCode = c4Code.toLowerCase();

        if (lowerCode.contains("component(") && lowerCode.contains("container(")) {
            return C4DiagramType.COMPONENT;
        } else if (lowerCode.contains("container(")) {
            return C4DiagramType.CONTAINER;
        } else if (lowerCode.contains("system(") || lowerCode.contains("person(")) {
            return C4DiagramType.CONTEXT;
        }

        return C4DiagramType.CONTEXT; // Default
    }

    public String generateC4Template(C4DiagramType type, String systemName) throws IOException {
        String templatePath = "plantuml/c4/templates/" + type.toString().toLowerCase() + ".puml";
        ClassPathResource resource = new ClassPathResource(templatePath);
        String template;
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            template = FileCopyUtils.copyToString(reader);
        }

        String systemAlias = systemName.toLowerCase().replaceAll("\\s+", "_");
        return template.replace("{systemName}", systemName).replace("{systemAlias}", systemAlias);
    }

    public enum C4DiagramType {
        CONTEXT,
        CONTAINER,
        COMPONENT
    }
}
