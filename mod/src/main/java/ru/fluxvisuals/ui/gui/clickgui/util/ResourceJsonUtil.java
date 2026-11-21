package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ResourceSource;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class ResourceJsonUtil {
    public static final Gson gson = new Gson();

    public static JsonObject toJson(ResourceSource resourceSource) {
        return JsonParser.parseString(toString(resourceSource)).getAsJsonObject();
    }

    public static <T> T fromJsonToInstance(ResourceSource resourceSource, Class<T> cls) {
        return (T) gson.fromJson(toString(resourceSource), cls);
    }

    public static String toString(ResourceSource resourceSource) {
        return toString(resourceSource, "\n");
    }

    public static String toString(ResourceSource resourceSource, String str) {
        try {
            InputStream inputStreamStream = resourceSource.stream();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStreamStream));
                try {
                    String str2 = (String) bufferedReader.lines().collect(Collectors.joining(str));
                    bufferedReader.close();
                    if (inputStreamStream != null) {
                        inputStreamStream.close();
                    }
                    return str2;
                } catch (Throwable th) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (inputStreamStream != null) {
                    try {
                        inputStreamStream.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                }
                throw th3;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
