package io.github.nfoert.buildingcompetition;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import static org.bukkit.Bukkit.getServer;

public class Utils {
    /**
     * Helper function for sending a rich text message to the executor of the command
     *
     * @param context The command context
     * @param message The message to send to the executor of the command
     */
    public static void sendMessage(CommandContext<CommandSourceStack> context, String message) {
        var executor = context.getSource().getExecutor();

        if (executor != null) {
            executor.sendRichMessage(message);
        } else {
            getServer().getConsoleSender().sendRichMessage(message);
        }
    }

    /**
     * Get the username of the command executor
     * If the command executor is not a player, instead return N/A
     *
     * @param context The command context
     * @return The username or N/A
     */
    public static String getUsername(CommandContext<CommandSourceStack> context) {
        var executor = context.getSource().getExecutor();

        if (executor != null) {
            return executor.getName();
        } else {
            return "N/A";
        }
    }
}
