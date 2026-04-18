package crackpixel.pitbots.util;

import org.bukkit.Location;

public final class BotMath {

    private BotMath() {
    }

    public static double distanceSquared2D(Location first, Location second) {
        if (first == null || second == null || first.getWorld() != second.getWorld()) {
            return Double.MAX_VALUE;
        }

        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return (dx * dx) + (dz * dz);
    }

    public static Location moveTowards2D(Location current, Location target, double step) {
        if (current == null || target == null || current.getWorld() != target.getWorld()) {
            return current == null ? null : current.clone();
        }

        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));

        if (distance <= 0.0001D) {
            return current.clone();
        }

        if (step >= distance) {
            Location result = current.clone();
            result.setX(target.getX());
            result.setY(current.getY());
            result.setZ(target.getZ());
            return result;
        }

        Location result = current.clone();
        result.setX(current.getX() + ((dx / distance) * step));
        result.setY(current.getY());
        result.setZ(current.getZ() + ((dz / distance) * step));
        return result;
    }

    public static float calculateYaw(Location from, Location to) {
        if (from == null || to == null || from.getWorld() != to.getWorld()) {
            return 0.0F;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
