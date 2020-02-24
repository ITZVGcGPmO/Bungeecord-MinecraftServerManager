package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Utils.*;
import static ga.bignigg.servermanager.Main.plugin;

public class RestartServer extends Command {
    public RestartServer() {
        super("restart", "", "restartserver", "rs");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String servername;
            if (args.length < 1) { // default to server player is on
                servername = plugin.getProxy().getPlayer(s.getName()).getServer().getInfo().getName();
                return;
            } else {
                servername = args[0].replaceAll("\\W", "");
            }
            if (s.hasPermission("msm."+servername+".restart")) {
                new Thread(() -> {
                    sendmsg(s, msg("server_restarting").replace("%server%", servername), ChatColor.GREEN);
                    if (getServerClass(servername).isRunning()) {
                        sendmsg(s, msg("server_stopping").replace("%server%", servername), ChatColor.GREEN);
                        try { getServerClass(servername).stopServer(); } catch (Exception ignored) { }
                    }
                    sendmsg(s, msg("server_starting").replace("%server%", servername), ChatColor.GREEN);
                    try { getServerClass(servername).startServer(); } catch (Exception ignored) { }
                    sendmsg(s, msg("server_restarted").replace("%server%", servername), ChatColor.GREEN);
                }).start();
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
