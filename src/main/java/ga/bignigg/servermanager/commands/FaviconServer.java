package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Main.plugin;
import static ga.bignigg.servermanager.Main.serverThreadHashMap;
import static ga.bignigg.servermanager.Utils.*;

public class FaviconServer extends Command {
    public FaviconServer() {
        super("favicon", "", "faviconserver", "fs");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String favicon = null;
            String servername = null;
            if (args.length > 0) {
                servername = args[0].replaceAll("\\W", "");
            }
            if (serverThreadHashMap.containsKey(servername)) {
                if (args.length > 1) {
                    favicon = args[1];
                }
            } else { // default to server player is on
                servername = plugin.getProxy().getPlayer(s.getName()).getServer().getInfo().getName();
                if (args.length > 0) {
                    favicon = args[0];
                }
            }
            if (s.hasPermission("msm."+servername+".favicon")) {
                try {
                    getServerClass(servername).setFavicon(favicon);
                    if (favicon == null) {
                        sendmsg(s, msg("reload_favicon").replace("%server%", servername), ChatColor.GREEN);
                    } else {
                        sendmsg(s, msg("set_favicon").replace("%server%", servername).replace("%favicon%", favicon), ChatColor.GREEN);
                    }
                } catch (Exception e) {
                    sendmsg(s, msg("favicon_error"), ChatColor.RED);
                }
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
