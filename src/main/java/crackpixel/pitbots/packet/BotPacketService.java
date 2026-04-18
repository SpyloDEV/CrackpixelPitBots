package crackpixel.pitbots.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.BotSkin;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.nms.BotEntityService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BotPacketService {

    private final Plugin plugin;
    private final BotSettings botSettings;
    private final BotEntityService botEntityService;
    private final Set<String> visibleBotViewers = new HashSet<String>();

    public BotPacketService(Plugin plugin, BotSettings botSettings, BotEntityService botEntityService) {
        this.plugin = plugin;
        this.botSettings = botSettings;
        this.botEntityService = botEntityService;
    }

    public void spawnBotForAll(PitBot bot) {
        if (bot == null) {
            return;
        }

        if (botEntityService != null) {
            botEntityService.ensureEntity(bot);
            botEntityService.spawnBot(bot);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            spawnBotFor(player, bot);
        }
    }

    public void spawnBotFor(Player viewer, PitBot bot) {
        if (viewer == null || bot == null || bot.getLocation() == null) {
            return;
        }

        org.bukkit.Location location = bot.getLocation();
        if (!canViewBot(viewer, bot)) {
            return;
        }

        if (!canSendPackets(viewer)) {
            return;
        }

        if (isBotVisibleTo(viewer, bot)) {
            return;
        }

        spawnFakePlayer(
                viewer,
                bot,
                bot.getUniqueId(),
                bot.getEntityId(),
                bot.getName(),
                bot.getSkin(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        visibleBotViewers.add(visibilityKey(viewer, bot));
    }

    public void updateBotFor(Player viewer, PitBot bot) {
        if (viewer == null || bot == null || bot.getLocation() == null) {
            return;
        }

        if (!canViewBot(viewer, bot)) {
            hideBotFrom(viewer, bot);
            return;
        }

        if (!isBotVisibleTo(viewer, bot)) {
            spawnBotFor(viewer, bot);
            return;
        }
    }

    public void hideBotFrom(Player viewer, PitBot bot) {
        if (viewer == null || bot == null) {
            return;
        }

        visibleBotViewers.remove(visibilityKey(viewer, bot));
        removeFakePlayerFromTab(viewer, bot.getUniqueId(), bot.getEntityId(), bot.getName());
    }

    public void hideAllBotsFrom(Player viewer, Collection<PitBot> bots) {
        if (viewer == null || bots == null) {
            return;
        }

        for (PitBot bot : bots) {
            hideBotFrom(viewer, bot);
        }
    }

    public void spawnFakePlayer(Player viewer, PitBot bot, UUID uuid, int entityId, String name, BotSkin skin, double x, double y, double z, float yaw, float pitch) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerPlayerInfo.PlayerData data = createPlayerData(uuid, name, skin);

        WrapperPlayServerPlayerInfo addPacket = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                Collections.singletonList(data)
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, addPacket);

        Location location = new Location(x, y, z, yaw, pitch);

        WrapperPlayServerSpawnPlayer spawnPacket = new WrapperPlayServerSpawnPlayer(
                entityId,
                uuid,
                location,
                Collections.<EntityData<?>>emptyList()
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawnPacket);
        sendStarterEquipment(viewer, entityId, bot);
        sendHeadLook(viewer, entityId, yaw);
        scheduleTabRemove(viewer, uuid, entityId, name);
    }

    public void removeFakePlayerFromTab(Player viewer, UUID uuid, int entityId, String name) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerPlayerInfo removePacket = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                Collections.singletonList(createPlayerData(uuid, name, null))
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, removePacket);
    }

    public void removeAllBotsFromTab(Player viewer, Collection<PitBot> bots) {
        if (viewer == null || bots == null) {
            return;
        }

        for (PitBot bot : bots) {
            removeFakePlayerFromTab(viewer, bot.getUniqueId(), bot.getEntityId(), bot.getName());
        }
    }

    public void moveFakePlayer(Player viewer, int entityId, double dx, double dy, double dz) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerEntityRelativeMove move =
                new WrapperPlayServerEntityRelativeMove(entityId, dx, dy, dz, true);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, move);
    }

    public void teleportFakePlayer(Player viewer, int entityId, double x, double y, double z, float yaw, float pitch) {
        if (!canSendPackets(viewer)) {
            return;
        }

        Vector3d position = new Vector3d(x, y, z);

        WrapperPlayServerEntityTeleport teleport =
                new WrapperPlayServerEntityTeleport(entityId, position, yaw, pitch, true);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleport);
        sendHeadLook(viewer, entityId, yaw);
    }

    public void swingArm(Player viewer, int entityId) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerEntityAnimation animation =
                new WrapperPlayServerEntityAnimation(entityId, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, animation);
    }

    public void damageEffect(Player viewer, int entityId) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerEntityAnimation hurtAnimation =
                new WrapperPlayServerEntityAnimation(entityId, WrapperPlayServerEntityAnimation.EntityAnimationType.HURT);
        WrapperPlayServerEntityStatus status =
                new WrapperPlayServerEntityStatus(entityId, 2);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, hurtAnimation);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, status);
    }

    public void swingArmForAll(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBotVisibleTo(player, bot)) {
                swingArm(player, bot.getEntityId());
            }
        }
    }

    public void damageEffectForAll(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBotVisibleTo(player, bot)) {
                damageEffect(player, bot.getEntityId());
            }
        }
    }

    public void destroyFakePlayer(Player viewer, int entityId) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerDestroyEntities destroy =
                new WrapperPlayServerDestroyEntities(entityId);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroy);
    }

    public void destroyBotForAll(PitBot bot) {
        if (bot == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            hideBotFrom(player, bot);
        }

        if (botEntityService != null) {
            botEntityService.despawnBot(bot);
        }
    }

    public void hideBotPacketsForAll(PitBot bot) {
        if (bot == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            hideBotFrom(player, bot);
        }
    }

    private void sendPlayerInfoAdd(Player viewer, UUID uuid, String name, BotSkin skin) {
        WrapperPlayServerPlayerInfo.PlayerData data = createPlayerData(uuid, name, skin);
        WrapperPlayServerPlayerInfo addPacket = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                Collections.singletonList(data)
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, addPacket);
    }

    public boolean canViewBot(Player viewer, PitBot bot) {
        if (viewer == null || bot == null || bot.getLocation() == null) {
            return false;
        }

        return canViewLocation(viewer, bot.getLocation());
    }

    public boolean canViewLocation(Player viewer, org.bukkit.Location location) {
        if (viewer == null || location == null || location.getWorld() == null || viewer.getWorld() != location.getWorld()) {
            return false;
        }

        if (!botSettings.isPacketViewRangeEnabled()) {
            return true;
        }

        double range = botSettings.getPacketViewRange();
        return viewer.getLocation().distanceSquared(location) <= range * range;
    }

    public boolean isBotVisibleTo(Player viewer, PitBot bot) {
        if (viewer == null || bot == null) {
            return false;
        }

        return visibleBotViewers.contains(visibilityKey(viewer, bot));
    }

    private void sendStarterEquipment(Player viewer, int entityId, PitBot bot) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerEntityEquipment equipment =
                new WrapperPlayServerEntityEquipment(entityId, createStarterEquipment(bot));

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, equipment);
    }

    private List<Equipment> createStarterEquipment(PitBot bot) {
        Player liveBotPlayer = botEntityService == null || bot == null ? null : botEntityService.getBukkitPlayer(bot);
        if (liveBotPlayer != null) {
            PlayerInventory inventory = liveBotPlayer.getInventory();
            if (inventory != null) {
                List<Equipment> liveEquipment = new ArrayList<Equipment>();
                liveEquipment.add(new Equipment(EquipmentSlot.MAIN_HAND, createItem(resolveLiveItemType(inventory.getItemInHand(), resolveItemType(bot == null ? null : bot.getMainHand(), botSettings.getMainHand())))));
                liveEquipment.add(new Equipment(EquipmentSlot.HELMET, createItem(resolveLiveItemType(inventory.getHelmet(), resolveItemType(bot == null ? null : bot.getHelmet(), botSettings.getHelmet())))));
                liveEquipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, createItem(resolveLiveItemType(inventory.getChestplate(), resolveItemType(bot == null ? null : bot.getChestPlate(), botSettings.getChestPlate())))));
                liveEquipment.add(new Equipment(EquipmentSlot.LEGGINGS, createItem(resolveLiveItemType(inventory.getLeggings(), resolveItemType(bot == null ? null : bot.getLeggings(), botSettings.getLeggings())))));
                liveEquipment.add(new Equipment(EquipmentSlot.BOOTS, createItem(resolveLiveItemType(inventory.getBoots(), resolveItemType(bot == null ? null : bot.getBoots(), botSettings.getBoots())))));
                return liveEquipment;
            }
        }

        List<Equipment> equipment = new ArrayList<Equipment>();
        equipment.add(new Equipment(EquipmentSlot.MAIN_HAND, createItem(resolveItemType(bot == null ? null : bot.getMainHand(), botSettings.getMainHand()))));
        equipment.add(new Equipment(EquipmentSlot.HELMET, createItem(resolveItemType(bot == null ? null : bot.getHelmet(), botSettings.getHelmet()))));
        equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, createItem(resolveItemType(bot == null ? null : bot.getChestPlate(), botSettings.getChestPlate()))));
        equipment.add(new Equipment(EquipmentSlot.LEGGINGS, createItem(resolveItemType(bot == null ? null : bot.getLeggings(), botSettings.getLeggings()))));
        equipment.add(new Equipment(EquipmentSlot.BOOTS, createItem(resolveItemType(bot == null ? null : bot.getBoots(), botSettings.getBoots()))));
        return equipment;
    }

    private ItemType resolveLiveItemType(org.bukkit.inventory.ItemStack itemStack, ItemType fallback) {
        if (itemStack == null) {
            return fallback;
        }

        Material material = itemStack.getType();
        if (material == null || material == Material.AIR) {
            return ItemTypes.AIR;
        }

        String normalized = material.name()
                .toLowerCase()
                .replace("gold_", "golden_")
                .replace("wood_", "wooden_");
        ItemType namespaced = ItemTypes.getByName("minecraft:" + normalized);
        if (namespaced != null) {
            return namespaced;
        }

        ItemType byName = ItemTypes.getByName(normalized);
        return byName == null ? fallback : byName;
    }

    private ItemStack createItem(ItemType itemType) {
        return ItemStack.builder()
                .type(itemType)
                .amount(1)
                .build();
    }

    private void sendHeadLook(Player viewer, int entityId, float yaw) {
        if (!canSendPackets(viewer)) {
            return;
        }

        WrapperPlayServerEntityHeadLook headLook =
                new WrapperPlayServerEntityHeadLook(entityId, yaw);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, headLook);
    }

    private WrapperPlayServerPlayerInfo.PlayerData createPlayerData(UUID uuid, String name, BotSkin skin) {
        return new WrapperPlayServerPlayerInfo.PlayerData(
                null,
                createProfile(uuid, name, skin),
                GameMode.SURVIVAL,
                0
        );
    }

    private UserProfile createProfile(UUID uuid, String name, BotSkin skin) {
        if (skin == null || !skin.isValid()) {
            return new UserProfile(uuid, name);
        }

        return new UserProfile(
                uuid,
                name,
                Collections.singletonList(new TextureProperty("textures", skin.getValue(), skin.getSignature()))
        );
    }

    private void scheduleTabRemove(final Player viewer, final UUID uuid, final int entityId, final String name) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                removeFakePlayerFromTab(viewer, uuid, entityId, name);
            }
        }, 10L);
    }

    private boolean canSendPackets(Player viewer) {
        return viewer != null
                && viewer.isOnline()
                && PacketEvents.getAPI() != null;
    }

    private String visibilityKey(Player viewer, PitBot bot) {
        return viewer.getUniqueId().toString() + ":" + bot.getUniqueId().toString();
    }

    private ItemType resolveItemType(ItemType current, ItemType fallback) {
        return current == null || current == ItemTypes.AIR ? fallback : current;
    }
}
