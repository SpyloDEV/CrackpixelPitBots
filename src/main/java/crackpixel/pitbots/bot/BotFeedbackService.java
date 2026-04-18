package crackpixel.pitbots.bot;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class BotFeedbackService {

    private final BotSettings botSettings;

    public BotFeedbackService(BotSettings botSettings) {
        this.botSettings = botSettings;
    }

    public void playHit(PitBot bot) {
        if (bot == null || !botSettings.isFeedbackEnabled()) {
            return;
        }

        Location location = bot.getLocation();
        playEffect(location, botSettings.getHitEffect(), botSettings.getHitEffectData());
        playSound(location, botSettings.getHitSound(), botSettings.getHitSoundVolume(), botSettings.getHitSoundPitch());
    }

    public void playKill(Player killer, PitBot bot) {
        if (bot == null || !botSettings.isFeedbackEnabled()) {
            return;
        }

        Location location = bot.getLocation();
        if (location == null && killer != null && killer.isOnline()) {
            location = killer.getLocation();
        }

        playEffect(location, botSettings.getKillEffect(), botSettings.getKillEffectData());
        playSound(location, botSettings.getKillSound(), botSettings.getKillSoundVolume(), botSettings.getKillSoundPitch());
    }

    private void playEffect(Location location, String effectName, int data) {
        if (location == null || location.getWorld() == null || effectName == null || effectName.trim().isEmpty()) {
            return;
        }

        Effect effect = parseEffect(effectName);
        if (effect == null) {
            return;
        }

        location.getWorld().playEffect(location, effect, data);
    }

    private void playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || location.getWorld() == null || soundName == null || soundName.trim().isEmpty()) {
            return;
        }

        Sound sound = parseSound(soundName);
        if (sound == null) {
            return;
        }

        World world = location.getWorld();
        world.playSound(location, sound, volume, pitch);
    }

    private Effect parseEffect(String name) {
        try {
            return Effect.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Sound parseSound(String name) {
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
