package ru.fluxvisuals.ui.gui.clickgui.util.type;

public interface PhaseRule {
    boolean shouldEnter();

    TpLootStage getPhase();
}
