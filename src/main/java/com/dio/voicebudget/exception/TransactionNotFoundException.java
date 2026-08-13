package com.dio.voicebudget.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long id) {
        super("Transacao nao encontrada com id: " + id);
    }
}
