package ru.fluxvisuals.client;

import java.io.File;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fluxvisuals.license.BlockedScreen;
import ru.fluxvisuals.license.LaunchGate;
import ru.fluxvisuals.cfg.ConfigManager;
import ru.fluxvisuals.commands.CommandBootstrap;
import ru.fluxvisuals.config.GuiManager;
import ru.fluxvisuals.config.StyleConfig;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.RenderHandler;
import ru.fluxvisuals.event.render.EventRender2D;
import ru.fluxvisuals.event.render.RenderEvent;
import ru.fluxvisuals.module.api.Manager;
import ru.fluxvisuals.module.bind.BindingManager;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.ui.gui.GuiClient;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.render.animation.AnimationSystem;
import ru.fluxvisuals.util.render.backends.gl.GlBackend;
import ru.fluxvisuals.util.render.backends.gl.GlState;
import ru.fluxvisuals.util.render.texture.TextureLoader;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontObject;
import ru.fluxvisuals.util.render.text.FontRegistry;
import ru.fluxvisuals.ui.gui.widget.settings.TooltipRegistrar;

/**
 * Точка входа FluxVisuals (клиент). Чисто клиентская визуальная отрисовка:
 * ни один пакет не отправляется и не модифицируется — только легитные визуальные модули.
 */
@Environment(EnvType.CLIENT)
public class FluxVisualsClient implements ClientModInitializer {
   public static final String MOD_ID = "fluxvisuals";
   public static final String NAME = "FluxVisuals";
   public static final String VERSION = "1.0.0";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static FluxVisualsClient get;
   public Manager manager;
   public ConfigManager configManager;
   public FriendManager friendManager;
   public GuiManager guiManager;
   public GuiClient guiClient;
   public File root;
   public final String rootRes = "fluxvisuals";

   private static GlBackend backend;
   private static Renderer2D renderer;
   private static FontObject uiFont;
   private static volatile boolean initialized = false;
   private static volatile boolean modInitialized = false;

   public static Renderer2D getRenderer() {
      ensureRendererInitialized();
      return renderer;
   }

   public static boolean isModInitialized() {
      return modInitialized;
   }

   @Override
   public void onInitializeClient() {
      LOGGER.info("{} client initializing...", NAME);

      // Защита: мод работает только если запущен через лоадер FluxVisuals
      // (одноразовый RSA-подписанный launch-тикет). Скопированный в TLauncher jar не работает.
      if (!LaunchGate.verify()) {
         LOGGER.warn("{} лицензия не подтверждена — мод запущен не через лоадер. Визуалы отключены.", NAME);
         showBlockedMessage();
         return; // не инициализируем: модули, HUD, команды, конфиг
      }

      get = this; // ТОЛЬКО после успешной верификации

      MinecraftClient mc = MinecraftClient.getInstance();
      this.root = new File(mc.runDirectory, "fluxvisuals");

      loadLoaderConfig(mc.runDirectory);

      this.manager = new Manager();
      this.friendManager = new FriendManager();
      this.configManager = new ConfigManager();
      FriendManager.init();
      this.guiManager = new GuiManager();
      this.guiManager.init();
      GuiScreen.selectedTheme = this.guiManager.getCurrentTheme();
      GuiScreen.preSelectedTheme = this.guiManager.getCurrentTheme();
      GuiScreen.selectedCategories = this.guiManager.getCurrentCategory();

      CommandBootstrap.initialize();
      BindingManager.getInstance().initialize();
      ru.fluxvisuals.ui.gui.widget.settings.TooltipManager.registerAll();

      if (this.configManager != null) {
         this.configManager.load();
         if (this.configManager.findConfig("default") != null) {
            this.configManager.loadConfig("default");
         }
      }

      this.guiClient = new GuiClient();
      GuiClient.registerEventHandlers();
      // Register Dynamic Lights world renderer
      try {
         EventManager.register(new ru.fluxvisuals.module.impl.visuals.DynamicLightsWorldRenderer());
      } catch (Exception ignored) {}
      // Register Motion Blur renderer
      try {
         EventManager.register(new ru.fluxvisuals.module.impl.visuals.MotionBlurRenderer());
      } catch (Exception ignored) {}
      EventManager.register(this);

      // Register all tooltips for ClickGUI
      TooltipRegistrar.registerAll();

      modInitialized = true;
      LOGGER.info("{} client initialized ({} modules)", NAME, manager.getModules().size());
   }

   /** Показывает сообщение о запуске только через лоадер (в игре — в чат, в меню — экран-заглушку). */
   private static void showBlockedMessage() {
      Thread t = new Thread(() -> {
         try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
         try {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
               try {
                  if (mc.currentScreen instanceof BlockedScreen) return;
                  if (mc.player != null) {
                     mc.player.sendMessage(Text.literal("§cFluxVisuals: клиент запускается только через лоадер FluxVisuals."), false);
                  } else {
                     mc.setScreen(new BlockedScreen());
                  }
               } catch (Exception ignored) {}
            });
         } catch (Exception ignored) {}
      }, "flux-blocked-message");
      t.setDaemon(true);
      t.start();
   }

   public static void ensureRendererInitialized() {
      if (!initialized) {
         onInit();
      }
   }

   private static synchronized void onInit() {
      if (!initialized) {
         backend = new GlBackend();
         renderer = new Renderer2D(backend);
         FontRegistry.initialize(backend, renderer);
         TextureLoader.initialize(backend);
         // Прелоад логотипа водяного знака. Если грузить его при отрисовке HUD,
         // glTexImage2D выполняется с привязанным временным FBO — драйвер NVIDIA
         // падает (EXCEPTION_ACCESS_VIOLATION в nvoglv64.dll) при заходе на сервер.
         try {
            TextureLoader.load("assets/fluxvisuals/textures/gui/logo.png");
         } catch (Exception ex) {
            LOGGER.warn("{} failed to preload watermark logo: {}", NAME, ex.getMessage());
         }
         uiFont = FontRegistry.INTER_MEDIUM;
         initialized = true;
      }
   }

   public static void onRender() {
      if (modInitialized) {
         GlState.Snapshot snapshot = GlState.push();

         try {
            if (!initialized) {
               onInit();
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getWindow() == null) {
               return;
            }

            int width = client.getWindow().getFramebufferWidth();
            int height = client.getWindow().getFramebufferHeight();
            if (width <= 0 || height <= 0) {
               return;
            }

            AnimationSystem.getInstance().tick();
            DraggableManager draggableManager = DraggableManager.getInstance();
            draggableManager.beginFrame(client, renderer, width, height);
            boolean rendererBegun = false;

            try {
               renderer.begin(width, height);
               rendererBegun = true;

               try {
                  EventManager.call(new RenderEvent(client, renderer, uiFont, width, height));
               } finally {
                  if (rendererBegun) {
                     renderer.end();
                  }
               }
            } finally {
               draggableManager.endFrame();
            }
         } finally {
            GlState.pop(snapshot);
         }
      }
   }

   /**
    * Читает fluxvisuals.json, который лаунчер кладёт в config/ игрового каталога.
    * Оттуда берём ник клиента (ватермарка) и акцент.
    */
   private static void loadLoaderConfig(File runDirectory) {
      try {
         File cfgFile = new File(runDirectory, "config" + File.separator + "fluxvisuals.json");
         if (!cfgFile.exists()) {
            return;
         }
         String content = new String(java.nio.file.Files.readAllBytes(cfgFile.toPath()));
         com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
         if (obj.has("clientName")) {
            String name = obj.get("clientName").getAsString();
            if (name != null && !name.isBlank()) {
               StyleConfig.clientName = name;
            }
         }
         LOGGER.info("{} loader config loaded: clientName={}", NAME, StyleConfig.clientName);
      } catch (Exception e) {
         LOGGER.warn("{} failed to read loader config: {}", NAME, e.getMessage());
      }
   }
}