package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.impl.visuals.betterminecraft.Animation;
import ru.fluxvisuals.module.impl.visuals.betterminecraft.Easings;

/**
 * Эталонная реализация: анимации ванильных экранов и хотбара на согласованных
 * {@link Animation}. Состояние живёт в singleton, потому что читается миксинами
 * до рендера интерфейса.
 */
@IModule(
   name = "BetterMinecraft",
   description = "xxx",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class BetterMinecraft extends Module {
    public static final BetterMinecraft INSTANCE = new BetterMinecraft();

    public final BooleanSetting inventoryAnim = new BooleanSetting("Inventory Anim", true);
    public final BooleanSetting tabAnim = new BooleanSetting("Tab Anim", true);
    public final BooleanSetting chatAnim = new BooleanSetting("Chat Anim", true);
    public final BooleanSetting chatCharAnim = new BooleanSetting("Chat Letter Anim", true);
    public final BooleanSetting hotbarAnim = new BooleanSetting("Hotbar Anim", true);

    public final MultiBooleanSetting anim = new MultiBooleanSetting("Animation",
          inventoryAnim, tabAnim, chatAnim, chatCharAnim, hotbarAnim);

    public final BooleanSetting stackMessages = new BooleanSetting("Stack Messages", false);
    public final SliderSetting animSpeed = new SliderSetting("Anim Speed", 180.0F, 150.0F, 300.0F, 1.0F, false);

    public final Animation inventoryAnimation = new Animation();
    public final Animation tabAnimation = new Animation();
    public final Animation chatAnimation = new Animation();
    public final Animation hotbarAnimation = new Animation();

    private boolean lastInventoryOpen = false;
    private boolean lastTabOpen = false;
    private boolean lastChatOpen = false;
    private boolean initialized = false;

    private BetterMinecraft() {
       this.addSettings(new Setting[]{anim, stackMessages, animSpeed});
    }

    public boolean isEnabled() {
       return this.enable;
    }

    /** Эталонный Event2D-обработчик: вызывается один раз за кадр до рендера HUD. */
    public void update() {
       if (mc.player == null) return;

       float dur = animSpeed.get() / 1000.0F;
       if (!initialized) {
           initialized = true;
           hotbarAnimation.set(1.0);
       }

       boolean inventoryOpen = mc.currentScreen instanceof HandledScreen<?>;
       boolean tabOpen = mc.options.playerListKey.isPressed();
       boolean chatOpen = mc.currentScreen instanceof ChatScreen;

       if (inventoryOpen != lastInventoryOpen) {
           if (inventoryOpen) inventoryAnimation.set(0.0);
           inventoryAnimation.run(inventoryOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
           lastInventoryOpen = inventoryOpen;
       }
       inventoryAnimation.update();

       if (tabOpen != lastTabOpen) {
           tabAnimation.run(tabOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
           lastTabOpen = tabOpen;
       }
       tabAnimation.update();

       if (chatOpen != lastChatOpen) {
           if (chatOpen) chatAnimation.set(0.0);
           chatAnimation.run(chatOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
           lastChatOpen = chatOpen;
       }
       chatAnimation.update();

       boolean hotbarVisible = !inventoryOpen;
       float hotbarTarget = hotbarVisible ? 1.0F : 0.0F;
       hotbarAnimation.run(hotbarTarget, dur, Easings.CUBIC_OUT, true);
       hotbarAnimation.update();
    }

    @Override
    public void onEnable() {
       super.onEnable();
       initialized = false;
       if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
       super.onDisable();
       inventoryAnimation.set(1.0);
       tabAnimation.set(0.0);
       chatAnimation.set(1.0);
       hotbarAnimation.set(1.0);
       lastInventoryOpen = false;
       lastTabOpen = false;
       lastChatOpen = false;
       initialized = false;
       if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public float getInventoryAnim() { return inventoryAnimation.get(); }
    public float getTabAnim() { return tabAnimation.get(); }
    public float getChatAnim() { return chatAnimation.get(); }
    public float getHotbarAnim() { return hotbarAnimation.get(); }
}
