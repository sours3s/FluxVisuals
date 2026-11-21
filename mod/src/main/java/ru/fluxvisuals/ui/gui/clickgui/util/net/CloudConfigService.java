package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.model.AccountProfile;
import ru.fluxvisuals.ui.gui.clickgui.util.AtomicFileWriter;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ConfigReference;
import ru.fluxvisuals.ui.gui.clickgui.config.ConfigSerializer;
import ru.fluxvisuals.ui.gui.clickgui.events.api.FutureCallback;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.config.LoadedConfig;
import ru.fluxvisuals.ui.gui.clickgui.config.ModuleRepository;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.events.api.VoidFutureCallback;
import ru.fluxvisuals.ui.gui.clickgui.WidgetStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CloudConfigService {
    public static final String baseUrl = "http://127.0.0.1:1/";
    public static final String configRoot = "flux/config/configs";
    public final ConfigSerializer serializer = new ConfigSerializer();

    public final AtomicReference<ConfigReference> activeConfigRef = new AtomicReference<>();

    public volatile CloudConfigApi api;

    public volatile AccountProfile accountProfile;

    public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public final Path configDir = Path.of(configRoot);

    public CompletableFuture<Void> initialize(String str, String str2, String str3, String str4, String str5) {
        return CompletableFuture.runAsync(() -> {
            this.accountProfile = new AccountProfile(str, str2, str3, str4, str5);
            ensureConfigDir();
        });
    }

    public CloudConfigApi buildApi(AccountProfile accountProfile) {
        return (CloudConfigApi) new Retrofit.Builder().baseUrl(baseUrl).client(new OkHttpClient.Builder().connectTimeout(10L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).addInterceptor(new AuthHeaderInterceptor(accountProfile)).build()).addConverterFactory(GsonConverterFactory.create()).build().create(CloudConfigApi.class);
    }

    public boolean initialized() {
        return (this.accountProfile == null || this.accountProfile.signature() == null) ? false : true;
    }

    public boolean configActive(String str) {
        ConfigReference configReference = this.activeConfigRef.get();
        return (configReference == null || str == null || !str.equals(configReference.id())) ? false : true;
    }

    public Optional<ConfigReference> activeConfig() {
        return Optional.ofNullable(this.activeConfigRef.get());
    }

    public CompletableFuture<List<CloudConfigMetadata>> listConfigs() {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<CloudConfigMetadata> arrayList = new ArrayList<>();
            ensureConfigDir();
            try (Stream<Path> streamList = Files.list(this.configDir)) {
                streamList.filter(path -> {
                    return path.getFileName().toString().endsWith(".json");
                }).forEach(path2 -> {
                    CloudConfigDto cloudConfigDto = readDto(path2);
                    if (cloudConfigDto != null) {
                        CloudConfigMetadata cloudConfigMetadata = new CloudConfigMetadata();
                        cloudConfigMetadata.id = cloudConfigDto.id();
                        cloudConfigMetadata.name = cloudConfigDto.name();
                        cloudConfigMetadata.author = cloudConfigDto.author();
                        cloudConfigMetadata.createdAt = cloudConfigDto.createdAt();
                        cloudConfigMetadata.updatedAt = cloudConfigDto.updatedAt();
                        cloudConfigMetadata.authorAvatarUrl = null;
                        cloudConfigMetadata.localOverride = cloudConfigDto.isLocalOverride();
                        arrayList.add(cloudConfigMetadata);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to list configs", e);
            }
            return arrayList;
        });
    }

    public CompletableFuture<CloudConfigDto> fetchConfig(String str) {
        return CompletableFuture.supplyAsync(() -> {
            CloudConfigDto cloudConfigDto = readDto(pathFor(str));
            if (cloudConfigDto == null) {
                throw new RuntimeException("Config not found");
            }
            return cloudConfigDto;
        });
    }

    public CompletableFuture<String> createConfig(String str, ModuleRepository moduleRepository, WidgetStack widgetStack) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConfigDir();
            if (nameExists(str)) {
                throw new RuntimeException("name already exists");
            }
            String strUuid = UUID.randomUUID().toString();
            String strNow = Instant.now().toString();
            CloudConfigDto cloudConfigDto = new CloudConfigDto();
            cloudConfigDto.id = strUuid;
            cloudConfigDto.name = str;
            cloudConfigDto.base64 = serializeToBase64(moduleRepository, widgetStack);
            cloudConfigDto.author = currentLogin();
            cloudConfigDto.createdAt = strNow;
            cloudConfigDto.updatedAt = strNow;
            cloudConfigDto.localOverride = true;
            writeDto(cloudConfigDto);
            return strUuid;
        });
    }

    public CompletableFuture<Void> saveConfig(String str, ModuleRepository moduleRepository, WidgetStack widgetStack) {
        return CompletableFuture.runAsync(() -> {
            CloudConfigDto cloudConfigDto = readDto(pathFor(str));
            if (cloudConfigDto == null) {
                throw new RuntimeException("Config not found");
            }
            cloudConfigDto.base64 = serializeToBase64(moduleRepository, widgetStack);
            cloudConfigDto.updatedAt = Instant.now().toString();
            writeDto(cloudConfigDto);
        });
    }

    public CompletableFuture<Void> renameConfig(String str, String str2) {
        return CompletableFuture.runAsync(() -> {
            CloudConfigDto cloudConfigDto = readDto(pathFor(str));
            if (cloudConfigDto == null) {
                throw new RuntimeException("Config not found");
            }
            if (nameExists(str2)) {
                throw new RuntimeException("name already exists");
            }
            cloudConfigDto.name = str2;
            cloudConfigDto.updatedAt = Instant.now().toString();
            writeDto(cloudConfigDto);
        });
    }

    public CompletableFuture<Void> deleteConfig(String str) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(pathFor(str));
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete config", e);
            }
        });
    }

    public CompletableFuture<Void> importConfig(String str) {
        return CompletableFuture.runAsync(() -> {
            if (readDto(pathFor(str)) == null) {
                throw new RuntimeException("Config not found");
            }
        });
    }

    public CompletableFuture<LoadedConfig> downloadConfig(String str) {
        return fetchConfig(str).thenApply(cloudConfigDto -> {
            try {
                return new LoadedConfig(cloudConfigDto, this.serializer.deserialize(cloudConfigDto.decodeData()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize config", e);
            }
        });
    }

    public void applyConfig(LoadedConfig loadedConfig) {
        try {
            this.serializer.applyConfig(loadedConfig);
            this.activeConfigRef.set(new ConfigReference(loadedConfig.details().id(), loadedConfig.details().name()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply config", e);
        }
    }

    public Translation errorMessage(Throwable th) {
        String strMethod007 = extractErrorMessage(th);
        return strMethod007 == null ? Lang.CLOUD_ERROR_GENERIC : mapErrorMessage(strMethod007);
    }

    public String extractErrorMessage(Throwable th) {
        if (th.getMessage() != null) {
            return th.getMessage();
        }
        if (th.getCause() != null) {
            return th.getCause().getMessage();
        }
        return null;
    }

    public Translation mapErrorMessage(String str) {
        if (str.contains("name already exists") || str.contains("name already named")) {
            return Lang.CLOUD_ERROR_NAME_EXISTS;
        }
        if (str.contains("not found")) {
            return Lang.CLOUD_ERROR_NOT_FOUND;
        }
        if (str.contains("already in user list")) {
            return Lang.CLOUD_ERROR_ALREADY_LISTED;
        }
        if (str.contains("unauthorized")) {
            return Lang.CLOUD_ERROR_UNAUTHORIZED;
        }
        if (str.contains("Failed to create")) {
            return Lang.CLOUD_ERROR_CREATE_FAILED;
        }
        return str.contains("Failed to get") ? Lang.CLOUD_ERROR_GET_FAILED : Lang.CLOUD_ERROR_GENERIC;
    }

    public String serializeToBase64(ModuleRepository moduleRepository, WidgetStack widgetStack) {
        try {
            return Base64.getEncoder().encodeToString(this.serializer.serialize(moduleRepository, widgetStack));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize modules config", e);
        }
    }

    public String currentLogin() {
        AccountProfile accountProfile013 = this.accountProfile;
        return accountProfile013 == null ? "" : accountProfile013.login();
    }

    public void ensureConfigDir() {
        try {
            Files.createDirectories(this.configDir, new FileAttribute[0]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }
    }

    public Path pathFor(String str) {
        return this.configDir.resolve(str + ".json");
    }

    public CloudConfigDto readDto(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            return null;
        }
        try {
            String strRead = Files.readString(path, StandardCharsets.UTF_8);
            return (CloudConfigDto) this.gson.fromJson(strRead, CloudConfigDto.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void writeDto(CloudConfigDto cloudConfigDto) {
        ensureConfigDir();
        byte[] bytes = this.gson.toJson(cloudConfigDto).getBytes(StandardCharsets.UTF_8);
        try {
            AtomicFileWriter.writeBytes(pathFor(cloudConfigDto.id()), bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config", e);
        }
    }

    public boolean nameExists(String str) {
        try (Stream<Path> streamList = Files.list(this.configDir)) {
            return streamList.filter(path -> {
                return path.getFileName().toString().endsWith(".json");
            }).map(this::readDto).filter(cloudConfigDto -> {
                return cloudConfigDto != null;
            }).anyMatch(class376Var2 -> {
                return str.equals(class376Var2.name());
            });
        } catch (IOException e) {
            return false;
        }
    }

    public static <T> CompletableFuture<T> toFuture(Call<T> call) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        call.enqueue(new FutureCallback(completableFuture));
        return completableFuture;
    }

    public static CompletableFuture<Void> toVoidFuture(Call<Void> call) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        call.enqueue(new VoidFutureCallback(completableFuture));
        return completableFuture;
    }

    public static RuntimeException httpError(Response<?> response) {
        String strString;
        try {
            strString = response.errorBody() != null ? response.errorBody().string() : "";
        } catch (IOException e) {
            strString = "";
        }
        return new RuntimeException("HTTP " + response.code() + ": " + strString);
    }
}
