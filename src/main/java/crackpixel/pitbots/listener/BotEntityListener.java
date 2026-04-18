package crackpixel.pitbots.listener;

import crackpixel.pitbots.bot.BotKillService;
import crackpixel.pitbots.bot.BotNameTagService;
import crackpixel.pitbots.bot.BotProfileService;
import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.BotSpawnService;
import crackpixel.pitbots.bot.BotStatsService;
import crackpixel.pitbots.bot.BotBehaviorState;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.nms.BotEntityService;
import crackpixel.pitbots.packet.BotPacketService;
import crackpixel.pitbots.pitcore.PitCoreBridgeService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BotEntityListener implements Listener {

    private static final double MAX_SAFE_HORIZONTAL_VELOCITY = 1.85D;
    private static final double MAX_SAFE_VERTICAL_VELOCITY = 0.65D;

    private final Plugin plugin;
    private final BotSettings botSettings;
    private final BotStatsService botStatsService;
    private final BotKillService botKillService;
    private final BotNameTagService botNameTagService;
    private final BotPacketService botPacketService;
    private final BotEntityService botEntityService;
    private final PitCoreBridgeService pitCoreBridgeService;
    private final BotProfileService botProfileService;
    private final BotSpawnService botSpawnService;
    private final Set<UUID> pendingDeaths = new HashSet<UUID>();

    private Method pitDeathGetDeathModelMethod;
    private Method deathModelGetVictimMethod;
    private Method deathModelSetDropChanceMethod;
    private Method pitRespawnGetPlayerMethod;
    private Method pitRespawnSetSpawnLocationMethod;

    public BotEntityListener(Plugin plugin,
                             BotSettings botSettings,
                             BotStatsService botStatsService,
                             BotKillService botKillService,
                             BotNameTagService botNameTagService,
                             BotPacketService botPacketService,
                             BotEntityService botEntityService,
                             PitCoreBridgeService pitCoreBridgeService,
                             BotProfileService botProfileService,
                             BotSpawnService botSpawnService) {
        this.plugin = plugin;
        this.botSettings = botSettings;
        this.botStatsService = botStatsService;
        this.botKillService = botKillService;
        this.botNameTagService = botNameTagService;
        this.botPacketService = botPacketService;
        this.botEntityService = botEntityService;
        this.pitCoreBridgeService = pitCoreBridgeService;
        this.botProfileService = botProfileService;
        this.botSpawnService = botSpawnService;
        registerPitCoreHooks();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBotDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        PitBot bot = botEntityService.getBot(victim);
        if (bot == null) {
            return;
        }

        long now = System.currentTimeMillis();
        bot.setLastDamageAt(now);
        final Player killer;
        final boolean usePitCoreLifecycle = pitCoreBridgeService != null && pitCoreBridgeService.isAvailable();

        if (event instanceof EntityDamageByEntityEvent) {
            Player attacker = resolveAttacker(((EntityDamageByEntityEvent) event).getDamager());
            if (attacker != null && !attacker.getUniqueId().equals(bot.getUniqueId())) {
                bot.setTargetPlayerId(attacker.getUniqueId());
                bot.setBehaviorState(BotBehaviorState.CHASE);
                bot.setLastSeenTargetAt(now);
                bot.setLastSeenTargetLocation(attacker.getLocation());
                bot.setSearchUntil(now + botSettings.createSearchDurationMs());
                if (botSettings.isDirectChaseEnabled()) {
                    bot.setTargetLockedUntil(now + botSettings.getAggroMemoryMs());
                } else {
                    bot.setTargetLockedUntil(0L);
                    bot.setTargetCommitUntil(0L);
                }
                applyIncomingKnockback(bot, attacker, now);
            }
            killer = attacker;
        } else {
            killer = null;
        }

        if (isLethal(victim, event) && pendingDeaths.add(bot.getUniqueId())) {
            if (usePitCoreLifecycle) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    pendingDeaths.remove(bot.getUniqueId());
                    handleBotDeath(bot, killer);
                }
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Player botPlayer = botEntityService.getBukkitPlayer(bot);
                if (botPlayer == null) {
                    return;
                }

                botEntityService.syncFromEntity(bot);
                if (bot.isDead()) {
                    return;
                }

                if (botPlayer.isDead() || bot.getHealth() <= 0.0D) {
                    if (usePitCoreLifecycle) {
                        return;
                    }
                    if (pendingDeaths.add(bot.getUniqueId())) {
                        pendingDeaths.remove(bot.getUniqueId());
                        handleBotDeath(bot, killer);
                    }
                    return;
                }

                botNameTagService.updateHealth(bot);
            }
        });
    }

    private void handleBotDeath(PitBot bot, Player killer) {
        if (bot == null || bot.isDead()) {
            return;
        }

        botEntityService.releaseBotsTargeting(bot.getUniqueId());
        bot.setAlive(false);
        bot.setHealth(0.0D);
        bot.setRespawnAt(System.currentTimeMillis() + botSettings.getRespawnDelayMs());
        bot.setPitCoreRespawnPending(false);
        bot.setTargetPlayerId(null);
        bot.setTargetLockedUntil(0L);
        bot.setTargetCommitUntil(0L);
        bot.setAttackChargeUntil(0L);
        bot.setObstacleRepositionUntil(0L);
        bot.setLastDirectLineOfSightAt(0L);

        Player botPlayer = botEntityService.getBukkitPlayer(bot);
        boolean killerIsRealPlayer = killer != null && (botEntityService == null || !botEntityService.isBot(killer));

        if (killerIsRealPlayer) {
            botStatsService.recordKill(killer);
            boolean handledByPitCore = pitCoreBridgeService != null
                    && botPlayer != null
                    && pitCoreBridgeService.handleBotKillEvent(killer, botPlayer);
            if (!handledByPitCore) {
                botKillService.handleBotKill(killer, bot);
            }
        }

        botNameTagService.unregisterBot(bot);
        botPacketService.destroyBotForAll(bot);
    }

    @SuppressWarnings("unchecked")
    private void registerPitCoreHooks() {
        if (pitCoreBridgeService == null || !pitCoreBridgeService.isAvailable()) {
            return;
        }

        Plugin pitPlugin = Bukkit.getPluginManager().getPlugin("Pit");
        if (pitPlugin == null || !pitPlugin.isEnabled()) {
            return;
        }

        try {
            ClassLoader classLoader = pitPlugin.getClass().getClassLoader();
            Class<?> deathModelClass = Class.forName("pit.sandbox.model.DeathModel", true, classLoader);
            Class<? extends Event> pitDeathEventClass = (Class<? extends Event>) Class.forName("pit.sandbox.event.pit.PitDeathEvent", true, classLoader);
            Class<? extends Event> pitRespawnEventClass = (Class<? extends Event>) Class.forName("pit.sandbox.event.pit.PitRespawnEvent", true, classLoader);

            pitDeathGetDeathModelMethod = pitDeathEventClass.getMethod("getDeathModel");
            deathModelGetVictimMethod = deathModelClass.getMethod("getVictim");
            deathModelSetDropChanceMethod = deathModelClass.getMethod("setDropChance", double.class);
            pitRespawnGetPlayerMethod = pitRespawnEventClass.getMethod("getPlayer");
            pitRespawnSetSpawnLocationMethod = pitRespawnEventClass.getMethod("setSpawnLocation", Location.class);

            Bukkit.getPluginManager().registerEvent(
                    pitDeathEventClass,
                    this,
                    EventPriority.LOWEST,
                    new EventExecutor() {
                        @Override
                        public void execute(Listener listener, Event event) {
                            onPitDeathEvent(event);
                        }
                    },
                    plugin,
                    false
            );

            Bukkit.getPluginManager().registerEvent(
                    pitRespawnEventClass,
                    this,
                    EventPriority.HIGHEST,
                    new EventExecutor() {
                        @Override
                        public void execute(Listener listener, Event event) {
                            onPitRespawnPrepare(event);
                        }
                    },
                    plugin,
                    false
            );

            Bukkit.getPluginManager().registerEvent(
                    pitRespawnEventClass,
                    this,
                    EventPriority.MONITOR,
                    new EventExecutor() {
                        @Override
                        public void execute(Listener listener, Event event) {
                            onPitRespawnComplete(event);
                        }
                    },
                    plugin,
                    false
            );
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not register PitCore bot lifecycle hooks: " + exception.getMessage());
        }
    }

    private void onPitDeathEvent(Event event) {
        Object deathModel = invoke(event, pitDeathGetDeathModelMethod);
        Player victim = deathModel == null ? null : asPlayer(invoke(deathModel, deathModelGetVictimMethod));
        PitBot bot = victim == null ? null : botEntityService.getBot(victim);
        if (bot == null) {
            return;
        }

        invokeVoid(deathModel, deathModelSetDropChanceMethod, 0.0D);
        pendingDeaths.remove(bot.getUniqueId());
        botEntityService.releaseBotsTargeting(bot.getUniqueId());
        bot.setAlive(false);
        bot.setHealth(0.0D);
        bot.setRespawnAt(System.currentTimeMillis() + botSettings.getRespawnDelayMs());
        bot.setPitCoreRespawnPending(true);
        bot.setTargetPlayerId(null);
        bot.setTargetLockedUntil(0L);
        bot.setTargetCommitUntil(0L);
        bot.setAttackChargeUntil(0L);
        bot.setHitReactUntil(0L);
        bot.setObstacleRepositionUntil(0L);
        bot.setLastDirectLineOfSightAt(0L);
        bot.setKnockbackX(0.0D);
        bot.setKnockbackZ(0.0D);
        botNameTagService.unregisterBot(bot);
        botPacketService.hideBotPacketsForAll(bot);
    }

    private void onPitRespawnPrepare(Event event) {
        Player player = asPlayer(invoke(event, pitRespawnGetPlayerMethod));
        PitBot bot = player == null ? null : botEntityService.getBot(player);
        if (bot == null) {
            return;
        }

        Location spawnLocation = botSpawnService.createSpawnLocation();
        if (spawnLocation == null) {
            return;
        }

        invokeVoid(event, pitRespawnSetSpawnLocationMethod, spawnLocation);
    }

    private void onPitRespawnComplete(Event event) {
        Player player = asPlayer(invoke(event, pitRespawnGetPlayerMethod));
        PitBot bot = player == null ? null : botEntityService.getBot(player);
        if (bot == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Location currentLocation = player.getLocation();
        boolean realDeathRespawn = bot.isPitCoreRespawnPending() || !bot.isAlive();
        pendingDeaths.remove(bot.getUniqueId());
        bot.setPitCoreRespawnPending(false);
        bot.setRespawnAt(0L);
        if (realDeathRespawn) {
            bot.resetForRespawn(currentLocation);
            botProfileService.configureBot(bot, now);
            botEntityService.applyState(bot);
            botEntityService.applyPitCoreHealthProfile(bot, true);
        } else {
            bot.setLocation(currentLocation);
            bot.setAlive(true);
            botEntityService.syncFromEntity(bot);
        }
        botNameTagService.registerBot(bot);
        botNameTagService.updateHealth(bot);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            botPacketService.updateBotFor(viewer, bot);
        }
    }

    private Object invoke(Object target, Method method, Object... args) {
        if (target == null || method == null) {
            return null;
        }

        try {
            return method.invoke(target, args);
        } catch (Exception exception) {
            return null;
        }
    }

    private void invokeVoid(Object target, Method method, Object... args) {
        invoke(target, method, args);
    }

    private Player asPlayer(Object value) {
        return value instanceof Player ? (Player) value : null;
    }

    private boolean isLethal(Player victim, EntityDamageEvent event) {
        return victim != null
                && event != null
                && (victim.getHealth() - event.getFinalDamage()) <= 0.01D;
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) damager;
            if (arrow.getShooter() instanceof Player) {
                return (Player) arrow.getShooter();
            }
        }

        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }

    private void applyIncomingKnockback(PitBot bot, Player attacker, long now) {
        if (bot == null || attacker == null || bot.getLocation() == null || attacker.getLocation() == null) {
            return;
        }

        bot.setAttackChargeUntil(0L);
        bot.setNextAttackAt(Math.max(bot.getNextAttackAt(), now + createReengageDelay(bot, 0.90D)));
        bot.setHitReactUntil(Math.max(bot.getHitReactUntil(), now + botSettings.getHitReactionMs()));
        bot.setSearchUntil(now + createReengageSearchDuration(bot, 0.82D));

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                resolveIncomingKnockback(bot, attacker);
            }
        });
    }

    private void resolveIncomingKnockback(PitBot bot, Player attacker) {
        if (bot == null || attacker == null || !attacker.isOnline() || bot.isDead()) {
            return;
        }

        Player botPlayer = botEntityService.getBukkitPlayer(bot);
        Vector vanillaVelocity = botPlayer == null ? null : botPlayer.getVelocity().clone();
        if (hasMeaningfulHorizontalVelocity(vanillaVelocity)) {
            bot.setKnockbackX(limitKnockback(vanillaVelocity.getX() * 0.92D));
            bot.setKnockbackZ(limitKnockback(vanillaVelocity.getZ() * 0.92D));
            if (botEntityService != null) {
                botEntityService.syncFromEntity(bot);
            }
            return;
        }

        Location botLocation = bot.getLocation();
        Location attackerLocation = attacker.getLocation();
        if (botLocation.getWorld() != attackerLocation.getWorld()) {
            return;
        }

        Vector direction = botLocation.toVector().subtract(attackerLocation.toVector());
        direction.setY(0.0D);
        if (direction.lengthSquared() <= 0.0001D) {
            float yawRadians = (float) Math.toRadians(attackerLocation.getYaw());
            direction.setX(-Math.sin(yawRadians));
            direction.setZ(Math.cos(yawRadians));
        }

        if (direction.lengthSquared() <= 0.0001D) {
            return;
        }

        direction.normalize();

        float yawRadians = (float) Math.toRadians(attackerLocation.getYaw());
        Vector facing = new Vector(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        double facingWeight = botSettings.getKnockbackFacingWeight();
        double mixedX = (facing.getX() * facingWeight) + (direction.getX() * (1.0D - facingWeight));
        double mixedZ = (facing.getZ() * facingWeight) + (direction.getZ() * (1.0D - facingWeight));
        Vector knockback = new Vector(mixedX, 0.0D, mixedZ);
        if (knockback.lengthSquared() <= 0.0001D) {
            return;
        }

        knockback.normalize().multiply(botSettings.calculatePlayerKnockbackHorizontal(attacker));
        double horizontalX = limitKnockback(knockback.getX());
        double horizontalZ = limitKnockback(knockback.getZ());

        if (botPlayer != null) {
            Vector velocity = botPlayer.getVelocity();
            velocity.setX(clampHorizontalVelocity((velocity.getX() * 0.20D) + horizontalX));
            velocity.setZ(clampHorizontalVelocity((velocity.getZ() * 0.20D) + horizontalZ));
            velocity.setY(clampVerticalVelocity(Math.max(Math.min(velocity.getY(), 0.28D), 0.34D)));
            botPlayer.setVelocity(velocity);
            bot.setKnockbackX(limitKnockback(velocity.getX() * 0.96D));
            bot.setKnockbackZ(limitKnockback(velocity.getZ() * 0.96D));
            return;
        }

        bot.setKnockbackX(horizontalX);
        bot.setKnockbackZ(horizontalZ);
    }

    private boolean hasMeaningfulHorizontalVelocity(Vector velocity) {
        return velocity != null
                && ((Math.abs(velocity.getX()) > 0.02D) || (Math.abs(velocity.getZ()) > 0.02D));
    }

    private void clearCustomKnockback(PitBot bot) {
        if (bot == null) {
            return;
        }

        bot.setKnockbackX(0.0D);
        bot.setKnockbackZ(0.0D);
    }

    private double limitKnockback(double value) {
        double max = 1.90D;
        if (value > max) {
            return max;
        }

        if (value < -max) {
            return -max;
        }

        return value;
    }

    private double clampHorizontalVelocity(double value) {
        if (value > MAX_SAFE_HORIZONTAL_VELOCITY) {
            return MAX_SAFE_HORIZONTAL_VELOCITY;
        }

        if (value < -MAX_SAFE_HORIZONTAL_VELOCITY) {
            return -MAX_SAFE_HORIZONTAL_VELOCITY;
        }

        return value;
    }

    private double clampVerticalVelocity(double value) {
        if (value > MAX_SAFE_VERTICAL_VELOCITY) {
            return MAX_SAFE_VERTICAL_VELOCITY;
        }

        if (value < -MAX_SAFE_VERTICAL_VELOCITY) {
            return -MAX_SAFE_VERTICAL_VELOCITY;
        }

        return value;
    }

    private long createReengageDelay(PitBot bot, double multiplier) {
        double effectiveMultiplier = multiplier;
        if (bot != null && bot.getTier() != null) {
            switch (bot.getTier()) {
                case EASY:
                    effectiveMultiplier *= 1.18D;
                    break;
                case TRYHARD:
                    effectiveMultiplier *= 0.86D;
                    break;
                default:
                    break;
            }
        }

        return Math.max(80L, Math.round(botSettings.getHurtCooldownMs() * effectiveMultiplier));
    }

    private long createReengageSearchDuration(PitBot bot, double multiplier) {
        long base = botSettings.createSearchDurationMs();
        double effectiveMultiplier = multiplier;
        if (bot != null && bot.getTier() != null) {
            switch (bot.getTier()) {
                case EASY:
                    effectiveMultiplier *= 0.92D;
                    break;
                case TRYHARD:
                    effectiveMultiplier *= 1.12D;
                    break;
                default:
                    break;
            }
        }

        return Math.max(450L, Math.round(base * effectiveMultiplier));
    }
}
