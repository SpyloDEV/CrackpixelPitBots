package crackpixel.pitbots.task;

import crackpixel.pitbots.ai.BotTargetingService;
import crackpixel.pitbots.bot.BotBehaviorState;
import crackpixel.pitbots.bot.BotManager;
import crackpixel.pitbots.bot.BotNameTagService;
import crackpixel.pitbots.bot.BotProfileService;
import crackpixel.pitbots.bot.BotScalingService;
import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.BotSpawnService;
import crackpixel.pitbots.bot.BotTier;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.nms.BotEntityService;
import crackpixel.pitbots.packet.BotPacketService;
import crackpixel.pitbots.performance.PerformanceMonitorService;
import crackpixel.pitbots.util.BotMath;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class BotTickTask extends BukkitRunnable {

    private static final long SCALE_INTERVAL_MS = 2000L;
    private static final double MAX_SAFE_HORIZONTAL_ATTACK_KNOCKBACK_VELOCITY = 1.85D;
    private static final double MAX_SAFE_VERTICAL_ATTACK_KNOCKBACK_VELOCITY = 0.65D;

    private final BotManager botManager;
    private final BotSettings botSettings;
    private final BotNameTagService botNameTagService;
    private final BotProfileService botProfileService;
    private final BotSpawnService botSpawnService;
    private final BotScalingService botScalingService;
    private final BotEntityService botEntityService;
    private final BotPacketService botPacketService;
    private final PerformanceMonitorService performanceMonitorService;
    private final BotTargetingService targetingService;
    private final BotMovementController movementController;
    private final Random random;

    private long nextScaleAt;

    public BotTickTask(BotManager botManager,
                       BotSettings botSettings,
                       BotNameTagService botNameTagService,
                       BotProfileService botProfileService,
                       BotSpawnService botSpawnService,
                       BotScalingService botScalingService,
                       BotEntityService botEntityService,
                       BotPacketService botPacketService,
                       PerformanceMonitorService performanceMonitorService) {
        this.botManager = botManager;
        this.botSettings = botSettings;
        this.botNameTagService = botNameTagService;
        this.botProfileService = botProfileService;
        this.botSpawnService = botSpawnService;
        this.botScalingService = botScalingService;
        this.botEntityService = botEntityService;
        this.botPacketService = botPacketService;
        this.performanceMonitorService = performanceMonitorService;
        this.targetingService = new BotTargetingService(botSettings, botEntityService);
        this.movementController = new BotMovementController();
        this.random = new Random();
        this.nextScaleAt = 0L;
    }

    @Override
    public void run() {
        if (!botSpawnService.hasCenterLocation()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now >= nextScaleAt) {
            resizeBotPool(now);
            nextScaleAt = now + SCALE_INTERVAL_MS;
        }

        List<PitBot> bots = botManager.getBotsSnapshot();
        for (PitBot bot : bots) {
            tickBot(bot, bots, now);
        }
    }

    private void resizeBotPool(long now) {
        int playerCount = countPlayersInPitWorld();
        int targetBotCount = botScalingService.getTargetBotCount(playerCount);
        if (performanceMonitorService != null) {
            targetBotCount = performanceMonitorService.applyBotCap(targetBotCount);
        }

        while (botManager.getAutomaticBotCount() < targetBotCount) {
            PitBot bot = botManager.createBot(botSpawnService.createSpawnLocation());
            configureBot(bot, now);
            botNameTagService.registerBot(bot);
            botPacketService.spawnBotForAll(bot);
        }

        while (botManager.getAutomaticBotCount() > targetBotCount) {
            PitBot removed = botManager.removeLastAutomaticBot();
            if (removed == null) {
                break;
            }
            botPacketService.destroyBotForAll(removed);
            botNameTagService.unregisterBot(removed);
        }
    }

    private int countPlayersInPitWorld() {
        int count = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (player.getGameMode() != GameMode.CREATIVE && botSpawnService.isInPitWorld(player.getLocation())) {
                count++;
            }
        }

        return count;
    }

    private void configureBot(PitBot bot, long now) {
        if (bot == null) {
            return;
        }

        botProfileService.configureBot(bot, now);
    }

    private void tickBot(PitBot bot, List<PitBot> bots, long now) {
        if (bot == null) {
            return;
        }

        if (bot.isDead()) {
            handleRespawn(bot, now);
            return;
        }

        if (botEntityService != null) {
            botEntityService.syncFromEntity(bot);
        }

        if (bot.getLocation() == null) {
            bot.resetForRespawn(botSpawnService.createSpawnLocation());
            configureBot(bot, now);
            botNameTagService.registerBot(bot);
            botPacketService.spawnBotForAll(bot);
            return;
        }

        if (walkBotBackIntoPit(bot, now)) {
            resetStuckCheck(bot, bot.getLocation());
            updateBotView(bot);
            return;
        }

        if (shouldHardReset(bot)) {
            hardResetBot(bot, now);
            return;
        }

        boolean underHitReaction = applyHitReaction(bot, now);
        if (underHitReaction) {
            bot.setAttackChargeUntil(0L);
            bot.setNextAttackAt(Math.max(bot.getNextAttackAt(), now + 35L));
        }

        Player target = targetingService.findTarget(bot, now);
        if (updateMovementProfile(bot, target, now)) {
            bot.setBehaviorState(BotBehaviorState.IDLE);
            resetStuckCheck(bot, bot.getLocation());
            updateBotView(bot);
            return;
        }

        if (target != null) {
            if (!target.getUniqueId().equals(bot.getTargetPlayerId())) {
                bot.setAimSettleUntil(0L);
                bot.setLastSeenTargetAt(0L);
                bot.setLastSeenTargetLocation(null);
                bot.setLastDirectLineOfSightAt(0L);
                bot.setAttackChargeUntil(0L);
                bot.setObstacleRepositionUntil(0L);
                bot.setSearchUntil(now + botSettings.createSearchDurationMs());
                long switchDelay = botSettings.createTargetSwitchAttackDelayMs(bot);
                if (!isBotTarget(target)) {
                    switchDelay = Math.max(4L, Math.round(switchDelay * 0.18D));
                } else {
                    switchDelay = Math.max(3L, Math.round(switchDelay * 0.12D));
                }
                bot.setNextAttackAt(Math.max(bot.getNextAttackAt(), now + switchDelay));
            }
            bot.setTargetPlayerId(target.getUniqueId());
            followTarget(bot, bots, target, now, shouldForceDirectChase(bot, target) || targetingService.shouldDirectChase(bot, target));
        } else if (shouldSearch(bot, now)) {
            bot.setTargetPlayerId(null);
            search(bot, bots, now);
        } else {
            bot.setTargetPlayerId(null);
            bot.setAimSettleUntil(0L);
            bot.setLastSeenTargetAt(0L);
            bot.setLastSeenTargetLocation(null);
            bot.setLastDirectLineOfSightAt(0L);
            bot.setObstacleRepositionUntil(0L);
            bot.setSearchUntil(0L);
            roam(bot, bots, now);
        }

        updateAntiStuck(bot, target, now);
        updateBotView(bot);
    }

    private void followTarget(PitBot bot, List<PitBot> bots, Player target, long now, boolean allowDirectChase) {
        Location current = bot.getLocation();
        Location targetLocation = target.getLocation();

        if (current == null || targetLocation == null || current.getWorld() != targetLocation.getWorld()) {
            return;
        }

        boolean directLineOfSight = hasDirectLineOfSight(bot, target);
        if (directLineOfSight) {
            long lastDirectLineOfSightAt = bot.getLastDirectLineOfSightAt();
            if (lastDirectLineOfSightAt > 0L && (now - lastDirectLineOfSightAt) > 250L) {
                bot.setNextAttackAt(Math.max(bot.getNextAttackAt(), now + botSettings.createLineOfSightReacquireDelayMs(bot)));
            }
            bot.setLastDirectLineOfSightAt(now);
            bot.setLastSeenTargetAt(now);
            bot.setLastSeenTargetLocation(targetLocation);
            bot.setSearchUntil(now + botSettings.createSearchDurationMs());
        }

        Location steeringTarget = resolveSteeringTarget(bot, target, targetLocation, now);
        steeringTarget = applyGroupSupportTarget(bot, bots, target, steeringTarget);

        double distanceSquared = current.distanceSquared(targetLocation);
        boolean crowdedBrawlContext = isCrowdedBrawlContext(bot, target, distanceSquared);
        boolean effectiveDirectLineOfSight = directLineOfSight || crowdedBrawlContext;
        Location aimTarget = targetLocation == null ? steeringTarget : targetLocation;
        double attackRangeSquared = bot.getAttackRange() * bot.getAttackRange();

        if (shouldForceSearchReset(bot, distanceSquared, effectiveDirectLineOfSight, now)) {
            beginSearchReset(bot, now);
            search(bot, bots, now);
            return;
        }

        if (distanceSquared > attackRangeSquared) {
            bot.setBehaviorState(BotBehaviorState.CHASE);
            bot.setAttackChargeUntil(0L);
            Location moved = BotMath.moveTowards2D(
                    current,
                    steeringTarget,
                    resolveChaseStep(bot, now, effectiveDirectLineOfSight, allowDirectChase || crowdedBrawlContext)
            );
            moved = applyStrafe(bot, current, steeringTarget, moved, now, botSettings.getStrafeStrength());
            moved = applySeparation(bot, bots, moved);
            moved = applyCrowdFlow(bot, bots, current, steeringTarget, moved, true);
            moved = keepInsidePit(moved);
            if (!(allowDirectChase || crowdedBrawlContext)) {
                moved = keepNearCenterCombatArea(moved);
            }
            Location grounded = applyMovementGround(current, moved);
            boolean blocked = isMovementBlocked(current, moved, grounded);
            moved = grounded;
            if (blocked || !effectiveDirectLineOfSight) {
                moved = applyObstacleReposition(bot, bots, current, steeringTarget, now, blocked);
            } else {
                bot.setObstacleRepositionUntil(0L);
            }
            moved = applyFacing(
                    bot,
                    moved,
                    aimTarget,
                    botSettings.getTurnSpeedDegrees(),
                    resolvePersonalityJitter(bot, effectiveDirectLineOfSight ? botSettings.getCombatAimJitter() : botSettings.getYawJitter())
            );
            commitMovement(bot, current, moved, true, allowDirectChase || crowdedBrawlContext);
            return;
        }

        bot.setBehaviorState(BotBehaviorState.ATTACK);
        current = applyFacing(
                bot,
                current,
                aimTarget,
                botSettings.getCombatTurnSpeedDegrees(),
                resolvePersonalityJitter(bot, effectiveDirectLineOfSight ? botSettings.getCombatAimJitter() : 0.0D)
        );
        bot.setLocation(current);
        attackTarget(bot, target, now);

        Location combatLocation = applyCombatStrafe(
                bot,
                bots,
                current,
                steeringTarget,
                now,
                distanceSquared,
                effectiveDirectLineOfSight,
                allowDirectChase || crowdedBrawlContext,
                crowdedBrawlContext
        );
        if (!effectiveDirectLineOfSight || BotMath.distanceSquared2D(current, combatLocation) <= 0.001D) {
            combatLocation = applyObstacleReposition(
                    bot,
                    bots,
                    current,
                    steeringTarget,
                    now,
                    BotMath.distanceSquared2D(current, combatLocation) <= 0.001D
            );
        } else {
            bot.setObstacleRepositionUntil(0L);
        }
        if (BotMath.distanceSquared2D(current, combatLocation) <= 0.001D) {
            combatLocation = applyAttackPressureMovement(bot, bots, current, steeringTarget, now, allowDirectChase);
        }
        combatLocation = applyFacing(
                bot,
                combatLocation,
                aimTarget,
                botSettings.getCombatTurnSpeedDegrees(),
                resolvePersonalityJitter(bot, effectiveDirectLineOfSight ? botSettings.getCombatAimJitter() : 0.0D)
        );
        commitMovement(bot, current, combatLocation, true, true);
    }

    private boolean shouldForceDirectChase(PitBot bot, Player target) {
        if (bot == null || target == null || bot.getLocation() == null || isBotTarget(target)) {
            return false;
        }

        Location botLocation = bot.getLocation();
        Location targetLocation = target.getLocation();
        if (botLocation.getWorld() != targetLocation.getWorld()) {
            return false;
        }

        double forceRange = Math.max(bot.getAttackRange() + 5.2D, bot.getFollowRange() * 0.95D);
        forceRange *= personalityChaseBias(bot);
        return botLocation.distanceSquared(targetLocation) <= forceRange * forceRange
                && hasDirectLineOfSight(bot, target);
    }

    private void roam(PitBot bot, List<PitBot> bots, long now) {
        bot.setBehaviorState(BotBehaviorState.ROAM);
        Location current = bot.getLocation();
        Location target = bot.getTargetLocation();

        if (target == null || now >= bot.getNextTargetUpdateAt() || BotMath.distanceSquared2D(current, target) <= 1.0D) {
            Location engagementAnchor = targetingService.findEngagementAnchor(bot);
            target = engagementAnchor == null
                    ? botSpawnService.createRoamLocation()
                    : botSpawnService.createCombatRoamLocation(engagementAnchor);
            bot.setTargetLocation(target);
            bot.setNextTargetUpdateAt(now + randomBetween(
                    botSettings.getRoamRetargetMinMs(),
                    botSettings.getRoamRetargetMaxMs()
            ));
        }

        if (target == null) {
            return;
        }

        Location moved = BotMath.moveTowards2D(current, target, resolveRoamStep(bot));
        moved = applyStrafe(bot, current, target, moved, now, botSettings.getStrafeStrength() * 0.5D);
        moved = applySeparation(bot, bots, moved);
        moved = applyCrowdFlow(bot, bots, current, target, moved, false);
        moved = keepInsidePit(moved);
        moved = keepNearCenterCombatArea(moved);
        Location grounded = applyMovementGround(current, moved);
        if (isMovementBlocked(current, moved, grounded)) {
            Location engagementAnchor = targetingService.findEngagementAnchor(bot);
            bot.setTargetLocation(engagementAnchor == null
                    ? botSpawnService.createRoamLocation()
                    : botSpawnService.createCombatRoamLocation(engagementAnchor));
            bot.setNextTargetUpdateAt(now + randomBetween(700L, 1600L));
        }

        moved = grounded;
        moved = applyFacing(bot, moved, target, botSettings.getTurnSpeedDegrees(), botSettings.getYawJitter());
        commitMovement(bot, current, moved, false, targetingService.hasNearbyEngagement(bot));
    }

    private void search(PitBot bot, List<PitBot> bots, long now) {
        if (bot == null || bot.getLocation() == null || bot.getLastSeenTargetLocation() == null) {
            roam(bot, bots, now);
            return;
        }

        Location current = bot.getLocation();
        Location searchTarget = bot.getLastSeenTargetLocation();
        if (current.getWorld() != searchTarget.getWorld()) {
            bot.setSearchUntil(0L);
            bot.setLastSeenTargetLocation(null);
            roam(bot, bots, now);
            return;
        }

        if (bot.getSearchUntil() <= 0L) {
            bot.setSearchUntil(now + botSettings.createSearchDurationMs());
        }

        if (now >= bot.getSearchUntil() || BotMath.distanceSquared2D(current, searchTarget) <= 1.10D) {
            bot.setSearchUntil(0L);
            bot.setLastSeenTargetLocation(null);
            roam(bot, bots, now);
            return;
        }

        bot.setBehaviorState(BotBehaviorState.SEARCH);
        Location moved = BotMath.moveTowards2D(
                current,
                searchTarget,
                Math.max(0.22D, bot.getSpeed() * 0.88D * bot.getSpeedMultiplier())
        );
        moved = applyStrafe(
                bot,
                current,
                searchTarget,
                moved,
                now,
                botSettings.getStrafeStrength() * 0.35D * bot.getStrafeStrengthMultiplier()
        );
        moved = applySeparation(bot, bots, moved);
        moved = applyCrowdFlow(bot, bots, current, searchTarget, moved, true);
        moved = keepInsidePit(moved);
        moved = keepNearCenterCombatArea(moved);
        Location grounded = applyMovementGround(current, moved);
        if (isMovementBlocked(current, moved, grounded)) {
            grounded = applyObstacleReposition(bot, bots, current, searchTarget, now, true);
        }

        grounded = applyFacing(bot, grounded, searchTarget, botSettings.getTurnSpeedDegrees(), botSettings.getYawJitter());
        commitMovement(bot, current, grounded, false, true);
    }

    private Location applyCombatStrafe(PitBot bot,
                                       List<PitBot> bots,
                                       Location current,
                                       Location target,
                                       long now,
                                       double distanceSquared,
                                       boolean directLineOfSight,
                                       boolean allowDirectChase,
                                       boolean crowdedBrawlContext) {
        if (bot == null
                || current == null
                || target == null
                || current.getWorld() != target.getWorld()) {
            return current == null ? null : current.clone();
        }

        double distance = Math.sqrt(distanceSquared);
        if (distance <= 0.001D) {
            return current.clone();
        }

        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        Location candidate = current.clone();
        double forwardAdjustment = resolveCombatForwardAdjustment(bot, distance, now, directLineOfSight, crowdedBrawlContext);
        if (Math.abs(forwardAdjustment) > 0.0001D) {
            candidate.setX(candidate.getX() + ((dx / distance) * forwardAdjustment));
            candidate.setZ(candidate.getZ() + ((dz / distance) * forwardAdjustment));
        }

        if (botSettings.isCombatStrafeEnabled() && botSettings.getCombatStrafeStrength() > 0.0D) {
            boolean pulseMove = now >= bot.getNextCombatMoveAt();
            if (pulseMove) {
                bot.setNextCombatMoveAt(now + randomBetween(
                        botSettings.getCombatStrafeMinIntervalMs(),
                        botSettings.getCombatStrafeMaxIntervalMs()
                ));
            }

            if (now >= bot.getNextStrafeSwitchAt()) {
                bot.setStrafeDirection(random.nextBoolean() ? 1.0D : -1.0D);
                bot.setNextStrafeSwitchAt(now + randomBetween(540L, 1100L));
            }

            double strength = botSettings.getCombatStrafeStrength() * bot.getSpeedMultiplier() * bot.getStrafeStrengthMultiplier();
            if (now < bot.getCombatResetUntil()) {
                strength *= crowdedBrawlContext ? 0.92D : 0.72D;
            } else if (now < bot.getCombatBurstUntil()) {
                strength *= 1.22D;
            }
            if (!directLineOfSight) {
                strength *= 1.14D;
            }
            if (crowdedBrawlContext) {
                strength *= 1.18D;
            }
            strength *= stateTransitionFactor(bot, now);
            strength *= (1.0D + movementCadence(bot, now, 0.10D, 0.07D, 0.05D));
            strength *= pulseMove ? 1.0D : (crowdedBrawlContext ? 0.94D : 0.78D);

            candidate.setX(candidate.getX() + ((-dz / distance) * strength * bot.getStrafeDirection()));
            candidate.setZ(candidate.getZ() + ((dx / distance) * strength * bot.getStrafeDirection()));
        }

        candidate = applySeparation(bot, bots, candidate);
        candidate = applyCrowdFlow(bot, bots, current, target, candidate, true);
        candidate = keepInsidePit(candidate);
        if (!allowDirectChase) {
            candidate = keepNearCenterCombatArea(candidate);
        }
        Location grounded = applyMovementGround(current, candidate);
        return isMovementBlocked(current, candidate, grounded) ? current.clone() : grounded;
    }

    private Location applyAttackPressureMovement(PitBot bot,
                                                List<PitBot> bots,
                                                Location current,
                                                Location target,
                                                long now,
                                                boolean allowDirectChase) {
        if (bot == null || current == null || target == null || current.getWorld() != target.getWorld()) {
            return current == null ? null : current.clone();
        }

        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance <= 0.001D) {
            return current.clone();
        }

        double forwardStrength = Math.max(0.16D, bot.getSpeed() * 0.94D * bot.getSpeedMultiplier());
        double orbitStrength = Math.max(0.12D, botSettings.getCombatStrafeStrength() * 1.08D * bot.getStrafeStrengthMultiplier());
        double preferredDistance = Math.max(0.9D, bot.getPreferredCombatDistance());
        double distanceDelta = distance - preferredDistance;
        double forwardBias = Math.max(-0.10D, Math.min(0.28D, distanceDelta * 0.72D));

        double[] orbitSigns = new double[]{bot.getStrafeDirection(), -bot.getStrafeDirection()};
        double[] forwardBiases = new double[]{forwardBias, forwardBias + 0.08D, forwardBias - 0.06D};

        for (double orbitSign : orbitSigns) {
            for (double bias : forwardBiases) {
                Location candidate = current.clone();
                candidate.setX(candidate.getX() + ((dx / distance) * (forwardStrength + bias)));
                candidate.setZ(candidate.getZ() + ((dz / distance) * (forwardStrength + bias)));
                candidate.setX(candidate.getX() + ((-dz / distance) * orbitStrength * orbitSign));
                candidate.setZ(candidate.getZ() + ((dx / distance) * orbitStrength * orbitSign));
                candidate = applySeparation(bot, bots, candidate);
                candidate = applyCrowdFlow(bot, bots, current, target, candidate, true);
                candidate = keepInsidePit(candidate);
                if (!allowDirectChase) {
                    candidate = keepNearCenterCombatArea(candidate);
                }
                Location grounded = applyMovementGround(current, candidate);
                if (!isMovementBlocked(current, candidate, grounded)
                        && BotMath.distanceSquared2D(current, grounded) > 0.001D) {
                    return grounded;
                }
            }
        }

        return current.clone();
    }

    private boolean applyHitReaction(PitBot bot, long now) {
        if (bot == null) {
            return false;
        }

        if (botEntityService != null && botEntityService.hasActivePhysics(bot)) {
            return now < bot.getHitReactUntil() || (now - bot.getLastDamageAt()) < botSettings.getHitReactionMs();
        }

        boolean movedByKnockback = applyKnockbackMotion(bot);
        return movedByKnockback || now < bot.getHitReactUntil() || (now - bot.getLastDamageAt()) < botSettings.getHitReactionMs();
    }

    private boolean applyKnockbackMotion(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return false;
        }

        if (botEntityService != null && botEntityService.hasActivePhysics(bot)) {
            return false;
        }

        double knockbackX = bot.getKnockbackX();
        double knockbackZ = bot.getKnockbackZ();
        if (Math.abs(knockbackX) < 0.015D && Math.abs(knockbackZ) < 0.015D) {
            bot.setKnockbackX(0.0D);
            bot.setKnockbackZ(0.0D);
            return false;
        }

        Location current = bot.getLocation();
        Location candidate = current.clone();
        candidate.setX(candidate.getX() + knockbackX);
        candidate.setZ(candidate.getZ() + knockbackZ);
        candidate = keepInsidePit(candidate);

        Location grounded = applyMovementGround(current, candidate);
        boolean blocked = isMovementBlocked(current, candidate, grounded);
        if (blocked) {
            Location partialCandidate = current.clone();
            partialCandidate.setX(partialCandidate.getX() + (knockbackX * 0.55D));
            partialCandidate.setZ(partialCandidate.getZ() + (knockbackZ * 0.55D));
            partialCandidate = keepInsidePit(partialCandidate);
            Location partialGrounded = applyMovementGround(current, partialCandidate);
            if (!isMovementBlocked(current, partialCandidate, partialGrounded)) {
                grounded = partialGrounded;
                blocked = false;
            }
        }

        if (!blocked) {
            movementController.reset(bot);
            bot.setLocation(grounded);
        }

        double decay = blocked ? Math.min(0.55D, botSettings.getKnockbackDecay()) : botSettings.getKnockbackDecay();
        bot.setKnockbackX(bot.getKnockbackX() * decay);
        bot.setKnockbackZ(bot.getKnockbackZ() * decay);

        if (Math.abs(bot.getKnockbackX()) < 0.015D) {
            bot.setKnockbackX(0.0D);
        }

        if (Math.abs(bot.getKnockbackZ()) < 0.015D) {
            bot.setKnockbackZ(0.0D);
        }

        return true;
    }

    private boolean updateMovementProfile(PitBot bot, Player target, long now) {
        if (bot == null) {
            return false;
        }

        if (now >= bot.getNextSpeedChangeAt()) {
            double min = botSettings.getSpeedMultiplierMin();
            double max = botSettings.getSpeedMultiplierMax();
            double range = Math.max(0.0D, max - min);
            bot.setSpeedMultiplier(min + (random.nextDouble() * range));
            bot.setNextSpeedChangeAt(now + randomBetween(
                    botSettings.getSpeedChangeMinMs(),
                    botSettings.getSpeedChangeMaxMs()
            ));
        }

        boolean engagementNearby = targetingService.hasNearbyEngagement(bot);
        if (target != null || engagementNearby || !botSettings.isIdleEnabled()) {
            bot.setIdleUntil(0L);
            if (engagementNearby) {
                bot.setNextIdleCheckAt(now + randomBetween(
                        Math.max(250L, botSettings.getIdleCheckMinMs() / 2L),
                        Math.max(500L, botSettings.getIdleCheckMaxMs() / 2L)
                ));
            }
            if (target == null) {
                bot.setAttackChargeUntil(0L);
                bot.setCombatResetUntil(0L);
                bot.setCombatBurstUntil(0L);
                bot.setAimSettleUntil(0L);
                bot.setLastSeenTargetAt(0L);
                bot.setLastSeenTargetLocation(null);
            }
            return false;
        }

        if (now < bot.getIdleUntil()) {
            return true;
        }

        if (now < bot.getNextIdleCheckAt()) {
            return false;
        }

        bot.setNextIdleCheckAt(now + randomBetween(
                botSettings.getIdleCheckMinMs(),
                botSettings.getIdleCheckMaxMs()
        ));

        if (random.nextDouble() >= botSettings.getIdleChance()) {
            return false;
        }

        bot.setIdleUntil(now + randomBetween(
                botSettings.getIdleMinMs(),
                botSettings.getIdleMaxMs()
        ));
        return true;
    }

    private Location applyStrafe(PitBot bot, Location current, Location target, Location candidate, long now, double strength) {
        if (bot == null || current == null || target == null || candidate == null || strength <= 0.0D) {
            return candidate;
        }

        if (now >= bot.getNextStrafeSwitchAt()) {
            bot.setStrafeDirection(random.nextBoolean() ? 1.0D : -1.0D);
            bot.setNextStrafeSwitchAt(now + randomBetween(800L, 1800L));
        }

        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));

        if (distance <= 0.001D) {
            return candidate;
        }

        Location result = candidate.clone();
        result.setX(result.getX() + ((-dz / distance) * strength * bot.getStrafeDirection()));
        result.setZ(result.getZ() + ((dx / distance) * strength * bot.getStrafeDirection()));
        return result;
    }

    private Location applySeparation(PitBot movingBot, List<PitBot> bots, Location candidate) {
        if (candidate == null || movingBot == null || movingBot.getLocation() == null) {
            return candidate;
        }

        double pushX = 0.0D;
        double pushZ = 0.0D;
        double minDistance = botSettings.getMinBotDistance();
        double minDistanceSquared = minDistance * minDistance;
        Location reference = candidate.getWorld() == movingBot.getLocation().getWorld()
                ? candidate
                : movingBot.getLocation();

        for (PitBot other : bots) {
            if (other == null || other == movingBot || other.isDead() || other.getLocation() == null) {
                continue;
            }

            Location otherLocation = other.getLocation();
            if (reference.getWorld() != otherLocation.getWorld()) {
                continue;
            }

            double dx = reference.getX() - otherLocation.getX();
            double dz = reference.getZ() - otherLocation.getZ();
            double distanceSquared = (dx * dx) + (dz * dz);

            if (distanceSquared <= 0.001D || distanceSquared >= minDistanceSquared) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            double strength = (minDistance - distance) / minDistance;
            strength *= strength;
            pushX += (dx / distance) * strength;
            pushZ += (dz / distance) * strength;
        }

        double pushLength = Math.sqrt((pushX * pushX) + (pushZ * pushZ));
        if (pushLength <= 0.001D) {
            return candidate;
        }

        Location result = candidate.clone();
        boolean engaged = movingBot.getBehaviorState() == BotBehaviorState.CHASE
                || movingBot.getBehaviorState() == BotBehaviorState.ATTACK;
        double crowdBias = movingBot.getPersonality() == null ? 1.0D : movingBot.getPersonality().getCrowdBias();
        double maxPush = (engaged ? 0.18D : 0.12D) / Math.max(0.72D, crowdBias);
        double appliedPush = Math.min(maxPush, ((engaged ? 0.04D : 0.03D) + (pushLength * (engaged ? 0.11D : 0.08D))) / Math.max(0.76D, crowdBias));
        result.setX(result.getX() + ((pushX / pushLength) * appliedPush));
        result.setZ(result.getZ() + ((pushZ / pushLength) * appliedPush));
        return result;
    }

    private Location applyCrowdFlow(PitBot movingBot,
                                    List<PitBot> bots,
                                    Location current,
                                    Location steeringTarget,
                                    Location candidate,
                                    boolean combatBias) {
        if (candidate == null
                || current == null
                || movingBot == null
                || bots == null
                || steeringTarget == null
                || current.getWorld() == null
                || current.getWorld() != candidate.getWorld()
                || current.getWorld() != steeringTarget.getWorld()) {
            return candidate;
        }

        double nearbyRadius = combatBias ? 2.9D : 2.5D;
        double nearbyRadiusSquared = nearbyRadius * nearbyRadius;
        double centroidX = 0.0D;
        double centroidZ = 0.0D;
        double alignmentX = 0.0D;
        double alignmentZ = 0.0D;
        int alignmentCount = 0;
        int nearbyCount = 0;

        for (PitBot other : bots) {
            if (other == null || other == movingBot || other.isDead() || other.getLocation() == null) {
                continue;
            }

            Location otherLocation = other.getLocation();
            if (otherLocation.getWorld() != current.getWorld()) {
                continue;
            }

            double distanceSquared = BotMath.distanceSquared2D(current, otherLocation);
            if (distanceSquared > nearbyRadiusSquared) {
                continue;
            }

            centroidX += otherLocation.getX();
            centroidZ += otherLocation.getZ();
            double neighborMomentumX = other.getMovementMomentumX();
            double neighborMomentumZ = other.getMovementMomentumZ();
            double neighborMomentumLength = Math.sqrt((neighborMomentumX * neighborMomentumX) + (neighborMomentumZ * neighborMomentumZ));
            if (neighborMomentumLength > 0.001D) {
                alignmentX += neighborMomentumX / neighborMomentumLength;
                alignmentZ += neighborMomentumZ / neighborMomentumLength;
                alignmentCount++;
            }
            nearbyCount++;
        }

        if (nearbyCount < 2) {
            return candidate;
        }

        centroidX /= nearbyCount;
        centroidZ /= nearbyCount;

        double awayX = current.getX() - centroidX;
        double awayZ = current.getZ() - centroidZ;
        double awayLength = Math.sqrt((awayX * awayX) + (awayZ * awayZ));
        if (awayLength <= 0.001D) {
            awayX = current.getX() - steeringTarget.getX();
            awayZ = current.getZ() - steeringTarget.getZ();
            awayLength = Math.sqrt((awayX * awayX) + (awayZ * awayZ));
        }

        if (awayLength <= 0.001D) {
            return candidate;
        }

        double pressureStrength = Math.min(0.22D, 0.05D + (nearbyCount * 0.025D));
        pressureStrength *= tierCrowdAvoidanceMultiplier(movingBot, combatBias);

        Location result = candidate.clone();
        result.setX(result.getX() + ((awayX / awayLength) * pressureStrength));
        result.setZ(result.getZ() + ((awayZ / awayLength) * pressureStrength));

        if (combatBias) {
            double toTargetX = steeringTarget.getX() - current.getX();
            double toTargetZ = steeringTarget.getZ() - current.getZ();
            double toTargetLength = Math.sqrt((toTargetX * toTargetX) + (toTargetZ * toTargetZ));
            if (toTargetLength > 0.001D) {
                double laneDirection = ((Math.abs(movingBot.getUniqueId().hashCode()) & 1) == 0) ? 1.0D : -1.0D;
                double laneStrength = Math.min(0.10D, 0.03D + (nearbyCount * 0.015D));
                laneStrength *= tierCrowdLaneMultiplier(movingBot);
                result.setX(result.getX() + ((-toTargetZ / toTargetLength) * laneStrength * laneDirection));
                result.setZ(result.getZ() + ((toTargetX / toTargetLength) * laneStrength * laneDirection));
            }
        }

        if (alignmentCount >= 2) {
            double alignmentLength = Math.sqrt((alignmentX * alignmentX) + (alignmentZ * alignmentZ));
            if (alignmentLength > 0.001D) {
                double alignmentStrength = combatBias ? 0.05D : 0.03D;
                result.setX(result.getX() + ((alignmentX / alignmentLength) * alignmentStrength));
                result.setZ(result.getZ() + ((alignmentZ / alignmentLength) * alignmentStrength));
            }
        }

        return result;
    }

    private Location keepInsidePit(Location location) {
        Location center = botSpawnService.getCenterLocation();
        if (location == null || center == null || location.getWorld() != center.getWorld()) {
            return location;
        }

        if (botSpawnService.isInsidePitArea(location)) {
            return location;
        }

        Location moved = BotMath.moveTowards2D(location, center, 0.8D);
        moved.setY(center.getY());
        return moved;
    }

    private Location keepNearCenterCombatArea(Location location) {
        Location center = botSpawnService.getCenterLocation();
        if (location == null || center == null || location.getWorld() != center.getWorld()) {
            return location;
        }

        double combatRadius = Math.max(
                9.5D,
                Math.max(botSpawnService.getCenterSpawnRadius() + 8.25D, botSpawnService.getMaxRadius() + 0.75D)
        );
        if (BotMath.distanceSquared2D(location, center) <= combatRadius * combatRadius) {
            return location;
        }

        Location moved = BotMath.moveTowards2D(location, center, 0.75D);
        moved.setY(location.getY());
        return moved;
    }

    private Location applyGroundFix(Location location) {
        if (!botSettings.isGroundFixEnabled() || location == null || location.getWorld() == null) {
            return location;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = Math.min(254, location.getBlockY() + 8);
        int minY = Math.max(1, location.getBlockY() - 48);

        for (int y = startY; y >= minY; y--) {
            Material feet = world.getBlockAt(x, y, z).getType();
            Material head = world.getBlockAt(x, y + 1, z).getType();
            Material below = world.getBlockAt(x, y - 1, z).getType();

            if (isPassable(feet) && isPassable(head) && below.isSolid()) {
                Location fixed = location.clone();
                fixed.setY(y);
                return fixed;
            }
        }

        int highestY = world.getHighestBlockYAt(x, z) + 1;
        if (highestY > 1 && highestY < 255) {
            Location fixed = location.clone();
            fixed.setY(highestY);
            return fixed;
        }

        return location;
    }

    private Location applyMovementGround(Location current, Location candidate) {
        Location fixed = applyGroundFix(candidate);

        if (!botSettings.isSafeStepEnabled()
                || current == null
                || fixed == null
                || current.getWorld() != fixed.getWorld()) {
            return fixed;
        }

        double yChange = fixed.getY() - current.getY();
        if (yChange > botSettings.getMaxStepUp() || -yChange > botSettings.getMaxStepDown()) {
            return current.clone();
        }

        return fixed;
    }

    private boolean isMovementBlocked(Location current, Location candidate, Location result) {
        if (current == null || candidate == null || result == null) {
            return false;
        }

        return BotMath.distanceSquared2D(current, candidate) > 0.001D
                && BotMath.distanceSquared2D(current, result) <= 0.001D;
    }

    private boolean isPassable(Material material) {
        return material == null || material == Material.AIR || !material.isSolid();
    }

    private void updateAntiStuck(PitBot bot, Player target, long now) {
        if (!botSettings.isAntiStuckEnabled() || bot == null || bot.getLocation() == null) {
            return;
        }

        Location current = bot.getLocation();

        if (target != null && target.getWorld() == current.getWorld()) {
            double attackRangeSquared = bot.getAttackRange() * bot.getAttackRange();
            if (current.distanceSquared(target.getLocation()) <= attackRangeSquared) {
                resetStuckCheck(bot, current);
                return;
            }
        }

        Location last = bot.getLastMovementCheckLocation();
        double threshold = botSettings.getStuckDistanceThreshold();

        if (last == null || BotMath.distanceSquared2D(current, last) > threshold * threshold) {
            resetStuckCheck(bot, current);
            return;
        }

        bot.setStuckTicks(bot.getStuckTicks() + 1);
        if (bot.getStuckTicks() < botSettings.getStuckTicksBeforeTeleport()) {
            return;
        }

        Location unstuckLocation = createLocalUnstuckLocation(bot, current, target);
        if (unstuckLocation != null) {
            movementController.reset(bot);
            bot.setLocation(unstuckLocation);
        } else {
            hardResetBot(bot, now);
            return;
        }
        bot.setAttackChargeUntil(0L);
        bot.setObstacleRepositionUntil(0L);
        bot.setTargetLocation(botSpawnService.createRoamLocation());

        resetStuckCheck(bot, bot.getLocation());
    }

    private void resetStuckCheck(PitBot bot, Location location) {
        bot.setLastMovementCheckLocation(location);
        bot.setStuckTicks(0);
    }

    private Location createLocalUnstuckLocation(PitBot bot, Location current, Player target) {
        if (bot == null || current == null || current.getWorld() == null) {
            return null;
        }

        Location reference = null;
        if (target != null && target.getWorld() == current.getWorld()) {
            reference = target.getLocation();
        } else if (bot.getTargetLocation() != null && bot.getTargetLocation().getWorld() == current.getWorld()) {
            reference = bot.getTargetLocation();
        } else if (botSpawnService.getCenterLocation() != null && botSpawnService.getCenterLocation().getWorld() == current.getWorld()) {
            reference = botSpawnService.getCenterLocation();
        }

        double baseAngle;
        if (reference == null) {
            baseAngle = random.nextDouble() * Math.PI * 2.0D;
        } else {
            baseAngle = Math.atan2(reference.getZ() - current.getZ(), reference.getX() - current.getX());
        }

        double[] angleOffsets = new double[] {
                Math.PI / 2.0D,
                -Math.PI / 2.0D,
                Math.PI / 4.0D,
                -Math.PI / 4.0D,
                Math.PI
        };
        double[] radii = new double[] {0.85D, 1.15D, 1.45D};

        for (double radius : radii) {
            for (double angleOffset : angleOffsets) {
                Location candidate = current.clone();
                double angle = baseAngle + angleOffset;
                candidate.setX(current.getX() + (Math.cos(angle) * radius));
                candidate.setZ(current.getZ() + (Math.sin(angle) * radius));
                candidate = keepInsidePit(candidate);
                Location grounded = applyMovementGround(current, candidate);
                if (!isMovementBlocked(current, candidate, grounded)
                        && BotMath.distanceSquared2D(current, grounded) > 0.04D) {
                    return grounded;
                }
            }
        }

        return current.clone();
    }

    private boolean walkBotBackIntoPit(PitBot bot, long now) {
        if (bot == null || bot.getLocation() == null) {
            return false;
        }

        Location current = bot.getLocation();
        Location center = botSpawnService.getCenterLocation();
        if (center == null || current.getWorld() == null || current.getWorld() != center.getWorld()) {
            return false;
        }

        if (botSpawnService.isInsidePitArea(current)) {
            return false;
        }

        double maxReturnDistance = Math.max(botSpawnService.getMaxRadius() + 10.0D, botSpawnService.getCenterSpawnRadius() + 8.0D);
        if (BotMath.distanceSquared2D(current, center) > maxReturnDistance * maxReturnDistance) {
            return false;
        }

        bot.setBehaviorState(BotBehaviorState.RESET);
        bot.setTargetPlayerId(null);
        bot.setTargetLockedUntil(0L);
        bot.setTargetCommitUntil(0L);
        bot.setAttackChargeUntil(0L);
        bot.setAimSettleUntil(0L);
        bot.setCombatResetUntil(0L);
        bot.setCombatBurstUntil(0L);
        bot.setObstacleRepositionUntil(0L);
        bot.setSearchUntil(0L);
        bot.setHitReactUntil(0L);
        bot.setTargetLocation(center);

        Location exitStep = createSpawnExitStep(bot, current, center);
        if (BotMath.distanceSquared2D(current, exitStep) > 0.001D) {
            Location faced = applyFacing(bot, exitStep, center, botSettings.getCombatTurnSpeedDegrees(), 0.0D);
            movementController.reset(bot);
            bot.setLocation(faced);
        } else {
            Location fallback = applyFacing(bot, current.clone(), center, botSettings.getCombatTurnSpeedDegrees(), 0.0D);
            movementController.reset(bot);
            bot.setLocation(fallback);
        }

        bot.setNextTargetUpdateAt(now + randomBetween(200L, 420L));
        return true;
    }

    private Location createSpawnExitStep(PitBot bot, Location current, Location center) {
        if (bot == null || current == null || center == null || current.getWorld() != center.getWorld()) {
            return current == null ? null : current.clone();
        }

        double baseStep = Math.max(1.15D, bot.getSpeed() * 1.75D * Math.max(0.95D, bot.getSpeedMultiplier()));
        double[] stepMultipliers = new double[]{1.0D, 1.45D, 1.95D};

        for (double multiplier : stepMultipliers) {
            double step = baseStep * multiplier;

            Location forward = BotMath.moveTowards2D(current, center, step);
            Location groundedForward = applyGroundFix(forward);
            if (BotMath.distanceSquared2D(current, groundedForward) > 0.04D) {
                return groundedForward;
            }

            Location leapCandidate = forward.clone();
            leapCandidate.setY(current.getY() + 1.0D);
            Location groundedLeap = applyGroundFix(leapCandidate);
            if (BotMath.distanceSquared2D(current, groundedLeap) > 0.04D) {
                return groundedLeap;
            }
        }

        return current.clone();
    }

    private Location applyFacing(PitBot bot, Location location, Location lookAt, double maxTurnDegrees, double jitter) {
        if (bot == null || location == null || lookAt == null || location.getWorld() != lookAt.getWorld()) {
            return location;
        }

        float desiredYaw = BotMath.calculateYaw(location, lookAt);
        desiredYaw += (float) resolveLookYawOffset(bot, System.currentTimeMillis());
        if (jitter <= 0.0D) {
            jitter = 0.0D;
        }

        desiredYaw = (float) (desiredYaw + ((random.nextDouble() * jitter * 2.0D) - jitter));

        float yaw = smoothYaw(bot.getYaw(), desiredYaw, maxTurnDegrees);
        float desiredPitch = calculatePitch(location, lookAt, botSettings.getHeadLookMaxPitch());
        if (botSettings.getHeadPitchJitter() > 0.0D) {
            desiredPitch += (float) ((random.nextDouble() * botSettings.getHeadPitchJitter() * 2.0D) - botSettings.getHeadPitchJitter());
        }
        desiredPitch = clampPitch(desiredPitch, botSettings.getHeadLookMaxPitch());
        float pitch = smoothPitch(bot.getPitch(), desiredPitch, Math.max(2.0D, maxTurnDegrees * 0.45D));
        location.setYaw(yaw);
        location.setPitch(pitch);
        bot.setYaw(yaw);
        bot.setPitch(pitch);
        return location;
    }

    private float smoothYaw(float currentYaw, float targetYaw, double maxTurnDegrees) {
        double limitedTurn = Math.max(1.0D, maxTurnDegrees);
        double difference = normalizeYaw(targetYaw - currentYaw);

        if (difference > limitedTurn) {
            difference = limitedTurn;
        } else if (difference < -limitedTurn) {
            difference = -limitedTurn;
        }

        return (float) normalizeYaw(currentYaw + difference);
    }

    private float smoothPitch(float currentPitch, float targetPitch, double maxTurnDegrees) {
        double limitedTurn = Math.max(1.0D, maxTurnDegrees);
        double difference = targetPitch - currentPitch;

        if (difference > limitedTurn) {
            difference = limitedTurn;
        } else if (difference < -limitedTurn) {
            difference = -limitedTurn;
        }

        return clampPitch((float) (currentPitch + difference), botSettings.getHeadLookMaxPitch());
    }

    private float calculatePitch(Location from, Location to, double maxPitch) {
        if (from == null || to == null || from.getWorld() != to.getWorld()) {
            return 0.0F;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt((dx * dx) + (dz * dz));
        if (horizontalDistance <= 0.001D) {
            return 0.0F;
        }

        double eyeAdjustment = 1.2D;
        double dy = (to.getY() + eyeAdjustment) - (from.getY() + 1.62D);
        double pitch = -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        return clampPitch((float) pitch, maxPitch);
    }

    private float clampPitch(float pitch, double maxPitch) {
        float safeMax = (float) Math.max(0.0D, maxPitch);
        if (pitch > safeMax) {
            return safeMax;
        }

        if (pitch < -safeMax) {
            return -safeMax;
        }

        return pitch;
    }

    private void commitMovement(PitBot bot, Location current, Location desired, boolean engaged, boolean highPressure) {
        if (bot == null || desired == null) {
            return;
        }

        if (current == null || current.getWorld() != desired.getWorld()) {
            bot.setLocation(desired);
            movementController.reset(bot);
            return;
        }

        Location smoothed = movementController.smooth(bot, current, desired, engaged, highPressure);
        bot.setLocation(smoothed);
    }

    private double normalizeYaw(double yaw) {
        while (yaw <= -180.0D) {
            yaw += 360.0D;
        }

        while (yaw > 180.0D) {
            yaw -= 360.0D;
        }

        return yaw;
    }

    private void attackTarget(PitBot bot, Player target, long now) {
        if (bot == null || target == null || target.isDead() || target.getGameMode() == GameMode.CREATIVE) {
            if (bot != null) {
                bot.setAttackChargeUntil(0L);
                bot.setAimSettleUntil(0L);
            }
            return;
        }

        if (now < bot.getCombatResetUntil()) {
            bot.setAttackChargeUntil(0L);
            return;
        }

        if (now < bot.getNextAttackAt()) {
            bot.setAttackChargeUntil(0L);
            return;
        }

        if (!canBotHitTarget(bot, target, now)) {
            return;
        }

        boolean crowdedBrawlContext = isCrowdedBrawlContext(
                bot,
                target,
                bot.getLocation() == null || target.getLocation() == null
                        ? Double.MAX_VALUE
                        : bot.getLocation().distanceSquared(target.getLocation())
        );

        if (bot.getAttackChargeUntil() <= 0L) {
            long chargeDelay = botSettings.createAttackWindupMs(bot) + calculateGroupAttackDelay(bot, target);
            if (isBotTarget(target)) {
                chargeDelay = Math.max(8L, Math.round(chargeDelay * 0.22D));
            } else {
                chargeDelay = Math.max(12L, Math.round(chargeDelay * 0.30D));
            }
            if (crowdedBrawlContext) {
                chargeDelay = Math.max(isBotTarget(target) ? 4L : 8L, Math.round(chargeDelay * 0.50D));
            }
            chargeDelay = Math.max(4L, Math.round(chargeDelay * personalityAttackTempo(bot)));
            bot.setAttackChargeUntil(now + chargeDelay);
            return;
        }

        if (now < bot.getAttackChargeUntil()) {
            return;
        }

        if (shouldMicroWhiff(bot, target, now)) {
            bot.setAttackChargeUntil(0L);
            bot.setAimSettleUntil(now + createMicroWhiffSettleMs(bot));
            bot.setNextAttackAt(Math.max(bot.getNextAttackAt(), now + createMicroWhiffRecoveryMs(bot)));
            botPacketService.swingArmForAll(bot);
            return;
        }

        bot.setAttackChargeUntil(0L);
        long nextAttackDelay = botSettings.createAttackCooldownMs(bot);
        if (isBotTarget(target)) {
            nextAttackDelay = Math.max(82L, Math.round(nextAttackDelay * 0.30D));
        } else {
            nextAttackDelay = Math.max(96L, Math.round(nextAttackDelay * 0.34D));
        }
        if (crowdedBrawlContext) {
            nextAttackDelay = Math.max(isBotTarget(target) ? 64L : 74L, Math.round(nextAttackDelay * 0.72D));
        }
        nextAttackDelay = Math.max(58L, Math.round(nextAttackDelay * personalityAttackTempo(bot)));
        bot.setNextAttackAt(now + nextAttackDelay);
        bot.setTargetLockedUntil(Math.max(bot.getTargetLockedUntil(), now + scaleFocusDuration(bot, Math.max(900L, botSettings.getAggroMemoryMs() / 3L))));
        bot.setTargetCommitUntil(Math.max(bot.getTargetCommitUntil(), now + scaleFocusDuration(bot, Math.max(260L, botSettings.createTargetCommitMs() / 2L))));
        botPacketService.swingArmForAll(bot);

        double safeHealthFloor = isBotTarget(target) ? 0.0D : botSettings.getEffectiveSafePlayerHealthFloor();
        double currentHealth = target.getHealth();
        if (currentHealth <= safeHealthFloor) {
            return;
        }

        queueCombatRhythm(bot, now);

        if (botEntityService != null && botEntityService.damageTarget(bot, target)) {
            applyBotAttackKnockback(bot, target);
            return;
        }

        boolean useBukkitDamage = !botSettings.isDirectPlayerDamage() || safeHealthFloor <= 0.0D;
        if (!useBukkitDamage) {
            target.setHealth(Math.max(safeHealthFloor, currentHealth - bot.getAttackDamage()));
            applyBotAttackKnockback(bot, target);
            return;
        }

        if (safeHealthFloor > 0.0D && currentHealth <= safeHealthFloor + bot.getAttackDamage()) {
            target.setHealth(safeHealthFloor);
            applyBotAttackKnockback(bot, target);
            return;
        }

        target.damage(bot.getAttackDamage());
        applyBotAttackKnockback(bot, target);
    }

    private void queueCombatRhythm(PitBot bot, long now) {
        if (bot == null || !botSettings.isCombatRhythmEnabled()) {
            return;
        }

        long resetUntil = now + scaleTierDuration(
                bot,
                randomBetween(botSettings.getCombatResetMinMs(), botSettings.getCombatResetMaxMs()),
                1.20D,
                1.0D,
                0.78D
        );
        long burstUntil = resetUntil + scaleTierDuration(
                bot,
                randomBetween(botSettings.getCombatBurstMinMs(), botSettings.getCombatBurstMaxMs()),
                0.88D,
                1.0D,
                1.22D
        );
        bot.setAimSettleUntil(0L);
        bot.setCombatResetUntil(resetUntil);
        bot.setCombatBurstUntil(burstUntil);
    }

    private double resolveChaseStep(PitBot bot, long now, boolean directLineOfSight, boolean allowDirectChase) {
        if (bot == null) {
            return 0.0D;
        }

        double chaseStep = bot.getSpeed() * bot.getSpeedMultiplier() * stateTransitionFactor(bot, now) * 1.20D;
        if (allowDirectChase) {
            chaseStep *= 1.24D;
        }
        chaseStep *= tierChaseMultiplier(bot);
        chaseStep *= personalityChaseBias(bot);
        chaseStep *= (1.0D + movementCadence(bot, now, 0.07D, 0.05D, 0.035D));
        if (!botSettings.isCombatRhythmEnabled()) {
            return chaseStep;
        }

        if (now < bot.getCombatResetUntil()) {
            return chaseStep * 0.97D;
        }

        if (now < bot.getCombatBurstUntil()) {
            return chaseStep + ((botSettings.getCombatForwardBurstStrength() * 1.12D) * tierBurstMultiplier(bot));
        }

        if (!directLineOfSight) {
            return chaseStep * (0.98D * tierNoLineOfSightMultiplier(bot));
        }

        return chaseStep;
    }

    private double resolveRoamStep(PitBot bot) {
        if (bot == null) {
            return 0.0D;
        }

        long now = System.currentTimeMillis();
        double roamStep = bot.getSpeed() * 1.06D * bot.getSpeedMultiplier() * stateTransitionFactor(bot, now);
        roamStep *= personalityRoamBias(bot);
        roamStep *= (1.0D + movementCadence(bot, now, 0.08D, 0.06D, 0.04D));
        return Math.max(0.34D, roamStep);
    }

    private double resolveCombatForwardAdjustment(PitBot bot, double distance, long now, boolean directLineOfSight, boolean crowdedBrawlContext) {
        if (bot == null || distance <= 0.001D) {
            return 0.0D;
        }

        if (botSettings.isCombatRhythmEnabled()) {
            if (now < bot.getCombatResetUntil()) {
                if (!crowdedBrawlContext) {
                    return -(botSettings.getCombatBackoffStrength() * 0.42D) * tierBackoffMultiplier(bot);
                }
                return Math.max(0.06D, botSettings.getCombatForwardBurstStrength() * 0.45D);
            }

            if (now < bot.getCombatBurstUntil()) {
                return (botSettings.getCombatForwardBurstStrength() * 1.26D) * tierBurstMultiplier(bot);
            }
        }

        if (!directLineOfSight) {
            if (crowdedBrawlContext) {
                return Math.max(0.04D, bot.getSpeed() * 0.20D * bot.getSpeedMultiplier());
            }
            return 0.0D;
        }

        double desiredDistance = Math.max(0.8D, bot.getPreferredCombatDistance());
        if (crowdedBrawlContext) {
            desiredDistance = Math.max(0.72D, desiredDistance * 0.76D);
        }
        double delta = distance - desiredDistance;
        if (Math.abs(delta) <= 0.08D) {
            return crowdedBrawlContext ? 0.04D : 0.0D;
        }

        double maxAdjust = Math.max(0.10D, bot.getSpeed() * 1.08D * bot.getSpeedMultiplier() * tierBurstMultiplier(bot));
        if (crowdedBrawlContext) {
            maxAdjust = Math.max(maxAdjust, bot.getSpeed() * 1.18D * bot.getSpeedMultiplier());
        }
        double pressure = delta * 0.72D;
        if (bot.getAttackChargeUntil() > now || now < bot.getNextAttackAt()) {
            pressure += Math.copySign(0.05D, delta);
        }
        if (crowdedBrawlContext) {
            pressure += 0.06D;
        }
        return Math.max(-maxAdjust, Math.min(maxAdjust, pressure));
    }

    private boolean canBotHitTarget(PitBot bot, Player target, long now) {
        if (bot == null || target == null || bot.getLocation() == null || target.getLocation() == null) {
            return false;
        }

        Location botLocation = bot.getLocation();
        Location targetLocation = target.getLocation();
        if (botLocation.getWorld() != targetLocation.getWorld()) {
            return false;
        }

        boolean botTarget = isBotTarget(target);
        double distanceSquared = botLocation.distanceSquared(targetLocation);
        boolean crowdedBrawlContext = isCrowdedBrawlContext(bot, target, distanceSquared);
        boolean stickyCloseFight = target.getUniqueId().equals(bot.getTargetPlayerId())
                && hasRecentLineOfSight(bot, target, now)
                && distanceSquared <= Math.pow(bot.getPreferredCombatDistance() + (botTarget ? 0.90D : 0.65D), 2);
        if (crowdedBrawlContext) {
            stickyCloseFight = true;
        }
        double reach = bot.getAttackRange() + (botTarget ? 0.92D : 0.35D) + tierReachBonus(bot);
        if (stickyCloseFight) {
            reach += botTarget ? 0.30D : 0.18D;
        }
        if (crowdedBrawlContext) {
            reach += botTarget ? 0.26D : 0.20D;
        }
        double closeBotRange = bot.getAttackRange() + 1.35D;

        boolean hasLineOfSight = crowdedBrawlContext || hasRecentLineOfSight(bot, target, now);
        if (!hasLineOfSight
                && (!botTarget || distanceSquared > closeBotRange * closeBotRange)) {
            if (!botTarget) {
                bot.setAimSettleUntil(0L);
            }
            return false;
        }

        if (distanceSquared > reach * reach) {
            double hardResetReach = reach + 0.90D;
            if (distanceSquared > hardResetReach * hardResetReach) {
                bot.setAimSettleUntil(0L);
            }
            return false;
        }

        float desiredYaw = BotMath.calculateYaw(botLocation, targetLocation);
        double yawDelta = Math.abs(normalizeYaw(desiredYaw - bot.getYaw()));
        double maxAngle = botSettings.getAttackFacingMaxAngle() + (botTarget ? 32.0D : 16.0D) + tierFacingBonus(bot);
        if (stickyCloseFight) {
            maxAngle += botTarget ? 16.0D : 10.0D;
        }
        if (crowdedBrawlContext) {
            maxAngle += botTarget ? 20.0D : 12.0D;
        }
        maxAngle /= personalityAimTightness(bot);
        if (yawDelta > maxAngle) {
            if (yawDelta > maxAngle + 24.0D) {
                bot.setAimSettleUntil(0L);
            } else if (bot.getAimSettleUntil() <= 0L) {
                long partialSettleMs = Math.max(45L, scaleTierDuration(bot, botSettings.createAimSettleMs() / 2L, 1.20D, 1.0D, 0.82D));
                if (botTarget) {
                    partialSettleMs = Math.max(30L, Math.round(partialSettleMs * 0.55D));
                } else {
                    partialSettleMs = Math.max(35L, Math.round(partialSettleMs * 0.72D));
                }
                bot.setAimSettleUntil(now + partialSettleMs);
            }
            return false;
        }

        if (bot.getAimSettleUntil() <= 0L) {
            long aimSettleMs = botSettings.createAimSettleMs(bot);
            if (botTarget) {
                aimSettleMs = Math.max(32L, Math.round(aimSettleMs * 0.52D));
            } else {
                aimSettleMs = Math.max(40L, Math.round(aimSettleMs * 0.70D));
            }
            if (stickyCloseFight) {
                aimSettleMs = Math.max(20L, Math.round(aimSettleMs * 0.58D));
            }
            if (crowdedBrawlContext) {
                aimSettleMs = Math.max(botTarget ? 12L : 16L, Math.round(aimSettleMs * 0.62D));
            }
            bot.setAimSettleUntil(now + aimSettleMs);
            return false;
        }

        if (now < bot.getAimSettleUntil()) {
            return false;
        }

        return true;
    }

    private boolean isBotTarget(Player target) {
        return target != null && botEntityService != null && botEntityService.isBot(target);
    }

    private long calculateGroupAttackDelay(PitBot bot, Player target) {
        if (bot == null || target == null || botEntityService == null) {
            return 0L;
        }

        int focusedOthers = botEntityService.countLivingBotsTargeting(target.getUniqueId(), bot.getUniqueId());
        if (focusedOthers <= 0) {
            return 0L;
        }

        if (isBotTarget(target)) {
            int waveCount = Math.min(2, focusedOthers + 1);
            int slot = Math.abs(bot.getUniqueId().hashCode()) % waveCount;
            long baseDelay = scaleTierDuration(bot, 2L + focusedOthers, 1.0D, 1.0D, 0.76D);
            return Math.min(10L, slot * baseDelay);
        }

        int waveCount = Math.min(3, focusedOthers + 1);
        int slot = Math.abs(bot.getUniqueId().hashCode()) % waveCount;
        long baseDelay = scaleTierDuration(bot, 6L + (focusedOthers * 2L), 1.02D, 1.0D, 0.78D);
        return Math.min(22L, slot * baseDelay);
    }

    private boolean shouldMicroWhiff(PitBot bot, Player target, long now) {
        if (bot == null || target == null || bot.getLocation() == null || target.getLocation() == null) {
            return false;
        }

        if (now < bot.getRespawnSettleUntil()) {
            return false;
        }

        Location botLocation = bot.getLocation();
        Location targetLocation = target.getLocation();
        if (botLocation.getWorld() != targetLocation.getWorld()) {
            return false;
        }

        double chance = tierMicroWhiffChance(bot);
        if (chance <= 0.0D) {
            return false;
        }

        if (isBotTarget(target)) {
            chance *= 0.28D;
        }

        double distance = Math.sqrt(botLocation.distanceSquared(targetLocation));
        double preferredDistance = Math.max(0.8D, bot.getPreferredCombatDistance());
        double distanceOffset = Math.abs(distance - preferredDistance);
        if (distanceOffset > 0.22D) {
            chance += Math.min(0.08D, distanceOffset * 0.06D);
        }

        float desiredYaw = BotMath.calculateYaw(botLocation, targetLocation);
        double yawDelta = Math.abs(normalizeYaw(desiredYaw - bot.getYaw()));
        double maxAngle = botSettings.getAttackFacingMaxAngle() + tierFacingBonus(bot);
        if (yawDelta > (maxAngle * 0.72D)) {
            chance += 0.05D;
        }

        if ((now - bot.getLastDirectLineOfSightAt()) > 320L) {
            chance += 0.03D;
        }

        if (target.getUniqueId().equals(bot.getTargetPlayerId())) {
            chance *= 0.9D;
        }

        return random.nextDouble() < Math.min(0.22D, chance);
    }

    private double tierMicroWhiffChance(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.11D;
        }

        if (tier == BotTier.TRYHARD) {
            return 0.025D;
        }

        return 0.055D;
    }

    private long createMicroWhiffSettleMs(PitBot bot) {
        return scaleTierDuration(bot, randomBetween(55L, 115L), 1.18D, 1.0D, 0.84D);
    }

    private long createMicroWhiffRecoveryMs(PitBot bot) {
        return scaleTierDuration(bot, randomBetween(80L, 150L), 1.16D, 1.0D, 0.86D);
    }

    private double movementCadence(PitBot bot, long now, double easyAmplitude, double normalAmplitude, double tryhardAmplitude) {
        if (bot == null) {
            return 0.0D;
        }

        double amplitude;
        BotTier tier = bot.getTier();
        if (tier == BotTier.EASY) {
            amplitude = easyAmplitude;
        } else if (tier == BotTier.TRYHARD) {
            amplitude = tryhardAmplitude;
        } else {
            amplitude = normalAmplitude;
        }

        amplitude *= personalityCadenceBias(bot);
        double seed = (Math.abs(bot.getUniqueId().hashCode()) % 1000) / 1000.0D;
        double phase = (seed * Math.PI * 2.0D) + personalityPhaseOffset(bot);
        double cadence = Math.sin((now / 340.0D) + phase);
        return cadence * amplitude;
    }

    private double stateTransitionFactor(PitBot bot, long now) {
        if (bot == null) {
            return 1.0D;
        }

        long stateAge = Math.max(0L, now - bot.getBehaviorStateChangedAt());
        BotBehaviorState state = bot.getBehaviorState();
        long easeDuration = stateEaseDuration(bot, state);
        if (easeDuration <= 0L || stateAge >= easeDuration) {
            return 1.0D;
        }

        double progress = Math.max(0.0D, Math.min(1.0D, stateAge / (double) easeDuration));
        double eased = 1.0D - Math.pow(1.0D - progress, 2.0D);
        double startFactor = stateEntrySpeedFactor(state);
        return startFactor + ((1.0D - startFactor) * eased);
    }

    private long stateEaseDuration(PitBot bot, BotBehaviorState state) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (state == BotBehaviorState.CHASE) {
            return tier == BotTier.EASY ? 360L : tier == BotTier.TRYHARD ? 180L : 260L;
        }

        if (state == BotBehaviorState.SEARCH) {
            return tier == BotTier.EASY ? 420L : tier == BotTier.TRYHARD ? 240L : 320L;
        }

        if (state == BotBehaviorState.ROAM) {
            return tier == BotTier.EASY ? 300L : tier == BotTier.TRYHARD ? 170L : 230L;
        }

        if (state == BotBehaviorState.ATTACK) {
            return tier == BotTier.EASY ? 220L : tier == BotTier.TRYHARD ? 120L : 170L;
        }

        return 0L;
    }

    private double stateEntrySpeedFactor(BotBehaviorState state) {
        if (state == BotBehaviorState.CHASE) {
            return 0.74D;
        }

        if (state == BotBehaviorState.SEARCH) {
            return 0.80D;
        }

        if (state == BotBehaviorState.ROAM) {
            return 0.86D;
        }

        if (state == BotBehaviorState.ATTACK) {
            return 0.88D;
        }

        return 1.0D;
    }

    private long scaleTierDuration(PitBot bot, long base, double easyMultiplier, double normalMultiplier, double tryhardMultiplier) {
        double multiplier;
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            multiplier = easyMultiplier;
        } else if (tier == BotTier.TRYHARD) {
            multiplier = tryhardMultiplier;
        } else {
            multiplier = normalMultiplier;
        }

        double reactionBias = bot == null ? 1.0D : bot.getReactionTimeMultiplier();
        return Math.max(40L, Math.round(base * multiplier * reactionBias));
    }

    private double tierChaseMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.94D;
        }

        if (tier == BotTier.TRYHARD) {
            return 1.08D;
        }

        return 1.0D;
    }

    private double personalityChaseBias(PitBot bot) {
        return bot == null || bot.getPersonality() == null ? 1.0D : bot.getPersonality().getChaseBias();
    }

    private double personalityRoamBias(PitBot bot) {
        return bot == null || bot.getPersonality() == null ? 1.0D : bot.getPersonality().getRoamBias();
    }

    private double personalityAttackTempo(PitBot bot) {
        return bot == null || bot.getPersonality() == null ? 1.0D : bot.getPersonality().getAttackTempoMultiplier();
    }

    private double personalityCadenceBias(PitBot bot) {
        return bot == null || bot.getPersonality() == null ? 1.0D : bot.getPersonality().getCadenceBias();
    }

    private double personalityPhaseOffset(PitBot bot) {
        return bot == null || bot.getPersonality() == null ? 0.0D : bot.getPersonality().getPhaseOffset();
    }

    private double personalityAimTightness(PitBot bot) {
        return bot == null || bot.getPersonality() == null
                ? 1.0D
                : Math.max(0.72D, Math.min(1.20D, bot.getPersonality().getAimJitterMultiplier()));
    }

    private double resolvePersonalityJitter(PitBot bot, double baseJitter) {
        if (baseJitter <= 0.0D || bot == null || bot.getPersonality() == null) {
            return baseJitter;
        }

        return baseJitter * bot.getPersonality().getAimJitterMultiplier();
    }

    private long scaleFocusDuration(PitBot bot, long base) {
        double focusBias = bot == null || bot.getPersonality() == null ? 1.0D : bot.getPersonality().getFocusBias();
        return Math.max(80L, Math.round(base * focusBias));
    }

    private double tierNoLineOfSightMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.92D;
        }

        if (tier == BotTier.TRYHARD) {
            return 1.08D;
        }

        return 1.0D;
    }

    private double tierBurstMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.88D;
        }

        if (tier == BotTier.TRYHARD) {
            return 1.16D;
        }

        return 1.0D;
    }

    private double tierBackoffMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 1.14D;
        }

        if (tier == BotTier.TRYHARD) {
            return 0.86D;
        }

        return 1.0D;
    }

    private double tierReachBonus(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return -0.04D;
        }

        if (tier == BotTier.TRYHARD) {
            return 0.10D;
        }

        return 0.0D;
    }

    private double tierFacingBonus(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return -6.0D;
        }

        if (tier == BotTier.TRYHARD) {
            return 10.0D;
        }

        return 0.0D;
    }

    private double tierRepathMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.88D;
        }

        if (tier == BotTier.TRYHARD) {
            return 1.22D;
        }

        return 1.0D;
    }

    private double tierCornerPeekMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.82D;
        }

        if (tier == BotTier.TRYHARD) {
            return 1.28D;
        }

        return 1.0D;
    }

    private double tierRetreatMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 1.08D;
        }

        if (tier == BotTier.TRYHARD) {
            return 0.72D;
        }

        return 1.0D;
    }

    private double tierCrowdAvoidanceMultiplier(PitBot bot, boolean combatBias) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return combatBias ? 1.18D : 1.10D;
        }

        if (tier == BotTier.TRYHARD) {
            return combatBias ? 0.78D : 0.88D;
        }

        return 1.0D;
    }

    private double tierCrowdLaneMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 0.88D;
        }

        if (tier == BotTier.TRYHARD) {
            return 1.18D;
        }

        return 1.0D;
    }

    private double tierSupportRadiusMultiplier(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 1.10D;
        }

        if (tier == BotTier.TRYHARD) {
            return 0.92D;
        }

        return 1.0D;
    }

    private boolean isCrowdedBrawlContext(PitBot bot, Player target, double distanceSquared) {
        if (bot == null || target == null || bot.getLocation() == null || target.getLocation() == null) {
            return false;
        }

        if (bot.getLocation().getWorld() != target.getLocation().getWorld()) {
            return false;
        }

        double brawlRange = bot.getAttackRange() + (isBotTarget(target) ? 2.55D : 2.90D);
        if (distanceSquared > brawlRange * brawlRange) {
            return false;
        }

        if (botEntityService == null) {
            return false;
        }

        double pressureRadius = isBotTarget(target) ? 3.30D : 3.70D;
        int nearbyFighters = botEntityService.countLivingBotsNear(target.getLocation(), pressureRadius, bot.getUniqueId());
        if (nearbyFighters > 0) {
            return true;
        }

        return targetingService.hasNearbyEngagement(bot);
    }

    private boolean hasDirectLineOfSight(PitBot bot, Player target) {
        if (bot == null || target == null || botEntityService == null) {
            return true;
        }

        Player botPlayer = botEntityService.getBukkitPlayer(bot);
        if (botPlayer == null || botPlayer.getWorld() != target.getWorld()) {
            return true;
        }

        try {
            return botPlayer.hasLineOfSight(target);
        } catch (Exception exception) {
            return true;
        }
    }

    private boolean hasRecentLineOfSight(PitBot bot, Player target, long now) {
        if (bot == null || target == null || !botSettings.isLineOfSightRequired()) {
            return true;
        }

        if (hasDirectLineOfSight(bot, target)) {
            bot.setLastSeenTargetAt(now);
            bot.setLastSeenTargetLocation(target.getLocation());
            return true;
        }

        return (now - bot.getLastSeenTargetAt()) <= botSettings.getLineOfSightGraceMs();
    }

    private boolean shouldSearch(PitBot bot, long now) {
        if (bot == null || bot.getLocation() == null || bot.getLastSeenTargetLocation() == null) {
            return false;
        }

        if (bot.getLocation().getWorld() != bot.getLastSeenTargetLocation().getWorld()) {
            return false;
        }

        if ((now - bot.getLastSeenTargetAt()) > (botSettings.getLineOfSightGraceMs() + 1200L)) {
            return false;
        }

        return bot.getSearchUntil() <= 0L || now < bot.getSearchUntil();
    }

    private void beginSearchReset(PitBot bot, long now) {
        if (bot == null) {
            return;
        }

        bot.setTargetPlayerId(null);
        bot.setTargetLockedUntil(0L);
        bot.setTargetCommitUntil(0L);
        bot.setAttackChargeUntil(0L);
        bot.setAimSettleUntil(0L);
        bot.setObstacleRepositionUntil(0L);
        bot.setBehaviorState(BotBehaviorState.SEARCH);
        bot.setSearchUntil(now + botSettings.createSearchDurationMs());
    }

    private boolean shouldForceSearchReset(PitBot bot, double distanceSquared, boolean directLineOfSight, long now) {
        if (bot == null || directLineOfSight) {
            return false;
        }

        double resetDistance = bot.getAttackRange() + tierSearchResetDistanceBonus(bot);
        if (distanceSquared <= resetDistance * resetDistance) {
            return false;
        }

        long lastSeenTargetAt = bot.getLastSeenTargetAt();
        if (lastSeenTargetAt <= 0L) {
            return true;
        }

        long patienceMs = scaleTierDuration(
                bot,
                botSettings.getLineOfSightGraceMs() + 850L,
                0.82D,
                1.0D,
                1.28D
        );
        return (now - lastSeenTargetAt) >= patienceMs;
    }

    private Location resolveSteeringTarget(PitBot bot, Player target, Location fallbackTarget, long now) {
        if (bot == null) {
            return fallbackTarget == null ? null : fallbackTarget.clone();
        }

        Location steeringTarget = fallbackTarget == null ? null : fallbackTarget.clone();
        if (steeringTarget == null) {
            return null;
        }

        if (target != null
                && bot.getLocation() != null
                && target.getWorld() == steeringTarget.getWorld()
                && target.getVelocity() != null) {
            steeringTarget = applyTargetPrediction(bot, target, steeringTarget);
        }

        Location remembered = bot.getLastSeenTargetLocation();
        if (remembered != null
                && remembered.getWorld() == steeringTarget.getWorld()
                && (now - bot.getLastSeenTargetAt()) <= (botSettings.getLineOfSightGraceMs() + 400L)) {
            double blend = target == null ? 1.0D : 0.34D;
            steeringTarget.setX((steeringTarget.getX() * (1.0D - blend)) + (remembered.getX() * blend));
            steeringTarget.setY((steeringTarget.getY() * (1.0D - blend)) + (remembered.getY() * blend));
            steeringTarget.setZ((steeringTarget.getZ() * (1.0D - blend)) + (remembered.getZ() * blend));
        }

        return steeringTarget;
    }

    private Location applyTargetPrediction(PitBot bot, Player target, Location steeringTarget) {
        if (bot == null || target == null || steeringTarget == null || bot.getLocation() == null) {
            return steeringTarget == null ? null : steeringTarget.clone();
        }

        Vector velocity = target.getVelocity();
        double horizontalSpeed = Math.sqrt((velocity.getX() * velocity.getX()) + (velocity.getZ() * velocity.getZ()));
        if (horizontalSpeed <= 0.02D) {
            return steeringTarget.clone();
        }

        double distance = Math.sqrt(bot.getLocation().distanceSquared(steeringTarget));
        double leadMultiplier = 0.55D;
        if (bot.getTier() == BotTier.EASY) {
            leadMultiplier = 0.34D;
        } else if (bot.getTier() == BotTier.TRYHARD) {
            leadMultiplier = 0.72D;
        }

        double lead = Math.min(1.15D, Math.max(0.14D, (distance / 6.8D) * leadMultiplier));
        Location predicted = steeringTarget.clone();
        predicted.setX(predicted.getX() + (velocity.getX() * lead));
        predicted.setZ(predicted.getZ() + (velocity.getZ() * lead));
        return predicted;
    }

    private double resolveLookYawOffset(PitBot bot, long now) {
        if (bot == null) {
            return 0.0D;
        }

        if (now >= bot.getNextLookOffsetAt()) {
            BotBehaviorState state = bot.getBehaviorState();
            double amplitude = stateLookAmplitude(bot, state);
            long minInterval = stateLookMinInterval(state);
            long maxInterval = stateLookMaxInterval(state);

            if (amplitude <= 0.0D) {
                bot.setLookYawOffset(0.0D);
            } else {
                bot.setLookYawOffset((random.nextDouble() * amplitude * 2.0D) - amplitude);
            }

            bot.setNextLookOffsetAt(now + randomBetween(minInterval, maxInterval));
        }

        return bot.getLookYawOffset();
    }

    private double stateLookAmplitude(PitBot bot, BotBehaviorState state) {
        double baseAmplitude;
        if (state == BotBehaviorState.IDLE) {
            baseAmplitude = 16.0D;
        } else if (state == BotBehaviorState.ROAM) {
            baseAmplitude = 11.5D;
        } else if (state == BotBehaviorState.SEARCH) {
            baseAmplitude = 9.0D;
        } else if (state == BotBehaviorState.CHASE) {
            baseAmplitude = 4.0D;
        } else if (state == BotBehaviorState.ATTACK) {
            baseAmplitude = 1.65D;
        } else {
            baseAmplitude = 3.0D;
        }

        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return baseAmplitude * 1.14D;
        }

        if (tier == BotTier.TRYHARD) {
            return baseAmplitude * 0.82D;
        }

        return baseAmplitude;
    }

    private long stateLookMinInterval(BotBehaviorState state) {
        if (state == BotBehaviorState.IDLE) {
            return 340L;
        }

        if (state == BotBehaviorState.ROAM) {
            return 260L;
        }

        if (state == BotBehaviorState.SEARCH) {
            return 180L;
        }

        if (state == BotBehaviorState.CHASE) {
            return 150L;
        }

        if (state == BotBehaviorState.ATTACK) {
            return 110L;
        }

        return 160L;
    }

    private long stateLookMaxInterval(BotBehaviorState state) {
        if (state == BotBehaviorState.IDLE) {
            return 1050L;
        }

        if (state == BotBehaviorState.ROAM) {
            return 780L;
        }

        if (state == BotBehaviorState.SEARCH) {
            return 520L;
        }

        if (state == BotBehaviorState.CHASE) {
            return 340L;
        }

        if (state == BotBehaviorState.ATTACK) {
            return 220L;
        }

        return 360L;
    }

    private double tierSearchResetDistanceBonus(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return 2.35D;
        }

        if (tier == BotTier.TRYHARD) {
            return 3.45D;
        }

        return 2.85D;
    }

    private Location applyGroupSupportTarget(PitBot bot, List<PitBot> bots, Player target, Location steeringTarget) {
        if (bot == null
                || bots == null
                || target == null
                || steeringTarget == null
                || steeringTarget.getWorld() == null
                || bot.getLocation() == null
                || bot.getLocation().getWorld() != steeringTarget.getWorld()) {
            return steeringTarget;
        }

        if (!isBotTarget(target)) {
            return steeringTarget;
        }

        UUID targetId = target.getUniqueId();
        int sameTargetCount = 0;
        int slotIndex = -1;

        for (PitBot other : bots) {
            if (other == null || other.isDead() || other.getTargetPlayerId() == null) {
                continue;
            }

            if (!targetId.equals(other.getTargetPlayerId())) {
                continue;
            }

            if (other.getLocation() == null || other.getLocation().getWorld() != steeringTarget.getWorld()) {
                continue;
            }

            if (other == bot) {
                slotIndex = sameTargetCount;
            }

            sameTargetCount++;
        }

        if (sameTargetCount <= 1 || slotIndex < 0) {
            return steeringTarget;
        }

        Location current = bot.getLocation();
        double currentDistanceSquared = current.distanceSquared(target.getLocation());
        double surroundRange = (bot.getAttackRange() + 4.2D) * (bot.getAttackRange() + 4.2D);
        if (currentDistanceSquared > surroundRange) {
            return steeringTarget;
        }

        double baseAngle = ((Math.PI * 2.0D) / sameTargetCount) * slotIndex;
        double hashOffset = (Math.abs(bot.getUniqueId().hashCode()) % 360) * (Math.PI / 180.0D) * 0.18D;
        double angle = baseAngle + hashOffset;
        double radius = Math.max(0.95D, Math.min(1.65D, bot.getPreferredCombatDistance() + 0.15D + ((sameTargetCount - 1) * 0.10D)));
        radius *= tierSupportRadiusMultiplier(bot);
        if (bot.getPersonality() != null) {
            radius *= Math.max(0.78D, Math.min(1.24D, bot.getPersonality().getSpacingBias() * bot.getPersonality().getCrowdBias()));
        }

        Location supported = target.getLocation().clone();
        supported.setX(supported.getX() + (Math.cos(angle) * radius));
        supported.setZ(supported.getZ() + (Math.sin(angle) * radius));
        supported.setY(steeringTarget.getY());
        return supported;
    }

    private Location applyObstacleReposition(PitBot bot,
                                             List<PitBot> bots,
                                             Location current,
                                             Location steeringTarget,
                                             long now,
                                             boolean blocked) {
        if (!botSettings.isObstacleRepositionEnabled()
                || bot == null
                || current == null
                || steeringTarget == null
                || current.getWorld() != steeringTarget.getWorld()) {
            return current == null ? null : current.clone();
        }

        double dx = steeringTarget.getX() - current.getX();
        double dz = steeringTarget.getZ() - current.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance <= 0.001D) {
            return current.clone();
        }

        if (now >= bot.getObstacleRepositionUntil()) {
            bot.setObstacleStrafeDirection(random.nextBoolean() ? 1.0D : -1.0D);
            bot.setObstacleRepositionUntil(now + botSettings.createObstacleRepositionMs());
        }

        Location primaryAttempt = createObstacleRepositionAttempt(
                bot,
                bots,
                current,
                dx,
                dz,
                distance,
                bot.getObstacleStrafeDirection(),
                blocked
        );
        if (BotMath.distanceSquared2D(current, primaryAttempt) > 0.001D) {
            return primaryAttempt;
        }

        double oppositeDirection = -bot.getObstacleStrafeDirection();
        Location secondaryAttempt = createObstacleRepositionAttempt(
                bot,
                bots,
                current,
                dx,
                dz,
                distance,
                oppositeDirection,
                blocked
        );
        if (BotMath.distanceSquared2D(current, secondaryAttempt) > 0.001D) {
            bot.setObstacleStrafeDirection(oppositeDirection);
            return secondaryAttempt;
        }

        return current.clone();
    }

    private Location createObstacleRepositionAttempt(PitBot bot,
                                                     List<PitBot> bots,
                                                     Location current,
                                                     double dx,
                                                     double dz,
                                                     double distance,
                                                     double direction,
                                                     boolean blocked) {
        Location candidate = current.clone();
        double lateral = botSettings.getObstacleRepositionStrength()
                * Math.max(0.75D, bot.getSpeedMultiplier())
                * tierRepathMultiplier(bot);
        double forward = botSettings.getObstacleCornerPeekStrength()
                * (blocked ? 1.00D : 0.65D)
                * tierCornerPeekMultiplier(bot);
        double retreat = blocked ? Math.min(0.10D, bot.getSpeed() * 0.45D * tierRetreatMultiplier(bot)) : 0.0D;

        candidate.setX(candidate.getX() + ((-dz / distance) * lateral * direction));
        candidate.setZ(candidate.getZ() + ((dx / distance) * lateral * direction));
        candidate.setX(candidate.getX() + ((dx / distance) * forward));
        candidate.setZ(candidate.getZ() + ((dz / distance) * forward));
        if (retreat > 0.0D) {
            candidate.setX(candidate.getX() - ((dx / distance) * retreat));
            candidate.setZ(candidate.getZ() - ((dz / distance) * retreat));
        }

        if (blocked && bot.getLastSeenTargetLocation() != null && bot.getLastSeenTargetLocation().getWorld() == current.getWorld()) {
            Location lastSeen = bot.getLastSeenTargetLocation();
            double seenDx = lastSeen.getX() - current.getX();
            double seenDz = lastSeen.getZ() - current.getZ();
            double seenDistance = Math.sqrt((seenDx * seenDx) + (seenDz * seenDz));
            if (seenDistance > 0.001D) {
                double commit = 0.08D * tierCornerPeekMultiplier(bot);
                candidate.setX(candidate.getX() + ((seenDx / seenDistance) * commit));
                candidate.setZ(candidate.getZ() + ((seenDz / seenDistance) * commit));
            }
        }

        candidate = applySeparation(bot, bots, candidate);
        candidate = keepInsidePit(candidate);
        Location grounded = applyMovementGround(current, candidate);
        return isMovementBlocked(current, candidate, grounded) ? current.clone() : grounded;
    }

    private void applyBotAttackKnockback(PitBot bot, Player target) {
        if (bot == null || target == null || bot.getLocation() == null || target.getLocation() == null) {
            return;
        }

        Location botLocation = bot.getLocation();
        Location targetLocation = target.getLocation();
        if (botLocation.getWorld() != targetLocation.getWorld()) {
            return;
        }

        double dx = targetLocation.getX() - botLocation.getX();
        double dz = targetLocation.getZ() - botLocation.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance <= 0.001D) {
            return;
        }

        double horizontal = botSettings.getBotHitKnockbackHorizontal();
        ItemStack heldItem = bot.getMainHandItem();
        if (heldItem != null) {
            int knockbackLevel = heldItem.getEnchantmentLevel(Enchantment.KNOCKBACK);
            if (knockbackLevel > 0) {
                horizontal += knockbackLevel * Math.max(0.10D, botSettings.getKnockbackEnchantBonus());
            }
        }

        Vector velocity = target.getVelocity();
        velocity.setX(clampAttackKnockbackHorizontal((velocity.getX() * 0.5D) + ((dx / distance) * horizontal)));
        velocity.setZ(clampAttackKnockbackHorizontal((velocity.getZ() * 0.5D) + ((dz / distance) * horizontal)));
        velocity.setY(clampAttackKnockbackVertical(Math.max(Math.min(velocity.getY(), 0.28D), botSettings.getBotHitKnockbackVertical())));
        target.setVelocity(velocity);
    }

    private double clampAttackKnockbackHorizontal(double value) {
        if (value > MAX_SAFE_HORIZONTAL_ATTACK_KNOCKBACK_VELOCITY) {
            return MAX_SAFE_HORIZONTAL_ATTACK_KNOCKBACK_VELOCITY;
        }

        if (value < -MAX_SAFE_HORIZONTAL_ATTACK_KNOCKBACK_VELOCITY) {
            return -MAX_SAFE_HORIZONTAL_ATTACK_KNOCKBACK_VELOCITY;
        }

        return value;
    }

    private double clampAttackKnockbackVertical(double value) {
        if (value > MAX_SAFE_VERTICAL_ATTACK_KNOCKBACK_VELOCITY) {
            return MAX_SAFE_VERTICAL_ATTACK_KNOCKBACK_VELOCITY;
        }

        if (value < -MAX_SAFE_VERTICAL_ATTACK_KNOCKBACK_VELOCITY) {
            return -MAX_SAFE_VERTICAL_ATTACK_KNOCKBACK_VELOCITY;
        }

        return value;
    }

    private void updateBotView(PitBot bot) {
        Location location = bot.getLocation();
        if (location == null) {
            return;
        }

        if (botEntityService != null) {
            botEntityService.applyState(bot);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            botPacketService.updateBotFor(player, bot);
        }
    }

    private boolean shouldHardReset(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return false;
        }

        Location location = bot.getLocation();
        if (location.getWorld() == null) {
            return true;
        }

        if (Double.isNaN(location.getX()) || Double.isNaN(location.getY()) || Double.isNaN(location.getZ())) {
            return true;
        }

        if (location.getY() < 1.0D || location.getY() > 255.0D) {
            return true;
        }

        return !botSpawnService.isInsidePitArea(location);
    }

    private void hardResetBot(PitBot bot, long now) {
        if (bot == null) {
            return;
        }

        Location respawnLocation = bot.isManualSpawn()
                ? botSpawnService.createTestSpawnLocation(botSpawnService.getCenterLocation())
                : botSpawnService.createSpawnLocation();
        bot.setBehaviorState(BotBehaviorState.RESET);
        bot.resetForRespawn(respawnLocation);
        configureBot(bot, now);
        botNameTagService.registerBot(bot);
        botPacketService.destroyBotForAll(bot);
        botPacketService.spawnBotForAll(bot);
    }

    private void handleRespawn(PitBot bot, long now) {
        if (bot.isPitCoreRespawnPending()) {
            if (bot.getRespawnAt() <= 0L || now < bot.getRespawnAt()) {
                return;
            }

            Location pitRespawnLocation = botSpawnService.createSpawnLocation();
            if (pitRespawnLocation != null && botEntityService != null && botEntityService.triggerPitRespawnEvent(bot, pitRespawnLocation)) {
                bot.setRespawnAt(0L);
                return;
            }

            bot.setPitCoreRespawnPending(false);
        }

        if (bot.getRespawnAt() <= 0L) {
            bot.setRespawnAt(now + botSettings.getRespawnDelayMs() + createRespawnJitterMs(bot));
            botPacketService.destroyBotForAll(bot);
            botNameTagService.unregisterBot(bot);
            return;
        }

        if (now < bot.getRespawnAt()) {
            return;
        }

        Location respawnLocation = botSpawnService.createSpawnLocation();
        bot.resetForRespawn(respawnLocation);

        configureBot(bot, now);
        botNameTagService.registerBot(bot);
        botPacketService.spawnBotForAll(bot);
    }

    private long createRespawnJitterMs(PitBot bot) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        if (tier == BotTier.EASY) {
            return randomBetween(180L, 700L);
        }

        if (tier == BotTier.TRYHARD) {
            return randomBetween(60L, 360L);
        }

        return randomBetween(110L, 520L);
    }

    private long randomBetween(long min, long max) {
        if (max <= min) {
            return min;
        }

        long range = max - min;
        return min + random.nextInt((int) Math.min(Integer.MAX_VALUE, range + 1L));
    }
}
