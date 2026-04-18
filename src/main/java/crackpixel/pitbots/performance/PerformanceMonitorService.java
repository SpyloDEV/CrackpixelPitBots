package crackpixel.pitbots.performance;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class PerformanceMonitorService extends BukkitRunnable {

    private boolean enabled = true;
    private double reduceBelowTps = 18.5D;
    private double pauseBelowTps = 16.0D;
    private int laggedBotPercent = 50;

    private double currentTps = 20.0D;
    private long lastSampleAt = 0L;

    public void load(FileConfiguration config) {
        if (config == null) {
            return;
        }

        enabled = config.getBoolean("performance.enabled", enabled);
        reduceBelowTps = positive(config.getDouble("performance.tps-reduce-below", reduceBelowTps), 1.0D);
        pauseBelowTps = positive(config.getDouble("performance.tps-pause-below", pauseBelowTps), 1.0D);
        if (pauseBelowTps > reduceBelowTps) {
            pauseBelowTps = reduceBelowTps;
        }
        laggedBotPercent = Math.max(0, Math.min(100, config.getInt("performance.lagged-bot-percent", laggedBotPercent)));
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        if (lastSampleAt <= 0L) {
            lastSampleAt = now;
            return;
        }

        long elapsed = Math.max(1L, now - lastSampleAt);
        double sample = Math.min(20.0D, (20.0D * 1000.0D) / elapsed);
        currentTps = (currentTps * 0.75D) + (sample * 0.25D);
        lastSampleAt = now;
    }

    public int applyBotCap(int targetBotCount) {
        if (!enabled || targetBotCount <= 0) {
            return targetBotCount;
        }

        if (currentTps <= pauseBelowTps) {
            return 0;
        }

        if (currentTps <= reduceBelowTps) {
            return Math.max(1, (int) Math.ceil(targetBotCount * (laggedBotPercent / 100.0D)));
        }

        return targetBotCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public double getReduceBelowTps() {
        return reduceBelowTps;
    }

    public double getPauseBelowTps() {
        return pauseBelowTps;
    }

    public int getLaggedBotPercent() {
        return laggedBotPercent;
    }

    private double positive(double value, double min) {
        return value < min ? min : value;
    }
}
