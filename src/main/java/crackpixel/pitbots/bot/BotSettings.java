package crackpixel.pitbots.bot;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BotSettings {

    private final Random random = new Random();

    private double maxHealth = 20.0D;
    private double minSpeed = 0.41D;
    private double maxSpeed = 0.53D;
    private double attackDamage = 1.0D;
    private double followRange = 11.75D;
    private double attackRange = 2.25D;
    private double safePlayerHealthFloor = 2.0D;
    private boolean canKillPlayers = true;
    private double minBotDistance = 2.4D;
    private long aggroMemoryMs = 4500L;
    private boolean groundFixEnabled = true;
    private boolean antiStuckEnabled = true;
    private double stuckDistanceThreshold = 0.12D;
    private int stuckTicksBeforeTeleport = 15;
    private boolean safeStepEnabled = true;
    private double maxStepUp = 1.5D;
    private double maxStepDown = 3.0D;
    private double strafeStrength = 0.16D;
    private double yawJitter = 0.8D;
    private double headPitchJitter = 2.0D;
    private double headLookMaxPitch = 18.0D;
    private double turnSpeedDegrees = 28.0D;
    private double combatTurnSpeedDegrees = 42.0D;
    private boolean idleEnabled = true;
    private double idleChance = 0.06D;
    private long idleCheckMinMs = 2500L;
    private long idleCheckMaxMs = 6500L;
    private long idleMinMs = 350L;
    private long idleMaxMs = 1000L;
    private long speedChangeMinMs = 1800L;
    private long speedChangeMaxMs = 4500L;
    private double speedMultiplierMin = 0.96D;
    private double speedMultiplierMax = 1.18D;
    private long respawnDelayMs = 3000L;
    private long spawnAttackDelayMs = 1200L;
    private long attackCooldownMinMs = 260L;
    private long attackCooldownMaxMs = 460L;
    private long roamRetargetMinMs = 2500L;
    private long roamRetargetMaxMs = 6000L;
    private long searchMinMs = 900L;
    private long searchMaxMs = 1650L;
    private boolean directPlayerDamage = true;
    private boolean directChaseEnabled = false;
    private double ambientEngageRange = 6.15D;
    private double botDamageMultiplier = 0.72D;
    private double sharedTargetPenalty = 0.28D;
    private boolean combatStrafeEnabled = true;
    private double combatStrafeStrength = 0.095D;
    private double combatStrafeMinDistance = 1.25D;
    private long combatStrafeMinIntervalMs = 220L;
    private long combatStrafeMaxIntervalMs = 520L;
    private long hurtCooldownMs = 220L;
    private long attackWindupMs = 40L;
    private long attackWindupRandomnessMs = 16L;
    private long aimSettleMinMs = 12L;
    private long aimSettleMaxMs = 28L;
    private double attackFacingMaxAngle = 58.0D;
    private double combatAimJitter = 1.1D;
    private double playerHitReach = 3.35D;
    private double playerFacingMaxAngle = 75.0D;
    private boolean lineOfSightRequired = true;
    private long lineOfSightGraceMs = 650L;
    private long targetSwitchMinCommitMs = 240L;
    private long targetSwitchMaxCommitMs = 540L;
    private double targetSwitchDistanceBias = 1.25D;
    private long attackCooldownJitterMs = 70L;
    private long targetSwitchAttackDelayMinMs = 4L;
    private long targetSwitchAttackDelayMaxMs = 18L;
    private long lineOfSightReacquireDelayMinMs = 4L;
    private long lineOfSightReacquireDelayMaxMs = 16L;
    private double knockbackHorizontal = 0.36D;
    private double knockbackSprintBonus = 0.10D;
    private double knockbackEnchantBonus = 0.06D;
    private double knockbackFacingWeight = 0.75D;
    private double knockbackDecay = 0.58D;
    private long hitReactionMs = 210L;
    private double botHitKnockbackHorizontal = 0.26D;
    private double botHitKnockbackVertical = 0.08D;
    private boolean combatRhythmEnabled = true;
    private long combatResetMinMs = 75L;
    private long combatResetMaxMs = 135L;
    private long combatBurstMinMs = 220L;
    private long combatBurstMaxMs = 360L;
    private double combatBackoffStrength = 0.08D;
    private double combatForwardBurstStrength = 0.20D;
    private double preferredCombatDistanceMin = 1.15D;
    private double preferredCombatDistanceMax = 1.55D;
    private boolean obstacleRepositionEnabled = true;
    private long obstacleRepositionMinMs = 280L;
    private long obstacleRepositionMaxMs = 620L;
    private double obstacleRepositionStrength = 0.28D;
    private double obstacleCornerPeekStrength = 0.16D;

    private boolean feedbackEnabled = false;
    private String hitEffect = "";
    private int hitEffectData = 0;
    private String hitSound = "";
    private float hitSoundVolume = 0.35F;
    private float hitSoundPitch = 1.4F;
    private String killEffect = "";
    private int killEffectData = 0;
    private String killSound = "";
    private float killSoundVolume = 0.8F;
    private float killSoundPitch = 1.35F;

    private boolean packetViewRangeEnabled = true;
    private double packetViewRange = 96.0D;
    private boolean edgeSpawnEnabled = true;
    private double edgeSpawnInset = 1.5D;
    private double edgeSpawnJitter = 2.25D;
    private double spawnAvoidPlayerRadius = 5.5D;
    private boolean ignoreAfkTargets = true;
    private long afkTargetThresholdMs = 8000L;
    private double afkTargetMoveThreshold = 1.2D;

    private double easyTierWeight = 0.34D;
    private double normalTierWeight = 0.46D;
    private double tryhardTierWeight = 0.20D;
    private double easyTierSpeedMultiplier = 0.92D;
    private double normalTierSpeedMultiplier = 1.0D;
    private double tryhardTierSpeedMultiplier = 1.10D;
    private double easyTierDamageMultiplier = 0.88D;
    private double normalTierDamageMultiplier = 1.0D;
    private double tryhardTierDamageMultiplier = 1.05D;
    private double easyTierRangeMultiplier = 0.92D;
    private double normalTierRangeMultiplier = 1.0D;
    private double tryhardTierRangeMultiplier = 1.08D;
    private double easyTierStrafeMultiplier = 0.72D;
    private double normalTierStrafeMultiplier = 1.0D;
    private double tryhardTierStrafeMultiplier = 1.22D;
    private double easyTierReactionMultiplier = 1.18D;
    private double normalTierReactionMultiplier = 1.0D;
    private double tryhardTierReactionMultiplier = 0.82D;

    private double fistDamage = 2.0D;
    private double woodSwordDamage = 4.0D;
    private double stoneSwordDamage = 5.0D;
    private double ironSwordDamage = 6.0D;
    private double diamondSwordDamage = 7.0D;

    private ItemType mainHand = ItemTypes.IRON_SWORD;
    private ItemType helmet = ItemTypes.CHAINMAIL_HELMET;
    private ItemType chestPlate = ItemTypes.CHAINMAIL_CHESTPLATE;
    private ItemType leggings = ItemTypes.CHAINMAIL_LEGGINGS;
    private ItemType boots = ItemTypes.CHAINMAIL_BOOTS;
    private boolean mirrorRandomPlayerLoadout = true;
    private List<String> botNames = new ArrayList<String>();
    private List<String> skinUsernames = new ArrayList<String>();
    private List<BotSkin> skins = new ArrayList<BotSkin>();

    public void load(FileConfiguration config) {
        if (config == null) {
            return;
        }

        maxHealth = positive(config.getDouble("bots.health", maxHealth), 1.0D);
        minSpeed = positive(config.getDouble("bots.speed-min", minSpeed), 0.01D);
        maxSpeed = positive(config.getDouble("bots.speed-max", maxSpeed), minSpeed);
        attackDamage = positive(config.getDouble("bots.attack-damage", attackDamage), 0.0D);
        followRange = positive(config.getDouble("bots.follow-range", followRange), 1.0D);
        attackRange = positive(config.getDouble("bots.attack-range", attackRange), 0.5D);
        safePlayerHealthFloor = positive(config.getDouble("bots.safe-player-health-floor", safePlayerHealthFloor), 0.0D);
        canKillPlayers = config.getBoolean("bots.can-kill-players", canKillPlayers);
        minBotDistance = positive(config.getDouble("bots.min-bot-distance", minBotDistance), 0.5D);
        aggroMemoryMs = positiveLong(config.getLong("bots.aggro-memory-ms", aggroMemoryMs), 0L);
        groundFixEnabled = config.getBoolean("movement.ground-fix", groundFixEnabled);
        antiStuckEnabled = config.getBoolean("movement.anti-stuck", antiStuckEnabled);
        stuckDistanceThreshold = positive(config.getDouble("movement.stuck-distance-threshold", stuckDistanceThreshold), 0.01D);
        stuckTicksBeforeTeleport = Math.max(1, config.getInt("movement.stuck-ticks-before-teleport", stuckTicksBeforeTeleport));
        safeStepEnabled = config.getBoolean("movement.safe-step", safeStepEnabled);
        maxStepUp = positive(config.getDouble("movement.max-step-up", maxStepUp), 0.0D);
        maxStepDown = positive(config.getDouble("movement.max-step-down", maxStepDown), 0.0D);
        strafeStrength = positive(config.getDouble("movement.strafe-strength", strafeStrength), 0.0D);
        yawJitter = positive(config.getDouble("movement.yaw-jitter", yawJitter), 0.0D);
        headPitchJitter = positive(config.getDouble("movement.head-pitch-jitter", headPitchJitter), 0.0D);
        headLookMaxPitch = positive(config.getDouble("movement.head-look-max-pitch", headLookMaxPitch), 0.0D);
        turnSpeedDegrees = positive(config.getDouble("movement.turn-speed-degrees", turnSpeedDegrees), 1.0D);
        combatTurnSpeedDegrees = positive(config.getDouble("movement.combat-turn-speed-degrees", combatTurnSpeedDegrees), 1.0D);
        idleEnabled = config.getBoolean("movement.idle-enabled", idleEnabled);
        idleChance = clamp(config.getDouble("movement.idle-chance", idleChance), 0.0D, 1.0D);
        idleCheckMinMs = positiveLong(config.getLong("movement.idle-check-min-ms", idleCheckMinMs), 100L);
        idleCheckMaxMs = positiveLong(config.getLong("movement.idle-check-max-ms", idleCheckMaxMs), idleCheckMinMs);
        idleMinMs = positiveLong(config.getLong("movement.idle-min-ms", idleMinMs), 100L);
        idleMaxMs = positiveLong(config.getLong("movement.idle-max-ms", idleMaxMs), idleMinMs);
        speedChangeMinMs = positiveLong(config.getLong("movement.speed-change-min-ms", speedChangeMinMs), 100L);
        speedChangeMaxMs = positiveLong(config.getLong("movement.speed-change-max-ms", speedChangeMaxMs), speedChangeMinMs);
        speedMultiplierMin = positive(config.getDouble("movement.speed-multiplier-min", speedMultiplierMin), 0.05D);
        speedMultiplierMax = positive(config.getDouble("movement.speed-multiplier-max", speedMultiplierMax), speedMultiplierMin);
        respawnDelayMs = positiveLong(config.getLong("bots.respawn-delay-ms", respawnDelayMs), 0L);
        spawnAttackDelayMs = positiveLong(config.getLong("bots.spawn-attack-delay-ms", spawnAttackDelayMs), 0L);
        attackCooldownMinMs = positiveLong(config.getLong("bots.attack-cooldown-min-ms", attackCooldownMinMs), 100L);
        attackCooldownMaxMs = positiveLong(config.getLong("bots.attack-cooldown-max-ms", attackCooldownMaxMs), attackCooldownMinMs);
        roamRetargetMinMs = positiveLong(config.getLong("bots.roam-retarget-min-ms", roamRetargetMinMs), 500L);
        roamRetargetMaxMs = positiveLong(config.getLong("bots.roam-retarget-max-ms", roamRetargetMaxMs), roamRetargetMinMs);
        searchMinMs = positiveLong(config.getLong("bots.search-min-ms", searchMinMs), 100L);
        searchMaxMs = positiveLong(config.getLong("bots.search-max-ms", searchMaxMs), searchMinMs);
        directPlayerDamage = config.getBoolean("combat.direct-player-damage", directPlayerDamage);
        directChaseEnabled = config.getBoolean("combat.direct-chase-enabled", directChaseEnabled);
        ambientEngageRange = Math.max(attackRange, positive(config.getDouble("combat.ambient-engage-range", ambientEngageRange), 0.5D));
        botDamageMultiplier = clamp(config.getDouble("combat.damage-multiplier", botDamageMultiplier), 0.1D, 2.0D);
        sharedTargetPenalty = positive(config.getDouble("combat.shared-target-penalty", sharedTargetPenalty), 0.0D);
        combatStrafeEnabled = config.getBoolean("combat.strafe-enabled", combatStrafeEnabled);
        combatStrafeStrength = positive(config.getDouble("combat.strafe-strength", combatStrafeStrength), 0.0D);
        combatStrafeMinDistance = positive(config.getDouble("combat.strafe-min-distance", combatStrafeMinDistance), 0.0D);
        combatStrafeMinIntervalMs = positiveLong(config.getLong("combat.strafe-min-interval-ms", combatStrafeMinIntervalMs), 50L);
        combatStrafeMaxIntervalMs = positiveLong(config.getLong("combat.strafe-max-interval-ms", combatStrafeMaxIntervalMs), combatStrafeMinIntervalMs);
        hurtCooldownMs = positiveLong(config.getLong("combat.hurt-cooldown-ms", hurtCooldownMs), 0L);
        attackWindupMs = positiveLong(config.getLong("combat.attack-windup-ms", attackWindupMs), 0L);
        attackWindupRandomnessMs = positiveLong(config.getLong("combat.attack-windup-randomness-ms", attackWindupRandomnessMs), 0L);
        aimSettleMinMs = positiveLong(config.getLong("combat.aim-settle-min-ms", aimSettleMinMs), 0L);
        aimSettleMaxMs = positiveLong(config.getLong("combat.aim-settle-max-ms", aimSettleMaxMs), aimSettleMinMs);
        attackFacingMaxAngle = clamp(config.getDouble("combat.attack-facing-max-angle", attackFacingMaxAngle), 5.0D, 180.0D);
        combatAimJitter = positive(config.getDouble("combat.aim-jitter", combatAimJitter), 0.0D);
        playerHitReach = positive(config.getDouble("combat.player-hit-reach", playerHitReach), 1.0D);
        playerFacingMaxAngle = clamp(config.getDouble("combat.player-facing-max-angle", playerFacingMaxAngle), 5.0D, 180.0D);
        lineOfSightRequired = config.getBoolean("combat.line-of-sight-required", lineOfSightRequired);
        lineOfSightGraceMs = positiveLong(config.getLong("combat.line-of-sight-grace-ms", lineOfSightGraceMs), 0L);
        targetSwitchMinCommitMs = positiveLong(config.getLong("combat.target-switch-min-commit-ms", targetSwitchMinCommitMs), 0L);
        targetSwitchMaxCommitMs = positiveLong(config.getLong("combat.target-switch-max-commit-ms", targetSwitchMaxCommitMs), targetSwitchMinCommitMs);
        targetSwitchDistanceBias = positive(config.getDouble("combat.target-switch-distance-bias", targetSwitchDistanceBias), 0.0D);
        attackCooldownJitterMs = positiveLong(config.getLong("combat.attack-cooldown-jitter-ms", attackCooldownJitterMs), 0L);
        targetSwitchAttackDelayMinMs = positiveLong(config.getLong("combat.target-switch-attack-delay-min-ms", targetSwitchAttackDelayMinMs), 0L);
        targetSwitchAttackDelayMaxMs = positiveLong(config.getLong("combat.target-switch-attack-delay-max-ms", targetSwitchAttackDelayMaxMs), targetSwitchAttackDelayMinMs);
        lineOfSightReacquireDelayMinMs = positiveLong(config.getLong("combat.line-of-sight-reacquire-delay-min-ms", lineOfSightReacquireDelayMinMs), 0L);
        lineOfSightReacquireDelayMaxMs = positiveLong(config.getLong("combat.line-of-sight-reacquire-delay-max-ms", lineOfSightReacquireDelayMaxMs), lineOfSightReacquireDelayMinMs);
        knockbackHorizontal = Math.max(0.48D, positive(config.getDouble("combat.knockback-horizontal", knockbackHorizontal), 0.0D));
        knockbackSprintBonus = Math.max(0.12D, positive(config.getDouble("combat.knockback-sprint-bonus", knockbackSprintBonus), 0.0D));
        knockbackEnchantBonus = Math.max(0.16D, positive(config.getDouble("combat.knockback-enchant-bonus", knockbackEnchantBonus), 0.0D));
        knockbackFacingWeight = clamp(config.getDouble("combat.knockback-facing-weight", knockbackFacingWeight), 0.0D, 1.0D);
        knockbackDecay = clamp(config.getDouble("combat.knockback-decay", knockbackDecay), 0.70D, 0.92D);
        hitReactionMs = positiveLong(config.getLong("combat.hit-reaction-ms", hitReactionMs), 0L);
        botHitKnockbackHorizontal = positive(config.getDouble("combat.bot-hit-knockback-horizontal", botHitKnockbackHorizontal), 0.0D);
        botHitKnockbackVertical = positive(config.getDouble("combat.bot-hit-knockback-vertical", botHitKnockbackVertical), 0.0D);
        combatRhythmEnabled = config.getBoolean("combat.rhythm-enabled", combatRhythmEnabled);
        combatResetMinMs = positiveLong(config.getLong("combat.rhythm-reset-min-ms", combatResetMinMs), 0L);
        combatResetMaxMs = positiveLong(config.getLong("combat.rhythm-reset-max-ms", combatResetMaxMs), combatResetMinMs);
        combatBurstMinMs = positiveLong(config.getLong("combat.rhythm-burst-min-ms", combatBurstMinMs), 0L);
        combatBurstMaxMs = positiveLong(config.getLong("combat.rhythm-burst-max-ms", combatBurstMaxMs), combatBurstMinMs);
        combatBackoffStrength = positive(config.getDouble("combat.rhythm-backoff-strength", combatBackoffStrength), 0.0D);
        combatForwardBurstStrength = positive(config.getDouble("combat.rhythm-forward-burst-strength", combatForwardBurstStrength), 0.0D);
        preferredCombatDistanceMin = positive(config.getDouble("combat.preferred-distance-min", preferredCombatDistanceMin), 0.5D);
        preferredCombatDistanceMax = positive(config.getDouble("combat.preferred-distance-max", preferredCombatDistanceMax), preferredCombatDistanceMin);
        obstacleRepositionEnabled = config.getBoolean("movement.obstacle-reposition-enabled", obstacleRepositionEnabled);
        obstacleRepositionMinMs = positiveLong(config.getLong("movement.obstacle-reposition-min-ms", obstacleRepositionMinMs), 0L);
        obstacleRepositionMaxMs = positiveLong(config.getLong("movement.obstacle-reposition-max-ms", obstacleRepositionMaxMs), obstacleRepositionMinMs);
        obstacleRepositionStrength = positive(config.getDouble("movement.obstacle-reposition-strength", obstacleRepositionStrength), 0.0D);
        obstacleCornerPeekStrength = positive(config.getDouble("movement.obstacle-corner-peek-strength", obstacleCornerPeekStrength), 0.0D);

        feedbackEnabled = config.getBoolean("feedback.enabled", feedbackEnabled);
        hitEffect = configuredString(config.getString("feedback.hit-effect", hitEffect), hitEffect);
        hitEffectData = Math.max(0, config.getInt("feedback.hit-effect-data", hitEffectData));
        hitSound = configuredString(config.getString("feedback.hit-sound", hitSound), "");
        hitSoundVolume = (float) positive(config.getDouble("feedback.hit-sound-volume", hitSoundVolume), 0.0D);
        hitSoundPitch = (float) positive(config.getDouble("feedback.hit-sound-pitch", hitSoundPitch), 0.0D);
        killEffect = configuredString(config.getString("feedback.kill-effect", killEffect), killEffect);
        killEffectData = Math.max(0, config.getInt("feedback.kill-effect-data", killEffectData));
        killSound = configuredString(config.getString("feedback.kill-sound", killSound), killSound);
        killSoundVolume = (float) positive(config.getDouble("feedback.kill-sound-volume", killSoundVolume), 0.0D);
        killSoundPitch = (float) positive(config.getDouble("feedback.kill-sound-pitch", killSoundPitch), 0.0D);

        packetViewRangeEnabled = config.getBoolean("packets.view-range-enabled", packetViewRangeEnabled);
        packetViewRange = positive(config.getDouble("packets.view-range", packetViewRange), 8.0D);
        edgeSpawnEnabled = config.getBoolean("spawn.edge-spawns-enabled", edgeSpawnEnabled);
        edgeSpawnInset = positive(config.getDouble("spawn.edge-spawn-inset", edgeSpawnInset), 0.0D);
        edgeSpawnJitter = positive(config.getDouble("spawn.edge-spawn-jitter", edgeSpawnJitter), 0.0D);
        spawnAvoidPlayerRadius = positive(config.getDouble("spawn.avoid-player-radius", spawnAvoidPlayerRadius), 0.0D);
        ignoreAfkTargets = config.getBoolean("anti-farm.ignore-afk-targets", ignoreAfkTargets);
        afkTargetThresholdMs = positiveLong(config.getLong("anti-farm.afk-threshold-ms", afkTargetThresholdMs), 0L);
        afkTargetMoveThreshold = positive(config.getDouble("anti-farm.afk-move-threshold", afkTargetMoveThreshold), 0.05D);

        easyTierWeight = positive(config.getDouble("tiers.easy.weight", easyTierWeight), 0.0D);
        normalTierWeight = positive(config.getDouble("tiers.normal.weight", normalTierWeight), 0.0D);
        tryhardTierWeight = positive(config.getDouble("tiers.tryhard.weight", tryhardTierWeight), 0.0D);
        easyTierSpeedMultiplier = positive(config.getDouble("tiers.easy.speed-multiplier", easyTierSpeedMultiplier), 0.1D);
        normalTierSpeedMultiplier = positive(config.getDouble("tiers.normal.speed-multiplier", normalTierSpeedMultiplier), 0.1D);
        tryhardTierSpeedMultiplier = positive(config.getDouble("tiers.tryhard.speed-multiplier", tryhardTierSpeedMultiplier), 0.1D);
        easyTierDamageMultiplier = positive(config.getDouble("tiers.easy.damage-multiplier", easyTierDamageMultiplier), 0.1D);
        normalTierDamageMultiplier = positive(config.getDouble("tiers.normal.damage-multiplier", normalTierDamageMultiplier), 0.1D);
        tryhardTierDamageMultiplier = positive(config.getDouble("tiers.tryhard.damage-multiplier", tryhardTierDamageMultiplier), 0.1D);
        easyTierRangeMultiplier = positive(config.getDouble("tiers.easy.range-multiplier", easyTierRangeMultiplier), 0.1D);
        normalTierRangeMultiplier = positive(config.getDouble("tiers.normal.range-multiplier", normalTierRangeMultiplier), 0.1D);
        tryhardTierRangeMultiplier = positive(config.getDouble("tiers.tryhard.range-multiplier", tryhardTierRangeMultiplier), 0.1D);
        easyTierStrafeMultiplier = positive(config.getDouble("tiers.easy.strafe-multiplier", easyTierStrafeMultiplier), 0.1D);
        normalTierStrafeMultiplier = positive(config.getDouble("tiers.normal.strafe-multiplier", normalTierStrafeMultiplier), 0.1D);
        tryhardTierStrafeMultiplier = positive(config.getDouble("tiers.tryhard.strafe-multiplier", tryhardTierStrafeMultiplier), 0.1D);
        easyTierReactionMultiplier = positive(config.getDouble("tiers.easy.reaction-multiplier", easyTierReactionMultiplier), 0.1D);
        normalTierReactionMultiplier = positive(config.getDouble("tiers.normal.reaction-multiplier", normalTierReactionMultiplier), 0.1D);
        tryhardTierReactionMultiplier = positive(config.getDouble("tiers.tryhard.reaction-multiplier", tryhardTierReactionMultiplier), 0.1D);

        fistDamage = positive(config.getDouble("player-damage.fist", fistDamage), 0.1D);
        woodSwordDamage = positive(config.getDouble("player-damage.wood-sword", woodSwordDamage), 0.1D);
        stoneSwordDamage = positive(config.getDouble("player-damage.stone-sword", stoneSwordDamage), 0.1D);
        ironSwordDamage = positive(config.getDouble("player-damage.iron-sword", ironSwordDamage), 0.1D);
        diamondSwordDamage = positive(config.getDouble("player-damage.diamond-sword", diamondSwordDamage), 0.1D);

        mainHand = itemType(config.getString("equipment.main-hand", "IRON_SWORD"), ItemTypes.IRON_SWORD);
        helmet = itemType(config.getString("equipment.helmet", "CHAINMAIL_HELMET"), ItemTypes.CHAINMAIL_HELMET);
        chestPlate = itemType(config.getString("equipment.chestplate", "CHAINMAIL_CHESTPLATE"), ItemTypes.CHAINMAIL_CHESTPLATE);
        leggings = itemType(config.getString("equipment.leggings", "CHAINMAIL_LEGGINGS"), ItemTypes.CHAINMAIL_LEGGINGS);
        boots = itemType(config.getString("equipment.boots", "CHAINMAIL_BOOTS"), ItemTypes.CHAINMAIL_BOOTS);
        mirrorRandomPlayerLoadout = config.getBoolean("equipment.mirror-random-player-loadout", mirrorRandomPlayerLoadout);
        loadBotNames(config);
        loadSkinUsernames(config);
        loadSkins(config);
    }

    public void applyTo(PitBot bot, double speed) {
        applyTo(bot, speed, maxHealth);
    }

    public void applyTo(PitBot bot, double speed, double overriddenMaxHealth) {
        if (bot == null) {
            return;
        }

        BotTier tier = bot.getTier();
        double resolvedMaxHealth = positive(overriddenMaxHealth, 1.0D);
        bot.setMaxHealth(resolvedMaxHealth);
        bot.setHealth(Math.min(bot.getHealth(), resolvedMaxHealth));
        if (bot.getHealth() <= 0.0D && bot.isAlive()) {
            bot.setHealth(resolvedMaxHealth);
        }
        bot.setSpeed(speed * getTierSpeedMultiplier(tier));
        bot.setAttackDamage(scaleBotDamage(attackDamage) * getTierDamageMultiplier(tier));
        bot.setFollowRange(followRange * getTierRangeMultiplier(tier));
        bot.setAttackRange(attackRange * getTierRangeMultiplier(tier));
        bot.setReactionTimeMultiplier(getTierReactionMultiplier(tier));
        bot.setStrafeStrengthMultiplier(getTierStrafeMultiplier(tier));
    }

    public double createRandomSpeed() {
        double range = Math.max(0.0D, maxSpeed - minSpeed);
        return minSpeed + (random.nextDouble() * range);
    }

    public void applyLoadout(PitBot bot, Player sourcePlayer) {
        if (bot == null) {
            return;
        }

        if (mirrorRandomPlayerLoadout && sourcePlayer != null) {
            ItemStack sourceMainHand = resolveMirroredMainHand(sourcePlayer);
            ItemStack sourceHelmet = cloneItem(sourcePlayer.getInventory().getHelmet());
            ItemStack sourceChestPlate = cloneItem(sourcePlayer.getInventory().getChestplate());
            ItemStack sourceLeggings = cloneItem(sourcePlayer.getInventory().getLeggings());
            ItemStack sourceBoots = cloneItem(sourcePlayer.getInventory().getBoots());

            bot.setMainHand(itemType(sourceMainHand, mainHand));
            bot.setHelmet(itemType(sourceHelmet, helmet));
            bot.setChestPlate(itemType(sourceChestPlate, chestPlate));
            bot.setLeggings(itemType(sourceLeggings, leggings));
            bot.setBoots(itemType(sourceBoots, boots));
            bot.setMainHandItem(sourceMainHand);
            bot.setHelmetItem(sourceHelmet);
            bot.setChestPlateItem(sourceChestPlate);
            bot.setLeggingsItem(sourceLeggings);
            bot.setBootsItem(sourceBoots);
            bot.setAttackDamage(scaleBotDamage(resolveMirroredAttackDamage(sourceMainHand)));
            return;
        }

        bot.setMainHand(mainHand);
        bot.setHelmet(helmet);
        bot.setChestPlate(chestPlate);
        bot.setLeggings(leggings);
        bot.setBoots(boots);
        bot.setMainHandItem(toBukkitItem(mainHand));
        bot.setHelmetItem(toBukkitItem(helmet));
        bot.setChestPlateItem(toBukkitItem(chestPlate));
        bot.setLeggingsItem(toBukkitItem(leggings));
        bot.setBootsItem(toBukkitItem(boots));
        bot.setAttackDamage(scaleBotDamage(attackDamage));
    }

    public boolean hasMirrorableMeleeWeapon(Player sourcePlayer) {
        return findMirrorableMainHand(sourcePlayer) != null;
    }

    public double resolveBotMaxHealth(Player sourcePlayer) {
        if (sourcePlayer == null || !mirrorRandomPlayerLoadout) {
            return maxHealth;
        }

        double sourceMaxHealth = sourcePlayer.getMaxHealth();
        if (Double.isNaN(sourceMaxHealth) || Double.isInfinite(sourceMaxHealth)) {
            return maxHealth;
        }

        return clamp(sourceMaxHealth, 10.0D, 40.0D);
    }

    public double getPlayerDamage(Material material) {
        if (material == Material.DIAMOND_SWORD) {
            return diamondSwordDamage;
        }

        if (material == Material.IRON_SWORD) {
            return ironSwordDamage;
        }

        if (material == Material.STONE_SWORD) {
            return stoneSwordDamage;
        }

        if (material == Material.WOOD_SWORD || material == Material.GOLD_SWORD) {
            return woodSwordDamage;
        }

        return fistDamage;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public double getFollowRange() {
        return followRange;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public double getSafePlayerHealthFloor() {
        return safePlayerHealthFloor;
    }

    public double getEffectiveSafePlayerHealthFloor() {
        return canKillPlayers ? 0.0D : safePlayerHealthFloor;
    }

    public boolean canKillPlayers() {
        return canKillPlayers;
    }

    public double getMinBotDistance() {
        return minBotDistance;
    }

    public long getAggroMemoryMs() {
        return aggroMemoryMs;
    }

    public boolean isGroundFixEnabled() {
        return groundFixEnabled;
    }

    public boolean isAntiStuckEnabled() {
        return antiStuckEnabled;
    }

    public double getStuckDistanceThreshold() {
        return stuckDistanceThreshold;
    }

    public int getStuckTicksBeforeTeleport() {
        return stuckTicksBeforeTeleport;
    }

    public boolean isSafeStepEnabled() {
        return safeStepEnabled;
    }

    public double getMaxStepUp() {
        return maxStepUp;
    }

    public double getMaxStepDown() {
        return maxStepDown;
    }

    public double getStrafeStrength() {
        return strafeStrength;
    }

    public double getYawJitter() {
        return yawJitter;
    }

    public double getHeadPitchJitter() {
        return headPitchJitter;
    }

    public double getHeadLookMaxPitch() {
        return headLookMaxPitch;
    }

    public double getTurnSpeedDegrees() {
        return turnSpeedDegrees;
    }

    public double getCombatTurnSpeedDegrees() {
        return combatTurnSpeedDegrees;
    }

    public boolean isIdleEnabled() {
        return idleEnabled;
    }

    public double getIdleChance() {
        return idleChance;
    }

    public long getIdleCheckMinMs() {
        return idleCheckMinMs;
    }

    public long getIdleCheckMaxMs() {
        return idleCheckMaxMs;
    }

    public long getIdleMinMs() {
        return idleMinMs;
    }

    public long getIdleMaxMs() {
        return idleMaxMs;
    }

    public long getSpeedChangeMinMs() {
        return speedChangeMinMs;
    }

    public long getSpeedChangeMaxMs() {
        return speedChangeMaxMs;
    }

    public double getSpeedMultiplierMin() {
        return speedMultiplierMin;
    }

    public double getSpeedMultiplierMax() {
        return speedMultiplierMax;
    }

    public long getRespawnDelayMs() {
        return respawnDelayMs;
    }

    public long getSpawnAttackDelayMs() {
        return spawnAttackDelayMs;
    }

    public long getAttackCooldownMinMs() {
        return attackCooldownMinMs;
    }

    public long getAttackCooldownMaxMs() {
        return attackCooldownMaxMs;
    }

    public long createAttackCooldownMs() {
        long base = randomBetween(attackCooldownMinMs, attackCooldownMaxMs);
        if (attackCooldownJitterMs <= 0L) {
            return base;
        }

        long jitter = randomBetween(-attackCooldownJitterMs, attackCooldownJitterMs);
        return Math.max(100L, base + jitter);
    }

    public long createAttackCooldownMs(PitBot bot) {
        return scaleTimingForTier(createAttackCooldownMs(), bot == null ? BotTier.NORMAL : bot.getTier());
    }

    public long getRoamRetargetMinMs() {
        return roamRetargetMinMs;
    }

    public long getRoamRetargetMaxMs() {
        return roamRetargetMaxMs;
    }

    public long createSearchDurationMs() {
        return randomBetween(searchMinMs, searchMaxMs);
    }

    public boolean isDirectPlayerDamage() {
        return directPlayerDamage;
    }

    public boolean isDirectChaseEnabled() {
        return directChaseEnabled;
    }

    public double getAmbientEngageRange() {
        return ambientEngageRange;
    }

    public double getBotDamageMultiplier() {
        return botDamageMultiplier;
    }

    public double getSharedTargetPenalty() {
        return sharedTargetPenalty;
    }

    public boolean isCombatStrafeEnabled() {
        return combatStrafeEnabled;
    }

    public double getCombatStrafeStrength() {
        return combatStrafeStrength;
    }

    public double getCombatStrafeMinDistance() {
        return combatStrafeMinDistance;
    }

    public long getCombatStrafeMinIntervalMs() {
        return combatStrafeMinIntervalMs;
    }

    public long getCombatStrafeMaxIntervalMs() {
        return combatStrafeMaxIntervalMs;
    }

    public long getHurtCooldownMs() {
        return hurtCooldownMs;
    }

    public long getAttackWindupMs() {
        return attackWindupMs;
    }

    public long createAttackWindupMs() {
        if (attackWindupRandomnessMs <= 0L) {
            return attackWindupMs;
        }

        long min = Math.max(0L, attackWindupMs - attackWindupRandomnessMs);
        long max = attackWindupMs + attackWindupRandomnessMs;
        if (max <= min) {
            return min;
        }

        long range = max - min;
        return min + random.nextInt((int) Math.min(Integer.MAX_VALUE, range + 1L));
    }

    public long createAttackWindupMs(PitBot bot) {
        return scaleTimingForTier(createAttackWindupMs(), bot == null ? BotTier.NORMAL : bot.getTier());
    }

    public long createAimSettleMs() {
        if (aimSettleMaxMs <= aimSettleMinMs) {
            return aimSettleMinMs;
        }

        long range = aimSettleMaxMs - aimSettleMinMs;
        return aimSettleMinMs + random.nextInt((int) Math.min(Integer.MAX_VALUE, range + 1L));
    }

    public long createAimSettleMs(PitBot bot) {
        return scaleTimingForTier(createAimSettleMs(), bot == null ? BotTier.NORMAL : bot.getTier());
    }

    public double getAttackFacingMaxAngle() {
        return attackFacingMaxAngle;
    }

    public double getCombatAimJitter() {
        return combatAimJitter;
    }

    public double getPlayerHitReach() {
        return playerHitReach;
    }

    public double getPlayerFacingMaxAngle() {
        return playerFacingMaxAngle;
    }

    public boolean isLineOfSightRequired() {
        return lineOfSightRequired;
    }

    public long getLineOfSightGraceMs() {
        return lineOfSightGraceMs;
    }

    public long createTargetCommitMs() {
        return randomBetween(targetSwitchMinCommitMs, targetSwitchMaxCommitMs);
    }

    public double getTargetSwitchDistanceBias() {
        return targetSwitchDistanceBias;
    }

    public long createTargetSwitchAttackDelayMs() {
        return randomBetween(targetSwitchAttackDelayMinMs, targetSwitchAttackDelayMaxMs);
    }

    public long createTargetSwitchAttackDelayMs(PitBot bot) {
        return scaleTimingForTier(createTargetSwitchAttackDelayMs(), bot == null ? BotTier.NORMAL : bot.getTier());
    }

    public long createLineOfSightReacquireDelayMs() {
        return randomBetween(lineOfSightReacquireDelayMinMs, lineOfSightReacquireDelayMaxMs);
    }

    public long createLineOfSightReacquireDelayMs(PitBot bot) {
        return scaleTimingForTier(createLineOfSightReacquireDelayMs(), bot == null ? BotTier.NORMAL : bot.getTier());
    }

    public double getKnockbackHorizontal() {
        return knockbackHorizontal;
    }

    public double getKnockbackSprintBonus() {
        return knockbackSprintBonus;
    }

    public double getKnockbackEnchantBonus() {
        return knockbackEnchantBonus;
    }

    public double getKnockbackFacingWeight() {
        return knockbackFacingWeight;
    }

    public double getKnockbackDecay() {
        return knockbackDecay;
    }

    public long getHitReactionMs() {
        return hitReactionMs;
    }

    public double getBotHitKnockbackHorizontal() {
        return botHitKnockbackHorizontal;
    }

    public double getBotHitKnockbackVertical() {
        return botHitKnockbackVertical;
    }

    public boolean isCombatRhythmEnabled() {
        return combatRhythmEnabled;
    }

    public long getCombatResetMinMs() {
        return combatResetMinMs;
    }

    public long getCombatResetMaxMs() {
        return combatResetMaxMs;
    }

    public long getCombatBurstMinMs() {
        return combatBurstMinMs;
    }

    public long getCombatBurstMaxMs() {
        return combatBurstMaxMs;
    }

    public double getCombatBackoffStrength() {
        return combatBackoffStrength;
    }

    public double getCombatForwardBurstStrength() {
        return combatForwardBurstStrength;
    }

    public double createPreferredCombatDistance() {
        double min = Math.max(0.5D, preferredCombatDistanceMin);
        double max = Math.max(min, preferredCombatDistanceMax);
        double range = max - min;
        return min + (random.nextDouble() * range);
    }

    public double createPreferredCombatDistance(BotTier tier) {
        double base = createPreferredCombatDistance();
        if (tier == BotTier.EASY) {
            return base + 0.12D;
        }

        if (tier == BotTier.TRYHARD) {
            return Math.max(0.8D, base - 0.08D);
        }

        return base;
    }

    public boolean isObstacleRepositionEnabled() {
        return obstacleRepositionEnabled;
    }

    public long createObstacleRepositionMs() {
        return randomBetween(obstacleRepositionMinMs, obstacleRepositionMaxMs);
    }

    public double getObstacleRepositionStrength() {
        return obstacleRepositionStrength;
    }

    public double getObstacleCornerPeekStrength() {
        return obstacleCornerPeekStrength;
    }

    public boolean isFeedbackEnabled() {
        return feedbackEnabled;
    }

    public String getHitEffect() {
        return hitEffect;
    }

    public int getHitEffectData() {
        return hitEffectData;
    }

    public String getHitSound() {
        return hitSound;
    }

    public float getHitSoundVolume() {
        return hitSoundVolume;
    }

    public float getHitSoundPitch() {
        return hitSoundPitch;
    }

    public String getKillEffect() {
        return killEffect;
    }

    public int getKillEffectData() {
        return killEffectData;
    }

    public String getKillSound() {
        return killSound;
    }

    public float getKillSoundVolume() {
        return killSoundVolume;
    }

    public float getKillSoundPitch() {
        return killSoundPitch;
    }

    public boolean isPacketViewRangeEnabled() {
        return packetViewRangeEnabled;
    }

    public double getPacketViewRange() {
        return packetViewRange;
    }

    public boolean isEdgeSpawnEnabled() {
        return edgeSpawnEnabled;
    }

    public double getEdgeSpawnInset() {
        return edgeSpawnInset;
    }

    public double getEdgeSpawnJitter() {
        return edgeSpawnJitter;
    }

    public double getSpawnAvoidPlayerRadius() {
        return spawnAvoidPlayerRadius;
    }

    public boolean isIgnoreAfkTargets() {
        return ignoreAfkTargets;
    }

    public long getAfkTargetThresholdMs() {
        return afkTargetThresholdMs;
    }

    public double getAfkTargetMoveThreshold() {
        return afkTargetMoveThreshold;
    }

    public BotTier createRandomTier() {
        double easy = Math.max(0.0D, easyTierWeight);
        double normal = Math.max(0.0D, normalTierWeight);
        double tryhard = Math.max(0.0D, tryhardTierWeight);
        double total = easy + normal + tryhard;

        if (total <= 0.0D) {
            return BotTier.NORMAL;
        }

        double roll = random.nextDouble() * total;
        if (roll < easy) {
            return BotTier.EASY;
        }

        if (roll < easy + normal) {
            return BotTier.NORMAL;
        }

        return BotTier.TRYHARD;
    }

    public double getTierStrafeMultiplier(BotTier tier) {
        if (tier == BotTier.EASY) {
            return easyTierStrafeMultiplier;
        }

        if (tier == BotTier.TRYHARD) {
            return tryhardTierStrafeMultiplier;
        }

        return normalTierStrafeMultiplier;
    }

    public double calculatePlayerDamageAgainstBot(Player player, PitBot bot) {
        if (player == null) {
            return 1.0D;
        }

        ItemStack item = player.getItemInHand();
        Material type = item == null ? Material.AIR : item.getType();
        double damage = getPlayerDamage(type);

        if (item != null) {
            int sharpness = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
            if (sharpness > 0) {
                damage += 1.25D * sharpness;
            }

            int fireAspect = item.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
            if (fireAspect > 0) {
                damage += 0.75D * fireAspect;
            }
        }

        PotionEffect strength = findPotionEffect(player, PotionEffectType.INCREASE_DAMAGE);
        if (strength != null) {
            damage += 3.0D * (strength.getAmplifier() + 1);
        }

        PotionEffect weakness = findPotionEffect(player, PotionEffectType.WEAKNESS);
        if (weakness != null) {
            damage -= 4.0D * (weakness.getAmplifier() + 1);
        }

        if (isCriticalHit(player)) {
            damage *= 1.5D;
        }

        damage = Math.max(0.5D, damage);
        return applyArmorReduction(damage, bot);
    }

    public double calculatePlayerKnockbackHorizontal(Player player) {
        double horizontal = Math.max(0.48D, knockbackHorizontal);
        if (player == null) {
            return horizontal;
        }

        if (player.isSprinting()) {
            horizontal += Math.max(0.12D, knockbackSprintBonus);
        }

        ItemStack item = player.getItemInHand();
        if (item != null) {
            int enchantLevel = item.getEnchantmentLevel(Enchantment.KNOCKBACK);
            if (enchantLevel > 0) {
                horizontal += enchantLevel * Math.max(0.16D, knockbackEnchantBonus);
            }
        }

        return horizontal;
    }

    public ItemType getMainHand() {
        return mainHand;
    }

    public ItemType getHelmet() {
        return helmet;
    }

    public ItemType getChestPlate() {
        return chestPlate;
    }

    public ItemType getLeggings() {
        return leggings;
    }

    public ItemType getBoots() {
        return boots;
    }

    public org.bukkit.inventory.ItemStack toBukkitItem(ItemType itemType) {
        Material material = toBukkitMaterial(itemType);
        return material == null || material == Material.AIR
                ? null
                : new org.bukkit.inventory.ItemStack(material, 1);
    }

    public boolean isMirrorRandomPlayerLoadout() {
        return mirrorRandomPlayerLoadout;
    }

    public String createRandomName(int number) {
        List<String> pool = buildNamePool();
        int sequence = Math.max(1, number) - 1;
        String base = pool.get(sequence % pool.size());
        int cycle = sequence / pool.size();
        String suffix = cycle <= 0 ? "" : String.valueOf(cycle + 1);
        int maxBaseLength = Math.max(1, 16 - suffix.length());
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }

        return base + suffix;
    }

    public synchronized BotSkin getRandomSkin() {
        if (skins.isEmpty()) {
            return null;
        }

        return skins.get(random.nextInt(skins.size()));
    }

    public List<String> getSkinUsernames() {
        return Collections.unmodifiableList(new ArrayList<String>(skinUsernames));
    }

    public synchronized int getLoadedSkinCount() {
        return skins.size();
    }

    public int getRandomNameCount() {
        return botNames.size();
    }

    public synchronized void addResolvedSkins(List<BotSkin> resolvedSkins) {
        if (resolvedSkins == null || resolvedSkins.isEmpty()) {
            return;
        }

        for (BotSkin skin : resolvedSkins) {
            if (skin != null && skin.isValid() && !containsSkin(skin)) {
                skins.add(skin);
            }
        }
    }

    private boolean containsSkin(BotSkin candidate) {
        if (candidate == null || !candidate.isValid()) {
            return false;
        }

        for (BotSkin skin : skins) {
            if (skin == null) {
                continue;
            }

            if (candidate.getValue().equals(skin.getValue())
                    && candidate.getSignature().equals(skin.getSignature())) {
                return true;
            }
        }

        return false;
    }

    private double positive(double value, double min) {
        return value < min ? min : value;
    }

    private long positiveLong(long value, long min) {
        return value < min ? min : value;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private String configuredString(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private ItemType itemType(String name, ItemType fallback) {
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("AIR") || name.equalsIgnoreCase("NONE")) {
            return ItemTypes.AIR;
        }

        String normalized = name.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase();

        try {
            Field field = ItemTypes.class.getField(normalized);
            Object value = field.get(null);
            if (value instanceof ItemType) {
                return (ItemType) value;
            }
        } catch (Exception ignored) {
        }

        ItemType byNamespacedKey = ItemTypes.getByName("minecraft:" + normalized.toLowerCase());
        if (byNamespacedKey != null) {
            return byNamespacedKey;
        }

        ItemType byKey = ItemTypes.getByName(normalized.toLowerCase());
        if (byKey != null) {
            return byKey;
        }

        return fallback;
    }

    private double applyArmorReduction(double damage, PitBot bot) {
        int armorPoints = getBotArmorPoints(bot);
        double reduction = Math.min(0.8D, armorPoints * 0.04D);
        return Math.max(0.5D, damage * (1.0D - reduction));
    }

    private int getBotArmorPoints(PitBot bot) {
        if (bot == null) {
            return armorPoints(helmet)
                    + armorPoints(chestPlate)
                    + armorPoints(leggings)
                    + armorPoints(boots);
        }

        return armorPoints(resolveItemType(bot.getHelmet(), helmet))
                + armorPoints(resolveItemType(bot.getChestPlate(), chestPlate))
                + armorPoints(resolveItemType(bot.getLeggings(), leggings))
                + armorPoints(resolveItemType(bot.getBoots(), boots));
    }

    private int armorPoints(ItemType itemType) {
        if (itemType == null || itemType == ItemTypes.AIR) {
            return 0;
        }

        if (itemType == ItemTypes.LEATHER_HELMET || itemType == ItemTypes.GOLDEN_HELMET || itemType == ItemTypes.CHAINMAIL_HELMET || itemType == ItemTypes.IRON_HELMET) {
            return 1;
        }

        if (itemType == ItemTypes.DIAMOND_HELMET) {
            return 3;
        }

        if (itemType == ItemTypes.LEATHER_CHESTPLATE || itemType == ItemTypes.GOLDEN_CHESTPLATE) {
            return 3;
        }

        if (itemType == ItemTypes.CHAINMAIL_CHESTPLATE || itemType == ItemTypes.IRON_CHESTPLATE) {
            return 5;
        }

        if (itemType == ItemTypes.DIAMOND_CHESTPLATE) {
            return 8;
        }

        if (itemType == ItemTypes.LEATHER_LEGGINGS || itemType == ItemTypes.GOLDEN_LEGGINGS) {
            return 2;
        }

        if (itemType == ItemTypes.CHAINMAIL_LEGGINGS) {
            return 4;
        }

        if (itemType == ItemTypes.IRON_LEGGINGS) {
            return 5;
        }

        if (itemType == ItemTypes.DIAMOND_LEGGINGS) {
            return 6;
        }

        if (itemType == ItemTypes.LEATHER_BOOTS || itemType == ItemTypes.GOLDEN_BOOTS || itemType == ItemTypes.CHAINMAIL_BOOTS) {
            return 1;
        }

        if (itemType == ItemTypes.IRON_BOOTS) {
            return 2;
        }

        if (itemType == ItemTypes.DIAMOND_BOOTS) {
            return 3;
        }

        return 0;
    }

    private Material toBukkitMaterial(ItemType itemType) {
        if (itemType == null || itemType == ItemTypes.AIR) {
            return Material.AIR;
        }

        if (itemType == ItemTypes.IRON_SWORD) {
            return Material.IRON_SWORD;
        }
        if (itemType == ItemTypes.STONE_SWORD) {
            return Material.STONE_SWORD;
        }
        if (itemType == ItemTypes.DIAMOND_SWORD) {
            return Material.DIAMOND_SWORD;
        }
        if (itemType == ItemTypes.WOODEN_SWORD || itemType == ItemTypes.GOLDEN_SWORD) {
            return Material.WOOD_SWORD;
        }
        if (itemType == ItemTypes.LEATHER_HELMET) {
            return Material.LEATHER_HELMET;
        }
        if (itemType == ItemTypes.CHAINMAIL_HELMET) {
            return Material.CHAINMAIL_HELMET;
        }
        if (itemType == ItemTypes.IRON_HELMET) {
            return Material.IRON_HELMET;
        }
        if (itemType == ItemTypes.GOLDEN_HELMET) {
            return Material.GOLD_HELMET;
        }
        if (itemType == ItemTypes.DIAMOND_HELMET) {
            return Material.DIAMOND_HELMET;
        }
        if (itemType == ItemTypes.LEATHER_CHESTPLATE) {
            return Material.LEATHER_CHESTPLATE;
        }
        if (itemType == ItemTypes.CHAINMAIL_CHESTPLATE) {
            return Material.CHAINMAIL_CHESTPLATE;
        }
        if (itemType == ItemTypes.IRON_CHESTPLATE) {
            return Material.IRON_CHESTPLATE;
        }
        if (itemType == ItemTypes.GOLDEN_CHESTPLATE) {
            return Material.GOLD_CHESTPLATE;
        }
        if (itemType == ItemTypes.DIAMOND_CHESTPLATE) {
            return Material.DIAMOND_CHESTPLATE;
        }
        if (itemType == ItemTypes.LEATHER_LEGGINGS) {
            return Material.LEATHER_LEGGINGS;
        }
        if (itemType == ItemTypes.CHAINMAIL_LEGGINGS) {
            return Material.CHAINMAIL_LEGGINGS;
        }
        if (itemType == ItemTypes.IRON_LEGGINGS) {
            return Material.IRON_LEGGINGS;
        }
        if (itemType == ItemTypes.GOLDEN_LEGGINGS) {
            return Material.GOLD_LEGGINGS;
        }
        if (itemType == ItemTypes.DIAMOND_LEGGINGS) {
            return Material.DIAMOND_LEGGINGS;
        }
        if (itemType == ItemTypes.LEATHER_BOOTS) {
            return Material.LEATHER_BOOTS;
        }
        if (itemType == ItemTypes.CHAINMAIL_BOOTS) {
            return Material.CHAINMAIL_BOOTS;
        }
        if (itemType == ItemTypes.IRON_BOOTS) {
            return Material.IRON_BOOTS;
        }
        if (itemType == ItemTypes.GOLDEN_BOOTS) {
            return Material.GOLD_BOOTS;
        }
        if (itemType == ItemTypes.DIAMOND_BOOTS) {
            return Material.DIAMOND_BOOTS;
        }

        return Material.AIR;
    }

    private boolean isCriticalHit(Player player) {
        return player != null
                && player.getFallDistance() > 0.0F
                && player.getVehicle() == null
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS);
    }

    private PotionEffect findPotionEffect(Player player, PotionEffectType type) {
        if (player == null || type == null) {
            return null;
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect != null && effect.getType().equals(type)) {
                return effect;
            }
        }

        return null;
    }

    private void loadBotNames(FileConfiguration config) {
        List<String> configuredNames = config.getStringList("cosmetics.random-names");
        List<String> loaded = new ArrayList<String>();

        for (String configuredName : configuredNames) {
            if (configuredName == null || configuredName.trim().isEmpty()) {
                continue;
            }

            loaded.add(configuredName.trim());
        }

        botNames = loaded;
    }

    private void loadSkinUsernames(FileConfiguration config) {
        List<String> configuredNames = config.getStringList("cosmetics.skin-usernames");
        List<String> loaded = new ArrayList<String>();

        for (String configuredName : configuredNames) {
            if (configuredName == null || configuredName.trim().isEmpty()) {
                continue;
            }

            loaded.add(configuredName.trim());
        }

        skinUsernames = loaded;
    }

    private synchronized void loadSkins(FileConfiguration config) {
        List<BotSkin> loaded = new ArrayList<BotSkin>();

        for (Map<?, ?> skinMap : config.getMapList("cosmetics.skins")) {
            Object value = skinMap.get("value");
            Object signature = skinMap.get("signature");
            BotSkin skin = new BotSkin(
                    value == null ? null : String.valueOf(value),
                    signature == null ? null : String.valueOf(signature)
            );

            if (skin.isValid()) {
                loaded.add(skin);
            }
        }

        skins = loaded;
    }

    private Material itemInHandType(Player player) {
        if (player == null || player.getItemInHand() == null) {
            return Material.AIR;
        }

        return player.getItemInHand().getType();
    }

    private ItemStack cloneItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        return itemStack.clone();
    }

    private ItemStack resolveMirroredMainHand(Player sourcePlayer) {
        ItemStack mirrored = findMirrorableMainHand(sourcePlayer);
        if (mirrored != null) {
            return mirrored.clone();
        }

        return toBukkitItem(mainHand);
    }

    private ItemStack findMirrorableMainHand(Player sourcePlayer) {
        if (sourcePlayer == null) {
            return null;
        }

        ItemStack heldItem = sourcePlayer.getItemInHand();
        if (isMirrorableMeleeWeapon(heldItem)) {
            return heldItem;
        }

        PlayerInventory inventory = sourcePlayer.getInventory();
        if (inventory == null) {
            return null;
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (isMirrorableMeleeWeapon(candidate)) {
                return candidate;
            }
        }

        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return null;
        }

        for (ItemStack candidate : contents) {
            if (isMirrorableMeleeWeapon(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isMirrorableMeleeWeapon(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        Material material = itemStack.getType();
        return material == Material.WOOD_SWORD
                || material == Material.STONE_SWORD
                || material == Material.IRON_SWORD
                || material == Material.GOLD_SWORD
                || material == Material.DIAMOND_SWORD;
    }

    private Material armorType(Player player, int slot) {
        if (player == null || player.getInventory() == null || player.getInventory().getArmorContents() == null) {
            return Material.AIR;
        }

        org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents();
        if (slot < 0 || slot >= armor.length || armor[slot] == null) {
            return Material.AIR;
        }

        return armor[slot].getType();
    }

    private ItemType itemType(Material material, ItemType fallback) {
        if (material == null || material == Material.AIR) {
            return ItemTypes.AIR;
        }

        return itemType(material.name(), fallback);
    }

    private ItemType itemType(ItemStack itemStack, ItemType fallback) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return ItemTypes.AIR;
        }

        return itemType(itemStack.getType(), fallback);
    }

    private double resolveMirroredAttackDamage(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return fistDamage;
        }

        double damage = getPlayerDamage(itemStack.getType());
        int sharpness = itemStack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        if (sharpness > 0) {
            damage += 1.25D * sharpness;
        }

        return Math.max(fistDamage, damage);
    }

    private long randomBetween(long min, long max) {
        if (max <= min) {
            return min;
        }

        long range = max - min;
        return min + random.nextInt((int) Math.min(Integer.MAX_VALUE, range + 1L));
    }

    private ItemType resolveItemType(ItemType value, ItemType fallback) {
        return value == null || value == ItemTypes.AIR ? fallback : value;
    }

    private long scaleTimingForTier(long base, BotTier tier) {
        double multiplier = getTierReactionMultiplier(tier);
        return Math.max(40L, Math.round(base * multiplier));
    }

    private double scaleBotDamage(double damage) {
        return Math.max(0.5D, damage * botDamageMultiplier);
    }

    private double getTierSpeedMultiplier(BotTier tier) {
        if (tier == BotTier.EASY) {
            return easyTierSpeedMultiplier;
        }

        if (tier == BotTier.TRYHARD) {
            return tryhardTierSpeedMultiplier;
        }

        return normalTierSpeedMultiplier;
    }

    private double getTierDamageMultiplier(BotTier tier) {
        if (tier == BotTier.EASY) {
            return easyTierDamageMultiplier;
        }

        if (tier == BotTier.TRYHARD) {
            return tryhardTierDamageMultiplier;
        }

        return normalTierDamageMultiplier;
    }

    private double getTierRangeMultiplier(BotTier tier) {
        if (tier == BotTier.EASY) {
            return easyTierRangeMultiplier;
        }

        if (tier == BotTier.TRYHARD) {
            return tryhardTierRangeMultiplier;
        }

        return normalTierRangeMultiplier;
    }

    private double getTierReactionMultiplier(BotTier tier) {
        if (tier == BotTier.EASY) {
            return easyTierReactionMultiplier;
        }

        if (tier == BotTier.TRYHARD) {
            return tryhardTierReactionMultiplier;
        }

        return normalTierReactionMultiplier;
    }

    private List<String> buildNamePool() {
        List<String> pool = new ArrayList<String>();

        for (String configuredName : botNames) {
            String sanitized = sanitizeBotName(configuredName);
            if (!pool.contains(sanitized)) {
                pool.add(sanitized);
            }
        }

        if (pool.isEmpty()) {
            pool.add("PitBot");
        }

        return pool;
    }

    private String sanitizeBotName(String value) {
        String sanitized = value == null ? "" : value.replaceAll("[^A-Za-z0-9_]", "");
        return sanitized.isEmpty() ? "PitBot" : sanitized;
    }
}
