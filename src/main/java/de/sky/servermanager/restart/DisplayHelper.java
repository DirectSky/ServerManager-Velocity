package de.sky.servermanager.restart;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.sky.servermanager.config.MessagesConfig;
import net.kyori.adventure.bossbar.BossBar;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayHelper {

    private final ProxyServer proxy;
    private final MessagesConfig messages;


    private final Map<String, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public DisplayHelper(ProxyServer proxy, MessagesConfig messages) {
        this.proxy = proxy;
        this.messages = messages;
    }

    public void sendCountdown(String serverName, String timeStr, long secondsLeft, long totalSeconds) {
        Collection<Player> players = getPlayers(serverName);
        if (players.isEmpty()) {
            removeBossBar(serverName);
            return;
        }

        if (messages.isUseBossbar()) {
            sendBossbar(serverName, players, timeStr, secondsLeft, totalSeconds);
        } else {
            sendActionbar(serverName, players, timeStr);
        }
    }


    public void clearCountdown(String serverName) {
        removeBossBar(serverName);
    }

    private void sendActionbar(String serverName, Collection<Player> players, String timeStr) {
        var text = messages.countdown(timeStr);
        players.forEach(p -> p.sendActionBar(text));
    }

    private void sendBossbar(String serverName, Collection<Player> players,
                             String timeStr, long secondsLeft, long totalSeconds) {
        BossBar bar = activeBossBars.computeIfAbsent(serverName, k ->
                BossBar.bossBar(
                        messages.countdown(timeStr),
                        1.0f,
                        messages.getBossbarColor(),
                        messages.getBossbarOverlay()
                )
        );

        bar.name(messages.countdown(timeStr));

        if (messages.isAnimatedProgress() && totalSeconds > 0) {
            float progress = Math.max(0f, Math.min(1f, (float) secondsLeft / totalSeconds));
            bar.progress(progress);
        }

        players.forEach(p -> p.showBossBar(bar));

        activeBossBars.forEach((sName, b) -> {
            proxy.getAllPlayers().forEach(p -> {
                boolean onThisServer = p.getCurrentServer()
                        .map(s -> s.getServerInfo().getName().equalsIgnoreCase(sName))
                        .orElse(false);
                if (!onThisServer) p.hideBossBar(b);
            });
        });
    }

    private void removeBossBar(String serverName) {
        BossBar bar = activeBossBars.remove(serverName);
        if (bar == null) return;
        proxy.getAllPlayers().forEach(p -> p.hideBossBar(bar));
    }

    @SuppressWarnings("unchecked")
    private Collection<Player> getPlayers(String serverName) {
        Optional<RegisteredServer> optServer = proxy.getServer(serverName);
        if (optServer.isEmpty()) return Collections.emptyList();
        return (Collection<Player>) (Collection<?>) optServer.get().getPlayersConnected();
    }
}