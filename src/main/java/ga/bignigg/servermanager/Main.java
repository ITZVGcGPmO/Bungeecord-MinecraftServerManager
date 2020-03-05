package ga.bignigg.servermanager;
import ga.bignigg.servermanager.commands.*;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static ga.bignigg.servermanager.Utils.getServerClass;
import static ga.bignigg.servermanager.Utils.loadConfig;

public class Main extends Plugin {
    public static HashMap<String, ServerThread> serverThreadHashMap = new HashMap<>();
    public static Plugin plugin;
    public static Logger log;
    public static String defaultserver;
    public static Configuration config, messages;
    public static ConfigurationProvider cProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
    public static File srvrsdir, srvdefsdir, bindir;

    @Override
    public void onEnable() {
        plugin = this;
        log = getLogger();
        loadConfig();
        // register bungeecord commands
        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, new ListenerClass());
        pluginManager.registerCommand(this, new CreateServer());
        pluginManager.registerCommand(this, new DeleteServer());
        pluginManager.registerCommand(this, new ExecuteServer());
        pluginManager.registerCommand(this, new FaviconServer());
        pluginManager.registerCommand(this, new MotdServer());
        pluginManager.registerCommand(this, new MSMCommand());
        pluginManager.registerCommand(this, new RelogCommand());
        pluginManager.registerCommand(this, new RestartServer());
        pluginManager.registerCommand(this, new StartServer());
        pluginManager.registerCommand(this, new StopServer());
        // remove default bungeecord servers
        if (config.getBoolean("deregester_default")) {
            plugin.getProxy().getServers().clear();
        }
        // autoload servers
        try {
            AtomicInteger counter = new AtomicInteger();
            for (String servername:
                    Objects.requireNonNull(srvrsdir.list((current, name) -> new File(current, name+File.separator+"serverjar.txt").isFile()))) {
                getServerClass(servername);
                counter.set(counter.get() + 1);
                log.info("loading server "+servername);
                new Thread(() -> {
                    try {
                        getServerClass(servername).loadServerData();
                        if (config.getInt("srv_stop_aft")<=0 || config.getStringList("exempt_srvstop").contains(servername)) {
                            getServerClass(servername).startServer();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    counter.set(counter.get() - 1);
                    log.info("loaded server "+servername);
                }).start();
            }
            while (counter.get()>0) {
                Thread.sleep(100);
            }
            log.info("finished loading all servers");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDisable() {
        AtomicInteger counter = new AtomicInteger();
        for (ServerThread th: serverThreadHashMap.values()) {
            counter.set(counter.get() + 1);
            try {
                new Thread(() -> {
                    try {
                        th.stopServer();
                    } catch (Exception ignored) { }
                    counter.set(counter.get() - 1);
                }).start();
            } catch (Exception ignored) {}
        }
        while (counter.get()>0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) { }
        }
    }
}
