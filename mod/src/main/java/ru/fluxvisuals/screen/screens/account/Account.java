package ru.fluxvisuals.screen.screens.account;

import java.util.UUID;

public class Account {
    private final String username;
    private final UUID uuid;
    private final AccountType type;

    public Account(String username, AccountType type) {
        this.username = username;
        this.type = type;
        this.uuid = type == AccountType.OFFLINE
                ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes())
                : UUID.randomUUID();
    }

    public Account(String username, UUID uuid, AccountType type) {
        this.username = username;
        this.uuid = uuid;
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public AccountType getType() {
        return type;
    }

    public String getTypeDisplayName() {
        return switch (type) {
            case OFFLINE -> "Offline";
            case MICROSOFT -> "Microsoft";
        };
    }

    public enum AccountType {
        OFFLINE,
        MICROSOFT
    }
}