package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AntiAfkAction implements DisplayNamed {
    TURN_HEAD(Lang.ANTIAFK_ACTION_TURN_HEAD),
    JUMP(Lang.ANTIAFK_ACTION_JUMP),
    SEND_CHAT_MESSAGE(Lang.ANTIAFK_ACTION_SEND_CHAT_MESSAGE);

    final Translation displayName;

    AntiAfkAction(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
