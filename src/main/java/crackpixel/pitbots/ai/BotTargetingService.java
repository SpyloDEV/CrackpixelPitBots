package crackpixel.pitbots.ai;

import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.nms.BotEntityService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BotTargetingService {

    private final BotSettings botSettings;
    private final BotEntityService botEntityService;
    private final Random random = new Random();
    private final Map<UUID, Location> lastObservedLocations = new HashMap<UUID, Location>();
    private final Map<UUID, Long> lastObservedMovementAt = new HashMap<UUID, Long>();

    public BotTargetingService(BotSettings botSettings, BotEntityService botEntityService) {
        this.botSettings = botSettings;
        this.botEntityService = botEntityService;
    }

    public Player findTarget(PitBot bot, long now) {
        if (bot == null) {
            return null;
        }

        if (now < bot.getRespawnSettleUntil()) {
            return null;
        }

        if (!botSettings.isDirectChaseEnabled()) {
            return findAmbientTarget(bot, now);
        }

        Player lockedTarget = findLockedTarget(bot, now);
        if (lockedTarget != null) {
            return lockedTarget;
        }

        TargetCandidate current = findCurrentTarget(bot, now);
        TargetCandidate best = findBestCandidate(bot);
        if (best == null) {
            return current == null ? null : current.player;
        }

        if (current == null) {
            bot.setTargetCommitUntil(now + scaleCommitDuration(bot, botSettings.createTargetCommitMs()));
            return best.player;
        }

        if (current.player.getUniqueId().equals(best.player.getUniqueId())) {
            return current.player;
        }

        if (now < bot.getTargetCommitUntil()) {
            return current.player;
        }

        if (shouldSwitchTarget(bot, current, best, now)) {
            bot.setTargetCommitUntil(now + scaleCommitDuration(bot, botSettings.createTargetCommitMs()));
            return best.player;
        }

        return current.player;
    }

    public Player findNearestTarget(PitBot bot) {
        double maxDistance = botSettings.isDirectChaseEnabled()
                ? (bot == null ? 0.0D : bot.getFollowRange())
                : resolveAmbientRange(bot);
        TargetCandidate candidate = findBestCandidate(bot, maxDistance);
        return candidate == null ? null : candidate.player;
    }

    public Location findEngagementAnchor(PitBot bot) {
        if (bot == null) {
            return null;
        }

        double maxDistance = Math.max(bot.getAttackRange() + 4.2D, Math.min(bot.getFollowRange(), 7.25D));
        List<TargetCandidate> candidates = findCandidates(bot, maxDistance);
        if (candidates.isEmpty()) {
            return null;
        }

        TargetCandidate anchorCandidate = chooseFightAnchorCandidate(candidates);
        return anchorCandidate == null || anchorCandidate.player == null ? null : anchorCandidate.player.getLocation();
    }

    public boolean hasNearbyEngagement(PitBot bot) {
        if (bot == null) {
            return false;
        }

        double engagementRange = Math.max(bot.getAttackRange() + 5.6D, Math.min(bot.getFollowRange(), 8.0D));
        return !findCandidates(bot, engagementRange).isEmpty();
    }

    public boolean shouldDirectChase(PitBot bot, Player target) {
        if (botSettings.isDirectChaseEnabled()) {
            return true;
        }

        if (bot == null || target == null || bot.getLocation() == null) {
            return false;
        }

        if (isRealPlayerTarget(target) && hasDirectLineOfSight(bot, target)) {
            double aggressiveChaseRange = Math.max(bot.getAttackRange() + 6.25D, bot.getFollowRange() * 0.95D);
            if (target.getLocation().distanceSquared(bot.getLocation()) <= aggressiveChaseRange * aggressiveChaseRange) {
                return true;
            }
        }

        int nearbyRealPlayerTargets = countNearbyRealPlayers(bot, Math.max(bot.getAttackRange() + 5.5D, 7.5D));
        int nearbyBotTargets = countNearbyBotTargets(bot, Math.max(bot.getAttackRange() + 4.8D, 6.8D));
        if (!isRealPlayerTarget(target)) {
            if (nearbyBotTargets <= 0) {
                return false;
            }

            if (nearbyRealPlayerTargets <= 0) {
                return isValidTarget(target, bot.getLocation());
            }

            if (nearbyRealPlayerTargets == 1 && nearbyBotTargets >= 2) {
                double directBotRange = Math.max(bot.getAttackRange() + 4.8D, Math.min(bot.getFollowRange(), 7.5D));
                return isValidTarget(target, bot.getLocation())
                        && target.getLocation().distanceSquared(bot.getLocation()) <= directBotRange * directBotRange;
            }

            return false;
        }

        if (nearbyRealPlayerTargets <= 1) {
            return isValidTarget(target, bot.getLocation());
        }

        double closeRealPlayerRange = Math.max(bot.getAttackRange() + 3.25D, 5.35D);
        return target.getLocation().distanceSquared(bot.getLocation()) <= closeRealPlayerRange * closeRealPlayerRange
                && isValidTarget(target, bot.getLocation());
    }

    private Player findLockedTarget(PitBot bot, long now) {
        if (bot == null || bot.getLocation() == null || bot.getTargetPlayerId() == null) {
            return null;
        }

        if (now >= bot.getTargetLockedUntil()) {
            bot.setTargetLockedUntil(0L);
            return null;
        }

        Player player = resolveTarget(bot.getTargetPlayerId());
        if (!isValidTarget(player, bot.getLocation())) {
            bot.setTargetLockedUntil(0L);
            return null;
        }

        double maxDistance = bot.getFollowRange() * 1.35D;
        if (player.getLocation().distanceSquared(bot.getLocation()) > maxDistance * maxDistance) {
            bot.setTargetLockedUntil(0L);
            return null;
        }

        return player;
    }

    private TargetCandidate findCurrentTarget(PitBot bot, long now) {
        if (bot == null || bot.getTargetPlayerId() == null) {
            return null;
        }

        TargetCandidate candidate = buildCandidate(bot, resolveTarget(bot.getTargetPlayerId()));
        if (candidate == null) {
            bot.setTargetCommitUntil(0L);
            return null;
        }

        if (!candidate.directLineOfSight
                && (now - bot.getLastSeenTargetAt()) > botSettings.getLineOfSightGraceMs()
                && candidate.distance > bot.getAttackRange() + 1.6D) {
            bot.setTargetCommitUntil(0L);
            return null;
        }

        return candidate;
    }

    private Player findAmbientTarget(PitBot bot, long now) {
        if (bot == null) {
            return null;
        }

        TargetCandidate immediateRealTarget = findImmediateVisibleRealPlayerTarget(bot);
        if (immediateRealTarget != null) {
            bot.setTargetLockedUntil(now + Math.max(850L, botSettings.getAggroMemoryMs() / 3L));
            bot.setTargetCommitUntil(now + scaleCommitDuration(bot, Math.max(220L, botSettings.createTargetCommitMs() / 2L)));
            return immediateRealTarget.player;
        }

        int nearbyRealPlayerTargets = countNearbyRealPlayers(bot, Math.max(bot.getAttackRange() + 5.2D, 7.0D));
        int nearbyBotTargets = countNearbyBotTargets(bot, Math.max(bot.getAttackRange() + 4.8D, 6.6D));
        if (nearbyRealPlayerTargets <= 1) {
            TargetCandidate botSoloTarget = nearbyBotTargets > 0
                    ? findBestBotCandidate(bot, Math.max(bot.getAttackRange() + 4.0D, bot.getFollowRange() * 0.92D))
                    : null;
            if (nearbyRealPlayerTargets == 0 && botSoloTarget != null) {
                return botSoloTarget.player;
            }
            TargetCandidate soloTarget = findBestRealPlayerCandidate(bot, bot.getFollowRange());
            if (shouldPreferBotFightInLowPlayerContext(bot, soloTarget, botSoloTarget, nearbyRealPlayerTargets, nearbyBotTargets)) {
                return botSoloTarget.player;
            }
            if (soloTarget != null) {
                return soloTarget.player;
            }

            if (botSoloTarget != null) {
                return botSoloTarget.player;
            }

            return null;
        }

        double ambientRange = resolveAmbientRange(bot);
        List<TargetCandidate> candidates = findCandidates(bot, ambientRange);
        TargetCandidate current = buildCandidate(bot, resolveTarget(bot.getTargetPlayerId()), ambientRange);
        if (shouldDropAmbientTarget(current, ambientRange, now, bot)) {
            current = null;
            bot.setTargetCommitUntil(0L);
        }
        if (candidates.isEmpty()) {
            return current == null ? null : current.player;
        }

        if (current != null && now < bot.getTargetCommitUntil()) {
            return current.player;
        }

        TargetCandidate chosen = chooseAmbientCandidate(candidates, current);
        if (chosen == null) {
            return current == null ? null : current.player;
        }

        bot.setTargetCommitUntil(now + scaleCommitDuration(bot, Math.max(250L, botSettings.createTargetCommitMs() / 2L)));
        return chosen.player;
    }

    private TargetCandidate findBestCandidate(PitBot bot) {
        return findBestCandidate(bot, bot == null ? 0.0D : bot.getFollowRange());
    }

    private TargetCandidate findBestCandidate(PitBot bot, double maxDistance) {
        List<TargetCandidate> candidates = findCandidates(bot, maxDistance);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private TargetCandidate findImmediateVisibleRealPlayerTarget(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return null;
        }

        double immediateRange = Math.max(bot.getAttackRange() + 4.6D, bot.getFollowRange());
        List<TargetCandidate> candidates = findCandidates(bot, immediateRange);
        for (TargetCandidate candidate : candidates) {
            if (!candidate.realPlayerTarget) {
                continue;
            }

            if (!candidate.directLineOfSight) {
                continue;
            }

            if (candidate.distance > immediateRange) {
                continue;
            }

            if (candidate.distance <= Math.max(bot.getAttackRange() + 2.9D, 4.7D)) {
                return candidate;
            }

            return candidate;
        }

        return null;
    }

    private TargetCandidate findBestRealPlayerCandidate(PitBot bot, double maxDistance) {
        List<TargetCandidate> candidates = findCandidates(bot, maxDistance);
        for (TargetCandidate candidate : candidates) {
            if (isRealPlayerTarget(candidate.player)) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private TargetCandidate findBestBotCandidate(PitBot bot, double maxDistance) {
        List<TargetCandidate> candidates = findCandidates(bot, maxDistance);
        for (TargetCandidate candidate : candidates) {
            if (!candidate.realPlayerTarget) {
                return candidate;
            }
        }
        return null;
    }

    private List<TargetCandidate> findCandidates(PitBot bot, double maxDistance) {
        if (bot == null || bot.getLocation() == null) {
            return Collections.emptyList();
        }

        List<TargetCandidate> candidates = new ArrayList<TargetCandidate>();
        for (Player player : getCandidatePlayers(bot)) {
            TargetCandidate candidate = buildCandidate(bot, player, maxDistance);
            if (candidate == null) {
                continue;
            }
            candidates.add(candidate);
        }

        Collections.sort(candidates, new Comparator<TargetCandidate>() {
            @Override
            public int compare(TargetCandidate first, TargetCandidate second) {
                return Double.compare(first.score, second.score);
            }
        });
        return candidates;
    }

    private TargetCandidate buildCandidate(PitBot bot, Player player) {
        return buildCandidate(bot, player, bot == null ? 0.0D : bot.getFollowRange());
    }

    private TargetCandidate buildCandidate(PitBot bot, Player player, double maxDistance) {
        if (bot == null || bot.getLocation() == null || !isValidTarget(player, bot.getLocation())) {
            return null;
        }

        long now = System.currentTimeMillis();
        trackObservedPlayer(player, now);
        if (shouldIgnoreForAfk(bot, player, now)) {
            return null;
        }

        Location botLocation = bot.getLocation();
        double safeMaxDistance = Math.max(0.5D, maxDistance);
        double maxDistanceSquared = safeMaxDistance * safeMaxDistance;
        double distanceSquared = player.getLocation().distanceSquared(botLocation);
        if (distanceSquared > maxDistanceSquared) {
            return null;
        }

        boolean directLineOfSight = hasDirectLineOfSight(bot, player);
        double distance = Math.sqrt(distanceSquared);
        double score = distance - (directLineOfSight ? 0.65D : 0.0D);
        boolean realPlayerTarget = isRealPlayerTarget(player);
        int nearbyRealPlayerTargets = countNearbyRealPlayers(bot, Math.max(bot.getAttackRange() + 5.2D, 7.0D));
        if (realPlayerTarget && directLineOfSight) {
            double closeVisibleRange = Math.max(bot.getAttackRange() + 4.4D, bot.getFollowRange() * 0.92D);
            if (distance <= closeVisibleRange) {
                score -= 2.35D;
            } else {
                score -= 0.80D;
            }
        }
        if (!realPlayerTarget) {
            score += botTargetPreferenceOffset(nearbyRealPlayerTargets);
            score -= Math.min(0.75D, Math.max(0.0D, 1 - nearbyRealPlayerTargets) * 0.18D);
        }
        int focusedBots = 0;
        int nearbyBotPressure = 0;
        if (botEntityService != null) {
            focusedBots = botEntityService.countLivingBotsTargeting(player.getUniqueId(), bot.getUniqueId());
            if (focusedBots > 0) {
                double focusPenaltyMultiplier = realPlayerTarget ? 0.40D : 0.03D;
                score += Math.min(1.8D, focusedBots * botSettings.getSharedTargetPenalty() * focusPenaltyMultiplier);
                if (focusedBots >= 3) {
                    score += Math.min(1.2D, (focusedBots - 2) * botSettings.getSharedTargetPenalty() * (realPlayerTarget ? 0.28D : 0.03D));
                }
            }
            double pressureRadius = realPlayerTarget ? 3.2D : 2.8D;
            nearbyBotPressure = botEntityService.countLivingBotsNear(player.getLocation(), pressureRadius, bot.getUniqueId());
            if (nearbyBotPressure > 1) {
                double pressureMultiplier = realPlayerTarget ? 0.24D : 0.02D;
                score += Math.min(1.35D, (nearbyBotPressure - 1) * 0.20D * pressureMultiplier);
            }
        }
        if (!realPlayerTarget && nearbyBotPressure > 0) {
            score -= Math.min(1.55D, 0.62D + (nearbyBotPressure * 0.20D));
        }
        if (player.getUniqueId().equals(bot.getTargetPlayerId())) {
            score -= 0.35D;
        }

        return new TargetCandidate(
                player,
                distance,
                directLineOfSight,
                score,
                focusedBots,
                nearbyBotPressure,
                realPlayerTarget,
                nearbyRealPlayerTargets
        );
    }

    private double resolveAmbientRange(PitBot bot) {
        double ambientRange = botSettings.getAmbientEngageRange();
        if (bot != null) {
            ambientRange = Math.max(ambientRange, bot.getAttackRange() + 0.55D);
        }
        return Math.min(ambientRange, bot == null ? ambientRange : Math.max(bot.getAttackRange() + 1.9D, 5.0D));
    }

    private boolean shouldDropAmbientTarget(TargetCandidate current, double ambientRange, long now, PitBot bot) {
        if (current == null || bot == null) {
            return false;
        }

        if (current.distance > ambientRange * 1.08D) {
            return true;
        }

        if (!current.directLineOfSight) {
            long unseenFor = Math.max(0L, now - bot.getLastSeenTargetAt());
            if (unseenFor > (botSettings.getLineOfSightGraceMs() + 280L)
                    && current.distance > bot.getAttackRange() + 1.05D) {
                return true;
            }
        }

        return false;
    }

    private TargetCandidate chooseFightAnchorCandidate(List<TargetCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        TargetCandidate best = null;
        double bestPressureScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < Math.min(4, candidates.size()); i++) {
            TargetCandidate candidate = candidates.get(i);
            double pressureScore = (candidate.realPlayerTarget ? 1.75D : 1.0D)
                    + Math.min(2.8D, candidate.nearbyBotPressure * 0.40D)
                    + Math.min(1.2D, candidate.focusedBots * 0.22D)
                    - (candidate.distance * 0.16D)
                    + (candidate.directLineOfSight ? 0.30D : 0.0D);
            if (pressureScore > bestPressureScore) {
                bestPressureScore = pressureScore;
                best = candidate;
            }
        }

        return best == null ? candidates.get(0) : best;
    }

    private TargetCandidate chooseAmbientCandidate(List<TargetCandidate> candidates, TargetCandidate current) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        List<TargetCandidate> pool = new ArrayList<TargetCandidate>();
        int limit = Math.min(4, candidates.size());
        for (int i = 0; i < limit; i++) {
            TargetCandidate candidate = candidates.get(i);
            if (current != null && candidate.player.getUniqueId().equals(current.player.getUniqueId())) {
                continue;
            }
            pool.add(candidate);
        }

        if (pool.isEmpty()) {
            return current == null ? candidates.get(0) : current;
        }

        if (current != null) {
            TargetCandidate reliefCandidate = findPressureReliefCandidate(current, pool);
            if (reliefCandidate != null) {
                return reliefCandidate;
            }
        }

        return chooseWeightedAmbientCandidate(pool);
    }

    private boolean shouldSwitchTarget(PitBot bot, TargetCandidate current, TargetCandidate challenger, long now) {
        if (bot == null || current == null || challenger == null) {
            return challenger != null;
        }

        if (shouldRelieveTargetPressure(current, challenger)) {
            return true;
        }

        if (!current.directLineOfSight && challenger.directLineOfSight) {
            if ((now - bot.getLastSeenTargetAt()) > botSettings.getLineOfSightGraceMs()) {
                return true;
            }

            return challenger.distance + (botSettings.getTargetSwitchDistanceBias() * 0.45D) < current.distance;
        }

        if (current.directLineOfSight && !challenger.directLineOfSight) {
            return challenger.distance + (botSettings.getTargetSwitchDistanceBias() * 1.60D) < current.distance;
        }

        return challenger.distance + botSettings.getTargetSwitchDistanceBias() < current.distance;
    }

    private TargetCandidate findPressureReliefCandidate(TargetCandidate current, List<TargetCandidate> pool) {
        if (current == null || pool == null || pool.isEmpty() || current.focusedBots < 4) {
            return null;
        }

        if (!current.realPlayerTarget) {
            return null;
        }

        for (TargetCandidate candidate : pool) {
            if (candidate == null) {
                continue;
            }

            if (!candidate.realPlayerTarget && current.realPlayerTarget) {
                continue;
            }

            if (candidate.focusedBots + 1 >= current.focusedBots
                    && candidate.nearbyBotPressure + 1 >= current.nearbyBotPressure) {
                continue;
            }

            if (candidate.score <= current.score + 0.45D) {
                return candidate;
            }
        }

        return null;
    }

    private TargetCandidate chooseWeightedAmbientCandidate(List<TargetCandidate> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }

        double totalWeight = 0.0D;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            TargetCandidate candidate = pool.get(i);
            double weight = 1.0D / Math.max(0.35D, candidate.score + 0.9D);
            if (candidate.realPlayerTarget) {
                weight *= 1.12D;
            } else if (candidate.nearbyRealPlayers <= 1) {
                weight *= 2.35D;
            } else {
                weight *= 1.35D;
            }
            if (candidate.focusedBots > 0) {
                double focusedPenalty = candidate.realPlayerTarget ? 0.12D : 0.005D;
                weight /= (1.0D + (candidate.focusedBots * focusedPenalty));
            }
            if (candidate.nearbyBotPressure > 1) {
                double pressurePenalty = candidate.realPlayerTarget ? 0.05D : 0.002D;
                weight /= (1.0D + ((candidate.nearbyBotPressure - 1) * pressurePenalty));
            }
            if (!candidate.realPlayerTarget && candidate.nearbyRealPlayers <= 0 && candidate.nearbyBotPressure > 0) {
                weight *= 2.45D;
            }
            weights[i] = weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0.001D) {
            return pool.get(random.nextInt(pool.size()));
        }

        double pick = random.nextDouble() * totalWeight;
        for (int i = 0; i < pool.size(); i++) {
            pick -= weights[i];
            if (pick <= 0.0D) {
                return pool.get(i);
            }
        }

        return pool.get(pool.size() - 1);
    }

    private boolean shouldRelieveTargetPressure(TargetCandidate current, TargetCandidate challenger) {
        if (current == null || challenger == null || current.focusedBots < 2) {
            return false;
        }

        if (!current.realPlayerTarget) {
            return false;
        }

        if (!challenger.realPlayerTarget && current.realPlayerTarget) {
            return false;
        }

        if (challenger.focusedBots + 1 >= current.focusedBots
                && challenger.nearbyBotPressure + 1 >= current.nearbyBotPressure) {
            return false;
        }

        return challenger.distance <= current.distance + (botSettings.getTargetSwitchDistanceBias() * 0.75D)
                || challenger.score <= current.score + 0.80D;
    }

    private boolean isValidTarget(Player player, Location botLocation) {
        if (player == null || botLocation == null || player.isDead()) {
            return false;
        }

        boolean botTarget = botEntityService != null && botEntityService.isBot(player);
        if (!botTarget && !player.isOnline()) {
            return false;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        if (player.getWorld() != botLocation.getWorld()) {
            return false;
        }

        if (botTarget) {
            return true;
        }

        return player.isOnline();
    }

    private int countValidTargets(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return 0;
        }

        int count = 0;
        for (Player player : getCandidatePlayers(bot)) {
            if (isValidTarget(player, bot.getLocation())) {
                count++;
            }
        }
        return count;
    }

    private int countValidRealPlayers(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return 0;
        }

        int count = 0;
        for (Player player : getCandidatePlayers(bot)) {
            if (isRealPlayerTarget(player) && isValidTarget(player, bot.getLocation())) {
                count++;
            }
        }
        return count;
    }

    private int countValidBotTargets(PitBot bot) {
        if (bot == null || bot.getLocation() == null) {
            return 0;
        }

        int count = 0;
        for (Player player : getCandidatePlayers(bot)) {
            if (!isRealPlayerTarget(player) && isValidTarget(player, bot.getLocation())) {
                count++;
            }
        }
        return count;
    }

    private int countNearbyRealPlayers(PitBot bot, double radius) {
        if (bot == null || bot.getLocation() == null) {
            return 0;
        }

        int count = 0;
        double radiusSquared = radius * radius;
        for (Player player : getCandidatePlayers(bot)) {
            if (!isRealPlayerTarget(player) || !isValidTarget(player, bot.getLocation())) {
                continue;
            }

            if (player.getLocation().distanceSquared(bot.getLocation()) <= radiusSquared) {
                count++;
            }
        }

        return count;
    }

    private int countNearbyBotTargets(PitBot bot, double radius) {
        if (bot == null || bot.getLocation() == null) {
            return 0;
        }

        int count = 0;
        double radiusSquared = radius * radius;
        for (Player player : getCandidatePlayers(bot)) {
            if (isRealPlayerTarget(player) || !isValidTarget(player, bot.getLocation())) {
                continue;
            }

            if (player.getLocation().distanceSquared(bot.getLocation()) <= radiusSquared) {
                count++;
            }
        }

        return count;
    }

    private double botTargetPreferenceOffset(int nearbyRealPlayerTargets) {
        if (nearbyRealPlayerTargets <= 0) {
            return -1.20D;
        }

        if (nearbyRealPlayerTargets == 1) {
            return -0.55D;
        }

        return -0.05D;
    }

    private boolean shouldPreferBotFightInLowPlayerContext(PitBot bot,
                                                           TargetCandidate realTarget,
                                                           TargetCandidate botTarget,
                                                           int nearbyRealPlayerTargets,
                                                           int nearbyBotTargets) {
        if (bot == null || botTarget == null || nearbyBotTargets <= 0) {
            return false;
        }

        if (realTarget == null) {
            return true;
        }

        if (nearbyRealPlayerTargets <= 0) {
            return true;
        }

        double closeRealPlayerRange = Math.max(bot.getAttackRange() + 3.0D, 5.0D);
        if (realTarget.distance <= closeRealPlayerRange) {
            return false;
        }

        if (realTarget.focusedBots <= 1 && realTarget.score <= botTarget.score + 0.45D) {
            return false;
        }

        if (botTarget.distance + 0.35D < realTarget.distance) {
            return true;
        }

        if (botTarget.focusedBots + 1 < realTarget.focusedBots
                && botTarget.score <= realTarget.score + 0.95D) {
            return true;
        }

        int spreadSeed = Math.abs(bot.getUniqueId().hashCode());
        if (nearbyBotTargets >= 2
                && (spreadSeed % 3) == 0
                && botTarget.score <= realTarget.score + 1.15D) {
            return true;
        }

        return nearbyBotTargets >= 3
                && botTarget.nearbyBotPressure > 0
                && botTarget.score <= realTarget.score + 1.10D;
    }

    private List<Player> getCandidatePlayers(PitBot bot) {
        Map<String, Player> candidates = new LinkedHashMap<String, Player>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null) {
                candidates.put(player.getUniqueId().toString(), player);
            }
        }

        if (botEntityService != null) {
            for (Player botPlayer : botEntityService.getSpawnedBotPlayers(bot)) {
                if (botPlayer != null) {
                    candidates.put(botPlayer.getUniqueId().toString(), botPlayer);
                }
            }
        }

        return new ArrayList<Player>(candidates.values());
    }

    private boolean isRealPlayerTarget(Player player) {
        return player != null && (botEntityService == null || !botEntityService.isBot(player));
    }

    private void trackObservedPlayer(Player player, long now) {
        if (!isRealPlayerTarget(player) || player == null || player.getLocation() == null) {
            return;
        }

        UUID uniqueId = player.getUniqueId();
        Location current = player.getLocation();
        Location last = lastObservedLocations.get(uniqueId);
        double moveThreshold = botSettings.getAfkTargetMoveThreshold();
        double moveThresholdSquared = moveThreshold * moveThreshold;

        if (last == null || last.getWorld() != current.getWorld()) {
            lastObservedLocations.put(uniqueId, current.clone());
            lastObservedMovementAt.put(uniqueId, now);
            return;
        }

        if (last.distanceSquared(current) >= moveThresholdSquared) {
            lastObservedLocations.put(uniqueId, current.clone());
            lastObservedMovementAt.put(uniqueId, now);
        }
    }

    private boolean shouldIgnoreForAfk(PitBot bot, Player player, long now) {
        if (bot == null
                || player == null
                || !isRealPlayerTarget(player)
                || !botSettings.isIgnoreAfkTargets()) {
            return false;
        }

        if (player.getUniqueId().equals(bot.getTargetPlayerId()) && now < bot.getTargetLockedUntil()) {
            return false;
        }

        Long lastMovementAt = lastObservedMovementAt.get(player.getUniqueId());
        return lastMovementAt != null && (now - lastMovementAt) >= botSettings.getAfkTargetThresholdMs();
    }

    private Player resolveTarget(java.util.UUID uniqueId) {
        if (uniqueId == null) {
            return null;
        }

        if (botEntityService != null) {
            return botEntityService.getPlayer(uniqueId);
        }

        return Bukkit.getPlayer(uniqueId);
    }

    private long scaleCommitDuration(PitBot bot, long baseDuration) {
        double focusBias = bot == null || bot.getPersonality() == null ? 1.0D : bot.getPersonality().getFocusBias();
        return Math.max(80L, Math.round(baseDuration * focusBias));
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

    private static final class TargetCandidate {

        private final Player player;
        private final double distance;
        private final boolean directLineOfSight;
        private final double score;
        private final int focusedBots;
        private final int nearbyBotPressure;
        private final boolean realPlayerTarget;
        private final int nearbyRealPlayers;

        private TargetCandidate(Player player,
                                double distance,
                                boolean directLineOfSight,
                                double score,
                                int focusedBots,
                                int nearbyBotPressure,
                                boolean realPlayerTarget,
                                int nearbyRealPlayers) {
            this.player = player;
            this.distance = distance;
            this.directLineOfSight = directLineOfSight;
            this.score = score;
            this.focusedBots = focusedBots;
            this.nearbyBotPressure = nearbyBotPressure;
            this.realPlayerTarget = realPlayerTarget;
            this.nearbyRealPlayers = nearbyRealPlayers;
        }
    }
}
