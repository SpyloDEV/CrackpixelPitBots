package crackpixel.pitbots.menu;

import crackpixel.pitbots.CrackpixelPitBotsPlugin;
import crackpixel.pitbots.bot.PitBot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PitBotsMenuListener implements Listener {

    private static final String TITLE = ChatColor.DARK_GREEN + "PitBots Menu";

    private final CrackpixelPitBotsPlugin plugin;

    public PitBotsMenuListener(CrackpixelPitBotsPlugin plugin) {
        this.plugin = plugin;
    }

    public static void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(10, item(Material.NETHER_STAR, ChatColor.GREEN + "Set Center", ChatColor.GRAY + "Use your current position."));
        inventory.setItem(11, item(Material.COMPASS, ChatColor.YELLOW + "Set Arena Pos 1", ChatColor.GRAY + "Use your current position."));
        inventory.setItem(12, item(Material.COMPASS, ChatColor.YELLOW + "Set Arena Pos 2", ChatColor.GRAY + "Use your current position."));
        inventory.setItem(13, item(Material.IRON_SWORD, ChatColor.AQUA + "Spawn Bot", ChatColor.GRAY + "Spawn one extra test bot."));
        inventory.setItem(14, item(Material.PAPER, ChatColor.GOLD + "Debug", ChatColor.GRAY + "Print runtime status."));
        inventory.setItem(15, item(Material.EMERALD, ChatColor.GREEN + "Reload", ChatColor.GRAY + "Reload config and refresh bots."));
        inventory.setItem(16, item(Material.REDSTONE, ChatColor.RED + "Clear Bots", ChatColor.GRAY + "Remove every active bot."));
        inventory.setItem(21, item(Material.REDSTONE_COMPARATOR, ChatColor.YELLOW + "Toggle Scaling", ChatColor.GRAY + "Enable or disable automatic bot scaling."));
        inventory.setItem(22, item(Material.BARRIER, ChatColor.RED + "Clear Bounds", ChatColor.GRAY + "Use radius spawning again."));
        inventory.setItem(23, item(Material.DIAMOND, ChatColor.AQUA + "Top Farmers", ChatColor.GRAY + "Show top bot kill stats."));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("pitbots.admin")) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You do not have permission to use PitBots.");
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        handleClick(player, event.getRawSlot());
    }

    private void handleClick(Player player, int slot) {
        if (slot == 10) {
            plugin.getBotSpawnService().setCenterLocation(player.getLocation());
            plugin.saveCenterLocation();
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "PitBots center set.");
            return;
        }

        if (slot == 11) {
            setBoundsPosition(player, true);
            return;
        }

        if (slot == 12) {
            setBoundsPosition(player, false);
            return;
        }

        if (slot == 13) {
            spawnBot(player);
            return;
        }

        if (slot == 14) {
            player.closeInventory();
            player.performCommand("pitbots debug");
            return;
        }

        if (slot == 15) {
            plugin.reloadPitBotsConfig();
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "PitBots config reloaded.");
            return;
        }

        if (slot == 16) {
            clearBots(player);
            return;
        }

        if (slot == 21) {
            boolean enabled = !plugin.getBotScalingService().isEnabled();
            plugin.setScalingEnabled(enabled);
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "PitBots scaling is now " +
                    (enabled ? ChatColor.YELLOW + "enabled" : ChatColor.RED + "disabled") +
                    ChatColor.GREEN + ".");
            return;
        }

        if (slot == 22) {
            plugin.getBotSpawnService().clearBounds();
            plugin.saveArenaBounds();
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "PitBots arena bounds cleared.");
            return;
        }

        if (slot == 23) {
            player.closeInventory();
            player.performCommand("pitbots top");
        }
    }

    private void setBoundsPosition(Player player, boolean firstPosition) {
        if (!plugin.getBotSpawnService().hasCenterLocation()) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Set the center first.");
            return;
        }

        if (!plugin.getBotSpawnService().isInPitWorld(player.getLocation())) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Bounds must be in the same world as the center.");
            return;
        }

        if (firstPosition) {
            plugin.getBotSpawnService().setCornerOne(player.getLocation());
        } else {
            plugin.getBotSpawnService().setCornerTwo(player.getLocation());
        }

        plugin.saveArenaBounds();
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Arena position " + (firstPosition ? "1" : "2") + " set.");
    }

    private void spawnBot(Player player) {
        if (!plugin.getBotSpawnService().hasCenterLocation()) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Set the center first.");
            return;
        }

        PitBot bot = plugin.getBotManager().createBot(
                plugin.getBotSpawnService().createTestSpawnLocation(player.getLocation()),
                true
        );
        bot.setSkin(plugin.getBotSettings().getRandomSkin());
        plugin.getBotProfileService().configureBot(bot);
        plugin.getBotNameTagService().registerBot(bot);
        plugin.getBotPacketService().spawnBotForAll(bot);

        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Spawned bot " + ChatColor.YELLOW + bot.getName() + ChatColor.GREEN + ".");
    }

    private void clearBots(Player player) {
        int removed = plugin.getBotManager().getBotCount();

        for (PitBot bot : plugin.getBotManager().getBotsSnapshot()) {
            plugin.getBotPacketService().destroyBotForAll(bot);
            plugin.getBotNameTagService().unregisterBot(bot);
        }

        plugin.getBotManager().clearBots();
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.YELLOW + removed + ChatColor.GREEN + " bots.");
    }

    private static ItemStack item(Material material, String name, String lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(Arrays.asList(lore));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
