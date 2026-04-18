package crackpixel.pitbots.combat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import crackpixel.pitbots.bot.BotFeedbackService;
import crackpixel.pitbots.bot.BotKillService;
import crackpixel.pitbots.bot.BotManager;
import crackpixel.pitbots.bot.BotNameTagService;
import crackpixel.pitbots.bot.BotSettings;
import crackpixel.pitbots.bot.BotStatsService;
import crackpixel.pitbots.bot.PitBot;
import crackpixel.pitbots.packet.BotPacketService;
import crackpixel.pitbots.pitcore.PitCoreBridgeService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BotCombatListener extends PacketListenerAbstract {

    private final BotManager botManager;
    private final BotSettings botSettings;
    private final BotNameTagService botNameTagService;
    private final BotStatsService botStatsService;
    private final BotKillService botKillService;
    private final BotFeedbackService botFeedbackService;
    private final BotPacketService botPacketService;
    private final PitCoreBridgeService pitCoreBridgeService;

    public BotCombatListener(BotManager botManager,
                             BotSettings botSettings,
                             BotNameTagService botNameTagService,
                             BotStatsService botStatsService,
                             BotKillService botKillService,
                             BotFeedbackService botFeedbackService,
                             BotPacketService botPacketService,
                             PitCoreBridgeService pitCoreBridgeService) {
        this.botManager = botManager;
        this.botSettings = botSettings;
        this.botNameTagService = botNameTagService;
        this.botStatsService = botStatsService;
        this.botKillService = botKillService;
        this.botFeedbackService = botFeedbackService;
        this.botPacketService = botPacketService;
        this.pitCoreBridgeService = pitCoreBridgeService;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        PitBot hitBot = botManager.getBotByEntityId(packet.getEntityId());
        if (hitBot == null || !hitBot.isAlive()) {
            return;
        }

        Player player = Bukkit.getPlayer(event.getUser().getUUID());
        if (player == null || hitBot.getLocation() == null || player.getWorld() != hitBot.getLocation().getWorld()) {
            return;
        }

        event.setCancelled(true);
        if (!isValidPlayerMeleeHit(player, hitBot)) {
            return;
        }

        long now = System.currentTimeMillis();
        double previousHealth = hitBot.getHealth();
        if (now - hitBot.getLastDamageAt() < botSettings.getHurtCooldownMs()) {
            return;
        }

        if (pitCoreBridgeService != null) {
            pitCoreBridgeService.markBotDamagedByPlayer(player);
        }

        hitBot.setTargetPlayerId(player.getUniqueId());
        hitBot.setTargetLockedUntil(now + botSettings.getAggroMemoryMs());
        applyHitReaction(hitBot, player, now);
        boolean died = hitBot.damage(botSettings.calculatePlayerDamageAgainstBot(player, hitBot));
        hitBot.setLastDamageAt(now);

        if (died) {
            hitBot.setRespawnAt(now + botSettings.getRespawnDelayMs());
            botFeedbackService.playKill(player, hitBot);
            botPacketService.destroyBotForAll(hitBot);
            botNameTagService.unregisterBot(hitBot);
            botStatsService.recordKill(player);
            botKillService.handleBotKill(player, hitBot);
            return;
        }

        if (pitCoreBridgeService != null) {
            pitCoreBridgeService.showBotDamageIndicator(player, hitBot, previousHealth, false);
        }

        botNameTagService.updateHealth(hitBot);
        botPacketService.damageEffectForAll(hitBot);
        botFeedbackService.playHit(hitBot);
    }

    private void applyHitReaction(PitBot bot, Player attacker, long now) {
        if (bot == null || attacker == null || bot.getLocation() == null || attacker.getLocation() == null) {
            return;
        }

        Location botLocation = bot.getLocation();
        Location attackerLocation = attacker.getLocation();
        double sourceX = botLocation.getX() - attackerLocation.getX();
        double sourceZ = botLocation.getZ() - attackerLocation.getZ();
        double sourceDistance = Math.sqrt((sourceX * sourceX) + (sourceZ * sourceZ));

        if (sourceDistance > 0.001D) {
            sourceX /= sourceDistance;
            sourceZ /= sourceDistance;
        }

        double yawRadians = Math.toRadians(attackerLocation.getYaw());
        double facingX = -Math.sin(yawRadians);
        double facingZ = Math.cos(yawRadians);
        double facingWeight = botSettings.getKnockbackFacingWeight();

        double dx = (facingX * facingWeight) + (sourceX * (1.0D - facingWeight));
        double dz = (facingZ * facingWeight) + (sourceZ * (1.0D - facingWeight));
        double distance = Math.sqrt((dx * dx) + (dz * dz));

        if (distance <= 0.001D) {
            return;
        }

        double strength = botSettings.calculatePlayerKnockbackHorizontal(attacker);
        double appliedX = (dx / distance) * strength;
        double appliedZ = (dz / distance) * strength;
        bot.setKnockbackX(limitKnockback((bot.getKnockbackX() * 0.35D) + appliedX));
        bot.setKnockbackZ(limitKnockback((bot.getKnockbackZ() * 0.35D) + appliedZ));
        bot.setAttackChargeUntil(0L);
        bot.setNextAttackAt(Math.max(bot.getNextAttackAt(), now + botSettings.getHurtCooldownMs()));
        bot.setHitReactUntil(Math.max(bot.getHitReactUntil(), now + botSettings.getHitReactionMs()));
    }

    private boolean isValidPlayerMeleeHit(Player player, PitBot bot) {
        if (player == null || bot == null || bot.getLocation() == null) {
            return false;
        }

        Location botLocation = bot.getLocation();
        Location eyeLocation = player.getEyeLocation();
        if (eyeLocation.getWorld() != botLocation.getWorld()) {
            return false;
        }

        double maxDistance = botSettings.getPlayerHitReach();
        if (eyeLocation.distanceSquared(botLocation) > maxDistance * maxDistance) {
            return false;
        }

        Vector toBot = botLocation.toVector().subtract(eyeLocation.toVector());
        if (toBot.lengthSquared() <= 0.0001D) {
            return true;
        }

        double angle = Math.toDegrees(eyeLocation.getDirection().angle(toBot.normalize()));
        return angle <= botSettings.getPlayerFacingMaxAngle();
    }

    private double limitKnockback(double value) {
        double max = 1.35D;
        if (value > max) {
            return max;
        }

        if (value < -max) {
            return -max;
        }

        return value;
    }
}
