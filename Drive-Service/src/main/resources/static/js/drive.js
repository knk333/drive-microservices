// The API base path comes from the Thymeleaf template (data-api on <body>),
// so it stays correct even if the app runs under a context path.
const API = document.body.dataset.api || "/api";

// Breadcrumb trail kept in the browser: [{ id, name }]. Root has id = null.
let path = [{ id: null, name: "My Drive" }];
const current = () => path[path.length - 1];

const $ = (sel) => document.querySelector(sel);
const listEl = $("#list");
const panel = $("#panel");
const toastEl = $("#toast");
let toastTimer;

/* ---------- helpers ---------- */
function toast(message, isError = false, ms = 3500) {
  toastEl.textContent = message;
  toastEl.className = "toast" + (isError ? " error" : "");
  toastEl.hidden = false;
  clearTimeout(toastTimer);
  if (ms) toastTimer = setTimeout(() => (toastEl.hidden = true), ms);
}

async function api(url, options) {
  const res = await fetch(API + url, options);
  if (!res.ok) {
    let message = "Request failed (" + res.status + ")";
    try {
      const problem = await res.json();
      message = problem.detail || problem.message || message;
    } catch (_) { /* body was not JSON */ }
    throw new Error(message);
  }
  return res;
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + " B";
  const units = ["KB", "MB", "GB"];
  let i = -1;
  do { bytes /= 1024; i++; } while (bytes >= 1024 && i < units.length - 1);
  return bytes.toFixed(bytes < 10 ? 1 : 0) + " " + units[i];
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

const SVG = "http://www.w3.org/2000/svg";
function icon(kind) {
  const svg = document.createElementNS(SVG, "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  const p = document.createElementNS(SVG, "path");
  if (kind === "FOLDER") {
    p.setAttribute("d", "M3 6.5A1.5 1.5 0 0 1 4.5 5h4.3l2 2.2h8.7A1.5 1.5 0 0 1 21 8.7v9.8a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z");
    p.setAttribute("fill", "var(--folder)");
  } else {
    p.setAttribute("d", "M6 3h8l5 5v12.5A1.5 1.5 0 0 1 17.5 22h-11A1.5 1.5 0 0 1 5 20.5v-16A1.5 1.5 0 0 1 6.5 3zM14 3v5h5");
    p.setAttribute("fill", "none");
    p.setAttribute("stroke", "var(--file)");
    p.setAttribute("stroke-width", "1.6");
    p.setAttribute("stroke-linejoin", "round");
  }
  svg.appendChild(p);
  return svg;
}

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  Object.assign(node, props);
  children.forEach((c) => node.append(c));
  return node;
}

/* ---------- rendering ---------- */
function renderCrumbs() {
  const nav = $("#crumbs");
  nav.replaceChildren();
  path.forEach((step, index) => {
    const last = index === path.length - 1;
    const btn = el("button", { type: "button", textContent: step.name });
    if (last) btn.setAttribute("aria-current", "page");
    else btn.addEventListener("click", () => { path = path.slice(0, index + 1); load(); });
    nav.append(btn);
    if (!last) nav.append(el("span", { className: "sep", textContent: "/" }));
  });
}

function renderRows(items) {
  listEl.replaceChildren();
  if (items.length === 0) {
    listEl.append(el("div", { className: "empty" },
      el("strong", { textContent: "This folder is empty" }),
      "Drop files here or choose Upload files."));
    return;
  }
  items.sort((a, b) => (a.kind === b.kind ? a.name.localeCompare(b.name) : a.kind === "FOLDER" ? -1 : 1));

  items.forEach((item) => {
    const isFolder = item.kind === "FOLDER";
    const downloadUrl = API + "/files/" + item.id;
    const label = isFolder
      ? el("button", { type: "button", className: "label", textContent: item.name, title: item.name })
      : el("a", { className: "label", textContent: item.name, title: item.name, href: downloadUrl });
    if (isFolder) label.addEventListener("click", () => { path.push({ id: item.id, name: item.name }); load(); });

    const actions = el("div", { className: "row-actions" });
    if (!isFolder) actions.append(el("a", { className: "btn", textContent: "Download", href: downloadUrl }));
    const del = el("button", { type: "button", className: "danger", textContent: "Delete" });
    del.addEventListener("click", () => confirmDelete(item));
    actions.append(del);

    listEl.append(el("div", { className: "row" },
      el("div", { className: "name" }, icon(item.kind), label),
      el("span", { className: "meta", textContent: isFolder ? "\u2014" : formatSize(item.size) }),
      el("span", { className: "meta", textContent: formatDate(item.createdAt) }),
      actions));
  });
}

async function load() {
  renderCrumbs();
  const id = current().id;
  try {
    const res = await api(id ? "/items?id=" + id : "/items");
    renderRows(await res.json());
  } catch (e) {
    renderRows([]);
    toast(e.message, true);
  }
}

/* ---------- actions ---------- */
async function createFolder(name) {
  try {
    await api("/folders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, parentId: current().id })
    });
    toast("Folder created");
    load();
  } catch (e) { toast(e.message, true); }
}

async function uploadFiles(files) {
  const list = Array.from(files);
  let done = 0;
  for (const file of list) {
    toast("Uploading " + file.name + "\u2026", false, 0);
    const form = new FormData();
    form.append("file", file);
    const query = current().id ? "?parentId=" + current().id : "";
    try {
      await api("/files" + query, { method: "POST", body: form });
      done++;
    } catch (e) {
      toast(file.name + ": " + e.message, true, 6000);
    }
  }
  if (done > 0) toast(done === 1 ? "File uploaded" : done + " files uploaded");
  load();
}

function confirmDelete(item) {
  const dialog = $("#deleteDialog");
  $("#deleteTitle").textContent = "Delete \u201C" + item.name + "\u201D?";
  dialog.returnValue = "";
  dialog.onclose = async () => {
    if (dialog.returnValue !== "delete") return;
    try {
      await api("/items/" + item.id, { method: "DELETE" });
      toast("Deleted");
      load();
    } catch (e) { toast(e.message, true); }
  };
  dialog.showModal();
}

/* ---------- wiring ---------- */
$("#newFolderBtn").addEventListener("click", () => {
  $("#folderName").value = "";
  $("#folderDialog").showModal();
  $("#folderName").focus();
});
$("#folderCancel").addEventListener("click", () => $("#folderDialog").close());
$("#folderForm").addEventListener("submit", () => {
  const name = $("#folderName").value.trim();
  if (name) createFolder(name);
});
$("#deleteCancel").addEventListener("click", () => $("#deleteDialog").close());

$("#uploadBtn").addEventListener("click", () => $("#fileInput").click());
$("#fileInput").addEventListener("change", (e) => {
  if (e.target.files.length) uploadFiles(e.target.files);
  e.target.value = "";
});

["dragenter", "dragover"].forEach((type) =>
  panel.addEventListener(type, (e) => { e.preventDefault(); panel.classList.add("dragging"); }));
["dragleave", "drop"].forEach((type) =>
  panel.addEventListener(type, (e) => { e.preventDefault(); panel.classList.remove("dragging"); }));
panel.addEventListener("drop", (e) => {
  if (e.dataTransfer.files.length) uploadFiles(e.dataTransfer.files);
});

load();