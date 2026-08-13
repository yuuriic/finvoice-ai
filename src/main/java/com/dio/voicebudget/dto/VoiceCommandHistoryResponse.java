package com.dio.voicebudget.dto;

import com.dio.voicebudget.domain.VoiceCommandLog;
import java.time.LocalDateTime;

public record VoiceCommandHistoryResponse(
        Long id,
        String transcribedText,
        String assistantReply,
        boolean success,
        String errorMessage,
        LocalDateTime createdAt) {

    public static VoiceCommandHistoryResponse from(VoiceCommandLog log) {
        return new VoiceCommandHistoryResponse(
                log.getId(),
                log.getTranscribedText(),
                log.getAssistantReply(),
                log.isSuccess(),
                log.getErrorMessage(),
                log.getCreatedAt());
    }
}
