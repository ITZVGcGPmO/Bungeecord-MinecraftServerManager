package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
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
            String playername = s.getName();
            if (args.length > 1) {
                playername = args[1];
            }
            ProxiedPlayer pl = plugin.getProxy().getPlayer(playername);
            ServerInfo sinf = pl.getServer().getInfo();
            if (playername.equals(s.getName()) && !s.hasPermission("msm."+sinf.getName()+".denyreconnect") || s.hasPermission("msm."+sinf.getName()+".reconnectothers")) {
                sendmsg(s, msg("player_reconnect"), ChatColor.GREEN);
                pl.connect(plugin.getProxy().getServerInfo(config.getString("idle_server"))); // send to idle server
                new Thread(() -> {
                    try {
                        // wait some time for player to log into idle server & then try to reconnect them to destination
                        Thread.sleep(config.getInt("time_error_margin_ms"));
                        tryReconnect(pl, sinf);
                    } catch (InterruptedException ignored) { }
                }).start();
            } else {
                sendmsg(s, msg("no_permission"), ChatColor.RED);
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
