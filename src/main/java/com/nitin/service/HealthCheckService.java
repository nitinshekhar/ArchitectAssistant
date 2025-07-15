
package com.nitin.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    public boolean isLlamaRunning() {
        try {
            // Send a simple test query to the Llama model
            chatLanguageModel.generate("Are you running?");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
