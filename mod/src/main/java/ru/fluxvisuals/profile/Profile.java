package ru.fluxvisuals.profile;

/** Профиль пользователя GUI FluxVisuals. */
public final class Profile {
    public enum Role {
        FREE,
        USER,
        ADMIN
    }

    public static Role role = Role.FREE;
    public static String subscriptionEndDate = "";

    private Profile() {
    }
}
