package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigMetadata;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.StaffProfile;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CloudConfigCard {
    public String name;
    public final String cloudId;
    public final String date;
    public final long lastModified;
    public StaffProfile author;

    public CloudConfigCard(CloudConfigMetadata cloudConfigMetadata, GlTexture glTexture) {
        this.name = cloudConfigMetadata.name();
        this.date = formatDate(cloudConfigMetadata.createdTimestamp());
        this.lastModified = cloudConfigMetadata.updatedTimestamp();
        this.cloudId = cloudConfigMetadata.id();
        this.author = new StaffProfile(cloudConfigMetadata.author(), "", glTexture, cloudConfigMetadata.authorAvatarUrl());
    }

    public String formatDate(long j) {
        return new SimpleDateFormat("dd.MM.yyyy").format(new Date(j));
    }

    public String name() {
        return this.name;
    }

    public String cloudId() {
        return this.cloudId;
    }

    public String date() {
        return this.date;
    }

    public long lastModified() {
        return this.lastModified;
    }

    public StaffProfile author() {
        return this.author;
    }

    public CloudConfigCard name(String str) {
        this.name = str;
        return this;
    }

    public CloudConfigCard author(StaffProfile staffProfile) {
        this.author = staffProfile;
        return this;
    }
}
