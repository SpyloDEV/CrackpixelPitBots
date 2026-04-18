package crackpixel.pitbots.bot;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BotManager {

    private final Map<UUID, PitBot> bots = new LinkedHashMap<UUID, PitBot>();
    private final BotSettings botSettings;
    private int nextEntityId = 100000;
    private int nextBotNumber = 1;

    public BotManager(BotSettings botSettings) {
        this.botSettings = botSettings;
    }

    public PitBot createBot(Location location) {
        return createBot(location, false);
    }

    public PitBot createBot(Location location, boolean manualSpawn) {
        String name = botSettings.createRandomName(nextBotNumber++);
        PitBot bot = createBot(name, location, manualSpawn);
        bot.setSkin(botSettings.getRandomSkin());
        return bot;
    }

    public PitBot createBot(String name, Location location) {
        return createBot(name, location, false);
    }

    public PitBot createBot(String name, Location location, boolean manualSpawn) {
        PitBot bot = new PitBot(UUID.randomUUID(), sanitizeName(name), location, generateEntityId());
        bot.setManualSpawn(manualSpawn);
        bots.put(bot.getUniqueId(), bot);
        return bot;
    }

    public void removeBot(UUID uniqueId) {
        bots.remove(uniqueId);
    }

    public PitBot removeLastBot() {
        PitBot lastBot = null;

        for (PitBot bot : bots.values()) {
            lastBot = bot;
        }

        if (lastBot != null) {
            bots.remove(lastBot.getUniqueId());
        }

        return lastBot;
    }

    public PitBot removeLastAutomaticBot() {
        PitBot lastBot = null;

        for (PitBot bot : bots.values()) {
            if (bot == null || bot.isManualSpawn()) {
                continue;
            }
            lastBot = bot;
        }

        if (lastBot != null) {
            bots.remove(lastBot.getUniqueId());
        }

        return lastBot;
    }

    public PitBot getBotByName(String name) {
        if (name == null) {
            return null;
        }

        for (PitBot bot : bots.values()) {
            if (bot.getName().equalsIgnoreCase(name)) {
                return bot;
            }
        }

        return null;
    }

    public PitBot getBotByEntityId(int entityId) {
        for (PitBot bot : bots.values()) {
            if (bot.getEntityId() == entityId) {
                return bot;
            }
        }

        return null;
    }

    public void clearBots() {
        bots.clear();
    }

    public Collection<PitBot> getBots() {
        return Collections.unmodifiableCollection(bots.values());
    }

    public List<PitBot> getBotsSnapshot() {
        return new ArrayList<PitBot>(bots.values());
    }

    public int getBotCount() {
        return bots.size();
    }

    public int getAutomaticBotCount() {
        int count = 0;

        for (PitBot bot : bots.values()) {
            if (bot != null && !bot.isManualSpawn()) {
                count++;
            }
        }

        return count;
    }

    private int generateEntityId() {
        return nextEntityId++;
    }

    private String sanitizeName(String name) {
        String value = name == null || name.trim().isEmpty() ? "PitBot" : name.trim();

        if (value.length() > 16) {
            value = value.substring(0, 16);
        }

        return value;
    }
}
