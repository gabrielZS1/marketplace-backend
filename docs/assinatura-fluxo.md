# Fluxo de assinatura / período de teste

## Regras de negócio

| Origem do cliente | Período grátis | Depois do período grátis |
|---|---|---|
| Código promocional Kiwify "Plano 1" (`free_days = 15`) | 15 dias | Mensalidade por nº de profissionais (Mercado Pago) |
| Código promocional Kiwify "Plano 2" (`free_days = 30`) | 30 dias | Mensalidade por nº de profissionais (Mercado Pago) |
| Sem código (fora do lançamento) | 7 dias (`app.subscription.default-trial-days`) | Mensalidade por nº de profissionais (Mercado Pago) |

**Tabela da mensalidade** (`PricingService.monthlyPriceFor`):

| Profissionais | Preço/mês |
|---|---|
| 1 | R$ 10,00 |
| 2 | R$ 20,00 |
| 3 | R$ 30,00 |
| 4 | R$ 40,00 |
| 5 ou mais | R$ 50,00 |

- **O período de teste conta a partir da criação da conta** (`business.createdAt`), não do fim do onboarding. Um código promocional aplicado no fim do onboarding recalcula o prazo a partir da mesma data de criação.
- Enquanto o onboarding não é concluído, o dono nunca é bloqueado (precisa conseguir terminar o cadastro e aplicar o código).
- Os R$30/R$50 do lançamento são cobrados **pelo Kiwify**, uma vez, fora do app. O app só lê o código e concede os dias.
- O Kiwify entrega 1 código único por comprador (upload da lista via `GET /api/promo-codes/export`).
- A cobrança recorrente é sempre **Mercado Pago** (preapproval mensal).

## Estados da assinatura (`subscriptionStatus`)

| Status | Acesso ao painel | Significado | Tela no app |
|---|---|---|---|
| `TRIAL` | ✅ liberado | Dentro dos 7/15/30 dias | Banner "seu teste acaba em X dias" |
| `ACTIVE` | ✅ liberado | Assinatura paga e em dia | Normal |
| `PAST_DUE` | ✅ liberado (tolerância) | Pagamento falhou, dentro da tolerância (4 dias) | Banner "regularize seu pagamento até DD/MM" |
| `EXPIRED` | ⛔ BLOQUEADO | Teste acabou e nunca assinou | **Tela de renovação** — "Seu período de teste terminou" |
| `SUSPENDED` | ⛔ BLOQUEADO | Assinou e o pagamento foi suspenso/cancelado | **Tela de renovação** — "Sua assinatura foi suspensa" |

> Bloqueio **não apaga dados**. Quando o pagamento é confirmado (webhook `authorized`), o acesso volta sozinho.

O backend calcula o status "real" na hora (`SubscriptionStatusService`), então o app recebe `EXPIRED`/`SUSPENDED` no mesmo instante em que vencem — não depende do job diário das 3h.

## Endpoints usados pelo app

### `GET /api/businesses/me`  → status da assinatura
Sempre liberado (mesmo bloqueado). O app deve chamar no boot e ao voltar do background.

```json
{
  "id": "uuid",
  "name": "Studio X",
  "onboardingCompleted": true,
  "subscriptionStatus": "EXPIRED",
  "active": false,
  "blockReason": "TRIAL_EXPIRED",       // null | "TRIAL_EXPIRED" | "PAYMENT_FAILED"
  "trialEndsAt": "2026-09-10T12:00:00Z",
  "graceEndsAt": null,
  "currentPeriodEnd": null,
  "teamSize": 2,
  "monthlyPrice": 20.00
}
```

### `POST /api/businesses/{businessId}/subscribe`  → gera o link de pagamento
Sempre liberado. Cria/recria a assinatura no Mercado Pago com o preço da tabela.
Se ainda estiver no trial, os dias restantes viram `free_trial` (não cobra em dobro).

```json
{ "businessId": "uuid", "onboardingCompleted": true, "paymentUrl": "https://www.mercadopago.com/..." }
```

### Qualquer outra rota de gestão quando bloqueado → `403`
```json
{ "error": "SUBSCRIPTION_BLOCKED", "status": 403, "reason": "TRIAL_EXPIRED" }
```

## O que o FRONT precisa implementar

1. **Guard de navegação global**
   - No boot / retorno do background: `GET /api/businesses/me`.
   - `active === false`  →  navegar pra `RenovarPlano` e **travar** o resto do app (voltar não sai da tela).
   - `subscriptionStatus === "TRIAL"`  →  liberado + banner com contagem regressiva (`trialEndsAt`).
   - `subscriptionStatus === "PAST_DUE"`  →  liberado + banner de alerta (`graceEndsAt`).
   - `subscriptionStatus === "ACTIVE"`  →  normal.

2. **Interceptor no cliente HTTP**
   - Resposta `403` com body `error === "SUBSCRIPTION_BLOCKED"` → limpar navegação e ir pra `RenovarPlano`.
   - (Não confundir com `401`, que é sessão expirada → login.)

3. **Tela `RenovarPlano`**
   - Título/texto conforme `blockReason`:
     - `TRIAL_EXPIRED` → "Seu período de teste terminou".
     - `PAYMENT_FAILED` → "Sua assinatura está suspensa".
   - Mostrar `monthlyPrice` e `teamSize` ("Plano para N profissionais — R$ X/mês").
   - Botão **Assinar agora** → `POST /api/businesses/{id}/subscribe` → abrir `paymentUrl` em WebView / navegador.
   - Ao voltar do pagamento: fazer *polling* de `GET /api/businesses/me` (ex: a cada 3s por ~2min) até `subscriptionStatus === "ACTIVE"`; então liberar o app.
   - Botão secundário "Já paguei / Atualizar" que refaz o `GET /me`.

4. **Onboarding** (sem mudança de contrato)
   - Campo opcional `promoCode` no `POST /api/businesses/onboarding/{id}/complete`.
   - Código inválido / já usado → `400` com mensagem; mostrar erro no campo.
   - Sem código → 7 dias de teste automático.

5. **Banner de trial** (enquanto `TRIAL`)
   - `dias = ceil((trialEndsAt - agora) / 1 dia)`.
   - CTA "Assinar agora" também chama `POST /subscribe` (o cliente pode adiantar; os dias restantes não são perdidos).

## Painel admin (gerar códigos pro Kiwify)

- `POST /api/promo-codes/generate`  `{ "quantity": 200, "freeDays": 15, "label": "LANCAMENTO-15" }`
- `POST /api/promo-codes/generate`  `{ "quantity": 200, "freeDays": 30, "label": "LANCAMENTO-30" }`
- `GET /api/promo-codes/export?label=LANCAMENTO-15`  → CSV pra subir no Kiwify (produto 1)
- `GET /api/promo-codes/export?label=LANCAMENTO-30`  → CSV pra subir no Kiwify (produto 2)

## Pontos a validar no sandbox do Mercado Pago

- `auto_recurring.free_trial` com `frequency_type: "days"` (usado quando o cliente assina ainda no trial).
- Webhook `authorized` → status vira `ACTIVE` e `active = true` (desbloqueia).
- Webhook `paused` → `PAST_DUE` + `graceEndsAt = agora + 4 dias`.
- Webhook `cancelled` → `SUSPENDED` + `active = false`.
