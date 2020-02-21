package ga.bignigg.servermanager;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.*;

import static ga.bignigg.servermanager.Main.*;
import static ga.bignigg.servermanager.Utils.*;

public class ListenerClass implements Listener {
    public static final UUID EMPTY_UUID = UUID.fromString("0-0-0-0-0");
    @EventHandler
    public void onTabComplete(TabCompleteEvent tce) {
        try {
            ProxiedPlayer p = (ProxiedPlayer) tce.getSender();
            if (tce.getCursor().matches("^/(?:stop|stopserver|sd) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "stop", true));
            } else if (tce.getCursor().matches("^/(?:start|startserver|st) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "start", false));
            } else if (tce.getCursor().matches("^/(?:restart|restartserver|rs) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "restart", true));
            } else if (tce.getCursor().matches("^/(?:motd|ms|motdserver) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "motd", null));
                String servername = p.getServer().getInfo().getName();
                if (p.hasPermission("msm."+servername+".motd")) {
                    tce.getSuggestions().add(serverThreadHashMap.get(servername).getMotd());
                }
            } else if(tce.getCursor().matches("^/(?:motd|ms|motdserver)(?: \\S*){2}")) {
                String servername = tce.getCursor().split(" ")[1];
                if (p.hasPermission("msm."+servername+".motd")) {
                    tce.getSuggestions().add(serverThreadHashMap.get(servername).getMotd());
                }
            } else if(tce.getCursor().matches("^/(?:favicon|fs|faviconserver) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "favicon", null));
            } else if(tce.getCursor().matches("^/(?:exec|es|execserver) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "exec", true));
            } else if(tce.getCursor().matches("^/(?:delete|ds|deleteserver) \\S*")) {
                tce.getSuggestions().addAll(getSuggestions(p, "delete", false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @EventHandler
    public void onLogin(LoginEvent le) {
        // forced hosts redirect
        String servername = getServerByHostname(le.getConnection().getVirtualHost().getHostString());
        le.getConnection().getListener().getServerPriority().set(0, servername);
    }
    @EventHandler
    public void onServerConnect(ServerConnectEvent sce) {
        String servername = sce.getTarget().getName();
        // sever autostartup
        if (serverThreadHashMap.containsKey(servername)) {
            serverThreadHashMap.get(servername).endSchShutdown();
        }
    }
    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent sde) {
        // server autoshutdown
        String servername = sde.getTarget().getName();
        if (serverThreadHashMap.containsKey(servername) && !config.getStringList("exempt_srvstop").contains(servername)) {
            serverThreadHashMap.get(servername).startSchShutdown();
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerKickEvent(ServerKickEvent ske) throws InterruptedException {
        String krsn = BaseComponent.toLegacyText(ske.getKickReasonComponent());
        ProxiedPlayer pl = ske.getPlayer();
        log.info(pl.getName()+" kicked: reason: "+krsn);
        if (!krsn.contains("banned")) {
            ServerInfo kfrom = ske.getKickedFrom();
            if (serverThreadHashMap.containsKey(kfrom.getName())) {
                tryReconnect(pl, kfrom, 25);
            }
            // send to idle server
            ske.setCancelServer(ProxyServer.getInstance().getServerInfo(config.getString("idle_server")));
            ske.setCancelled(true);
        }
    }
    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent ppe) {
        try {
            String servername = getServerByHostname(ppe.getConnection().getVirtualHost().getHostString());
            ServerThread srv = serverThreadHashMap.get(servername);
            List<String> hov = new ArrayList<>();
            if (srv.isRunning()) {
                hov.add("\u00A7a» Server is ONLINE «");
            } else {
                hov.add("\u00A7c» Server is OFFLINE «");
            }
            int playercount = 0;
            try {
                Collection<ProxiedPlayer> plrz = ProxyServer.getInstance().getServers().get(servername).getPlayers();
                playercount = plrz.size();
                Iterator<ProxiedPlayer> players = plrz.iterator();
                for (int i = 0; i < 20; i++) {
                    hov.add(players.next().getDisplayName());
                }
            } catch (Exception ignored) { }
//            String hover = "This is cool!\nHover!";
//            List<String> hov = Arrays.asList(hover.replaceAll("&", "\u00A7").replaceAll("\u00A7n", "\n").replaceAll("\\\\&", "&").split("\n")); // custom playerlist hover
            ServerPing.PlayerInfo[] infos = new ServerPing.PlayerInfo[hov.size()];
            for (int i = 0; i < infos.length; i++)
                infos[i] = new ServerPing.PlayerInfo(hov.get(i), EMPTY_UUID);
            Integer maxPlayers = srv.getMaxPlayers();
            if (maxPlayers == null) { // null sets maxplayers to "plusone" mode
                maxPlayers = playercount + 1;
            }
            ppe.setResponse(new ServerPing(
                    new ServerPing.Protocol("Minecraft", ppe.getConnection().getVersion()),
                    new ServerPing.Players(maxPlayers, playercount, infos),
                    new TextComponent(srv.getMotd().replaceAll("&", "\u00A7").replaceAll("\u00A7n", "\n").replaceAll("\\\\&", "&")),
                    srv.getFavicon()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
