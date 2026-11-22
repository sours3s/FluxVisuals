package ru.fluxvisuals.ui.fluxgui.client.gui.screen;

import ru.fluxvisuals.ui.fluxgui.client.gui.ChatMessengerScreen;
import ru.fluxvisuals.ui.fluxgui.client.gui.font.icon.IconTtfBridge;
import ru.fluxvisuals.ui.fluxgui.client.gui.hud.Watermark;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.BrandLogo;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.IconRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.ShadowRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.friend.Friend;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.ui.fluxgui.command.CommandManager;
import ru.fluxvisuals.ui.fluxgui.config.ConfigManager;
import ru.fluxvisuals.ui.fluxgui.cosmetics.BedrockModel;
import ru.fluxvisuals.ui.fluxgui.cosmetics.CosmeticItem;
import ru.fluxvisuals.ui.fluxgui.cosmetics.CosmeticsManager;
import ru.fluxvisuals.ui.fluxgui.module.Category;
import ru.fluxvisuals.ui.fluxgui.module.Module;
import ru.fluxvisuals.ui.fluxgui.module.ModuleManager;
import ru.fluxvisuals.ui.fluxgui.module.misc.ChatHelper;
import ru.fluxvisuals.ui.fluxgui.module.misc.Macros;
import ru.fluxvisuals.ui.fluxgui.module.render.Markers;
import ru.fluxvisuals.ui.fluxgui.module.render.Markers$MarkerData;
import ru.fluxvisuals.ui.fluxgui.setting.ActionSetting;
import ru.fluxvisuals.ui.fluxgui.setting.BindSetting;
import ru.fluxvisuals.ui.fluxgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ColorSetting;
import ru.fluxvisuals.ui.fluxgui.setting.FriendRequestRowSetting;
import ru.fluxvisuals.ui.fluxgui.setting.FriendRowSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ItemHighlightSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ItemScaleSetting;
import ru.fluxvisuals.ui.fluxgui.setting.LabelSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ModeSetting;
import ru.fluxvisuals.ui.fluxgui.setting.MultiBooleanSetting;
import ru.fluxvisuals.ui.fluxgui.setting.NumberSetting;
import ru.fluxvisuals.ui.fluxgui.setting.Setting;
import ru.fluxvisuals.ui.fluxgui.setting.StringSetting;
import ru.fluxvisuals.ui.fluxgui.util.ClientProfile;
import ru.fluxvisuals.ui.fluxgui.util.events.FluxEventsClient;
import ru.fluxvisuals.ui.fluxgui.util.events.FluxEventsClient$FluxEvent;
import ru.fluxvisuals.ui.fluxgui.util.events.FluxEventsClient$FluxMine;
import ru.fluxvisuals.ui.fluxgui.util.events.FluxEventsClient$MineRarityKind;
import ru.fluxvisuals.ui.fluxgui.util.events.ServerEventsClient;
import ru.fluxvisuals.ui.fluxgui.util.events.ServerEventsClient$ServerEvent;
import ru.fluxvisuals.ui.fluxgui.util.friendscreen.FriendScreenClient$Entry;
import ru.fluxvisuals.ui.fluxgui.util.policy.ClickGuiVisibilityPolicy;
import ru.fluxvisuals.ui.fluxgui.util.render.FluxGlassPipeline;
import ru.fluxvisuals.ui.fluxgui.util.render.RenderUtil;
import ru.fluxvisuals.profile.Profile;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Screen {
    static final Set<String> LOWER_WING_PREVIEWS = Set.of(
        "wings_7_ee401e1a", "wings_8_ee401e1a", "wings_10_bc304070", "wings_12_f684c6fe", "wings_13_04875af2", "wings_14_19ec3eb9"
    );
    static final Set<String> HIGHER_WING_PREVIEWS = Set.of("wings_3_81b06a4b", "wings_4_ac93f2ee");
    static final String LOWER_BONE_REAPER_I_PREVIEW = "wings_7_ee401e1a";
    static final Set<String> OPPOSITE_VERTICAL_WING_PREVIEWS = Set.of(
        "wings_3_81b06a4b", "wings_4_ac93f2ee", "wings_8_ee401e1a", "wings_9_ee401e1a", "wings_13_04875af2", "wings_22_3dfa663d", "wings_23_28b34740"
    );
    static final float DESIGN_WIDTH = 1920.0F;
    static final float DESIGN_HEIGHT = 1080.0F;
    static final float GUI_WIDTH = 987.0F;
    static final float GUI_HEIGHT = 536.0F;
    static final float GUI_RADIUS = 18.0F;
    static final float BACKDROP_BLUR_SIGMA = 32.0F;
    static final float BLUR_DISK_PER_SIGMA = 2.462F;
    static int ACCENT = -45478;
    static final int PANEL_COLOR = -872415232;
    static final int CARD_COLOR = 1073741824;
    static final int SETTING_BG = 603979776;
    static final int SEPARATOR_COLOR = 352321535;
    static final int TEXT_COLOR = -1;
    static final int TEXT_SECOND = -2130706433;
    static final int TEXT_CATEGORY = 1946157055;
    static final int TEXT_SEARCH = 1728053247;
    static final int TEXT_INACTIVE = 1090519039;
    static final int TRACK_COLOR = 536870911;
    static final int KNOB_COLOR = -2130706433;
    static final int SCRIM_EDGE = -1342177280;
    static final int SCRIM_CENTER = -1912602624;
    static final float TITLE_SIZE = 14.241F;
    static final float TEXT_SIZE = 12.206F;
    static final float SETTING_TEXT_SIZE = 12.65F;
    static final float CARD_RADIUS = 12.0F;
    static final float TILE_RADIUS = 8.0F;
    static final float LINE_RADIUS = 1.5F;
    static final float HEADER_X = 10.0F;
    static final float HEADER_Y = 10.0F;
    static final float HEADER_WIDTH = 224.0F;
    static final float HEADER_HEIGHT = 54.0F;
    static final float LOGO_TILE_X = 22.0F;
    static final float LOGO_TILE_Y = 20.0F;
    static final float LOGO_TILE_SIZE = 34.0F;
    static final float LOGO_ICON_X = 32.0F;
    static final float LOGO_ICON_Y = 30.0F;
    static final float HEADER_TEXT_X = 67.82F;
    static final float HEADER_TITLE_BASELINE = 34.936F;
    static final float HEADER_BUILD_BASELINE = 49.313F;
    static final String HEADER_TITLE = "FluxVisuals";
    static final String HEADER_BUILD = "Build: #1.0.23";
    static final float SEARCH_X = 10.0F;
    static final float SEARCH_Y = 74.0F;
    static final float SEARCH_WIDTH = 224.0F;
    static final float SEARCH_HEIGHT = 37.0F;
    static final float SEARCH_ICON_X = 23.0F;
    static final float SEARCH_ICON_Y = 86.5F;
    static final float SEARCH_TEXT_X = 46.095F;
    static final float SEARCH_TEXT_BASELINE = 98.089F;
    static final String SEARCH_PLACEHOLDER = "Search...";
    static final float SIDEBAR_X = 10.0F;
    static final float SIDEBAR_Y = 121.0F;
    static final float SIDEBAR_WIDTH = 224.0F;
    static final float SIDEBAR_HEIGHT = 341.0F;
    static final float SIDEBAR_ICON_X = 21.25F;
    static final float SIDEBAR_ICON_Y = 131.75F;
    static final float SIDEBAR_TITLE_X = 43.968F;
    static final float SIDEBAR_TITLE_BASELINE = 143.076F;
    static final String SIDEBAR_TITLE = "Categories";
    static final float SIDEBAR_LINE_X = 22.0F;
    static final float SIDEBAR_LINE_Y = 158.0F;
    static final float SIDEBAR_LINE_WIDTH = 200.0F;
    static final float ROW_X = 22.0F;
    static final float ROW_Y = 168.0F;
    static final float ROW_WIDTH = 200.0F;
    static final float ROW_HEIGHT = 32.0F;
    static final float ROW_BASELINE = 20.5F;
    static final float ROW_ICON_CENTER = 27.33F;
    static final float ROW_ICON_CENTER_ACTIVE = 36.0F;
    static final float ROW_LABEL_X = 42.546F;
    static final float ROW_LABEL_X_ACTIVE = 51.909F;
    static final float USER_X = 10.0F;
    static final float USER_Y = 472.0F;
    static final float USER_WIDTH = 224.0F;
    static final float USER_HEIGHT = 54.0F;
    static final float AVATAR_X = 22.0F;
    static final float AVATAR_Y = 482.0F;
    static final float AVATAR_SIZE = 34.0F;
    static final float USER_TEXT_X = 67.93F;
    static final float USER_NAME_BASELINE = 496.595F;
    static final float USER_EXPIRY_BASELINE = 511.944F;
    static final float DIVIDER_X = 244.0F;
    static final float DIVIDER_Y = 10.0F;
    static final float DIVIDER_WIDTH = 3.0F;
    static final float DIVIDER_HEIGHT = 516.0F;
    static final float CATEGORY_CARD_X = 257.0F;
    static final float CATEGORY_CARD_Y = 10.0F;
    static final float CATEGORY_CARD_WIDTH = 437.0F;
    static final float CATEGORY_CARD_HEIGHT = 52.0F;
    static final float CATEGORY_ICON_CENTER_X = 277.0F;
    static final float CATEGORY_ICON_CENTER_Y = 36.0F;
    static final float CATEGORY_TEXT_X = 296.98F;
    static final float CATEGORY_NAME_BASELINE = 33.936F;
    static final float CATEGORY_DESC_BASELINE = 48.944F;
    static final String CATEGORY_DESC = "This category includes many modules that help you during the game.";
    static final float COUNTER_CARD_X = 704.0F;
    static final float COUNTER_CARD_Y = 10.0F;
    static final float COUNTER_CARD_WIDTH = 273.0F;
    static final float COUNTER_CARD_HEIGHT = 52.0F;
    static final float COUNTER_ICON_X = 716.0F;
    static final float COUNTER_ICON_Y = 28.0F;
    static final float COUNTER_TEXT_X = 743.84F;
    static final float COUNTER_VALUE_BASELINE = 33.765F;
    static final float COUNTER_DESC_BASELINE = 49.076F;
    static final String COUNTER_DESC = "Shows how many modules are enabled.";
    static final float MODULE_X = 257.0F;
    static final float MODULE_Y = 72.0F;
    static final float MODULE_WIDTH = 355.0F;
    static final float MODULE_GAP = 10.0F;
    static final float MODULE_BASE_HEIGHT = 57.2F;
    static final float SETTING_ROW_HEIGHT = 30.8F;
    static final float MODULE_ICON_X = 14.0F;
    static final float MODULE_ICON_Y = 20.7F;
    static final float MODULE_TEXT_X = 42.0F;
    static final float MODULE_NAME_BASELINE = 23.1F;
    static final float MODULE_DESC_BASELINE = 41.8F;
    static final float MODULE_BIND_RIGHT = 297.0F;
    static final float MODULE_BIND_BASELINE = 33.55F;
    static final float MODULE_NAME_SIZE = 14.85F;
    static final float MODULE_DESC_SIZE = 11.55F;
    static final float MODULE_BIND_SIZE = 15.665F;
    static final float TRACK_X = 313.4F;
    static final float TRACK_Y = 20.9F;
    static final float TRACK_WIDTH = 28.6F;
    static final float TRACK_HEIGHT = 15.4F;
    static final float TRACK_RADIUS = 7.7F;
    static final float KNOB_WIDTH = 11.0F;
    static final float KNOB_HEIGHT = 11.0F;
    static final float KNOB_RADIUS = 5.5F;
    static final float KNOB_INSET = 2.2F;
    static final float KNOB_TRAVEL = 13.2F;
    static final float SETTING_SLIDER_WIDTH = 132.0F;
    static final float SETTING_VALUE_WIDTH = 128.0F;
    static final float STRING_FIELD_WIDTH = 154.0F;
    static final float MODE_POPUP_WIDTH = 174.0F;
    static final float MODE_POPUP_MAX_HEIGHT = 313.0F;
    static final float MODE_POPUP_ROW_HEIGHT = 27.0F;
    static final float PICKER_WIDTH = 224.0F;
    static final float PICKER_HEIGHT = 218.0F;
    static final float PICKER_RADIUS = 12.0F;
    static final int[] PALETTE_PRESETS = new int[]{-45478, -5745161, -10262799, -12877066, -16337196, -15681151, -680437, -429290, -770210, -1};
    static final String[] MARKER_COLOR_LABELS = new String[]{"Фон", "Текст", "Иконка", "Фон иконки"};
    static final int MODULE_COLUMNS = 2;
    static final Identifier AVATAR = Identifier.of("fluxvisuals", "icons/avatar.png");
    static final ClickGui$SidebarEntry[] SIDEBAR = new ClickGui$SidebarEntry[]{
        new ClickGui$SidebarEntry(Category.VISUALS, "star", "star", 1.25F),
        new ClickGui$SidebarEntry(Category.UTILITY, "command", "command_large", 1.0F),
        new ClickGui$SidebarEntry(Category.CONFIGS, "list", "list", 1.05F),
        new ClickGui$SidebarEntry(Category.FRIENDS, "person", "person", 1.1F),
        new ClickGui$SidebarEntry(Category.COSMETICS, "flask", "flask", 1.08F)
    };
    static Category selectedCategory = Category.VISUALS;
    static int eventsTab = 0;
    static int eventsServer = 0;
    static final Set<String> EVENT_FILTERS = new HashSet<>();
    static String eventPhaseFilter = "all";
    static String mineRarityFilter = "all";
    static final Set<String> SERVER_EVENT_FILTERS = new HashSet<>();
    public static String configInput = "";
    public static boolean configInputFocus = false;
    public static float configsScroll = 0.0F;
    static float configsTargetScroll = 0.0F;
    static int friendsTab = 0;
    static String friendsSearch = "";
    static boolean friendsSearchFocus = false;
    static float friendsScroll = 0.0F;
    static float friendsTargetScroll = 0.0F;
    static final Map<String, Float> CONFIG_ENTRY_ANIMS = new HashMap<>();
    static final Map<String, Float> CONFIG_HOVER_ANIMS = new HashMap<>();
    static int cosmeticsTab = 0;
    static long lastEventsRefreshTime = 0L;
    static final Set<Module> EXPANDED_MODULES = Collections.newSetFromMap(new IdentityHashMap<>());
    static final Map<Module, Float> TOGGLE_ANIMS = new IdentityHashMap<>();
    static final Map<Module, Float> HOVER_ANIMS = new IdentityHashMap<>();
    static final Map<Module, Float> DESCRIPTION_SCROLLS = new IdentityHashMap<>();
    static final Map<ItemHighlightSetting, ColorSetting> ITEM_COLOR_PROXIES = new IdentityHashMap<>();
    static final Map<Setting<?>, Float> MACRO_DELETE_ANIMS = new IdentityHashMap<>();
    static final Map<Module, Float> EXPAND_ANIMS = new IdentityHashMap<>();
    static final Map<BooleanSetting, Float> SETTING_TOGGLE_ANIMS = new IdentityHashMap<>();
    static final Map<NumberSetting, Float> SLIDER_PRESS_ANIMS = new IdentityHashMap<>();
    static final Map<NumberSetting, Float> SLIDER_VALUE_ANIMS = new IdentityHashMap<>();
    static final Map<MultiBooleanSetting, Float> MULTI_HOVER_ANIMS = new IdentityHashMap<>();
    static final Map<MultiBooleanSetting, Float> MULTI_SCROLL_OFFSETS = new IdentityHashMap<>();
    static final Map<ClickGui$SidebarEntry, Float> SIDEBAR_SELECTION_ANIMS = new IdentityHashMap<>();
    static final Map<Setting<?>, Map<String, Float>> MODE_OPTION_ANIMS = new IdentityHashMap<>();
    static final Map<Setting<?>, Float> SETTING_VISIBILITY_ANIMS = new IdentityHashMap<>();
    static ColorSetting activeColorPicker = null;
    boolean colorPickerOpen = false;
    float colorPickerProgress = 0.0F;
    static BindSetting bindingSetting = null;
    static FriendRowSetting bindingFriendSetting = null;
    static StringSetting activeTextSetting = null;
    Setting<?> modePopupSetting;
    boolean modePopupOpen;
    float modePopupProgress;
    float modePopupX;
    float modePopupY;
    float modePopupHeight;
    float modePopupScroll;
    float modePopupTargetScroll;
    float modePopupAnchorContentY;
    float modePopupAnchorOffsetY;
    float scale = 1.0F;
    float originX;
    float originY;
    float openProgress = 0.0F;
    boolean closing;
    float sidebarGliderY = 168.0F;
    static float scrollOffset = 0.0F;
    static float targetScroll = 0.0F;
    static final float SETTINGS_X = 1000.0F;
    static final float SETTINGS_Y = 10.0F;
    static final float SETTINGS_W = 360.0F;
    static final float SETTINGS_H = 516.0F;
    static final float SETTINGS_TOP = 76.0F;
    static final float SETTINGS_BOTTOM = 514.0F;
    static Module settingsModule = null;
    static float settingsScroll = 0.0F;
    static float settingsTargetScroll = 0.0F;
    NumberSetting draggingSlider = null;
    float draggingSliderX;
    float draggingSliderWidth;
    boolean draggingSV = false;
    boolean draggingHue = false;
    boolean draggingAlpha = false;
    float colorPickerHue;
    static boolean isSearching = false;
    static String searchQuery = "";
    float searchSelectionAnim;
    String markerNameInput = "";
    String markerXInput = "";
    String markerYInput = "";
    String markerZInput = "";
    int markerInputFocus;
    int selectedMarkerIcon;
    final ColorSetting markerBackgroundColor = new ColorSetting("Фон метки", new Color(36, 38, 46));
    final ColorSetting markerTextColor = new ColorSetting("Цвет текста", Color.WHITE);
    final ColorSetting markerIconColor = new ColorSetting("Цвет иконки", Color.WHITE);
    float markerSize = 1.575F;
    boolean draggingMarkerSize;
    float markerListScroll;
    float markerListTargetScroll;
    long lastRenderNanos;
    float frameDeltaSeconds = 0.016666668F;
    boolean sliderCursorRequested;
    boolean resizeCursorActive;
    static long resizeCursorHandle;
    static final Map<Setting<?>, Float> SETTING_GENERIC_TOGGLE_ANIMS = new IdentityHashMap<>();

    public ClickGui() {
        super(Text.empty());
    }

    /** Подготавливает экран, когда он используется внутри обёртки FluxVisuals. */
    public void prepareForWrapper(int screenWidth, int screenHeight) {
        this.width = screenWidth;
        this.height = screenHeight;
        this.init(screenWidth, screenHeight);
    }

    public static boolean overlapsMainPanel(float left, float top, float right, float bottom) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ClickGui gui) {
            float var11 = gui.scale;
            float panelX = gui.originX;
            float panelY = gui.originY;
            if (gui.lastRenderNanos == 0L && client.getWindow() != null) {
                var11 = Math.min(client.getWindow().getScaledWidth() / 1920.0F, client.getWindow().getScaledHeight() / 1080.0F)
                    * ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.getGuiSizeFactor()
                    * 0.94F;
                panelX = Math.round((client.getWindow().getScaledWidth() - 987.0F * var11) / 2.0F);
                panelY = Math.round((client.getWindow().getScaledHeight() - 536.0F * var11) / 2.0F);
            }

            float panelRight = panelX + 987.0F * var11;
            float panelBottom = panelY + 536.0F * var11;
            return right > panelX && left < panelRight && bottom > panelY && top < panelBottom;
        } else {
            return false;
        }
    }

    static float getRowY(int index) {
        return switch (index) {
            case 0 -> 168.0F;
            case 1 -> 210.0F;
            case 2 -> 252.0F;
            case 3 -> 298.0F;
            case 4 -> 340.0F;
            case 5 -> 386.0F;
            case 6 -> 428.0F;
            default -> 168.0F + index * 36.0F;
        };
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        float scrimAlpha = 0.85F * this.openProgress;
        int edge = fade(-1342177280, scrimAlpha);
        int center = fade(-1912602624, scrimAlpha);
        RenderUtil.drawVerticalGradientRect(context, 0.0F, 0.0F, this.width, this.height / 2.0F, edge, center);
        RenderUtil.drawVerticalGradientRect(context, 0.0F, this.height / 2.0F, this.width, this.height, center, edge);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        ACCENT = 0xFF000000 | ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.getAccentColor();
        long now = System.nanoTime();
        if (this.lastRenderNanos != 0L) {
            this.frameDeltaSeconds = Math.min(0.05F, (float)(now - this.lastRenderNanos) / 1.0E9F);
        }

        this.lastRenderNanos = now;
        if (this.closing) {
            this.openProgress = Math.max(0.0F, this.openProgress - this.frameDeltaSeconds / 0.24F);
            if (this.openProgress <= 0.0F) {
                this.restoreDefaultCursor();
                MinecraftClient.getInstance().setScreen(null);
                return;
            }
        } else {
            this.openProgress = Math.min(1.0F, this.openProgress + this.frameDeltaSeconds / 0.24F);
        }

        float easeOpen = 1.0F - (float)Math.pow(1.0F - this.openProgress, 3.0);
        float animScale = 0.94F + 0.06F * easeOpen;
        float configuredScale = ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.getGuiSizeFactor();
        this.scale = Math.min(this.width / 1920.0F, this.height / 1080.0F) * configuredScale * animScale;
        this.originX = Math.round((this.width - 987.0F * this.scale) / 2.0F);
        this.originY = Math.round((this.height - 536.0F * this.scale) / 2.0F);
        float maxScroll = this.getMaxScroll();
        targetScroll = MathHelper.clamp(targetScroll, 0.0F, maxScroll);
        scrollOffset = this.approach(scrollOffset, targetScroll, 10.0F);
        if (Math.abs(scrollOffset - targetScroll) < 0.02F) {
            scrollOffset = targetScroll;
        }

        if (this.colorPickerOpen && activeColorPicker != null) {
            this.colorPickerProgress = Math.min(1.0F, this.colorPickerProgress + this.frameDeltaSeconds / 0.18F);
        } else {
            this.colorPickerProgress = Math.max(0.0F, this.colorPickerProgress - this.frameDeltaSeconds / 0.28F);
        }

        if (!this.colorPickerOpen && this.colorPickerProgress <= 0.0F) {
            this.colorPickerProgress = 0.0F;
            activeColorPicker = null;
        }

        if (this.modePopupOpen && this.modePopupSetting != null) {
            this.modePopupProgress = Math.min(1.0F, this.modePopupProgress + this.frameDeltaSeconds / 0.18F);
        } else {
            this.modePopupProgress = Math.max(0.0F, this.modePopupProgress - this.frameDeltaSeconds / 0.2F);
        }

        if (!this.modePopupOpen && this.modePopupProgress <= 0.0F) {
            this.modePopupSetting = null;
        }

        this.modePopupScroll = this.approach(this.modePopupScroll, this.modePopupTargetScroll, 12.0F);
        this.updateModePopupPosition();
        this.sliderCursorRequested = this.draggingSlider != null;
        float designMouseX = (mouseX - this.originX) / this.scale;
        float designMouseY = (mouseY - this.originY) / this.scale;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.originX, this.originY);
        context.getMatrices().scale(this.scale, this.scale);
        FluxGlassPipeline.captureFrame();
        if (ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.isLiquidGlassStyle()) {
            Color selected = ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.guiGlassColor.getColor();
            int glassRgb = selected.getRGB() & 16777215;
            int glassAlpha = selected.getAlpha();
            int materialAlpha = MathHelper.clamp(Math.round(108.0F + glassAlpha * 0.45F), 108, 223);
            Color tint = new Color(materialAlpha << 24 | glassRgb, true);
            float glowStrength = ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.guiGlassGlow.getValue()
                ? ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.guiGlowStrength.getFloatValue()
                : 0.0F;
            int shadowAlpha = Math.round(40.0F + glowStrength * 20.0F);
            float shadowSigma = 18.0F + glowStrength * 4.0F;
            ShadowRender.draw(context, -0.75F, -0.75F, 988.5F, 537.5F, 18.75F, shadowSigma, fade(withAlpha(glassRgb, shadowAlpha), this.openProgress));
            FluxGlassPipeline.drawLiquidAt(context, 0.0F, 0.0F, 987.0F, 536.0F, 18.0F, 78.784F, 1.0F, 0.46F, this.openProgress, tint, true);
            int contourRed = Math.round(191.25F + selected.getRed() * 0.25F);
            int contourGreen = Math.round(191.25F + selected.getGreen() * 0.25F);
            int contourBlue = Math.round(191.25F + selected.getBlue() * 0.25F);
            int contourRgb = contourRed << 16 | contourGreen << 8 | contourBlue;
            RoundedRenderer.drawRoundedOutline(context, 1.0F, 1.0F, 985.0F, 534.0F, 17.0F, 1.1F, fade(withAlpha(contourRgb, 92), this.openProgress));
        } else {
            FluxGlassPipeline.drawBlurAt(context, 0.0F, 0.0F, 987.0F, 536.0F, 18.0F, 78.784F * this.scale, this.openProgress);
            RoundedRenderer.drawRoundedRect(context, 0.0F, 0.0F, 987.0F, 536.0F, 18.0F, fade(-872415232, this.openProgress));
        }

        this.renderHeader(context);
        this.renderSearch(context);
        this.renderSidebar(context, designMouseX, designMouseY);
        this.renderUser(context);
        RoundedRenderer.drawRoundedRect(context, 244.0F, 10.0F, 3.0F, 516.0F, 1.5F, 352321535);
        if (selectedCategory == Category.SERVER_EVENTS && !isSearchMode()) {
            if (System.currentTimeMillis() - lastEventsRefreshTime > 30000L) {
                lastEventsRefreshTime = System.currentTimeMillis();
                if (eventsServer == 0) {
                    FluxEventsClient.refreshAsync(() -> lastEventsRefreshTime = System.currentTimeMillis());
                    FluxEventsClient.refreshMinesAsync(null);
                } else {
                    ServerEventsClient.refreshAsync(() -> lastEventsRefreshTime = System.currentTimeMillis());
                }
            }

            this.renderEventsHeader(context);
            context.enableScissor(257, 56, 977, 526);
            this.renderEventsContent(context, designMouseX, designMouseY);
            context.disableScissor();
        } else if (selectedCategory == Category.COSMETICS && !isSearchMode()) {
            this.renderCosmeticsHeader(context);
            context.enableScissor(257, 56, 977, 526);
            this.renderCosmeticsContent(context, designMouseX, designMouseY);
            context.disableScissor();
        } else if (selectedCategory == Category.CONFIGS && !isSearchMode()) {
            this.renderCategoryCard(context);
            this.renderConfigCounterCard(context);
            context.enableScissor(257, 72, 977, 526);
            this.renderConfigsPage(context, designMouseX, designMouseY);
            context.disableScissor();
        } else if (selectedCategory == Category.FRIENDS && !isSearchMode()) {
            this.renderCategoryCard(context);
            this.renderFriendsCounterCard(context);
            context.enableScissor(257, 72, 977, 526);
            this.renderFriendsPage(context, designMouseX, designMouseY);
            context.disableScissor();
        } else if (selectedCategory == Category.MARKS && !isSearchMode()) {
            this.renderCategoryCard(context);
            this.renderMarkerCounterCard(context);
            context.enableScissor(257, 72, 977, 526);
            this.renderMarkersPage(context, designMouseX, designMouseY);
            context.disableScissor();
        } else {
            this.renderCategoryCard(context);
            this.renderCounterCard(context);
            context.enableScissor(257, 72, 977, 526);
            this.renderModules(context, designMouseX, designMouseY);
            context.disableScissor();
        }

        settingsScroll = this.approach(settingsScroll, settingsTargetScroll, 10.0F);
        if (Math.abs(settingsScroll - settingsTargetScroll) < 0.02F) {
            settingsScroll = settingsTargetScroll;
        }
        if (settingsModule != null && selectedCategory != Category.CONFIGS && selectedCategory != Category.MARKS) {
            this.renderSettingsPanel(context, designMouseX, designMouseY);
        }

        if (activeColorPicker != null && this.colorPickerProgress > 0.001F) {
            this.renderFloatingColorPicker(context, designMouseX, designMouseY);
        }

        if (this.modePopupSetting != null && this.modePopupProgress > 0.001F) {
            context.enableScissor(257, 72, 977, 526);
            this.renderModePopup(context, designMouseX, designMouseY);
            context.disableScissor();
        }

        context.getMatrices().popMatrix();
        if (this.colorPickerOpen || this.modePopupOpen) {
            this.sliderCursorRequested = false;
        }

        this.updateSliderCursor();
    }

    void renderHeader(DrawContext context) {
        RoundedRenderer.drawRoundedRect(context, 10.0F, 10.0F, 224.0F, 54.0F, 12.0F, 1073741824);
        if (!Watermark.drawCompactLogo(context, 39.0F, 37.0F, 25.5F, accentAt(22.0F, 20.0F))) {
            RoundedRenderer.drawRoundedRect(context, 22.0F, 20.0F, 34.0F, 34.0F, 8.0F, accentAt(22.0F, 20.0F));
            BrandLogo.draw(context, 26.0F, 24.0F, 26.0F);
        }

        TextRender.draw(context, "FluxVisuals @", 67.82F, 34.936F, 10.0F, -1);
        TextRender.draw(context, "Build: #1.0.23", 67.82F, 49.313F, 12.206F, -2130706433);
    }

    void renderSearch(DrawContext context) {
        boolean selected = isSearchMode();
        this.searchSelectionAnim = this.approach(this.searchSelectionAnim, selected ? 1.0F : 0.0F, 11.0F);
        int searchAccent = accentAt(10.0F, 74.0F);
        RoundedRenderer.drawRoundedRect(context, 10.0F, 74.0F, 224.0F, 37.0F, 12.0F, lerpColor(1073741824, searchAccent, this.searchSelectionAnim));
        IconRender.draw(context, "search", 23.0F, 86.5F, IconRender.getDesignSize("search"), lerpColor(1728053247, -1, this.searchSelectionAnim));
        String displayText = searchQuery.isEmpty()
            ? (isSearching ? "|" : "Search...")
            : searchQuery + (isSearching && System.currentTimeMillis() / 500L % 2L == 0L ? "|" : "");
        int textColor = lerpColor(searchQuery.isEmpty() ? 1728053247 : -1, -1, this.searchSelectionAnim);
        TextRender.draw(context, displayText, 46.095F, 98.089F, 14.241F, textColor);
    }

    void renderSidebar(DrawContext context, float mouseX, float mouseY) {
        RoundedRenderer.drawRoundedRect(context, 10.0F, 121.0F, 224.0F, 341.0F, 12.0F, 1073741824);
        IconRender.draw(context, "list", 21.25F, 131.75F, IconRender.getDesignSize("list"), 1946157055);
        TextRender.draw(context, "Categories", 43.968F, 143.076F, 12.206F, 1946157055);
        context.fill(22, 158, 222, 160, 352321535);
        int selectedIndex = 0;

        for (int i = 0; i < SIDEBAR.length; i++) {
            if (SIDEBAR[i].category() == selectedCategory) {
                selectedIndex = i;
                break;
            }
        }

        float targetGliderY = getRowY(selectedIndex);
        this.sidebarGliderY = this.approach(this.sidebarGliderY, targetGliderY, 12.0F);
        if (!isSearchMode()) {
            RoundedRenderer.drawRoundedRect(context, 22.0F, this.sidebarGliderY, 200.0F, 32.0F, 8.0F, accentAt(22.0F, this.sidebarGliderY));
        }

        for (int index = 0; index < SIDEBAR.length; index++) {
            ClickGui$SidebarEntry entry = SIDEBAR[index];
            boolean selected = entry.category() == selectedCategory && !isSearchMode();
            float rowY = getRowY(index);
            float iconSize = entry.category() == Category.CONFIGS
                ? 13.0F
                : (
                    entry.category() == Category.MARKS
                        ? 15.3F
                        : (
                            entry.category() == Category.COSMETICS
                                ? 13.95F
                                : (
                                    entry.category() != Category.SERVER_EVENTS && entry.category() != Category.MINES
                                        ? IconRender.getDesignSize(entry.icon())
                                        : IconRender.getDesignSize(entry.icon()) * 1.07F
                                )
                        )
                );
            float selectionAnim = SIDEBAR_SELECTION_ANIMS.compute(entry, (key, previous) -> {
                float target = selected ? 1.0F : 0.0F;
                return previous == null ? target : this.approach(previous, target, 11.0F);
            });
            int color = lerpColor(1090519039, -1, selectionAnim);
            float iconCenter = MathHelper.lerp(selectionAnim, 27.33F, 36.0F);
            float labelX = MathHelper.lerp(selectionAnim, 42.546F, 51.909F);
            drawCategoryIcon(context, entry, iconCenter - iconSize / 2.0F, rowY + (32.0F - iconSize) / 2.0F, iconSize, color);
            TextRender.draw(context, entry.category().getName(), labelX, rowY + 20.5F, 12.206F, color);
        }
    }

    static void drawCategoryIcon(DrawContext context, ClickGui$SidebarEntry entry, float x, float y, float size, int color) {
        if (entry.category() != Category.CONFIGS
                && entry.category() != Category.MARKS
                && entry.category() != Category.MESSENGER
                && entry.category() != Category.COSMETICS
            || !IconTtfBridge.tryDrawCategoryIcon(context, entry.category(), x, y, size, color)) {
            IconRender.draw(context, entry.icon(), x, y, size, color);
        }
    }

    static boolean drawConfigIcon(DrawContext context, float x, float y, float size, int color) {
        return IconTtfBridge.tryDrawCategoryIcon(context, Category.CONFIGS, x, y, size, color);
    }

    String getConfigAuthorName() {
        return ClientProfile.getUsername();
    }

    void renderUser(DrawContext context) {
        RoundedRenderer.drawRoundedRect(context, 10.0F, 472.0F, 224.0F, 54.0F, 12.0F, 1073741824);
        MinecraftClient client = MinecraftClient.getInstance();
        String name = "FluxVisuals @";
        String role = Profile.role != null ? "(" + Profile.role.name() + ")" : "(User)";
        String expiry = Profile.subscriptionEndDate != null && !Profile.subscriptionEndDate.isEmpty()
            ? "Till: " + Profile.subscriptionEndDate
            : "Till: Lifetime";
        ClientPlayerEntity var7 = client.player;
        if (var7 instanceof AbstractClientPlayerEntity) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(22.0F, 482.0F);
            context.getMatrices().scale(4.25F, 4.25F);
            PlayerSkinDrawer.draw(context, var7.getSkin(), 0, 0, 8);
            context.getMatrices().popMatrix();
        } else {
            RenderUtil.drawRoundedTexture(context, AVATAR, 22.0F, 482.0F, 34.0F, 34.0F, 8.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        TextRender.draw(context, name, 67.93F, 496.595F, 9.0F, -1);
        TextRender.draw(context, role, 67.93F + TextRender.width(name + " ", 9.0F), 496.595F, 9.0F, accentAt(67.93F, 496.595F));
        TextRender.draw(context, expiry, 67.93F, 511.944F, 12.206F, -2130706433);
    }

    void renderFriendsCounterCard(DrawContext context) {
        int online = friendsOnlinePlayers().size();
        int friends = FriendManager.getFriends().size();
        RoundedRenderer.drawRoundedRect(context, 704.0F, 10.0F, 273.0F, 52.0F, 12.0F, 1073741824);
        IconRender.draw(context, "person", 716.0F, 28.0F, IconRender.getDesignSize("person"), accentAt(716.0F, 28.0F));
        TextRender.draw(context, online + " / " + friends, 743.84F, 33.765F, 14.241F, -1);
        TextRender.draw(context, "Игроков онлайн / друзей", 743.84F, 49.076F, 12.206F, -2130706433);
    }

    static List<String> friendsOnlinePlayers() {
        List<String> names = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return names;
        }
        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        if (entries == null) {
            return names;
        }
        String self = client.player.getName().getString();
        for (PlayerListEntry entry : entries) {
            if (entry.getProfile() == null || entry.getProfile().name() == null) {
                continue;
            }
            String name = entry.getProfile().name();
            if (!name.equalsIgnoreCase(self)) {
                names.add(name);
            }
        }
        return names;
    }

    static List<String> friendsVisibleNames() {
        String query = friendsSearch.trim().toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if (friendsTab == 0) {
            for (Friend friend : FriendManager.getFriends()) {
                String name = friend.getName();
                if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) {
                    result.add(name);
                }
            }
            return result;
        }

        List<String> online = friendsOnlinePlayers();
        for (String name : online) {
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) {
                result.add(name);
            }
        }
        if (!query.isEmpty() && online.stream().noneMatch(name -> name.equalsIgnoreCase(friendsSearch.trim()))) {
            result.add(0, friendsSearch.trim());
        }
        return result;
    }

    static float friendsRowY(int index) {
        return 174.0F + index * 42.0F - friendsScroll;
    }

    void renderFriendsPage(DrawContext context, float mouseX, float mouseY) {
        friendsScroll = this.approach(friendsScroll, friendsTargetScroll, 10.0F);
        float x = 267.0F;
        float w = 700.0F;
        float tabW = (w - 8.0F) / 2.0F;
        float tabY = 84.0F;
        float tabH = 32.0F;
        for (int index = 0; index < 2; index++) {
            float tabX = x + index * (tabW + 8.0F);
            boolean selected = friendsTab == index;
            boolean hovered = inside(mouseX, mouseY, tabX, tabY, tabW, tabH);
            int bg = selected ? lerpColor(603979776, accentAt(tabX, tabY), 0.42F) : (hovered ? 855638015 : 603979776);
            RoundedRenderer.drawRoundedRect(context, tabX, tabY, tabW, tabH, 8.0F, bg);
            String label = index == 0 ? "Друзья" : "Сервер";
            TextRender.draw(context, label, tabX + (tabW - TextRender.width(label, 12.65F)) / 2.0F, tabY + 20.0F, 12.65F, selected ? -1 : -2130706433);
        }

        float inputY = 126.0F;
        float inputH = 34.0F;
        boolean inputHovered = inside(mouseX, mouseY, x, inputY, w, inputH);
        RoundedRenderer.drawRoundedRect(context, x, inputY, w, inputH, 8.0F, friendsSearchFocus || inputHovered ? 855638015 : 603979776);
        String shownInput = trimTextFromStart(friendsSearch, 12.65F, w - 24.0F);
        if (shownInput.isEmpty() && !friendsSearchFocus) {
            TextRender.draw(context, "Поиск игрока на сервере", x + 11.0F, inputY + 21.0F, 12.65F, -2130706433);
        } else {
            TextRender.draw(context, shownInput, x + 11.0F, inputY + 21.0F, 12.65F, -1);
            if (friendsSearchFocus && System.currentTimeMillis() % 1000L < 540L) {
                float cursorX = x + 11.0F + TextRender.width(shownInput, 12.65F) + 1.0F;
                RoundedRenderer.drawRoundedRect(context, cursorX, inputY + 8.0F, 1.2F, 18.0F, 0.6F, -1);
            }
        }

        List<String> names = friendsVisibleNames();
        float listTop = 174.0F;
        float viewport = 526.0F - listTop;
        float totalHeight = Math.max(0.0F, names.size() * 42.0F - 6.0F);
        friendsTargetScroll = MathHelper.clamp(friendsTargetScroll, 0.0F, Math.max(0.0F, totalHeight - viewport));
        List<String> online = friendsOnlinePlayers();
        for (int index = 0; index < names.size(); index++) {
            float rowY = friendsRowY(index);
            if (rowY + 36.0F < 72.0F || rowY > 526.0F) {
                continue;
            }
            String name = names.get(index);
            boolean friend = FriendManager.isFriend(name);
            RoundedRenderer.drawRoundedRect(context, x, rowY, w, 36.0F, 8.0F, 603979776);
            if (online.stream().anyMatch(value -> value.equalsIgnoreCase(name))) {
                RoundedRenderer.drawRoundedRect(context, x + 11.0F, rowY + 15.5F, 5.0F, 5.0F, 2.5F, -11870592);
            }
            TextRender.draw(context, trimText(name, 12.65F, w - 180.0F), x + 24.0F, rowY + 21.0F, 12.65F, -1);
            String action = friend ? "Удалить" : "Добавить";
            float buttonW = 92.0F;
            float buttonX = x + w - buttonW - 12.0F;
            float buttonY = rowY + 5.0F;
            boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, 26.0F);
            int buttonColor = friend
                  ? (hovered ? 1442795098 : 587202559)
                  : lerpColor(603979776, accentAt(buttonX, buttonY), hovered ? 0.65F : 0.38F);
            RoundedRenderer.drawRoundedRect(context, buttonX, buttonY, buttonW, 26.0F, 6.0F, buttonColor);
            TextRender.draw(context, action, buttonX + (buttonW - TextRender.width(action, 12.65F)) / 2.0F, buttonY + 16.0F, 12.65F, friend && hovered ? -38026 : -1);
        }
    }

    boolean handleFriendsPageClick(float mouseX, float mouseY, int button) {
        float x = 267.0F;
        float w = 700.0F;
        float tabW = (w - 8.0F) / 2.0F;
        for (int index = 0; index < 2; index++) {
            float tabX = x + index * (tabW + 8.0F);
            if (inside(mouseX, mouseY, tabX, 84.0F, tabW, 32.0F)) {
                friendsTab = index;
                friendsSearch = "";
                friendsSearchFocus = false;
                friendsScroll = 0.0F;
                friendsTargetScroll = 0.0F;
                return true;
            }
        }

        if (inside(mouseX, mouseY, x, 126.0F, w, 34.0F)) {
            friendsSearchFocus = true;
            return true;
        }
        friendsSearchFocus = false;

        List<String> names = friendsVisibleNames();
        float buttonW = 92.0F;
        float buttonX = x + w - buttonW - 12.0F;
        for (int index = 0; index < names.size(); index++) {
            float rowY = friendsRowY(index);
            if (inside(mouseX, mouseY, buttonX, rowY + 5.0F, buttonW, 26.0F)) {
                String name = names.get(index);
                if (FluxVisualsClient.get == null || FluxVisualsClient.get.friendManager == null) {
                    return true;
                }
                if (FriendManager.isFriend(name)) {
                    FluxVisualsClient.get.friendManager.remove(name);
                } else {
                    FluxVisualsClient.get.friendManager.add(name);
                }
                return true;
            }
        }
        return true;
    }

    void renderCategoryCard(DrawContext context) {
        RoundedRenderer.drawRoundedRect(context, 257.0F, 10.0F, 437.0F, 52.0F, 12.0F, 1073741824);
        ClickGui$SidebarEntry entry = selectedEntry();
        float iconSize = entry.category() == Category.CONFIGS
            ? 18.7F
            : (entry.category() == Category.MARKS ? 20.5F : IconRender.getDesignSize(entry.headerIcon()) * entry.headerScale());
        drawCategoryIcon(context, entry, 277.0F - iconSize / 2.0F, 36.0F - iconSize / 2.0F, iconSize, accentAt(277.0F, 36.0F));
        String categoryTitle = isSearchMode() ? "Search Results" : selectedCategory.getName();
        String categoryDesc = isSearchMode()
            ? (searchQuery.isEmpty() ? "Enter a module name or description" : "Found matching modules for \"" + searchQuery + "\"")
            : (
                selectedCategory == Category.MARKS
                    ? "Create and customize markers displayed in the world."
                    : (
                        selectedCategory == Category.CONFIGS
                            ? "Create, update and load your client configurations."
                            : "This category includes many modules that help you during the game."
                    )
            );
        TextRender.draw(context, categoryTitle, 296.98F, 33.936F, 14.241F, -1);
        TextRender.draw(context, categoryDesc, 296.98F, 48.944F, 12.206F, -2130706433);
    }

    void renderCounterCard(DrawContext context) {
        List<Module> modules = this.getFilteredModules();
        int enabled = 0;

        for (Module module : modules) {
            if (module.isEnabled()) {
                enabled++;
            }
        }

        RoundedRenderer.drawRoundedRect(context, 704.0F, 10.0F, 273.0F, 52.0F, 12.0F, 1073741824);
        IconRender.draw(context, "grid", 716.0F, 28.0F, IconRender.getDesignSize("grid"), accentAt(716.0F, 28.0F));
        TextRender.draw(context, enabled + "/" + modules.size(), 743.84F, 33.765F, 14.241F, -1);
        TextRender.draw(context, "Shows how many modules are enabled.", 743.84F, 49.076F, 12.206F, -2130706433);
    }

    void renderMarkerCounterCard(DrawContext context) {
        Markers markers = ModuleManager.INSTANCE.getModule(Markers.class);
        int count = markers == null ? 0 : markers.getMarkers().size();
        RoundedRenderer.drawRoundedRect(context, 704.0F, 10.0F, 273.0F, 52.0F, 12.0F, 1073741824);
        IconRender.draw(context, "grid", 716.0F, 28.0F, IconRender.getDesignSize("grid"), accentAt(716.0F, 28.0F));
        TextRender.draw(context, Integer.toString(count), 743.84F, 33.765F, 14.241F, -1);
        TextRender.draw(context, "Shows how many world markers are saved.", 743.84F, 49.076F, 12.206F, -2130706433);
    }

    void renderConfigCounterCard(DrawContext context) {
        int count = ConfigManager.INSTANCE.listConfigs().size();
        RoundedRenderer.drawRoundedRect(context, 704.0F, 10.0F, 273.0F, 52.0F, 12.0F, 1073741824);
        IconRender.draw(context, "grid", 716.0F, 28.0F, IconRender.getDesignSize("grid"), accentAt(716.0F, 28.0F));
        TextRender.draw(context, Integer.toString(count), 743.84F, 33.765F, 14.241F, -1);
        TextRender.draw(context, "Shows how many configurations are saved.", 743.84F, 49.076F, 12.206F, -2130706433);
    }

    void renderConfigsPage(DrawContext context, float mouseX, float mouseY) {
        float pageX = 257.0F;
        float pageY = 72.0F;
        float pageW = 720.0F;
        float createH = 80.0F;
        float pad = 14.0F;
        float controlY = pageY + 31.0F;
        float controlH = 36.0F;
        float folderW = 36.0F;
        float createW = 126.0F;
        float gap = 8.0F;
        float folderX = pageX + pageW - pad - folderW;
        float createX = folderX - gap - createW;
        float inputX = pageX + pad;
        float inputW = createX - gap - inputX;
        RoundedRenderer.drawRoundedRect(context, pageX, pageY, pageW, createH, 12.0F, 1073741824);
        TextRender.draw(context, "Создать конфиг", inputX, pageY + 22.0F, 14.85F, -1);
        boolean inputHovered = inside(mouseX, mouseY, inputX, controlY, inputW, controlH);
        RoundedRenderer.drawRoundedRect(context, inputX, controlY, inputW, controlH, 8.0F, !configInputFocus && !inputHovered ? 603979776 : 855638015);
        String shownInput = trimTextFromStart(configInput, 12.65F, inputW - 24.0F);
        if (shownInput.isEmpty() && !configInputFocus) {
            TextRender.draw(context, "Название конфига", inputX + 11.0F, controlY + 23.0F, 12.65F, -2130706433);
        } else {
            TextRender.draw(context, shownInput, inputX + 11.0F, controlY + 23.0F, 12.65F, -1);
            if (configInputFocus && System.currentTimeMillis() % 1000L < 540L) {
                float cursorX = inputX + 11.0F + TextRender.width(shownInput, 12.65F) + 1.0F;
                RoundedRenderer.drawRoundedRect(context, cursorX, controlY + 9.0F, 1.2F, 18.0F, 0.6F, -1);
            }
        }

        boolean importCode = configInput.trim().toUpperCase(Locale.ROOT).startsWith("FLUX-");
        int createColor = accentAt(createX, controlY);
        RoundedRenderer.drawFourCornerGradientRoundedRect(
            context,
            createX,
            controlY,
            createW,
            controlH,
            8.0F,
            lerpColor(createColor, -1, 0.1F),
            createColor,
            lerpColor(createColor, -16777216, 0.1F),
            createColor
        );
        String createLabel = importCode ? "Импортировать" : "Создать";
        TextRender.draw(context, createLabel, createX + (createW - TextRender.width(createLabel, 12.65F)) / 2.0F, controlY + 23.0F, 12.65F, -1);
        boolean folderHovered = inside(mouseX, mouseY, folderX, controlY, folderW, controlH);
        RoundedRenderer.drawRoundedRect(context, folderX, controlY, folderW, controlH, 8.0F, folderHovered ? 855638015 : 603979776);
        if (!IconTtfBridge.tryDrawIconsFontChar(context, 'a', folderX + 9.0F, controlY + 9.0F, 18.0F, folderHovered ? -1 : -2130706433)) {
            TextRender.draw(context, "…", folderX + 11.0F, controlY + 23.0F, 15.0F, folderHovered ? -1 : -2130706433);
        }

        float listY = pageY + createH + 10.0F;
        float listH = 536.0F - listY - 10.0F;
        RoundedRenderer.drawRoundedRect(context, pageX, listY, pageW, listH, 12.0F, 1073741824);
        List<String> configs = new ArrayList<>(ConfigManager.INSTANCE.listConfigs());
        configs.sort(String.CASE_INSENSITIVE_ORDER);
        Set<String> present = new HashSet<>(configs);
        CONFIG_ENTRY_ANIMS.keySet().removeIf(namex -> !present.contains(namex));
        CONFIG_HOVER_ANIMS.keySet().removeIf(namex -> !present.contains(namex));
        TextRender.draw(context, "Сохранённые конфиги", pageX + pad, listY + 25.0F, 14.85F, -1);
        String activeName = ConfigManager.INSTANCE.getActiveConfigName();
        String activeText = "Активный: " + (activeName == null ? "—" : activeName);
        TextRender.draw(
            context,
            trimText(activeText, 12.206F, pageW * 0.42F),
            pageX + pageW - pad - TextRender.width(trimText(activeText, 12.206F, pageW * 0.42F), 12.206F),
            listY + 24.0F,
            12.206F,
            -2130706433
        );
        float listTop = listY + 40.0F;
        float listBottom = listY + listH - 12.0F;
        float entryH = 58.0F;
        float entryGap = 8.0F;
        float totalHeight = Math.max(0.0F, configs.size() * (entryH + entryGap) - entryGap);
        float maxScroll = Math.max(0.0F, totalHeight - (listBottom - listTop));
        configsTargetScroll = MathHelper.clamp(configsTargetScroll, 0.0F, maxScroll);
        configsScroll = MathHelper.clamp(this.approach(configsScroll, configsTargetScroll, 12.0F), 0.0F, maxScroll);
        if (configs.isEmpty()) {
            String empty = "Пока нет сохранённых конфигов";
            TextRender.draw(context, empty, pageX + (pageW - TextRender.width(empty, 12.0F)) / 2.0F, listTop + 38.0F, 12.0F, -2130706433);
        } else {
            context.enableScissor((int)(pageX + 10.0F), (int)listTop, (int)(pageX + pageW - 10.0F), (int)listBottom);

            for (int index = 0; index < configs.size(); index++) {
                String name = configs.get(index);
                float entryY = listTop + index * (entryH + entryGap) - configsScroll;
                if (!(entryY + entryH < listTop) && !(entryY > listBottom)) {
                    float entryX = pageX + 12.25F;
                    float entryW = pageW - 24.5F;
                    boolean hovered = inside(mouseX, mouseY, entryX, entryY, entryW, entryH);
                    float appear = this.approach(CONFIG_ENTRY_ANIMS.getOrDefault(name, 0.0F), 1.0F, 10.0F);
                    float hover = this.approach(CONFIG_HOVER_ANIMS.getOrDefault(name, 0.0F), hovered ? 1.0F : 0.0F, 12.0F);
                    CONFIG_ENTRY_ANIMS.put(name, appear);
                    CONFIG_HOVER_ANIMS.put(name, hover);
                    context.getMatrices().pushMatrix();
                    float scale = 0.97F + appear * 0.03F;
                    float centerX = entryX + entryW / 2.0F;
                    float centerY = entryY + entryH / 2.0F;
                    context.getMatrices().translate(centerX, centerY);
                    context.getMatrices().scale(scale, scale);
                    context.getMatrices().translate(-centerX, -centerY);
                    RoundedRenderer.drawRoundedRect(context, entryX, entryY, entryW, entryH, 10.0F, fade(lerpColor(603979776, 855638015, hover), appear));
                    float iconX = entryX + 11.0F;
                    float iconY = entryY + 11.0F;
                    RoundedRenderer.drawRoundedRect(
                        context, iconX, iconY, 36.0F, 36.0F, 9.0F, fade(lerpColor(accentAt(iconX, iconY), -1, 0.08F), 0.31F * appear)
                    );
                    drawConfigIcon(context, iconX + 7.0F, iconY + 7.0F, 22.0F, fade(-1, appear));
                    float textX = iconX + 47.0F;
                    TextRender.draw(context, trimText(name, 14.85F, entryW - 250.0F), textX, entryY + 24.0F, 14.85F, fade(-1, appear));
                    boolean active = name.equalsIgnoreCase(activeName);
                    String author = "Автор: " + this.getConfigAuthorName();
                    TextRender.draw(context, author, textX, entryY + 42.0F, 12.206F, fade(active ? accentAt(textX, entryY + 42.0F) : -2130706433, appear));
                    float actionsX = entryX + entryW - 122.0F;
                    this.renderConfigAction(context, actionsX, entryY + 13.0F, 'g', "×", mouseX, mouseY, -38540, appear);
                    this.renderConfigAction(context, actionsX + 40.0F, entryY + 13.0F, 'k', "S", mouseX, mouseY, accentAt(actionsX + 40.0F, entryY), appear);
                    this.renderConfigAction(context, actionsX + 80.0F, entryY + 13.0F, 'i', "↓", mouseX, mouseY, -1, appear);
                    context.getMatrices().popMatrix();
                }
            }

            context.disableScissor();
        }
    }

    void renderConfigAction(DrawContext context, float x, float y, char icon, String fallback, float mouseX, float mouseY, int activeColor, float opacity) {
        boolean hovered = inside(mouseX, mouseY, x, y, 32.0F, 32.0F);
        RoundedRenderer.drawRoundedRect(context, x, y, 32.0F, 32.0F, 8.0F, fade(hovered ? activeColor : 553648127, hovered ? 0.18F * opacity : opacity));
        int color = fade(hovered ? activeColor : -2130706433, opacity);
        if (!IconTtfBridge.tryDrawIconsFontChar(context, icon, x + 7.0F, y + 7.0F, 18.0F, color)) {
            TextRender.draw(context, fallback, x + 10.0F, y + 21.0F, 14.0F, color);
        }
    }

    void renderMarkersPage(DrawContext context, float mouseX, float mouseY) {
        float pageX = 257.0F;
        float pageW = 720.0F;
        float y = 72.0F;
        float formH = 208.0F;
        float listY = y + formH + 10.0F;
        float listH = 536.0F - listY - 10.0F;
        RoundedRenderer.drawRoundedRect(context, pageX, y, pageW, formH, 12.0F, 1073741824);
        float pad = 14.0F;
        float innerX = pageX + pad;
        float innerW = pageW - pad * 2.0F;
        TextRender.draw(context, "Создать метку", innerX, y + 24.0F, 14.85F, -1);
        float nameY = y + 35.0F;
        this.renderMarkerInput(context, this.markerNameInput, "Название метки", innerX, nameY, innerW, 32.0F, this.markerInputFocus == 1);
        float coordsY = nameY + 38.0F;
        float coordGap = 6.0F;
        float coordW = (innerW - coordGap * 2.0F) / 3.0F;
        this.renderMarkerInput(context, this.markerXInput, "X", innerX, coordsY, coordW, 32.0F, this.markerInputFocus == 2);
        this.renderMarkerInput(context, this.markerYInput, "Y", innerX + coordW + coordGap, coordsY, coordW, 32.0F, this.markerInputFocus == 3);
        this.renderMarkerInput(context, this.markerZInput, "Z", innerX + (coordW + coordGap) * 2.0F, coordsY, coordW, 32.0F, this.markerInputFocus == 4);
        float iconsY = coordsY + 38.0F;
        float iconGap = 3.0F;
        float iconBox = (innerW - iconGap * (Markers.MARK_ICON_CHARS.length - 1)) / Markers.MARK_ICON_CHARS.length;

        for (int i = 0; i < Markers.MARK_ICON_CHARS.length; i++) {
            float iconX = innerX + i * (iconBox + iconGap);
            boolean selected = this.selectedMarkerIcon == i;
            RoundedRenderer.drawRoundedRect(context, iconX, iconsY, iconBox, 29.0F, 6.0F, selected ? fade(accentAt(iconX, iconsY), 0.42F) : 603979776);
            float iconDrawSize = 21.0F;
            float iconDrawX = iconX + (iconBox - iconDrawSize) / 2.0F;
            if (!IconTtfBridge.tryDrawIconsFontChar(context, Markers.MARK_ICON_CHARS[i], iconDrawX, iconsY + 4.0F, iconDrawSize, selected ? -1 : -2130706433)) {
                IconRender.draw(context, "diamond", iconDrawX + 2.0F, iconsY + 6.0F, iconDrawSize - 4.0F, selected ? -1 : -2130706433);
            }
        }

        float controlsY = iconsY + 36.0F;
        int[] colors = new int[]{
            this.markerBackgroundColor.getValue(), this.markerTextColor.getValue(), this.markerIconColor.getValue(), Markers.iconBackgroundColor.getValue()
        };

        for (int ix = 0; ix < MARKER_COLOR_LABELS.length; ix++) {
            String label = MARKER_COLOR_LABELS[ix];
            float labelX = markerColorLabelX(innerX, ix);
            float swatchX = labelX + TextRender.width(label, 10.5F) + 6.0F;
            TextRender.draw(context, label, labelX, controlsY + 20.5F, 10.5F, -2130706433);
            RoundedRenderer.drawRoundedRect(context, swatchX, controlsY + 8.0F, 16.0F, 16.0F, 4.5F, colors[ix]);
        }

        float createW = 134.0F;
        float createX = innerX + innerW - createW;
        float sizeX = markerColorsEndX(innerX) + 18.0F;
        float sizeW = createX - sizeX - 12.0F;
        TextRender.draw(context, "Размер", sizeX, controlsY + 20.0F, 11.0F, -1);
        String sizeText = String.format(Locale.ROOT, "%.2f", this.markerSize);
        float sliderX = sizeX + 52.0F;
        float sliderW = sizeW - 83.0F;
        float sliderY = controlsY + 14.0F;
        float ratio = MathHelper.clamp(this.markerSize - 1.0F, 0.0F, 1.0F);
        RoundedRenderer.drawRoundedRect(context, sliderX, sliderY, sliderW, 4.0F, 2.0F, 536870911);
        RoundedRenderer.drawRoundedRect(context, sliderX, sliderY, sliderW * ratio, 4.0F, 2.0F, accentAt(sliderX, sliderY));
        RoundedRenderer.drawRoundedRect(context, sliderX + sliderW * ratio - 5.0F, sliderY - 3.0F, 10.0F, 10.0F, 5.0F, -1);
        TextRender.draw(context, sizeText, sizeX + sizeW - TextRender.width(sizeText, 10.5F), controlsY + 20.0F, 10.5F, -2130706433);
        boolean createHover = inside(mouseX, mouseY, createX, controlsY, createW, 32.0F);
        RoundedRenderer.drawRoundedRect(
            context, createX, controlsY, createW, 32.0F, 8.0F, createHover ? accentAt(createX, controlsY) : fade(accentAt(createX, controlsY), 0.72F)
        );
        String create = "Создать метку";
        TextRender.draw(context, create, createX + (createW - TextRender.width(create, 12.0F)) / 2.0F, controlsY + 20.5F, 12.0F, -1);
        this.renderMarkersList(context, pageX, listY, pageW, listH, mouseX, mouseY);
    }

    void renderMarkerInput(DrawContext context, String value, String placeholder, float x, float y, float width, float height, boolean focused) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, 7.0F, focused ? fade(accentAt(x, y), 0.2F) : 603979776);
        String shown = value != null && !value.isEmpty() ? value : placeholder;
        int color = value != null && !value.isEmpty() ? -1 : -2130706433;
        String clipped = trimTextFromStart(shown, 12.0F, width - 17.0F);
        TextRender.draw(context, clipped, x + 8.0F, y + 20.5F, 12.0F, color);
        if (focused && System.currentTimeMillis() / 500L % 2L == 0L) {
            float cursorX = x + 8.0F + TextRender.width(clipped, 12.0F) + 1.0F;
            RoundedRenderer.drawRoundedRect(context, cursorX, y + 8.0F, 1.0F, height - 16.0F, 0.5F, -1);
        }
    }

    static float markerColorLabelX(float startX, int index) {
        float x = startX;

        for (int i = 0; i < index; i++) {
            x += TextRender.width(MARKER_COLOR_LABELS[i], 10.5F) + 16.0F + 18.0F;
        }

        return x;
    }

    static float markerColorSwatchX(float startX, int index) {
        return markerColorLabelX(startX, index) + TextRender.width(MARKER_COLOR_LABELS[index], 10.5F) + 6.0F;
    }

    static float markerColorsEndX(float startX) {
        int last = MARKER_COLOR_LABELS.length - 1;
        return markerColorSwatchX(startX, last) + 16.0F;
    }

    void renderMarkersList(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY) {
        Markers markers = ModuleManager.INSTANCE.getModule(Markers.class);
        List<Markers$MarkerData> list = markers == null ? Collections.emptyList() : markers.getMarkers();
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, 12.0F, 1073741824);
        TextRender.draw(context, "Ваши метки", x + 14.0F, y + 24.0F, 14.85F, -1);
        String count = Integer.toString(list.size());
        TextRender.draw(context, count, x + width - TextRender.width(count, 12.206F) - 14.0F, y + 23.0F, 12.206F, -2130706433);
        float listTop = y + 40.0F;
        float listBottom = y + height - 12.0F;
        float entryH = 52.0F;
        float gap = 7.0F;
        float totalHeight = Math.max(0.0F, list.size() * (entryH + gap) - gap);
        float maxScroll = Math.max(0.0F, totalHeight - (listBottom - listTop));
        this.markerListTargetScroll = MathHelper.clamp(this.markerListTargetScroll, 0.0F, maxScroll);
        this.markerListScroll = MathHelper.clamp(this.approach(this.markerListScroll, this.markerListTargetScroll, 12.0F), 0.0F, maxScroll);
        if (list.isEmpty()) {
            String empty = "Пока нет сохранённых меток";
            TextRender.draw(context, empty, x + (width - TextRender.width(empty, 12.0F)) / 2.0F, listTop + 28.0F, 12.0F, -2130706433);
        } else {
            context.enableScissor((int)(x + 10.0F), (int)listTop, (int)(x + width - 10.0F), (int)listBottom);

            for (int i = 0; i < list.size(); i++) {
                Markers$MarkerData marker = list.get(i);
                float entryY = listTop + i * (entryH + gap) - this.markerListScroll;
                if (!(entryY + entryH < listTop) && !(entryY > listBottom)) {
                    boolean hovered = inside(mouseX, mouseY, x + 12.0F, entryY, width - 24.0F, entryH);
                    RoundedRenderer.drawRoundedRect(context, x + 12.25F, entryY + 0.25F, width - 24.5F, entryH - 0.5F, 9.5F, hovered ? 788529151 : 603979776);
                    float iconX = x + 20.25F;
                    float iconY = entryY + 10.25F;
                    RenderUtil.drawVerticalGradientRoundedRect(
                        context,
                        iconX,
                        iconY,
                        31.5F,
                        31.5F,
                        7.5F,
                        Markers.getIconBackgroundGradientTop(marker.getIconBackgroundColor()),
                        Markers.getIconBackgroundGradientBottom(marker.getIconBackgroundColor())
                    );
                    if (!IconTtfBridge.tryDrawIconsFontChar(context, marker.getIconChar(), iconX + 5.0F, iconY + 5.0F, 22.0F, marker.getIconColor())) {
                        IconRender.draw(context, "diamond", iconX + 8.0F, iconY + 8.0F, 16.0F, marker.getIconColor());
                    }

                    float textX = iconX + 42.0F;
                    TextRender.draw(context, trimText(marker.getName(), 12.5F, width - 120.0F), textX, entryY + 20.0F, 12.5F, marker.getTextColor());
                    String coordinates = (int)marker.getX() + ", " + (int)marker.getY() + ", " + (int)marker.getZ();
                    TextRender.draw(context, coordinates, textX, entryY + 38.0F, 10.5F, -2130706433);
                    float deleteX = x + width - 42.0F;
                    boolean deleteHover = inside(mouseX, mouseY, deleteX, entryY + 13.0F, 25.0F, 25.0F);
                    RoundedRenderer.drawRoundedRect(context, deleteX, entryY + 13.0F, 25.0F, 25.0F, 6.0F, deleteHover ? 889146970 : 369098752);
                    TextRender.draw(context, "×", deleteX + 7.0F, entryY + 31.0F, 15.0F, deleteHover ? -38540 : -2130706433);
                }
            }

            context.disableScissor();
        }
    }

    boolean handleMarkersPageClick(float mouseX, float mouseY, int button) {
        if (button != 0) {
            return true;
        }

        float pageX = 257.0F;
        float pageW = 720.0F;
        float y = 72.0F;
        float formH = 208.0F;
        float listY = y + formH + 10.0F;
        float listH = 536.0F - listY - 10.0F;
        float innerX = pageX + 14.0F;
        float innerW = pageW - 28.0F;
        float nameY = y + 35.0F;
        if (inside(mouseX, mouseY, innerX, nameY, innerW, 32.0F)) {
            this.markerInputFocus = 1;
            return true;
        }

        float coordsY = nameY + 38.0F;
        float coordGap = 6.0F;
        float coordW = (innerW - coordGap * 2.0F) / 3.0F;

        for (int i = 0; i < 3; i++) {
            float coordX = innerX + i * (coordW + coordGap);
            if (inside(mouseX, mouseY, coordX, coordsY, coordW, 32.0F)) {
                this.markerInputFocus = i + 2;
                return true;
            }
        }

        float iconsY = coordsY + 38.0F;
        float iconGap = 3.0F;
        float iconBox = (innerW - iconGap * (Markers.MARK_ICON_CHARS.length - 1)) / Markers.MARK_ICON_CHARS.length;

        for (int ix = 0; ix < Markers.MARK_ICON_CHARS.length; ix++) {
            float iconX = innerX + ix * (iconBox + iconGap);
            if (inside(mouseX, mouseY, iconX, iconsY, iconBox, 29.0F)) {
                this.selectedMarkerIcon = ix;
                this.markerInputFocus = 0;
                return true;
            }
        }

        float controlsY = iconsY + 36.0F;
        ColorSetting[] colorSettings = new ColorSetting[]{this.markerBackgroundColor, this.markerTextColor, this.markerIconColor, Markers.iconBackgroundColor};

        for (int ixx = 0; ixx < colorSettings.length; ixx++) {
            float swatchX = markerColorSwatchX(innerX, ixx);
            if (inside(mouseX, mouseY, swatchX, controlsY + 8.0F, 16.0F, 16.0F)) {
                this.openColorPicker(colorSettings[ixx]);
                this.markerInputFocus = 0;
                return true;
            }
        }

        float createW = 134.0F;
        float createX = innerX + innerW - createW;
        float sizeX = markerColorsEndX(innerX) + 18.0F;
        float sizeW = createX - sizeX - 12.0F;
        float sliderX = sizeX + 52.0F;
        float sliderW = sizeW - 83.0F;
        if (inside(mouseX, mouseY, sliderX - 6.0F, controlsY, sliderW + 12.0F, 32.0F)) {
            this.markerSize = 1.0F + MathHelper.clamp((mouseX - sliderX) / sliderW, 0.0F, 1.0F);
            this.draggingMarkerSize = true;
            this.markerInputFocus = 0;
            return true;
        }

        if (inside(mouseX, mouseY, createX, controlsY, createW, 32.0F)) {
            this.addMarkerFromInput();
            return true;
        }

        Markers markers = ModuleManager.INSTANCE.getModule(Markers.class);
        if (markers != null) {
            float listTop = listY + 40.0F;
            float listBottom = listY + listH - 12.0F;
            float entryH = 52.0F;
            float gap = 7.0F;
            List<Markers$MarkerData> list = markers.getMarkers();

            for (int ixxx = 0; ixxx < list.size(); ixxx++) {
                float entryY = listTop + ixxx * (entryH + gap) - this.markerListScroll;
                float deleteX = pageX + pageW - 42.0F;
                if (entryY + entryH >= listTop && entryY <= listBottom && inside(mouseX, mouseY, deleteX, entryY + 13.0F, 25.0F, 25.0F)) {
                    list.remove(ixxx);
                    this.markerInputFocus = 0;
                    return true;
                }
            }
        }

        this.markerInputFocus = 0;
        return true;
    }

    boolean handleConfigsPageClick(float mouseX, float mouseY, int button) {
        if (button != 0) {
            return true;
        }

        float pageX = 257.0F;
        float pageY = 72.0F;
        float pageW = 720.0F;
        float pad = 14.0F;
        float controlY = pageY + 31.0F;
        float controlH = 36.0F;
        float folderW = 36.0F;
        float createW = 126.0F;
        float gap = 8.0F;
        float folderX = pageX + pageW - pad - folderW;
        float createX = folderX - gap - createW;
        float inputX = pageX + pad;
        float inputW = createX - gap - inputX;
        if (inside(mouseX, mouseY, inputX, controlY, inputW, controlH)) {
            configInputFocus = true;
            this.markerInputFocus = 0;
            return true;
        }

        if (inside(mouseX, mouseY, createX, controlY, createW, controlH)) {
            this.createOrImportConfig();
            return true;
        }

        if (inside(mouseX, mouseY, folderX, controlY, folderW, controlH)) {
            configInputFocus = false;

            try {
                Path directory = ConfigManager.INSTANCE.getConfigDirectory();
                Files.createDirectories(directory);
                Util.getOperatingSystem().open(directory);
            } catch (Throwable var32) {
            }

            return true;
        } else {
            float listY = pageY + 90.0F;
            float listH = 536.0F - listY - 10.0F;
            float listTop = listY + 40.0F;
            float listBottom = listY + listH - 12.0F;
            List<String> configs = new ArrayList<>(ConfigManager.INSTANCE.listConfigs());
            configs.sort(String.CASE_INSENSITIVE_ORDER);
            float entryH = 58.0F;
            float entryGap = 8.0F;

            for (int index = 0; index < configs.size(); index++) {
                String name = configs.get(index);
                float entryY = listTop + index * (entryH + entryGap) - configsScroll;
                if (!(entryY + entryH < listTop) && !(entryY > listBottom)) {
                    float entryX = pageX + 12.25F;
                    float entryW = pageW - 24.5F;
                    float actionsX = entryX + entryW - 122.0F;

                    for (int action = 0; action < 3; action++) {
                        float actionX = actionsX + action * 40.0F;
                        if (inside(mouseX, mouseY, actionX, entryY + 13.0F, 32.0F, 32.0F)) {
                            configInputFocus = false;
                            if (action == 0) {
                                if (ConfigManager.INSTANCE.deleteConfig(name)) {
                                    CONFIG_ENTRY_ANIMS.remove(name);
                                    CONFIG_HOVER_ANIMS.remove(name);
                                    CommandManager.addChatMessage("§cКонфиг удалён: §f" + name);
                                }
                            } else if (action == 1) {
                                ConfigManager.INSTANCE.saveConfig(name);
                                CommandManager.addChatMessage("§dКонфиг обновлён: §f" + name);
                            } else {
                                ConfigManager.INSTANCE.loadConfig(name);
                                CommandManager.addChatMessage("§aКонфиг загружен: §f" + name);
                            }

                            return true;
                        }
                    }
                }
            }

            configInputFocus = false;
            return true;
        }
    }

    void createOrImportConfig() {
        String value = configInput.trim();
        if (!value.isEmpty()) {
            if (value.toUpperCase(Locale.ROOT).startsWith("FLUX-")) {
                boolean imported = ConfigManager.INSTANCE.importFromCode(value);
                CommandManager.addChatMessage(imported ? "§aКонфиг из кода успешно применён." : "§cНе удалось импортировать конфиг: неверный FLUX-код.");
                if (imported) {
                    configInput = "";
                }

                configInputFocus = !imported;
            } else {
                String safeName = value.replaceAll("[\\\\/:*?\"<>|]", "_").replace("..", "_").trim();

                while (safeName.endsWith(".")) {
                    safeName = safeName.substring(0, safeName.length() - 1).trim();
                }

                if (safeName.length() > 48) {
                    safeName = safeName.substring(0, 48).trim();
                }

                if (safeName.isEmpty()) {
                    CommandManager.addChatMessage("§cУкажите корректное название конфига.");
                } else {
                    ConfigManager.INSTANCE.saveConfig(safeName);
                    CONFIG_ENTRY_ANIMS.put(safeName, 0.0F);
                    configInput = "";
                    configInputFocus = false;
                    CommandManager.addChatMessage("§aКонфиг создан: §f" + safeName);
                }
            }
        }
    }

    void addMarkerFromInput() {
        Markers markers = ModuleManager.INSTANCE.getModule(Markers.class);
        if (markers != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            double fallbackX = client.player == null ? 0.0 : client.player.getX();
            double fallbackY = client.player == null ? 0.0 : client.player.getY();
            double fallbackZ = client.player == null ? 0.0 : client.player.getZ();
            String name = this.markerNameInput.trim().isEmpty() ? "Метка" : this.markerNameInput.trim();
            char icon = Markers.MARK_ICON_CHARS[MathHelper.clamp(this.selectedMarkerIcon, 0, Markers.MARK_ICON_CHARS.length - 1)];
            markers.addMarker(
                new Markers$MarkerData(
                    name,
                    parseMarkerCoordinate(this.markerXInput, fallbackX),
                    parseMarkerCoordinate(this.markerYInput, fallbackY),
                    parseMarkerCoordinate(this.markerZInput, fallbackZ),
                    icon,
                    Markers.opaqueColor(this.markerBackgroundColor.getValue()),
                    this.markerTextColor.getValue(),
                    this.markerIconColor.getValue(),
                    255,
                    this.markerSize,
                    Markers.getIconBackgroundColor()
                )
            );
            this.markerNameInput = "";
            this.markerXInput = "";
            this.markerYInput = "";
            this.markerZInput = "";
            this.markerInputFocus = 0;
            CommandManager.addChatMessage("§dМетка создана: §f" + name);
        }
    }

    static double parseMarkerCoordinate(String value, double fallback) {
        if (value != null && !value.isBlank()) {
            try {
                return Double.parseDouble(value.trim().replace(',', '.'));
            } catch (NumberFormatException var4) {
                return fallback;
            }
        } else {
            return fallback;
        }
    }

    void openColorPicker(ColorSetting setting) {
        if (activeColorPicker == setting && this.colorPickerOpen) {
            this.colorPickerOpen = false;
        } else {
            activeColorPicker = setting;
            int pickerColor = setting.getValue();
            this.colorPickerHue = Color.RGBtoHSB(pickerColor >> 16 & 0xFF, pickerColor >> 8 & 0xFF, pickerColor & 0xFF, null)[0];
            this.colorPickerOpen = true;
        }
    }

    void renderCosmeticsHeader(DrawContext context) {
        float hX = 257.0F;
        float hY = 10.0F;
        float hW = 987.0F - hX - 10.0F;
        RoundedRenderer.drawRoundedRect(context, hX, hY, hW, 52.0F, 12.0F, 1073741824);
        int headerAccent = accentAt(hX + 16.0F, hY + 18.0F);
        if (!IconTtfBridge.tryDrawCategoryIcon(context, Category.COSMETICS, hX + 13.0F, hY + 16.5F, 18.0F, headerAccent)) {
            IconRender.draw(context, "flask", hX + 16.0F, hY + 19.5F, IconRender.getDesignSize("flask"), headerAccent);
        }

        TextRender.draw(context, "Cosmetics", hX + 42.0F, hY + 32.0F, 14.5F, -1);
        TextRender.draw(
            context, "—  3D Wings, Hats, Backpacks, Pets & Visuals", hX + 42.0F + TextRender.width("Cosmetics  ", 14.5F), hY + 32.0F, 12.0F, -2130706433
        );
    }

    void renderCosmeticsContent(DrawContext context, float mouseX, float mouseY) {
        float areaX = 257.0F;
        float areaW = 987.0F - areaX - 10.0F;
        String[] tabKeys = new String[]{"all", "wings", "hats", "weapons", "models"};
        String[] tabNames = new String[]{
            "All (" + CosmeticsManager.INSTANCE.getAllCosmetics().size() + ")",
            "Wings (" + CosmeticsManager.INSTANCE.getByCategory("wings").size() + ")",
            "Hats (" + CosmeticsManager.INSTANCE.getByCategory("hats").size() + ")",
            "Weapons (" + CosmeticsManager.INSTANCE.getByCategory("weapons").size() + ")",
            "Models (" + CosmeticsManager.INSTANCE.getByCategory("models").size() + ")"
        };
        float tabY = 70.0F - scrollOffset;
        float tabH = 32.0F;
        float tabW = (areaW - (tabNames.length - 1) * 8.0F) / tabNames.length;

        for (int tab = 0; tab < tabNames.length; tab++) {
            float tabX = areaX + tab * (tabW + 8.0F);
            boolean selected = cosmeticsTab == tab;
            RoundedRenderer.drawRoundedRect(context, tabX, tabY, tabW, tabH, 8.0F, selected ? accentAt(tabX, tabY) : 1073741824);
            float textWidth = TextRender.width(tabNames[tab], 11.5F);
            TextRender.draw(context, tabNames[tab], tabX + (tabW - textWidth) / 2.0F, tabY + 20.5F, 11.5F, selected ? -1 : -2130706433);
        }

        float startCardY = 112.0F - scrollOffset;
        float cardGap = 10.0F;
        float cardW = (areaW - 3.0F * cardGap) / 4.0F;
        float cardH = 145.0F;
        List<CosmeticItem> cosmetics = CosmeticsManager.INSTANCE.getByCategory(tabKeys[cosmeticsTab]);

        for (int index = 0; index < cosmetics.size(); index++) {
            CosmeticItem item = cosmetics.get(index);
            int column = index % 4;
            int row = index / 4;
            float cardX = areaX + column * (cardW + cardGap);
            float cardY = startCardY + row * (cardH + cardGap);
            if (!(cardY + cardH < 56.0F) && !(cardY > 536.0F)) {
                boolean equipped = CosmeticsManager.INSTANCE.isEquipped(item);
                boolean hovered = inside(mouseX, mouseY, cardX, cardY, cardW, cardH);
                int background = equipped ? 1308577370 : (hovered ? 1931616810 : 1073741824);
                RoundedRenderer.drawRoundedRect(context, cardX, cardY, cardW, cardH, 10.0F, background);
                if (equipped) {
                    RoundedRenderer.drawRoundedRect(context, cardX, cardY, cardW, 2.5F, 1.0F, accentAt(cardX, cardY));
                }

                float starX = cardX + cardW - 18.0F;
                float starY = cardY + 7.0F;
                TextRender.draw(context, equipped ? "★" : "☆", starX, starY + 11.0F, 11.0F, equipped ? -10166 : (hovered ? -1711276033 : 1090519039));
                float previewPad = 8.0F;
                float previewW = cardW - previewPad * 2.0F;
                float previewH = 82.0F;
                float previewX = cardX + previewPad;
                float previewY = cardY + 8.0F;
                this.renderCosmeticCard3DPreview(context, item, previewX, previewY, previewW, previewH, hovered);
                String itemName = trimText(item.getName(), 11.5F, cardW - 16.0F);
                float nameW = TextRender.width(itemName, 11.5F);
                float nameX = cardX + (cardW - nameW) / 2.0F;
                float nameY = cardY + cardH - 14.0F;
                TextRender.draw(context, itemName, nameX, nameY, 11.5F, -1);
            }
        }
    }

    void renderCosmeticCard3DPreview(DrawContext context, CosmeticItem item, float x, float y, float w, float h, boolean hovered) {
        if (item != null) {
            CosmeticsManager.INSTANCE.ensureModelLoaded(item);
            BedrockModel model = item.getModel();
            if (model == null) {
                RenderUtil.drawRoundedTexture(context, item.getPreviewId(), x, y, w, h, 8.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                float width = Math.max(0.01F, model.maxX - model.minX);
                float height = Math.max(0.01F, model.maxY - model.minY);
                float depth = Math.max(0.01F, model.maxZ - model.minZ);
                boolean isWings = "wings".equalsIgnoreCase(item.getCategory());
                boolean isHat = "hats".equalsIgnoreCase(item.getCategory());
                boolean isBack = "back".equalsIgnoreCase(item.getCategory());
                float autoYaw = isWings && depth > width * 1.35F ? 90.0F : 0.0F;
                float boundsW = autoYaw != 0.0F ? depth : width;
                float widthFill = isHat ? 0.9F : (isBack ? 0.86F : 0.84F);
                float heightFill = isHat ? 0.88F : 0.84F;
                float scale = Math.min(w * widthFill / Math.max(0.01F, boundsW), h * heightFill / Math.max(0.01F, height));
                float cx = (model.minX + model.maxX) * 0.5F;
                float cy = (model.minY + model.maxY) * 0.5F;
                float cz = (model.minZ + model.maxZ) * 0.5F;
                float screenCx = x + w * 0.5F;
                float previewOffsetY = HIGHER_WING_PREVIEWS.contains(item.getSlug())
                    ? -8.0F
                    : ("wings_7_ee401e1a".equals(item.getSlug()) ? 10.0F : (LOWER_WING_PREVIEWS.contains(item.getSlug()) ? 4.0F : 0.0F));
                float screenCy = y + h * 0.52F + previewOffsetY;
                long time = System.currentTimeMillis();
                float animTime = (float)(time % 100000L) * 0.001F;
                MatrixStack matrices = new MatrixStack();
                matrices.push();
                matrices.translate(screenCx, screenCy, 150.0F);
                matrices.scale(scale, -scale, scale);
                if (isWings) {
                    float hoverYaw = hovered ? (float)Math.sin(time % 3000L / 3000.0 * Math.PI * 2.0) * 14.0F : 0.0F;
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F + autoYaw + hoverYaw));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(hovered ? 6.0F : 2.0F));
                    if (OPPOSITE_VERTICAL_WING_PREVIEWS.contains(item.getSlug())) {
                        matrices.scale(-1.0F, 1.0F, 1.0F);
                    } else {
                        matrices.scale(1.0F, -1.0F, 1.0F);
                    }
                } else {
                    float hoverYaw = hovered ? (float)(time % 4200L / 4200.0 * 360.0) : 0.0F;
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F + hoverYaw));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(isHat ? 5.0F : 8.0F));
                    if ("hats_2_cf6887c0".equals(item.getSlug())) {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
                    }
                }

                matrices.translate(-cx, -cy, -cz);
                if (!model.renderGui(context, item.getTextureId(), matrices, item.getAnimation(), animTime)) {
                    RenderUtil.drawRoundedTexture(context, item.getPreviewId(), x, y, w, h, 8.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                }

                matrices.pop();
            }
        }
    }

    void renderEventsHeader(DrawContext context) {
        float hX = 257.0F;
        float hY = 10.0F;
        float hW = 987.0F - hX - 10.0F;
        float hH = 52.0F;
        RoundedRenderer.drawRoundedRect(context, hX, hY, hW, hH, 12.0F, 1073741824);
        float baseIconSize = IconRender.getDesignSize("clock");
        float iconSize = baseIconSize * 1.27116F;
        float iconGrowOffset = (iconSize - baseIconSize) / 2.0F;
        IconRender.draw(context, "clock", hX + 16.0F - iconGrowOffset, hY + 19.5F - iconGrowOffset, iconSize, accentAt(hX + 16.0F, hY + 19.5F));
        TextRender.draw(context, "Events/Mines", hX + 42.0F, hY + 32.0F, 14.5F, -1);
        TextRender.draw(
            context,
            "—  Server events: current, upcoming, and auto-mines",
            hX + 42.0F + TextRender.width("Events/Mines  ", 14.5F),
            hY + 32.0F,
            12.0F,
            -2130706433
        );
        String[] servers = new String[]{"FluxEvents", "Server"};
        float[] widths = new float[]{70.0F, 82.0F};
        float switchX = hX + hW - widths[0] - widths[1] - 18.0F;

        for (int index = 0; index < servers.length; index++) {
            float width = widths[index];
            boolean selected = eventsServer == index;
            RoundedRenderer.drawRoundedRect(context, switchX, hY + 12.0F, width, 30.0F, 8.0F, selected ? accentAt(switchX, hY + 12.0F) : 603979776);
            float textWidth = TextRender.width(servers[index], 11.0F);
            TextRender.draw(context, servers[index], switchX + (width - textWidth) / 2.0F, hY + 31.0F, 11.0F, selected ? -1 : -2130706433);
            switchX += width + 6.0F;
        }
    }

    void renderEventsContent(DrawContext context, float mouseX, float mouseY) {
        float areaX = 257.0F;
        float areaW = 987.0F - areaX - 10.0F;
        if (eventsServer == 1) {
            this.renderServerContent(context, mouseX, mouseY, areaX, areaW);
        } else {
            List<FluxEventsClient$FluxEvent> allEvents = FluxEventsClient.getEvents();
            List<FluxEventsClient$FluxEvent> activeEvents = new ArrayList<>();
            List<FluxEventsClient$FluxEvent> upcomingEvents = new ArrayList<>();

            for (FluxEventsClient$FluxEvent ev : allEvents) {
                if (ev.shouldDisplay()
                    && (
                        !ev.isActiveEvent()
                            || (EVENT_FILTERS.isEmpty() || EVENT_FILTERS.contains(ev.id))
                                && ("all".equals(eventPhaseFilter) || ev.phase != null && eventPhaseFilter.equalsIgnoreCase(ev.phase))
                    )) {
                    if (ev.isActiveEvent()) {
                        activeEvents.add(ev);
                    } else {
                        upcomingEvents.add(ev);
                    }
                }
            }

            List<FluxEventsClient$FluxMine> mines = FluxEventsClient.getMines()
                .stream()
                .filter(mine -> "all".equals(mineRarityFilter) || mine.nextMineRarity.name().equalsIgnoreCase(mineRarityFilter))
                .toList();
            float tabY = 70.0F - scrollOffset;
            float tabH = 32.0F;
            float tabW = (areaW - 24.0F) / 4.0F;
            String[] tabNames = new String[]{
                "Текущие (" + (FluxEventsClient.isEventsLoading() ? "…" : activeEvents.size()) + ")",
                "Предстоящие (" + (FluxEventsClient.isEventsLoading() ? "…" : upcomingEvents.size()) + ")",
                "Авто-шахты (" + (FluxEventsClient.isMinesLoading() ? "…" : mines.size()) + ")",
                "Фильтры"
            };

            for (int t = 0; t < 4; t++) {
                float tX = areaX + t * (tabW + 8.0F);
                boolean isSel = eventsTab == t;
                int tabBg = isSel ? accentAt(tX, tabY) : 1073741824;
                RoundedRenderer.drawRoundedRect(context, tX, tabY, tabW, tabH, 8.0F, tabBg);
                float textW = TextRender.width(tabNames[t], 12.0F);
                TextRender.draw(context, tabNames[t], tX + (tabW - textW) / 2.0F, tabY + 20.5F, 12.0F, isSel ? -1 : -2130706433);
            }

            float startCardY = 112.0F - scrollOffset;
            float cardW = 355.0F;
            float cardH = 76.0F;
            float cardGap = 10.0F;
            if (eventsTab == 3) {
                this.renderEventFilters(context, areaX, startCardY, areaW);
            } else {
                int selectedCount = eventsTab == 2 ? mines.size() : (eventsTab == 0 ? activeEvents.size() : upcomingEvents.size());
                if (selectedCount == 0) {
                    boolean loading = eventsTab == 2 ? FluxEventsClient.isMinesLoading() : FluxEventsClient.isEventsLoading();
                    String error = eventsTab == 2 ? FluxEventsClient.getLastMineError() : FluxEventsClient.getLastError();
                    String stateText = loading ? "Загрузка данных FluxEvents API…" : (!error.isBlank() ? "Ошибка API: " + error : "Сейчас данных нет");
                    float stateW = TextRender.width(stateText, 12.0F);
                    TextRender.draw(context, stateText, areaX + (areaW - stateW) / 2.0F, startCardY + 34.0F, 12.0F, loading ? -2130706433 : -38029);
                }

                if (eventsTab == 2) {
                    for (int i = 0; i < mines.size(); i++) {
                        FluxEventsClient$FluxMine mine = mines.get(i);
                        int col = i % 2;
                        int row = i / 2;
                        float cX = areaX + col * (cardW + cardGap);
                        float cY = startCardY + row * (cardH + cardGap);
                        if (!(cY + cardH < 56.0F) && !(cY > 536.0F)) {
                            boolean hov = inside(mouseX, mouseY, cX, cY, cardW, cardH);
                            RoundedRenderer.drawRoundedRect(context, cX, cY, cardW, cardH, 10.0F, hov ? 1428300330 : 1073741824);
                            String anText = mine.getDisplayServer();
                            float anW = TextRender.width(anText, 10.5F) + 12.0F;
                            RoundedRenderer.drawRoundedRect(context, cX + 10.0F, cY + 8.0F, anW, 18.0F, 5.0F, 721420287);
                            TextRender.draw(context, anText, cX + 16.0F, cY + 20.5F, 10.5F, -1);
                            String resetText = "Сброс: " + mine.getDisplayTime();
                            TextRender.draw(
                                context, resetText, cX + cardW - TextRender.width(resetText, 11.0F) - 12.0F, cY + 20.5F, 11.0F, accentAt(cX + cardW, cY)
                            );
                            String mineTitle = "Следующая шахта";
                            TextRender.draw(context, mineTitle, cX + 12.0F, cY + 44.0F, 13.0F, -1);
                            String rarLabel = mine.nextMineRarity.displayLabel();
                            int rarCol = mine.nextMineRarity == FluxEventsClient$MineRarityKind.LEGENDARY
                                ? -680437
                                : (mine.nextMineRarity == FluxEventsClient$MineRarityKind.MYTHICAL ? -5745161 : -12877066);
                            TextRender.draw(context, rarLabel, cX + 14.0F + TextRender.width(mineTitle + " ", 13.0F), cY + 44.0F, 12.0F, rarCol);
                            TextRender.draw(context, "Появится после сброса", cX + 12.0F, cY + 64.0F, 10.5F, -2130706433);
                            float playX = cX + cardW - 28.0F;
                            float playY = cY + 48.0F;
                            boolean playHov = inside(mouseX, mouseY, playX - 4.0F, playY - 4.0F, 22.0F, 22.0F);
                            TextRender.draw(context, "▶", playX, playY + 12.0F, 12.0F, playHov ? -1 : accentAt(playX, playY));
                        }
                    }
                } else {
                    List<FluxEventsClient$FluxEvent> list = eventsTab == 0 ? activeEvents : upcomingEvents;

                    for (int ix = 0; ix < list.size(); ix++) {
                        FluxEventsClient$FluxEvent evx = list.get(ix);
                        int col = ix % 2;
                        int row = ix / 2;
                        float cX = areaX + col * (cardW + cardGap);
                        float cY = startCardY + row * (cardH + cardGap);
                        if (!(cY + cardH < 56.0F) && !(cY > 536.0F)) {
                            boolean hov = inside(mouseX, mouseY, cX, cY, cardW, cardH);
                            RoundedRenderer.drawRoundedRect(context, cX, cY, cardW, cardH, 10.0F, hov ? 1428300330 : 1073741824);
                            String anText = "Анархия " + (evx.anarchyId > 0 ? evx.anarchyId : evx.server);
                            float anW = TextRender.width(anText, 10.5F) + 12.0F;
                            RoundedRenderer.drawRoundedRect(context, cX + 10.0F, cY + 8.0F, anW, 18.0F, 5.0F, 721420287);
                            TextRender.draw(context, anText, cX + 16.0F, cY + 20.5F, 10.5F, -1);
                            String timeLabel = evx.getTimerLabel();
                            int timeCol = eventsTab == 0 ? accentAt(cX + cardW, cY) : -2130706433;
                            TextRender.draw(context, timeLabel, cX + cardW - TextRender.width(timeLabel, 11.0F) - 12.0F, cY + 20.5F, 11.0F, timeCol);
                            TextRender.draw(context, evx.name, cX + 12.0F, cY + 44.0F, 13.0F, -1);
                            String rar = evx.rarity != null && !evx.rarity.isEmpty() ? evx.rarity : "";
                            int rarCol = -5745161;
                            if (rar.toLowerCase(Locale.ROOT).contains("богат")) {
                                rarCol = -42442;
                            } else if (rar.toLowerCase(Locale.ROOT).contains("легендар")) {
                                rarCol = -10166;
                            } else if (evx.name.contains("Метеорит")) {
                                rarCol = -680437;
                            } else if (evx.name.contains("Вулкан")) {
                                rarCol = -45478;
                            } else if (evx.name.contains("Резня")) {
                                rarCol = -1292135;
                            }

                            TextRender.draw(context, rar, cX + 14.0F + TextRender.width(evx.name + " ", 13.0F), cY + 44.0F, 12.0F, rarCol);
                            String coords = evx.getCoordinatesDisplay();
                            IconRender.draw(context, "ping", cX + 12.0F, cY + 54.0F, 12.0F, evx.hasLocation() ? accentAt(cX + 12.0F, cY + 54.0F) : -2130706433);
                            TextRender.draw(context, coords, cX + 28.0F, cY + 64.0F, 10.5F, -2130706433);
                            float playX = cX + cardW - 28.0F;
                            float playY = cY + 48.0F;
                            boolean playHov = inside(mouseX, mouseY, playX - 4.0F, playY - 4.0F, 22.0F, 22.0F);
                            TextRender.draw(context, "▶", playX, playY + 12.0F, 12.0F, playHov ? -1 : accentAt(playX, playY));
                        }
                    }
                }
            }
        }
    }

    void renderEventFilters(DrawContext context, float areaX, float y, float areaW) {
        float gap = 10.0F;
        float panelW = (areaW - gap) / 2.0F;
        this.renderFilterPanel(context, areaX, y, panelW, "Фильтры ивентов");
        this.renderFilterPanel(context, areaX + panelW + gap, y, panelW, "Фильтры шахт");
        String[][] eventOptions = new String[][]{
            {"all", "Все"},
            {"myst_beacon", "Призывной маяк"},
            {"meteor_rain", "Метеоритный дождь"},
            {"vulkan", "Вулкан"},
            {"altarundead", "Алтарь нежити"},
            {"deathmatch", "Дезматч"},
            {"beacon", "Маяк"},
            {"deathchest", "Сундук смерти"},
            {"geyser", "Гейзер"},
            {"hellm", "Адская резня"}
        };
        float optionY = y + 42.0F;

        for (int index = 0; index < eventOptions.length; index++) {
            float optionX = areaX + 12.0F + index % 2 * ((panelW - 30.0F) / 2.0F + 6.0F);
            float optionW = (panelW - 30.0F) / 2.0F;
            float rowY = optionY + index / 2 * 30.0F;
            this.drawFilterOption(
                context,
                optionX,
                rowY,
                optionW,
                eventOptions[index][1],
                "all".equals(eventOptions[index][0]) ? EVENT_FILTERS.isEmpty() : EVENT_FILTERS.contains(eventOptions[index][0])
            );
        }

        TextRender.draw(context, "Статус", areaX + 12.0F, y + 202.0F, 11.5F, -2130706433);
        String[][] phases = new String[][]{{"all", "Все"}, {"WAITING", "Ожидает"}, {"ACTIVATING", "Активация"}, {"RUNNING", "Активен"}};

        for (int index = 0; index < phases.length; index++) {
            float optionX = areaX + 12.0F + index % 2 * ((panelW - 30.0F) / 2.0F + 6.0F);
            float optionW = (panelW - 30.0F) / 2.0F;
            float rowY = y + 214.0F + index / 2 * 30.0F;
            this.drawFilterOption(context, optionX, rowY, optionW, phases[index][1], eventPhaseFilter.equals(phases[index][0]));
        }

        String[][] rarities = new String[][]{{"all", "Все"}, {"LEGENDARY", "Легендарные"}, {"MYTHICAL", "Мифические"}, {"NORMAL", "Обычные"}};
        float rightX = areaX + panelW + gap;

        for (int index = 0; index < rarities.length; index++) {
            float optionX = rightX + 12.0F;
            float rowY = y + 42.0F + index * 32.0F;
            this.drawFilterOption(context, optionX, rowY, panelW - 24.0F, rarities[index][1], mineRarityFilter.equals(rarities[index][0]));
        }
    }

    void renderFilterPanel(DrawContext context, float x, float y, float width, String title) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, 286.0F, 10.0F, 1073741824);
        TextRender.draw(context, title, x + 12.0F, y + 25.0F, 13.0F, -1);
        RoundedRenderer.drawRoundedRect(context, x + 12.0F, y + 32.0F, width - 24.0F, 1.0F, 0.5F, 352321535);
    }

    void drawFilterOption(DrawContext context, float x, float y, float width, String text, boolean selected) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, 24.0F, 6.0F, selected ? accentAt(x, y) : 603979776);
        float textWidth = TextRender.width(text, 10.5F);
        TextRender.draw(context, text, x + (width - textWidth) / 2.0F, y + 16.0F, 10.5F, selected ? -1 : -2130706433);
    }

    void renderServerContent(DrawContext context, float mouseX, float mouseY, float areaX, float areaW) {
        List<ServerEventsClient$ServerEvent> events = ServerEventsClient.getEvents()
            .stream()
            .filter(eventx -> SERVER_EVENT_FILTERS.isEmpty() || SERVER_EVENT_FILTERS.contains(eventx.id()))
            .toList();
        float startY = 70.0F - scrollOffset;
        float cardW = 355.0F;
        float cardH = 76.0F;
        float gap = 10.0F;
        if (events.isEmpty()) {
            String error = ServerEventsClient.getLastError();
            String state = ServerEventsClient.isLoading()
                ? "Загрузка данных Server API…"
                : (!error.isBlank() ? "Ошибка API: " + error : "Сейчас активных ивентов нет");
            float width = TextRender.width(state, 12.0F);
            TextRender.draw(context, state, areaX + (areaW - width) / 2.0F, startY + 40.0F, 12.0F, ServerEventsClient.isLoading() ? -2130706433 : -38029);
        } else {
            for (int index = 0; index < events.size(); index++) {
                ServerEventsClient$ServerEvent event = events.get(index);
                int column = index % 2;
                int row = index / 2;
                float x = areaX + column * (cardW + gap);
                float y = startY + row * (cardH + gap);
                if (!(y + cardH < 56.0F) && !(y > 536.0F)) {
                    boolean hovered = inside(mouseX, mouseY, x, y, cardW, cardH);
                    RoundedRenderer.drawRoundedRect(context, x, y, cardW, cardH, 10.0F, hovered ? 1428300330 : 1073741824);
                    float serverW = TextRender.width(event.serverName(), 10.5F) + 12.0F;
                    RoundedRenderer.drawRoundedRect(context, x + 10.0F, y + 8.0F, serverW, 18.0F, 5.0F, 721420287);
                    TextRender.draw(context, event.serverName(), x + 16.0F, y + 20.5F, 10.5F, -1);
                    TextRender.draw(context, "Активен", x + cardW - TextRender.width("Активен", 11.0F) - 12.0F, y + 20.5F, 11.0F, -11216500);
                    TextRender.draw(context, event.name(), x + 12.0F, y + 48.0F, 13.0F, -1);
                    int rarityColor = this.ServerRarityColor(event.rarity());
                    TextRender.draw(context, event.rarity(), x + 14.0F + TextRender.width(event.name() + " ", 13.0F), y + 48.0F, 12.0F, rarityColor);
                }
            }
        }
    }

    int ServerRarityColor(String rarity) {
        String normalized = rarity.toLowerCase(Locale.ROOT);
        if (normalized.contains("легендар") || normalized.contains("роскош")) {
            return -10166;
        } else if (normalized.contains("эпичес") || normalized.contains("зажит")) {
            return -5745161;
        } else if (normalized.contains("редк")) {
            return -12877066;
        } else if (normalized.contains("смерт")) {
            return -45478;
        } else if (normalized.contains("мирн")) {
            return -15681151;
        } else {
            return normalized.contains("взрыв") ? -30172 : -2130706433;
        }
    }

    boolean handleEventFiltersClick(float mouseX, float mouseY, float areaX, float y, float areaW) {
        float gap = 10.0F;
        float panelW = (areaW - gap) / 2.0F;
        String[][] eventOptions = new String[][]{
            {"all", "Все"},
            {"myst_beacon", "Призывной маяк"},
            {"meteor_rain", "Метеоритный дождь"},
            {"vulkan", "Вулкан"},
            {"altarundead", "Алтарь нежити"},
            {"deathmatch", "Дезматч"},
            {"beacon", "Маяк"},
            {"deathchest", "Сундук смерти"},
            {"geyser", "Гейзер"},
            {"hellm", "Адская резня"}
        };

        for (int index = 0; index < eventOptions.length; index++) {
            float optionX = areaX + 12.0F + index % 2 * ((panelW - 30.0F) / 2.0F + 6.0F);
            float optionW = (panelW - 30.0F) / 2.0F;
            float optionY = y + 42.0F + index / 2 * 30.0F;
            if (inside(mouseX, mouseY, optionX, optionY, optionW, 24.0F)) {
                String value = eventOptions[index][0];
                if ("all".equals(value)) {
                    EVENT_FILTERS.clear();
                } else if (!EVENT_FILTERS.add(value)) {
                    EVENT_FILTERS.remove(value);
                }

                return true;
            }
        }

        String[][] phases = new String[][]{{"all", "Все"}, {"WAITING", "Ожидает"}, {"ACTIVATING", "Активация"}, {"RUNNING", "Активен"}};

        for (int indexx = 0; indexx < phases.length; indexx++) {
            float optionX = areaX + 12.0F + indexx % 2 * ((panelW - 30.0F) / 2.0F + 6.0F);
            float optionW = (panelW - 30.0F) / 2.0F;
            float optionY = y + 214.0F + indexx / 2 * 30.0F;
            if (inside(mouseX, mouseY, optionX, optionY, optionW, 24.0F)) {
                eventPhaseFilter = phases[indexx][0];
                return true;
            }
        }

        String[][] rarities = new String[][]{{"all", "Все"}, {"LEGENDARY", "Легендарные"}, {"MYTHICAL", "Мифические"}, {"NORMAL", "Обычные"}};
        float rightX = areaX + panelW + gap;

        for (int indexxx = 0; indexxx < rarities.length; indexxx++) {
            float optionY = y + 42.0F + indexxx * 32.0F;
            if (inside(mouseX, mouseY, rightX + 12.0F, optionY, panelW - 24.0F, 24.0F)) {
                mineRarityFilter = rarities[indexxx][0];
                return true;
            }
        }

        return true;
    }

    void renderModules(DrawContext context, float mouseX, float mouseY) {
        List<Module> modules = this.getFilteredModules();
        float[] columnY = new float[2];
        Arrays.fill(columnY, 72.0F);

        for (int index = 0; index < modules.size(); index++) {
            Module module = modules.get(index);
            int col = index % 2;
            float cardX = 257.0F + col * 365.0F;
            float cardY = columnY[col] - scrollOffset;
            columnY[col] += 67.2F;
            if (cardY + 57.2F < 72.0F || cardY > 536.0F) {
                continue;
            }

            boolean enabled = module.isEnabled();
            float toggleAnim = TOGGLE_ANIMS.compute(module, (m, prev) -> {
                float target = enabled ? 1.0F : 0.0F;
                return prev == null ? target : this.approach(prev, target, 10.0F);
            });
            boolean hovered = inside(mouseX, mouseY, cardX, cardY, 355.0F, 57.2F);
            float hoverAnim = HOVER_ANIMS.compute(module, (m, prev) -> {
                float target = hovered || settingsModule == module ? 1.0F : 0.0F;
                return prev == null ? target : this.approach(prev, target, 10.0F);
            });
            float opacity = 0.6F + 0.4F * toggleAnim;
            int cardBg = fade(1073741824, opacity);
            if (hoverAnim > 0.01F) {
                cardBg = lerpColor(cardBg, 1428300330, hoverAnim);
            }

            RoundedRenderer.drawRoundedRect(context, cardX, cardY, 355.0F, 57.2F, 12.0F, cardBg);
            int cardAccent = accentAt(cardX, cardY);
            int iconColor = lerpColor(fade(-2130706433, opacity), fade(cardAccent, opacity), toggleAnim);
            float moduleIconSize = IconRender.getDesignSize("diamond") * 1.1F;
            IconRender.draw(context, "diamond", cardX + 14.0F, cardY + 20.7F, moduleIconSize, iconColor);
            TextRender.draw(context, module.getName(), cardX + 42.0F, cardY + 23.1F, 14.85F, fade(-1, opacity));
            this.renderModuleDescription(context, module, cardX, cardY, mouseX, mouseY, fade(-2130706433, opacity));
            String bind = module.isBinding() ? "[...]" : bindName(module);
            int bindColor = module.isBinding() ? cardAccent : fade(-2130706433, opacity);
            TextRender.draw(context, bind, cardX + 297.0F - TextRender.width(bind, 15.665F), cardY + 33.55F, 15.665F, bindColor);
            float trackX = cardX + 313.4F;
            float trackY = cardY + 20.9F;
            int currentTrackColor = lerpColor(fade(536870911, opacity), fade(cardAccent, opacity), toggleAnim);
            int currentKnobColor = lerpColor(fade(-2130706433, opacity), fade(-1, opacity), toggleAnim);
            this.drawSwitch(context, trackX, trackY, 28.6F, 15.4F, toggleAnim, currentTrackColor, currentKnobColor, 11.8F);
        }
    }

    void renderSettingsPanel(DrawContext context, float mouseX, float mouseY) {
        Module module = settingsModule;
        if (module == null) {
            return;
        }

        float x = SETTINGS_X;
        float y = SETTINGS_Y;
        float w = SETTINGS_W;
        RoundedRenderer.drawRoundedRect(context, x, y, w, SETTINGS_H, 12.0F, 0xD0101420);
        RoundedRenderer.drawRoundedOutline(context, x + 1.0F, y + 1.0F, w - 2.0F, SETTINGS_H - 2.0F, 11.0F, 1.1F, withAlpha(ACCENT, 120));
        TextRender.draw(context, module.getName(), x + 16.0F, y + 19.0F, 14.85F, -1);
        TextRender.draw(context, "×", x + w - 28.0F, y + 18.0F, 14.0F, -1);

        context.enableScissor((int)(x + 8.0F), (int)SETTINGS_TOP, (int)(x + w - 8.0F), (int)SETTINGS_BOTTOM);
        float settingY = SETTINGS_TOP + 6.0F - settingsScroll;
        for (Setting<?> setting : module.getSettings()) {
            float visibility = SETTING_VISIBILITY_ANIMS.compute(setting, (key, previous) -> {
                float target = setting.isVisible() ? 1.0F : 0.0F;
                return previous == null ? target : this.approach(previous, target, 8.5F);
            });
            if (visibility <= 0.01F) {
                continue;
            }

            float sH = 26.8F;
            this.renderSetting(context, module, setting, x + 12.0F, settingY, w - 24.0F, sH, mouseX, mouseY);
            settingY += 30.8F * visibility;
        }
        context.disableScissor();
    }

    void renderSetting(DrawContext context, Module module, Setting<?> setting, float x, float y, float w, float h, float mouseX, float mouseY) {
        RoundedRenderer.drawRoundedRect(context, x, y, w, h, 6.0F, 603979776);
        if (setting instanceof ItemHighlightSetting itemSetting) {
            ColorSetting colorProxy = ITEM_COLOR_PROXIES.computeIfAbsent(
                itemSetting, keyx -> new ColorSetting(keyx.getName() + " — цвет", new Color(0xFF000000 | keyx.getColor() & 16777215, true))
            );
            if (activeColorPicker == colorProxy) {
                itemSetting.setColor(colorProxy.getValue() & 16777215);
            } else if ((colorProxy.getValue() & 16777215) != (itemSetting.getColor() & 16777215)) {
                colorProxy.setValue(0xFF000000 | itemSetting.getColor() & 16777215);
            }

            context.drawItem(itemSetting.getItem().getDefaultStack(), Math.round(x + 5.0F), Math.round(y + 5.0F));
            TextRender.draw(context, trimText(itemSetting.getDisplayName(), 12.65F, w - 92.0F), x + 29.0F, y + 17.6F, 12.65F, -1);
            float switchWidth = 24.2F;
            float switchHeight = 13.2F;
            float swX = x + w - switchWidth - 6.6F;
            float swY = y + (h - switchHeight) / 2.0F;
            float previewSize = 13.2F;
            float previewX = swX - previewSize - 7.0F;
            RoundedRenderer.drawRoundedRect(context, previewX, swY, previewSize, previewSize, 3.2F, 0xFF000000 | itemSetting.getColor() & 16777215);
            float toggle = SETTING_GENERIC_TOGGLE_ANIMS.compute(
                itemSetting,
                (keyx, previous) -> previous == null
                    ? (itemSetting.getValue() ? 1.0F : 0.0F)
                    : this.approach(previous, itemSetting.getValue() ? 1.0F : 0.0F, 11.0F)
            );
            this.drawCompactSettingSwitch(context, swX, swY, switchWidth, switchHeight, toggle, x, y);
        } else if (setting instanceof ItemScaleSetting itemSetting) {
            context.drawItem(itemSetting.getItem().getDefaultStack(), Math.round(x + 5.0F), Math.round(y + 5.0F));
            TextRender.draw(context, trimText(itemSetting.getDisplayName(), 12.65F, w - 70.0F), x + 29.0F, y + 17.6F, 12.65F, -1);
            float switchWidth = 24.2F;
            float switchHeight = 13.2F;
            float swX = x + w - switchWidth - 6.6F;
            float swY = y + (h - switchHeight) / 2.0F;
            float toggle = SETTING_GENERIC_TOGGLE_ANIMS.compute(
                itemSetting,
                (keyx, previous) -> previous == null
                    ? (itemSetting.getValue() ? 1.0F : 0.0F)
                    : this.approach(previous, itemSetting.getValue() ? 1.0F : 0.0F, 11.0F)
            );
            this.drawCompactSettingSwitch(context, swX, swY, switchWidth, switchHeight, toggle, x, y);
        } else if (setting instanceof BooleanSetting boolSetting) {
            TextRender.draw(context, boolSetting.getDisplayName(), x + 8.0F, y + 17.6F, 12.65F, -1);
            boolean val = boolSetting.getValue();
            float toggleAnim = SETTING_TOGGLE_ANIMS.compute(boolSetting, (settingKey, previous) -> {
                float target = val ? 1.0F : 0.0F;
                return previous == null ? target : this.approach(previous, target, 11.0F);
            });
            float switchWidth = 24.2F;
            float switchHeight = 13.2F;
            float swX = x + w - switchWidth - 6.6F;
            float swY = y + (h - switchHeight) / 2.0F;
            int settingAccent = accentAt(x, y);
            int switchColor = lerpColor(536870911, settingAccent, toggleAnim);
            this.drawSwitch(context, swX, swY, switchWidth, switchHeight, toggleAnim, switchColor, -1, 11.8F);
        } else if (setting instanceof NumberSetting numSetting) {
            float sliderW = 132.0F;
            float sliderX = x + w - sliderW - 8.0F;
            float sliderH = 4.4F;
            float sliderY = y + (h - sliderH) / 2.0F;
            float targetRatio = (float)((numSetting.getValue() - numSetting.getMin()) / (numSetting.getMax() - numSetting.getMin()));
            targetRatio = MathHelper.clamp(targetRatio, 0.0F, 1.0F);
            float sliderRatio = targetRatio;
            float ratio = SLIDER_VALUE_ANIMS.compute(
                numSetting, (keyx, previous) -> previous == null ? sliderRatio : this.approach(previous, sliderRatio, 13.0F)
            );
            String valueText = formatNumber(numSetting.getValue());
            float valueWidth = TextRender.width(valueText, 12.65F);
            float valueX = sliderX - valueWidth - 7.0F;
            float labelMaxWidth = Math.max(24.0F, valueX - x - 14.0F);
            String label = trimText(numSetting.getDisplayName(), 12.65F, labelMaxWidth);
            TextRender.draw(context, label, x + 8.0F, y + 17.6F, 12.65F, -1);
            TextRender.draw(context, valueText, valueX, y + 17.6F, 12.65F, -2130706433);
            RoundedRenderer.drawRoundedRect(context, sliderX, sliderY, sliderW, sliderH, sliderH / 2.0F, 536870911);
            if (ratio > 0.01F) {
                RoundedRenderer.drawRoundedRect(context, sliderX, sliderY, sliderW * ratio, sliderH, sliderH / 2.0F, accentAt(sliderX, sliderY));
            }

            if (this.draggingSlider == numSetting) {
                this.sliderCursorRequested = true;
            }

            float pressAnim = SLIDER_PRESS_ANIMS.compute(numSetting, (keyx, previous) -> {
                float target = this.draggingSlider == numSetting ? 1.0F : 0.0F;
                return previous == null ? target : this.approach(previous, target, 14.0F);
            });
            float thumbCenterX = sliderX + sliderW * ratio;
            float thumbWidth = MathHelper.lerp(pressAnim, 10.5F, 12.5F);
            float thumbHeight = MathHelper.lerp(pressAnim, 10.5F, 7.8F);
            RoundedRenderer.drawRoundedRect(
                context, thumbCenterX - thumbWidth / 2.0F, y + h / 2.0F - thumbHeight / 2.0F, thumbWidth, thumbHeight, thumbHeight / 2.0F, -1
            );
        } else if (setting instanceof ModeSetting modeSetting) {
            String val = modeSetting.getValue();
            float valW = modeFieldWidth(modeSetting);
            float valX = x + w - valW - 8.0F;
            TextRender.draw(context, trimText(modeSetting.getDisplayName(), 12.65F, Math.max(24.0F, valX - x - 14.0F)), x + 8.0F, y + 17.6F, 12.65F, -1);
            boolean hovered = inside(mouseX, mouseY, valX, y + 4.4F, valW, h - 8.8F);
            RoundedRenderer.drawRoundedRect(context, valX, y + 4.4F, valW, h - 8.8F, 4.4F, hovered ? 855638015 : 536870911);
            String shownValue = trimText(val, 12.65F, valW - 13.2F);
            float shownWidth = TextRender.width(shownValue, 12.65F);
            TextRender.draw(context, shownValue, valX + (valW - shownWidth) / 2.0F, y + 17.6F, 12.65F, accentAt(valX, y));
        } else if (setting instanceof MultiBooleanSetting multiSetting) {
            TextRender.draw(context, multiSetting.getDisplayName(), x + 8.0F, y + 17.6F, 12.65F, -1);
            String full = multiDisplayValue(multiSetting);
            float valW = multiFieldWidth(multiSetting);
            float valX = x + w - valW - 8.0F;
            boolean hovered = inside(mouseX, mouseY, valX, y + 4.4F, valW, h - 8.8F);
            float hoverAnim = MULTI_HOVER_ANIMS.compute(multiSetting, (keyx, previous) -> {
                float target = hovered ? 1.0F : 0.0F;
                return previous == null ? target : this.approach(previous, target, 8.0F);
            });
            RoundedRenderer.drawRoundedRect(context, valX, y + 4.4F, valW, h - 8.8F, 4.4F, hovered ? 855638015 : 536870911);
            float textMax = valW - 13.2F;
            float fullWidth = TextRender.width(full, 12.65F);
            float textX = fullWidth <= textMax ? valX + (valW - fullWidth) / 2.0F : valX + 6.6F;
            context.enableScissor((int)valX, (int)y, (int)(valX + valW), (int)(y + h));
            float marqueeOffset = MULTI_SCROLL_OFFSETS.compute(multiSetting, (keyx, previous) -> {
                float offset = previous == null ? 0.0F : previous;
                if (hovered && fullWidth > textMax) {
                    float cycleLength = fullWidth + 24.0F;
                    offset += 30.6F * this.frameDeltaSeconds * Math.max(0.15F, hoverAnim);
                    if (offset >= cycleLength) {
                        offset -= cycleLength;
                    }

                    return offset;
                } else {
                    return this.approach(offset, 0.0F, 7.0F);
                }
            });
            int fieldAccent = accentAt(valX, y);
            TextRender.draw(context, full, textX - marqueeOffset, y + 17.6F, 12.65F, fieldAccent);
            if (fullWidth > textMax && hoverAnim > 0.01F) {
                TextRender.draw(context, full, textX - marqueeOffset + fullWidth + 24.0F, y + 17.6F, 12.65F, fade(fieldAccent, hoverAnim));
            }

            context.disableScissor();
        } else if (setting instanceof ActionSetting actionSetting) {
            float buttonX = x + 6.0F;
            float buttonY = y + 4.0F;
            float buttonW = w - 12.0F;
            float buttonH = h - 8.0F;
            boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH);
            RoundedRenderer.drawRoundedRect(context, buttonX, buttonY, buttonW, buttonH, 5.0F, hovered ? fade(accentAt(buttonX, buttonY), 0.28F) : 536870911);
            float textWidth = TextRender.width(actionSetting.getDisplayName(), 12.65F);
            TextRender.draw(
                context,
                trimText(actionSetting.getDisplayName(), 12.65F, buttonW - 16.0F),
                buttonX + Math.max(8.0F, (buttonW - textWidth) / 2.0F),
                y + 17.6F,
                12.65F,
                hovered ? accentAt(buttonX, buttonY) : -1
            );
        } else if (setting instanceof StringSetting stringSetting) {
            float macroShift = this.renderMacroDeleteAffordance(context, module, setting, x, y, w, h, mouseX, mouseY);
            TextRender.draw(context, stringSetting.getDisplayName(), x + 8.0F + macroShift, y + 17.6F, 12.65F, -1);
            float fieldW = 154.0F;
            float fieldX = x + w - fieldW - 8.0F;
            boolean focused = activeTextSetting == stringSetting;
            RoundedRenderer.drawRoundedRect(context, fieldX, y + 4.4F, fieldW, h - 8.8F, 4.4F, focused ? 956301311 : 536870911);
            String rawValue = stringSetting.getValue() == null ? "" : stringSetting.getValue();
            String shownValue = trimTextFromStart(rawValue, 12.65F, fieldW - 14.0F);
            int valueColor = rawValue.isEmpty() && !focused ? -2130706433 : -1;
            TextRender.draw(context, rawValue.isEmpty() && !focused ? "Введите текст" : shownValue, fieldX + 6.6F, y + 17.6F, 12.65F, valueColor);
            if (focused && System.currentTimeMillis() / 500L % 2L == 0L) {
                float cursorX = fieldX + 6.6F + TextRender.width(shownValue, 12.65F) + 1.5F;
                RoundedRenderer.drawRoundedRect(context, cursorX, y + 7.0F, 1.2F, h - 14.0F, 0.6F, -1);
            }
        } else if (setting instanceof ColorSetting colorSetting) {
            TextRender.draw(context, colorSetting.getDisplayName(), x + 8.0F, y + 17.6F, 12.65F, -1);
            int col = colorSetting.getValue();
            float boxSize = 15.4F;
            float boxX = x + w - boxSize - 8.0F;
            float boxY = y + (h - boxSize) / 2.0F;
            RoundedRenderer.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 4.0F, col);
            String hex = String.format("#%06X", 16777215 & col);
            int hexCol = activeColorPicker == colorSetting ? accentAt(boxX, boxY) : -2130706433;
            TextRender.draw(context, hex, boxX - TextRender.width(hex, 11.0F) - 6.6F, y + 16.5F, 11.0F, hexCol);
        } else if (setting instanceof BindSetting bindSetting) {
            float macroShift = this.renderMacroDeleteAffordance(context, module, setting, x, y, w, h, mouseX, mouseY);
            TextRender.draw(context, bindSetting.getDisplayName(), x + 8.0F + macroShift, y + 17.6F, 12.65F, -1);
            boolean isThisBinding = bindingSetting == bindSetting;
            String bindName = isThisBinding
                ? "[...]"
                : "[" + (bindSetting.getValue() == 0 ? "-" : Type.KEYSYM.createFromCode(bindSetting.getValue()).getLocalizedText().getString()) + "]";
            int bindCol = isThisBinding ? accentAt(x + w, y) : -2130706433;
            TextRender.draw(context, bindName, x + w - TextRender.width(bindName, 12.65F) - 8.0F, y + 17.6F, 12.65F, bindCol);
        } else if (setting instanceof FriendRequestRowSetting reqSetting) {
            FriendScreenClient$Entry entry = reqSetting.getEntry();
            String name = entry != null ? entry.name : "Заявка #" + (reqSetting.getIndex() + 1);
            TextRender.draw(context, trimText(name, 12.65F, w - 145.0F), x + 8.0F, y + 17.6F, 12.65F, -1);
            float btnH = h - 9.0F;
            float btnY = y + 4.5F;
            float declineW = 64.0F;
            float declineX = x + w - declineW - 8.0F;
            boolean declineHovered = inside(mouseX, mouseY, declineX, btnY, declineW, btnH);
            int declineBg = declineHovered ? 1442795098 : 587202559;
            int declineTextCol = declineHovered ? -38026 : -2130706433;
            RoundedRenderer.drawRoundedRect(context, declineX, btnY, declineW, btnH, 4.4F, declineBg);
            float decTextW = TextRender.width("Отклонить", 12.65F);
            TextRender.draw(context, "Отклонить", declineX + (declineW - decTextW) / 2.0F, y + 17.6F, 12.65F, declineTextCol);
            float btnW = 58.0F;
            float acceptX = declineX - btnW - 8.0F;
            boolean acceptHovered = inside(mouseX, mouseY, acceptX, btnY, btnW, btnH);
            int acceptBg = acceptHovered ? 1714991705 : 607695449;
            int acceptTextCol = acceptHovered ? -10296710 : -580722054;
            RoundedRenderer.drawRoundedRect(context, acceptX, btnY, btnW, btnH, 4.4F, acceptBg);
            float textW = TextRender.width("Принять", 12.65F);
            TextRender.draw(context, "Принять", acceptX + (btnW - textW) / 2.0F, y + 17.6F, 12.65F, acceptTextCol);
        } else if (setting instanceof FriendRowSetting friendSetting) {
            FriendScreenClient$Entry entry = friendSetting.getEntry();
            String name = entry != null ? entry.name : "Друг #" + (friendSetting.getIndex() + 1);
            float nameX = x + 8.0F;
            if (entry != null && entry.online) {
                RoundedRenderer.drawRoundedRect(context, x + 8.0F, y + h / 2.0F - 2.5F, 5.0F, 5.0F, 2.5F, -11870592);
                nameX = x + 17.0F;
            }

            TextRender.draw(context, trimText(name, 12.65F, w - 165.0F), nameX, y + 17.6F, 12.65F, -1);
            boolean isBinding = bindingFriendSetting == friendSetting;
            int key = friendSetting.getKey();
            String bindText = isBinding ? "[...]" : "[" + (key == 0 ? "-" : Type.KEYSYM.createFromCode(key).getLocalizedText().getString()) + "]";
            float bindW = Math.max(34.0F, TextRender.width(bindText, 12.65F) + 8.0F);
            float bindX = x + w - bindW - 8.0F;
            float btnH = h - 9.0F;
            float btnY = y + 4.5F;
            boolean bindHovered = inside(mouseX, mouseY, bindX, btnY, bindW, btnH);
            int bindBg = isBinding ? 1157582426 : (bindHovered ? 872415231 : 536870911);
            int bindCol = isBinding ? ACCENT : -2130706433;
            RoundedRenderer.drawRoundedRect(context, bindX, btnY, bindW, btnH, 4.4F, bindBg);
            float bindTextW = TextRender.width(bindText, 12.65F);
            TextRender.draw(context, bindText, bindX + (bindW - bindTextW) / 2.0F, y + 17.6F, 12.65F, bindCol);
            boolean isHidden = friendSetting.isHidden();
            String toggleText = isHidden ? "Показать" : "Скрыть";
            float toggleW = 62.0F;
            float toggleX = bindX - toggleW - 6.0F;
            boolean toggleHovered = inside(mouseX, mouseY, toggleX, btnY, toggleW, btnH);
            int toggleBg = isHidden ? (toggleHovered ? 1157601536 : 587176192) : (toggleHovered ? 1157627903 : 536870911);
            int toggleCol = isHidden ? -19641 : (toggleHovered ? -1 : -2130706433);
            RoundedRenderer.drawRoundedRect(context, toggleX, btnY, toggleW, btnH, 4.4F, toggleBg);
            float toggleTextW = TextRender.width(toggleText, 12.65F);
            TextRender.draw(context, toggleText, toggleX + (toggleW - toggleTextW) / 2.0F, y + 17.6F, 12.65F, toggleCol);
        } else if (setting instanceof LabelSetting labelSetting) {
            String labelx = labelSetting.getDisplayName();
            float labelWidth = TextRender.width(labelx, 12.65F);
            float labelX = labelSetting.isCentered() ? x + (w - labelWidth) / 2.0F : x + 8.0F;
            TextRender.draw(context, labelx, labelX, y + 17.6F, 12.65F, -1);
        } else {
            TextRender.draw(context, setting.getDisplayName(), x + 8.0F, y + 17.6F, 12.65F, -1);
        }
    }

    float renderMacroDeleteAffordance(DrawContext context, Module module, Setting<?> setting, float x, float y, float w, float h, float mouseX, float mouseY) {
        if (macroSettingIndex(module, setting) < 0) {
            return 0.0F;
        }

        float labelWidth = Math.min(190.0F, Math.max(40.0F, w - 164.0F));
        boolean hovered = inside(mouseX, mouseY, x + 4.0F, y + 2.0F, labelWidth, h - 4.0F);
        float progress = MACRO_DELETE_ANIMS.compute(setting, (key, previous) -> {
            float current = previous == null ? 0.0F : previous;
            return this.approach(current, hovered ? 1.0F : 0.0F, 12.0F);
        });
        if (progress > 0.01F) {
            TextRender.draw(context, "×", x + 7.0F, y + 18.0F, 14.0F, fade(-38540, progress));
        }

        return 15.0F * progress;
    }

    static int macroSettingIndex(Module module, Setting<?> setting) {
        if (!(module instanceof Macros macros)) {
            return -1;
        } else {
            for (int index = 0; index < macros.getMacroCount(); index++) {
                if (setting == macros.getCommandSetting(index) || setting == macros.getBindSetting(index)) {
                    return index;
                }
            }

            return -1;
        }
    }

    void drawCompactSettingSwitch(DrawContext context, float x, float y, float width, float height, float progress, float accentX, float accentY) {
        this.drawSwitch(context, x, y, width, height, progress, lerpColor(536870911, accentAt(accentX, accentY), progress), -1, 11.8F);
    }

    void drawSwitch(DrawContext context, float x, float y, float width, float height, float progress, int trackColor, int knobColor, float knobSize) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, height / 2.0F, trackColor);
        float safeKnobSize = Math.min(knobSize, Math.max(2.0F, height - 2.4F));
        float radius = safeKnobSize / 2.0F;
        float edgeInset = Math.max(1.2F, (height - safeKnobSize) / 2.0F);
        float clampedProgress = MathHelper.clamp(progress, 0.0F, 1.0F);
        float smoothProgress = clampedProgress * clampedProgress * (3.0F - 2.0F * clampedProgress);
        float travel = Math.max(0.0F, width - edgeInset * 2.0F - safeKnobSize);
        float centerX = x + edgeInset + radius + travel * smoothProgress;
        float centerY = y + height / 2.0F;
        RoundedRenderer.drawSmoothFilledCircle(context, centerX, centerY, radius, knobColor);
    }

    void renderModuleDescription(DrawContext context, Module module, float cardX, float cardY, float mouseX, float mouseY, int color) {
        String description = module.getDescription();
        float maxWidth = 232.0F;
        float fullWidth = TextRender.width(description, 11.55F);
        float textX = cardX + 42.0F;
        float textY = cardY + 41.8F;
        if (fullWidth <= maxWidth) {
            DESCRIPTION_SCROLLS.remove(module);
            TextRender.draw(context, description, textX, textY, 11.55F, color);
        } else {
            boolean hovered = inside(mouseX, mouseY, textX, cardY + 27.0F, maxWidth, 18.0F);
            float offset = DESCRIPTION_SCROLLS.getOrDefault(module, 0.0F);
            if (hovered) {
                float cycle = fullWidth + 28.0F;
                offset = (offset + 34.0F * this.frameDeltaSeconds) % cycle;
            } else {
                offset = this.approach(offset, 0.0F, 8.0F);
            }

            DESCRIPTION_SCROLLS.put(module, offset);
            context.enableScissor(Math.round(textX), Math.round(cardY + 27.0F), Math.round(textX + maxWidth), Math.round(cardY + 47.0F));
            TextRender.draw(context, description, textX - offset, textY, 11.55F, color);
            if (hovered) {
                TextRender.draw(context, description, textX - offset + fullWidth + 28.0F, textY, 11.55F, color);
            }

            context.disableScissor();
        }
    }

    float getPickerX() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getWindow() != null && this.originX + 1225.0F * this.scale > client.getWindow().getScaledWidth() ? 743.0F : 1001.0F;
    }

    void renderFloatingColorPicker(DrawContext context, float mouseX, float mouseY) {
        if (activeColorPicker != null) {
            float pX = this.getPickerX();
            float pY = 40.0F;
            float pW = 224.0F;
            float pH = 218.0F;
            float easedProgress = 1.0F - (float)Math.pow(1.0F - this.colorPickerProgress, 3.0);
            float pickerScale = this.colorPickerOpen ? 0.9F + 0.1F * easedProgress : 1.0F;
            float pickerDrop = this.colorPickerOpen ? 0.0F : (1.0F - easedProgress) * 20.0F;
            float centerX = pX + pW / 2.0F;
            float centerY = pY + pH / 2.0F;
            float pickerMouseX = centerX + (mouseX - centerX) / pickerScale;
            float pickerMouseY = centerY + (mouseY - centerY - pickerDrop) / pickerScale;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(centerX, centerY + pickerDrop);
            context.getMatrices().scale(pickerScale, pickerScale);
            context.getMatrices().translate(-centerX, -centerY);
            RoundedRenderer.drawRoundedRect(context, pX, pY, pW, pH, 12.0F, fade(-872415232, this.openProgress * easedProgress));
            int col = activeColorPicker.getValue();
            float[] hsb = Color.RGBtoHSB(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, null);
            float hue = this.colorPickerHue;
            float sat = hsb[1];
            float bri = hsb[2];
            int alpha = col >>> 24 & 0xFF;
            String title = trimText(activeColorPicker.getName(), 12.0F, 120.0F);
            TextRender.draw(context, title, pX + 12.0F, pY + 22.0F, 12.5F, fade(-1, easedProgress));
            float closeX = pX + pW - 28.0F;
            float closeY = pY + 8.0F;
            float closeSize = 20.0F;
            boolean closeHov = inside(pickerMouseX, pickerMouseY, closeX, closeY, closeSize, closeSize);
            RoundedRenderer.drawRoundedRect(context, closeX, closeY, closeSize, closeSize, 6.0F, fade(closeHov ? 822083583 : 402653184, easedProgress));
            drawCloseGlyph(
                context, closeX + closeSize / 2.0F, closeY + closeSize / 2.0F, 10.4F, fade(closeHov ? accentAt(closeX, closeY) : -2130706433, easedProgress)
            );
            float previewW = 16.0F;
            float previewH = 16.0F;
            float previewX = closeX - previewW - 8.0F;
            float previewY = pY + 10.0F;
            RoundedRenderer.drawRoundedRect(context, previewX, previewY, previewW, previewH, 4.0F, fade(col, easedProgress));
            String hex = String.format("#%06X", 16777215 & col);
            TextRender.draw(context, hex, previewX - TextRender.width(hex, 10.5F) - 6.0F, pY + 21.0F, 10.5F, fade(-2130706433, easedProgress));
            float svX = pX + 12.0F;
            float svY = pY + 34.0F;
            float svW = pW - 24.0F;
            float svH = 110.0F;
            int pureHue = Color.HSBtoRGB(hue, 1.0F, 1.0F);
            RenderUtil.drawHorizontalGradientRect(context, svX, svY, svW, svH, fade(-1, easedProgress), fade(pureHue, easedProgress));
            RenderUtil.drawVerticalGradientRect(context, svX, svY, svX + svW, svY + svH, 0, fade(-16777216, easedProgress));
            float svKnobX = svX + sat * svW;
            float svKnobY = svY + (1.0F - bri) * svH;
            RoundedRenderer.drawRoundedRect(context, svKnobX - 4.0F, svKnobY - 4.0F, 8.0F, 8.0F, 4.0F, fade(-1, easedProgress));
            RoundedRenderer.drawRoundedRect(context, svKnobX - 3.0F, svKnobY - 3.0F, 6.0F, 6.0F, 3.0F, fade(col, easedProgress));
            float hBarX = pX + 12.0F;
            float hBarY = svY + svH + 10.0F;
            float hBarW = pW - 24.0F;
            float hBarH = 10.0F;
            int segs = 16;
            float segW = hBarW / segs;

            for (int s = 0; s < segs; s++) {
                float h1 = (float)s / segs;
                float h2 = (float)(s + 1) / segs;
                int c1 = Color.HSBtoRGB(h1, 1.0F, 1.0F);
                int c2 = Color.HSBtoRGB(h2, 1.0F, 1.0F);
                RenderUtil.drawHorizontalGradientRect(context, hBarX + s * segW, hBarY, segW + 0.5F, hBarH, fade(c1, easedProgress), fade(c2, easedProgress));
            }

            float hueKnobX = hBarX + hue * hBarW;
            RoundedRenderer.drawRoundedRect(context, hueKnobX - 2.5F, hBarY - 2.0F, 5.0F, hBarH + 4.0F, 2.5F, fade(-1, easedProgress));
            float aBarX = pX + 12.0F;
            float aBarY = hBarY + hBarH + 8.0F;
            float aBarW = pW - 24.0F;
            float aBarH = 10.0F;
            int rgbPure = col & 16777215;
            RenderUtil.drawHorizontalGradientRect(context, aBarX, aBarY, aBarW, aBarH, fade(rgbPure, easedProgress), fade(0xFF000000 | rgbPure, easedProgress));
            float alphaKnobX = aBarX + alpha / 255.0F * aBarW;
            RoundedRenderer.drawRoundedRect(context, alphaKnobX - 2.5F, aBarY - 2.0F, 5.0F, aBarH + 4.0F, 2.5F, fade(-1, easedProgress));
            float palY = aBarY + aBarH + 10.0F;
            float dotSize = 14.0F;
            float dotGap = 6.0F;
            float dotStartX = pX + 12.0F;

            for (int i = 0; i < PALETTE_PRESETS.length; i++) {
                float dotX = dotStartX + i * (dotSize + dotGap);
                if (dotX + dotSize > pX + pW - 12.0F) {
                    break;
                }

                int preset = PALETTE_PRESETS[i];
                boolean sel = (preset & 16777215) == (col & 16777215);
                RoundedRenderer.drawRoundedRect(context, dotX, palY, dotSize, dotSize, dotSize / 2.0F, fade(preset, easedProgress));
                if (sel) {
                    RoundedRenderer.drawRoundedRect(
                        context, dotX + 3.0F, palY + 3.0F, dotSize - 6.0F, dotSize - 6.0F, (dotSize - 6.0F) / 2.0F, fade(-16777216, easedProgress)
                    );
                }
            }

            context.getMatrices().popMatrix();
        }
    }

    void openModePopup(Setting<?> setting, float cardX, float settingY, int column) {
        if (setting instanceof ModeSetting || setting instanceof MultiBooleanSetting) {
            boolean samePopup = this.modePopupSetting == setting && this.modePopupOpen;
            if (samePopup) {
                this.modePopupOpen = false;
            } else {
                this.colorPickerOpen = false;
                this.modePopupSetting = setting;
                this.modePopupOpen = true;
                this.modePopupProgress = 0.0F;
                this.modePopupScroll = 0.0F;
                this.modePopupTargetScroll = 0.0F;
                this.modePopupHeight = MathHelper.clamp(52.0F + modeOptions(setting).size() * 27.0F, 79.0F, 313.0F);
                this.modePopupX = column == 0 ? cardX + 355.0F + 8.0F : cardX + 14.0F - 8.0F;
                this.modePopupAnchorContentY = settingY + scrollOffset;
                float topLimit = 72.0F;
                float bottomLimit = 524.0F;
                this.modePopupY = MathHelper.clamp(settingY + 15.4F - this.modePopupHeight / 2.0F, topLimit, bottomLimit - this.modePopupHeight);
                this.modePopupAnchorOffsetY = this.modePopupY - settingY;
            }
        }
    }

    void updateModePopupPosition() {
        if (this.modePopupSetting != null) {
            if (this.modePopupOpen && !this.modePopupSetting.isVisible()) {
                this.modePopupOpen = false;
            } else {
                float anchorY = this.modePopupAnchorContentY - scrollOffset;
                this.modePopupY = anchorY + this.modePopupAnchorOffsetY;
            }
        }
    }

    void renderModePopup(DrawContext context, float mouseX, float mouseY) {
        if (this.modePopupSetting != null) {
            float eased = 1.0F - (float)Math.pow(1.0F - this.modePopupProgress, 3.0);
            boolean liquidGlass = ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.isLiquidGlassStyle();
            if (!liquidGlass) {
                RoundedRenderer.drawRoundedRect(
                    context, this.modePopupX - 3.0F, this.modePopupY - 3.0F, 180.0F, this.modePopupHeight + 6.0F, 13.0F, fade(637534208, eased)
                );
            }

            if (liquidGlass) {
                Color selected = ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.guiGlassColor.getColor();
                int glassRgb = selected.getRGB() & 16777215;
                int darkGlassRgb = new Color(
                        Math.round(selected.getRed() * 0.22F), Math.round(selected.getGreen() * 0.22F), Math.round(selected.getBlue() * 0.22F)
                    )
                    .getRGB();
                int opaqueGlass = 0xFF000000 | glassRgb;
                int edge = lerpColor(-13946824, opaqueGlass, 0.3F);
                int topLeft = lerpColor(-15460322, opaqueGlass, 0.24F);
                int topRight = lerpColor(-15723494, opaqueGlass, 0.14F);
                int bottomLeft = lerpColor(-16184046, 0xFF000000 | darkGlassRgb & 16777215, 0.3F);
                int bottomRight = lerpColor(-15986666, opaqueGlass, 0.18F);
                RoundedRenderer.drawRoundedRect(context, this.modePopupX, this.modePopupY, 174.0F, this.modePopupHeight, 10.5F, fade(edge, eased));
                RoundedRenderer.drawFourCornerGradientRoundedRect(
                    context,
                    this.modePopupX + 1.0F,
                    this.modePopupY + 1.0F,
                    172.0F,
                    this.modePopupHeight - 2.0F,
                    9.5F,
                    fade(topLeft, eased),
                    fade(topRight, eased),
                    fade(bottomLeft, eased),
                    fade(bottomRight, eased)
                );
            } else {
                RoundedRenderer.drawRoundedRect(context, this.modePopupX, this.modePopupY, 174.0F, this.modePopupHeight, 10.5F, fade(881626260, eased));
                RoundedRenderer.drawRoundedRect(
                    context, this.modePopupX + 0.75F, this.modePopupY + 0.75F, 172.5F, this.modePopupHeight - 1.5F, 9.75F, fade(-49871095, eased)
                );
            }

            String title = this.modePopupSetting instanceof MultiBooleanSetting ? "Выберите режимы" : "Выберите режим";
            TextRender.draw(context, title, this.modePopupX + 14.0F, this.modePopupY + 25.0F, 13.0F, fade(-1, eased));
            context.fill(
                (int)(this.modePopupX + 12.0F),
                (int)(this.modePopupY + 35.0F),
                (int)(this.modePopupX + 174.0F - 12.0F),
                (int)(this.modePopupY + 36.0F),
                fade(352321535, eased)
            );
            List<String> options = modeOptions(this.modePopupSetting);
            float listTop = this.modePopupY + 42.0F;
            float listBottom = this.modePopupY + this.modePopupHeight - 10.0F;
            context.enableScissor((int)this.modePopupX, (int)listTop, (int)(this.modePopupX + 174.0F), (int)listBottom);

            for (int index = 0; index < options.size(); index++) {
                String option = options.get(index);
                float rowY = listTop + index * 27.0F - this.modePopupScroll;
                if (!(rowY + 27.0F < listTop) && !(rowY > listBottom)) {
                    boolean selected = isModeSelected(this.modePopupSetting, option);
                    Map<String, Float> optionAnimations = MODE_OPTION_ANIMS.computeIfAbsent(this.modePopupSetting, key -> new HashMap<>());
                    float selectionAnim = optionAnimations.compute(option, (key, previous) -> {
                        float target = selected ? 1.0F : 0.0F;
                        return previous == null ? target : this.approach(previous, target, 10.0F);
                    });
                    boolean hovered = inside(mouseX, mouseY, this.modePopupX + 8.0F, rowY, 158.0F, 27.0F);
                    if (hovered) {
                        RoundedRenderer.drawRoundedRect(context, this.modePopupX + 8.0F, rowY + 2.0F, 158.0F, 23.0F, 6.0F, fade(620756991, eased));
                    }

                    if (selectionAnim > 0.01F) {
                        float dotSize = 5.0F * selectionAnim;
                        RoundedRenderer.drawRoundedRect(
                            context,
                            this.modePopupX + 13.0F,
                            rowY + 13.5F - dotSize / 2.0F,
                            dotSize,
                            dotSize,
                            dotSize / 2.0F,
                            fade(accentAt(this.modePopupX + 13.0F, rowY), eased * selectionAnim)
                        );
                    }

                    int optionColor = lerpColor(-2130706433, accentAt(this.modePopupX, rowY), selectionAnim);
                    float optionX = MathHelper.lerp(selectionAnim, this.modePopupX + 16.0F, this.modePopupX + 25.0F);
                    TextRender.draw(context, trimText(option, 12.5F, 132.0F), optionX, rowY + 18.0F, 12.5F, fade(optionColor, eased));
                }
            }

            context.disableScissor();
        }
    }

    boolean handleModePopupClick(float mouseX, float mouseY) {
        if (this.modePopupSetting != null && this.modePopupOpen) {
            boolean insideVisibleArea = mouseY >= 72.0F && mouseY <= 526.0F;
            if (insideVisibleArea && inside(mouseX, mouseY, this.modePopupX, this.modePopupY, 174.0F, this.modePopupHeight)) {
                float listTop = this.modePopupY + 42.0F;
                float listBottom = this.modePopupY + this.modePopupHeight - 10.0F;
                if (mouseY >= listTop && mouseY <= listBottom) {
                    List<String> options = modeOptions(this.modePopupSetting);
                    int index = (int)((mouseY - listTop + this.modePopupScroll) / 27.0F);
                    if (index >= 0 && index < options.size()) {
                        String option = options.get(index);
                        if (this.modePopupSetting instanceof ModeSetting modeSetting) {
                            modeSetting.setValue(option);
                        } else if (this.modePopupSetting instanceof MultiBooleanSetting multiSetting) {
                            multiSetting.toggle(option);
                        }
                    }
                }

                return true;
            } else {
                this.modePopupOpen = false;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.closing) {
            return true;
        }

        float mouseX = ((float)click.comp_4798() - this.originX) / this.scale;
        float mouseY = ((float)click.comp_4799() - this.originY) / this.scale;
        if (settingsModule != null && click.button() == 0
            && inside(mouseX, mouseY, SETTINGS_X + SETTINGS_W - 42.0F, SETTINGS_Y + 8.0F, 32.0F, 28.0F)) {
            settingsModule = null;
            settingsScroll = 0.0F;
            settingsTargetScroll = 0.0F;
            return true;
        }
        if (this.handleModePopupClick(mouseX, mouseY)) {
            return true;
        }

        cancelBinding();
        activeTextSetting = null;
        if (activeColorPicker != null && this.colorPickerOpen) {
            float pX = this.getPickerX();
            float pY = 40.0F;
            float pW = 224.0F;
            float pH = 218.0F;
            if (inside(mouseX, mouseY, pX, pY, pW, pH)) {
                float closeX = pX + pW - 28.0F;
                float closeY = pY + 8.0F;
                if (inside(mouseX, mouseY, closeX, closeY, 20.0F, 20.0F)) {
                    this.colorPickerOpen = false;
                    return true;
                }

                int col = activeColorPicker.getValue();
                float[] hsb = Color.RGBtoHSB(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, null);
                float svX = pX + 12.0F;
                float svY = pY + 34.0F;
                float svW = pW - 24.0F;
                float svH = 110.0F;
                if (inside(mouseX, mouseY, svX, svY, svW, svH)) {
                    float sat = MathHelper.clamp((mouseX - svX) / svW, 0.0F, 1.0F);
                    float bri = MathHelper.clamp(1.0F - (mouseY - svY) / svH, 0.0F, 1.0F);
                    int newRgb = Color.HSBtoRGB(this.colorPickerHue, sat, bri);
                    activeColorPicker.setValue(activeColorPicker.getAlpha() << 24 | newRgb & 16777215);
                    this.draggingSV = true;
                    return true;
                }

                float hBarX = pX + 12.0F;
                float hBarY = svY + svH + 10.0F;
                float hBarW = pW - 24.0F;
                float hBarH = 10.0F;
                if (inside(mouseX, mouseY, hBarX, hBarY - 3.0F, hBarW, hBarH + 6.0F)) {
                    float newHue = MathHelper.clamp((mouseX - hBarX) / hBarW, 0.0F, 1.0F);
                    this.colorPickerHue = newHue;
                    int newRgb = Color.HSBtoRGB(newHue, hsb[1], hsb[2]);
                    activeColorPicker.setValue(activeColorPicker.getAlpha() << 24 | newRgb & 16777215);
                    this.draggingHue = true;
                    return true;
                }

                float aBarX = pX + 12.0F;
                float aBarY = hBarY + hBarH + 8.0F;
                float aBarW = pW - 24.0F;
                float aBarH = 10.0F;
                if (inside(mouseX, mouseY, aBarX, aBarY - 3.0F, aBarW, aBarH + 6.0F)) {
                    float alphaRatio = MathHelper.clamp((mouseX - aBarX) / aBarW, 0.0F, 1.0F);
                    int newAlpha = Math.round(alphaRatio * 255.0F);
                    activeColorPicker.setValue(newAlpha << 24 | col & 16777215);
                    this.draggingAlpha = true;
                    return true;
                }

                float palY = aBarY + aBarH + 10.0F;
                float dotSize = 14.0F;
                float dotGap = 6.0F;
                float dotStartX = pX + 12.0F;

                for (int i = 0; i < PALETTE_PRESETS.length; i++) {
                    float dotX = dotStartX + i * (dotSize + dotGap);
                    if (inside(mouseX, mouseY, dotX, palY, dotSize, dotSize)) {
                        activeColorPicker.setValue(PALETTE_PRESETS[i]);
                        int preset = PALETTE_PRESETS[i];
                        this.colorPickerHue = Color.RGBtoHSB(preset >> 16 & 0xFF, preset >> 8 & 0xFF, preset & 0xFF, null)[0];
                        return true;
                    }
                }

                return true;
            }
        }

        if (inside(mouseX, mouseY, 10.0F, 74.0F, 224.0F, 37.0F)) {
            isSearching = true;
            return true;
        }

        isSearching = false;
        if (click.button() == 0) {
            for (int index = 0; index < SIDEBAR.length; index++) {
                float rowY = getRowY(index);
                if (inside(mouseX, mouseY, 22.0F, rowY, 200.0F, 32.0F)) {
                    Category cat = SIDEBAR[index].category();
                    if (cat == Category.MESSENGER) {
                        MinecraftClient.getInstance().setScreen(new ChatMessengerScreen());
                        return true;
                    }

                    selectedCategory = cat;
                    searchQuery = "";
                    this.markerInputFocus = 0;
                    configInputFocus = false;
                    friendsSearchFocus = false;
                    targetScroll = 0.0F;
                    scrollOffset = 0.0F;
                    return true;
                }
            }
        }

        if (selectedCategory == Category.COSMETICS && !isSearchMode()) {
            float areaX = 257.0F;
            float areaW = 987.0F - areaX - 10.0F;
            float tabY = 70.0F - scrollOffset;
            float tabW = (areaW - 32.0F) / 5.0F;

            for (int tab = 0; tab < 5; tab++) {
                float tabX = areaX + tab * (tabW + 8.0F);
                if (inside(mouseX, mouseY, tabX, tabY, tabW, 32.0F)) {
                    cosmeticsTab = tab;
                    targetScroll = 0.0F;
                    scrollOffset = 0.0F;
                    return true;
                }
            }

            float startCardY = 112.0F - scrollOffset;
            float cardGap = 10.0F;
            float cardW = (areaW - 3.0F * cardGap) / 4.0F;
            float cardH = 145.0F;
            String[] tabKeys = new String[]{"all", "wings", "hats", "weapons", "models"};
            List<CosmeticItem> cosmetics = CosmeticsManager.INSTANCE.getByCategory(tabKeys[cosmeticsTab]);

            for (int indexx = 0; indexx < cosmetics.size(); indexx++) {
                CosmeticItem item = cosmetics.get(indexx);
                int column = indexx % 4;
                int row = indexx / 4;
                float cardX = areaX + column * (cardW + cardGap);
                float cardY = startCardY + row * (cardH + cardGap);
                if (inside(mouseX, mouseY, cardX, cardY, cardW, cardH)) {
                    if (click.button() == 0) {
                        CosmeticsManager.INSTANCE.toggleEquip(item);
                        boolean equipped = CosmeticsManager.INSTANCE.isEquipped(item);
                        CommandManager.addChatMessage(equipped ? "§dНадето: §f" + item.getName() : "§cСнято: §f" + item.getName());
                    }

                    return true;
                }
            }

            return true;
        } else if (selectedCategory == Category.SERVER_EVENTS && !isSearchMode()) {
            float areaX = 257.0F;
            float areaW = 987.0F - areaX - 10.0F;
            float headerW = 720.0F;
            float serverX = 257.0F + headerW - 170.0F;
            if (inside(mouseX, mouseY, serverX, 22.0F, 70.0F, 30.0F)) {
                eventsServer = 0;
                eventsTab = 0;
                lastEventsRefreshTime = 0L;
                scrollOffset = 0.0F;
                targetScroll = 0.0F;
                return true;
            }

            if (inside(mouseX, mouseY, serverX + 76.0F, 22.0F, 82.0F, 30.0F)) {
                eventsServer = 1;
                lastEventsRefreshTime = 0L;
                scrollOffset = 0.0F;
                targetScroll = 0.0F;
                ServerEventsClient.refreshAsync(null);
                return true;
            }

            if (eventsServer == 1) {
                return true;
            }

            float tabY = 70.0F - scrollOffset;
            float tabH = 32.0F;
            float tabW = (areaW - 24.0F) / 4.0F;

            for (int t = 0; t < 4; t++) {
                float tX = areaX + t * (tabW + 8.0F);
                if (inside(mouseX, mouseY, tX, tabY, tabW, tabH)) {
                    eventsTab = t;
                    targetScroll = 0.0F;
                    scrollOffset = 0.0F;
                    return true;
                }
            }

            if (eventsTab == 3) {
                return this.handleEventFiltersClick(mouseX, mouseY, areaX, 112.0F - scrollOffset, areaW);
            }

            float startCardY = 112.0F - scrollOffset;
            float cardW = 355.0F;
            float cardH = 76.0F;
            float cardGap = 10.0F;
            if (eventsTab == 2) {
                List<FluxEventsClient$FluxMine> mines = FluxEventsClient.getMines()
                    .stream()
                    .filter(mine -> "all".equals(mineRarityFilter) || mine.nextMineRarity.name().equalsIgnoreCase(mineRarityFilter))
                    .toList();

                for (int ix = 0; ix < mines.size(); ix++) {
                    FluxEventsClient$FluxMine mine = mines.get(ix);
                    int colx = ix % 2;
                    int row = ix / 2;
                    float cX = areaX + colx * (cardW + cardGap);
                    float cY = startCardY + row * (cardH + cardGap);
                    float playX = cX + cardW - 28.0F;
                    float playY = cY + 48.0F;
                    if (inside(mouseX, mouseY, playX - 6.0F, playY - 6.0F, 24.0F, 24.0F) || inside(mouseX, mouseY, cX, cY, cardW, cardH)) {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null && client.player.networkHandler != null) {
                            String cmd = "an" + (mine.anarchyId > 0 ? mine.anarchyId : mine.server.replaceAll("[^0-9]", ""));
                            client.player.networkHandler.sendChatCommand(cmd);
                            CommandManager.addChatMessage("§dПодключение к §f/" + cmd);
                        }

                        return true;
                    }
                }
            } else {
                List<FluxEventsClient$FluxEvent> allEvents = FluxEventsClient.getEvents();
                List<FluxEventsClient$FluxEvent> list = new ArrayList<>();

                for (FluxEventsClient$FluxEvent ev : allEvents) {
                    if (ev.shouldDisplay()
                        && (
                            !ev.isActiveEvent()
                                || (EVENT_FILTERS.isEmpty() || EVENT_FILTERS.contains(ev.id))
                                    && ("all".equals(eventPhaseFilter) || ev.phase != null && eventPhaseFilter.equalsIgnoreCase(ev.phase))
                        )) {
                        if (eventsTab == 0 && ev.isActiveEvent()) {
                            list.add(ev);
                        } else if (eventsTab == 1 && !ev.isActiveEvent()) {
                            list.add(ev);
                        }
                    }
                }

                for (int ixx = 0; ixx < list.size(); ixx++) {
                    FluxEventsClient$FluxEvent evx = list.get(ixx);
                    int colx = ixx % 2;
                    int row = ixx / 2;
                    float cX = areaX + colx * (cardW + cardGap);
                    float cY = startCardY + row * (cardH + cardGap);
                    String coords = evx.getCoordinatesDisplay();
                    if (evx.hasLocation() && inside(mouseX, mouseY, cX + 10.0F, cY + 52.0F, 190.0F, 20.0F)) {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.keyboard != null) {
                            client.keyboard.setClipboard(coords);
                        }

                        CommandManager.addChatMessage("§dКоординаты скопированы в буфер: §f" + coords);
                        return true;
                    }

                    float playX = cX + cardW - 28.0F;
                    float playY = cY + 48.0F;
                    if (inside(mouseX, mouseY, playX - 6.0F, playY - 6.0F, 24.0F, 24.0F) || inside(mouseX, mouseY, cX, cY, cardW, cardH)) {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null && client.player.networkHandler != null) {
                            String cmd = "an" + (evx.anarchyId > 0 ? evx.anarchyId : evx.server.replaceAll("[^0-9]", ""));
                            client.player.networkHandler.sendChatCommand(cmd);
                            CommandManager.addChatMessage("§dПодключение к §f/" + cmd);
                        }

                        return true;
                    }
                }
            }

            return true;
        } else {
            if (selectedCategory == Category.CONFIGS && !isSearchMode()) {
                return this.handleConfigsPageClick(mouseX, mouseY, click.button());
            }

            if (selectedCategory == Category.MARKS && !isSearchMode()) {
                return this.handleMarkersPageClick(mouseX, mouseY, click.button());
            }

            if (selectedCategory == Category.FRIENDS && !isSearchMode()) {
                return this.handleFriendsPageClick(mouseX, mouseY, click.button());
            }

            List<Module> modules = this.getFilteredModules();
            float[] columnY = new float[2];
            Arrays.fill(columnY, 72.0F);

            for (int indexxx = 0; indexxx < modules.size(); indexxx++) {
                Module module = modules.get(indexxx);
                int colxx = indexxx % 2;
                float cardX = 257.0F + colxx * 365.0F;
                float cardY = columnY[colxx] - scrollOffset;
                List<Setting<?>> settings = module.getSettings();
                columnY[colxx] += 67.2F;
                if (inside(mouseX, mouseY, cardX, cardY, 355.0F, 57.2F)) {
                    if (click.button() == 0) {
                        float bindRight = cardX + 297.0F;
                        String bind = module.isBinding() ? "[...]" : bindName(module);
                        float bindLeft = bindRight - TextRender.width(bind, 15.665F) - 6.0F;
                        if (inside(mouseX, mouseY, bindLeft, cardY + 12.0F, bindRight - bindLeft + 8.0F, 24.0F)) {
                            module.setBinding(true);
                            return true;
                        }

                        module.toggle();
                        return true;
                    }

                    if (click.button() == 1) {
                        if (!settings.isEmpty()) {
                            settingsModule = settingsModule == module ? null : module;
                            settingsScroll = 0.0F;
                            settingsTargetScroll = 0.0F;
                            modePopupOpen = false;
                            colorPickerOpen = false;
                            activeTextSetting = null;
                        }
                        return true;
                    }

                    if (click.button() == 2) {
                        module.setBinding(true);
                        return true;
                    }
                }

                if (settingsModule == module
                    && inside(mouseX, mouseY, SETTINGS_X + 8.0F, SETTINGS_TOP, SETTINGS_W - 16.0F, SETTINGS_BOTTOM - SETTINGS_TOP)) {
                    float settingY = SETTINGS_TOP + 6.0F - settingsScroll;

                    for (Setting<?> setting : settings) {
                        float visibility = SETTING_VISIBILITY_ANIMS.getOrDefault(setting, setting.isVisible() ? 1.0F : 0.0F);
                        if (!(visibility <= 0.01F)) {
                            float sX = SETTINGS_X + 12.0F;
                            float sW = SETTINGS_W - 24.0F;
                            float sH = 26.8F;
                            float visibleHeight = Math.min(sH, 30.8F * visibility);
                            if (setting.isVisible() && visibility > 0.55F && inside(mouseX, mouseY, sX, settingY, sW, visibleHeight)) {
                                int macroIndex = macroSettingIndex(module, setting);
                                if (macroIndex >= 0
                                    && MACRO_DELETE_ANIMS.getOrDefault(setting, 0.0F) > 0.15F
                                    && inside(mouseX, mouseY, sX + 3.0F, settingY + 2.0F, 21.0F, sH - 4.0F)) {
                                    ((Macros)module).removeMacro(macroIndex);
                                    return true;
                                }

                                if (setting instanceof ItemHighlightSetting itemSetting) {
                                    float switchWidth = 24.2F;
                                    float previewSize = 13.2F;
                                    float swX = sX + sW - switchWidth - 6.6F;
                                    float previewX = swX - previewSize - 7.0F;
                                    float previewY = settingY + (sH - previewSize) / 2.0F;
                                    if (inside(mouseX, mouseY, previewX - 2.0F, previewY - 2.0F, previewSize + 4.0F, previewSize + 4.0F)) {
                                        ColorSetting proxy = ITEM_COLOR_PROXIES.computeIfAbsent(
                                            itemSetting,
                                            keyx -> new ColorSetting(keyx.getName() + " — цвет", new Color(0xFF000000 | keyx.getColor() & 16777215, true))
                                        );
                                        this.openColorPicker(proxy);
                                        return true;
                                    }

                                    itemSetting.toggle();
                                    return true;
                                }

                                if (setting instanceof ItemScaleSetting itemSetting) {
                                    itemSetting.toggle();
                                    return true;
                                }

                                if (setting instanceof BooleanSetting boolSetting) {
                                    boolSetting.setValue(!boolSetting.getValue());
                                    return true;
                                }

                                if (setting instanceof ModeSetting modeSetting) {
                                    float fieldW = modeFieldWidth(modeSetting);
                                    float fieldX = sX + sW - fieldW - 8.0F;
                                    if (inside(mouseX, mouseY, fieldX, settingY + 4.4F, fieldW, sH - 8.8F)) {
                                        this.openModePopup(modeSetting, sX, settingY + settingsScroll - scrollOffset, 0);
                                        return true;
                                    }
                                } else if (setting instanceof MultiBooleanSetting multiSetting) {
                                    float fieldW = multiFieldWidth(multiSetting);
                                    float fieldX = sX + sW - fieldW - 8.0F;
                                    if (inside(mouseX, mouseY, fieldX, settingY + 4.4F, fieldW, sH - 8.8F)) {
                                        this.openModePopup(multiSetting, sX, settingY + settingsScroll - scrollOffset, 0);
                                        return true;
                                    }
                                } else if (setting instanceof NumberSetting numSetting) {
                                    float sliderW = 132.0F;
                                    float sliderX = sX + sW - sliderW - 8.0F;
                                    if (inside(mouseX, mouseY, sliderX - 6.0F, settingY, sliderW + 12.0F, sH)) {
                                        float ratio = MathHelper.clamp((mouseX - sliderX) / sliderW, 0.0F, 1.0F);
                                        double newVal = numSetting.getMin() + ratio * (numSetting.getMax() - numSetting.getMin());
                                        numSetting.setValue(newVal);
                                        this.draggingSlider = numSetting;
                                        this.draggingSliderX = sliderX;
                                        this.draggingSliderWidth = sliderW;
                                        return true;
                                    }
                                } else {
                                    if (setting instanceof ActionSetting actionSetting) {
                                        actionSetting.trigger();
                                        return true;
                                    }

                                    if (setting instanceof StringSetting stringSetting) {
                                        float fieldX = sX + sW - 154.0F - 8.0F;
                                        if (inside(mouseX, mouseY, fieldX, settingY + 4.4F, 154.0F, sH - 8.8F)) {
                                            activeTextSetting = stringSetting;
                                            isSearching = false;
                                            return true;
                                        }
                                    } else {
                                        if (setting instanceof ColorSetting colorSetting) {
                                            this.openColorPicker(colorSetting);
                                            return true;
                                        }

                                        if (setting instanceof BindSetting bindSetting) {
                                            bindingSetting = bindingSetting == bindSetting ? null : bindSetting;
                                            bindingFriendSetting = null;
                                            return true;
                                        }

                                        if (setting instanceof FriendRequestRowSetting reqSetting) {
                                            float declineW = 64.0F;
                                            float declineX = sX + sW - declineW - 8.0F;
                                            float btnW = 58.0F;
                                            float acceptX = declineX - btnW - 8.0F;
                                            float btnH = sH - 9.0F;
                                            float btnY = settingY + 4.5F;
                                            if (inside(mouseX, mouseY, acceptX, btnY, btnW, btnH)) {
                                                reqSetting.accept();
                                                return true;
                                            }

                                            if (inside(mouseX, mouseY, declineX, btnY, declineW, btnH)) {
                                                reqSetting.decline();
                                                return true;
                                            }
                                        } else if (setting instanceof FriendRowSetting friendSetting) {
                                            int key = friendSetting.getKey();
                                            String bindText = bindingFriendSetting == friendSetting
                                                ? "[...]"
                                                : "[" + (key == 0 ? "-" : Type.KEYSYM.createFromCode(key).getLocalizedText().getString()) + "]";
                                            float bindW = Math.max(34.0F, TextRender.width(bindText, 12.65F) + 8.0F);
                                            float bindX = sX + sW - bindW - 8.0F;
                                            float btnHx = sH - 9.0F;
                                            float btnYx = settingY + 4.5F;
                                            if (inside(mouseX, mouseY, bindX, btnYx, bindW, btnHx)) {
                                                bindingFriendSetting = bindingFriendSetting == friendSetting ? null : friendSetting;
                                                bindingSetting = null;
                                                return true;
                                            }

                                            float toggleW = 62.0F;
                                            float toggleX = bindX - toggleW - 6.0F;
                                            if (inside(mouseX, mouseY, toggleX, btnYx, toggleW, btnHx)) {
                                                friendSetting.toggle();
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }

                            settingY += 30.8F * visibility;
                        }
                    }
                }
            }

            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        float mouseX = ((float)click.comp_4798() - this.originX) / this.scale;
        float mouseY = ((float)click.comp_4799() - this.originY) / this.scale;
        if (this.draggingMarkerSize && selectedCategory == Category.MARKS && !isSearchMode()) {
            float innerX = 271.0F;
            float innerW = 692.0F;
            float createX = innerX + innerW - 134.0F;
            float sizeX = markerColorsEndX(innerX) + 18.0F;
            float sizeW = createX - sizeX - 12.0F;
            float sliderX = sizeX + 52.0F;
            float sliderW = sizeW - 83.0F;
            this.markerSize = 1.0F + MathHelper.clamp((mouseX - sliderX) / sliderW, 0.0F, 1.0F);
            return true;
        }

        if (this.draggingSlider != null) {
            float ratio = MathHelper.clamp((mouseX - this.draggingSliderX) / this.draggingSliderWidth, 0.0F, 1.0F);
            double value = this.draggingSlider.getMin() + ratio * (this.draggingSlider.getMax() - this.draggingSlider.getMin());
            this.draggingSlider.setValue(value);
            return true;
        }

        if (activeColorPicker != null && this.colorPickerOpen) {
            float pX = this.getPickerX();
            float pY = 40.0F;
            float pW = 224.0F;
            int col = activeColorPicker.getValue();
            float[] hsb = Color.RGBtoHSB(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, null);
            if (this.draggingSV) {
                float svX = pX + 12.0F;
                float svY = pY + 34.0F;
                float svW = pW - 24.0F;
                float svH = 110.0F;
                float sat = MathHelper.clamp((mouseX - svX) / svW, 0.0F, 1.0F);
                float bri = MathHelper.clamp(1.0F - (mouseY - svY) / svH, 0.0F, 1.0F);
                int newRgb = Color.HSBtoRGB(this.colorPickerHue, sat, bri);
                activeColorPicker.setValue(activeColorPicker.getAlpha() << 24 | newRgb & 16777215);
                return true;
            }

            if (this.draggingHue) {
                float hBarX = pX + 12.0F;
                float hBarY = pY + 34.0F + 110.0F + 10.0F;
                float hBarW = pW - 24.0F;
                float newHue = MathHelper.clamp((mouseX - hBarX) / hBarW, 0.0F, 1.0F);
                this.colorPickerHue = newHue;
                int newRgb = Color.HSBtoRGB(newHue, hsb[1], hsb[2]);
                activeColorPicker.setValue(activeColorPicker.getAlpha() << 24 | newRgb & 16777215);
                return true;
            }

            if (this.draggingAlpha) {
                float aBarX = pX + 12.0F;
                float aBarY = pY + 34.0F + 110.0F + 10.0F + 10.0F + 8.0F;
                float aBarW = pW - 24.0F;
                float alphaRatio = MathHelper.clamp((mouseX - aBarX) / aBarW, 0.0F, 1.0F);
                int newAlpha = Math.round(alphaRatio * 255.0F);
                activeColorPicker.setValue(newAlpha << 24 | col & 16777215);
                return true;
            }
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.draggingSlider = null;
        this.draggingMarkerSize = false;
        this.draggingSV = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float designMouseX = ((float)mouseX - this.originX) / this.scale;
        float designMouseY = ((float)mouseY - this.originY) / this.scale;
        if (settingsModule != null
            && inside(designMouseX, designMouseY, SETTINGS_X, SETTINGS_TOP, SETTINGS_W, SETTINGS_BOTTOM - SETTINGS_TOP)) {
            int count = 0;
            for (Setting<?> setting : settingsModule.getSettings()) {
                if (setting.isVisible()) {
                    count++;
                }
            }
            float maxSettingsScroll = Math.max(0.0F, count * 30.8F - (SETTINGS_BOTTOM - SETTINGS_TOP));
            settingsTargetScroll = MathHelper.clamp(settingsTargetScroll - (float)verticalAmount * 34.0F, 0.0F, maxSettingsScroll);
            return true;
        }
        if (selectedCategory == Category.FRIENDS && !isSearchMode()) {
            if (inside(designMouseX, designMouseY, 267.0F, 174.0F, 700.0F, 352.0F)) {
                List<String> names = friendsVisibleNames();
                float maxScroll = Math.max(0.0F, names.size() * 42.0F - 6.0F - 352.0F);
                friendsTargetScroll = MathHelper.clamp(friendsTargetScroll - (float)verticalAmount * 34.0F, 0.0F, maxScroll);
            }
            return true;
        }
        if (selectedCategory == Category.CONFIGS && !isSearchMode()) {
            float pageW = 720.0F;
            float listY = 162.0F;
            float listTop = listY + 40.0F;
            float listBottom = 514.0F;
            if (inside(designMouseX, designMouseY, 267.0F, listTop, pageW - 20.0F, listBottom - listTop)) {
                int count = ConfigManager.INSTANCE.listConfigs().size();
                float totalHeight = Math.max(0.0F, count * 66.0F - 8.0F);
                float maxScroll = Math.max(0.0F, totalHeight - (listBottom - listTop));
                configsTargetScroll = MathHelper.clamp(configsTargetScroll - (float)verticalAmount * 34.0F, 0.0F, maxScroll);
            }

            return true;
        } else if (selectedCategory == Category.MARKS && !isSearchMode()) {
            float pageW = 720.0F;
            float listY = 290.0F;
            float listTop = listY + 40.0F;
            float listBottom = 514.0F;
            if (inside(designMouseX, designMouseY, 267.0F, listTop, pageW - 20.0F, listBottom - listTop)) {
                Markers markers = ModuleManager.INSTANCE.getModule(Markers.class);
                int count = markers == null ? 0 : markers.getMarkers().size();
                float totalHeight = Math.max(0.0F, count * 59.0F - 7.0F);
                float maxScroll = Math.max(0.0F, totalHeight - (listBottom - listTop));
                this.markerListTargetScroll = MathHelper.clamp(this.markerListTargetScroll - (float)verticalAmount * 34.0F, 0.0F, maxScroll);
            }

            return true;
        } else if (this.modePopupSetting != null
            && this.modePopupOpen
            && designMouseY >= 72.0F
            && designMouseY <= 526.0F
            && inside(designMouseX, designMouseY, this.modePopupX, this.modePopupY, 174.0F, this.modePopupHeight)) {
            float contentHeight = Math.max(1.0F, this.modePopupHeight - 52.0F);
            float maxPopupScroll = Math.max(0.0F, modeOptions(this.modePopupSetting).size() * 27.0F - contentHeight);
            this.modePopupTargetScroll = MathHelper.clamp(this.modePopupTargetScroll - (float)verticalAmount * 34.0F, 0.0F, maxPopupScroll);
            return true;
        } else {
            targetScroll -= (float)verticalAmount * 45.0F;
            float maxScroll = this.getMaxScroll();
            targetScroll = MathHelper.clamp(targetScroll, 0.0F, maxScroll);
            return true;
        }
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (selectedCategory == Category.CONFIGS && !isSearchMode() && configInputFocus) {
            String text = input.asString();
            if (!text.isEmpty() && configInput.length() < 512) {
                configInput = configInput + text;
                return true;
            }
        }

        if (selectedCategory == Category.MARKS && !isSearchMode() && this.markerInputFocus != 0) {
            String text = input.asString();
            if (!text.isEmpty()) {
                char character = text.charAt(0);
                if (this.markerInputFocus == 1
                    && this.markerNameInput.length() < 24
                    && (Character.isLetterOrDigit(character) || Character.isSpaceChar(character) || character == '_' || character == '-' || character == '.')) {
                    this.markerNameInput = this.markerNameInput + character;
                    return true;
                }

                if (this.markerInputFocus >= 2
                    && this.markerInputFocus <= 4
                    && (Character.isDigit(character) || character == '-' || character == '.' || character == ',')) {
                    if (this.markerInputFocus == 2 && this.markerXInput.length() < 14) {
                        this.markerXInput = this.markerXInput + character;
                    }

                    if (this.markerInputFocus == 3 && this.markerYInput.length() < 14) {
                        this.markerYInput = this.markerYInput + character;
                    }

                    if (this.markerInputFocus == 4 && this.markerZInput.length() < 14) {
                        this.markerZInput = this.markerZInput + character;
                    }

                    return true;
                }
            }
        }

        if (friendsSearchFocus) {
            String text = input.asString();
            if (!text.isEmpty() && friendsSearch.length() < 64) {
                friendsSearch = friendsSearch + text;
                friendsScroll = 0.0F;
                friendsTargetScroll = 0.0F;
                return true;
            }
        }

        if (activeTextSetting != null) {
            String text = input.asString();
            if (!text.isEmpty() && activeTextSetting.getValue().length() < 256) {
                activeTextSetting.setValue(activeTextSetting.getValue() + text);
                return true;
            }
        }

        if (isSearching) {
            String text = input.asString();
            if (!text.isEmpty()) {
                searchQuery = searchQuery + ChatHelper.toEnglishLayout(text);
                targetScroll = 0.0F;
                return true;
            }
        }

        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.closing) {
            return true;
        }

        if (selectedCategory == Category.CONFIGS && !isSearchMode() && configInputFocus) {
            if (input.comp_4795() == 259) {
                if (!configInput.isEmpty()) {
                    configInput = configInput.substring(0, configInput.length() - 1);
                }

                return true;
            }

            if (input.comp_4795() == 257 || input.comp_4795() == 335) {
                this.createOrImportConfig();
                return true;
            }
        }

        if (friendsSearchFocus) {
            int key = input.comp_4795();
            if (key == 259) {
                if (!friendsSearch.isEmpty()) {
                    friendsSearch = friendsSearch.substring(0, friendsSearch.length() - 1);
                }
                return true;
            }
            if (key == 257 || key == 335) {
                String name = friendsSearch.trim();
                if (friendsTab == 1 && !name.isEmpty() && FluxVisualsClient.get != null
                      && FluxVisualsClient.get.friendManager != null && !FriendManager.isFriend(name)) {
                    FluxVisualsClient.get.friendManager.add(name);
                    friendsSearch = "";
                }
                friendsSearchFocus = false;
                return true;
            }
            if (key == 256) {
                friendsSearchFocus = false;
                return true;
            }
            return true;
        }

        if (activeTextSetting != null) {
            if (input.comp_4795() == 259) {
                String value = activeTextSetting.getValue();
                activeTextSetting.setValue(removeLastCodePoint(value));
                return true;
            }

            if (input.comp_4795() == 257 || input.comp_4795() == 335 || input.comp_4795() == 256) {
                activeTextSetting = null;
                return true;
            }
        }

        if (bindingFriendSetting != null) {
            if (input.comp_4795() == 261 || input.comp_4795() == 259) {
                bindingFriendSetting.setKey(0);
            } else if (input.comp_4795() != 256) {
                bindingFriendSetting.setKey(input.comp_4795());
            }

            bindingFriendSetting = null;
            return true;
        } else if (bindingSetting != null) {
            if (input.comp_4795() == 261 || input.comp_4795() == 259) {
                bindingSetting.setValue(0);
            } else if (input.comp_4795() != 256) {
                bindingSetting.setValue(input.comp_4795());
            }

            bindingSetting = null;
            return true;
        } else {
            Module bindingModule = findBindingModule();
            if (bindingModule != null) {
                if (input.comp_4795() == 261 || input.comp_4795() == 259) {
                    bindingModule.setKey(0);
                } else if (input.comp_4795() != 256) {
                    bindingModule.setKey(input.comp_4795());
                }

                bindingModule.setBinding(false);
                return true;
            } else {
                if (isSearching) {
                    if (input.comp_4795() == 259) {
                        if (!searchQuery.isEmpty()) {
                            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                        }

                        return true;
                    }

                    if (input.comp_4795() == 256 || input.comp_4795() == 257) {
                        isSearching = false;
                        return true;
                    }
                }

                if (input.comp_4795() != 344 && input.comp_4795() != 256) {
                    return super.keyPressed(input);
                }

                if (input.comp_4795() == 344) {
                    ru.fluxvisuals.ui.fluxgui.module.render.ClickGui.markToggleHandledByScreen();
                }

                this.requestClose();
                return true;
            }
        }
    }

    static String removeLastCodePoint(String value) {
        if (value != null && !value.isEmpty()) {
            int end = value.offsetByCodePoints(value.length(), -1);
            return value.substring(0, end);
        } else {
            return "";
        }
    }

    float getMaxScroll() {
        if (selectedCategory == Category.CONFIGS && !isSearchMode()) {
            return 0.0F;
        }

        if (selectedCategory == Category.MARKS && !isSearchMode()) {
            return 0.0F;
        }

        if (selectedCategory == Category.SERVER_EVENTS && !isSearchMode()) {
            if (eventsServer == 1) {
                int count = ServerEventsClient.getEvents().size();
                int rows = (count + 1) / 2;
                return Math.max(0.0F, 70.0F + rows * 86.0F - 516.0F);
            }

            if (eventsTab == 3) {
                return 0.0F;
            }

            int count;
            if (eventsTab == 2) {
                count = (int)FluxEventsClient.getMines()
                    .stream()
                    .filter(mine -> "all".equals(mineRarityFilter) || mine.nextMineRarity.name().equalsIgnoreCase(mineRarityFilter))
                    .count();
            } else {
                count = (int)FluxEventsClient.getEvents()
                    .stream()
                    .filter(FluxEventsClient$FluxEvent::shouldDisplay)
                    .filter(event -> !event.isActiveEvent() || EVENT_FILTERS.isEmpty() || EVENT_FILTERS.contains(event.id))
                    .filter(
                        event -> !event.isActiveEvent()
                            || "all".equals(eventPhaseFilter)
                            || event.phase != null && eventPhaseFilter.equalsIgnoreCase(event.phase)
                    )
                    .filter(event -> eventsTab == 0 ? event.isActiveEvent() : !event.isActiveEvent())
                    .count();
            }

            int rows = (count + 1) / 2;
            float totalH = 112.0F + rows * 86.0F;
            return Math.max(0.0F, totalH - 516.0F);
        } else {
            if (selectedCategory == Category.COSMETICS && !isSearchMode()) {
                String[] tabKeys = new String[]{"all", "wings", "hats", "weapons", "models"};
                int count = CosmeticsManager.INSTANCE.getByCategory(tabKeys[cosmeticsTab]).size();
                int rows = (count + 3) / 4;
                float totalH = 112.0F + rows * 155.0F;
                return Math.max(0.0F, totalH - 516.0F);
            }

            List<Module> modules = this.getFilteredModules();
            float[] columnY = new float[2];
            Arrays.fill(columnY, 72.0F);

            for (int index = 0; index < modules.size(); index++) {
                int col = index % 2;
                columnY[col] += 67.2F;
            }

            float maxColumnY = Math.max(columnY[0], columnY[1]);
            float visibleHeight = 454.0F;
            return Math.max(0.0F, maxColumnY - 72.0F - visibleHeight);
        }
    }

    static Module findBindingModule() {
        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.isBinding()) {
                return module;
            }
        }

        return null;
    }

    static void cancelBinding() {
        bindingSetting = null;

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.isBinding()) {
                module.setBinding(false);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void requestClose() {
        if (!this.closing) {
            this.closing = true;
            this.colorPickerOpen = false;
            this.modePopupOpen = false;
            activeTextSetting = null;
            cancelBinding();
        }
    }

    @Override
    public void close() {
        this.requestClose();
    }

    @Override
    public void removed() {
        ConfigManager.INSTANCE.saveConfig("default");
        if (ru.fluxvisuals.client.FluxVisualsClient.get != null
              && ru.fluxvisuals.client.FluxVisualsClient.get.configManager != null) {
            ru.fluxvisuals.client.FluxVisualsClient.get.configManager.autoSave();
        }
        cancelBinding();
        this.draggingSlider = null;
        this.draggingSV = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        activeTextSetting = null;
        this.modePopupOpen = false;
        this.modePopupSetting = null;
        this.modePopupProgress = 0.0F;
        this.colorPickerOpen = false;
        this.colorPickerProgress = 0.0F;
        activeColorPicker = null;
        this.restoreDefaultCursor();
        super.removed();
    }

    static ClickGui$SidebarEntry selectedEntry() {
        for (ClickGui$SidebarEntry entry : SIDEBAR) {
            if (entry.category() == selectedCategory) {
                return entry;
            }
        }

        return SIDEBAR[0];
    }

    List<Module> getFilteredModules() {
        List<Module> result = new ArrayList<>();
        if (isSearchMode()) {
            String query = searchQuery.toLowerCase(Locale.ROOT).trim();

            for (Module m : ModuleManager.INSTANCE.getModules()) {
                if (query.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(query) || m.getDescription().toLowerCase(Locale.ROOT).contains(query)) {
                    result.add(m);
                }
            }
        } else if (selectedCategory == Category.UTILITY) {
            for (Module mx : ModuleManager.INSTANCE.getModules()) {
                Category cat = mx.getCategory();
                if (cat == Category.UTILITY || cat == Category.PLAYER || cat == Category.MISC || cat == Category.INTERFACE) {
                    result.add(mx);
                }
            }
        } else {
            result.addAll(ModuleManager.INSTANCE.getModulesByCategory(selectedCategory));
        }

        result.removeIf(ClickGuiVisibilityPolicy::shouldHideModuleFromClickGui);
        result.removeIf(
            module -> module.getName().equalsIgnoreCase("FriendLabels")
                || module.getName().equalsIgnoreCase("AutoEvent")
                || module.getName().equalsIgnoreCase("AnarchyFinder")
        );
        result.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    static void drawCloseGlyph(DrawContext context, float centerX, float centerY, float size, int color) {
        float thickness = Math.max(1.6F, size * 0.2F);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().rotate((float) (Math.PI / 4));
        RoundedRenderer.drawRoundedRect(context, -size / 2.0F, -thickness / 2.0F, size, thickness, thickness / 2.0F, color);
        context.getMatrices().rotate((float) (Math.PI / 2));
        RoundedRenderer.drawRoundedRect(context, -size / 2.0F, -thickness / 2.0F, size, thickness, thickness / 2.0F, color);
        context.getMatrices().popMatrix();
    }

    float approach(float current, float target, float speed) {
        float factor = 1.0F - (float)Math.exp(-speed * this.frameDeltaSeconds);
        return MathHelper.lerp(factor, current, target);
    }

    static boolean isSearchMode() {
        return isSearching || !searchQuery.isEmpty();
    }

    static String formatNumber(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        return Math.abs(rounded - Math.rint(rounded)) < 1.0E-4 ? Integer.toString((int)Math.rint(rounded)) : Double.toString(rounded);
    }

    static List<String> enabledValues(MultiBooleanSetting setting) {
        List<String> enabled = new ArrayList<>();

        for (String value : setting.getValues()) {
            if (setting.isEnabled(value)) {
                enabled.add(value);
            }
        }

        return enabled;
    }

    static float modeFieldWidth(ModeSetting setting) {
        float textWidth = (float)Math.ceil(TextRender.width(setting.getValue(), 12.65F)) + 13.2F;
        return MathHelper.clamp(textWidth, 28.0F, 128.0F);
    }

    static String multiDisplayValue(MultiBooleanSetting setting) {
        List<String> enabled = enabledValues(setting);
        return enabled.isEmpty() ? "Не выбрано" : String.join(", ", enabled);
    }

    static float multiFieldWidth(MultiBooleanSetting setting) {
        float textWidth = (float)Math.ceil(TextRender.width(multiDisplayValue(setting), 12.65F)) + 13.2F;
        return MathHelper.clamp(textWidth, 28.0F, 128.0F);
    }

    static List<String> modeOptions(Setting<?> setting) {
        if (setting instanceof ModeSetting modeSetting) {
            return modeSetting.getModes();
        } else {
            return setting instanceof MultiBooleanSetting multiSetting ? multiSetting.getValues() : Collections.emptyList();
        }
    }

    static boolean isModeSelected(Setting<?> setting, String option) {
        return setting instanceof ModeSetting modeSetting
            ? modeSetting.is(option)
            : setting instanceof MultiBooleanSetting multiSetting && multiSetting.isEnabled(option);
    }

    static String trimTextFromStart(String text, float size, float maxWidth) {
        if (text != null && !text.isEmpty() && !(TextRender.width(text, size) <= maxWidth)) {
            String result = text;

            while (result.length() > 1 && TextRender.width(result, size) > maxWidth) {
                result = result.substring(1);
            }

            return result;
        } else {
            return text == null ? "" : text;
        }
    }

    void updateSliderCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() != null && this.sliderCursorRequested != this.resizeCursorActive) {
            long window = client.getWindow().getHandle();
            if (this.sliderCursorRequested) {
                if (resizeCursorHandle == 0L) {
                    resizeCursorHandle = GLFW.glfwCreateStandardCursor(221189);
                }

                GLFW.glfwSetCursor(window, resizeCursorHandle);
            } else {
                GLFW.glfwSetCursor(window, 0L);
            }

            this.resizeCursorActive = this.sliderCursorRequested;
        }
    }

    void restoreDefaultCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() != null && this.resizeCursorActive) {
            GLFW.glfwSetCursor(client.getWindow().getHandle(), 0L);
        }

        this.resizeCursorActive = false;
        this.sliderCursorRequested = false;
    }

    static String bindName(Module module) {
        return module.getKey() != -1 && module.getKey() != 0 ? "[" + Type.KEYSYM.createFromCode(module.getKey()).getLocalizedText().getString() + "]" : "[-]";
    }

    static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    static int fade(int color, float opacity) {
        int alpha = Math.round((color >>> 24) * opacity);
        return alpha << 24 | color & 16777215;
    }

    static int withAlpha(int rgb, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
    }

    static int accentAt(float x, float y) {
        float nx = MathHelper.clamp(x / 987.0F, 0.0F, 1.0F);
        float ny = MathHelper.clamp(y / 536.0F, 0.0F, 1.0F);
        float diagonal = (nx + ny) * 0.5F;
        float brightness = MathHelper.lerp(diagonal, 1.14F, 0.78F);
        int alpha = ACCENT >>> 24 & 0xFF;
        int r = ACCENT >> 16 & 0xFF;
        int g = ACCENT >> 8 & 0xFF;
        int b = ACCENT & 0xFF;
        r = MathHelper.clamp(Math.round(r * brightness), 0, 255);
        g = MathHelper.clamp(Math.round(g * brightness), 0, 255);
        b = MathHelper.clamp(Math.round(b * brightness), 0, 255);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    static int lerpColor(int from, int to, float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        int a1 = from >>> 24 & 0xFF;
        int r1 = from >> 16 & 0xFF;
        int g1 = from >> 8 & 0xFF;
        int b1 = from & 0xFF;
        int a2 = to >>> 24 & 0xFF;
        int r2 = to >> 16 & 0xFF;
        int g2 = to >> 8 & 0xFF;
        int b2 = to & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    static String trimText(String text, float size, float maxWidth) {
        if (text != null && !text.isEmpty()) {
            if (TextRender.width(text, size) <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            float ellW = TextRender.width(ellipsis, size);

            while (!text.isEmpty() && TextRender.width(text, size) + ellW > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }

            return text.trim() + ellipsis;
        } else {
            return "";
        }
    }
}
