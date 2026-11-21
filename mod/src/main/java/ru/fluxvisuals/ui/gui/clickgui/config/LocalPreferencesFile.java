package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.AtomicFileWriter;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Language;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PreferencesData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

public class LocalPreferencesFile {
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public final Path filePath;
    public PreferencesData data = new PreferencesData();

    public void load() {
        if (Files.exists(this.filePath, new LinkOption[0])) {
            PreferencesData preferencesData;
            try {
                preferencesData = (PreferencesData) gson.fromJson(Files.readString(this.filePath, StandardCharsets.UTF_8), PreferencesData.class);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            if (preferencesData != null) {
                this.data = preferencesData;
                ensureCollections();
                applyLanguage();
                applyDpiScale();
            }
        }
    }

    public void save() throws IOException {
        ensureCollections();
        AtomicFileWriter.writeBytes(this.filePath, gson.toJson(this.data).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public boolean isModuleFavorite(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.favoriteModules.contains(str);
    }

    public void setModuleFavorite(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.favoriteModules.add(str);
        } else {
            this.data.favoriteModules.remove(str);
        }
    }

    public boolean isConfigFavorite(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.favoriteConfigs.contains(str);
    }

    public void setConfigFavorite(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.favoriteConfigs.add(str);
        } else {
            this.data.favoriteConfigs.remove(str);
        }
    }

    public void setLanguage(Language language313) {
        if (language313 == null) {
            return;
        }
        this.data.language = language313.name();
    }

    public void setDpiScale(boolean z, float f) {
        this.data.autoDpiScale = Boolean.valueOf(z);
        this.data.dpiScaleFactor = Float.valueOf(f);
    }

    public void applyLanguage() {
        if (this.data.language == null || this.data.language.isBlank()) {
            return;
        }
        try {
            FluxVisuals.INSTANCE.languages().language(Language.valueOf(this.data.language));
        } catch (IllegalArgumentException e) {
            FluxVisuals.LOGGER.warn("Unknown saved language: {}", this.data.language);
        }
    }

    public void applyDpiScale() {
        if (this.data.autoDpiScale == null && this.data.dpiScaleFactor == null) {
            return;
        }
        if (Boolean.TRUE.equals(this.data.autoDpiScale)) {
            FluxVisuals.INSTANCE.windowController().enableAutoDpiScale();
        } else if (this.data.dpiScaleFactor != null) {
            FluxVisuals.INSTANCE.windowController().setManualDpiScaleFactor(this.data.dpiScaleFactor.floatValue());
        }
    }

    public boolean isAutoSaveDisabled(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.autoSaveDisabled.contains(str);
    }

    public void setAutoSaveDisabled(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.autoSaveDisabled.add(str);
        } else {
            this.data.autoSaveDisabled.remove(str);
        }
    }

    public void ensureCollections() {
        if (this.data.favoriteModules == null) {
            this.data.favoriteModules = new HashSet();
        }
        if (this.data.favoriteConfigs == null) {
            this.data.favoriteConfigs = new HashSet();
        }
        if (this.data.autoSaveDisabled == null) {
            this.data.autoSaveDisabled = new HashSet();
        }
    }

    public LocalPreferencesFile(Path path) {
        this.filePath = path;
    }
}
