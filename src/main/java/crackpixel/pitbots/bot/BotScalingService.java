package crackpixel.pitbots.bot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BotScalingService {

    private final List<ScalingRule> rules = new ArrayList<ScalingRule>();
    private boolean enabled = true;
    private int absoluteMaxBots = 28;
    private int minimumRealPlayers = 1;

    public BotScalingService() {
        loadDefaults();
    }

    public void load(FileConfiguration config) {
        if (config == null) {
            loadDefaults();
            return;
        }

        enabled = config.getBoolean("bot-scaling.enabled", enabled);
        absoluteMaxBots = Math.max(0, config.getInt("bot-scaling.absolute-max-bots", absoluteMaxBots));
        minimumRealPlayers = Math.max(0, config.getInt("bot-scaling.minimum-real-players", minimumRealPlayers));
        rules.clear();

        List<Map<?, ?>> configuredRules = config.getMapList("bot-scaling.rules");
        for (Map<?, ?> configuredRule : configuredRules) {
            Integer maxPlayers = asInteger(configuredRule.get("max-players"));
            Integer bots = asInteger(configuredRule.get("bots"));

            if (maxPlayers == null || bots == null) {
                continue;
            }

            rules.add(new ScalingRule(maxPlayers, Math.max(0, bots)));
        }

        if (rules.isEmpty()) {
            addDefaultRules();
        }

        Collections.sort(rules, new Comparator<ScalingRule>() {
            @Override
            public int compare(ScalingRule first, ScalingRule second) {
                return Integer.compare(first.maxPlayers, second.maxPlayers);
            }
        });
    }

    public int getTargetBotCount(int realPlayers) {
        if (!enabled) {
            return 0;
        }

        int safePlayers = Math.max(0, realPlayers);
        if (safePlayers < minimumRealPlayers) {
            return 0;
        }

        for (ScalingRule rule : rules) {
            if (safePlayers <= rule.maxPlayers) {
                return Math.min(rule.bots, absoluteMaxBots);
            }
        }

        return 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAbsoluteMaxBots() {
        return absoluteMaxBots;
    }

    public int getMinimumRealPlayers() {
        return minimumRealPlayers;
    }

    public List<String> describeRules() {
        List<String> description = new ArrayList<String>();

        for (ScalingRule rule : rules) {
            description.add("<=" + rule.maxPlayers + " players: " + rule.bots + " bots");
        }

        return description;
    }

    private void loadDefaults() {
        enabled = true;
        absoluteMaxBots = 28;
        minimumRealPlayers = 1;
        rules.clear();
        addDefaultRules();
    }

    private void addDefaultRules() {
        rules.add(new ScalingRule(9, 22));
        rules.add(new ScalingRule(15, 18));
        rules.add(new ScalingRule(20, 14));
        rules.add(new ScalingRule(30, 10));
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private static final class ScalingRule {

        private final int maxPlayers;
        private final int bots;

        private ScalingRule(int maxPlayers, int bots) {
            this.maxPlayers = maxPlayers;
            this.bots = bots;
        }
    }
}
