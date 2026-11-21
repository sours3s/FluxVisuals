package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ConfigIdStub;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface CloudConfigApi {
    @GET("configs/")
    Call<List<CloudConfigMetadata>> listConfigs();

    @GET("configs/{id}")
    Call<CloudConfigDto> fetchConfig(@Path("id") String str);

    @POST("configs/")
    Call<ConfigIdStub> createConfig(@Body CreateConfigRequest createConfigRequest);

    @PUT("configs/")
    Call<Void> updateConfig(@Body UpdateConfigRequest updateConfigRequest);

    @PATCH("configs/")
    Call<Void> renameConfig(@Body RenameConfigRequest renameConfigRequest);

    @HTTP(method = "DELETE", path = "configs/", hasBody = true)
    Call<Void> deleteConfig(@Body DeleteConfigRequest deleteConfigRequest);

    @POST("configs/import")
    Call<Void> importConfig(@Body ImportConfigRequest importConfigRequest);
}
