package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Main.serverThreadHashMap;
import static ga.bignigg.servermanager.Utils.*;

public class StartServer extends Command {
    public StartServer() {
        super("start", "", "startserver", "st");
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
            if (s.hasPermission("msm."+servername+".start")) {
                // server name is args[0]
                if (serverThreadHashMap.containsKey(servername)) {
                    try {
                        sendmsg(s, msg("server_starting").replace("%server%", servername), ChatColor.GREEN);
                        getServerClass(servername).startServer();
                        sendmsg(s, msg("server_started").replace("%server%", servername), ChatColor.GREEN);
                    } catch (Exception e) {
                        sendmsg(s, msg("server_runnning").replace("%server%", servername), ChatColor.RED);
                    }
                } else {
                    sendmsg(s, msg("not_a_server").replace("%server%", servername), ChatColor.RED);
                }
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
