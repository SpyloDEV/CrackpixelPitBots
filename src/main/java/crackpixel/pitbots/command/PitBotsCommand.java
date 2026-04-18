package crackpixel.pitbots.command;

import com.github.retrooper.packetevents.PacketEvents;
import crackpixel.pitbots.CrackpixelPitBotsPlugin;
import crackpixel.pitbots.bot.BotManager;
import crackpixel.pitbots.bot.BotProfileService;
import crackpixel.pitbots.bot.BotScalingService;
import crackpixel.pitbots.bot.BotSpawnService;
import crackpixel.pitbots.bot.BotStatsService;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.menu.PitBotsMenuListener;
import crackpixel.pitbots.packet.BotPacketService;
import crackpixel.pitbots.pitcore.PitCoreBridgeService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PitBotsCommand implements CommandExecutor {

    private final CrackpixelPitBotsPlugin plugin;
    private final BotManager botManager;
    private final BotSpawnService botSpawnService;
    private final BotScalingService botScalingService;
    private final BotStatsService botStatsService;
    private final BotPacketService botPacketService;
    private final BotProfileService botProfileService;
    private final PitCoreBridgeService pitCoreBridgeService;

    public PitBotsCommand(CrackpixelPitBotsPlugin plugin,
                          BotManager botManager,
                          BotSpawnService botSpawnService,
                          BotScalingService botScalingService,
                          BotStatsService botStatsService,
                          BotPacketService botPacketService,
                          BotProfileService botProfileService,
                          PitCoreBridgeService pitCoreBridgeService) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.botSpawnService = botSpawnService;
        this.botScalingService = botScalingService;
        this.botStatsService = botStatsService;
        this.botPacketService = botPacketService;
        this.botProfileService = botProfileService;
        this.pitCoreBridgeService = pitCoreBridgeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pitbots.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use PitBots.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("menu")) {
            openMenu(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("setcenter")) {
            setCenter(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("pos1")) {
            setBoundsPosition(sender, true);
            return true;
        }

        if (args[0].equalsIgnoreCase("pos2")) {
            setBoundsPosition(sender, false);
            return true;
        }

        if (args[0].equalsIgnoreCase("clearbounds")) {
            clearBounds(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            spawnOne(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("count")) {
            int totalBots = botManager.getBotCount();
            int automaticBots = botManager.getAutomaticBotCount();
            int manualBots = Math.max(0, totalBots - automaticBots);
            sender.sendMessage(ChatColor.GREEN + "Current bot count: " + ChatColor.YELLOW + totalBots
                    + ChatColor.GRAY + " (" + ChatColor.AQUA + automaticBots + " auto"
                    + ChatColor.GRAY + ", " + ChatColor.GOLD + manualBots + " manual" + ChatColor.GRAY + ")");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            sendDebug(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            toggleScaling(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            sendStats(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            sendTop(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            clearBots(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPitBotsConfig();
            sender.sendMessage(ChatColor.GREEN + "PitBots config reloaded.");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void setCenter(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;
        botSpawnService.setCenterLocation(player.getLocation());
        plugin.saveCenterLocation();

        sender.sendMessage(ChatColor.GREEN + "PitBots center set. Bots will now spawn in the middle and spread out.");
    }

    private void openMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        PitBotsMenuListener.open((Player) sender);
    }

    private void spawnOne(CommandSender sender, String[] args) {
        if (!botSpawnService.hasCenterLocation()) {
            sender.sendMessage(ChatColor.RED + "Set the center first with /pitbots setcenter.");
            return;
        }

        String name = args.length >= 2 ? args[1] : null;
        org.bukkit.Location spawnLocation = sender instanceof Player
                ? botSpawnService.createTestSpawnLocation(((Player) sender).getLocation())
                : botSpawnService.createSpawnLocation();
        PitBot bot = name == null
                ? botManager.createBot(spawnLocation, true)
                : botManager.createBot(name, spawnLocation, true);
        bot.setSkin(plugin.getBotSettings().getRandomSkin());

        botProfileService.configureBot(bot);
        plugin.getBotNameTagService().registerBot(bot);
        botPacketService.spawnBotForAll(bot);
        sender.sendMessage(ChatColor.GREEN + "Created bot " + ChatColor.YELLOW + bot.getName() + ChatColor.GREEN + ".");
    }

    private void sendInfo(CommandSender sender) {
        int realPlayers = countPlayersInPitWorld();
        int targetBots = botScalingService.getTargetBotCount(realPlayers);
        int totalBots = botManager.getBotCount();
        int automaticBots = botManager.getAutomaticBotCount();
        int manualBots = Math.max(0, totalBots - automaticBots);

        sender.sendMessage(ChatColor.GOLD + "PitBots Info");
        sender.sendMessage(ChatColor.YELLOW + "Center: " +
                (botSpawnService.hasCenterLocation() ? ChatColor.GREEN + "set" : ChatColor.RED + "not set"));
        sender.sendMessage(ChatColor.YELLOW + "Players in pit world: " + ChatColor.GREEN + realPlayers);
        sender.sendMessage(ChatColor.YELLOW + "Target bots: " + ChatColor.GREEN + targetBots);
        sender.sendMessage(ChatColor.YELLOW + "Current bots: " + ChatColor.GREEN + totalBots
                + ChatColor.GRAY + " (" + ChatColor.AQUA + automaticBots + " auto"
                + ChatColor.GRAY + ", " + ChatColor.GOLD + manualBots + " manual" + ChatColor.GRAY + ")");
        sender.sendMessage(ChatColor.YELLOW + "Scaling: " +
                (botScalingService.isEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Max bot cap: " + ChatColor.GREEN + botScalingService.getAbsoluteMaxBots());
        sender.sendMessage(ChatColor.YELLOW + "Arena bounds: " + ChatColor.GREEN + botSpawnService.getBoundsSummary());
        sender.sendMessage(ChatColor.YELLOW + "Tracked players: " + ChatColor.GREEN + botStatsService.getTrackedPlayers());
        sender.sendMessage(ChatColor.YELLOW + "Total bot kills: " + ChatColor.GREEN + botStatsService.getTotalKills());
    }

    private void sendDebug(CommandSender sender) {
        int realPlayers = countPlayersInPitWorld();
        int targetBots = botScalingService.getTargetBotCount(realPlayers);
        int totalBots = botManager.getBotCount();
        int automaticBots = botManager.getAutomaticBotCount();
        int manualBots = Math.max(0, totalBots - automaticBots);

        sender.sendMessage(ChatColor.GOLD + "PitBots Debug");
        sender.sendMessage(ChatColor.YELLOW + "Plugin: " + ChatColor.GREEN + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Engine: " + ChatColor.GREEN + "real entity players (NMS)");
        sender.sendMessage(ChatColor.YELLOW + "PacketEvents API: " +
                (PacketEvents.getAPI() != null ? ChatColor.GREEN + "available" : ChatColor.RED + "missing"));
        sender.sendMessage(ChatColor.YELLOW + "PitCore bridge: " +
                (pitCoreBridgeService != null && pitCoreBridgeService.isAvailable()
                        ? ChatColor.GREEN + "available"
                        : ChatColor.RED + "missing"));
        sender.sendMessage(ChatColor.YELLOW + "Center: " +
                (botSpawnService.hasCenterLocation() ? ChatColor.GREEN + formatLocation(botSpawnService.getCenterLocation()) : ChatColor.RED + "not set"));
        sender.sendMessage(ChatColor.YELLOW + "Players in pit world: " + ChatColor.GREEN + realPlayers);
        sender.sendMessage(ChatColor.YELLOW + "Current bots: " + ChatColor.GREEN + totalBots);
        sender.sendMessage(ChatColor.YELLOW + "Automatic bots: " + ChatColor.AQUA + automaticBots);
        sender.sendMessage(ChatColor.YELLOW + "Manual test bots: " + ChatColor.GOLD + manualBots);
        sender.sendMessage(ChatColor.YELLOW + "Target auto bots: " + ChatColor.GREEN + targetBots);
        sender.sendMessage(ChatColor.YELLOW + "Scaling: " +
                (botScalingService.isEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Minimum real players: " + ChatColor.GREEN + botScalingService.getMinimumRealPlayers());
        sender.sendMessage(ChatColor.YELLOW + "Aggro memory: " + ChatColor.GREEN + plugin.getBotSettings().getAggroMemoryMs() + "ms");
        sender.sendMessage(ChatColor.YELLOW + "Spawn attack delay: " + ChatColor.GREEN + plugin.getBotSettings().getSpawnAttackDelayMs() + "ms");
        sender.sendMessage(ChatColor.YELLOW + "Combat strafe: " +
                (plugin.getBotSettings().isCombatStrafeEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Hit reaction: " + ChatColor.GREEN + plugin.getBotSettings().getHitReactionMs() + "ms"
                + ChatColor.GRAY + " | "
                + ChatColor.YELLOW + "Hurt cooldown: "
                + ChatColor.GREEN + plugin.getBotSettings().getHurtCooldownMs() + "ms");
        sender.sendMessage(ChatColor.YELLOW + "Feedback: " +
                (plugin.getBotSettings().isFeedbackEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Packet view range: " +
                (plugin.getBotSettings().isPacketViewRangeEnabled()
                        ? ChatColor.GREEN + String.valueOf(plugin.getBotSettings().getPacketViewRange())
                        : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "TPS guard: " +
                (plugin.getPerformanceMonitorService().isEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") +
                ChatColor.GRAY + " | " +
                ChatColor.YELLOW + "TPS: " +
                ChatColor.GREEN + formatTps(plugin.getPerformanceMonitorService().getCurrentTps()) +
                ChatColor.GRAY + " | " +
                ChatColor.YELLOW + "Lag cap: " +
                ChatColor.GREEN + plugin.getPerformanceMonitorService().getLaggedBotPercent() + "%");
        sender.sendMessage(ChatColor.YELLOW + "Configured skin names: " + ChatColor.GREEN + plugin.getBotSettings().getSkinUsernames().size());
        sender.sendMessage(ChatColor.YELLOW + "Loaded signed skins: " + ChatColor.GREEN + plugin.getBotSettings().getLoadedSkinCount());
        sender.sendMessage(ChatColor.YELLOW + "Random name pool: " + ChatColor.GREEN + plugin.getBotSettings().getRandomNameCount());
        sender.sendMessage(ChatColor.YELLOW + "Mirror player loadout: " +
                (plugin.getBotSettings().isMirrorRandomPlayerLoadout() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Anti-stuck: " +
                (plugin.getBotSettings().isAntiStuckEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Ground fix: " +
                (plugin.getBotSettings().isGroundFixEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Safe step: " +
                (plugin.getBotSettings().isSafeStepEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Arena bounds: " + ChatColor.GREEN + botSpawnService.getBoundsSummary());
    }

    private void setBoundsPosition(CommandSender sender, boolean firstPosition) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;
        if (!botSpawnService.hasCenterLocation()) {
            sender.sendMessage(ChatColor.RED + "Set the center first with /pitbots setcenter.");
            return;
        }

        if (!botSpawnService.isInPitWorld(player.getLocation())) {
            sender.sendMessage(ChatColor.RED + "Bounds must be in the same world as the PitBots center.");
            return;
        }

        if (firstPosition) {
            botSpawnService.setCornerOne(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "PitBots arena position 1 set.");
        } else {
            botSpawnService.setCornerTwo(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "PitBots arena position 2 set.");
        }

        plugin.saveArenaBounds();

        if (botSpawnService.hasBounds()) {
            sender.sendMessage(ChatColor.GREEN + "Arena bounds active: " + ChatColor.YELLOW + botSpawnService.getBoundsSummary());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Set the other position to activate arena bounds.");
        }
    }

    private void clearBounds(CommandSender sender) {
        botSpawnService.clearBounds();
        plugin.saveArenaBounds();
        sender.sendMessage(ChatColor.GREEN + "PitBots arena bounds cleared. Radius spawning is active again.");
    }

    private void toggleScaling(CommandSender sender) {
        boolean enabled = !botScalingService.isEnabled();
        plugin.setScalingEnabled(enabled);

        sender.sendMessage(ChatColor.GREEN + "PitBots scaling is now " +
                (enabled ? ChatColor.YELLOW + "enabled" : ChatColor.RED + "disabled") +
                ChatColor.GREEN + ".");
    }

    private void sendStats(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            BotStatsService.PlayerStats playerStats = botStatsService.getByName(args[1]);
            if (playerStats == null) {
                sender.sendMessage(ChatColor.RED + "No PitBots stats found for " + args[1] + ".");
                return;
            }

            sender.sendMessage(ChatColor.GOLD + playerStats.getName() + ChatColor.YELLOW + " bot kills: " + ChatColor.GREEN + playerStats.getKills());
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Usage: /pitbots stats <player>");
            return;
        }

        Player player = (Player) sender;
        sender.sendMessage(ChatColor.YELLOW + "Your bot kills: " + ChatColor.GREEN + botStatsService.getKills(player.getUniqueId()));
    }

    private void sendTop(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "PitBots Top Kills");

        int position = 1;
        for (BotStatsService.PlayerStats playerStats : botStatsService.getTop(5)) {
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(position) + ". " +
                    ChatColor.GREEN + playerStats.getName() +
                    ChatColor.GRAY + " - " +
                    ChatColor.AQUA + playerStats.getKills());
            position++;
        }

        if (position == 1) {
            sender.sendMessage(ChatColor.GRAY + "No kills tracked yet.");
        }
    }

    private int countPlayersInPitWorld() {
        int count = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null
                    && player.isOnline()
                    && player.getGameMode() != GameMode.CREATIVE
                    && botSpawnService.isInPitWorld(player.getLocation())) {
                count++;
            }
        }

        return count;
    }

    private void clearBots(CommandSender sender) {
        int removed = botManager.getBotCount();

        for (PitBot bot : botManager.getBotsSnapshot()) {
            botPacketService.destroyBotForAll(bot);
            plugin.getBotNameTagService().unregisterBot(bot);
        }

        botManager.clearBots();
        sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.YELLOW + removed + ChatColor.GREEN + " bots.");
    }

    private String formatLocation(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return "not set";
        }

        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + ", "
                + location.getBlockY()
                + ", "
                + location.getBlockZ();
    }

    private String formatTps(double tps) {
        return String.valueOf(Math.round(tps * 10.0D) / 10.0D);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "PitBots Commands");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots menu" + ChatColor.GRAY + " - Open admin menu");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots setcenter" + ChatColor.GRAY + " - Set the middle of the pit");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots pos1" + ChatColor.GRAY + " - Set arena bounds position 1");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots pos2" + ChatColor.GRAY + " - Set arena bounds position 2");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots clearbounds" + ChatColor.GRAY + " - Clear arena bounds");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots info" + ChatColor.GRAY + " - Show scaling info");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots debug" + ChatColor.GRAY + " - Show runtime debug info");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots toggle" + ChatColor.GRAY + " - Toggle automatic scaling");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots stats [player]" + ChatColor.GRAY + " - Show bot kills");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots top" + ChatColor.GRAY + " - Show top bot farmers");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots count" + ChatColor.GRAY + " - Show current bot count");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots spawn [name]" + ChatColor.GRAY + " - Spawn one extra bot");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots clear" + ChatColor.GRAY + " - Remove all bots");
        sender.sendMessage(ChatColor.YELLOW + "/pitbots reload" + ChatColor.GRAY + " - Reload config/center");
    }
}
