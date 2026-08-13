package com.dio.voicebudget.service.tools;

import com.dio.voicebudget.domain.Transaction;
import com.dio.voicebudget.domain.TransactionType;
import com.dio.voicebudget.dto.BalanceResponse;
import com.dio.voicebudget.dto.CategorySummaryResponse;
import com.dio.voicebudget.dto.TransactionRequest;
import com.dio.voicebudget.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Ferramentas expostas ao modelo de linguagem via Tool Calling. Cada metodo
 * anotado com @Tool vira uma funcao real que a IA pode decidir chamar para
 * atender a um comando de voz do usuario.
 */
@Component
public class TransactionTools {

    private final TransactionService transactionService;

    public TransactionTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Tool(name = "registrarTransacao",
            description = "Registra uma nova transacao financeira (receita ou despesa) no orcamento do usuario. "
                    + "Use quando o usuario disser algo como 'gastei X em Y' ou 'recebi X de Y'.")
    public String registrarTransacao(
            @ToolParam(description = "Descricao curta da transacao, ex: 'almoco no restaurante'") String descricao,
            @ToolParam(description = "Valor monetario da transacao, sempre positivo") BigDecimal valor,
            @ToolParam(description = "Tipo da transacao: INCOME para receita/entrada, EXPENSE para despesa/gasto")
                    TransactionType tipo,
            @ToolParam(description = "Categoria da transacao, ex: alimentacao, transporte, salario, lazer")
                    String categoria,
            @ToolParam(required = false,
                    description = "Data da transacao no formato ISO (yyyy-MM-dd). Se omitida, usa a data de hoje")
                    LocalDate data) {
        LocalDate transactionDate = data != null ? data : LocalDate.now();
        Transaction transaction = transactionService.create(
                new TransactionRequest(descricao, valor, tipo, categoria, transactionDate));
        return "Transacao registrada com sucesso: id=%d, descricao='%s', valor=%s, tipo=%s, categoria=%s, data=%s"
                .formatted(transaction.getId(), transaction.getDescription(), transaction.getAmount(),
                        transaction.getType(), transaction.getCategory(), transaction.getTransactionDate());
    }

    @Tool(name = "consultarSaldo",
            description = "Consulta o saldo atual (receitas menos despesas) do usuario, opcionalmente dentro de um "
                    + "periodo. Use quando o usuario perguntar 'qual meu saldo', 'quanto eu tenho' ou similar.")
    public String consultarSaldo(
            @ToolParam(required = false, description = "Data inicial do periodo (yyyy-MM-dd), opcional")
                    LocalDate dataInicial,
            @ToolParam(required = false, description = "Data final do periodo (yyyy-MM-dd), opcional")
                    LocalDate dataFinal) {
        BalanceResponse balance = transactionService.calculateBalance(dataInicial, dataFinal);
        return "Receitas totais: %s. Despesas totais: %s. Saldo: %s"
                .formatted(balance.totalIncome(), balance.totalExpense(), balance.balance());
    }

    @Tool(name = "listarTransacoes",
            description = "Lista as transacoes financeiras registradas, podendo filtrar por tipo e/ou categoria. "
                    + "Use quando o usuario pedir para ver, listar ou revisar seus gastos ou receitas.")
    public String listarTransacoes(
            @ToolParam(required = false, description = "Tipo para filtrar: INCOME ou EXPENSE, opcional")
                    TransactionType tipo,
            @ToolParam(required = false, description = "Categoria para filtrar, opcional") String categoria) {
        List<Transaction> transactions = transactionService.findAll(tipo, categoria, null, null);
        if (transactions.isEmpty()) {
            return "Nenhuma transacao encontrada para os filtros informados.";
        }
        return transactions.stream()
                .map(t -> "#%d %s | %s | %s | %s | %s".formatted(t.getId(), t.getTransactionDate(), t.getType(),
                        t.getCategory(), t.getDescription(), t.getAmount()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(name = "resumoPorCategoria",
            description = "Retorna um resumo de receitas e despesas agrupado por categoria. Use quando o usuario "
                    + "perguntar 'onde estou gastando mais' ou pedir um resumo por categoria.")
    public String resumoPorCategoria() {
        List<CategorySummaryResponse> summary = transactionService.summarizeByCategory();
        if (summary.isEmpty()) {
            return "Nenhuma transacao registrada ainda.";
        }
        return summary.stream()
                .map(s -> "%s: receitas=%s, despesas=%s".formatted(s.category(), s.totalIncome(), s.totalExpense()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(name = "excluirTransacao",
            description = "Exclui uma transacao pelo seu identificador numerico. Use apenas quando o usuario "
                    + "informar claramente o id da transacao a ser removida.")
    public String excluirTransacao(@ToolParam(description = "Id numerico da transacao a excluir") Long id) {
        transactionService.delete(id);
        return "Transacao %d excluida com sucesso.".formatted(id);
    }
}
