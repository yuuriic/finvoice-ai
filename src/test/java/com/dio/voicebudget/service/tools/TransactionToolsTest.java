package com.dio.voicebudget.service.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import com.dio.voicebudget.dto.BalanceResponse;
import com.dio.voicebudget.dto.TransactionRequest;
import com.dio.voicebudget.service.TransactionService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionToolsTest {

    @Mock
    private TransactionService transactionService;

    private TransactionTools tools;

    @BeforeEach
    void setUp() {
        tools = new TransactionTools(transactionService);
    }

    @Test
    void registrarTransacaoDelegatesToServiceAndReturnsSummary() throws Exception {
        Transaction saved = newTransaction(1L, "cafe", "12.00", TransactionType.EXPENSE, "alimentacao");
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(saved);

        String result = tools.registrarTransacao("cafe", new BigDecimal("12.00"), TransactionType.EXPENSE,
                "alimentacao", null);

        assertThat(result).contains("id=1").contains("cafe").contains("12.00");
    }

    @Test
    void consultarSaldoReturnsFormattedBalance() {
        when(transactionService.calculateBalance(null, null))
                .thenReturn(new BalanceResponse(new BigDecimal("500"), new BigDecimal("200"), new BigDecimal("300")));

        String result = tools.consultarSaldo(null, null);

        assertThat(result).contains("500").contains("200").contains("300");
    }

    @Test
    void listarTransacoesReturnsMessageWhenEmpty() {
        when(transactionService.findAll(null, null, null, null)).thenReturn(List.of());

        String result = tools.listarTransacoes(null, null);

        assertThat(result).contains("Nenhuma transacao");
    }

    private Transaction newTransaction(Long id, String description, String amount, TransactionType type,
            String category) throws Exception {
        Transaction transaction = new Transaction(description, new BigDecimal(amount), type, category,
                LocalDate.now());
        Field idField = Transaction.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(transaction, id);
        return transaction;
    }
}
