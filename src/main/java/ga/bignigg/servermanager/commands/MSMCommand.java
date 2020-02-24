package ga.bignigg.servermanager.commands;

import ga.bignigg.servermanager.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Utils.*;

public class MSMCommand extends Command {
    public MSMCommand() {
        super("msm", "", "MinecraftServerManager");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            if(args.length<1) {
                sendmsg(s, msg("subcommand"), ChatColor.RED);
            } else {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (s.hasPermission("msm.reload")) {
                        new Thread(() -> {
                            sendmsg(s, msg("reloading"), ChatColor.GREEN);
                            loadConfig();
                            sendmsg(s, msg("reloaded"), ChatColor.GREEN);
                        }).start();
                    } else {
                        sendmsg(s, msg("no_permission"), ChatColor.RED);
                    }
                } else {
                    int cmdstart = 0;
                    String cmd = args[cmdstart];
                    for (int i = cmdstart+1; i < args.length; i++) { cmd = cmd+" "+args[i]; }
//                    Main.log.info("command: "+cmd);
                    // just remove the `msm` prefix and try running command
                    Main.plugin.getProxy().getPluginManager().dispatchCommand(s, cmd);
                }
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
