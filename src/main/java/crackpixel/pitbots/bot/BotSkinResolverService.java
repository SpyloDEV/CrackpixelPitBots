package crackpixel.pitbots.bot;

import crackpixel.pitbots.packet.BotPacketService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotSkinResolverService {

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String PROFILE_URL_FALLBACK = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String CACHE_FILE_NAME = "skins-cache.yml";

    private final Plugin plugin;
    private final BotSettings botSettings;
    private final BotManager botManager;
    private final BotPacketService botPacketService;

    public BotSkinResolverService(Plugin plugin,
                                  BotSettings botSettings,
                                  BotManager botManager,
                                  BotPacketService botPacketService) {
        this.plugin = plugin;
        this.botSettings = botSettings;
        this.botManager = botManager;
        this.botPacketService = botPacketService;
    }

    public void resolveConfiguredSkinsAsync() {
        final List<String> usernames = botSettings.getSkinUsernames();
        if (usernames.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Map<String, BotSkin> cache = loadCachedSkins();
                Map<String, BotSkin> resolvedByUsername = new LinkedHashMap<String, BotSkin>();
                int cachedCount = 0;

                for (String username : usernames) {
                    String key = normalizeUsername(username);
                    BotSkin cachedSkin = cache.get(key);
                    if (cachedSkin != null && cachedSkin.isValid()) {
                        resolvedByUsername.put(key, cachedSkin);
                        cachedCount++;
                    }
                }

                int resolvedNow = 0;
                for (String username : usernames) {
                    String key = normalizeUsername(username);
                    if (resolvedByUsername.containsKey(key)) {
                        continue;
                    }

                    BotSkin skin = resolveSkin(username);
                    if (skin == null || !skin.isValid()) {
                        continue;
                    }

                    resolvedByUsername.put(key, skin);
                    cache.put(key, skin);
                    resolvedNow++;
                }

                if (resolvedByUsername.isEmpty()) {
                    plugin.getLogger().warning("No signed skins could be resolved or loaded from cache. Bots will keep default skins.");
                    return;
                }

                saveCachedSkins(cache);
                final List<BotSkin> resolvedSkins = new ArrayList<BotSkin>(resolvedByUsername.values());
                final int loadedFromCache = cachedCount;
                final int freshlyResolved = resolvedNow;

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        botSettings.addResolvedSkins(resolvedSkins);
                        refreshExistingBots();
                        plugin.getLogger().info(
                                "Loaded "
                                        + resolvedSkins.size()
                                        + " bot skins ("
                                        + loadedFromCache
                                        + " cached, "
                                        + freshlyResolved
                                        + " fresh)."
                        );
                    }
                });
            }
        });
    }

    private void refreshExistingBots() {
        for (PitBot bot : botManager.getBotsSnapshot()) {
            if (bot.getSkin() == null) {
                bot.setSkin(botSettings.getRandomSkin());
            }
            botPacketService.destroyBotForAll(bot);
            botPacketService.spawnBotForAll(bot);
        }
    }

    private BotSkin resolveSkin(String username) {
        try {
            String uuid = resolveUuid(username);
            if (uuid == null || uuid.trim().isEmpty()) {
                plugin.getLogger().warning("Could not resolve Mojang profile for skin name: " + username);
                return null;
            }

            String skinJson = get(SESSION_URL + uuid + "?unsigned=false");
            String value = jsonString(skinJson, "value");
            String signature = jsonString(skinJson, "signature");

            if (value == null || signature == null) {
                plugin.getLogger().warning("Mojang profile has no signed skin texture: " + username);
                return null;
            }

            return new BotSkin(value, signature);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not load skin for " + username + ": " + ex.getMessage());
            return null;
        }
    }

    private String resolveUuid(String username) throws Exception {
        Exception lastException = null;
        String[] urls = new String[]{
                PROFILE_URL_FALLBACK + username,
                PROFILE_URL + username
        };

        for (String url : urls) {
            try {
                String profileJson = get(url);
                String uuid = jsonString(profileJson, "id");
                if (uuid != null && !uuid.trim().isEmpty()) {
                    return uuid;
                }
            } catch (Exception exception) {
                lastException = exception;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        return null;
    }

    private Map<String, BotSkin> loadCachedSkins() {
        Map<String, BotSkin> cache = new LinkedHashMap<String, BotSkin>();
        File cacheFile = getCacheFile();
        if (!cacheFile.isFile()) {
            return cache;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cacheFile);
        for (String key : yaml.getKeys(false)) {
            String value = yaml.getString(key + ".value");
            String signature = yaml.getString(key + ".signature");
            BotSkin skin = new BotSkin(value, signature);
            if (skin.isValid()) {
                cache.put(normalizeUsername(key), skin);
            }
        }

        return cache;
    }

    private void saveCachedSkins(Map<String, BotSkin> cache) {
        if (cache == null || cache.isEmpty()) {
            return;
        }

        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                return;
            }

            File cacheFile = getCacheFile();
            YamlConfiguration yaml = new YamlConfiguration();

            for (Map.Entry<String, BotSkin> entry : cache.entrySet()) {
                BotSkin skin = entry.getValue();
                if (skin == null || !skin.isValid()) {
                    continue;
                }

                yaml.set(entry.getKey() + ".value", skin.getValue());
                yaml.set(entry.getKey() + ".signature", skin.getSignature());
            }

            yaml.save(cacheFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save skin cache: " + exception.getMessage());
        }
    }

    private File getCacheFile() {
        return new File(plugin.getDataFolder(), CACHE_FILE_NAME);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String get(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(2500);
        connection.setReadTimeout(3500);
        connection.setRequestProperty("User-Agent", "CrackpixelPitBots");

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        );

        try {
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            return builder.toString();
        } finally {
            reader.close();
            connection.disconnect();
        }
    }

    private String jsonString(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}
