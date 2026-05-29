const state = {
  order: { page: 1, size: 10 },
  payment: { page: 1, size: 10 },
  inventory: { page: 1, size: 10 }
};

async function fetchJson(url, options = {}) {
  const response = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const payload = await response.json();
  if (payload.code !== 200) {
    throw new Error(payload.message || "request failed");
  }
  return payload.data;
}

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) {
    element.textContent = value ?? "";
  }
}

function warningMarkup(warning) {
  return `
    <div class="warning-item ${warning.severity || ""}">
      <strong>${warning.code}</strong>
      <span>${warning.message}</span>
    </div>
  `;
}

function rowStatus(status) {
  const mapping = {
    0: "pending",
    1: "sending",
    2: "sent",
    3: "failed"
  };
  return mapping[status] || String(status ?? "");
}

function inventoryStatus(status) {
  const mapping = {
    0: "processing",
    1: "processed",
    2: "failed"
  };
  return mapping[status] || String(status ?? "");
}

function serializeParams(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined) {
      search.set(key, value);
    }
  });
  return search.toString();
}

async function loadOverview() {
  const overview = await fetchJson("/admin/api/reliability/overview");
  setText("order-failed-count", overview.failedOrderOutboxCount);
  setText("payment-failed-count", overview.failedPaymentOutboxCount);
  setText("exhausted-count", overview.exhaustedRetryCount);
  setText("inventory-processing-count", overview.inventoryProcessingCount);
  setText("inventory-failed-count", overview.inventoryFailedCount);
  setText("degraded-sections", (overview.degradedSections || []).join(", "));

  const warnings = document.getElementById("warnings");
  warnings.innerHTML = (overview.warnings || []).length
    ? overview.warnings.map(warningMarkup).join("")
    : `<div class="warning-item"><strong>OK</strong><span>No active reliability warnings</span></div>`;
}

async function loadOutbox(service) {
  const topic = document.getElementById(`${service}-topic`).value.trim();
  const aggregateId = document.getElementById(`${service}-aggregateId`).value.trim();
  const status = document.getElementById(`${service}-status`).value.trim();
  const query = serializeParams({
    topic,
    aggregateId,
    status,
    page: state[service].page,
    size: state[service].size
  });
  const data = await fetchJson(`/admin/api/reliability/outbox/${service}/messages?${query}`);
  const tbody = document.getElementById(`${service}-rows`);
  tbody.innerHTML = (data.records || []).map(record => `
    <tr>
      <td>${record.id ?? ""}</td>
      <td>${record.aggregateId ?? ""}</td>
      <td>${record.topic ?? ""}</td>
      <td>${rowStatus(record.status)}</td>
      <td>${record.retryCount ?? 0}</td>
      <td>${record.createdAt ?? ""}</td>
      <td><button class="toggle-button row-action" data-service="${service}" data-message-id="${record.id}">Retry</button></td>
    </tr>
  `).join("");
}

async function loadInventory() {
  const topic = document.getElementById("inventory-topic").value.trim();
  const orderNo = document.getElementById("inventory-orderNo").value.trim();
  const status = document.getElementById("inventory-status").value.trim();
  const query = serializeParams({
    topic,
    orderNo,
    status,
    page: state.inventory.page,
    size: state.inventory.size
  });
  const data = await fetchJson(`/admin/api/reliability/inventory/events?${query}`);
  const tbody = document.getElementById("inventory-rows");
  tbody.innerHTML = (data.records || []).map(record => `
    <tr>
      <td>${record.id ?? ""}</td>
      <td>${record.orderNo ?? ""}</td>
      <td>${record.topic ?? ""}</td>
      <td>${inventoryStatus(record.status)}</td>
      <td>${record.createdAt ?? ""}</td>
    </tr>
  `).join("");
}

async function retryMessage(service, messageId) {
  await fetchJson(`/admin/api/reliability/outbox/${service}/retry`, {
    method: "POST",
    body: JSON.stringify({ messageId })
  });
  await loadOverview();
  await loadOutbox(service);
}

async function retryBatch(service) {
  const topic = document.getElementById(`${service}-topic`).value.trim();
  const aggregateId = document.getElementById(`${service}-aggregateId`).value.trim();
  const status = document.getElementById(`${service}-status`).value.trim();
  const limit = Number(document.getElementById(`${service}-limit`).value || 20);
  await fetchJson(`/admin/api/reliability/outbox/${service}/retry-batch`, {
    method: "POST",
    body: JSON.stringify({
      topic: topic || null,
      aggregateId: aggregateId || null,
      status: status === "" ? null : Number(status),
      limit
    })
  });
  await loadOverview();
  await loadOutbox(service);
}

function activateView(view) {
  document.querySelectorAll(".toggle-button[data-view]").forEach(button => {
    button.classList.toggle("active", button.dataset.view === view);
  });
  document.querySelectorAll(".view").forEach(panel => {
    panel.classList.toggle("active", panel.id === `${view}-view`);
  });
}

function bindEvents() {
  document.getElementById("refresh-overview").addEventListener("click", () => {
    loadOverview().catch(console.error);
  });

  document.querySelectorAll(".toggle-button[data-view]").forEach(button => {
    button.addEventListener("click", () => activateView(button.dataset.view));
  });

  document.getElementById("order-filter").addEventListener("click", () => loadOutbox("order").catch(console.error));
  document.getElementById("payment-filter").addEventListener("click", () => loadOutbox("payment").catch(console.error));
  document.getElementById("inventory-filter").addEventListener("click", () => loadInventory().catch(console.error));
  document.getElementById("order-retry-batch").addEventListener("click", () => retryBatch("order").catch(console.error));
  document.getElementById("payment-retry-batch").addEventListener("click", () => retryBatch("payment").catch(console.error));

  document.body.addEventListener("click", event => {
    const button = event.target.closest(".row-action");
    if (!button) {
      return;
    }
    retryMessage(button.dataset.service, Number(button.dataset.messageId)).catch(console.error);
  });
}

async function init() {
  bindEvents();
  activateView("order");
  await Promise.all([
    loadOverview(),
    loadOutbox("order"),
    loadOutbox("payment"),
    loadInventory()
  ]);
}

init().catch(console.error);
