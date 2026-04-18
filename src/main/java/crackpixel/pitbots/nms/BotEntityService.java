package crackpixel.pitbots.nms;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.BotSkin;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.pitcore.PitCoreBridgeService;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumProtocolDirection;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BotEntityService {

    private static final double HARD_SYNC_DISTANCE_SQUARED = 4.0D;
    private static final double MIN_MOVE_DISTANCE_SQUARED = 0.0004D;

    private final Plugin plugin;
    private final BotSettings botSettings;
    private final PitCoreBridgeService pitCoreBridgeService;
    private final Map<UUID, EntityPlayer> entities = new HashMap<UUID, EntityPlayer>();
    private final Map<UUID, PitBot> botsByUniqueId = new HashMap<UUID, PitBot>();
    private final Set<UUID> spawnedBots = new HashSet<UUID>();

    public BotEntityService(Plugin plugin, BotSettings botSettings, PitCoreBridgeService pitCoreBridgeService) {
        this.plugin = plugin;
        this.botSettings = botSettings;
        this.pitCoreBridgeService = pitCoreBridgeService;
    }

    public EntityPlayer ensureEntity(PitBot bot) {
        if (bot == null || bot.getLocation() == null || bot.getLocation().getWorld() == null) {
            return null;
        }

        EntityPlayer entity = entities.get(bot.getUniqueId());
        if (entity != null) {
            botsByUniqueId.put(bot.getUniqueId(), bot);
            bot.setEntityId(entity.getId());
            return entity;
        }

        entity = createEntity(bot);
        entities.put(bot.getUniqueId(), entity);
        botsByUniqueId.put(bot.getUniqueId(), bot);
        bot.setEntityId(entity.getId());
        applyState(bot);
        return entity;
    }

    public boolean spawnBot(PitBot bot) {
        if (bot == null || bot.getLocation() == null || bot.getLocation().getWorld() == null) {
            return false;
        }

        EntityPlayer entity = ensureEntity(bot);
        if (entity == null) {
            return false;
        }

        if (spawnedBots.contains(bot.getUniqueId())) {
            applyState(bot);
            return true;
        }

        WorldServer world = ((CraftWorld) bot.getLocation().getWorld()).getHandle();
        if (entity.world != world) {
            despawnBot(bot);
            entity = createEntity(bot);
            entities.put(bot.getUniqueId(), entity);
            botsByUniqueId.put(bot.getUniqueId(), bot);
            bot.setEntityId(entity.getId());
        }

        world.addEntity(entity);
        spawnedBots.add(bot.getUniqueId());
        applyState(bot);
        applyPitCoreHealthProfile(bot, true);
        runPitJoinLifecycle(bot);
        return true;
    }

    public void applyState(PitBot bot) {
        EntityPlayer entity = getEntity(bot);
        if (bot == null || entity == null || bot.getLocation() == null) {
            return;
        }

        Location location = bot.getLocation();
        double dx = location.getX() - entity.locX;
        double dy = location.getY() - entity.locY;
        double dz = location.getZ() - entity.locZ;
        double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);
        boolean preservePhysics = spawnedBots.contains(bot.getUniqueId()) && shouldPreservePhysics(bot, entity);

        if (!preservePhysics) {
            dampStaleMotion(entity);
        }

        if (!preservePhysics && (!spawnedBots.contains(bot.getUniqueId())
                || distanceSquared >= HARD_SYNC_DISTANCE_SQUARED
                || Math.abs(dy) > 1.25D)) {
            entity.setLocation(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()
            );
        } else if (!preservePhysics && distanceSquared >= MIN_MOVE_DISTANCE_SQUARED) {
            entity.move(dx, dy, dz);
        }

        entity.yaw = location.getYaw();
        entity.pitch = location.getPitch();
        entity.lastYaw = location.getYaw();
        entity.aK = location.getYaw();
        if (!preservePhysics && (!spawnedBots.contains(bot.getUniqueId())
                || distanceSquared >= HARD_SYNC_DISTANCE_SQUARED
                || Math.abs(dy) > 1.25D)) {
            entity.fallDistance = 0.0F;
        }

        Player player = getBukkitPlayer(bot);
        if (player == null) {
            return;
        }

        try {
            if (Math.abs(player.getMaxHealth() - bot.getMaxHealth()) > 0.01D) {
                player.setMaxHealth(bot.getMaxHealth());
            }
        } catch (Exception ignored) {
        }

        if (!shouldLetPitHandleEquipment()) {
            applyEquipment(bot, player.getInventory());
        }
    }

    public void syncFromEntity(PitBot bot) {
        Player player = getBukkitPlayer(bot);
        if (bot == null || player == null || !spawnedBots.contains(bot.getUniqueId())) {
            return;
        }

        bot.setLocation(player.getLocation());
        bot.setHealth(Math.max(0.0D, Math.min(player.getHealth(), player.getMaxHealth())));
        bot.setMaxHealth(player.getMaxHealth());
        bot.setAlive(!player.isDead());
    }

    public boolean damageTarget(PitBot bot, Player target) {
        Player botPlayer = getBukkitPlayer(bot);
        if (bot == null || target == null || botPlayer == null || !spawnedBots.contains(bot.getUniqueId())) {
            return false;
        }

        if (botPlayer.getWorld() != target.getWorld()) {
            return false;
        }

        target.damage(bot.getAttackDamage(), botPlayer);
        ItemStack heldItem = botPlayer.getItemInHand();
        if (heldItem != null) {
            int fireAspect = heldItem.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
            if (fireAspect > 0) {
                target.setFireTicks(Math.max(target.getFireTicks(), fireAspect * 80));
            }
        }
        return true;
    }

    public void despawnBot(PitBot bot) {
        if (bot == null) {
            return;
        }

        Player botPlayer = getBukkitPlayer(bot);
        if (pitCoreBridgeService != null && botPlayer != null) {
            pitCoreBridgeService.clearTrackedPlayer(botPlayer);
        }

        EntityPlayer entity = entities.remove(bot.getUniqueId());
        botsByUniqueId.remove(bot.getUniqueId());
        spawnedBots.remove(bot.getUniqueId());

        if (entity == null) {
            return;
        }

        try {
            if (entity.world != null) {
                entity.world.removeEntity(entity);
            }
        } catch (Exception ignored) {
        }

        try {
            entity.die();
        } catch (Exception ignored) {
        }
    }

    public void clearAll() {
        for (UUID uniqueId : new HashSet<UUID>(entities.keySet())) {
            PitBot bot = botsByUniqueId.get(uniqueId);
            if (bot != null) {
                despawnBot(bot);
            } else {
                entities.remove(uniqueId);
                spawnedBots.remove(uniqueId);
            }
        }
    }

    public boolean isBot(Player player) {
        return player != null && botsByUniqueId.containsKey(player.getUniqueId());
    }

    public PitBot getBot(Player player) {
        return player == null ? null : botsByUniqueId.get(player.getUniqueId());
    }

    public EntityPlayer getEntity(PitBot bot) {
        return bot == null ? null : entities.get(bot.getUniqueId());
    }

    public Player getBukkitPlayer(PitBot bot) {
        EntityPlayer entity = getEntity(bot);
        return entity == null ? null : entity.getBukkitEntity();
    }

    public boolean hasActivePhysics(PitBot bot) {
        return hasActivePhysics(getEntity(bot));
    }

    public Player getPlayer(UUID uniqueId) {
        if (uniqueId == null) {
            return null;
        }

        Player onlinePlayer = Bukkit.getPlayer(uniqueId);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        PitBot bot = botsByUniqueId.get(uniqueId);
        return bot == null ? null : getBukkitPlayer(bot);
    }

    public List<Player> getSpawnedBotPlayers(PitBot excludingBot) {
        List<Player> players = new ArrayList<Player>();
        UUID excludingId = excludingBot == null ? null : excludingBot.getUniqueId();

        for (UUID uniqueId : spawnedBots) {
            if (excludingId != null && excludingId.equals(uniqueId)) {
                continue;
            }

            PitBot otherBot = botsByUniqueId.get(uniqueId);
            if (otherBot == null || otherBot.isDead()) {
                continue;
            }

            Player player = getBukkitPlayer(otherBot);
            if (player != null && !player.isDead()) {
                players.add(player);
            }
        }

        return players;
    }

    public int countLivingBotsTargeting(UUID targetUniqueId, UUID excludingBotUniqueId) {
        if (targetUniqueId == null) {
            return 0;
        }

        int count = 0;
        for (UUID uniqueId : spawnedBots) {
            if (excludingBotUniqueId != null && excludingBotUniqueId.equals(uniqueId)) {
                continue;
            }

            PitBot bot = botsByUniqueId.get(uniqueId);
            if (bot == null || bot.isDead()) {
                continue;
            }

            if (targetUniqueId.equals(bot.getTargetPlayerId())) {
                count++;
            }
        }

        return count;
    }

    public int countLivingBotsNear(Location location, double radius, UUID excludingBotUniqueId) {
        if (location == null || location.getWorld() == null || radius <= 0.0D) {
            return 0;
        }

        double radiusSquared = radius * radius;
        int count = 0;
        for (UUID uniqueId : spawnedBots) {
            if (excludingBotUniqueId != null && excludingBotUniqueId.equals(uniqueId)) {
                continue;
            }

            PitBot bot = botsByUniqueId.get(uniqueId);
            if (bot == null || bot.isDead() || bot.getLocation() == null) {
                continue;
            }

            Location botLocation = bot.getLocation();
            if (botLocation.getWorld() != location.getWorld()) {
                continue;
            }

            double dx = botLocation.getX() - location.getX();
            double dz = botLocation.getZ() - location.getZ();
            double distanceSquared = (dx * dx) + (dz * dz);
            if (distanceSquared <= radiusSquared) {
                count++;
            }
        }

        return count;
    }

    public void releaseBotsTargeting(UUID targetUniqueId) {
        if (targetUniqueId == null) {
            return;
        }

        for (UUID uniqueId : spawnedBots) {
            PitBot bot = botsByUniqueId.get(uniqueId);
            if (bot == null || bot.isDead() || !targetUniqueId.equals(bot.getTargetPlayerId())) {
                continue;
            }

            bot.setTargetPlayerId(null);
            bot.setTargetLockedUntil(0L);
            bot.setTargetCommitUntil(0L);
            bot.setAttackChargeUntil(0L);
            bot.setAimSettleUntil(0L);
            bot.setLastSeenTargetAt(0L);
            bot.setLastSeenTargetLocation(null);
        }
    }

    public boolean isSpawned(PitBot bot) {
        return bot != null && spawnedBots.contains(bot.getUniqueId());
    }

    public void applyPitCoreHealthProfile(PitBot bot, boolean heal) {
        if (bot == null) {
            return;
        }

        Player botPlayer = getBukkitPlayer(bot);
        if (botPlayer == null) {
            return;
        }

        boolean handledByPitCore = pitCoreBridgeService != null
                && pitCoreBridgeService.syncBotHealthProfile(botPlayer, resolveProfileSource(bot), heal);
        if (!handledByPitCore && heal) {
            try {
                botPlayer.setMaxHealth(bot.getMaxHealth());
                botPlayer.setHealth(Math.max(0.1D, Math.min(bot.getHealth(), bot.getMaxHealth())));
            } catch (Exception ignored) {
            }
        }

        syncFromEntity(bot);
    }

    public boolean triggerPitRespawnEvent(PitBot bot, Location spawnLocation) {
        if (pitCoreBridgeService == null || bot == null || !pitCoreBridgeService.isAvailable()) {
            return false;
        }

        Player botPlayer = getBukkitPlayer(bot);
        if (botPlayer == null) {
            return false;
        }

        return pitCoreBridgeService.handleBotRespawnEvent(botPlayer, spawnLocation);
    }

    private void runPitJoinLifecycle(PitBot bot) {
        if (pitCoreBridgeService == null || bot == null || !pitCoreBridgeService.isAvailable()) {
            return;
        }

        Player botPlayer = getBukkitPlayer(bot);
        if (botPlayer == null) {
            return;
        }

        pitCoreBridgeService.handleBotJoinLifecycle(botPlayer, bot.getLocation());
    }

    private boolean shouldLetPitHandleEquipment() {
        return pitCoreBridgeService != null && pitCoreBridgeService.isAvailable();
    }

    private EntityPlayer createEntity(PitBot bot) {
        Location location = bot.getLocation();
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        PlayerInteractManager interactManager = new PlayerInteractManager(world);
        GameProfile profile = new GameProfile(bot.getUniqueId(), bot.getName());

        BotSkin skin = bot.getSkin();
        if (skin != null && skin.isValid()) {
            profile.getProperties().put("textures", new Property("textures", skin.getValue(), skin.getSignature()));
        }

        EntityPlayer entity = new EntityPlayer(server, world, profile, interactManager);
        entity.playerConnection = new BotPlayerConnection(server, createBotNetworkManager(), entity);
        entity.setLocation(
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        entity.aK = location.getYaw();
        entity.onGround = true;
        return entity;
    }

    private void applyEquipment(PitBot bot, PlayerInventory inventory) {
        if (bot == null || inventory == null) {
            return;
        }

        inventory.setHeldItemSlot(0);
        inventory.setItemInHand(resolveEquipmentItem(bot.getMainHandItem(), bot.getMainHand()));
        inventory.setHelmet(resolveEquipmentItem(bot.getHelmetItem(), bot.getHelmet()));
        inventory.setChestplate(resolveEquipmentItem(bot.getChestPlateItem(), bot.getChestPlate()));
        inventory.setLeggings(resolveEquipmentItem(bot.getLeggingsItem(), bot.getLeggings()));
        inventory.setBoots(resolveEquipmentItem(bot.getBootsItem(), bot.getBoots()));
    }

    private ItemStack resolveEquipmentItem(ItemStack exactItem, com.github.retrooper.packetevents.protocol.item.type.ItemType fallbackType) {
        if (exactItem != null && exactItem.getType() != org.bukkit.Material.AIR) {
            return exactItem.clone();
        }

        return botSettings.toBukkitItem(fallbackType);
    }

    private Player resolveProfileSource(PitBot bot) {
        if (bot == null || bot.getProfileSourcePlayerId() == null) {
            return null;
        }

        Player sourcePlayer = Bukkit.getPlayer(bot.getProfileSourcePlayerId());
        if (sourcePlayer == null || !sourcePlayer.isOnline()) {
            return null;
        }

        return sourcePlayer;
    }

    private NetworkManager createBotNetworkManager() {
        NetworkManager networkManager = new NetworkManager(EnumProtocolDirection.CLIENTBOUND);

        try {
            Field channelField = NetworkManager.class.getDeclaredField("channel");
            channelField.setAccessible(true);

            Object current = channelField.get(networkManager);
            if (!(current instanceof Channel)) {
                channelField.set(networkManager, new EmbeddedChannel());
            }
        } catch (Exception ignored) {
        }

        return networkManager;
    }

    private boolean shouldPreservePhysics(PitBot bot, EntityPlayer entity) {
        if (!hasActivePhysics(entity)) {
            return false;
        }

        if (bot == null) {
            return true;
        }

        double horizontalMotion = Math.hypot(entity.motX, entity.motZ);
        double verticalMotion = Math.abs(entity.motY);
        long now = System.currentTimeMillis();
        long recentDamageWindow = Math.max(220L, botSettings.getHitReactionMs() + 80L);
        boolean recentlyDamaged = (now - bot.getLastDamageAt()) <= recentDamageWindow || now < bot.getHitReactUntil();
        if (recentlyDamaged) {
            return horizontalMotion > 0.045D || verticalMotion > 0.04D;
        }

        if (!entity.onGround) {
            return verticalMotion > 0.04D || horizontalMotion > 0.11D;
        }

        return verticalMotion > 0.08D || horizontalMotion > 0.18D;
    }

    private boolean hasActivePhysics(EntityPlayer entity) {
        if (entity == null) {
            return false;
        }

        return Math.abs(entity.motX) > 0.02D
                || Math.abs(entity.motY) > 0.02D
                || Math.abs(entity.motZ) > 0.02D;
    }

    private void dampStaleMotion(EntityPlayer entity) {
        if (entity == null) {
            return;
        }

        if (Math.abs(entity.motX) < 0.08D) {
            entity.motX = 0.0D;
        } else {
            entity.motX *= 0.35D;
        }

        if (Math.abs(entity.motZ) < 0.08D) {
            entity.motZ = 0.0D;
        } else {
            entity.motZ *= 0.35D;
        }

        if (entity.onGround && Math.abs(entity.motY) < 0.08D) {
            entity.motY = 0.0D;
        }
    }
}
