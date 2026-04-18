package crackpixel.pitbots.bot;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PitBot {

    private final UUID uniqueId;
    private final String name;
    private int entityId;
    private BotSkin skin;
    private BotTier tier;
    private BotBehaviorState behaviorState;
    private long behaviorStateChangedAt;

    private Location location;
    private Location targetLocation;
    private Location lastMovementCheckLocation;

    private long nextTargetUpdateAt;
    private long nextStrafeSwitchAt;
    private long idleUntil;
    private long nextIdleCheckAt;
    private long nextSpeedChangeAt;
    private long nextCombatMoveAt;
    private long attackChargeUntil;
    private long hitReactUntil;
    private long lastDamageAt;
    private long combatResetUntil;
    private long combatBurstUntil;
    private long aimSettleUntil;
    private long lastSeenTargetAt;
    private float yaw;
    private float pitch;
    private long nextLookOffsetAt;
    private double lookYawOffset;
    private double speed;
    private double speedMultiplier;
    private double strafeDirection;
    private double knockbackX;
    private double knockbackZ;
    private double movementMomentumX;
    private double movementMomentumZ;
    private int stuckTicks;

    private double maxHealth;
    private double health;
    private boolean alive;
    private boolean manualSpawn;
    private long respawnAt;
    private boolean pitCoreRespawnPending;
    private ItemType mainHand;
    private ItemType helmet;
    private ItemType chestPlate;
    private ItemType leggings;
    private ItemType boots;
    private ItemStack mainHandItem;
    private ItemStack helmetItem;
    private ItemStack chestPlateItem;
    private ItemStack leggingsItem;
    private ItemStack bootsItem;

    private UUID targetPlayerId;
    private UUID profileSourcePlayerId;
    private long targetLockedUntil;
    private long targetCommitUntil;
    private long nextAttackAt;
    private double attackDamage;
    private double followRange;
    private double attackRange;
    private double preferredCombatDistance;
    private Location lastSeenTargetLocation;
    private long obstacleRepositionUntil;
    private double obstacleStrafeDirection;
    private long lastDirectLineOfSightAt;
    private long searchUntil;
    private double reactionTimeMultiplier;
    private double strafeStrengthMultiplier;
    private long respawnSettleUntil;
    private BotPersonality personality;

    public PitBot(UUID uniqueId, String name, Location location, int entityId) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.location = location == null ? null : location.clone();
        this.targetLocation = null;
        this.lastMovementCheckLocation = location == null ? null : location.clone();
        this.entityId = entityId;
        this.tier = BotTier.NORMAL;
        this.behaviorState = BotBehaviorState.IDLE;
        this.behaviorStateChangedAt = System.currentTimeMillis();
        this.nextTargetUpdateAt = 0L;
        this.nextStrafeSwitchAt = 0L;
        this.idleUntil = 0L;
        this.nextIdleCheckAt = 0L;
        this.nextSpeedChangeAt = 0L;
        this.nextCombatMoveAt = 0L;
        this.attackChargeUntil = 0L;
        this.hitReactUntil = 0L;
        this.lastDamageAt = 0L;
        this.combatResetUntil = 0L;
        this.combatBurstUntil = 0L;
        this.aimSettleUntil = 0L;
        this.lastSeenTargetAt = 0L;
        this.yaw = location != null ? location.getYaw() : 0.0F;
        this.pitch = location != null ? location.getPitch() : 0.0F;
        this.nextLookOffsetAt = 0L;
        this.lookYawOffset = 0.0D;
        this.speed = 0.20D;
        this.speedMultiplier = 1.0D;
        this.strafeDirection = 1.0D;
        this.knockbackX = 0.0D;
        this.knockbackZ = 0.0D;
        this.movementMomentumX = 0.0D;
        this.movementMomentumZ = 0.0D;
        this.stuckTicks = 0;

        this.maxHealth = 20.0D;
        this.health = 20.0D;
        this.alive = true;
        this.manualSpawn = false;
        this.respawnAt = 0L;
        this.pitCoreRespawnPending = false;
        this.mainHand = ItemTypes.AIR;
        this.helmet = ItemTypes.AIR;
        this.chestPlate = ItemTypes.AIR;
        this.leggings = ItemTypes.AIR;
        this.boots = ItemTypes.AIR;
        this.mainHandItem = null;
        this.helmetItem = null;
        this.chestPlateItem = null;
        this.leggingsItem = null;
        this.bootsItem = null;

        this.targetPlayerId = null;
        this.profileSourcePlayerId = null;
        this.targetLockedUntil = 0L;
        this.targetCommitUntil = 0L;
        this.nextAttackAt = 0L;
        this.attackDamage = 1.0D;
        this.followRange = 8.0D;
        this.attackRange = 1.9D;
        this.preferredCombatDistance = 1.6D;
        this.lastSeenTargetLocation = null;
        this.obstacleRepositionUntil = 0L;
        this.obstacleStrafeDirection = 1.0D;
        this.lastDirectLineOfSightAt = 0L;
        this.searchUntil = 0L;
        this.reactionTimeMultiplier = 1.0D;
        this.strafeStrengthMultiplier = 1.0D;
        this.respawnSettleUntil = 0L;
        this.personality = BotPersonality.neutral();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public BotSkin getSkin() {
        return skin;
    }

    public void setSkin(BotSkin skin) {
        this.skin = skin;
    }

    public BotTier getTier() {
        return tier;
    }

    public void setTier(BotTier tier) {
        this.tier = tier == null ? BotTier.NORMAL : tier;
    }

    public BotBehaviorState getBehaviorState() {
        return behaviorState;
    }

    public void setBehaviorState(BotBehaviorState behaviorState) {
        BotBehaviorState safeState = behaviorState == null ? BotBehaviorState.IDLE : behaviorState;
        if (this.behaviorState != safeState) {
            this.behaviorStateChangedAt = System.currentTimeMillis();
        }
        this.behaviorState = safeState;
    }

    public long getBehaviorStateChangedAt() {
        return behaviorStateChangedAt;
    }

    public void setBehaviorStateChangedAt(long behaviorStateChangedAt) {
        this.behaviorStateChangedAt = behaviorStateChangedAt;
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    public void setLocation(Location location) {
        this.location = location == null ? null : location.clone();
    }

    public Location getTargetLocation() {
        return targetLocation == null ? null : targetLocation.clone();
    }

    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation == null ? null : targetLocation.clone();
    }

    public Location getLastMovementCheckLocation() {
        return lastMovementCheckLocation == null ? null : lastMovementCheckLocation.clone();
    }

    public void setLastMovementCheckLocation(Location lastMovementCheckLocation) {
        this.lastMovementCheckLocation = lastMovementCheckLocation == null ? null : lastMovementCheckLocation.clone();
    }

    public long getNextTargetUpdateAt() {
        return nextTargetUpdateAt;
    }

    public void setNextTargetUpdateAt(long nextTargetUpdateAt) {
        this.nextTargetUpdateAt = nextTargetUpdateAt;
    }

    public long getNextStrafeSwitchAt() {
        return nextStrafeSwitchAt;
    }

    public void setNextStrafeSwitchAt(long nextStrafeSwitchAt) {
        this.nextStrafeSwitchAt = nextStrafeSwitchAt;
    }

    public long getIdleUntil() {
        return idleUntil;
    }

    public void setIdleUntil(long idleUntil) {
        this.idleUntil = idleUntil;
    }

    public long getNextIdleCheckAt() {
        return nextIdleCheckAt;
    }

    public void setNextIdleCheckAt(long nextIdleCheckAt) {
        this.nextIdleCheckAt = nextIdleCheckAt;
    }

    public long getNextSpeedChangeAt() {
        return nextSpeedChangeAt;
    }

    public void setNextSpeedChangeAt(long nextSpeedChangeAt) {
        this.nextSpeedChangeAt = nextSpeedChangeAt;
    }

    public long getNextCombatMoveAt() {
        return nextCombatMoveAt;
    }

    public void setNextCombatMoveAt(long nextCombatMoveAt) {
        this.nextCombatMoveAt = nextCombatMoveAt;
    }

    public long getAttackChargeUntil() {
        return attackChargeUntil;
    }

    public void setAttackChargeUntil(long attackChargeUntil) {
        this.attackChargeUntil = attackChargeUntil;
    }

    public long getHitReactUntil() {
        return hitReactUntil;
    }

    public void setHitReactUntil(long hitReactUntil) {
        this.hitReactUntil = hitReactUntil;
    }

    public long getLastDamageAt() {
        return lastDamageAt;
    }

    public void setLastDamageAt(long lastDamageAt) {
        this.lastDamageAt = lastDamageAt;
    }

    public long getCombatResetUntil() {
        return combatResetUntil;
    }

    public void setCombatResetUntil(long combatResetUntil) {
        this.combatResetUntil = combatResetUntil;
    }

    public long getCombatBurstUntil() {
        return combatBurstUntil;
    }

    public void setCombatBurstUntil(long combatBurstUntil) {
        this.combatBurstUntil = combatBurstUntil;
    }

    public long getAimSettleUntil() {
        return aimSettleUntil;
    }

    public void setAimSettleUntil(long aimSettleUntil) {
        this.aimSettleUntil = aimSettleUntil;
    }

    public long getLastSeenTargetAt() {
        return lastSeenTargetAt;
    }

    public void setLastSeenTargetAt(long lastSeenTargetAt) {
        this.lastSeenTargetAt = lastSeenTargetAt;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public long getNextLookOffsetAt() {
        return nextLookOffsetAt;
    }

    public void setNextLookOffsetAt(long nextLookOffsetAt) {
        this.nextLookOffsetAt = nextLookOffsetAt;
    }

    public double getLookYawOffset() {
        return lookYawOffset;
    }

    public void setLookYawOffset(double lookYawOffset) {
        this.lookYawOffset = lookYawOffset;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public double getStrafeDirection() {
        return strafeDirection;
    }

    public void setStrafeDirection(double strafeDirection) {
        this.strafeDirection = strafeDirection;
    }

    public double getKnockbackX() {
        return knockbackX;
    }

    public void setKnockbackX(double knockbackX) {
        this.knockbackX = knockbackX;
    }

    public double getKnockbackZ() {
        return knockbackZ;
    }

    public void setKnockbackZ(double knockbackZ) {
        this.knockbackZ = knockbackZ;
    }

    public double getMovementMomentumX() {
        return movementMomentumX;
    }

    public void setMovementMomentumX(double movementMomentumX) {
        this.movementMomentumX = movementMomentumX;
    }

    public double getMovementMomentumZ() {
        return movementMomentumZ;
    }

    public void setMovementMomentumZ(double movementMomentumZ) {
        this.movementMomentumZ = movementMomentumZ;
    }

    public int getStuckTicks() {
        return stuckTicks;
    }

    public void setStuckTicks(int stuckTicks) {
        this.stuckTicks = stuckTicks;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isDead() {
        return !alive;
    }

    public void setDead(boolean dead) {
        this.alive = !dead;
    }

    public boolean isManualSpawn() {
        return manualSpawn;
    }

    public void setManualSpawn(boolean manualSpawn) {
        this.manualSpawn = manualSpawn;
    }

    public long getRespawnAt() {
        return respawnAt;
    }

    public void setRespawnAt(long respawnAt) {
        this.respawnAt = respawnAt;
    }

    public boolean isPitCoreRespawnPending() {
        return pitCoreRespawnPending;
    }

    public void setPitCoreRespawnPending(boolean pitCoreRespawnPending) {
        this.pitCoreRespawnPending = pitCoreRespawnPending;
    }

    public ItemType getMainHand() {
        return mainHand;
    }

    public void setMainHand(ItemType mainHand) {
        this.mainHand = mainHand == null ? ItemTypes.AIR : mainHand;
    }

    public ItemType getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemType helmet) {
        this.helmet = helmet == null ? ItemTypes.AIR : helmet;
    }

    public ItemType getChestPlate() {
        return chestPlate;
    }

    public void setChestPlate(ItemType chestPlate) {
        this.chestPlate = chestPlate == null ? ItemTypes.AIR : chestPlate;
    }

    public ItemType getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemType leggings) {
        this.leggings = leggings == null ? ItemTypes.AIR : leggings;
    }

    public ItemType getBoots() {
        return boots;
    }

    public void setBoots(ItemType boots) {
        this.boots = boots == null ? ItemTypes.AIR : boots;
    }

    public ItemStack getMainHandItem() {
        return mainHandItem == null ? null : mainHandItem.clone();
    }

    public void setMainHandItem(ItemStack mainHandItem) {
        this.mainHandItem = mainHandItem == null ? null : mainHandItem.clone();
    }

    public ItemStack getHelmetItem() {
        return helmetItem == null ? null : helmetItem.clone();
    }

    public void setHelmetItem(ItemStack helmetItem) {
        this.helmetItem = helmetItem == null ? null : helmetItem.clone();
    }

    public ItemStack getChestPlateItem() {
        return chestPlateItem == null ? null : chestPlateItem.clone();
    }

    public void setChestPlateItem(ItemStack chestPlateItem) {
        this.chestPlateItem = chestPlateItem == null ? null : chestPlateItem.clone();
    }

    public ItemStack getLeggingsItem() {
        return leggingsItem == null ? null : leggingsItem.clone();
    }

    public void setLeggingsItem(ItemStack leggingsItem) {
        this.leggingsItem = leggingsItem == null ? null : leggingsItem.clone();
    }

    public ItemStack getBootsItem() {
        return bootsItem == null ? null : bootsItem.clone();
    }

    public void setBootsItem(ItemStack bootsItem) {
        this.bootsItem = bootsItem == null ? null : bootsItem.clone();
    }

    public UUID getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public UUID getProfileSourcePlayerId() {
        return profileSourcePlayerId;
    }

    public void setProfileSourcePlayerId(UUID profileSourcePlayerId) {
        this.profileSourcePlayerId = profileSourcePlayerId;
    }

    public long getTargetLockedUntil() {
        return targetLockedUntil;
    }

    public void setTargetLockedUntil(long targetLockedUntil) {
        this.targetLockedUntil = targetLockedUntil;
    }

    public long getTargetCommitUntil() {
        return targetCommitUntil;
    }

    public void setTargetCommitUntil(long targetCommitUntil) {
        this.targetCommitUntil = targetCommitUntil;
    }

    public long getNextAttackAt() {
        return nextAttackAt;
    }

    public void setNextAttackAt(long nextAttackAt) {
        this.nextAttackAt = nextAttackAt;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    public double getFollowRange() {
        return followRange;
    }

    public void setFollowRange(double followRange) {
        this.followRange = followRange;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(double attackRange) {
        this.attackRange = attackRange;
    }

    public double getPreferredCombatDistance() {
        return preferredCombatDistance;
    }

    public void setPreferredCombatDistance(double preferredCombatDistance) {
        this.preferredCombatDistance = preferredCombatDistance;
    }

    public Location getLastSeenTargetLocation() {
        return lastSeenTargetLocation == null ? null : lastSeenTargetLocation.clone();
    }

    public void setLastSeenTargetLocation(Location lastSeenTargetLocation) {
        this.lastSeenTargetLocation = lastSeenTargetLocation == null ? null : lastSeenTargetLocation.clone();
    }

    public long getObstacleRepositionUntil() {
        return obstacleRepositionUntil;
    }

    public void setObstacleRepositionUntil(long obstacleRepositionUntil) {
        this.obstacleRepositionUntil = obstacleRepositionUntil;
    }

    public double getObstacleStrafeDirection() {
        return obstacleStrafeDirection;
    }

    public void setObstacleStrafeDirection(double obstacleStrafeDirection) {
        this.obstacleStrafeDirection = obstacleStrafeDirection;
    }

    public long getLastDirectLineOfSightAt() {
        return lastDirectLineOfSightAt;
    }

    public void setLastDirectLineOfSightAt(long lastDirectLineOfSightAt) {
        this.lastDirectLineOfSightAt = lastDirectLineOfSightAt;
    }

    public long getSearchUntil() {
        return searchUntil;
    }

    public void setSearchUntil(long searchUntil) {
        this.searchUntil = searchUntil;
    }

    public double getReactionTimeMultiplier() {
        return reactionTimeMultiplier;
    }

    public void setReactionTimeMultiplier(double reactionTimeMultiplier) {
        this.reactionTimeMultiplier = reactionTimeMultiplier;
    }

    public double getStrafeStrengthMultiplier() {
        return strafeStrengthMultiplier;
    }

    public void setStrafeStrengthMultiplier(double strafeStrengthMultiplier) {
        this.strafeStrengthMultiplier = strafeStrengthMultiplier;
    }

    public long getRespawnSettleUntil() {
        return respawnSettleUntil;
    }

    public void setRespawnSettleUntil(long respawnSettleUntil) {
        this.respawnSettleUntil = respawnSettleUntil;
    }

    public BotPersonality getPersonality() {
        return personality;
    }

    public void setPersonality(BotPersonality personality) {
        this.personality = personality == null ? BotPersonality.neutral() : personality;
    }

    public void resetForRespawn(Location respawnLocation) {
        this.location = respawnLocation == null ? null : respawnLocation.clone();
        this.targetLocation = null;
        this.lastMovementCheckLocation = respawnLocation == null ? null : respawnLocation.clone();
        this.nextTargetUpdateAt = 0L;
        this.nextStrafeSwitchAt = 0L;
        this.idleUntil = 0L;
        this.nextIdleCheckAt = 0L;
        this.nextSpeedChangeAt = 0L;
        this.nextCombatMoveAt = 0L;
        this.hitReactUntil = 0L;
        this.lastDamageAt = 0L;
        this.combatResetUntil = 0L;
        this.combatBurstUntil = 0L;
        this.aimSettleUntil = 0L;
        this.lastSeenTargetAt = 0L;
        this.nextLookOffsetAt = 0L;
        this.lookYawOffset = 0.0D;
        this.lastSeenTargetLocation = null;
        this.speedMultiplier = 1.0D;
        this.knockbackX = 0.0D;
        this.knockbackZ = 0.0D;
        this.movementMomentumX = 0.0D;
        this.movementMomentumZ = 0.0D;
        this.health = this.maxHealth;
        this.alive = true;
        this.respawnAt = 0L;
        this.pitCoreRespawnPending = false;
        this.targetPlayerId = null;
        this.profileSourcePlayerId = null;
        this.targetLockedUntil = 0L;
        this.targetCommitUntil = 0L;
        this.nextAttackAt = 0L;
        this.stuckTicks = 0;
        this.behaviorState = BotBehaviorState.RESET;
        this.obstacleRepositionUntil = 0L;
        this.obstacleStrafeDirection = 1.0D;
        this.lastDirectLineOfSightAt = 0L;
        this.searchUntil = 0L;
        this.reactionTimeMultiplier = 1.0D;
        this.strafeStrengthMultiplier = 1.0D;
        this.respawnSettleUntil = 0L;

        if (respawnLocation != null) {
            this.yaw = respawnLocation.getYaw();
            this.pitch = respawnLocation.getPitch();
        } else {
            this.yaw = 0.0F;
            this.pitch = 0.0F;
        }

        this.behaviorState = BotBehaviorState.RESET;
        this.behaviorStateChangedAt = System.currentTimeMillis();
    }

    public boolean damage(double amount) {
        if (!alive) {
            return false;
        }

        this.health -= amount;

        if (this.health <= 0.0D) {
            this.health = 0.0D;
            this.alive = false;
            this.targetPlayerId = null;
            this.profileSourcePlayerId = null;
            this.targetLockedUntil = 0L;
            this.targetCommitUntil = 0L;
            this.hitReactUntil = 0L;
            this.lastDamageAt = 0L;
            this.combatResetUntil = 0L;
            this.combatBurstUntil = 0L;
            this.aimSettleUntil = 0L;
            this.lastSeenTargetAt = 0L;
            this.nextLookOffsetAt = 0L;
            this.lookYawOffset = 0.0D;
            this.lastSeenTargetLocation = null;
            this.obstacleRepositionUntil = 0L;
            this.obstacleStrafeDirection = 1.0D;
            this.lastDirectLineOfSightAt = 0L;
            this.behaviorState = BotBehaviorState.RESET;
            this.behaviorStateChangedAt = System.currentTimeMillis();
            this.searchUntil = 0L;
            this.knockbackX = 0.0D;
            this.knockbackZ = 0.0D;
            this.movementMomentumX = 0.0D;
            this.movementMomentumZ = 0.0D;
            return true;
        }

        return false;
    }
}
