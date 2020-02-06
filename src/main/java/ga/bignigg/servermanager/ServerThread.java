package ga.bignigg.servermanager;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ga.bignigg.servermanager.Main.*;
import static ga.bignigg.servermanager.Utils.*;

public class ServerThread {
    private ExecutorService service = Executors.newCachedThreadPool();
    private ScheduledFuture<?> sched_shutdown;
    private String servername, serverdir, jarfile, mc_dist, mc_ver, dist_ver, srvmotd, srvip;
    private Favicon srvicon = null;
    private int srvport, srvmaxpl;
    private BufferedWriter writer;
    private boolean auto_reboot_flag = false;
    private CountDownLatch runlatch = new CountDownLatch(0);
    private CountDownLatch done_load = new CountDownLatch(0);
    public ServerThread(String servername) {
        this.servername = servername;
        this.serverdir = srvrsdir + File.separator + servername;
    }
    public void startSchShutdown() {
        if (config.getInt("srv_stop_aft")>0 && ProxyServer.getInstance().getServers().get(servername).getPlayers().size()==0){
            log.info("start scheduled shutdown");
            sched_shutdown = Executors.newScheduledThreadPool(1).schedule(() -> {
                if (ProxyServer.getInstance().getServers().get(servername).getPlayers().size()==0) {
                    try {
                        writeCmd("say "+msg("sched_shutdown_msg").replace("%time%", ""+config.getInt("srv_stop_aft")));
                        if (config.getBoolean("shutdown_proxyserver") && ProxyServer.getInstance().getPlayers().size()==0) {
                            ProxyServer.getInstance().stop();
                        } else {
                            stopServer();
                        }
                    } catch (Exception ignored) {}
                }
            }, config.getInt("srv_stop_aft"), TimeUnit.SECONDS);
        }
    }
    public void endSchShutdown() {
        if (sched_shutdown!=null) { sched_shutdown.cancel(false); }
        if (config.getInt("srv_stop_aft")>0) {
            try {
                startServer();
            } catch (Exception ignored) { }
            try {
                done_load.await(ProxyServer.getInstance().getConfig().getTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) { }
        }
    }

    public void deleteServer() {
        try { stopServer(); }
        catch (Exception ignored) { }
        ProxyServer.getInstance().getServers().remove(servername);
        serverThreadHashMap.remove(servername);
        deleteFolder(new File(serverdir));
    }

    public void setMotd(String motd) throws IOException {
        List<String> serverprop = Files.readAllLines(Paths.get(serverdir, "server.properties"));
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(serverdir+File.separator+"server.properties"), true));
        for (String line: serverprop) {
            out.write(line.replaceFirst("^motd=.*$", "motd="+motd));
            out.newLine();
        }
        out.close();
        loadServerData();
    }
    public String getMotd() {
        return srvmotd;
    }
    public Integer getMaxPlayers() {
        if (new File(serverdir + File.separator + "maxplayers_plusone").exists()) {
            return null;
        } else {
            return srvmaxpl;
        }
    }

    public void setFavicon(String url) throws Exception {
        if (url!=null) {
            downloadFile(url, serverdir, "server-icon.png");
        }
        File servericon = new File(serverdir + File.separator + "server-icon.png");
        if (servericon.exists()) {
            srvicon = Favicon.create(ImageIO.read(servericon));
        } else {
            srvicon = null;
        }
    }
    public Favicon getFavicon() { return srvicon; }

    public void loadServerData() {
        try {
            List<String> sjlines = Files.readAllLines(Paths.get(serverdir+File.separator+"serverjar.txt"));
            mc_dist = sjlines.get(0);
            mc_ver = sjlines.get(1);
            dist_ver = sjlines.get(2);
            if (mc_dist.equalsIgnoreCase("paper")) {
                jarfile = getPaper(mc_ver, dist_ver);
            } else {
                log.warning(msg("unsupported_mcdist").replace("%vdists%", "paper").replace("%dist%", mc_dist));
            }
            File sprop = new File(serverdir + File.separator + "server.properties");
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sprop)));
            Pattern p_motd = Pattern.compile("^motd=(.+)$");
            Pattern p_srvip = Pattern.compile("^server-ip=(.+)$");
            Pattern p_port = Pattern.compile("^server-port=(\\d+)$");
            Pattern p_maxpl = Pattern.compile("^max-players=(\\d+)$");
            Matcher m;
            String aLine;
            while ((aLine = in.readLine()) != null) {
                m = p_motd.matcher(aLine);
                if (m.find()) { srvmotd = m.group(1); }
                m = p_srvip.matcher(aLine);
                if (m.find()) { srvip = m.group(1); }
                m = p_port.matcher(aLine);
                if (m.find()) { srvport = Integer.parseInt(m.group(1)); }
                m = p_maxpl.matcher(aLine);
                if (m.find()) { srvmaxpl = Integer.parseInt(m.group(1)); }
            }
            in.close();
            if (!ProxyServer.getInstance().getServers().containsKey(servername)) {
                ProxyServer.getInstance().getServers().put(servername, ProxyServer.getInstance().constructServerInfo(
                        servername, new InetSocketAddress(srvip, srvport), "",
                        new File(serverdir + File.separator + "bungee_restricted").exists()));
            }
            setFavicon(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServer() throws Exception {
        if (runlatch.getCount()==1) {
            auto_reboot_flag = false;
            writeCmd("stop");
            writeCmd("end");
            runlatch.await();
        } else {
            throw new Exception();
        }
    }
    public void writeCmd(String command) {
        log.info("sending \""+command+"\" to "+servername);
        try {
            writer.write(command);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() throws Exception {
        if (runlatch.getCount()==1) {
            throw new Exception();
        } else {
            if (sched_shutdown!=null) { sched_shutdown.cancel(false); }
            runlatch = new CountDownLatch(1);
            done_load = new CountDownLatch(1);
            log.info("starting server "+servername);
            loadServerData();
            service.submit(new Thread(() -> {
                try {
                    auto_reboot_flag = true;
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    processBuilder.command("java", "-jar", jarfile, "nogui");
                    processBuilder.directory(new File(serverdir));
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    String line;
                    String lastPrint = null;
                    while ((line = reader.readLine()) != null) {
                        String pline = line.replaceFirst("^\\[\\d+:\\d+:\\d+ \\w+]: ", "");
                        if (!pline.equalsIgnoreCase(lastPrint)) { // stop console spam
                            if (done_load.getCount()==1 && line.contains(""+srvip)) {
                                ProxyServer.getInstance().getServerInfo(servername).ping((ping, err) -> done_load.countDown());
//                                done_load.countDown();
                            }
                            log.info("["+servername+"] "+pline);
                            lastPrint = pline;
                        }
                    }
                    int exitCode = process.waitFor();
                    log.info("["+servername+"] Exited with code : "+exitCode);
                    runlatch.countDown();
                    if (auto_reboot_flag && runlatch.getCount()==0) {
                        log.info(msg("server_crashed").replace("%server%", servername));
                        startServer();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }
    }

    public Boolean isRunning() {
        return auto_reboot_flag;
    }

    public void generateServer(String dist, String mcver, String distver, boolean is_restricted) {
        // server name is args[0], optional mcversion is args[1]
        File serverdir = new File(srvrsdir + File.separator + servername);
        if (!serverdir.exists()) { serverdir.mkdir(); }
        copyFolder(srvdefsdir, serverdir, servername, getAPort().toString());
        writeTextFile(serverdir.toString()+File.separator+"serverjar.txt",dist+"\n"+mcver+"\n"+distver );
        if (is_restricted) {
            writeTextFile(serverdir+File.separator+"bungee_restricted");
        }
        loadServerData();
    }
}
