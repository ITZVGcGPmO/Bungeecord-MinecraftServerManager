package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Main.serverThreadHashMap;
import static ga.bignigg.servermanager.Utils.*;
import static ga.bignigg.servermanager.Utils.sendmsg;

public class ExecuteServer extends Command {
    public ExecuteServer() {
        super("exec", "", "execserver", "es");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String cmd = null;
            int cmdstart;
            String servername = null;
            if (args.length>0) {
                servername = args[0].replaceAll("\\W", "");
            }
            if (serverThreadHashMap.containsKey(servername)) {
                cmdstart = 1;
            } else {  // default to server player is on
                servername = ProxyServer.getInstance().getPlayer(s.getName()).getServer().getInfo().getName();
                cmdstart = 0;
            }
            if(s.hasPermission("msm."+servername+".exec")) {
                if (args.length>cmdstart) { // generate motd string
                    cmd = args[cmdstart];
                    for (int i = cmdstart+1; i < args.length; i++) { cmd = cmd+" "+args[i]; }
                }
                if (cmd==null) {
                    sendmsg(s, msg("executeserver"), ChatColor.RED);
                } else {
                    sendmsg(s, msg("executing_command").replace("%server%", servername).replace("%cmd%", cmd), ChatColor.GREEN);
                    getServerClass(servername).writeCmd(cmd);
                }
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
