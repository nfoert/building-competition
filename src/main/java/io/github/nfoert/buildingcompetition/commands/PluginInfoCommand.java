package io.github.nfoert.buildingcompetition.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public class PluginInfoCommand {
    public int execute(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getExecutor().sendRichMessage("\n" +
                "<b><dark_aqua>Building Competition</dark_aqua></b> by <gray>nfoert</gray>\n" +
                "<click:open_url:'https://github.com/nfoert/building-competition'><blue><i>github.com/nfoert/building-competition</i></blue></click>\n");
        ctx.getSource().getExecutor().sendRichMessage(
                "<dark_aqua>Available commands:</dark_aqua>\n" +
                        "\n" +
                        "<aqua>/bc reload</aqua> <gray>- Reloads the configuration</gray>\n" +
                        "<aqua>/bc buildplot</aqua> <gray>- Builds a plot for the sender</gray>\n" +
                        "<aqua>/bc reset</aqua> <gray>- Resets the plot file</gray>\n" +
                        "<aqua>/bc info</aqua> <gray>- Get info for a plot, based on where you're standing</gray>\n" +
                        "<aqua>/bc pause</aqua> <gray>- Disable plot building</gray>\n" +
                        "<aqua>/bc unpause</aqua> <gray>- Enable plot building</gray>\n"
        );

        return Command.SINGLE_SUCCESS;
    }
}