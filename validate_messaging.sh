#!/bin/bash

# Script de validation des fonctionnalités de messagerie
# À exécuter après chaque changement majeur

set -e

WORK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$WORK_DIR"

echo "🔍 Validation de la branche gestion-messages"
echo "=============================================="

# Vérifier que nous sommes sur la bonne branche
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "gestion-messages" ]; then
    echo "❌ Vous n'êtes pas sur la branche 'gestion-messages'"
    echo "Branche actuelle: $CURRENT_BRANCH"
    exit 1
fi

echo "✅ Branche correcte: $CURRENT_BRANCH"

# 1. Vérifier les fichiers contrôleurs
echo ""
echo "📋 Vérification des fichiers contrôleurs..."
FILES_TO_CHECK=(
    "khademni-web/assets/controllers/message_controller.js"
    "khademni-web/assets/controllers/call_controller.js"
    "khademni-web/assets/styles/message.css"
)

for file in "${FILES_TO_CHECK[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file MANQUANT"
        exit 1
    fi
done

# 2. Vérifier les contrôleurs PHP
echo ""
echo "📋 Vérification des contrôleurs PHP..."
if grep -q "createEvent\|sendMeetingInvitation" khademni-web/src/Controller/MessageController.php; then
    echo "✅ MessageController complet"
else
    echo "❌ MessageController incomplet"
    exit 1
fi

if grep -q "handleOffer\|handleAnswer\|handleCandidate" khademni-web/src/Controller/CallController.php; then
    echo "✅ CallController complet"
else
    echo "❌ CallController incomplet"
    exit 1
fi

# 3. Vérifier les services
echo ""
echo "📋 Vérification des services..."
SERVICES=(
    "khademni-web/src/Service/MeetingNotificationService.php"
    "khademni-web/src/Service/GoogleCalendarService.php"
    "khademni-web/src/Service/SummarizeService.php"
)

for service in "${SERVICES[@]}"; do
    if [ -f "$service" ]; then
        echo "✅ $(basename "$service")"
    else
        echo "❌ $(basename "$service") MANQUANT"
        exit 1
    fi
done

# 4. Vérifier la template
echo ""
echo "📋 Vérification de la template..."
if grep -q "data-controller=\"message\"" khademni-web/templates/modules/message_thread.html.twig; then
    echo "✅ Contrôleur Stimulus enregistré"
else
    echo "❌ Contrôleur Stimulus non enregistré"
    exit 1
fi

if grep -q "startAudioCall\|startVideoCall\|summarizeChat" khademni-web/templates/modules/message_thread.html.twig; then
    echo "✅ Boutons et fonctionnalités présents"
else
    echo "❌ Boutons ou fonctionnalités manquants"
    exit 1
fi

# 5. Vérifier la documentation
echo ""
echo "📋 Vérification de la documentation..."
if [ -f "MESSAGING_SETUP.md" ]; then
    echo "✅ Documentation de configuration"
else
    echo "⚠️  MESSAGING_SETUP.md manquant (documentation)"
fi

# 6. Valider la syntaxe JavaScript
echo ""
echo "📋 Validation de la syntaxe JavaScript..."
if command -v node &> /dev/null; then
    for js_file in khademni-web/assets/controllers/*.js; do
        if node -c "$js_file" 2>/dev/null; then
            echo "✅ $(basename "$js_file")"
        else
            echo "⚠️  $(basename "$js_file") - Vérifiez la syntaxe"
        fi
    done
else
    echo "⚠️  Node.js non trouvé, syntaxe JavaScript non vérifiée"
fi

# 7. Vérifier les bases de données
echo ""
echo "📋 Vérification du schéma de base de données..."
if grep -q "face_embedding\|is_admin" schema.sql; then
    echo "✅ Schema.sql à jour"
else
    echo "⚠️  Vérifiez schema.sql"
fi

echo ""
echo "✅ VALIDATION TERMINÉE AVEC SUCCÈS"
echo "=============================================="
echo ""
echo "Prochaines étapes:"
echo "1. Installer les dépendances: cd khademni-web && npm install"
echo "2. Compiler les assets: npm run build"
echo "3. Démarrer le serveur: symfony serve"
echo "4. Tester les fonctionnalités sur http://localhost:8000/messages"
