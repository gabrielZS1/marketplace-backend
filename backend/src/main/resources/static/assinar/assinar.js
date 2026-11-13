(function () {
  "use strict";

  // O token fica só em memória: fechou ou recarregou a página, precisa entrar de novo.
  var token = null;
  var business = null;

  var $ = function (id) { return document.getElementById(id); };

  var STATUS = {
    TRIAL: "Período de teste",
    ACTIVE: "Assinatura ativa",
    PAST_DUE: "Pagamento pendente",
    EXPIRED: "Teste encerrado",
    SUSPENDED: "Assinatura suspensa"
  };

  function money(v) {
    if (v == null) return "—";
    return "R$ " + Number(v).toFixed(2).replace(".", ",");
  }

  function fmtDate(iso) {
    if (!iso) return "";
    var d = new Date(iso);
    return isNaN(d.getTime()) ? "" : d.toLocaleDateString("pt-BR");
  }

  async function api(method, path, body) {
    var headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (token) headers["Authorization"] = "Bearer " + token;

    var res;
    try {
      res = await fetch(path, { method: method, headers: headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch (e) {
      throw new Error("Sem conexão. Tente novamente.");
    }

    var data = null;
    try { data = await res.json(); } catch (e) { /* corpo vazio */ }
    if (!res.ok) {
      throw new Error((data && data.message) || "Não foi possível concluir. Tente novamente.");
    }
    return data;
  }

  function showLogin() {
    token = null;
    business = null;
    $("plan").hidden = true;
    $("login").hidden = false;
    $("password").value = "";
  }

  function render() {
    var st = business.subscriptionStatus;
    $("bizName").textContent = business.name || "";
    $("status").textContent = STATUS[st] || st;
    $("team").textContent = business.teamSize === 1 ? "1 profissional" : "Até " + (business.teamSize || "?") + " profissionais";
    $("price").textContent = money(business.monthlyPrice);

    var note = "";
    var canPay = true;
    if (st === "ACTIVE") {
      canPay = false;
      note = business.currentPeriodEnd ? "Próxima cobrança em " + fmtDate(business.currentPeriodEnd) + "." : "Sua assinatura está em dia.";
    } else if (st === "TRIAL") {
      note = business.trialEndsAt ? "Seu teste grátis vai até " + fmtDate(business.trialEndsAt) + ". Quem assina antes não paga em dobro." : "";
    } else if (st === "PAST_DUE") {
      note = "Regularize o pagamento para não perder o acesso.";
    } else {
      note = "Seus dados continuam salvos. Assine para voltar a usar o app.";
    }
    $("statusNote").textContent = note;

    var pay = $("payBtn");
    pay.hidden = !canPay;
    pay.textContent = st === "PAST_DUE" || st === "SUSPENDED" ? "Regularizar pagamento" : "Assinar agora";

    $("login").hidden = true;
    $("plan").hidden = false;
  }

  $("loginForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    var btn = $("loginBtn");
    $("loginError").textContent = "";
    btn.disabled = true;
    try {
      var auth = await api("POST", "/api/auth/login", {
        email: $("email").value.trim().toLowerCase(),
        password: $("password").value
      });
      if (auth.role !== "BUSINESS_OWNER") {
        throw new Error("Só o dono do estabelecimento pode assinar. Entre com a conta do dono.");
      }
      token = auth.token;
      business = await api("GET", "/api/businesses/me");
      render();
    } catch (err) {
      token = null;
      $("loginError").textContent = err.message;
    } finally {
      btn.disabled = false;
      $("password").value = "";
    }
  });

  $("payBtn").addEventListener("click", async function () {
    var btn = $("payBtn");
    $("planError").textContent = "";
    btn.disabled = true;
    try {
      var result = await api("POST", "/api/businesses/" + encodeURIComponent(business.id) + "/subscribe");
      var url = result && result.paymentUrl;
      // Só segue para um link seguro do Mercado Pago.
      if (typeof url !== "string" || url.indexOf("https://") !== 0) {
        throw new Error("Não recebemos um link de pagamento válido. Tente novamente.");
      }
      window.location.assign(url);
    } catch (err) {
      $("planError").textContent = err.message;
      btn.disabled = false;
    }
  });

  $("logoutBtn").addEventListener("click", showLogin);
})();
