package com.khademni.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "5ademni.tn";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CALENDAR_ID = "haythem.benmessaoud003@gmail.com";
    private static final String CREDENTIALS_FILE_PATH = "/google-calendar-credentials.json";

    private Calendar calendarService;

    public GoogleCalendarService() throws IOException, GeneralSecurityException {
        initializeService();
    }

    private void initializeService() throws IOException, GeneralSecurityException {
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new IOException("❌ Fichier credentials introuvable");
        }

        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/calendar"));

        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        calendarService = new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        System.out.println("✅ Service Google Calendar initialisé");
    }

    public String createMeetingEvent(String titre, String description,
                                     LocalDateTime startDateTime, int durationMinutes) throws IOException {

        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

        Event event = new Event()
                .setSummary("📅 " + titre + " - 5ademni.tn")
                .setDescription(description + "\n\nRéunion planifiée via 5ademni.tn");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(startDateTime.format(formatter)))
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(endDateTime.format(formatter)))
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);

        Event createdEvent = calendarService.events().insert(CALENDAR_ID, event).execute();
        return createdEvent.getId();
    }
}