package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum BetterChatOption implements DisplayNamed {
    ANTI_SPAM(Lang.BETTERCHAT_CHAT_IMPROVEMENTS_ANTI_SPAM),
    ANTI_CLEAR(Lang.BETTERCHAT_CHAT_IMPROVEMENTS_ANTI_CLEAR),
    INFINITY(Lang.BETTERCHAT_CHAT_IMPROVEMENTS_INFINITY);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    BetterChatOption(Translation translation) {
        this.displayName = translation;
    }
}
