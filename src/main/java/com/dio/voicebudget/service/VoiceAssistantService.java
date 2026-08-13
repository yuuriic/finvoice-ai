package com.dio.voicebudget.service;

import com.dio.voicebudget.domain.VoiceCommandLog;
import com.dio.voicebudget.dto.VoiceCommandResponse;
import com.dio.voicebudget.exception.VoiceCommandProcessingException;
import com.dio.voicebudget.repository.VoiceCommandLogRepository;
import com.dio.voicebudget.service.tools.TransactionTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orquestra o fluxo principal do assistente: audio -> texto (Whisper) ->
 * intencao + execucao (ChatClient com Tool Calling) -> texto de resposta, com
 * auditoria de cada comando processado.
 */
@Service
public class VoiceAssistantService {

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final ChatClient chatClient;
    private final TransactionTools transactionTools;
    private final VoiceCommandLogRepository voiceCommandLogRepository;

    public VoiceAssistantService(OpenAiAudioTranscriptionModel transcriptionModel,
            OpenAiAudioSpeechModel speechModel, ChatClient chatClient, TransactionTools transactionTools,
            VoiceCommandLogRepository voiceCommandLogRepository) {
        this.transcriptionModel = transcriptionModel;
        this.speechModel = speechModel;
        this.chatClient = chatClient;
        this.transactionTools = transactionTools;
        this.voiceCommandLogRepository = voiceCommandLogRepository;
    }

    public VoiceCommandResponse processVoiceCommand(MultipartFile audioFile) {
        // A transcrição acontece antes da chamada ao LLM para transformar fala em texto.
        String transcribedText = transcribe(audioFile);
        try {
            // As ferramentas permitem ao modelo consultar ou alterar dados reais com segurança.
            String assistantReply = chatClient.prompt()
                    .user(transcribedText)
                    .tools(transactionTools)
                    .call()
                    .content();
            // Toda tentativa bem-sucedida fica disponível no histórico da interface.
            voiceCommandLogRepository.save(new VoiceCommandLog(transcribedText, assistantReply, true, null));
            return new VoiceCommandResponse(transcribedText, assistantReply);
        } catch (RuntimeException ex) {
            // Falhas também são auditadas antes de serem convertidas em erro HTTP.
            voiceCommandLogRepository.save(new VoiceCommandLog(transcribedText, null, false, ex.getMessage()));
            throw new VoiceCommandProcessingException("Falha ao interpretar o comando de voz", ex);
        }
    }

    public byte[] synthesizeSpeech(String text) {
        // O modelo de fala devolve os bytes que o controller publica como audio/mpeg.
        return speechModel.call(text);
    }

    private String transcribe(MultipartFile audioFile) {
        // Impede chamadas desnecessárias ao provedor quando nenhum áudio foi recebido.
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de audio nao pode estar vazio");
        }
        try {
            return transcriptionModel.call(audioFile.getResource());
        } catch (RuntimeException ex) {
            throw new VoiceCommandProcessingException("Falha ao transcrever o audio", ex);
        }
    }
}
