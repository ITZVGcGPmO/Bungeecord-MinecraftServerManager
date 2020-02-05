package ga.bignigg.servermanager.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import static ga.bignigg.servermanager.Utils.*;

public class CreateServer extends Command {
    public CreateServer() {
        super("create", "", "cs", "createserver");
    }

    @Override
    public void execute(CommandSender s, String[] args) {
        try {
            if(args.length<1) {
                sendmsg(s, msg("createserver"), ChatColor.RED);
            } else {
                // server name is args[0], optional mcversion is args[1]
                String servername = args[0].replaceAll("\\W", "");
                if(s.hasPermission("msm."+servername+".create")) {
                    String dist = "paper";
                    if (args.length >1) {
                        dist = args[1];
                    }
                    String mcver = "latest";
                    if (args.length >2) {
                        mcver = args[2].replaceAll("[^\\w\\.]", "");
                    }
                    String distver = "latest";
                    if (args.length >3) {
                        distver = args[3];
                    }
                    boolean is_restricted = false;
                    if (args.length >4 && args[4].equalsIgnoreCase("true")) {
                        is_restricted = true;
                    }
                    s.sendMessage(new ComponentBuilder(msg("server_creating").replace("%server%", servername)).color(ChatColor.GREEN).create());
                    getServerClass(servername).generateServer(dist, mcver, distver, is_restricted);
                    s.sendMessage(new ComponentBuilder(msg("server_created").replace("%server%", servername)).color(ChatColor.GREEN).create());
                } else {
                    sendmsg(s, msg("no_permission"), ChatColor.RED);
                }
            }
        } catch (Exception e) {
            dumpError(s, e);
        }
    }
}
