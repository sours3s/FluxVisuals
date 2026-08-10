package ru.fluxvisuals.screen.screens.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AccountManager {

    private static AccountManager instance;
    private final List<Account> accounts = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private Account currentAccount;
    private final File accountsFile;
    private final File favoritesFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private AccountManager() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, "_FLUX_ACCOUNTS_DO_NOT_SEND_TO_ANYONE");
        accountsFile = new File(dir, "accounts.json");
        favoritesFile = new File(dir, "favorites.json");
        load();
        loadFavorites();
    }

    public static AccountManager getInstance() {
        if (instance == null) {
            instance = new AccountManager();
        }
        return instance;
    }

    public void addAccount(Account account) {
        if (accounts.stream().noneMatch(a -> a.getUsername().equalsIgnoreCase(account.getUsername()))) {
            accounts.add(account);
            save();
        }
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        save();
    }

    public void removeAccount(String username) {
        accounts.removeIf(a -> a.getUsername().equalsIgnoreCase(username));
        save();
    }

    public boolean isFavorite(String username) {
        return favorites.contains(username.toLowerCase());
    }

    public void toggleFavorite(String username) {
        String key = username.toLowerCase();
        if (favorites.contains(key)) {
            favorites.remove(key);
        } else {
            favorites.add(key);
        }
        saveFavorites();
    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public Optional<Account> getAccount(String username) {
        return accounts.stream()
                .filter(a -> a.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public boolean loginOffline(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        username = username.trim();
        Account account = new Account(username, Account.AccountType.OFFLINE);
        try {
            setSession(username, account.getUuid());
            currentAccount = account;
            addAccount(account);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(Account account) {
        if (account.getType() == Account.AccountType.OFFLINE) {
            return loginOffline(account.getUsername());
        }
        return false;
    }

    private void setSession(String username, UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();

        Session newSession = new Session(
                username,
                uuid,
                "",
                Optional.empty(),
                Optional.empty()
        );

        try {
            java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(client, newSession);
        } catch (NoSuchFieldException e) {
            try {
                for (java.lang.reflect.Field field : MinecraftClient.class.getDeclaredFields()) {
                    if (field.getType() == Session.class) {
                        field.setAccessible(true);
                        field.set(client, newSession);
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            accountsFile.getParentFile().mkdirs();

            List<AccountData> dataList = accounts.stream()
                    .map(a -> new AccountData(a.getUsername(), a.getUuid().toString(), a.getType().name()))
                    .toList();

            try (Writer writer = new FileWriter(accountsFile)) {
                gson.toJson(dataList, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!accountsFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(accountsFile)) {
            Type listType = new TypeToken<List<AccountData>>(){}.getType();
            List<AccountData> dataList = gson.fromJson(reader, listType);

            if (dataList != null) {
                accounts.clear();
                for (AccountData data : dataList) {
                    Account.AccountType type = Account.AccountType.valueOf(data.type);
                    UUID uuid = UUID.fromString(data.uuid);
                    accounts.add(new Account(data.username, uuid, type));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static class AccountData {
        String username;
        String uuid;
        String type;

        AccountData(String username, String uuid, String type) {
            this.username = username;
            this.uuid = uuid;
            this.type = type;
        }
    }

    private void saveFavorites() {
        try {
            favoritesFile.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(favoritesFile)) {
                gson.toJson(new ArrayList<>(favorites), writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFavorites() {
        if (!favoritesFile.exists()) return;
        try (Reader reader = new FileReader(favoritesFile)) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> list = gson.fromJson(reader, listType);
            if (list != null) {
                favorites.clear();
                favorites.addAll(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}