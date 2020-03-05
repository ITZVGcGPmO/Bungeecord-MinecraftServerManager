package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import static ga.bignigg.servermanager.Main.config;
import static ga.bignigg.servermanager.Main.plugin;
import static ga.bignigg.servermanager.Utils.*;

public class RelogCommand extends Command {
    public RelogCommand() {
        super("relog", "", "reconnect", "rl");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            String playername = null;
            String servername = plugin.getProxy().getPlayer(s.getName()).getServer().getInfo().getName();
            if (args.length > 1 && s.hasPermission("msm."+servername+".reconnect.others")) {
                playername = args[1];
            } else if (s.hasPermission("msm."+servername+".reconnect.self")) {
                playername = s.getName();
            }
            if (playername==null) {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            } else {
                sendmsg(s, msg("player_reconnect"), ChatColor.GREEN);
                ProxiedPlayer pl = plugin.getProxy().getPlayer(playername);
                pl.connect(plugin.getProxy().getServerInfo(config.getString("idle_server"))); // send to idle server
                new Thread(() -> {
                    try {
                        // wait some time for player to log into idle server & then try to reconnect them to destination
                        Thread.sleep(config.getInt("time_error_margin_ms"));
                        tryReconnect(pl, plugin.getProxy().getServerInfo(servername));
                    } catch (InterruptedException ignored) { }
                }).start();
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
