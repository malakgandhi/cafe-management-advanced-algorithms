const API = `${window.location.origin}/api`;

async function getJson(path) {
  const response = await fetch(`${API}${path}`);
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Request failed (${response.status}).`);
  }
  return data;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, character => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

function showError(element, error) {
  if (!element) return;
  element.textContent = `${error.message} Check that the Java backend is running.`;
  element.className = "notice error";
}

function renderRecords(records, target, editable = false) {
  if (!records.length) {
    target.innerHTML = `<tr><td colspan="${editable ? 4 : 3}" class="empty-state">No usage records found.</td></tr>`;
    return;
  }

  target.innerHTML = records.map(record => `
    <tr>
      <td>${escapeHtml(record.date)}</td>
      <td>${escapeHtml(record.ingredient.replaceAll("_", " "))}</td>
      <td class="numeric quantity">${Number(record.quantity).toFixed(2)}</td>
      ${editable ? `<td><button class="table-action" type="button" data-edit-date="${escapeHtml(record.date)}" data-edit-ingredient="${escapeHtml(record.ingredient)}" data-edit-quantity="${escapeHtml(record.quantity)}">Edit</button></td>` : ""}
    </tr>
  `).join("");
}

async function loadDashboard() {
  const message = document.querySelector("#dashboard-message");
  try {
    const [ingredients, records] = await Promise.all([
      getJson("/ingredients"),
      getJson("/usage")
    ]);
    document.querySelector("#ingredient-count").textContent = ingredients.length;
    document.querySelector("#record-count").textContent = records.length;
    document.querySelector("#latest-date").textContent = records.length
      ? records[records.length - 1].date
      : "No records";
    renderRecords(records.slice(-5).reverse(), document.querySelector("#recent-rows"));
  } catch (error) {
    showError(message, error);
    document.querySelector("#recent-rows").innerHTML =
      '<tr><td colspan="3" class="empty-state">Could not load usage records.</td></tr>';
  }
}

async function loadRecordsPage() {
  const message = document.querySelector("#records-message");
  const body = document.querySelector("#usage-rows");
  const count = document.querySelector("#records-count");
  const filterInput = document.querySelector("#record-filter");
  const addForm = document.querySelector("#add-usage-form");
  let allRecords = [];

  const renderFiltered = () => {
    const query = filterInput.value.trim().toLowerCase();
    const filtered = allRecords.filter(record =>
      record.ingredient.replaceAll("_", " ").toLowerCase().includes(query));
    count.textContent = query
      ? `${filtered.length} of ${allRecords.length} records`
      : `${allRecords.length} records`;
    renderRecords(filtered, body, true);
  };

  try {
    allRecords = await getJson("/usage");
    allRecords.sort((a, b) => b.date.localeCompare(a.date)
      || a.ingredient.localeCompare(b.ingredient));
    renderFiltered();

    filterInput.addEventListener("input", renderFiltered);

    const dateInput = document.querySelector("#new-date");
    const ingredientInput = document.querySelector("#new-ingredient");
    const quantityInput = document.querySelector("#new-quantity");
    const cancelEdit = document.querySelector("#cancel-edit");
    const submitButton = document.querySelector("#record-submit");
    const formTitle = document.querySelector("#record-form-title");
    let recordBeingEdited = null;
    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    dateInput.value = today.toISOString().slice(0, 10);

    addForm.addEventListener("submit", async event => {
      event.preventDefault();
      const updatingRecord = Boolean(recordBeingEdited);
      message.textContent = updatingRecord ? "Updating record…" : "Saving record…";
      message.className = "notice";

      try {
        const formValues = new FormData(addForm);
        const payload = new URLSearchParams(formValues);
        const method = updatingRecord ? "PUT" : "POST";
        if (updatingRecord) {
          payload.set("originalDate", recordBeingEdited.date);
          payload.set("originalIngredient", recordBeingEdited.ingredient);
          payload.set("originalQuantity", recordBeingEdited.quantity);
        }
        const response = await fetch(`${API}/usage`, {
          method,
          headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
          body: payload
        });

        let saved;
        try {
          saved = await response.json();
        } catch {
          throw new Error("The server returned an unreadable response.");
        }

        if (!response.ok) {
          throw new Error(saved.message || `Could not save record (${response.status}).`);
        }

        if (updatingRecord) {
          const index = allRecords.findIndex(record => record.date === recordBeingEdited.date
            && record.ingredient === recordBeingEdited.ingredient
            && Number(record.quantity) === Number(recordBeingEdited.quantity));
          if (index >= 0) allRecords[index] = saved;
        } else {
          allRecords.push(saved);
        }
        allRecords.sort((a, b) => b.date.localeCompare(a.date)
          || a.ingredient.localeCompare(b.ingredient));
        resetRecordForm();
        renderFiltered();
        message.textContent = updatingRecord
          ? "Record updated in sample-usage.csv."
          : "Record saved to sample-usage.csv.";
        message.className = "notice success";
      } catch (error) {
        showError(message, error);
      }
    });

    body.addEventListener("click", event => {
      const button = event.target.closest("[data-edit-date]");
      if (!button) return;
      recordBeingEdited = {
        date: button.dataset.editDate,
        ingredient: button.dataset.editIngredient,
        quantity: button.dataset.editQuantity
      };
      dateInput.value = recordBeingEdited.date;
      ingredientInput.value = recordBeingEdited.ingredient;
      quantityInput.value = recordBeingEdited.quantity;
      formTitle.textContent = "Update usage record";
      submitButton.textContent = "Update record";
      cancelEdit.hidden = false;
      addForm.scrollIntoView({ behavior: "smooth", block: "center" });
    });

    const resetRecordForm = () => {
      recordBeingEdited = null;
      addForm.reset();
      dateInput.value = today.toISOString().slice(0, 10);
      formTitle.textContent = "Add a usage record";
      submitButton.textContent = "Save record +";
      cancelEdit.hidden = true;
    };
    cancelEdit.addEventListener("click", resetRecordForm);
  } catch (error) {
    showError(message, error);
    count.textContent = "Unavailable";
    body.innerHTML = '<tr><td colspan="4" class="empty-state">Could not load usage records.</td></tr>';
  }
}

async function loadAnalysisPage() {
  const message = document.querySelector("#analysis-message");
  const ingredientSelect = document.querySelector("#ingredient");
  const fromInput = document.querySelector("#from");
  const toInput = document.querySelector("#to");
  const form = document.querySelector("#analysis-form");

  try {
    const [ingredients, records] = await Promise.all([
      getJson("/ingredients"),
      getJson("/usage")
    ]);
    ingredientSelect.innerHTML = ingredients.map(name =>
      `<option value="${escapeHtml(name)}">${escapeHtml(name.replaceAll("_", " "))}</option>`
    ).join("");

    const dates = records.map(record => record.date).sort();
    if (dates.length) {
      fromInput.value = dates[0];
      toInput.value = dates[dates.length - 1];
    }

    form.addEventListener("submit", async event => {
      event.preventDefault();
      const params = new URLSearchParams({
        ingredient: ingredientSelect.value,
        from: fromInput.value,
        to: toInput.value
      });
      message.textContent = "Calculating usage…";
      message.className = "notice";

      try {
        const result = await getJson(`/analysis?${params}`);
        document.querySelector("#result-title").textContent =
          `${result.ingredient.replaceAll("_", " ")} summary`;
        document.querySelector("#range-total").textContent = Number(result.rangeTotal).toFixed(2);
        document.querySelector("#percentile-value").textContent = Number(result.percentile90).toFixed(2);
        document.querySelector("#range-caption").textContent = `${result.from} through ${result.to}`;
        document.querySelector("#analysis-results").hidden = false;
        message.textContent = "Analysis complete.";
        message.className = "notice";
      } catch (error) {
        document.querySelector("#analysis-results").hidden = true;
        showError(message, error);
      }
    });

    form.requestSubmit();
  } catch (error) {
    showError(message, error);
  }
}

if (document.querySelector("#recent-rows")) loadDashboard();
if (document.querySelector("#usage-rows")) loadRecordsPage();
if (document.querySelector("#analysis-form")) loadAnalysisPage();
