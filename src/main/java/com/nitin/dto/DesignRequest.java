package com.nitin.dto;

import java.util.List;

public class DesignRequest {
    private String request;
    private List<Conversation> conversationHistory;

    public String getRequest() { return request; }
    public void setRequest(String request) { this.request = request; }

    public List<Conversation> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<Conversation> conversationHistory) { this.conversationHistory = conversationHistory; }
}
