(function () {
  "use strict";

  // Token só em memória: recarregou a página, entra de novo. Todo texto vindo da API é
  // inserido com textContent (nunca innerHTML), porque é conteúdo escrito por usuários.
  var token = null;
  var currentStatus = "OPEN";

  var $ = function (id) { return document.getElementById(id); };

  var REASONS = {
    SPAM: "Spam",
    OFFENSIVE: "Ofensivo",
    FAKE: "Falso / enganoso",
    INAPPROPRIATE: "Impróprio",
    OTHER: "Outro"
  };

  function el(tag, opts, children) {
    var node = document.createElement(tag);
    opts = opts || {};
    if (opts.className) node.className = opts.className;
    if (opts.text != null) node.textContent = String(opts.text);
    if (opts.type) node.type = opts.type;
    (children || []).forEach(function (c) { if (c) node.appendChild(c); });
    return node;
  }

  function fmtDate(iso) {
    if (!iso) return "";
    var d = new Date(iso);
    return isNaN(d.getTime()) ? "" : d.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
  }

  // Só mostra imagens do próprio servidor ou https (o CSP da página também garante isso).
  function safeImage(url) {
    if (typeof url !== "string" || !url) return null;
    if (url.indexOf("https://") === 0 || url.indexOf("/uploads/") === 0) return url;
    return null;
  }

  async function api(method, path) {
    var headers = { "Accept": "application/json" };
    if (token) headers["Authorization"] = "Bearer " + token;
    var res;
    try {
      res = await fetch(path, { method: method, headers: headers });
    } catch (e) {
      throw new Error("Sem conexão. Tente novamente.");
    }
    var data = null;
    try { data = await res.json(); } catch (e) { /* corpo vazio */ }
    if (res.status === 401) { showLogin("Sua sessão expirou. Entre de novo."); throw new Error(""); }
    if (!res.ok) throw new Error((data && data.message) || "Não foi possível concluir.");
    return data;
  }

  async function login(email, password) {
    var res;
    try {
      res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify({ email: email, password: password })
      });
    } catch (e) {
      throw new Error("Sem conexão. Tente novamente.");
    }
    var data = null;
    try { data = await res.json(); } catch (e) { /* vazio */ }
    if (!res.ok || !data || !data.token) {
      throw new Error((data && data.message) || "E-mail ou senha inválidos.");
    }
    return data.token;
  }

  function showLogin(message) {
    token = null;
    $("panel").hidden = true;
    $("login").hidden = false;
    $("password").value = "";
    $("loginError").textContent = message || "";
    $("list").replaceChildren();
    $("suspended").replaceChildren();
  }

  function setBusy(container, busy) {
    container.querySelectorAll("button").forEach(function (b) { b.disabled = busy; });
  }

  function renderReport(item) {
    var t = item.target || {};
    var typeLabel = item.targetType === "REVIEW" ? "Avaliação" : "Estabelecimento";

    var head = el("div", {}, [
      el("span", { className: "chip", text: typeLabel }),
      el("span", { className: "chip", text: REASONS[item.reason] || item.reason }),
      item.openReportsForTarget > 1
        ? el("span", { className: "chip", text: item.openReportsForTarget + " denúncias" })
        : null,
      el("span", { className: "muted", text: fmtDate(item.createdAt) + " · por " + (item.reporterName || "cliente") })
    ]);

    var info = el("div", {}, [
      el("p", {}, [el("b", { text: t.title || "—" })]),
      t.businessName ? el("p", { className: "muted", text: t.businessName }) : null,
      t.rating != null ? el("p", { text: "Nota: " + t.rating + " de 5" }) : null,
      t.text ? el("p", { className: "quote", text: t.text }) : null,
      t.suspended ? el("p", { className: "muted", text: "Já está suspenso." }) : null
    ]);

    var target = el("div", { className: "target" });
    var img = safeImage(t.imageUrl);
    if (img) {
      var image = el("img");
      image.alt = "";
      image.loading = "lazy";
      image.referrerPolicy = "no-referrer";
      image.src = img;
      target.appendChild(image);
    }
    target.appendChild(info);

    var card = el("div", { className: "card" }, [head, target]);
    if (item.details) {
      card.appendChild(el("p", { className: "muted quote", text: "Comentário de quem denunciou: " + item.details }));
    }

    if (item.status === "OPEN") {
      var removeLabel = item.targetType === "REVIEW" ? "Apagar avaliação" : "Suspender estabelecimento";
      var dismissBtn = el("button", { className: "ghost", type: "button", text: "Descartar" });
      var removeBtn = el("button", { className: "danger", type: "button", text: removeLabel });
      if (!t.exists) removeBtn.disabled = true;
      var err = el("p", { className: "error" });

      dismissBtn.addEventListener("click", function () { act(card, err, item.id, "dismiss"); });
      removeBtn.addEventListener("click", function () {
        var msg = item.targetType === "REVIEW"
          ? "Apagar esta avaliação de vez? Não dá para desfazer."
          : "Suspender este estabelecimento? Ele some da busca até ser reativado aqui.";
        if (window.confirm(msg)) act(card, err, item.id, "remove");
      });

      card.appendChild(el("div", { className: "actions" }, [dismissBtn, removeBtn]));
      card.appendChild(err);
    }
    return card;
  }

  async function act(card, errEl, id, action) {
    errEl.textContent = "";
    setBusy(card, true);
    try {
      await api("POST", "/api/admin/moderation/reports/" + encodeURIComponent(id) + "/" + action);
      await refresh();
    } catch (e) {
      errEl.textContent = e.message;
      setBusy(card, false);
    }
  }

  function renderSuspended(list) {
    var box = $("suspended");
    box.replaceChildren();
    if (!list.length) {
      box.appendChild(el("div", { className: "card empty muted", text: "Nenhum estabelecimento suspenso." }));
      return;
    }
    list.forEach(function (b) {
      var btn = el("button", { className: "ghost", type: "button", text: "Reativar" });
      var err = el("p", { className: "error" });
      var card = el("div", { className: "card bar" }, [
        el("div", {}, [
          el("b", { text: b.name || "Sem nome" }),
          el("p", { className: "muted", text: (b.city || "") + (b.updatedAt ? " · suspenso em " + fmtDate(b.updatedAt) : "") })
        ]),
        btn
      ]);
      btn.addEventListener("click", async function () {
        if (!window.confirm("Reativar este estabelecimento? Ele volta a aparecer se a assinatura estiver em dia.")) return;
        btn.disabled = true;
        try {
          await api("POST", "/api/admin/moderation/businesses/" + encodeURIComponent(b.id) + "/reinstate");
          await refresh();
        } catch (e) {
          err.textContent = e.message;
          btn.disabled = false;
        }
      });
      box.appendChild(card);
      box.appendChild(err);
    });
  }

  async function refresh() {
    $("panelError").textContent = "";
    try {
      var results = await Promise.all([
        api("GET", "/api/admin/moderation/reports?status=" + encodeURIComponent(currentStatus)),
        api("GET", "/api/admin/moderation/businesses/suspended")
      ]);
      var list = $("list");
      list.replaceChildren();
      if (!results[0].length) {
        list.appendChild(el("div", {
          className: "card empty muted",
          text: currentStatus === "OPEN" ? "Nenhuma denúncia aberta. Tudo em dia." : "Nada por aqui."
        }));
      } else {
        results[0].forEach(function (item) { list.appendChild(renderReport(item)); });
      }
      renderSuspended(results[1]);
    } catch (e) {
      if (e.message) $("panelError").textContent = e.message;
    }
  }

  $("loginForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    var btn = $("loginBtn");
    $("loginError").textContent = "";
    btn.disabled = true;
    try {
      token = await login($("email").value.trim().toLowerCase(), $("password").value);
      await api("GET", "/api/admin/moderation/me");   // 403 se não for administrador
      $("login").hidden = true;
      $("panel").hidden = false;
      await refresh();
    } catch (err) {
      token = null;
      if (err.message) $("loginError").textContent = /permiss/i.test(err.message)
        ? "Esta conta não tem acesso à moderação."
        : err.message;
    } finally {
      btn.disabled = false;
      $("password").value = "";
    }
  });

  document.querySelectorAll(".tabs button").forEach(function (b) {
    b.addEventListener("click", function () {
      currentStatus = b.getAttribute("data-status");
      document.querySelectorAll(".tabs button").forEach(function (o) {
        o.setAttribute("aria-pressed", o === b ? "true" : "false");
      });
      refresh();
    });
  });

  $("reloadBtn").addEventListener("click", refresh);
  $("logoutBtn").addEventListener("click", function () { showLogin(""); });
})();
