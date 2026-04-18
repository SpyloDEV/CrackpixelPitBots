package crackpixel.pitbots.bot;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BotStatsService {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> stats = new LinkedHashMap<UUID, PlayerStats>();

    public BotStatsService(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        stats.clear();

        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String name = section.getString(key + ".name", "Unknown");
                int kills = Math.max(0, section.getInt(key + ".kills", 0));
                stats.put(uuid, new PlayerStats(uuid, name, kills));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();

        for (PlayerStats playerStats : stats.values()) {
            String path = "players." + playerStats.getUniqueId().toString();
            config.set(path + ".name", playerStats.getName());
            config.set(path + ".kills", playerStats.getKills());
        }

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save PitBots stats: " + ex.getMessage());
        }
    }

    public int recordKill(Player player) {
        PlayerStats playerStats = getOrCreate(player);
        playerStats.setKills(playerStats.getKills() + 1);
        playerStats.setName(player.getName());
        save();
        return playerStats.getKills();
    }

    public int getKills(UUID uniqueId) {
        PlayerStats playerStats = stats.get(uniqueId);
        return playerStats == null ? 0 : playerStats.getKills();
    }

    public PlayerStats getByName(String name) {
        if (name == null) {
            return null;
        }

        for (PlayerStats playerStats : stats.values()) {
            if (playerStats.getName().equalsIgnoreCase(name)) {
                return playerStats;
            }
        }

        return null;
    }

    public List<PlayerStats> getTop(int limit) {
        List<PlayerStats> top = new ArrayList<PlayerStats>(stats.values());
        Collections.sort(top, new Comparator<PlayerStats>() {
            @Override
            public int compare(PlayerStats first, PlayerStats second) {
                return Integer.compare(second.getKills(), first.getKills());
            }
        });

        if (top.size() <= limit) {
            return top;
        }

        return new ArrayList<PlayerStats>(top.subList(0, limit));
    }

    public int getTotalKills() {
        int total = 0;

        for (PlayerStats playerStats : stats.values()) {
            total += playerStats.getKills();
        }

        return total;
    }

    public int getTrackedPlayers() {
        return stats.size();
    }

    private PlayerStats getOrCreate(Player player) {
        PlayerStats playerStats = stats.get(player.getUniqueId());
        if (playerStats == null) {
            playerStats = new PlayerStats(player.getUniqueId(), player.getName(), 0);
            stats.put(player.getUniqueId(), playerStats);
        }

        return playerStats;
    }

    public static final class PlayerStats {

        private final UUID uniqueId;
        private String name;
        private int kills;

        private PlayerStats(UUID uniqueId, String name, int kills) {
            this.uniqueId = uniqueId;
            this.name = name == null ? "Unknown" : name;
            this.kills = kills;
        }

        public UUID getUniqueId() {
            return uniqueId;
        }

        public String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name == null ? "Unknown" : name;
        }

        public int getKills() {
            return kills;
        }

        private void setKills(int kills) {
            this.kills = kills;
        }
    }
}
