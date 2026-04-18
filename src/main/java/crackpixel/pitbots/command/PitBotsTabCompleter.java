package crackpixel.pitbots.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PitBotsTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Collections.unmodifiableList(Arrays.asList(
            "menu",
            "setcenter",
            "pos1",
            "pos2",
            "clearbounds",
            "info",
            "debug",
            "toggle",
            "stats",
            "top",
            "count",
            "spawn",
            "clear",
            "reload"
    ));

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pitbots.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(SUB_COMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return filter(Collections.singletonList("PitBot"), args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String prefix = input == null ? "" : input.toLowerCase();
        List<String> result = new ArrayList<String>();

        for (String value : values) {
            if (value.toLowerCase().startsWith(prefix)) {
                result.add(value);
            }
        }

        return result;
    }
}
