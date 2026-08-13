package com.dio.voicebudget.controller;

import com.dio.voicebudget.dto.VoiceCommandHistoryResponse;
import com.dio.voicebudget.dto.VoiceCommandResponse;
import com.dio.voicebudget.repository.VoiceCommandLogRepository;
import com.dio.voicebudget.service.VoiceAssistantService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assistant")
/** Recebe áudios do frontend e expõe o histórico do assistente financeiro. */
public class VoiceAssistantController {

    private final VoiceAssistantService voiceAssistantService;
    private final VoiceCommandLogRepository voiceCommandLogRepository;

    public VoiceAssistantController(VoiceAssistantService voiceAssistantService,
            VoiceCommandLogRepository voiceCommandLogRepository) {
        this.voiceAssistantService = voiceAssistantService;
        this.voiceCommandLogRepository = voiceCommandLogRepository;
    }

    @PostMapping(value = "/voice-commands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    /** Processa o campo multipart "audio" e devolve transcrição e resposta em JSON. */
    public VoiceCommandResponse handleVoiceCommand(@RequestParam("audio") MultipartFile audio) {
        return voiceAssistantService.processVoiceCommand(audio);
    }

    @PostMapping(value = "/voice-commands/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mpeg")
    /** Executa o comando e converte a resposta textual do assistente em MP3. */
    public byte[] handleVoiceCommandWithSpeech(@RequestParam("audio") MultipartFile audio) {
        VoiceCommandResponse response = voiceAssistantService.processVoiceCommand(audio);
        return voiceAssistantService.synthesizeSpeech(response.assistantReply());
    }

    @GetMapping("/voice-commands")
    /** Lista os comandos mais recentes; o limite padrão evita respostas muito grandes. */
    public List<VoiceCommandHistoryResponse> history(@RequestParam(defaultValue = "20") int limit) {
        return voiceCommandLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(VoiceCommandHistoryResponse::from)
                .toList();
    }
}
