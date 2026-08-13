package com.dio.voicebudget.repository;

import com.dio.voicebudget.domain.VoiceCommandLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Persiste e consulta o histórico de interações realizadas por voz. */
public interface VoiceCommandLogRepository extends JpaRepository<VoiceCommandLog, Long> {

    /** Busca os registros mais recentes primeiro e respeita o limite informado. */
    List<VoiceCommandLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
