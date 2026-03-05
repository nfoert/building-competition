package io.github.nfoert.buildingcompetition;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import static org.bukkit.Bukkit.getServer;

public class CommandsHelper {
    public LiteralCommandNode<CommandSourceStack> getCommands() {
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal("reload")
                .executes(ctx -> {
                    ctx.getSource().getExecutor().sendRichMessage("<b><dark_aqua>BC:</dark_aqua></b> <green>Configuration reloaded!</green>");

                    return Command.SINGLE_SUCCESS;
                });

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bc").executes(ctx -> {
                    ctx.getSource().getExecutor().sendRichMessage("\n" +
                            "<b><dark_aqua>Building Competition</dark_aqua></b> by <gray>nfoert</gray>\n" +
                            "<click:open_url:'https://github.com/nfoert/building-competition'><blue><i>github.com/nfoert/building-competition</i></blue></click>\n");
                    ctx.getSource().getExecutor().sendRichMessage("<dark_aqua>Available commands:</dark_aqua>\n" +
                            "\n" +
                            "<aqua>/bc reload</aqua> <gray>- Reloads the configuration</gray>");

                    return Command.SINGLE_SUCCESS;
                }).then(reloadCommand);

        return root.build();
    }
}
