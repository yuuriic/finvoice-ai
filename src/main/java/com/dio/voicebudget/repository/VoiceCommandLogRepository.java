package com.dio.voicebudget.repository;

import com.dio.voicebudget.domain.VoiceCommandLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoiceCommandLogRepository extends JpaRepository<VoiceCommandLog, Long> {

    List<VoiceCommandLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
