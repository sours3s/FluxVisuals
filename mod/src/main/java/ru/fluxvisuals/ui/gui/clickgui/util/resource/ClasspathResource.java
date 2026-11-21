package ru.fluxvisuals.ui.gui.clickgui.util.resource;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ClasspathResource implements ResourceSource {
    public final String path;
    static final String assetPrefix = "/assets/fluxvisuals/";

    @Override
    public InputStream stream() {
        try {
            String strSubstring = this.path;
            if (strSubstring.startsWith("/")) {
                strSubstring = strSubstring.substring(1);
            }
            if (!strSubstring.startsWith("assets/fluxvisuals/")) {
                strSubstring = "assets/fluxvisuals/" + strSubstring;
            }
            String str = "/" + strSubstring;
            FluxVisuals.LOGGER.debug("Trying to load classpath resource: {}", str);
            URL resource = ClasspathResource.class.getResource(str);
            if (resource == null) {
                throw new IllegalStateException("Classpath resource not found: " + str);
            }
            FluxVisuals.LOGGER.debug("Found resource URL: {}", resource);
            URLConnection uRLConnectionOpenConnection = resource.openConnection();
            uRLConnectionOpenConnection.setUseCaches(false);
            return uRLConnectionOpenConnection.getInputStream();
        } catch (Exception e) {
            FluxVisuals.LOGGER.error("Failed to open classpath resource: {}", this.path, e);
            throw new RuntimeException("Failed to open classpath resource: " + this.path, e);
        }
    }

    public ClasspathResource(String str) {
        this.path = str;
    }
}
