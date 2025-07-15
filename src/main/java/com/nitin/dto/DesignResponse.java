package com.nitin.dto;

public class DesignResponse {
    private String userRequest;
    private String explanation;
    private String plantUmlCode;
    private String diagramPath;
    private String diagramFilename;
    private boolean success;
    private String errorMessage;
    private Conversation conversation;
    private boolean clarificationNeeded;

    // Private constructor to force use of the builder
    private DesignResponse() {}

    // Builder pattern
    public static DesignResponseBuilder builder() {
        return new DesignResponseBuilder();
    }

    // Getters
    public String getUserRequest() { return userRequest; }
    public String getExplanation() { return explanation; }
    public String getPlantUmlCode() { return plantUmlCode; }
    public String getDiagramPath() { return diagramPath; }
    public String getDiagramFilename() { return diagramFilename; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public Conversation getConversation() { return conversation; }
    public boolean isClarificationNeeded() { return clarificationNeeded; }

    public static class DesignResponseBuilder {
        private DesignResponse response = new DesignResponse();

        public DesignResponseBuilder userRequest(String userRequest) {
            response.userRequest = userRequest;
            return this;
        }

        public DesignResponseBuilder explanation(String explanation) {
            response.explanation = explanation;
            return this;
        }

        public DesignResponseBuilder plantUmlCode(String plantUmlCode) {
            response.plantUmlCode = plantUmlCode;
            return this;
        }

        public DesignResponseBuilder diagramPath(String diagramPath) {
            response.diagramPath = diagramPath;
            return this;
        }

        public DesignResponseBuilder diagramFilename(String diagramFilename) {
            response.diagramFilename = diagramFilename;
            return this;
        }

        public DesignResponseBuilder success(boolean success) {
            response.success = success;
            return this;
        }

        public DesignResponseBuilder errorMessage(String errorMessage) {
            response.errorMessage = errorMessage;
            return this;
        }

        public DesignResponseBuilder conversation(Conversation conversation) {
            response.conversation = conversation;
            return this;
        }

        public DesignResponseBuilder clarificationNeeded(boolean clarificationNeeded) {
            response.clarificationNeeded = clarificationNeeded;
            return this;
        }

        public DesignResponse build() {
            return response;
        }
    }
}
