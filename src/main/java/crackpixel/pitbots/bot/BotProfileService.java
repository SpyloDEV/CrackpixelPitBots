package crackpixel.pitbots.bot;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotProfileService {

    private final BotSettings botSettings;
    private final BotSpawnService botSpawnService;
    private final Random random = new Random();

    public BotProfileService(BotSettings botSettings, BotSpawnService botSpawnService) {
        this.botSettings = botSettings;
        this.botSpawnService = botSpawnService;
    }

    public void configureBot(PitBot bot) {
        configureBot(bot, System.currentTimeMillis());
    }

    public void configureBot(PitBot bot, long now) {
        if (bot == null) {
            return;
        }

        BotTier tier = botSettings.createRandomTier();
        bot.setTier(tier);
        Player loadoutSource = pickLoadoutSource(bot.getLocation());
        double maxHealth = botSettings.resolveBotMaxHealth(loadoutSource);
        botSettings.applyTo(bot, botSettings.createRandomSpeed(), maxHealth);
        BotPersonality personality = createPersonality(tier);
        bot.setPersonality(personality);
        bot.setReactionTimeMultiplier(bot.getReactionTimeMultiplier() * personality.getAttackTempoMultiplier() * personality.getFocusBias());
        bot.setStrafeStrengthMultiplier(bot.getStrafeStrengthMultiplier() * personality.getStrafeBias());
        bot.setHealth(maxHealth);
        botSettings.applyLoadout(bot, loadoutSource);
        bot.setProfileSourcePlayerId(loadoutSource == null ? null : loadoutSource.getUniqueId());
        bot.setNextTargetUpdateAt(now + randomBetween(250L, 1400L));
        bot.setNextAttackAt(now + botSettings.getSpawnAttackDelayMs() + randomBetween(0L, 700L));
        bot.setAttackChargeUntil(0L);
        bot.setLastMovementCheckLocation(bot.getLocation());
        bot.setStuckTicks(0);
        bot.setStrafeDirection(random.nextBoolean() ? 1.0D : -1.0D);
        bot.setNextStrafeSwitchAt(now + randomBetween(250L, 1400L));
        bot.setIdleUntil(0L);
        bot.setNextIdleCheckAt(now + randomBetween(botSettings.getIdleCheckMinMs(), botSettings.getIdleCheckMaxMs()));
        bot.setSpeedMultiplier(createInitialSpeedMultiplier() * personalitySpeedBias(personality));
        bot.setNextSpeedChangeAt(now + randomBetween(botSettings.getSpeedChangeMinMs(), botSettings.getSpeedChangeMaxMs()));
        bot.setNextCombatMoveAt(now + randomBetween(120L, 450L));
        bot.setTargetPlayerId(null);
        bot.setTargetLockedUntil(0L);
        bot.setTargetCommitUntil(0L);
        bot.setHitReactUntil(0L);
        bot.setCombatResetUntil(0L);
        bot.setCombatBurstUntil(0L);
        bot.setAimSettleUntil(0L);
        bot.setLastSeenTargetAt(0L);
        bot.setLastSeenTargetLocation(null);
        bot.setObstacleRepositionUntil(0L);
        bot.setObstacleStrafeDirection(random.nextBoolean() ? 1.0D : -1.0D);
        bot.setLastDirectLineOfSightAt(0L);
        bot.setKnockbackX(0.0D);
        bot.setKnockbackZ(0.0D);
        bot.setLastDamageAt(0L);
        bot.setPreferredCombatDistance(botSettings.createPreferredCombatDistance(tier) * personality.getSpacingBias());
        bot.setBehaviorState(BotBehaviorState.IDLE);
        bot.setSearchUntil(0L);
        bot.setRespawnSettleUntil(now + createRespawnSettleMs(tier));
        bot.setLookYawOffset(createInitialLookYawOffset(tier));
        bot.setNextLookOffsetAt(now + randomBetween(180L, 900L));
    }

    private double createInitialSpeedMultiplier() {
        double min = botSettings.getSpeedMultiplierMin();
        double max = Math.max(min, botSettings.getSpeedMultiplierMax());
        double range = max - min;
        return min + (random.nextDouble() * range);
    }

    private BotPersonality createPersonality(BotTier tier) {
        double chaseBias;
        double attackTempoMultiplier;
        double spacingBias;
        double strafeBias;
        double aimJitterMultiplier;
        double focusBias;
        double crowdBias;
        double roamBias;
        double cadenceBias;

        if (tier == BotTier.EASY) {
            chaseBias = randomDouble(0.88D, 1.04D);
            attackTempoMultiplier = randomDouble(0.96D, 1.14D);
            spacingBias = randomDouble(0.95D, 1.20D);
            strafeBias = randomDouble(0.82D, 1.10D);
            aimJitterMultiplier = randomDouble(0.96D, 1.28D);
            focusBias = randomDouble(0.84D, 1.02D);
            crowdBias = randomDouble(0.86D, 1.08D);
            roamBias = randomDouble(0.94D, 1.18D);
            cadenceBias = randomDouble(0.88D, 1.14D);
        } else if (tier == BotTier.TRYHARD) {
            chaseBias = randomDouble(1.00D, 1.18D);
            attackTempoMultiplier = randomDouble(0.78D, 0.98D);
            spacingBias = randomDouble(0.82D, 1.05D);
            strafeBias = randomDouble(0.96D, 1.34D);
            aimJitterMultiplier = randomDouble(0.68D, 1.00D);
            focusBias = randomDouble(1.02D, 1.24D);
            crowdBias = randomDouble(0.94D, 1.22D);
            roamBias = randomDouble(0.94D, 1.10D);
            cadenceBias = randomDouble(0.96D, 1.18D);
        } else {
            chaseBias = randomDouble(0.92D, 1.12D);
            attackTempoMultiplier = randomDouble(0.86D, 1.08D);
            spacingBias = randomDouble(0.86D, 1.16D);
            strafeBias = randomDouble(0.84D, 1.26D);
            aimJitterMultiplier = randomDouble(0.78D, 1.14D);
            focusBias = randomDouble(0.90D, 1.12D);
            crowdBias = randomDouble(0.84D, 1.16D);
            roamBias = randomDouble(0.90D, 1.16D);
            cadenceBias = randomDouble(0.90D, 1.16D);
        }

        return new BotPersonality(
                chaseBias,
                attackTempoMultiplier,
                spacingBias,
                strafeBias,
                aimJitterMultiplier,
                focusBias,
                crowdBias,
                roamBias,
                cadenceBias,
                randomDouble(0.0D, Math.PI * 2.0D)
        );
    }

    private double personalitySpeedBias(BotPersonality personality) {
        if (personality == null) {
            return 1.0D;
        }

        double chaseInfluence = (personality.getChaseBias() - 1.0D) * 0.22D;
        double roamInfluence = (personality.getRoamBias() - 1.0D) * 0.14D;
        return Math.max(0.90D, Math.min(1.16D, 1.0D + chaseInfluence + roamInfluence));
    }

    private long randomBetween(long min, long max) {
        if (max <= min) {
            return min;
        }

        long range = max - min;
        return min + random.nextInt((int) Math.min(Integer.MAX_VALUE, range + 1L));
    }

    private double randomDouble(double min, double max) {
        if (max <= min) {
            return min;
        }

        return min + (random.nextDouble() * (max - min));
    }

    private double createInitialLookYawOffset(BotTier tier) {
        double amplitude;
        if (tier == BotTier.EASY) {
            amplitude = 8.0D;
        } else if (tier == BotTier.TRYHARD) {
            amplitude = 4.0D;
        } else {
            amplitude = 6.0D;
        }

        return (random.nextDouble() * amplitude * 2.0D) - amplitude;
    }

    private long createRespawnSettleMs(BotTier tier) {
        if (tier == BotTier.EASY) {
            return randomBetween(650L, 1250L);
        }

        if (tier == BotTier.TRYHARD) {
            return randomBetween(280L, 640L);
        }

        return randomBetween(420L, 900L);
    }

    private Player pickLoadoutSource(Location location) {
        if (!botSettings.isMirrorRandomPlayerLoadout()) {
            return null;
        }

        List<Player> candidates = new ArrayList<Player>();
        List<Player> meleeCandidates = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline() || player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }

            if (!botSpawnService.isInPitWorld(player.getLocation())) {
                continue;
            }

            if (!botSpawnService.isInsidePitArea(player.getLocation())) {
                continue;
            }

            if (location != null && location.getWorld() != null && player.getWorld() != location.getWorld()) {
                continue;
            }

            candidates.add(player);
            if (botSettings.hasMirrorableMeleeWeapon(player)) {
                meleeCandidates.add(player);
            }
        }

        List<Player> pool = meleeCandidates.isEmpty() ? candidates : meleeCandidates;
        if (pool.isEmpty()) {
            return null;
        }

        return pool.get(random.nextInt(pool.size()));
    }
}
