package de.sky.servermanager.restart;

import com.velocitypowered.api.proxy.ProxyServer;
import de.sky.servermanager.api.PterodactylAPI;
import de.sky.servermanager.config.MessagesConfig;
import de.sky.servermanager.config.PluginConfig;
import de.sky.servermanager.config.RestartConfig;
import de.sky.servermanager.config.RestartConfig.RestartEntry;
import org.slf4j.Logger;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RestartManager {

    private final PluginConfig config;
    private final RestartConfig restartConfig;
    private final PterodactylAPI api;
    private final Logger logger;
    private final DisplayHelper display;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public RestartManager(ProxyServer proxy, PluginConfig config, RestartConfig restartConfig,
                          PterodactylAPI api, MessagesConfig messages, Logger logger) {
        this.config = config;
        this.restartConfig = restartConfig;
        this.api = api;
        this.logger = logger;
        this.display = new DisplayHelper(proxy, messages);
    }

    public void start() {
        for (Map.Entry<String, List<RestartEntry>> entry : restartConfig.getRestartTimes().entrySet()) {
            String serverName = entry.getKey();
            if (!config.getServers().containsKey(serverName)) {
                logger.warn("restarttimes.yml: Server '{}' nicht in config.yml gefunden, wird übersprungen.", serverName);
                continue;
            }
            for (RestartEntry restartEntry : entry.getValue()) {
                scheduleNextRestart(serverName, restartEntry);
            }
        }
    }

    private void scheduleNextRestart(String serverName, RestartEntry entry) {
        ZonedDateTime now = ZonedDateTime.now(restartConfig.getTimezone());

        ZonedDateTime next = findNextOccurrence(now, entry);
        long secondsUntil = ChronoUnit.SECONDS.between(now, next);

        String daysInfo = entry.isEveryDay() ? "täglich" : entry.days().toString();
        logger.info("Restart für '{}' geplant: {} um {} (in {} Sekunden)",
                serverName, daysInfo, entry.time(), secondsUntil);

        long countdownDelay = secondsUntil - (5 * 60L);
        if (countdownDelay > 0) {
            long totalCountdown = 5 * 60L;
            scheduler.schedule(() -> startCountdown(serverName, next, totalCountdown),
                    countdownDelay, TimeUnit.SECONDS);
        } else {
            startCountdown(serverName, next, secondsUntil);
        }

        scheduler.schedule(() -> {
            doRestart(serverName);
            scheduler.schedule(() -> scheduleNextRestart(serverName, entry), 30, TimeUnit.SECONDS);
        }, secondsUntil, TimeUnit.SECONDS);
    }

    private ZonedDateTime findNextOccurrence(ZonedDateTime now, RestartEntry entry) {
        ZonedDateTime candidate = now.toLocalDate().atTime(entry.time()).atZone(restartConfig.getTimezone());

        for (int i = 0; i <= 7; i++) {
            DayOfWeek candidateDay = candidate.getDayOfWeek();
            if (entry.days().contains(candidateDay) && candidate.isAfter(now)) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }

        return candidate;
    }

    private void startCountdown(String serverName, ZonedDateTime restartTime, long totalSeconds) {
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            ZonedDateTime now = ZonedDateTime.now(restartConfig.getTimezone());
            long secondsLeft = ChronoUnit.SECONDS.between(now, restartTime);

            if (secondsLeft <= 0) {
                display.clearCountdown(serverName);
                ScheduledFuture<?> f = futureRef.get();
                if (f != null) f.cancel(false);
                return;
            }

            long mins = secondsLeft / 60;
            long secs = secondsLeft % 60;
            display.sendCountdown(serverName, String.format("%02d:%02d", mins, secs), secondsLeft, totalSeconds);

        }, 0, 1, TimeUnit.SECONDS);

        futureRef.set(future);
    }

    public void doRestart(String serverName) {
        display.clearCountdown(serverName);
        logger.info("[{}] Starte Restart...", serverName);
        scheduler.schedule(() -> {
            String serverId = config.getServerId(serverName);
            if (serverId != null) {
                api.sendPowerAction(serverId, "restart");
                logger.info("[{}] Restart-Befehl an Pterodactyl gesendet.", serverName);
            }
        }, 3, TimeUnit.SECONDS);
    }

    public DisplayHelper getDisplay() { return display; }
    public ScheduledExecutorService getScheduler() { return scheduler; }
}