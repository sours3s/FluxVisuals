package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigDto;


public final class LoadedConfig {
    public final CloudConfigDto details;
    public final ConfigFile bundle;

    public LoadedConfig(CloudConfigDto cloudConfigDto, ConfigFile configFile) {
        this.details = cloudConfigDto;
        this.bundle = configFile;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "details=" + this.details + ", " + "bundle=" + this.bundle + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.details, this.bundle);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LoadedConfig)) return false;
        LoadedConfig o = (LoadedConfig) obj;
        return java.util.Objects.equals(this.details, o.details) && java.util.Objects.equals(this.bundle, o.bundle);
    }
public CloudConfigDto details() {
        return this.details;
    }

    public ConfigFile bundle() {
        return this.bundle;
    }
}
