package com.dio.voicebudget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import com.dio.voicebudget.dto.BalanceResponse;
import com.dio.voicebudget.dto.TransactionRequest;
import com.dio.voicebudget.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
/** Teste de fatia web: valida rotas, status e JSON sem iniciar banco ou IA. */
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    // Substitui a regra real para que o teste avalie apenas o comportamento HTTP.
    private TransactionService transactionService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void createReturnsCreatedTransaction() throws Exception {
        Transaction saved = newTransaction(1L);
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(saved);

        TransactionRequest request = new TransactionRequest("Almoco", new BigDecimal("35.50"),
                TransactionType.EXPENSE, "alimentacao", LocalDate.now());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Almoco"));
    }

    @Test
    void createRejectsInvalidAmount() throws Exception {
        TransactionRequest request = new TransactionRequest("Almoco", BigDecimal.ZERO, TransactionType.EXPENSE,
                "alimentacao", LocalDate.now());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void balanceReturnsIncomeExpenseAndBalance() throws Exception {
        when(transactionService.calculateBalance(null, null))
                .thenReturn(new BalanceResponse(new BigDecimal("500"), new BigDecimal("200"), new BigDecimal("300")));

        mockMvc.perform(get("/api/transactions/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300));
    }

    private Transaction newTransaction(Long id) throws Exception {
        Transaction transaction = new Transaction("Almoco", new BigDecimal("35.50"), TransactionType.EXPENSE,
                "alimentacao", LocalDate.now());
        Field idField = Transaction.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(transaction, id);
        return transaction;
    }
}
