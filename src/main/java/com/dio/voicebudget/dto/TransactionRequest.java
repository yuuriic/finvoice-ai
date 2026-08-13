package com.dio.voicebudget.dto;

import com.dio.voicebudget.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(

        @NotBlank(message = "A descricao e obrigatoria") String description,

        @NotNull(message = "O valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero") BigDecimal amount,

        @NotNull(message = "O tipo e obrigatorio (INCOME ou EXPENSE)") TransactionType type,

        @NotBlank(message = "A categoria e obrigatoria") String category,

        @NotNull(message = "A data e obrigatoria")
        @PastOrPresent(message = "A data nao pode ser no futuro") LocalDate transactionDate) {
}
