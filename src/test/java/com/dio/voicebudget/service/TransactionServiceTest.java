package com.dio.voicebudget.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import com.dio.voicebudget.dto.BalanceResponse;
import com.dio.voicebudget.dto.TransactionRequest;
import com.dio.voicebudget.exception.TransactionNotFoundException;
import com.dio.voicebudget.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(repository);
    }

    @Test
    void createsTransactionWhenRequestIsValid() {
        TransactionRequest request = new TransactionRequest("Almoco", new BigDecimal("35.50"),
                TransactionType.EXPENSE, "alimentacao", LocalDate.now());
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created = service.create(request);

        assertThat(created.getDescription()).isEqualTo("Almoco");
        assertThat(created.getAmount()).isEqualByComparingTo("35.50");
        verify(repository).save(any(Transaction.class));
    }

    @Test
    void rejectsTransactionWithNonPositiveAmount() {
        TransactionRequest request = new TransactionRequest("Almoco", BigDecimal.ZERO, TransactionType.EXPENSE,
                "alimentacao", LocalDate.now());

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTransactionWithFutureDate() {
        TransactionRequest request = new TransactionRequest("Salario", new BigDecimal("100"),
                TransactionType.INCOME, "trabalho", LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenTransactionNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void calculatesBalanceFromIncomeAndExpense() {
        Transaction income = new Transaction("Salario", new BigDecimal("1000"), TransactionType.INCOME, "trabalho",
                LocalDate.now());
        Transaction expense = new Transaction("Aluguel", new BigDecimal("400"), TransactionType.EXPENSE, "moradia",
                LocalDate.now());
        when(repository.findAll()).thenReturn(List.of(income, expense));

        BalanceResponse balance = service.calculateBalance(null, null);

        assertThat(balance.totalIncome()).isEqualByComparingTo("1000");
        assertThat(balance.totalExpense()).isEqualByComparingTo("400");
        assertThat(balance.balance()).isEqualByComparingTo("600");
    }
}
