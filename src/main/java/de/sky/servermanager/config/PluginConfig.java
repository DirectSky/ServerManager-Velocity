package de.sky.servermanager.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PluginConfig {

    private final Path dataDirectory;
    private final Logger logger;

    private String apiKey;
    private String panelUrl;
    private int connectCheckInterval;
    private int connectTimeout;
    private final Map<String, String> servers = new HashMap<>();

    public PluginConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        load();
    }

    public void load() {
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);

            Path configFile = dataDirectory.resolve("config.yml");
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) Files.copy(in, configFile);
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configFile).build();
            ConfigurationNode root = loader.load();

            this.apiKey = root.node("api-key").getString("");
            this.panelUrl = root.node("panel-url").getString("").replaceAll("/$", "");
            this.connectCheckInterval = root.node("connect-check-interval").getInt(3);
            this.connectTimeout = root.node("connect-timeout").getInt(60);

            servers.clear();
            ConfigurationNode serversNode = root.node("servers");
            if (!serversNode.isNull()) {
                serversNode.childrenMap().forEach((key, value) ->
                        servers.put(key.toString().toLowerCase(), value.getString("")));
            }

        } catch (IOException e) {
            logger.error("Fehler beim Laden von config.yml", e);
        }
    }

    public String getApiKey() { return apiKey; }
    public String getPanelUrl() { return panelUrl; }
    public int getConnectCheckInterval() { return connectCheckInterval; }
    public int getConnectTimeout() { return connectTimeout; }
    public Map<String, String> getServers() { return servers; }
    public String getServerId(String name) { return servers.get(name.toLowerCase()); }
}
