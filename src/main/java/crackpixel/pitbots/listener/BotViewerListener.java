package crackpixel.pitbots.listener;

import crackpixel.pitbots.bot.BotManager;
import crackpixel.pitbots.bot.BotNameTagService;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.packet.BotPacketService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class BotViewerListener implements Listener {

    private final Plugin plugin;
    private final BotManager botManager;
    private final BotNameTagService botNameTagService;
    private final BotPacketService botPacketService;

    public BotViewerListener(Plugin plugin, BotManager botManager, BotNameTagService botNameTagService, BotPacketService botPacketService) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.botNameTagService = botNameTagService;
        this.botPacketService = botPacketService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        spawnBotsDelayed(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        botPacketService.hideAllBotsFrom(player, botManager.getBotsSnapshot());

        spawnBotsDelayed(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        botPacketService.hideAllBotsFrom(event.getPlayer(), botManager.getBotsSnapshot());
    }

    private void spawnBotsDelayed(final Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    return;
                }

                for (PitBot bot : botManager.getBotsSnapshot()) {
                    botNameTagService.registerBot(bot);
                    botPacketService.spawnBotFor(player, bot);
                }
            }
        }, 20L);
    }
}
