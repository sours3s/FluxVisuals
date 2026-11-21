package ru.fluxvisuals.ui.gui.clickgui.util.net;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import ru.fluxvisuals.ui.gui.clickgui.mixin.accessors.ServerListAccessor;

public final class PinnedServerInjector {
    public static final List<PinnedServer> pinnedServers = List.of(new PinnedServer("MetaHvH", "exp.MetaHVH.space"));

    public PinnedServerInjector() {
    }

    public static void inject(ServerList serverList) {
        List<ServerInfo> listFlux$getServers = ((ServerListAccessor) serverList).flux$getServers();
        for (int size = pinnedServers.size() - 1; size >= 0; size--) {
            PinnedServer pinnedServer = pinnedServers.get(size);
            boolean z = false;
            Iterator<ServerInfo> it = listFlux$getServers.iterator();
            while (it.hasNext()) {
                ServerInfo serverInfoEntry = it.next();
                PinnableServer pinnableServer = (PinnableServer) (Object) serverInfoEntry;
                if (serverInfoEntry.address.equalsIgnoreCase(pinnedServer.address())) {
                    pinnableServer.flux$setPinned(true);
                    z = true;
                    break;
                }
            }
            if (!z) {
                ServerInfo serverInfo = new ServerInfo(pinnedServer.name(), pinnedServer.address(), ServerInfo.ServerType.OTHER);
                ((PinnableServer) (Object) serverInfo).flux$setPinned(true);
                listFlux$getServers.add(0, serverInfo);
            }
        }
    }
}
