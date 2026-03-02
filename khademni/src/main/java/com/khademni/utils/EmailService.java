package com.khademni.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // Configuration SMTP
    private static final String SMTP_HOST = "smtp.gmail.com"; // Serveur SMTP
    private static final String SMTP_PORT = "587"; // Port pour TLS
    private static final String EMAIL_USERNAME = "votre_email@gmail.com"; // Remplacez par votre email
    private static final String EMAIL_PASSWORD = "votre_mot_de_passe"; // Remplacez par votre mot de passe ou mot de passe d'application

    /**
     * Envoie un email à un destinataire.
     *
     * @param to      L'adresse email du destinataire.
     * @param subject Le sujet de l'email.
     * @param body    Le contenu de l'email.
     * @throws MessagingException En cas d'erreur lors de l'envoi.
     */
    public static void sendEmail(String to, String subject, String body) throws MessagingException {
        // Configuration des propriétés SMTP
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        // Authentification
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
            }
        });

        // Création du message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_USERNAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        // Envoi de l'email
        Transport.send(message);

        System.out.println("Email envoyé avec succès à " + to);
    }
}