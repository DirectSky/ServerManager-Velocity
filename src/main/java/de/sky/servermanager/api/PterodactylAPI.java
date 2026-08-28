package de.sky.servermanager.api;

import de.sky.servermanager.config.PluginConfig;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;

public class PterodactylAPI {

    private static final MediaType JSON = MediaType.get("application/json");

    private final PluginConfig config;
    private final Logger logger;
    private final OkHttpClient client;

    public PterodactylAPI(PluginConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.client = new OkHttpClient();
    }

    public boolean sendPowerAction(String serverId, String signal) {
        String url = config.getPanelUrl() + "/api/client/servers/" + serverId + "/power";
        String body = "{\"signal\":\"" + signal + "\"}";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 204) return true;
            logger.warn("Power action '{}' für Server {} gab HTTP {} zurück.", signal, serverId, response.code());
            return false;
        } catch (IOException e) {
            logger.error("Fehler beim Senden der Power Action an Pterodactyl!", e);
            return false;
        }
    }

    public String getServerState(String serverId) {
        String url = config.getPanelUrl() + "/api/client/servers/" + serverId + "/resources";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Accept", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String b = response.body().string();
            if (b.contains("\"current_state\":\"running\"")) return "running";
            if (b.contains("\"current_state\":\"starting\"")) return "starting";
            if (b.contains("\"current_state\":\"stopping\"")) return "stopping";
            if (b.contains("\"current_state\":\"offline\"")) return "offline";
            return null;
        } catch (IOException e) {
            logger.error("Fehler beim Abrufen des Server-Status von Pterodactyl!", e);
            return null;
        }
    }
}
