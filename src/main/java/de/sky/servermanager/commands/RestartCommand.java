package de.sky.servermanager.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.sky.servermanager.api.PterodactylAPI;
import de.sky.servermanager.config.MessagesConfig;
import de.sky.servermanager.config.PluginConfig;
import de.sky.servermanager.restart.DisplayHelper;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RestartCommand implements SimpleCommand {

    private final PluginConfig config;
    private final PterodactylAPI api;
    private final Logger logger;
    private final boolean allServers;
    private final DisplayHelper display;
    private final MessagesConfig messages;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public RestartCommand(ProxyServer proxy, PluginConfig config, PterodactylAPI api,
                          MessagesConfig messages, Logger logger, boolean allServers) {
        this.config = config;
        this.api = api;
        this.messages = messages;
        this.logger = logger;
        this.allServers = allServers;
        this.display = new DisplayHelper(proxy, messages);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        String permission = allServers ? "servermanager.restartall" : "servermanager.restart";
        if (!source.hasPermission(permission) && !source.hasPermission("servermanager.*")) {
            source.sendMessage(messages.noPermission());
            return;
        }

        if (allServers) handleRestartAll(source, args);
        else handleRestartSingle(source, args);
    }

    private void handleRestartSingle(CommandSource source, String[] args) {
        if (args.length == 0) {
            source.sendMessage(messages.parse("<yellow>Verwendung: /restartserver <server> [zeit|now]"));
            source.sendMessage(messages.parse("<gray>Zeit-Formate: now, 30s, 5m, 2h"));
            source.sendMessage(messages.parse("<gray>Verfügbare Server: " + String.join(", ", config.getServers().keySet())));
            return;
        }

        String serverName = args[0].toLowerCase();
        String serverId = config.getServerId(serverName);

        if (serverId == null) {
            source.sendMessage(messages.serverNotFound(serverName));
            return;
        }

        if (!source.hasPermission("servermanager.restart.*")
                && !source.hasPermission("servermanager.*")
                && !source.hasPermission("servermanager.restart." + serverName)) {
            source.sendMessage(messages.noPermissionServer());
            return;
        }

        long seconds = parseTime(args.length > 1 ? args[1] : "now");
        if (seconds < 0) {
            source.sendMessage(messages.invalidTime());
            return;
        }

        scheduleRestart(source, serverName, serverId, seconds);
    }

    private void handleRestartAll(CommandSource source, String[] args) {
        long seconds = parseTime(args.length > 0 ? args[0] : "now");
        if (seconds < 0) {
            source.sendMessage(messages.invalidTime());
            return;
        }

        source.sendMessage(seconds == 0 ? messages.restartAllNow() : messages.restartAllScheduled(formatTime(seconds)));

        int delay = 0;
        for (Map.Entry<String, String> entry : config.getServers().entrySet()) {
            final String serverName = entry.getKey();
            final String serverId = entry.getValue();
            final int startDelay = delay;
            scheduler.schedule(() -> scheduleRestart(source, serverName, serverId, seconds),
                    startDelay, TimeUnit.SECONDS);
            delay += 3;
        }
    }

    private void scheduleRestart(CommandSource source, String serverName, String serverId, long seconds) {
        if (seconds == 0) {
            source.sendMessage(messages.restartNow(serverName));
            scheduler.schedule(() -> {
                api.sendPowerAction(serverId, "restart");
                logger.info("[{}] Manueller Sofort-Restart ausgeführt.", serverName);
            }, 3, TimeUnit.SECONDS);
            return;
        }

        source.sendMessage(messages.restartScheduled(serverName, formatTime(seconds)));
        logger.info("[{}] Manueller Restart in {} Sekunden geplant.", serverName, seconds);

        long countdownStartDelay = Math.max(0, seconds - (5 * 60L));
        long countdownDuration = seconds - countdownStartDelay;

        scheduler.schedule(() -> startCountdown(serverName, countdownDuration, seconds),
                countdownStartDelay, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            display.clearCountdown(serverName);
            api.sendPowerAction(serverId, "restart");
            logger.info("[{}] Manueller Restart ausgeführt.", serverName);
        }, seconds, TimeUnit.SECONDS);
    }

    private void startCountdown(String serverName, long countdownSeconds, long totalSeconds) {
        long[] secondsLeft = {countdownSeconds};
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (secondsLeft[0] <= 0) {
                display.clearCountdown(serverName);
                ScheduledFuture<?> f = futureRef.get();
                if (f != null) f.cancel(false);
                return;
            }

            long mins = secondsLeft[0] / 60;
            long secs = secondsLeft[0] % 60;
            display.sendCountdown(serverName, String.format("%02d:%02d", mins, secs), secondsLeft[0], totalSeconds);
            secondsLeft[0]--;

        }, 0, 1, TimeUnit.SECONDS);

        futureRef.set(future);
    }


    private long parseTime(String input) {
        if (input.equalsIgnoreCase("now")) return 0;

        try {
            if (input.endsWith("s") || input.endsWith("S")) {
                long val = Long.parseLong(input.substring(0, input.length() - 1));
                return val >= 0 ? val : -1;
            } else if (input.endsWith("m") || input.endsWith("M")) {
                long val = Long.parseLong(input.substring(0, input.length() - 1));
                return val >= 0 ? val * 60 : -1;
            } else if (input.endsWith("h") || input.endsWith("H")) {
                long val = Long.parseLong(input.substring(0, input.length() - 1));
                return val >= 0 ? val * 3600 : -1;
            } else {
                long val = Long.parseLong(input);
                return val >= 0 ? val * 60 : -1;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) {
            long m = seconds / 60;
            long s = seconds % 60;
            return s > 0 ? m + "m " + s + "s" : m + "m";
        }
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder(h + "h");
        if (m > 0) sb.append(" ").append(m).append("m");
        if (s > 0) sb.append(" ").append(s).append("s");
        return sb.toString();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (!allServers && args.length <= 1) {
            String input = args.length == 0 ? "" : args[0].toLowerCase();
            return config.getServers().keySet().stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        if ((!allServers && args.length == 2) || (allServers && args.length <= 1)) {
            return List.of("now", "30s", "1m", "5m", "10m", "30m", "1h", "2h");
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String permission = allServers ? "servermanager.restartall" : "servermanager.restart";
        return invocation.source().hasPermission(permission)
                || invocation.source().hasPermission("servermanager.*");
    }
}