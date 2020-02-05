package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Main.serverThreadHashMap;
import static ga.bignigg.servermanager.Utils.*;

public class DeleteServer extends Command {
    public DeleteServer() {
        super("delete", "msm.delete", "ds", "deleteserver");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            if(args.length<1) {
                s.sendMessage(new ComponentBuilder(msg("no_cmd_param").replace("%cmd%", "delete")).color(ChatColor.RED).create());
            } else {
                String servername = args[0].replaceAll("\\W", "");
                if(s.hasPermission("msm."+servername+".delete")) {
                    // server name is args[0]
                    s.sendMessage(new ComponentBuilder(msg("server_deleting").replace("%server%", servername)).color(ChatColor.GREEN).create());
                    getServerClass(servername).deleteServer();
                    s.sendMessage(new ComponentBuilder(msg("server_deleted").replace("%server%", servername)).color(ChatColor.GREEN).create());
                } else {
                    sendmsg(s, msg("no_permission"), ChatColor.RED);
                }
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
