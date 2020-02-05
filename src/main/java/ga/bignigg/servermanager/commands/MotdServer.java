package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Main.serverThreadHashMap;
import static ga.bignigg.servermanager.Utils.*;
import static ga.bignigg.servermanager.Utils.sendmsg;

public class MotdServer extends Command {
    public MotdServer() {
        super("motd", "", "motdserver", "ms");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String motd = null;
            int motdstart;
            String servername = null;
            if (args.length>0) {
                servername = args[0].replaceAll("\\W", "");
            }
            if (serverThreadHashMap.containsKey(servername)) {
                motdstart = 1;
            } else {  // default to server player is on
                servername = ProxyServer.getInstance().getPlayer(s.getName()).getServer().getInfo().getName();
                motdstart = 0;
            }
            if(s.hasPermission("msm."+servername+".motd")) {
                if (args.length>motdstart) { // generate motd string
                    motd = args[motdstart];
                    for (int i = motdstart+1; i < args.length; i++) { motd = motd+" "+args[i]; }
                }
                if (motd==null) {
                    getServerClass(servername).loadServerData(); // reload motd
                    sendmsg(s, msg("reload_motd").replace("%server%", servername), ChatColor.GREEN);
                } else {
                    getServerClass(servername).setMotd(motd); // set motd
                    sendmsg(s, msg("set_motd").replace("%server%", servername).replace("%motd%", motd), ChatColor.GREEN);
                }
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}