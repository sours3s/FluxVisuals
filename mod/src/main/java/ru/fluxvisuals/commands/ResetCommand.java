package ru.fluxvisuals.commands;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;

@Environment(EnvType.CLIENT)
public final class ResetCommand implements Command {
    private static final ResetCommand INSTANCE = new ResetCommand();
    private static final List<String> ALIASES = List.of(".reset");

    private ResetCommand() {
    }

    public static ResetCommand getInstance() {
        return INSTANCE;
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public List<String> aliases() {
        return ALIASES;
    }

    @Override
    public String usage() {
        return ".reset";
    }

    @Override
    public String description() {
        return "Полный сброс клиента: модули, настройки, позиции HUD, друзья и темы";
    }

    @Override
    public void execute(CommandContext context, String args) throws CommandException {
        if (FluxVisualsClient.get == null || FluxVisualsClient.get.configManager == null) {
            throw new CommandException("Клиент ещё не инициализирован");
        }
        FluxVisualsClient.get.configManager.resetAll();
        context.sendSuccess("FluxVisuals: все настройки и позиции сброшены");
    }
}
