package crackpixel.pitbots;

import crackpixel.pitbots.bot.BotFeedbackService;
import crackpixel.pitbots.bot.BotKillService;
import crackpixel.pitbots.bot.BotManager;
import crackpixel.pitbots.bot.BotNameTagService;
import crackpixel.pitbots.bot.BotProfileService;
import crackpixel.pitbots.bot.BotScalingService;
import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.BotSkinResolverService;
import crackpixel.pitbots.bot.BotSpawnService;
import crackpixel.pitbots.bot.BotStatsService;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.command.PitBotsCommand;
import crackpixel.pitbots.command.PitBotsTabCompleter;
import crackpixel.pitbots.listener.BotEntityListener;
import crackpixel.pitbots.listener.BotViewerListener;
import crackpixel.pitbots.menu.PitBotsMenuListener;
import crackpixel.pitbots.nms.BotEntityService;
import crackpixel.pitbots.packet.BotPacketService;
import crackpixel.pitbots.performance.PerformanceMonitorService;
import crackpixel.pitbots.pitcore.PitCoreBridgeService;
import crackpixel.pitbots.task.BotTickTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CrackpixelPitBotsPlugin extends JavaPlugin {

    private static CrackpixelPitBotsPlugin instance;

    private BotManager botManager;
    private BotSettings botSettings;
    private BotNameTagService botNameTagService;
    private BotProfileService botProfileService;
    private BotSpawnService botSpawnService;
    private BotScalingService botScalingService;
    private BotEntityService botEntityService;
    private BotPacketService botPacketService;
    private BotSkinResolverService botSkinResolverService;
    private BotStatsService botStatsService;
    private BotKillService botKillService;
    private BotFeedbackService botFeedbackService;
    private PerformanceMonitorService performanceMonitorService;
    private PitCoreBridgeService pitCoreBridgeService;
    private BotTickTask botTickTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfigWithDefaults();

        this.botSettings = new BotSettings();
        this.botSettings.load(getConfig());
        this.botManager = new BotManager(botSettings);
        this.botNameTagService = new BotNameTagService();
        this.botSpawnService = new BotSpawnService();
        this.botSpawnService.loadCenter(getConfig());
        this.botProfileService = new BotProfileService(botSettings, botSpawnService);
        this.botScalingService = new BotScalingService();
        this.botScalingService.load(getConfig());
        this.botStatsService = new BotStatsService(this);
        this.botStatsService.load();
        this.pitCoreBridgeService = new PitCoreBridgeService(this);
        this.botEntityService = new BotEntityService(this, botSettings, pitCoreBridgeService);
        this.botKillService = new BotKillService(this, pitCoreBridgeService);
        this.botFeedbackService = new BotFeedbackService(botSettings);
        this.performanceMonitorService = new PerformanceMonitorService();
        this.performanceMonitorService.load(getConfig());
        this.botPacketService = new BotPacketService(this, botSettings, botEntityService);
        this.botSkinResolverService = new BotSkinResolverService(this, botSettings, botManager, botPacketService);
        this.botTickTask = new BotTickTask(
                botManager,
                botSettings,
                botNameTagService,
                botProfileService,
                botSpawnService,
                botScalingService,
                botEntityService,
                botPacketService,
                performanceMonitorService
        );

        if (getCommand("pitbots") != null) {
            getCommand("pitbots").setExecutor(
                    new PitBotsCommand(
                            this,
                            botManager,
                            botSpawnService,
                            botScalingService,
                            botStatsService,
                            botPacketService,
                            botProfileService,
                            pitCoreBridgeService
                    )
            );
            getCommand("pitbots").setTabCompleter(new PitBotsTabCompleter());
        }

        getServer().getPluginManager().registerEvents(
                new BotViewerListener(this, botManager, botNameTagService, botPacketService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new BotEntityListener(
                        this,
                        botSettings,
                        botStatsService,
                        botKillService,
                        botNameTagService,
                        botPacketService,
                        botEntityService,
                        pitCoreBridgeService,
                        botProfileService,
                        botSpawnService
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PitBotsMenuListener(this),
                this
        );

        botTickTask.runTaskTimer(this, 20L, 2L);
        performanceMonitorService.runTaskTimer(this, 20L, 20L);
        botSkinResolverService.resolveConfiguredSkinsAsync();

        getLogger().info("CrackpixelPitBots enabled.");
    }

    @Override
    public void onDisable() {
        if (botTickTask != null) {
            botTickTask.cancel();
        }

        if (performanceMonitorService != null) {
            performanceMonitorService.cancel();
        }

        if (botPacketService != null && botManager != null) {
            for (PitBot bot : botManager.getBotsSnapshot()) {
                botPacketService.destroyBotForAll(bot);
            }
            if (botNameTagService != null) {
                botNameTagService.clear(botManager.getBotsSnapshot());
            }

            for (Player player : getServer().getOnlinePlayers()) {
                botPacketService.removeAllBotsFromTab(player, botManager.getBotsSnapshot());
            }
        }

        if (botManager != null) {
            botManager.clearBots();
        }

        if (botEntityService != null) {
            botEntityService.clearAll();
        }

        if (botStatsService != null) {
            botStatsService.save();
        }

        getLogger().info("CrackpixelPitBots disabled.");
    }

    public void saveCenterLocation() {
        botSpawnService.saveCenter(getConfig());
        saveConfig();
    }

    public void saveArenaBounds() {
        botSpawnService.saveBounds(getConfig());
        saveConfig();
    }

    public void setScalingEnabled(boolean enabled) {
        botScalingService.setEnabled(enabled);
        getConfig().set("bot-scaling.enabled", enabled);
        saveConfig();
    }

    public void reloadPitBotsConfig() {
        reloadConfigWithDefaults();
        botSettings.load(getConfig());
        botSpawnService.loadCenter(getConfig());
        botScalingService.load(getConfig());
        performanceMonitorService.load(getConfig());
        botSkinResolverService.resolveConfiguredSkinsAsync();

        for (PitBot bot : botManager.getBotsSnapshot()) {
            botProfileService.configureBot(bot);
            botNameTagService.updateHealth(bot);
            botPacketService.destroyBotForAll(bot);
            botPacketService.spawnBotForAll(bot);
        }
    }

    private void reloadConfigWithDefaults() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public static CrackpixelPitBotsPlugin getInstance() {
        return instance;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public BotSettings getBotSettings() {
        return botSettings;
    }

    public BotNameTagService getBotNameTagService() {
        return botNameTagService;
    }

    public BotSpawnService getBotSpawnService() {
        return botSpawnService;
    }

    public BotProfileService getBotProfileService() {
        return botProfileService;
    }

    public BotScalingService getBotScalingService() {
        return botScalingService;
    }

    public BotPacketService getBotPacketService() {
        return botPacketService;
    }

    public BotStatsService getBotStatsService() {
        return botStatsService;
    }

    public BotKillService getBotKillService() {
        return botKillService;
    }

    public BotFeedbackService getBotFeedbackService() {
        return botFeedbackService;
    }

    public PerformanceMonitorService getPerformanceMonitorService() {
        return performanceMonitorService;
    }

    public PitCoreBridgeService getPitCoreBridgeService() {
        return pitCoreBridgeService;
    }
}
