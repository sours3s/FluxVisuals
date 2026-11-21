package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.ChatUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ConfigErrorNotice;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.KeyInputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeyPressState;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MacroEntry;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroRepository {
    public final List<MacroEntry> macroList = new ArrayList();
    public static final Logger logger = LoggerFactory.getLogger(MacroRepository.class);

    public MacroRepository() {
        FluxVisuals.INSTANCE.eventDispatcher().register(KeyInputEvent.class, keyInputEvent -> {
            if (Mc.INSTANCE.isWorldLoaded() && !this.macroList.isEmpty() && keyInputEvent.action() == KeyPressState.PRESS) {
                int iKey = keyInputEvent.key();
                this.macroList.stream().filter(macroEntry -> {
                    return macroEntry.key() == iKey;
                }).findFirst().ifPresent(class724Var2 -> {
                    try {
                        if (class724Var2.content().startsWith("/")) {
                            Mc.INSTANCE.getNetworkHandler().sendChatCommand(class724Var2.content().substring(1));
                        } else {
                            Mc.INSTANCE.getNetworkHandler().sendChatMessage(class724Var2.content());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to send macro command {}", class724Var2.name(), e);
                        ChatUtil.addChatMessage("Ошибка при отправке команды. См. latest.log");
                    }
                });
            }
        });
    }

    public boolean isEmpty() {
        return this.macroList.isEmpty();
    }

    public void addMacro(String str, String str2, int i) {
        this.macroList.add(new MacroEntry(str, str2, i));
        try {
            FluxVisuals.INSTANCE.configManager().saveMacros();
        } catch (IOException e) {
            logger.error("Failed to persist macros after addMacro(name={})", str, e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении макросов"));
        }
    }

    public boolean hasMacro(String str) {
        Iterator<MacroEntry> it = this.macroList.iterator();
        while (it.hasNext()) {
            if (it.next().name().equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public void deleteMacro(String str) {
        if (this.macroList.stream().anyMatch(macroEntry -> {
            return macroEntry.name().equals(str);
        })) {
            this.macroList.removeIf(class724Var2 -> {
                return class724Var2.name().equalsIgnoreCase(str);
            });
            try {
                FluxVisuals.INSTANCE.configManager().saveMacros();
            } catch (IOException e) {
                logger.error("Failed to persist macros after deleteMacro(name={})", str, e);
                ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("удалении макроса"));
            }
        }
    }

    public void clearList() {
        if (this.macroList.isEmpty()) {
            return;
        }
        this.macroList.clear();
        try {
            FluxVisuals.INSTANCE.configManager().saveMacros();
        } catch (IOException e) {
            logger.error("Failed to persist macros after clearList()", e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("очистке макросов"));
        }
    }

    public List<MacroEntry> getMacroList() {
        return this.macroList;
    }
}
