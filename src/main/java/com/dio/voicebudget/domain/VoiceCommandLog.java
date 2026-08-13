package com.dio.voicebudget.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Registro de auditoria de cada comando de voz processado pelo assistente,
 * permitindo consultar depois o que foi transcrito, o que a IA respondeu e se
 * a execucao teve sucesso.
 */
@Entity
@Table(name = "voice_command_logs")
@EntityListeners(AuditingEntityListener.class)
public class VoiceCommandLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "transcribed_text", nullable = false)
    private String transcribedText;

    @Lob
    @Column(name = "assistant_reply")
    private String assistantReply;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected VoiceCommandLog() {
    }

    public VoiceCommandLog(String transcribedText, String assistantReply, boolean success, String errorMessage) {
        this.transcribedText = transcribedText;
        this.assistantReply = assistantReply;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getTranscribedText() {
        return transcribedText;
    }

    public String getAssistantReply() {
        return assistantReply;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
