package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Utils.*;

public class RestartServer extends Command {
    public RestartServer() {
        super("restart", "", "restartserver", "rs");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String servername;
            if (args.length < 1) { // default to server player is on
                servername = ProxyServer.getInstance().getPlayer(s.getName()).getServer().getInfo().getName();
                return;
            } else {
                servername = args[0].replaceAll("\\W", "");
            }
            if (s.hasPermission("msm."+servername+".restart")) {
                getServerClass(servername).restartServer(s);
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
