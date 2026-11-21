package ru.fluxvisuals.ui.gui.clickgui.util.net;


public final class PinnedServer {
    public final String name;
    public final String address;

    public PinnedServer(String str, String str2) {
        this.name = str;
        this.address = str2;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "name=" + this.name + ", " + "address=" + this.address + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.name, this.address);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PinnedServer)) return false;
        PinnedServer o = (PinnedServer) obj;
        return java.util.Objects.equals(this.name, o.name) && java.util.Objects.equals(this.address, o.address);
    }
public String name() {
        return this.name;
    }

    public String address() {
        return this.address;
    }
}
