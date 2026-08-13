package com.dio.voicebudget.dto;

import java.math.BigDecimal;

public record CategorySummaryResponse(String category, BigDecimal totalIncome, BigDecimal totalExpense) {
}
