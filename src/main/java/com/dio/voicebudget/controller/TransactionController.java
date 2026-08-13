package com.dio.voicebudget.controller;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import com.dio.voicebudget.dto.BalanceResponse;
import com.dio.voicebudget.dto.CategorySummaryResponse;
import com.dio.voicebudget.dto.TransactionRequest;
import com.dio.voicebudget.dto.TransactionResponse;
import com.dio.voicebudget.service.TransactionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
/** Expõe os endpoints HTTP do cadastro e dos relatórios financeiros. */
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    /** Cria uma transação e responde 201 com a localização do novo recurso. */
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.create(request);
        TransactionResponse response = TransactionResponse.from(transaction);
        return ResponseEntity.created(URI.create("/api/transactions/" + transaction.getId())).body(response);
    }

    @GetMapping
    /** Lista transações e aceita filtros opcionais por query string. */
    public List<TransactionResponse> list(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.findAll(type, category, startDate, endDate).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    /** Consulta uma única movimentação pelo identificador. */
    public TransactionResponse getById(@PathVariable Long id) {
        return TransactionResponse.from(transactionService.findById(id));
    }

    @PutMapping("/{id}")
    /** Substitui os dados editáveis de uma transação existente. */
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return TransactionResponse.from(transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    /** Exclui o recurso e responde 204, sem corpo. */
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/balance")
    /** Calcula totais e saldo geral ou de um intervalo de datas. */
    public BalanceResponse balance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.calculateBalance(startDate, endDate);
    }

    @GetMapping("/summary/by-category")
    /** Agrupa receitas e despesas pelas categorias cadastradas. */
    public List<CategorySummaryResponse> summaryByCategory() {
        return transactionService.summarizeByCategory();
    }
}
