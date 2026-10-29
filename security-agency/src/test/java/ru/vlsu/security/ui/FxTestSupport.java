package ru.vlsu.security.ui;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/** Запуск кода на потоке JavaFX и снимки экранов в target/ui-snapshots. */
public final class FxTestSupport {

    private static boolean started;

    private FxTestSupport() {
    }

    public static synchronized void startFx() {
        if (!started) {
            Platform.startup(() -> Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()));
            Platform.setImplicitExit(false);
            started = true;
        }
    }

    public static <T> T onFx(Callable<T> action) throws Exception {
        startFx();
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                f.complete(action.call());
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f.get(30, TimeUnit.SECONDS);
    }

    /** Кладёт корень в сцену 1280×720 с app.css и сохраняет PNG. Вызывать на потоке FX. */
    public static Scene snapshot(Parent root, String name) throws Exception {
        Scene scene = root.getScene() != null ? root.getScene() : new Scene(root, 1280, 720);
        scene.getStylesheets().add(FxTestSupport.class.getResource("/ru/vlsu/security/ui/app.css").toExternalForm());
        root.applyCss();
        root.layout();
        WritableImage img = scene.snapshot(null);
        Path dir = Path.of("target", "ui-snapshots");
        Files.createDirectories(dir);
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", dir.resolve(name + ".png").toFile());
        return scene;
    }
}
