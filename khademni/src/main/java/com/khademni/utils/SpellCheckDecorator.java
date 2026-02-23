package com.khademni.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Attaches a real-time LanguageTool spell/grammar check to any JavaFX
 * {@link TextInputControl} (TextField or TextArea).
 *
 * <p>
 * A debounce of 1.5 s is applied so the API is only called once the user
 * pauses typing — this keeps us well within the free-tier rate limit.
 * </p>
 *
 * <p>
 * All network calls are made on a background thread; all UI updates are
 * dispatched via {@link Platform#runLater(Runnable)}.
 * </p>
 */
public class SpellCheckDecorator {

    /** Shared executor — one daemon thread is enough for all fields. */
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "spellcheck-debounce");
        t.setDaemon(true);
        return t;
    });

    /** Millis to wait after the last keystroke before calling the API. */
    private static final long DEBOUNCE_MS = 1500;

    /**
     * Attaches a live spell-check listener to a text field.
     *
     * @param field       the field to watch (TextField or TextArea)
     * @param statusLabel an optional label to display the correction hint
     *                    (may be {@code null})
     * @param language    BCP47 language code passed to LanguageTool (e.g.
     *                    {@code "fr"})
     */
    public static void attach(TextInputControl field, Label statusLabel, String language) {
        // Holds the pending debounce task
        final ScheduledFuture<?>[] pending = { null };

        field.textProperty().addListener((obs, oldText, newText) -> {
            // Cancel any previously scheduled check
            if (pending[0] != null && !pending[0].isDone()) {
                pending[0].cancel(false);
            }

            // Schedule a new check 1.5 s from now
            pending[0] = SCHEDULER.schedule(() -> {
                String current = newText;
                if (current == null || current.isBlank())
                    return;

                List<LanguageToolService.Match> matches = LanguageToolService.check(current, language);

                if (matches.isEmpty()) {
                    // Nothing to fix — optionally clear the status
                    if (statusLabel != null) {
                        Platform.runLater(() -> {
                            statusLabel.setText("");
                            statusLabel.setVisible(false);
                            statusLabel.setManaged(false);
                        });
                    }
                    return;
                }

                // Build the corrected text by applying suggestions in reverse
                // order (so earlier offsets are not shifted by replacements).
                StringBuilder sb = new StringBuilder(current);
                // Sort matches by offset descending
                matches.sort((a, b) -> Integer.compare(b.offset, a.offset));
                int fixCount = 0;
                for (LanguageToolService.Match m : matches) {
                    if (m.offset < 0 || m.offset + m.length > sb.length())
                        continue;
                    sb.replace(m.offset, m.offset + m.length, m.replacements.get(0));
                    fixCount++;
                }

                final String corrected = sb.toString();
                final int fc = fixCount;
                Platform.runLater(() -> {
                    // Only update if the text actually changed
                    if (!corrected.equals(field.getText())) {
                        // Preserve caret at end to avoid jarring jumps
                        field.setText(corrected);
                        field.positionCaret(corrected.length());
                    }
                    if (statusLabel != null && fc > 0) {
                        statusLabel.setText("✔ " + fc + " correction(s) appliquée(s)");
                        statusLabel.setStyle(
                                "-fx-text-fill: #059669; -fx-font-size: 11; -fx-font-style: italic;");
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);

                        // Auto-hide the hint after 3 s
                        SCHEDULER.schedule(() -> Platform.runLater(() -> {
                            statusLabel.setVisible(false);
                            statusLabel.setManaged(false);
                        }), 3, TimeUnit.SECONDS);
                    }
                });

            }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        });
    }

    /** Convenience overload with no status label. */
    public static void attach(TextInputControl field, String language) {
        attach(field, null, language);
    }
}
