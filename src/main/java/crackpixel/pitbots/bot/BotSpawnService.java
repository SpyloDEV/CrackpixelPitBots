package crackpixel.pitbots.bot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotSpawnService {

    private final Random random = new Random();

    private Location centerLocation;
    private Location cornerOne;
    private Location cornerTwo;
    private double minRadius = 0.0D;
    private double maxRadius = 8.75D;
    private double centerSpawnRadius = 1.0D;
    private boolean arenaBoundsEnabled = false;
    private boolean edgeSpawnsEnabled = true;
    private double edgeSpawnInset = 1.5D;
    private double edgeSpawnJitter = 2.25D;
    private double avoidPlayerRadius = 5.5D;
    private final List<Location> edgeSpawnPoints = new ArrayList<Location>();
    private int nextEdgeSpawnIndex = 0;

    public void loadCenter(FileConfiguration config) {
        if (config == null) {
            return;
        }

        minRadius = positive(config.getDouble("spawn.min-radius", minRadius), 0.0D);
        maxRadius = positive(config.getDouble("spawn.max-radius", maxRadius), minRadius);
        centerSpawnRadius = positive(config.getDouble("spawn.center-spawn-radius", centerSpawnRadius), 0.0D);
        edgeSpawnsEnabled = config.getBoolean("spawn.edge-spawns-enabled", edgeSpawnsEnabled);
        edgeSpawnInset = positive(config.getDouble("spawn.edge-spawn-inset", edgeSpawnInset), 0.0D);
        edgeSpawnJitter = positive(config.getDouble("spawn.edge-spawn-jitter", edgeSpawnJitter), 0.0D);
        avoidPlayerRadius = positive(config.getDouble("spawn.avoid-player-radius", avoidPlayerRadius), 0.0D);
        loadBounds(config);
        loadEdgeSpawnPoints(config);

        if (!config.isString("center.world")) {
            return;
        }

        World world = Bukkit.getWorld(config.getString("center.world"));
        if (world == null) {
            return;
        }

        setCenterLocation(new Location(
                world,
                config.getDouble("center.x"),
                config.getDouble("center.y"),
                config.getDouble("center.z"),
                (float) config.getDouble("center.yaw"),
                (float) config.getDouble("center.pitch")
        ));
    }

    public void saveCenter(FileConfiguration config) {
        if (config == null || centerLocation == null || centerLocation.getWorld() == null) {
            return;
        }

        saveLocation(config, "center", centerLocation);
    }

    public void saveBounds(FileConfiguration config) {
        if (config == null) {
            return;
        }

        config.set("arena-bounds.enabled", arenaBoundsEnabled);

        if (cornerOne == null || cornerOne.getWorld() == null) {
            config.set("arena-bounds.pos1", null);
        } else {
            saveLocation(config, "arena-bounds.pos1", cornerOne);
        }

        if (cornerTwo == null || cornerTwo.getWorld() == null) {
            config.set("arena-bounds.pos2", null);
        } else {
            saveLocation(config, "arena-bounds.pos2", cornerTwo);
        }
    }

    public void setCenterLocation(Location centerLocation) {
        this.centerLocation = centerLocation == null ? null : centerLocation.clone();
    }

    public Location getCenterLocation() {
        return centerLocation == null ? null : centerLocation.clone();
    }

    public void setCornerOne(Location cornerOne) {
        this.cornerOne = cornerOne == null ? null : cornerOne.clone();
        this.arenaBoundsEnabled = hasRawBounds();
    }

    public void setCornerTwo(Location cornerTwo) {
        this.cornerTwo = cornerTwo == null ? null : cornerTwo.clone();
        this.arenaBoundsEnabled = hasRawBounds();
    }

    public Location getCornerOne() {
        return cornerOne == null ? null : cornerOne.clone();
    }

    public Location getCornerTwo() {
        return cornerTwo == null ? null : cornerTwo.clone();
    }

    public void clearBounds() {
        this.cornerOne = null;
        this.cornerTwo = null;
        this.arenaBoundsEnabled = false;
    }

    public boolean hasBounds() {
        return arenaBoundsEnabled && hasRawBounds();
    }

    public boolean hasCenterLocation() {
        return centerLocation != null && centerLocation.getWorld() != null;
    }

    public boolean isInPitWorld(Location location) {
        return location != null
                && location.getWorld() != null
                && centerLocation != null
                && centerLocation.getWorld() == location.getWorld();
    }

    public boolean isInsidePitArea(Location location) {
        if (!isInPitWorld(location)) {
            return false;
        }

        if (hasBounds()) {
            return isInsideBounds(location);
        }

        return distanceSquared2D(location, centerLocation) <= maxRadius * maxRadius;
    }

    public Location createSpawnLocation() {
        Location fallback = edgeSpawnsEnabled ? createEdgeSpawnLocation() : createRandomLocation(0.0D, centerSpawnRadius);
        return createProtectedSpawnLocation(fallback);
    }

    public Location createTestSpawnLocation(Location preferredLocation) {
        if (preferredLocation != null && isInsidePitArea(preferredLocation)) {
            return createNearbyLocation(preferredLocation, 1.5D, 3.0D);
        }

        return createSpawnLocation();
    }

    public Location createRoamLocation() {
        if (hasBounds()) {
            return createBoundsLocation();
        }

        return createRandomLocation(minRadius, maxRadius);
    }

    public Location createCombatRoamLocation(Location anchor) {
        if (anchor == null || anchor.getWorld() == null || !isInPitWorld(anchor) || !isInsidePitArea(anchor)) {
            return createRoamLocation();
        }

        double localRoamRadius = Math.max(6.20D, Math.max(centerSpawnRadius + 4.80D, maxRadius - 1.10D));
        Location candidate = createNearbyLocation(anchor, 1.20D, localRoamRadius);
        if (candidate == null) {
            return createRoamLocation();
        }

        return keepInsideConfiguredArea(candidate);
    }

    public String getBoundsSummary() {
        if (!hasBounds()) {
            return "not set";
        }

        return cornerOne.getWorld().getName()
                + " "
                + minBlockX()
                + ","
                + minBlockZ()
                + " -> "
                + maxBlockX()
                + ","
                + maxBlockZ();
    }

    public void setMinRadius(double minRadius) {
        this.minRadius = Math.max(0.0D, minRadius);
    }

    public void setMaxRadius(double maxRadius) {
        this.maxRadius = Math.max(minRadius, maxRadius);
    }

    public double getMinRadius() {
        return minRadius;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public double getCenterSpawnRadius() {
        return centerSpawnRadius;
    }

    public void setCenterSpawnRadius(double centerSpawnRadius) {
        this.centerSpawnRadius = Math.max(0.0D, centerSpawnRadius);
    }

    private Location createRandomLocation(double min, double max) {
        if (!hasCenterLocation()) {
            return null;
        }

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double safeMax = Math.max(min, max);
        double radius = min + (random.nextDouble() * (safeMax - min));

        Location location = centerLocation.clone();
        location.setX(centerLocation.getX() + (Math.cos(angle) * radius));
        location.setZ(centerLocation.getZ() + (Math.sin(angle) * radius));
        location.setY(findSafeY(location));
        location.setYaw(random.nextFloat() * 360.0F);
        location.setPitch(0.0F);
        return location;
    }

    private Location createEdgeSpawnLocation() {
        if (!hasCenterLocation()) {
            return null;
        }

        if (!edgeSpawnPoints.isEmpty()) {
            return createConfiguredEdgeSpawnLocation();
        }

        if (hasBounds()) {
            return createBoundsEdgeLocation();
        }

        double safeInset = Math.max(0.0D, edgeSpawnInset);
        double safeJitter = Math.max(0.0D, edgeSpawnJitter);
        double ringRadius = Math.max(centerSpawnRadius, maxRadius - safeInset);
        double minRingRadius = Math.max(centerSpawnRadius, ringRadius - safeJitter);
        return createRandomLocation(minRingRadius, ringRadius);
    }

    private Location createConfiguredEdgeSpawnLocation() {
        if (edgeSpawnPoints.isEmpty()) {
            return null;
        }

        int startIndex = nextEdgeSpawnIndex;
        for (int attempts = 0; attempts < edgeSpawnPoints.size(); attempts++) {
            int index = (startIndex + attempts) % edgeSpawnPoints.size();
            Location base = edgeSpawnPoints.get(index);
            if (base == null || base.getWorld() == null) {
                continue;
            }

            Location candidate = base.clone();
            double jitter = Math.max(0.0D, edgeSpawnJitter);
            if (jitter > 0.0D) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = random.nextDouble() * jitter;
                candidate.setX(candidate.getX() + (Math.cos(angle) * radius));
                candidate.setZ(candidate.getZ() + (Math.sin(angle) * radius));
            }

            candidate = keepInsideConfiguredArea(candidate);
            candidate.setY(findSafeY(candidate));
            candidate.setYaw(random.nextFloat() * 360.0F);
            candidate.setPitch(0.0F);

            nextEdgeSpawnIndex = (index + 1) % edgeSpawnPoints.size();
            return candidate;
        }

        return null;
    }

    private Location createBoundsEdgeLocation() {
        int side = random.nextInt(4);
        double safeInset = Math.max(0.0D, edgeSpawnInset);
        double safeJitter = Math.max(0.0D, edgeSpawnJitter);
        double minX = minX() + safeInset;
        double maxX = maxX() - safeInset;
        double minZ = minZ() + safeInset;
        double maxZ = maxZ() - safeInset;

        Location location = centerLocation.clone();
        if (side == 0) {
            location.setX(minX + random.nextDouble() * Math.max(1.0D, maxX - minX));
            location.setZ(minZ + (random.nextDouble() * Math.max(0.25D, safeJitter)));
        } else if (side == 1) {
            location.setX(minX + random.nextDouble() * Math.max(1.0D, maxX - minX));
            location.setZ(maxZ - (random.nextDouble() * Math.max(0.25D, safeJitter)));
        } else if (side == 2) {
            location.setX(minX + (random.nextDouble() * Math.max(0.25D, safeJitter)));
            location.setZ(minZ + random.nextDouble() * Math.max(1.0D, maxZ - minZ));
        } else {
            location.setX(maxX - (random.nextDouble() * Math.max(0.25D, safeJitter)));
            location.setZ(minZ + random.nextDouble() * Math.max(1.0D, maxZ - minZ));
        }

        location.setY(findSafeY(location));
        location.setYaw(random.nextFloat() * 360.0F);
        location.setPitch(0.0F);
        return location;
    }

    private Location createProtectedSpawnLocation(Location fallback) {
        if (fallback == null || avoidPlayerRadius <= 0.0D) {
            return fallback;
        }

        for (int attempt = 0; attempt < 12; attempt++) {
            Location candidate = edgeSpawnsEnabled ? createEdgeSpawnLocation() : createRandomLocation(0.0D, centerSpawnRadius);
            if (candidate != null && isSafeFromPlayers(candidate)) {
                return candidate;
            }
        }

        return fallback;
    }

    private boolean isSafeFromPlayers(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        double minDistanceSquared = avoidPlayerRadius * avoidPlayerRadius;
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline() || player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                continue;
            }

            if (player.getWorld() != location.getWorld()) {
                continue;
            }

            if (player.getLocation().distanceSquared(location) < minDistanceSquared) {
                return false;
            }
        }

        return true;
    }

    private Location createBoundsLocation() {
        if (!hasBounds()) {
            return createRandomLocation(minRadius, maxRadius);
        }

        int minX = minBlockX();
        int maxX = maxBlockX();
        int minZ = minBlockZ();
        int maxZ = maxBlockZ();

        Location location = centerLocation.clone();
        location.setX(minX + random.nextDouble() * Math.max(1, maxX - minX));
        location.setZ(minZ + random.nextDouble() * Math.max(1, maxZ - minZ));
        location.setY(findSafeY(location));
        location.setYaw(random.nextFloat() * 360.0F);
        location.setPitch(0.0F);
        return location;
    }

    private Location createNearbyLocation(Location baseLocation, double min, double max) {
        if (baseLocation == null || baseLocation.getWorld() == null) {
            return createSpawnLocation();
        }

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double safeMax = Math.max(min, max);
        double radius = min + (random.nextDouble() * (safeMax - min));

        Location location = baseLocation.clone();
        location.setX(baseLocation.getX() + (Math.cos(angle) * radius));
        location.setZ(baseLocation.getZ() + (Math.sin(angle) * radius));
        location.setY(findSafeY(location));
        location.setYaw(random.nextFloat() * 360.0F);
        location.setPitch(0.0F);
        return location;
    }

    private void loadBounds(FileConfiguration config) {
        arenaBoundsEnabled = config.getBoolean("arena-bounds.enabled", false);
        cornerOne = loadLocation(config, "arena-bounds.pos1");
        cornerTwo = loadLocation(config, "arena-bounds.pos2");
    }

    private void loadEdgeSpawnPoints(FileConfiguration config) {
        edgeSpawnPoints.clear();
        nextEdgeSpawnIndex = 0;

        if (config == null) {
            return;
        }

        List<?> configured = config.getList("spawn.edge-spawn-points");
        if (configured == null) {
            return;
        }

        for (int index = 0; index < configured.size(); index++) {
            Location location = loadLocation(config, "spawn.edge-spawn-points." + index);
            if (location != null) {
                edgeSpawnPoints.add(location);
            }
        }
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.isString(path + ".world")) {
            return null;
        }

        World world = Bukkit.getWorld(config.getString(path + ".world"));
        if (world == null) {
            return null;
        }

        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }

    private void saveLocation(FileConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private Location keepInsideConfiguredArea(Location location) {
        if (location == null) {
            return null;
        }

        if (hasBounds()) {
            Location clamped = location.clone();
            clamped.setX(Math.max(minX(), Math.min(maxX(), clamped.getX())));
            clamped.setZ(Math.max(minZ(), Math.min(maxZ(), clamped.getZ())));
            return clamped;
        }

        if (!hasCenterLocation()) {
            return location;
        }

        if (isInsidePitArea(location)) {
            return location;
        }

        double edgeRadius = Math.max(centerSpawnRadius, maxRadius - Math.max(0.0D, edgeSpawnInset));
        double dx = location.getX() - centerLocation.getX();
        double dz = location.getZ() - centerLocation.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance <= 0.001D) {
            return centerLocation.clone();
        }

        Location clamped = location.clone();
        clamped.setX(centerLocation.getX() + ((dx / distance) * edgeRadius));
        clamped.setZ(centerLocation.getZ() + ((dz / distance) * edgeRadius));
        return clamped;
    }

    private boolean hasRawBounds() {
        return cornerOne != null
                && cornerTwo != null
                && cornerOne.getWorld() != null
                && cornerOne.getWorld() == cornerTwo.getWorld();
    }

    private boolean isInsideBounds(Location location) {
        return location.getX() >= minX()
                && location.getX() <= maxX()
                && location.getZ() >= minZ()
                && location.getZ() <= maxZ();
    }

    private int minBlockX() {
        return Math.min(cornerOne.getBlockX(), cornerTwo.getBlockX());
    }

    private int maxBlockX() {
        return Math.max(cornerOne.getBlockX(), cornerTwo.getBlockX());
    }

    private int minBlockZ() {
        return Math.min(cornerOne.getBlockZ(), cornerTwo.getBlockZ());
    }

    private int maxBlockZ() {
        return Math.max(cornerOne.getBlockZ(), cornerTwo.getBlockZ());
    }

    private double minX() {
        return Math.min(cornerOne.getX(), cornerTwo.getX());
    }

    private double maxX() {
        return Math.max(cornerOne.getX(), cornerTwo.getX());
    }

    private double minZ() {
        return Math.min(cornerOne.getZ(), cornerTwo.getZ());
    }

    private double maxZ() {
        return Math.max(cornerOne.getZ(), cornerTwo.getZ());
    }

    private double distanceSquared2D(Location first, Location second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return (dx * dx) + (dz * dz);
    }

    private double positive(double value, double min) {
        return value < min ? min : value;
    }

    private double findSafeY(Location location) {
        if (location == null || location.getWorld() == null) {
            return centerLocation == null ? 0.0D : centerLocation.getY();
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int centerY = centerLocation == null ? location.getBlockY() : centerLocation.getBlockY();
        int startY = Math.min(254, centerY + 16);
        int minY = Math.max(1, centerY - 48);

        for (int y = startY; y >= minY; y--) {
            Material feet = world.getBlockAt(x, y, z).getType();
            Material head = world.getBlockAt(x, y + 1, z).getType();
            Material below = world.getBlockAt(x, y - 1, z).getType();

            if (isPassable(feet) && isPassable(head) && below.isSolid()) {
                return y;
            }
        }

        int highestY = world.getHighestBlockYAt(x, z) + 1;
        if (highestY > 1 && highestY < 255) {
            return highestY;
        }

        return centerLocation == null ? location.getY() : centerLocation.getY();
    }

    private boolean isPassable(Material material) {
        return material == null || material == Material.AIR || !material.isSolid();
    }
}
