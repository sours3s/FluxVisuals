package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.AtomicFileWriter;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ConfigReference;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;

public class LastConfigStore {
    public final Path filePath;
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void save() {
        if (FluxVisuals.INSTANCE.cloudConfigService().initialized()) {
            Optional<ConfigReference> optionalActiveConfig = FluxVisuals.INSTANCE.cloudConfigService().activeConfig();
            if (optionalActiveConfig.isEmpty()) {
                try {
                    Files.deleteIfExists(this.filePath);
                    return;
                } catch (IOException e) {
                    FluxVisuals.LOGGER.error("Failed to delete last config file", e);
                    return;
                }
            }
            byte[] bytes = gson.toJson(optionalActiveConfig.get()).getBytes(StandardCharsets.UTF_8);
            try {
                if (this.filePath.getParent() != null) {
                    Files.createDirectories(this.filePath.getParent(), new FileAttribute[0]);
                }
                AtomicFileWriter.writeBytes(this.filePath, bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            } catch (IOException e2) {
                FluxVisuals.LOGGER.error("Failed to save last config ID", e2);
            }
        }
    }

    public void load() {
        if (Files.exists(this.filePath, new LinkOption[0]) && FluxVisuals.INSTANCE.cloudConfigService().initialized()) {
            try {
                String strTrim = Files.readString(this.filePath, StandardCharsets.UTF_8).trim();
                if (strTrim.isBlank() || !strTrim.startsWith("{")) {
                    Files.deleteIfExists(this.filePath);
                    return;
                }
                ConfigReference configReference = (ConfigReference) gson.fromJson(strTrim, ConfigReference.class);
                if (configReference == null || configReference.id() == null) {
                    Files.deleteIfExists(this.filePath);
                } else {
                    FluxVisuals.INSTANCE.cloudConfigService().downloadConfig(configReference.id()).thenAccept(loadedConfig -> {
                        FluxVisuals.INSTANCE.cloudConfigService().applyConfig(loadedConfig);
                        FluxVisuals.LOGGER.info("Config applied: {} by {}", loadedConfig.details().name(), loadedConfig.details().author());
                    }).exceptionally(th -> {
                        FluxVisuals.LOGGER.error("Failed to load/apply config", th);
                        return null;
                    });
                }
            } catch (JsonSyntaxException e) {
                FluxVisuals.LOGGER.warn("Invalid config format, resetting...");
                try {
                    Files.deleteIfExists(this.filePath);
                } catch (IOException e2) {
                }
            } catch (IOException e3) {
                FluxVisuals.LOGGER.error("Failed to read config file", e3);
            }
        }
    }

    public LastConfigStore(Path path) {
        this.filePath = path;
    }
}
