package ru.fluxvisuals.config.friend;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.client.FluxVisualsClient;

@Environment(EnvType.CLIENT)
public class FriendManager {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static final List<Friend> friends = new ArrayList<>();
    public static final File file = new File(FluxVisualsClient.get.root + "\\configs", "friend.cfg");

    public static void init() {
        try {
            Files.createDirectories(file.toPath().getParent());
            if (!file.exists()) {
                file.createNewFile();
            } else {
                friends.clear();
                readFriends();
            }
        } catch (Exception var1) {
            var1.printStackTrace();
        }
    }

    public void add(String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (!normalizedName.isEmpty() && !this.isFriend(normalizedName)) {
            friends.add(new Friend(normalizedName));
            this.updateFile();
        }
    }

    public Friend getFriend(String friend) {
        return friends.stream().filter(isFriend -> isFriend.getName().equalsIgnoreCase(friend)).findFirst().get();
    }

    public static boolean isFriend(String friend) {
        return friend != null && friends.stream().anyMatch(isFriend -> isFriend.getName().equalsIgnoreCase(friend));
    }

    public void remove(String name) {
        friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
        this.updateFile();
    }

    public void clearFriend() {
        friends.clear();
        this.updateFile();
    }

    public static List<Friend> getFriends() {
        return friends;
    }

    public static boolean getNearFriends(String name) {
        return mc.world.getPlayers().stream().anyMatch(player -> player.getName().getString().equals(name));
    }

    public void updateFile() {
        try {
            StringBuilder builder = new StringBuilder();
            friends.forEach(friend -> builder.append(friend.getName()).append("\n"));
            Files.writeString(file.toPath(), builder.toString(), StandardCharsets.UTF_8);
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    public static void readFriends() {
        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new DataInputStream(new FileInputStream(file.getAbsolutePath())), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String name = line.trim();
                    if (!name.isEmpty() && friends.stream().noneMatch(friend -> friend.getName().equalsIgnoreCase(name))) {
                        friends.add(new Friend(name));
                    }
                }
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }
}