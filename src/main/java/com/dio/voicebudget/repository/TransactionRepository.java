package com.dio.voicebudget.repository;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Camada de acesso às transações. O Spring Data gera as consultas abaixo a
 * partir do nome de cada método, sem necessidade de SQL manual.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Filtra receitas ou despesas. */
    List<Transaction> findByType(TransactionType type);

    /** Localiza uma categoria sem diferenciar letras maiúsculas e minúsculas. */
    List<Transaction> findByCategoryIgnoreCase(String category);

    /** Retorna movimentações dentro de um período inclusivo. */
    List<Transaction> findByTransactionDateBetween(LocalDate start, LocalDate end);

    /** Combina filtro por tipo e período. */
    List<Transaction> findByTypeAndTransactionDateBetween(TransactionType type, LocalDate start, LocalDate end);
}
