package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.model.AccountProfile;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class AuthHeaderInterceptor implements Interceptor {
    public final AccountProfile accountProfile;

    public AuthHeaderInterceptor(AccountProfile accountProfile013) {
        this.accountProfile = accountProfile013;
    }

    @NotNull
    public Response intercept(Interceptor.Chain chain) throws java.io.IOException {
        Request.Builder builderHeader = chain.request().newBuilder().header("X-Uid", this.accountProfile.uid()).header("X-Hwid", this.accountProfile.hwid()).header("X-Login", this.accountProfile.login()).header("Authorization", this.accountProfile.signature());
        if (this.accountProfile.avatarUrl() != null) {
            builderHeader.header("X-Avatar-Url", this.accountProfile.avatarUrl());
        }
        return chain.proceed(builderHeader.build());
    }
}
