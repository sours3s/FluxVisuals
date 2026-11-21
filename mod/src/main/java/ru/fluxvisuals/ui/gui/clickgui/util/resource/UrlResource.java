package ru.fluxvisuals.ui.gui.clickgui.util.resource;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

public class UrlResource implements ResourceSource {
    public final String url;

    @Override
    public InputStream stream() {
        try {
            FluxVisuals.LOGGER.debug("Trying to load URL resource: {}", this.url);
            URI uriCreate = URI.create(this.url);
            FluxVisuals.LOGGER.debug("Parsed URI: {}", uriCreate);
            URLConnection uRLConnectionOpenConnection = uriCreate.toURL().openConnection();
            uRLConnectionOpenConnection.setUseCaches(false);
            uRLConnectionOpenConnection.setConnectTimeout(5000);
            uRLConnectionOpenConnection.setReadTimeout(5000);
            uRLConnectionOpenConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            return uRLConnectionOpenConnection.getInputStream();
        } catch (Exception e) {
            FluxVisuals.LOGGER.error("Failed to open URL resource: {}", this.url, e);
            throw new RuntimeException("Failed to open URL resource: " + this.url, e);
        }
    }

    public UrlResource(String str) {
        this.url = str;
    }
}
