package de.sky.servermanager;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.sky.servermanager.api.PterodactylAPI;
import de.sky.servermanager.commands.RestartCommand;
import de.sky.servermanager.commands.ServerCommand;
import de.sky.servermanager.config.MessagesConfig;
import de.sky.servermanager.config.PluginConfig;
import de.sky.servermanager.config.RestartConfig;
import de.sky.servermanager.restart.RestartManager;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "servermanager",
        name = "ServerManager",
        version = "2.0.0",
        description = "Velocity Server Manager",
        authors = {"DirectSky"}
)
public class ServerManagerPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private PluginConfig config;
    private RestartConfig restartConfig;
    private MessagesConfig messagesConfig;
    private PterodactylAPI pterodactylAPI;
    private RestartManager restartManager;

    @Inject
    public ServerManagerPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = new PluginConfig(dataDirectory, logger);
        this.restartConfig = new RestartConfig(dataDirectory, logger);
        this.messagesConfig = new MessagesConfig(dataDirectory, logger);
        this.pterodactylAPI = new PterodactylAPI(config, logger);

        CommandManager cm = server.getCommandManager();
        cm.register(cm.metaBuilder("startserver").plugin(this).build(),
                new ServerCommand(server, config, pterodactylAPI, messagesConfig, logger, "start"));
        cm.register(cm.metaBuilder("stopserver").plugin(this).build(),
                new ServerCommand(server, config, pterodactylAPI, messagesConfig, logger, "stop"));
        cm.register(cm.metaBuilder("restartserver").plugin(this).build(),
                new RestartCommand(server, config, pterodactylAPI, messagesConfig, logger, false));
        cm.register(cm.metaBuilder("restartallservers").plugin(this).build(),
                new RestartCommand(server, config, pterodactylAPI, messagesConfig, logger, true));

        this.restartManager = new RestartManager(server, config, restartConfig, pterodactylAPI, messagesConfig, logger);
        restartManager.start();

        logger.info("ServerManager v2.0.0 geladen! {} Server konfiguriert, {} Restart-Zeitpläne aktiv.",
                config.getServers().size(), restartConfig.getRestartTimes().size());
    }

    public ProxyServer getServer() { return server; }
    public PluginConfig getPluginConfig() { return config; }
    public RestartConfig getRestartConfig() { return restartConfig; }
    public MessagesConfig getMessagesConfig() { return messagesConfig; }
}