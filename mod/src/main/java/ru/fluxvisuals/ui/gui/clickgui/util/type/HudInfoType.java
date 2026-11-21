package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum HudInfoType implements DisplayNamed {
    NICKNAME(Lang.WIDGET_WATERMARK_LINE_NICKNAME),
    LATENCY(Lang.WIDGET_WATERMARK_LINE_LATENCY),
    FRAMERATE(Lang.WIDGET_WATERMARK_LINE_FRAMERATE),
    CURRENT_TIME(Lang.WIDGET_WATERMARK_LINE_CURRENT_TIME),
    SERVER_NAME(Lang.WIDGET_WATERMARK_LINE_SERVER_NAME);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    HudInfoType(Translation translation) {
        this.displayName = translation;
    }

    public Translation displayName() {
        return this.displayName;
    }
}
