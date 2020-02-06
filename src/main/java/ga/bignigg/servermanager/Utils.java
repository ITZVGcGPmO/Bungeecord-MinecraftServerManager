package ga.bignigg.servermanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jcabi.aspects.Cacheable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ga.bignigg.servermanager.Main.*;

public class Utils {
    public static Configuration loadConfigFile(String name)
    {
        File file = new File(Main.plugin.getDataFolder(), name);
        if (!Main.plugin.getDataFolder().exists())
        {
            Main.plugin.getDataFolder().mkdir();
        }
        try
        {
            if (!file.exists())
            {
                Files.copy(Main.plugin.getResourceAsStream(name), file.toPath());
            }
            return cProvider.load(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
    public static void sConf(String path, Object value) {
        config.set(path, value);
        try {
            cProvider.save(config, new File(Main.plugin.getDataFolder(), "config.yml"));
            log.info("Saved "+path+" as "+value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String msg(String message) {
        return messages.getString(message);
    }

    public static void sendmsg(CommandSender s, String message, ChatColor color) {
        String text = msg("message_prefix")+message;
        s.sendMessage(new ComponentBuilder(text).color(color).create());
        if (ProxyServer.getInstance().getConsole()!=s) {
            ProxyServer.getInstance().getConsole().sendMessage(new ComponentBuilder(s.getName()+": "+text).color(color).create());
        }
    }

    public static String downloadFile(String fileURL, String saveDir) throws IOException {
        return downloadFile(fileURL, saveDir, "");
    }
    public static String downloadFile(String fileURL, String saveDir, String fileName) throws IOException {
        log.info(msg("downloading_file").replace("%fileURL%", fileURL).replace("%saveDir%", saveDir));
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestProperty("User-Agent", "BungeeServerManager");
        int responseCode = httpConn.getResponseCode();
        File svedir = new File(saveDir);
        if (!svedir.exists()) { svedir.mkdirs(); }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();
            int index = disposition.indexOf("filename=");
            if (index > 0 && fileName.equals("")) {
                fileName = disposition.substring(index + 9);
            }
            String saveFilePath = saveDir + File.separator + fileName;
            File f = new File(saveFilePath);
            if(!f.exists()) {
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);
                InputStream inputStream = httpConn.getInputStream();
                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                log.info(msg("download_saved").replace("%saveFilePath%", saveFilePath));
            } else {
                log.info(msg("download_alreadyexist").replace("%saveFilePath%", saveFilePath));
            }
            return f.getAbsolutePath();
        } else {
            log.info(msg("download_httperr").replace("%responseCode%", ""+responseCode));
        }
        httpConn.disconnect();
        return null;
    }
    public static JsonObject readJson(String sURL) throws IOException {
        // Connect to the URL using java's native library
        URL url = new URL(sURL);
        URLConnection request = url.openConnection();
        request.setRequestProperty("User-Agent", "BungeeServerManager");
        request.connect();
        // Convert to a JSON object to print data
        JsonParser jp = new JsonParser(); //from gson
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        return root.getAsJsonObject(); //May be an array, may be an object.
    }
    public static Integer getAPort() {
        ServerSocket ssock = null;
        try {
            ssock = new ServerSocket(0);
            Integer port = ssock.getLocalPort();
            ssock.close();
            return port;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void writeTextFile(String path) {
        writeTextFile(path, "");
    }
    public static void writeTextFile(String path, String data) {
        try {
            Path pth = Paths.get(path);
            new File(pth.getParent().toString()).mkdirs();
            Files.write(pth, data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void copyFolder(File src, File dest, String servername, String serverport) {
        // checks
        if(src==null || dest==null)
            return;
        if(!src.isDirectory())
            return;
        if(dest.exists()){
            if(!dest.isDirectory()){
                //System.out.println("destination not a folder " + dest);
                return;
            }
        } else {
            dest.mkdir();
        }

        if(src.listFiles()==null || src.listFiles().length==0)
            return;

        for(File file: src.listFiles()){
            File fileDest = new File(dest, file.getName());
            //System.out.println(fileDest.getAbsolutePath());
            if(file.isDirectory()){
                copyFolder(file, fileDest, servername, serverport);
            } else {
                if(fileDest.exists())
                    continue;
                try {
                    if (file.getName().endsWith(".yml") || file.getName().endsWith(".properties")) {
                        FileInputStream fis = new FileInputStream(file);
                        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
                        FileWriter fstream = new FileWriter(fileDest, true);
                        BufferedWriter out = new BufferedWriter(fstream);
                        String aLine = null;
                        while ((aLine = in.readLine()) != null) {
                            //Process each line and add output to Dest.txt file
                            out.write(aLine.replace("%servername%", servername).replace("%serverport%", serverport));
                            out.newLine();
                        }
                        // do not forget to close the buffer reader
                        in.close();
                        // close buffer writer
                        out.close();
                    } else {
                        Files.copy(file.toPath(), fileDest.toPath());
                    }
                    log.info("copied "+file+" to "+fileDest);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }
    }
    @Cacheable(lifetime = 12, unit = TimeUnit.HOURS)
    public static String getPaper(String mc_ver, String dist_ver) throws IOException {
        if (mc_ver.equalsIgnoreCase("latest")) {
            mc_ver = getLatestPaperMCVer();
        }
        String saveDir = bindir.toString()+File.separator+mc_ver.replaceAll("\\.", "_");
        String file = downloadFile("https://papermc.io/api/v1/paper/"+mc_ver+"/"+dist_ver+"/download", saveDir);
        return file;
    }
    @Cacheable(lifetime = 12, unit = TimeUnit.HOURS)
    public static String getLatestPaperMCVer() {
        try {
            JsonArray versions = readJson("https://papermc.io/api/v1/paper/").get("versions").getAsJsonArray();
            for (int i = 0; i < versions.size(); i++) {
                String version = versions.get(i).getAsString();
                if (version.matches("\\d+\\.\\d+\\.\\d+")) {
                    return version;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static ServerThread getServerClass(String servername) {
        if (!serverThreadHashMap.containsKey(servername)) {
            serverThreadHashMap.put(servername, new ServerThread(servername));
        }
        return serverThreadHashMap.get(servername);
    }
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    public static void dumpError(CommandSender s, Exception e) {
        String errfile = plugin.getDataFolder().toString() + File.separator + "errors" + File.separator + "err_" + System.currentTimeMillis();
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        writeTextFile(errfile, sw.toString());
        sendmsg(s, msg("command_error").replace("%errfile%", errfile), ChatColor.RED);
    }
    public static String getServerByHostname(String hostname) {
        String servername = hostname.split("\\.", 2)[0];
        if (serverThreadHashMap.containsKey(servername)) {
            return servername;
        } else {
            return defaultserver;
        }
    }

    public static List<String> getSuggestions(ProxiedPlayer p, String permtype, Boolean onlineYayNay) {
        List<String> rets = new ArrayList();
        for (Map.Entry<String,ServerThread> entry: serverThreadHashMap.entrySet()) {
            if (onlineYayNay==null || entry.getValue().isRunning()==onlineYayNay) {
                if (p.hasPermission("msm."+entry.getKey()+"."+permtype)) {
                    rets.add(entry.getKey());
                }
            }
        }
        return rets;
    }
}
