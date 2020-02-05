package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Utils.*;

public class StopServer extends Command {
    public StopServer() {
        super("stop", "", "stopserver", "sd");
    }
    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String servername;
            if(args.length<1) { // default to server player is on
                servername = ProxyServer.getInstance().getPlayer(s.getName()).getServer().getInfo().getName();
                return;
            } else {
                servername = args[0].replaceAll("\\W", "");
            }
            if(s.hasPermission("msm."+servername+".stop")) {
                try {
                    getServerClass(servername).stopServer(s);
                    sendmsg(s, msg("server_stopping").replace("%server%", servername), ChatColor.GREEN);
                } catch (Exception e) {
                    sendmsg(s, msg("server_not_runnning").replace("%server%", servername), ChatColor.RED);
                }
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
