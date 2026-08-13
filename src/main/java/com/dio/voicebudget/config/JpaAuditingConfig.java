package com.dio.voicebudget.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
/** Habilita o preenchimento automático dos campos @CreatedDate e @LastModifiedDate. */
public class JpaAuditingConfig {
}
