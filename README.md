# 💰 Finvoice AI

> API de controle financeiro com assistente de voz, construída com Spring Boot, Spring AI e Groq.

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot 3.5.16" src="https://img.shields.io/badge/Spring_Boot-3.5.16-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="Spring AI 1.0.7" src="https://img.shields.io/badge/Spring_AI-1.0.7-6DB33F?style=for-the-badge">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
</p>

O **Finvoice AI** recebe comandos de voz, transcreve o áudio, interpreta a intenção do usuário e executa operações reais sobre suas transações financeiras. Também oferece uma API REST completa para cadastrar, consultar, atualizar e excluir receitas e despesas.

Projeto desenvolvido para o desafio da trilha [Spring Boot Learning Track — Spring AI](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/blob/main/05-spring-ai/README.md), da DIO.

## ✨ Principais recursos

- CRUD completo de receitas e despesas;
- filtros por tipo, categoria e período;
- cálculo de saldo e resumo por categoria;
- comandos financeiros por áudio;
- Tool Calling para registrar, consultar, listar e excluir transações;
- resposta do assistente em texto ou áudio MP3;
- histórico e auditoria dos comandos de voz;
- validações de negócio e tratamento global de erros;
- persistência local com H2;
- testes automatizados com JUnit, Mockito, MockMvc e AssertJ.

## 🔄 Como funciona

```text
Áudio do usuário
      ↓
Groq Whisper (transcrição)
      ↓
Spring AI + Llama (interpretação)
      ↓
Tool Calling → TransactionService → H2
      ↓
Resposta em texto ou áudio + registro de auditoria
```

Os modelos utilizados estão configurados em `application.yml`:

| Função | Modelo |
|---|---|
| Chat e Tool Calling | `llama-3.3-70b-versatile` |
| Transcrição | `whisper-large-v3-turbo` |
| Texto para fala | `canopylabs/orpheus-v1-english` |

## 🛠️ Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Aplicação web | Spring Boot 3.5.16 |
| Inteligência artificial | Spring AI 1.0.7 |
| Provedor de IA | Groq, via API compatível com OpenAI |
| Persistência | Spring Data JPA + H2 Database |
| Validação | Jakarta Bean Validation |
| Build | Maven 3.9+ |
| Testes | JUnit 5, Mockito, MockMvc e AssertJ |

## 🚀 Como executar

### Pré-requisitos

- **JDK 21 ou superior**;
- **Maven 3.9 ou superior**;
- **Git** para clonar o projeto;
- uma **Groq API Key** para utilizar os endpoints de inteligência artificial.

> A chave da Groq não é necessária para usar o CRUD financeiro. Sem ela, apenas os endpoints de voz ficam indisponíveis.

Confira a instalação:

```bash
java -version
mvn -version
git --version
```

### 1. Clone o repositório

```bash
git clone https://github.com/yuuriic/finvoice-ai.git
cd finvoice-ai
```

### 2. Configure a chave da Groq

Crie gratuitamente uma chave em [console.groq.com/keys](https://console.groq.com/keys) e exponha-a como `GROQ_API_KEY`.

Linux ou macOS:

```bash
export GROQ_API_KEY="sua-chave-aqui"
```

Windows PowerShell:

```powershell
$env:GROQ_API_KEY="sua-chave-aqui"
```

Windows Prompt de Comando:

```bat
set GROQ_API_KEY=sua-chave-aqui
```

O arquivo `.env.example` serve como referência, mas o Spring Boot **não carrega `.env` automaticamente**. Se optar por um arquivo `.env`, carregue a variável no terminal antes de iniciar a aplicação ou configure sua IDE para lê-lo.

### 3. Execute os testes

```bash
mvn clean test
```

### 4. Inicie a aplicação

```bash
mvn spring-boot:run
```

A API estará disponível em [http://localhost:8080](http://localhost:8080).

### Executar pelo JAR

```bash
mvn clean package
java -jar target/voice-budget-ai-0.1.0.jar
```

Para gerar o pacote sem repetir os testes:

```bash
mvn clean package -DskipTests
```

## 🗄️ Banco de dados H2

O banco é criado automaticamente e persiste os dados em `./data/voicebudget`.

| Configuração | Valor |
|---|---|
| Console | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) |
| JDBC URL | `jdbc:h2:file:./data/voicebudget` |
| Usuário | `sa` |
| Senha | deixar em branco |

No console H2, use a JDBC URL sem o parâmetro interno `AUTO_SERVER=TRUE`, conforme a tabela acima.

## ✅ Teste rápido

Com a aplicação em execução, crie uma receita:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Salário",
    "amount": 3000.00,
    "type": "INCOME",
    "category": "trabalho",
    "transactionDate": "2025-08-01"
  }'
```

Crie uma despesa:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Almoço",
    "amount": 50.00,
    "type": "EXPENSE",
    "category": "alimentação",
    "transactionDate": "2025-08-02"
  }'
```

Consulte as transações e o saldo:

```bash
curl http://localhost:8080/api/transactions
curl http://localhost:8080/api/transactions/balance
```

Resposta de saldo esperada:

```json
{
  "totalIncome": 3000.00,
  "totalExpense": 50.00,
  "balance": 2950.00
}
```

> No PowerShell, `curl` pode ser um alias de `Invoke-WebRequest`. Use `curl.exe` nos exemplos acima caso o comando não se comporte como esperado.

## 📡 Endpoints

### Transações

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/transactions` | Cria uma transação |
| `GET` | `/api/transactions` | Lista e filtra transações |
| `GET` | `/api/transactions/{id}` | Busca uma transação pelo ID |
| `PUT` | `/api/transactions/{id}` | Atualiza uma transação |
| `DELETE` | `/api/transactions/{id}` | Exclui uma transação |
| `GET` | `/api/transactions/balance` | Calcula receitas, despesas e saldo |
| `GET` | `/api/transactions/summary/by-category` | Agrupa receitas e despesas por categoria |

Filtros opcionais de `GET /api/transactions`:

| Parâmetro | Formato | Exemplo |
|---|---|---|
| `type` | `INCOME` ou `EXPENSE` | `?type=EXPENSE` |
| `category` | texto | `?category=alimentação` |
| `startDate` | `yyyy-MM-dd` | `?startDate=2025-08-01` |
| `endDate` | `yyyy-MM-dd` | `?endDate=2025-08-31` |

Os filtros podem ser combinados:

```bash
curl "http://localhost:8080/api/transactions?type=EXPENSE&startDate=2025-08-01&endDate=2025-08-31"
curl "http://localhost:8080/api/transactions/balance?startDate=2025-08-01&endDate=2025-08-31"
```

Corpo de `POST` e `PUT`:

```json
{
  "description": "Almoço",
  "amount": 50.00,
  "type": "EXPENSE",
  "category": "alimentação",
  "transactionDate": "2025-08-02"
}
```

Regras de validação:

- todos os campos são obrigatórios;
- `amount` deve ser igual ou superior a `0.01`;
- `type` aceita somente `INCOME` ou `EXPENSE`;
- `transactionDate` deve estar no formato ISO e não pode ser futura.

### Assistente de voz

| Método | Endpoint | Retorno |
|---|---|---|
| `POST` | `/api/assistant/voice-commands` | Transcrição e resposta em JSON |
| `POST` | `/api/assistant/voice-commands/speech` | Resposta sintetizada em `audio/mpeg` |
| `GET` | `/api/assistant/voice-commands?limit=20` | Histórico dos comandos mais recentes |

Os dois endpoints `POST` recebem `multipart/form-data`, no campo obrigatório `audio`. O limite configurado para o upload é **25 MB**.

Resposta em texto:

```bash
curl -X POST http://localhost:8080/api/assistant/voice-commands \
  -F "audio=@/caminho/para/comando.mp3"
```

```json
{
  "transcribedText": "gastei cinquenta reais com almoço",
  "assistantReply": "Registrei uma despesa de R$ 50,00 em alimentação."
}
```

Resposta em áudio:

```bash
curl -X POST http://localhost:8080/api/assistant/voice-commands/speech \
  -F "audio=@/caminho/para/comando.mp3" \
  --output resposta.mp3
```

Histórico de auditoria:

```bash
curl "http://localhost:8080/api/assistant/voice-commands?limit=10"
```

O assistente pode interpretar pedidos como:

- “Gastei cinquenta reais com almoço hoje.”
- “Recebi três mil reais de salário.”
- “Qual é o meu saldo?”
- “Liste minhas despesas com transporte.”
- “Mostre um resumo por categoria.”
- “Exclua a transação de ID 4.”

## 🧪 Testes automatizados

```bash
# Suíte completa
mvn clean test

# Uma classe específica
mvn test -Dtest=TransactionServiceTest

# Relatórios gerados pelo Maven Surefire
# target/surefire-reports/
```

A suíte cobre regras de negócio, cálculo de saldo, Tool Calling, endpoints REST e carregamento do contexto Spring.

## 📂 Estrutura do projeto

```text
src/
├── main/
│   ├── java/com/dio/voicebudget/
│   │   ├── config/       # ChatClient e auditoria JPA
│   │   ├── controller/   # Endpoints REST
│   │   ├── domain/       # Entidades e enums
│   │   ├── dto/          # Objetos de entrada e saída
│   │   ├── exception/    # Exceções e tratamento global
│   │   ├── repository/   # Repositórios Spring Data JPA
│   │   └── service/      # Regras de negócio e ferramentas da IA
│   └── resources/
│       └── application.yml
└── test/                 # Testes automatizados
```

## ⚙️ Configuração

As configurações principais ficam em `src/main/resources/application.yml`.

| Propriedade | Padrão | Finalidade |
|---|---|---|
| `GROQ_API_KEY` | vazio | Autenticação dos recursos de IA |
| `server.port` | `8080` | Porta HTTP |
| `spring.servlet.multipart.max-file-size` | `25MB` | Tamanho máximo do arquivo de áudio |
| `spring.jpa.hibernate.ddl-auto` | `update` | Atualização automática do schema local |

Para usar outra porta sem alterar o arquivo:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

## 🐛 Solução de problemas

| Problema | Como resolver |
|---|---|
| `java` ou `mvn` não reconhecido | Instale o JDK/Maven e adicione-os ao `PATH` |
| Porta `8080` em uso | Encerre o processo ou inicie com outra porta |
| Falha `401`/autenticação da IA | Confirme se `GROQ_API_KEY` está definida no mesmo terminal |
| Erro ao enviar áudio | Confirme o campo `audio`, o caminho e o limite de 25 MB |
| Data rejeitada | Use `yyyy-MM-dd` e uma data atual ou passada |
| Banco bloqueado | Feche outras instâncias da aplicação e do console H2 |
| CRUD funciona, mas voz falha | Verifique chave, conexão e disponibilidade dos modelos na Groq |

## 📚 Aprendizados do projeto

- configuração de múltiplos modelos com um único starter do Spring AI;
- integração com API compatível com OpenAI por meio de `base-url` customizada;
- uso de `@Tool` e `@ToolParam` para executar operações reais a partir da linguagem natural;
- separação da IA das regras de negócio para manter o código testável;
- auditoria persistente de comandos processados;
- isolamento de `@EnableJpaAuditing` para preservar testes de fatia com `@WebMvcTest`.

---

<p align="center">
  Desenvolvido como parte da formação Spring Boot + Spring AI da <strong>DIO</strong>.
</p>
