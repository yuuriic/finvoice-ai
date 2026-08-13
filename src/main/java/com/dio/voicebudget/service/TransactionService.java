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
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
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

    public Transaction findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

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
    public void delete(Long id) {
        Transaction transaction = findById(id);
        repository.delete(transaction);
    }

    public BalanceResponse calculateBalance(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = (startDate != null && endDate != null)
                ? repository.findByTransactionDateBetween(startDate, endDate)
                : repository.findAll();

        BigDecimal income = sumByType(transactions, TransactionType.INCOME);
        BigDecimal expense = sumByType(transactions, TransactionType.EXPENSE);
        return new BalanceResponse(income, expense, income.subtract(expense));
    }

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

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

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
