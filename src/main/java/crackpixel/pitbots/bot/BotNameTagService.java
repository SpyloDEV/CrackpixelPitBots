package crackpixel.pitbots.bot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;

public class BotNameTagService {

    private static final String TEAM_PREFIX = "pb";

    private final Scoreboard scoreboard;

    public BotNameTagService() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void registerBot(PitBot bot) {
        if (bot == null) {
            return;
        }

        applyBotToScoreboard(scoreboard, bot);
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyBotToScoreboard(player == null ? null : player.getScoreboard(), bot);
        }
        updateHealth(bot);
    }

    public void updateHealth(PitBot bot) {
        if (bot == null) {
            return;
        }

        updateHealthOnScoreboard(scoreboard, bot);
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateHealthOnScoreboard(player == null ? null : player.getScoreboard(), bot);
        }
    }

    public void unregisterBot(PitBot bot) {
        if (bot == null) {
            return;
        }

        removeBotFromScoreboard(scoreboard, bot);
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBotFromScoreboard(player == null ? null : player.getScoreboard(), bot);
        }
    }

    public void clear(Collection<PitBot> bots) {
        if (bots == null) {
            return;
        }

        for (PitBot bot : bots) {
            unregisterBot(bot);
        }
    }

    private void applyBotToScoreboard(Scoreboard target, PitBot bot) {
        Team team = getOrCreateTeam(target, bot);
        if (team == null) {
            return;
        }

        if (!team.hasEntry(bot.getName())) {
            team.addEntry(bot.getName());
        }
    }

    private void updateHealthOnScoreboard(Scoreboard target, PitBot bot) {
        Team team = getOrCreateTeam(target, bot);
        if (team == null) {
            return;
        }

        team.setPrefix(ChatColor.GRAY.toString());
        team.setSuffix(ChatColor.DARK_GRAY + " [" + healthColor(bot) + formatHealth(bot.getHealth()) + "HP" + ChatColor.DARK_GRAY + "]");
    }

    private void removeBotFromScoreboard(Scoreboard target, PitBot bot) {
        if (target == null || bot == null) {
            return;
        }

        Team team = target.getTeam(teamName(bot));
        if (team == null) {
            return;
        }

        if (team.hasEntry(bot.getName())) {
            team.removeEntry(bot.getName());
        }

        team.unregister();
    }

    private Team getOrCreateTeam(Scoreboard target, PitBot bot) {
        if (target == null || bot == null) {
            return null;
        }

        String teamName = teamName(bot);
        Team team = target.getTeam(teamName);

        if (team == null) {
            team = target.registerNewTeam(teamName);
            team.setAllowFriendlyFire(true);
            team.setCanSeeFriendlyInvisibles(false);
        }

        return team;
    }

    private String teamName(PitBot bot) {
        String unique = bot.getUniqueId().toString().replace("-", "");
        String value = TEAM_PREFIX + unique.substring(0, Math.min(14, unique.length()));
        return value.length() > 16 ? value.substring(0, 16) : value;
    }

    private ChatColor healthColor(PitBot bot) {
        if (bot.getMaxHealth() <= 0.0D) {
            return ChatColor.GREEN;
        }

        double percent = bot.getHealth() / bot.getMaxHealth();
        if (percent <= 0.35D) {
            return ChatColor.RED;
        }

        if (percent <= 0.65D) {
            return ChatColor.YELLOW;
        }

        return ChatColor.GREEN;
    }

    private String formatHealth(double health) {
        int rounded = (int) Math.ceil(Math.max(0.0D, health));
        return String.valueOf(rounded);
    }
}
