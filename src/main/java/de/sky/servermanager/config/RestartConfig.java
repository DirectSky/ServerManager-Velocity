package de.sky.servermanager.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class RestartConfig {

    private final Path dataDirectory;
    private final Logger logger;

    private ZoneId timezone;
    private final Map<String, List<RestartEntry>> restartTimes = new HashMap<>();

    public record RestartEntry(LocalTime time, Set<DayOfWeek> days) {
        public boolean isEveryDay() {
            return days.size() == 7;
        }
    }

    public RestartConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        load();
    }

    public void load() {
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);

            Path file = dataDirectory.resolve("restarttimes.yml");
            if (!Files.exists(file)) {
                try (InputStream in = getClass().getResourceAsStream("/restarttimes.yml")) {
                    if (in != null) Files.copy(in, file);
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file).build();
            ConfigurationNode root = loader.load();

            String tz = root.node("timezone").getString("Europe/Berlin");
            try {
                this.timezone = ZoneId.of(tz);
            } catch (Exception e) {
                logger.warn("Ungültige Zeitzone '{}', nutze Europe/Berlin als Fallback.", tz);
                this.timezone = ZoneId.of("Europe/Berlin");
            }

            restartTimes.clear();
            ConfigurationNode timesNode = root.node("restarttimes");
            if (!timesNode.isNull()) {
                timesNode.childrenMap().forEach((key, serverNode) -> {
                    String serverName = key.toString().toLowerCase();
                    List<RestartEntry> entries = new ArrayList<>();

                    serverNode.childrenList().forEach(entryNode -> {
                        String timeStr = entryNode.node("time").getString("");
                        String daysStr = entryNode.node("days").getString("*");

                        try {
                            LocalTime time = LocalTime.parse(timeStr);
                            Set<DayOfWeek> days = parseDays(daysStr, serverName);
                            entries.add(new RestartEntry(time, days));
                        } catch (Exception e) {
                            logger.warn("Falscher Eintrag für Server '{}': time='{}' days='{}'",
                                    serverName, timeStr, daysStr);
                        }
                    });

                    restartTimes.put(serverName, entries);
                });
            }

        } catch (IOException e) {
            logger.error("Fehler beim Laden von restarttimes.yml", e);
        }
    }

    private Set<DayOfWeek> parseDays(String daysStr, String serverName) {
        if (daysStr.trim().equals("*")) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        Set<DayOfWeek> days = new HashSet<>();
        for (String part : daysStr.split(",")) {
            switch (part.trim().toLowerCase()) {
                case "mo" -> days.add(DayOfWeek.MONDAY);
                case "di" -> days.add(DayOfWeek.TUESDAY);
                case "mi" -> days.add(DayOfWeek.WEDNESDAY);
                case "do" -> days.add(DayOfWeek.THURSDAY);
                case "fr" -> days.add(DayOfWeek.FRIDAY);
                case "sa" -> days.add(DayOfWeek.SATURDAY);
                case "so" -> days.add(DayOfWeek.SUNDAY);
                default -> logger.warn("Unbekannter Wochentag '{}' für Server '{}'", part.trim(), serverName);
            }
        }
        return days;
    }

    public ZoneId getTimezone() { return timezone; }
    public Map<String, List<RestartEntry>> getRestartTimes() { return restartTimes; }
}