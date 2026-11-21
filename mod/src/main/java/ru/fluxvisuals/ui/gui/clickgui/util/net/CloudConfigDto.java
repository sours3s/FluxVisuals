package ru.fluxvisuals.ui.gui.clickgui.util.net;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.Base64;

public class CloudConfigDto {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("base64")
    public String base64;

    @SerializedName("author")
    public String author;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    @SerializedName("is_local_override")
    public boolean localOverride;

    public static CloudConfigDto of(String str, String str2, String str3) {
        CloudConfigDto cloudConfigDto = new CloudConfigDto();
        cloudConfigDto.id = str;
        cloudConfigDto.name = str2;
        cloudConfigDto.author = str3;
        return cloudConfigDto;
    }

    public byte[] decodeData() {
        return Base64.getDecoder().decode(this.base64);
    }

    public long createdTimestamp() {
        return parseTimestamp(this.createdAt);
    }

    public long updatedTimestamp() {
        return parseTimestamp(this.updatedAt);
    }

    public static long parseTimestamp(String str) {
        try {
            return Instant.parse(str).toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String base64() {
        return this.base64;
    }

    public String author() {
        return this.author;
    }

    public String createdAt() {
        return this.createdAt;
    }

    public String updatedAt() {
        return this.updatedAt;
    }

    public boolean isLocalOverride() {
        return this.localOverride;
    }
}
