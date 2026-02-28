package com.khademni.utils;

import com.khademni.service.GrammarCorrectionService;
import com.khademni.service.GrammarCorrectionService.CorrectionMatch;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

import java.util.Comparator;
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
 * A debounce of 500 ms is applied so the API is only called once the user
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
    private static final long DEBOUNCE_MS = 500;

    /** Shared service instance. */
    private static final GrammarCorrectionService CORRECTION_SERVICE = GrammarCorrectionService.getInstance();

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

            // Schedule a new check after the debounce delay
            pending[0] = SCHEDULER.schedule(() -> {
                String current = newText;
                if (current == null || current.isBlank())
                    return;

                // Use GrammarCorrectionService.check() for granular match info
                List<CorrectionMatch> matches = CORRECTION_SERVICE.check(current, language);

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
                // offset order (so earlier offsets are not shifted).
                matches.sort(Comparator.comparingInt(CorrectionMatch::offset).reversed());
                StringBuilder sb = new StringBuilder(current);
                int fixCount = 0;
                for (CorrectionMatch m : matches) {
                    if (m.offset() < 0 || m.offset() + m.length() > sb.length())
                        continue;
                    sb.replace(m.offset(), m.offset() + m.length(), m.replacements().get(0));
                    fixCount++;
                }

                final String corrected = sb.toString();
                final int fc = fixCount;
                Platform.runLater(() -> {
                    // Only update if the text actually changed
                    if (!corrected.equals(field.getText())) {
                        // Preserve caret position as best we can
                        int caretPos = field.getCaretPosition();
                        field.setText(corrected);
                        field.positionCaret(Math.min(caretPos, corrected.length()));
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
