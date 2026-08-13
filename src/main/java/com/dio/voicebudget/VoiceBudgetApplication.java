package com.dio.voicebudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * Ponto de entrada da aplicação. A anotação habilita a configuração automática,
 * a varredura dos componentes e o servidor web embutido do Spring Boot.
 */
public class VoiceBudgetApplication {

    public static void main(String[] args) {
        // Cria o contexto Spring e inicia o servidor HTTP (porta 8080 por padrão).
        SpringApplication.run(VoiceBudgetApplication.class, args);
    }
}
