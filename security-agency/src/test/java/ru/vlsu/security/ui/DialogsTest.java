package ru.vlsu.security.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;

class DialogsTest {

    /** Ошибка БД во время таймера (автообновление, отсчёт ГБР) не должна ронять поток JavaFX. */
    @Test
    void error_fromTimeline_doesNotThrow() throws Exception {
        FxTestSupport.startFx();
        CompletableFuture<Throwable> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(10), e -> {
                try {
                    Dialogs.error("Сбой");
                    result.complete(null);
                } catch (Throwable ex) {
                    result.complete(ex);
                }
            }));
            t.play();
        });
        Throwable thrown = result.get(10, TimeUnit.SECONDS);
        FxTestSupport.onFx(() -> {
            for (Window w : List.copyOf(Window.getWindows())) {
                if (w instanceof Stage s) {
                    s.close();
                }
            }
            return null;
        });
        assertNull(thrown);
    }
}
