# PowerShell script to set Stripe test keys for the current session and run the JavaFX app
# Usage: Open PowerShell, run: .\run-with-stripe.ps1

# Replace with your test keys
$env:STRIPE_SECRET_KEY = "sk_test_your_secret_key_here"
$env:STRIPE_PUBLISHABLE_KEY = "pk_test_your_publishable_key_here"

mvn -f "${PWD}\khademni\pom.xml" clean javafx:run
