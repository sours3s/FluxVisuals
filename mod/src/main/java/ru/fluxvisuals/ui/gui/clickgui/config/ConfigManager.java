package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CredentialKey;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class ConfigManager {
    public final Path configDir;
    public final MacroStorage macrosConfig;

    public final FriendStore friendsConfig;

    public final LastConfigStore lastConfigIDConfig;

    public final WaypointStorage waysConfig;

    public final CredentialStore autoAuthConfig;

    public final SessionNicknameStore sessionNicknameConfig;

    public final LocalPreferencesFile menuStateConfig;

    public ScheduledExecutorService executor;

    public ConfigManager(Path path, WaysRepository waysRepository, MacroRepository macroRepository) throws IOException {
        this.configDir = path;
        Files.createDirectories(path, new FileAttribute[0]);
        this.macrosConfig = new MacroStorage(path.resolve("macros.json"), macroRepository);
        this.friendsConfig = new FriendStore(path.resolve("friends.json"));
        this.waysConfig = new WaypointStorage(path.resolve("ways.json"), waysRepository);
        this.lastConfigIDConfig = new LastConfigStore(path.resolve("lastConfigID.json"));
        this.menuStateConfig = new LocalPreferencesFile(path.resolve("menu-state.json"));
        this.autoAuthConfig = new CredentialStore(path.resolve("auto-auth.json"));
        this.sessionNicknameConfig = new SessionNicknameStore(path.resolve("session-nickname.json"));
    }

    public void saveMacros() throws IOException {
        this.macrosConfig.save();
    }

    public void loadMacros() throws IOException {
        this.macrosConfig.load();
    }

    public void saveLastConfigID() {
        this.lastConfigIDConfig.save();
    }

    public void loadLastConfigID() {
        this.lastConfigIDConfig.load();
    }

    public void saveWays() throws IOException {
        this.waysConfig.save();
    }

    public void loadWays() throws IOException {
        this.waysConfig.load();
    }

    public void saveFriends() throws IOException {
        this.friendsConfig.save();
    }

    public void loadFriends() throws IOException {
        this.friendsConfig.load();
    }

    public void saveMenuState() throws IOException {
        this.menuStateConfig.save();
    }

    public void loadMenuState() {
        this.menuStateConfig.load();
    }

    public void saveLocalConfig() {
        try {
            byte[] data = new ConfigSerializer().serialize(FluxVisuals.INSTANCE.moduleRepository(), FluxVisuals.INSTANCE.widgetStack());
            Files.write(this.configDir.resolve("local-config.json"), data);
        } catch (Exception e) {
            FluxVisuals.LOGGER.error("Failed to save local config", e);
        }
    }

    public void loadLocalConfig() {
        try {
            Path path = this.configDir.resolve("local-config.json");
            if (!Files.exists(path)) {
                return;
            }
            ConfigSerializer serializer = new ConfigSerializer();
            ConfigFile bundle = serializer.deserialize(Files.readAllBytes(path));
            serializer.applyConfig(new LoadedConfig(null, bundle));
            FluxVisuals.LOGGER.info("Loaded local config ({} modules)", bundle.modules() == null ? 0 : bundle.modules().size());
        } catch (Exception e) {
            FluxVisuals.LOGGER.error("Failed to load local config", e);
        }
    }

    public void saveAutoAuth(Map<CredentialKey, String> map) throws IOException {
        this.autoAuthConfig.saveCredentials(map);
    }

    public void saveSessionNickname() {
        this.sessionNicknameConfig.saveCurrentSessionNickname();
    }

    public String loadSessionNickname() {
        return this.sessionNicknameConfig.loadSessionNickname();
    }

    public Map<CredentialKey, String> loadAutoAuth() throws IOException {
        return this.autoAuthConfig.loadCredentials();
    }

    public Path configDir() {
        return this.configDir;
    }

    public MacroStorage macrosConfig() {
        return this.macrosConfig;
    }

    public FriendStore friendsConfig() {
        return this.friendsConfig;
    }

    public LastConfigStore lastConfigIDConfig() {
        return this.lastConfigIDConfig;
    }

    public WaypointStorage waysConfig() {
        return this.waysConfig;
    }

    public CredentialStore autoAuthConfig() {
        return this.autoAuthConfig;
    }

    public SessionNicknameStore sessionNicknameConfig() {
        return this.sessionNicknameConfig;
    }

    public ScheduledExecutorService executor() {
        return this.executor;
    }

    public LocalPreferencesFile menuStateConfig() {
        return this.menuStateConfig;
    }
}
