package crackpixel.pitbots.task;

import crackpixel.pitbots.bot.BotTier;
import crackpixel.pitbots.bot.PitBot;
import org.bukkit.Location;

final class BotMovementController {

    Location smooth(PitBot bot, Location current, Location desired, boolean engaged, boolean highPressure) {
        if (bot == null || current == null || desired == null || current.getWorld() != desired.getWorld()) {
            return desired == null ? current : desired.clone();
        }

        double dx = desired.getX() - current.getX();
        double dz = desired.getZ() - current.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));

        double currentVelocityX = bot.getMovementMomentumX();
        double currentVelocityZ = bot.getMovementMomentumZ();
        double currentSpeed = Math.sqrt((currentVelocityX * currentVelocityX) + (currentVelocityZ * currentVelocityZ));

        double stopRadius = resolveStopRadius(bot, engaged, highPressure);
        if (distance <= stopRadius) {
            currentVelocityX *= 0.28D;
            currentVelocityZ *= 0.28D;
            bot.setMovementMomentumX(currentVelocityX);
            bot.setMovementMomentumZ(currentVelocityZ);

            Location settled = current.clone();
            settled.setY(desired.getY());
            settled.setYaw(desired.getYaw());
            settled.setPitch(desired.getPitch());
            return settled;
        }

        double maxSpeed = resolveMaxSpeed(bot, engaged, highPressure);
        double minCruiseSpeed = resolveMinCruiseSpeed(bot, engaged, highPressure);
        double slowingDistance = resolveSlowingDistance(bot, engaged, highPressure, maxSpeed);
        double desiredSpeed = maxSpeed;
        if (distance < slowingDistance) {
            desiredSpeed = maxSpeed * (distance / Math.max(0.0001D, slowingDistance));
        }
        if (engaged && distance > stopRadius) {
            desiredSpeed = Math.max(minCruiseSpeed, desiredSpeed);
        }

        double desiredVelocityX = (dx / Math.max(0.0001D, distance)) * desiredSpeed;
        double desiredVelocityZ = (dz / Math.max(0.0001D, distance)) * desiredSpeed;

        double steeringX = desiredVelocityX - currentVelocityX;
        double steeringZ = desiredVelocityZ - currentVelocityZ;
        double steeringLength = Math.sqrt((steeringX * steeringX) + (steeringZ * steeringZ));
        double maxAcceleration = resolveMaxAcceleration(bot, engaged, highPressure);
        if (steeringLength > maxAcceleration && steeringLength > 0.0001D) {
            double scale = maxAcceleration / steeringLength;
            steeringX *= scale;
            steeringZ *= scale;
        }

        double nextVelocityX = currentVelocityX + steeringX;
        double nextVelocityZ = currentVelocityZ + steeringZ;
        double nextSpeed = Math.sqrt((nextVelocityX * nextVelocityX) + (nextVelocityZ * nextVelocityZ));

        double damping = resolveDamping(bot, distance, engaged, highPressure, currentSpeed, nextSpeed);
        nextVelocityX *= damping;
        nextVelocityZ *= damping;
        nextSpeed = Math.sqrt((nextVelocityX * nextVelocityX) + (nextVelocityZ * nextVelocityZ));

        if (nextSpeed > maxSpeed && nextSpeed > 0.0001D) {
            double scale = maxSpeed / nextSpeed;
            nextVelocityX *= scale;
            nextVelocityZ *= scale;
            nextSpeed = maxSpeed;
        }

        if (distance > (stopRadius + 0.08D) && engaged && nextSpeed < minCruiseSpeed) {
            double scale = minCruiseSpeed / Math.max(0.0001D, nextSpeed);
            nextVelocityX *= scale;
            nextVelocityZ *= scale;
            nextSpeed = minCruiseSpeed;
        }

        if (distance > 0.22D && nextSpeed < 0.018D) {
            double nudgeSpeed = Math.min(maxSpeed, Math.max(minCruiseSpeed, engaged ? 0.12D : 0.08D));
            nextVelocityX = (dx / Math.max(0.0001D, distance)) * nudgeSpeed;
            nextVelocityZ = (dz / Math.max(0.0001D, distance)) * nudgeSpeed;
            nextSpeed = nudgeSpeed;
        }

        if (nextSpeed > distance && nextSpeed > 0.0001D) {
            double scale = distance / nextSpeed;
            nextVelocityX *= scale;
            nextVelocityZ *= scale;
        }

        bot.setMovementMomentumX(nextVelocityX);
        bot.setMovementMomentumZ(nextVelocityZ);

        Location result = current.clone();
        result.setX(current.getX() + nextVelocityX);
        result.setY(desired.getY());
        result.setZ(current.getZ() + nextVelocityZ);
        result.setYaw(desired.getYaw());
        result.setPitch(desired.getPitch());
        return result;
    }

    void reset(PitBot bot) {
        if (bot == null) {
            return;
        }

        bot.setMovementMomentumX(0.0D);
        bot.setMovementMomentumZ(0.0D);
    }

    private double resolveMaxSpeed(PitBot bot, boolean engaged, boolean highPressure) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        double baseSpeed = bot == null ? 0.32D : bot.getSpeed() * Math.max(0.78D, bot.getSpeedMultiplier());
        double value;
        if (engaged) {
            value = highPressure ? baseSpeed * 1.42D : baseSpeed * 1.26D;
        } else {
            value = highPressure ? baseSpeed * 1.10D : baseSpeed * 0.98D;
        }

        if (bot != null && bot.getPersonality() != null) {
            value *= engaged ? bot.getPersonality().getChaseBias() : bot.getPersonality().getRoamBias();
        }

        if (tier == BotTier.EASY) {
            value *= 0.92D;
        } else if (tier == BotTier.TRYHARD) {
            value *= 1.06D;
        }

        return Math.max(engaged ? 0.18D : 0.12D, value);
    }

    private double resolveMinCruiseSpeed(PitBot bot, boolean engaged, boolean highPressure) {
        if (!engaged) {
            return highPressure ? 0.08D : 0.0D;
        }

        double baseSpeed = bot == null ? 0.26D : bot.getSpeed() * Math.max(0.75D, bot.getSpeedMultiplier());
        double value = highPressure ? baseSpeed * 0.62D : baseSpeed * 0.44D;
        if (bot != null && bot.getTier() == BotTier.EASY) {
            value *= 0.92D;
        } else if (bot != null && bot.getTier() == BotTier.TRYHARD) {
            value *= 1.05D;
        }
        if (bot != null && bot.getPersonality() != null) {
            value *= bot.getPersonality().getChaseBias();
        }
        return Math.max(highPressure ? 0.15D : 0.11D, value);
    }

    private double resolveMaxAcceleration(PitBot bot, boolean engaged, boolean highPressure) {
        BotTier tier = bot == null ? BotTier.NORMAL : bot.getTier();
        double value;
        if (engaged) {
            value = highPressure ? 0.18D : 0.14D;
        } else {
            value = highPressure ? 0.13D : 0.10D;
        }

        if (tier == BotTier.EASY) {
            value *= 0.90D;
        } else if (tier == BotTier.TRYHARD) {
            value *= 1.08D;
        }

        return Math.max(0.07D, Math.min(0.24D, value));
    }

    private double resolveSlowingDistance(PitBot bot, boolean engaged, boolean highPressure, double maxSpeed) {
        double value = engaged
                ? (highPressure ? 0.95D : 0.78D)
                : (highPressure ? 0.82D : 0.68D);
        value += maxSpeed * (engaged ? 1.10D : 0.82D);
        if (bot != null && bot.getTier() == BotTier.EASY) {
            value *= 1.08D;
        } else if (bot != null && bot.getTier() == BotTier.TRYHARD) {
            value *= 0.94D;
        }
        if (bot != null && bot.getPersonality() != null) {
            value *= engaged ? bot.getPersonality().getSpacingBias() : bot.getPersonality().getRoamBias();
        }
        return Math.max(0.44D, value);
    }

    private double resolveStopRadius(PitBot bot, boolean engaged, boolean highPressure) {
        double value = engaged ? (highPressure ? 0.055D : 0.070D) : 0.080D;
        if (bot != null && bot.getTier() == BotTier.TRYHARD) {
            value *= 0.92D;
        }
        return value;
    }

    private double resolveDamping(PitBot bot,
                                  double distance,
                                  boolean engaged,
                                  boolean highPressure,
                                  double currentSpeed,
                                  double nextSpeed) {
        double damping = 1.0D;
        double stopRadius = resolveStopRadius(bot, engaged, highPressure);
        if (distance <= stopRadius * 2.0D) {
            damping *= 0.58D;
        } else if (distance <= stopRadius * 3.5D) {
            damping *= 0.82D;
        }

        if (!engaged && nextSpeed < currentSpeed * 0.65D) {
            damping *= 0.96D;
        }

        return Math.max(0.40D, Math.min(1.0D, damping));
    }
}
