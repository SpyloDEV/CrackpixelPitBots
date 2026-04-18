package crackpixel.pitbots.pitcore;

import crackpixel.pitbots.bot.PitBot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PitCoreBridgeService {

    private static final double BASE_GOLD = 10.0D;
    private static final int BASE_XP = 5;

    private final Plugin plugin;
    private final DecimalFormat goldFormat = new DecimalFormat("#,##0.00");

    private boolean available;
    private String unavailableReason = "Pit plugin not found";

    private Method getPlayerMethod;
    private Method removePlayerMethod;
    private Method updateStatusMethod;
    private Method runCombatTimeMethod;
    private Method setLastAttackerMethod;
    private Method addGoldMethod;
    private Method addXpMethod;
    private Method getKillsMethod;
    private Method setKillsMethod;
    private Method addStreakMethod;
    private Method setLastKillMethod;
    private Method displayIndicatorMethod;
    private Method applyHealthMethod;
    private Method deathModelSetDropChanceMethod;
    private Method pitRespawnSetSpawnLocationMethod;

    private Field equippedPerksField;
    private Field equippedKillstreaksField;
    private Field renownPerksField;
    private Field passivesField;
    private Field pitMegastreakField;
    private Field megaActiveField;
    private Field streakField;
    private Field bountyField;
    private Field combatTimeField;
    private Field multiKillsNumberField;
    private Field prestigeField;
    private Field levelField;

    private Constructor<?> deathModelConstructor;
    private Constructor<?> pitJoinEventConstructor;
    private Constructor<?> pitKillEventConstructor;
    private Constructor<?> pitRespawnEventConstructor;
    private Object respawnReasonDeath;
    private Object respawnReasonJoin;
    private Object overdriveMegastreak;

    public PitCoreBridgeService(Plugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    public boolean isAvailable() {
        return available;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public void markPlayerCombat(Player player) {
        Object model = getPlayerModel(player);
        if (model == null) {
            return;
        }

        invoke(model, updateStatusMethod);
        invoke(model, runCombatTimeMethod);
    }

    public void markBotDamagedByPlayer(Player attacker) {
        markPlayerCombat(attacker);
    }

    public void clearTrackedPlayer(Player player) {
        if (!available || player == null || removePlayerMethod == null) {
            return;
        }

        invokeStatic(removePlayerMethod, player);
    }

    public void markPlayerDamagedByBot(Player victim) {
        Object model = getPlayerModel(victim);
        if (model == null) {
            return;
        }

        invoke(model, setLastAttackerMethod, new Object[]{null});
        invoke(model, updateStatusMethod);
        invoke(model, runCombatTimeMethod);
    }

    public boolean syncBotHealthProfile(Player botPlayer, Player sourcePlayer, boolean heal) {
        if (!available || botPlayer == null || applyHealthMethod == null) {
            return false;
        }

        Object botModel = getPlayerModel(botPlayer);
        if (botModel == null) {
            return false;
        }

        Object sourceModel = sourcePlayer == null ? null : getPlayerModel(sourcePlayer);
        if (sourceModel != null) {
            copyHealthProfileState(sourceModel, botModel);
        } else {
            resetHealthProfileState(botModel);
        }

        normalizeBotCombatProfile(botModel);

        invokeStatic(applyHealthMethod, botPlayer, heal);
        invoke(botModel, updateStatusMethod);
        return true;
    }

    public void showBotDamageIndicator(Player attacker, PitBot bot, double previousHealth, boolean killed) {
        if (!available || attacker == null || bot == null || displayIndicatorMethod == null) {
            return;
        }

        String message = killed
                ? ChatColor.GRAY + bot.getName() + " " + ChatColor.GREEN + ChatColor.BOLD + "KILL!"
                : buildDamageIndicator(bot, previousHealth, bot.getHealth());
        if (message.isEmpty()) {
            return;
        }

        invokeStatic(displayIndicatorMethod, attacker, message);
    }

    public boolean handleBotKill(Player killer, PitBot bot) {
        Object model = getPlayerModel(killer);
        if (model == null || killer == null || bot == null) {
            return false;
        }

        Integer kills = invokeInt(model, getKillsMethod);
        if (kills != null) {
            invoke(model, setKillsMethod, kills + 1);
        }

        invoke(model, addStreakMethod, 1.0D);
        invoke(model, setLastKillMethod, System.currentTimeMillis());
        invoke(model, addXpMethod, BASE_XP);
        invoke(model, addGoldMethod, BASE_GOLD);

        showBotDamageIndicator(killer, bot, bot.getMaxHealth(), true);
        killer.sendMessage(
                ChatColor.GREEN.toString() + ChatColor.BOLD + "KILL! "
                        + ChatColor.GRAY + "on "
                        + ChatColor.GREEN + bot.getName()
                        + " "
                        + ChatColor.AQUA + "+" + BASE_XP + "XP "
                        + ChatColor.GOLD + "+" + goldFormat.format(BASE_GOLD) + "g"
        );
        return true;
    }

    public boolean handleBotKillEvent(Player killer, Player victim) {
        if (!available || killer == null || victim == null || pitKillEventConstructor == null) {
            return false;
        }

        Object deathModel = createDeathModel(victim, killer, killer);
        if (deathModel == null) {
            return false;
        }

        Object event = instantiate(pitKillEventConstructor, deathModel);
        return callEvent(event);
    }

    public boolean handleBotRespawnEvent(Player player, Location spawnLocation) {
        if (!available || player == null || pitRespawnEventConstructor == null || respawnReasonDeath == null) {
            return false;
        }

        Object event = instantiate(pitRespawnEventConstructor, player, respawnReasonDeath);
        if (event == null) {
            return false;
        }

        if (spawnLocation != null) {
            invoke(event, pitRespawnSetSpawnLocationMethod, spawnLocation.clone());
        }

        return callEvent(event);
    }

    public boolean handleBotJoinLifecycle(Player player, Location spawnLocation) {
        if (!available || player == null || pitRespawnEventConstructor == null || respawnReasonJoin == null) {
            return false;
        }

        boolean called = false;
        if (pitJoinEventConstructor != null) {
            Object joinEvent = instantiate(pitJoinEventConstructor, player);
            called = callEvent(joinEvent) || called;
        }

        Object respawnEvent = instantiate(pitRespawnEventConstructor, player, respawnReasonJoin);
        if (respawnEvent == null) {
            return called;
        }

        if (spawnLocation != null) {
            invoke(respawnEvent, pitRespawnSetSpawnLocationMethod, spawnLocation.clone());
        }

        return callEvent(respawnEvent) || called;
    }

    private void initialize() {
        available = false;

        Plugin pitPlugin = Bukkit.getPluginManager().getPlugin("Pit");
        if (pitPlugin == null || !pitPlugin.isEnabled()) {
            unavailableReason = "Pit plugin is not loaded";
            return;
        }

        try {
            ClassLoader classLoader = pitPlugin.getClass().getClassLoader();
            Class<?> playerServiceClass = Class.forName("pit.sandbox.service.PlayerService", true, classLoader);
            Class<?> playerModelClass = Class.forName("pit.sandbox.model.PlayerModel", true, classLoader);
            Class<?> playerUtilClass = Class.forName("pit.sandbox.util.PlayerUtil", true, classLoader);
            Class<?> deathModelClass = Class.forName("pit.sandbox.model.DeathModel", true, classLoader);
            Class<?> pitJoinEventClass = Class.forName("pit.sandbox.event.pit.PitJoinEvent", true, classLoader);
            Class<?> pitKillEventClass = Class.forName("pit.sandbox.event.pit.PitKillEvent", true, classLoader);
            Class<?> pitRespawnEventClass = Class.forName("pit.sandbox.event.pit.PitRespawnEvent", true, classLoader);
            Class<?> pitMegastreakClass = Class.forName("pit.sandbox.enums.PitMegastreak", true, classLoader);
            Class<?> respawnReasonClass = Class.forName("pit.sandbox.enums.RespawnReason", true, classLoader);

            getPlayerMethod = playerServiceClass.getMethod("getPlayer", Player.class);
            removePlayerMethod = playerServiceClass.getMethod("removePlayer", Player.class);
            updateStatusMethod = playerModelClass.getMethod("updateStatus");
            runCombatTimeMethod = playerModelClass.getMethod("runCombatTime");
            setLastAttackerMethod = playerModelClass.getMethod("setLastAttacker", Player.class);
            addGoldMethod = playerModelClass.getMethod("addGold", double.class);
            addXpMethod = playerModelClass.getMethod("addXP", int.class);
            getKillsMethod = playerModelClass.getMethod("getKills");
            setKillsMethod = playerModelClass.getMethod("setKills", int.class);
            addStreakMethod = playerModelClass.getMethod("addStreak", double.class);
            setLastKillMethod = playerModelClass.getMethod("setLastKill", long.class);
            displayIndicatorMethod = playerUtilClass.getMethod("displayIndicator", Player.class, String.class);
            applyHealthMethod = playerUtilClass.getMethod("applyHealth", Player.class, boolean.class);
            deathModelSetDropChanceMethod = deathModelClass.getMethod("setDropChance", double.class);
            pitRespawnSetSpawnLocationMethod = pitRespawnEventClass.getMethod("setSpawnLocation", Location.class);

            equippedPerksField = accessibleField(playerModelClass, "equippedPerks");
            equippedKillstreaksField = accessibleField(playerModelClass, "equippedKillstreaks");
            renownPerksField = accessibleField(playerModelClass, "renownPerks");
            passivesField = accessibleField(playerModelClass, "passives");
            pitMegastreakField = accessibleField(playerModelClass, "pitMegastreak");
            megaActiveField = accessibleField(playerModelClass, "megaActive");
            streakField = accessibleField(playerModelClass, "streak");
            bountyField = accessibleField(playerModelClass, "bounty");
            combatTimeField = accessibleField(playerModelClass, "combatTime");
            multiKillsNumberField = accessibleField(playerModelClass, "multiKillsNumber");
            prestigeField = accessibleField(playerModelClass, "prestige");
            levelField = accessibleField(playerModelClass, "level");

            deathModelConstructor = deathModelClass.getConstructor(Player.class, Entity.class, Player.class);
            pitJoinEventConstructor = pitJoinEventClass.getConstructor(Player.class);
            pitKillEventConstructor = pitKillEventClass.getConstructor(deathModelClass);
            pitRespawnEventConstructor = pitRespawnEventClass.getConstructor(Player.class, respawnReasonClass);

            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) respawnReasonClass.asSubclass(Enum.class);
            respawnReasonDeath = Enum.valueOf(enumClass, "DEATH");
            respawnReasonJoin = Enum.valueOf(enumClass, "JOIN");
            @SuppressWarnings("unchecked")
            Class<? extends Enum> megaClass = (Class<? extends Enum>) pitMegastreakClass.asSubclass(Enum.class);
            overdriveMegastreak = Enum.valueOf(megaClass, "OVERDRIVE");

            available = true;
            unavailableReason = "";
        } catch (Exception exception) {
            unavailableReason = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            plugin.getLogger().warning("PitCore bridge unavailable: " + unavailableReason);
        }
    }

    private Object getPlayerModel(Player player) {
        if (!available || player == null) {
            return null;
        }

        try {
            return getPlayerMethod.invoke(null, player);
        } catch (Exception exception) {
            return null;
        }
    }

    private void copyHealthProfileState(Object sourceModel, Object botModel) {
        copyFieldValue(sourceModel, botModel, equippedPerksField);
        copyFieldValue(sourceModel, botModel, renownPerksField);
        copyFieldValue(sourceModel, botModel, passivesField);
        copyFieldValue(sourceModel, botModel, prestigeField);
        copyFieldValue(sourceModel, botModel, levelField);
    }

    private void resetHealthProfileState(Object botModel) {
        setFieldValue(botModel, equippedPerksField, new HashMap<Object, Object>());
        setFieldValue(botModel, equippedKillstreaksField, new HashMap<Object, Object>());
        setFieldValue(botModel, renownPerksField, new HashMap<Object, Object>());
        setFieldValue(botModel, passivesField, new HashMap<Object, Object>());
        setFieldValue(botModel, pitMegastreakField, overdriveMegastreak);
        setFieldValue(botModel, prestigeField, 0);
        setFieldValue(botModel, levelField, 1);
        normalizeBotCombatProfile(botModel);
    }

    private void normalizeBotCombatProfile(Object botModel) {
        setFieldValue(botModel, equippedKillstreaksField, new HashMap<Object, Object>());
        setFieldValue(botModel, pitMegastreakField, overdriveMegastreak);
        setFieldValue(botModel, megaActiveField, false);
        setFieldValue(botModel, streakField, 0.0D);
        setFieldValue(botModel, bountyField, 0);
        setFieldValue(botModel, combatTimeField, 0);
        setFieldValue(botModel, multiKillsNumberField, 1);
        invoke(botModel, setKillsMethod, 0);
        invoke(botModel, setLastAttackerMethod, new Object[]{null});
        invoke(botModel, setLastKillMethod, System.currentTimeMillis() - 5000L);
    }

    private void copyFieldValue(Object source, Object target, Field field) {
        if (source == null || target == null || field == null) {
            return;
        }

        try {
            Object value = field.get(source);
            if (value instanceof Map<?, ?>) {
                value = new HashMap<Object, Object>((Map<?, ?>) value);
            } else if (value instanceof java.util.Set<?>) {
                value = new HashSet<Object>((java.util.Set<?>) value);
            } else if (value instanceof java.util.List<?>) {
                value = new java.util.ArrayList<Object>((java.util.List<?>) value);
            }

            field.set(target, value);
        } catch (Exception ignored) {
        }
    }

    private void setFieldValue(Object target, Field field, Object value) {
        if (target == null || field == null) {
            return;
        }

        try {
            field.set(target, value);
        } catch (Exception ignored) {
        }
    }

    private Field accessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private Integer invokeInt(Object instance, Method method) {
        if (instance == null || method == null) {
            return null;
        }

        try {
            Object value = method.invoke(instance);
            return value instanceof Number ? ((Number) value).intValue() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private void invoke(Object instance, Method method, Object... args) {
        if (instance == null || method == null) {
            return;
        }

        try {
            method.invoke(instance, args);
        } catch (Exception ignored) {
        }
    }

    private void invokeStatic(Method method, Object... args) {
        if (method == null) {
            return;
        }

        try {
            method.invoke(null, args);
        } catch (Exception ignored) {
        }
    }

    private Object createDeathModel(Player victim, Entity attacker, Player trueAttacker) {
        Object deathModel = instantiate(deathModelConstructor, victim, attacker, trueAttacker);
        if (deathModel == null) {
            return null;
        }

        invoke(deathModel, deathModelSetDropChanceMethod, 0.0D);
        return deathModel;
    }

    private Object instantiate(Constructor<?> constructor, Object... args) {
        if (constructor == null) {
            return null;
        }

        try {
            return constructor.newInstance(args);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean callEvent(Object event) {
        if (!(event instanceof Event)) {
            return false;
        }

        try {
            Bukkit.getPluginManager().callEvent((Event) event);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private String buildDamageIndicator(PitBot bot, double previousHealth, double currentHealth) {
        if (bot == null) {
            return "";
        }

        int max = Math.max(1, hearts(bot.getMaxHealth()));
        int before = Math.max(0, Math.min(max, hearts(previousHealth)));
        int after = Math.max(0, Math.min(max, hearts(currentHealth)));
        int taken = Math.max(0, before - after);
        int empty = Math.max(0, max - before);

        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GRAY).append(bot.getName()).append(" ");
        appendHearts(builder, ChatColor.DARK_RED, after);
        appendHearts(builder, ChatColor.RED, taken);
        appendHearts(builder, ChatColor.BLACK, empty);
        return builder.toString();
    }

    private int hearts(double health) {
        return (int) Math.ceil(Math.max(0.0D, health) / 2.0D);
    }

    private void appendHearts(StringBuilder builder, ChatColor color, int count) {
        if (count <= 0) {
            return;
        }

        builder.append(color);
        for (int index = 0; index < count; index++) {
            builder.append("\u2764");
        }
    }
}
