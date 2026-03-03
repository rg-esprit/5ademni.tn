package com.khademni.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class IASummaryService {

    // Version locale qui fonctionne à coup sûr
    public static String generateSummary(List<String> messages) {
        return generateLocalSummary(messages);
    }

    public static String generateLocalSummary(List<String> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("📊 RÉSUMÉ COMPLET DE LA CONVERSATION\n");
        summary.append("════════════════════════════════════\n\n");

        // === STATISTIQUES GÉNÉRALES ===
        summary.append("📈 STATISTIQUES\n");
        summary.append("────────────────\n");
        summary.append("• Total messages: ").append(messages.size()).append("\n");

        // === COMPTAGE PAR EXPÉDITEUR ===
        Map<String, Integer> senderCount = new HashMap<>();
        Map<String, List<String>> messagesBySender = new HashMap<>();

        for (String msg : messages) {
            String[] parts = msg.split(":", 2);
            String sender = parts[0].replace("[", "").replace("]", "").trim();
            String content = parts.length > 1 ? parts[1].trim() : "";

            senderCount.put(sender, senderCount.getOrDefault(sender, 0) + 1);

            if (!messagesBySender.containsKey(sender)) {
                messagesBySender.put(sender, new ArrayList<>());
            }
            messagesBySender.get(sender).add(content);
        }

        summary.append("\n👥 PARTICIPANTS\n");
        summary.append("────────────────\n");
        for (Map.Entry<String, Integer> entry : senderCount.entrySet()) {
            summary.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" messages\n");
        }

        // === RÉSUMÉ PAR PERSONNE ===
        summary.append("\n💬 RÉSUMÉ PAR PERSONNE\n");
        summary.append("──────────────────────\n");

        for (Map.Entry<String, List<String>> entry : messagesBySender.entrySet()) {
            String sender = entry.getKey();
            List<String> senderMsgs = entry.getValue();

            summary.append("\n🔹 ").append(sender.toUpperCase()).append("\n");

            if (senderMsgs.size() <= 5) {
                for (int i = 0; i < senderMsgs.size(); i++) {
                    String msg = senderMsgs.get(i);
                    if (msg.length() > 50) {
                        msg = msg.substring(0, 47) + "...";
                    }
                    summary.append("   ").append(i+1).append(". ").append(msg).append("\n");
                }
            } else {
                summary.append("   Premiers messages:\n");
                for (int i = 0; i < 3; i++) {
                    String msg = senderMsgs.get(i);
                    if (msg.length() > 50) msg = msg.substring(0, 47) + "...";
                    summary.append("      • ").append(msg).append("\n");
                }
                summary.append("      ...\n");
                summary.append("   Derniers messages:\n");
                for (int i = senderMsgs.size() - 3; i < senderMsgs.size(); i++) {
                    String msg = senderMsgs.get(i);
                    if (msg.length() > 50) msg = msg.substring(0, 47) + "...";
                    summary.append("      • ").append(msg).append("\n");
                }
            }
        }

        // === DERNIERS ÉCHANGES ===
        summary.append("\n⏱️ DERNIERS ÉCHANGES\n");
        summary.append("────────────────────\n");
        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            String msg = messages.get(i);
            if (msg.length() > 60) {
                msg = msg.substring(0, 57) + "...";
            }
            summary.append("• ").append(msg).append("\n");
        }

        // === STATISTIQUES DES FICHIERS ===
        long audioCount = messages.stream().filter(m -> m.contains("🎤")).count();
        long imageCount = messages.stream().filter(m -> m.contains("📷")).count();
        long fileCount = messages.stream().filter(m -> m.contains("📎")).count();

        if (audioCount > 0 || imageCount > 0 || fileCount > 0) {
            summary.append("\n📎 FICHIERS PARTAGÉS\n");
            summary.append("────────────────────\n");
            if (audioCount > 0) summary.append("• Messages vocaux: ").append(audioCount).append("\n");
            if (imageCount > 0) summary.append("• Images: ").append(imageCount).append("\n");
            if (fileCount > 0) summary.append("• Documents: ").append(fileCount).append("\n");
        }

        return summary.toString();
    }
}