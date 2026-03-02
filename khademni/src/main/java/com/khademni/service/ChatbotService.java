package com.khademni.service;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'intelligence du chatbot.
 * Analyse les messages utilisateur via un moteur d'intentions rule-based
 * et produit des réponses contextuelles (stats, PDF, onboarding, sécurité).
 */
public class ChatbotService {

    // ── Intent Enum ──────────────────────────────────────────────────────
    public enum Intent {
        GREETING, STATS, PDF_REPORT, ONBOARDING, STRIPE_SECURITY, HELP, DEFAULT
    }

    // ── ChatResponse DTO ─────────────────────────────────────────────────
    public static class ChatResponse {
        private final String message;
        private final Intent intent;
        private final String pdfPath; // non-null only for PDF_REPORT
        private final int offresCount;
        private final int demandesCount;
        private final int cvCount;

        public ChatResponse(String message, Intent intent) {
            this(message, intent, null, 0, 0, 0);
        }

        public ChatResponse(String message, Intent intent, String pdfPath,
                int offresCount, int demandesCount, int cvCount) {
            this.message = message;
            this.intent = intent;
            this.pdfPath = pdfPath;
            this.offresCount = offresCount;
            this.demandesCount = demandesCount;
            this.cvCount = cvCount;
        }

        public String getMessage() {
            return message;
        }

        public Intent getIntent() {
            return intent;
        }

        public String getPdfPath() {
            return pdfPath;
        }

        public int getOffresCount() {
            return offresCount;
        }

        public int getDemandesCount() {
            return demandesCount;
        }

        public int getCvCount() {
            return cvCount;
        }
    }

    // ── Intent Detection ─────────────────────────────────────────────────

    /**
     * Analyse le texte utilisateur et détermine l'intention.
     */
    public Intent detectIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            return Intent.DEFAULT;
        String msg = userMessage.toLowerCase().trim();

        // PDF / Rapport
        if (containsAny(msg, "rapport", "pdf", "génère", "genere", "générer", "generer", "export", "exporter")) {
            return Intent.PDF_REPORT;
        }
        // Statistiques
        if (containsAny(msg, "combien", "statistique", "stats", "nombre", "total")) {
            return Intent.STATS;
        }
        // Onboarding / Freelancer guide
        if (containsAny(msg, "comment", "créer", "creer", "inscription", "freelancer",
                "devenir", "compte", "étape", "etape", "guide", "commencer", "démarrer", "demarrer")) {
            return Intent.ONBOARDING;
        }
        // Stripe / Sécurité paiement
        if (containsAny(msg, "sécurité", "securite", "sûr", "sur", "paiement", "stripe",
                "cryptage", "ssl", "sécurisé", "securise", "pci", "confiance", "fiable")) {
            return Intent.STRIPE_SECURITY;
        }
        // Aide
        if (containsAny(msg, "aide", "help", "menu", "commande", "option", "quoi faire", "fonctionnalité")) {
            return Intent.HELP;
        }
        // Greeting
        if (containsAny(msg, "bonjour", "salut", "hello", "hi", "bonsoir", "hey", "coucou")) {
            return Intent.GREETING;
        }
        return Intent.DEFAULT;
    }

    // ── Response Generation ──────────────────────────────────────────────

    /**
     * Produit une réponse structurée en fonction de l'intention détectée.
     */
    public ChatResponse processMessage(String userMessage) {
        Intent intent = detectIntent(userMessage);
        return switch (intent) {
            case GREETING -> handleGreeting();
            case STATS -> handleStats();
            case PDF_REPORT -> handlePdfReport();
            case ONBOARDING -> handleOnboarding();
            case STRIPE_SECURITY -> handleStripeSecurity();
            case HELP -> handleHelp();
            default -> handleDefault();
        };
    }

    // ── Greeting ─────────────────────────────────────────────────────────

    private ChatResponse handleGreeting() {
        UserModel user = App.getCurrentUser();
        String name = (user != null) ? user.getFirstName() : "ami(e)";
        String msg = "👋 Bonjour " + name + " ! Je suis votre assistant 5ademni.tn.\n\n"
                + "Voici ce que je peux faire pour vous :\n"
                + "📊  Statistiques — \"Combien d'offres ?\"\n"
                + "📄  Rapport PDF — \"Génère mon rapport\"\n"
                + "🎓  Guide Freelancer — \"Comment devenir freelancer ?\"\n"
                + "🔒  Sécurité Paiement — \"Le paiement est sûr ?\"\n\n"
                + "Comment puis-je vous aider ?";
        return new ChatResponse(msg, Intent.GREETING);
    }

    // ── Stats ────────────────────────────────────────────────────────────

    private ChatResponse handleStats() {
        int offres = 0, demandes = 0, cvCount = 0;
        try (Connection conn = MyDataBase.getConnection()) {
            offres = countQuery(conn, "SELECT COUNT(*) FROM offres");
            demandes = countQuery(conn, "SELECT COUNT(*) FROM demandes");
            cvCount = countQuery(conn, "SELECT COUNT(*) FROM users WHERE cv_path IS NOT NULL AND cv_path != ''");
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("❌ Désolé, je n'ai pas pu récupérer les statistiques. Veuillez réessayer.",
                    Intent.STATS);
        }

        String msg = "📊 **Statistiques en temps réel :**\n\n"
                + "📦  Offres créées : **" + offres + "**\n"
                + "📋  Demandes reçues : **" + demandes + "**\n"
                + "📄  CV ajoutés : **" + cvCount + "**\n\n"
                + "Ces données reflètent l'état actuel de la plateforme.";
        return new ChatResponse(msg, Intent.STATS, null, offres, demandes, cvCount);
    }

    private int countQuery(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── PDF Report ───────────────────────────────────────────────────────

    private ChatResponse handlePdfReport() {
        UserModel user = App.getCurrentUser();
        if (user == null) {
            return new ChatResponse("❌ Vous devez être connecté pour générer un rapport.", Intent.PDF_REPORT);
        }

        // Gather data
        int offresCount = 0, demandesCount = 0;
        List<String[]> recentOffres = new ArrayList<>();
        List<String[]> recentDemandes = new ArrayList<>();

        try (Connection conn = MyDataBase.getConnection()) {
            // Counts
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM offres WHERE user_id = ?")) {
                ps.setInt(1, user.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    offresCount = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM demandes WHERE user_id = ?")) {
                ps.setInt(1, user.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    demandesCount = rs.getInt(1);
            }
            // Recent offres (last 5)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT titre, statut, date_creation FROM offres WHERE user_id = ? ORDER BY date_creation DESC LIMIT 5")) {
                ps.setInt(1, user.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    recentOffres.add(new String[] {
                            rs.getString("titre"), rs.getString("statut"),
                            rs.getTimestamp("date_creation").toLocalDateTime()
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    });
                }
            }
            // Recent demandes (last 5)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT titre, statut, date_creation FROM demandes WHERE user_id = ? ORDER BY date_creation DESC LIMIT 5")) {
                ps.setInt(1, user.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    recentDemandes.add(new String[] {
                            rs.getString("titre"), rs.getString("statut"),
                            rs.getTimestamp("date_creation").toLocalDateTime()
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("❌ Erreur lors de la récupération des données.", Intent.PDF_REPORT);
        }

        // Generate PDF
        String fileName = "Rapport_5ademni_" + user.getUniqueId() + ".pdf";
        String dest = System.getProperty("user.home") + "/Downloads/" + fileName;
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, fos);
            doc.open();

            // Fonts
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(108, 13, 242));
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(51, 51, 51));
            Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(80, 80, 80));
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(51, 51, 51));

            // Title
            Paragraph title = new Paragraph("5ademni.tn — Rapport Personnel", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            doc.add(title);

            // Date
            Paragraph date = new Paragraph("Généré le : "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    normalFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            doc.add(date);

            doc.add(new LineSeparator());
            doc.add(Chunk.NEWLINE);

            // User Info
            doc.add(new Paragraph("Informations Utilisateur", headerFont));
            doc.add(new Paragraph("Nom : " + user.getFirstName() + " " + user.getLastName(), normalFont));
            doc.add(new Paragraph("Email : " + user.getEmail(), normalFont));
            doc.add(new Paragraph("ID Unique : " + user.getUniqueId(), normalFont));
            doc.add(new Paragraph("Solde : " + String.format("%.2f", user.getBalance()) + " DT", normalFont));
            doc.add(Chunk.NEWLINE);

            // Summary
            doc.add(new Paragraph("Résumé de l'Activité", headerFont));
            doc.add(new Paragraph("Offres créées : " + offresCount, boldFont));
            doc.add(new Paragraph("Demandes créées : " + demandesCount, boldFont));
            doc.add(Chunk.NEWLINE);

            // Recent offres
            if (!recentOffres.isEmpty()) {
                doc.add(new Paragraph("Dernières Offres :", headerFont));
                for (String[] o : recentOffres) {
                    doc.add(new Paragraph("  • " + o[0] + " — " + o[1] + " (" + o[2] + ")", normalFont));
                }
                doc.add(Chunk.NEWLINE);
            }

            // Recent demandes
            if (!recentDemandes.isEmpty()) {
                doc.add(new Paragraph("Dernières Demandes :", headerFont));
                for (String[] d : recentDemandes) {
                    doc.add(new Paragraph("  • " + d[0] + " — " + d[1] + " (" + d[2] + ")", normalFont));
                }
                doc.add(Chunk.NEWLINE);
            }

            doc.add(new LineSeparator());
            Paragraph footer = new Paragraph("© 5ademni.tn — Tous droits réservés", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            doc.add(footer);

            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("❌ Erreur lors de la génération du PDF : " + e.getMessage(), Intent.PDF_REPORT);
        }

        String msg = "✅ Votre rapport a été généré avec succès !\n\n"
                + "📁 Emplacement : Downloads/" + fileName + "\n\n"
                + "Le rapport contient :\n"
                + "• Vos informations personnelles\n"
                + "• " + offresCount + " offre(s) créée(s)\n"
                + "• " + demandesCount + " demande(s) créée(s)\n"
                + "• Vos 5 dernières activités";
        return new ChatResponse(msg, Intent.PDF_REPORT, dest, offresCount, demandesCount, 0);
    }

    // ── Onboarding ───────────────────────────────────────────────────────

    private ChatResponse handleOnboarding() {
        String msg = "🎓 **Guide : Comment utiliser 5ademni.tn**\n\n"
                + "**Étape 1 — Créer un compte :**\n"
                + "Rendez-vous sur la page d'inscription, remplissez vos informations (nom, email, mot de passe) et validez.\n\n"
                + "**Étape 2 — Compléter votre profil :**\n"
                + "Ajoutez votre photo, votre bio et vos informations de contact.\n\n"
                + "**Étape 3 — Devenir Freelancer :**\n"
                + "Pour devenir Freelancer, vous devez :\n"
                + "   1. Aller dans l'espace Freelancer\n"
                + "   2. Déposer votre CV (format PDF recommandé)\n"
                + "   3. Attendre la validation de votre profil\n"
                + "   4. Une fois validé, vous pourrez créer des offres de service !\n\n"
                + "**Étape 4 — Explorer les opportunités :**\n"
                + "Parcourez les offres et demandes disponibles dans la section Jobs & Services.\n\n"
                + "💡 Astuce : Un profil complet avec photo et bio attire plus de clients !";
        return new ChatResponse(msg, Intent.ONBOARDING);
    }

    // ── Stripe Security ──────────────────────────────────────────────────

    private ChatResponse handleStripeSecurity() {
        String msg = "🔒 **Sécurité des Paiements — 5ademni.tn**\n\n"
                + "Vos paiements sont **100% sécurisés**. Voici pourquoi :\n\n"
                + "🛡️ **Stripe** — Notre partenaire de paiement est Stripe, leader mondial utilisé par des millions d'entreprises (Amazon, Google, Shopify...).\n\n"
                + "🔐 **Cryptage SSL/TLS** — Toutes les transactions sont protégées par un cryptage SSL 256 bits. "
                + "Vos données bancaires sont chiffrées de bout en bout.\n\n"
                + "✅ **Conformité PCI DSS** — Stripe est certifié PCI DSS Niveau 1, le plus haut niveau de certification "
                + "de sécurité dans l'industrie des paiements.\n\n"
                + "🚫 **Aucune donnée stockée** — Nous ne stockons JAMAIS vos informations bancaires. "
                + "Elles sont gérées exclusivement par Stripe.\n\n"
                + "📧 **Support** — En cas de problème, notre équipe est disponible pour vous assister.\n\n"
                + "Vous pouvez effectuer vos transactions en toute confiance ! 💪";
        return new ChatResponse(msg, Intent.STRIPE_SECURITY);
    }

    // ── Help ─────────────────────────────────────────────────────────────

    private ChatResponse handleHelp() {
        String msg = "🤖 **Commandes disponibles :**\n\n"
                + "📊  \"Combien d'offres ?\" — Voir les statistiques\n"
                + "📄  \"Génère mon rapport\" — Créer un PDF récapitulatif\n"
                + "🎓  \"Comment devenir freelancer ?\" — Guide d'inscription\n"
                + "🔒  \"Le paiement est sûr ?\" — Infos sécurité Stripe\n\n"
                + "Vous pouvez aussi me poser des questions en langage naturel ! 😊";
        return new ChatResponse(msg, Intent.HELP);
    }

    // ── Default ──────────────────────────────────────────────────────────

    private ChatResponse handleDefault() {
        String msg = "🤔 Je ne suis pas sûr de comprendre votre demande.\n\n"
                + "Essayez l'une de ces commandes :\n"
                + "• \"Combien d'offres ?\" — Statistiques\n"
                + "• \"Génère mon rapport\" — Export PDF\n"
                + "• \"Comment devenir freelancer ?\" — Guide\n"
                + "• \"Le paiement est sûr ?\" — Sécurité\n"
                + "• \"Aide\" — Voir toutes les commandes";
        return new ChatResponse(msg, Intent.DEFAULT);
    }

    // ── Util ─────────────────────────────────────────────────────────────

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw))
                return true;
        }
        return false;
    }
}
