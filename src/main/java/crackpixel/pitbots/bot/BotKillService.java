package crackpixel.pitbots.bot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import crackpixel.pitbots.pitcore.PitCoreBridgeService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BotKillService {

    private final Plugin plugin;
    private final PitCoreBridgeService pitCoreBridgeService;
    private final Random random = new Random();
    private final Map<UUID, List<KillSample>> recentKillSamples = new HashMap<UUID, List<KillSample>>();

    public BotKillService(Plugin plugin, PitCoreBridgeService pitCoreBridgeService) {
        this.plugin = plugin;
        this.pitCoreBridgeService = pitCoreBridgeService;
    }

    public void handleBotKill(Player killer, PitBot bot) {
        if (killer == null || bot == null) {
            return;
        }

        if (pitCoreBridgeService != null && pitCoreBridgeService.handleBotKill(killer, bot)) {
            return;
        }

        int gold = randomInt("bot-rewards.gold-min", "bot-rewards.gold-max", 5, 10);
        int xp = randomInt("bot-rewards.xp-min", "bot-rewards.xp-max", 10, 20);
        RewardAdjustment adjustment = applyRewardGuard(killer, gold, xp);
        gold = adjustment.gold;
        xp = adjustment.xp;

        boolean rewarded = rewardViaPitApi(killer, gold, xp);
        rewarded = runConfiguredRewardCommands(killer, bot, gold, xp) || rewarded;

        int vanillaExp = plugin.getConfig().getInt("bot-rewards.vanilla-exp", 0);
        if (vanillaExp > 0) {
            killer.giveExp(vanillaExp);
            rewarded = true;
        }

        killer.sendMessage(
                ChatColor.GREEN + "Killed " + ChatColor.YELLOW + bot.getName() +
                        ChatColor.GRAY + " | " +
                        ChatColor.GOLD + "+" + gold + "g " +
                        ChatColor.AQUA + "+" + xp + "XP" +
                        (adjustment.reduced ? ChatColor.GRAY + " (reduced anti-farm rewards)" : "") +
                        (rewarded ? "" : ChatColor.GRAY + " (configure bot-rewards.commands for your Pit plugin)")
        );
    }

    private int randomInt(String minPath, String maxPath, int defaultMin, int defaultMax) {
        int min = plugin.getConfig().getInt(minPath, defaultMin);
        int max = plugin.getConfig().getInt(maxPath, defaultMax);

        if (max < min) {
            max = min;
        }

        return min + random.nextInt((max - min) + 1);
    }

    private boolean runConfiguredRewardCommands(Player killer, PitBot bot, int gold, int xp) {
        List<String> commands = plugin.getConfig().getStringList("bot-rewards.commands");
        if (commands == null || commands.isEmpty()) {
            return false;
        }

        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }

            String prepared = command
                    .replace("%player%", killer.getName())
                    .replace("%uuid%", killer.getUniqueId().toString())
                    .replace("%bot%", bot.getName())
                    .replace("%gold%", String.valueOf(gold))
                    .replace("%xp%", String.valueOf(xp));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
        }

        return true;
    }

    private boolean rewardViaPitApi(Player killer, int gold, int xp) {
        Plugin pitPlugin = findPitPlugin();
        if (pitPlugin == null) {
            return false;
        }

        try {
            Object playerService = firstNonNull(
                    invokeNoArgs(pitPlugin, "getPlayerService"),
                    invokeNoArgs(pitPlugin, "getProfileService"),
                    invokeNoArgs(pitPlugin, "getUserService")
            );

            if (playerService == null) {
                return false;
            }

            Object playerModel = findPlayerModel(playerService, killer);
            if (playerModel == null) {
                return false;
            }

            boolean goldRewarded = callFirstAmountMethod(
                    playerModel,
                    new String[]{"addGold", "addCoins", "addMoney", "giveGold", "giveCoins"},
                    gold
            );
            boolean xpRewarded = callFirstAmountMethod(
                    playerModel,
                    new String[]{"addXP", "addXp", "addExperience", "addExp", "giveXP", "giveXp"},
                    xp
            );

            return goldRewarded || xpRewarded;
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not reward Pit bot kill through the Pit API: " + ex.getMessage());
            return false;
        }
    }

    private RewardAdjustment applyRewardGuard(Player killer, int gold, int xp) {
        if (killer == null || !plugin.getConfig().getBoolean("bot-rewards.anti-farm-enabled", true)) {
            return new RewardAdjustment(gold, xp, false);
        }

        long now = System.currentTimeMillis();
        long windowMs = Math.max(1000L, plugin.getConfig().getLong("bot-rewards.abnormal-window-ms", 30000L));
        int maxKills = Math.max(1, plugin.getConfig().getInt("bot-rewards.abnormal-max-kills", 8));
        double maxRadius = Math.max(0.5D, plugin.getConfig().getDouble("bot-rewards.abnormal-max-radius", 4.5D));
        double rewardMultiplier = Math.max(0.0D, Math.min(1.0D, plugin.getConfig().getDouble("bot-rewards.abnormal-reward-multiplier", 0.25D)));

        List<KillSample> samples = recentKillSamples.get(killer.getUniqueId());
        if (samples == null) {
            samples = new ArrayList<KillSample>();
            recentKillSamples.put(killer.getUniqueId(), samples);
        }

        while (!samples.isEmpty() && (now - samples.get(0).timestamp) > windowMs) {
            samples.remove(0);
        }

        KillSample current = new KillSample(now, killer.getLocation());
        samples.add(current);

        int clusteredKills = 0;
        double radiusSquared = maxRadius * maxRadius;
        for (KillSample sample : samples) {
            if (sample.location == null
                    || current.location == null
                    || sample.location.getWorld() != current.location.getWorld()) {
                continue;
            }

            if (sample.location.distanceSquared(current.location) <= radiusSquared) {
                clusteredKills++;
            }
        }

        if (clusteredKills < maxKills) {
            return new RewardAdjustment(gold, xp, false);
        }

        return new RewardAdjustment(
                Math.max(0, (int) Math.round(gold * rewardMultiplier)),
                Math.max(0, (int) Math.round(xp * rewardMultiplier)),
                true
        );
    }

    private Plugin findPitPlugin() {
        String[] names = {"Pit", "CrackpixelPit", "CrackpixelCore"};

        for (String name : names) {
            Plugin found = Bukkit.getPluginManager().getPlugin(name);
            if (found != null && found.isEnabled() && found != plugin) {
                return found;
            }
        }

        return null;
    }

    private Object findPlayerModel(Object playerService, Player killer) {
        String[] methodNames = {"getPlayer", "getProfile", "getUser", "loadPlayer"};

        for (String methodName : methodNames) {
            Object byPlayer = invokeOneArg(playerService, methodName, killer);
            if (byPlayer != null) {
                return byPlayer;
            }

            Object byUuid = invokeOneArg(playerService, methodName, killer.getUniqueId());
            if (byUuid != null) {
                return byUuid;
            }

            Object byName = invokeOneArg(playerService, methodName, killer.getName());
            if (byName != null) {
                return byName;
            }
        }

        return null;
    }

    private Object invokeNoArgs(Object instance, String methodName) {
        try {
            Method method = instance.getClass().getMethod(methodName);
            return method.invoke(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeOneArg(Object instance, String methodName, Object arg) {
        Method[] methods = instance.getClass().getMethods();

        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || arg == null || !wrap(parameterTypes[0]).isAssignableFrom(arg.getClass())) {
                continue;
            }

            try {
                return method.invoke(instance, arg);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    private boolean callFirstAmountMethod(Object instance, String[] methodNames, int amount) {
        for (String methodName : methodNames) {
            if (callAmountMethod(instance, methodName, amount)) {
                return true;
            }
        }

        return false;
    }

    private boolean callAmountMethod(Object instance, String methodName, int amount) {
        Method[] methods = instance.getClass().getMethods();

        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }

            try {
                Class<?> parameter = wrap(parameterTypes[0]);
                if (parameter == Integer.class) {
                    method.invoke(instance, amount);
                    return true;
                }

                if (parameter == Double.class) {
                    method.invoke(instance, (double) amount);
                    return true;
                }

                if (parameter == Long.class) {
                    method.invoke(instance, (long) amount);
                    return true;
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not call reward method " + methodName + ": " + ex.getMessage());
                return false;
            }
        }

        return false;
    }

    private Object firstNonNull(Object first, Object second, Object third) {
        if (first != null) {
            return first;
        }

        if (second != null) {
            return second;
        }

        return third;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        if (type == int.class) {
            return Integer.class;
        }

        if (type == double.class) {
            return Double.class;
        }

        if (type == long.class) {
            return Long.class;
        }

        if (type == boolean.class) {
            return Boolean.class;
        }

        if (type == float.class) {
            return Float.class;
        }

        if (type == short.class) {
            return Short.class;
        }

        if (type == byte.class) {
            return Byte.class;
        }

        if (type == char.class) {
            return Character.class;
        }

        return type;
    }

    private static final class KillSample {

        private final long timestamp;
        private final org.bukkit.Location location;

        private KillSample(long timestamp, org.bukkit.Location location) {
            this.timestamp = timestamp;
            this.location = location == null ? null : location.clone();
        }
    }

    private static final class RewardAdjustment {

        private final int gold;
        private final int xp;
        private final boolean reduced;

        private RewardAdjustment(int gold, int xp, boolean reduced) {
            this.gold = gold;
            this.xp = xp;
            this.reduced = reduced;
        }
    }
}
