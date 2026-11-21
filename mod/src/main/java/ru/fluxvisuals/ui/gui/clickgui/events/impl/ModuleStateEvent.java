package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;


public final class ModuleStateEvent implements Event {
    public final Module module;
    public final boolean moduleState;

    public ModuleStateEvent(Module module605, boolean z) {
        this.module = module605;
        this.moduleState = z;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "module=" + this.module + ", " + "moduleState=" + this.moduleState + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.module, this.moduleState);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ModuleStateEvent)) return false;
        ModuleStateEvent o = (ModuleStateEvent) obj;
        return java.util.Objects.equals(this.module, o.module) && java.util.Objects.equals(this.moduleState, o.moduleState);
    }
public Module module() {
        return this.module;
    }

    public boolean moduleState() {
        return this.moduleState;
    }
}
