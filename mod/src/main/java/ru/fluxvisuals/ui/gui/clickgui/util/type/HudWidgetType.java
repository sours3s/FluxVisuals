package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum HudWidgetType implements DisplayNamed {
    WATERMARK(Translation.clearText("Watermark")),
    POTION_LIST(Translation.clearText("Potion List")),
    TARGET_HUD(Translation.clearText("Target Hud")),
    HOTKEYS(Translation.clearText("Hotkeys")),
    COORDS(Translation.clearText("Coords")),
    ITEM_BIND(Translation.clearText("Item Bind")),
    STAFF_LIST(Translation.clearText("Staff List")),
    NOTIFICATION(Translation.clearText("Notifications")),
    TRAP_TIMER(Translation.clearText("Trap Timer"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    HudWidgetType(Translation translation) {
        this.displayName = translation;
    }
}
