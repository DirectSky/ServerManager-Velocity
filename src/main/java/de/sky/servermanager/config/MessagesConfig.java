package de.sky.servermanager.config;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessagesConfig {

    private final Path dataDirectory;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Display mode
    private boolean useBossbar;

    // Bossbar settings
    private BossBar.Color bossbarColor;
    private BossBar.Overlay bossbarOverlay;
    private boolean animatedProgress;

    // Raw message strings (MiniMessage format)
    private String countdownText;
    private String noPermission;
    private String serverNotFound;
    private String noPermissionServer;
    private String startSending;
    private String startSent;
    private String startOnline;
    private String startTimeout;
    private String startNotRegistered;
    private String stopSending;
    private String stopSent;
    private String stopError;
    private String restartNow;
    private String restartScheduled;
    private String restartAllNow;
    private String restartAllScheduled;
    private String invalidTime;

    public MessagesConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        load();
    }

    public void load() {
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);

            Path file = dataDirectory.resolve("messages.yml");
            if (!Files.exists(file)) {
                try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
                    if (in != null) Files.copy(in, file);
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file).build();
            ConfigurationNode root = loader.load();

            useBossbar = root.node("countdown-display").getString("bossbar").equalsIgnoreCase("bossbar");


            String colorStr = root.node("bossbar", "color").getString("RED");
            try {
                bossbarColor = BossBar.Color.valueOf(colorStr.toUpperCase());
            } catch (Exception e) {
                logger.warn("Ungültige Bossbar-Farbe '{}', nutze RED.", colorStr);
                bossbarColor = BossBar.Color.RED;
            }

            String overlayStr = root.node("bossbar", "overlay").getString("PROGRESS");
            try {
                bossbarOverlay = BossBar.Overlay.valueOf(overlayStr.toUpperCase());
            } catch (Exception e) {
                logger.warn("Ungültiger Bossbar-Stil '{}', nutze PROGRESS.", overlayStr);
                bossbarOverlay = BossBar.Overlay.PROGRESS;
            }

            animatedProgress = root.node("bossbar", "animated-progress").getBoolean(true);

            countdownText      = root.node("countdown", "text").getString("<red>⚠ ꜱᴇʀᴠᴇʀ ɴᴇᴜꜱᴛᴀʀᴛ ɪɴ: <yellow>{time}");
            noPermission       = root.node("commands", "no-permission").getString("<red>✗ Du hast keine Berechtigung dafür.");
            serverNotFound     = root.node("commands", "server-not-found").getString("<red>✗ Server '{server}' nicht gefunden.");
            noPermissionServer = root.node("commands", "no-permission-server").getString("<red>✗ Du hast keine Berechtigung für diesen Server.");
            startSending       = root.node("commands", "start-sending").getString("<yellow>⏳ Starte Server '{server}'...");
            startSent          = root.node("commands", "start-sent").getString("<green>✓ Startbefehl gesendet!");
            startOnline        = root.node("commands", "start-online").getString("<green>✓ Server ist online! Verbinde...");
            startTimeout       = root.node("commands", "start-timeout").getString("<red>✗ Timeout: Server '{server}' ist nach {time}s nicht online.");
            startNotRegistered = root.node("commands", "start-not-registered").getString("<red>✗ Server nicht in velocity.toml registriert.");
            stopSending        = root.node("commands", "stop-sending").getString("<yellow>⏳ Stoppe Server '{server}'...");
            stopSent           = root.node("commands", "stop-sent").getString("<green>✓ Server '{server}' wird gestoppt.");
            stopError          = root.node("commands", "stop-error").getString("<red>✗ Fehler beim Stoppen. Prüfe die Logs.");
            restartNow         = root.node("commands", "restart-now").getString("<red>⚠ Server '{server}' wird jetzt neu gestartet.");
            restartScheduled   = root.node("commands", "restart-scheduled").getString("<yellow>⚠ Server '{server}' wird in {time} Minute(n) neu gestartet.");
            restartAllNow      = root.node("commands", "restartall-now").getString("<red>⚠ Alle Server werden jetzt neu gestartet!");
            restartAllScheduled= root.node("commands", "restartall-scheduled").getString("<yellow>⚠ Alle Server werden in {time} Minute(n) neu gestartet.");
            invalidTime        = root.node("commands", "invalid-time").getString("<red>✗ Ungültige Zeit. Nutze eine Zahl (Minuten) oder 'now'.");

        } catch (IOException e) {
            logger.error("Fehler beim Laden der messages.yml!", e);
        }
    }


    public Component parse(String raw, TagResolver... resolvers) {
        return mm.deserialize(raw, resolvers);
    }

    public Component countdown(String time) {
        return parse(countdownText, Placeholder.unparsed("time", time));
    }

    public Component noPermission()       { return parse(noPermission); }
    public Component noPermissionServer() { return parse(noPermissionServer); }
    public Component invalidTime()        { return parse(invalidTime); }
    public Component restartAllNow()      { return parse(restartAllNow); }
    public Component stopError()          { return parse(stopError); }
    public Component startSent()          { return parse(startSent); }
    public Component startOnline()        { return parse(startOnline); }
    public Component startNotRegistered() { return parse(startNotRegistered); }

    public Component serverNotFound(String server) {
        return parse(serverNotFound, Placeholder.unparsed("server", server));
    }

    public Component startSending(String server) {
        return parse(startSending, Placeholder.unparsed("server", server));
    }

    public Component startTimeout(String server, int seconds) {
        return parse(startTimeout,
                Placeholder.unparsed("server", server),
                Placeholder.unparsed("time", String.valueOf(seconds)));
    }

    public Component stopSending(String server) {
        return parse(stopSending, Placeholder.unparsed("server", server));
    }

    public Component stopSent(String server) {
        return parse(stopSent, Placeholder.unparsed("server", server));
    }

    public Component restartNow(String server) {
        return parse(restartNow, Placeholder.unparsed("server", server));
    }

    public Component restartScheduled(String server, String time) {
        return parse(restartScheduled,
                Placeholder.unparsed("server", server),
                Placeholder.unparsed("time", time));
    }

    public Component restartAllScheduled(String time) {
        return parse(restartAllScheduled, Placeholder.unparsed("time", time));
    }

    public boolean isUseBossbar()          { return useBossbar; }
    public BossBar.Color getBossbarColor() { return bossbarColor; }
    public BossBar.Overlay getBossbarOverlay() { return bossbarOverlay; }
    public boolean isAnimatedProgress()    { return animatedProgress; }
}