# Configuration des Services de Messagerie

## Variables d'environnement requises (.env ou .env.local)

```bash
# Mailer (Mailtrap/SMTP)
MAILER_DSN=smtp://username:password@smtp.mailtrap.io:587?encryption=tls
MAILER_FROM=noreply@5ademni.tn

# Google Calendar API (optionnel - pour la planification de réunions)
GOOGLE_CALENDAR_SERVICE_ACCOUNT_JSON=config/google-calendar-service-account.json
GOOGLE_CALENDAR_ID=your-calendar-id@gmail.com

# OpenRouter API (optionnel - pour le résumé IA)
OPENROUTER_API_KEY=your-api-key
OPENROUTER_URL=https://openrouter.ai/api/v1/chat/completions
```

## Installation

### 1. Dépendances npm/JS

Les contrôleurs Stimulus utilisent des APIs natives du navigateur et Hotwired Stimulus. Assurez-vous que Stimulus est correctement installé:

```bash
cd khademni-web
npm install
```

### 2. Enregistrer les contrôleurs Stimulus

Le framework Stimulus doit automatiquement découvrir les contrôleurs dans `assets/controllers/`. Vérifiez que les fichiers sont présents:

- `assets/controllers/message_controller.js` - Gestion du chat
- `assets/controllers/call_controller.js` - Gestion des appels

### 3. Compiler les assets

```bash
npm run build
# ou en développement avec watch:
npm run watch
```

## Fonctionnalités Implémentées

### 1. Envoi de Messages
- Texte
- Emojis (picker intégré)
- Pièces jointes (images, fichiers, PDF)
- Messages programmés
- Messages vocaux (enregistrement audio)

### 2. Appels
- Appels audio bidirectionnels (WebRTC)
- Appels vidéo bidirectionnels (WebRTC)
- Gestion automatique des candidats ICE
- Offre/Réponse SDP

### 3. Planification de Réunions
- Formulaire modal de planification
- Intégration Google Calendar
- Notifications par email via Mailtrap
- Lien automatique vers Google Calendar

### 4. Résumé IA
- Résumé automatique de la conversation
- Utilise OpenRouter API (optionnel)
- Statistiques sur les participants et types de messages

### 5. Recherche Dynamique
- Recherche en temps réel dans le chat
- Filtrage par expéditeur et contenu
- Surbrillance des résultats

### 6. Édition et Suppression de Messages
- Éditer un message envoyé
- Supprimer un message
- Confirmation avant suppression

## Endpoints API Disponibles

### Messages
- `POST /messages/{id}` - Envoyer/Recevoir des messages
- `POST /api/messages/{id}/edit` - Éditer un message
- `POST /api/messages/{id}/delete` - Supprimer un message
- `GET /api/conversations/{conversationId}/summarize` - Résumer une conversation

### Appels
- `POST /messages/{id}/call` - Démarrer un appel
- `GET /messages/{id}/call/{sessionId}/join` - Rejoindre un appel
- `GET /api/call/{sessionId}/status` - État de l'appel
- `POST /api/call/{sessionId}/end` - Terminer l'appel
- `POST /api/call/{sessionId}/offer` - Envoyer une offre SDP
- `POST /api/call/{sessionId}/answer` - Envoyer une réponse SDP
- `POST /api/call/{sessionId}/candidate` - Envoyer un candidat ICE
- `GET /api/call/{sessionId}/offer` - Récupérer l'offre
- `GET /api/call/{sessionId}/answer` - Récupérer la réponse
- `GET /api/call/{sessionId}/candidates` - Récupérer les candidats

## Test des Fonctionnalités

### 1. Test local
```bash
# Démarrer Symfony dev server
symfony serve

# Puis naviguer à: http://localhost:8000/messages
```

### 2. Test des Appels
- Ouvrir deux navigateurs/onglets
- Se connecter avec deux utilisateurs différents
- Créer une conversation
- Cliquer sur "Appel" ou "Vidéo"
- Vérifier que la connexion WebRTC s'établit

### 3. Test de la Planification de Réunions
- Cliquer sur "Réunion"
- Remplir le formulaire
- Vérifier que:
  - La réunion apparaît dans le chat
  - Un email est envoyé via Mailtrap
  - Le lien Google Calendar fonctionne

### 4. Test du Résumé IA
- Avoir plusieurs messages dans une conversation
- Cliquer sur "Résumé IA"
- Vérifier que le résumé s'affiche

## Dépannage

### Les boutons ne répondent pas
1. Vérifier que Stimulus est chargé: `console.log(window.Stimulus)`
2. Vérifier que le contrôleur est enregistré: `ctrl + k` → "Stimulus"
3. Vérifier les erreurs console

### Les appels ne fonctionnent pas
1. Vérifier les permissions de caméra/microphone du navigateur
2. Vérifier la console pour les erreurs WebRTC
3. Vérifier que les STUN servers sont accessibles

### Email non reçu
1. Vérifier les variables `MAILER_DSN` et `MAILER_FROM`
2. Vérifier les logs Symfony: `tail -f var/log/dev.log`
3. Vérifier les emails de spam/junk

### Google Calendar ne fonctionne pas
1. Vérifier que `GOOGLE_CALENDAR_SERVICE_ACCOUNT_JSON` pointe vers le bon fichier
2. Vérifier que le service account a les permissions Calendar
3. Vérifier que `GOOGLE_CALENDAR_ID` est correct

## Architecture

### Frontend (JavaScript/Stimulus)
- **message_controller.js**: Gestion du chat (emoji, pièces jointes, recherche, édition/suppression)
- **call_controller.js**: Gestion des appels WebRTC

### Backend (PHP/Symfony)
- **MessageController**: Envoyer/Recevoir/Éditer/Supprimer messages
- **CallController**: Gérer les sessions d'appel
- **ApiSummarizeController**: Résumer les conversations
- **MeetingNotificationService**: Envoyer les notifications de réunion
- **GoogleCalendarService**: Ajouter les événements au calendrier

### Services Externes
- **Mailtrap**: Envoi d'emails
- **Google Calendar API**: Gestion des réunions
- **OpenRouter API**: Résumé IA des conversations
- **WebRTC STUN Servers**: Établir les connexions P2P

## Prochaines Améliorations

- [ ] Notifications en temps réel (Socket.io / WebSocket)
- [ ] Présence des utilisateurs (statut en ligne)
- [ ] Lecture des messages (accusé de réception)
- [ ] Partage d'écran dans les appels
- [ ] Transcription audio vers texte
- [ ] Chiffrement E2E des messages
- [ ] Suppression programmée de messages
- [ ] Réactions aux messages (emojis)
- [ ] Mentions (@username)
- [ ] Gestion des images de profil dans les bulles de messages
