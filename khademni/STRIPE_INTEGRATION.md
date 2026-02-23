Stripe integration steps for `khademni` module

1) Provide credentials (choose one):
   - Preferred (dev): set environment variables on Windows Terminal / PowerShell:

```powershell
$env:STRIPE_SECRET_KEY="<your_secret_key>"
$env:STRIPE_PUBLISHABLE_KEY="<your_publishable_key>"
```

   - Alternative (not recommended for production): edit `src/main/resources/stripe.properties` and replace placeholders:

```
stripe.secret_key=YOUR_SECRET_KEY
stripe.publishable_key=YOUR_PUBLISHABLE_KEY
stripe.default_currency=usd
# Optional: a connected account id to send transfers to (Stripe Connect)
stripe.connected_account_id=acct_XXXXXXXX
```

2) How it works in the app:
   - `PaiementController.handlePay()` / `PaiementFormController` call `StripeService.createCheckoutSession(...)` which creates a Stripe Checkout session and returns the hosted checkout URL.
   - The app opens the URL in an embedded `WebView` or the system browser. After redirection to the configured success URL the app extracts the `session_id` and calls `StripeService.verifySession(session_id)` to confirm payment.

3) Testing tips:
   - Use your Stripe test keys (visible in the Dashboard test mode).
   - Use test card numbers such as `4242 4242 4242 4242` to simulate successful payments.

4) Post-payment verification:
   - The app verifies the session via Stripe API (expanding `payment_intent`) and marks the contract `PAYE` on success.

5) Troubleshooting:
   - If you see: "Stripe secret key not configured..." then set `STRIPE_SECRET_KEY` or fill `stripe.properties`.
   - If you see 401/403 responses, confirm the secret key is correct and not revoked.
   - Check logs / exception dialogs for full response bodies.

6) Security note:
   - Do NOT commit real secrets to source control. Keep secret keys on a secure backend or environment variables.
