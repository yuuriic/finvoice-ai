const api = {
  transactions: "/api/transactions",
  balance: "/api/transactions/balance",
  categories: "/api/transactions/summary/by-category",
  voice: "/api/assistant/voice-commands",
  history: "/api/assistant/voice-commands"
};

const $ = (selector) => document.querySelector(selector);
const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormat = new Intl.DateTimeFormat("pt-BR");
let mediaRecorder;
let mediaStream;
let audioChunks = [];
let recordedBlob;
let timerInterval;
let seconds = 0;

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  $("#transactionDate").max = today();
  loadDashboard();
});

function bindEvents() {
  $("#refreshButton").addEventListener("click", loadDashboard);
  $("#newTransactionButton").addEventListener("click", () => openTransactionDialog());
  $("#closeDialogButton").addEventListener("click", closeTransactionDialog);
  $("#cancelDialogButton").addEventListener("click", closeTransactionDialog);
  $("#transactionForm").addEventListener("submit", saveTransaction);
  $("#recordButton").addEventListener("click", toggleRecording);
  $("#discardButton").addEventListener("click", resetRecording);
  $("#sendRecordingButton").addEventListener("click", () => sendAudio(recordedBlob, recordingFilename()));
  $("#audioFile").addEventListener("change", handleFileSelection);
  $("#transactionsBody").addEventListener("click", handleTransactionAction);
}

async function request(url, options = {}) {
  const response = await fetch(url, options);
  if (!response.ok) {
    let message = `Erro ${response.status}`;
    try {
      const body = await response.json();
      message = body.message || body.detail || body.error || message;
    } catch (_) { /* resposta sem JSON */ }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

async function loadDashboard() {
  $("#refreshButton").disabled = true;
  try {
    const [balance, transactions, categories, history] = await Promise.all([
      request(api.balance), request(api.transactions), request(api.categories), request(`${api.history}?limit=10`)
    ]);
    renderBalance(balance);
    renderTransactions(transactions);
    renderCategories(categories);
    renderHistory(history);
  } catch (error) {
    toast(`Não foi possível carregar os dados: ${error.message}`, true);
  } finally {
    $("#refreshButton").disabled = false;
  }
}

function renderBalance(data) {
  $("#balanceValue").textContent = money.format(data.balance || 0);
  $("#incomeValue").textContent = money.format(data.totalIncome || 0);
  $("#expenseValue").textContent = money.format(data.totalExpense || 0);
}

function renderTransactions(items) {
  const body = $("#transactionsBody");
  body.innerHTML = "";
  $("#transactionsEmpty").hidden = items.length > 0;
  items.sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)).forEach((item) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(item.description)}</td>
      <td>${escapeHtml(item.category)}</td>
      <td>${dateFormat.format(new Date(`${item.transactionDate}T12:00:00`))}</td>
      <td><span class="tag ${item.type === "EXPENSE" ? "expense" : ""}">${item.type === "INCOME" ? "Receita" : "Despesa"}</span></td>
      <td class="${item.type === "INCOME" ? "amount-income" : "amount-expense"}">${item.type === "INCOME" ? "+" : "−"} ${money.format(item.amount)}</td>
      <td><button class="table-action" data-action="edit" data-id="${item.id}" title="Editar">Editar</button><button class="table-action delete" data-action="delete" data-id="${item.id}" title="Excluir">Excluir</button></td>`;
    row.dataset.transaction = JSON.stringify(item);
    body.appendChild(row);
  });
}

function renderCategories(items) {
  const list = $("#categorySummary");
  if (!items.length) { list.innerHTML = '<p class="empty">Nenhuma categoria registrada.</p>'; return; }
  list.innerHTML = items.map((item) => `<article class="category-item"><strong>${escapeHtml(item.category)}</strong><small>Receitas: ${money.format(item.totalIncome || 0)}</small><small>Despesas: ${money.format(item.totalExpense || 0)}</small></article>`).join("");
}

function renderHistory(items) {
  const list = $("#commandHistory");
  if (!items.length) { list.innerHTML = '<p class="empty">Nenhum comando enviado.</p>'; return; }
  list.innerHTML = items.map((item) => `<article class="history-item"><div><strong>${item.success ? "Comando processado" : "Falha no comando"}</strong><time>${new Date(item.createdAt).toLocaleString("pt-BR")}</time></div><p>${escapeHtml(item.transcribedText || item.errorMessage || "Sem transcrição")}</p></article>`).join("");
}

function openTransactionDialog(transaction = null) {
  $("#transactionForm").reset();
  $("#transactionId").value = transaction?.id || "";
  $("#dialogTitle").textContent = transaction ? "Editar transação" : "Nova transação";
  $("#description").value = transaction?.description || "";
  $("#amount").value = transaction?.amount || "";
  $("#type").value = transaction?.type || "EXPENSE";
  $("#category").value = transaction?.category || "";
  $("#transactionDate").value = transaction?.transactionDate || today();
  $("#transactionDialog").showModal();
}

function closeTransactionDialog() { $("#transactionDialog").close(); }

async function saveTransaction(event) {
  event.preventDefault();
  const id = $("#transactionId").value;
  const payload = {
    description: $("#description").value.trim(), amount: Number($("#amount").value),
    type: $("#type").value, category: $("#category").value.trim(), transactionDate: $("#transactionDate").value
  };
  try {
    await request(id ? `${api.transactions}/${id}` : api.transactions, {
      method: id ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload)
    });
    closeTransactionDialog();
    toast(id ? "Transação atualizada." : "Transação criada.");
    await loadDashboard();
  } catch (error) { toast(`Não foi possível salvar: ${error.message}`, true); }
}

async function handleTransactionAction(event) {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const transaction = JSON.parse(button.closest("tr").dataset.transaction);
  if (button.dataset.action === "edit") return openTransactionDialog(transaction);
  if (!confirm(`Excluir “${transaction.description}”?`)) return;
  try {
    await request(`${api.transactions}/${transaction.id}`, { method: "DELETE" });
    toast("Transação excluída.");
    await loadDashboard();
  } catch (error) { toast(`Não foi possível excluir: ${error.message}`, true); }
}

async function toggleRecording() {
  if (mediaRecorder?.state === "recording") return stopRecording();
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
    return toast("Este navegador não oferece suporte à gravação de áudio.", true);
  }
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const preferred = ["audio/webm;codecs=opus", "audio/webm", "audio/mp4"].find((type) => MediaRecorder.isTypeSupported(type));
    mediaRecorder = preferred ? new MediaRecorder(mediaStream, { mimeType: preferred }) : new MediaRecorder(mediaStream);
    audioChunks = [];
    mediaRecorder.ondataavailable = (event) => { if (event.data.size) audioChunks.push(event.data); };
    mediaRecorder.onstop = finishRecording;
    mediaRecorder.start();
    setRecordingState(true);
  } catch (error) {
    toast(error.name === "NotAllowedError" ? "Permissão do microfone negada." : `Erro ao acessar o microfone: ${error.message}`, true);
  }
}

function stopRecording() {
  mediaRecorder.stop();
  mediaStream?.getTracks().forEach((track) => track.stop());
  setRecordingState(false);
}

function finishRecording() {
  recordedBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || "audio/webm" });
  $("#audioPreview").src = URL.createObjectURL(recordedBlob);
  $("#audioPreview").hidden = false;
  $("#recordingActions").hidden = false;
  $("#recordingTitle").textContent = "Gravação pronta";
  $("#recordingHelp").textContent = "Ouça antes de enviar para o assistente.";
}

function setRecordingState(recording) {
  $("#recordButton").classList.toggle("recording", recording);
  $("#recordButton").setAttribute("aria-label", recording ? "Parar gravação" : "Iniciar gravação");
  $("#recordingIndicator").hidden = !recording;
  $("#recordingTitle").textContent = recording ? "Estamos ouvindo..." : "Processando gravação...";
  if (recording) {
    seconds = 0; updateTimer(); timerInterval = setInterval(() => { seconds += 1; updateTimer(); }, 1000);
  } else clearInterval(timerInterval);
}

function updateTimer() {
  $("#recordingTime").textContent = `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function resetRecording() {
  if ($("#audioPreview").src) URL.revokeObjectURL($("#audioPreview").src);
  recordedBlob = null;
  $("#audioPreview").removeAttribute("src");
  $("#audioPreview").hidden = true;
  $("#recordingActions").hidden = true;
  $("#recordingTitle").textContent = "Toque para gravar";
  $("#recordingHelp").textContent = "O navegador pedirá acesso ao seu microfone.";
}

function handleFileSelection(event) {
  const file = event.target.files[0];
  if (!file) return;
  $("#selectedFile").textContent = file.name;
  if (file.size > 25 * 1024 * 1024) return toast("O arquivo excede o limite de 25 MB.", true);
  sendAudio(file, file.name);
}

async function sendAudio(blob, filename) {
  if (!blob) return;
  const form = new FormData();
  form.append("audio", blob, filename);
  setVoiceBusy(true);
  try {
    const result = await request(api.voice, { method: "POST", body: form });
    $("#voiceResult").innerHTML = `<strong>${escapeHtml(result.assistantReply)}</strong><span>Entendido: “${escapeHtml(result.transcribedText)}”</span>`;
    $("#voiceResult").hidden = false;
    toast("Comando processado com sucesso.");
    resetRecording();
    $("#audioFile").value = "";
    $("#selectedFile").textContent = "MP3, WAV, M4A ou WEBM — máximo de 25 MB";
    await loadDashboard();
  } catch (error) { toast(`Não foi possível processar o áudio: ${error.message}`, true); }
  finally { setVoiceBusy(false); }
}

function setVoiceBusy(busy) {
  $("#sendRecordingButton").disabled = busy;
  $("#recordButton").disabled = busy;
  $("#recordingTitle").textContent = busy ? "Enviando para o assistente..." : $("#recordingTitle").textContent;
}

function recordingFilename() {
  const type = recordedBlob?.type || "audio/webm";
  return `comando-${Date.now()}.${type.includes("mp4") ? "m4a" : "webm"}`;
}

function today() { return new Date().toISOString().slice(0, 10); }
function escapeHtml(value = "") { const node = document.createElement("div"); node.textContent = value; return node.innerHTML; }
function toast(message, error = false) {
  const element = $("#toast");
  element.textContent = message;
  element.classList.toggle("error", error);
  element.classList.add("show");
  clearTimeout(toast.timeout);
  toast.timeout = setTimeout(() => element.classList.remove("show"), 3500);
}
