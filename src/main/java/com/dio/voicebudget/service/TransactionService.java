package com.dio.voicebudget.service;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import com.dio.voicebudget.dto.BalanceResponse;
import com.dio.voicebudget.dto.CategorySummaryResponse;
import com.dio.voicebudget.dto.TransactionRequest;
import com.dio.voicebudget.exception.TransactionNotFoundException;
import com.dio.voicebudget.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** Centraliza as regras de negócio usadas tanto pela API quanto pelas ferramentas da IA. */
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    /** Valida, converte o DTO em entidade e persiste uma nova movimentação. */
    public Transaction create(TransactionRequest request) {
        validate(request);
        Transaction transaction = new Transaction(
                request.description(),
                request.amount(),
                request.type(),
                request.category(),
                request.transactionDate());
        return repository.save(transaction);
    }

    /** Retorna a transação ou lança uma exceção de domínio quando o ID não existe. */
    public Transaction findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    /**
     * Aplica a combinação de filtros disponível e ordena o resultado da data
     * mais recente para a mais antiga.
     */
    public List<Transaction> findAll(TransactionType type, String category, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions;
        if (startDate != null && endDate != null && type != null) {
            transactions = repository.findByTypeAndTransactionDateBetween(type, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            transactions = repository.findByTransactionDateBetween(startDate, endDate);
        } else if (type != null) {
            transactions = repository.findByType(type);
        } else if (category != null && !category.isBlank()) {
            transactions = repository.findByCategoryIgnoreCase(category);
        } else {
            transactions = repository.findAll();
        }

        // Complementa em memória os casos nos quais categoria vem combinada a outro filtro.
        if (category != null && !category.isBlank()) {
            transactions = transactions.stream()
                    .filter(t -> t.getCategory().equalsIgnoreCase(category))
                    .toList();
        }
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .toList();
    }

    @Transactional
    /** Atualiza a entidade gerenciada; o JPA grava as mudanças ao concluir a transação. */
    public Transaction update(Long id, TransactionRequest request) {
        validate(request);
        Transaction transaction = findById(id);
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setTransactionDate(request.transactionDate());
        return transaction;
    }

    @Transactional
    /** Exige que o registro exista antes de solicitar sua exclusão. */
    public void delete(Long id) {
        Transaction transaction = findById(id);
        repository.delete(transaction);
    }

    /** Soma receitas e despesas e calcula o saldo, opcionalmente por período. */
    public BalanceResponse calculateBalance(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = (startDate != null && endDate != null)
                ? repository.findByTransactionDateBetween(startDate, endDate)
                : repository.findAll();

        BigDecimal income = sumByType(transactions, TransactionType.INCOME);
        BigDecimal expense = sumByType(transactions, TransactionType.EXPENSE);
        return new BalanceResponse(income, expense, income.subtract(expense));
    }

    /** Agrupa em memória os totais de entrada e saída de cada categoria. */
    public List<CategorySummaryResponse> summarizeByCategory() {
        Map<String, BigDecimal[]> totals = new LinkedHashMap<>();
        for (Transaction transaction : repository.findAll()) {
            BigDecimal[] pair = totals.computeIfAbsent(transaction.getCategory(),
                    key -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            if (transaction.getType() == TransactionType.INCOME) {
                pair[0] = pair[0].add(transaction.getAmount());
            } else {
                pair[1] = pair[1].add(transaction.getAmount());
            }
        }
        return totals.entrySet().stream()
                .map(entry -> new CategorySummaryResponse(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    /** Soma somente os valores pertencentes ao tipo solicitado. */
    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Reforça regras essenciais mesmo quando o serviço é chamado fora do controller. */
    private void validate(TransactionRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transacao deve ser maior que zero");
        }
        if (request.transactionDate() != null && request.transactionDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data da transacao nao pode ser no futuro");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("A descricao da transacao e obrigatoria");
        }
    }
}
