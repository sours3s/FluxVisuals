package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.AesCipherUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.AtomicFileWriter;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CredentialKey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CredentialStore {
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    static final Path legacyFile = Path.of("autoauth_credentials.txt", new String[0]);

    public final Path path;

    public Path path() {
        return this.path;
    }

    public Map<CredentialKey, String> loadCredentials() throws IOException {
        List<StoredCredential> list;
        if (!Files.exists(this.path, new LinkOption[0])) {
            Map<CredentialKey, String> mapMethod001 = loadLegacyCredentials();
            if (!mapMethod001.isEmpty()) {
                saveCredentials(mapMethod001);
            }
            return mapMethod001;
        }
        String string = Files.readString(this.path, StandardCharsets.UTF_8);
        if (!string.isBlank() && (list = (List) gson.fromJson(string, new CredentialListTypeToken(this).getType())) != null) {
            HashMap map = new HashMap();
            for (StoredCredential storedCredential : list) {
                if (storedCredential != null && storedCredential.password != null && storedCredential.token != null && storedCredential.username != null) {
                    try {
                        map.put(new CredentialKey(storedCredential.password, storedCredential.token), AesCipherUtil.decrypt(storedCredential.username));
                    } catch (Exception e) {
                    }
                }
            }
            return map;
        }
        return Map.of();
    }

    public void saveCredentials(Map<CredentialKey, String> map) throws IOException {
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<CredentialKey, String> entry : map.entrySet()) {
            CredentialKey key = entry.getKey();
            if (key != null && entry.getValue() != null) {
                try {
                    arrayList.add(new StoredCredential(key.serverAddress(), key.username(), AesCipherUtil.encrypt(entry.getValue())));
                } catch (Exception e) {
                }
            }
        }
        AtomicFileWriter.writeBytes(this.path, gson.toJson(arrayList).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public Map<CredentialKey, String> loadLegacyCredentials() throws IOException {
        if (!Files.exists(legacyFile, new LinkOption[0])) {
            return Map.of();
        }
        HashMap map = new HashMap();
        Iterator<String> it = Files.readAllLines(legacyFile, StandardCharsets.UTF_8).iterator();
        while (it.hasNext()) {
            String[] strArrSplit = it.next().split(":", 3);
            try {
                map.put(new CredentialKey(strArrSplit[0], strArrSplit[1]), AesCipherUtil.decrypt(strArrSplit[2]));
            } catch (Exception e) {
            }
        }
        return map;
    }

    public CredentialStore(Path path) {
        this.path = path;
    }
}
