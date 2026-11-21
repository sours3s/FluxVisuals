package ru.fluxvisuals.ui.gui.clickgui;

import ru.fluxvisuals.ui.gui.clickgui.util.annotation.Aliases;
import ru.fluxvisuals.ui.gui.clickgui.config.ConfigFile;
import ru.fluxvisuals.ui.gui.clickgui.config.ConfigManager;
import ru.fluxvisuals.ui.gui.clickgui.config.ConfigSerializer;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.config.HandlerRepository;
import ru.fluxvisuals.ui.gui.clickgui.config.MacroRepository;
import ru.fluxvisuals.ui.gui.clickgui.config.ListenerRepository;
import ru.fluxvisuals.ui.gui.clickgui.config.ModuleRepository;
import ru.fluxvisuals.ui.gui.clickgui.config.NotificationRepository;
import ru.fluxvisuals.ui.gui.clickgui.util.TabsController;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Theme;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ThemeData;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ThemeMode;
import ru.fluxvisuals.ui.gui.clickgui.config.WaysRepository;
import ru.fluxvisuals.ui.gui.clickgui.util.WindowController;
import ru.fluxvisuals.ui.gui.clickgui.util.render.WindowControllerAdapter;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.fluxvisuals.module.api.Manager;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigService;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigOrigin;
import ru.fluxvisuals.ui.gui.clickgui.util.type.UserSession;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.util.EventDispatcher;

public class FluxVisuals {
    public static final Logger LOGGER = LoggerFactory.getLogger(FluxVisuals.class);
    public static FluxVisuals INSTANCE;
    public final WidgetStack widgetStack = new WidgetStack();

    public final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() / 2), runnable -> {
        Thread thread = new Thread(runnable, "fluxvisuals-worker");
        thread.setDaemon(true);
        return thread;
    });

    public final WaysRepository waysRepository = new WaysRepository();

    public final WindowController windowController = new WindowController();

    public final WindowControllerAdapter windowControllerAdapter = new WindowControllerAdapter(this.windowController);

    public final TabsController tabsController = new TabsController();

    public final boolean devMode = detectDevMode();

    public ConfigManager configManager;

    public final MacroRepository macroRepository;

    public final HandlerRepository handlerRepository;

    public final NotificationRepository notificationRepository;

    public final ModuleRepository moduleRepository;
    public CloudConfigService cloudConfigService = new CloudConfigService();
    public UserSession userSessionInstance;

    public final ListenerRepository listenerRepository;

    public final EventDispatcher eventDispatcher = new EventDispatcher();

    public ConfigFile defaultConfigFile;

    public final Lang languages;

    public DrawEngine drawEngine;

    public MenuWindow menuWindow;

    public Theme theme;
    public boolean viaLoaded;

    private final Manager moduleManager;

    public FluxVisuals() throws NoSuchAlgorithmException, InvalidKeyException {
        if (INSTANCE != null) {
            throw new IllegalStateException(getClass().getName() + " already initialized!");
        }
        INSTANCE = this;
        this.languages = new Lang();
        this.languages.primaryLanguage();
        this.macroRepository = new MacroRepository();
        this.notificationRepository = new NotificationRepository();
        this.listenerRepository = new ListenerRepository();
        this.handlerRepository = new HandlerRepository();
        // ClientInfo removed - visual client only
        initConfigs();

        // Initialize user's module manager and wrap for clickgui
        this.moduleManager = new Manager();
        List<Supplier<ClickGuiModule>> moduleSuppliers = moduleManager.getModules().stream()
                .map(m -> (Supplier<ClickGuiModule>) () -> new ClickGuiModuleAdapter(m))
                .collect(Collectors.toList());
        this.moduleRepository = new ModuleRepository(moduleSuppliers);

        captureDefaultConfig();
        setupMenu();
        this.configManager.loadLocalConfig();
        this.executor.scheduleAtFixedRate(this.configManager::saveLocalConfig, 30L, 30L, TimeUnit.SECONDS);
        registerShutdownHook();
        try {
            Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
            if (com.viaversion.viafabricplus.ViaFabricPlus.getImpl() != null) {
                this.viaLoaded = true;
            }
        } catch (ClassNotFoundException e2) {
        }
    }

    public void captureDefaultConfig() {
        try {
            ConfigSerializer configSerializer = new ConfigSerializer();
            this.defaultConfigFile = configSerializer.deserialize(configSerializer.serialize(this.moduleRepository, this.widgetStack));
        } catch (IOException e) {
            LOGGER.error("Failed to capture default config", e);
        }
    }

    public void setupMenu() {
        this.menuWindow = new MenuWindow();
        this.windowController.newWindow(this.menuWindow);
        this.theme = ThemeData.defaultDark();

        // Map user's module categories to clickgui tabs
        Map<ModuleTab, MenuTabElement> tabMap = Map.of(
            ModuleTab.COMBAT, this.tabsController.combat(),
            ModuleTab.MOVEMENT, this.tabsController.movement(),
            ModuleTab.PLAYER, this.tabsController.player(),
            ModuleTab.RENDER, this.tabsController.render(),
            ModuleTab.MISC, this.tabsController.misc()
        );

        // Use moduleManager to get modules and wrap them with adapter
        for (Module module : moduleManager.getModules()) {
            ClickGuiModuleAdapter adapter = new ClickGuiModuleAdapter(module);
            ModuleTab tab = adapter.getModuleTab();
            MenuTabElement menuTab = tabMap.get(tab);
            if (menuTab != null) {
                String[] aliases = module.getClass().isAnnotationPresent(Aliases.class)
                    ? ((Aliases) module.getClass().getAnnotation(Aliases.class)).aliases()
                    : new String[0];
                menuTab.newFrame(new ModuleCard(adapter, module, aliases));
            }
        }

        ConfigTabLayout configTabLayout = new ConfigTabLayout();
        this.tabsController.config().setRenderStrategy(configTabLayout);
        configTabLayout.updateConfigList();
        this.tabsController.theme().setRenderStrategy(new ThemeTabLayout());
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < 25; i++) {
            int iNextInt = ThreadLocalRandom.current().nextInt(8);
            boolean zNextBoolean = ThreadLocalRandom.current().nextBoolean();
            arrayList.add(new ThemeCardFrame(new ThemeCard(Theme.of("id" + iNextInt, "Element" + iNextInt, zNextBoolean ? "owner" + iNextInt : "FluxVisuals", zNextBoolean ? ConfigOrigin.OFFICIAL : ConfigOrigin.USER, zNextBoolean ? ThemeMode.DARK : ThemeMode.LIGHT, ThemeData.defaultDark().palette(), Instant.now(), Instant.now()), null)));
        }
        this.tabsController.theme().updateFramesSafe(arrayList);
    }

    public void initConfigs() {
        try {
            this.configManager = new ConfigManager(Path.of("fluxvisuals/config", new String[0]), this.waysRepository, this.macroRepository);
            this.configManager.loadMacros();
            this.configManager.loadFriends();
            this.configManager.loadWays();
            this.configManager.loadMenuState();
            this.configManager.loadSessionNickname();
        } catch (Exception e) {
            LOGGER.error("Failed to init configs", e);
        }
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(5L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                this.configManager.saveMacros();
                this.configManager.saveWays();
                this.configManager.saveMenuState();
                this.configManager.saveLocalConfig();
            } catch (Exception e2) {
                LOGGER.error("Failed to save on shutdown", e2);
            }
        }));
    }

    public boolean devMode() {
        return this.devMode;
    }

    public static boolean detectDevMode() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(str -> {
            return str.contains("-Ddebugger.agent.enable.coroutines=true");
        });
    }

    public WidgetStack widgetStack() {
        return this.widgetStack;
    }

    public ScheduledExecutorService executor() {
        return this.executor;
    }

    public WaysRepository waysRepository() {
        return this.waysRepository;
    }

    public WindowController windowController() {
        return this.windowController;
    }

    public WindowControllerAdapter windowControllerAdapter() {
        return this.windowControllerAdapter;
    }

    public TabsController tabsController() {
        return this.tabsController;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public MacroRepository macroRepository() {
        return this.macroRepository;
    }

    public HandlerRepository handlerRepository() {
        return this.handlerRepository;
    }

    public NotificationRepository notificationRepository() {
        return this.notificationRepository;
    }

    public ModuleRepository moduleRepository() {
        return this.moduleRepository;
    }

    public ListenerRepository listenerRepository() {
        return this.listenerRepository;
    }

    public EventDispatcher eventDispatcher() {
        return this.eventDispatcher;
    }

    public ConfigFile defaultConfigFile() {
        return this.defaultConfigFile;
    }

    public Lang languages() {
        return this.languages;
    }

    public DrawEngine drawEngine() {
        return this.drawEngine;
    }

    public MenuWindow menuWindow() {
        return this.menuWindow;
    }

    public Theme theme() {
        return this.theme;
    }

    public boolean viaLoaded() {
        return this.viaLoaded;
    }

    public FluxVisuals drawEngine(DrawEngine drawEngine154) {
        this.drawEngine = drawEngine154;
        return this;
    }

    public Manager getModuleManager() {
        return moduleManager;
    }

    public CloudConfigService cloudConfigService() {
        return this.cloudConfigService;
    }

    public UserSession userSession() {
        if (this.userSessionInstance == null) {
            String username = "Player";
            try {
                if (Mc.INSTANCE.getPlayer() != null) {
                    username = Mc.INSTANCE.getPlayer().getGameProfile().name();
                } else {
                    username = net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername();
                }
            } catch (Exception e) {
                FluxVisuals.LOGGER.warn("Failed to resolve session username", e);
            }
            this.userSessionInstance = new UserSession(username);
        }
        return this.userSessionInstance;
    }
}