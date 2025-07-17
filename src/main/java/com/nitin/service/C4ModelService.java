package com.nitin.service;

import org.springframework.stereotype.Service;

@Service
public class C4ModelService {

    public String validateAndEnhanceC4Syntax(String c4Code) {
        String enhanced = c4Code.trim();

        // Ensure C4 includes are present
        if (!enhanced.contains("!include") || !enhanced.contains("C4_")) {
            enhanced = addC4Includes() + "\n\n" + enhanced;
        }

        // Add @startuml and @enduml if missing
        if (!enhanced.startsWith("@startuml")) {
            enhanced = "@startuml\n" + enhanced;
        }

        if (!enhanced.endsWith("@enduml")) {
            enhanced = enhanced + "\n@enduml";
        }

        // Add basic C4 styling if not present
        if (!enhanced.contains("LAYOUT_") && !enhanced.contains("HIDE_STEREOTYPE")) {
            enhanced = enhanced.replace("@startuml", "@startuml\n" + getC4Styling());
        }

        return enhanced;
    }

    private String addC4Includes() {
        return """
                !include C4_Context.puml
                !include C4_Container.puml
                !include C4_Component.puml""";
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

    public String generateC4Template(C4DiagramType type, String systemName) {
        return switch (type) {
            case CONTEXT -> generateContextTemplate(systemName);
            case CONTAINER -> generateContainerTemplate(systemName);
            case COMPONENT -> generateComponentTemplate(systemName);
        };
    }

    private String generateContextTemplate(String systemName) {
        return String.format("""
                @startuml
                !include C4_Context.puml
                
                LAYOUT_WITH_LEGEND()
                HIDE_STEREOTYPE()
                
                title System Context diagram for %s
                
                Person(user, "User", "A user of the system")
                System(%s_system, "%s System", "The main system")
                System_Ext(external_system, "External System", "External dependency")
                
                Rel(user, %s_system, "Uses")
                Rel(%s_system, external_system, "Integrates with")
                
                @enduml
                """, systemName,
                systemName.toLowerCase().replaceAll("\\s+", "_"),
                systemName,
                systemName.toLowerCase().replaceAll("\\s+", "_"),
                systemName.toLowerCase().replaceAll("\\s+", "_"));
    }

    private String generateContainerTemplate(String systemName) {
        String systemAlias = systemName.toLowerCase().replaceAll("\\s+", "_");
        return String.format("""
                @startuml
                !include C4_Container.puml
                
                LAYOUT_WITH_LEGEND()
                HIDE_STEREOTYPE()
                
                title Container diagram for %s
                
                Person(user, "User", "A user of the system")
                
                System_Boundary(%s_boundary, "%s System") {
                    Container(web_app, "Web Application", "Technology", "Description")
                    Container(api, "API Application", "Technology", "Description")
                    ContainerDb(database, "Database", "Technology", "Description")
                }
                
                System_Ext(external_system, "External System", "External dependency")
                
                Rel(user, web_app, "Uses", "HTTPS")
                Rel(web_app, api, "Makes API calls to", "JSON/HTTPS")
                Rel(api, database, "Reads from and writes to", "JDBC")
                Rel(api, external_system, "Integrates with", "JSON/HTTPS")
                
                @enduml
                """, systemName, systemAlias, systemName);
    }

    private String generateComponentTemplate(String systemName) {
        String systemAlias = systemName.toLowerCase().replaceAll("\\s+", "_");
        return String.format("""
                @startuml
                !include C4_Component.puml
                
                LAYOUT_WITH_LEGEND()
                HIDE_STEREOTYPE()
                
                title Component diagram for %s - API Application
                
                Container(web_app, "Web Application", "Technology", "Description")
                ContainerDb(database, "Database", "Technology", "Description")
                System_Ext(external_system, "External System", "External dependency")
                
                Container_Boundary(%s_api, "API Application") {
                    Component(controller, "Controller", "Technology", "Description")
                    Component(service, "Service", "Technology", "Description")
                    Component(repository, "Repository", "Technology", "Description")
                }
                
                Rel(web_app, controller, "Makes API calls to", "JSON/HTTPS")
                Rel(controller, service, "Uses")
                Rel(service, repository, "Uses")
                Rel(repository, database, "Reads from and writes to", "JDBC")
                Rel(service, external_system, "Integrates with", "JSON/HTTPS")
                
                @enduml
                """, systemName, systemAlias);
    }

    public enum C4DiagramType {
        CONTEXT,
        CONTAINER,
        COMPONENT
    }
}