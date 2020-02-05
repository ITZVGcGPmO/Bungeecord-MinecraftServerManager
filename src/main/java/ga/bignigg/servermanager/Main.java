package ga.bignigg.servermanager;
import ga.bignigg.servermanager.commands.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import static ga.bignigg.servermanager.Utils.*;

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
        defaultserver = this.getProxy().getConfig().getListeners().iterator().next().getServerPriority().get(0);
        log = getLogger();
        config = loadConfigFile("config.yml");
        messages = loadConfigFile("messages.yml");
        // force mojang EULA acceptance
        if (!config.getBoolean("mojang_eula") ) {
            Scanner myObj = new Scanner(System.in);
            log.warning(msg("accept_eula"));
            if (myObj.nextLine().toLowerCase().matches("true")){
                log.info(msg("eula_accepted"));
                sConf("mojang_eula", true);
            } else {
                log.info(msg("eula_denied"));
                ProxyServer.getInstance().stop();
            }
        }
        // load server directory + files and such
        srvrsdir = new File(config.getString("servers_dir"));
        if (!srvrsdir.exists()) { srvrsdir.mkdirs(); }
        bindir = new File(plugin.getDataFolder().toString()+File.separator+"bin");
        if (!bindir.exists()) { bindir.mkdirs(); }
        // default server configs
        srvdefsdir = new File(config.getString("defaults_dir"));
        if (!srvdefsdir.exists()) {
            srvdefsdir.mkdirs();
            String base = config.getString("defaults_dir")+File.separator;
            new File(base+"plugins").mkdir();
            writeTextFile(base+"eula.txt","eula=true" );
            writeTextFile(base+"server.properties", "spawn-protection=0\nquery.port=%serverport%\nsnooper-enabled=false\nmax-players=999\nserver-port=%serverport%\nserver-ip="+config.getString("default_bind")+"\nonline-mode=false\nmotd=%servername% via Bungee-MinecraftServerManager");
            writeTextFile(base+"spigot.yml", "config-version: 12\nsettings:\n  bungeecord: true\n");
        }
        // register bungeecord commands
        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, new ListenerClass());
        pluginManager.registerCommand(this, new CreateServer());
        pluginManager.registerCommand(this, new DeleteServer());
        pluginManager.registerCommand(this, new ExecuteServer());
        pluginManager.registerCommand(this, new FaviconServer());
        pluginManager.registerCommand(this, new MotdServer());
        pluginManager.registerCommand(this, new RestartServer());
        pluginManager.registerCommand(this, new StartServer());
        pluginManager.registerCommand(this, new StopServer());
        // remove default bungeecord servers
        if (config.getBoolean("deregester_default")) {
            ProxyServer.getInstance().getServers().clear();
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
        for (ServerThread th: serverThreadHashMap.values()) {
            try {
                th.stopServer();
            } catch (Exception ignored) {}
        }
    }
}
