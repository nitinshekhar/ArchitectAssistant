
package com.nitin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Conversation {
    private String message;
    private Sender sender;
    private DesignResponse design;

    public enum Sender {
        USER, ASSISTANT
    }
}
