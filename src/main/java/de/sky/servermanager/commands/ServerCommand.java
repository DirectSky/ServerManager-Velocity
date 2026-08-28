package de.sky.servermanager.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.sky.servermanager.api.PterodactylAPI;
import de.sky.servermanager.config.MessagesConfig;
import de.sky.servermanager.config.PluginConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ServerCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final PluginConfig config;
    private final PterodactylAPI api;
    private final MessagesConfig messages;
    private final Logger logger;
    private final String action;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ServerCommand(ProxyServer proxy, PluginConfig config, PterodactylAPI api,
                         MessagesConfig messages, Logger logger, String action) {
        this.proxy = proxy;
        this.config = config;
        this.api = api;
        this.messages = messages;
        this.logger = logger;
        this.action = action;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("servermanager." + action) && !source.hasPermission("servermanager.*")) {
            source.sendMessage(messages.noPermission());
            return;
        }

        if (args.length == 0) {
            source.sendMessage(messages.parse("<yellow>Usage: /" + action + "server <name>"));
            source.sendMessage(messages.parse("<gray>Verfügbare Server: " + String.join(", ", config.getServers().keySet())));
            return;
        }

        String serverName = args[0].toLowerCase();
        String serverId = config.getServerId(serverName);

        if (serverId == null) {
            source.sendMessage(messages.serverNotFound(serverName));
            return;
        }

        if (!source.hasPermission("servermanager." + action + ".*")
                && !source.hasPermission("servermanager.*")
                && !source.hasPermission("servermanager." + action + "." + serverName)) {
            source.sendMessage(messages.noPermissionServer());
            return;
        }

        if (action.equals("start")) handleStart(source, serverName, serverId);
        else handleStop(source, serverName, serverId);
    }

    private void handleStart(CommandSource source, String serverName, String serverId) {
        source.sendMessage(messages.startSending(serverName));
        CompletableFuture.runAsync(() -> {
            boolean success = api.sendPowerAction(serverId, "start");
            if (!success) {
                source.sendMessage(messages.stopError());
                return;
            }
            source.sendMessage(messages.startSent());
            if (source instanceof Player player) waitAndConnect(player, serverName, serverId);
        });
    }

    private void handleStop(CommandSource source, String serverName, String serverId) {
        source.sendMessage(messages.stopSending(serverName));
        CompletableFuture.runAsync(() -> {
            boolean success = api.sendPowerAction(serverId, "stop");
            if (!success) {
                source.sendMessage(messages.stopError());
                return;
            }
            source.sendMessage(messages.stopSent(serverName));
        });
    }

    private void waitAndConnect(Player player, String serverName, String serverId) {
        int maxChecks = config.getConnectTimeout() / config.getConnectCheckInterval();
        AtomicInteger attempts = new AtomicInteger(0);

        scheduler.scheduleAtFixedRate(() -> {
            if (!player.isActive()) throw new RuntimeException("stop");

            if (attempts.incrementAndGet() > maxChecks) {
                player.sendMessage(messages.startTimeout(serverName, config.getConnectTimeout()));
                throw new RuntimeException("stop");
            }

            if ("running".equals(api.getServerState(serverId))) {
                Optional<RegisteredServer> reg = proxy.getServer(serverName);
                if (reg.isEmpty()) {
                    player.sendMessage(messages.startNotRegistered());
                    throw new RuntimeException("stop");
                }
                player.sendMessage(messages.startOnline());
                player.createConnectionRequest(reg.get()).fireAndForget();
                throw new RuntimeException("stop");
            }
        }, config.getConnectCheckInterval(), config.getConnectCheckInterval(), TimeUnit.SECONDS);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            String input = invocation.arguments().length == 0 ? "" : invocation.arguments()[0].toLowerCase();
            return config.getServers().keySet().stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("servermanager." + action)
                || invocation.source().hasPermission("servermanager.*");
    }
}